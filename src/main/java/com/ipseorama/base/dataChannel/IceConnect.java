/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.base.dataChannel;

import com.ipseorama.base.certHolders.CertHolder;
import com.ipseorama.base.certHolders.JksCertHolder;
import com.ipseorama.sctp.AssociationListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.Agent;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.NominationStrategy;
import org.ice4j.ice.harvest.StunCandidateHarvester;
import org.ice4j.ice.harvest.TurnCandidateHarvester;
import org.ice4j.security.LongTermCredential;
import com.phono.srtplight.Log;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.crypto.tls.DatagramTransport;
import org.ice4j.ice.Candidate;
import org.ice4j.ice.CandidatePair;
import org.ice4j.ice.CandidateType;
import org.ice4j.ice.CompatibilityMode;
import org.ice4j.ice.Component;
import org.ice4j.ice.IceProcessingState;
import org.ice4j.ice.RemoteCandidate;

/**
 *
 * @author tim
 */
public class IceConnect implements PropertyChangeListener {

    private long startTime;
    protected Agent _localAgent;
    private static CertHolder _cert;
    private AssociationListener _al;
    private String _ffp;
    public Runnable cleanup;
    private DTLSEndpoint _dtls;
    protected boolean _dtlsClientRole = true;
    private boolean _wantToStartIce;
    protected boolean _haveLocalCandy;
    protected boolean _haveRemoteCandy;
    private boolean _iceStarted;

    public void setAssociationListener(AssociationListener al) {
        this._al = al;
    }

    IceConnect(int port) throws IOException, UnrecoverableEntryException, KeyStoreException, FileNotFoundException, NoSuchAlgorithmException, CertificateException {
        _cert = new JksCertHolder();
        _ffp = null;
        _localAgent = createAgent(true);
        _localAgent.addStateChangeListener(this);
        //_localAgent.setTa(250);

        _localAgent.setNominationStrategy(
                NominationStrategy.NOMINATE_FIRST_VALID);

        //let them fight ... fights forge character.
        _localAgent.setControlling(_dtlsClientRole);

        //STREAMS
        createStream(port, "data", _localAgent);

    }
    /*
     void addRemoteCandidate( ) {

     List<Component> localComponents = localStream.getComponents();

     for (Component localComponent : localComponents) {
     int id = localComponent.getComponentID();

     // localComponent.setDefaultRemoteCandidate(remoteComponent.getDefaultCandidate());
     localComponent.addRemoteCandidate(new RemoteCandidate(
     rCand.getTransportAddress(),
     localComponent,
     rCand.getType(),
     rCand.getFoundation(),
     rCand.getPriority(),
     null));

     }
     }
     */

    private void tryToStartIce() {
        if (_wantToStartIce && _haveRemoteCandy && _haveLocalCandy && !_iceStarted) {
            _iceStarted = true;
            _localAgent.startConnectivityEstablishment();
            Log.debug("Actually starting Ice ");

        } else {
            if (_iceStarted){
                Log.debug("Ice already started ");
            }else {
                Log.debug("cant start Ice yet "+_wantToStartIce +" "+ _haveRemoteCandy +" "+ _haveLocalCandy );
            }
        }
    }

    public void startIce() {
        _wantToStartIce = true;
        tryToStartIce();
    }

    void buildIce(String ufrag, String upass) throws InterruptedException, IllegalArgumentException, IOException {
        startTime = System.currentTimeMillis();

        long endTime = System.currentTimeMillis();

        //transferRemoteCandidates(localAgent, remotePeer);
        for (IceMediaStream stream : _localAgent.getStreams()) {
            stream.setRemoteUfrag(ufrag);
            stream.setRemotePassword(upass);
        }

        Log.info("Total candidate gathering time: "
                + (endTime - startTime) + "ms");
        Log.info("LocalAgent:\n" + _localAgent);


        /* Log.info("Local audio clist:\n"
         + _localAgent.getStream("audio").getCheckList());*/
    }

    String getUfrag() {
        return _localAgent.getLocalUfrag();
    }

