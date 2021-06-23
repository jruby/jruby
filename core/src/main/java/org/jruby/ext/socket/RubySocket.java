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
import jnr.constants.platform.AddressInfo;
import jnr.constants.platform.Errno;
import jnr.constants.platform.INAddr;
import jnr.constants.platform.IP;
import jnr.constants.platform.IPProto;
import jnr.constants.platform.NameInfo;
import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Shutdown;
import jnr.constants.platform.Sock;
import jnr.constants.platform.SocketLevel;
import jnr.constants.platform.SocketMessage;
import jnr.constants.platform.SocketOption;
import jnr.constants.platform.TCP;
import jnr.netdb.Protocol;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.ChannelFD;
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
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
@JRubyClass(name="Socket", parent="BasicSocket", include="Socket::Constants")
public class RubySocket extends RubyBasicSocket {
    static void createSocket(Ruby runtime) {
        RubyClass rb_cSocket = runtime.defineClass("Socket", runtime.getClass("BasicSocket"), RubySocket::new);

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
        runtime.loadConstantSet(rb_mConstants, SocketMessage.class);

        // this value seems to be hardcoded in MRI to 5 when not defined, but
        // it is 128 on OS X. We use 128 for now until we can get it added to
        // jnr-constants.
        rb_mConstants.setConstant("SOMAXCONN", RubyFixnum.newFixnum(runtime, 128));
        
        // for all platforms
        rb_mConstants.setConstant("IPPORT_RESERVED", RubyFixnum.newFixnum(runtime, 1024));
        rb_mConstants.setConstant("IPPORT_USERRESERVED", RubyFixnum.newFixnum(runtime, 5000));

        rb_mConstants.setConstant("AI_PASSIVE", runtime.newFixnum(AddressInfo.AI_PASSIVE));
        rb_mConstants.setConstant("AI_CANONNAME", runtime.newFixnum(AddressInfo.AI_CANONNAME));
        rb_mConstants.setConstant("AI_NUMERICHOST", runtime.newFixnum(AddressInfo.AI_NUMERICHOST));
        rb_mConstants.setConstant("AI_ALL", runtime.newFixnum(AddressInfo.AI_ALL));
        rb_mConstants.setConstant("AI_V4MAPPED_CFG", runtime.newFixnum(AddressInfo.AI_V4MAPPED_CFG));
        rb_mConstants.setConstant("AI_ADDRCONFIG", runtime.newFixnum(AddressInfo.AI_ADDRCONFIG));
        rb_mConstants.setConstant("AI_V4MAPPED", runtime.newFixnum(AddressInfo.AI_V4MAPPED));
        rb_mConstants.setConstant("AI_NUMERICSERV", runtime.newFixnum(AddressInfo.AI_NUMERICSERV));

        rb_mConstants.setConstant("AI_DEFAULT", runtime.newFixnum(AddressInfo.AI_DEFAULT));
        rb_mConstants.setConstant("AI_MASK", runtime.newFixnum(AddressInfo.AI_MASK));

        // More constants needed by specs
        rb_mConstants.setConstant("IP_MULTICAST_TTL", runtime.newFixnum(IP.IP_MULTICAST_TTL.value()));
        rb_mConstants.setConstant("IP_MULTICAST_LOOP", runtime.newFixnum(IP.IP_MULTICAST_LOOP.value()));
        rb_mConstants.setConstant("IP_ADD_MEMBERSHIP", runtime.newFixnum(IP.IP_ADD_MEMBERSHIP.value()));
        rb_mConstants.setConstant("IP_MAX_MEMBERSHIPS", runtime.newFixnum(IP.IP_MAX_MEMBERSHIPS.value()));
        rb_mConstants.setConstant("IP_DEFAULT_MULTICAST_LOOP", runtime.newFixnum(IP.IP_DEFAULT_MULTICAST_LOOP));
        rb_mConstants.setConstant("IP_DEFAULT_MULTICAST_TTL", runtime.newFixnum(IP.IP_DEFAULT_MULTICAST_TTL));

        rb_cSocket.includeModule(rb_mConstants);

        rb_cSocket.defineAnnotatedMethods(RubySocket.class);
    }

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

    @JRubyMethod(name = "initialize", visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject domain, IRubyObject type) {
        Ruby runtime = context.runtime;

        initFromArgs(runtime, domain, type);

        return this;
    }

    @JRubyMethod(name = "initialize", visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject domain, IRubyObject type, IRubyObject protocol) {
        Ruby runtime = context.runtime;

        initFromArgs(runtime, domain, type, protocol);

        return this;
    }

