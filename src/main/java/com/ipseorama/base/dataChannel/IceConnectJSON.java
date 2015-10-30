/*
 * Copyright (C) 2015 Westhawk Ltd<thp@westhawk.co.uk>
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
package com.ipseorama.base.dataChannel;

import com.phono.srtplight.Log;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;
import org.ice4j.ice.Candidate;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class IceConnectJSON extends IceConnect {
    private String _session,_to,_type,_mid;

    public IceConnectJSON(int port) throws IOException, UnrecoverableEntryException, KeyStoreException, FileNotFoundException, NoSuchAlgorithmException, CertificateException {
        super(port);
    }

    public void setOffer(JsonObject message) throws IOException {
        // honestly - you look at this and you just wish for groovy or xpath to write this in a declarative way.
        JsonObject sdpo = message.getJsonObject("sdp");
        _session = message.getString("session");
        _to = message.getString("to");
        _type = message.getString("type");
        
        JsonArray contents = sdpo.getJsonArray("contents");
        for (JsonValue content : contents) {
            JsonObject fpo = ((JsonObject) content).getJsonObject("fingerprint");
            if ("sha-256".equalsIgnoreCase(fpo.getString("hash"))) {
                String ffp = fpo.getString("print");
                setFarFingerprint(ffp);
            }
            JsonObject media = ((JsonObject) content).getJsonObject("media");

            String proto = media.getString("proto");
            if ("DTLS/SCTP".equals(proto)) {
                JsonObject ice = ((JsonObject) content).getJsonObject("ice");
                String ufrag = ice.getString("ufrag");
                String pass = ice.getString("pwd");
                try {
                    buildIce(ufrag, pass);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ex) {
                        ;
                    }
                } catch (InterruptedException ex) {
                    Log.error(ex.toString());
                } catch (IllegalArgumentException ex) {
                    Log.error(ex.toString());
                }
                JsonArray candies = ((JsonObject) content).getJsonArray("candidates");
                for (JsonValue v_candy : candies) {
                    JsonObject jcandy = (JsonObject) v_candy;
                    addCandidate(jcandy.getString("foundation"), jcandy.getString("component"), jcandy.getString("protocol"), jcandy.getString("priority"), jcandy.getString("ip"), jcandy.getString("port"), jcandy.getString("type"));
                }
                _mid = ((JsonObject) content).getString("mid","data");
            }
        }
        String fp = this.getFarFingerprint();
        if( fp == null){
            throw new IOException("No fingerptint set");
        }
    }

    public JsonArrayBuilder mkCandidates() {
        JsonArrayBuilder ret = Json.createArrayBuilder();
        List<Candidate> candies = getCandidates();
        //{"sdpMLineIndex":1,"sdpMid":"data","candidate":{"foundation":"2169522962","component":"1","protocol":"tcp","priority":"1509957375","ip":"192.67.4.33","port":"0","type":"host","generation":"0\r\n"}
        for (Candidate candy : candies) {
            ret.add(Json.createObjectBuilder()
                    .add("foundation", candy.getFoundation())
                    .add("component", candy.getParentComponent().getComponentID())
                    .add("protocol", candy.getTransport().toString())
                    .add("priority", candy.getPriority())
                    .add("ip", candy.getTransportAddress().getHostAddress())
                    .add("port", candy.getTransportAddress().getPort())
                    .add("type", candy.getType().toString())
                    .add("generation", "0")
            );
        }
        return ret;
    }

    public JsonObject mkAnswer() {
        String farPrint = getFarFingerprint().replaceAll(":","");
        Log.debug("farprint is "+farPrint);
        JsonObject ans = Json.createObjectBuilder()
                .add("to", farPrint)
                .add("type", "answer")
                .add("session",_session)
                .add("sdp", Json.createObjectBuilder()
                        .add("contents", Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("candidates", mkCandidates()
                                        )
                                        .add("codecs", Json.createArrayBuilder()
                                        )
                                        .add("ice", Json.createObjectBuilder()
                                                .add("ufrag", getUfrag())
                                                .add("pwd", getPass())
                                        )
                                        .add("media", Json.createObjectBuilder()
                                                .add("type", "application")
                                                .add("port", "1")
                                                .add("proto", "DTLS/SCTP")
                                                .add("sctpmap", Json.createArrayBuilder().add("5000"))
                                                .add("pts", Json.createArrayBuilder().add("5000"))
                                        )
                                        .add("connection", Json.createObjectBuilder()
                                                .add("nettype", "IN")
                                                .add("addrtype", "IP4")
                                                .add("address", "0.0.0.0")
                                        )
                                        .add("fingerprint", Json.createObjectBuilder()
                                                .add("hash", "sha-256")
                                                .add("print", getPrint())
                                                .add("required", "1")
                                        )
                                        .add("mid", _mid)
                                        .add("setup", "passive")
                                        .add("sctpmap", Json.createArrayBuilder()
                                                .add(Json.createObjectBuilder()
                                                    .add("port",5000)
                                                    .add("app","webrtc-datachannel")
                                                    .add("count",256)
                                                )
                                        )
                                )
                        )
                        .add("session", Json.createObjectBuilder()
                                .add("username", "-")
                                .add("id", "4648475892259889561")
                                .add("ver", "2")
                                .add("nettype", "IN")
                                .add("addrtype", "IP4")
                                .add("address", "127.0.0.1")
                        )
                )
                /*.add("group", Json.createObjectBuilder()
                 .add("type", "BUNDLE")
                 .add("contents", Json.createArrayBuilder().add("data"))
                 )*/
                .build();
        return ans;
        //{"contents":[
        //{"candidates":[]
        //,"codecs":[]
        //,"ice":{"ufrag":"l384GvXPBPSGiGaR","pwd":"twPGZ/n25nH5SBbg7qyYNYT7","options":"google-ice"}
        //,"media":{"type":"application","port":"1","proto":"DTLS/SCTP","pts":["5000"]}
        //,"connection":{"nettype":"IN","addrtype":"IP4","address":"0.0.0.0"}
        //,"fingerprint":{"hash":"sha-256","print":"C1:8F:6F:80:5B:D6:C4:DF:A6:95:37:78:4E:57:0E:88:42:CB:1D:6A:20:26:07:CC:84:E4:41:C4:36:9A:3B:4D","required":"1"},
        //"mid":"data"}]
        //,"session":{"username":"-","id":"4648475892259889561","ver":"2","nettype":"IN","addrtype":"IP4","address":"127.0.0.1"},"group":{"type":"BUNDLE","contents":["audio","data"]}}} 
    }

}
