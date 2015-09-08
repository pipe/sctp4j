/*
 * Copyright (C) 2014 Westhawk Ltd<thp@westhawk.co.uk>
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
package com.ipseorama.sctp.messages;

import com.ipseorama.sctp.messages.params.KnownError;
import com.ipseorama.sctp.messages.params.VariableParam;
import com.phono.srtplight.Log;
import java.nio.ByteBuffer;
import java.util.ArrayList;

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
