/*
 * Copyright 2017 pi.pe gmbh .
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package pe.pi.sctp4j.sctp.small;

import pe.pi.sctp4j.sctp.SCTPTimer;
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
