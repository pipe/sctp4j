/*
 */
package pe.pi.sctp4j.sctp.messages;

import com.phono.srtplight.Log;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author thp
 */
public class IForwardTSNChunk extends Chunk {

    private static int maxexps = 128; // gotta fit in a packet.
    /*
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |   Type = 194  | Flags = 0x00  |      Length = Variable        |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                       New Cumulative TSN                      |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |       Stream Identifier       |          Reserved           |U|
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                       Message Identifier                      |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   \                                                               \
   /                                                               /
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |       Stream Identifier       |          Reserved           |U|
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                       Message Identifier                      |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                   Figure 4: I-FORWARD-TSN Chunk Format
    */
    private SUM[] sums;
    private long newTSN;
    
    public IForwardTSNChunk(byte type, byte flags, int length, ByteBuffer pkt) {
        super(type, flags, length, pkt);
    }
    public IForwardTSNChunk() {
        super((byte) Chunk.I_FORWARD_TSN);
    }
    @Override
    void putFixedParams(ByteBuffer ret) {
        Chunk.putUnsignedInt(ret, newTSN);
        for (SUM sum : sums) {
            ret.putChar((char) sum.stream);
            ret.putChar((char) (sum.unordered ? 1 : 0));
            Chunk.putUnsignedInt(ret, sum.mid);
        }
    }
    class SUM {
        int stream;
        boolean unordered;
        long mid;
    }
    public void setExpired(ArrayList<IDataChunk> exds){
        int exn = Math.min(exds.size(),maxexps);
        sums = new SUM[exn];
        List<IDataChunk> oexds = exds.stream().sorted().toList();
        newTSN = oexds.getFirst()._tsn;
        if (newTSN < oexds.getLast()._tsn){
            Log.error("IDATACHUNK Sorted wrong!");
        }
        List<IDataChunk> loexds = oexds.subList(0, exn-1);
        for (int i=0;i<loexds.size();i++){
            IDataChunk lo = loexds.get(i);
            var s = new SUM();
            s.mid = lo.getMid();
            s.stream = lo.getStreamId();
            s.unordered = ! lo.isOrdered(); 
            sums[i] =s;
        }
        exds.removeAll(loexds);
    }
}
