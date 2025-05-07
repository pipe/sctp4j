/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package pe.pi.sctp4j.sctp.behave;

import com.phono.srtplight.Log;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import pe.pi.sctp4j.sctp.SCTPMessage;
import pe.pi.sctp4j.sctp.SCTPStream;
import pe.pi.sctp4j.sctp.SCTPStreamListener;
import pe.pi.sctp4j.sctp.messages.Chunk;
import pe.pi.sctp4j.sctp.messages.DataChunk;
import pe.pi.sctp4j.sctp.messages.IDataChunk;

/**
 *
 * @author thp
 */
public class IOrderedStreamBehaviour implements SCTPStreamBehaviour {

    protected boolean ordered = true;
    protected int expectedMessageNo = 1; // dcep is the zeroth message
    private final HashMap<Integer, MessageHolder> messages = new HashMap();
    private final Comparator<IDataChunk> fsnComparator;

    public IOrderedStreamBehaviour() {
        fsnComparator = (IDataChunk o1, IDataChunk o2) -> (o1.getFsn() - o2.getFsn());
    }

    @Override
    public void deliver(SCTPStream s, SortedSet<DataChunk> stash, SCTPStreamListener l) {
        //stash is the list of all unallocated data chunks.
        //we move all of them into their respective messages
        //and deliver the complete ones.
        //holding onto the incomplete ones 'till next time.
        MessageHolder message;

        for (DataChunk dc : stash) {
            if (dc instanceof IDataChunk) {
                var idc = (IDataChunk) dc;
                Integer mid = idc.getMid();
                message = messages.get(mid);
                if (message == null) {
                    message = new MessageHolder();
                    messages.put(mid, message);
                    Log.info("start collecting for message " + mid);
                }
                message.addFrag(idc);
            } else {
                Log.error("Wrong sort of chunk");
            }
        }
        Log.info("want message " + expectedMessageNo);

        if (ordered) {
            while (messages.keySet().contains(expectedMessageNo)) {
                Log.info("found message " + expectedMessageNo);
                var m = messages.get(expectedMessageNo);
                if (m.isDeliverable()) {
                    Log.info("deliverable message " + expectedMessageNo);
                    var all = m.getFrags();
                    var sm = new SCTPMessage(s, all);
                    if (sm.deliver(l)) {
                        Log.info("delivered message " + expectedMessageNo);
                        messages.remove(expectedMessageNo);
                        expectedMessageNo++;
                    } else {
                        Log.info("didn't deliver message " + expectedMessageNo);
                        break;
                    }
                } else {
                    Log.info("incomplete message " + expectedMessageNo);
                    break;
                }
            }
        } else {
            var delivered = messages.entrySet().stream()
                    .filter((e) -> e.getValue().isDeliverable())
                    .filter((d) -> {
                        Log.info("deliverable message " + d.getKey());
                        var all = d.getValue().getFrags();
                        var sm = new SCTPMessage(s, all);
                        var ret = sm.deliver(l);
                        if (ret) {
                            Log.info("delivered "+d.getKey());
                        }
                        return ret;
                    }).map((k)->k.getKey()).toList();
            delivered.forEach((x)->messages.remove(x));
        }
        stash.clear();
    }

    @Override
    public Chunk[] respond(SCTPStream a) {
        return null;
    }

    class MessageHolder {

        HashMap<Integer, IDataChunk> frags = new HashMap();
        boolean hasStart = false;
        boolean hasEnd = false;
        int expectedLen = 0;
        int actualLen;

        void addFrag(IDataChunk idc) {
            int flags = idc.getFlags() & DataChunk.SINGLEFLAG; // mask to the bits we want
            switch (flags) {
                case DataChunk.SINGLEFLAG:
                    hasStart = true;
                    hasEnd = true;
                    expectedLen = 1;
                    break;
                case DataChunk.BEGINFLAG:
                    hasStart = true;
                    break;
                case DataChunk.ENDFLAG:
                    hasEnd = true;
                    expectedLen = idc.getFsn() + 1;
                    break;
            }
            frags.put(idc.getFsn(), idc);
            actualLen = frags.keySet().size();
        }

        boolean isDeliverable() {
            return (hasEnd && hasStart && (expectedLen == actualLen));
        }

        List<IDataChunk> getFrags() {
            return frags.values().stream().sorted(fsnComparator).toList();
        }
    };
}