    String getPass() {
        return _localAgent.getLocalPassword();
    }

    public String getPrint() {
        String ret = "";
        try {
            ret = _cert.getPrint();
        } catch (IOException ex) {
            Log.debug("cant get fingerprint" + ex.toString());
        }
        return ret;
    }

    protected void setFarFingerprint(String ffp) {
        _ffp = ffp;
    }

    String getFarFingerprint() {
        return _ffp;
    }

    protected void haveLocalCandy(boolean have) {
        _haveLocalCandy |= have;
        if (_haveLocalCandy) {
            tryToStartIce();
        }
    }
    protected void haveRemoteCandy(boolean have) {
        _haveRemoteCandy |= have;
        if (_haveRemoteCandy) {
            tryToStartIce();
        }
    }
    public List<Candidate> getCandidates() {
        ArrayList<Candidate> ret = new ArrayList();
        IceMediaStream st = getStream("data");
        if (st != null) {
            List<Component> a = st.getComponents();
            if (a != null) {
                for (Component comp : a) {
                    for (Candidate candy : comp.getLocalCandidates()) {
                        Log.debug("adding " + candy);
                        ret.add(candy);
                    }
                }
            }
        }
        haveLocalCandy(ret.size() > 0);
        return ret;
    }

    public DatagramTransport mkTransport(DatagramSocket lds, TransportAddress rta) {
        return new QueuingDatagramTransport(lds, rta);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Log.debug("got PCE evt on " + evt.getPropertyName() + " value is now " + evt.getNewValue());
        if (evt.getPropertyName().equals("IceProcessingState")) {
            IceProcessingState st = (IceProcessingState) evt.getNewValue();

            switch (st) {
                case COMPLETED:
                    synchronized (this) {
                        IceMediaStream s = getStream("data");

                        Component comp = s.getComponent(1);
                        Transport t = comp.getTransport();

                        CandidatePair cp = comp.getSelectedPair();
                        DatagramSocket lds = cp.getLocalCandidate().getDatagramSocket();

                        Log.debug("selected Datagram socket" + lds.toString());
                        TransportAddress rta = cp.getRemoteCandidate().getTransportAddress();
                        if (lds.isBound()) {
                            Log.debug("local ds bound to " + lds.getLocalSocketAddress());
                        } else {
                            Log.debug("local ds not bound "+ lds.getLocalSocketAddress());
                            break;
                        }
                        if (lds.isConnected()) {
                            Log.debug("local ds connected to" + lds.getRemoteSocketAddress());
                        } else {
                            Log.debug("local ds not connected "+ lds.getRemoteSocketAddress());
                        }
                        if (_dtls != null) {
                            Log.error("Duplicate  completion we have a DTLS service so now what ??? ");
                            break;
                        }
                        try {
                            DatagramTransport ds = mkTransport(lds, rta);
                            Log.debug("DTLS role " + (_dtlsClientRole ? "client" : "server"));

                            if (_dtlsClientRole) {
                                _dtls = new DTLSClient(_cert, ds, _al, _ffp);
                            } else {
                                _dtls = new DTLSServer(_cert, ds, _al, _ffp);
                            }
                        } catch (Exception ex) {
                            Log.debug("DTLS exception");
                            Log.error(ex.toString());
                            ex.printStackTrace();
                        }

                    }
                    break;
                case RUNNING:
                    Log.debug("Ice Running");
                    break;
                case FAILED:
                    Log.debug("Ice Failed");
                    cleanup.run();
                    break;
                case TERMINATED:
                    Log.debug("Ice Terminated");
                    _dtls = null;
                    //_localAgent.free();
                    //cleanup.run();
                    break;
                case WAITING:
                    Log.debug("Ice Waiting");
                    break;
            }
        }
    }

