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
package com.ipseorama.sctp.messages;

import com.ipseorama.sctp.messages.params.RequestedHMACAlgorithmParameter;
import com.ipseorama.sctp.messages.params.StateCookie;
import com.ipseorama.sctp.messages.params.VariableParam;
import com.phono.srtplight.Log;
import java.nio.ByteBuffer;
import java.util.ArrayList;

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
                + " initialTSN : " + _initialTSN;//+ " farForwardTSNsupported : "+_farForwardTSNsupported;
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
