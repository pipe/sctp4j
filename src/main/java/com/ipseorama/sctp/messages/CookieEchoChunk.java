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

import com.ipseorama.sctp.Association;
import com.ipseorama.sctp.messages.exceptions.SctpPacketFormatException;
import com.ipseorama.sctp.messages.params.StateCookie;
import com.ipseorama.sctp.messages.params.VariableParam;
import com.phono.srtplight.Log;
import java.nio.ByteBuffer;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 * 
 * <code> 
 * 3.3.11.  Cookie Echo (COOKIE ECHO) (10)

   This chunk is used only during the initialization of an association.
   It is sent by the initiator of an association to its peer to complete
   the initialization process.  This chunk MUST precede any DATA chunk
   sent within the association, but MAY be bundled with one or more DATA
   chunks in the same packet.

        0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |   Type = 10   |Chunk  Flags   |         Length                |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       /                     Cookie                                    /
       \                                                               \
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

   Chunk Flags: 8 bit

      Set to 0 on transmit and ignored on receipt.

   Length: 16 bits (unsigned integer)

      Set to the size of the chunk in bytes, including the 4 bytes of
      the chunk header and the size of the cookie.





Stewart                     Standards Track                    [Page 50]

RFC 4960          Stream Control Transmission Protocol    September 2007


   Cookie: variable size

      This field must contain the exact cookie received in the State
      Cookie parameter from the previous INIT ACK.

      An implementation SHOULD make the cookie as small as possible to
      ensure interoperability.

      Note: A Cookie Echo does NOT contain a State Cookie parameter;
      instead, the data within the State Cookie's Parameter Value
      becomes the data within the Cookie Echo's Chunk Value.  This
      allows an implementation to change only the first 2 bytes of the
      State Cookie parameter to become a COOKIE ECHO chunk.
 * </code>
 */
public class CookieEchoChunk extends Chunk {
    private byte[] _cookieData;

    public CookieEchoChunk(byte type, byte flags, int length, ByteBuffer pkt) {
        super(type, flags, length, pkt);
        _cookieData = new byte[_body.remaining()];
        _body.get(_cookieData);
    }

    public CookieEchoChunk() {
        super((byte)COOKIE_ECHO);
    }
    @Override
    public void validate() throws SctpPacketFormatException{
        if (_cookieData.length != Association.COOKIESIZE){
            throw new SctpPacketFormatException("cookie Echo wrong length for our association "+ _cookieData.length +" != "+ Association.COOKIESIZE);
        }
    }
    
    public void setCookieData(byte[] cd){
        _cookieData = cd;
    }
    
    public byte [] getCookieData(){
        return _cookieData;
    }
    @Override
    void putFixedParams(ByteBuffer ret) {
        Log.debug("cookie is "+_cookieData +"and buffer is "+ret);
        ret.put(_cookieData);
    }
    

}
