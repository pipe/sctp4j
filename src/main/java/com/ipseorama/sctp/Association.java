/*
 * Copyright (C) 2014 Westhawk Ltd<thp@westhawk.co.uk>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.ipseorama.sctp;

import com.ipseorama.sctp.messages.exceptions.UnreadyAssociationException;
import com.ipseorama.sctp.messages.exceptions.StaleCookieException;
import com.ipseorama.base.dataChannel.DECP.DCOpen;
import com.ipseorama.sctp.messages.*;

import com.ipseorama.sctp.messages.exceptions.*;
import com.ipseorama.sctp.messages.params.StaleCookieError;
import com.ipseorama.sctp.small.BlockingSCTPStream;

import com.phono.srtplight.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.bouncycastle.crypto.tls.DatagramTransport;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
abstract public class Association {

    /**
     * <code>
     *                     -----          -------- (from any state)
     *                   /       \      /  rcv ABORT      [ABORT]
     *  rcv INIT        |         |    |   ----------  or ----------
     *  --------------- |         v    v   delete TCB     snd ABORT
     *  generate Cookie  \    +---------+                 delete TCB
     *  snd INIT ACK       ---|  CLOSED |
     *                        +---------+
     *                         /      \      [ASSOCIATE]
     *                        /        \     ---------------
     *                       |          |    create TCB
     *                       |          |    snd INIT
     *                       |          |    strt init timer
     *        rcv valid      |          |
     *      COOKIE  ECHO     |          v
     *  (1) ---------------- |      +------------+
     *      create TCB       |      | COOKIE-WAIT| (2)
     *      snd COOKIE ACK   |      +------------+
     *                       |          |
     *                       |          |    rcv INIT ACK
     *                       |          |    -----------------
     *                       |          |    snd COOKIE ECHO
     *                       |          |    stop init timer
     *                       |          |    strt cookie timer
     *                       |          v
     *                       |      +--------------+
     *                       |      | COOKIE-ECHOED| (3)
     *                       |      +--------------+
     *                       |          |
     *                       |          |    rcv COOKIE ACK
     *                       |          |    -----------------
     *                       |          |    stop cookie timer
     *                       v          v
     *                     +---------------+
     *                     |  ESTABLISHED  |
     *                     +---------------+
     * </code>
     */
    public enum State {

        COOKIEWAIT, COOKIEECHOED, ESTABLISHED,
        SHUTDOWNPENDING, SHUTDOWNSENT, SHUTDOWNRECEIVED,
        SHUTDOWNACKSENT, CLOSED
    };

    private byte[] _supportedExtensions = {};
    /*
     For what it is worth, here's the logic as to why we don't have any supported extensions.
     { 
     ASCONF, // this is ICE's job so we never send ASCONF or 
     ASCONF-ACK, // ASCONF-ACK
     FORWARDTSN, // we may end up wanting this - it supports partial reliability - aka giving up..
     PKTDROP, // thie is an optional performance enhancement especially valuable for middleboxes (we aren't one)
     RE-CONFIG, // not sure about this - but lets assume for now that the w3c interface doesn't support stream resets.
     AUTH // Assume DTLS will cover this for us if we never send ASCONF packets.
     */

    public static int COOKIESIZE = 32;
    private static long VALIDCOOKIELIFE = 60000;
    /*
     RTO.Initial - 3 seconds
     RTO.Min - 1 second
     RTO.Max - 60 seconds
     Max.Burst - 4
     RTO.Alpha - 1/8
     RTO.Beta - 1/4
     Valid.Cookie.Life - 60 seconds
     Association.Max.Retrans - 10 attempts
     Path.Max.Retrans - 5 attempts (per destination address)
     Max.Init.Retransmits - 8 attempts
     HB.interval - 30 seconds
     HB.Max.Burst - 1
     */
    protected final DatagramTransport _transp;
    private Thread _rcv;
    private int _peerVerTag;
    protected int _myVerTag;
    private final SecureRandom _random;
    private long _winCredit;
    private long _farTSN;
    private int MAXSTREAMS = 1000;
    private int _maxOutStreams;
    private int _maxInStreams;
    final static int MAXBUFF = 128 * 1024;
    public long _nearTSN;
    private int _srcPort;
    private int _destPort;
    final private HashMap<Integer, SCTPStream> _streams;
    private final AssociationListener _al;
    private HashMap<Long, DataChunk> _outbound;
    private State _state;
    private HashMap<Long, DataChunk> _holdingPen;

    ;

    class CookieHolder {

        byte[] cookieData;
        long cookieTime;
    };
    private final ArrayList<CookieHolder> _cookies = new ArrayList();

    byte[] getSupportedExtensions() {
        return _supportedExtensions;
    }

    byte[] getUnionSupportedExtensions(byte far[]) {
        ByteBuffer unionbb = ByteBuffer.allocate(far.length);
        for (int f = 0; f < far.length; f++) {
            for (int n = 0; n < _supportedExtensions.length; n++) {
                if (_supportedExtensions[n] == far[f]) {
                    unionbb.put(far[f]);
                }
            }
        }
        byte[] res = new byte[unionbb.position()];
        unionbb.get(res);
        return res;
    }

    void deal(Packet rec) throws Exception {
        List<Chunk> cl;
        rec.validate(this);
        cl = rec.getChunkList();
        for (Chunk c : cl) {
            c.validate();
        }
        if (cl.get(0).getType() == Chunk.INIT) {
            _srcPort = rec.getDestPort();
            _destPort = rec.getSrcPort();
        }
        for (Chunk c : cl) {
            if (!deal(c)) {
                break; // drop the rest of the packet.
            }
        }
    }

    void startRcv() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] buf = new byte[_transp.getReceiveLimit()];
                    while (_rcv != null) {
                        int length = _transp.receive(buf, 0, buf.length, _transp.getReceiveLimit());
                        String b = Packet.getHex(buf, length);
                        Log.verb("DTLS message recieved\n" + b.toString());
                        ByteBuffer pbb = ByteBuffer.wrap(buf);
                        pbb.limit(length);
                        Packet rec = new Packet(pbb);
                        Log.debug("SCTP message parsed\n" + rec.toString());
                        deal(rec);
                    }
                    Log.verb("SCTP message recv null\n Shutting down.");

                    _transp.close();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        };
        _rcv = new Thread(r);
        _rcv.start();
    }

    public Association(DatagramTransport transport, AssociationListener al) {
        Log.setLevel(Log.ALL);
        Log.debug("Created an Associaction of type: " + this.getClass().getSimpleName());

        _al = al;
        _random = new SecureRandom();
        _myVerTag = _random.nextInt();
        _transp = transport;
        _streams = new HashMap();
        _outbound = new HashMap<Long, DataChunk>();
        _holdingPen = new HashMap<Long, DataChunk>();

        _state = State.CLOSED;
        if (_transp != null) {
            startRcv();
        } else {
            Log.error("Created an Associaction with a null transport somehow...");
        }
    }

    public void send(Chunk c[]) throws SctpPacketFormatException, IOException {
        if ((c != null) && (c.length > 0)) {
            ByteBuffer obb = mkPkt(c);
            Log.verb("sending packet" + Packet.getHex(obb));
            _transp.send(obb.array(), obb.arrayOffset(), obb.position());
        } else {
            Log.warn("Heh ? Blocked empty packet send()");
        }
    }

    /**
     *
     * @param c - Chunk to be processed
     * @return valid - false if the remaining chunks of the packet should be
     * ignored.
     * @throws IOException
     * @throws SctpPacketFormatException
     */
    private boolean deal(Chunk c) throws IOException, SctpPacketFormatException {
        int ty = c.getType();
        boolean ret = true;
        Chunk[] reply = null;
        switch (ty) {
            case Chunk.INIT:
                if (_state == State.CLOSED) {
                    InitChunk init = (InitChunk) c;
                    reply = inboundInit(init);
                } else {
                    Log.debug("Got an INIT when not closed - ignoring it");
                }
                break;
            case Chunk.COOKIE_ECHO:
                Log.debug("got data " + c.toString());
                reply = cookieEchoDeal((CookieEchoChunk) c);

                if (reply.length > 0) {
                    ret = !(reply[0] instanceof ErrorChunk); // ignore any following data chunk. 
                }
                break;
            case Chunk.DATA:
                Log.debug("got data " + c.toString());
                reply = dataDeal((DataChunk) c);
                break;
            case Chunk.ABORT:
                // no reply we should just bail I think.
                if (null != _al) {
                    _al.onDisAssociated(this);
                }
                _rcv = null;
                _transp.close();
                break;
            case Chunk.HEARTBEAT:
                reply = ((HeartBeatChunk) c).mkReply();
                break;
            case Chunk.SACK:
                Log.debug("got tsak for TSN " + ((SackChunk) c).getCumuTSNAck());
                reply = sackDeal((SackChunk) c);
                // fix the outbound list here
                break;
        }
        if (reply != null) {
            // theoretically could be multiple DATA in a single packet - 
            // we'd send multiple SACKs in reply - ToDo fix that
            send(reply);
        }
        return ret;
    }

    ByteBuffer mkPkt(Chunk[] cs) throws SctpPacketFormatException {
        Packet ob = new Packet(_srcPort, _destPort, _peerVerTag);
        for (Chunk r : cs) {
            Log.debug("adding chunk to outbound packet " + r.toString());
            ob.getChunkList().add(r);
            //todo - this needs to workout if all the chunks will fit...
        }
        ByteBuffer obb = ob.getByteBuffer();
        return obb;
    }

    public int getPeerVerTag() {
        return _peerVerTag;
    }

    public int getMyVerTag() {
        return _myVerTag;
    }

    /*
     Ok - confession here - we are not following the RFC. 
     We don't encode a pile of stuff into the cookie and decode it
     when we get the cookie back, then use that data to initialize the Association.
     The rationale in the RFC is to protect the assocaition from resource exhaustion
     by fake cookies from bad guys - which makes sense if you are a naked SCTP stack on
     the internet accepting UDP packets (or IP ones).
     We on the other hand have already been through 2 levels of validation with ICE and DTLS,
     and have already committed a pile of resource to this connection, so 32 bytes more won't kill us.
    
     The only downside is that if the far end spams us with a pile of inits at speed, we may erase one that we've
     replied to and that was about to be a happy camper. Shrug.
     */
    private CookieHolder checkCookieEcho(byte[] cookieData) {
        CookieHolder same = null;
        Iterator<CookieHolder> cooks = _cookies.iterator();
        while (cooks.hasNext()) {
            CookieHolder cookie = cooks.next();
            byte[] cd = cookie.cookieData;
            if (cd.length == cookieData.length) {
                int i = 0;
                while (i < cd.length) {
                    if (cd[i] != cookieData[i]) {
                        break;
                    }
                    i++;
                }
                if (i == cd.length) {
                    same = cookie;
                    break;
                }
            }
        }
        return same;
    }

    private long howStaleIsMyCookie(CookieHolder cookie) {
        long ret = 0;
        long now = System.currentTimeMillis();

        if ((now - cookie.cookieTime) < VALIDCOOKIELIFE) {
            ret = 0;
        } else {
            ret = (now - cookie.cookieTime) - VALIDCOOKIELIFE;
        }
        return ret;
    }

    protected Chunk[] inboundInit(InitChunk init) {
        Chunk[] reply = null;
        _peerVerTag = init.getInitiateTag();
        _winCredit = init.getAdRecWinCredit();
        _farTSN = init.getInitialTSN()-1;
        _nearTSN = _random.nextInt(Integer.MAX_VALUE);
        _maxOutStreams = Math.min(init.getNumInStreams(), MAXSTREAMS);
        _maxInStreams = Math.min(init.getNumOutStreams(), MAXSTREAMS);
        InitAckChunk iac = new InitAckChunk();
        iac.setAdRecWinCredit(MAXBUFF);
        iac.setNumInStreams(_maxInStreams);
        iac.setNumOutStreams(_maxOutStreams);
        iac.setInitialTSN((int) _nearTSN);
        iac.setInitiateTag(_myVerTag);
        CookieHolder cookie = new CookieHolder();
        cookie.cookieData = new byte[Association.COOKIESIZE];
        cookie.cookieTime = System.currentTimeMillis();
        _random.nextBytes(cookie.cookieData);
        iac.setCookie(cookie.cookieData);
        _cookies.add(cookie);

        byte[] fse = init.getFarSupportedExtensions();
        if (fse != null) {
            iac.setSupportedExtensions(this.getUnionSupportedExtensions(fse));
        }
        reply = new Chunk[1];
        reply[0] = iac;
        return reply;
    }

    private void ingest(DataChunk dc, ArrayList<Chunk> rep) {
        Log.verb("ingesting " + dc.toString());

        Integer sno = dc.getStreamId();
        long tsn = dc.getTsn();
        SCTPStream in = _streams.get(sno);
        if (in == null) {
            in = mkStream(sno);
            _streams.put(sno, in);
        }
        Chunk[] repa;
        if (dc.getDCEP() != null) {
            repa = dcepDeal(in, dc, dc.getDCEP());
            if (_al != null) {
                _al.onStream(in);
            }
        } else {
            repa = in.append(dc);
        }
        if (repa != null) {
            for (Chunk r : repa) {
                rep.add(r);
            }
        }
        in.inbound(dc);
        _farTSN = tsn;
    }

    private Chunk[] dataDeal(DataChunk dc) {
        ArrayList<Chunk> rep = new ArrayList();
        ArrayList<Long> duplicates = new ArrayList();

        Chunk dummy[] = new Chunk[0];
        long tsn = dc.getTsn();
        Long tsn_L = new Long(tsn);
        if (tsn > _farTSN) {
            // put it in the pen.
            DataChunk dup = _holdingPen.get(tsn_L) ;
            if (dup!=null){
                duplicates.add(tsn_L);
            } else {
                _holdingPen.put(tsn_L, dc);
            }
            // now see if we can deliver anything new to the streams
            boolean gap = false;
            for (long t = _farTSN + 1; !gap; t++) {
                Long l = new Long(t);
                dc = _holdingPen.remove(l);
                if (dc != null) {
                    ingest(dc, rep);
                } else {
                    Log.verb("gap in inbound tsns at "+t);
                    gap = true;
                }
            }
        } else {
            // probably wrong now.. 
            Log.warn("Already seen . " + tsn + " expecting " + (_farTSN));
            duplicates.add(tsn_L);
        }
        ArrayList<Long> l = new ArrayList();
        l.addAll(_holdingPen.keySet());
        Collections.sort(l);

        SackChunk sack = mkSack(l,duplicates);
        rep.add(sack);
        return rep.toArray(dummy);
    }

    private Chunk[] dcepDeal(SCTPStream s, DataChunk dc, DCOpen dcep) {
        Chunk[] rep = null;
        Log.debug("dealing with a decp for stream " + dc.getDataAsString());
        if (!dcep.isAck()) {
            Log.debug("decp is not an ack... ");

            SCTPStreamBehaviour behave = dcep.mkStreamBehaviour();
            s.setBehave(behave);
            s.setLabel(dcep.getLabel());

            rep = new Chunk[1];
            DataChunk ack = dc.mkAck(dcep);
            s.outbound(ack);
            ack.setTsn(_nearTSN++);
            // check rollover - will break at maxint.
            rep[0] = ack;

        }
        return rep;
    }

    /**
     * <code>
     * 2)  Authenticate the State Cookie as one that it previously generated
     * by comparing the computed MAC against the one carried in the
     * State Cookie.  If this comparison fails, the SCTP packet,
     * including the COOKIE ECHO and any DATA chunks, should be silently
     * discarded,
     *
     * 3)  Compare the port numbers and the Verification Tag contained
     * within the COOKIE ECHO chunk to the actual port numbers and the
     * Verification Tag within the SCTP common header of the received
     * packet.  If these values do not match, the packet MUST be
     * silently discarded.
     *
     * 4)  Compare the creation timestamp in the State Cookie to the current
     * local time.  If the elapsed time is longer than the lifespan
     * carried in the State Cookie, then the packet, including the
     * COOKIE ECHO and any attached DATA chunks, SHOULD be discarded,
     * and the endpoint MUST transmit an ERROR chunk with a "Stale
     * Cookie" error cause to the peer endpoint.
     *
     * 5)  If the State Cookie is valid, create an association to the sender
     * of the COOKIE ECHO chunk with the information in the TCB data
     * carried in the COOKIE ECHO and enter the ESTABLISHED state.
     *
     * 6)  Send a COOKIE ACK chunk to the peer acknowledging receipt of the
     * COOKIE ECHO.  The COOKIE ACK MAY be bundled with an outbound DATA
     * chunk or SACK chunk; however, the COOKIE ACK MUST be the first
     * chunk in the SCTP packet.
     *
     * 7)  Immediately acknowledge any DATA chunk bundled with the COOKIE
     * ECHO with a SACK (subsequent DATA chunk acknowledgement should
     * follow the rules defined in Section 6.2).  As mentioned in step
     * 6, if the SACK is bundled with the COOKIE ACK, the COOKIE ACK
     * MUST appear first in the SCTP packet.
     * </code>
     */
    private Chunk[] cookieEchoDeal(CookieEchoChunk echo) {
        Chunk[] reply = new Chunk[0];
        if (_state == State.CLOSED) {
            // Authenticate the State Cookie
            CookieHolder cookie;
            if (null != (cookie = checkCookieEcho(echo.getCookieData()))) {
                // Compare the creation timestamp in the State Cookie to the current local time.
                long howStale = howStaleIsMyCookie(cookie);
                if (howStale == 0) {
                    //enter the ESTABLISHED state
                    _state = State.ESTABLISHED;
                    /*
                     Send a COOKIE ACK chunk to the peer acknowledging receipt of the
                     COOKIE ECHO.  The COOKIE ACK MAY be bundled with an outbound DATA
                     chunk or SACK chunk; however, the COOKIE ACK MUST be the first
                     chunk in the SCTP packet.
                     */
                    if (null != _al) {
                        _al.onAssociated(this);
                    }
                    reply = new Chunk[1];
                    reply[0] = new CookieAckChunk();
                } else {
                    reply = new Chunk[1];
                    /* If the elapsed time is longer than the lifespan
                     * carried in the State Cookie, then the packet, including the
                     * COOKIE ECHO and any attached DATA chunks, SHOULD be discarded,
                     * and the endpoint MUST transmit an ERROR chunk with a "Stale
                     * Cookie" error cause to the peer endpoint.*/
                    StaleCookieError sce = new StaleCookieError();
                    sce.setMeasure(howStale * 1000);
                    ErrorChunk ec = new ErrorChunk(sce);
                    reply[0] = ec;
                }
            } else {
                Log.error("Got a COOKIE_ECHO that doesn't match any we sent. ?!?");
            }

        } else {
            Log.debug("Got an COOKIE_ECHO when not closed - ignoring it");
        }
        return reply;
    }

    private SackChunk mkSack(ArrayList<Long> pen,ArrayList<Long> dups) {
        // Notice that this is the dumbest possible sack implementation
        // it assumes no gaps, because we ignore unexpected packets in dataDeal()
        // mucho room for improvement....
        SackChunk ret = new SackChunk();
        ret.setCumuTSNAck(_farTSN);
        int stashcap = calcStashCap();
        ret.setArWin(MAXBUFF - stashcap);
        ret.setGaps(pen);
        ret.setDuplicates(dups);
        Log.debug("made SACK " + ret.toString());
        return ret;
    }

    private int calcStashCap() {
        int ret = 0;
        for (SCTPStream s : this._streams.values()) {
            ret += s.stashCap();
        }
        return ret;
    }

    public abstract void enqueue(DataChunk d);

    public abstract SCTPStream mkStream(int id);

    public SCTPStream mkStream(int sno, String label) throws StreamNumberInUseException, UnreadyAssociationException, SctpPacketFormatException, IOException {
        SCTPStream sout;
        if (canSend()) {
            synchronized (_streams) {
                sout = _streams.get(sno);
                if (sout != null) {
                    throw new StreamNumberInUseException();
                }
                sout = mkStream(sno);
                _streams.put(sno, sout);
            }
            DataChunk dcopen = DataChunk.mkDCOpen(label);
            sout.outbound(dcopen);
            dcopen.setTsn(_nearTSN++);
            Chunk[] hack = {dcopen};
            send(hack);
        } else {
            throw new UnreadyAssociationException();
        }
        return sout;
    }

    public int maxMessageSize() {
        return 1 << 16; // shrug - I don't know 
    }

    public boolean canSend() {
        boolean ok;
        switch (_state) {
            case ESTABLISHED:
                SHUTDOWNPENDING:
                SHUTDOWNRECEIVED:
                ok = true;
                break;
            default:
                ok = false;
        }
        return ok;
    }

    abstract public void sendAndBlock(SCTPMessage m) throws Exception;

    abstract public SCTPMessage makeMessage(byte[] bytes, BlockingSCTPStream aThis);

    abstract public SCTPMessage makeMessage(String s, BlockingSCTPStream aThis);

    abstract protected Chunk[] sackDeal(SackChunk sackChunk);

}
