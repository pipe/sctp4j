/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.sctp.behave;

import com.ipseorama.sctp.SCTPStream;
import com.ipseorama.sctp.SCTPStreamListener;
import com.ipseorama.sctp.messages.Chunk;
import com.ipseorama.sctp.messages.DataChunk;
import com.phono.srtplight.Log;
import java.util.Set;
import java.util.SortedSet;

/**
 *
 * @author tim
 * what DCEPS do 
 */
public class DCEPStreamBehaviour implements
        SCTPStreamBehaviour {

    @Override
    public Chunk[] respond(SCTPStream a) {
        Log.debug("in respond() for a opened stream " + a.getLabel());
        return null;
    }

    @Override
    public void deliver(SCTPStream s, SortedSet<DataChunk> a, SCTPStreamListener l) {
        Log.debug("in deliver() for stream " + s.getLabel() + " with " + a.size() + " chunks. ");
        // strictly this should be looking at flags etc, and bundling the result into a message
        for (DataChunk dc : a) {
            if (dc.getDCEP() != null) {
                Log.debug("in deliver() for a DCEP message " + dc.getDataAsString());
            } else {
                Log.debug("inbound data chunk is " + dc.toString());
                l.onMessage(s, dc.getDataAsString());
            }
        }
        a.clear();
    }

}
