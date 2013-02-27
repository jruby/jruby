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
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.common.IRubyWarnings;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.ModeFlags;
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
        RubyClass rb_cSocket = runtime.defineClass("ServerSocket", runtime.getClass("Socket"), SERVER_SOCKET_ALLOCATOR);

        rb_cSocket.defineAnnotatedMethods(RubyServerSocket.class);
    }

    private static ObjectAllocator SERVER_SOCKET_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyServerSocket(runtime, klass);
        }
    };

    public RubyServerSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(name = "listen")
    public IRubyObject listen(ThreadContext context, IRubyObject backlog) {
        context.runtime.getWarnings().warnOnce(
                IRubyWarnings.ID.LISTEN_SERVER_SOCKET,
                "pass backlog to #bind instead of #listen (http://wiki.jruby.org/ServerSocket)");

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
        InetSocketAddress iaddr = Sockaddr.addressFromSockaddr_in(context, addr);

        doBind(context, getChannel(), iaddr, 0);

        return RubyFixnum.zero(context.runtime);
    }

    @JRubyMethod()
    public IRubyObject bind(ThreadContext context, IRubyObject addr, IRubyObject backlog) {
        InetSocketAddress iaddr = Sockaddr.addressFromSockaddr_in(context, addr);

        doBind(context, getChannel(), iaddr, RubyFixnum.fix2int(backlog));

        return RubyFixnum.zero(context.runtime);
    }

    @JRubyMethod()
    public IRubyObject accept(ThreadContext context) {
        return doAccept(context, getChannel());
    }

    @JRubyMethod()
    public IRubyObject accept_nonblock(ThreadContext context) {
        return doAcceptNonblock(context, getChannel());
    }

    protected ChannelDescriptor initChannel(Ruby runtime) {
        Channel channel;

        try {
            if(soType == Sock.SOCK_STREAM) {
                channel = ServerSocketChannel.open();

            } else {
                throw runtime.newArgumentError("unsupported server socket type `" + soType + "'");

            }

            ModeFlags modeFlags = newModeFlags(runtime, ModeFlags.RDWR);

            return new ChannelDescriptor(channel, modeFlags);

        } catch(IOException e) {
            throw SocketUtils.sockerr(runtime, "initialize: " + e.toString());

        }
    }

    private RubyArray doAcceptNonblock(ThreadContext context, Channel channel) {
        try {
            if (channel instanceof SelectableChannel) {
                SelectableChannel selectable = (SelectableChannel)channel;

                synchronized (selectable.blockingLock()) {
                    boolean oldBlocking = selectable.isBlocking();

                    try {
                        selectable.configureBlocking(false);

                        RubySocket socket = doAccept(context, channel);
                        SocketChannel socketChannel = (SocketChannel)socket.getChannel();
                        InetSocketAddress addr = (InetSocketAddress)socketChannel.socket().getLocalSocketAddress();

                        return context.runtime.newArray(
                                socket,
                                Sockaddr.packSockaddrFromAddress(context, addr));
                    } finally {
                        selectable.configureBlocking(oldBlocking);
                    }
                }
            } else {
                throw getRuntime().newErrnoENOPROTOOPTError();

            }

        } catch(IOException e) {
            throw SocketUtils.sockerr(context.runtime, e.getLocalizedMessage());

        }
    }

    private RubySocket doAccept(ThreadContext context, Channel channel) {
        Ruby runtime = context.runtime;

        try {
            if (channel instanceof ServerSocketChannel) {
                ServerSocketChannel serverChannel = (ServerSocketChannel)getChannel();

                SocketChannel socket = serverChannel.accept();

                if (socket == null) {
                    // This appears to be undocumented in JDK; null as a sentinel value
                    // for a nonblocking accept with nothing available. We raise for Ruby.
                    // indicates that no connection is available in non-blocking mode
                    throw runtime.newErrnoEAGAINReadableError("accept(2) would block");
                }

                RubySocket rubySocket = new RubySocket(runtime, runtime.getClass("Socket"));
                rubySocket.initFromServer(runtime, this, socket);

                return rubySocket;

            } else {
                throw runtime.newErrnoENOPROTOOPTError();
            }

        } catch (IllegalBlockingModeException ibme) {
            // indicates that no connection is available in non-blocking mode
            throw runtime.newErrnoEAGAINReadableError("accept(2) would block");

        } catch(IOException e) {
            throw SocketUtils.sockerr(runtime, e.getLocalizedMessage());

        }
    }

    private void doBind(ThreadContext context, Channel channel, InetSocketAddress iaddr, int backlog) {
        Ruby runtime = context.runtime;

        try {
            if (channel instanceof ServerSocketChannel) {
                ServerSocket socket = ((ServerSocketChannel)channel).socket();
                socket.bind(iaddr, backlog);

            } else {
                throw runtime.newErrnoENOPROTOOPTError();
            }

        } catch(UnknownHostException e) {
            throw SocketUtils.sockerr(runtime, "bind(2): unknown host");

        } catch(SocketException e) {
            handleSocketException(runtime, "bind", e);

        } catch(IOException e) {
            throw SocketUtils.sockerr(runtime, "bind(2): name or service not known");

        } catch (IllegalArgumentException iae) {
            throw SocketUtils.sockerr(runtime, iae.getMessage());

        }
    }
}// RubySocket
