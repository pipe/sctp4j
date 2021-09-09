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
package pe.pi.sctp4j.sctp.small;

import pe.pi.sctp4j.sctp.Association;
import pe.pi.sctp4j.sctp.behave.SCTPStreamBehaviour;
import pe.pi.sctp4j.sctp.behave.UnorderedStreamBehaviour;
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
        super((ThreadedAssociation)a, id);
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


    private SCTPStreamBehaviour mkBehave() {
        return new UnorderedStreamBehaviour();
    }
}
