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
package com.ipseorama.sctp.small;

import com.ipseorama.sctp.Association;
import com.ipseorama.sctp.AssociationListener;
import com.ipseorama.sctp.SCTPMessage;
import com.ipseorama.sctp.SCTPStream;
import com.ipseorama.sctp.messages.Chunk;
import com.ipseorama.sctp.messages.DataChunk;
import com.ipseorama.sctp.messages.InitAckChunk;
import com.ipseorama.sctp.messages.InitChunk;
import com.ipseorama.sctp.messages.SackChunk;
import com.ipseorama.sctp.messages.exceptions.SctpPacketFormatException;
import com.phono.srtplight.Log;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import org.bouncycastle.crypto.tls.DatagramTransport;

/**
 * An association who's retries etc are managed with plain old threads.
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class ThreadedAssociation extends Association implements Runnable {

    final static int MAXBLOCKS = 128; // some number....
    private ArrayBlockingQueue<DataChunk> _freeBlocks;
    private HashMap<Long, DataChunk> _inFlight;
    private long _lastCumuTSNAck;
    final static int MAX_INIT_RETRANS = 8;
    // a guess at the round-trip time
    private long _rto = 200;

    /*   
     o  Receiver advertised window size (rwnd, in bytes), which is set by
     the receiver based on its available buffer space for incoming
     packets.

     Note: This variable is kept on the entire association.
     */
    private long _rwnd;
    /*
     o  Congestion control window (cwnd, in bytes), which is adjusted by
     the sender based on observed network conditions.

     Note: This variable is maintained on a per-destination-address
     basis.
     */
    private long _cwnd;
    // assume a single destination via ICE
