/*
 * Copyright 2017 pi.pe gmbh .
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package pe.pi.sctp4j.sctp.leakey;

import pe.pi.sctp4j.sctp.AssociationListener;
import pe.pi.sctp4j.sctp.small.ThreadedAssociation;
import org.bouncycastle.tls.DatagramTransport;

/**
 *
 * @author Westhawk Ltd<thp@westhawk.co.uk>
 */
public class LeakyAssociation extends ThreadedAssociation {

    public LeakyAssociation(DatagramTransport transport, AssociationListener al) {
        super(new LeakyTransport(transport), al);
    }
    
}

