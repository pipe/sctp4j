/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.sctp.small;

import com.ipseorama.sctp.Association;
import com.ipseorama.sctp.AssociationListener;
import com.ipseorama.sctp.SCTPByteStreamListener;
import com.ipseorama.sctp.SCTPMessage;
import com.ipseorama.sctp.SCTPStream;
import com.ipseorama.sctp.SCTPStreamListener;
import com.ipseorama.sctp.behave.OrderedStreamBehaviour;
import com.phono.srtplight.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class ThreadedAssociationTest {
    private static Vector<DatagramTransport> __transList;

    public ThreadedAssociationTest() {
       
    }

    @BeforeClass
    public static void setUpClass() {
        Log.setLevel(Log.WARN);
        __transList = new Vector<DatagramTransport>();
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
        for(DatagramTransport t:__transList){
            try {
                t.close();
            } catch (IOException ex) {
                Log.warn(ex.getMessage());
            }
        } 
        __transList.clear();
    }

    private DatagramTransport[] mkMockTransports() {
        BlockingQueue<byte[]> left = new ArrayBlockingQueue<>(20);
        BlockingQueue<byte[]> right = new ArrayBlockingQueue<>(20);
        DatagramTransport[] ret = new DatagramTransport[2];
        ret[0] = new MockTransport(left, right);
        ret[1] = new MockTransport(right, left);
        __transList.add(ret[0]);
        __transList.add(ret[1]);
        return ret;
    }

    class MockAssociationListener implements AssociationListener {

        Boolean associated = null;
        SCTPStream stream = null;

        @Override
        synchronized public void onAssociated(Association a) {
            Log.debug("associated");
            associated = true;
            this.notifyAll();
        }

        @Override
        synchronized public void onDisAssociated(Association a) {
            Log.debug("dis associated");
            associated = false;
            this.notifyAll();
        }

        @Override
        public void onDCEPStream(SCTPStream s, String label, int type) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void onRawStream(SCTPStream s) {
            stream = s;
        }
    }

    class MockTransport implements DatagramTransport {

        private final BlockingQueue<byte[]> _packetQueueIn;
        private boolean _isShutdown;
        private final BlockingQueue<byte[]> _packetQueueOut;

        MockTransport(BlockingQueue<byte[]> in, BlockingQueue<byte[]> out) {
            _packetQueueIn = in;
            _packetQueueOut = out;
        }

        @Override
        public int getReceiveLimit() throws IOException {
            return 1200;
        }

        @Override
        public int getSendLimit() throws IOException {
            return 1200;
        }

        @Override
        public int receive(byte[] buf, int off, int len, int waitMillis) throws IOException {
            int ret = -1;
            if (!_isShutdown || (_packetQueueIn.peek() != null)) {
                try {
                    Log.debug("Mock recv ");
                    byte pkt[] = _packetQueueIn.poll(waitMillis, TimeUnit.MILLISECONDS);
                    if (pkt != null) {
                        Log.debug("Mock recv pkt length =" + pkt.length);
                        Log.debug("Mock recv buff length =" + len);
                        if (pkt.length > len){
                            throw new IllegalArgumentException("We should not be here");
                        }
                        ret = Math.min(len, pkt.length);
                        System.arraycopy(pkt, 0, buf, off, ret);
                    } else {
                        throw new java.io.InterruptedIOException("empty Queue");
                    }
                } catch (InterruptedException ex) {
                    Log.debug("recv interrupted ");
                    throw new java.io.InterruptedIOException(ex.getMessage());
                }
            } else {
                Log.debug("Transport  shutdown - throw exception.");
                throw new java.io.EOFException("Transport was shutdown.");
            }
            Log.debug("Mock receive returning " + ret);
            return ret;
        }

        @Override
        public void send(byte[] bytes, int off, int len) throws IOException {
            if (len > 0) {
                byte stack[] = new byte[len];
                System.arraycopy(bytes, 0, stack, 0, len);
                boolean res = _packetQueueOut.offer(stack);
                if (!res) {
                    Log.debug("overflowed stack");
                }
            }
        }

        @Override
        public void close() throws IOException {
            if (_isShutdown) {
                Log.debug("Transport  already shutdown - throw exception.");
                throw new java.io.EOFException("transport shut.");
            }
            Log.debug("Transport  shutdown.");
            _isShutdown = true;

        }

    }

    @Test
    public void testTransportLeft() throws Exception {
        testTransport(0, 1);
    }

    @Test
    public void testTransportRight() throws Exception {
        testTransport(1, 0);
    }

    private void testTransport(int l, int r) throws Exception {
        DatagramTransport trans[] = mkMockTransports();
        String mess = "Test Message";
        trans[l].send(mess.getBytes(), 0, mess.length());
        int rl = trans[r].getReceiveLimit();
        byte[] bytes = new byte[rl];
        int sz = trans[r].receive(bytes, 0, rl, 100);
        byte[] sb = new byte[sz];
        System.arraycopy(bytes, 0, sb, 0, sz);
        String got = new String(sb);
        assertEquals(mess, got);
    }

    /**
     * Test of associate method, of class ThreadedAssociation.
     */
    @Test
    public void testAssociate() throws Exception {
        System.out.println("--> associate");

        DatagramTransport trans[] = mkMockTransports();
        MockAssociationListener listenLeft = new MockAssociationListener();
        MockAssociationListener listenRight = new MockAssociationListener();
        ThreadedAssociation instanceLeft = new ThreadedAssociation(trans[0], listenLeft);
        ThreadedAssociation instanceRight = new ThreadedAssociation(trans[1], listenRight);
        instanceLeft.associate();
        synchronized (listenLeft) {
            listenLeft.wait(1000);
            assertTrue (listenLeft.associated);
            assertTrue (listenRight.associated);
        }
    }

    /**
     * Test of mkStream method, of class ThreadedAssociation.
     */
    @Test
    public void testMkStream() throws Exception {
        System.out.println("--> mkStream");

        DatagramTransport trans[] = mkMockTransports();
        MockAssociationListener listenLeft = new MockAssociationListener();
        MockAssociationListener listenRight = new MockAssociationListener();
        ThreadedAssociation instanceLeft = new ThreadedAssociation(trans[0], listenLeft);
        ThreadedAssociation instanceRight = new ThreadedAssociation(trans[1], listenRight);
        instanceLeft.associate();
        synchronized (listenLeft) {
            listenLeft.wait(1000);
            assertTrue (listenLeft.associated);
            assertTrue (listenRight.associated);
        }
        int id = 10;
        SCTPStream result = instanceLeft.mkStream(id);
        assertTrue (result instanceof BlockingSCTPStream);
    }

    /**
     * Test of sendAndBlock method, of class ThreadedAssociation.
     */
    @Test
    public void testSendAndBlock() throws Exception {
        System.out.println("--> sendAndBlock");
        final StringBuffer rightout = new StringBuffer();
        final SCTPStreamListener rsl = new SCTPStreamListener() {
            @Override
            synchronized public void onMessage(SCTPStream s, String message) {
                Log.debug("onmessage : " + message);
                rightout.append(message);
                this.notify();
            }
        };
        DatagramTransport trans[] = mkMockTransports();
        MockAssociationListener listenLeft = new MockAssociationListener();
        MockAssociationListener listenRight = new MockAssociationListener() {
            @Override
            public void onRawStream(SCTPStream s) {
                super.onRawStream(s);
                s.setBehave(new OrderedStreamBehaviour());
                s.setSCTPStreamListener(rsl);
            }
        };
        ThreadedAssociation instanceLeft = new ThreadedAssociation(trans[0], listenLeft);
        ThreadedAssociation instanceRight = new ThreadedAssociation(trans[1], listenRight);
        instanceLeft.associate();
        synchronized (listenLeft) {
            listenLeft.wait(1000);
            assertTrue (listenLeft.associated);
            assertTrue (listenRight.associated);
        }
        int id = 10;
        SCTPStream result = instanceLeft.mkStream(id);
        assert (result instanceof BlockingSCTPStream);
        String test = "Test message";
        SCTPMessage m = new SCTPMessage(test, result);
        instanceLeft.sendAndBlock(m);
        synchronized (rightout) {
            rightout.wait(1000);
            assertEquals (rightout.toString(),test);
        }
    }

    /**
     * Test of makeMessage method, of class ThreadedAssociation.
     */
    @Test
    public void testMakeMessage_byteArr_BlockingSCTPStream() throws Exception {
        System.out.println("---->makeMessage bytes");

        final ByteBuffer rightout = ByteBuffer.allocate(10000);
        final StringBuffer empty = new StringBuffer();
        final SCTPByteStreamListener rsl = new SCTPByteStreamListener() {
            @Override
            public void onMessage(SCTPStream s, String message) {
                empty.append(message);
                Log.debug("String onmessage : " + message);
                synchronized (rightout) {
                    rightout.notify();
                }
            }

            @Override
            public void onMessage(SCTPStream s, byte[] message) {
                rightout.put(message);
                Log.debug("Byte onmessage : " + message.length);
                synchronized (rightout) {
                    rightout.notify();
                }
            }
        };
        DatagramTransport trans[] = mkMockTransports();
        MockAssociationListener listenLeft = new MockAssociationListener();
        MockAssociationListener listenRight = new MockAssociationListener() {
            @Override
            public void onRawStream(SCTPStream s) {
                super.onRawStream(s);
                s.setBehave(new OrderedStreamBehaviour());
                s.setSCTPStreamListener(rsl);
            }
        };
        ThreadedAssociation instanceLeft = new ThreadedAssociation(trans[0], listenLeft);
        ThreadedAssociation instanceRight = new ThreadedAssociation(trans[1], listenRight);
        instanceLeft.associate();
        synchronized (listenLeft) {
            listenLeft.wait(2000);
            assertTrue (listenLeft.associated);
            assertTrue (listenRight.associated);
        }
        int id = 10;
        SCTPStream s = instanceLeft.mkStream(id);
        assert (s instanceof BlockingSCTPStream);
        String test = "Test message";
        SCTPMessage m = instanceLeft.makeMessage(test.getBytes(), (BlockingSCTPStream) s);
        instanceLeft.sendAndBlock(m);
        synchronized (rightout) {
            rightout.wait(2000);
            int l = rightout.position();
            String res = new String(rightout.array(), 0, l);
            assertEquals (res,test);
            assertEquals (empty.length() , 0);
        }
    }

    /**
     * Test of makeMessage method, of class ThreadedAssociation.
     */
    @Test
    public void testMakeMessage_String_BlockingSCTPStream() throws Exception {
        System.out.println("---->makeMessage string");

        final StringBuffer rightout = new StringBuffer();
        final SCTPStreamListener rsl = new SCTPStreamListener() {
            @Override
            public void onMessage(SCTPStream s, String message) {
                rightout.append(message);
                Log.debug("onmessage : " + message);
                synchronized (rightout) {
                    rightout.notify();
                }
            }
        };
        DatagramTransport trans[] = mkMockTransports();
        MockAssociationListener listenLeft = new MockAssociationListener();
        MockAssociationListener listenRight = new MockAssociationListener() {
            @Override
            public void onRawStream(SCTPStream s) {
                super.onRawStream(s);
                s.setBehave(new OrderedStreamBehaviour());
                s.setSCTPStreamListener(rsl);
            }
        };
        ThreadedAssociation instanceLeft = new ThreadedAssociation(trans[0], listenLeft);
        ThreadedAssociation instanceRight = new ThreadedAssociation(trans[1], listenRight);
        instanceLeft.associate();
        synchronized (listenLeft) {
            listenLeft.wait(2000);
            assert (listenLeft.associated);
            assert (listenRight.associated);
        }
        int id = 10;
        SCTPStream s = instanceLeft.mkStream(id);
        assert (s instanceof BlockingSCTPStream);
        String test = "Test message";
        SCTPMessage m = instanceLeft.makeMessage(test, (BlockingSCTPStream) s);
        instanceLeft.sendAndBlock(m);
        synchronized (rightout) {
            rightout.wait(2000);
            assertEquals (rightout.toString(),test);
        }
    }

    /**
     * Test of makeMessage method, of class ThreadedAssociation.
     */
    @Test
    public void testDCEPStreamSend() throws Exception {
        System.out.println("---->testDCEPStreamSend string");

        final StringBuffer rightout = new StringBuffer();
        final StringBuffer rightLabel = new StringBuffer();
        final SCTPStreamListener rsl = new SCTPStreamListener() {
            @Override
            public void onMessage(SCTPStream s, String message) {
                rightout.append(message);
                Log.debug("DCEP onmessage : " + message);
                synchronized (rightout) {
                    rightout.notify();
                }
            }
        };
        DatagramTransport trans[] = mkMockTransports();
        MockAssociationListener listenLeft = new MockAssociationListener() {
            @Override
            public void onDCEPStream(SCTPStream s, String label, int type) {
            }
        };
        MockAssociationListener listenRight = new MockAssociationListener() {
            @Override
            public void onDCEPStream(SCTPStream s, String label, int type) {
                s.setSCTPStreamListener(rsl);
                rightLabel.append(label);
            }
        };
        ThreadedAssociation instanceLeft = new ThreadedAssociation(trans[0], listenLeft);
        ThreadedAssociation instanceRight = new ThreadedAssociation(trans[1], listenRight);
        instanceLeft.associate();
        synchronized (listenLeft) {
            listenLeft.wait(2000);
            assertTrue (listenLeft.associated);
            assertTrue (listenRight.associated);
        }
        int id = 10;
        String label = "test Stream";
        final SCTPStream s = instanceLeft.mkStream(id, label);
        synchronized (rightLabel) {
            rightLabel.wait(2000);
            assertEquals (rightLabel.toString(),label);
        }
        assert (s instanceof BlockingSCTPStream);
        BlockingSCTPStream bs = (BlockingSCTPStream) s;
        StringBuffer longTestMessage = new StringBuffer();
        for (int i=0;i< 10000;i++){
            longTestMessage.append(" "+i);
        }
        final String testString = longTestMessage.toString();
        System.out.println("-------> String length = "+testString.length());
        Runnable send = new Runnable(){
            @Override
            public void run() {
                try {
                    s.send(testString);
                } catch (Exception ex) {
                    Log.warn(ex.getMessage());
                }
            }
        };
        Thread st = new Thread(send,"sender ");
        st.start();
        
        synchronized (rightout) {
            rightout.wait(10000);
            assertEquals (testString,rightout.toString());
        }

    }
}
