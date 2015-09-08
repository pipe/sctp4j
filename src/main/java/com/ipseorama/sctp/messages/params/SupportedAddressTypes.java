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
package com.ipseorama.sctp.messages.params;

import com.ipseorama.sctp.messages.params.VariableParam;
import java.nio.ByteBuffer;

/**
 *
 * @author tim
 */
public class SupportedAddressTypes extends KnownParam {

    int supported[];

    public SupportedAddressTypes(int t, String n) {
        super(t, n);
    }

    public void readBody(ByteBuffer body, int blen) {
        supported = new int[blen / 2];
        for (int i = 0; i < supported.length; i++) {
            supported[i] = body.getChar();
        }
    }

    public void writeBody(ByteBuffer body) {
        if (supported != null) {
            for (int i = 0; i < supported.length; i++) {
                body.putChar((char) supported[i]);
            }
        }
    }
}
