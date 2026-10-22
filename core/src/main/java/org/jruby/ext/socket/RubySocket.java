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
import jnr.constants.platform.InterfaceInfo;
import jnr.netdb.Protocol;
import jnr.unixsocket.UnixSocketAddress;
import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Access;
import org.jruby.api.Convert;
import org.jruby.api.Create;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.platform.Platform;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.ChannelFD;
import org.jruby.util.io.OpenFile;
import org.jruby.util.io.Sockaddr;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
import java.nio.channels.NetworkChannel;
import java.nio.channels.ServerSocketChannel;
import java.util.Enumeration;
import java.util.regex.Pattern;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Create.*;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;

@JRubyClass(name="Socket", parent="BasicSocket", include="Socket::Constants")
public class RubySocket extends RubyBasicSocket {

    static RubyClass createSocket(ThreadContext context, RubyClass BasicSocket) {
        RubyClass Socket = defineClass(context, "Socket", BasicSocket, RubySocket::new).
                defineMethods(context, RubySocket.class);

        Socket.setInternalVariable("tcp_fast_fallback", getFastFallbackDefault(context));

        RubyModule SocketConstants = Socket.defineModuleUnder(context, "Constants").
                defineConstantsFrom(context, Sock.class).
                defineConstantsFrom(context, SocketOption.class).
                defineConstantsFrom(context, SocketLevel.class).
                defineConstantsFrom(context, ProtocolFamily.class).
                defineConstantsFrom(context, AddressFamily.class).
                defineConstantsFrom(context, INAddr.class).
                defineConstantsFrom(context, IPProto.class).
                defineConstantsFrom(context, Shutdown.class).
                defineConstantsFrom(context, TCP.class).
                defineConstantsFrom(context, IP.class).
                defineConstantsFrom(context, InterfaceInfo.class).
                defineConstantsFrom(context, NameInfo.class).
                defineConstantsFrom(context, SocketMessage.class);

        // this value seems to be hardcoded in MRI to 5 when not defined, but
        // it is 128 on OS X. We use 128 for now until we can get it added to
        // jnr-constants.
        SocketConstants.defineConstant(context, "SOMAXCONN", asFixnum(context, 128)).
                defineConstant(context, "IPPORT_RESERVED", asFixnum(context, 1024)).
                defineConstant(context, "IPPORT_USERRESERVED", asFixnum(context, 5000)).
                defineConstant(context, "AI_PASSIVE", asFixnum(context, AddressInfo.AI_PASSIVE.longValue())).
                defineConstant(context, "AI_CANONNAME", asFixnum(context, AddressInfo.AI_CANONNAME.longValue())).
                defineConstant(context, "AI_NUMERICHOST", asFixnum(context, AddressInfo.AI_NUMERICHOST.longValue())).
                defineConstant(context, "AI_ALL", asFixnum(context, AddressInfo.AI_ALL.longValue())).
                defineConstant(context, "AI_V4MAPPED_CFG", asFixnum(context, AddressInfo.AI_V4MAPPED_CFG.longValue())).
                defineConstant(context, "AI_ADDRCONFIG", asFixnum(context, AddressInfo.AI_ADDRCONFIG.longValue())).
                defineConstant(context, "AI_V4MAPPED", asFixnum(context, AddressInfo.AI_V4MAPPED.longValue())).
                defineConstant(context, "AI_NUMERICSERV", asFixnum(context, AddressInfo.AI_NUMERICSERV.longValue())).
                defineConstant(context, "AI_DEFAULT", asFixnum(context, AddressInfo.AI_DEFAULT.longValue())).
                defineConstant(context, "AI_MASK", asFixnum(context, AddressInfo.AI_MASK.longValue()));

        Socket.include(context, SocketConstants);

        return Socket;
    }

    public RubySocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject for_fd(ThreadContext context, IRubyObject socketClass, IRubyObject _fd) {
        int intFD = Convert.castAsFixnum(context, _fd).asInt(context);
        ChannelFD fd = context.runtime.getFilenoUtil().getWrapperFromFileno(intFD);

        if (fd == null) throw context.runtime.newErrnoEBADFError();

        RubySocket socket = (RubySocket)((RubyClass)socketClass).allocate(context);

        socket.initFieldsFromDescriptor(context.runtime, fd);
        socket.initSocket(fd);

        return socket;
    }

