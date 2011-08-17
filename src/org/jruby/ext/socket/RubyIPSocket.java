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
 * Copyright (C) 2007-2010 JRuby Team <team@jruby.org>
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
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.BadDescriptorException;
import static org.jruby.CompatVersion.*;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
@JRubyClass(name="IPSocket", parent="BasicSocket")
public class RubyIPSocket extends RubyBasicSocket {
    static void createIPSocket(Ruby runtime) {
        RubyClass rb_cIPSocket = runtime.defineClass("IPSocket", runtime.fastGetClass("BasicSocket"), IPSOCKET_ALLOCATOR);
        
        rb_cIPSocket.defineAnnotatedMethods(RubyIPSocket.class);

        runtime.getObject().fastSetConstant("IPsocket",rb_cIPSocket);
    }
    
    private static ObjectAllocator IPSOCKET_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyIPSocket(runtime, klass);
        }
    };

    public RubyIPSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }
    
    protected static RuntimeException sockerr(Ruby runtime, String msg) {
        return new RaiseException(runtime, runtime.fastGetClass("SocketError"), msg, true);
    }

    public IRubyObject packSockaddrFromAddress(InetSocketAddress sock, ThreadContext context) {
        if (sock == null) {
            return RubySocket.pack_sockaddr_in(context, this, 0, "");
        } else {
            return RubySocket.pack_sockaddr_in(context, sock);
        }
    }

    private IRubyObject addrFor(ThreadContext context, InetSocketAddress addr, boolean reverse) {
        Ruby r = context.getRuntime();
        IRubyObject[] ret = new IRubyObject[4];
        ret[0] = r.newString("AF_INET");
        ret[1] = r.newFixnum(addr.getPort());
        String hostAddress = addr.getAddress().getHostAddress();
        if (!reverse || doNotReverseLookup(context)) {
            ret[2] = r.newString(hostAddress);
        } else {
            ret[2] = r.newString(addr.getHostName());
        }
        ret[3] = r.newString(hostAddress);
        return r.newArrayNoCopy(ret);
    }
    
    @Deprecated
    public IRubyObject addr() {
        return addr(getRuntime().getCurrentContext());
    }
    
    @JRubyMethod
    public IRubyObject addr(ThreadContext context) {
        return addrCommon(context, true);
    }
    
    @JRubyMethod(name = "addr", compat = RUBY1_9)
    public IRubyObject addr19(ThreadContext context) {
        return addrCommon(context, true);
    }
    
    @JRubyMethod(name = "addr", compat = RUBY1_9)
    public IRubyObject addr19(ThreadContext context, IRubyObject reverse) {
        return addrCommon(context, reverse.isTrue());
    }
    
    private IRubyObject addrCommon(ThreadContext context, boolean reverse) {
        try {
            InetSocketAddress address = getLocalSocket("addr");
            if (address == null) {
                throw context.getRuntime().newErrnoENOTSOCKError("Not socket or not connected");
            }
            return addrFor(context, address, reverse);
        } catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError();
        }
    }
    
    @Deprecated
    public IRubyObject peeraddr() {
        return peeraddr(getRuntime().getCurrentContext());
    }
    
    @JRubyMethod
    public IRubyObject peeraddr(ThreadContext context) {
        return peeraddrCommon(context, true);
    }
    
    @JRubyMethod(name = "peeraddr", compat = RUBY1_9)
    public IRubyObject peeraddr19(ThreadContext context) {
        return peeraddrCommon(context, true);
    }
    
    @JRubyMethod(name = "peeraddr", compat = RUBY1_9)
    public IRubyObject peeraddr19(ThreadContext context, IRubyObject reverse) {
        return peeraddrCommon(context, reverse.isTrue());
    }
    
    private IRubyObject peeraddrCommon(ThreadContext context, boolean reverse) {
        try {
            InetSocketAddress address = getRemoteSocket();
            if (address == null) {
                throw context.getRuntime().newErrnoENOTSOCKError("Not socket or not connected");
            }
            return addrFor(context, address, reverse);
        } catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError();
        }
    }
    
    @Override
    protected IRubyObject getSocknameCommon(ThreadContext context, String caller) {
        try {
            InetSocketAddress sock = getLocalSocket(caller);
            return packSockaddrFromAddress(sock, context);
        } catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError();
        }
    }
    @Override
    public IRubyObject getpeername(ThreadContext context) {
        try {
            InetSocketAddress sock = getRemoteSocket();
            return packSockaddrFromAddress(sock, context);
        } catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError();
        }
    }
    @Deprecated
    public static IRubyObject getaddress(IRubyObject recv, IRubyObject hostname) {
        return getaddress(recv.getRuntime().getCurrentContext(), recv, hostname);
    }
    @JRubyMethod(meta = true)
    public static IRubyObject getaddress(ThreadContext context, IRubyObject recv, IRubyObject hostname) {
        try {
            return context.getRuntime().newString(InetAddress.getByName(hostname.convertToString().toString()).getHostAddress());
        } catch(UnknownHostException e) {
            throw sockerr(context.getRuntime(), "getaddress: name or service not known");
        }
    }

    @JRubyMethod(required = 1, optional = 1)
    public IRubyObject recvfrom(ThreadContext context, IRubyObject[] args) {
        try {
            IRubyObject result = recv(context, args);
            InetSocketAddress sender = getRemoteSocket();

            int port;
            String hostName;
            String hostAddress;

            if (sender == null) {
                port = 0;
                hostName = hostAddress = "0.0.0.0";
            } else {
                port = sender.getPort();
                hostName = sender.getHostName();
                hostAddress = sender.getAddress().getHostAddress();
            }

            IRubyObject addressArray = context.getRuntime().newArray(
                    new IRubyObject[] {
                            context.getRuntime().newString("AF_INET"),
                            context.getRuntime().newFixnum(port),
                            context.getRuntime().newString(hostName),
                            context.getRuntime().newString(hostAddress)
                    });

            return context.getRuntime().newArray(new IRubyObject[] { result, addressArray });
        } catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError();
        }
    }

}// RubyIPSocket
