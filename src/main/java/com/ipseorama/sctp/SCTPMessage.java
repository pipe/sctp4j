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
package com.ipseorama.sctp;

import com.ipseorama.sctp.messages.DataChunk;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class SCTPMessage {

    private final SCTPStream _stream;
    private final byte[] _data;
    private int _offset = 0;
    private int _pPid = 0;
    private int _hiSeq; // note do we need these ?
    private int _loSeq;
    
    /**
     * Outbound message - note that we assume no one will mess with data between
     * calls to fill()
     *
     * @param data
     * @param s
     */
    public SCTPMessage(byte[] data, SCTPStream s) {
        _data = data;
        _stream = s;
        _pPid = DataChunk.WEBRTCBINARY;
    }

    public SCTPMessage(String data, SCTPStream s) {
        _data = data.getBytes();
        _stream = s;
        _pPid = DataChunk.WEBRTCSTRING;
    }

    public SCTPMessage(SCTPStream s,SortedSet<DataChunk> chunks) {
        _stream = s;
        int tot = 0;
        if ((chunks.first().getFlags() & DataChunk.BEGINFLAG) == 0){
            throw new IllegalArgumentException("must start with 'start' chunk");
        }
        if ((chunks.last().getFlags() & DataChunk.ENDFLAG) == 0){
            throw new IllegalArgumentException("must end with 'end' chunk");
        }
        int count = chunks.last().getSSeqNo() -chunks.first().getSSeqNo();
        if (chunks.size() != count){
            throw new IllegalArgumentException("chunk count is wrong "+chunks.size() +" vs "+count);
        }
        for (DataChunk dc:chunks){
            tot += dc.getDataSize();
        }
        _data = new byte[tot];
        int offs = 0;
        for (DataChunk dc:chunks){
            System.arraycopy(dc.getData(), 0, _data, offs, dc.getDataSize());
            offs+= dc.getDataSize();
        }
    }

    public SCTPMessage(SCTPStream s,DataChunk singleChunk) {
        _stream = s;
        int flags = singleChunk.getFlags();
        if ((flags & singleChunk.SINGLEFLAG) > 0) {
            _data = singleChunk.getData();
        } else {
            throw new IllegalArgumentException("must use a 'single' chunk");
        }
    }
    
    public void setCompleteHandler(MessageCompleteHandler mch) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean hasMoreData() {
        return (_offset < _data.length);
    }


    /**
     * available datachunks are put here to be filled with data from this
     * outbound message
     *
     * @param dc
     */
    public void fill(DataChunk dc) {
        boolean last = false;
        boolean first = false;
        int dsz = dc.getCapacity();
        int remain = _data.length - _offset;
        if (_offset == 0) {
            if (remain <= dsz) {
                // only one chunk
                dc.setFlags(dc.SINGLEFLAG);
                dc.setData(_data);
                _offset = _data.length;
                last = true;
                first = true;
            } else {
                // first chunk of many
                dc.setFlags(dc.BEGINFLAG);
                dc.setData(_data, _offset, dsz);
                _offset += dsz;
                first = true;
            }
        } else // not first
        if (remain <= dsz) {
            // last chunk, this will all fit.
            dc.setFlags(dc.ENDFLAG);
            dc.setData(_data, _offset, remain);
            _offset += remain; // should be _data_length now
            last = true;
        } else {
            // middle chunk.
            dc.setData(_data, _offset, dsz);
            _offset += dsz;
        }
        dc.setPpid(_pPid);

        _stream.outbound(dc);
        if (last) {
            _hiSeq = dc.getSSeqNo();
        }
        if (first) {
            _loSeq = dc.getSSeqNo();
        }
    }

    public byte[] getData() {
        return _data;
    }

}
