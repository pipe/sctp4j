/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.sctp;

import com.ipseorama.sctp.messages.Chunk;
import com.ipseorama.sctp.messages.DataChunk;
import com.ipseorama.sctp.messages.SackChunk;
import com.ipseorama.sctp.messages.exceptions.SctpPacketFormatException;
import com.ipseorama.sctp.small.BlockingSCTPStream;
import java.io.IOException;
import java.util.Random;
import java.util.TreeSet;
import org.bouncycastle.crypto.tls.DatagramTransport;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tim
 */
public class SCTPMessageTest {

    private Random _rand = new Random(1); // deterministic non crypto quality random for repeatable tests
    private SCTPStream _fakeStream;

    public SCTPMessageTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }
    Association _fakeAssociation;

    @Before
    public void setUp() {
        DatagramTransport _fakedt = new DatagramTransport() {

            @Override
            public int getReceiveLimit() throws IOException {
                return 1200;
            }

            @Override
            public int getSendLimit() throws IOException {
                return 1200;
            }

            @Override
            public int receive(byte[] bytes, int i, int i1, int waitMs) throws IOException {
                try {
                    Thread.sleep(waitMs);
                } catch (Exception x) {
                }
                throw new java.io.InterruptedIOException("empty");
            }

            @Override
            public void send(byte[] bytes, int i, int i1) throws IOException {
            }

            @Override
            public void close() throws IOException {
            }

        };
        _fakeAssociation = new Association(_fakedt, null) {
            @Override
            public void associate() throws SctpPacketFormatException, IOException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void enqueue(DataChunk d) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public SCTPStream mkStream(int id) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void sendAndBlock(SCTPMessage m) throws Exception {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public SCTPMessage makeMessage(byte[] bytes, BlockingSCTPStream aThis) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public SCTPMessage makeMessage(String s, BlockingSCTPStream aThis) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            protected Chunk[] sackDeal(SackChunk sackChunk) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
        _fakeStream = new SCTPStream(_fakeAssociation, new Integer(22)) {
            @Override
            public void send(String message) throws Exception {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void deliverMessage(SCTPMessage message) {
                message.run();
            }

            @Override
            public void send(byte[] message) throws Exception {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void delivered(DataChunk d) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

        };
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of setCompleteHandler method, of class SCTPMessage.
     */
    /*
     @Test
     public void testSetCompleteHandler() {
     System.out.println("setCompleteHandler");
     MessageCompleteHandler mch = null;
     SCTPMessage instance = null;
     instance.setCompleteHandler(mch);
     // TODO review the generated test code and remove the default call to fail.
     fail("The test case is a prototype.");
     }
     */
    /**
     * Test of hasMoreData method, of class SCTPMessage.
     */
    /*
     @Test
     public void testHasMoreData() {

     }
     */
    /**
     * Test of fill method, of class SCTPMessage.
     */
    @Test
    public void testFillShortString() {
        System.out.println("--> fill short string ");
        String testString = "This is a short test";
        SCTPMessage instance = new SCTPMessage(testString, _fakeStream);
        TreeSet<DataChunk> chunks = new TreeSet<DataChunk>();
        while (instance.hasMoreData()) {
            DataChunk dc = new DataChunk();
            instance.fill(dc);
            chunks.add(dc);
        }
        assertEquals(chunks.size(), 1);
        int ppid = ((DataChunk)chunks.first()).getPpid();
        assertEquals(ppid,DataChunk.WEBRTCSTRING);
    }
    @Test
    public void testFillShortBlob() {
        System.out.println("--> fill short blob ");
        byte [] testBlob = new byte[21];
        _rand.nextBytes(testBlob);
        SCTPMessage instance = new SCTPMessage(testBlob, _fakeStream);
        TreeSet<DataChunk> chunks = new TreeSet<DataChunk>();
        while (instance.hasMoreData()) {
            DataChunk dc = new DataChunk();
            instance.fill(dc);
            chunks.add(dc);
        }
        assertEquals(chunks.size(), 1);
        int ppid = ((DataChunk)chunks.first()).getPpid();
        assertEquals(ppid,DataChunk.WEBRTCBINARY);
    }
    @Test
    public void testFillLongString() {
        System.out.println("--> fill long");
        StringBuffer sb = new StringBuffer("This is a");
        for (int i = 0; i < 1030; i++) {
            sb.append(" long");
        }
        sb.append(" test.");
        String testString = sb.toString();
        SCTPMessage instance = new SCTPMessage(testString, _fakeStream);
        TreeSet<DataChunk> chunks = new TreeSet<DataChunk>();
        long tsn = 111;

        while (instance.hasMoreData()) {
            DataChunk dc = new DataChunk();
            dc.setTsn(tsn++);
            instance.fill(dc);
            chunks.add(dc);
        }
        double pktsz = chunks.first().getDataSize();
        int estimate = (int) Math.ceil(testString.length() / pktsz);
        assertEquals(chunks.size(), estimate);
    }
    
    @Test
    public void testEmptyString() {
        System.out.println("--> fill empty string");
        StringBuffer sb = new StringBuffer("");
        String testString = sb.toString();
        SCTPMessage instance = new SCTPMessage(testString, _fakeStream);
        TreeSet<DataChunk> chunks = new TreeSet<DataChunk>();
        long tsn = 111;

        while (instance.hasMoreData()) {
            DataChunk dc = new DataChunk();
            dc.setTsn(tsn++);
            instance.fill(dc);
            chunks.add(dc);
        }
        int pktsz = chunks.first().getDataSize();
        assertEquals(chunks.size(), 1);
        assertEquals(pktsz,1);
        int ppid = ((DataChunk)chunks.first()).getPpid();
        assertEquals(ppid,DataChunk.WEBRTCSTRINGEMPTY);
    }
    
    @Test
    public void testEmptyBlob() {
        System.out.println("--> fill empty blob");
        byte [] testBlob = new byte[0];
        SCTPMessage instance = new SCTPMessage(testBlob, _fakeStream);
        TreeSet<DataChunk> chunks = new TreeSet<DataChunk>();
        long tsn = 111;

        while (instance.hasMoreData()) {
            DataChunk dc = new DataChunk();
            dc.setTsn(tsn++);
            instance.fill(dc);
            chunks.add(dc);
        }
        assertEquals(chunks.size(), 1);
        int pktsz = chunks.first().getDataSize();
        assertEquals(pktsz,1);
        int ppid = ((DataChunk)chunks.first()).getPpid();
        assertEquals(ppid,DataChunk.WEBRTCBINARYEMPTY);
    }
    
    /**
     * Test of getData method, of class SCTPMessage.
     */
    /*
     @Test
     public void testGetData() {
     System.out.println("getData");
     SCTPMessage instance = null;
     byte[] expResult = null;
     byte[] result = instance.getData();
     assertArrayEquals(expResult, result);
     // TODO review the generated test code and remove the default call to fail.
     fail("The test case is a prototype.");
     }*/
}
