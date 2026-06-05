/**
 * thing that attempts to implement SNAP
 */
package pe.pi.sctp4j.sctp.small;

import com.phono.srtplight.Log;
import java.nio.ByteBuffer;
import org.bouncycastle.tls.DTLSTransport;
import pe.pi.sctp4j.sctp.AssociationListener;
import pe.pi.sctp4j.sctp.messages.Chunk;
import pe.pi.sctp4j.sctp.messages.InitChunk;
import pe.pi.sctp4j.sctp.messages.exceptions.MessageException;
import pe.pi.sctp4j.sctp.messages.exceptions.SctpPacketFormatException;
import pe.pi.sctp4j.sctp.messages.exceptions.UnreadyAssociationException;

/**
 *
 * @author thp
 */
public class SNAPAssociation extends ThreadedAssociation {

    private boolean haveset = false;
    private boolean havegot = false;
    private boolean haveSentEstablished = false;

    public SNAPAssociation() {
        super(null, null);
        Log.debug("new SNAPAssociation");

    }

    private void checkPreconditions() {
        if ((!haveSentEstablished)
                && (_transp != null)
                && (_al != null)
                && (haveset)
                && (havegot)) {
            _state = State.ESTABLISHED;
            haveSentEstablished = true;
            Log.debug("SNAP preconditions met. Calling onAssociated() on "+_al.getClass().getName());
            _al.onAssociated(this);
        }

    }

    public void setEstablished(byte[] chunk) throws Exception {

        if (this._state != State.CLOSED) {
            throw new UnreadyAssociationException();
        }
        var bb = ByteBuffer.wrap(chunk);

        var first = Chunk.mkChunk(bb);
        if (!(first instanceof InitChunk)) {
            throw new MessageException();
        }
        var init = (InitChunk) first;
        inboundInit(init);
        Log.debug("haveset Init Chunk from SNAP sctp-init:b64 " + init.toString());
        haveset = true;
    }

    public byte[] getEstablished() throws Exception {
        var c = new InitChunk(){
            byte [] toBuff() throws SctpPacketFormatException{
                var bb = ByteBuffer.allocate(500);
                this.write(bb);
                var dst = new byte[bb.position()];
                bb.get(0,dst);
                return dst;
            }
        };
        c.setInitialTSN(this._nearTSN);
        c.setNumInStreams(SNAPAssociation.MAXSTREAMS);
        c.setNumOutStreams(SNAPAssociation.MAXSTREAMS);
        c.setAdRecWinCredit(SNAPAssociation.MAXBUFF);
        c.setInitiate(this.getMyVerTag());
        byte[] sex = getSupportedExtensions();

        c.setSupportedExtensions(sex);
        
        havegot = true;
        Log.debug("havegot Init Chunk for SNAP sctp-init:b64 " + c.toString());
        return c.toBuff();
    }

    public void setTransport(DTLSTransport trans) {
        Log.debug("late set of transport in SNAP association ");

        super._transp = trans;
    }

    public void setListener(AssociationListener li) {
        Log.debug("late set of listener in SNAP association ");

        super._al = new ExecutorAssociationListener(li);
    }

    @Override
    public boolean canSend() {
        return ((super._transp != null) && (super.canSend()));
    }

}
