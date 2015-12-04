/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ipseorama.device.endpoints;

import com.ipseorama.sctp.SCTPStream;
import com.ipseorama.sctp.SCTPStreamListener;
import com.phono.srtplight.Log;
import java.util.Timer;
import java.util.TimerTask;


/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class TickEndpoint implements SCTPStreamListener {
    Timer ticker;
    public TickEndpoint(SCTPStream s) {
        Log.debug("Made Tick endpoint");
        final SCTPStream fs =s ;
        TimerTask tt = new TimerTask(){
            @Override
            public void run() {
                try {
                    fs.send(""+System.currentTimeMillis());
                } catch (Exception ex) {
                    Log.error("cant send tick");
                    ticker.cancel();
                }
            }
        };
        ticker = new Timer();
        ticker.schedule(tt, 1000, 1000);
    }

    @Override
    public void onMessage(SCTPStream s, String message) {
        Log.debug("Ignoring "+message);
    }
    
}
