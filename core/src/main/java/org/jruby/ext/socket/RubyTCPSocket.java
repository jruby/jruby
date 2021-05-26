/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2007 Thomas E Enebo <enebo@acm.org>
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
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import static jnr.constants.platform.AddressFamily.AF_INET;
import static jnr.constants.platform.AddressFamily.AF_INET6;

public class RubyTCPSocket extends RubyIPSocket {
    static void createTCPSocket(Ruby runtime) {
        RubyClass rb_cTCPSocket = runtime.defineClass("TCPSocket", runtime.getClass("IPSocket"), RubyTCPSocket::new);

        rb_cTCPSocket.defineAnnotatedMethods(RubyTCPSocket.class);

        runtime.getObject().setConstant("TCPsocket",rb_cTCPSocket);
    }

    public RubyTCPSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }


    private SocketChannel attemptConnect(ThreadContext context, IRubyObject host, String localHost, int localPort,
                                         String remoteHost, int remotePort) throws IOException {
        for (InetAddress address: InetAddress.getAllByName(remoteHost)) {
            // This is a bit convoluted because (1) SocketChannel.bind is only in jdk 7 and
            // (2) Socket.getChannel() seems to return null in some cases
            SocketChannel channel = SocketChannel.open();
            Socket socket = channel.socket();

            openFile = null; // Second or later attempts will have non-closeable failed attempt to connect.

            initSocket(newChannelFD(context.runtime, channel));

            // Do this nonblocking so we can be interrupted
            channel.configureBlocking(false);

            if (localHost != null) {
                socket.setReuseAddress(true);
                socket.bind( new InetSocketAddress(InetAddress.getByName(localHost), localPort) );
            }
            try {
                channel.connect(new InetSocketAddress(address, remotePort));

                // wait for connection
                while (!context.getThread().select(channel, this, SelectionKey.OP_CONNECT)) {
                    context.pollThreadEvents();
                }

                // complete connection
                while (!channel.finishConnect()) {
                    context.pollThreadEvents();
                }

                channel.configureBlocking(true);

                return channel;
            } catch (ConnectException e) {
                // fall through and try next valid address for the host.
            }
        }

        // did not complete and only path out is n repeated ConnectExceptions
        throw context.runtime.newErrnoECONNREFUSEDError("connect(2) for " + host.inspect() + " port " + remotePort);
    }

    @JRubyMethod(required = 2, optional = 2, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        IRubyObject host = args[0];
        IRubyObject port = args[1];

        final String remoteHost = host.isNil() ? "localhost" : host.convertToString().toString();
        final int remotePort = SocketUtils.getPortFrom(context, port);

        String localHost = (args.length >= 3 && !args[2].isNil()) ? args[2].convertToString().toString() : null;
        int localPort = (args.length == 4 && !args[3].isNil()) ? SocketUtils.getPortFrom(context, args[3]) : 0;

        // try to ensure the socket closes if it doesn't succeed
        boolean success = false;
        SocketChannel channel = null;

        try {
            try {
                channel = attemptConnect(context, host, localHost, localPort, remoteHost, remotePort);
                success = true;
            } catch (BindException e) {
            	throw runtime.newErrnoEADDRFromBindException(e, " to: " + remoteHost + ':' + remotePort);
            } catch (NoRouteToHostException e) {
                throw runtime.newErrnoEHOSTUNREACHError("SocketChannel.connect");
            } catch (UnknownHostException e) {
                throw SocketUtils.sockerr(runtime, "initialize: name or service not known");
            }
        } catch (ClosedChannelException e) {
            throw runtime.newErrnoECONNREFUSEDError();
        } catch (BindException e) {
            throw runtime.newErrnoEADDRFromBindException(e, " on: " + localHost + ':' + localPort);
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        } catch (IllegalArgumentException e) {
            // NOTE: MRI does -1 as SocketError but +65536 as ECONNREFUSED
            // ... which JRuby does currently not blindly follow!
            throw sockerr(runtime, e.getMessage(), e);
        } finally {
            if (!success && channel != null) try { channel.close(); } catch (IOException ioe) {}
        }

        return context.nil;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject gethostbyname(ThreadContext context, IRubyObject recv, IRubyObject hostname) {
        Ruby runtime = context.runtime;
        IRubyObject ret0, ret1, ret2, ret3;
        String hostString = hostname.convertToString().toString();

        try {
            InetAddress addr = InetAddress.getByName(hostString);

            ret0 = runtime.newString(do_not_reverse_lookup(context, recv).isTrue() ? addr.getHostAddress() : addr.getCanonicalHostName());
            ret1 = runtime.newArray();

            if (addr instanceof Inet4Address) {
                ret2 = runtime.newFixnum(AF_INET);
            } else { // if (addr instanceof Inet6Address) {
                ret2 = runtime.newFixnum(AF_INET6);
            }

            ret3 = runtime.newString(addr.getHostAddress());

            return RubyArray.newArray(runtime, ret0, ret1, ret2, ret3);
        }
        catch(UnknownHostException e) {
            throw SocketUtils.sockerr(runtime, "gethostbyname: name or service not known");
        }
    }

    @Deprecated
    public static IRubyObject open(IRubyObject recv, IRubyObject[] args, Block block) {
        return open(recv.getRuntime().getCurrentContext(), recv, args, block);
    }

    @Deprecated
    public static IRubyObject gethostbyname(IRubyObject recv, IRubyObject hostname) {
        return gethostbyname(recv.getRuntime().getCurrentContext(), recv, hostname);
    }
}
