/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.base.dataChannel;

import com.ipseorama.base.certHolders.CertHolder;
import com.ipseorama.device.endpoints.PermittedAssociationListener;
import com.ipseorama.sctp.Association;
import com.ipseorama.sctp.AssociationListener;
import com.ipseorama.sctp.small.ThreadedAssociation;
import com.phono.srtplight.Log;
import java.io.IOException;
import java.security.SecureRandom;
import org.bouncycastle.crypto.tls.Certificate;
import org.bouncycastle.crypto.tls.CertificateRequest;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.crypto.tls.DTLSClientProtocol;
import org.bouncycastle.crypto.tls.DatagramTransport;
import org.bouncycastle.crypto.tls.DefaultTlsSignerCredentials;
import org.bouncycastle.crypto.tls.ProtocolVersion;
import org.bouncycastle.crypto.tls.TlsAuthentication;
import org.bouncycastle.crypto.tls.TlsCredentials;

/**
 *
 * @author tim
 */
abstract class DTLSClient extends
        org.bouncycastle.crypto.tls.DefaultTlsClient implements Runnable, DTLSEndpoint {

    private final CertHolder _cert;
    private final AssociationListener _al;
    private final String _ffp;
    private boolean _verified = false;
    private Thread _client;
    private final DatagramTransport _dt;
    private final static int[] suites = {
        /*CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
         CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,
         CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
         CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256,
         CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA256,
         CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA */
        CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,// (0xc00a)
        CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,// (0xc014)
        CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA,// (0x0039)
        CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA,// (0x0035)
        CipherSuite.TLS_RSA_WITH_3DES_EDE_CBC_SHA,// (0x000a)
        CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV
    };

    public DTLSClient(CertHolder cert, DatagramTransport dt, AssociationListener al, String farFingerprint) throws Exception {

        _al = al;
        _cert = cert;
        _ffp = farFingerprint;
        if (_al instanceof PermittedAssociationListener) {
            PermittedAssociationListener pal = (PermittedAssociationListener) _al;
            pal.setCert(cert);
            pal.setFarFinger(farFingerprint);
        }
        _dt = dt;
        _client = new Thread(this);
        _client.setName("DTLSlient");
        _client.start();
    }

    @Override
    public ProtocolVersion getClientVersion() {
        return ProtocolVersion.DTLSv10;
    }

    @Override
    public ProtocolVersion getMinimumVersion() {
        return ProtocolVersion.DTLSv10;
    }

    public void run() {
        Log.debug("starting DTLS client");
        try {
            SecureRandom sec = new SecureRandom();
            DTLSClientProtocol protocol = new DTLSClientProtocol(sec);
            Log.debug("DTLS client protocol created " + _verified);

            DatagramTransport dtls = protocol.connect(this, _dt);
            Log.debug("DTLS client connected. verified = " + _verified);
            if (_verified) {
                Association a = new ThreadedAssociation(dtls, _al); // todo - association listener api is wrong.
                Log.debug("Association = " + a.toString());
                if (shouldInitiateAssociation()){
                    a.sendInit();
                }
            } else {
                Log.error("Not the client fingerprint we were looking for (waves hand)");
            }

        } catch (Exception ex) {
            Log.error("dtls client failed " + ex.toString());
            ex.printStackTrace();
        }
    }
    /* not needed for SCTP ?
     public Hashtable getClientExtensions() throws IOException {

     Hashtable ret = super.getClientExtensions();
     TlsSRTPUtils.addUseSRTPExtension(ret, null);
     return ret;
     }*/

    public int[] getCipherSuites() {
        return suites;
    }

    @Override
    public TlsAuthentication getAuthentication() throws IOException {
        return new TlsAuthentication() {

            @Override
            public void notifyServerCertificate(Certificate crtfct) throws IOException {
                org.bouncycastle.asn1.x509.Certificate[] cs = crtfct.getCertificateList();
                if ((cs == null) || (cs.length < 1)) {
                    throw new IOException("no certs offered");
                }
                String ffp = CertHolder.getPrint(cs[0], true);
                if (!ffp.equalsIgnoreCase(_ffp)) {
                    throw new IOException("fingerprints don't match ");
                }
                _verified = true;
                if (_al instanceof PermittedAssociationListener) {
                    ((PermittedAssociationListener) _al).verifiedFarCert(cs[0]);
                }
            }

            @Override
            public TlsCredentials getClientCredentials(CertificateRequest cr) throws IOException {
                return new DefaultTlsSignerCredentials(context, _cert.getCert(), _cert.getKey());
            }
        };
    }

}
