/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2007 Damian Steer <pldms@mac.com>
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

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import java.nio.channels.SelectionKey;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyIO;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.ChannelDescriptor;

/**
 * @author <a href="mailto:pldms@mac.com">Damian Steer</a>
 */
@JRubyClass(name="UDPSocket", parent="IPSocket")
public class RubyUDPSocket extends RubyIPSocket {

    static void createUDPSocket(Ruby runtime) {
        RubyClass rb_cUDPSocket = runtime.defineClass("UDPSocket", runtime.fastGetClass("IPSocket"), UDPSOCKET_ALLOCATOR);
        
        rb_cUDPSocket.includeModule(runtime.fastGetClass("Socket").fastGetConstant("Constants"));

        rb_cUDPSocket.defineAnnotatedMethods(RubyUDPSocket.class);

        runtime.getObject().fastSetConstant("UDPsocket", rb_cUDPSocket);
    }
    private static ObjectAllocator UDPSOCKET_ALLOCATOR = new ObjectAllocator() {

        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyUDPSocket(runtime, klass);
        }
    };

    public RubyUDPSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context) {
        try {
            DatagramChannel channel = DatagramChannel.open();
            initSocket(context.getRuntime(), new ChannelDescriptor(channel, RubyIO.getNewFileno(), new ModeFlags(ModeFlags.RDWR), new FileDescriptor()));
        } catch (org.jruby.util.io.InvalidValueException ex) {
            throw context.getRuntime().newErrnoEINVALError();
        } catch (ConnectException e) {
            throw context.getRuntime().newErrnoECONNREFUSEDError();
        } catch (UnknownHostException e) {
            throw sockerr(context.getRuntime(), "initialize: name or service not known");
        } catch (IOException e) {
            throw sockerr(context.getRuntime(), "initialize: name or service not known");
        }
        return this;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject protocol) {
        // we basically ignore protocol. let someone report it...
        return initialize(context);
    }
    
    @Deprecated
    public IRubyObject bind(IRubyObject host, IRubyObject port) {
        return bind(getRuntime().getCurrentContext(), host, port);
    }
    @JRubyMethod
    public IRubyObject bind(ThreadContext context, IRubyObject host, IRubyObject port) {
        InetSocketAddress addr = null;
        try {
            if (host.isNil()
		|| ((host instanceof RubyString)
		    && ((RubyString) host).isEmpty())) {
		// host is nil or the empty string, bind to INADDR_ANY
                addr = new InetSocketAddress(RubyNumeric.fix2int(port));
            } else {
                addr = new InetSocketAddress(InetAddress.getByName(host.convertToString().toString()), RubyNumeric.fix2int(port));
            }
            ((DatagramChannel) this.getChannel()).socket().bind(addr);
            return RubyFixnum.zero(context.getRuntime());
        } catch (UnknownHostException e) {
            throw sockerr(context.getRuntime(), "bind: name or service not known");
        } catch (SocketException e) {
            throw sockerr(context.getRuntime(), "bind: name or service not known");
        } catch (Error e) {
            // Workaround for a bug in Sun's JDK 1.5.x, see
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6303753
            if (e.getCause() instanceof SocketException) {
                throw sockerr(context.getRuntime(), "bind: name or service not known");
            } else {
                throw e;
            }
        }
    }
    @Deprecated
    public IRubyObject connect(IRubyObject host, IRubyObject port) {
        return connect(getRuntime().getCurrentContext(), host, port);
    }
    @JRubyMethod
    public IRubyObject connect(ThreadContext context, IRubyObject host, IRubyObject port) {
        try {
            InetSocketAddress addr;
            addr = new InetSocketAddress(InetAddress.getByName(host.convertToString().toString()), RubyNumeric.fix2int(port));
            ((DatagramChannel) this.getChannel()).connect(addr);
            return RubyFixnum.zero(context.getRuntime());
        } catch (UnknownHostException e) {
            throw sockerr(context.getRuntime(), "connect: name or service not known");
        } catch (IOException e) {
            throw sockerr(context.getRuntime(), "connect: name or service not known");
        }
    }

    @Deprecated
    public IRubyObject recvfrom(IRubyObject[] args) {
        return recvfrom(getRuntime().getCurrentContext(), args);
    }

    @JRubyMethod(required = 1, rest = true)
    public IRubyObject recvfrom(ThreadContext context, IRubyObject[] args) {
        try {
            int length = RubyNumeric.fix2int(args[0]);
            ByteBuffer buf = ByteBuffer.allocate(length);
            ((DatagramChannel) this.getChannel()).configureBlocking(false);
            context.getThread().select(this, SelectionKey.OP_READ);
            InetSocketAddress sender = (InetSocketAddress) ((DatagramChannel) this.getChannel()).receive(buf);
            IRubyObject addressArray = context.getRuntime().newArray(new IRubyObject[]{
                context.getRuntime().newString("AF_INET"),
                context.getRuntime().newFixnum(sender.getPort()),
                context.getRuntime().newString(sender.getHostName()),
                context.getRuntime().newString(sender.getAddress().getHostAddress())
            });
            IRubyObject result = context.getRuntime().newString(new ByteList(buf.array(), 0, buf.position()));
            return context.getRuntime().newArray(new IRubyObject[]{result, addressArray});
        } catch (UnknownHostException e) {
            throw sockerr(context.getRuntime(), "recvfrom: name or service not known");
        } catch (IOException e) {
            throw sockerr(context.getRuntime(), "recvfrom: name or service not known");
        }
    }

    @Override
    public IRubyObject recv(ThreadContext context, IRubyObject[] args) {
        try {
            int length = RubyNumeric.fix2int(args[0]);
            ByteBuffer buf = ByteBuffer.allocate(length);
            context.getThread().select(this, SelectionKey.OP_READ);
            ((DatagramChannel) this.getChannel()).receive(buf);
            return context.getRuntime().newString(new ByteList(buf.array(), 0, buf.position()));
        } catch (IOException e) {
            throw sockerr(context.getRuntime(), "recv: name or service not known");
        }
    }
    @Deprecated
    public IRubyObject send(IRubyObject[] args) {
        return send(getRuntime().getCurrentContext(), args);
    }

    @JRubyMethod(required = 1, rest = true)
    public IRubyObject send(ThreadContext context, IRubyObject[] args) {
        try {
            int written;
            if (args.length >= 3) { // host and port given
                RubyString nameStr = args[2].convertToString();
                RubyString data = args[0].convertToString();
                ByteBuffer buf = ByteBuffer.wrap(data.getBytes());

                InetAddress address = RubySocket.getRubyInetAddress(nameStr.getByteList());
                InetSocketAddress addr =
                        new InetSocketAddress(address, RubyNumeric.fix2int(args[3]));
                written = ((DatagramChannel) this.getChannel()).send(buf, addr);
            } else {
                RubyString data = args[0].convertToString();
                ByteBuffer buf = ByteBuffer.wrap(data.getBytes());
                written = ((DatagramChannel) this.getChannel()).write(buf);
            }
            return context.getRuntime().newFixnum(written);
        } catch (UnknownHostException e) {
            throw sockerr(context.getRuntime(), "send: name or service not known");
        } catch (IOException e) {
            throw sockerr(context.getRuntime(), "send: name or service not known");
        }
    }
    @Deprecated
    public static IRubyObject open(IRubyObject recv, IRubyObject[] args, Block block) {
        return open(recv.getRuntime().getCurrentContext(), recv, args, block);
    }
    @JRubyMethod(rest = true, frame = true, meta = true)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        RubyUDPSocket sock = (RubyUDPSocket) recv.callMethod(context, "new", args);
        if (!block.isGiven()) {
            return sock;
        }

        try {
            return block.yield(context, sock);
        } finally {
            if (sock.openFile.isOpen()) {
                sock.close();
            }
        }
    }
}// RubyUDPSocket

