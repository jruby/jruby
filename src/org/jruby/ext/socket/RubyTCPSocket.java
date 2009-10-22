/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.socket;

import static com.kenai.constantine.platform.AddressFamily.*;

import java.io.FileDescriptor;
import java.io.IOException;

import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyIO;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.InvalidValueException;

public class RubyTCPSocket extends RubyIPSocket {
    static void createTCPSocket(Ruby runtime) {
        RubyClass rb_cTCPSocket = runtime.defineClass("TCPSocket", runtime.fastGetClass("IPSocket"), TCPSOCKET_ALLOCATOR);

        rb_cTCPSocket.includeModule(runtime.fastGetClass("Socket").fastGetConstant("Constants"));
        
        rb_cTCPSocket.defineAnnotatedMethods(RubyTCPSocket.class);

        runtime.getObject().fastSetConstant("TCPsocket",rb_cTCPSocket);
    }

    private static ObjectAllocator TCPSOCKET_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyTCPSocket(runtime, klass);
        }
    };

    public RubyTCPSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }
    
    private int getPortFrom(Ruby runtime, IRubyObject arg) {
        return RubyNumeric.fix2int(arg instanceof RubyString ? 
                RubyNumeric.str2inum(runtime, (RubyString) arg, 0, true) : arg);
    }

    @JRubyMethod(required = 2, optional = 2, visibility = Visibility.PRIVATE, backtrace = true)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context.getRuntime(), args, 2, 4);

        String remoteHost = args[0].isNil()? "localhost" : args[0].convertToString().toString();
        int remotePort = getPortFrom(context.getRuntime(), args[1]);
        String localHost = args.length >= 3 && !args[2].isNil() ? args[2].convertToString().toString() : null;
        int localPort = args.length == 4 ? getPortFrom(context.getRuntime(), args[3]) : 0;

        try {
            // This is a bit convoluted because (1) SocketChannel.bind is only in jdk 7 and
            // (2) Socket.getChannel() seems to return null in some cases
            final SocketChannel channel = SocketChannel.open();
            final Socket socket = channel.socket();
            if (localHost != null) {
                socket.bind( new InetSocketAddress(InetAddress.getByName(localHost), localPort) );
            }
            try {
                channel.configureBlocking(false);
                channel.connect( new InetSocketAddress(InetAddress.getByName(remoteHost), remotePort) );
                context.getThread().select(channel, this, SelectionKey.OP_CONNECT);
                channel.finishConnect();
            } finally {
                channel.configureBlocking(true);
            }
            initSocket(context.getRuntime(), new ChannelDescriptor(channel, RubyIO.getNewFileno(), new ModeFlags(ModeFlags.RDWR), new FileDescriptor()));
        } catch (InvalidValueException ex) {
            throw context.getRuntime().newErrnoEINVALError();
        } catch(ConnectException e) {
            throw context.getRuntime().newErrnoECONNREFUSEDError();
        } catch (ClosedChannelException cce) {
            throw context.getRuntime().newErrnoECONNREFUSEDError();
        } catch(UnknownHostException e) {
            throw sockerr(context.getRuntime(), "initialize: name or service not known");
        } catch(IOException e) {
            throw sockerr(context.getRuntime(), "initialize: name or service not known");
        } catch (IllegalArgumentException iae) {
            throw sockerr(context.getRuntime(), iae.getMessage());
        }
        return this;
    }

    @Deprecated
    public static IRubyObject open(IRubyObject recv, IRubyObject[] args, Block block) {
        return open(recv.getRuntime().getCurrentContext(), recv, args, block);
    }
    @JRubyMethod(frame = true, rest = true, meta = true)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        RubyTCPSocket sock = (RubyTCPSocket)recv.callMethod(context,"new",args);
        if (!block.isGiven()) return sock;

        try {
            return block.yield(context, sock);
        } finally {
            if (sock.openFile.isOpen()) sock.close();
        }
    }

    @Deprecated
    public static IRubyObject gethostbyname(IRubyObject recv, IRubyObject hostname) {
        return gethostbyname(recv.getRuntime().getCurrentContext(), recv, hostname);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject gethostbyname(ThreadContext context, IRubyObject recv, IRubyObject hostname) {
        try {
            IRubyObject[] ret = new IRubyObject[4];
            Ruby r = context.getRuntime();
            InetAddress addr;
            String hostString = hostname.convertToString().toString();
            addr = InetAddress.getByName(hostString);
            
            ret[0] = r.newString(addr.getCanonicalHostName());
            ret[1] = r.newArray();
            ret[3] = r.newString(addr.getHostAddress());
            
            if (addr instanceof Inet4Address) {
                Inet4Address addr4 = (Inet4Address)addr;
                ret[2] = r.newFixnum(AF_INET); //AF_INET
            } else if (addr instanceof Inet6Address) {
                Inet6Address addr6 = (Inet6Address)addr;
                ret[2] = r.newFixnum(AF_INET6); //AF_INET
            }
            return r.newArrayNoCopy(ret);
        } catch(UnknownHostException e) {
            throw sockerr(context.getRuntime(), "gethostbyname: name or service not known");
        }
    }
}
