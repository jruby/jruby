/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2010 Joshua Go <joshuago@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.ext.socket;

import jnr.constants.platform.IP;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;


/**
 * @author <a href="mailto:joshuago@gmail.com">Joshua Go</a>
 */
public class MulticastStateManager {
    private MulticastSocket multicastSocket;
    private final ArrayList membershipGroups;
    public static final int IP_ADD_MEMBERSHIP = IP.IP_ADD_MEMBERSHIP.intValue();

    public MulticastStateManager() {
        membershipGroups = new ArrayList();
    }

    public void addMembership(byte [] ipaddr_buf) throws IOException {
        String ipString = "";
        if (ipaddr_buf.length >= 4) {
            ipString += String.valueOf((int) ipaddr_buf[0] & 0xff);
            ipString += ".";
            ipString += String.valueOf((int) ipaddr_buf[1] & 0xff);
            ipString += ".";
            ipString += String.valueOf((int) ipaddr_buf[2] & 0xff);
            ipString += ".";
            ipString += String.valueOf((int) ipaddr_buf[3] & 0xff);
        }

        membershipGroups.add(ipString);
        updateMemberships();
    }

    public void rebindToPort(int port) throws IOException {
        if (multicastSocket != null) {
            multicastSocket.close();
        }

        multicastSocket = new MulticastSocket(port);
        updateMemberships();
    }

    public MulticastSocket getMulticastSocket() {
        return multicastSocket;
    }

    private void updateMemberships() throws IOException {
        if (multicastSocket == null) {
            return;
        }

        for (int i = 0; i < membershipGroups.size(); i++) {
            String ipString = (String) membershipGroups.get(i);
            InetAddress group = InetAddress.getByName(ipString);
            multicastSocket.joinGroup(new InetSocketAddress(group, 0), null);
        }
    }

}
