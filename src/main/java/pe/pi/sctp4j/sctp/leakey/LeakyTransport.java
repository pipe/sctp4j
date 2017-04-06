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
package pe.pi.sctp4j.sctp.leakey;

import com.phono.srtplight.Log;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.bouncycastle.crypto.tls.DatagramTransport;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 *
 * class that taps off the DTLS decoded SCTP traffic and echoes it to a pair of
 * UDP ports so they can be pcaped by wireshark Do not use this class in
 * production code. test/debug only!
 *
 */
class LeakyTransport implements DatagramTransport {

    DatagramTransport _dtls;
    DatagramSocket _logrec;
    DatagramSocket _logsend;
    final static short SCTP = 9899;

    public LeakyTransport(DatagramTransport transport) {
        try {
            _dtls = transport;
            InetAddress me = InetAddress.getLocalHost();
            _logrec = new DatagramSocket(SCTP, me);
            _logsend = new DatagramSocket(SCTP + 1, me);

            InetSocketAddress s = (InetSocketAddress) _logsend.getLocalSocketAddress();
            Log.warn("Leaking to send address " + s.getHostString() + ":" + s.getPort());
            InetSocketAddress r = (InetSocketAddress) _logrec.getLocalSocketAddress();
            Log.warn("Leaking to recv address " + r.getHostString() + ":" + r.getPort());
        } catch (Exception ex) {
            Log.error("exception in making Leaky socket");
        }
    }

    @Override
    public int getReceiveLimit() throws IOException {
        return _dtls.getReceiveLimit();
    }

    @Override
    public int getSendLimit() throws IOException {
        return _dtls.getSendLimit();
    }

    @Override
    public int receive(byte[] bytes, int offs, int len, int sleep) throws IOException {
        int sz = _dtls.receive(bytes, offs, len, sleep);
        if (sz > 0) {
            DatagramPacket p = new DatagramPacket(bytes, offs, sz, _logsend.getLocalSocketAddress());
            _logrec.send(p);
        }
        return sz;
    }

    @Override
    public void send(byte[] bytes, int offs, int len) throws IOException {
        if ((bytes == null) || (bytes.length < offs + len) || (bytes.length <1)){
            Log.error("Implausible packet for encryption ");
            if (bytes == null) {
                Log.error("null buffer");
            }else {
                Log.error("Length ="+bytes.length+" len ="+len+" offs="+offs);
            }
            return;
        }
        try {
            DatagramPacket p = new DatagramPacket(bytes, offs, len, _logrec.getLocalSocketAddress());
            _logsend.send(p);
        } catch (Exception x) {
            Log.error("can't leak to " + _logrec.getLocalSocketAddress());
            x.printStackTrace();
        }
        _dtls.send(bytes, offs, len);
    }

    @Override
    public void close() throws IOException {
        _dtls.close();
        _logrec.close();
        _logsend.close();
    }

}
