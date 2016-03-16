/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.sctp.behave;

import com.ipseorama.sctp.Association;
import com.ipseorama.sctp.SCTPStream;
import com.ipseorama.sctp.SCTPStreamListener;
import com.ipseorama.sctp.messages.DataChunk;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

/**
 *
 * @author tim
 */
public class OrderedStreamBehaviourTest {

    public OrderedStreamBehaviourTest() {
    }
    static Comparator<DataChunk> comp;

    @BeforeClass
    public static void setUpClass() {
       // Log.setLevel(Log.VERB);
        comp = new Comparator<DataChunk>() {
            @Override
            public int compare(DataChunk o1, DataChunk o2) {
                return o1.getSSeqNo() - o2.getSSeqNo();
            }
        };
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
            //System.out.println("delivered '"+message+"'");
            assert (_results.remove(message));
        }

    };

    SCTPStream mockStream(){
        Association a = null;
        Integer n = new Integer(10);
        return new SCTPStream(a,n) {
            @Override
            public void send(String message) throws Exception {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }
    
    @org.junit.Test
    public void testDeliverSingle() {
        System.out.println("--> deliver single");
        SCTPStream s = mockStream();
        SortedSet<DataChunk> stash = new TreeSet(comp);
        DataChunk single = new DataChunk();
        final String testString = "Test String";
        single.setData(testString.getBytes());
        single.setPpid(DataChunk.WEBRTCSTRING);
        single.setFlags(DataChunk.SINGLEFLAG);
        stash.add(single);
        ArrayList<String> result = new ArrayList<String>();
        result.add(testString);
        SCTPStreamListener l = new CheckingStreamListener(result);
        OrderedStreamBehaviour instance = new OrderedStreamBehaviour();
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
        SortedSet<DataChunk> stash = new TreeSet(comp);
        DataChunk single = new DataChunk();
        final String testString = "Test String";
        single.setData(testString.getBytes());
        single.setPpid(DataChunk.WEBRTCSTRING);
        single.setFlags(flag);
        stash.add(single);
        ArrayList<String> result = new ArrayList<String>();
        result.add(testString);
        SCTPStreamListener l = new CheckingStreamListener(result);
        OrderedStreamBehaviour instance = new OrderedStreamBehaviour();
        instance.deliver(s, stash, l);
        int remain = result.size();
        assertEquals(1, remain);
    }

    @org.junit.Test
    public void testDeliverTwo() {
        System.out.println("--> deliver two");
        SCTPStream s = mockStream();
        SortedSet<DataChunk> stash = new TreeSet(comp);
        String testStrings[] = {"Test String A", "Test String B"};
        ArrayList<String> result = new ArrayList<String>();
        int n = 0;
        for (String ts : testStrings) {
            DataChunk single = new DataChunk();
            single.setsSeqNo(n++);
            single.setData(ts.getBytes());
            single.setPpid(DataChunk.WEBRTCSTRING);
            single.setFlags(DataChunk.SINGLEFLAG);
            stash.add(single);
            result.add(ts);
        }
        SCTPStreamListener l = new CheckingStreamListener(result);
        OrderedStreamBehaviour instance = new OrderedStreamBehaviour();
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
        System.out.println("--> deliver many part long message");
        for (int i = 0; i < testStrings.length; i++) {
            testStrings[i] = " Test string " + i;
        }
        multiPartMessage(testStrings);
    }

    @org.junit.Test
    public void testDeliverVeryLongMessage() {
        String testStrings[] = new String[10000];
        System.out.println("--> deliver many part very long message");
        for (int i = 0; i < testStrings.length; i++) {
            testStrings[i] = "" + i;
        }
        multiPartMessage(testStrings);
    }
    
    void multiPartMessage(String[] testStrings) {
        SCTPStream s = mockStream();
        SortedSet<DataChunk> stash = new TreeSet(comp);
        ArrayList<String> result = new ArrayList<String>();
        int n = 0;
        StringBuffer bs = new StringBuffer();
        for (String ts : testStrings) {
            DataChunk single = new DataChunk();
            single.setsSeqNo(n);
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
        OrderedStreamBehaviour instance = new OrderedStreamBehaviour();
        instance.deliver(s, stash, l);

        int remain = result.size();
        assertEquals(remain, 0);
    }

    void oneMissingPartMessages(String[] testStrings, String es, int ec) {
        SCTPStream s = mockStream();
        SortedSet<DataChunk> stash = new TreeSet(comp);
        ArrayList<String> result = new ArrayList<String>();
        int n = 0;
        int expectedToRemain = 0;
        boolean skip = false;
        for (String ts : testStrings) {
            for (int i = 0; i < ts.length(); i++) {
                DataChunk single = new DataChunk();
                single.setsSeqNo(n);
                String letter = ts.substring(i, i + 1);
                single.setData(letter.getBytes());
                single.setPpid(DataChunk.WEBRTCSTRING);
                if (i == 0) {
                    single.setFlags(DataChunk.BEGINFLAG);
                } else if (i == ts.length() - 1) {
                    single.setFlags(DataChunk.ENDFLAG);
                }
                n++;
                if ((ec != i) || !ts.equals(es)) {
                    stash.add(single);
                }
            }
            if (ts.equals(es)) {
                skip = true;
            }
            if (skip){
                expectedToRemain ++;
            }
            result.add(ts);
        }

        SCTPStreamListener l = new CheckingStreamListener(result);
        OrderedStreamBehaviour instance = new OrderedStreamBehaviour();

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
                String letter = ts.substring(i, i + 1);
                single.setData(letter.getBytes());
                single.setPpid(DataChunk.WEBRTCSTRING);
                if (i == 0) {
                    single.setFlags(DataChunk.BEGINFLAG);
                } else if (i == ts.length() - 1) {
                    single.setFlags(DataChunk.ENDFLAG);
                }
                all.add(single);
                n++;
            }
            result.add(ts);
        }

        SCTPStreamListener l = new CheckingStreamListener(result);
        OrderedStreamBehaviour instance = new OrderedStreamBehaviour();
        SortedSet<DataChunk> stash = new TreeSet(comp);
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
