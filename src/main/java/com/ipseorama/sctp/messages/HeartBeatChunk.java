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

import com.ipseorama.sctp.messages.exceptions.SctpPacketFormatException;
import com.ipseorama.sctp.messages.params.VariableParam;
import java.nio.ByteBuffer;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class HeartBeatChunk extends Chunk {

    public HeartBeatChunk(byte type, byte flags, int length, ByteBuffer pkt) {
        super(type, flags, length, pkt);
        if (_body.remaining() >= 4) {
            while (_body.hasRemaining()) {
                VariableParam v = readVariable();
                _varList.add(v);
            }
        }
    }
    @Override
    public void validate() throws SctpPacketFormatException{
        VariableParam hbd;
        if ((_varList == null) || (_varList.size() != 1)) {
            throw new SctpPacketFormatException("No (or too much content in this heartbeat packet");
        }
        hbd = _varList.get(0);
        if (!(hbd instanceof HeartbeatInfo)){
            throw new SctpPacketFormatException("Expected a heartbeatinfo in this packet");
        }
    }
    
    public Chunk[] mkReply(){
        Chunk[] rep = new Chunk[1];
        HeartBeatAckChunk dub = new HeartBeatAckChunk();
        dub._varList = this._varList;
        rep[0] = dub;
        return rep;
    }
    @Override
    void putFixedParams(ByteBuffer ret) {
        // none
    }

}
