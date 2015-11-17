/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.base.certHolders;

import static com.ipseorama.base.certHolders.CertHolder._cert;
import com.phono.srtplight.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.tls.Certificate;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.util.BigIntegers;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class JksCertMaker extends JksCertHolder {

    public JksCertMaker() throws UnrecoverableEntryException, KeyStoreException, IOException, FileNotFoundException, NoSuchAlgorithmException, CertificateException {
        super();
    }

    String makeCn(SecureRandom random) {
        byte cn[] = new byte[20];
        random.nextBytes(cn);
        StringBuffer b = new StringBuffer();
        for (byte r : cn) {
            String dig = Integer.toHexString((0xff) & r).toUpperCase();
            if (dig.length() == 1) {
                b.append('0');
            }
            b.append(dig);
        }
        return b.toString();
    }

    void mkCertNkey() throws OperatorCreationException, NoSuchAlgorithmException, NoSuchProviderException, IOException, KeyStoreException, CertificateException {
        SecureRandom random = new SecureRandom();
        AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256withRSA");
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

        int strength = 2048;
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(strength);

        KeyPair keyPair = generator.generateKeyPair();
        PrivateKey priv = keyPair.getPrivate();
        _key = PrivateKeyFactory.createKey(priv.getEncoded());

        PublicKey pub = keyPair.getPublic();
        byte[] encoded = pub.getEncoded();
        SubjectPublicKeyInfo subjectPublicKeyInfo = new SubjectPublicKeyInfo(
                ASN1Sequence.getInstance(encoded));

        Date startDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        Date endDate = new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000);
        BigInteger serialNumber
                = BigInteger.valueOf(Math.abs(random.nextLong()));
        ContentSigner sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(_key);

        String cn = this.makeCn(random);
        X500Name rname = new X500Name("CN="+cn);
        X509v1CertificateBuilder v1CertGen = new X509v1CertificateBuilder(
                rname,
                serialNumber,
                startDate, endDate,
                rname,
                subjectPublicKeyInfo);
        X509CertificateHolder certHolder = v1CertGen.build(sigGen);

        org.bouncycastle.asn1.x509.Certificate carry[] = new org.bouncycastle.asn1.x509.Certificate[1];
        carry[0] = org.bouncycastle.asn1.x509.Certificate.getInstance(certHolder.getEncoded());

        _cert = new Certificate(carry);

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        java.security.cert.Certificate[] chain = new java.security.cert.Certificate[1];

        ByteArrayInputStream bis = new ByteArrayInputStream(certHolder.getEncoded());

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        while (bis.available() > 0) {
            chain[0] = cf.generateCertificate(bis);
            System.out.println(chain[0].toString());
        }
        ks.load(null);
        ks.setKeyEntry(_alias, priv, _pass, chain);
        FileOutputStream fos = new FileOutputStream(_ksFileName);
        ks.store(fos, _pass);
        fos.close();
    }

    protected void loadKeyNCert() throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableEntryException {
        _ksFileName = "ipsecert.ks";
        _pass = new char[6];
        _alias = "ipseorama";
        "secret".getChars(0, 5, _pass, 0);
        Log.debug("setting ks file to " + _ksFileName);

        File ks = new File(_ksFileName);
        if (!ks.exists()) {
            Log.debug("need to make key and cert - no " + _ksFileName);
            try {
                mkCertNkey();
            } catch (Exception ex) {
                Log.error(ex.toString());
                ex.printStackTrace();
            }
        } else {
            super.loadKeyNCert();
        }
    }

    public static void main(String argv[]) {
        try {
            Log.setLevel(Log.DEBUG);
            CertHolder s = new JksCertMaker();
            Log.debug("fingerprint is " + s.getPrint());
            Log.debug("fingerprint is " + s.getPrint(false));

            byte[] certBytes = s.getCert().getCertificateAt(0).getEncoded();

            org.bouncycastle.asn1.x509.Certificate c = org.bouncycastle.asn1.x509.Certificate.getInstance(certBytes);
            System.out.print(c.getSubject().toString());
            System.out.println(" Issuer " + c.getIssuer().toString());
        } catch (Exception ex) {
            Log.error(ex.toString());
            ex.printStackTrace();
        }
    }

}
