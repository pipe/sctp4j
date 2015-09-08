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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ice4j.TransportAddress;

/**
 *
 * @author tim
 */
public class QueuingDatagramTransport implements org.bouncycastle.crypto.tls.DatagramTransport, Runnable {

    private final DatagramSocket _ds;
    private final TransportAddress _dest;
    private boolean _isShutdown;
    private Thread _recvEnqueue;
    BlockingQueue<byte[]> _packetQueue = new ArrayBlockingQueue<>(10);

    QueuingDatagramTransport(DatagramSocket lds, TransportAddress rta) {
        _ds = lds;
        _dest = rta;
        String tname = "rcv-" + lds.getLocalSocketAddress().toString();
        _recvEnqueue = new Thread(this, tname);
        _recvEnqueue.start();
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

    @Override
    public void run() {
        try {
            Log.debug("recv queue starting ");
            byte buffer[] = new byte[this.getReceiveLimit()];
            _ds.setSoTimeout(1000);

            while (!_isShutdown) {
                DatagramPacket p = new DatagramPacket(buffer, 0, buffer.length);
                _ds.receive(p);
                Log.debug("recv'd " + p.getLength());
                int ret = p.getLength();
                if (ret > 0) {
                    boolean ok = checkPacketIsDTLS(buffer[0]);
                    if (!ok) {
                        ret = 0;
                    }
                }
                if (ret > 0) {
                    byte stack[] = new byte[ret];
                    System.arraycopy(buffer, 0, stack, 0, ret);
                    boolean res = _packetQueue.offer(stack);
                    if (!res) {
                        Log.debug("overflowed stack");
                    }
                }
            }
        } catch (IOException ex) {
            Log.debug("problem in recv thread " + ex.getMessage());
        }
    }

    public int receive(byte[] buf, int off, int len, int waitMillis) throws IOException {
        int ret = 0;
        try {
            Log.debug("recv ");
            byte pkt[] = _packetQueue.poll(waitMillis, TimeUnit.MILLISECONDS);
            if (pkt != null) {
                ret = Math.min(len, pkt.length);
                System.arraycopy(pkt, 0, buf, off, ret);
            }
        } catch (InterruptedException ex) {
            Log.debug("recv interrupted ");
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
