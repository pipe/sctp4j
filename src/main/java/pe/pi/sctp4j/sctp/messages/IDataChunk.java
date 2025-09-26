/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package pe.pi.sctp4j.sctp.messages;

import com.phono.srtplight.Log;
import java.nio.ByteBuffer;
import pe.pi.sctp4j.sctp.Association;
import static pe.pi.sctp4j.sctp.messages.Chunk.getUnsignedInt;

/**
 *
 * @author thp
 */
public class IDataChunk extends DataChunk {

    private int _mid;
    private int _fsn;
    private boolean ordered;

    public IDataChunk(byte type, byte flags, int length, ByteBuffer pkt) {
        super(type, flags, length, pkt);
    }

    /*
    The following Figure 3 shows the new I-DATA chunk allowing user
   message interleaving.

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |   Type = 64   |  Res  |I|U|B|E|       Length = Variable       |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                              TSN                              |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |        Stream Identifier      |           Reserved            |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                      Message Identifier                       |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |    Payload Protocol Identifier / Fragment Sequence Number     |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   \                                                               \
   /                           User Data                           /
   \                                                               \
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    
     */
    @Override
    void bodyParser(ByteBuffer body) {
        if (body.remaining() >= 16) {
            _tsn = getUnsignedInt(body);
            _streamId = body.getChar();
            var reserved = body.getChar();
            _mid = body.getInt();
            var thing = body.getInt();
            if ((_flags & DataChunk.BEGINFLAG) != 0) {
                _ppid = thing;
                _fsn = 0;
            } else {
                _fsn = thing;
            }
            
            Log.info(" _tsn : " + _tsn
                    + " _streamId : " + _streamId
                    + " _mid :"+ _mid
                    + " _fsn : " + _fsn
                    + " _ppid : " + _ppid);
            Log.info("data size remaining " + body.remaining());

            _data = new byte[body.remaining()];
            body.get(_data);
            _dataOffset = 0;
            _dataLength = _data.length;

        }
    }

    @Override
    void putFixedParams(ByteBuffer ret) {
        putUnsignedInt(ret, _tsn);// = _body.getInt();
        ret.putChar((char) _streamId);// = _body.getChar();
        ret.putChar((char) 0); // reserved
        ret.putInt(_mid);
        if ((_flags & DataChunk.BEGINFLAG) != 0) {
            ret.putInt(_ppid);
        } else {
            ret.putInt(_fsn);
        }
        ret.put(_data, _dataOffset, _dataLength);
        //Log.info("sending an idata " + this.toString());
    }

    public IDataChunk() {
        super((byte) Chunk.I_DATA);
        setFlags(0); // default assumption.
    }

    @Override
    public String toString() {
        String ret = super.toString();
        ret += " mid =" + _mid + " fsn =" + _fsn;
        return ret;
    }

    @Override
    public void setFsn(int fsn) {
        _fsn = fsn;
    }

    @Override
    public void setsSeqNo(int sSeqNo) {
        _mid = sSeqNo; // this is wrong because it _should_ be a separate counter for ordered vs unordered -and be 32 bits.... but for now...
    }

    public int getMid() {
        return _mid;
    }

    public void setMid(int m) {
        _mid = m;
    }

    public int getFsn() {
        return _fsn;
    }


    public boolean expired(Association par,long now) {
        boolean ret = false;
        var s = par.getStream(this._streamId);
        if (!s.isReliable()){
            Log.info("Checking expire on "+this.toString());
            Long retries = s.getMaxRetries();
            if (retries != null){
                ret = (this._retryCount > retries);
            } else {
                Long maxtime = s.getMaxTime();
                ret = ((now - this._sentTime)> maxtime);
            }           
            Log.info(" expire is "+ret);

        }
       this.ordered = s.isOrdered();
       return ret;
    }
    boolean isOrdered(){
        return ordered;
    }
}
