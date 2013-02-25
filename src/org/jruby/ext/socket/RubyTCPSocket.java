/*
 ***** BEGIN LICENSE BLOCK *****
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

import static jnr.constants.platform.AddressFamily.*;

import java.io.IOException;

import java.net.BindException;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.UnknownHostException;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.ChannelDescriptor;

public class RubyTCPSocket extends RubyIPSocket {
    static void createTCPSocket(Ruby runtime) {
        RubyClass rb_cTCPSocket = runtime.defineClass("TCPSocket", runtime.getClass("IPSocket"), TCPSOCKET_ALLOCATOR);
        
        rb_cTCPSocket.defineAnnotatedMethods(RubyTCPSocket.class);

        runtime.getObject().setConstant("TCPsocket",rb_cTCPSocket);
    }

    private static ObjectAllocator TCPSOCKET_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyTCPSocket(runtime, klass);
        }
    };

    public RubyTCPSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(required = 2, optional = 2, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        IRubyObject _host = args[0];
        IRubyObject _port = args[1];

        String remoteHost = _host.isNil()? "localhost" : _host.convertToString().toString();
        int remotePort = SocketUtils.getPortFrom(context, _port);

        String localHost = (args.length >= 3 && !args[2].isNil()) ? args[2].convertToString().toString() : null;
        int localPort = (args.length == 4 && !args[3].isNil()) ? SocketUtils.getPortFrom(context, args[3]) : 0;

        // try to ensure the socket closes if it doesn't succeed
        boolean success = false;
        SocketChannel channel = null;

        try {
            // This is a bit convoluted because (1) SocketChannel.bind is only in jdk 7 and
            // (2) Socket.getChannel() seems to return null in some cases
            channel = SocketChannel.open();
            Socket socket = channel.socket();

            if (localHost != null) {
                socket.setReuseAddress(true);
                socket.bind( new InetSocketAddress(InetAddress.getByName(localHost), localPort) );
            }

            try {
                // Do this nonblocking so we can be interrupted
                channel.configureBlocking(false);
                channel.connect( new InetSocketAddress(InetAddress.getByName(remoteHost), remotePort) );
                context.getThread().select(channel, this, SelectionKey.OP_CONNECT);
                channel.finishConnect();

                // only try to set blocking back if we succeeded to finish connecting
                channel.configureBlocking(true);

                initSocket(runtime, new ChannelDescriptor(channel, newModeFlags(runtime, ModeFlags.RDWR)));
                success = true;
            } catch(BindException e) {
            	throw runtime.newErrnoEADDRFromBindException(e, " to: " + remoteHost + ":" + String.valueOf(remotePort));

            } catch (NoRouteToHostException nrthe) {
                throw runtime.newErrnoEHOSTUNREACHError("SocketChannel.connect");

            } catch(ConnectException e) {
                throw runtime.newErrnoECONNREFUSEDError();

            } catch(UnknownHostException e) {
                throw SocketUtils.sockerr(runtime, "initialize: name or service not known");

            }

        } catch (ClosedChannelException cce) {
            throw runtime.newErrnoECONNREFUSEDError();

        } catch(BindException e) {
            throw runtime.newErrnoEADDRFromBindException(e, " on: " + localHost + ":" + String.valueOf(localPort));

        } catch(IOException e) {
            throw SocketUtils.sockerr(runtime, e.getLocalizedMessage());

        } catch (IllegalArgumentException iae) {
            throw SocketUtils.sockerr(runtime, iae.getMessage());

        } finally {
            if (!success && channel != null) {
                try {channel.close();} catch (IOException ioe) {}
            }
        }

        return context.nil;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject gethostbyname(ThreadContext context, IRubyObject recv, IRubyObject hostname) {
        Ruby runtime = context.runtime;
        IRubyObject[] ret = new IRubyObject[4];
        String hostString = hostname.convertToString().toString();

        try {
            InetAddress addr = InetAddress.getByName(hostString);
            
            ret[0] = runtime.newString(do_not_reverse_lookup(context, recv).isTrue() ? addr.getHostAddress() : addr.getCanonicalHostName());
            ret[1] = runtime.newArray();

            if (addr instanceof Inet4Address) {
                ret[2] = runtime.newFixnum(AF_INET);
            } else if (addr instanceof Inet6Address) {
                ret[2] = runtime.newFixnum(AF_INET6);
            }

            ret[3] = runtime.newString(addr.getHostAddress());

            return runtime.newArrayNoCopy(ret);

        } catch(UnknownHostException e) {
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
