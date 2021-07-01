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

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.Sockaddr;

import java.net.InetSocketAddress;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
@JRubyClass(name="IPSocket", parent="BasicSocket")
public class RubyIPSocket extends RubyBasicSocket {
    static void createIPSocket(Ruby runtime) {
        RubyClass rb_cIPSocket = runtime.defineClass("IPSocket", runtime.getClass("BasicSocket"), RubyIPSocket::new);

        rb_cIPSocket.defineAnnotatedMethods(RubyIPSocket.class);
        rb_cIPSocket.undefineMethod("initialize");
    }

    public RubyIPSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(name = "addr")
    public IRubyObject addr(ThreadContext context) {
        return addrCommon(context, !context.getRuntime().isDoNotReverseLookupEnabled());
    }

    @JRubyMethod(name = "addr")
    public IRubyObject addr(ThreadContext context, IRubyObject reverse) {
        return addrCommon(context, reverse);
    }

    @JRubyMethod(name = "peeraddr")
    public IRubyObject peeraddr(ThreadContext context) {
        return peeraddrCommon(context, !context.getRuntime().isDoNotReverseLookupEnabled());
    }

    @JRubyMethod(name = "peeraddr")
    public IRubyObject peeraddr(ThreadContext context, IRubyObject reverse) {
        return peeraddrCommon(context, reverse);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject getaddress(ThreadContext context, IRubyObject self, IRubyObject hostname) {
        return SocketUtils.getaddress(context, hostname);
    }

    @JRubyMethod
    public IRubyObject recvfrom(ThreadContext context, IRubyObject _length) {
        Ruby runtime = context.runtime;

        IRubyObject result = recv(context, _length);
        InetSocketAddress sender = getInetRemoteSocket();

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

        IRubyObject addressArray = context.runtime.newArrayNoCopy(
                runtime.newString("AF_INET"),
                runtime.newFixnum(port),
                runtime.newString(hostName),
                runtime.newString(hostAddress)
        );

        return runtime.newArray(result, addressArray);
    }

    @JRubyMethod
    public IRubyObject recvfrom(ThreadContext context, IRubyObject _length, IRubyObject _flags) {
        // TODO: implement flags
        return recvfrom(context, _length);
    }

    @JRubyMethod(name = "getpeereid", notImplemented = true)
    public IRubyObject getpeereid(ThreadContext context) {
        throw context.runtime.newNotImplementedError("getpeereid not implemented");
    }

    @Override
    protected IRubyObject getSocknameCommon(ThreadContext context, String caller) {
        InetSocketAddress sock = getInetSocketAddress();

        return Sockaddr.packSockaddrFromAddress(context, sock);
    }

    @Override
    public IRubyObject getpeername(ThreadContext context) {
       InetSocketAddress sock = getInetRemoteSocket();

       return Sockaddr.packSockaddrFromAddress(context, sock);
    }

    private IRubyObject addrCommon(ThreadContext context, IRubyObject reverse) {
        Boolean doReverse = doReverseLookup(context, reverse);
        if (doReverse == null) doReverse = false;

        return addrCommon(context, doReverse);
    }

    private IRubyObject addrCommon(ThreadContext context, boolean reverse) {
        InetSocketAddress address = getInetSocketAddress();

        checkAddress(context, address);

        return addrFor(context, address, reverse);
    }

    private void checkAddress(ThreadContext context, InetSocketAddress address) {
        if (address == null) {
            throw context.runtime.newErrnoENOTSOCKError("Not socket or not connected");
        }
    }

    private IRubyObject peeraddrCommon(ThreadContext context, IRubyObject reverse) {
        Boolean doReverse = doReverseLookup(context, reverse);
        if (doReverse == null) doReverse = !context.runtime.isDoNotReverseLookupEnabled();

        return peeraddrCommon(context, doReverse);
    }

    private IRubyObject peeraddrCommon(ThreadContext context, boolean reverse) {
        InetSocketAddress address = getInetRemoteSocket();

        checkAddress(context, address);

        return addrFor(context, address, reverse);
    }

    public static Boolean doReverseLookup(ThreadContext context, IRubyObject noreverse) {
        if (noreverse == context.tru) {
            return false;
        } else if (noreverse == context.fals) {
            return true;
        } else if (noreverse == context.nil) {
            return null;
        } else {
            Ruby runtime = context.runtime;

            TypeConverter.checkType(context, noreverse, runtime.getSymbol());
            switch (noreverse.toString()) {
                case "numeric": return true;
                case "hostname": return false;
                default: throw runtime.newArgumentError("invalid reverse_lookup flag: " + noreverse);
            }
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
