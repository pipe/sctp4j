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
        return Math.min(1480,_ds.getReceiveBufferSize());
    }

    @Override
    public int getSendLimit() throws IOException {
        return Math.min(1480,_ds.getSendBufferSize());
    }



    public int receive(byte[] buf, int off, int len, int waitMillis) throws IOException {
        int ret = 0;
        _ds.setSoTimeout(waitMillis);

        if (!_isShutdown) {
            DatagramPacket p = new DatagramPacket(buf, off, len);
            _ds.receive(p);
            Log.debug("recv'd " + p.getLength());
            ret = p.getLength();
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

}
