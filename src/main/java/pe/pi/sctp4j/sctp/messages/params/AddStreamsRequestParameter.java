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
package pe.pi.sctp4j.sctp.messages.params;

import pe.pi.sctp4j.sctp.messages.Chunk;
import java.nio.ByteBuffer;

/**
 *
 * @author tim
 */
public class AddStreamsRequestParameter extends Unknown {

    public AddStreamsRequestParameter(int t, String n) {
        super(t, n);
    }
    /*
     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |     Parameter Type = 17       |      Parameter Length = 12    |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |          Re-configuration Request Sequence Number             |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |      Number of new streams    |         Reserved              |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    long reconfReqSeqNo;
    int numNewStreams;
    int reserved;

    @Override
    public void readBody(ByteBuffer body, int blen) {
        reconfReqSeqNo = Chunk.getUnsignedInt(body);
        numNewStreams = body.getChar();
        reserved = body.getChar();
    }

    @Override
    public void writeBody(ByteBuffer body) {
        Chunk.putUnsignedInt(body,reconfReqSeqNo);
        body.putChar((char) numNewStreams);
        body.putChar((char) reserved);
    }
}
