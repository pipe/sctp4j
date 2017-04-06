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
package pe.pi.sctp4j.sctp.messages.params;

import pe.pi.sctp4j.sctp.messages.Chunk;
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
