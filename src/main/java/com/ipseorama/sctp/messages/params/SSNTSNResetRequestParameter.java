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
 * along with this program; if not, writeBody to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.ipseorama.sctp.messages.params;

import com.ipseorama.sctp.messages.Chunk;
import com.ipseorama.sctp.messages.exceptions.SctpPacketFormatException;
import com.ipseorama.sctp.messages.params.Unknown;
import java.nio.ByteBuffer;

/**
 *
 * @author tim
 */
public class SSNTSNResetRequestParameter extends Unknown {

    long _seqno;
    public SSNTSNResetRequestParameter(int t, String n) {
        super(t, n);
    }
    /*
     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |     Parameter Type = 15       |      Parameter Length = 8     |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |         Re-configuration Request Sequence Number              |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    @Override
    public void readBody(ByteBuffer body, int blen) throws SctpPacketFormatException {
        _seqno = Chunk.getUnsignedInt(body);
    }
        @Override
    public void writeBody(ByteBuffer body) throws SctpPacketFormatException {
        Chunk.putUnsignedInt(body, _seqno);
    }
}
