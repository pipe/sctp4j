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

import pe.pi.sctp4j.sctp.messages.exceptions.SctpPacketFormatException;
import java.nio.ByteBuffer;

/**
 *
 * @author tim
 */
public class CookiePreservative extends KnownParam {
    Long time ;
    public CookiePreservative(int t, String n) {
        super(t, n);
    }
    @Override
    public void readBody(ByteBuffer body, int blen) throws SctpPacketFormatException {
        int ctime = body.getInt();
        time = new Long(ctime);
    }
    
        @Override
    public void writeBody(ByteBuffer body) throws SctpPacketFormatException {
        body.putInt(time.intValue());
    }
}
