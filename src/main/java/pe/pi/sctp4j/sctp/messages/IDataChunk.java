/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package pe.pi.sctp4j.sctp.messages;

import com.phono.srtplight.Log;
import java.nio.ByteBuffer;
import static pe.pi.sctp4j.sctp.messages.Chunk.getUnsignedInt;
import static pe.pi.sctp4j.sctp.messages.DataChunk.getDataFromPkt;
import pe.pi.sctp4j.sctp.messages.exceptions.InvalidDataChunkException;

/**
 *
 * @author thp
 */
public class IDataChunk extends DataChunk {

    private int _mid;
    private int _fsn;

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
        if (_body.remaining() >= 12) {
            _tsn = getUnsignedInt(_body);
            _streamId = _body.getChar();
            var reserved = _body.getChar();
            _mid = _body.getInt();
            var thing = _body.getInt();
            if ((_flags & DataChunk.BEGINFLAG) != 0) {
                _ppid = thing;
                _fsn = 0;
            } else {
                _fsn = thing;
            }

            Log.info(" _tsn : " + _tsn
                    + " _streamId : " + _streamId
                    + " _sSeqNo : " + _sSeqNo
                    + " _ppid : " + _ppid);
            Log.debug("data size remaining " + _body.remaining());
            try {
                _data = getDataFromPkt(_ppid, _body);
            } catch (InvalidDataChunkException ex) {
                _invalid = ex;
            }
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
        Log.info("sending an idata " + this.toString());
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
}
