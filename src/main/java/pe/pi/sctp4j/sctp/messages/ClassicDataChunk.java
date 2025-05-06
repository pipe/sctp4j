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
public class ClassicDataChunk extends DataChunk {

    public ClassicDataChunk(byte type, byte flags, int length, ByteBuffer pkt) {
        super(type, flags, length, pkt);
    }

    public ClassicDataChunk() {
        super((byte) Chunk.DATA);
        setFlags(0); // default assumption.
    }

    @Override
    void bodyParser(ByteBuffer body) {
        if (_body.remaining() >= 12) {
            _tsn = getUnsignedInt(_body);
            _streamId = _body.getChar();
            _sSeqNo = _body.getChar();
            _ppid = _body.getInt();

            Log.debug(" _tsn : " + _tsn
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
        ret.putChar((char) _sSeqNo);// = _body.getChar();
        ret.putInt(_ppid);// = _body.getInt();
        ret.put(_data, _dataOffset, _dataLength);
    }

    @Override
    public void setFsn(int fsn) {
    }
}
