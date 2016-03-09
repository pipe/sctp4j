/*
 * Copyright (C) 2014 tim
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
package com.ipseorama.sctp.behave;

import com.ipseorama.sctp.SCTPStream;
import com.ipseorama.sctp.SCTPStreamListener;
import com.ipseorama.sctp.messages.Chunk;
import com.ipseorama.sctp.messages.DataChunk;
import java.util.SortedSet;

/**
 *
 * @author tim
 */
public interface SCTPStreamBehaviour {

    // Something has happend to the stream, this is our chance to respond.
    // typically this means sending nothing
    public Chunk[] respond(SCTPStream a);

    // we have a sorted queue of datachunks for this stream to deliver
    // according to the appropriate behaviour.
    public void deliver(SCTPStream s, SortedSet<DataChunk> a, SCTPStreamListener l) ;

}
