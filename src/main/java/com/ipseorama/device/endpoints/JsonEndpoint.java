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
import java.io.StringWriter;
import javax.json.*;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
abstract class JsonEndpoint implements SCTPStreamListener {

    SCTPStream _stream;

    public JsonEndpoint(SCTPStream s) {
        _stream = s;
    }
    public void sendJson(JsonObject jo) throws Exception{
        StringWriter stWriter = new StringWriter();
        try (JsonWriter jsonWriter = Json.createWriter(stWriter)) {
            jsonWriter.writeObject(jo);
        }
        String jsonData = stWriter.toString();
        Log.debug("sending : "+jsonData);
        _stream.send(jsonData);
    }
    @Override
    public void onMessage(SCTPStream s, String message) {
        try {
            Log.debug("got json message "+message);
            ByteArrayInputStream bin = new ByteArrayInputStream(message.getBytes());
            JsonReader reader = Json.createReader(bin);
            JsonObject messj = (JsonObject) reader.read();
            JsonObject reply = onJsonMessage(messj);
            sendJson(reply);
        } catch (Exception ex) {
            Log.error("cant deal with Json message to " + s.getLabel());
        }
    }

    protected abstract JsonObject  onJsonMessage(JsonObject messj) ;

}
