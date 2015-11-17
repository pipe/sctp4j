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
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.tls.Certificate;
import org.bouncycastle.crypto.util.PrivateKeyFactory;

/**
 *
 * @author tim
 */
public abstract class CertHolder {

    static protected Certificate _cert;
    static protected AsymmetricKeyParameter _key;

    public CertHolder() throws UnrecoverableEntryException, KeyStoreException, IOException, FileNotFoundException, NoSuchAlgorithmException, CertificateException {
        if ((_key == null) || (_cert == null)) {
            loadKeyNCert();
            Log.debug("Key and cert loaded.");
        }
    }

    protected abstract void loadKeyNCert() throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableEntryException ;
    public Certificate getCert() {
        return this._cert;
    }

    public AsymmetricKeyParameter getKey() {
        return this._key;
    }


    public String getPrint() throws IOException {
        return getPrint(true);
    }
    public String getPrint(boolean withColon) throws IOException {
        org.bouncycastle.asn1.x509.Certificate fpc = _cert.getCertificateAt(0);
        return getPrint(fpc,withColon);
    }

    public static String getPrint(org.bouncycastle.asn1.x509.Certificate fpc,boolean withColon) throws IOException {
        StringBuilder b = new StringBuilder();
        byte[] enc = fpc.getEncoded();
        SHA256Digest d = new SHA256Digest();
        d.update(enc, 0, enc.length);
        byte[] result = new byte[d.getDigestSize()];
        d.doFinal(result, 0);
        for (byte r : result) {
            String dig = Integer.toHexString((0xff) & r).toUpperCase();
            if (dig.length() == 1) {
                b.append('0');
            }
            b.append(dig);
            if (withColon) b.append(":");
        }
        if (withColon) b.deleteCharAt(b.length() - 1);
        return b.toString();
    }
}
