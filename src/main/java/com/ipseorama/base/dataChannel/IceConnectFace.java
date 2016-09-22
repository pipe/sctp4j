/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.base.dataChannel;

import com.ipseorama.sctp.AssociationListener;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import javax.json.JsonValue;

/**
 *
 * @author thp
 */
public interface IceConnectFace {

    public void haveLocalCandy(boolean b);

    public void startIce();

    public void setFarFingerprint(String ffp);

    public void buildIce(String ufrag, String pass)throws InterruptedException, IllegalArgumentException, IOException;

    public String getFarFingerprint();

    public void addCandidate(String string, String string0, String string1, String string2, String string3, String string4, String string5, String string6, String string7);

    public List getCandidates();

    public String getUfrag();

    public String getPass();

    public String getPrint();

    public void setAssociationListener(AssociationListener al);

    public void setDtlsClientRole(boolean b);

    public boolean getDtlsClientRole();

    public void setOfferer(boolean b);

    public void setCleanup(Runnable clean);


    public void startCandidateTrickle(Consumer<Object> cons);
    
}
