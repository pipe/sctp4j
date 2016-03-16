/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.base.dataChannel;

import com.phono.srtplight.Log;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
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
        return 4094; //Math.min(1200,_ds.getReceiveBufferSize());
    }

    @Override
    public int getSendLimit() throws IOException {
        return 4096;// Math.min(1200,_ds.getSendBufferSize());
    }

    @Override
    public void run() {
        byte buffer[] = new byte[0];
        Log.debug("recv queue starting ");
        try {
            buffer = new byte[this.getReceiveLimit()];
            _ds.setSoTimeout(1000);
        } catch (IOException ex) {
            _isShutdown = true;
            Log.debug("problem in starting recv thread " + ex.getMessage());
        }
        while (!_isShutdown) {
            try {

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

            } catch (InterruptedIOException ex) {
                Log.verb("tick... ");
            } catch (IOException ex) {
                Log.verb("problem in running recv thread " + ex.getMessage());
            }
        }
        Log.debug("shutdown - rcv thread finishing");
    }

    public int receive(byte[] buf, int off, int len, int waitMillis) throws IOException {
        int ret = 0;
        if (!_isShutdown || (_packetQueue.peek() != null)) {
            try {
                Log.verb("recv " + waitMillis);
                byte pkt[] = _packetQueue.poll(waitMillis, TimeUnit.MILLISECONDS);
                if (pkt != null) {
                    ret = Math.min(len, pkt.length);
                    System.arraycopy(pkt, 0, buf, off, ret);
                }
            } catch (InterruptedException ex) {
                Log.debug("recv interrupted ");
                throw new java.io.InterruptedIOException(ex.getMessage());
            }
        } else {
            Log.debug("Transport  shutdown - throw exception.");
            throw new java.io.EOFException("Transport was shutdown.");
        }
        return ret;
    }

    @Override
    public void send(byte[] buf, int off, int len) throws IOException {
        if (_isShutdown) {
            Log.debug("Transport  shutdown - throw exception.");
            throw new java.io.EOFException("transport shut.");
        }
        DatagramPacket p = new DatagramPacket(buf, off, len, _dest);
        _ds.send(p);
        Log.debug("sent " + p.getLength() + " to " + _dest.toString());

    }

    @Override
    public void close() throws IOException {
        if (_isShutdown) {
            Log.debug("Transport  already shutdown - throw exception.");
            throw new java.io.EOFException("transport shut.");
        }
        Log.debug("Transport  shutdown.");
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
