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
public class ReconfigurationResponseParameter extends KnownParam {

    /*
     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |     Parameter Type = 16       |      Parameter Length         |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |         Re-configuration Response Sequence Number             |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                            Result                             |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                   Sender's Next TSN (optional)                |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                  Receiver's Next TSN (optional)               |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    long seqNo;
    long result;
    long senderNextTSN;
    long receiverNextTSN;
    boolean hasTSNs;
    public final static int SUCCESS_NOTHING_TO_DO = 0;
    public final static int SUCCESS_PERFORMED = 1;
    public final static int DENIED = 2;
    public final static int ERROR_WRONG_SSN = 3;
    public final static int ERROR_REQUEST_ALREADY_IN_PROGESS = 4;
    public final static int ERROR_BAD_SEQUENCE_NUMBER = 5;
    public final static int IN_PROGRESS = 6;
    final static String valuenames[] = {
        "Success - Nothing to do",
        "Success - Performed",
        "Denied",
        "Error - Wrong SSN",
        "Error - Request already in progress",
        "Error - Bad Sequence Number",
        "In progress"
    };

    /*
                 +--------+-------------------------------------+
             | Result | Description                         |
             +--------+-------------------------------------+
             | 0      | Success - Nothing to do             |
             | 1      | Success - Performed                 |
             | 2      | Denied                              |
             | 3      | Error - Wrong SSN                   |
             | 4      | Error - Request already in progress |
             | 5      | Error - Bad Sequence Number         |
             | 6      | In progress                         |
             +--------+-------------------------------------+
     */
    public ReconfigurationResponseParameter(int t, String n) {
        super(t, n);
    }

    public ReconfigurationResponseParameter() {
        this(16, "ReconfigurationResponseParameter");
    }

    @Override
    public void readBody(ByteBuffer body, int blen) {
        this.seqNo = Chunk.getUnsignedInt(body);
        this.result = Chunk.getUnsignedInt(body);
        if (blen == 16) {
            this.senderNextTSN = Chunk.getUnsignedInt(body);
            this.receiverNextTSN = Chunk.getUnsignedInt(body);
            hasTSNs = true;
        }
    }

    @Override
    public void writeBody(ByteBuffer body) {
        Chunk.putUnsignedInt(body, seqNo);
        Chunk.putUnsignedInt(body, result);
        if (hasTSNs) {
            Chunk.putUnsignedInt(body, senderNextTSN);
            Chunk.putUnsignedInt(body, receiverNextTSN);
        }
    }

    private String resultToName() {
        return ((result >= 0) && (result < valuenames.length))
                ? valuenames[(int) result] : "invalid value";
    }

    public String toString() {
        StringBuffer ret = new StringBuffer();
        ret.append(this.getClass().getSimpleName()).append(" ");
        ret.append("seqNo:").append(this.seqNo).append(" ");
        ret.append("result:").append(resultToName()).append(" ");
        if (hasTSNs) {
            ret.append("senderNextTSN:").append(this.senderNextTSN).append(" ");
            ret.append("receiverNextTSN:").append(this.receiverNextTSN).append(" ");
        }
        return ret.toString();
    }

    public void setResult(int res) {
        result = res;
    }

    public void setSeq(long reqSeqNo) {
        seqNo = reqSeqNo;
    }

}
