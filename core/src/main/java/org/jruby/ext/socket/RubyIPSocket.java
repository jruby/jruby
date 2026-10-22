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

import org.jruby.util.io.OpenFile;
import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.Sockaddr;

import java.net.InetSocketAddress;

import static org.jruby.api.Access.symbolClass;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.*;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;

@JRubyClass(name="IPSocket", parent="BasicSocket")
public class RubyIPSocket extends RubyBasicSocket {
    static RubyClass createIPSocket(ThreadContext context, RubyClass BasicSocket) {
        return defineClass(context, "IPSocket", BasicSocket, RubyIPSocket::new).
                defineMethods(context, RubyIPSocket.class).
                undefMethods(context, "initialize", "getpeereid"); // MRI undefines getpeereid on IPSocket
    }

    public RubyIPSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    /** MRI: #<TCPSocket:fd 9, AF_INET, 127.0.0.1, 48410>, or #<TCPSocket:(closed)> (UDPSocket has its own) */
    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        OpenFile fptr = this.openFile;
        String className = getMetaClass().getRealClass().getName(context);
        if (fptr == null || fptr.fd() == null || !fptr.isOpen()) return newString(context, "#<" + className + ":(closed)>");

        StringBuilder buf = new StringBuilder("#<").append(className).append(":fd ").append(fptr.fd().bestFileno());
        InetSocketAddress local = getInetSocketAddress();
        if (local != null) {
            buf.append(local.getAddress() instanceof java.net.Inet6Address ? ", AF_INET6, " : ", AF_INET, ")
                    .append(SocketUtils.ipString(local.getAddress())).append(", ").append(local.getPort());
        }
        return newString(context, buf.append('>').toString());
    }

    @JRubyMethod(name = "addr")
    public IRubyObject addr(ThreadContext context) {
        return addrCommon(context, !doNotReverseLookup(context));
    }

    @JRubyMethod(name = "addr")
    public IRubyObject addr(ThreadContext context, IRubyObject reverse) {
        return addrCommon(context, reverse);
    }

    @JRubyMethod(name = "peeraddr")
    public IRubyObject peeraddr(ThreadContext context) {
        return peeraddrCommon(context, !doNotReverseLookup(context));
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
        IRubyObject result = recv(context, _length);
        // MRI: a connected stream's recvfrom gives the peer's address array
        // (addr's format and do_not_reverse_lookup), and nil at EOF
        if (result.isNil()) return context.nil;
        InetSocketAddress peer = getInetRemoteSocket(context);
        if (peer != null) return newArray(context, result, addrFor(context, peer, !doNotReverseLookup(context)));
        InetSocketAddress sender = peer;

        int port;
        String hostName;
        String hostAddress;

        if (sender == null) {
            port = 0;
            hostName = hostAddress = "0.0.0.0";
        } else {
            port = sender.getPort();
            hostName = sender.getHostName();
            hostAddress = SocketUtils.ipString(sender.getAddress());
        }

        IRubyObject addressArray = newArrayNoCopy(context,
                newString(context, "AF_INET"),
                asFixnum(context, port),
                newString(context, hostName),
                newString(context, hostAddress));

        return newArray(context, result, addressArray);
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
        return Sockaddr.packSockaddrFromAddress(context, getInetSocketAddress());
    }

    @Override
    public IRubyObject getpeername(ThreadContext context) {
       return Sockaddr.packSockaddrFromAddress(context, getInetRemoteSocket(context));
    }

    private IRubyObject addrCommon(ThreadContext context, IRubyObject reverse) {
        Boolean doReverse = doReverseLookup(context, reverse);
        if (doReverse == null) doReverse = !doNotReverseLookup(context);

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
        if (doReverse == null) doReverse = !doNotReverseLookup(context);

        return peeraddrCommon(context, doReverse);
    }

    private IRubyObject peeraddrCommon(ThreadContext context, boolean reverse) {
        InetSocketAddress address = getInetRemoteSocket(context);

        checkAddress(context, address);

        return addrFor(context, address, reverse);
    }

    /**
     * MRI's reverse_lookup argument (IPSocket#addr/#peeraddr,
     * Socket.getaddrinfo): true or :hostname reverse-resolves, false or
     * :numeric doesn't, nil defers to do_not_reverse_lookup. Returns whether
     * to reverse-resolve (null: the default). This used to return the
     * opposite ("no reverse") while every caller treated it as "do reverse",
     * so addr(true) and getaddrinfo(..., true) were numeric and :numeric
     * resolved a hostname.
     */
    public static Boolean doReverseLookup(ThreadContext context, IRubyObject reverse) {
        if (reverse == context.tru) return true;
        if (reverse == context.fals) return false;
        if (reverse == context.nil) return null;

        TypeConverter.checkType(context, reverse, symbolClass(context));
        return switch (reverse.toString()) {
            case "numeric" -> false;
            case "hostname" -> true;
            default -> throw argumentError(context, "invalid reverse_lookup flag: " + reverse);
        };
    }
}// RubyIPSocket
