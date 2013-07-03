/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.socket;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.jruby.CompatVersion;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.BadDescriptorException;
import org.jruby.util.io.Sockaddr;

import static org.jruby.CompatVersion.*;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
@JRubyClass(name="IPSocket", parent="BasicSocket")
public class RubyIPSocket extends RubyBasicSocket {
    static void createIPSocket(Ruby runtime) {
        RubyClass rb_cIPSocket = runtime.defineClass("IPSocket", runtime.getClass("BasicSocket"), IPSOCKET_ALLOCATOR);
        
        rb_cIPSocket.defineAnnotatedMethods(RubyIPSocket.class);

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

    @JRubyMethod(meta = true)
    public static IRubyObject getaddress(ThreadContext context, IRubyObject self, IRubyObject hostname) {
        return SocketUtils.getaddress(context, hostname);
    }

    @JRubyMethod
    public IRubyObject recvfrom(ThreadContext context, IRubyObject _length) {
        Ruby runtime = context.runtime;

        try {
            IRubyObject result = recv(context, _length);
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

            IRubyObject addressArray = context.runtime.newArray(
                    new IRubyObject[]{
                            runtime.newString("AF_INET"),
                            runtime.newFixnum(port),
                            runtime.newString(hostName),
                            runtime.newString(hostAddress)
                    });

            return runtime.newArray(result, addressArray);

        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        }
    }

    @JRubyMethod
    public IRubyObject recvfrom(ThreadContext context, IRubyObject _length, IRubyObject _flags) {
        // TODO: implement flags
        return recvfrom(context, _length);
    }

    @JRubyMethod(name = "getpeereid", compat = CompatVersion.RUBY1_9, notImplemented = true)
    public IRubyObject getpeereid(ThreadContext context) {
        throw context.runtime.newNotImplementedError("getpeereid not implemented");
    }

    @Override
    protected IRubyObject getSocknameCommon(ThreadContext context, String caller) {
        try {
            InetSocketAddress sock = getSocketAddress();

            return Sockaddr.packSockaddrFromAddress(context, sock);

        } catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError();
        }
    }

    @Override
    public IRubyObject getpeername(ThreadContext context) {
        try {
            InetSocketAddress sock = getRemoteSocket();

            return Sockaddr.packSockaddrFromAddress(context, sock);

        } catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError();
        }
    }

    private IRubyObject addrCommon(ThreadContext context, boolean reverse) {
        try {
            InetSocketAddress address = getSocketAddress();

            if (address == null) {
                throw context.runtime.newErrnoENOTSOCKError("Not socket or not connected");
            }

            return addrFor(context, address, reverse);

        } catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError();
        }
    }

    private IRubyObject peeraddrCommon(ThreadContext context, boolean reverse) {
        try {
            InetSocketAddress address = getRemoteSocket();

            if (address == null) {
                throw context.runtime.newErrnoENOTSOCKError("Not socket or not connected");
            }

            return addrFor(context, address, reverse);

        } catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError();
        }
    }

    @Deprecated
    public IRubyObject addr() {
        return addr(getRuntime().getCurrentContext());
    }

    @Deprecated
    public IRubyObject peeraddr() {
        return peeraddr(getRuntime().getCurrentContext());
    }

    @Deprecated
    public static IRubyObject getaddress(IRubyObject recv, IRubyObject hostname) {
        return getaddress(recv.getRuntime().getCurrentContext(), recv, hostname);
    }

    @Deprecated
    public IRubyObject recvfrom(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 1:
                return recvfrom(context, args[0]);
            case 2:
                return recvfrom(context, args[0], args[1]);
            default:
                Arity.raiseArgumentError(context.runtime, args, 1, 2);
                return null; // not reached
        }
    }

}// RubyIPSocket
