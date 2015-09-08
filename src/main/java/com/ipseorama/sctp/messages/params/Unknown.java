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

import com.ipseorama.sctp.messages.exceptions.SctpPacketFormatException;
import java.nio.ByteBuffer;

/**
 *
 * @author tim
 */
public class Unknown implements VariableParam {

    byte _data[];
    final int _type;
    final String _name;
    
    public Unknown(int t,String n){
        _type = t;
        _name = n;
    }
    
    @Override
    public void readBody(ByteBuffer b, int len) throws SctpPacketFormatException {
        _data = new byte[len];
        b.get(_data);
    }

    @Override
    public void writeBody(ByteBuffer b) throws SctpPacketFormatException {
        b.put(this._data);
    }

    @Override
    public int getType() {
        return _type;
    }

    @Override
    public String getName() {
        return _name;
    }

}
