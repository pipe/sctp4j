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
package pe.pi.sctp4j.sctp.behave;

import pe.pi.sctp4j.sctp.behave.UnorderedStreamBehaviour;
import pe.pi.sctp4j.sctp.Association;
import pe.pi.sctp4j.sctp.SCTPMessage;
import pe.pi.sctp4j.sctp.SCTPStream;
import pe.pi.sctp4j.sctp.SCTPStreamListener;
import pe.pi.sctp4j.sctp.messages.DataChunk;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import pe.pi.sctp4j.sctp.dataChannel.DECP.DCOpen;
import pe.pi.sctp4j.sctp.dummy.DummyStream;

/**
 *
 * @author tim
 */
public class UnorderedStreamBehaviourTest {

    private long _tsn = 111;

    public UnorderedStreamBehaviourTest() {
    }

    @BeforeClass
    public static void setUpClass() {
       //Log.setLevel(Log.ALL);
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }
    
    SCTPStream mockStream() {
        Association a = null;
        Integer n = new Integer(10);
        return new DummyStream(a, n) {
            @Override
            public void deliverMessage(SCTPMessage message) {
                message.run();
            }
        };
    }
    /**
     * Test of respond method, of class UnreliableStreamBehaviour.
     */
    /*@org.junit.Test
     public void testRespond() {
     System.out.println("respond");
     SCTPStream a = null;
     UnreliableStreamBehaviour instance = new UnreliableStreamBehaviour();
     Chunk[] expResult = null;
     Chunk[] result = instance.respond(a);
     assertArrayEquals(expResult, result);
     // TODO review the generated test code and remove the default call to fail.
     fail("The test case is a prototype.");
     }*/
    /**
     * Test of deliver method, of class UnreliableStreamBehaviour.
     */
    class CheckingStreamListener implements SCTPStreamListener {

        List<String> _results;

        CheckingStreamListener(List<String> results) {
            _results = results;
        }

        @Override
        public void onMessage(SCTPStream s, String message) {
            assert (_results.remove(message));
        }

        @Override
        public void close(SCTPStream aThis) {
        }

    };

    @org.junit.Test
    public void testDeliverSingle() {
        System.out.println("--> deliver single");
        SCTPStream s = mockStream();
        SortedSet<DataChunk> stash = new TreeSet();
        DataChunk single = new DataChunk();
        final String testString = "Test String";
        single.setData(testString.getBytes());
        single.setPpid(DataChunk.WEBRTCSTRING);
        single.setFlags(DataChunk.SINGLEFLAG);
        single.setTsn(_tsn++);
        single.setsSeqNo(0);
        stash.add(single);
        ArrayList<String> result = new ArrayList<String>();
        result.add(testString);
        SCTPStreamListener l = new CheckingStreamListener(result);
        UnorderedStreamBehaviour instance = new UnorderedStreamBehaviour();
        instance.deliver(s, stash, l);
        int remain = result.size();
        assertEquals(remain, 0);
    }

    @org.junit.Test
    public void testDontDeliverBegin() {
        System.out.println("--> dont deliver Lone Begin");

        dontDeliverOnePart(DataChunk.BEGINFLAG);
    }

    @org.junit.Test
    public void testDontDeliverMiddle() {
        System.out.println("--> dont deliver Lone Middle");

        dontDeliverOnePart(0);
    }

    @org.junit.Test
    public void testDontDeliverEnd() {
        System.out.println("--> dont deliver Lone End");

        dontDeliverOnePart(DataChunk.ENDFLAG);
    }

    void dontDeliverOnePart(int flag) {
        SCTPStream s = mockStream();
        SortedSet<DataChunk> stash = new TreeSet();
        DataChunk single = new DataChunk();
        final String testString = "Test String";
        single.setData(testString.getBytes());
        single.setPpid(DataChunk.WEBRTCSTRING);
        single.setFlags(flag);
        single.setTsn(_tsn++);
        single.setsSeqNo(0);
        stash.add(single);
        ArrayList<String> result = new ArrayList<String>();
        result.add(testString);
        SCTPStreamListener l = new CheckingStreamListener(result);
        UnorderedStreamBehaviour instance = new UnorderedStreamBehaviour();
        instance.deliver(s, stash, l);
        int remain = result.size();
        assertEquals(1, remain);
    }

    @org.junit.Test
    public void testDeliverTwo() {
        System.out.println("--> deliver two");
        SCTPStream s = mockStream();
        SortedSet<DataChunk> stash = new TreeSet();
        String testStrings[] = {"Test String A", "Test String B"};
        ArrayList<String> result = new ArrayList<String>();
        int n = 0;
        for (String ts : testStrings) {
            DataChunk single = new DataChunk();
            single.setTsn(_tsn++);
            single.setsSeqNo(0);
            single.setData(ts.getBytes());
            single.setPpid(DataChunk.WEBRTCSTRING);
            single.setFlags(DataChunk.SINGLEFLAG);
            stash.add(single);
            result.add(ts);
        }
        SCTPStreamListener l = new CheckingStreamListener(result);
        UnorderedStreamBehaviour instance = new UnorderedStreamBehaviour();
        instance.deliver(s, stash, l);

        int remain = result.size();
        assertEquals(remain, 0);
    }

    @org.junit.Test
    public void testDeliverTwoPartMessage() {
        String testStrings[] = {"Test String A, ", "Test String B."};
        System.out.println("--> deliver two part message");
        multiPartMessage(testStrings);
    }

