/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.device.endpoints;

import com.ipseorama.base.certHolders.JksCertMaker;
import com.ipseorama.sctp.SCTPStream;
import com.phono.srtplight.Log;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.bouncycastle.asn1.x509.Certificate;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class CertManagerEndpoint extends JsonEndpoint {

    JksCertMaker _cm;

    public CertManagerEndpoint(SCTPStream s, JksCertMaker cm) {
        super(s);
        _cm = cm;
    }

    @Override
    public JsonObject onJsonMessage(JsonObject messj) {
        JsonObjectBuilder ret = Json.createObjectBuilder();
        String action = messj.getString("action");
        String certb64 = messj.getString("cert");
        byte[] certb = biz.source_code.Base64Coder.decode(certb64);
        Certificate cert = org.bouncycastle.asn1.x509.Certificate.getInstance(certb);
        String id = messj.getString("id");
        try {
            switch (action) {
                case "introduce":
                    if (id.equalsIgnoreCase("master")) {
                        throw new Exception("Friends are not masters");
                    }
                    _cm.putFriendCert(id, cert);
                    break;
                case "shun":
                    _cm.removeFriendCert(id, cert);
                    break;
                case "list":
                    List<String> list = _cm.listFriends();
                    JsonArrayBuilder ab = Json.createArrayBuilder();
                    for (String friend : list) {
                        ab.add(friend);
                    }
                    ret.add("friends", ab);
                    break;
                default:
                    throw new Exception("unknown action");
            }
            ret.add("status", "ok");

        } catch (Exception x) {
            ret.add("status", "failed");
            ret.add("errorMessage", x.getMessage());
            Log.debug("Failed " + x.getMessage());
            x.printStackTrace();
        }
        return ret.build();
    }

}
