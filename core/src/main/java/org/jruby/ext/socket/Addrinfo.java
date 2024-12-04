package org.jruby.ext.socket;

import jnr.constants.platform.AddressFamily;
import jnr.constants.platform.NameInfo;
import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Sock;
import jnr.netdb.Protocol;
import jnr.netdb.Service;
import jnr.unixsocket.UnixSocketAddress;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyClass;
import org.jruby.RubyInteger;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.Sockaddr;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import static jnr.constants.platform.AddressFamily.AF_INET;
import static jnr.constants.platform.AddressFamily.AF_INET6;
import static jnr.constants.platform.AddressFamily.AF_UNIX;
import static jnr.constants.platform.AddressFamily.AF_UNSPEC;
import static jnr.constants.platform.IPProto.IPPROTO_TCP;
import static jnr.constants.platform.IPProto.IPPROTO_UDP;
import static jnr.constants.platform.ProtocolFamily.PF_INET;
import static jnr.constants.platform.ProtocolFamily.PF_INET6;
import static jnr.constants.platform.ProtocolFamily.PF_UNIX;
import static jnr.constants.platform.ProtocolFamily.PF_UNSPEC;
import static jnr.constants.platform.Sock.*;
import static org.jruby.api.Check.checkEmbeddedNulls;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newString;
import static org.jruby.ext.socket.SocketUtils.IP_V4_MAPPED_ADDRESS_PREFIX;
import static org.jruby.ext.socket.SocketUtils.sockerr;

public class Addrinfo extends RubyObject {

    // TODO: (gf) these constants should be in their respective .h files
    final short ARPHRD_ETHER    =   1;  // ethernet hatype (if_arp.h)
    final short ARPHRD_LOOPBACK	= 772;	// loopback hatype (if_arp.h)
    final short AF_PACKET       =  17;  // packet socket (socket.h)
    final byte  PACKET_HOST     =   0;  // host packet type (if_packet.h)

    public static void createAddrinfo(Ruby runtime) {
        RubyClass addrinfo = runtime.defineClass(
                "Addrinfo",
                runtime.getObject(),
                Addrinfo::new);

        addrinfo.defineAnnotatedMethods(Addrinfo.class);
    }

    public Addrinfo(Ruby runtime, RubyClass cls) {
        super(runtime, cls);
    }

    public Addrinfo(Ruby runtime, RubyClass cls, NetworkInterface networkInterface, boolean isBroadcast) {
        super(runtime, cls);
        this.networkInterface = networkInterface;
        this.interfaceLink = true;
        this.isBroadcast = isBroadcast;
        this.interfaceName = networkInterface.getName();
        this.socketType = SocketType.SOCKET;
    }

    public Addrinfo(Ruby runtime, RubyClass cls, InetAddress inetAddress) {
        super(runtime, cls);
        this.socketAddress = new InetSocketAddress(inetAddress, 0);
        this.socketType = SocketType.SOCKET;
    }

    public Addrinfo(Ruby runtime, RubyClass cls, InetAddress inetAddress, int port, Sock sock) {
        super(runtime, cls);
        this.socketAddress = new InetSocketAddress(inetAddress, port);
        this.pfamily = ProtocolFamily.valueOf(getAddressFamily().intValue());
        this.socketType = SocketType.SOCKET;
        setSockAndProtocol(sock);
    }

    public Addrinfo(Ruby runtime, RubyClass cls, SocketAddress socketAddress, Sock sock, SocketType socketType) {
        super(runtime, cls);
        this.socketAddress = socketAddress;
        this.pfamily = ProtocolFamily.valueOf(getAddressFamily().intValue());
        this.socketType = socketType;
        setSockAndProtocol(sock);
    }

    public Addrinfo(Ruby runtime, RubyClass cls, SocketAddress socketAddress, Sock sock, SocketType socketType, boolean displaysCanonical) {
        super(runtime, cls);
        this.socketAddress = socketAddress;
        this.pfamily = ProtocolFamily.valueOf(getAddressFamily().intValue());
        this.socketType = socketType;
        this.displaysCanonical = displaysCanonical;
        setSockAndProtocol(sock);
    }

    public Addrinfo(Ruby runtime, RubyClass cls, InetAddress inetAddress, int port) {
        super(runtime, cls);
        this.socketAddress = new InetSocketAddress(inetAddress, port);
        setSockAndProtocol(SOCK_STREAM);
    }

    public Addrinfo(Ruby runtime, RubyClass cls, SocketAddress socketAddress) {
        super(runtime, cls);
        this.socketAddress = socketAddress;
        this.pfamily = ProtocolFamily.valueOf(getAddressFamily().intValue());
        setSockAndProtocol(SOCK_STREAM);
    }

