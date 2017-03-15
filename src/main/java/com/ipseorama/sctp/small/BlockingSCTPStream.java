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
package com.ipseorama.sctp.small;

import com.ipseorama.sctp.SCTPMessage;
import com.ipseorama.sctp.Association;
import com.ipseorama.sctp.SCTPStream;
import com.ipseorama.sctp.messages.DataChunk;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class BlockingSCTPStream extends SCTPStream {

    private final ExecutorService _ex_service;
    private HashMap<Integer,SCTPMessage> undeliveredOutboundMessages = new HashMap();
    
    BlockingSCTPStream(Association a, Integer id) {
        super(a, id);
        _ex_service = Executors.newSingleThreadExecutor();
    }

    @Override
    synchronized public void send(String message) throws Exception {
        Association a = super.getAssociation();
        SCTPMessage m = a.makeMessage(message, this);
        
        a.sendAndBlock(m);
    }
    @Override
    synchronized public void send(byte[] message) throws Exception {
        Association a = super.getAssociation();
        SCTPMessage m = a.makeMessage(message, this);
        undeliveredOutboundMessages.put(m.getSeq(),m);
        a.sendAndBlock(m);
    }

    @Override
    public void deliverMessage(SCTPMessage message) {
        _ex_service.execute(message); // switch to callable ?
    }

    @Override
    public void delivered(DataChunk d) {
        int f = d.getFlags();
        if ((f & DataChunk.ENDFLAG) > 0){
            int ssn = d.getSSeqNo();
            SCTPMessage st = undeliveredOutboundMessages.remove(ssn);
            if (st != null) {st.acked();}
        }
    }

    @Override
    public boolean idle(){
        return undeliveredOutboundMessages.isEmpty();
    }
    
}
