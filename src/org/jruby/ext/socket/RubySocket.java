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

import jnr.constants.platform.AddressFamily;
import jnr.constants.platform.Sock;
import jnr.netdb.Service;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.Sockaddr;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
@JRubyClass(name="Socket", parent="BasicSocket", include="Socket::Constants")
public class RubySocket extends RubyBasicSocket {
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
        rb_mConstants.setConstant("MSG_WAITALL", runtime.newFixnum(MSG_WAITALL));

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

    private static ObjectAllocator SOCKET_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubySocket(runtime, klass);
        }
    };

    public RubySocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @Override
    protected int getSoTypeDefault() {
        return soType;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject for_fd(ThreadContext context, IRubyObject socketClass, IRubyObject fd) {
        Ruby runtime = context.getRuntime();

        if (fd instanceof RubyFixnum) {
            int intFD = (int)((RubyFixnum)fd).getLongValue();

            ChannelDescriptor descriptor = ChannelDescriptor.getDescriptorByFileno(intFD);

            if (descriptor == null) {
                throw runtime.newErrnoEBADFError();
            }

            RubySocket socket = (RubySocket)((RubyClass)socketClass).allocate();

            socket.initFieldsFromDescriptor(runtime, descriptor);

            socket.initSocket(runtime, descriptor);

            return socket;
        } else {
            throw runtime.newTypeError(fd, context.getRuntime().getFixnum());
        }
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject domain, IRubyObject type, IRubyObject protocol) {
        Ruby runtime = context.runtime;

        initFieldsFromArgs(runtime, domain, type, protocol);

        ChannelDescriptor descriptor = initChannel(runtime);

        initSocket(runtime, descriptor);

        return this;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject gethostname(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.runtime;

        try {
            return runtime.newString(InetAddress.getLocalHost().getHostName());

        } catch(UnknownHostException e) {

            try {
                return runtime.newString(InetAddress.getByAddress(new byte[]{0,0,0,0}).getHostName());

            } catch(UnknownHostException e2) {
                throw sockerr(runtime, "gethostname: name or service not known");

            }
        }
    }

    @JRubyMethod(required = 1, rest = true, meta = true)
    public static IRubyObject gethostbyaddr(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        IRubyObject[] ret = new IRubyObject[4];

        ret[0] = runtime.newString(Sockaddr.addressFromString(runtime, args[0].convertToString().toString()).getCanonicalHostName());
        ret[1] = runtime.newArray();
        ret[2] = runtime.newFixnum(2); // AF_INET
        ret[3] = args[0];

        return runtime.newArrayNoCopy(ret);
    }

    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject getservbyname(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        String name = args[0].convertToString().toString();
        String proto = args.length ==  1 ? "tcp" : args[1].convertToString().toString();
        Service service = Service.getServiceByName(name, proto);
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

    @JRubyMethod(name = "listen")
    public IRubyObject listen(ThreadContext context, IRubyObject backlog) {
        return context.getRuntime().newFixnum(0);
    }

    @JRubyMethod(name = {"pack_sockaddr_un", "sockaddr_un"}, meta = true)
    public static IRubyObject pack_sockaddr_un(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        String str = filename.convertToString().toString();

        StringBuilder sb = new StringBuilder()
                .append((char)0)
                .append((char) 1)
                .append(str);

        for(int i=str.length();i<104;i++) {
            sb.append((char)0);
        }

        return context.runtime.newString(sb.toString());
    }

    @JRubyMethod()
    public IRubyObject connect_nonblock(ThreadContext context, IRubyObject arg) {
        InetSocketAddress iaddr = Sockaddr.addressFromSockaddr_in(context, arg);

        doConnectNonblock(context, getChannel(), iaddr);

        return RubyFixnum.zero(context.runtime);
    }

    @JRubyMethod()
    public IRubyObject connect(ThreadContext context, IRubyObject arg) {
        InetSocketAddress iaddr = Sockaddr.addressFromSockaddr_in(context, arg);

        doConnect(context, getChannel(), iaddr);

        return RubyFixnum.zero(context.runtime);
    }

    @JRubyMethod()
    public IRubyObject bind(ThreadContext context, IRubyObject arg) {
        InetSocketAddress iaddr = Sockaddr.addressFromSockaddr_in(context, arg);

        doBind(context, getChannel(), iaddr);

        return RubyFixnum.zero(context.getRuntime());
    }

    @JRubyMethod(name = {"pack_sockaddr_in", "sockaddr_in"}, meta = true)
    public static IRubyObject pack_sockaddr_in(ThreadContext context, IRubyObject recv, IRubyObject port, IRubyObject host) {
        int portNum = port instanceof RubyString ?
                Integer.parseInt(port.convertToString().toString()) :
                RubyNumeric.fix2int(port);

        return Sockaddr.pack_sockaddr_in(
                context,
                portNum,
                host.isNil() ? null : host.convertToString().toString());
    }

    @JRubyMethod(meta = true)
    public static IRubyObject unpack_sockaddr_in(ThreadContext context, IRubyObject recv, IRubyObject addr) {
        return Sockaddr.unpack_sockaddr_in(context, addr);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject gethostbyname(ThreadContext context, IRubyObject recv, IRubyObject hostname) {
        Ruby runtime = context.runtime;

        try {
            InetAddress addr = getRubyInetAddress(hostname.convertToString().getByteList());
            IRubyObject[] ret = new IRubyObject[4];

            ret[0] = runtime.newString(addr.getCanonicalHostName());
            ret[1] = runtime.newArray();
            ret[2] = runtime.newFixnum(2); // AF_INET
            ret[3] = runtime.newString(new ByteList(addr.getAddress()));
            return runtime.newArrayNoCopy(ret);

        } catch(UnknownHostException e) {
            throw sockerr(runtime, "gethostbyname: name or service not known");

        }
    }

    /**
     * Ruby definition would look like:
     *
     * def self.getaddrinfo(host, port, family = nil, socktype = nil, protocol = nil, flags = nil)
     */
    @JRubyMethod(required = 2, optional = 4, meta = true)
    public static IRubyObject getaddrinfo(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        IRubyObject host = args[0];
        IRubyObject port = args[1];
        boolean emptyHost = host.isNil() || host.convertToString().isEmpty();

        try {
            if(port instanceof RubyString) {
                port = getservbyname(context, recv, new IRubyObject[]{port});
            }

            IRubyObject family = args.length > 2 ? args[2] : context.nil;
            IRubyObject socktype = args.length > 3 ? args[3] : context.nil;
            //IRubyObject protocol = args[4];
            IRubyObject flags = args.length > 5 ? args[5] : context.nil;

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
                    c[0] = runtime.newString(is_ipv6 ? "AF_INET6" : "AF_INET");
                    c[1] = port;
                    c[2] = runtime.newString(getHostAddress(recv, addrs[i]));
                    c[3] = runtime.newString(addrs[i].getHostAddress());
                    c[4] = runtime.newFixnum(is_ipv6 ? PF_INET6 : PF_INET);
                    c[5] = runtime.newFixnum(SOCK_DGRAM);
                    c[6] = runtime.newFixnum(IPPROTO_UDP);
                    l.add(runtime.newArrayNoCopy(c));
                }

                if(sock_stream) {
                    c = new IRubyObject[7];
                    c[0] = runtime.newString(is_ipv6 ? "AF_INET6" : "AF_INET");
                    c[1] = port;
                    c[2] = runtime.newString(getHostAddress(recv, addrs[i]));
                    c[3] = runtime.newString(addrs[i].getHostAddress());
                    c[4] = runtime.newFixnum(is_ipv6 ? PF_INET6 : PF_INET);
                    c[5] = runtime.newFixnum(SOCK_STREAM);
                    c[6] = runtime.newFixnum(IPPROTO_TCP);
                    l.add(runtime.newArrayNoCopy(c));
                }
            }

            return runtime.newArray(l);

        } catch(UnknownHostException e) {
            throw sockerr(runtime, "getaddrinfo: name or service not known");

        }
    }

    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject getnameinfo(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        int flags = args.length == 2 ? RubyNumeric.num2int(args[1]) : 0;
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

                } else {
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

    private void initFieldsFromDescriptor(Ruby runtime, ChannelDescriptor descriptor) {
        Channel mainChannel = descriptor.getChannel();

        if (mainChannel instanceof SocketChannel) {
            // ok, it's a socket...set values accordingly
            // just using AF_INET since we can't tell from SocketChannel...
            soDomain = AddressFamily.AF_INET.intValue();
            soType = Sock.SOCK_STREAM.intValue();
            soProtocol = 0;

        } else if (mainChannel instanceof DatagramChannel) {
            // datagram, set accordingly
            // again, AF_INET
            soDomain = AddressFamily.AF_INET.intValue();
            soType = Sock.SOCK_DGRAM.intValue();
            soProtocol = 0;

        } else {
            throw runtime.newErrnoENOTSOCKError("can't Socket.new/for_fd against a non-socket");
        }
    }

    private void initFieldsFromArgs(Ruby runtime, IRubyObject domain, IRubyObject type, IRubyObject protocol) {
        initDomain(runtime, domain);

        initType(runtime, type);

        initProtocol(protocol);
    }

    private ChannelDescriptor initChannel(Ruby runtime) {
        Channel channel;

        try {
            if(soType == Sock.SOCK_STREAM.intValue()) {
                channel = SocketChannel.open();

            } else if(soType == Sock.SOCK_DGRAM.intValue()) {
                channel = DatagramChannel.open();

            } else {
                throw runtime.newArgumentError("unsupported socket type `" + soType + "'");

            }

            ModeFlags modeFlags = newModeFlags(runtime, ModeFlags.RDWR);

            return new ChannelDescriptor(channel, modeFlags);

        } catch(IOException e) {
            throw sockerr(runtime, "initialize: " + e.toString());

        }
    }

    private void initProtocol(IRubyObject protocol) {
        soProtocol = RubyNumeric.fix2int(protocol);
    }

    private void initType(Ruby runtime, IRubyObject type) {
        if(type instanceof RubyString) {
            String typeString = type.toString();
            if(typeString.equals("SOCK_STREAM")) {
                soType = SOCK_STREAM.intValue();
            } else if(typeString.equals("SOCK_DGRAM")) {
                soType = SOCK_DGRAM.intValue();
            } else {
                throw sockerr(runtime, "unknown socket type " + typeString);
            }
        } else {
            soType = RubyNumeric.fix2int(type);
        }
    }

    private void initDomain(Ruby runtime, IRubyObject domain) {
        if(domain instanceof RubyString) {
            String domainString = domain.toString();
            if(domainString.equals("AF_INET")) {
                soDomain = AF_INET.intValue();
            } else if(domainString.equals("PF_INET")) {
                soDomain = PF_INET.intValue();
            } else {
                throw sockerr(runtime, "unknown socket domain " + domainString);
            }
        } else {
            soDomain = RubyNumeric.fix2int(domain);
        }
    }

    private void doConnectNonblock(ThreadContext context, Channel channel, InetSocketAddress iaddr) {
        try {
            if (channel instanceof SelectableChannel) {
                SelectableChannel selectable = (SelectableChannel)channel;
                selectable.configureBlocking(false);

                doConnect(context, channel, iaddr);
            } else {
                throw getRuntime().newErrnoENOPROTOOPTError();

            }

        } catch(ClosedChannelException e) {
            throw context.getRuntime().newErrnoECONNREFUSEDError();

        } catch(IOException e) {
            throw sockerr(context.getRuntime(), "connect(2): name or service not known");

        }
    }

    private void doConnect(ThreadContext context, Channel channel, InetSocketAddress iaddr) {
        Ruby runtime = context.runtime;

        try {
            if (channel instanceof SocketChannel) {
                SocketChannel socket = (SocketChannel)channel;

                if(!socket.connect(iaddr)) {
                    throw context.getRuntime().newErrnoEINPROGRESSError();
                }

            } else if (channel instanceof DatagramChannel) {
                ((DatagramChannel)channel).connect(iaddr);

            } else {
                throw getRuntime().newErrnoENOPROTOOPTError();

            }

        } catch(AlreadyConnectedException e) {
            throw runtime.newErrnoEISCONNError();

        } catch(ConnectionPendingException e) {
            throw runtime.newErrnoEINPROGRESSError();

        } catch(UnknownHostException e) {
            throw sockerr(context.getRuntime(), "connect(2): unknown host");

        } catch(SocketException e) {
            handleSocketException(context.getRuntime(), "connect", e);

        } catch(IOException e) {
            throw sockerr(context.getRuntime(), "connect(2): name or service not known");

        } catch (IllegalArgumentException iae) {
            throw sockerr(context.getRuntime(), iae.getMessage());

        }
    }

    private void doBind(ThreadContext context, Channel channel, InetSocketAddress iaddr) {
        Ruby runtime = context.runtime;

        try {
            if (channel instanceof SocketChannel) {
                Socket socket = ((SocketChannel)channel).socket();
                socket.bind(iaddr);

            } else if (channel instanceof DatagramChannel) {
                DatagramSocket socket = ((DatagramChannel)channel).socket();
                socket.bind(iaddr);

            } else {
                throw runtime.newErrnoENOPROTOOPTError();
            }

        } catch(UnknownHostException e) {
            throw sockerr(runtime, "bind(2): unknown host");

        } catch(SocketException e) {
            handleSocketException(runtime, "bind", e);

        } catch(IOException e) {
            throw sockerr(runtime, "bind(2): name or service not known");

        } catch (IllegalArgumentException iae) {
            throw sockerr(runtime, iae.getMessage());

        }
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

    public static InetAddress getRubyInetAddress(ByteList address) throws UnknownHostException {
        if (address.equal(BROADCAST)) {
            return InetAddress.getByAddress(INADDR_BROADCAST);

        } else if (address.equal(ANY)) {
            return InetAddress.getByAddress(INADDR_ANY);

        } else {
            return InetAddress.getByName(address.toString());

        }
    }

    public static RuntimeException sockerr(Ruby runtime, String msg) {
        return new RaiseException(runtime, runtime.getClass("SocketError"), msg, true);
    }

    private static String getHostAddress(IRubyObject recv, InetAddress addr) {
        return do_not_reverse_lookup(recv).isTrue() ? addr.getHostAddress() : addr.getCanonicalHostName();
    }

    private static final Pattern STRING_IPV4_ADDRESS_PATTERN =
        Pattern.compile("((.*)\\/)?([\\.0-9]+)(:([0-9]+))?");

    private static final Pattern ALREADY_BOUND_PATTERN = Pattern.compile("[Aa]lready.*bound");
    private static final Pattern ADDR_NOT_AVAIL_PATTERN = Pattern.compile("assign.*address");
    private static final Pattern PERM_DENIED_PATTERN = Pattern.compile("[Pp]ermission.*denied");

    private static final int IPV4_HOST_GROUP = 3;
    private static final int IPV4_PORT_GROUP = 5;

    public static final int MSG_OOB = 0x1;
    public static final int MSG_PEEK = 0x2;
    public static final int MSG_DONTROUTE = 0x4;
    public static final int MSG_WAITALL = 0x100;

    private static final ByteList BROADCAST = new ByteList("<broadcast>".getBytes());
    private static final byte[] INADDR_BROADCAST = new byte[] {-1,-1,-1,-1}; // 255.255.255.255
    private static final ByteList ANY = new ByteList("<any>".getBytes());
    private static final byte[] INADDR_ANY = new byte[] {0,0,0,0}; // 0.0.0.0

    private int soDomain;
    private int soType;
    private int soProtocol;
}// RubySocket