    @JRubyMethod(name = "initialize", visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject domain, IRubyObject type) {
        initFromArgs(context, domain, type);
        return this;
    }

    @JRubyMethod(name = "initialize", visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject domain, IRubyObject type, IRubyObject protocol) {
        initFromArgs(context, domain, type, protocol);
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

    @JRubyMethod(required = 1, optional = 3, checkArity = false)
    public IRubyObject recvfrom_nonblock(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 1, 4);

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

    @JRubyMethod(notImplemented = true, optional = 1, checkArity = false)
    public IRubyObject accept_nonblock(ThreadContext context, IRubyObject[] args) {
        throw SocketUtils.sockerr(context.runtime, JRUBY_SERVER_SOCKET_ERROR);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject gethostname(ThreadContext context, IRubyObject recv) {
        return SocketUtils.gethostname(context);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject getifaddrs(ThreadContext context, IRubyObject recv) {
        var list = newArray(context);
        RubyClass Ifaddr = (RubyClass) context.runtime.getClassFromPath("Socket::Ifaddr");
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface iface = en.nextElement();
                // create interface link layer ifaddr
                list.append(context, new Ifaddr(context.runtime, Ifaddr, iface));
                for ( InterfaceAddress iaddr : iface.getInterfaceAddresses() ) {
                    list.append(context, new Ifaddr(context.runtime, Ifaddr, iface, iaddr));
                }
            }
        }
        catch (SocketException | RuntimeException ex) {
            if ( ex instanceof RaiseException) throw (RaiseException) ex;
            throw SocketUtils.sockerr_with_trace(context.runtime, "getifaddrs: " + ex.toString(), ex.getStackTrace());
        }
        return list;
    }

    @JRubyMethod(required = 1, rest = true, checkArity = false, meta = true)
    public static IRubyObject gethostbyaddr(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 1, -1);

        return SocketUtils.gethostbyaddr(context, args);
    }

    @JRubyMethod(required = 1, optional = 1, checkArity = false, meta = true)
    public static IRubyObject getservbyname(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 1, 2);

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

    @JRubyMethod(required = 2, optional = 5, checkArity = false, meta = true)
    public static IRubyObject getaddrinfo(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 2, 7);

