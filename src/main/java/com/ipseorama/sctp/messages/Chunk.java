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
 * along with this program; if not, writeBody to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.ipseorama.sctp.messages;

import com.ipseorama.sctp.messages.exceptions.InvalidDataChunkException;
import com.ipseorama.sctp.messages.params.RequestedHMACAlgorithmParameter;
import com.ipseorama.sctp.messages.params.AddIncomingStreamsRequestParameter;
import com.ipseorama.sctp.messages.params.AddOutgoingStreamsRequestParameter;
import com.ipseorama.sctp.messages.params.ReconfigurationResponseParameter;
import com.ipseorama.sctp.messages.params.SSNTSNResetRequestParameter;
import com.ipseorama.sctp.messages.params.CookiePreservative;
import com.ipseorama.sctp.messages.params.UnrecognizedParameters;
import com.ipseorama.sctp.messages.params.StateCookie;
import com.ipseorama.sctp.messages.params.IPv6Address;
import com.ipseorama.sctp.messages.params.IPv4Address;
import com.ipseorama.sctp.messages.exceptions.SctpPacketFormatException;
import com.ipseorama.sctp.messages.params.HostNameAddress;
import com.ipseorama.sctp.messages.params.IncomingSSNResetRequestParameter;
import com.ipseorama.sctp.messages.params.KnownError;
import com.ipseorama.sctp.messages.params.KnownParam;
import com.ipseorama.sctp.messages.params.OutgoingSSNResetRequestParameter;
import com.ipseorama.sctp.messages.params.ProtocolViolationError;
import com.ipseorama.sctp.messages.params.StaleCookieError;
import com.ipseorama.sctp.messages.params.SupportedAddressTypes;
import com.ipseorama.sctp.messages.params.Unknown;
import com.ipseorama.sctp.messages.params.VariableParam;
import com.phono.srtplight.Log;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public abstract class Chunk {
    /*
     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |   Chunk Type  | Chunk  Flags  |        Chunk Length           |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     \                                                               \
     /                          Chunk Value                          /
     \                                                               \
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    public final static int DATA = 0;
    public final static int INIT = 1;
    public final static int INITACK = 2;
    public final static int SACK = 3;
    public final static int HEARTBEAT = 4;
    public final static int HEARTBEAT_ACK = 5;
    public final static int ABORT = 6;
    public final static int ERROR = 9;
    public final static int COOKIE_ECHO = 10;
    public final static int COOKIE_ACK = 11;
    public final static int SHUTDOWN_COMPLETE = 14;


    static byte TBIT = 1;

    /*
       Chunk Length: 16 bits (unsigned integer)

      This value represents the size of the chunk in bytes, including
      the Chunk Type, Chunk Flags, Chunk Length, and Chunk Value fields.
      Therefore, if the Chunk Value field is zero-length, the Length
      field will be set to 4.  The Chunk Length field does not count any
      chunk padding.
    */
    static Chunk mkChunk(ByteBuffer pkt) {
        Chunk ret = null;
        if (pkt.remaining() >= 4) {
            byte type = pkt.get();
            byte flags = pkt.get();
            int length = pkt.getChar();
            // at somepoint this may end up being a switch making individual typed subclasses.
            switch ((int) type) {
                case DATA:
                    ret = new DataChunk(type, flags, length, pkt);
                    break;
                case INIT:
                    ret = new InitChunk(type, flags, length, pkt);
                    break;
                case SACK:
                    ret = new SackChunk(type, flags, length, pkt);
                    break;
                case INITACK:
                    ret = new InitAckChunk(type, flags, length, pkt);
                    break;
                case COOKIE_ECHO:
                    ret = new CookieEchoChunk(type, flags, length, pkt);
                    break;
                case COOKIE_ACK:
                    ret = new CookieAckChunk(type, flags, length, pkt);
                    break;
                case ABORT:
                    ret = new AbortChunk(type, flags, length, pkt);
                    break;
                case HEARTBEAT:
                    ret = new HeartBeatChunk(type, flags, length, pkt);
                    break;
                default:
                    ret = new Chunk(type, flags, length, pkt) {
                        @Override
                        void putFixedParams(ByteBuffer ret) {
                            return;
                        }
                    };
                    break;

            }
            if (ret != null) {
                if (pkt.hasRemaining()) {
                    int mod = ret.getLength() % 4;
                    if (mod != 0) {
                        for (int pad = mod; pad < 4; pad++) {
                            pkt.get();
                        }
                    }
                }
            }
        }
        return ret;
    }
    /*
     0          - Payload Data (DATA)
     1          - Initiation (INIT)
     2          - Initiation Acknowledgement (INIT ACK)
     3          - Selective Acknowledgement (SACK)
     4          - Heartbeat Request (HEARTBEAT)
     5          - Heartbeat Acknowledgement (HEARTBEAT ACK)
     6          - Abort (ABORT)
     7          - Shutdown (SHUTDOWN)
     8          - Shutdown Acknowledgement (SHUTDOWN ACK)
     9          - Operation Error (ERROR)
     10         - State Cookie (COOKIE ECHO)
     11         - Cookie Acknowledgement (COOKIE ACK)




     Stewart                     Standards Track                    [Page 17]

     RFC 4960          Stream Control Transmission Protocol    September 2007


     12         - Reserved for Explicit Congestion Notification Echo
     (ECNE)
     13         - Reserved for Congestion Window Reduced (CWR)
     14         - Shutdown Complete (SHUTDOWN COMPLETE)
     */
    /*
    
     Chunk Type  Chunk Name
     --------------------------------------------------------------
     0xC1    Address Configuration Change Chunk        (ASCONF)
     0x80    Address Configuration Acknowledgment      (ASCONF-ACK)
    
     +------------+------------------------------------+
     | Chunk Type | Chunk Name                         |
     +------------+------------------------------------+
     | 130        | Re-configuration Chunk (RE-CONFIG) |
     +------------+------------------------------------+
    
     The following new chunk type is defined:

     Chunk Type    Chunk Name
     ------------------------------------------------------
     192 (0xC0)    Forward Cumulative TSN (FORWARD TSN)
     
    
     Chunk Type  Chunk Name
     --------------------------------------------------------------
     0x81    Packet Drop Chunk        (PKTDROP)
     */

    final static HashMap<Integer, String> _typeLookup;//= //{"DATA", "INIT", "INIT ACK", "SACK", "HEARTBEAT", "HEARTBEAT ACK", "ABORT", "SHUTDOWN", "SHUTDOWN ACK", "ERROR", "COOKIE ECHO", "COOKIE ACK", "ECNE", "CWR", "SHUTDOWN COMPLETE","AUTH"};
    byte _type;
    byte _flags;
    int _length;
    ByteBuffer _body;
    ArrayList<VariableParam> _varList = new ArrayList<VariableParam>();

    static {
        _typeLookup = new HashMap();
        String[] names4960 = {"DATA", "INIT", "INIT ACK", "SACK", "HEARTBEAT", "HEARTBEAT ACK", "ABORT", "SHUTDOWN", "SHUTDOWN ACK", "ERROR", "COOKIE ECHO", "COOKIE ACK", "ECNE", "CWR", "SHUTDOWN COMPLETE", "AUTH"};
        for (int i = 0; i < names4960.length; i++) {
            _typeLookup.put(new Integer(i), names4960[i]);
        }
        _typeLookup.put(new Integer(0xC1), "ASCONF");
        _typeLookup.put(new Integer(0x80), "ASCONF-ACK");
        _typeLookup.put(new Integer(130), "RE-CONFIG");
        _typeLookup.put(new Integer(192), "FORWARDTSN");
        _typeLookup.put(new Integer(0x81), "PKTDROP");

    }

    protected Chunk(byte type) {
        _type = type;
    }

    protected Chunk(byte type, byte flags, int length, ByteBuffer pkt) {
        _type = type;
        _flags = flags;
        _length = length;
        /* Copy version 
        byte bb[] = new byte[length -4]; 
        pkt.get(bb);
        _body = ByteBuffer.wrap(bb);
        */
        // or use same data but different bytebuffers wrapping it 
        _body = pkt.slice();
        _body.limit(length-4);
        pkt.position(pkt.position()+(length -4));
    }
// sad ommission in ByteBuffer 

    public static long getUnsignedInt(ByteBuffer bb) {
        return ((long) bb.getInt() & 0xffffffffL);
    }

    public static void putUnsignedInt(ByteBuffer bb, long value) {
        bb.putInt((int) (value & 0xffffffffL));
    }

    void write(ByteBuffer ret) throws SctpPacketFormatException {
        ret.put(_type);
        ret.put(_flags);
        ret.putChar((char) 4); // marker for length;
        putFixedParams(ret);
        int pad = 0;
        if (_varList != null) {
            for (VariableParam v : this._varList) {
                System.out.println("var " + v.getName() + " at " + ret.position());

                ByteBuffer var = ret.slice();
                var.putChar((char) v.getType());
                var.putChar((char) 4); // length holder.
                v.writeBody(var);
                var.putChar(2, (char) var.position());
                System.out.println("setting var length to " + var.position());
                pad = var.position() % 4;
                pad = (pad != 0) ? 4 - pad : 0;
                System.out.println("padding by " + pad);
                ret.position(ret.position() + var.position() + pad);
            }
        }
        System.out.println("un padding by " + pad);
        ret.position(ret.position() - pad);
        // and push the new length into place.
        ret.putChar(2, (char) ret.position());
        System.out.println("setting chunk length to " + ret.position());
    }

    public String typeLookup() {
        return typeLookup(this._type);
    }

    public static String typeLookup(byte t) {
        Integer k = new Integer((int) ((0xff) & t));
        String ret = _typeLookup.get(k);
        if (ret == null) {
            ret = "unknown(" + (int) ((0xff) & t) + ")";
        }
        return ret;
    }

    public String toString() {
        return "Chunk : type " + typeLookup(_type) + " flags " + Integer.toHexString((0xff) & _flags) + " length = " + _length;
    }

    public int getType() {
        return _type;
    }

    int getLength() {
        return _length;
    }
    /*
    
     1	Heartbeat Info	[RFC4960]
     2-4	Unassigned	
     5	IPv4 Address	[RFC4960]
     6	IPv6 Address	[RFC4960]
     7	State Cookie	[RFC4960]
     8	Unrecognized Parameters	[RFC4960]
     9	Cookie Preservative	[RFC4960]
     10	Unassigned	
     11	Host Name Address	[RFC4960]
     12	Supported Address Types	[RFC4960]
     13	Outgoing SSN Reset Request Parameter	[RFC6525]
     14	Incoming SSN Reset Request Parameter	[RFC6525]
     15	SSN/TSN Reset Request Parameter	[RFC6525]
     16	Re-configuration Response Parameter	[RFC6525]
     17	Add Outgoing Streams Request Parameter	[RFC6525]
     18	Add Incoming Streams Request Parameter	[RFC6525]
     19-32767	Unassigned	
     32768	Reserved for ECN Capable (0x8000)	
     32770	Random (0x8002)	[RFC4805]
     32771	Chunk List (0x8003)	[RFC4895]
     32772	Requested HMAC Algorithm Parameter (0x8004)	[RFC4895]
     32773	Padding (0x8005)	
     32776	Supported Extensions (0x8008)	[RFC5061]
     32777-49151	Unassigned	
     49152	Forward TSN supported (0xC000)	[RFC3758]
     49153	Add IP Address (0xC001)	[RFC5061]
     49154	Delete IP Address (0xC002)	[RFC5061]
     49155	Error Cause Indication (0xC003)	[RFC5061]
     49156	Set Primary Address (0xC004)	[RFC5061]
     49157	Success Indication (0xC005)	[RFC5061]
     49158	Adaptation Layer Indication (0xC006)	[RFC5061]

    
     */

    protected VariableParam readVariable() {
        int type = _body.getChar();
        int len = _body.getChar();
        int blen = len - 4;
        byte[] data = null;
        Unknown var;
        switch (type) {
            case 1:
                var = new HeartbeatInfo(1, "HeartbeatInfo");
                break;
//      2-4	Unassigned	
            case 5:
                var = new IPv4Address(5, "IPv4Address");
                break;
            case 6:
                var = new IPv6Address(6, "IPv6Address");
                break;
            case 7:
                var = new StateCookie(7, "StateCookie");
                break;
            case 8:
                var = new UnrecognizedParameters(8, "UnrecognizedParameters");
                break;
            case 9:
                var = new CookiePreservative(9, "CookiePreservative");
                break;
//      10	Unassigned	
            case 11:
                var = new HostNameAddress(11, "HostNameAddress");
                break;
            case 12:
                var = new SupportedAddressTypes(12, "SupportedAddressTypes");
                break;
            case 13:
                var = new OutgoingSSNResetRequestParameter(13, "OutgoingSSNResetRequestParameter");
                break;
            case 14:
                var = new IncomingSSNResetRequestParameter(14, "IncomingSSNResetRequestParameter");
                break;
            case 15:
                var = new SSNTSNResetRequestParameter(15, "SSNTSNResetRequestParameter");
                break;
            case 16:
                var = new ReconfigurationResponseParameter(16, "ReconfigurationResponseParameter");
                break;
            case 17:
                var = new AddOutgoingStreamsRequestParameter(17, "AddOutgoingStreamsRequestParameter");
                break;
            case 18:
                var = new AddIncomingStreamsRequestParameter(18, "AddIncomingStreamsRequestParameter");
                break;
//      19-32767	Unassigned	
            case 32768:
                var = new Unknown(32768, "ReservedforECNCapable");
                break;
            case 32770:
                var = new RandomParam(32770, "Random");
                break;
            case 32771:
                var = new ChunkListParam(32771, "ChunkList");
                break;
            case 32772:
                var = new RequestedHMACAlgorithmParameter(32772, "RequestedHMACAlgorithmParameter");
                break;
            case 32773:
                var = new Unknown(32773, "Padding");
                break;
            case 32776:
                var = new SupportedExtensions(32776, "SupportedExtensions");
                break;
//      32777-49151	Unassigned	
            case 49152:
                var = new ForwardTSNsupported(49152, "ForwardTSNsupported");
                break;
            case 49153:
                var = new Unknown(49153, "AddIPAddress");
                break;
            case 49154:
                var = new Unknown(49154, "DeleteIPAddress");
                break;
            case 49155:
                var = new Unknown(49155, "ErrorCauseIndication");
                break;
            case 49156:
                var = new Unknown(49156, "SetPrimaryAddress");
                break;
            case 49157:
                var = new Unknown(49157, "SuccessIndication");
                break;
            case 49158:
                var = new Unknown(49158, "AdaptationLayerIndication");
                break;
            default:
                var = new Unknown(-1, "Unknown");
                break;
        }
        try {
            var.readBody(_body, blen);
            System.out.println("variable type " + var.getType() + " name " + var.getName());
        } catch (SctpPacketFormatException ex) {
            Log.error(ex.getMessage());
        }
        if (_body.hasRemaining()) {
            int mod = blen % 4;
            if (mod != 0) {
                for (int pad = mod; pad < 4; pad++) {
                    _body.get();
                }
            }
        }
        return var;
    }

    protected VariableParam readErrorParam() {
        int type = _body.getChar();
        int len = _body.getChar();
        int blen = len - 4;
        byte[] data = null;
        KnownError var = null;
        switch (type) {
            case 1:
                var = new KnownError(1, "InvalidStreamIdentifier");
                break;//[RFC4960]
            case 2:
                var = new KnownError(2, "MissingMandatoryParameter");
                break;//[RFC4960]
            case 3:
                var = new StaleCookieError();
                break;//[RFC4960]
            case 4:
                var = new KnownError(4, "OutofResource");
                break;//[RFC4960]
            case 5:
                var = new KnownError(5, "UnresolvableAddress");
                break;//[RFC4960]
            case 6:
                var = new KnownError(6, "UnrecognizedChunkType");
                break;//[RFC4960]
            case 7:
                var = new KnownError(7, "InvalidMandatoryParameter");
                break;//[RFC4960]
            case 8:
                var = new KnownError(8, "UnrecognizedParameters");
                break;//[RFC4960]
            case 9:
                var = new KnownError(9, "NoUserData");
                break;//[RFC4960]
            case 10:
                var = new KnownError(10, "CookieReceivedWhileShuttingDown");
                break;//[RFC4960]
            case 11:
                var = new KnownError(11, "RestartofanAssociationwithNewAddresses");
                break;//[RFC4960]
            case 12:
                var = new KnownError(12, "UserInitiatedAbort");
                break;//[RFC4460]
            case 13:
                var = new ProtocolViolationError(13, "ProtocolViolation");
                break;//[RFC4460]
// 14-159,Unassigned,
            case 160:
                var = new KnownError(160, "RequesttoDeleteLastRemainingIPAddress");
                break;//[RFC5061]
            case 161:
                var = new KnownError(161, "OperationRefusedDuetoResourceShortage");
                break;//[RFC5061]
            case 162:
                var = new KnownError(162, "RequesttoDeleteSourceIPAddress");
                break;//[RFC5061]
            case 163:
                var = new KnownError(163, "AssociationAbortedduetoillegalASCONF-ACK");
                break;//[RFC5061]
            case 164:
                var = new KnownError(164, "Requestrefused-noauthorization");
                break;//[RFC5061]
// 165-260,Unassigned,
            case 261:
                var = new KnownError(261, "UnsupportedHMACIdentifier");
                break;//[RFC4895]
// 262-65535,Unassigned,
        }
        try {
            var.readBody(_body, blen);
            Log.verb("variable type " + var.getType() + " name " + var.getName());
            Log.verb("additional info " + var.toString());
        } catch (SctpPacketFormatException ex) {
            Log.error(ex.getMessage());
        }
        if (_body.hasRemaining()) {
            int mod = blen % 4;
            if (mod != 0) {
                for (int pad = mod; pad < 4; pad++) {
                    _body.get();
                }
            }
        }
        return var;
    }

    abstract void putFixedParams(ByteBuffer ret);

    public void validate() throws Exception { // todo be more specific in the Exception tree

        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    protected static class HeartbeatInfo extends KnownParam {

        public HeartbeatInfo(int t, String n) {
            super(t, n);
        }
    }

    protected static class ForwardTSNsupported extends KnownParam {

        public ForwardTSNsupported(int t, String n) {
            super(t, n);
        }

    }

    protected static class RandomParam extends KnownParam {

        public RandomParam(int t, String n) {
            super(t, n);
        }

        public String toString() {
            String ret = " random value ";
            ret += Packet.getHex(this.getData());
            return super.toString() + ret;
        }

    }

    protected static class ChunkListParam extends KnownParam {

        public ChunkListParam(int t, String n) {
            super(t, n);
        }

        public String toString() {
            String ret = " ChunksTypes ";
            byte[] data = this.getData();
            for (int i = 0; i < data.length; i++) {
                ret += " " + typeLookup(data[i]);
            }
            return super.toString() + ret;
        }
    }

    protected static class SupportedExtensions extends KnownParam {

        public SupportedExtensions() {
            this(32776, "SupportedExtensions");
        }

        public SupportedExtensions(int t, String n) {
            super(t, n);
        }

        public String toString() {
            String ret = " ChunksTypes ";
            byte[] data = this.getData();
            for (int i = 0; i < data.length; i++) {
                ret += " " + typeLookup(data[i]);
            }
            return super.toString() + ret;
        }
    }

}
