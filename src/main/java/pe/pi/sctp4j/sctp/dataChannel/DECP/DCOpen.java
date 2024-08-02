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
package pe.pi.sctp4j.sctp.dataChannel.DECP;

import pe.pi.sctp4j.sctp.behave.SCTPStreamBehaviour;
import pe.pi.sctp4j.sctp.behave.OrderedStreamBehaviour;
import pe.pi.sctp4j.sctp.behave.UnorderedStreamBehaviour;
import pe.pi.sctp4j.sctp.messages.Packet;
import pe.pi.sctp4j.sctp.messages.exceptions.InvalidDataChunkException;
import com.phono.srtplight.Log;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class DCOpen {

    /*
     +------------------------------------------------+------+-----------+
     | Name                                           | Type | Reference |
     +------------------------------------------------+------+-----------+
     | DATA_CHANNEL_RELIABLE                          | 0x00 | [RFCXXXX] |
     | DATA_CHANNEL_RELIABLE_UNORDERED                | 0x80 | [RFCXXXX] |
     | DATA_CHANNEL_PARTIAL_RELIABLE_REXMIT           | 0x01 | [RFCXXXX] |
     | DATA_CHANNEL_PARTIAL_RELIABLE_REXMIT_UNORDERED | 0x81 | [RFCXXXX] |
     | DATA_CHANNEL_PARTIAL_RELIABLE_TIMED            | 0x02 | [RFCXXXX] |
     | DATA_CHANNEL_PARTIAL_RELIABLE_TIMED_UNORDERED  | 0x82 | [RFCXXXX] |
     | Reserved                                       | 0x7f | [RFCXXXX] |
     | Reserved                                       | 0xff | [RFCXXXX] |
     | Unassigned                                     | rest |           |
     +------------------------------------------------+------+-----------+
     */
    public final static byte RELIABLE = 0x0;
    public final static byte PARTIAL_RELIABLE_REXMIT = 0x01;
    public final static byte PARTIAL_RELIABLE_REXMIT_UNORDERED = (byte) 0x81;
    public final static byte PARTIAL_RELIABLE_TIMED = 0x02;
    public final static byte PARTIAL_RELIABLE_TIMED_UNORDERED = (byte) 0x82;
    public final static byte RELIABLE_UNORDERED = (byte) 0x80;

    /*
     5.1.  DATA_CHANNEL_OPEN Message

     This message is sent initially on the stream used for user messages
     using the channel.

     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |  Message Type |  Channel Type |            Priority           |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                    Reliability Parameter                      |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |         Label Length          |       Protocol Length         |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     \                                                               /
     |                             Label                             |
     /                                                               \
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     \                                                               /
     |                            Protocol                           |
     /                                                               \
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    private byte _messType;
    private byte _chanType;
    private int _priority;
    private long _reliablity;
    int _labLen;
    int _protLen;
    private byte[] _label;
    private byte[] _protocol;
    final static int OPEN = 0x03;
    final static int ACK = 0x02;
    boolean _isAck = false;

    public DCOpen(String label) {
        this((byte) RELIABLE, 0, 0, label, "");
    }

    // used to make Acks only
    private DCOpen() {
    }

    public DCOpen(
            byte chanType,
            int priority,
            long reliablity,
            String label,
            String protocol) {
        _messType = (byte) OPEN;
        _chanType = chanType;
        _priority = priority;
        _reliablity = reliablity;
        _label = label.getBytes();
        _protocol = protocol.getBytes();
        _labLen = _label.length;
        _protLen = _protocol.length;
    }

    public byte[] getBytes() {
        int sz = 12 + _labLen + pad(_labLen) + _protLen + pad(_protLen);
        Log.verb("dcopen needs " + sz + " bytes ");

        byte[] ret = new byte[sz];
        ByteBuffer buff = ByteBuffer.wrap(ret);
        buff.put((byte) _messType);
        buff.put((byte) _chanType);
        buff.putChar((char) _priority);
        buff.putInt((int) _reliablity);
        buff.putChar((char) _labLen);
        buff.putChar((char) _protLen);
        buff.put(_label);
        Buffer bu = (Buffer)buff; // work around for needless incompatibility between JDK 11 and 8
        bu.position(bu.position() + pad(_labLen));
        buff.put(_protocol);
        bu.position(bu.position() + pad(_protLen));

        return ret;
    }

    final static public int pad(int len) {
        int mod = len % 4;
        int res = 0;
        Log.verb("field of " + len + " mod 4 is " + mod);

        if (mod > 0) {
            res = (4 - mod);
        }
        Log.verb("padded by " + res);
        return res;
    }

    public DCOpen(ByteBuffer bb) throws InvalidDataChunkException {
        _messType = bb.get();
        switch (_messType) {
            case OPEN:
                _chanType = bb.get();
                _priority = bb.getChar();
                _reliablity = bb.getInt();
                _labLen = bb.getChar();
                _protLen = bb.getChar();
                _label = new byte[_labLen];
                bb.get(_label);
                _protocol = new byte[_protLen];
                bb.get(_protocol);
                break;
            case ACK:
                _isAck = true;
                break;
            default:
                throw new InvalidDataChunkException("Unexpected DCEP message type " + _messType);
            //break;

        }
    }

    public String toString() {
        return _isAck ? "Ack " : "Open "
                + " _chanType =" + (int) _chanType
                + " _priority = " + _priority
                + " _reliablity = " + _reliablity
                + " _label = " + new String(_label)
                + " _protocol = " + Packet.getHex(_protocol);
    }

    public boolean isAck() {
        return _isAck;
    }

    public SCTPStreamBehaviour mkStreamBehaviour() {
        Log.debug("Making a behaviour for dcep stream " + new String(_label));
        SCTPStreamBehaviour behave = null;
        switch (_chanType) {
            case RELIABLE:
                behave = new OrderedStreamBehaviour();
                break;
            case RELIABLE_UNORDERED:
                behave = new UnorderedStreamBehaviour();
                break;
            // todo these next 4 are wrong... the odering is atleast correct
            // even if the retry is wrong.
            case PARTIAL_RELIABLE_REXMIT:
            case PARTIAL_RELIABLE_TIMED:
                behave = new OrderedStreamBehaviour();
                break;
            case PARTIAL_RELIABLE_REXMIT_UNORDERED:
            case PARTIAL_RELIABLE_TIMED_UNORDERED:
                behave = new UnorderedStreamBehaviour();
                break;
        }
        if (behave != null) {
            Log.debug(new String(_label) + " behaviour is " + behave.getClass().getSimpleName());
        }

        return behave;
    }

    public String getLabel() {
        return new String(_label);
    }

    public static DCOpen mkAck() {
        DCOpen ack = new DCOpen() {
            @Override
            public byte[] getBytes() {
                byte[] a = new byte[1];
                a[0] = ACK;
                return a;
            }
        };
        ack._isAck = true;
        return ack;
    }

}
