/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.device.endpoints;

import com.ipseorama.sctp.SCTPStream;
import com.ipseorama.sctp.SCTPStreamListener;
import com.phono.srtplight.Log;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.*;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public abstract class JsonEndpoint implements SCTPStreamListener, JsonFace {

    SCTPStream _stream;

    public JsonEndpoint(SCTPStream s) {
        _stream = s;
    }

    public void sendJson(JsonObject jo) throws Exception {
        Writer jstream = new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                Log.debug("write on fake json stream");
                int sent = 0;
                while (sent < len) {
                    int chunk = len - sent;
                    if (chunk >= Character.MAX_VALUE) {
                        chunk = Character.MAX_VALUE - 1;
                    }
                    String j = new String(cbuf, off + sent, chunk);
                    try {
                        _stream.send(j);
                    } catch (Exception ex) {
                        throw new IOException(ex.getMessage());
                    }
                    sent += chunk;
                }
            }

            @Override
            public void flush() throws IOException {
                Log.debug("fluah on fake json stream");

            }

            @Override
            public void close() throws IOException {
                Log.debug("close on fake json stream");

            }

        };
        JsonWriter jsonWriter = Json.createWriter(jstream);
        jsonWriter.writeObject(jo);

    }

    @Override
    public void onMessage(SCTPStream s, String message) {
        try {
            Log.debug("got json message " + message);
            ByteArrayInputStream bin = new ByteArrayInputStream(message.getBytes());
            JsonReader reader = Json.createReader(bin);
            JsonObject messj = (JsonObject) reader.read();
            JsonObject reply = onJsonMessage(messj);
            sendJson(reply);
        } catch (Exception ex) {
            Log.error("cant deal with Json message to " + s.getLabel());
            ex.printStackTrace();
        }
    }

    @Override
    public void close(SCTPStream aThis) {
        Log.debug("Closed an json stream");
    }

}
