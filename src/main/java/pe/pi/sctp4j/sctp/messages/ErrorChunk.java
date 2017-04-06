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
package pe.pi.sctp4j.sctp.messages;

import pe.pi.sctp4j.sctp.messages.params.KnownError;
import pe.pi.sctp4j.sctp.messages.params.VariableParam;
import com.phono.srtplight.Log;
import java.nio.ByteBuffer;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */

/*
 3.3.10.  Operation Error (ERROR) (9)

 An endpoint sends this chunk to its peer endpoint to notify it of
 certain error conditions.  It contains one or more error causes.  An
 Operation Error is not considered fatal in and of itself, but may be
 used with an ABORT chunk to report a fatal condition.  It has the
 following parameters:

 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 |   Type = 9    | Chunk  Flags  |           Length              |
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 \                                                               \
 /                    one or more Error Causes                   /
 \                                                               \
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

 Chunk Flags: 8 bits

 Set to 0 on transmit and ignored on receipt.

 Length: 16 bits (unsigned integer)

 Set to the size of the chunk in bytes, including the chunk header
 and all the Error Cause fields present.
 */
public class ErrorChunk extends Chunk {

    public ErrorChunk() {
        super((byte) Chunk.ERROR);
    }

    public ErrorChunk(KnownError e) {
        this();
        _varList.add(e);
    }

    public ErrorChunk(KnownError[] el) {
        this();
        for (KnownError e : el) {
            _varList.add(e);
        }
    }

    public ErrorChunk(byte type, byte flags, int length, ByteBuffer pkt) {
        super(type, flags, length, pkt);
        if (_body.remaining() >= 4) {
            Log.verb("Error" + this.toString());
            while (_body.hasRemaining()) {
                VariableParam v = readErrorParam();
                _varList.add(v);
            }
        }
    }

    @Override
    void putFixedParams(ByteBuffer ret) {

    }

}
