/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pe.pi.sctp4j.sctp.dummy;

import pe.pi.sctp4j.sctp.Association;
import pe.pi.sctp4j.sctp.SCTPMessage;
import pe.pi.sctp4j.sctp.SCTPStream;
import pe.pi.sctp4j.sctp.dataChannel.DECP.DCOpen;
import pe.pi.sctp4j.sctp.messages.DataChunk;

/**
 * Concrete stream which mocks the abstract methods - used as a place holder in tests etc
 * @author tim
 */
public class DummyStream extends SCTPStream{

    public DummyStream(Association a, Integer id) {
        super(a, id);
    }

    @Override
    public void delivered(DataChunk d) {
    }

    @Override
    public void send(String message) throws Exception {
    }

    @Override
    public void send(byte[] message) throws Exception {
    }

    @Override
    public void send(DCOpen message) throws Exception {
    }

    @Override
    public void deliverMessage(SCTPMessage message) {
    }
    
}
