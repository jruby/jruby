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

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyInteger;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.RubyThread;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.ModeFlags;

import org.jruby.util.io.SelectorFactory;
import java.nio.channels.spi.SelectorProvider;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
@JRubyClass(name="TCPServer", parent="TCPSocket")
public class RubyTCPServer extends RubyTCPSocket {
    static void createTCPServer(Ruby runtime) {
        RubyClass rb_cTCPServer = runtime.defineClass(
                "TCPServer", runtime.getClass("TCPSocket"), TCPSERVER_ALLOCATOR);

        rb_cTCPServer.defineAnnotatedMethods(RubyTCPServer.class);

        runtime.getObject().setConstant("TCPserver",rb_cTCPServer);
    }

    private static ObjectAllocator TCPSERVER_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyTCPServer(runtime, klass);
        }
    };

    public RubyTCPServer(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(name = "initialize", required = 1, optional = 1, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        IRubyObject _host = args[0];
        IRubyObject _port = args.length > 1 ? args[1] : context.nil;

        String host;
        if(_host.isNil()|| ((_host instanceof RubyString) && ((RubyString) _host).isEmpty())) {
            host = "0.0.0.0";
        } else if (_host instanceof RubyFixnum) {
            // numeric host, use it for port
            _port = _host;
            host = "0.0.0.0";
        } else {
            host = _host.convertToString().toString();
        }

        int port = SocketUtils.getPortFrom(context, _port);

        try {
            InetAddress addr = InetAddress.getByName(host);

            ssc = ServerSocketChannel.open();
            socket_address = new InetSocketAddress(addr, port);

            ssc.socket().bind(socket_address);

            initSocket(runtime, new ChannelDescriptor(ssc, newModeFlags(runtime, ModeFlags.RDWR)));

        } catch(UnknownHostException e) {
            throw SocketUtils.sockerr(runtime, "initialize: name or service not known");

        } catch(BindException e) {
            throw runtime.newErrnoEADDRFromBindException(e);

        } catch(SocketException e) {
            String msg = e.getMessage();

            if(msg.indexOf("Permission denied") != -1) {
                throw runtime.newErrnoEACCESError("bind(2)");
            } else {
                throw SocketUtils.sockerr(runtime, "initialize: name or service not known");
            }

        } catch(IOException e) {
            throw SocketUtils.sockerr(runtime, "initialize: name or service not known");

        } catch (IllegalArgumentException iae) {
            throw SocketUtils.sockerr(runtime, iae.getMessage());
        }

        return this;
    }

    @JRubyMethod(name = "accept")
    public IRubyObject accept(ThreadContext context) {
        Ruby runtime = context.runtime;
        RubyTCPSocket socket = new RubyTCPSocket(runtime, runtime.getClass("TCPSocket"));

        try {
            RubyThread thread = context.getThread();

            while (true) {
                boolean ready = thread.select(this, SelectionKey.OP_ACCEPT);

                if (!ready) {
                    // we were woken up without being selected...poll for thread events and go back to sleep
                    context.pollThreadEvents();

                } else {
                    SocketChannel connected = ssc.accept();
                    if (connected == null) continue;

                    connected.finishConnect();

                    // Force the client socket to be blocking
                    synchronized (connected.blockingLock()) {
                        connected.configureBlocking(false);
                        connected.configureBlocking(true);
                    }

                    // otherwise one key has been selected (ours) so we get the channel and hand it off
                    socket.initSocket(runtime, new ChannelDescriptor(connected, newModeFlags(runtime, ModeFlags.RDWR)));

                    return socket;
                }
            }

        } catch(IOException e) {
            throw SocketUtils.sockerr(runtime, "problem when accepting");
        }
    }

    @JRubyMethod(name = "accept_nonblock")
    public IRubyObject accept_nonblock(ThreadContext context) {
        Ruby runtime = context.runtime;
        RubyTCPSocket socket = new RubyTCPSocket(runtime, runtime.getClass("TCPSocket"));
        Selector selector = null;

        synchronized (ssc.blockingLock()) {
            boolean oldBlocking = ssc.isBlocking();

            try {
                ssc.configureBlocking(false);
                selector = SelectorFactory.openWithRetryFrom(runtime, SelectorProvider.provider());

                boolean ready = context.getThread().select(this, SelectionKey.OP_ACCEPT, 0);

                if (!ready) {
                    // no connection immediately accepted, let them try again
                    throw runtime.newErrnoEAGAINError("Resource temporarily unavailable");

                } else {
                    // otherwise one key has been selected (ours) so we get the channel and hand it off
                    socket.initSocket(context.runtime, new ChannelDescriptor(ssc.accept(), newModeFlags(runtime, ModeFlags.RDWR)));

                    return socket;
                }

            } catch(IOException e) {
                throw SocketUtils.sockerr(context.runtime, "problem when accepting");

            } finally {
                try {
                    if (selector != null) selector.close();
                } catch (Exception e) {
                }
                try {ssc.configureBlocking(oldBlocking);} catch (IOException ioe) {}

            }
        }
    }

    @JRubyMethod(name = "listen", required = 1)
    public IRubyObject listen(ThreadContext context, IRubyObject backlog) {
        return RubyFixnum.zero(context.runtime);
    }

    @JRubyMethod(name = "peeraddr", rest = true)
    public IRubyObject peeraddr(ThreadContext context, IRubyObject[] args) {
        throw context.runtime.newNotImplementedError("not supported");
    }

    @JRubyMethod(name = "getpeername", rest = true)
    public IRubyObject getpeername(ThreadContext context, IRubyObject[] args) {
        throw context.runtime.newNotImplementedError("not supported");
    }

    @JRubyMethod(rest = true, meta = true)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        IRubyObject tcpServer = recv.callMethod(context, "new", args);

        if (!block.isGiven()) return tcpServer;

        try {
            return block.yield(context, tcpServer);
        } finally {
            tcpServer.callMethod(context, "close");
        }
    }

    @Override
    public IRubyObject shutdown(ThreadContext context, IRubyObject[] args) {
        // act like a platform that does not support shutdown for server sockets
        throw context.runtime.newErrnoENOTCONNError();
    }

    @Override
    public IRubyObject gets(ThreadContext context) {
        throw context.runtime.newErrnoENOTCONNError();
    }

    @Override
    public IRubyObject gets(ThreadContext context, IRubyObject sep) {
        throw context.runtime.newErrnoENOTCONNError();
    }

    @Override
    public IRubyObject gets19(ThreadContext context) {
        throw context.runtime.newErrnoENOTCONNError();
    }

    @Override
    public IRubyObject gets19(ThreadContext context, IRubyObject sep) {
        throw context.runtime.newErrnoENOTCONNError();
    }

    @Override
    public IRubyObject gets19(ThreadContext context, IRubyObject sep, IRubyObject limit) {
        throw context.runtime.newErrnoENOTCONNError();
    }

    @Deprecated
    public IRubyObject accept() {
        return accept(getRuntime().getCurrentContext());
    }

    @Deprecated
    public IRubyObject listen(IRubyObject backlog) {
        return listen(getRuntime().getCurrentContext(), backlog);
    }

    @Deprecated
    public static IRubyObject open(IRubyObject recv, IRubyObject[] args, Block block) {
        return open(recv.getRuntime().getCurrentContext(), recv, args, block);
    }

    private ServerSocketChannel ssc;
    private InetSocketAddress socket_address;
}