        return SocketUtils.getaddrinfo(context, args);
    }

    @JRubyMethod(required = 1, optional = 1, checkArity = false, meta = true)
    public static IRubyObject getnameinfo(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 1, 2);

        return SocketUtils.getnameinfo(context, args);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject ip_address_list(ThreadContext context, IRubyObject self) {
        return SocketUtils.ip_address_list(context);
    }

    @JRubyMethod(name = {"socketpair", "pair"}, meta = true)
    public static IRubyObject socketpair(ThreadContext context, IRubyObject recv, IRubyObject domain, IRubyObject type, IRubyObject protocol, Block block) {
        ProtocolFamily pf = SocketUtils.protocolFamilyFromArg(context, protocol);
        if (pf == null ) pf = ProtocolFamily.PF_UNIX;

        if (pf != ProtocolFamily.PF_UNIX && pf.ordinal() != 0) {
            throw context.runtime.newErrnoEOPNOTSUPPError("Socket.socketpair only supports streaming UNIX sockets");
        }

        return socketpair(context, recv, domain, type, block);
    }

    @JRubyMethod(name = {"socketpair", "pair"}, meta = true)
    public static IRubyObject socketpair(ThreadContext context, IRubyObject recv, IRubyObject domain, IRubyObject type, Block block) {
        AddressFamily af = SocketUtils.addressFamilyFromArg(context, domain);
        if (af == null) af = AddressFamily.AF_UNIX;
        Sock s = SocketUtils.sockFromArg(context, type);
        if (s == null) s = Sock.SOCK_STREAM;

        // socketpair(2) is AF_UNIX-only on Linux; stream and datagram pairs via
        // FFM (UnixDomain#pair), replacing JNR's stream-only UnixSocketChannel.pair()
        if (af != AddressFamily.AF_UNIX || (s != Sock.SOCK_STREAM && s != Sock.SOCK_DGRAM && s != Sock.SOCK_SEQPACKET)) {
            throw context.runtime.newErrnoEOPNOTSUPPError("socketpair(2)");
        }

        final Ruby runtime = context.runtime;

        final RubyClass socketClass = Access.getClass(context, "Socket");
        RubySocket sock0 = new RubySocket(runtime, socketClass);
        RubySocket sock1 = new RubySocket(runtime, socketClass);

        try {
            Channel[] sp = UnixDomain.pair(s.intValue());
            sock0.initUnixPairSocket(runtime, sp[0], s);
            sock1.initUnixPairSocket(runtime, sp[1], s);
        } catch (UnsupportedOperationException uoe) {
            throw runtime.newErrnoEOPNOTSUPPError("socketpair(2)");
        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }

        return yieldPair(context, sock0, sock1, block);
    }

    private void initUnixPairSocket(Ruby runtime, Channel channel, Sock type) {
        soDomain = AddressFamily.AF_UNIX;
        soType = type;
        soProtocolFamily = ProtocolFamily.PF_UNIX;
        initSocket(newChannelFD(runtime, channel));
    }

    private void initFieldsFromDescriptor(Ruby runtime, ChannelFD fd) {
        Channel mainChannel = fd.ch;

        if (UnixDomain.isUnixChannel(mainChannel)) {
            // JEP-380 unix SocketChannel or FFM UnixFdChannel
            soDomain = AddressFamily.AF_UNIX;
            soType = mainChannel instanceof UnixFdChannel ufc && ufc.sockType() == UnixDomainNative.SOCK_DGRAM ?
                    Sock.SOCK_DGRAM : Sock.SOCK_STREAM;
            soProtocolFamily = ProtocolFamily.PF_UNIX;

        } else if (mainChannel instanceof SocketChannel) {
            // ok, it's a socket...set values accordingly
            // just using AF_INET since we can't tell from SocketChannel...
            soDomain = AddressFamily.AF_INET;
            soType = Sock.SOCK_STREAM;
            soProtocolFamily = ProtocolFamily.PF_INET;
            soProtocol = Protocol.getProtocolByName("tcp");

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

    private void initFromArgs(ThreadContext context, IRubyObject domain, IRubyObject type, IRubyObject protocol) {
        setProtocol(context, protocol);
        initFromArgs(context, domain, type);
    }

    private void initFromArgs(ThreadContext context, IRubyObject domain, IRubyObject type) {
        setDomain(context, domain);
        setType(context, type);

        ChannelFD fd = initChannelFD(context.runtime);
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
                        // JEP-380, as UNIXSocket/UNIXServer: this was the last
                        // JNR UnixSocketChannel.open() (bind was a no-op on it)
                        channel = SocketChannel.open(java.net.StandardProtocolFamily.UNIX);
                    }
                    else if ( soProtocolFamily == ProtocolFamily.PF_INET ||
                              soProtocolFamily == ProtocolFamily.PF_INET6 ||
                              soProtocolFamily == ProtocolFamily.PF_UNSPEC ) {
                        java.net.ProtocolFamily family = javaFamily();
                        channel = family == null ? SocketChannel.open() : SocketChannel.open(family);
                    }
                    else {
                        throw argumentError(runtime.getCurrentContext(), "unsupported protocol family '" + soProtocolFamily + "'");
                    }
                    break;
                case SOCK_DGRAM:
                    if ( soProtocolFamily == ProtocolFamily.PF_UNIX ||
                         soProtocolFamily == ProtocolFamily.PF_LOCAL ) {
                        // JEP-380 has no Unix-domain DatagramChannel at all
                        // (DatagramChannel.open(ProtocolFamily) rejects UNIX
                        // outright) -- raw socket(2) via FFM, wrapped the
                        // same way pair()/for_fd fds are (see
                        // UnixDomainNative#rawSocket's doc comment).
                        try {
                            int fd = UnixDomainNative.rawSocket(UnixDomainNative.AF_UNIX, UnixDomainNative.SOCK_DGRAM, 0);
                            channel = UnixDomainNative.channelFromFd(fd);
                        } catch (UnixDomainNative.NativeException ne) {
                            throw sockerr(runtime, "initialize: " + ne.getMessage(), ne);
                        }
                    } else {
                        java.net.ProtocolFamily family = javaFamily();
                        channel = family == null ? DatagramChannel.open() : DatagramChannel.open(family);
                    }
                    break;
                default:
                    throw argumentError(runtime.getCurrentContext(), "unsupported socket type '" + soType + "'");
            }

            return newChannelFD(runtime, channel);
        }
        catch (IOException e) {
            throw sockerr(runtime, "initialize: " + e.toString(), e);
        }
    }

    /**
     * The JDK family to open an inet channel with. A no-arg open() is a
     * dual-stack IPv6 socket, so an AF_INET socket bound to 0.0.0.0 reported
     * itself as [::] (AF_INET6) -- wrong local_address/connect_address. MRI's
     * socket(AF_INET, ...) is IPv4-only, which an INET-family channel matches.
     */
    private java.net.ProtocolFamily javaFamily() {
        if (soProtocolFamily == ProtocolFamily.PF_INET) return java.net.StandardProtocolFamily.INET;
        if (soProtocolFamily == ProtocolFamily.PF_INET6) return java.net.StandardProtocolFamily.INET6;
        return null;
    }

    private void setProtocol(ThreadContext context, IRubyObject protocol) {
        if (protocol instanceof RubySymbol || protocol instanceof RubyString) {
            // MRI takes the protocol as an Integer only (Socket.new(:INET, :STREAM, :TCP) is a TypeError)
            throw typeError(context, "no implicit conversion of " + protocol.getMetaClass().getRealClass().getName(context) + " into Integer");
        }
        soProtocol = SocketUtils.protocolFromArg(context, protocol);
    }

    private void setType(ThreadContext context, IRubyObject type) {
        Sock sockType = SocketUtils.sockFromArg(context, type);

        if (sockType == null) {
            throw SocketUtils.sockerr(context.runtime, "unknown socket type " + type);
        }

        soType = sockType;
    }

    private void setDomain(ThreadContext context, IRubyObject domain) {
        AddressFamily family = SocketUtils.addressFamilyFromArg(context, domain);

        if (family == null) {
            throw SocketUtils.sockerr(context.runtime, "unknown socket domain " + domain);
        }

        soDomain = family;
        String name = soDomain.name();
        if (name.startsWith("pseudo_")) name = name.substring(7);
        soProtocolFamily = ProtocolFamily.valueOf("PF" + name.substring(2));
    }

    private IRubyObject doConnectNonblock(ThreadContext context, SocketAddress addr, boolean ex) {
        Channel channel = getChannel();

        if (!(channel instanceof SelectableChannel)) throw context.runtime.newErrnoENOPROTOOPTError();

        boolean result = tryConnect(context, channel, addr, ex, false);

        if ( !result ) {
            if (!ex) return Convert.asSymbol(context, "wait_writable");
            throw context.runtime.newErrnoEINPROGRESSWritableError();
        }

        return asFixnum(context, 0);
    }

    protected IRubyObject doConnect(ThreadContext context, SocketAddress addr, boolean ex) {
        tryConnect(context, getChannel(), addr, ex, true);

        return asFixnum(context, 0);
    }

    private boolean tryConnect(ThreadContext context, Channel channel, SocketAddress addr, boolean ex, boolean blocking) {
        SelectableChannel selectable = (SelectableChannel) channel;

        // whether to clean up after a failed connection
        boolean cleanup = false;

        try {
            synchronized (selectable.blockingLock()) {
                boolean oldBlocking = selectable.isBlocking();
                try {
                    selectable.configureBlocking(false);

                    boolean firstAttempt = true;
                    while (true) {
                        boolean result = true;
                        if (channel instanceof SocketChannel) {
                            SocketChannel socket = (SocketChannel) channel;

                            if (socket.isConnected()) {
                                // connected before this call: connect(2) says EISCONN (MRI);
                                // connected by a pending connect this call finished: done
                                if (firstAttempt) throw new AlreadyConnectedException();
                                return true;
                            } else if (socket.isConnectionPending()) {
                                // connection initiated but not finished
                                result = socket.finishConnect();
                            } else {
                                result = socket.connect(addr);
                            }
                        } else if (channel instanceof DatagramChannel datagram) {
                            datagram.connect(addr);
                        } else {
                            throw context.runtime.newErrnoENOPROTOOPTError();
                        }

                        if (!blocking || result) return result;
                        firstAttempt = false;

                        while (!context.getThread().select(channel, this, SelectionKey.OP_CONNECT)) {
                            context.blockingThreadPoll();
                        }
                    }
                } finally {
                    selectable.configureBlocking(oldBlocking);
                }
            }
        } catch (ClosedChannelException e) {
            cleanup = true;
            throw context.runtime.newErrnoECONNREFUSEDError();
        } catch (AlreadyConnectedException e) {
            if (!ex) return false;
            throw context.runtime.newErrnoEISCONNError();
        } catch (ConnectionPendingException e) {
            throw context.runtime.newErrnoEINPROGRESSWritableError();
        } catch (UnknownHostException e) {
            cleanup = true;
            throw SocketUtils.sockerr(context.runtime, "connect(2): unknown host");
        } catch (SocketException e) {
            cleanup = true;
            throw buildSocketException(context.runtime, e, "connect(2)", addr);
        } catch (IOException e) {
            cleanup = true;
            throw sockerr(context.runtime, "connect(2): name or service not known", e);
        } catch (IllegalArgumentException e) {
            cleanup = true;
            throw sockerr(context.runtime, e.getMessage(), e);
        } finally {
            // Some exceptions indicate failure to connect, which leaves the channel closed.
            // At this point the socket channel is no longer usable, so we clean up.
            if (cleanup) getOpenFile().cleanup(context.runtime, true);
        }
    }

    protected void doBind(ThreadContext context, SocketAddress iaddr) {
        Ruby runtime = context.runtime;

        Channel channel = getChannel();

        try {
            if (channel instanceof SocketChannel sc) {
                // SocketChannel#bind, not socket().bind: socket() is
                // unsupported on a JEP-380 unix channel
                sc.bind(jdkAddress(iaddr));
            }
            else if (channel instanceof UnixFdChannel ufc) {
                // raw Socket.new(:UNIX, :DGRAM) fd (see initChannelFD) --
                // iaddr here is jnr.unixsocket.UnixSocketAddress (Sockaddr's
                // UNIX parsing predates this PR and wasn't in scope to
                // replace), not JEP-380's UnixDomainSocketAddress.
                String path = iaddr instanceof UnixSocketAddress usa ? usa.path() : null;
                if (path == null) throw runtime.newErrnoENOPROTOOPTError();
                try {
                    UnixDomainNative.bindPath(ufc.fd(), path);
                } catch (UnixDomainNative.NativeException ne) {
                    throw sockerr(runtime, "bind(2): " + ne.getMessage(), ne);
                }
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

    /**
     * Sockaddr/Addrinfo give unix addresses as JNR's UnixSocketAddress; a
     * JEP-380 channel needs java.net.UnixDomainSocketAddress.
     */
    static SocketAddress jdkAddress(SocketAddress address) {
        if (address instanceof UnixSocketAddress unix) return java.net.UnixDomainSocketAddress.of(unix.path());
        return address;
    }

    private SocketAddress addressForChannel(ThreadContext context, IRubyObject arg) {
        if (arg instanceof Addrinfo) return jdkAddress(((Addrinfo) arg).getSocketAddress());

        switch (soProtocolFamily) {
            case PF_UNIX:
            case PF_LOCAL:
                return jdkAddress(Sockaddr.addressFromSockaddr_un(context, arg));

            case PF_INET:
            case PF_INET6:
            case PF_UNSPEC:
                return Sockaddr.addressFromSockaddr_in(context, arg);

            default:
                throw argumentError(context, "unsupported protocol family '" + soProtocolFamily + "'");
        }
    }

    @Override
    protected IRubyObject addrFor(ThreadContext context, InetSocketAddress addr, boolean reverse) {
        return new Addrinfo(context.runtime, Access.getClass(context, "Addrinfo"), addr.getAddress(), addr.getPort(), Sock.SOCK_DGRAM);
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

    @JRubyMethod(meta = true)
    public static IRubyObject tcp_fast_fallback(ThreadContext context, IRubyObject self) {
        return (IRubyObject) Access.getClass(context, "Socket").getInternalVariable("tcp_fast_fallback");
    }

    @JRubyMethod(name = "tcp_fast_fallback=", meta = true)
    public static IRubyObject tcp_fast_fallback_set(ThreadContext context, IRubyObject self, IRubyObject value) {
        Access.getClass(context, "Socket").setInternalVariable("tcp_fast_fallback", value);
        return value;
    }

    private static RubyBoolean getFastFallbackDefault(ThreadContext context) {
        IRubyObject tcpNoFastFallbackConfig = context.runtime.getENV().fastARef(Create.newString(context, "RUBY_TCP_NO_FAST_FALLBACK"));
        if (tcpNoFastFallbackConfig == null || tcpNoFastFallbackConfig.isNil()
                || tcpNoFastFallbackConfig.toString().equals("0")) {
            return context.tru;
        } else {
            return context.fals;
        }
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
