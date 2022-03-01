/*
 * Copyright 2017 pi.pe gmbh .
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package pe.pi.sctp4j.sctp;

import pe.pi.sctp4j.sctp.messages.exceptions.SctpPacketFormatException;
import pe.pi.sctp4j.sctp.messages.exceptions.UnreadyAssociationException;

import pe.pi.sctp4j.sctp.messages.*;

import pe.pi.sctp4j.sctp.messages.params.StaleCookieError;

import com.phono.srtplight.Log;
import java.io.EOFException;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bouncycastle.tls.DatagramTransport;
import pe.pi.sctp4j.sctp.dataChannel.DECP.DCOpen;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
abstract public class Association {

    private final boolean _even;

    public abstract void associate() throws SctpPacketFormatException, IOException;



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

    private byte[] _supportedExtensions = {(byte) Chunk.RE_CONFIG};
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

    public static int COOKIESIZE = 40;
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
    final private ConcurrentHashMap<Integer, SCTPStream> _streams;
    private final AssociationListener _al;
    private HashMap<Long, DataChunk> _outbound;
    protected State _state;
    private HashMap<Long, DataChunk> _holdingPen;
    private static int TICK = 1000; // loop time in rcv
    protected static int __assocNo = 1;
    private ReconfigState reconfigState;

    private String peerId;

    class CookieHolder {

        byte[] cookieData;
        long cookieTime;
    };
    private final ArrayList<CookieHolder> _cookies = new ArrayList();

    protected byte[] getSupportedExtensions() { // this lets others swithc features off.
        return _supportedExtensions;
    }

    long getNearTSN() {
        return _nearTSN;
    }

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peer) {
        peerId = peer;
    }

    byte[] getUnionSupportedExtensions(byte far[]) {
        ByteBuffer unionbb = ByteBuffer.allocate(far.length);
        for (int f = 0; f < far.length; f++) {
            Log.verb("offered extension " + Chunk.typeLookup(far[f]));
            for (int n = 0; n < _supportedExtensions.length; n++) {
                Log.verb("supported extension " + Chunk.typeLookup(_supportedExtensions[n]));
                if (_supportedExtensions[n] == far[f]) {
                    Log.verb("matching extension " + Chunk.typeLookup(_supportedExtensions[n]));
                    unionbb.put(far[f]);
                }
            }
        }
        byte[] res = new byte[((Buffer)unionbb).position()];
        ((Buffer)unionbb).rewind();
        unionbb.get(res);
        Log.verb("union of extensions contains :" + Chunk.chunksToNames(res));
        return res;
    }

    void deal(Packet rec) throws Exception {
        List<Chunk> cl;
        Chunk[] s = {};
        ArrayList<Chunk> replies = new ArrayList();
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
            if (!deal(c, replies)) {
                break; // drop the rest of the packet.
            }
        }
        // find the highest sack.
        Optional<Chunk> hisack = replies.stream().filter((Chunk c) -> {
            return c.getType() == Chunk.SACK;
        })
                .sorted((Chunk a, Chunk b) -> {
                    return (int) (((SackChunk) b).getCumuTSNAck() - ((SackChunk) a).getCumuTSNAck());
                })
                .findFirst();
        // remove all sacks
        replies.removeIf((Chunk c) -> {
            return c.getType() == Chunk.SACK;
        });
        // insert the higest one first.
        if (hisack.isPresent()) {
            replies.add(0, hisack.get());
        }
        try {
            send(replies.toArray(s));
        } catch (java.io.EOFException end) {
            unexpectedClose(end);
        }
    }

    void startRcv() {
        final Association me = this;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                int length = -99;
                try {
                    byte[] buf = new byte[1500];
                    while (_rcv != null) {
                        try {
                            length = _transp.receive(buf, 0, buf.length, TICK);
                            if (length > 0) {
                                String b = Packet.getHex(buf, length);
                                Log.verb("DTLS message recieved\n" + b.toString());
                                ByteBuffer pbb = ByteBuffer.wrap(buf);
                                ((Buffer) pbb).limit(length);
                                Packet rec = new Packet(pbb);
                                Log.debug("SCTP message parsed\n" + rec.toString());
                                deal(rec);
                            } else {
                                Log.verb("Timeout -> short packet " + length);
                                //_rcv = null;
                            }
                        } catch (java.io.InterruptedIOException iox) {
                            ;// ignore. it should be a timeout.
                            Log.verb("tick time out");
                        }
                    }
                    Log.verb("SCTP message recv null\n Shutting down.");

                    _transp.close();

                } catch (java.io.EOFException eof) {
                    unexpectedClose(eof);
                } catch (IllegalArgumentException iex) {
                    Log.warn("Exception " + iex.getMessage());
                    Log.warn("Buffer Length was invalid " + length);
                } catch (Exception ex) {
                    Log.debug("Association rcv failed " + ex.getClass().getName() + " " + ex.getMessage());
                    ex.printStackTrace();

                }

            }
        };
        _rcv = new Thread(r);
        _rcv.setPriority(Thread.MAX_PRIORITY);
        _rcv.setName("AssocRcv" + __assocNo);
        _rcv.start();
    }

    public Association(DatagramTransport transport, AssociationListener al) {
        this(transport, al, false); // default is server
    }

    public Association(DatagramTransport transport, AssociationListener al, boolean client) {
        //Log.setLevel(Log.ALL);
        Log.debug("Created an Associaction of type: " + this.getClass().getSimpleName());
        _al = al;
        _random = new SecureRandom();
        _myVerTag = _random.nextInt();
        _transp = transport;
        _streams = new ConcurrentHashMap();
        _outbound = new HashMap<Long, DataChunk>();
        _holdingPen = new HashMap<Long, DataChunk>();
        _nearTSN = _random.nextInt(Integer.MAX_VALUE);
        _state = State.CLOSED;
        if (_transp != null) {
            startRcv();
        } else {
            Log.error("Created an Associaction with a null transport somehow...");
        }
        __assocNo++;
        /*
        the method used to determine which
   side uses odd or even is based on the underlying DTLS connection
   role: the side acting as the DTLS client MUST use Streams with even
   Stream Identifiers, the side acting as the DTLS server MUST use
   Streams with odd Stream Identifiers. */
        _even = client;

    }

    /**
     * override this and return false to disable the bi-directionalinit gamble
     * that webRTC expects. Only do this in testing. Production should have it
     * enabled since it also provides glare resolution.
     *
     * @return true
     */
    public boolean doBidirectionalInit() {
        return true;
    }

    public void sendHeartBeat() throws Exception {
        Chunk[] dub = new Chunk[1];
        dub[0] = new HeartBeatChunk();
        send(dub);
    }

    protected void send(Chunk c[]) throws SctpPacketFormatException, IOException {
        if ((c != null) && (c.length > 0)) {
            ByteBuffer obb = mkPkt(c);
            Log.verb("sending SCTP packet" + Packet.getHex(obb));
            _transp.send(obb.array(), obb.arrayOffset(), ((Buffer)obb).position());
        } else {
            Log.verb("Blocked empty packet send() - probably no response needed.");
        }
    }

    /**
     * decide if we want to do the webRTC specified bidirectional init _very_
     * useful to be able to switch this off for testing
     *
     * @return
     */
    private boolean acceptableStateForInboundInit() {
        boolean ret = false;
        if (doBidirectionalInit()) {
            ret = ((_state == State.CLOSED) || (_state == State.COOKIEWAIT) || (_state == State.COOKIEECHOED));
        } else {
            ret = (_state == State.CLOSED);
        }
        return ret;
    }

    /**
     *
     * @param c - Chunk to be processed
     * @return valid - false if the remaining chunks of the packet should be
     * ignored.
     * @throws IOException
     * @throws SctpPacketFormatException
     */
    private boolean deal(Chunk c, ArrayList<Chunk> replies) throws IOException, SctpPacketFormatException {
        int ty = c.getType();
        if (ty < 0) {
            // todo this is the wrong place to do this....
            Log.debug("fixing negative " + ty);
            ty = (ty & 0x7f) + 128;
            Log.debug("fixed negative " + ty);
        }
        boolean ret = true;
        State oldState = _state;
        Chunk[] reply = null;
        switch (ty) {
            case Chunk.INIT:
                if (acceptableStateForInboundInit()) {
                    InitChunk init = (InitChunk) c;
                    reply = inboundInit(init);
                } else {
                    Log.debug("Got an INIT when state was " + _state.name() + " - ignoring it for now ");
                }
                break;
            case Chunk.INITACK:
                Log.debug("got initack " + c.toString());
                if (_state == State.COOKIEWAIT) {
                    InitAckChunk iack = (InitAckChunk) c;
                    reply = iackDeal(iack);
                } else {
                    Log.debug("Got an INITACK when not waiting for it - ignoring it");
                }
                break;
            case Chunk.COOKIE_ECHO:
                Log.debug("got cookie echo " + c.toString());
                reply = cookieEchoDeal((CookieEchoChunk) c);
                if (reply.length > 0) {
                    ret = !(reply[0] instanceof ErrorChunk); // ignore any following data chunk. 
                }
                break;
            case Chunk.COOKIE_ACK:
                Log.debug("got cookie ack " + c.toString());
                if (_state == State.COOKIEECHOED) {
                    _state = State.ESTABLISHED;
                }
                break;
            case Chunk.DATA:
                Log.debug("got data " + c.toString());
                reply = dataDeal((DataChunk) c);
                break;
            case Chunk.ABORT:
                unexpectedClose(new EOFException("ABORT received"));
                oldState = _state; // prevent either of the statechange if's happenin
                // by this point, it is _all_ over! 
                ret = false;
                break;
            case Chunk.HEARTBEAT:
                Log.debug("got heartbeat " + c.toString());
                reply = ((HeartBeatChunk) c).mkReply();
                Log.debug("sending " + reply[0].typeLookup());
                break;
            case Chunk.SACK:
                Log.debug("got tsak for TSN " + ((SackChunk) c).getCumuTSNAck());
                reply = sackDeal((SackChunk) c);
                break;
            case Chunk.RE_CONFIG:
                reply = reconfigState.deal((ReConfigChunk) c);
                break;
        }
        if (reply != null) {
            for (Chunk r : reply) {
                replies.add(r);
            }
            // theoretically could be multiple DATA in a single packet - 
            // we'd send multiple SACKs in reply - ToDo fix that

        }
        if ((_state == State.ESTABLISHED) && (oldState != State.ESTABLISHED)) {
            if (null != _al) {
                _al.onAssociated(this);
            }
            reconfigState = new ReconfigState(this, _farTSN);

        }
        if ((oldState == State.ESTABLISHED) && (_state != State.ESTABLISHED)) {
            closeAllStreams();
            if (null != _al) {
                _al.onDisAssociated(this);
            }
        }
        return ret;
    }

    ByteBuffer mkPkt(Chunk[] cs) throws SctpPacketFormatException {
        Packet ob = new Packet(_srcPort, _destPort, _peerVerTag);
        // cookie ACKs _must_ come before data or sacks
        Arrays.asList(cs).stream()
                .filter((Chunk c) -> {
                    return c.getType() == Chunk.COOKIE_ACK;
                })
                .forEach((Chunk r) -> {
                    Log.debug("adding Cookie Ack chunk to outbound packet " + r.toString());
                    ob.getChunkList().add(r);
                }
                );
        Arrays.asList(cs).stream()
                .filter((Chunk c) -> {
                    return c.getType() != Chunk.COOKIE_ACK;
                })
                .forEach((Chunk r) -> {
                    Log.debug("adding normal chunk to outbound packet " + r.toString());
                    ob.getChunkList().add(r);
                }
                );
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

    public void sendInit() throws SctpPacketFormatException, IOException {
        InitChunk c = new InitChunk();
        c.setInitialTSN(this._nearTSN);
        c.setNumInStreams(this.MAXSTREAMS);
        c.setNumOutStreams(this.MAXSTREAMS);
        c.setAdRecWinCredit(this.MAXBUFF);
        c.setInitiate(this.getMyVerTag());
        Chunk[] s = new Chunk[1];
        s[0] = c;
        this._state = State.COOKIEWAIT;
        try {
            this.send(s);
        } catch (java.io.EOFException end) {
            unexpectedClose(end);
        } // todo need timer here.....
    }

    protected Chunk[] iackDeal(InitAckChunk iack) {
        Chunk[] reply = null;

        _peerVerTag = iack.getInitiateTag();
        _winCredit = iack.getAdRecWinCredit();
        _farTSN = iack.getInitialTSN() - 1;
        _maxOutStreams = Math.min(iack.getNumInStreams(), MAXSTREAMS);
        _maxInStreams = Math.min(iack.getNumOutStreams(), MAXSTREAMS);

        iack.getSupportedExtensions(_supportedExtensions);
        byte[] data = iack.getCookie();
        CookieEchoChunk ce = new CookieEchoChunk();
        ce.setCookieData(data);
        reply = new Chunk[1];
        reply[0] = ce;
        this._state = State.COOKIEECHOED;
        return reply;
    }

    /* <pre>
     5.2.1.  INIT Received in COOKIE-WAIT or COOKIE-ECHOED State (Item B)

     This usually indicates an initialization collision, i.e., each
     endpoint is attempting, at about the same time, to establish an
     association with the other endpoint.

     Upon receipt of an INIT in the COOKIE-WAIT state, an endpoint MUST
     respond with an INIT ACK using the same parameters it sent in its
     original INIT chunk (including its Initiate Tag, unchanged).  When
     responding, the endpoint MUST send the INIT ACK back to the same
     address that the original INIT (sent by this endpoint) was sent.

     Upon receipt of an INIT in the COOKIE-ECHOED state, an endpoint MUST
     respond with an INIT ACK using the same parameters it sent in its
     original INIT chunk (including its Initiate Tag, unchanged), provided
     that no NEW address has been added to the forming association.  If
     the INIT message indicates that a new address has been added to the
     association, then the entire INIT MUST be discarded, and NO changes
     should be made to the existing association.  An ABORT SHOULD be sent
     in response that MAY include the error 'Restart of an association
     with new addresses'.  The error SHOULD list the addresses that were
     added to the restarting association.

     When responding in either state (COOKIE-WAIT or COOKIE-ECHOED) with
     an INIT ACK, the original parameters are combined with those from the
     newly received INIT chunk.  The endpoint shall also generate a State
     Cookie with the INIT ACK.  The endpoint uses the parameters sent in
     its INIT to calculate the State Cookie.

     After that, the endpoint MUST NOT change its state, the T1-init timer
     shall be left running, and the corresponding TCB MUST NOT be
     destroyed.  The normal procedures for handling State Cookies when a
     TCB exists will resolve the duplicate INITs to a single association.

     For an endpoint that is in the COOKIE-ECHOED state, it MUST populate
     its Tie-Tags within both the association TCB and inside the State
     Cookie (see Section 5.2.2 for a description of the Tie-Tags).
     </pre>
     */
    protected Chunk[] inboundInit(InitChunk init) {
        Chunk[] reply = null;
        _peerVerTag = init.getInitiateTag();
        _winCredit = init.getAdRecWinCredit();
        _farTSN = init.getInitialTSN() - 1;

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
        byte[] watermark = " |pi.pe|".getBytes();
        int wlen = Math.min(watermark.length, cookie.cookieData.length);
        for (int w=0;w<wlen;w++){
            cookie.cookieData[w] = watermark[w]; // tell google who we are.
        }
        iac.setCookie(cookie.cookieData);
        _cookies.add(cookie);

        byte[] fse = init.getFarSupportedExtensions();
        if (fse != null) {
            iac.setSupportedExtensions(this.getUnionSupportedExtensions(fse));
        }
        reply = new Chunk[1];
        reply[0] = iac;
        Log.debug("Got in bound init :" + init.toString());
        Log.debug("Replying with init-ack :" + iac.toString());
        return reply;
    }

    private void ingest(DataChunk dc, ArrayList<Chunk> rep) {
        Log.verb("ingesting " + dc.toString());
        Chunk closer = null;
        Integer sno = dc.getStreamId();
        long tsn = dc.getTsn();
        SCTPStream in = _streams.get(sno);
        if (in == null) {
            Log.verb("making new stream " + sno);
            in = mkStream(sno);
            _streams.put(sno, in);
            _al.onRawStream(in);
        }
        Chunk[] repa;
        repa = in.append(dc);

        if (repa != null) {
            for (Chunk r : repa) {
                rep.add(r);
            }
        }
        if (closer != null) {
            rep.add(closer);
        } //closer logic is broken here - perhaps use an exception ?
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
            DataChunk dup = _holdingPen.get(tsn_L);
            if (dup != null) {
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
                    Log.verb("gap in inbound tsns at " + t);
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

        SackChunk sack = mkSack(l, duplicates);
        rep.add(sack);
        return rep.toArray(dummy);
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
        if (_state == State.CLOSED || _state == State.COOKIEWAIT || _state == State.COOKIEECHOED || _state == State.ESTABLISHED) {
            // Authenticate the State Cookie
            CookieHolder cookie;
            if (null != (cookie = checkCookieEcho(echo.getCookieData()))) {
                // Compare the creation timestamp in the State Cookie to the current local time.
                long howStale = howStaleIsMyCookie(cookie);
                if (howStale == 0) {
                    if (_state == State.ESTABLISHED) {
                        Log.debug("Repeating a lost cookie Ack");
                    }
                    //enter the ESTABLISHED state
                    _state = State.ESTABLISHED;
                    /*
                     Send a COOKIE ACK chunk to the peer acknowledging receipt of the
                     COOKIE ECHO.  The COOKIE ACK MAY be bundled with an outbound DATA
                     chunk or SACK chunk; however, the COOKIE ACK MUST be the first
                     chunk in the SCTP packet.
                     */
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
            Log.debug("Got an COOKIE_ECHO when not expecting one - ignoring it");
        }
        return reply;
    }

    private SackChunk mkSack(ArrayList<Long> pen, ArrayList<Long> dups) {
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
        return _streams.values().stream().mapToInt((s) -> s.stashCap()).sum();
    }

    public abstract void enqueue(DataChunk d);

    public abstract SCTPStream mkStream(int id);

    long getCumAckPt() {
        return _farTSN;
    }

    ReConfigChunk addToCloseList(SCTPStream st) throws Exception {
        return reconfigState.makeClose(st);
    }

    public void closeStream(SCTPStream st) throws SctpPacketFormatException, IOException, Exception {
        Chunk[] cs = new Chunk[1];
        if (canSend()) {
            Log.debug("due to reconfig stream " + st);
            cs[0] = reconfigState.makeClose(st);
            this.send(cs);
        }
    }

    public SCTPStream mkStream(String label, SCTPStreamListener sl) throws StreamNumberInUseException, UnreadyAssociationException, SctpPacketFormatException, IOException, Exception {
        SCTPStream s = mkStream(label);
        s.setSCTPStreamListener(sl);
        return s;
    }

    public SCTPStream mkStream(String label) throws StreamNumberInUseException, UnreadyAssociationException, SctpPacketFormatException, IOException, Exception {
        int n = 1;
        int tries = this._maxOutStreams;

        do {
            n = 2 * _random.nextInt(this._maxOutStreams / 2);
            if (!_even) {
                n += 1;
            }
            if (--tries < 0) {
                throw new StreamNumberInUseException();
            }
        } while (_streams.containsKey(new Integer(n)));

        return mkStream(n, label);
    }

    int[] allStreams() {
        int[] ret = new int[0];
        Set<Integer> ks = _streams.keySet();
        ret = new int[ks.size()];
        int i = 0;
        for (Integer k : ks) {
            ret[i++] = k;
        }

        return ret;
    }

    protected SCTPStream getStream(int s) {
        return _streams.get(s);

    }

    SCTPStream delStream(int s) {
        return _streams.remove(s);

    }

    public SCTPStream mkStream(int sno, String label) throws StreamNumberInUseException, UnreadyAssociationException, SctpPacketFormatException, IOException, Exception {
        SCTPStream sout;
        if (canSend()) {

            sout = mkStream(sno);
            sout.setLabel(label);
            SCTPStream sold = _streams.putIfAbsent(sno, sout);
            if (sold != null) {
                Log.warn("Stream number already in use " + sno);
                throw new StreamNumberInUseException();
            }
        } else {
            throw new UnreadyAssociationException();
        }
        return sout;
    }

    public int maxMessageSize() {
        return 1 << 16; // shrug - I don't know 
    }

    public State getState() {
        return _state;
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

    public void unexpectedClose(EOFException end) {
        Log.debug("Unxepected association close " + end.getMessage());
        try {
            closeAllStreams();
            _al.onDisAssociated(this);
        } catch (Throwable t) {
            Log.error("Threw " + t.getMessage() + " in unexpectedClose");
            if (Log.getLevel() >= Log.DEBUG) {
                t.printStackTrace();
            }
        }
        _state = State.CLOSED;
        _rcv = null;

    }

    public void closeAllStreams() {

        _streams.forEach((Integer sn, SCTPStream st) -> {
            Log.debug("closing " + st.getLabel());
            try {
                SCTPStreamListener li = st.getSCTPStreamListener();
                if (li != null) {
                    li.close(st);
                }
            } catch (Exception x) {
                Log.error("problem closing stream");
                if (Log.getLevel() >= Log.DEBUG) {
                    x.printStackTrace();
                }
            }
        });
        _streams.clear();

    }

    public boolean isAssociated() {
        return _state == State.ESTABLISHED;
    }

    //abstract public void sendAndBlock(SCTPMessage m) throws Exception;

    synchronized public SCTPMessage makeMessage(byte[] bytes, SCTPStream s) {
        SCTPMessage m = null;
        if (canSend()) {
            if (bytes.length < this.maxMessageSize()) {
                m = new SCTPMessage(bytes, s);
                s.setAsNextMessage(m);
            } else {
                Log.warn("Message too long " + bytes.length + " > " + this.maxMessageSize());
            }
        }
        return m;
    }

    synchronized public SCTPMessage makeMessage(String string, SCTPStream s) {
        SCTPMessage m = null;
        if (canSend()) {
            if (string.length() < this.maxMessageSize()) {
                m = new SCTPMessage(string, s);
                s.setAsNextMessage(m);
            } else {
                Log.warn("Message too long " + string.length() + " > " + this.maxMessageSize());
            }
        }
        return m;
    }

    synchronized public SCTPMessage makeMessage(DCOpen dco, SCTPStream s) {
        SCTPMessage m = null;
        byte[] bytes = dco.getBytes();
        if (canSend()) {
            if (bytes.length < this.maxMessageSize()) {
                m = new SCTPMessage(dco, s);
                s.setAsNextMessage(m);
                if (m.getSeq() != 0) {
                    Log.warn("DCO should be the first message in a stream was "+m.getSeq()+" type = dcep"+(dco.isAck()?"ACK":"OPEN"));
                }
            } else {
                Log.warn("Message too long " + bytes.length + " > " + this.maxMessageSize());
            }
        }
        return m;
    }
    void alOnDCEPStream(SCTPStream _stream, String label, int _pPid) throws Exception {
        _al.onDCEPStream(_stream, label, _pPid);
    }
    abstract protected Chunk[] sackDeal(SackChunk sackChunk);

}
