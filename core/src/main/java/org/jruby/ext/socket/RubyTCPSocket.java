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
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.runtime.Arity;
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
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import static jnr.constants.platform.AddressFamily.AF_INET;
import static jnr.constants.platform.AddressFamily.AF_INET6;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.*;
import static org.jruby.api.Define.defineClass;

public class RubyTCPSocket extends RubyIPSocket {
    static RubyClass createTCPSocket(ThreadContext context, RubyClass IPSocket, RubyClass Object) {
        return (RubyClass) Object.setConstant(context, "TCPsocket",
                defineClass(context, "TCPSocket", IPSocket, RubyTCPSocket::new).
                        defineMethods(context, RubyTCPSocket.class));
    }

    public RubyTCPSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }


    private SocketChannel attemptConnect(ThreadContext context, IRubyObject host, String localHost, int localPort,
                                         String remoteHost, int remotePort, RubyHash opts) throws IOException {
        for (InetAddress address: InetAddress.getAllByName(remoteHost)) {
            SocketChannel channel = SocketChannel.open();

            openFile = null; // Second or later attempts will have non-closeable failed attempt to connect.

            initSocket(newChannelFD(context.runtime, channel));

            // Do this nonblocking so we can be interrupted
            channel.configureBlocking(false);

            if (localHost != null) {
                channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
                channel.bind( new InetSocketAddress(InetAddress.getByName(localHost), localPort) );
            }
            try {
                channel.connect(new InetSocketAddress(address, remotePort));

                long timeout = -1;
                if (opts != null) {
                    IRubyObject timeoutObj = ArgsUtil.extractKeywordArg(context, opts, "connect_timeout");
                    if (!timeoutObj.isNil()) {
                        timeout = (long) (timeoutObj.convertToFloat().asDouble(context) * 1000);
                    }
                }

                // wait for connection
                if (context.getThread().select(channel, this, SelectionKey.OP_CONNECT, timeout)) {
                    // complete connection
                    while (!channel.finishConnect()) {
                        context.pollThreadEvents();
                    }

                    channel.configureBlocking(true);

                    return channel;
                }

                throw context.runtime.newErrnoETIMEDOUTError();
            } catch (ConnectException e) {
                // fall through and try next valid address for the host.
            }
        }

        // did not complete and only path out is n repeated ConnectExceptions
        throw context.runtime.newErrnoECONNREFUSEDError("connect(2) for " + host.inspect(context) + " port " + remotePort);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject host, IRubyObject port) {
        final String remoteHost = host.isNil() ? "localhost" : host.convertToString().toString();
        final int remotePort = SocketUtils.getPortFrom(context, port);

        return initialize(context, remoteHost, remotePort, host, null, 0, null);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject host, IRubyObject port, IRubyObject localOrOpts) {
        final String remoteHost = host.isNil() ? "localhost" : host.convertToString().toString();
        final int remotePort = SocketUtils.getPortFrom(context, port);
        IRubyObject opts = ArgsUtil.getOptionsArg(context, localOrOpts);

        if (!opts.isNil()) return initialize(context, remoteHost, remotePort, host, null, 0, (RubyHash) opts);

        String localHost = localOrOpts.isNil() ? null : localOrOpts.convertToString().toString();

        return initialize(context, remoteHost, remotePort, host, localHost, 0, null);

    }

    @JRubyMethod(required = 2, optional = 3, checkArity = false, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 2, 5);

        String localHost = null;
        int localPort = 0;
        IRubyObject maybeOpts;
        RubyHash opts = null;

        switch (argc) {
            case 2 -> { return initialize(context, args[0], args[1]); }
            case 3 -> { return initialize(context, args[0], args[1], args[2]); }
        }

        // cut switch in half to evaluate early args first
        IRubyObject host = args[0];
        IRubyObject port = args[1];

        final String remoteHost = host.isNil() ? "localhost" : host.convertToString().toString();
        final int remotePort = SocketUtils.getPortFrom(context, port);

        switch (argc) {
            case 4:
                if (!args[2].isNil()) localHost = args[2].convertToString().toString();

                maybeOpts = ArgsUtil.getOptionsArg(context, args[3]);
                if (!maybeOpts.isNil()) {
                    opts = (RubyHash) maybeOpts;
                } else if (!args[3].isNil()) {
                    localPort = SocketUtils.getPortFrom(context, args[3]);
                }

                break;
            case 5:
                if (!args[4].isNil()) opts = (RubyHash) ArgsUtil.getOptionsArg(context.runtime, args[4], true);
                break;
        }

        return initialize(context, remoteHost, remotePort, host, localHost, localPort, opts);
    }

    public IRubyObject initialize(ThreadContext context, String remoteHost, int remotePort, IRubyObject host, String localHost, int localPort, RubyHash opts) {
        Ruby runtime = context.runtime;

        // try to ensure the socket closes if it doesn't succeed
        boolean success = false;
        SocketChannel channel = null;

        try {
            try {
                channel = attemptConnect(context, host, localHost, localPort, remoteHost, remotePort, opts);
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
        try {
            var addr = InetAddress.getByName(hostname.convertToString().toString());

            return RubyArray.newArray(context.runtime,
                    newString(context, do_not_reverse_lookup(context, recv).isTrue() ? addr.getHostAddress() : addr.getCanonicalHostName()),
                    newEmptyArray(context),
                    asFixnum(context, addr instanceof Inet4Address ? AF_INET.longValue() : AF_INET6.longValue()),
                    newString(context, addr.getHostAddress()));
        }
        catch(UnknownHostException e) {
            throw SocketUtils.sockerr(context.runtime, "gethostbyname: name or service not known");
        }
    }

    @Deprecated
    public static IRubyObject open(IRubyObject recv, IRubyObject[] args, Block block) {
        return open(((RubyBasicObject) recv).getCurrentContext(), recv, args, block);
    }

    @Deprecated
    public static IRubyObject gethostbyname(IRubyObject recv, IRubyObject hostname) {
        return gethostbyname(((RubyBasicObject) recv).getCurrentContext(), recv, hostname);
    }
}
