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

import com.ipseorama.sctp.AssociationListener;
import com.phono.srtplight.Log;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;


/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
abstract public class IceConnectJSON {

    public String _session, _to, _type, _mid;
    String _us;
    IceConnectFace _ice;
    public PropertyChangeListener onPropertyChange;



    public IceConnectJSON(IceConnectFace ice) {
        _ice = ice;
        _us = _ice.getPrint().replace(":", "");
    }

    public void setCleanupCB(Runnable clean) {
        _ice.setCleanup(clean);
    }

    abstract public void startIce(final CandidateSender cs) ;

    public void setOffer(JsonObject message) throws IOException {
        String offer = message.getString("type");
        if (!"offer".equalsIgnoreCase(offer)) {
            throw new java.io.IOException("we expected an offer, got: " + offer);
        }
        setSDP(message);

    }

    public void setAnswer(JsonObject message) throws IOException {
        String answer = message.getString("type");
        String session = message.getString("session");
        if (!_session.equalsIgnoreCase(session)) {
            throw new java.io.IOException("session mixup, expected : " + _session);
        }
        if (!"answer".equalsIgnoreCase(answer)) {
            throw new java.io.IOException("we expected an answer, got: " + answer);
        }
        setSDP(message);
    }

    /*public void setSession(String session) {
        _session = session;
    }*/

    public void setMid(String mid) {
        _mid = mid;
    }

    public void setSDP(JsonObject message) throws IOException {
        // honestly - you look at this and you just wish for groovy or xpath to write this in a declarative way.
        JsonObject sdpo = message.getJsonObject("sdp");
        String to = message.getString("to");
        String from = message.getString("from");
        String type = message.getString("type");

        String session = message.getString("session");
        
        // Some basic verification 
        if (to.equals(_us)){
            _to = to;
        }else {
            throw new IOException("This message isn't for us.");
        }
        
        if (_session == null){
            _session = session;
            if ("offer".equals(type)){ // you cant start a session with an answer
                _type = type;
            } else {
                throw new IOException("Sessions must start with an offer");
            }
        } else {
            if (!_session.equals(session)){
                throw new IOException("Wrong session");
            }
        }


        JsonArray contents = sdpo.getJsonArray("contents");
        for (JsonValue content : contents) {
            JsonObject fpo = ((JsonObject) content).getJsonObject("fingerprint");
            if ("sha-256".equalsIgnoreCase(fpo.getString("hash"))) {
                String ffp = fpo.getString("print");
                String ff = ffp.replace(":", "");
                if (!ff.equals(from)){
                    throw new IOException("offered fingerprint isn't same as from address - not a valid |pipe| app");
                }
                _ice.setFarFingerprint(ffp);
            }
            JsonObject media = ((JsonObject) content).getJsonObject("media");

            String proto = media.getString("proto");
            _mid = ((JsonObject) content).getString("mid", "data");
            String setup = ((JsonObject) content).getString("setup", "unknown");
            switch (setup) {
                case "active":
                    _ice.setDtlsClientRole(false); // ie they are active, so we arent
                    break;
                case "passive":
                    _ice.setDtlsClientRole(true); // they are passive, so we have to take the lead
                    break;
                case "actpass":
                    _ice.setDtlsClientRole(false); // we have a mild preference for going first and
                    // but this doesn't seem to work - dtls client does not retry correctly
                    // so for now we will be a passive server
                    break;
                default:
                    Log.error("Huh? setup type unrecognized " + setup);
                    break;
            }
            if ("DTLS/SCTP".equals(proto)) {
                JsonObject ice = ((JsonObject) content).getJsonObject("ice");
                String ufrag = ice.getString("ufrag");
                String pass = ice.getString("pwd");
                try {
                    _ice.buildIce(ufrag, pass);
                } catch (InterruptedException | IllegalArgumentException ex) {
                    Log.error(ex.toString());
                }
                JsonArray candies = ((JsonObject) content).getJsonArray("candidates");
                candies.stream().map((v_candy) -> (JsonObject) v_candy).forEach((jcandy) -> {
                    addJasonCandy(jcandy);
                });
            }
        }
        String fp = _ice.getFarFingerprint();
        if (fp == null) {
            throw new IOException("No fingerprint set");
        }
    }

    private void addJasonCandy(JsonObject jcandy) {
        _ice.addCandidate(jcandy.getString("foundation"),
                jcandy.getString("component"),
                jcandy.getString("protocol"),
                jcandy.getString("priority"),
                jcandy.getString("ip"),
                jcandy.getString("port"),
                jcandy.getString("type"),
                jcandy.getString("raddr", null),
                jcandy.getString("rport", null)
        );
    }

    public void addRemoteCandidate(JsonObject message) throws IOException {
        String session = message.getString("session");
        String to = message.getString("to");
        String type = message.getString("type");
        if (to.equals(_to) && (type.equalsIgnoreCase("candidate")) && session.equals(_session)) {
            JsonObject jcandy = message.getJsonObject("candidate");
            addJasonCandy(jcandy);
        }
    }



    abstract public JsonArrayBuilder mkCandidates() ;

    public JsonObject mkAnswer() {
        return mkSDP("answer", _ice.getDtlsClientRole() ? "active" : "passive");
    }

    public JsonObject mkOffer() {
        _ice.setOfferer(true);
        return mkSDP("offer", "actpass");
    }

    public JsonObject mkSDP(String type, String setup) {
        String farPrint = _ice.getFarFingerprint().replaceAll(":", "");
        Log.debug("farprint is " + farPrint);
        JsonObject ans = Json.createObjectBuilder()
                .add("to", farPrint)
                .add("from", _us)
                .add("type", type)
                .add("session", _session)
                .add("sdp", Json.createObjectBuilder()
                        .add("contents", Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("candidates", mkCandidates()
                                        )
                                        .add("codecs", Json.createArrayBuilder()
                                        )
                                        .add("ice", Json.createObjectBuilder()
                                                .add("ufrag", _ice.getUfrag())
                                                .add("pwd", _ice.getPass())
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
                                                .add("print", _ice.getPrint())
                                                .add("required", "1")
                                        )
                                        .add("mid", _mid)
                                        .add("setup", setup)
                                        .add("sctpmap", Json.createArrayBuilder()
                                                .add(Json.createObjectBuilder()
                                                        .add("port", 5000)
                                                        .add("app", "webrtc-datachannel")
                                                        .add("count", 256)
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
                .add("group", Json.createObjectBuilder()
                 .add("type", "BUNDLE")
                 .add("contents", Json.createArrayBuilder().add("data"))
                 )
                .build();
        Log.verb("Sending" + ans.toString());

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

    public void setAssociationListener(AssociationListener al) {
        _ice.setAssociationListener(al);
    }

}
