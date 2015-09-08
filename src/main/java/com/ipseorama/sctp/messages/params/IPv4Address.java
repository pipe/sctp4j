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

import com.ipseorama.sctp.messages.exceptions.SctpPacketFormatException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 *
 * @author tim
 */
public class IPv4Address extends KnownParam {

    Inet4Address addr;

    public IPv4Address(int t, String n) {
        super(t, n);
    }

    @Override
    public void readBody(ByteBuffer body, int blen) throws SctpPacketFormatException {
        byte[] data = new byte[blen];
        body.get(data);
        try {
            addr = (Inet4Address) Inet4Address.getByAddress(data);
        } catch (UnknownHostException ex) {
            throw new SctpPacketFormatException(ex.getMessage());
        }
    }

    @Override
    public void writeBody(ByteBuffer body) throws SctpPacketFormatException {
        body.put(addr.getAddress());
    }
}
