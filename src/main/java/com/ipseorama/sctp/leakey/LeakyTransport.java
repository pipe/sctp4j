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
package com.ipseorama.sctp.leakey;

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
            Log.debug("Leaking to send address " + s.getHostString() + ":" + s.getPort());
            InetSocketAddress r = (InetSocketAddress) _logrec.getLocalSocketAddress();
            Log.debug("Leaking to recv address " + r.getHostString() + ":" + r.getPort());
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
