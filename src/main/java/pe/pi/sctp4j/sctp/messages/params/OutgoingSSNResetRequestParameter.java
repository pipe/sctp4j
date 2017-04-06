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
public class OutgoingSSNResetRequestParameter extends KnownParam {

    

    /*
     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |     Parameter Type = 13       | Parameter Length = 16 + 2 * N |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |           Re-configuration Request Sequence Number            |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |           Re-configuration Response Sequence Number           |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                Sender's Last Assigned TSN                     |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |  Stream Number 1 (optional)   |    Stream Number 2 (optional) |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     /                            ......                             /
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |  Stream Number N-1 (optional) |    Stream Number N (optional) |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    long reqSeqNo;
    long respSeqNo;
    long lastTsn;
    int streams[];

    public OutgoingSSNResetRequestParameter(int t, String n) {
        super(t, n);
    }
    public OutgoingSSNResetRequestParameter() {
          super(13, "OutgoingSSNResetRequestParameter");
    }
    public OutgoingSSNResetRequestParameter(long reqNo,long respNo, long lastTsn) {
        this();
        this.respSeqNo = respNo;
        this.lastTsn = lastTsn;
        this.reqSeqNo = reqNo;
    }

    public long getRespSeqNo() {
        return respSeqNo;
    }

    public long getReqSeqNo() {
        return reqSeqNo;
    }

    @Override
    public void readBody(ByteBuffer body, int blen) {
        reqSeqNo = Chunk.getUnsignedInt(body);
        respSeqNo = Chunk.getUnsignedInt(body);
        lastTsn = Chunk.getUnsignedInt(body);
        streams = new int[(blen - 12) / 2];
        for (int i = 0; i < streams.length; i++) {
            streams[i] = body.getChar();
        }
    }

    @Override
    public void writeBody(ByteBuffer body) {
        Chunk.putUnsignedInt(body, reqSeqNo);
        Chunk.putUnsignedInt(body, respSeqNo);
        Chunk.putUnsignedInt(body, lastTsn);
        if (streams != null) {
            for (int i = 0; i < streams.length; i++) {
                body.putChar((char) streams[i]);
            }
        }
    }

    @Override
    public String toString() {
        StringBuffer ret = new StringBuffer();
        ret.append(this.getClass().getSimpleName()).append(" ");
        ret.append("reqseq:").append(this.reqSeqNo).append(" ");
        ret.append("respseq:").append(this.respSeqNo).append(" ");
        ret.append("latsTSN:").append(this.lastTsn).append(" ");

        if (streams != null) {
            ret.append("streams {");
            for (int s : streams) {
                ret.append("" + s);
            }
            ret.append("}");
        } else {
            ret.append("no streams");
        }
        return ret.toString();
    }

    public long getLastAssignedTSN() {
        return lastTsn;
    }

    public int[] getStreams() {
        return streams;
    }
    public void setStreams(int[] ss) {
        this.streams =ss;
    }

    public boolean sameAs(OutgoingSSNResetRequestParameter other) {
        return this.reqSeqNo == other.reqSeqNo;
    }
}
