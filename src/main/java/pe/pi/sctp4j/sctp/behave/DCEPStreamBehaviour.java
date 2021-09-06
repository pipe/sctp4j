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
package pe.pi.sctp4j.sctp.behave;

import pe.pi.sctp4j.sctp.SCTPStream;
import pe.pi.sctp4j.sctp.SCTPStreamListener;
import pe.pi.sctp4j.sctp.messages.Chunk;
import pe.pi.sctp4j.sctp.messages.DataChunk;
import com.phono.srtplight.Log;
import java.util.SortedSet;
import pe.pi.sctp4j.sctp.AssociationListener;
import pe.pi.sctp4j.sctp.SCTPOutboundStreamOpenedListener;
import pe.pi.sctp4j.sctp.dataChannel.DECP.DCOpen;

/**
 *
 * @author tim what DCEPS do
 */
public class DCEPStreamBehaviour implements
        SCTPStreamBehaviour {

    private final AssociationListener al;

    public DCEPStreamBehaviour(AssociationListener associationListener) {
        al = associationListener;
        Log.debug("DCEPStreamBehaviour");
    }

    @Override
    public Chunk[] respond(SCTPStream a) {
        return null;
    }

    @Override
    public void deliver(SCTPStream s, SortedSet<DataChunk> a, SCTPStreamListener l) {
        DataChunk dc = a.first();
        int flags = dc.getFlags() & DataChunk.SINGLEFLAG; // mask to the bits we want
        long tsn = dc.getTsn();
        int messageNo = s.getNextMessageSeqIn();
       
        

// only interested in the first chunk which should be an ack or an open.
        DCOpen dcep = dc.getDCEP();
        if (dcep != null) {
            Log.debug("DCEPStreamBehaviour has a dcep first.");
            if(flags != DataChunk.SINGLEFLAG){
                Log.error("Dcep isn't a single !!?!");
            }
            messageNo++;
            s.setNextMessageSeqIn(messageNo);
            a.remove(dc);
            SCTPStreamBehaviour behave = dcep.mkStreamBehaviour();
            s.setBehave(behave);
            if (!dcep.isAck()) {
                Log.debug("decp open  " + dcep.toString());
                s.setLabel(dcep.getLabel());
                try {
                    s.openAck(dcep);
                    al.onDCEPStream(s, s.getLabel(), dc.getPpid());
                    // really you would rather have these two the other way around. Not acking
                    // unless the creation fully works, But this is a 0-rtt protocol - so 
                    // it is best to ack asap.
                } catch (Exception x) {
                    try {
                        s.close();
                    } catch (Exception sx) {
                        Log.error("Can't close " + s.toString() + " because " + x.getMessage());
                    }
                }
            } else {
                Log.debug("got a dcep ack for " + s.getLabel());
                if ((l != null) && (l instanceof SCTPOutboundStreamOpenedListener)) {
                    ((SCTPOutboundStreamOpenedListener) l).opened(s);
                }
            }

            // and consume the rest using the new behave.
            if (behave != null) {
                behave.deliver(s, a, l);
            }
        } else {
            Log.debug("Cant deliver chunks before DCEP");
        }
    }

}