    @JRubyMethod()
    public IRubyObject connect_nonblock(ThreadContext context, IRubyObject arg) {
        return connect_nonblock(context, arg, context.nil);
    }

    @JRubyMethod()
    public IRubyObject connect_nonblock(ThreadContext context, IRubyObject arg, IRubyObject opts) {
        SocketAddress addr = addressForChannel(context, arg);

        return doConnectNonblock(context, addr, extractExceptionArg(context, opts));
    }

    @JRubyMethod()
    public IRubyObject connect(ThreadContext context, IRubyObject arg) {
        SocketAddress addr = addressForChannel(context, arg);

        return doConnect(context, addr, true);
    }

    @JRubyMethod()
    public IRubyObject bind(ThreadContext context, IRubyObject arg) {
        final SocketAddress sockaddr;

        if (arg instanceof Addrinfo) {
            Addrinfo addr = (Addrinfo) arg;
            sockaddr = addr.getSocketAddress();
        } else {
            sockaddr = Sockaddr.addressFromSockaddr(context, arg);
        }

        doBind(context, sockaddr);

        return RubyFixnum.zero(context.runtime);
    }

    @JRubyMethod
    public IRubyObject recvfrom(ThreadContext context, IRubyObject _length) {
        return RubyUDPSocket.recvfrom(this, context, _length);
    }

    /**
     * Overrides IPSocket#recvfrom
     */
    @JRubyMethod
    public IRubyObject recvfrom(ThreadContext context, IRubyObject _length, IRubyObject _flags) {
        // TODO: handle flags
        return recvfrom(context, _length);
    }

