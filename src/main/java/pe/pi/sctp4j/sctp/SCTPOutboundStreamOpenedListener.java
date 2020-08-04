/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pe.pi.sctp4j.sctp;

/**
 *
 * @author thp
 */
public interface SCTPOutboundStreamOpenedListener {
    public void opened(SCTPStream s);
}
