/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ipseorama.base.dataChannel;

import com.ipseorama.sctp.Association;
import com.ipseorama.sctp.AssociationListener;
import org.bouncycastle.crypto.tls.DTLSTransport;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public interface DTLSEndpoint {
    public boolean shouldInitiateAssociation();
    public Association makeAssociation(DTLSTransport trans, AssociationListener li);
}
