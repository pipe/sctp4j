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

import java.nio.ByteBuffer;

/**
 *
 * @author tim
 */
public class HostNameAddress extends KnownParam {

    String hostname;

    public HostNameAddress(int t, String n) {
        super(t, n);
    }

    public void readBody(ByteBuffer body, int blen) {
        byte data[] = new byte[blen];
        body.get(data);
        int off = blen - 1;
        // trim any 0 bytes off the end.
        while ((off > 0) && (data[off--] == 0)) {
            blen--;
        }
        hostname = new String(data, 0, blen);
    }
    public void writeBody(ByteBuffer body) {
        // gonz up a C style string.
        byte [] b = hostname.getBytes();
        _data = new byte[b.length+1];
        System.arraycopy(b, 0, _data, 0, b.length);
        body.put(_data);
    }
}