    @org.junit.Test
    public void testDeliverThreePartMessage() {
        String testStrings[] = {"Test String A, ", "Test String B ", "and Test String C"};
        System.out.println("--> deliver three part message");
        multiPartMessage(testStrings);
    }

    @org.junit.Test
    public void testDeliverLongMessage() {
        String testStrings[] = new String[333];
        System.out.println("--> deliver many part message");
        for (int i = 0; i < testStrings.length; i++) {
            testStrings[i] = " Test string " + i;
        }
        multiPartMessage(testStrings);
    }

    void multiPartMessage(String[] testStrings) {
        SCTPStream s = mockStream();
        SortedSet<DataChunk> stash = new TreeSet();
        ArrayList<String> result = new ArrayList<String>();
        int n = 0;
        StringBuffer bs = new StringBuffer();
        for (String ts : testStrings) {
            DataChunk single = new DataChunk();
            single.setTsn(_tsn++);
            single.setsSeqNo(0);
            single.setData(ts.getBytes());
            single.setPpid(DataChunk.WEBRTCSTRING);
            if (n == 0) {
                single.setFlags(DataChunk.BEGINFLAG);
            } else if (n == testStrings.length - 1) {
                single.setFlags(DataChunk.ENDFLAG);
            }
            n++;
            bs.append(ts);
            stash.add(single);
        }
        result.add(bs.toString());
        SCTPStreamListener l = new CheckingStreamListener(result);
        UnorderedStreamBehaviour instance = new UnorderedStreamBehaviour();
        instance.deliver(s, stash, l);

        int remain = result.size();
        assertEquals(remain, 0);
    }

    void oneMissingPartMessages(String[] testStrings, String es, int ec) {
        SCTPStream s = mockStream();
        SortedSet<DataChunk> stash = new TreeSet();
        ArrayList<String> result = new ArrayList<String>();
        int n = 0;
        int expectedToRemain = 0;
        for (String ts : testStrings) {
            for (int i = 0; i < ts.length(); i++) {
                DataChunk single = new DataChunk();
                single.setsSeqNo(n);
                single.setTsn(_tsn++);
                String letter = ts.substring(i, i + 1);
                single.setData(letter.getBytes());
                single.setPpid(DataChunk.WEBRTCSTRING);
                if (i == 0) {
                    single.setFlags(DataChunk.BEGINFLAG);
                } else if (i == ts.length() - 1) {
                    single.setFlags(DataChunk.ENDFLAG);
                }
                if ((ec != i) || !ts.equals(es)) {
                    stash.add(single);
                }
            }
            if (ts.equals(es)) {
                expectedToRemain += 1;
            }
            result.add(ts);
            n++;

        }

        SCTPStreamListener l = new CheckingStreamListener(result);
        UnorderedStreamBehaviour instance = new UnorderedStreamBehaviour();

        instance.deliver(s, stash, l);

        int remain = result.size();
        //System.out.println("expected:" + expectedToRemain + " remain:" + remain);

        assertEquals(remain, expectedToRemain);
    }

    @org.junit.Test
    public void testDeliverNoMissingPartMessage() {
        System.out.println("--> deliver no missing part message");
        String testStrings[] = {"Test String A, ", "Test String B ", "and Test String C"};

        oneMissingPartMessages(testStrings, "", -1);
    }
/*
    // todo - fix this - 
    @org.junit.Test
    public void testDeliverOneMissingPartMessage() {
        System.out.println("--> deliver one missing part message");
        String testStrings[] = {"Test String A, ", "Test String B ", "and Test String C"};

        for (String es : testStrings) {
            for (int ec = 0; ec < es.length(); ec++) {
                oneMissingPartMessages(testStrings, es, ec);
            }
        }
    }
*/
    @org.junit.Test
    public void testDeliverUnorderedPackets() {
        System.out.println("--> deliver messages with random packet arrival");
        for (int i = 0; i < 100; i++) {
            deliverUnorderedPackets(i);
        }
    }

    public void deliverUnorderedPackets(int seed) {
        Random rand = new Random(seed); // deliberately not crypto random so test is repeatable 
        // System.out.println("seed = "+seed);
        String testStrings[] = {"Test String A, ", "Test String B ", "and Test String C"};
        SCTPStream s = mockStream();
        ArrayList<String> result = new ArrayList<String>();
        int n = 0;
        ArrayList<DataChunk> all = new ArrayList<DataChunk>();
        for (String ts : testStrings) {
            for (int i = 0; i < ts.length(); i++) {
                DataChunk single = new DataChunk();
                single.setsSeqNo(n);
                single.setTsn(_tsn++);
                String letter = ts.substring(i, i + 1);
                single.setData(letter.getBytes());
                single.setPpid(DataChunk.WEBRTCSTRING);
                if (i == 0) {
                    single.setFlags(DataChunk.BEGINFLAG);
                } else if (i == ts.length() - 1) {
                    single.setFlags(DataChunk.ENDFLAG);
                }
                all.add(single);
            }
            result.add(ts);
            n++;

        }

        SCTPStreamListener l = new CheckingStreamListener(result);
        UnorderedStreamBehaviour instance = new UnorderedStreamBehaviour();
        SortedSet<DataChunk> stash = new TreeSet();
        while (all.size() > 0) {
            int v = rand.nextInt(all.size());
            DataChunk c = all.remove(v);
            stash.add(c);
            instance.deliver(s, stash, l);
        }

        int remain = result.size();
        assertEquals(remain, 0);
    }

}
