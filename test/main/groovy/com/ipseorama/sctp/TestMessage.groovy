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

package com.ipseorama.sctp

import com.ipseorama.sctp.Association
import com.ipseorama.sctp.AssociationListener
import com.ipseorama.sctp.messages.DataChunk
import com.ipseorama.sctp.messages.Packet
import com.ipseorama.sctp.messages.SackChunk
import com.ipseorama.sctp.messages.exceptions.SctpPacketFormatException;
import com.ipseorama.base.dataChannel.DECP.DCOpen;
import java.nio.Buffer
import java.nio.ByteBuffer;
import java.security.SecureRandom
import org.bouncycastle.crypto.tls.DTLSTransport
import org.bouncycastle.crypto.tls.DatagramTransport

import com.phono.srtplight.Log;
/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
class TestMessage extends GroovyTestCase {
    def data = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    Association a;
    SCTPStream stream;
    def id;
    
    void setUp(){
        Log.setLevel(Log.ALL);
        a = null;
        id = 1000;
        stream = new SCTPStream(a, id){
            public void send(String message){
            } 
        };
    }
    
    void testMessFillSingle(){
        // assume capacity > 52        
        SCTPMessage m = new SCTPMessage(data,stream);
        def chunks = [];
        while (m.hasMoreData()) {
            DataChunk dc = new DataChunk()
            m.fill(dc);
            chunks += dc;
        }
        Log.debug("chunks "+chunks.size);
        assertEquals ("Wrong number of chunks",chunks.size,1);
        
        assertEquals ("First (and only) chunk should have single flag set",chunks[0].getFlags(),DataChunk.SINGLEFLAG);

    }
    
    void testMessFill1(){
 
        SCTPMessage m = new SCTPMessage(data,stream);
        def chunks = [];
        while (m.hasMoreData()) {
            DataChunk dc = new DataChunk(){
                public int getCapacity() {
                    return 1; // shrug
                }
            };
            m.fill(dc);
            chunks += dc;
        }
        Log.debug("chunks "+chunks.size);
        assertEquals ("Wrong number of chunks",chunks.size,data.length());
        
        assertEquals ("Start chunk should have start flag set",chunks[0].getFlags(),DataChunk.BEGINFLAG);
        assertEquals ("End chunk should have end flag set",chunks[data.length()-1].getFlags(),DataChunk.ENDFLAG);
        for (int i = 1; i< data.length()-1;i++){
            assertEquals ("middle chunk should have no flag set",chunks[i].getFlags(),0);
            assertEquals ("middle data should match input",chunks[i].getDataAsString(),data[i]);
        }
    }

}