    protected static Agent createAgent(boolean isTrickling) throws IllegalArgumentException, IOException {
        long startTime = System.currentTimeMillis();
        Agent agent = new Agent(CompatibilityMode.RFC5245);
        agent.setTrickling(isTrickling);

        // STUN
        StunCandidateHarvester stunHarv = new StunCandidateHarvester(
                new TransportAddress("stun.l.google.com", 19302, Transport.UDP));

        agent.addCandidateHarvester(stunHarv);
        // TURN
        String[] hostnames = new String[]{"146.148.121.175"};
        int port = 3478;
        LongTermCredential longTermCredential
                = new LongTermCredential("pet", "snoopy");

        for (String hostname : hostnames) {
            agent.addCandidateHarvester(
                    new TurnCandidateHarvester(
                            new TransportAddress(hostname, port, Transport.UDP),
                            longTermCredential));
        }

        //UPnP: adding an UPnP harvester because they are generally slow
        //which makes it more convenient to test things like trickle.
        //agent.addCandidateHarvester(new UPNPHarvester());
        long endTime = System.currentTimeMillis();
        long total = endTime - startTime;

        Log.info("Total harvesting time: " + total + "ms.");
        return agent;
    }

    private static IceMediaStream createStream(int rtpPort,
            String streamName,
            Agent agent) throws IllegalArgumentException, IOException {
        IceMediaStream stream = agent.createMediaStream(streamName);

        long startTime = System.currentTimeMillis();

        //TODO: component creation should probably be part of the library. it
        //should also be started after we've defined all components to be
        //created so that we could run the harvesting for everyone of them
        //simultaneously with the others.
        //rtp
        agent.createComponent(
                stream, Transport.UDP, rtpPort, rtpPort, rtpPort + 100);

        long endTime = System.currentTimeMillis();
        Log.info("RTP Component created in "
                + (endTime - startTime) + " ms");
        startTime = endTime;
        /*
         //rtcpComp
         agent.createComponent(
         stream, Transport.UDP, rtpPort + 1, rtpPort + 1, rtpPort + 101);

         endTime = System.currentTimeMillis();
         Log.info("RTCP Component created in "
         + (endTime - startTime) + " ms");
         */
        return stream;
    }

    void addCandidate(String foundation, String component, String protocol, String priority, String ip, String port, String type) {
        IceMediaStream localStream = getStream("data");
        List<Component> localComponents = localStream.getComponents();
        int cid = Integer.parseInt(component);
        boolean rc = false;
        for (Component localComponent : localComponents) {
            int id = localComponent.getComponentID();
            if (cid == id) {
                int iport = Integer.parseInt(port);
                long lpriority = Long.parseLong(priority);
                TransportAddress ta = new TransportAddress(ip, iport, Transport.parse(protocol));
                // localComponent.setDefaultRemoteCandidate(remoteComponent.getDefaultCandidate());
                localComponent.addRemoteCandidate(new RemoteCandidate(
                        ta,
                        localComponent,
                        CandidateType.parse(type),
                        foundation,
                        lpriority,
                        null));
                rc = true;
            }
        }
        if (rc){
            haveRemoteCandy(true);
        }
        
    }

    IceMediaStream getStream(String target) {
        IceMediaStream ret = null;
        List<IceMediaStream> l = _localAgent.getStreams();
        Log.debug(" count of streams =" + l.size());
        for (IceMediaStream stream : l) {
            if (target.equals(stream.getName())) {
                Log.debug("found " + target);
                ret = stream;
                break;
            } else {
                Log.debug("looking at " + stream.getName());
            }
        }
        return ret;
    }

    public static void main(String[] argv) {
        Log.setLevel(Log.DEBUG);
        try {
            IceConnect ice = new IceConnect(12345);
            ice.buildIce("wbrUTaHy7SNtu20y", "jg/ezzHpJSLGJvUB3N8XwXj9");
            ice.addCandidate("2169522962", "1", "udp", "1509957375", "192.67.4.33", "54321", "host");
            Log.debug("local ufrag " + ice.getUfrag());
            Log.debug("local pass " + ice.getPass());
            List<Candidate> candies = ice.getCandidates();
            for (Candidate c : candies) {
                Log.debug("candy :" + c.toString());
            }
            ice.startIce();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void freeAgent() {
        if (_localAgent != null) {
            _localAgent.free();
            _localAgent = null;
        }
    }

}