    public int getPort() {
        return socketAddress instanceof InetSocketAddress ? getInetSocketAddress().getPort() : -1;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject _sockaddr) {
        initializeCommon(context, _sockaddr, context.nil, context.nil, context.nil);
        return context.nil;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject _sockaddr, IRubyObject _family) {
        initializeCommon(context, _sockaddr, _family, context.nil, context.nil);
        return context.nil;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject _sockaddr, IRubyObject _family, IRubyObject _socktype) {
        initializeCommon(context, _sockaddr, _family, _socktype, context.nil);
        return context.nil;
    }

    @JRubyMethod(required = 1, optional = 3, checkArity = false, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 1:
                return initialize(context, args[0]);
            case 2:
                return initialize(context, args[0], args[1]);
            case 3:
                return initialize(context, args[0], args[1], args[2]);
            case 4:
                // use logic below
                break;
            default:
                Arity.raiseArgumentError(context, args.length, 1, 4);
        }

        IRubyObject _sockaddr = args[0];
        IRubyObject _family = args[1];
        IRubyObject _socktype = args[2];
        IRubyObject _protocol = args[3];

        initializeCommon(context, _sockaddr, _family, _socktype, _protocol);

        return context.nil;
    }
    /*
     * Flag values for getaddrinfo()
    */
    public static final int AI_PASSIVE = 0x00000001; /* get address to use bind() */
    public static final int AI_CANONNAME = 0x00000002; /* fill ai_canonname */
    public static final int AI_NUMERICHOST = 0x00000004; /* prevent name resolution */
    public static final int AI_NUMERICSERV = 0x00000008; /* prevent service name resolution */
    public static final int AI_MASK = (AI_PASSIVE | AI_CANONNAME | AI_NUMERICHOST | AI_NUMERICSERV);

    public static final int AI_ALL = 0x00000100; /* IPv6 and IPv4-mapped (with AI_V4MAPPED) */
    public static final int AI_V4MAPPED_CFG = 0x00000200; /* accept IPv4-mapped if kernel supports */
    public static final int AI_ADDRCONFIG = 0x00000400; /* only if any address     is assigned */
    public static final int AI_V4MAPPED = 0x00000800; /* accept IPv4-mapped IPv6 address */
    /* special recommended flags for getipnodebyname */
    public static final int AI_DEFAULT = (AI_V4MAPPED_CFG | AI_ADDRCONFIG);

    private Addrinfo initializeSimple(IRubyObject host, IRubyObject port, int socketType, int protocolFamily) {
        InetAddress inetAddress = getRubyInetAddress(host);

        this.socketAddress = new InetSocketAddress(inetAddress, port != null ? SocketUtils.portToInt(port) : 0);
        this.pfamily = getInetAddress() instanceof Inet4Address ? PF_INET : PF_INET6;
        this.protocol = Protocol.getProtocolByNumber(protocolFamily);
        this.socketType = SocketType.values()[socketType];
        this.sock = Sock.valueOf(socketType);

        return this;
    }

    private void initializeCommon(ThreadContext context, IRubyObject sockaddrArg, IRubyObject protocolFamilyArg,
                                  IRubyObject socketTypeArg, IRubyObject protocolArg) {
        Ruby runtime = context.runtime;

        try {
            IRubyObject testArray = TypeConverter.checkArrayType(context, sockaddrArg);
            if (testArray != context.nil) {
                RubyArray sockaddAry = (RubyArray) testArray;
                AddressFamily af = SocketUtils.addressFamilyFromArg(sockaddAry.entry(0).convertToString());

                ProtocolFamily pf;
                if (protocolFamilyArg.isNil()) {
                    pf = af == AF_INET6 ? PF_INET6 : PF_INET;
                } else {
                    pf = SocketUtils.protocolFamilyFromArg(protocolFamilyArg);
                }

                if (pf != PF_UNIX) {
                    if (af == AF_INET6 && pf != PF_INET && pf != PF_INET6) {
                        throw sockerr(runtime, "The given protocol and address families are incompatible");
                    }
                    af = AddressFamily.valueOf(pf.intValue());
                }

                if (af != AF_UNIX &&  af != AF_UNSPEC && af != AF_INET && af != AF_INET6) {
                    throw sockerr(runtime, "Address family must be AF_UNIX, AF_INET, AF_INET6, PF_INET or PF_INET6");
                }

                int proto = protocolArg.isNil() ? 0 : protocolArg.convertToInteger().getIntValue();
                int socketType = socketTypeArg.isNil() ? 0 : socketTypeArg.convertToInteger().getIntValue();

                if (socketType == 0) {
                    if (proto != 0 && proto != IPPROTO_UDP.intValue()) throw sockerr(runtime, "getaddrinfo: ai_socktype not supported");
                } else if (socketType == SOCK_DGRAM.intValue()) {
                    if (proto != 0 && proto != IPPROTO_UDP.intValue()) {
                        throw sockerr(runtime, "getaddrinfo: ai_socktype not supported");
                    }
                } else if (socketType == SOCK_STREAM.intValue()) {
                    if (proto != 0 && proto != IPPROTO_TCP.intValue()) {
                        throw sockerr(runtime, "getaddrinfo: ai_socktype not supported");
                    }
                } else if (socketType == SOCK_SEQPACKET.intValue()) {
                    if (proto != 0) throw sockerr(runtime, "getaddrinfo: ai_socktype not supported");
                } else if (socketType != SOCK_RAW.intValue()) {
                    throw sockerr(runtime, "getaddrinfo: ai_socktype not supported");
                }

                if (af == AF_UNIX || pf == PF_UNIX) { /* ["AF_UNIX", "/tmp/sock"] */
                    IRubyObject path = sockaddAry.eltOk(1).checkStringType();
                    if (path.isNil()) throw sockerr(runtime, "getaddrinfo: ai_family not supported");
                    socketAddress = new UnixSocketAddress(new File(path.toString()));
                    this.socketType = SocketType.UNIX;
                    this.sock = SOCK_STREAM;

                    return;
                } else if (af == AF_INET || pf == PF_INET ||    // ["AF_INET", 46102, "localhost.localdomain", "127.0.0.1"]
                           af == AF_INET6 || pf == PF_INET6) {  // ["AF_INET6", 42304, "ip6-localhost", "::1"]
                    IRubyObject service = sockaddAry.entry(1).convertToInteger();
                    IRubyObject nodename = sockaddAry.entry(2);
                    String numericnode = sockaddAry.entry(3).convertToString().toString();
                    int _port = service.convertToInteger().getIntValue();

                    InetAddress inetAddress;
                    boolean ipv4PrefixedString;
                    if (!nodename.isNil()) {
                        String address = nodename.convertToString().toString();
                        inetAddress = getRubyInetAddress(address, numericnode);
                        ipv4PrefixedString = false;
                    } else {
                        inetAddress = getRubyInetAddress(numericnode);
                        ipv4PrefixedString = SocketUtils.isIPV4MappedAddressPrefix(numericnode);
                        if (ipv4PrefixedString) looksLikeV4ButIsV6 = true;
                    }

                    this.socketAddress = new InetSocketAddress(inetAddress, _port);

                    if (af == AF_INET6 && getInetAddress() instanceof Inet4Address && !ipv4PrefixedString) {
                        throw sockerr(runtime, "getaddrinfo: Address family for hostname not supported");
                    }

                    if (SocketType.values().length <= socketType) {
                        this.socketType = SocketType.SOCKET;
                    } else {
                        this.socketType = SocketType.values()[socketType];
                        Sock sock = Sock.valueOf(socketType);
                        if (sock != __UNKNOWN_CONSTANT__) this.sock = sock;
                    }

                    this.pfamily = pf;
                    // unknown protos will end up as null but this should be an indication of a mismatch of networking
                    // constants and netdb not lining up (constants which should not exist of db entries not existing
                    // which should).
                    this.protocol = Protocol.getProtocolByNumber(proto);
                    return;
                } else {
                    throw sockerr(runtime, "getaddrinfo: unknown address family: " + protocolFamilyArg);
                }

            } else {
                this.socketAddress = Sockaddr.sockaddrFromBytes(context, sockaddrArg.convertToString().getBytes());
                this.pfamily = protocolFamilyArg.isNil() ? PF_UNSPEC: SocketUtils.protocolFamilyFromArg(protocolFamilyArg);
                if (!protocolArg.isNil()) this.protocol = SocketUtils.protocolFromArg(protocolArg);
                if (!socketTypeArg.isNil()) this.sock = SocketUtils.sockFromArg(socketTypeArg);
                this.socketType = SocketType.SOCKET;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw runtime.newIOErrorFromException(ioe);
        }
    }

    private void setSockAndProtocol(Sock sock) {
        this.sock = sock;
        if (socketAddress instanceof InetSocketAddress) {
            if (this.sock == SOCK_STREAM) {
                protocol = Protocol.getProtocolByName("tcp");
            } else if (this.sock == SOCK_DGRAM) {
                protocol = Protocol.getProtocolByName("udp");
            }
        }
    }

    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        String base = "#<Addrinfo: %s>";
        StringBuilder val = new StringBuilder();

        if (interfaceLink == true) {
            val.append(packet_inspect());
        }  else {
            val.append(inspect_sockaddr(context).toString());
        }
        
        if ((pfamily == PF_INET || pfamily == PF_INET6) && (sock == SOCK_STREAM || sock == SOCK_DGRAM)) {
            val.append(" ").append(protocol.getName().toUpperCase());
        } else if (sock != null) {
            val.append(" ").append(sock.name().toUpperCase());
        } else if (protocol != null && protocol.getProto() != 0) {
            val.append(" ").append(String.format("UNKNOWN PROTOCOL(%d)", protocol.getProto()));
        }

        String inspectName = inspectname();
        if (inspectName != null && interfaceLink == false) {
            val.append(" (").append(inspectName).append(")");
        }
       
        return newString(context, String.format(base, val.toString()));
    }

    @JRubyMethod
    public IRubyObject inspect_sockaddr(ThreadContext context) {
        if (socketAddress instanceof UnixSocketAddress) {
            String path = getUnixSocketAddress().path();

            return newString(context, path.startsWith("/") ? path : "UNIX " + path);
        }

        int port = getInetSocketAddress().getPort();

        if (getInetAddress() instanceof Inet6Address) {
            String host = ipv6_ip();
            String hostPort = port == 0 ? host : "[" + host + "]:" + port;

            return newString(context, hostPort);
        }

        String portString = port == 0 ? "" : ":" + port;
        String host = (looksLikeV4ButIsV6 ? IP_V4_MAPPED_ADDRESS_PREFIX : "") +
                getInetSocketAddress().getAddress().getHostAddress();

        return newString(context, host + portString);
    }

    @JRubyMethod(rest = true, meta = true)
    public static IRubyObject getaddrinfo(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyArray.newArray(context.runtime, SocketUtils.getaddrinfoList(context, args));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject ip(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        String host = checkEmbeddedNulls(context, arg.convertToString()).toString();
        try {
            boolean specifiedV6 = host.indexOf(':') != -1;
            InetAddress addy = SocketUtils.getRubyInetAddress(host);
            Addrinfo addrinfo = new Addrinfo(context.runtime, (RubyClass) recv, addy);
            addrinfo.protocol = Protocol.getProtocolByName("ip");
            if (specifiedV6 && addy instanceof Inet4Address) {
                addrinfo.looksLikeV4ButIsV6 = true;
                addrinfo.pfamily = PF_INET6;
            } else if (addy instanceof Inet6Address) {
                addrinfo.pfamily = PF_INET6;
            } else {
                addrinfo.pfamily = PF_INET;
            }

            return addrinfo;
        } catch (UnknownHostException uhe) {
            throw sockerr(context.runtime, "host not found");
        }
    }

    @JRubyMethod(meta = true)
    public static IRubyObject tcp(ThreadContext context, IRubyObject recv, IRubyObject host, IRubyObject port) {
        Addrinfo addrinfo = new Addrinfo(context.runtime, (RubyClass) recv);
        return addrinfo.initializeSimple(host, port, SOCK_STREAM.intValue(), IPPROTO_TCP.intValue());
    }

    @JRubyMethod(meta = true)
    public static IRubyObject udp(ThreadContext context, IRubyObject recv, IRubyObject host, IRubyObject port) {
        Addrinfo addrinfo = new Addrinfo(context.runtime, (RubyClass) recv);
        return addrinfo.initializeSimple(host, port, SOCK_DGRAM.intValue(), IPPROTO_UDP.intValue());
    }

    @JRubyMethod(meta = true)
    public static IRubyObject unix(ThreadContext context, IRubyObject recv, IRubyObject path) {
        Addrinfo addrinfo = new Addrinfo(context.runtime, (RubyClass) recv);

        addrinfo.socketAddress = new UnixSocketAddress(new File(path.convertToString().toString()));
        addrinfo.sock = SOCK_STREAM;
        addrinfo.socketType = SocketType.UNIX;
        addrinfo.pfamily = PF_UNIX;
        addrinfo.protocol = Protocol.getProtocolByName("ip");

        return addrinfo;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject unix(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject type) {
        Addrinfo addrinfo = new Addrinfo(context.runtime, (RubyClass) recv);

        addrinfo.socketAddress = new UnixSocketAddress(new File(path.convertToString().toString()));
        addrinfo.sock = SocketUtils.sockFromArg(type);
        addrinfo.socketType = SocketType.UNIX;
        addrinfo.pfamily = PF_UNIX;
        addrinfo.protocol = Protocol.getProtocolByName("ip");

        return addrinfo;
    }

    @JRubyMethod
    public IRubyObject afamily(ThreadContext context) {
        return asFixnum(context, getAddressFamily().intValue());
    }

    @JRubyMethod
    public IRubyObject pfamily(ThreadContext context) {
        return asFixnum(context, pfamily.intValue());
    }

    @JRubyMethod
    public IRubyObject socktype(ThreadContext context) {
      return asFixnum(context, sock == null ? 0 : sock.intValue());
    }

    @JRubyMethod
    public IRubyObject protocol(ThreadContext context) {
        // Any unknown protocol will end up null but the one case we see is IPPROTO_RAW is not listed in /etc/protocols
        return asFixnum(context, protocol == null ? 255 : protocol.getProto());
    }

    @JRubyMethod
    public IRubyObject canonname(ThreadContext context) {
        if (!displaysCanonical) return context.nil;

        if (socketAddress instanceof InetSocketAddress) {
            return newString(context, getInetSocketAddress().getAddress().getCanonicalHostName());
        } else if (socketAddress instanceof UnixSocketAddress) {
            return newString(context, getUnixSocketAddress().path());
        }
        throw context.runtime.newNotImplementedError("canonname not implemented for socket address: " + socketAddress);
    }

    @JRubyMethod(name = "ipv4?")
    public IRubyObject ipv4_p(ThreadContext context) {
        return asBoolean(context, getAddressFamily() == AF_INET);
    }

    @JRubyMethod(name = "ipv6?")
    public IRubyObject ipv6_p(ThreadContext context) {
        return asBoolean(context, getAddressFamily() == AF_INET6);
    }

    @JRubyMethod(name = "unix?")
    public IRubyObject unix_p(ThreadContext context) {
        return asBoolean(context, getAddressFamily() == AF_UNIX);
    }

    @JRubyMethod(name = "ip?")
    public IRubyObject ip_p(ThreadContext context) {
        return asBoolean(context, getAddressFamily() == AF_INET || getAddressFamily() == AF_INET6);
    }

    @JRubyMethod
    public IRubyObject ip_unpack(ThreadContext context) {
        return newArray(context, ip_address(context), ip_port(context));
    }

    @JRubyMethod
    public IRubyObject ip_address(ThreadContext context) {
        if (getAddressFamily() != AF_INET && getAddressFamily() != AF_INET6) {
            throw sockerr(context.runtime, "need IPv4 or IPv6 address");
        }
        // TODO: (gf) for IPv6 link-local address this appends a numeric interface index (like MS-Windows), should append interface name on Linux
        String host = (getAddressFamily() == AF_INET6) ?
                ipv6_ip() :
                ((InetSocketAddress) socketAddress).getAddress().getHostAddress();

        return newString(context, host);
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject ip_port(ThreadContext context) {
        if (getAddressFamily() != AF_INET && getAddressFamily() != AF_INET6) {
            throw sockerr(context.runtime, "need IPv4 or IPv6 address");
        }
        return asFixnum(context, ((InetSocketAddress) socketAddress).getPort());
    }

    @JRubyMethod(name = "ipv4_private?")
    public IRubyObject ipv4_private_p(ThreadContext context) {
        if (getAddressFamily() == AF_INET) {
            return asBoolean(context, getInet4Address().isSiteLocalAddress());
        }
        return asBoolean(context, false);
    }

    @JRubyMethod(name = "ipv4_loopback?")
    public IRubyObject ipv4_loopback_p(ThreadContext context) {
      if (getAddressFamily() == AF_INET) {
        return asBoolean(context, ((InetSocketAddress) socketAddress).getAddress().isLoopbackAddress());
      }
      return asBoolean(context, false);
    }

    @JRubyMethod(name = "ipv4_multicast?")
    public IRubyObject ipv4_multicast_p(ThreadContext context) {
        if (getAddressFamily() == AF_INET) {
            return asBoolean(context, getInet4Address().isMulticastAddress());
        }
        return context.fals;
    }

    @JRubyMethod(name = "ipv6_unspecified?")
    public IRubyObject ipv6_unspecified_p(ThreadContext context) {
        if (getAddressFamily() == AF_INET6) {
            return asBoolean(context, ipv6_ip().equals("::"));
        }
        return context.fals;
    }

    @JRubyMethod(name = "ipv6_loopback?")
    public IRubyObject ipv6_loopback_p(ThreadContext context) {
      if (getAddressFamily() == AF_INET6) {
        return asBoolean(context, getInetSocketAddress().getAddress().isLoopbackAddress());
      }
      return asBoolean(context, false);
    }

    @JRubyMethod(name = "ipv6_multicast?")
    public IRubyObject ipv6_multicast_p(ThreadContext context) {
        if (getAddressFamily() == AF_INET6) {
            return asBoolean(context, getInet6Address().isMulticastAddress());
        }
        return context.fals;
    }

    @JRubyMethod(name = "ipv6_linklocal?")
    public IRubyObject ipv6_linklocal_p(ThreadContext context) {
        return asBoolean(context, getInetSocketAddress().getAddress().isLinkLocalAddress());
    }

    @JRubyMethod(name = "ipv6_sitelocal?")
    public IRubyObject ipv6_sitelocal_p(ThreadContext context) {
        return asBoolean(context, ((InetSocketAddress) socketAddress).getAddress().isSiteLocalAddress());
    }

    @JRubyMethod(name = "ipv6_unique_local?")
    public IRubyObject ipv6_unique_local_p(ThreadContext context) {
        if (getAddressFamily() != AF_INET6) {
            return context.fals;
        }
        Inet6Address address = getInet6Address();
        if (address == null) {
            return context.fals;
        }
        int firstAddrByte = address.getAddress()[0] & 0xff;
        return asBoolean(context, firstAddrByte == 0xfc || firstAddrByte == 0xfd);
    }

    @JRubyMethod(name = "ipv6_v4mapped?")
    public IRubyObject ipv6_v4mapped_p(ThreadContext context) {
        return asBoolean(context, looksLikeV4ButIsV6);
    }

    @JRubyMethod(name = "ipv6_v4compat?")
    public IRubyObject ipv6_v4compat_p(ThreadContext context) {
        return asBoolean(context, isIPV6() && !looksLikeV4ButIsV6);
    }

    @JRubyMethod(name = "ipv6_mc_nodelocal?")
    public IRubyObject ipv6_mc_nodelocal_p(ThreadContext context) {
        Inet6Address in6 = getInet6Address();
        return asBoolean(context, in6 != null && in6.isMCNodeLocal());
    }

    @JRubyMethod(name = "ipv6_mc_linklocal?")
    public IRubyObject ipv6_mc_linklocal_p(ThreadContext context) {
        Inet6Address in6 = getInet6Address();
        return asBoolean(context, in6 != null && in6.isMCLinkLocal());
    }

    @JRubyMethod(name = "ipv6_mc_sitelocal?")
    public IRubyObject ipv6_mc_sitelocal_p(ThreadContext context) {
        Inet6Address in6 = getInet6Address();
        return asBoolean(context, in6 != null && in6.isMCSiteLocal());
    }

    @JRubyMethod(name = "ipv6_mc_orglocal?")
    public IRubyObject ipv6_mc_orglocal_p(ThreadContext context) {
        Inet6Address in6 = getInet6Address();
        return asBoolean(context, in6 != null && in6.isMCOrgLocal());
    }

    @JRubyMethod(name = "ipv6_mc_global?")
    public IRubyObject ipv6_mc_global_p(ThreadContext context) {
        Inet6Address in6 = getInet6Address();
        return asBoolean(context, in6 != null && in6.isMCGlobal());
    }

    // FIXME: What is the actual name for ::0.0.0.1
    private boolean is_0001(byte[] raw) {
        int length = raw.length;

        return raw[length - 4] == 0 && raw[length - 3] == 0 && raw[length - 2] == 0 && raw[length - 1] == 1;
    }

    private boolean isIPV6() {
        return (getInetAddress() instanceof Inet6Address || looksLikeV4ButIsV6) && !is_0001(getInetAddress().getAddress());
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject ipv6_to_ipv4(ThreadContext context) {
        InetAddress in = getInetAddress();

        if (in instanceof Inet6Address) {
            Inet6Address addr = (Inet6Address) in;

            if (addr.isIPv4CompatibleAddress()) {
                byte[] raw = addr.getAddress();
                if (is_0001(raw)) return context.nil;
                byte[] ip4Raw = new byte[4];
                System.arraycopy(raw, 12, ip4Raw, 0, 4);

                try {
                    InetAddress newAddr = InetAddress.getByAddress(ip4Raw);
                    Addrinfo newAddrInfo = new Addrinfo(getRuntime(), getMetaClass());
                    newAddrInfo.pfamily = PF_INET;
                    newAddrInfo.socketAddress = new InetSocketAddress(newAddr, getPort());
                    newAddrInfo.sock = sock;
                    newAddrInfo.socketType = socketType;

                    return newAddrInfo;
                } catch (UnknownHostException e) {
                    // should not happen since we would already have a valid ipv6 address.
                }
            }
        } else if (in instanceof Inet4Address) {
            // This will be specified originally as ipv6 but Java converts it to ipv4.
            if (looksLikeV4ButIsV6) {
                if (is_0001(((Inet4Address) in).getAddress())) return context.nil;
                if (getAddressFamily() != AF_INET) {
                    Addrinfo newAddrInfo = new Addrinfo(getRuntime(), getMetaClass());
                    newAddrInfo.pfamily = PF_INET;
                    newAddrInfo.socketAddress = socketAddress;
                    newAddrInfo.sock = sock;
                    newAddrInfo.socketType = socketType;
                    return newAddrInfo;
                }
            }
        }

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject unix_path(ThreadContext context) {
        if (getAddressFamily() != AF_UNIX) throw sockerr(context.runtime, "need AF_UNIX address");

        // Get path and strip out preceeding NUL (sometimes on linux) and any trailing NUL.
        String path = getUnixSocketAddress().humanReadablePath();
        int index = path.indexOf(0);
        if (index != -1) path = path.substring(0, index);
        return newString(context, path);
    }

    @JRubyMethod(name = {"to_sockaddr", "to_s"})
    public IRubyObject to_sockaddr(ThreadContext context) {
        switch (getAddressFamily()) {
            case AF_INET:
            case AF_INET6:
                InetAddress inetAddress = ((InetSocketAddress) socketAddress).getAddress();
                int port = ((InetSocketAddress) socketAddress).getPort();
                return Sockaddr.pack_sockaddr_in(context, port, inetAddress.getHostAddress());
            case AF_UNIX:
                return Sockaddr.pack_sockaddr_un(context, getUnixSocketAddress().path());
            case AF_UNSPEC:
                ByteArrayOutputStream bufS = new ByteArrayOutputStream();
                DataOutputStream ds = new DataOutputStream(bufS);
                try {                                                      // struct sockaddr_ll {  (see: man 7 packet)
                  ds.writeShort(swapShortEndian(AF_PACKET));               //   unsigned short sll_family;   /* Always AF_PACKET */
                  ds.writeShort(0);                                        //   unsigned short sll_protocol; /* Physical layer protocol */
                  ds.writeInt(swapIntEndian(networkInterface.getIndex())); //   int            sll_ifindex;  /* Interface number */
                  ds.writeShort(swapShortEndian(hatype()));                //   unsigned short sll_hatype;   /* ARP hardware type */
                  ds.writeByte(PACKET_HOST);                               //   unsigned char  sll_pkttype;  /* Packet type */
                  byte[] hw = hwaddr();
                  ds.writeByte(hw.length);                                 //   unsigned char  sll_halen;    /* Length of address */
                  ds.write(hw);                                            //   unsigned char  sll_addr[8];  /* Physical layer address */
                } catch (IOException e) {
                    throw sockerr(context.runtime, "to_sockaddr: " + e.getMessage());
                }
                return context.runtime.newString(new ByteList(bufS.toByteArray(), false));
      }
      return context.nil;
    }

    private short hatype() {
      try {
        short ht = ARPHRD_ETHER;
        if (networkInterface.isLoopback()) {
          ht = ARPHRD_LOOPBACK;
        }
        return ht;
      } catch (IOException e) {
        return 0;
      }
    }

    private byte[] hwaddr() {
      try {
        byte[] hw = {0,0,0,0,0,0};                            // loopback
        if (!networkInterface.isLoopback()) {
          hw = networkInterface.getHardwareAddress();
          if (hw == null) {
            hw = new byte[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}; // UNSPEC link encap
          }
        }
        if (isBroadcast) {
          hw = new byte[]{-1,-1,-1,-1,-1,-1};  // == 0xFF
        }
        return hw;
      } catch (IOException e) {
        return ByteList.NULL_ARRAY; // if bad things happened return empty address rather than null or raising exception
      }
    }

    public String packet_inspect() {
      StringBuffer hwaddr_sb = new StringBuffer();
      String sep = "";
      for (byte b: hwaddr()) {
        hwaddr_sb.append(sep);
        sep = ":";
        hwaddr_sb.append(String.format("%02x", b));
      }
      return "PACKET[protocol=0 " + interfaceName + " hatype=" + hatype() + " HOST hwaddr=" + hwaddr_sb + "]";
    }

    // public String packet_inspect() {
    //   StringBuffer hwaddr_sb = new StringBuffer();
    //   String sep = "";
    //   byte[] hwaddr_ba = hwaddr();
    //   if (hwaddr_ba != null && hwaddr_ba.length > 0) {
    //     for (byte b: hwaddr_ba) {
    //       hwaddr_sb.append(sep);
    //       sep = ":";
    //       hwaddr_sb.append(String.format("%02x", b));
    //     }
    //   }
    //   return "PACKET[protocol=0 " + interfaceName + " hatype=" + hatype() + " HOST hwaddr=" + hwaddr_sb + "]";
    // }

    private int swapIntEndian(int i) {
      return ((i&0xff)<<24)+((i&0xff00)<<8)+((i&0xff0000)>>8)+((i>>24)&0xff);
    }

    private int swapShortEndian(short i) {
      return ((i&0xff)<<8)+((i&0xff00)>>8);
    }

    private String ipv6_ip() {
        if (getAddressFamily() != AF_INET6) return null;

        InetAddress in = getInetAddress();

        if (in.isLoopbackAddress()) return "::1";
        return SocketUtilsIPV6.getIPV6Address(in.getHostAddress());
    }

    private String inspectname() {
        if (socketAddress instanceof InetSocketAddress) {
            InetAddress address = getInetSocketAddress().getAddress();
            if (!address.toString().startsWith("/")) { // contains hostname
                return address.getHostName();
            }
        }
        return null;
    }

    private static InetAddress getRubyInetAddress(IRubyObject node) {
        try {
            if (node instanceof RubyInteger) {
                byte[] bytes;
                if (node instanceof RubyBignum) {
                    // IP6 addresses will be 16 bytes wide
                    bytes = ((RubyBignum) node).getBigIntegerValue().toByteArray();
                } else {
                    long i = node.convertToInteger().getIntValue() & 0xFFFFL;

                    bytes = new byte[]{
                            (byte) ((i >> 24) & 0xFF),
                            (byte) ((i >> 16) & 0xFF),
                            (byte) ((i >> 8) & 0xFF),
                            (byte) (i & 0xFF),
                    };
                }
                return SocketUtils.getRubyInetAddress(bytes);
            }
            return SocketUtils.getRubyInetAddress(node.convertToString().toString());
        } catch (UnknownHostException uhe) {
            return null;
        }
    }

    private static InetAddress getRubyInetAddress(String node) {
        try {
            return SocketUtils.getRubyInetAddress(node);
        } catch (UnknownHostException uhe) {
            return null;
        }
    }

    private static InetAddress getRubyInetAddress(String hostname, String node) {
        try {
            return SocketUtils.getRubyInetAddress(hostname, node);
        } catch (UnknownHostException uhe) {
            return null;
        }
    }

    @JRubyMethod
    public IRubyObject getnameinfo(ThreadContext context) {
        return getnameinfo(context, 0);
    }

    @JRubyMethod
    public IRubyObject getnameinfo(ThreadContext context, IRubyObject flags) {
        return getnameinfo(context, flags.convertToInteger().getIntValue());
    }

    public IRubyObject getnameinfo(ThreadContext context, int flags) {
        boolean unix = socketType == SocketType.UNIX;
        RubyString hostname = unix ?
                (RubyString) SocketUtils.gethostname(context) :
                newString(context, getInetSocketAddress().getHostName());

        String serviceName;
        if ((flags & NameInfo.NI_NUMERICSERV.intValue()) != 0) {
            serviceName = Integer.toString(getPort());
        } else if (unix) {
            serviceName = getUnixSocketAddress().path();
        } else {
            serviceName = Service.getServiceByPort(getPort(), protocol.getName()).getName();
        }

        return newArray(context, hostname, newString(context, serviceName));
    }


    @JRubyMethod(notImplemented = true)
    public IRubyObject marshal_dump(ThreadContext context) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject marshal_load(ThreadContext context, IRubyObject arg) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject to_str(ThreadContext context){
        return newString(context, toString());
    }

    public Inet6Address getInet6Address() {
        InetSocketAddress in = getInetSocketAddress();
        if (in != null && in.getAddress() instanceof Inet6Address) {
            return (Inet6Address) in.getAddress();
        }
        return null;
    }

    public Inet4Address getInet4Address() {
        InetSocketAddress in = getInetSocketAddress();
        if (in != null && in.getAddress() instanceof Inet4Address) {
            return (Inet4Address) in.getAddress();
        }
        return null;
    }

    public InetAddress getInetAddress() {
        if (socketAddress instanceof InetSocketAddress) {
            return ((InetSocketAddress) socketAddress).getAddress();
        }
        return null;
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    public InetSocketAddress getInetSocketAddress() {
        if (socketAddress instanceof InetSocketAddress) {
            return (InetSocketAddress) socketAddress;
        }
        return null;
    }

    public UnixSocketAddress getUnixSocketAddress() {
        if (socketAddress instanceof UnixSocketAddress) {
            return (UnixSocketAddress) socketAddress;
        }
        return null;
    }

    public String toString(){
        return socketAddress.toString();
    }

    AddressFamily getAddressFamily() {
        if (socketAddress instanceof InetSocketAddress) {
            if (!looksLikeV4ButIsV6 && getInetAddress() instanceof Inet4Address) return AF_INET;
            return AF_INET6;
        } else if (socketAddress instanceof UnixSocketAddress) {
            return AF_UNIX;
        }
        return AF_UNSPEC;
    }

    private SocketAddress socketAddress;
    private ProtocolFamily pfamily = PF_UNSPEC;
    private Sock sock;
    private SocketType socketType;
    private String interfaceName;
    private boolean interfaceLink;
    private NetworkInterface networkInterface;
    private boolean isBroadcast;
    private boolean displaysCanonical;
    private Protocol protocol = Protocol.getProtocolByNumber(0);

    // InetAddress can choose Inet4Address for ipv6 addrs which happen to be compatible with ipv4.
    // We make sure we don't lose what we specified via Ruby with this field.
    private boolean looksLikeV4ButIsV6;
}
