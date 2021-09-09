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
package pe.pi.sctp4j.sctp.small;

import pe.pi.sctp4j.sctp.SCTPMessage;
import pe.pi.sctp4j.sctp.SCTPStream;
import pe.pi.sctp4j.sctp.messages.DataChunk;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import pe.pi.sctp4j.sctp.dataChannel.DECP.DCOpen;
import com.phono.srtplight.Log;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class BlockingSCTPStream extends SCTPStream {

    private final ExecutorService _ex_service;
    private HashMap<Integer, SCTPMessage> undeliveredOutboundMessages = new HashMap();
    private final ThreadedAssociation _ta;

    BlockingSCTPStream(ThreadedAssociation a, Integer id) {
        super(a, id);
        _ta = a;
        _ex_service = Executors.newSingleThreadExecutor();
    }

    @Override
    synchronized public void send(String message) throws Exception {
        SCTPMessage m = _ta.makeMessage(message, this);
        if (m != null) {
            undeliveredOutboundMessages.put(m.getSeq(), m);
            _ta.sendAndBlock(m);
        }
    }

    @Override
    synchronized public void send(byte[] message) throws Exception {
        SCTPMessage m = _ta.makeMessage(message, this);
        undeliveredOutboundMessages.put(m.getSeq(), m);
        _ta.sendAndBlock(m);
    }

    @Override
    public void send(DCOpen message) throws Exception {
        final SCTPMessage m = _ta.makeMessage(message, this);
        undeliveredOutboundMessages.put(m.getSeq(), m);
        _ex_service.execute(() -> {
            try {
                _ta.sendAndBlock(m);
            } catch (Exception ex) {
                Log.error("failed to send DCOpen" + m.toString());
            }
        });

    }

    @Override
    public void deliverMessage(SCTPMessage message) {
        _ex_service.execute(message); // switch to callable ?
    }

    @Override
    public void delivered(DataChunk d) {
        int f = d.getFlags();
        if ((f & DataChunk.ENDFLAG) > 0) {
            int ssn = d.getSSeqNo();
            SCTPMessage st = undeliveredOutboundMessages.remove(ssn);
            if (st != null) {
                st.acked();
            }
        }
    }

    @Override
    public boolean idle() {
        return undeliveredOutboundMessages.isEmpty();
    }

}
