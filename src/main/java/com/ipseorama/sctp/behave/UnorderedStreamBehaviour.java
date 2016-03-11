/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.sctp.behave;

import com.ipseorama.sctp.SCTPMessage;
import com.ipseorama.sctp.SCTPStream;
import com.ipseorama.sctp.SCTPStreamListener;
import com.ipseorama.sctp.messages.Chunk;
import com.ipseorama.sctp.messages.DataChunk;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author tim
 */
public class UnorderedStreamBehaviour implements SCTPStreamBehaviour {

    @Override
    public Chunk[] respond(SCTPStream a) {
        return null; // nothing to say.
    }

    @Override
    public void deliver(SCTPStream s, SortedSet<DataChunk> stash, SCTPStreamListener l) {
        //stash is the list of all DataChunks that have not yet been turned into whole messages
        //we assume that it is sorted by stream sequence number.
        ArrayList<DataChunk> delivered = new ArrayList<DataChunk>();
        SortedSet<DataChunk> message = null;
        int expectedSeq = 0;

        for (DataChunk dc : stash) {
            int flags = dc.getFlags() & DataChunk.SINGLEFLAG; // mask to the bits we want
            int seq = dc.getSSeqNo();
            switch (flags) {
                case DataChunk.SINGLEFLAG:
                    // singles are easy - just dispatch.
                    delivered.add(dc);
                    SCTPMessage single = new SCTPMessage(dc.getData(), s);
                    l.onMessage(s, new String(single.getData()));//todo deliver bytes when appropriate
                    break;
                case DataChunk.BEGINFLAG:
                    message = new TreeSet(stash.comparator());
                    message.add(dc);
                    expectedSeq = seq + 1;
                    break;
                case 0: // middle 
                    if ((message != null) && (expectedSeq == seq)) {
                        message.add(dc);
                        expectedSeq = seq + 1;
                    } else {
                        message = null;
                    }
                    break;
                case DataChunk.ENDFLAG:
                    if ((message != null) && (expectedSeq == seq)) {
                        message.add(dc);
                        SCTPMessage deliverable = new SCTPMessage(s, message);
                        l.onMessage(s, new String(deliverable.getData()));//todo deliver bytes when appropriate
                        delivered.addAll(message);
                        message = null;
                    } else {
                        message = null;
                    }
                    break;
                default:
                    throw new IllegalStateException("Impossible value in stream logic");
            }
        }
        stash.removeAll(delivered);
    }

}
