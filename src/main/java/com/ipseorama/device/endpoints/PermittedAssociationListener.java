/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.device.endpoints;

import com.ipseorama.base.certHolders.CertHolder;
import com.ipseorama.sctp.AssociationListener;
import org.bouncycastle.asn1.x509.Certificate;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public interface PermittedAssociationListener extends AssociationListener {

    public void verifiedFarCert(Certificate cs);

    public void setCert(CertHolder cert);

    public void setFarFinger(String farFingerprint);

}
