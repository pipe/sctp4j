/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.device.endpoints;

import com.ipseorama.sctp.SCTPMessage;
import com.ipseorama.sctp.SCTPStream;
import com.phono.srtplight.Log;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class ShellEndpoint extends JsonEndpoint {

    private Process _proc;
    private DataInputStream _shin;
    private DataOutputStream _shout;
    private DataInputStream _sherror;
    private Thread _outSend;
    private Thread _errSend;

    public ShellEndpoint(SCTPStream s) {
        super(s);
        Log.debug("Shell endpoint  ");
        try {
            Runtime r = Runtime.getRuntime();
            Log.debug("Got runtime " + r.toString());
            _proc = r.exec("/bin/sh -i");
            Log.debug("got proc " + _proc.toString());
            _shin = new DataInputStream(_proc.getInputStream());
            _shout = new DataOutputStream(_proc.getOutputStream());
            _sherror = new DataInputStream(_proc.getErrorStream());
            Runnable sendErr = new Runnable() {
                @Override
                public void run() {
                    sendLoop(_sherror, "stderr");
                }
            };
            Runnable sendOut = new Runnable() {
                @Override
                public void run() {
                    sendLoop(_shin, "stdout");
                }
            };
            _outSend = new Thread(sendOut, "shellout");
            _outSend.start();
            _errSend = new Thread(sendErr, "shellerr");
            _errSend.start();
        } catch (IOException ex) {
            Log.debug("can't create shell" + ex.toString());
            ex.printStackTrace();
        }
    }

    private void sendMessage(String tag, String message) throws Exception {
        JsonObject jo = Json.createObjectBuilder().add(tag, message).build();
        this.sendJson(jo);
    }

    private void sendLoop(DataInputStream s, String tag) {
        try {
            Log.debug("starting loop for " + tag);
            while (_proc.isAlive()) {
                Log.debug("proc is alive  " + tag);
                String line = s.readLine();
                sendMessage(tag, line);
            }
        } catch (Exception x) {
            try {
                sendMessage("error-tag", x.getMessage());
            } catch (Exception ex) {
                Log.error("shell exception in " + tag + " " + ex);
            }
        }
    }

    @Override
    public JsonObject onJsonMessage(JsonObject jo) {
        String resp = "unknown command error";
        JsonObjectBuilder ret = Json.createObjectBuilder();
        String command = jo.getString("command", "none");
        switch (command) {
            case "kill":
                if (_proc.isAlive()) {
                    _proc.destroyForcibly();
                }
                ret.add("alive", _proc.isAlive());
                resp = command;
                break;
            case "write":
                String v = jo.getString("text", "");
                 {
                    try {
                        String cmd = v + "\n";
                        Log.debug("write command to shell -> " + cmd);
                        _shout.writeBytes(cmd);
                        _shout.flush();
                        resp = "ok";
                    } catch (IOException ex) {
                        resp = ex.getLocalizedMessage();
                        Log.debug(ex.getMessage());
                    }
                }
                break;
            default:
                break;
        }
        ret.add("response", resp);
        return ret.build();
    }

    public static void main(String argv[]) {
        Log.setLevel(Log.VERB);
        Log.debug("starting... ");
        SCTPStream dummyStream = new SCTPStream(null, 100) {
            @Override
            public void send(String message) throws Exception {
                Log.debug("Sending -> " + message);
            }

            @Override
            public void deliverMessage(SCTPMessage message) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void send(byte[] message) throws Exception {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

        };
        Log.debug("made dummy stream... ");

        try {
            ShellEndpoint shell = new ShellEndpoint(dummyStream);
            Log.debug("made shell endpoint stream... ");

            JsonObject jo = Json.createObjectBuilder().add("command", "write").add("text", "date").build();
            Log.debug("sending command to shell endpoint stream... ");
            shell.onJsonMessage(jo);
            Thread.sleep(10000);
            jo = Json.createObjectBuilder().add("command", "kill").build();
            Log.debug("sending kill to shell endpoint stream... ");

            shell.onJsonMessage(jo);
        } catch (Exception x) {
            x.printStackTrace();
        }
    }
}
