/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.base.dataChannel;

import biz.source_code.Base64Coder;
import com.ipseorama.base.certHolders.CertHolder;
import com.ipseorama.device.endpoints.PermittedAssociationListener;
import com.ipseorama.sctp.Association;
import com.ipseorama.sctp.AssociationListener;
import com.phono.srtplight.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Hashtable;
import java.util.Properties;
import org.bouncycastle.crypto.tls.Certificate;
import org.bouncycastle.crypto.tls.CertificateRequest;
import org.bouncycastle.crypto.tls.ClientCertificateType;
import org.bouncycastle.crypto.tls.DTLSServerProtocol;
import org.bouncycastle.crypto.tls.DTLSTransport;
import org.bouncycastle.crypto.tls.DatagramTransport;
import org.bouncycastle.crypto.tls.DefaultTlsServer;
import org.bouncycastle.crypto.tls.DefaultTlsSignerCredentials;
import org.bouncycastle.crypto.tls.ExtensionType;
import org.bouncycastle.crypto.tls.ProtocolVersion;
import org.bouncycastle.crypto.tls.TlsClientContext;
import org.bouncycastle.crypto.tls.TlsContext;
import org.bouncycastle.crypto.tls.TlsSRTPUtils;
import org.bouncycastle.crypto.tls.TlsSignerCredentials;
import org.bouncycastle.crypto.tls.UseSRTPData;

/**
 *
 * @author tim
 */
public abstract class DTLSServer extends
        DefaultTlsServer implements Runnable, DTLSEndpoint {

    private DTLSServerProtocol _serverProtocol;
    private boolean _isShutdown;
    private Thread _acceptor;
    private final CertHolder _cert;
    private final AssociationListener _al;
    private final String _ffp;
    private boolean _verified = false;
    private Object nap;
    private final DatagramTransport _dt;
    private boolean wantRTP;

    public DTLSServer(CertHolder cert, DatagramTransport dt, AssociationListener al, String farFingerprint) throws Exception {

        _al = al;
        _cert = cert;
        _ffp = farFingerprint;
        if (_al instanceof PermittedAssociationListener) {
            PermittedAssociationListener pal = (PermittedAssociationListener) _al;
            pal.setCert(cert);
            pal.setFarFinger(farFingerprint);
        }
        _dt = dt;
        nap = new Object();
        if (_dt != null) {
            SecureRandom secureRandom = new SecureRandom();
            _serverProtocol = new DTLSServerProtocol(secureRandom);
            _acceptor = new Thread(this);
            _acceptor.setName("DTLSServer");
            _acceptor.start();
        } else {
            Log.debug("no socket or destination");
        }

    }
    protected void setWantRTP(boolean want){
        this.wantRTP = want;
    }
    static String getHex(byte[] in) {
        return getHex(in, in.length);
    }

    static String getHex(byte[] in, int len) {
        char cmap[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        StringBuffer ret = new StringBuffer();
        int top = Math.min(in.length, len);
        for (int i = 0; i < top; i++) {
            ret.append("(byte)0x");
            ret.append(cmap[0x0f & (in[i] >>> 4)]);
            ret.append(cmap[in[i] & 0x0f]);
            ret.append(", ");
            if ((i > 0) && ((i % 8) == 0)) {
                ret.append("\n");
            }
        }
        return ret.toString();
    }

    public void run() {
        boolean _dtlsStatusOk = true;
        DTLSTransport dtlsServer = null;
        try {
            dtlsServer = _serverProtocol.accept(this, _dt);
            Log.debug("DTLS accept. verified = " + _verified);
            if (_verified) {
                Association a = makeAssociation(dtlsServer, _al); // todo - association listener api is wrong.
                if (shouldInitiateAssociation()) {
                    a.associate();
                }
            } else {
                Log.error("Not the fingerprint we were looking for (waves hand)");
            }
        } catch (Exception e) {
            _dtlsStatusOk = false;
            e.printStackTrace();
        }
        synchronized (nap) {
            while (_dtlsStatusOk == true) {
                try {
                    Log.verb(" Napping ");
                    nap.wait(10000);
                } catch (InterruptedException ex) {
                    Log.error("Error Napping " + ex.getMessage());
                }
            }
        }
        if (dtlsServer != null) {
            try {
                dtlsServer.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }

    @Override
    public CertificateRequest getCertificateRequest() {
        return new CertificateRequest(new short[]{ClientCertificateType.rsa_sign, ClientCertificateType.dss_sign, ClientCertificateType.ecdsa_sign}, null, null);
    }

    @Override
    protected ProtocolVersion getMaximumVersion() {
        return ProtocolVersion.DTLSv10;
    }

    @Override
    protected ProtocolVersion getMinimumVersion() {
        return ProtocolVersion.DTLSv10;
    }

    @Override
    protected TlsSignerCredentials getRSASignerCredentials()
            throws IOException {
        return new DefaultTlsSignerCredentials(context, _cert.getCert(), _cert.getKey());
    }

    @Override
    public Hashtable getServerExtensions() throws IOException {

        // see https://tools.ietf.org/html/rfc5764 
        byte[] SRTP_AES128_CM_HMAC_SHA1_80 = {0x00, 0x01};
        Hashtable ret = super.getServerExtensions();

        byte[] prof = new byte[5];

        ByteBuffer profileB = ByteBuffer.wrap(prof);
        profileB.putChar((char) 2); // length;
        profileB.put(SRTP_AES128_CM_HMAC_SHA1_80);
        profileB.put((byte) 0);// mkti
        if (wantRTP) {
            if (ret == null) {
                ret = new Hashtable();
            }
            ret.put(ExtensionType.use_srtp, prof);
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyClientCertificate(Certificate clientCertificate)
            throws IOException {
        org.bouncycastle.asn1.x509.Certificate[] cs = clientCertificate.getCertificateList();
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

    /**
     * {@inheritDoc}
     *
     * Makes sure that the DTLS extended client hello contains the
     * <tt>use_srtp</tt> extension.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void processClientExtensions(Hashtable clientExtensions)
            throws IOException {
        UseSRTPData d
                = TlsSRTPUtils.getUseSRTPExtension(clientExtensions);

        if (d == null) {
            Log.debug("Browser didn't send a use_srtp in the client hello");
        }
        // remove something chrome sends wrongly
        if (clientVersion == ProtocolVersion.DTLSv10) {
            Log.debug("checking for signaturealgo extension as DTLS 1.0 shouldn't support it");
            Integer salg = new Integer(ExtensionType.signature_algorithms);
            if (clientExtensions.containsKey(salg)) {
                Log.debug("removing signature algos for now as DTLS 1.0 shouldn't support it");
                clientExtensions.remove(salg);
            }
        }
        super.processClientExtensions(clientExtensions);
    }

    public TlsContext getContext() {
        return context;
    }

}