/*
     o  Slow-start threshold (ssthresh, in bytes), which is used by the
     sender to distinguish slow-start and congestion avoidance phases.

     Note: This variable is maintained on a per-destination-address
     basis.
     */
    private long _ssthresh;
    /*



     Stewart                     Standards Track                    [Page 95]

     RFC 4960          Stream Control Transmission Protocol    September 2007


     SCTP also requires one additional control variable,
     partial_bytes_acked, which is used during congestion avoidance phase
     to facilitate cwnd adjustment.
     */
    private long _partial_bytes_acked;
    private boolean _fastRecovery;
    /*
     10.2.  Probing Method Using SCTP

     In the Stream Control Transmission Protocol (SCTP) [RFC2960], the
     application writes messages to SCTP, which divides the data into
     smaller "chunks" suitable for transmission through the network.  Each
     chunk is assigned a Transmission Sequence Number (TSN).  Once a TSN
     has been transmitted, SCTP cannot change the chunk size.  SCTP multi-
     path support normally requires SCTP to choose a chunk size such that
     its messages to fit the smallest PMTU of all paths.  Although not
     required, implementations may bundle multiple data chunks together to
     make larger IP packets to send on paths with a larger PMTU.  Note
     that SCTP must independently probe the PMTU on each path to the peer.

     The RECOMMENDED method for generating probes is to add a chunk
     consisting only of padding to an SCTP message.  The PAD chunk defined
     in [RFC4820] SHOULD be attached to a minimum length HEARTBEAT (HB)
     chunk to build a probe packet.  This method is fully compatible with
     all current SCTP implementations.

     SCTP MAY also probe with a method similar to TCP's described above,
     using inline data.  Using such a method has the advantage that
     successful probes have no additional overhead; however, failed probes
     will require retransmission of data, which may impact flow
     performance.

     To do .....
     */
    private int _transpMTU = 768;
    private final SimpleSCTPTimer _timer;
    private Chunk[] _stashCookieEcho;

    public ThreadedAssociation(DatagramTransport transport, AssociationListener al) {
        super(transport, al);
        /*try {
            _transpMTU = Math.min(transport.getReceiveLimit(), transport.getSendLimit());
            Log.debug("Transport MTU is now " + _transpMTU);
        } catch (IOException x) {
            Log.warn("Failed to get suitable transport mtu ");
        }*/
        _freeBlocks = new ArrayBlockingQueue(MAXBLOCKS);
        _inFlight = new HashMap(MAXBLOCKS);

        for (int i = 0; i < MAXBLOCKS; i++) {
            DataChunk dc = new DataChunk();
            _freeBlocks.add(dc);
        }
        resetCwnd();
        try {
            _transpMTU = transport.getSendLimit();
        } catch (IOException ex) {
            ;
        }
        _timer = new SimpleSCTPTimer();

    }
    /*
     If the T1-init timer expires at "A" after the INIT or COOKIE ECHO
     chunks are sent, the same INIT or COOKIE ECHO chunk with the same
     Initiate Tag (i.e., Tag_A) or State Cookie shall be retransmitted and
     the timer restarted.  This shall be repeated Max.Init.Retransmits
     times before "A" considers "Z" unreachable and reports the failure to
     its upper layer (and thus the association enters the CLOSED state).

     When retransmitting the INIT, the endpoint MUST follow the rules
     defined in Section 6.3 to determine the proper timer value.
     */

    @Override
    protected Chunk[] iackDeal(InitAckChunk iack) {
        Chunk[] ret = super.iackDeal(iack);
        _stashCookieEcho = ret;
        return ret;
    }

    @Override
    public void associate() throws SctpPacketFormatException, IOException {
        final SimpleSCTPTimer init = new SimpleSCTPTimer();
        sendInit();
        Runnable r = new Runnable() {
            int retries = 0;

            @Override
            public void run() {
                Log.debug("T1 init timer expired in state " + _state.name());

                if ((_state == State.COOKIEECHOED) || (_state == State.COOKIEWAIT)) {
                    try {
                        if (_state == State.COOKIEWAIT) {
                            sendInit();
                        } else { // COOKIEECHOED
                            send(_stashCookieEcho);
                        }
                    } catch (java.io.EOFException end) {
                        unexpectedClose(end);
                    } catch (Exception ex) {
                        Log.error("Cant send Init/cookie retry " + retries + " because " + ex.toString());
                    }
                    retries++;
                    if (retries < MAX_INIT_RETRANS) {
                        init.setRunnable(this, _rto);
                    }
                } else {
                    Log.debug("T1 init timer expired with nothing to do");
                }
            }
        };
        init.setRunnable(r, getT1());
    }

    public SCTPStream mkStream(int id) {
        Log.debug("Make new Blocking stream " + id);
        return new BlockingSCTPStream(this, id);
    }

    public long getT3() {
        return this._rto * 2; // todo - proper estimate 
    }

    @Override
    public void enqueue(DataChunk d) {
        // todo - this worries me - 2 nested synchronized 
        synchronized (this) {
            long now = System.currentTimeMillis();
            d.setTsn(_nearTSN++);
            d.setGapAck(false);
            d.setRetryTime(now + getT3() - 1);
            _timer.setRunnable(this, getT3());
            reduceRwnd(d.getDataSize());
            //_outbound.put(new Long(d.getTsn()), d);
            Log.verb(" DataChunk enqueued " + d.toString());
            // all sorts of things wrong here - being in a synchronized not the least of them

            Chunk[] toSend = addSackIfNeeded(d);
            try {
                send(toSend);
                synchronized (_inFlight) {
                    _inFlight.put(new Long(d.getTsn()), d);
                }
            } catch (SctpPacketFormatException ex) {
                Log.error("badly formatted chunk " + d.toString());
            } catch (java.io.EOFException end) {
                unexpectedClose(end);
            } catch (IOException ex) {
                Log.error("Can not send chunk " + d.toString());
            }
        }
    }

    @Override
    public void sendAndBlock(SCTPMessage m) throws Exception {
        while (m.hasMoreData()) {
            DataChunk dc = _freeBlocks.take();
            m.fill(dc);
            // todo check rollover - will break at maxint.
            enqueue(dc);
        }
    }

    @Override
    public SCTPMessage makeMessage(byte[] bytes, BlockingSCTPStream s) {
        SCTPMessage m = null;
        if (super.canSend()) {
            if (bytes.length < this.maxMessageSize()) {
                m = new SCTPMessage(bytes, s);
            }
        }
        return m;
    }

    @Override
    public SCTPMessage makeMessage(String bytes, BlockingSCTPStream s) {
        SCTPMessage m = null;
        if (super.canSend()) {
            if (bytes.length() < this.maxMessageSize()) {
                m = new SCTPMessage(bytes, s);
            }
        }
        return m;
    }

    /**
     * try and create a sack packet - if we have any new acks to send or null if
     * we don't
     *
     * @return
     */
    private Chunk[] addSackIfNeeded(DataChunk d) {
        /*
         Before an endpoint transmits a DATA chunk, if any received DATA
         chunks have not been acknowledged (e.g., due to delayed ack), the
         sender should create a SACK and bundle it with the outbound DATA
         chunk, as long as the size of the final SCTP packet does not exceed
         the current MTU.  See Section 6.2.
         */
        // ToDo - check on unacked recvs (why?)
        // and then check on size - will it fit?
        // then add sack
        Chunk[] ret = {d};
        return ret;
    }


    /*
     6.2.1.  Processing a Received SACK

     Each SACK an endpoint receives contains an a_rwnd value.  This value
     represents the amount of buffer space the data receiver, at the time
     of transmitting the SACK, has left of its total receive buffer space
     (as specified in the INIT/INIT ACK).  Using a_rwnd, Cumulative TSN
     Ack, and Gap Ack Blocks, the data sender can develop a representation
     of the peer's receive buffer space.

     One of the problems the data sender must take into account when
     processing a SACK is that a SACK can be received out of order.  That
     is, a SACK sent by the data receiver can pass an earlier SACK and be
     received first by the data sender.  If a SACK is received out of




     Stewart                     Standards Track                    [Page 81]

     RFC 4960          Stream Control Transmission Protocol    September 2007


     order, the data sender can develop an incorrect view of the peer's
     receive buffer space.

     Since there is no explicit identifier that can be used to detect
     out-of-order SACKs, the data sender must use heuristics to determine
     if a SACK is new.

     An endpoint SHOULD use the following rules to calculate the rwnd,
     using the a_rwnd value, the Cumulative TSN Ack, and Gap Ack Blocks in
     a received SACK.
     */
    /*
     A) At the establishment of the association, the endpoint initializes
     the rwnd to the Advertised Receiver Window Credit (a_rwnd) the
     peer specified in the INIT or INIT ACK.
     */
    protected Chunk[] inboundInit(InitChunk init) {
        _rwnd = init.getAdRecWinCredit();
        setSsthresh(init);
        return super.inboundInit(init);
    }
    /*
     B) Any time a DATA chunk is transmitted (or retransmitted) to a peer,
     the endpoint subtracts the data size of the chunk from the rwnd of
     that peer.
     */

    private void reduceRwnd(int dataSize) {
        _rwnd -= dataSize;
        if (_rwnd < 0) {
            _rwnd = 0;
        }
    }
    /*
     C) Any time a DATA chunk is marked for retransmission, either via
     T3-rtx timer expiration (Section 6.3.3) or via Fast Retransmit
     (Section 7.2.4), add the data size of those chunks to the rwnd.

     Note: If the implementation is maintaining a timer on each DATA
     chunk, then only DATA chunks whose timer expired would be marked
     for retransmission.
     */

    private void incrRwnd(int dataSize) {
        _rwnd += dataSize;
    }
    /*

     D) Any time a SACK arrives, the endpoint performs the following:

     ...

     ToDo :
    
     iii) If the SACK is missing a TSN that was previously acknowledged
     via a Gap Ack Block (e.g., the data receiver reneged on the
     data), then consider the corresponding DATA that might be
     possibly missing: Count one miss indication towards Fast
     Retransmit as described in Section 7.2.4, and if no
     retransmit timer is running for the destination address to
     which the DATA chunk was originally transmitted, then T3-rtx
     is started for that destination address.

     iv) If the Cumulative TSN Ack matches or exceeds the Fast
     Recovery exitpoint (Section 7.2.4), Fast Recovery is exited.

     */

    @Override
    protected Chunk[] sackDeal(SackChunk sack) {
        Chunk[] ret = {};
        /*
         i) If Cumulative TSN Ack is less than the Cumulative TSN Ack
         Point, then drop the SACK.  Since Cumulative TSN Ack is
         monotonically increasing, a SACK whose Cumulative TSN Ack is
         less than the Cumulative TSN Ack Point indicates an out-of-
         order SACK.
         */
        if (sack.getCumuTSNAck() >= this._lastCumuTSNAck) {
            long ackedTo = sack.getCumuTSNAck();
            int totalAcked = 0;

            // interesting SACK
            // process acks
            synchronized (_inFlight) {
                ArrayList<Long> removals = new ArrayList();
                for (Long k : _inFlight.keySet()) {
                    if (k <= ackedTo) {
                        removals.add(k);
                    }
                }
                for (Long k : removals) {
                    DataChunk d = _inFlight.remove(k);
                    totalAcked += d.getDataSize();
                    try {
                        // todo Notify stream that this one is acked
                        _freeBlocks.put(d);
                    } catch (InterruptedException ex) {
                        Log.error("eek - can't replace free block on list!?!");
                    }
                }
            }
            /*
             Gap Ack Blocks:

             These fields contain the Gap Ack Blocks.  They are repeated for
             each Gap Ack Block up to the number of Gap Ack Blocks defined in
             the Number of Gap Ack Blocks field.  All DATA chunks with TSNs
             greater than or equal to (Cumulative TSN Ack + Gap Ack Block
             Start) and less than or equal to (Cumulative TSN Ack + Gap Ack
             Block End) of each Gap Ack Block are assumed to have been received
             correctly.
             */
            for (SackChunk.GapBlock gb : sack.getGaps()) {
                long ts = gb.getStart() + ackedTo;
                long te = gb.getEnd() + ackedTo;
                synchronized (_inFlight) {
                    for (long t = ts; t <= te; t++) {
                        Long l = new Long(t);
                        DataChunk d = _inFlight.get(l);
                        Log.verb("gap block says far end has seen " + l);

                        if (d == null) {
                            Log.debug("Huh? gap for something not inFlight ?!? " + l);
                        } else {
                            d.setGapAck(true);
                            totalAcked += d.getDataSize();
                        }
                    }
                }
            }
            /*
             ii) Set rwnd equal to the newly received a_rwnd minus the number
             of bytes still outstanding after processing the Cumulative
             TSN Ack and the Gap Ack Blocks.
             */
            int totalDataInFlight = 0;
            synchronized (_inFlight) {
                for (Long k : _inFlight.keySet()) {
                    DataChunk d = _inFlight.get(k);
                    if (!d.getGapAck()) {
                        totalDataInFlight += d.getDataSize();
                    }
                }
            }
            _rwnd = sack.getArWin() - totalDataInFlight;
            Log.debug("Setting rwnd to " + _rwnd);
            boolean advanced = (_lastCumuTSNAck < ackedTo);
            adjustCwind(advanced, totalDataInFlight, totalAcked);
            _lastCumuTSNAck = ackedTo;

        } else {
            Log.debug("Dumping Sack - already seen later sack.");
        }
        return ret;
    }

    /* 
    
     7.2.1.  Slow-Start

     Beginning data transmission into a network with unknown conditions or
     after a sufficiently long idle period requires SCTP to probe the
     network to determine the available capacity.  The slow-start
     algorithm is used for this purpose at the beginning of a transfer, or
     after repairing loss detected by the retransmission timer.

     o  The initial cwnd before DATA transmission or after a sufficiently
     long idle period MUST be set to min(4*MTU, max (2*MTU, 4380
     bytes)).
     */
    private void resetCwnd() {

        _cwnd = Math.min(4 * _transpMTU, Math.max(2 * _transpMTU, 4380));
    }
    /*
     o  The initial cwnd after a retransmission timeout MUST be no more
     than 1*MTU.
     */

    private void setCwndPostRetrans() {
        _cwnd = _transpMTU;
    }
    /*
    

     o  The initial value of ssthresh MAY be arbitrarily high (for
     example, implementations MAY use the size of the receiver
     advertised window).
     */

    void setSsthresh(InitChunk init) {
        this._ssthresh = init.getAdRecWinCredit();
    }

    /*
     o  Whenever cwnd is greater than zero, the endpoint is allowed to
     have cwnd bytes of data outstanding on that transport address.
     */
    boolean maySend(int sz) {
        return (sz <= _cwnd);
    }

    /*
     o  When cwnd is less than or equal to ssthresh, an SCTP endpoint MUST
     use the slow-start algorithm to increase cwnd only if the current
     congestion window is being fully utilized, an incoming SACK
     advances the Cumulative TSN Ack Point, and the data sender is not
     in Fast Recovery.  Only when these three conditions are met can
     the cwnd be increased; otherwise, the cwnd MUST not be increased.
     If these conditions are met, then cwnd MUST be increased by, at
     most, the lesser of 1) the total size of the previously
     outstanding DATA chunk(s) acknowledged, and 2) the destination's
     path MTU.  This upper bound protects against the ACK-Splitting
     attack outlined in [SAVAGE99].
     */
    void adjustCwind(boolean didAdvance, int inFlightBytes, int totalAcked) {
        boolean fullyUtilized = ((inFlightBytes - _cwnd) < DataChunk.getCapacity()); // could we fit one more in?

        if (_cwnd <= _ssthresh) {
            // slow start
            Log.debug("slow start");

            if (didAdvance && fullyUtilized && !_fastRecovery) {
                int incCwinBy = Math.min(_transpMTU, totalAcked);
                _cwnd += incCwinBy;
                Log.debug("cwnd now " + _cwnd);
            } else {
                Log.debug("cwnd static at " + _cwnd + " (didAdvance fullyUtilized  _fastRecovery inFlightBytes totalAcked)  " + didAdvance + " " + fullyUtilized + " " + _fastRecovery + " " + inFlightBytes + " " + totalAcked);
            }

        } else {
            /*
             7.2.2.  Congestion Avoidance

             When cwnd is greater than ssthresh, cwnd should be incremented by
             1*MTU per RTT if the sender has cwnd or more bytes of data
             outstanding for the corresponding transport address.

             In practice, an implementation can achieve this goal in the following
             way:

             o  partial_bytes_acked is initialized to 0.

             o  Whenever cwnd is greater than ssthresh, upon each SACK arrival
             that advances the Cumulative TSN Ack Point, increase
             partial_bytes_acked by the total number of bytes of all new chunks
             acknowledged in that SACK including chunks acknowledged by the new
             Cumulative TSN Ack and by Gap Ack Blocks.

             o  When partial_bytes_acked is equal to or greater than cwnd and
             before the arrival of the SACK the sender had cwnd or more bytes
             of data outstanding (i.e., before arrival of the SACK, flightsize
             was greater than or equal to cwnd), increase cwnd by MTU, and
             reset partial_bytes_acked to (partial_bytes_acked - cwnd).

             o  Same as in the slow start, when the sender does not transmit DATA
             on a given transport address, the cwnd of the transport address
             should be adjusted to max(cwnd / 2, 4*MTU) per RTO.





             Stewart                     Standards Track                    [Page 97]

             RFC 4960          Stream Control Transmission Protocol    September 2007


             o  When all of the data transmitted by the sender has been
             acknowledged by the receiver, partial_bytes_acked is initialized
             to 0.

             */
            if (didAdvance) {
                _partial_bytes_acked += totalAcked;
                if ((_partial_bytes_acked >= _cwnd) && fullyUtilized) {
                    _cwnd += _transpMTU;
                    _partial_bytes_acked -= _cwnd;
                }
            }
        }
    }
    /*
     In instances where its peer endpoint is multi-homed, if an endpoint
     receives a SACK that advances its Cumulative TSN Ack Point, then it
     should update its cwnd (or cwnds) apportioned to the destination
     addresses to which it transmitted the acknowledged data.  However, if



     Stewart                     Standards Track                    [Page 96]

     RFC 4960          Stream Control Transmission Protocol    September 2007


     the received SACK does not advance the Cumulative TSN Ack Point, the
     endpoint MUST NOT adjust the cwnd of any of the destination
     addresses.

     Because an endpoint's cwnd is not tied to its Cumulative TSN Ack
     Point, as duplicate SACKs come in, even though they may not advance
     the Cumulative TSN Ack Point an endpoint can still use them to clock
     out new data.  That is, the data newly acknowledged by the SACK
     diminishes the amount of data now in flight to less than cwnd, and so
     the current, unchanged value of cwnd now allows new data to be sent.
     On the other hand, the increase of cwnd must be tied to the
     Cumulative TSN Ack Point advancement as specified above.  Otherwise,
     the duplicate SACKs will not only clock out new data, but also will
     adversely clock out more new data than what has just left the
     network, during a time of possible congestion.

     o  When the endpoint does not transmit data on a given transport
     address, the cwnd of the transport address should be adjusted to
     max(cwnd/2, 4*MTU) per RTO.

     */

    // timer goes off,
    @Override
    public void run() {
        if (canSend()) {
            long now = System.currentTimeMillis();
            Log.debug("retry timer went off at " + now);
            ArrayList<DataChunk> dcs = new ArrayList();
            int space = _transpMTU;
            boolean resetTimer = false;
            synchronized (_inFlight) {
                for (Long k : _inFlight.keySet()) {
                    DataChunk d = _inFlight.get(k);
                    if (d.getGapAck()) {
                        Log.debug("skipping gap-acked tsn " + d.getTsn());
                        continue;
                    }
                    if (d.getRetryTime() <= now) {
                        dcs.add(d);
                        d.setRetryTime(now + getT3() - 1);
                        space -= d.getChunkLength();
                        Log.debug("available space in pkt is " + space);
                        if (space <= 0) {
                            resetTimer = true;
                            break;
                        }
                    } else {
                        Log.debug("retry not yet due for  " + d.toString());
                        resetTimer = true;
                    }
                }
            }
            if (!dcs.isEmpty()) {
                Comparator<? super DataChunk> dcc = dcs.get(0);
                Collections.sort(dcs, dcc);
                DataChunk[] da = new DataChunk[dcs.size()];
                int i = 0;
                for (DataChunk d : dcs) {
                    da[i++] = d;
                }
                resetTimer = true;
                try {
                    Log.debug("Sending retry for  " + da.length + " data chunks");
                    this.send(da);
                } catch (java.io.EOFException end) {
                    unexpectedClose(end);
                    resetTimer = false;
                } catch (Exception ex) {
                    Log.error("Cant send retry - eek " + ex.toString());
                }
            } else {
                Log.debug("Nothing to do ");
            }
            if (resetTimer) {
                _timer.setRunnable(this, getT3());
                Log.debug("Try again in a while  ");

            }
        }
    }

    private long getT1() {
        return this._rto * 10;
    }

}
