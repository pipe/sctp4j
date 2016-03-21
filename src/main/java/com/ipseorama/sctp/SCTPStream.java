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
package com.ipseorama.sctp;

import com.ipseorama.sctp.behave.OrderedStreamBehaviour;
import com.ipseorama.sctp.behave.SCTPStreamBehaviour;
import com.ipseorama.sctp.messages.Chunk;
import com.ipseorama.sctp.messages.DataChunk;
import com.phono.srtplight.Log;
import java.util.TreeSet;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public abstract class SCTPStream {
    /* unfortunately a webRTC SCTP stream can change it's reliability rules etc post creation
     so we can't encapsulate the streams into multiple implementations of the same interface/abstract
     So what we do is put the bulk of the stream code here, then delegate the variant rules off to the
     behave class - which has to be stateless since it can be swapped out - it is ugly 
     - and I wonder if closures would do it better.
     */

    private SCTPStreamBehaviour _behave;
    Association _ass;
    private Integer _sno;
    private String _label;
    private TreeSet<DataChunk> _stash;
    private SCTPStreamListener _sl;
    private int _nextMessageSeqIn;
    private int _nextMessageSeqOut;

    public SCTPStream(Association a, Integer id) {
        _ass = a;
        _sno = id;
        _stash = new TreeSet<>(); // sort bt tsn
        _behave = new OrderedStreamBehaviour(); // default 'till we know different
    }

    public void setLabel(String l) {
        _label = l;
    }

    public Chunk[] append(DataChunk dc) {
        Log.debug("adding data to stash on stream " + _label + "(" + dc + ")");
        _stash.add(dc);
        return _behave.respond(this);
    }

    /**
     * note that behaviours must be stateless - since they can be swapped out
     * when we finally get the 'open'
     *
     * @param behave
     */
    public void setBehave(SCTPStreamBehaviour behave) {
        _behave = behave;
    }

    // seqno management.
    /**
     * annotate the outgoing chunk with stuff this stream knows.
     *
     * @param chunk
     */
    public void outbound(DataChunk chunk) {
        chunk.setStreamId(_sno.intValue());
        // roll seqno here.... hopefully....
    }

    void inbound(DataChunk dc) {
        if (_behave != null) {
            _behave.deliver(this, _stash, _sl);
        } else {
            Log.warn("No behaviour set");
        }
    }

    public String getLabel() {
        return _label;
    }

    int stashCap() {
        int ret = 0;
        for (DataChunk d : _stash) {
            ret += d.getData().length;
        }
        return ret;
    }

    public void setSCTPStreamListener(SCTPStreamListener sl) {
        _sl = sl;
        Log.debug("action a delayed delivery now we have a listener.");
        //todo think about what reliablility looks like here.
        _behave.deliver(this, _stash, _sl);
    }

    abstract public void send(String message) throws Exception;

    public Association getAssociation() {
        return _ass;
    }

    public void close() {
        Log.debug("close() Not supported yet.");
    }

    public void setNextMessageSeqIn(int expectedSeq) {
        _nextMessageSeqIn = expectedSeq;
    }

    public int getNextMessageSeqIn() {
        return _nextMessageSeqIn;
    }

    public void setNextMessageSeqOut(int expectedSeq) {
        _nextMessageSeqOut = expectedSeq;
    }

    public int getNextMessageSeqOut() {
        return _nextMessageSeqOut;
    }

    abstract public void deliverMessage(SCTPMessage message); 
}
