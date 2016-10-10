/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.device.endpoints;

import com.ipseorama.sctp.SCTPStream;
import com.ipseorama.sctp.SCTPStreamListener;
import com.phono.srtplight.Log;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class EchoEndpoint implements SCTPStreamListener {

    public EchoEndpoint() {
        Log.debug("Making an echo stream");
    }

    @Override
    public void onMessage(SCTPStream s, String message) {
        try {
            s.send(message);
            Log.verb("echo message " + message + " to " + s.getLabel());
        } catch (Exception ex) {
            Log.error("cant echo message to " + s.getLabel());
        }
    }

    @Override
    public void close(SCTPStream aThis) {
        Log.debug("Closed an echo stream");
    }

}
