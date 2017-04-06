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

/**
 *
 * @author tim
 * what DCEPS do 
 */
public class DCEPStreamBehaviour implements
        SCTPStreamBehaviour {

    @Override
    public Chunk[] respond(SCTPStream a) {
        Log.debug("in respond() for a opened stream " + a.getLabel());
        return null;
    }

    @Override
    public void deliver(SCTPStream s, SortedSet<DataChunk> a, SCTPStreamListener l) {
        Log.debug("in deliver() for stream " + s.getLabel() + " with " + a.size() + " chunks. ");
        // strictly this should be looking at flags etc, and bundling the result into a message
        for (DataChunk dc : a) {
            if (dc.getDCEP() != null) {
                Log.debug("in deliver() for a DCEP message " + dc.getDataAsString());
            } else {
                Log.debug("inbound data chunk is " + dc.toString());
                l.onMessage(s, dc.getDataAsString());
            }
        }
        a.clear();
    }

}
