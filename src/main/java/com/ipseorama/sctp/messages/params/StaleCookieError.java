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
package com.ipseorama.sctp.messages.params;

import com.ipseorama.sctp.messages.Chunk;
import java.nio.ByteBuffer;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class StaleCookieError extends KnownError {
    private long _measure;
    /*
     <code>
     Stale Cookie Error (3)

     Cause of error
     --------------

     Stale Cookie Error: Indicates the receipt of a valid State Cookie
     that has expired.

     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |     Cause Code=3              |       Cause Length=8          |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                 Measure of Staleness (usec.)                  |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     Measure of Staleness: 32 bits (unsigned integer)

     This field contains the difference, in microseconds, between the
     current time and the time the State Cookie expired.

     The sender of this error cause MAY choose to report how long past
     expiration the State Cookie is by including a non-zero value in
     the Measure of Staleness field.  If the sender does not wish to
     provide this information, it should set the Measure of Staleness
     field to the value of zero.
     </code>
     */

    public StaleCookieError() {
        super(3, "StaleCookieError");
    }
    
    @Override
    public void readBody(ByteBuffer body, int blen) {
        _measure = Chunk.getUnsignedInt(body);
    }
    
    
    @Override
    public void writeBody(ByteBuffer body) {
        Chunk.putUnsignedInt(body,_measure);
    }
    
    public long getMeasure(){
        return _measure;
    }
        
    public void setMeasure(long mes){
        _measure = mes;
    }
}
