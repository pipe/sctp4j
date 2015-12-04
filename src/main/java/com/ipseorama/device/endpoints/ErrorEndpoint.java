/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ipseorama.device.endpoints;

import com.ipseorama.sctp.SCTPStream;
import com.ipseorama.sctp.SCTPStreamListener;
import javax.json.Json;
import javax.json.JsonObject;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class ErrorEndpoint extends JsonEndpoint {

    public ErrorEndpoint(SCTPStream s) {
        super(s);
    }

    @Override
    protected JsonObject onJsonMessage(JsonObject messj) {
        return Json.createObjectBuilder().add("status", "failed").add("errorMessage", "You are not my master").build();
    }
    
}
