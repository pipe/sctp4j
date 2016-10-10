/*
 * Copyright (C) 2014 tim
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
package com.ipseorama.sctp.messages.params;

import com.ipseorama.sctp.messages.Chunk;
import com.ipseorama.sctp.messages.params.VariableParam;
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
    public OutgoingSSNResetRequestParameter(IncomingSSNResetRequestParameter ireset,long reqNo, long lastTsn) {
        this();
        this.respSeqNo = ireset.reqSeqNo;
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
