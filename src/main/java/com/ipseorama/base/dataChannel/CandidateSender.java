/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ipseorama.base.dataChannel;

import javax.json.JsonObject;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public interface CandidateSender {

    public void sendCandidate(JsonObject j);
    
}
