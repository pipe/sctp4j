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
package pe.pi.sctp4j.sctp.messages;

import pe.pi.sctp4j.sctp.messages.params.AddIncomingStreamsRequestParameter;
import pe.pi.sctp4j.sctp.messages.params.AddOutgoingStreamsRequestParameter;
import pe.pi.sctp4j.sctp.messages.params.IncomingSSNResetRequestParameter;
import pe.pi.sctp4j.sctp.messages.params.OutgoingSSNResetRequestParameter;
import pe.pi.sctp4j.sctp.messages.params.ReconfigurationResponseParameter;
import pe.pi.sctp4j.sctp.messages.params.VariableParam;
import com.phono.srtplight.Log;
import java.nio.ByteBuffer;

/**
 *
 * @author thp
 */
public class ReConfigChunk extends Chunk {

    private long sentAt;
    private int retries;

    public ReConfigChunk(byte type, byte flags, int length, ByteBuffer pkt) {
        super(type, flags, length, pkt);
        Log.debug("ReConfig chunk" + this.toString());
        if (_body.remaining() >= 4) {
            while (_body.hasRemaining()) {
                VariableParam v = this.readVariable();
                _varList.add(v);
                Log.debug("\tParam :" + v.toString());
            }
        }
    }

    public ReConfigChunk() {
        super((byte) Chunk.RE_CONFIG);
    }

    @Override
    void putFixedParams(ByteBuffer ret) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean hasIncomingReset() {
        return _varList.stream().anyMatch((VariableParam v) -> {
            return v instanceof IncomingSSNResetRequestParameter;
        });
    }

    public IncomingSSNResetRequestParameter getIncomingReset() {
        return _varList.stream().filter((VariableParam v) -> {
            return v instanceof IncomingSSNResetRequestParameter;
        })
                .map((VariableParam v) -> {
                    return (IncomingSSNResetRequestParameter) v;
                })
                .findFirst().orElse(null);
    }

    public boolean hasOutgoingReset() {
        return _varList.stream().anyMatch((VariableParam v) -> {
            return v instanceof OutgoingSSNResetRequestParameter;
        });
    }

    private boolean hasOutgoingAdd() {
        return _varList.stream().anyMatch((VariableParam v) -> {
            return v instanceof AddOutgoingStreamsRequestParameter;
        });
    }

    private boolean hasResponse() {
        return _varList.stream().anyMatch((VariableParam v) -> {
            return v instanceof ReconfigurationResponseParameter;
        });
    }

    public OutgoingSSNResetRequestParameter getOutgoingReset() {
        return _varList.stream().filter((VariableParam v) -> {
            return v instanceof OutgoingSSNResetRequestParameter;
        })
                .map((VariableParam v) -> {
                    return (OutgoingSSNResetRequestParameter) v;
                })
                .findFirst().orElse(null);
    }

    public boolean hasParam() {
        return !_varList.isEmpty();
    }

    /*
       1.   Outgoing SSN Reset Request Parameter.

   2.   Incoming SSN Reset Request Parameter.

   3.   Outgoing SSN Reset Request Parameter, Incoming SSN Reset Request
        Parameter.

   4.   SSN/TSN Reset Request Parameter.

   5.   Add Outgoing Streams Request Parameter.

   6.   Add Incoming Streams Request Parameter.

   7.   Add Outgoing Streams Request Parameter, Add Incoming Streams
        Request Parameter.

   8.   Re-configuration Response Parameter.

   9.   Re-configuration Response Parameter, Outgoing SSN Reset Request
        Parameter.

   10.  Re-configuration Response Parameter, Re-configuration Response
        Parameter.
     */
    @Override
    public void validate() {
        if (_varList.size() < 1) {
            throw new IllegalArgumentException("Too few params " + _varList.size());
        }
        if (_varList.size() > 2) {
            throw new IllegalArgumentException("Too many params " + _varList.size());
        }
        // now check for invalid combos
        if ((_varList.size() == 2)) {
            if (this.hasOutgoingReset()) {
                VariableParam remain = _varList.stream().filter((VariableParam v) -> {
                    return !(v instanceof OutgoingSSNResetRequestParameter);
                }).findFirst().orElse(null);
                if (remain == null) {
                    throw new IllegalArgumentException("2 OutgoingSSNResetRequestParameter in one Chunk not allowed ");
                }
                if (!((remain instanceof IncomingSSNResetRequestParameter) //3
                        || (remain instanceof ReconfigurationResponseParameter))) //9
                {
                    throw new IllegalArgumentException("OutgoingSSNResetRequestParameter and " + remain.getClass().getSimpleName() + " in same Chunk not allowed ");
                }
            } else if (this.hasOutgoingAdd()) {
                VariableParam remain = _varList.stream().filter((VariableParam v) -> {
                    return !(v instanceof AddOutgoingStreamsRequestParameter);
                }).findFirst().orElse(null);
                if (remain == null) {
                    throw new IllegalArgumentException("2 AddOutgoingStreamsRequestParameter in one Chunk not allowed ");
                }
                if (!(remain instanceof AddIncomingStreamsRequestParameter)) //7
                {
                    throw new IllegalArgumentException("OutgoingSSNResetRequestParameter and " + remain.getClass().getSimpleName() + " in same Chunk not allowed ");
                }
            } else if (this.hasResponse()) {
                VariableParam remain = _varList.stream().filter((VariableParam v) -> {
                    return !(v instanceof ReconfigurationResponseParameter);
                }).findFirst().orElse(null);
                if (remain != null) { // 10
                    throw new IllegalArgumentException("ReconfigurationResponseParameter and " + remain.getClass().getSimpleName() + " in same Chunk not allowed ");
                }
            }
        } // implicitly just one - which is ok 1,2,4,5,6,8
    }

    public void addParam(VariableParam rep) {
        Log.debug("adding "+rep+" to "+this);
        _varList.add(rep);
        validate();
    }

    public boolean sameAs(ReConfigChunk other) {
        // we ignore other var types for now....
        boolean ret = false; // assume the negative.
        if (other != null) {
            // if there are 2 params and both match
            if ((this.hasIncomingReset() && other.hasIncomingReset())
                    && (this.hasOutgoingReset() && other.hasOutgoingReset())) {
                ret = this.getIncomingReset().sameAs(other.getIncomingReset())
                        && this.getOutgoingReset().sameAs(other.getOutgoingReset());
            } else {
                // there is only one (of these) params
                // that has to match too
                if (this.hasIncomingReset() && other.hasIncomingReset()) {
                    ret = this.getIncomingReset().sameAs(other.getIncomingReset());
                }
                if (this.hasOutgoingReset() && other.hasOutgoingReset()) {
                    ret = this.getOutgoingReset().sameAs(other.getOutgoingReset());
                }
            }
        }
        return ret;
    }
// stuff to manage outbound retries
    public long getSentTime(){
        return sentAt;
    }
    public void setSentTime(long now){
        sentAt = now;
    }
    public int getAndIncrementRetryCount(){
        return retries++;
    }

}
