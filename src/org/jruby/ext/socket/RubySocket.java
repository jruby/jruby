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
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
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

import static jnr.constants.platform.AddressFamily.AF_INET;
import static jnr.constants.platform.AddressFamily.AF_INET6;
import static jnr.constants.platform.IPProto.IPPROTO_TCP;
import static jnr.constants.platform.IPProto.IPPROTO_UDP;
import static jnr.constants.platform.NameInfo.NI_NUMERICHOST;
import static jnr.constants.platform.NameInfo.NI_NUMERICSERV;
import static jnr.constants.platform.ProtocolFamily.PF_INET;
import static jnr.constants.platform.ProtocolFamily.PF_INET6;
import static jnr.constants.platform.Sock.SOCK_DGRAM;
import static jnr.constants.platform.Sock.SOCK_STREAM;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.platform.Platform;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.InvalidValueException;
import org.jruby.util.io.ModeFlags;

import jnr.constants.platform.AddressFamily;
import jnr.constants.platform.Sock;
import java.net.Inet6Address;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
@JRubyClass(name="Socket", parent="BasicSocket", include="Socket::Constants")
public class RubySocket extends RubyBasicSocket {
    @JRubyClass(name="SocketError", parent="StandardError")
    public static class SocketError {}

    private static ObjectAllocator SOCKET_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubySocket(runtime, klass);
        }
    };

    public static final int MSG_OOB = 0x1;
    public static final int MSG_PEEK = 0x2;
    public static final int MSG_DONTROUTE = 0x4;

    @JRubyModule(name="Socket::Constants")
    public static class Constants {}

    static void createSocket(Ruby runtime) {
        RubyClass rb_cSocket = runtime.defineClass("Socket", runtime.getClass("BasicSocket"), SOCKET_ALLOCATOR);

        RubyModule rb_mConstants = rb_cSocket.defineModuleUnder("Constants");
        // we don't have to define any that we don't support; see socket.c

        runtime.loadConstantSet(rb_mConstants, jnr.constants.platform.Sock.class);
        runtime.loadConstantSet(rb_mConstants, jnr.constants.platform.SocketOption.class);
        runtime.loadConstantSet(rb_mConstants, jnr.constants.platform.SocketLevel.class);
        runtime.loadConstantSet(rb_mConstants, jnr.constants.platform.ProtocolFamily.class);
        runtime.loadConstantSet(rb_mConstants, jnr.constants.platform.AddressFamily.class);
        runtime.loadConstantSet(rb_mConstants, jnr.constants.platform.INAddr.class);
        runtime.loadConstantSet(rb_mConstants, jnr.constants.platform.IPProto.class);
        runtime.loadConstantSet(rb_mConstants, jnr.constants.platform.Shutdown.class);
        runtime.loadConstantSet(rb_mConstants, jnr.constants.platform.TCP.class);
        runtime.loadConstantSet(rb_mConstants, jnr.constants.platform.NameInfo.class);

        // mandatory constants we haven't implemented
        rb_mConstants.setConstant("MSG_OOB", runtime.newFixnum(MSG_OOB));
        rb_mConstants.setConstant("MSG_PEEK", runtime.newFixnum(MSG_PEEK));
        rb_mConstants.setConstant("MSG_DONTROUTE", runtime.newFixnum(MSG_DONTROUTE));

        // constants webrick crashes without
        rb_mConstants.setConstant("AI_PASSIVE", runtime.newFixnum(1));

        // More constants needed by specs
        rb_mConstants.setConstant("IP_MULTICAST_TTL", runtime.newFixnum(10));
        rb_mConstants.setConstant("IP_MULTICAST_LOOP", runtime.newFixnum(11));
        rb_mConstants.setConstant("IP_ADD_MEMBERSHIP", runtime.newFixnum(12));
        rb_mConstants.setConstant("IP_MAX_MEMBERSHIPS", runtime.newFixnum(20));
        rb_mConstants.setConstant("IP_DEFAULT_MULTICAST_LOOP", runtime.newFixnum(1));
        rb_mConstants.setConstant("IP_DEFAULT_MULTICAST_TTL", runtime.newFixnum(1));

        rb_cSocket.includeModule(rb_mConstants);

        rb_cSocket.defineAnnotatedMethods(RubySocket.class);
    }

    public RubySocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @Override
    protected int getSoTypeDefault() {
        return soType;
    }

    private int soDomain;
    private int soType;
    private int soProtocol;
    @Deprecated
    public static IRubyObject for_fd(IRubyObject socketClass, IRubyObject fd) {
        return for_fd(socketClass.getRuntime().getCurrentContext(), socketClass, fd);
    }
    @JRubyMethod(meta = true)
    public static IRubyObject for_fd(ThreadContext context, IRubyObject socketClass, IRubyObject fd) {
        Ruby ruby = context.getRuntime();
        if (fd instanceof RubyFixnum) {
            RubySocket socket = (RubySocket)((RubyClass)socketClass).allocate();

            // normal file descriptor..try to work with it
            ChannelDescriptor descriptor = ChannelDescriptor.getDescriptorByFileno((int)((RubyFixnum)fd).getLongValue());

            if (descriptor == null) {
                throw ruby.newErrnoEBADFError();
            }

            Channel mainChannel = descriptor.getChannel();

            if (mainChannel instanceof SocketChannel) {
                // ok, it's a socket...set values accordingly
                // just using AF_INET since we can't tell from SocketChannel...
                socket.soDomain = AddressFamily.AF_INET.intValue();
                socket.soType = Sock.SOCK_STREAM.intValue();
                socket.soProtocol = 0;
            } else if (mainChannel instanceof DatagramChannel) {
                // datagram, set accordingly
                // again, AF_INET
                socket.soDomain = AddressFamily.AF_INET.intValue();
                socket.soType = Sock.SOCK_DGRAM.intValue();
                socket.soProtocol = 0;
            } else {
                throw context.getRuntime().newErrnoENOTSOCKError("can't Socket.new/for_fd against a non-socket");
            }

            socket.initSocket(ruby, descriptor);

            return socket;
        } else {
            throw context.getRuntime().newTypeError(fd, context.getRuntime().getFixnum());
        }
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject domain, IRubyObject type, IRubyObject protocol) {
        try {
            if(domain instanceof RubyString) {
                String domainString = domain.toString();
                if(domainString.equals("AF_INET")) {
                    soDomain = AF_INET.intValue();
                } else if(domainString.equals("PF_INET")) {
                    soDomain = PF_INET.intValue();
                } else {
                    throw sockerr(context.getRuntime(), "unknown socket domain " + domainString);
                }
            } else {
                soDomain = RubyNumeric.fix2int(domain);
            }

            if(type instanceof RubyString) {
                String typeString = type.toString();
                if(typeString.equals("SOCK_STREAM")) {
                    soType = SOCK_STREAM.intValue();
                } else if(typeString.equals("SOCK_DGRAM")) {
                    soType = SOCK_DGRAM.intValue();
                } else {
                    throw sockerr(context.getRuntime(), "unknown socket type " + typeString);
                }
            } else {
                soType = RubyNumeric.fix2int(type);
            }

            soProtocol = RubyNumeric.fix2int(protocol);

            Channel channel = null;
            if(soType == Sock.SOCK_STREAM.intValue()) {
                channel = SocketChannel.open();
            } else if(soType == Sock.SOCK_DGRAM.intValue()) {
                channel = DatagramChannel.open();
            }

            initSocket(context.getRuntime(), new ChannelDescriptor(channel, new ModeFlags(ModeFlags.RDWR)));
        } catch (InvalidValueException ex) {
            throw context.getRuntime().newErrnoEINVALError();
        } catch(IOException e) {
            throw sockerr(context.getRuntime(), "initialize: " + e.toString());
        }

        return this;
    }

    private static RuntimeException sockerr(Ruby runtime, String msg) {
        return new RaiseException(runtime, runtime.getClass("SocketError"), msg, true);
    }

    @Deprecated
    public static IRubyObject gethostname(IRubyObject recv) {
        return gethostname(recv.getRuntime().getCurrentContext(), recv);
    }
    @JRubyMethod(meta = true)
    public static IRubyObject gethostname(ThreadContext context, IRubyObject recv) {
        try {
            return context.getRuntime().newString(InetAddress.getLocalHost().getHostName());
        } catch(UnknownHostException e) {
            try {
                return context.getRuntime().newString(InetAddress.getByAddress(new byte[]{0,0,0,0}).getHostName());
            } catch(UnknownHostException e2) {
                throw sockerr(context.getRuntime(), "gethostname: name or service not known");
            }
        }
    }

    private static InetAddress intoAddress(Ruby runtime, String s) {
        try {
            byte[] bs = ByteList.plain(s);
            return InetAddress.getByAddress(bs);
        } catch(Exception e) {
            throw sockerr(runtime, "strtoaddr: " + e.toString());
        }
    }

    private static String intoString(Ruby runtime, InetAddress as) {
        try {
            return new String(ByteList.plain(as.getAddress()));
        } catch(Exception e) {
            throw sockerr(runtime, "addrtostr: " + e.toString());
        }
    }
    @Deprecated
    public static IRubyObject gethostbyaddr(IRubyObject recv, IRubyObject[] args) {
        return gethostbyaddr(recv.getRuntime().getCurrentContext(), recv, args);
    }
    @JRubyMethod(required = 1, rest = true, meta = true)
    public static IRubyObject gethostbyaddr(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        IRubyObject[] ret = new IRubyObject[4];
        ret[0] = runtime.newString(intoAddress(runtime,args[0].convertToString().toString()).getCanonicalHostName());
        ret[1] = runtime.newArray();
        ret[2] = runtime.newFixnum(2); // AF_INET
        ret[3] = args[0];
        return runtime.newArrayNoCopy(ret);
    }

    @Deprecated
    public static IRubyObject getservbyname(IRubyObject recv, IRubyObject[] args) {
        return getservbyname(recv.getRuntime().getCurrentContext(), recv, args);
    }

    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject getservbyname(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        int argc = Arity.checkArgumentCount(runtime, args, 1, 2);
        String name = args[0].convertToString().toString();
        String proto = argc == 1 ? "tcp" : args[1].convertToString().toString();

        jnr.netdb.Service service = jnr.netdb.Service.getServiceByName(name, proto);

        int port;
        if (service != null) {
            port = service.getPort();
        } else {
            // MRI behavior: try to convert the name string to port directly
            try {
                port = Integer.parseInt(name.trim());
            } catch (NumberFormatException nfe) {
                throw sockerr(runtime, "no such service " + name + "/" + proto);
            }
        }

        return runtime.newFixnum(port);
    }

    @JRubyMethod(name = "listen", backtrace = true)
    public IRubyObject listen(ThreadContext context, IRubyObject backlog) {
        return context.getRuntime().newFixnum(0);
    }

    @Deprecated
    public static IRubyObject pack_sockaddr_un(IRubyObject recv, IRubyObject filename) {
        return pack_sockaddr_un(recv.getRuntime().getCurrentContext(), recv, filename);
    }
    @JRubyMethod(name = {"pack_sockaddr_un", "sockaddr_un"}, meta = true)
    public static IRubyObject pack_sockaddr_un(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        StringBuilder sb = new StringBuilder();
        sb.append((char)0);
        sb.append((char)1);
        String str = filename.convertToString().toString();
        sb.append(str);
        for(int i=str.length();i<104;i++) {
            sb.append((char)0);
        }
        return context.getRuntime().newString(sb.toString());
    }

    @JRubyMethod(backtrace = true)
    public IRubyObject connect_nonblock(ThreadContext context, IRubyObject arg) {
        Channel socketChannel = getChannel();
        try {
            if (socketChannel instanceof AbstractSelectableChannel) {
                ((AbstractSelectableChannel) socketChannel).configureBlocking(false);
                connect(context, arg);
            } else {
                throw getRuntime().newErrnoENOPROTOOPTError();
            }
        } catch(ClosedChannelException e) {
            throw context.getRuntime().newErrnoECONNREFUSEDError();
        } catch(IOException e) {
            throw sockerr(context.getRuntime(), "connect(2): name or service not known");
        } catch (Error e) {
            // Workaround for a bug in Sun's JDK 1.5.x, see
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6303753
            Throwable cause = e.getCause();
            if (cause instanceof SocketException) {
                handleSocketException(context.getRuntime(), "connect", (SocketException)cause);
            } else {
                throw e;
            }
        }

        return RubyFixnum.zero(context.getRuntime());
    }

    @JRubyMethod(backtrace = true)
    public IRubyObject connect(ThreadContext context, IRubyObject arg) {
        RubyArray sockaddr = (RubyArray) unpack_sockaddr_in(context, this, arg);

        try {
            IRubyObject addr = sockaddr.pop(context);
            IRubyObject port = sockaddr.pop(context);
            InetSocketAddress iaddr = new InetSocketAddress(
                    addr.convertToString().toString(), RubyNumeric.fix2int(port));

            Channel socketChannel = getChannel();
            if (socketChannel instanceof SocketChannel) {
                if(!((SocketChannel) socketChannel).connect(iaddr)) {
                    throw context.getRuntime().newErrnoEINPROGRESSError();
                }
            } else if (socketChannel instanceof DatagramChannel) {
                ((DatagramChannel)socketChannel).connect(iaddr);
            } else {
                throw getRuntime().newErrnoENOPROTOOPTError();
            }
        } catch(AlreadyConnectedException e) {
            throw context.getRuntime().newErrnoEISCONNError();
        } catch(ConnectionPendingException e) {
            Channel socketChannel = getChannel();
            if (socketChannel instanceof SocketChannel) {
                try {
                    if (((SocketChannel) socketChannel).finishConnect()) {
                        throw context.getRuntime().newErrnoEISCONNError();
                    }
                    throw context.getRuntime().newErrnoEINPROGRESSError();
                } catch (IOException ex) {
                    throw sockerr(context.getRuntime(), "connect(2): name or service not known");
                }
            }
            throw context.getRuntime().newErrnoEINPROGRESSError();
        } catch(UnknownHostException e) {
            throw sockerr(context.getRuntime(), "connect(2): unknown host");
        } catch(SocketException e) {
            handleSocketException(context.getRuntime(), "connect", e);
        } catch(IOException e) {
            throw sockerr(context.getRuntime(), "connect(2): name or service not known");
        } catch (IllegalArgumentException iae) {
            throw sockerr(context.getRuntime(), iae.getMessage());
        } catch (Error e) {
            // Workaround for a bug in Sun's JDK 1.5.x, see
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6303753
            Throwable cause = e.getCause();
            if (cause instanceof SocketException) {
                handleSocketException(context.getRuntime(), "connect", (SocketException)cause);
            } else {
                throw e;
            }
        }
        return RubyFixnum.zero(context.getRuntime());
    }

    @JRubyMethod(backtrace = true)
    public IRubyObject bind(ThreadContext context, IRubyObject arg) {
        RubyArray sockaddr = (RubyArray) unpack_sockaddr_in(context, this, arg);

        try {
            IRubyObject addr = sockaddr.pop(context);
            IRubyObject port = sockaddr.pop(context);
            InetSocketAddress iaddr = new InetSocketAddress(
                    addr.convertToString().toString(), RubyNumeric.fix2int(port));

            Channel socketChannel = getChannel();
            if (socketChannel instanceof SocketChannel) {
                Socket socket = ((SocketChannel)socketChannel).socket();
                socket.bind(iaddr);
            } else if (socketChannel instanceof DatagramChannel) {
                DatagramSocket socket = ((DatagramChannel)socketChannel).socket();
                socket.bind(iaddr);
            } else {
                throw getRuntime().newErrnoENOPROTOOPTError();
            }
        } catch(UnknownHostException e) {
            throw sockerr(context.getRuntime(), "bind(2): unknown host");
        } catch(SocketException e) {
            handleSocketException(context.getRuntime(), "bind", e);
        } catch(IOException e) {
            throw sockerr(context.getRuntime(), "bind(2): name or service not known");
        } catch (IllegalArgumentException iae) {
            throw sockerr(context.getRuntime(), iae.getMessage());
        } catch (Error e) {
            // Workaround for a bug in Sun's JDK 1.5.x, see
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6303753
            Throwable cause = e.getCause();
            if (cause instanceof SocketException) {
                handleSocketException(context.getRuntime(), "bind", (SocketException)cause);
            } else {
                throw e;
            }
        }
        return RubyFixnum.zero(context.getRuntime());
    }

    private void handleSocketException(Ruby runtime, String caller, SocketException e) {
        // e.printStackTrace();
        String msg = formatMessage(e, "bind");

        // This is ugly, but what can we do, Java provides the same exception type
        // for different situations, so we differentiate the errors
        // based on the exception's message.
        if (ALREADY_BOUND_PATTERN.matcher(msg).find()) {
            throw runtime.newErrnoEINVALError(msg);
        } else if (ADDR_NOT_AVAIL_PATTERN.matcher(msg).find()) {
            throw runtime.newErrnoEADDRNOTAVAILError(msg);
        } else if (PERM_DENIED_PATTERN.matcher(msg).find()) {
            throw runtime.newErrnoEACCESError(msg);
        } else {
            throw runtime.newErrnoEADDRINUSEError(msg);
        }
    }

    private static String formatMessage(Throwable e, String defaultMsg) {
        String msg = e.getMessage();
        if (msg == null) {
            msg = defaultMsg;
        } else {
            msg = defaultMsg + " - " + msg;
        }
        return msg;
    }

    @Deprecated
    public static IRubyObject pack_sockaddr_in(IRubyObject recv, IRubyObject port, IRubyObject host) {
        return pack_sockaddr_in(recv.getRuntime().getCurrentContext(), recv, port, host);
    }
    @JRubyMethod(name = {"pack_sockaddr_in", "sockaddr_in"}, meta = true)
    public static IRubyObject pack_sockaddr_in(ThreadContext context, IRubyObject recv, IRubyObject port, IRubyObject host) {
        return pack_sockaddr_in(context, recv,
                RubyNumeric.fix2int(Integer.parseInt(port)),
                host.isNil() ? null : host.convertToString().toString());
    }
    public static IRubyObject pack_sockaddr_in(ThreadContext context, IRubyObject recv, int iport, String host) {
        ByteArrayOutputStream bufS = new ByteArrayOutputStream();
        try {
            DataOutputStream ds = new DataOutputStream(bufS);

            writeSockaddrHeader(ds);
            writeSockaddrPort(ds, iport);

            try {
                if(host != null && "".equals(host)) {
                    ds.writeInt(0);
                } else {
                    InetAddress[] addrs = InetAddress.getAllByName(host);
                    byte[] addr = addrs[0].getAddress();
                    ds.write(addr, 0, addr.length);
                }
            } catch (UnknownHostException e) {
                throw sockerr(context.getRuntime(), "getaddrinfo: No address associated with nodename");
            }

            writeSockaddrFooter(ds);
        } catch (IOException e) {
            throw sockerr(context.getRuntime(), "pack_sockaddr_in: internal error");
        }

        return context.getRuntime().newString(new ByteList(bufS.toByteArray(),
                false));
    }
    static IRubyObject pack_sockaddr_in(ThreadContext context, InetSocketAddress sock) {
        ByteArrayOutputStream bufS = new ByteArrayOutputStream();
        try {
            DataOutputStream ds = new DataOutputStream(bufS);

            writeSockaddrHeader(ds);
            writeSockaddrPort(ds, sock);

            String host = sock.getAddress().getHostAddress();
            if(host != null && "".equals(host)) {
                ds.writeInt(0);
            } else {
                byte[] addr = sock.getAddress().getAddress();
                ds.write(addr, 0, addr.length);
            }

            writeSockaddrFooter(ds);
        } catch (IOException e) {
            throw sockerr(context.getRuntime(), "pack_sockaddr_in: internal error");
        }

        return context.getRuntime().newString(new ByteList(bufS.toByteArray(),
                false));
    }
    @Deprecated
    public static IRubyObject unpack_sockaddr_in(IRubyObject recv, IRubyObject addr) {
        return unpack_sockaddr_in(recv.getRuntime().getCurrentContext(), recv, addr);
    }
    @JRubyMethod(meta = true)
    public static IRubyObject unpack_sockaddr_in(ThreadContext context, IRubyObject recv, IRubyObject addr) {
        ByteList val = addr.convertToString().getByteList();
        if((Platform.IS_BSD && val.get(0) != 16 && val.get(1) != 2) || (!Platform.IS_BSD && val.get(0) != 2)) {
            throw context.getRuntime().newArgumentError("can't resolve socket address of wrong type");
        }

        int port = ((val.get(2)&0xff) << 8) + (val.get(3)&0xff);
        StringBuilder sb = new StringBuilder();
        sb.append(val.get(4)&0xff);
        sb.append(".");
        sb.append(val.get(5)&0xff);
        sb.append(".");
        sb.append(val.get(6)&0xff);
        sb.append(".");
        sb.append(val.get(7)&0xff);

        IRubyObject[] result = new IRubyObject[]{
                context.getRuntime().newFixnum(port),
                context.getRuntime().newString(sb.toString())};

        return context.getRuntime().newArrayNoCopy(result);
    }

    private static final ByteList BROADCAST = new ByteList("<broadcast>".getBytes());
    private static final byte[] INADDR_BROADCAST = new byte[] {-1,-1,-1,-1}; // 255.255.255.255
    private static final ByteList ANY = new ByteList("<any>".getBytes());
    private static final byte[] INADDR_ANY = new byte[] {0,0,0,0}; // 0.0.0.0

    public static InetAddress getRubyInetAddress(ByteList address) throws UnknownHostException {
        if (address.equal(BROADCAST)) {
            return InetAddress.getByAddress(INADDR_BROADCAST);
        } else if (address.equal(ANY)) {
            return InetAddress.getByAddress(INADDR_ANY);
        } else {
            return InetAddress.getByName(address.toString());
        }
    }

    @Deprecated
    public static IRubyObject gethostbyname(IRubyObject recv, IRubyObject hostname) {
        return gethostbyname(recv.getRuntime().getCurrentContext(), recv, hostname);
    }
    @JRubyMethod(meta = true)
    public static IRubyObject gethostbyname(ThreadContext context, IRubyObject recv, IRubyObject hostname) {
        try {
            InetAddress addr = getRubyInetAddress(hostname.convertToString().getByteList());
            Ruby runtime = context.getRuntime();
            IRubyObject[] ret = new IRubyObject[4];
            ret[0] = runtime.newString(addr.getCanonicalHostName());
            ret[1] = runtime.newArray();
            ret[2] = runtime.newFixnum(2); // AF_INET
            ret[3] = runtime.newString(new ByteList(addr.getAddress()));
            return runtime.newArrayNoCopy(ret);
        } catch(UnknownHostException e) {
            throw sockerr(context.getRuntime(), "gethostbyname: name or service not known");
        }
    }

    @Deprecated
    public static IRubyObject getaddrinfo(IRubyObject recv, IRubyObject[] args) {
        return getaddrinfo(recv.getRuntime().getCurrentContext(), recv, args);
    }
    //def self.getaddrinfo(host, port, family = nil, socktype = nil, protocol = nil, flags = nil)
    @JRubyMethod(required = 2, optional = 4, meta = true)
    public static IRubyObject getaddrinfo(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        args = Arity.scanArgs(context.getRuntime(),args,2,4);
        try {
            Ruby r = context.getRuntime();
            IRubyObject host = args[0];
            IRubyObject port = args[1];
            boolean emptyHost = host.isNil() || host.convertToString().isEmpty();

            if(port instanceof RubyString) {
                port = getservbyname(context, recv, new IRubyObject[]{port});
            }

            IRubyObject family = args[2];
            IRubyObject socktype = args[3];
            //IRubyObject protocol = args[4];
            IRubyObject flags = args[5];

            boolean is_ipv6 = (! family.isNil()) && (RubyNumeric.fix2int(family) & AF_INET6.intValue()) == AF_INET6.intValue();
            boolean sock_stream = true;
            boolean sock_dgram = true;
            if(!socktype.isNil()) {
                int val = RubyNumeric.fix2int(socktype);
                if(val == SOCK_STREAM.intValue()) {
                    sock_dgram = false;
                } else if(val == SOCK_DGRAM.intValue()) {
                    sock_stream = false;
                }
            }

            // When Socket::AI_PASSIVE and host is nil, return 'any' address.
            InetAddress[] addrs = null;
            if(!flags.isNil() && RubyFixnum.fix2int(flags) > 0) {
                // The value of 1 is for Socket::AI_PASSIVE.
                int flag = RubyNumeric.fix2int(flags);
                if ((flag == 1) && emptyHost ) {
                    // use RFC 2732 style string to ensure that we get Inet6Address
                    addrs = InetAddress.getAllByName(is_ipv6 ? "[::]" : "0.0.0.0");
                }

            }

            if (addrs == null) {
                addrs = InetAddress.getAllByName(emptyHost ? (is_ipv6 ? "[::1]" : null) : host.convertToString().toString());
            }

            List<IRubyObject> l = new ArrayList<IRubyObject>();
            for(int i = 0; i < addrs.length; i++) {
                IRubyObject[] c;
                if(sock_dgram) {
                    c = new IRubyObject[7];
                    c[0] = r.newString(is_ipv6 ? "AF_INET6" : "AF_INET");
                    c[1] = port;
                    c[2] = r.newString(getHostAddress(recv, addrs[i]));
                    c[3] = r.newString(addrs[i].getHostAddress());
                    c[4] = r.newFixnum(is_ipv6 ? PF_INET6 : PF_INET);
                    c[5] = r.newFixnum(SOCK_DGRAM);
                    c[6] = r.newFixnum(IPPROTO_UDP);
                    l.add(r.newArrayNoCopy(c));
                }
                if(sock_stream) {
                    c = new IRubyObject[7];
                    c[0] = r.newString(is_ipv6 ? "AF_INET6" : "AF_INET");
                    c[1] = port;
                    c[2] = r.newString(getHostAddress(recv, addrs[i]));
                    c[3] = r.newString(addrs[i].getHostAddress());
                    c[4] = r.newFixnum(is_ipv6 ? PF_INET6 : PF_INET);
                    c[5] = r.newFixnum(SOCK_STREAM);
                    c[6] = r.newFixnum(IPPROTO_TCP);
                    l.add(r.newArrayNoCopy(c));
                }
            }
            return r.newArray(l);
        } catch(UnknownHostException e) {
            throw sockerr(context.getRuntime(), "getaddrinfo: name or service not known");
        }
    }

    @Deprecated
    public static IRubyObject getnameinfo(IRubyObject recv, IRubyObject[] args) {
        return getnameinfo(recv.getRuntime().getCurrentContext(), recv, args);
    }
    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject getnameinfo(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        int argc = Arity.checkArgumentCount(runtime, args, 1, 2);
        int flags = argc == 2 ? RubyNumeric.num2int(args[1]) : 0;
        IRubyObject arg0 = args[0];

        String host, port;
        if (arg0 instanceof RubyArray) {
            List list = ((RubyArray)arg0).getList();
            int len = list.size();
            if (len < 3 || len > 4) {
                throw runtime.newArgumentError("array size should be 3 or 4, "+len+" given");
            }
            // if array has 4 elements, third element is ignored
            host = list.size() == 3 ? list.get(2).toString() : list.get(3).toString();
            port = list.get(1).toString();
        } else if (arg0 instanceof RubyString) {
            String arg = ((RubyString)arg0).toString();
            Matcher m = STRING_IPV4_ADDRESS_PATTERN.matcher(arg);
            if (!m.matches()) {
                IRubyObject obj = unpack_sockaddr_in(context, recv, arg0);
                if (obj instanceof RubyArray) {
                    List list = ((RubyArray)obj).getList();
                    int len = list.size();
                    if (len != 2) {
                        throw runtime.newArgumentError("invalid address representation");
                    }
                    host = list.get(1).toString();
                    port = list.get(0).toString();
                }
                else {
                    throw runtime.newArgumentError("invalid address string");
                }
            } else if ((host = m.group(IPV4_HOST_GROUP)) == null || host.length() == 0 ||
                    (port = m.group(IPV4_PORT_GROUP)) == null || port.length() == 0) {
                throw runtime.newArgumentError("invalid address string");
            } else {
                // Try IPv6
                try {
                    InetAddress ipv6_addr = InetAddress.getByName(host);
                    if (ipv6_addr instanceof Inet6Address) {
                        host = ipv6_addr.getHostAddress();
                    }
                } catch (UnknownHostException uhe) {
                    throw runtime.newArgumentError("invalid address string");
                }
            }
        } else {
            throw runtime.newArgumentError("invalid args");
        }

        InetAddress addr;
        try {
            addr = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw sockerr(runtime, "unknown host: "+ host);
        }
        if ((flags & NI_NUMERICHOST.intValue()) == 0) {
            host = addr.getCanonicalHostName();
        } else {
            host = addr.getHostAddress();
        }
        jnr.netdb.Service serv = jnr.netdb.Service.getServiceByPort(Integer.parseInt(port), null);
        if (serv != null) {
            if ((flags & NI_NUMERICSERV.intValue()) == 0) {
                port = serv.getName();
            } else {
                port = Integer.toString(serv.getPort());
            }
        }
        return runtime.newArray(runtime.newString(host), runtime.newString(port));

    }

    private static String getHostAddress(IRubyObject recv, InetAddress addr) {
        return do_not_reverse_lookup(recv).isTrue() ? addr.getHostAddress() : addr.getCanonicalHostName();
    }

    private static void writeSockaddrHeader(DataOutputStream ds) throws IOException {
        if (Platform.IS_BSD) {
            ds.write(16);
            ds.write(2);
        } else {
            ds.write(2);
            ds.write(0);
        }
    }

    private static void writeSockaddrFooter(DataOutputStream ds) throws IOException {
        ds.writeInt(0);
        ds.writeInt(0);
    }

    private static void writeSockaddrPort(DataOutputStream ds, InetSocketAddress sockaddr) throws IOException {
        writeSockaddrPort(ds, sockaddr.getPort());
    }

    private static void writeSockaddrPort(DataOutputStream ds, int port) throws IOException {
        ds.write(port >> 8);
        ds.write(port);
    }

    private static final Pattern STRING_IPV4_ADDRESS_PATTERN =
        Pattern.compile("((.*)\\/)?([\\.0-9]+)(:([0-9]+))?");

    private final static Pattern ALREADY_BOUND_PATTERN = Pattern.compile("[Aa]lready.*bound");
    private final static Pattern ADDR_NOT_AVAIL_PATTERN = Pattern.compile("assign.*address");
    private final static Pattern PERM_DENIED_PATTERN = Pattern.compile("[Pp]ermission.*denied");

    private static final int IPV4_HOST_GROUP = 3;
    private static final int IPV4_PORT_GROUP = 5;
}// RubySocket
