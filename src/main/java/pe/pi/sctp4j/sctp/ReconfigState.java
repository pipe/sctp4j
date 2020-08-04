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

import pe.pi.sctp4j.sctp.messages.Chunk;
import pe.pi.sctp4j.sctp.messages.ReConfigChunk;
import pe.pi.sctp4j.sctp.messages.params.IncomingSSNResetRequestParameter;
import pe.pi.sctp4j.sctp.messages.params.OutgoingSSNResetRequestParameter;
import pe.pi.sctp4j.sctp.messages.params.ReconfigurationResponseParameter;
import com.phono.srtplight.Log;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author thp
 */
class ReconfigState {

    ReConfigChunk recentInbound = null;
    ReConfigChunk recentOutboundRequest = null;
    ReConfigChunk sentReply = null;
    boolean timerRunning = false;
    long nearSeqno = 0;
    long farSeqno = 0;
    Association assoc;
    ConcurrentLinkedQueue<SCTPStream> listOfStreamsToReset;

    ReconfigState(Association a, long farTSN) {
        nearSeqno = a.getNearTSN();
        farSeqno = farTSN;
        assoc = a;
        listOfStreamsToReset = new ConcurrentLinkedQueue();
    }

    private boolean haveSeen(ReConfigChunk rconf) {
        return rconf.sameAs(recentInbound);
    }

    private ReConfigChunk getPrevious(ReConfigChunk rconf) {
        return rconf.sameAs(recentInbound) ? sentReply : null;
    }

    private boolean timerIsRunning() {
        return timerRunning;
    }

    private void markAsAcked(ReConfigChunk rconf) {
        // ooh, what does this button do ??? To Do
    }

    private long nextNearNo() {
        return nearSeqno++;
    }

    private long nextFarNo() {
        return farSeqno++;
    }

    public long nextDue() {
        return 1000L;
    }

    /*
     * https://tools.ietf.org/html/rfc6525
     */
    Chunk[] deal(ReConfigChunk rconf) {
        Chunk[] ret = new Chunk[1];
        ReConfigChunk reply = null;
        Log.debug("Got a reconfig message to deal with");
        if (haveSeen(rconf)) {
            // if not - is this a repeat
            reply = getPrevious(rconf); // then send the same reply
        }
        if (reply == null) {
            // not a repeat then
            reply = new ReConfigChunk(); // create a new thing
            if (rconf.hasOutgoingReset()) {
                OutgoingSSNResetRequestParameter oreset = rconf.getOutgoingReset();
                int[] streams = oreset.getStreams();
                if (streams.length == 0) {
                    streams = assoc.allStreams();
                }
                if (timerIsRunning()) {
                    markAsAcked(rconf);
                }
                // if we are behind, we are supposed to wait untill we catch up.
                if (oreset.getLastAssignedTSN() > assoc.getCumAckPt()) {
                    Log.debug("Last assigned > farTSN " + oreset.getLastAssignedTSN() + " v " + assoc.getCumAckPt());
                    for (int s : streams) {
                        SCTPStream defstr = assoc.getStream(s);
                        defstr.setDeferred(true);
                    }
                    ReconfigurationResponseParameter rep = new ReconfigurationResponseParameter();
                    rep.setSeq(oreset.getReqSeqNo());
                    rep.setResult(rep.IN_PROGRESS);
                    reply.addParam(rep);
                } else {
                    // somehow invoke this when TSN catches up ?!?! ToDo
                    Log.debug("we are up-to-date ");
                    ReconfigurationResponseParameter rep = new ReconfigurationResponseParameter();
                    rep.setSeq(oreset.getReqSeqNo());
                    int result = streams.length > 0 ? rep.SUCCESS_PERFORMED : rep.SUCCESS_NOTHING_TO_DO;
                    rep.setResult(result); // assume all good
                    for (int s : streams) {
                        SCTPStream cstrm = assoc.delStream(s);
                        if (cstrm == null) {
                            Log.error("(re)Close a non existant stream ="+s);
                            //rep.setResult(rep.ERROR_WRONG_SSN);
                            //break;
                            // bidriectional might be a problem here...
                        } else {
                            cstrm.reset();
                        }
                    }
                    reply.addParam(rep);
                }
            }
            // ponder putting this in a second chunk ?
            if (rconf.hasIncomingReset()) {
                IncomingSSNResetRequestParameter ireset = rconf.getIncomingReset();
                /*The Re-configuration
                Response Sequence Number of the Outgoing SSN Reset Request
                Parameter MUST be the Re-configuration Request Sequence Number
                of the Incoming SSN Reset Request Parameter. */
                OutgoingSSNResetRequestParameter rep = new OutgoingSSNResetRequestParameter(nextNearNo(), ireset.getReqNo(), assoc.getNearTSN());
                int[] streams = ireset.getStreams();
                rep.setStreams(streams);
                if (streams.length == 0) {
                    streams = assoc.allStreams();
                }
                for (int s : streams) {
                    SCTPStream st = assoc.getStream(s);
                    if (st != null) {
                        st.setClosing(true);
                    }
                }
                reply.addParam(rep);
                // set outbound timer running here ???
                Log.debug("Ireset " + ireset);
            }
        }
        if (reply.hasParam()) {
            ret[0] = reply;
            // todo should add sack here
            Log.debug("about to reply with " + reply.toString());
        } else {
            ret = null;
        }
        return ret;
    }

    /* we can only demand they close their outbound streams */
 /* we can request they start to close inbound (ie ask us to shut our outbound */
 /* DCEP treats streams as bi-directional - so this is somewhat of an inpedance mis-match */
 /* resulting in a temporary 'half closed' state */
 /* mull this over.... */
    ReConfigChunk makeClose(SCTPStream st) throws Exception {
        ReConfigChunk ret = null;
        Log.debug("building reconfig so close stream " + st);
        st.setClosing(true);
        listOfStreamsToReset.add(st);
        if (!timerIsRunning()) {
            ret = makeSSNResets();
        }
        return ret;
    }

    private ReConfigChunk makeSSNResets() throws Exception {

        ReConfigChunk reply = new ReConfigChunk(); // create a new thing
        Log.debug("closing streams n=" + listOfStreamsToReset.size());
        int[] streams = listOfStreamsToReset.stream().filter((SCTPStream s) -> {
            return s.InboundIsOpen();
        }).mapToInt((SCTPStream s) -> {
            return s.getNum();
        }).toArray();
        if (streams.length > 0) {
            OutgoingSSNResetRequestParameter rep = new OutgoingSSNResetRequestParameter(nextNearNo(), farSeqno - 1, assoc.getNearTSN());
            rep.setStreams(streams);
            reply.addParam(rep);
        }
        streams = listOfStreamsToReset.stream().filter((SCTPStream s) -> {
            return s.OutboundIsOpen();
        }).mapToInt((SCTPStream s) -> {
            return s.getNum();
        }).toArray();   
        if (streams.length > 0) {
            IncomingSSNResetRequestParameter rep = new IncomingSSNResetRequestParameter(nextNearNo());
            rep.setStreams(streams);
            reply.addParam(rep);
        }
        Log.debug("reconfig chunk is " + reply.toString());
        return reply;
    }


}
