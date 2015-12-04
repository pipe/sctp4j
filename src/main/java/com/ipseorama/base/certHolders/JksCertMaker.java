/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.base.certHolders;

import static com.ipseorama.base.certHolders.CertHolder._cert;
import com.phono.srtplight.Log;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.crypto.tls.Certificate;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

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
        X500Name rname = new X500Name("CN=" + cn);
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

    public org.bouncycastle.asn1.x509.Certificate getMasterCert() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        return getFriendCert("master");
    }

    public void putMasterCert(org.bouncycastle.asn1.x509.Certificate masterCert) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        this.putFriendCert("master", masterCert);
    }

    public boolean hasMaster() throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore ks = openKeyStore();
        boolean ret = ks.isCertificateEntry("master");
        return ret;
    }

    public String getMasterPrint() throws KeyStoreException, IOException, FileNotFoundException, NoSuchAlgorithmException, CertificateException {
        return getFriendPrint("master");
    }

    protected KeyStore openKeyStore() throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        FileInputStream fis = new java.io.FileInputStream(_ksFileName);
        ks.load(fis, _pass);
        return ks;
    }

    public String getFriendPrint(String name) throws KeyStoreException, IOException, FileNotFoundException, NoSuchAlgorithmException, CertificateException {
        org.bouncycastle.asn1.x509.Certificate c = getFriendCert(name);
        return getPrint(c, true);
    }

    public org.bouncycastle.asn1.x509.Certificate getFriendCert(String name) throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        KeyStore ks = openKeyStore();
        java.security.cert.Certificate c = ks.getCertificate(name);
        org.bouncycastle.asn1.x509.Certificate ret = convertCert(c);
        return ret;
    }

    public boolean isAFriendPrint(String fp) throws KeyStoreException, IOException, FileNotFoundException, NoSuchAlgorithmException, CertificateException {
        List<String> friends = listFriends();
        boolean ret = false;
        for (String friend : friends) {
            String cfp = getFriendPrint(friend);
            Log.debug("testing " + friend);
            if (cfp.equalsIgnoreCase(fp)) {
                ret = true;
                Log.debug(friend + " fp matches");
                break;
            } else {
                Log.debug(friend +" \n\t"+fp +"\nvs\n\t"+cfp);
            }
        }
        return ret;
    }

    private static java.security.cert.Certificate convertCert(org.bouncycastle.asn1.x509.Certificate cert) throws IOException, CertificateException {

        ByteArrayInputStream bis = new ByteArrayInputStream(cert.getEncoded());

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        java.security.cert.Certificate fc = null;

        while (bis.available() > 0) {
            fc = cf.generateCertificate(bis);
            Log.verb(fc.toString());
        }
        return fc;
    }

    private static org.bouncycastle.asn1.x509.Certificate convertCert(java.security.cert.Certificate cert) throws IOException, CertificateException {
        byte mb[] = cert.getEncoded();
        org.bouncycastle.asn1.x509.Certificate c = org.bouncycastle.asn1.x509.Certificate.getInstance(mb);
        return c;
    }

    public void putFriendCert(String name, org.bouncycastle.asn1.x509.Certificate friendCert) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        FileInputStream fis = new java.io.FileInputStream(_ksFileName);
        ks.load(fis, _pass);
        java.security.cert.Certificate fc = convertCert(friendCert);
        ks.setCertificateEntry(name, fc);
        FileOutputStream fos = new FileOutputStream(_ksFileName);
        ks.store(fos, _pass);
        fos.close();
    }

    public List<String> listFriends() throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        Log.debug("Listing friends ");

        FileInputStream fis = new java.io.FileInputStream(_ksFileName);
        ks.load(fis, _pass);
        Enumeration<String> ase = ks.aliases();
        List<String> ret = new ArrayList();
        while (ase.hasMoreElements()) {
            String fr = ase.nextElement();
            if (ks.isCertificateEntry(fr)) {
                ret.add(fr);
                Log.debug("Alias " + fr + " is friend");
            } else {
                Log.debug("Alias " + fr + " isn't friend");
            }
        }
        fis.close();
        return ret;
    }

    public static void main(String argv[]) {
        try {
            Log.setLevel(Log.DEBUG);
            JksCertMaker s = new JksCertMaker();
            String googDerBase64 = "MIIBmTCCAQKgAwIBAgIEPlDfrjANBgkqhkiG9w0BAQsFADARMQ8wDQYDVQQDDAZXZWJSVEMwHhcNMTUxMTIzMTE1NDI3WhcNMTUxMjIzMTE1NDI3WjARMQ8wDQYDVQQDDAZXZWJSVEMwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBALhwh5n2+2IozDaycqMpLmfF1u03KAzOcxd+925lKPPrqDqy9NWF5IgTeYRg42k2Vu/RNVbGOpZBzP8DHENleBLJHJ29ZuZy2CRPSQF7CfwMPsBYDnOTkGtZbPGu+37Yn6/ZTOdRXJHFxHPrc5yy5CSCdDvznEBffSj7xgV55txzAgMBAAEwDQYJKoZIhvcNAQELBQADgYEAZjzk20bBPZuopQU2hdLRl5iFzukzGaQPb0lfiplYWwR+vNWRAVvtxCqY8DGTQ7xyaDGjV00OCzCKAhBNGJlI7WPgd23FTBr5SIHS6JmSCOlGaqBkr1u6DH7NJEYlN4g7tz2BnixIwm55zJqK8X/7o5SAfgjxFU2kWv8xHGJEHgg=";
            String googFingerprint = "41:5A:6B:17:29:C1:24:9F:BE:10:AC:BA:1C:C9:B0:A9:57:2E:50:E9:3A:4C:8F:65:4B:1F:DD:27:D5:A9:29:2A";
            String fgoogDerBase64 = "MIIBmTCCAQKgAwIBAgIEV4t3kzANBgkqhkiG9w0BAQsFADARMQ8wDQYDVQQDDAZXZWJSVEMwHhcNMTUxMjA0MTMwODM1WhcNMTYwMTAzMTMwODM1WjARMQ8wDQYDVQQDDAZXZWJSVEMwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAK6VpdM0VTH4F7NrteazILXkZlTl9lT4JXq4LbAipX/hbTrgGb+XcP45EvuCWh2BBFo7pfFZS1goEEJ5w/fWbWnIpioIadKI+cu2ZZv6nFH2LnAx0ytTAwZ87t2HWVL4zLCzmTiRtqZephqK5oZQCBRvVO47I6bMPoDoNR5Zcv0/AgMBAAEwDQYJKoZIhvcNAQELBQADgYEAMnFJuGI5FOVhQKuwH/SVyBh3MT8QBx88ebxSk8qx9M8l6gEnwaXCHYxhl5gdGQVxV42TS2X1XDjypZlnPAdANBF0kJH3xVW3c28wJsP2xtt3OYwLIuKsjogjeIVKUdwoyfGC1CJAlOwSKNTeoqkOERw7Fa8bTte4L7xGMxDyUwk=";
            String fgoogFingerprint = "89:B5:9B:FD:EE:16:DC:72:CC:24:22:DC:BF:73:03:F5:1B:B0:93:35:D5:DD:77:5B:44:EB:69:8C:60:2A:CA:5A";
            Log.debug("My fingerprint is " + s.getPrint(false));

            boolean master = s.hasMaster();
            Log.debug("Key store " + (master ? "has" : "hasnt") + " got a master");

            byte[] mb = biz.source_code.Base64Coder.decode(googDerBase64);
            org.bouncycastle.asn1.x509.Certificate c = org.bouncycastle.asn1.x509.Certificate.getInstance(mb);
            System.out.print(c.getSubject().toString());
            System.out.println(" Issuer " + c.getIssuer().toString());
            s.putMasterCert(c);
            master = s.hasMaster();
            Log.debug("Key store " + (master ? "has" : "hasnt") + " got a master");
            s.listFriends();
            byte[] fmb = biz.source_code.Base64Coder.decode(fgoogDerBase64);
            org.bouncycastle.asn1.x509.Certificate fc = org.bouncycastle.asn1.x509.Certificate.getInstance(fmb);
            s.putFriendCert("canary", fc);
            s.listFriends();
            org.bouncycastle.asn1.x509.Certificate mc = s.getMasterCert();
            Log.debug("Retrieved master cert " + mc.getSubject().toString());
            String mprint = s.getMasterPrint();
            Log.debug("Retrieved master fp " + mprint);
            boolean isfr = s.isAFriendPrint(fgoogFingerprint);
            Log.debug("prints " + (isfr ? "match " : "dont match"));
            boolean isMaster = s.isMaster(googFingerprint);
            Log.debug("sample print " + (isMaster ? "master " : "not master"));
            s.removeFriendCert("master", mc);
            master = s.hasMaster();
            s.removeFriendCert("canary", fc);
            Log.debug("Key store " + (master ? "has" : "hasnt") + " got a master");
            s.listFriends();

        } catch (Exception ex) {
            Log.error(ex.toString());
            ex.printStackTrace();
        }
    }

    public void removeFriendCert(String id, org.bouncycastle.asn1.x509.Certificate cert) throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        FileInputStream fis = new java.io.FileInputStream(_ksFileName);
        ks.load(fis, _pass);
        ks.deleteEntry(id);
        FileOutputStream fos = new FileOutputStream(_ksFileName);
        ks.store(fos, _pass);
        fos.close();
    }

    public boolean isMaster(String farFinger) throws KeyStoreException, IOException, FileNotFoundException, NoSuchAlgorithmException, CertificateException {
        String master = getMasterPrint();
        return master.equalsIgnoreCase(farFinger);
    }

}
