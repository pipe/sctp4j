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
