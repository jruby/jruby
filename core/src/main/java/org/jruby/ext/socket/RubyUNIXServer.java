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
 * Copyright (C) 2008 Ola Bini <ola.bini@gmail.com>
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


import org.jruby.FiberScheduler;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Access;
import org.jruby.api.Define;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Create.*;
import static org.jruby.api.Define.defineClass;

@JRubyClass(name="UNIXServer", parent="UNIXSocket")
public class RubyUNIXServer extends RubyUNIXSocket {
    static void createUNIXServer(ThreadContext context, RubyClass UNIXSocket) {
        defineClass(context, "UNIXServer", UNIXSocket, RubyUNIXServer::new).
                defineMethods(context, RubyUNIXServer.class);
    }

    public RubyUNIXServer(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject path) {
        init_unixsock(context, path, true);

        return this;
    }

    @JRubyMethod
    public IRubyObject accept(ThreadContext context) {
        SocketChannel socketChannel = acceptChannel(context);

        RubyUNIXSocket sock = (RubyUNIXSocket)(Helpers.invoke(context, Access.getClass(context, "UNIXSocket"), "allocate"));

        sock.init_sock(context.runtime, socketChannel, "");

        return sock;
    }

    private SocketChannel acceptChannel(ThreadContext context) {
        IRubyObject scheduler = FiberScheduler.current(context);
        if (scheduler != null) return schedulerAcceptChannel(context, scheduler);

        try {
            while (true) {
                boolean ready = context.getThread().select(this, SelectionKey.OP_ACCEPT);
                if (!ready) {
                    context.pollThreadEvents();
                } else {
                    SocketChannel socketChannel = UnixDomain.accept(asUnixServer());
                    // null when another thread made the channel nonblocking and took the connection first
                    if (socketChannel == null) continue;

                    return socketChannel;
                }
            }
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        }
    }

    // MRI: rsock_s_accept, which retries a nonblocking accept(2) whenever io_wait reports the server readable
    private SocketChannel schedulerAcceptChannel(ThreadContext context, IRubyObject scheduler) {
        while (true) {
            ServerSocketChannel server = asUnixServer();
            SocketChannel socketChannel;

            synchronized (server.blockingLock()) {
                boolean oldBlocking = server.isBlocking();

                try {
                    server.configureBlocking(false);
                    socketChannel = UnixDomain.accept(server);
                } catch (IOException ioe) {
                    throw context.runtime.newIOErrorFromException(ioe);
                } finally {
                    try {server.configureBlocking(oldBlocking);} catch (IOException ioe) {}
                }
            }

            if (socketChannel != null) return socketChannel;

            // outside the blocking lock, which a sibling fiber may need to accept on this server
            schedulerWaitAcceptable(context, scheduler);
        }
    }

    @JRubyMethod
    public IRubyObject accept_nonblock(ThreadContext context) {
        return accept_nonblock(context, context.runtime, true);
    }

    @JRubyMethod
    public IRubyObject accept_nonblock(ThreadContext context, IRubyObject opts) {
        return accept_nonblock(context, context.runtime, extractExceptionArg(context, opts));
    }

    public IRubyObject accept_nonblock(ThreadContext context, Ruby runtime, boolean ex) {
        SelectableChannel selectable = (SelectableChannel) getChannel();
        synchronized (selectable.blockingLock()) {
            boolean oldBlocking = selectable.isBlocking();
            try {
                selectable.configureBlocking(false);
                try {
                    java.nio.channels.SocketChannel socketChannel = ((java.nio.channels.ServerSocketChannel) selectable).accept();
                    if (socketChannel == null) {
                        if (!ex) return asSymbol(context, "wait_readable");
                        throw runtime.newErrnoEAGAINReadableError("accept(2) would block");
                    }
                    socketChannel.configureBlocking(true);
                    RubyUNIXSocket sock = (RubyUNIXSocket) Helpers.invoke(
                            context, Access.getClass(context, "UNIXSocket"), "allocate");
                    sock.init_sock(context.runtime, socketChannel, "");
                    return sock;
                } finally {
                    selectable.configureBlocking(oldBlocking);
                }
            } catch (IOException ioe) {
                if (ioe.getMessage() != null
                        && ioe.getMessage().contains("Resource temporarily unavailable")) {
                    if (!ex) return asSymbol(context, "wait_readable");
                    throw runtime.newErrnoEAGAINReadableError("accept");
                }
                throw context.runtime.newIOErrorFromException(ioe);
            }
        }
    }

    @JRubyMethod
    public IRubyObject listen(ThreadContext context, IRubyObject log) {
        // TODO listen backlog
        return asFixnum(context, 0);
    }

    @JRubyMethod
    public IRubyObject sysaccept(ThreadContext context) {
        return asFixnum(context, newChannelFD(context.runtime, acceptChannel(context)).bestFileno());
    }

    // path() is inherited from RubyUNIXSocket: openFile.getPath() is null
    // for a server built via for_fd (no path was ever bound to it here),
    // and RubyUNIXSocket#path already falls back through
    // UnixDomain.localAddress / UnixDomainNative.pathOf / "" for exactly
    // that case -- this override used to call newString on that null
    // directly and NPE.

    @JRubyMethod
    public IRubyObject addr(ThreadContext context) {
        return newArray(context, newString(context, "AF_UNIX"), path(context));
    }

    @JRubyMethod
    public IRubyObject peeraddr(ThreadContext context) {
        throw context.runtime.newErrnoENOTCONNError();
    }

    @Override
    protected SocketAddress getUnixSocketAddress() {
        Channel channel = getChannel();
        SocketAddress socketAddress = SocketType.forChannel(channel).getLocalSocketAddress(channel);
        return UnixDomain.isUnixAddress(socketAddress) ? socketAddress : null;
    }

    @Override
    protected SocketAddress getUnixRemoteSocket(ThreadContext context) {
        return getUnixSocketAddress();
    }

    private java.nio.channels.ServerSocketChannel asUnixServer() {
        return (java.nio.channels.ServerSocketChannel) getChannel();
    }
}// RubyUNIXServer
