/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ipseorama.base.certHolders;

import com.phono.srtplight.Log;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import org.bouncycastle.crypto.tls.Certificate;
import org.bouncycastle.crypto.util.PrivateKeyFactory;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class JksCertHolder extends CertHolder{
    protected String _ksFileName = "/project2/westhawk/keys/wildcard/wildwesthawk.ks";
    protected char [] _pass = {'y', 'e', 'e', 'h', 'a', 'a'};
    protected String _alias = "wildwest";
    public JksCertHolder() throws UnrecoverableEntryException, KeyStoreException, IOException, FileNotFoundException, NoSuchAlgorithmException, CertificateException {
        super();
    }

    @Override
    protected void loadKeyNCert() throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableEntryException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        // get user password and file input stream
        char[] password = _pass;

        java.io.FileInputStream fis = null;
        try {
            fis = new java.io.FileInputStream(_ksFileName);
            ks.load(fis, password);
            Key k = ks.getKey(_alias, password);
            _key = PrivateKeyFactory.createKey(k.getEncoded());
            java.security.cert.Certificate cert[] = ks.getCertificateChain(_alias);
            org.bouncycastle.asn1.x509.Certificate carry[] = new org.bouncycastle.asn1.x509.Certificate[cert.length];
            int n = 0;
            for (java.security.cert.Certificate c : cert) {
                carry[n++] = org.bouncycastle.asn1.x509.Certificate.getInstance(c.getEncoded());
            }
            _cert = new Certificate(carry);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }
    public static void main(String argv[]) {
        try {
            Log.setLevel(Log.DEBUG);
            CertHolder s = new JksCertHolder();
            Log.debug("fingerprint is " + s.getPrint());
            Log.debug("fingerprint is " + s.getPrint(false));

        } catch (Exception ex) {
            Log.error(ex.toString());
        }
    }
}
