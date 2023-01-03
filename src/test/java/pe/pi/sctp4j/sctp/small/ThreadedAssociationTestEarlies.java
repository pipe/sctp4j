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
import pe.pi.sctp4j.sctp.dataChannel.DECP.DCOpen;
import pe.pi.sctp4j.sctp.messages.DataChunk;

/**
 *
 * @author tim
 */
public class ThreadedAssociationTestEarlies {

    private static Vector<DatagramTransport> __transList;

    public ThreadedAssociationTestEarlies() {

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
    public void testEarlyMessage_String_BlockingSCTPStream() throws Exception {
        System.out.println("---->earlyMessage string");
        Log.setLevel(Log.WARN);
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

        synchronized (rsa) {
            rsa.wait(200);
            assert (rsa[0] != null);
            assert (rsa[0] instanceof BlockingSCTPStream);
        }
        assert (s instanceof BlockingSCTPStream);
        String test = "Test message";
        s.send(test);
        new Thread(() -> {
            try {
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
        Log.setLevel(Log.WARN);
    }

    @Test
    public void testVeryEarlyMessage_String_BlockingSCTPStream() throws Exception {
        System.out.println("---->veryEarlyMessage string");
        Log.setLevel(Log.DEBUG);
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
        // SCTPStream s = instanceLeft.mkStream("TestStream");
        DataChunk earlies[] = new DataChunk[2];
        DCOpen dcop = new DCOpen("TestStream");
        DataChunk dco = new DataChunk();
        dco.setData(dcop.getBytes());
        dco.setPpid(DataChunk.WEBRTCCONTROL);
        dco.setFlags(DataChunk.SINGLEFLAG);
        dco.setsSeqNo(0);

        DataChunk single = new DataChunk();
        final String test = "Test String";
        single.setData(test.getBytes());
        single.setPpid(DataChunk.WEBRTCSTRING);
        single.setFlags(DataChunk.SINGLEFLAG);
        single.setsSeqNo(1);
        earlies[0] = dco;
        earlies[1] = single;
        instanceLeft.enqueue(earlies);
        synchronized (rsa) {
            rsa.wait(200);
            assert (rsa[0] != null);
            assert (rsa[0] instanceof BlockingSCTPStream);
        }
        new Thread(() -> {
            try {
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
        Log.setLevel(Log.WARN);
    }
}
