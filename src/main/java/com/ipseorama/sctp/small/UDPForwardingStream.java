/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.sctp.small;

import com.ipseorama.sctp.Association;
import com.ipseorama.sctp.SCTPMessage;
import com.ipseorama.sctp.behave.SCTPStreamBehaviour;
import com.ipseorama.sctp.behave.UnreliableStreamBehaviour;
import com.phono.srtplight.Log;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 *
 * @author tim
 */
public class UDPForwardingStream extends BlockingSCTPStream implements Runnable {

    DatagramSocket _udpSock;
    private final Thread _rcv;

    public UDPForwardingStream(Association a, Integer id, Integer toPort) throws SocketException {
        super(a, id);
        _udpSock = new DatagramSocket();
        SocketAddress addr = new InetSocketAddress("localhost", toPort.shortValue());
        _udpSock.bind(addr);
        _rcv = new Thread(this, "UDPForwarding_rcv");
        _rcv.start();
        SCTPStreamBehaviour behave = mkBehave();
        super.setBehave(behave);
    }

    @Override
    public void run() {
        try {
            _udpSock.setSoTimeout(1000);
                byte buff[] = new byte[4096];
                DatagramPacket dgp = new DatagramPacket(buff, buff.length);
            while (_rcv != null) {
                try {
                    _udpSock.receive(dgp);
                    int l = dgp.getLength();
                    if (l > buff.length) {
                        Log.warn("truncated packet from " + _udpSock.getRemoteSocketAddress().toString());
                        l = buff.length;
                    }
                    byte pkt[] = new byte[l];
                    System.arraycopy(buff, 0, pkt, 0, l);
                    send(pkt);
                } catch (SocketTimeoutException stx) {
                    ; // ignore - lets us check for close....
                }
            }
        } catch (Exception x) {

        }
        // clean up here.....
    }

    synchronized private void send(byte[] buff) throws Exception {
        Association a = super.getAssociation();
        SCTPMessage m = a.makeMessage(buff, this);
        a.sendAndBlock(m);
    }

    private SCTPStreamBehaviour mkBehave() {
        return new UnreliableStreamBehaviour();
    }
}
