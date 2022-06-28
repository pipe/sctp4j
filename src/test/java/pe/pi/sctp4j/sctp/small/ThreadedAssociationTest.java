/*
 * Copyright 2017 pi.pe gmbh .
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package pe.pi.sctp4j.sctp.small;

import pe.pi.sctp4j.sctp.Association;
import pe.pi.sctp4j.sctp.AssociationListener;
import pe.pi.sctp4j.sctp.SCTPByteStreamListener;
import pe.pi.sctp4j.sctp.SCTPStream;
import pe.pi.sctp4j.sctp.SCTPStreamListener;
import com.phono.srtplight.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.bouncycastle.tls.DatagramTransport;
import org.bouncycastle.util.Arrays;
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
        for (DatagramTransport t : __transList) {
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

        Boolean associated = false;
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
            Log.debug("dcep stream");
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
                        if (pkt.length > len) {
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
            if (Log.getLevel() >= Log.DEBUG) {
                Exception where = new Exception("stack trace");
                where.printStackTrace();
            }
            _isShutdown = true;

        }

    }

    abstract class ASCTPStreamListener implements SCTPStreamListener {

        @Override
        public void close(SCTPStream aThis) {
            Log.debug("closed");
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
            assertTrue(listenLeft.associated);
            assertTrue(listenRight.associated);
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
            assertTrue(listenLeft.associated);
            assertTrue(listenRight.associated);
        }
        int id = 10;
        SCTPStream result = instanceLeft.mkStream(id);
        assertTrue(result instanceof BlockingSCTPStream);
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

            @Override
            public void close(SCTPStream aThis) {
                Log.debug("closed");
            }
        };
        DatagramTransport trans[] = mkMockTransports();
        MockAssociationListener listenLeft = new MockAssociationListener();
        MockAssociationListener listenRight = new MockAssociationListener() {
            @Override
            public void onRawStream(SCTPStream s) {
                super.onRawStream(s);
                s.setSCTPStreamListener(rsl);
            }
        };
        ThreadedAssociation instanceLeft = new ThreadedAssociation(trans[0], listenLeft);
        ThreadedAssociation instanceRight = new ThreadedAssociation(trans[1], listenRight);
        instanceLeft.associate();
        synchronized (listenLeft) {
            listenLeft.wait(2000);
            assertTrue(listenLeft.associated);
            assertTrue(listenRight.associated);
        }
        //Log.setLevel(Log.VERB);
        SCTPStream s = instanceLeft.mkStream("test Stream", rsl);
        assert (s instanceof BlockingSCTPStream);
        Random r = new Random(); // yep -intentionally not the secure variant.
        byte[] testBytes = new byte[1337];
        r.nextBytes(testBytes);
        s.send(testBytes);
        synchronized (rightout) {
            rightout.wait(2000);
            rightout.flip();
            byte[] rec = new byte[rightout.limit()];
            rightout.get(rec);
            assertTrue(Arrays.areEqual(testBytes, rec));
            assertEquals(empty.length(), 0);
        }
    }

    /**
     * Test of makeMessage method, of class ThreadedAssociation.
     */
    @Test
    public void testMakeMessage_String_BlockingSCTPStream() throws Exception {
        System.out.println("---->makeMessage string");
        //Log.setLevel(Log.DEBUG);
        final StringBuffer rightout = new StringBuffer();
        final SCTPStreamListener rsl = new ASCTPStreamListener() {
            @Override
            public void onMessage(SCTPStream s, String message) {
                rightout.append(message);
                Log.info("onmessage : " + message);
                synchronized (rightout) {
                    rightout.notify();
                }
            }
        };
        DatagramTransport trans[] = mkMockTransports();
        MockAssociationListener listenLeft = new MockAssociationListener();
        MockAssociationListener listenRight = new MockAssociationListener() {
            @Override
            public void onDCEPStream(SCTPStream s, String label, int type) {
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
        SCTPStream s = instanceLeft.mkStream("TestStream");
        assert (s instanceof BlockingSCTPStream);
        String test = "Test message";
        new Thread(() -> {
            try {
                s.send(test);
            } catch (Exception ex) {
                assert (false);
            }
        }).start();
        synchronized (rightout) {
            rightout.wait(2000);
            assertEquals(test, rightout.toString());
        }
        //Log.setLevel(Log.INFO);
    }

    /**
     * Test of makeMessage method, of class ThreadedAssociation.
     */
    @Test
    public void testDCEPStreamSend() throws Exception {
        //Log.setLevel(Log.VERB);
        System.out.println("---->testDCEPStreamSend string");

        final StringBuffer rightout = new StringBuffer();
        final StringBuffer rightLabel = new StringBuffer();
        final SCTPStreamListener rsl = new ASCTPStreamListener() {
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
            assertTrue(listenLeft.associated);
            assertTrue(listenRight.associated);
        }
        String label = "test Stream";
        final SCTPStream s = instanceLeft.mkStream(label);
        synchronized (rightLabel) {
            rightLabel.wait(2000);
            assertEquals(rightLabel.toString(), label);
        }
        assert (s instanceof BlockingSCTPStream);
        BlockingSCTPStream bs = (BlockingSCTPStream) s;
        StringBuffer longTestMessage = new StringBuffer();
        for (int i = 0; i < 1000; i++) {
            longTestMessage.append("This is a test message number: " + i);
        }
        //Log.setLevel(Log.VERB);
        final String testString = longTestMessage.toString();
        System.out.println("-------> String length = " + testString.length());
        new Thread(() -> {
            try {
                s.send(testString);
            } catch (Exception ex) {
                Log.warn(ex.getMessage());
                assert (false);
            }
        }, "sender ").start();

        synchronized (rightout) {
            rightout.wait(10000);
            assertEquals(testString.length(), rightout.toString().length());
            assertEquals(testString.hashCode(), rightout.toString().hashCode());
        }
        //Log.setLevel(Log.INFO);

    }

    @Test
    public void testDCEPStreamSendMultiple() throws Exception {
        //Log.setLevel(Log.INFO);
        System.out.println("---->testDCEPStreamSend strings");

        final ArrayList<String> rightout = new ArrayList();
        final StringBuffer rightLabel = new StringBuffer();
        final SCTPStreamListener rsl = new SCTPByteStreamListener() {
            @Override
            public void onMessage(SCTPStream s, String message) {
                rightout.add(message);
                Log.info("DCEP onmessage(String) : " + rightout.size());
            }

            @Override
            public void onMessage(SCTPStream s, byte[] message) {
                Log.info("DCEP onmessage(Byte[])  " + message.length);
                if ((message.length == 1) && (message[0] == 0)) {
                    synchronized (rightout) {
                        rightout.notify();
                    }
                }
            }

            @Override
            public void close(SCTPStream aThis) {
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
            assertTrue(listenLeft.associated);
            assertTrue(listenRight.associated);
        }
        String label = "test Stream";
        final SCTPStream s = instanceLeft.mkStream(label);
        synchronized (rightLabel) {
            rightLabel.wait(2000);
            assertEquals(rightLabel.toString(), label);
        }
        assert (s instanceof BlockingSCTPStream);
        BlockingSCTPStream bs = (BlockingSCTPStream) s;
        StringBuffer longTestMessage = new StringBuffer();
        for (int i = 0; i < 500; i++) {
            longTestMessage.append("This is a test message number: " + i);
        }
        //Log.setLevel(Log.DEBUG);
        final String testString = longTestMessage.toString();
        System.out.println("-------> String length = " + testString.length());
        int[] sentHash = new int[5];
        new Thread(() -> {
            try {
                for (int i = 0; i < sentHash.length; i++) {
                    String toSend = "i=" + i + testString;
                    sentHash[i] = toSend.hashCode();
                    s.send(toSend);
                    System.out.println("-------> String count " + i);
                }
                s.send(new byte[1]);

            } catch (Exception ex) {
                Log.warn(ex.getMessage());
                assert (false);
            }
        }, "sender ").start();

        synchronized (rightout) {
            rightout.wait(100000);
            assertEquals(sentHash.length, rightout.size());
            for (int i = 0; i < sentHash.length; i++) {
                assertEquals(sentHash[i], rightout.get(i).hashCode());
            }
        }
        //Log.setLevel(Log.NONE);

    }

    @Test
    public void testDCEPStreamMultiple() throws Exception {
        //Log.setLevel(Log.INFO);
        String label = "testStream";
        String labeln = "";
        ArrayList<String> leftList = new ArrayList();
        ArrayList<String> rightList = new ArrayList();

        for (int i = 0; i < 20; i++) {
            labeln = label + i;
            leftList.add(labeln);
        }

        System.out.println("---->testDCEPStreamMultiple");

        DatagramTransport trans[] = mkMockTransports();
        final SCTPStreamListener rsl = new SCTPByteStreamListener() {
            @Override
            public void onMessage(SCTPStream s, String message) {
            }

            @Override
            public void onMessage(SCTPStream s, byte[] message) {
                String m = new String(message);
                Log.info("DCEP onmessage(Byte[])  " + m);

                assert (m.contentEquals(s.getLabel()));
                rightList.remove(m);
                if (rightList.isEmpty()) {
                    synchronized (rightList) {
                        rightList.notify();
                    }
                }
            }

            @Override
            public void close(SCTPStream aThis) {
            }
        };

        MockAssociationListener listenLeft = new MockAssociationListener() {
            @Override
            public void onDCEPStream(SCTPStream s, String label, int type) {
            }
        };
        MockAssociationListener listenRight = new MockAssociationListener() {
            @Override
            public void onDCEPStream(SCTPStream s, String label, int type) {
                s.setSCTPStreamListener(rsl);
            }
        };
        ThreadedAssociation instanceLeft = new ThreadedAssociation(trans[0], listenLeft);
        ThreadedAssociation instanceRight = new ThreadedAssociation(trans[1], listenRight);

        instanceLeft.associate();
        synchronized (listenLeft) {
            listenLeft.wait(2000);
            assertTrue(listenLeft.associated);
            assertTrue(listenRight.associated);
        }

        new Thread(() -> {

            leftList.forEach((l) -> {
                try {
                    SCTPStream s = instanceLeft.mkStream(l);
                    System.out.println("-------> Stream " + l);
                    if (s != null) {
                        s.send(l.getBytes());
                    }
                } catch (Exception ex) {
                    Log.warn(ex.getMessage());
                    assert (false);
                }
            });

        }, "sender ").start();

        synchronized (rightList) {
            rightList.wait(10000);
            assertEquals(0, rightList.size());
        }
        //Log.setLevel(Log.NONE);

    }

    @Test
    public void testEarlyMessage_String_BlockingSCTPStream() throws Exception {
        System.out.println("---->earlyMessage string");
        //Log.setLevel(Log.VERB);
        final StringBuffer rightout = new StringBuffer();
        final SCTPStreamListener rsl = new ASCTPStreamListener() {
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
        final SCTPStream[] rsa = new SCTPStream[1];
        MockAssociationListener listenRight = new MockAssociationListener() {
            @Override
            public void onDCEPStream(SCTPStream s, String label, int type) {
                rsa[0] = s;
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
        SCTPStream s = instanceLeft.mkStream("TestStream");
        assert (s instanceof BlockingSCTPStream);
        synchronized (rsa) {
            rsa.wait(200);
            assert (rsa[0] != null);
            assert (rsa[0] instanceof BlockingSCTPStream);
        }
        String test = "Test message";
        new Thread(() -> {
            try {
                s.send(test);
                Thread.sleep(500);
                rsa[0].setSCTPStreamListener(rsl);
            } catch (Exception ex) {
                assert (false);
            }
        }).start();
        synchronized (rightout) {
            rightout.wait(2000);
            assertEquals(test, rightout.toString());
        }
        //Log.setLevel(Log.WARN);
    }
}
