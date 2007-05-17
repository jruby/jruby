/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.socket;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubyIPSocket extends RubyBasicSocket {
    static void createIPSocket(Ruby runtime) {
        RubyClass rb_cIPSocket = runtime.defineClass("IPSocket", runtime.getClass("BasicSocket"), IPSOCKET_ALLOCATOR);
        CallbackFactory cfact = runtime.callbackFactory(RubyIPSocket.class);

        rb_cIPSocket.defineFastMethod("addr", cfact.getFastMethod("addr"));
        rb_cIPSocket.defineFastMethod("peeraddr", cfact.getFastMethod("peeraddr"));
        rb_cIPSocket.getMetaClass().defineFastMethod("getaddress", cfact.getFastSingletonMethod("getaddress", IRubyObject.class));

        runtime.getObject().setConstant("IPsocket",rb_cIPSocket);
    }
    
    private static ObjectAllocator IPSOCKET_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyIPSocket(runtime, klass);
        }
    };

    public RubyIPSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    protected static RuntimeException sockerr(IRubyObject recv, String msg) {
        return new RaiseException(recv.getRuntime(), recv.getRuntime().getClass("SocketError"), null, true);
    }

    private IRubyObject addrFor(InetSocketAddress addr) {
        IRubyObject[] ret = new IRubyObject[4];
        Ruby r = getRuntime();
        ret[0] = r.newString("AF_INET");
        ret[1] = r.newFixnum(addr.getPort());
        if(r.isDoNotReverseLookupEnabled()) {
            ret[2] = r.newString(addr.getAddress().getHostAddress());
        } else {
            ret[2] = r.newString(addr.getHostName());
        }
        ret[3] = r.newString(addr.getAddress().getHostAddress());
        return r.newArrayNoCopy(ret);
    }

    public IRubyObject addr() {
        return addrFor(getLocalSocket());
    }

    public IRubyObject peeraddr() {
        return addrFor(getRemoteSocket());
    }

    public static IRubyObject getaddress(IRubyObject recv, IRubyObject hostname) {
        try {
            return recv.getRuntime().newString(InetAddress.getByName(hostname.convertToString().toString()).getHostAddress());
        } catch(UnknownHostException e) {
            throw sockerr(recv, "getaddress: name or service not known");
        }
    }
}// RubyIPSocket
