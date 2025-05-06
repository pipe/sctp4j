
package pe.pi.sctp4j.sctp.messages;

import java.nio.ByteBuffer;

/**
 *
 * @author thp
 */

/*
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |   Type = 192  |  Flags = 0x00 |        Length = Variable      |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                      New Cumulative TSN                       |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |         Stream-1              |       Stream Sequence-1       |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   \                                                               /
   /                                                               \
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |         Stream-N              |       Stream Sequence-N       |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

   Chunk Flags:

     Set to all zeros on transmit and ignored on receipt.

   New Cumulative TSN: 32 bit u_int

    This indicates the new cumulative TSN to the data receiver.  Upon
    the reception of this value, the data receiver MUST consider
    any missing TSNs earlier than or equal to this value as received,
    and stop reporting them as gaps in any subsequent SACKs.

   Stream-N: 16 bit u_int

    This field holds a stream number that was skipped by this
    FWD-TSN.

   Stream Sequence-N: 16 bit u_int

    This field holds the sequence number associated with the stream
    that was skipped.  The stream sequence field holds the largest
    stream sequence number in this stream being skipped.  The receiver
    of the FWD-TSN's can use the Stream-N and Stream Sequence-N fields
    to enable delivery of any stranded TSN's that remain on the stream
    re-ordering queues.  This field MUST NOT report TSN's corresponding
    to DATA chunks that are marked as unordered.  For ordered DATA
    chunks this field MUST be filled in.



Stewart, et al.             Standards Track                     [Page 6]
*/
public class ForwardTSNChunk extends Chunk {

    public ForwardTSNChunk(byte type, byte flags, int length, ByteBuffer pkt) {
                super(type, flags, length, pkt);
    }
    public ForwardTSNChunk() {
        super((byte) Chunk.FORWARD_TSN);
    }
    @Override
    void putFixedParams(ByteBuffer ret) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
}
