/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package pe.pi.sctp4j.sctp.messages;

import java.nio.ByteBuffer;

/**
 *
 * @author thp
 */
public class IForwardTSNChunk extends ForwardTSNChunk {

    public IForwardTSNChunk(byte type, byte flags, int length, ByteBuffer pkt) {
        super(type, flags, length, pkt);
    }

}
