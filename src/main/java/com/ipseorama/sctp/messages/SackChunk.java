/*
 * Copyright (C) 2014 Westhawk Ltd<thp@westhawk.co.uk>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.ipseorama.sctp.messages;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class SackChunk extends Chunk {

    /**
     * @return the cumuTSNAck
     */
    public long getCumuTSNAck() {
        return _cumuTSNAck;
    }

    /**
     * @param _cumuTSNAck the _cumuTSNAck to set
     */
    public void setCumuTSNAck(long cumuTSNAck) {
        _cumuTSNAck = cumuTSNAck;
    }

    /**
     * @return the _arWin
     */
    public long getArWin() {
        return _arWin;
    }

    /**
     * @param _arWin the _arWin to set
     */
    public void setArWin(long arWin) {
        _arWin = arWin;
    }
    /*
    
     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |   Type = 3    |Chunk  Flags   |      Chunk Length             |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                      Cumulative TSN Ack                       |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |          Advertised Receiver Window Credit (a_rwnd)           |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     | Number of Gap Ack Blocks = N  |  Number of Duplicate TSNs = X |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |  Gap Ack Block #1 Start       |   Gap Ack Block #1 End        |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     /                                                               /
     \                              ...                              \
     /                                                               /
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |   Gap Ack Block #N Start      |  Gap Ack Block #N End         |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                       Duplicate TSN 1                         |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     /                                                               /
     \                              ...                              \
     /                                                               /
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                       Duplicate TSN X                         |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */



    public class GapBlock {
        char _start;
        char _end;
        GapBlock(ByteBuffer b){
            _start = b.getChar();
            _end = b.getChar();
        }
        GapBlock(char start){
            _start = start;
        }
        void setEnd(char end){
            _end = end;
        }

        private void put(ByteBuffer b) {
            b.putChar(_start);
            b.putChar(_end);
        }
        public char getStart(){
            return _start;
        }
        public char getEnd(){
            return _end;
        }
    }

    GapBlock[] _gaps;
    long[] _duplicateTSNs;
    private long _cumuTSNAck;
    private long _arWin;

    public SackChunk(byte type, byte flags, int length, ByteBuffer pkt) {
        super(type, flags, length, pkt);
        _cumuTSNAck = Chunk.getUnsignedInt(_body);
        _arWin = Chunk.getUnsignedInt(_body);
        int ngaps = _body.getChar();
        int nDTSNs = _body.getChar();
        _gaps = new GapBlock[ngaps];
        _duplicateTSNs = new long[nDTSNs];
        for (int i=0;i<ngaps;i++){
            _gaps[i] = new GapBlock(_body);
        }
        for (int i=0;i<nDTSNs;i++){
            _duplicateTSNs[i] = Chunk.getUnsignedInt(_body);
        }
    }

    public GapBlock [] getGaps(){
        return _gaps;
    }
    public long [] getDupTSNs(){
        return _duplicateTSNs;
    }
    public SackChunk() {
        super((byte) SACK);
        _gaps = new GapBlock[0];
        _duplicateTSNs = new long[0];
    }
    
    public void setDuplicates(ArrayList<Long> dups) {
        _duplicateTSNs = new long[dups.size()];
        int i=0;
        for (Long du:dups){
            _duplicateTSNs[i++]= du.longValue();
        }
    }
    
    public void setGaps(ArrayList<Long> seenTsns){
        long cuTsn = _cumuTSNAck;
        ArrayList<GapBlock> gaplist = new ArrayList();
        GapBlock currentGap = null;
        char prevoff = 0;
        for(Long t:seenTsns){
            char offs = (char) (t.longValue() - cuTsn);
            if (currentGap == null){
                currentGap = new GapBlock(offs);
                currentGap.setEnd(offs);
                gaplist.add(currentGap);
            } else {
                if (offs == prevoff +1){
                    currentGap.setEnd(offs);
                } else {
                    currentGap = new GapBlock(offs);
                    currentGap.setEnd(offs);
                    gaplist.add(currentGap);
                }
            }
            prevoff = offs;
        }
        _gaps = new GapBlock[gaplist.size()];
        int i=0;
        for (GapBlock g:gaplist){
            _gaps[i++] = g;
        }
    }
    
    @Override
    void putFixedParams(ByteBuffer ret) {
        Chunk.putUnsignedInt(ret, _cumuTSNAck);
        Chunk.putUnsignedInt(ret,_arWin);
        ret.putChar((char)_gaps.length);
        ret.putChar((char)_duplicateTSNs.length);
        for (int i=0;i<_gaps.length;i++){
            _gaps[i].put(ret);
        }
        for (int i=0;i<_duplicateTSNs.length;i++){
             Chunk.putUnsignedInt(ret,_duplicateTSNs[i]);
        }
    }
    public String toString(){
        StringBuffer ret= new StringBuffer("SACK cumuTSNAck="+_cumuTSNAck)
                .append(" _arWin="+_arWin)
                .append(" _gaps="+_gaps.length+" [");
                for (GapBlock g:_gaps){
                    ret.append("\n\t{"+(int)g._start+","+(int)g._end+"}");
                }
                ret.append("]\n _duplicateTSNs="+_duplicateTSNs.length);
                for( long t:_duplicateTSNs){
                    ret.append("\n\t"+t);
                }
        return ret.toString();
    }
}
