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

import com.phono.srtplight.Log;
import pe.pi.sctp4j.sctp.messages.exceptions.SctpPacketFormatException;
import pe.pi.sctp4j.sctp.messages.params.VariableParam;
import java.nio.ByteBuffer;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class HeartBeatChunk extends Chunk {

    public HeartBeatChunk() throws SctpPacketFormatException {
        super((byte) HEARTBEAT);
        HeartbeatInfo req = new HeartbeatInfo(1, "HeartbeatInfo");
        String t = ""+ System.currentTimeMillis();
        req.setData(t.getBytes());
        Log.debug("adding "+req+" to "+this);
        _varList.add(req);
        validate();
    }

    public HeartBeatChunk(byte type, byte flags, int length, ByteBuffer pkt) {
        super(type, flags, length, pkt);
        if (_body.remaining() >= 4) {
            while (_body.hasRemaining()) {
                VariableParam v = readVariable();
                _varList.add(v);
            }
        }
    }

    @Override
    public void validate() throws SctpPacketFormatException {
        VariableParam hbd;
        if ((_varList == null) || (_varList.size() != 1)) {
            throw new SctpPacketFormatException("No (or too much content in this heartbeat packet");
        }
        hbd = _varList.get(0);
        if (!(hbd instanceof HeartbeatInfo)) {
            throw new SctpPacketFormatException("Expected a heartbeatinfo in this packet");
        }
    }

    public Chunk[] mkReply() {
        Chunk[] rep = new Chunk[1];
        HeartBeatAckChunk dub = new HeartBeatAckChunk();
        dub._varList = this._varList;
        rep[0] = dub;
        return rep;
    }

    @Override
    void putFixedParams(ByteBuffer ret) {
        // none
    }

}
