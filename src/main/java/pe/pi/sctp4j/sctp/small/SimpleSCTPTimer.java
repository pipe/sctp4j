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

import com.phono.srtplight.Log;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author tim Only one queued invocation. New requests replace queued ones if
 * they are earlier.
 */
abstract class SimpleSCTPTimer {
    
    protected static Timer _timer = new Timer("SCTPTimer", true);
    static int tno = 1;
    long scheduledAt = Long.MAX_VALUE;
    TimerTask task = null;
    
    public abstract void tick();
    
    public void setNextRun(long by) {
        long now = System.currentTimeMillis();
        long when = now + by;
        if (when < scheduledAt) {
            if (task != null) {
                task.cancel();
                Log.verb("cancelled future task scheduled for " + scheduledAt + " because new task at " + when);
            }
            task = new TimerTask() {
                @Override
                public void run() {
                    try {
                        Log.verb("running task");
                        tick();
                        scheduledAt = Long.MAX_VALUE;
                    } catch (Throwable t) {
                        Log.error("SCTPTimerTask threw Exception " + t);
                        if (Log.getLevel() >= Log.DEBUG) {
                            t.printStackTrace();
                        }
                    }
                }
            };
            _timer.schedule(task, by);
            Log.verb("SCTPTimer task now scheduled at " + when);
            scheduledAt = when;
        } else {
            Log.verb ("already have a task scheduled for "+scheduledAt+" which is earlier than "+when);
        }
    }
    
}
