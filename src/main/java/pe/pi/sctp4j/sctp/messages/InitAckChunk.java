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
package pe.pi.sctp4j.sctp.messages;

import pe.pi.sctp4j.sctp.messages.params.StateCookie;
import pe.pi.sctp4j.sctp.messages.params.VariableParam;
import com.phono.srtplight.Log;
import java.nio.ByteBuffer;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */

/*

 The format of the INIT ACK chunk is shown below:

 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 |   Type = 2    |  Chunk Flags  |      Chunk Length             |
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 |                         Initiate Tag                          |
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 |              Advertised Receiver Window Credit                |
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
public class InitAckChunk extends Chunk {

    int _initiateTag;
    long _adRecWinCredit;
    int _numOutStreams;
    int _numInStreams;
    long _initialTSN;
    private byte[] _cookie;
    private byte[] _supportedExtensions;

    public InitAckChunk() {
        super((byte) INITACK);
    }

    public int getInitiateTag() {
        return _initiateTag;
    }

    public void setInitiateTag(int v) {
        _initiateTag = v;
    }

    public long getAdRecWinCredit() {
        return _adRecWinCredit;
    }

    public void setAdRecWinCredit(int v) {
        _adRecWinCredit = v;
    }

    public int getNumOutStreams() {
        return _numOutStreams;
    }

    public void setNumOutStreams(int v) {
        _numOutStreams = v;
    }

    public int getNumInStreams() {
        return _numInStreams;
    }

    public void setNumInStreams(int v) {
        _numInStreams = v;
    }

    public long getInitialTSN() {
        return _initialTSN;
    }

    public void setInitialTSN(long v) {
        _initialTSN = v;
    }

    public byte[] getCookie() {
        return _cookie;
    }

    public void setCookie(byte[] v) {
        _cookie = v;
    }

    public InitAckChunk(byte type, byte flags, int length, ByteBuffer pkt) {
        super(type, flags, length, pkt);
        if (_body.remaining() >= 16) {
            _initiateTag = _body.getInt();
            _adRecWinCredit = getUnsignedInt(_body);
            _numOutStreams = _body.getChar();
            _numInStreams = _body.getChar();
            _initialTSN = getUnsignedInt(_body);
            Log.verb("Init Ack" + this.toString());
            while (_body.hasRemaining()) {
                VariableParam v = readVariable();
                _varList.add(v);
            }

            for (VariableParam v : _varList) {
                // now look for variables we are expecting...
                Log.verb("variable of type: " + v.getName() + " " + v.toString());
                if (v instanceof StateCookie) {
                    _cookie = ((StateCookie) v).getData();
                } else {
                    Log.verb("ignored variable of type: " + v.getName());
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
                + ((_supportedExtensions == null) ? " no supported extensions" : " supported extensions are: " + chunksToNames(_supportedExtensions));
        ;
        return ret;
    }

    @Override
    void putFixedParams(ByteBuffer ret) {
        ret.putInt(_initiateTag);
        putUnsignedInt(ret, _adRecWinCredit);
        ret.putChar((char) _numOutStreams);
        ret.putChar((char) _numInStreams);
        putUnsignedInt(ret, _initialTSN);
        if (_cookie != null) {
            StateCookie sc = new StateCookie();
            sc.setData(_cookie);
            _varList.add(sc);
        }
        if (_supportedExtensions != null) {
            SupportedExtensions se = new SupportedExtensions();
            se.setData(_supportedExtensions);
            _varList.add(se);
        }
    }

    public byte[] getSupportedExtensions(byte[] v) {
        return _supportedExtensions;
    }

    public void setSupportedExtensions(byte[] v) {
        _supportedExtensions = v;
    }

}
