/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.base.dataChannel;

import com.phono.srtplight.Log;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.ice4j.TransportAddress;

/**
 *
 * @author tim
 */
public class PlainDatagramTransport implements org.bouncycastle.crypto.tls.DatagramTransport {

    private final DatagramSocket _ds;
    private final TransportAddress _dest;
    private boolean _isShutdown;

    PlainDatagramTransport(DatagramSocket lds, TransportAddress rta) {
        _ds = lds;
        _dest = rta;
    }

    public void doShutdown() {
        _isShutdown = true;
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
        _ds.setSoTimeout(waitMillis);

        if (!_isShutdown) {
            DatagramPacket p = new DatagramPacket(buf, off, len);
            _ds.receive(p);
            Log.debug("recv'd " + p.getLength());
            ret = p.getLength();
            if (ret > 0) {
                boolean ok = checkPacketIsDTLS(buf[0]);
                if (!ok) {
                    ret = 0;
                }
            }
        }
        return ret;
    }

    @Override
    public void send(byte[] buf, int off, int len) throws IOException {
        DatagramPacket p = new DatagramPacket(buf, off, len, _dest);
        _ds.send(p);
        Log.debug("sent " + p.getLength() + " to " + _dest.toString());

    }

    @Override
    public void close() throws IOException {
        _isShutdown = true;
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
