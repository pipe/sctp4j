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
class TestAssociation extends GroovyTestCase {
    
    byte [] sampleDataOpen = [(byte)0x13, (byte)0x88, (byte)0x13, (byte)0x88, (byte)0x13, (byte)0xfc, (byte)0x3a, (byte)0x88, (byte)0x8d, (byte)0xa9
        , (byte)0xa7, (byte)0x20, (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x32, (byte)0x03, (byte)0x00
        , (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x63, (byte)0x68, (byte)0x61, (byte)0x74];
    
    void setUp(){
        Log.setLevel(Log.ALL);
    }

    
    void testAss(){
        ByteBuffer sent;
        DatagramTransport mock = new DatagramTransport(){
            
            public int getReceiveLimit() throws IOException{
                return 1200;
            }

            public int getSendLimit() throws IOException{
                return 1200;
            }

            public int receive(byte[] bytes, int i, int i1, int i2) throws IOException{
                Thread.sleep(i2);
                return 0;
            }

            public void send(byte[] ou, int off, int pos) throws IOException{
                byte [] bsent = new byte[pos-off];
                java.lang.System.arraycopy(ou,off,bsent,(int)0,bsent.length);
                sent = ByteBuffer.wrap(bsent)
            }

            public void close() throws IOException{
        
            }
        };
        SCTPStream stream ;
        AssociationListener al = new AssociationListener(){
                
            public void onAssociated(Association a){
                Log.debug("Associated");

            }
            public void onDisAssociated(Association a){
                Log.debug("Disssociated");

            }
            public void onStream(SCTPStream s){
                Log.debug("Association opened stream ");
                stream = s;
            }
        };
        MockAssociation ass = new MockAssociation(mock,al);
        ass.setMyVerTag(335297160);
        ByteBuffer b = ByteBuffer.wrap(sampleDataOpen);
        Packet p = new Packet(b);
        ass.deal(p);

        Packet ack = new Packet(sent);
        List chunks = ack.getChunkList();
        assertEquals("Expecting 1 chunk ",1,chunks.size());
        DataChunk dat = chunks.get(0);
        assertEquals("Expecting a Data chunk",dat.getType(),DataChunk.DATA);
        Log.debug("got "+dat.getClass().getName()+ " chunk" + dat.toString());
        assertEquals("Expecting seqno of zero",dat.getSSeqNo(),0);
        assertEquals("Expecting stream of zero",dat.getStreamId(),0);
        assertEquals("Expecting an DCEP",dat.getPpid(),50);
        assertEquals("Data should be zero",dat.getData(),null);
        assertEquals("Expected  to parse a DCEP packet",dat.getDCEP()!=null,true);
        assertEquals("Expected an ack DCEP packet ",dat.getDCEP().isAck(),true);

        assertEquals("expecting a stream",(stream == null), false);
        stream.send("hello");
        // ugh - uses a side effect on the sent buffer, which we capture.
        Packet pack = new Packet(sent);
        chunks = pack.getChunkList();
        assertEquals("Expecting 1 chunk ",1,chunks.size());
        dat = chunks.get(0);
        assertEquals("Expecting a Data chunk",dat.getType(),DataChunk.DATA);
        Log.debug("got "+dat.getClass().getName()+ " chunk" + dat.toString());
        assertEquals("Expecting seqno of one",dat.getSSeqNo(),1); // we've done a DCEP ack by now.
        assertEquals("Expecting stream of zero",dat.getStreamId(),0);   
        assertEquals("Expecting hello in the data",dat.getDataAsString(),"hello");   
        
    }
}

