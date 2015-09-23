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
package com.ipseorama.sctp;

import com.ipseorama.sctp.messages.Chunk;
import com.ipseorama.sctp.messages.DataChunk;
import com.ipseorama.sctp.messages.InitChunk;
import com.ipseorama.sctp.messages.SackChunk;
import com.ipseorama.sctp.small.BlockingSCTPStream;
import org.bouncycastle.crypto.tls.DatagramTransport;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class MockAssociation extends Association {

    public MockAssociation(DatagramTransport transport, AssociationListener al) {
        super(transport, al);
    }



    @Override
    public void enqueue(DataChunk d) {
        throw new UnsupportedOperationException("Not supported yet. (enqueue)"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SCTPStream mkStream(int id) {
        return new SCTPStream(this, id) {

            @Override
            public void send(String message) throws Exception {
                SCTPMessage m = new SCTPMessage(message, this);
                _ass.sendAndBlock(m);
            }

        };
    }

    @Override
    public void sendAndBlock(SCTPMessage m) throws Exception {
        Chunk[] dar = new Chunk[1];

        DataChunk dc = new DataChunk();
        m.fill(dc);
        dc.setTsn(_nearTSN++);
        // check rollover - will break at maxint.
        dar[0] = dc;
        send(dar);

    }

    @Override
    public SCTPMessage makeMessage(byte[] bytes, BlockingSCTPStream aThis) {
        throw new UnsupportedOperationException("Not supported yet. (makeMessage)"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Chunk[] inboundInit(InitChunk i) {
        return super.inboundInit(i);
    }

    public void setMyVerTag(int v) {
        super._myVerTag = v;
    }

    @Override
    public SCTPMessage makeMessage(String s, BlockingSCTPStream aThis) {
        throw new UnsupportedOperationException("Not supported yet.(Make Message - string"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected Chunk[] sackDeal(SackChunk sackChunk) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


}