    @JRubyMethod(required = 1, optional = 3)
    public IRubyObject recvfrom_nonblock(ThreadContext context, IRubyObject[] args) {
        if (getOpenFile() == null) {
            throw context.runtime.newErrnoENOTCONNError("socket is not connected");
        }
        return RubyUDPSocket.recvfrom_nonblock(this, context, args);
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject listen(ThreadContext context, IRubyObject backlog) {
        throw SocketUtils.sockerr(context.runtime, JRUBY_SERVER_SOCKET_ERROR);
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject accept(ThreadContext context) {
        throw SocketUtils.sockerr(context.runtime, JRUBY_SERVER_SOCKET_ERROR);
    }

    @JRubyMethod(notImplemented = true, optional = 1)
    public IRubyObject accept_nonblock(ThreadContext context, IRubyObject[] args) {
        throw SocketUtils.sockerr(context.runtime, JRUBY_SERVER_SOCKET_ERROR);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject gethostname(ThreadContext context, IRubyObject recv) {
        return SocketUtils.gethostname(context);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject getifaddrs(ThreadContext context, IRubyObject recv) {
        RubyArray list = RubyArray.newArray(context.runtime);
        RubyClass Ifaddr = (RubyClass) context.runtime.getClassFromPath("Socket::Ifaddr");
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface iface = en.nextElement();
                // create interface link layer ifaddr
                list.append(new Ifaddr(context.runtime, Ifaddr, iface));
                for ( InterfaceAddress iaddr : iface.getInterfaceAddresses() ) {
                    list.append(new Ifaddr(context.runtime, Ifaddr, iface, iaddr));
                }
            }
        }
        catch (SocketException | RuntimeException ex) {
            if ( ex instanceof RaiseException) throw (RaiseException) ex;
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
        return Sockaddr.pack_sockaddr_in(context, port, host);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject unpack_sockaddr_in(ThreadContext context, IRubyObject recv, IRubyObject addr) {
        return Sockaddr.unpack_sockaddr_in(context, addr);
    }

    @JRubyMethod(name = {"pack_sockaddr_un", "sockaddr_un"}, meta = true)
    public static IRubyObject pack_sockaddr_un(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        String path = filename.convertToString().asJavaString();
        return Sockaddr.pack_sockaddr_un(context, path);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject unpack_sockaddr_un(ThreadContext context, IRubyObject recv, IRubyObject addr) {
        return Sockaddr.unpack_sockaddr_un(context, addr);
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

    @JRubyMethod(name = {"socketpair", "pair"}, meta = true)
    public static IRubyObject socketpair(ThreadContext context, IRubyObject recv, IRubyObject domain, IRubyObject type, IRubyObject protocol) {
        ProtocolFamily pf = SocketUtils.protocolFamilyFromArg(protocol);
        if (pf == null ) pf = ProtocolFamily.PF_UNIX;

        if (pf != ProtocolFamily.PF_UNIX && pf.ordinal() != 0) {
            throw context.runtime.newErrnoEOPNOTSUPPError("Socket.socketpair only supports streaming UNIX sockets");
        }

        return socketpair(context, recv, domain, type);
    }

    @JRubyMethod(name = {"socketpair", "pair"}, meta = true)
    public static IRubyObject socketpair(ThreadContext context, IRubyObject recv, IRubyObject domain, IRubyObject type) {
        AddressFamily af = SocketUtils.addressFamilyFromArg(domain);
        if (af == null) af = AddressFamily.AF_UNIX;
        Sock s = SocketUtils.sockFromArg(type);
        if (s == null) s = Sock.SOCK_STREAM;

        if (af != AddressFamily.AF_UNIX || s != Sock.SOCK_STREAM) {
            throw context.runtime.newErrnoEOPNOTSUPPError("Socket.socketpair only supports streaming UNIX sockets");
        }

        final Ruby runtime = context.runtime;

        // TODO: type and protocol

        UnixSocketChannel[] sp;

        try {
            sp = UnixSocketChannel.pair();
            final RubyClass socketClass = runtime.getClass("Socket");

            RubySocket sock0 = new RubySocket(runtime, socketClass);
            ChannelFD fd0 = newChannelFD(runtime, sp[0]);
            sock0.initFieldsFromDescriptor(runtime, fd0);
            sock0.initSocket(fd0);

            RubySocket sock1 = new RubySocket(runtime, socketClass);
            ChannelFD fd1 = newChannelFD(runtime, sp[1]);
            sock1.initFieldsFromDescriptor(runtime, fd1);
            sock1.initSocket(fd1);

            return runtime.newArray(sock0, sock1);

        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }
    }

    private void initFieldsFromDescriptor(Ruby runtime, ChannelFD fd) {
        Channel mainChannel = fd.ch;

        if (mainChannel instanceof SocketChannel) {
            // ok, it's a socket...set values accordingly
            // just using AF_INET since we can't tell from SocketChannel...
            soDomain = AddressFamily.AF_INET;
            soType = Sock.SOCK_STREAM;
            soProtocolFamily = ProtocolFamily.PF_INET;
            soProtocol = Protocol.getProtocolByName("tcp");

        } else if (mainChannel instanceof UnixSocketChannel) {
            soDomain = AddressFamily.AF_UNIX;
            soType = Sock.SOCK_STREAM;
            soProtocolFamily = ProtocolFamily.PF_UNIX;

        } else if (mainChannel instanceof DatagramChannel) {
            // datagram, set accordingly
            // again, AF_INET
            soDomain = AddressFamily.AF_INET;
            soType = Sock.SOCK_DGRAM;
            soProtocolFamily = ProtocolFamily.PF_INET;

        } else {
            throw runtime.newErrnoENOTSOCKError("can't Socket.new/for_fd against a non-socket");
        }
    }

    private void initFromArgs(Ruby runtime, IRubyObject domain, IRubyObject type, IRubyObject protocol) {
        setProtocol(protocol);
        initFromArgs(runtime, domain, type);
    }

    private void initFromArgs(Ruby runtime, IRubyObject domain, IRubyObject type) {
        setDomain(runtime, domain);
        setType(runtime, type);

        ChannelFD fd = initChannelFD(runtime);
        initSocket(fd);
    }

    protected void initFromServer(Ruby runtime, RubySocket serverSocket, SocketChannel socketChannel) {
        soDomain = serverSocket.soDomain;
        soType = serverSocket.soType;
        soProtocol = serverSocket.soProtocol;

        initSocket(newChannelFD(runtime, socketChannel));
    }

    protected ChannelFD initChannelFD(Ruby runtime) {
        try {
            Channel channel;
            switch (soType) {
                case SOCK_STREAM:
                    if ( soProtocolFamily == ProtocolFamily.PF_UNIX ||
                         soProtocolFamily == ProtocolFamily.PF_LOCAL ) {
                        channel = UnixSocketChannel.open();
                    }
                    else if ( soProtocolFamily == ProtocolFamily.PF_INET ||
                              soProtocolFamily == ProtocolFamily.PF_INET6 ||
                              soProtocolFamily == ProtocolFamily.PF_UNSPEC ) {
                        channel = SocketChannel.open();
                    }
                    else {
                        throw runtime.newArgumentError("unsupported protocol family `" + soProtocolFamily + "'");
                    }
                    break;
                case SOCK_DGRAM:
                    channel = DatagramChannel.open();
                    break;
                default:
                    throw runtime.newArgumentError("unsupported socket type `" + soType + "'");
            }

            return newChannelFD(runtime, channel);
        }
        catch (IOException e) {
            throw sockerr(runtime, "initialize: " + e.toString(), e);
        }
    }

    private void setProtocol(IRubyObject protocol) {
        soProtocol = SocketUtils.protocolFromArg(protocol);
    }

    private void setType(Ruby runtime, IRubyObject type) {
        Sock sockType = SocketUtils.sockFromArg(type);

        if (sockType == null) {
            throw SocketUtils.sockerr(runtime, "unknown socket type " + type);
        }

        soType = sockType;
    }

    private void setDomain(Ruby runtime, IRubyObject domain) {
        AddressFamily family = SocketUtils.addressFamilyFromArg(domain);

        if (family == null) {
            throw SocketUtils.sockerr(runtime, "unknown socket domain " + domain);
        }

        soDomain = family;
        String name = soDomain.name();
        if (name.startsWith("pseudo_")) name = name.substring(7);
        soProtocolFamily = ProtocolFamily.valueOf("PF" + name.substring(2));
    }

    private IRubyObject doConnectNonblock(ThreadContext context, SocketAddress addr, boolean ex) {
        Ruby runtime = context.runtime;

        Channel channel = getChannel();

        if ( ! (channel instanceof SelectableChannel) ) {
            throw runtime.newErrnoENOPROTOOPTError();
        }

        boolean result = tryConnect(context, runtime, channel, addr, ex, false);

        if ( !result ) {
            if (!ex) return runtime.newSymbol("wait_writable");
            throw runtime.newErrnoEINPROGRESSWritableError();
        }

        return runtime.newFixnum(0);
    }

    protected IRubyObject doConnect(ThreadContext context, SocketAddress addr, boolean ex) {
        Ruby runtime = context.runtime;
        Channel channel = getChannel();

        tryConnect(context, runtime, channel, addr, ex, true);

        return runtime.newFixnum(0);
    }

    private boolean tryConnect(ThreadContext context, Ruby runtime, Channel channel, SocketAddress addr, boolean ex, boolean blocking) {
        SelectableChannel selectable = (SelectableChannel) channel;
        try {
            synchronized (selectable.blockingLock()) {
                boolean oldBlocking = selectable.isBlocking();
                try {
                    selectable.configureBlocking(false);

                    while (true) {
                        boolean result = true;
                        if (channel instanceof SocketChannel) {
                            SocketChannel socket = (SocketChannel) channel;

                            if (socket.isConnectionPending()) {
                                // connection initiated but not finished
                                result = socket.finishConnect();
                            } else {
                                result = socket.connect(addr);
                            }
                        } else if (channel instanceof UnixSocketChannel) {
                            result = ((UnixSocketChannel) channel).connect((UnixSocketAddress) addr);

                        } else if (channel instanceof DatagramChannel) {
                            ((DatagramChannel) channel).connect(addr);
                        } else {
                            throw runtime.newErrnoENOPROTOOPTError();
                        }

                        if (!blocking || result) return result;

                        while (!context.getThread().select(channel, this, SelectionKey.OP_CONNECT)) {
                            context.pollThreadEvents();
                        }
                    }
                } finally {
                    selectable.configureBlocking(oldBlocking);
                }
            }
        } catch (ClosedChannelException e) {
            throw context.runtime.newErrnoECONNREFUSEDError();
        } catch (AlreadyConnectedException e) {
            if (!ex) return false;
            throw runtime.newErrnoEISCONNError();
        } catch (ConnectionPendingException e) {
            throw runtime.newErrnoEINPROGRESSWritableError();
        } catch (UnknownHostException e) {
            throw SocketUtils.sockerr(runtime, "connect(2): unknown host");
        } catch (SocketException e) {
            // Subclasses of SocketException all indicate failure to connect, which leaves the channel closed.
            // At this point the socket channel is no longer usable, so we clean up.
            getOpenFile().cleanup(runtime, true);
            throw buildSocketException(runtime, e, "connect(2)", addr);
        } catch (IOException e) {
            throw sockerr(runtime, "connect(2): name or service not known", e);
        } catch (IllegalArgumentException e) {
            throw sockerr(runtime, e.getMessage(), e);
        }
    }

    protected void doBind(ThreadContext context, SocketAddress iaddr) {
        Ruby runtime = context.runtime;

        Channel channel = getChannel();

        try {
            if (channel instanceof SocketChannel) {
                Socket socket = ((SocketChannel) channel).socket();
                socket.bind(iaddr);
            }
            else if (channel instanceof UnixSocketChannel) {
                // do nothing
            }
            else if (channel instanceof DatagramChannel) {
                DatagramSocket socket = ((DatagramChannel) channel).socket();
                socket.bind(iaddr);
            }
            else {
                throw runtime.newErrnoENOPROTOOPTError();
            }
        }
        catch (UnknownHostException e) {
            throw SocketUtils.sockerr(runtime, "bind(2): unknown host");
        }
        catch (SocketException e) {
            throw buildSocketException(runtime, e, "bind(2)", iaddr); // throws
        }
        catch (IOException e) {
            throw sockerr(runtime, "bind(2): name or service not known", e);
        }
        catch (IllegalArgumentException e) {
            throw sockerr(runtime, e.getMessage(), e);
        }
    }

    static RaiseException buildSocketException(final Ruby runtime, final SocketException ex,
                                               final String caller, final SocketAddress addr) {

        final Errno errno = Helpers.errnoFromException(ex);
        final String callerWithAddr = caller + " for " + formatAddress(addr);

        if (errno != null) {
            return runtime.newErrnoFromErrno(errno, callerWithAddr);
        }

        final String message = ex.getMessage();

        if (message != null) {
            // This is ugly, but what can we do, Java provides the same exception type
            // for different situations, so we differentiate the errors
            // based on the exception's message.
            if (ALREADY_BOUND_PATTERN.matcher(message).find()) {
                return runtime.newErrnoEINVALError(callerWithAddr);
            }
            if (ADDR_NOT_AVAIL_PATTERN.matcher(message).find()) {
                return runtime.newErrnoEADDRNOTAVAILError(callerWithAddr);
            }
        }

        return runtime.newIOError(callerWithAddr);
    }

    private static CharSequence formatAddress(final SocketAddress addr) {
        if ( addr == null ) return null;
        final String str = addr.toString();
        if ( str.length() > 0 && str.charAt(0) == '/' ) {
            return str.substring(1);
        }
        return str;
    }

    private SocketAddress addressForChannel(ThreadContext context, IRubyObject arg) {
        if (arg instanceof Addrinfo) return ((Addrinfo) arg).getSocketAddress();

        switch (soProtocolFamily) {
            case PF_UNIX:
            case PF_LOCAL:
                return Sockaddr.addressFromSockaddr_un(context, arg);

            case PF_INET:
            case PF_INET6:
            case PF_UNSPEC:
                return Sockaddr.addressFromSockaddr_in(context, arg);

            default:
                throw context.runtime.newArgumentError("unsupported protocol family `" + soProtocolFamily + "'");
        }
    }

    @Override
    protected IRubyObject addrFor(ThreadContext context, InetSocketAddress addr, boolean reverse) {
        final Ruby runtime = context.runtime;

        return new Addrinfo(runtime, runtime.getClass("Addrinfo"), addr.getAddress(), addr.getPort(), Sock.SOCK_DGRAM);
    }

    @Override
    @JRubyMethod
    public IRubyObject close(final ThreadContext context) {
        if (getOpenFile() != null) {
            if (isClosed()) return context.nil;
            openFile.checkClosed();
            return rbIoClose(context);
        }
        return context.nil;
    }

    @Override
    public RubyBoolean closed_p(ThreadContext context) {
        if (getOpenFile() == null) return context.fals;

        return super.closed_p(context);
    }

    protected SocketAddress getSocketAddress() {
        Channel channel = getChannel();

        return SocketType.forChannel(channel).getLocalSocketAddress(channel);
    }

    @Deprecated
    public static RuntimeException sockerr(Ruby runtime, String msg) {
        return SocketUtils.sockerr(runtime, msg);
    }

    private static final Pattern ALREADY_BOUND_PATTERN = Pattern.compile("[Aa]lready.*bound");
    private static final Pattern ADDR_NOT_AVAIL_PATTERN = Pattern.compile("assign.*address");
    //private static final Pattern PERM_DENIED_PATTERN = Pattern.compile("[Pp]ermission.*denied");

    protected AddressFamily soDomain;
    protected ProtocolFamily soProtocolFamily;
    protected Sock soType;
    protected Protocol soProtocol = Protocol.getProtocolByNumber(0);

    private static final String JRUBY_SERVER_SOCKET_ERROR =
            "use ServerSocket for servers (https://github.com/jruby/jruby/wiki/ServerSocket)";
}// RubySocket
