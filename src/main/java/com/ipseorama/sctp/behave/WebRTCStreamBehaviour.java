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
 * behaviour if we are a webRTC endpoint expecting a DCOpen
 * 
 * @author tim
 */
public class WebRTCStreamBehaviour implements SCTPStreamBehaviour {

    @Override
    public Chunk[] respond(SCTPStream s) {
        return null; // so we don't respond until we see the open.
        // assumes a datachannel TODO - undo that....
    }

    public void deliver(SCTPStream s, SortedSet<DataChunk> a, SCTPStreamListener l) {
        Log.error("In default deliver - shouldn't happen - stream " + s.getLabel());
    }
}
