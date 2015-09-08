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

import com.ipseorama.sctp.messages.params.VariableParam;
import java.nio.ByteBuffer;

/**
 *
 * @author tim
 */
public class HostNameAddress extends KnownParam {

    String hostname;

    public HostNameAddress(int t, String n) {
        super(t, n);
    }

    public void readBody(ByteBuffer body, int blen) {
        byte data[] = new byte[blen];
        body.get(data);
        int off = blen - 1;
        // trim any 0 bytes off the end.
        while ((off > 0) && (data[off--] == 0)) {
            blen--;
        }
        hostname = new String(data, 0, blen);
    }
    public void writeBody(ByteBuffer body) {
        // gonz up a C style string.
        byte [] b = hostname.getBytes();
        _data = new byte[b.length+1];
        System.arraycopy(b, 0, _data, 0, b.length);
        body.put(_data);
    }
}
