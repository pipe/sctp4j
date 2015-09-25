/*
 * Copyright (C) 2014 Westhawk Ltd<thp@westhawk.co.uk>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.ipseorama.sctp.small;

import com.ipseorama.sctp.SCTPTimer;
import com.phono.srtplight.Log;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author tim
 * Assumption is that timers _always_ go off - it is up to the 
 * runnable to decide if something needs to be done or not.
 */
class SimpleSCTPTimer implements SCTPTimer {
    private Timer _timer;

    public SimpleSCTPTimer() {
        _timer = new Timer();        
    }

    @Override
    public void setRunnable(Runnable r, long at) {
        final Runnable torun = r;
        TimerTask tick = new TimerTask(){
            @Override
            public void run() {
                torun.run();
            }         
        };
        try {
            _timer.schedule(tick, at);
        } catch (IllegalStateException x) {
            Log.warn("Stupid Java7 timer died with "+x.getMessage()+" creating a new one");
            _timer = new Timer();
            _timer.schedule(tick, at);
        }
    }
    
}
