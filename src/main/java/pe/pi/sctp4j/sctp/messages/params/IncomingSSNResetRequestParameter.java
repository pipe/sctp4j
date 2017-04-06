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
import com.phono.srtplight.Log;
import java.nio.ByteBuffer;

/**
 *
 * @author tim
 */
public class IncomingSSNResetRequestParameter extends KnownParam {

    /*
     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |     Parameter Type = 14       |  Parameter Length = 8 + 2 * N |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |          Re-configuration Request Sequence Number             |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |  Stream Number 1 (optional)   |    Stream Number 2 (optional) |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     /                            ......                             /
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |  Stream Number N-1 (optional) |    Stream Number N (optional) |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    long reqSeqNo;
    int streams[];

    public IncomingSSNResetRequestParameter(int t, String n) {
        super(t, n);
    }

    public IncomingSSNResetRequestParameter() {
        super(14, "IncomingSSNResetRequestParameter");
    }

    public IncomingSSNResetRequestParameter(long reqNo) {
        this();
        this.reqSeqNo = reqNo;
    }

    @Override
    public void readBody(ByteBuffer body, int blen) {
        if (blen < 4) {
            Log.error("Huh ? No body to this " + this.getName());
            return;
        }
        reqSeqNo = Chunk.getUnsignedInt(body);
        if (blen > 4) {
            this.streams = new int[(blen - 4) / 2];
            for (int i = 0; i < streams.length; i++) {
                streams[i] = body.getChar();
            }
        } else {
            this.streams = new int[0];
            Log.warn("No inbound stream mentioned");
        }
    }

    @Override
    public void writeBody(ByteBuffer body) {
        Chunk.putUnsignedInt(body, reqSeqNo);
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
        ret.append("seq:" + this.reqSeqNo);
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

    public boolean sameAs(IncomingSSNResetRequestParameter other) {
        return this.reqSeqNo == other.reqSeqNo;
    }

    public int[] getStreams() {
        return streams;
    }

    public long getReqNo() {
        return this.reqSeqNo;
    }
    public void setStreams(int[] ss) {
        this.streams =ss;
    }
}
