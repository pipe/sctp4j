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
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 *
 * @author tim
 */
public class IPv6Address extends KnownParam {

    Inet6Address addr;

    public IPv6Address(int t, String n) {
        super(t, n);
    }

    @Override
    public void readBody(ByteBuffer body, int blen) throws SctpPacketFormatException {
        byte[] data = new byte[blen];
        body.get(data);
        try {
            addr = (Inet6Address) Inet6Address.getByAddress(data);
        } catch (UnknownHostException ex) {
            throw new SctpPacketFormatException(ex.getMessage());
        }
    }

    @Override
    public void writeBody(ByteBuffer body) throws SctpPacketFormatException {
        body.put(addr.getAddress());
    }
}
