/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.device.endpoints;

import javax.json.JsonObject;

/**
 *
 * @author tim
 */
public interface JsonFace {

    public JsonObject onJsonMessage(JsonObject messj);

    public void sendJson(JsonObject jo) throws Exception;

}
