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

import com.google.cloud.Crc32c;
import com.ipseorama.sctp.Association;
import com.ipseorama.sctp.messages.exceptions.ChecksumException;
import com.ipseorama.sctp.messages.exceptions.InvalidSCTPPacketException;
import com.ipseorama.sctp.messages.exceptions.SctpPacketFormatException;
import com.phono.srtplight.Log;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class Packet {
    /*
     SCTP Common Header Format

     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |     Source Port Number        |     Destination Port Number   |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                      Verification Tag                         |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                           Checksum                            |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    static int MTU = 1500;
    char _srcPort;
    char _destPort;
    int _verTag;
    int _chksum;
    List<Chunk> _chunks;
    private static int SUMOFFSET = 8;

    /**
     * Constructor used to parse an incoming packet
     *
     * @param pkt
     */
    public Packet(ByteBuffer pkt) throws SctpPacketFormatException, ChecksumException {
        if (pkt.limit() < 12) {
            throw new SctpPacketFormatException("SCTP packet too short expected 12 bytes, got " + pkt.limit());
        }
        checkChecksum(pkt); // if this isn't ok, then we dump the packet silently - by throwing an exception.

        _srcPort = pkt.getChar();
        _destPort = pkt.getChar();
        _verTag = pkt.getInt();
        _chksum = pkt.getInt();
        _chunks = mkChunks(pkt);

        pkt.rewind();
    }

    public Packet(int sp, int dp, int vertag) {
        _srcPort = (char) sp;
        _destPort = (char) dp;
        _verTag = vertag;
        _chunks = new ArrayList<Chunk>();
    }

    public ByteBuffer getByteBuffer() throws SctpPacketFormatException {
        ByteBuffer ret = ByteBuffer.allocate(MTU);
        ret.putChar(_srcPort);
        ret.putChar(_destPort);
        ret.putInt(_verTag);
        ret.putInt(_chksum);
        int pad = 0;
        for (Chunk c : _chunks) {
            ByteBuffer cs = ret.slice();            // create a zero offset buffer to play in
            c.write(cs); // ask the chunk to write itself into there.
            pad = cs.position() % 4;
            pad = (pad != 0) ? 4 - pad : 0;
            Log.verb("padding by " + pad);
            ret.position(pad + ret.position() + cs.position());// move us along.
        }
        /*Log.verb("un padding by " + pad);
        ret.position(ret.position() - pad);*/
        ret = (ByteBuffer) ret.flip();
        setChecksum(ret);
        return ret;
    }

    public int getSrcPort() {
        return _srcPort;
    }

    public int getDestPort() {
        return _destPort;
    }

    public long getVerTag() {
        return _verTag;
    }

    public long getChksum() {
        return _chksum;
    }

    public static String getHex(ByteBuffer in) {
        byte[] bin = new byte[in.limit()];
        in.get(bin);
        return getHex(bin, bin.length);
    }

    public static String getHex(byte[] in) {
        return getHex(in, in.length);
    }

    public static String getHex(byte[] in, int len) {
        char cmap[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        StringBuffer ret = new StringBuffer();
        int top = Math.min(in.length, len);
        for (int i = 0; i < top; i++) {
            ret.append(cmap[0x0f & (in[i] >>> 4)]);
            ret.append(cmap[in[i] & 0x0f]);
        }
        return ret.toString();
    }

    private List<Chunk> mkChunks(ByteBuffer pkt) {
        ArrayList<Chunk> ret = new ArrayList();
        Chunk next = null;
        while (null != (next = Chunk.mkChunk(pkt))) {
            ret.add(next);
            Log.debug("saw chunk: "+next.typeLookup());
        }
        return ret;
    }

    public List<Chunk> getChunkList() {
        return _chunks;
    }

    /*
     When an SCTP packet is received, the receiver MUST first check the
     CRC32c checksum as follows:

     1)  Store the received CRC32c checksum value aside.

     2)  Replace the 32 bits of the checksum field in the received SCTP
     packet with all '0's and calculate a CRC32c checksum value of the
     whole received packet.

     3)  Verify that the calculated CRC32c checksum is the same as the
     received CRC32c checksum.  If it is not, the receiver MUST treat
     the packet as an invalid SCTP packet.

     The default procedure for handling invalid SCTP packets is to
     silently discard them.

     Any hardware implementation SHOULD be done in a way that is
     verifiable by the software.
     */
    void setChecksum(ByteBuffer pkt) {
        byte b[] = new byte[4];
        pkt.putInt(SUMOFFSET, 0);
        Crc32c crc = new Crc32c();
        crc.update(pkt.array(), pkt.arrayOffset(), pkt.limit());
        long result = crc.getValue();
        b[0] = (byte) (result & 0xff);
        b[1] = (byte) ((result >> 8) & 0xff);
        b[2] = (byte) ((result >> 16) & 0xff);
        b[3] = (byte) ((result >> 24) & 0xff);
        long flip = (((0xff & b[0]) << 24)
                + ((0xff & b[1]) << 16)
                + ((0xff & b[2]) << 8)
                + (0xff & b[3]));
        pkt.putInt(8, (int) flip);
    }

    void checkChecksum(ByteBuffer pkt) throws ChecksumException {
        long farsum = (long) pkt.getInt(SUMOFFSET);
        setChecksum(pkt);
        long calc = (long) pkt.getInt(SUMOFFSET);
        if (calc != farsum) {
            Log.error("Checksums don't match " + Long.toHexString(calc) + " vs " + Long.toHexString(farsum));
            byte []p = pkt.array();
            Log.error("for packet "+getHex(p));
            throw new ChecksumException();
        }
    }
    /*
     8.5.  Verification Tag

     The Verification Tag rules defined in this section apply when sending
     or receiving SCTP packets that do not contain an INIT, SHUTDOWN
     COMPLETE, COOKIE ECHO (see Section 5.1), ABORT, or SHUTDOWN ACK
     chunk.  The rules for sending and receiving SCTP packets containing
     one of these chunk types are discussed separately in Section 8.5.1.

     When sending an SCTP packet, the endpoint MUST fill in the
     Verification Tag field of the outbound packet with the tag value in
     the Initiate Tag parameter of the INIT or INIT ACK received from its
     peer.

     When receiving an SCTP packet, the endpoint MUST ensure that the
     value in the Verification Tag field of the received SCTP packet
     matches its own tag.  If the received Verification Tag value does not
     match the receiver's own tag value, the receiver shall silently
     discard the packet and shall not process it any further except for
     those cases listed in Section 8.5.1 below.

     8.5.1.  Exceptions in Verification Tag Rules

     A) Rules for packet carrying INIT:

     -   The sender MUST set the Verification Tag of the packet to 0.

     -   When an endpoint receives an SCTP packet with the Verification
     Tag set to 0, it should verify that the packet contains only an
     INIT chunk.  Otherwise, the receiver MUST silently discard the
     packet.

     B) Rules for packet carrying ABORT:

     -   The endpoint MUST always fill in the Verification Tag field of
     the outbound packet with the destination endpoint's tag value, if
     it is known.

     -   If the ABORT is sent in response to an OOTB packet, the endpoint
     MUST follow the procedure described in Section 8.4.



     Stewart                     Standards Track                   [Page 105]

     RFC 4960          Stream Control Transmission Protocol    September 2007


     -   The receiver of an ABORT MUST accept the packet if the
     Verification Tag field of the packet matches its own tag and the
     T bit is not set OR if it is set to its peer's tag and the T bit
     is set in the Chunk Flags.  Otherwise, the receiver MUST silently
     discard the packet and take no further action.

     C) Rules for packet carrying SHUTDOWN COMPLETE:

     -   When sending a SHUTDOWN COMPLETE, if the receiver of the SHUTDOWN
     ACK has a TCB, then the destination endpoint's tag MUST be used,
     and the T bit MUST NOT be set.  Only where no TCB exists should
     the sender use the Verification Tag from the SHUTDOWN ACK, and
     MUST set the T bit.

     -   The receiver of a SHUTDOWN COMPLETE shall accept the packet if
     the Verification Tag field of the packet matches its own tag and
     the T bit is not set OR if it is set to its peer's tag and the T
     bit is set in the Chunk Flags.  Otherwise, the receiver MUST
     silently discard the packet and take no further action.  An
     endpoint MUST ignore the SHUTDOWN COMPLETE if it is not in the
     SHUTDOWN-ACK-SENT state.

     D) Rules for packet carrying a COOKIE ECHO

     -   When sending a COOKIE ECHO, the endpoint MUST use the value of
     the Initiate Tag received in the INIT ACK.

     -   The receiver of a COOKIE ECHO follows the procedures in Section
     5.

     E) Rules for packet carrying a SHUTDOWN ACK

     -   If the receiver is in COOKIE-ECHOED or COOKIE-WAIT state the
     procedures in Section 8.4 SHOULD be followed; in other words, it
     should be treated as an Out Of The Blue packet.

     */

    private void reflectedVerify(int cno, Association ass) throws InvalidSCTPPacketException {
        Chunk chunk = _chunks.get(cno);
        boolean t = ((Chunk.TBIT & chunk._flags) > 0);
        int cverTag = t ? ass.getPeerVerTag() : ass.getMyVerTag();
        if (cverTag != _verTag) {
            throw new InvalidSCTPPacketException("VerTag on an " + chunk.typeLookup() + " doesn't match " + (t ? "their " : "our ") + " vertag " + _verTag + " != " + cverTag);
        }
    }

    public void validate(Association ass) throws InvalidSCTPPacketException {
        // step 1 - deduce the validation rules:
        // validation depends on the types of chunk in the list.
        if ((_chunks != null) && (_chunks.size() > 0)) {
            int init = findChunk(Chunk.INIT);
            if (init >= 0) {
                if (init != 0) {
                    throw new InvalidSCTPPacketException("Init must be only chunk in a packet");
                }
                if (_verTag != 0) {
                    throw new InvalidSCTPPacketException("VerTag on an init packet expected to be Zeros");
                }
            } else {
                int abo = findChunk(Chunk.ABORT);
                if (abo >= 0) {
                    // we have an abort
                    _chunks = _chunks.subList(0, abo + 1); // remove any subsequent chunks.
                    reflectedVerify(abo, ass);
                } else {
                    int sdc = findChunk(Chunk.SHUTDOWN_COMPLETE);
                    if (sdc >= 0) {
                        if (sdc == 0) {
                            reflectedVerify(sdc, ass);
                        } else {
                            throw new InvalidSCTPPacketException("SHUTDOWN_COMPLETE must be only chunk in a packet");
                        }
                    } else {
                        // somewhat hidden here - but this is the normal case - not init abort or shutdown complete 
                        if (_verTag != ass.getMyVerTag()) {
                            throw new InvalidSCTPPacketException("VerTag on plain packet expected to match ours " + _verTag + " != " + ass.getMyVerTag());
                        }
                    }
                }
            }
        }
    }

    private int findChunk(int ty) {
        byte bty = (byte) ty;
        int ret = 0;
        for (Chunk c : _chunks) {
            if (c._type == bty) {
                break;
            } else {
                ret++;
            }
        }
        return (ret < _chunks.size()) ? ret : -1;
    }

}
