/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.sctp.lossy;

import com.phono.srtplight.Log;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.security.SecureRandom;
import org.ice4j.TransportAddress;

/**
 * A lossy datagram transport - it loses a %age of packets
 *
 * @author tim
 */
public class LossyDatagramTransport implements org.bouncycastle.crypto.tls.DatagramTransport {

    private final DatagramSocket _ds;
    private final TransportAddress _dest;
    private final int _inLoss;
    private final int _outLoss;
    private SecureRandom _rand = new SecureRandom();

    public LossyDatagramTransport(DatagramSocket lds, TransportAddress rta, int inLoss, int outLoss) {
        _ds = lds;
        _dest = rta;
        _inLoss = inLoss;
        _outLoss = outLoss;
        Log.warn("Lossy DatagramTransport in place in =" + inLoss + "% loss out = " + outLoss + "% loss.");

    }

    @Override
    public int getReceiveLimit() throws IOException {
        return 1200;
    }

    @Override
    public int getSendLimit() throws IOException {
        return 1200;
    }

    public int receive(byte[] buf, int off, int len, int waitMillis) throws IOException {
        int ret = 0;
        byte buffer[] = new byte[Math.min(this.getReceiveLimit(),len)];
        _ds.setSoTimeout(waitMillis);
        DatagramPacket p = new DatagramPacket(buffer, 0, buffer.length);
        _ds.receive(p);
        Log.debug("recv'd " + p.getLength());

        ret = p.getLength();
        if (ret > 0) {
            boolean ok = checkPacketIsDTLS(buffer[0]);
            if (!ok) {
                ret = 0;
            }
        }
        if (_rand.nextInt(100) <= _inLoss) {
            ret = 0;
            Log.debug("Discarding inbound packet.");
        } else {
            System.arraycopy(buffer, 0, buf, off, ret);
        }
        return ret;
    }

    @Override
    public void send(byte[] buf, int off, int len) throws IOException {
        DatagramPacket p = new DatagramPacket(buf, off, len, _dest);
        if (_rand.nextInt(100) > _outLoss) {
            _ds.send(p);
            Log.debug("sent " + p.getLength() + " to " + _dest.toString());
        } else {
            Log.debug("Discarding outbound packet");
        }
    }

    @Override
    public void close() throws IOException {
        _ds.close();
    }

    private boolean checkPacketIsDTLS(byte first) {
        boolean dtls = false;
        int b = 0x7f & first;
        if (first < 0) {
            b += 128;
        }

        /*
         RFC
         +----------------+
         | 127 < B < 192 -+--> forward to RTP
         |                |
         packet-->|  19 < B < 64  -+--> forward to DTLS
         |                |
         |       B < 2   -+--> forward to STUN
         +----------------+
         */
        if ((b > 127) && (b < 192)) {
            Log.debug("RTP packet!");
        } else if ((b > 19) && (b < 64)) {
            Log.debug("DTLS packet!");
            dtls = true;
        } else if (b < 2) {
            Log.debug("STUN packet!");
        }
        return dtls;
    }

}
