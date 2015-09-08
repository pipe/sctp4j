/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.sctp.messages;

import com.ipseorama.sctp.messages.params.RequestedHMACAlgorithmParameter;
import com.ipseorama.sctp.messages.params.VariableParam;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 *
 * @author tim
 */
public class InitChunk extends Chunk {
    /*
     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |   Type = 1    |  Chunk Flags  |      Chunk Length             |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                         Initiate Tag                          |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |           Advertised Receiver Window Credit (a_rwnd)          |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |  Number of Outbound Streams   |  Number of Inbound Streams    |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                          Initial TSN                          |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     \                                                               \
     /              Optional/Variable-Length Parameters              /
     \                                                               \
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    int _initiateTag;
    long _adRecWinCredit;
    int _numOutStreams;
    int _numInStreams;
    long _initialTSN;
    byte [] _farSupportedExtensions;
    byte [] _farRandom;
    boolean _farForwardTSNsupported;
    byte[] _farHmacs;
    byte[] _farChunks;
    public int _outStreams;

    public InitChunk(){
        super((byte)INIT);
    }
    
    public InitChunk(byte type, byte flags, int length, ByteBuffer pkt) {
        super(type, flags, length, pkt);
        if (_body.remaining() >= 16) {
            _initiateTag = _body.getInt();
            _adRecWinCredit = getUnsignedInt(_body);
            _numOutStreams = _body.getChar();
            _numInStreams = _body.getChar();
            _initialTSN =  getUnsignedInt(_body);
            System.out.println("Init " + this.toString());
            while (_body.hasRemaining()) {
                VariableParam v = readVariable();
                _varList.add(v);
            }
            for (VariableParam v : _varList){
                // now look for variables we are expecting...
                System.out.println("variable of type: "+v.getName()+" "+ v.toString());
                if (v instanceof SupportedExtensions){
                    _farSupportedExtensions = ((SupportedExtensions)v).getData();
                } else if (v instanceof RandomParam){
                    _farRandom = ((RandomParam)v).getData();
                } else if (v instanceof ForwardTSNsupported){
                    _farForwardTSNsupported = true;
                } else if (v instanceof RequestedHMACAlgorithmParameter){
                    _farHmacs = ((RequestedHMACAlgorithmParameter)v).getData();
                } else if (v instanceof ChunkListParam){
                    _farChunks = ((ChunkListParam)v).getData();
                } else {
                    System.out.println("unexpected variable of type: "+v.getName());
                }
            }
        }
    }
    public String toString() {
        String ret = super.toString();
        ret += " initiateTag : " + _initiateTag
                + " adRecWinCredit : " + _adRecWinCredit
                + " numOutStreams : " + _numOutStreams
                + " numInStreams : " + _numInStreams
                + " initialTSN : " + _initialTSN
                + " farForwardTSNsupported : "+_farForwardTSNsupported;
        return ret;
    }

    @Override
    void putFixedParams(ByteBuffer ret) {
        ret.putInt(_initiateTag);
        putUnsignedInt(ret,_adRecWinCredit);
        ret.putChar((char) _numOutStreams);
        ret.putChar((char) _numInStreams);
        Chunk.putUnsignedInt(ret,_initialTSN);
    }

    public int getInitiateTag() {
        return _initiateTag;
    }
    
    public long getAdRecWinCredit(){
        return _adRecWinCredit;
    }
    public int getNumOutStreams(){
        return _numOutStreams;
    }
    public int getNumInStreams(){
        return _numInStreams;
    }
    public long getInitialTSN(){
        return _initialTSN;
    }
    public byte [] getFarSupportedExtensions(){
        return _farSupportedExtensions;
    }        
}
