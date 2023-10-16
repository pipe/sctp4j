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
package pe.pi.sctp4j.sctp;

import pe.pi.sctp4j.sctp.behave.OrderedStreamBehaviour;
import pe.pi.sctp4j.sctp.behave.SCTPStreamBehaviour;
import pe.pi.sctp4j.sctp.messages.Chunk;
import pe.pi.sctp4j.sctp.messages.DataChunk;
import com.phono.srtplight.Log;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import pe.pi.sctp4j.sctp.dataChannel.DECP.DCOpen;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public abstract class SCTPStream  {

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
    private ConcurrentSkipListSet<DataChunk> _stash;
    private SCTPStreamListener _sl;
    private int _nextMessageSeqIn;
    private int _nextMessageSeqOut;
    protected LinkedBlockingQueue<SCTPMessage> _earlyQueue;
    private boolean closing;
    private State state = State.OPEN;

    public boolean InboundIsOpen() {
        return ((state == State.OPEN) || (state == State.INBOUNDONLY));
    }

    public boolean OutboundIsOpen() {
        return ((state == State.OPEN) || (state == State.OUTBOUNDONLY));
    }

    public Chunk immediateClose() {
        Chunk ret = null;
        try {
            ret = _ass.addToCloseList(this);
        } catch (Exception ex) {
            Log.error("Can't make immediate close for " + this._sno + " because " + ex.getMessage());
        }
        return ret;
    }

    abstract public void delivered(DataChunk d);

    public SCTPStreamListener getSCTPStreamListener() {
        return _sl;
    }

    public String toString() {
        return this.getClass().getSimpleName()
                + "[" + this._sno + "]"
                + "=" + this._label
                + "|" + _behave.getClass().getSimpleName() + "|"
                + "->"
                + ((_sl != null) ? _sl.getClass().getSimpleName() : "null");
    }

    public String getBehave(){
        return  (_behave==null)?"unknown":_behave.getClass().getSimpleName();
    }
    /*
    void send(SCTPMessage mess)  {
        try {
            _ass.sendAndBlock(mess);
        } catch (Exception ex) {
            Log.warn("Can't send SCTPmessage because "+ex.getMessage());
        }
    }
     */
    synchronized void setAsNextMessage(SCTPMessage m) {
        int mseq = getNextMessageSeqOut();
        setNextMessageSeqOut(mseq + 1);
        m.setSeq(mseq);
    }

    public void openAck(DCOpen dcep) throws Exception {
        DCOpen ack = DCOpen.mkAck();
        Log.debug("made a dcep ack for "+_label);
        send(ack);
    }

    protected void alOnDCEPStream(SCTPStream _stream, String label, int _pPid) throws Exception {
        _ass.alOnDCEPStream(_stream, label, _pPid);
    }

    void earlyMessageEnqueue(SCTPMessage early) {
        Log.debug("enqueue an early message seq "+early.getSeq()+" on "+this.toString());
        _earlyQueue.add(early);
    }

    enum State {
        CLOSED, INBOUNDONLY, OUTBOUNDONLY, OPEN
    }

    public SCTPStream(Association a, Integer id) {
        _ass = a;
        _sno = id;
        _stash = new ConcurrentSkipListSet(); // sort bt tsn
        _behave = new OrderedStreamBehaviour(); // default 'till we know different
        _earlyQueue = new LinkedBlockingQueue(100);
    }

    public void setLabel(String l) {
        _label = l;
    }

    public Integer getNum() {
        return new Integer(_sno);
    }

    public Chunk[] append(DataChunk dc) {
        Log.debug("adding data to stash on stream " + ((_label == null) ? "*unnamed*" : _label) + "(" + dc + ")");
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
        Log.debug("adding listener for "+this._label+" of "+sl.getClass().getName());
        if (_earlyQueue != null) {
            Log.debug("delivering early " + _earlyQueue.size() + " messages to "+sl.getClass().getName());
            SCTPMessage e = null;
            while (null != (e = _earlyQueue.poll())) {
                e.deliver(_sl);
            }
        } else {
            Log.debug("no early queue for "+_label);
        }
    }

    abstract public void send(String message) throws Exception;

    abstract public void send(byte[] message) throws Exception;

    abstract public void send(DCOpen message) throws Exception;

    public Association getAssociation() {
        return _ass;
    }

    public void close() throws Exception {
        Log.debug("closing stream " + this._label + " " + this._sno);
        _ass.closeStream(this);
    }

    public void setNextMessageSeqIn(int expectedSeq) {
        _nextMessageSeqIn = (expectedSeq == 1 + Character.MAX_VALUE) ? 0 : expectedSeq;
    }

    public int getNextMessageSeqIn() {
        return _nextMessageSeqIn;
    }

    public void setNextMessageSeqOut(int expectedSeq) {
        _nextMessageSeqOut = (expectedSeq == 1 + Character.MAX_VALUE) ? 0 : expectedSeq;
    }

    public int getNextMessageSeqOut() {
        return _nextMessageSeqOut;
    }

    abstract public void deliverMessage(SCTPMessage message);

    void setDeferred(boolean b) {
        boolean deferred = true;
    }

    void reset() {
        Log.debug("Resetting stream " + this._sno);
        if (this._sl != null) {
            _sl.close(this);
        }
    }

    void setClosing(boolean b) {
        closing = b;
    }

    boolean isClosing() {
        return closing;
    }

    void setOutboundClosed() {
        switch (state) {
            case OPEN:
                state = State.INBOUNDONLY;
                break;
            case OUTBOUNDONLY:
                state = State.CLOSED;
                break;
            case CLOSED:
            case INBOUNDONLY:
                break;
        }
        Log.debug("Stream State for " + _sno + " is now " + state);
    }

    void setInboundClosed() {
        switch (state) {
            case OPEN:
                state = State.OUTBOUNDONLY;
                break;
            case INBOUNDONLY:
                state = State.CLOSED;
                break;
            case CLOSED:
            case OUTBOUNDONLY:
                break;
        }
        Log.debug("Stream State for " + _sno + " is now " + state);
    }

    State getState() {
        Log.debug("Stream State for " + _sno + " is currently " + state);
        return state;
    }

    public boolean idle() {
        return true;
    }
}
