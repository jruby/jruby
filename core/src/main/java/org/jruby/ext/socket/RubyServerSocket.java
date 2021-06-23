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

import jnr.constants.platform.Sock;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.common.IRubyWarnings;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.ChannelFD;
import org.jruby.util.io.Sockaddr;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.Channel;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
@JRubyClass(name="Socket", parent="BasicSocket", include="Socket::Constants")
public class RubyServerSocket extends RubySocket {
    static void createServerSocket(Ruby runtime) {
        RubyClass rb_cSocket = runtime.defineClass("ServerSocket", runtime.getClass("Socket"), RubyServerSocket::new);

        rb_cSocket.defineAnnotatedMethods(RubyServerSocket.class);
    }

    public RubyServerSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(name = "listen")
    public IRubyObject listen(ThreadContext context, IRubyObject backlog) {
        context.runtime.getWarnings().warnOnce(
                IRubyWarnings.ID.LISTEN_SERVER_SOCKET,
                "pass backlog to #bind instead of #listen (https://github.com/jruby/jruby/wiki/ServerSocket)");

        return context.runtime.newFixnum(0);
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject connect_nonblock(ThreadContext context, IRubyObject arg) {
        throw SocketUtils.sockerr(context.runtime, "server socket cannot connect");
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject connect(ThreadContext context, IRubyObject arg) {
        throw SocketUtils.sockerr(context.runtime, "server socket cannot connect");
    }

    @JRubyMethod()
    public IRubyObject bind(ThreadContext context, IRubyObject addr) {
        return bind(context, addr, RubyFixnum.zero(context.runtime));
    }

    @JRubyMethod()
    public IRubyObject bind(ThreadContext context, IRubyObject addr, IRubyObject backlog) {
        final InetSocketAddress iaddr;

        if (addr instanceof Addrinfo) {
            Addrinfo addrInfo = (Addrinfo) addr;
            if (!addrInfo.ip_p(context).isTrue()) {
                throw context.runtime.newTypeError("not an INET or INET6 address: " + addrInfo);
            }
            iaddr = new InetSocketAddress(addrInfo.getInetAddress().getHostAddress(), addrInfo.getPort());
        } else {
            iaddr = Sockaddr.addressFromSockaddr_in(context, addr);
        }
        doBind(context, getChannel(), iaddr, RubyFixnum.fix2int(backlog));

        return RubyFixnum.zero(context.runtime);
    }

    @JRubyMethod()
    public IRubyObject accept(ThreadContext context) {
        return doAccept(this, context, true);
    }

    @JRubyMethod()
    public IRubyObject accept_nonblock(ThreadContext context) {
        return accept_nonblock(context, context.nil);
    }

    @JRubyMethod()
    public IRubyObject accept_nonblock(ThreadContext context, IRubyObject opts) {
        return doAcceptNonblock(this, context, extractExceptionArg(context, opts));
    }

    @Override
    protected ChannelFD initChannelFD(Ruby runtime) {
        Channel channel;

        try {
            if (soType == Sock.SOCK_STREAM) {
                channel = ServerSocketChannel.open();
            }
            else {
                throw runtime.newArgumentError("unsupported server socket type `" + soType + "'");
            }

            return newChannelFD(runtime, channel);
        }
        catch (IOException e) {
            throw sockerr(runtime, "initialize: " + e.toString(), e);
        }
    }

    public static IRubyObject doAcceptNonblock(RubySocket sock, ThreadContext context, boolean ex) {
        try {
            Channel channel = sock.getChannel();
            if (channel instanceof SelectableChannel) {
                SelectableChannel selectable = (SelectableChannel)channel;

                synchronized (selectable.blockingLock()) {
                    boolean oldBlocking = selectable.isBlocking();

                    try {
                        selectable.configureBlocking(false);

                        IRubyObject socket = doAccept(sock, context, ex);
                        if (!(socket instanceof RubySocket)) return socket;
                        SocketChannel socketChannel = (SocketChannel)((RubySocket)socket).getChannel();
                        InetSocketAddress addr = (InetSocketAddress)socketChannel.socket().getRemoteSocketAddress();

                        return context.runtime.newArray(
                                socket,
                                Sockaddr.packSockaddrFromAddress(context, addr));
                    } finally {
                        selectable.configureBlocking(oldBlocking);
                    }
                }
            }
            else {
                throw context.runtime.newErrnoENOPROTOOPTError();
            }
        }
        catch (IOException e) {
            throw sockerr(context.runtime, e.getLocalizedMessage(), e);
        }
    }

    public static IRubyObject doAccept(RubySocket sock, ThreadContext context, boolean ex) {
        Ruby runtime = context.runtime;

        Channel channel = sock.getChannel();

        try {
            if (channel instanceof ServerSocketChannel) {
                ServerSocketChannel serverChannel = (ServerSocketChannel)sock.getChannel();

                SocketChannel socket = serverChannel.accept();

                if (socket == null) {
                    // This appears to be undocumented in JDK; null as a sentinel value
                    // for a nonblocking accept with nothing available. We raise for Ruby.
                    // indicates that no connection is available in non-blocking mode
                    if (!ex) return runtime.newSymbol("wait_readable");
                    throw runtime.newErrnoEAGAINReadableError("accept(2) would block");
                }

                RubySocket rubySocket = new RubySocket(runtime, runtime.getClass("Socket"));
                rubySocket.initFromServer(runtime, sock, socket);

                return runtime.newArray(rubySocket, new Addrinfo(runtime, runtime.getClass("Addrinfo"), socket.getRemoteAddress()));
            }
            throw runtime.newErrnoENOPROTOOPTError();
        }
        catch (IllegalBlockingModeException e) {
            // indicates that no connection is available in non-blocking mode
            if (!ex) return runtime.newSymbol("wait_readable");
            throw runtime.newErrnoEAGAINReadableError("accept(2) would block");
        }
        catch (IOException e) {
            throw sockerr(runtime, e.getLocalizedMessage(), e);
        }
    }

    private void doBind(ThreadContext context, Channel channel, InetSocketAddress iaddr, int backlog) {
        Ruby runtime = context.runtime;

        try {
            if (channel instanceof ServerSocketChannel) {
                ServerSocket socket = ((ServerSocketChannel)channel).socket();
                socket.bind(iaddr, backlog);
            }
            else {
                throw runtime.newErrnoENOPROTOOPTError();
            }
        }
        catch (UnknownHostException e) {
            throw SocketUtils.sockerr(runtime, "bind(2): unknown host");
        }
        catch (SocketException e) {
            throw buildSocketException(runtime, e, "bind(2)", iaddr);
        }
        catch (IOException e) {
            throw sockerr(runtime, "bind(2): name or service not known", e);
        }
        catch (IllegalArgumentException e) {
            throw sockerr(runtime, e.getMessage(), e);
        }
    }
}// RubySocket
