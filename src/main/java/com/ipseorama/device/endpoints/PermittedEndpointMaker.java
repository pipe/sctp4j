/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.device.endpoints;

import com.ipseorama.base.certHolders.CertHolder;
import com.ipseorama.base.certHolders.JksCertMaker;
import com.ipseorama.sctp.Association;
import com.ipseorama.sctp.SCTPStream;
import com.ipseorama.sctp.SCTPStreamListener;
import com.phono.srtplight.Log;
import org.bouncycastle.asn1.x509.Certificate;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public abstract class PermittedEndpointMaker implements PermittedAssociationListener {

    private JksCertMaker _certMaker;
    private Certificate _verifiedFarCert;
    private String _farFinger;

    public PermittedEndpointMaker() {
    }

    @Override
    public void onAssociated(Association a) throws java.lang.IllegalAccessError { //todo: think twice about this api
        if (_certMaker == null) {
            throw new IllegalAccessError("No valid certificate store to validate against.");
        }
        try {
            boolean hasMaster = _certMaker.hasMaster();
            if (!_certMaker.isAFriendPrint(_farFinger)) {
                if (!hasMaster && (_verifiedFarCert != null)) {
                    _certMaker.putMasterCert(_verifiedFarCert);
                    addedMaster();
                } else {
                    throw new IllegalAccessError("You are not friend - and we are not just hatched.");
                }
            }
        } catch (Exception ex) {
            throw new IllegalAccessError("Problem detecting master cert");
        }
    }

    @Override
    abstract public void onDisAssociated(Association a);

    @Override
    public void onDCEPStream(SCTPStream s, String lab, int type) {
        try {
            SCTPStreamListener sl = null;
            if (_certMaker.isAFriendPrint(_farFinger)) {
                switch (lab) {
                    case "echo":
                        sl = new EchoEndpoint();
                        break;
                    case "tick":
                        sl = new TickEndpoint(s);
                        break;
                    case "cert":
                        if (_certMaker.isMaster(_farFinger)) {
                            sl = new CertManagerEndpoint(s, _certMaker);
                        } else {
                            sl = new ErrorEndpoint(s);
                        }
                        break;
                    case "shell":
                        //if (_certMaker.isMaster(_farFinger)) {
                            sl = new ShellEndpoint(s);
                        //} else {
                        //    sl = new ErrorEndpoint(s);
                        //}
                        break;
                    default:
                        sl = mkEndpointForLabel(lab, s);
                }
                s.setSCTPStreamListener(sl);
            } else {
                Log.debug("You are no longer a friend.");
            }
        } catch (Exception ex) {
            Log.debug("problem making stream " + lab);
            Log.debug("exception is  " + ex.getMessage());

            if (Log.getLevel() >= Log.DEBUG) {
                ex.printStackTrace(System.err);
            }
        }
    }

    @Override
    public void verifiedFarCert(Certificate cs) {
        _verifiedFarCert = cs;
    }

    @Override
    public void setCert(CertHolder cert) {
        if (cert instanceof JksCertMaker) {
            _certMaker = (JksCertMaker) cert;
        }
    }

    @Override
    public void setFarFinger(String farFingerprint) {
        _farFinger = farFingerprint;
    }

    public void addedMaster() {
    }

    protected SCTPStreamListener mkEndpointForLabel(String lab, SCTPStream s) {
        Log.debug("No customized stream makers available");
        SCTPStreamListener ret = new SCTPStreamListener() {

            @Override
            public void onMessage(SCTPStream s, String message) {
                s.close();
            }

        };
        return ret;
    }

    @Override
    public void onRawStream(SCTPStream s) {
        Log.debug("ignore raw stream notification");
    }
}
