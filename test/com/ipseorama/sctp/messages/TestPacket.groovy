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

package com.ipseorama.sctp.messages

import com.ipseorama.sctp.Association
import com.ipseorama.sctp.MockAssociation
import com.ipseorama.sctp.messages.exceptions.SctpPacketFormatException;
import java.nio.Buffer
import java.nio.ByteBuffer;
import org.bouncycastle.crypto.tls.DatagramTransport

import com.phono.srtplight.Log;
/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
class TestPacket extends GroovyTestCase {
    byte [] sampleInit = [    
        (byte)0x13, (byte)0x88, (byte)0x13, (byte)0x88, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x68, 
        (byte)0x1c, (byte)0xb9, (byte)0xa0, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x56, (byte)0x95, 
        (byte)0xaa, (byte)0x39, (byte)0xc0, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x03, 
        (byte)0xff, (byte)0x08, (byte)0x00, (byte)0x83, (byte)0x14, (byte)0xf5, (byte)0xb3, (byte)0xc0, 
        (byte)0x00, (byte)0x00, (byte)0x04, (byte)0x80, (byte)0x08, (byte)0x00, (byte)0x0a, (byte)0xc1, 
        (byte)0x80, (byte)0xc0, (byte)0x81, (byte)0x82, (byte)0x0f, (byte)0x00, (byte)0x00, (byte)0x80, 
        (byte)0x02, (byte)0x00, (byte)0x24, (byte)0x8e, (byte)0xb2, (byte)0x16, (byte)0x46, (byte)0x28, 
        (byte)0xe1, (byte)0xaf, (byte)0x0d, (byte)0xf7, (byte)0x19, (byte)0xef, (byte)0x53, (byte)0xa7, 
        (byte)0xa7, (byte)0x7c, (byte)0x6c, (byte)0x0e, (byte)0x93, (byte)0x73, (byte)0x60, (byte)0x54, 
        (byte)0x73, (byte)0xee, (byte)0x2c, (byte)0x6f, (byte)0x8c, (byte)0x23, (byte)0x6c, (byte)0x51, 
        (byte)0xe1, (byte)0xbe, (byte)0x5f, (byte)0x80, (byte)0x04, (byte)0x00, (byte)0x06, (byte)0x00, 
        (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x80, (byte)0x03, (byte)0x00, (byte)0x06, (byte)0x80, 
        (byte)0xc1, (byte)0x00, (byte)0x00 ];
    
    byte [] sampleAbort  = [(byte)0x13, (byte)0x88, (byte)0x13, (byte)0x88, (byte)0x27, (byte)0x44, (byte)0xfa, (byte)0x52, (byte)0xb0, (byte)0x85
        , (byte)0xdd, (byte)0x7b, (byte)0x06, (byte)0x01, (byte)0x00, (byte)0x0c, (byte)0x00, (byte)0x0d, (byte)0x00, (byte)0x08, (byte)0x40, (byte)0x00, (byte)0x00
        , (byte)0x01];
    byte [] sampleAbort2 = [(byte)0x13,(byte)0x88,(byte)0x13,(byte)0x88,(byte)0x53,(byte)0x05,(byte)0x6d,(byte)0xb5,(byte)0x97,(byte)0xe0,(byte)0xd0
        ,(byte)0x20,(byte)0x06,(byte)0x00,(byte)0x00,(byte)0x14,(byte)0x00,(byte)0x0d,(byte)0x00,(byte)0x10,(byte)0x30,(byte)0x00,(byte)0x00,(byte)0x02
        ,(byte)0x44,(byte)0x15,(byte)0xd2,(byte)0x71,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00];
    
    byte [] sampleData = [(byte)0x13, (byte)0x88, (byte)0x13, (byte)0x88, (byte)0x13, (byte)0xfc, (byte)0x3a, (byte)0x88, (byte)0x87, (byte)0xa9
        , (byte)0x5b, (byte)0xfc, (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x20, (byte)0xf8, (byte)0x13, (byte)0x18, (byte)0x5b, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x32, (byte)0x03, (byte)0x00
        , (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x63, (byte)0x68, (byte)0x61, (byte)0x74];
    
    byte [] sampleHeartBeat = [(byte)0x13, (byte)0x88, (byte)0x13, (byte)0x88, (byte)0x00, (byte)0x6f, (byte)0x0e, (byte)0x57, (byte)0x9a, (byte)0x69, (byte)0x21, (byte)0x26
        , (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x2c, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x28, (byte)0xd7, (byte)0x47, (byte)0x62, (byte)0x53, (byte)0x88, (byte)0xc4, (byte)0x0d, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x7b, (byte)0x08, (byte)0x00, (byte)0x00, (byte)0xa0, (byte)0x82
        , (byte)0x07, (byte)0x7c, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00];
    
    byte [] dcepack = [(byte)0x13,(byte)0x88,(byte)0x13,(byte)0x88,(byte)0x96,(byte)0x83,(byte)0x0e,(byte)0xe2,(byte)0xa4,(byte)0xed,(byte)0x62,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x11,(byte)0x57,(byte)0xd3,(byte)0x59,(byte)0x0a,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x32,(byte)0x02];
    byte [] cookieEcho = [(byte)0x13,(byte)0x88,(byte)0x13,(byte)0x88,(byte)0xf5,(byte)0x36,(byte)0xd8,(byte)0x68,(byte)0x23,(byte)0xf0,(byte)0x16,(byte)0x11,(byte)0x0a,(byte)0x00
        ,(byte)0x00,(byte)0x24,(byte)0x59,(byte)0xfe,(byte)0xc8,(byte)0x5b,(byte)0xf0,(byte)0x2c,(byte)0x25,(byte)0xe6,(byte)0x97,(byte)0x23,(byte)0x33,(byte)0xa7,(byte)0x71,(byte)0x5e,(byte)0xb0,(byte)0x42,(byte)0x16,(byte)0x5f
        ,(byte)0xdd,(byte)0xa9,(byte)0x0a,(byte)0x5a,(byte)0xfa,(byte)0xa1,(byte)0x90,(byte)0xfe,(byte)0x0f,(byte)0x2b,(byte)0xd0,(byte)0x08,(byte)0x56,(byte)0xd4,(byte)0x00,(byte)0x03,(byte)0x00,(byte)0x20,(byte)0xad
        ,(byte)0xac,(byte)0x5b,(byte)0xe2,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x32,(byte)0x03,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
        ,(byte)0x00,(byte)0x04,(byte)0x00,(byte)0x00,(byte)0x63,(byte)0x68,(byte)0x61,(byte)0x74];
    void setUp(){
        Log.setLevel(Log.ALL);
    }

    class NonvalidatingPacket extends Packet{
        NonvalidatingPacket(ByteBuffer pkt){
            super(pkt);
        }
        void setChecksum(ByteBuffer pkt){}
        void checkChecksum(ByteBuffer pkt){
            // some of the tests would produce invalid checksums - we don't care - for the moment.
        }
    }
    void testValidHeader(){
        ByteBuffer b = ByteBuffer.wrap(sampleInit,0,12);
        Packet p = new NonvalidatingPacket(b);
        assertEquals("DestPort should be 5000", 5000, p.getDestPort()); 
        assertEquals("SrcPort should be 5000", 5000, p.getSrcPort());        
        assertEquals("verTag should be 0", 0, p.getVerTag());        
    }
    void testShortHeader(){
        ByteBuffer b = ByteBuffer.allocate(1);
        Exception x = null;
        try {
            Packet p = new NonvalidatingPacket(b);
        } catch (Exception e){
            x = e;
        }
        assertEquals("Exception should be thrown ",false,(x==null));
        assertEquals("Expecting exception SctpPacketException ",x.getClass(),SctpPacketFormatException.class);
    }
    
    void testRecycleHeader(){
        ByteBuffer b = ByteBuffer.wrap(sampleInit,0,12);
        Packet p = new NonvalidatingPacket(b);
        Buffer bb = p.getByteBuffer();
        assertEquals("Expecting same content in packets ",p.getHex(b),p.getHex(bb));
    }
    
    void testChunks(){
        ByteBuffer b = ByteBuffer.wrap(sampleInit);
        Packet p = new Packet(b);
        assertEquals("Expecting 1 chunk ",1,p.getChunkList().size())
    }

    void testInitWrite(){
        ByteBuffer b = ByteBuffer.wrap(sampleInit);
        Packet p = new Packet(b);
        List chunks = p.getChunkList();
        assertEquals("Expecting 1 chunk ",1,chunks.size());
        Chunk init = chunks.get(0);
        assertEquals("Expecting an init chunk",init.getType(),Chunk.INIT);
        ByteBuffer bout = ByteBuffer.allocate(sampleInit.length-12);
        init.write(bout);
        byte[]sin = new byte[sampleInit.length-12];
        System.arraycopy(sampleInit,12,sin,0, sin.length);
        assertEquals("Expected to re-make same packet",Packet.getHex(sin),Packet.getHex(bout.array()));
    }

    void testAbort(){
        ByteBuffer b = ByteBuffer.wrap(sampleAbort);
        Log.debug("Sample Abort is " + sampleAbort.length);

        Packet p = new Packet(b);
        List chunks = p.getChunkList();
        assertEquals("Expecting 1 chunk ",1,chunks.size());
        Chunk abort = chunks.get(0);
        assertEquals("Expecting an abort chunk",abort.getType(),Chunk.ABORT);
    }
    
    void testAbort2(){
        ByteBuffer b = ByteBuffer.wrap(sampleAbort2);
        Log.debug("Sample Abort is " + sampleAbort2.length);

        Packet p = new Packet(b);
        List chunks = p.getChunkList();
        assertEquals("Expecting 1 chunk ",1,chunks.size());
        Chunk abort = chunks.get(0);
        assertEquals("Expecting an abort chunk",abort.getType(),Chunk.ABORT);
    }
    
    void testCookieEcho(){
        ByteBuffer b = ByteBuffer.wrap(cookieEcho);
        Packet p = new Packet(b);
        List chunks = p.getChunkList();
        assertEquals("Expecting 2 chunks ",2,chunks.size());
        Chunk ic = chunks.get(0);
        assertEquals("Expecting a cookie echo chunk",ic.getType(),Chunk.COOKIE_ECHO);
        Chunk dopen = chunks.get(1);
        assertEquals("Expecting a data chunk",dopen.getType(),Chunk.DATA);

    }
    void testInitAck(){
        ByteBuffer b = ByteBuffer.wrap(sampleInit);
        Packet p = new Packet(b);
        List chunks = p.getChunkList();
        assertEquals("Expecting 1 chunk ",1,chunks.size());
        Chunk ic = chunks.get(0);
        assertEquals("Expecting an init chunk",ic.getType(),Chunk.INIT);

        Association ass = new MockAssociation(null,null);
        Chunk [] ca = ass.inboundInit(ic);
        assertEquals("Expecting a single reply chunk",1,ca.length);
        ByteBuffer iacbb = ass.mkPkt(ca);
        Packet iac = new Packet(iacbb);
        chunks = iac.getChunkList();
        assertEquals("Expecting 1 chunk ",1,chunks.size());
        ic  = chunks.get(0);
        assertEquals("Expecting an InitAck chunk",ic.getType(),Chunk.INITACK);

    }
    
    
    void testOpenDataChunk(){
        ByteBuffer b = ByteBuffer.wrap(sampleData);
        Packet p = new Packet(b);
        List chunks = p.getChunkList();
        assertEquals("Expecting 1 chunk ",1,chunks.size());
        DataChunk dat = chunks.get(0);
        assertEquals("Expecting a Data chunk",dat.getType(),Chunk.DATA);
        Log.debug("got "+dat.getClass().getName()+ " chunk" + dat.toString());
        assertEquals("Expecting seqno of zero",dat.getSSeqNo(),0);
        assertEquals("Expecting stream of zero",dat.getStreamId(),0);
        assertEquals("Expecting an DCEP",dat.getPpid(),50);
        assertEquals("Data should be zero",dat.getData(),null);
        assertEquals("Expected  to parse a DCEP packet",dat.getDCEP()!=null,true);
        assertEquals("Expected an open DCEP packet ",dat.getDCEP().isAck(),false);


    }
    
    void testHeartBeatChunk(){
        ByteBuffer b = ByteBuffer.wrap(sampleHeartBeat);
        Packet p = new Packet(b);
        List chunks = p.getChunkList();
        assertEquals("Expecting 1 chunk ",1,chunks.size());
        HeartBeatChunk dat = chunks.get(0);
        assertEquals("Expecting a HeartBeatChunk",dat.getType(),Chunk.HEARTBEAT);
        Chunk[] r = dat.mkReply();
        assertEquals("Expecting 1 chunk ",1,r.length);
        assertEquals("Expecting a HeartBeatAckChunk",r[0].getType(),Chunk.HEARTBEAT_ACK);
    }
    
    void testDCEPAckChunk(){
        ByteBuffer b = ByteBuffer.wrap(dcepack);
        Packet p = new Packet(b);
        List chunks = p.getChunkList();
        assertEquals("Expecting 1 chunk ",1,chunks.size());
        DataChunk dat = chunks.get(0);
        assertEquals("Expecting a DataChunk",dat.getType(),Chunk.DATA);
    }
    
}

