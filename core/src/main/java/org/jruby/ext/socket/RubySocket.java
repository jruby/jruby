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

import jnr.constants.platform.AddressFamily;
import jnr.constants.platform.INAddr;
import jnr.constants.platform.IPProto;
import jnr.constants.platform.NameInfo;
import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Shutdown;
import jnr.constants.platform.Sock;
import jnr.constants.platform.SocketLevel;
import jnr.constants.platform.SocketOption;
import jnr.constants.platform.TCP;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.ChannelFD;
import org.jruby.util.io.FilenoUtil;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.Sockaddr;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import org.jruby.RubyArray;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
@JRubyClass(name="Socket", parent="BasicSocket", include="Socket::Constants")
public class RubySocket extends RubyBasicSocket {
    static void createSocket(Ruby runtime) {
        RubyClass rb_cSocket = runtime.defineClass("Socket", runtime.getClass("BasicSocket"), SOCKET_ALLOCATOR);

        RubyModule rb_mConstants = rb_cSocket.defineModuleUnder("Constants");
        // we don't have to define any that we don't support; see socket.c

        runtime.loadConstantSet(rb_mConstants, Sock.class);
        runtime.loadConstantSet(rb_mConstants, SocketOption.class);
        runtime.loadConstantSet(rb_mConstants, SocketLevel.class);
        runtime.loadConstantSet(rb_mConstants, ProtocolFamily.class);
        runtime.loadConstantSet(rb_mConstants, AddressFamily.class);
        runtime.loadConstantSet(rb_mConstants, INAddr.class);
        runtime.loadConstantSet(rb_mConstants, IPProto.class);
        runtime.loadConstantSet(rb_mConstants, Shutdown.class);
        runtime.loadConstantSet(rb_mConstants, TCP.class);
        runtime.loadConstantSet(rb_mConstants, NameInfo.class);

        // this value seems to be hardcoded in MRI to 5 when not defined, but
        // it is 128 on OS X. We use 128 for now until we can get it added to
        // jnr-constants.
        rb_mConstants.setConstant("SOMAXCONN", RubyFixnum.newFixnum(runtime, 128));

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
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubySocket(runtime, klass);
        }
    };

    public RubySocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject for_fd(ThreadContext context, IRubyObject socketClass, IRubyObject _fd) {
        Ruby runtime = context.runtime;

        if (_fd instanceof RubyFixnum) {
            int intFD = (int)((RubyFixnum)_fd).getLongValue();

            ChannelFD fd = runtime.getFilenoUtil().getWrapperFromFileno(intFD);

            if (fd == null) {
                throw runtime.newErrnoEBADFError();
            }

            RubySocket socket = (RubySocket)((RubyClass)socketClass).allocate();

            socket.initFieldsFromDescriptor(runtime, fd);

            socket.initSocket(fd);

            return socket;
        } else {
            throw runtime.newTypeError(_fd, context.runtime.getFixnum());
        }
    }

    public IRubyObject initialize(ThreadContext context, IRubyObject domain, IRubyObject type, IRubyObject protocol) {
        return initialize(context, domain, type, protocol);
    }

    @JRubyMethod(name = "initialize", visibility = Visibility.PRIVATE)
    public IRubyObject initialize19(ThreadContext context, IRubyObject domain, IRubyObject type) {
        Ruby runtime = context.runtime;

        initFieldsFromArgs(runtime, domain, type);

        ChannelFD fd = initChannelFD(runtime);

        initSocket(fd);

        return this;
    }

    @JRubyMethod(name = "initialize", visibility = Visibility.PRIVATE)
    public IRubyObject initialize19(ThreadContext context, IRubyObject domain, IRubyObject type, IRubyObject protocol) {
        Ruby runtime = context.runtime;

        initFieldsFromArgs(runtime, domain, type, protocol);

        ChannelFD fd = initChannelFD(runtime);

        initSocket(fd);

        return this;
    }

    @JRubyMethod()
    public IRubyObject connect_nonblock(ThreadContext context, IRubyObject arg) {
        SocketAddress addr = addressForChannel(context, arg);

        doConnectNonblock(context, getChannel(), addr);

        return RubyFixnum.zero(context.runtime);
    }

    @JRubyMethod()
    public IRubyObject connect(ThreadContext context, IRubyObject arg) {
        SocketAddress addr = addressForChannel(context, arg);

        doConnect(context, getChannel(), addr);

        return RubyFixnum.zero(context.runtime);
    }

    @JRubyMethod()
    public IRubyObject bind(ThreadContext context, IRubyObject arg) {
        InetSocketAddress iaddr = null;
        
        if (arg instanceof Addrinfo){
            Addrinfo addr = (Addrinfo) arg;
            iaddr = new InetSocketAddress(addr.getInetAddress().getHostAddress(), addr.getPort());
        } else {
             iaddr = Sockaddr.addressFromSockaddr_in(context, arg);
        }

        doBind(context, getChannel(), iaddr);

        return RubyFixnum.zero(context.runtime);
    }

    @JRubyMethod
    public IRubyObject recvfrom(ThreadContext context, IRubyObject length) {
        return super.recv(context, length);
    }

    @JRubyMethod
    public IRubyObject recvfrom(ThreadContext context, IRubyObject length, IRubyObject flags) {
        return super.recv(context, length, flags);
    }

    @JRubyMethod
    public IRubyObject recvfrom_nonblock(ThreadContext context, IRubyObject length) {
        return super.recv_nonblock(context, length);
    }

    @JRubyMethod
    public IRubyObject recvfrom_nonblock(ThreadContext context, IRubyObject length, IRubyObject flags) {
        return super.recv_nonblock(context, length, flags);
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject listen(ThreadContext context, IRubyObject backlog) {
        throw SocketUtils.sockerr(context.runtime, JRUBY_SERVER_SOCKET_ERROR);
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject accept(ThreadContext context) {
        throw SocketUtils.sockerr(context.runtime, JRUBY_SERVER_SOCKET_ERROR);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject gethostname(ThreadContext context, IRubyObject recv) {
        return SocketUtils.gethostname(context);
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject getifaddrs(ThreadContext context, IRubyObject recv) {
        RubyArray list = RubyArray.newArray(context.runtime);
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface ni = en.nextElement();
                // create interface link layer ifaddr
                list.append(new Ifaddr(context.runtime, (RubyClass)context.runtime.getClassFromPath("Socket::Ifaddr"), ni));
                List<InterfaceAddress> listIa = ni.getInterfaceAddresses();
                Iterator<InterfaceAddress> it = listIa.iterator();
                while (it.hasNext()) {
                    InterfaceAddress ia = it.next();
                    list.append(new Ifaddr(context.runtime, (RubyClass)context.runtime.getClassFromPath("Socket::Ifaddr"), ni, ia));
                }
            }
        } catch (Exception ex) {
            throw SocketUtils.sockerr_with_trace(context.runtime, "getifaddrs: " + ex.toString(), ex.getStackTrace());
        }
        return list;
    }

    @JRubyMethod(required = 1, rest = true, meta = true)
    public static IRubyObject gethostbyaddr(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return SocketUtils.gethostbyaddr(context, args);
    }

    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject getservbyname(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return SocketUtils.getservbyname(context, args);
    }

    @JRubyMethod(name = {"pack_sockaddr_in", "sockaddr_in"}, meta = true)
    public static IRubyObject pack_sockaddr_in(ThreadContext context, IRubyObject recv, IRubyObject port, IRubyObject host) {
        return SocketUtils.pack_sockaddr_in(context, port, host);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject unpack_sockaddr_in(ThreadContext context, IRubyObject recv, IRubyObject addr) {
        return Sockaddr.unpack_sockaddr_in(context, addr);
    }

    @JRubyMethod(name = {"pack_sockaddr_un", "sockaddr_un"}, meta = true)
    public static IRubyObject pack_sockaddr_un(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        return SocketUtils.pack_sockaddr_un(context, filename);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject gethostbyname(ThreadContext context, IRubyObject recv, IRubyObject hostname) {
        return SocketUtils.gethostbyname(context, hostname);
    }

    @JRubyMethod(required = 2, optional = 5, meta = true)
    public static IRubyObject getaddrinfo(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return SocketUtils.getaddrinfo(context, args);
    }

    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject getnameinfo(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return SocketUtils.getnameinfo(context, args);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject ip_address_list(ThreadContext context, IRubyObject self) {
        return SocketUtils.ip_address_list(context);
    }

    @Override
    protected Sock getDefaultSocketType() {
        return soType;
    }

    private void initFieldsFromDescriptor(Ruby runtime, ChannelFD fd) {
        Channel mainChannel = fd.ch;

        if (mainChannel instanceof SocketChannel) {
            // ok, it's a socket...set values accordingly
            // just using AF_INET since we can't tell from SocketChannel...
            soDomain = AddressFamily.AF_INET;
            soType = Sock.SOCK_STREAM;
            soProtocol = ProtocolFamily.PF_INET;

        } else if (mainChannel instanceof UnixSocketChannel) {
            soDomain = AddressFamily.AF_UNIX;
            soType = Sock.SOCK_STREAM;
            soProtocol = ProtocolFamily.PF_UNIX;

        } else if (mainChannel instanceof DatagramChannel) {
            // datagram, set accordingly
            // again, AF_INET
            soDomain = AddressFamily.AF_INET;
            soType = Sock.SOCK_DGRAM;
            soProtocol = ProtocolFamily.PF_INET;

        } else {
            throw runtime.newErrnoENOTSOCKError("can't Socket.new/for_fd against a non-socket");
        }
    }

    private void initFieldsFromArgs(Ruby runtime, IRubyObject domain, IRubyObject type, IRubyObject protocol) {
        initDomain(runtime, domain);

        initType(runtime, type);

        initProtocol(runtime, protocol);
    }

    private void initFieldsFromArgs(Ruby runtime, IRubyObject domain, IRubyObject type) {
        initDomain(runtime, domain);

        initType(runtime, type);
    }

    protected void initFromServer(Ruby runtime, RubyServerSocket serverSocket, SocketChannel socketChannel) {
        soDomain = serverSocket.soDomain;
        soType = serverSocket.soType;
        soProtocol = serverSocket.soProtocol;

        initSocket(newChannelFD(runtime, socketChannel));
    }

    protected ChannelFD initChannelFD(Ruby runtime) {
        Channel channel;

        try {
            if(soType == Sock.SOCK_STREAM) {

                if (soProtocol == ProtocolFamily.PF_UNIX ||
                        soProtocol == ProtocolFamily.PF_LOCAL) {
                    channel = UnixSocketChannel.open();
                } else if (soProtocol == ProtocolFamily.PF_INET ||
                        soProtocol == ProtocolFamily.PF_INET6 ||
                        soProtocol == ProtocolFamily.PF_UNSPEC) {
                    channel = SocketChannel.open();
                } else {
                    throw runtime.newArgumentError("unsupported protocol family `" + soProtocol + "'");
                }

            } else if(soType == Sock.SOCK_DGRAM) {
                channel = DatagramChannel.open();

            } else {
                throw runtime.newArgumentError("unsupported socket type `" + soType + "'");

            }

            return newChannelFD(runtime, channel);

        } catch(IOException e) {
            throw SocketUtils.sockerr(runtime, "initialize: " + e.toString());

        }
    }

    private void initProtocol(Ruby runtime, IRubyObject protocol) {
        ProtocolFamily protocolFamily = SocketUtils.protocolFamilyFromArg(protocol);

        if (protocolFamily == null) {
            return; // no protocol specified, ignore it
        }

        soProtocol = protocolFamily;
    }

    private void initType(Ruby runtime, IRubyObject type) {
        Sock sockType = SocketUtils.sockFromArg(type);

        if (sockType == null) {
            throw SocketUtils.sockerr(runtime, "unknown socket type " + type);
        }

        soType = sockType;
    }

    private void initDomain(Ruby runtime, IRubyObject domain) {
        AddressFamily family = SocketUtils.addressFamilyFromArg(domain);

        if (family == null) {
            throw SocketUtils.sockerr(runtime, "unknown socket domain " + domain);
        }

        soDomain = family;
        soProtocol = ProtocolFamily.valueOf("PF" + soDomain.name().substring(2));
    }

    private void doConnectNonblock(ThreadContext context, Channel channel, SocketAddress addr) {
        if (!(channel instanceof SelectableChannel)) {
            throw getRuntime().newErrnoENOPROTOOPTError();
        }

        SelectableChannel selectable = (SelectableChannel)channel;
        synchronized (selectable.blockingLock()) {
            boolean oldBlocking = selectable.isBlocking();
            try {
                selectable.configureBlocking(false);

                try {
                    doConnect(context, channel, addr);

                } finally {
                    selectable.configureBlocking(oldBlocking);
                }

            } catch(ClosedChannelException e) {
                throw context.runtime.newErrnoECONNREFUSEDError();

            } catch(IOException e) {
                throw SocketUtils.sockerr(context.runtime, "connect(2): name or service not known");
            }
        }
    }

    protected void doConnect(ThreadContext context, Channel channel, SocketAddress addr) {
        Ruby runtime = context.runtime;

        try {
            if (channel instanceof SocketChannel) {
                SocketChannel socket = (SocketChannel)channel;
                boolean result;
                
                if (socket.isConnectionPending()) {
                    // connection initiated but not finished
                    result = socket.finishConnect();
                } else {
                    result = socket.connect(addr);
                }

                if(!result) {
                    throw runtime.newErrnoEINPROGRESSWritableError();
                }

            } else if (channel instanceof UnixSocketChannel) {
                ((UnixSocketChannel)channel).connect((UnixSocketAddress)addr);

            } else if (channel instanceof DatagramChannel) {
                ((DatagramChannel)channel).connect(addr);

            } else {
                throw runtime.newErrnoENOPROTOOPTError();

            }

        } catch(AlreadyConnectedException e) {
            throw runtime.newErrnoEISCONNError();

        } catch(ConnectionPendingException e) {
            throw runtime.newErrnoEINPROGRESSWritableError();

        } catch(UnknownHostException e) {
            throw SocketUtils.sockerr(runtime, "connect(2): unknown host");

        } catch(SocketException e) {
            handleSocketException(runtime, "connect", e);

        } catch(IOException e) {
            throw SocketUtils.sockerr(runtime, "connect(2): name or service not known");

        } catch (IllegalArgumentException iae) {
            throw SocketUtils.sockerr(runtime, iae.getMessage());

        }
    }

    protected void doBind(ThreadContext context, Channel channel, InetSocketAddress iaddr) {
        Ruby runtime = context.runtime;

        try {
            if (channel instanceof SocketChannel) {
                Socket socket = ((SocketChannel)channel).socket();
                socket.bind(iaddr);

            } else if (channel instanceof UnixSocketChannel) {
                // do nothing

            } else if (channel instanceof DatagramChannel) {
                DatagramSocket socket = ((DatagramChannel)channel).socket();
                socket.bind(iaddr);

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

    protected void handleSocketException(Ruby runtime, String caller, SocketException e) {
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

    private SocketAddress addressForChannel(ThreadContext context, IRubyObject arg) {
        if (arg instanceof Addrinfo) return Sockaddr.addressFromArg(context, arg);

        switch (soProtocol) {
            case PF_UNIX:
            case PF_LOCAL:
                return Sockaddr.addressFromSockaddr_un(context, arg);

            case PF_INET:
            case PF_INET6:
            case PF_UNSPEC:
                return Sockaddr.addressFromSockaddr_in(context, arg);

            default:
                throw context.runtime.newArgumentError("unsupported protocol family `" + soProtocol + "'");
        }
    }

    @Deprecated
    public static RuntimeException sockerr(Ruby runtime, String msg) {
        return new RaiseException(runtime, runtime.getClass("SocketError"), msg, true);
    }

    private static final Pattern ALREADY_BOUND_PATTERN = Pattern.compile("[Aa]lready.*bound");
    private static final Pattern ADDR_NOT_AVAIL_PATTERN = Pattern.compile("assign.*address");
    private static final Pattern PERM_DENIED_PATTERN = Pattern.compile("[Pp]ermission.*denied");

    public static final int MSG_OOB = 0x1;
    public static final int MSG_PEEK = 0x2;
    public static final int MSG_DONTROUTE = 0x4;
    public static final int MSG_WAITALL = 0x100;

    protected AddressFamily soDomain;
    protected Sock soType;
    protected ProtocolFamily soProtocol;

    private static final String JRUBY_SERVER_SOCKET_ERROR =
            "use ServerSocket for servers (http://wiki.jruby.org/ServerSocket)";
}// RubySocket
