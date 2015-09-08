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
import com.ipseorama.sctp.messages.params.KnownParam;
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

    public ReconfigurationResponseParameter(int t, String n) {
        super(t, n);
    }

    public void readBody(ByteBuffer body, int blen) {
        this.seqNo = Chunk.getUnsignedInt(body);
        this.result = Chunk.getUnsignedInt(body);
        if (blen == 16) {
            this.senderNextTSN = Chunk.getUnsignedInt(body);
            this.receiverNextTSN = Chunk.getUnsignedInt(body);
            hasTSNs = true;
        }
    }

    public void writeBody(ByteBuffer body, int blen) {
        Chunk.putUnsignedInt(body, seqNo);
        Chunk.putUnsignedInt(body,result);
        if (hasTSNs) {
            Chunk.putUnsignedInt(body,senderNextTSN);
            Chunk.putUnsignedInt(body,receiverNextTSN);
        }
    }
}
