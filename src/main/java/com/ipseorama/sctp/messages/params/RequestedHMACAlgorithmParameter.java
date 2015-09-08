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

import java.nio.ByteBuffer;

/**
 *
 * @author tim
 */
public class RequestedHMACAlgorithmParameter extends KnownParam {
    /*
    
     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |     Parameter Type = 0x8004   |       Parameter Length        |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |          HMAC Identifier 1    |      HMAC Identifier 2        |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     /                                                               /
     \                              ...                              \
     /                                                               /
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |        HMAC Identifier n      |           Padding             |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    int hmacs[];
    /*
     +-----------------+--------------------------+
     | HMAC Identifier | Message Digest Algorithm |
     +-----------------+--------------------------+
     | 0               | Reserved                 |
     | 1               | SHA-1 defined in [8]     |
     | 2               | Reserved                 |
     | 3               | SHA-256 defined in [8]   |
     +-----------------+--------------------------+
     */

    public RequestedHMACAlgorithmParameter(int t, String n) {
        super(t, n);
    }

    public void readBody(ByteBuffer body, int blen) {
        hmacs = new int[blen / 2];
        for (int i = 0; i < hmacs.length; i++) {
            hmacs[i] = body.getChar();
        }
    }

    public void writeBody(ByteBuffer body) {
        if (hmacs != null) {
            for (int i = 0; i < hmacs.length; i++) {
                body.putChar((char) hmacs[i]);
            }
        }
    }

    public String toString() {
        String ret = " Hmacs are ";
        for (int i = 0; i < hmacs.length; i++) {
            switch (hmacs[i]) {
                case 0:
                case 2:
                    ret += " Reserved ";
                    break;
                case 1:
                    ret += " SHA-1 ";
                    break;
                case 3:
                    ret += " SHA-256 ";
                    break;
            }
        }
        return super.toString() + ret;
    }

    public boolean doesSha256() {
        boolean ret = false;
        for (int i = 0; i < hmacs.length; i++) {
            if (3 == hmacs[i]) {
                ret = true;
            }
        }
        return ret;
    }
}
