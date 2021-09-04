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
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyInteger;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;
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
import static jnr.constants.platform.ProtocolFamily.PF_INET;
import static jnr.constants.platform.ProtocolFamily.PF_INET6;
import static jnr.constants.platform.ProtocolFamily.PF_UNIX;
import static jnr.constants.platform.ProtocolFamily.PF_UNSPEC;

public class Addrinfo extends RubyObject {

    // TODO: (gf) these constants should be in their respective .h files
    final short ARPHRD_ETHER    =   1;  // ethernet hatype (if_arp.h)
    final short ARPHRD_LOOPBACK	= 772;	// loopback hatype (if_arp.h)
    final short AF_PACKET       =  17;  // packet socket (socket.h)
    final byte  PACKET_HOST     =   0;  // host packet type (if_packet.h)

    public static void createAddrinfo(Ruby runtime) {
        RubyClass addrinfo = runtime.defineClass(
                "Addrinfo",
                runtime.getData(),
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

    public Addrinfo(Ruby runtime, RubyClass cls, InetAddress inetAddress, int port) {
        super(runtime, cls);
        this.socketAddress = new InetSocketAddress(inetAddress, port);
        setSockAndProtocol(Sock.SOCK_STREAM);
    }

    public Addrinfo(Ruby runtime, RubyClass cls, SocketAddress socketAddress) {
        super(runtime, cls);
        this.socketAddress = socketAddress;
        this.pfamily = ProtocolFamily.valueOf(getAddressFamily().intValue());
        setSockAndProtocol(Sock.SOCK_STREAM);
    }

    public int getPort() {
        return socketAddress instanceof InetSocketAddress ? getInetSocketAddress().getPort() : -1;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject _sockaddr) {
        initializeCommon(context, _sockaddr, null, null, null);
        return context.nil;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject _sockaddr, IRubyObject _family) {
        initializeCommon(context, _sockaddr, _family, null, null);
        return context.nil;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject _sockaddr, IRubyObject _family, IRubyObject _socktype) {
        initializeCommon(context, _sockaddr, _family, _socktype, null);
        return context.nil;
    }

    @JRubyMethod(required = 1, optional = 4, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 1:
                return initialize(context, args[0]);
            case 2:
                return initialize(context, args[0], args[1]);
            case 3:
                return initialize(context, args[0], args[1], args[2]);
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
    public static final int AI_ADDRCONFIG = 0x00000400; /* only if any address is assigned */
    public static final int AI_V4MAPPED = 0x00000800; /* accept IPv4-mapped IPv6 address */
    /* special recommended flags for getipnodebyname */
    public static final int AI_DEFAULT = (AI_V4MAPPED_CFG | AI_ADDRCONFIG);

    private void initializeCommon(ThreadContext context, IRubyObject sockaddr, IRubyObject family, IRubyObject sock, IRubyObject port) {
        Ruby runtime = context.runtime;

        try {
            IRubyObject _sockaddrAry = TypeConverter.checkArrayType(context, sockaddr);

            if (_sockaddrAry != context.nil) {
                RubyArray sockaddAry = (RubyArray)_sockaddrAry;

                family = sockaddAry.eltOk(0).convertToString();
                AddressFamily af = SocketUtils.addressFamilyFromArg(family);
                ProtocolFamily pf = SocketUtils.protocolFamilyFromArg(family);

                if (af == AF_UNIX || pf == PF_UNIX) { /* ["AF_UNIX", "/tmp/sock"] */
                    IRubyObject path = sockaddAry.eltOk(1).convertToString();
                    socketAddress = new UnixSocketAddress(new File(path.toString()));
                    this.socketType = SocketType.UNIX;
                    this.sock = Sock.SOCK_STREAM;

                    return;
                }

                if (
                        af == AF_INET || pf == PF_INET       /* ["AF_INET", 46102, "localhost.localdomain", "127.0.0.1"] */
                                || af == AF_INET6 || pf == PF_INET6  /* ["AF_INET6", 42304, "ip6-localhost", "::1"] */
                        ) {

                    IRubyObject service = sockaddAry.eltOk(1).convertToInteger();
                    IRubyObject nodename = sockaddAry.eltOk(2);
                    IRubyObject numericnode = sockaddAry.eltOk(3);

                    InetAddress inetAddress = null;
                    int _port = service.convertToInteger().getIntValue();

                    if (!nodename.isNil()) inetAddress = getRubyInetAddress(nodename);
                    if (inetAddress == null) inetAddress = getRubyInetAddress(numericnode);

                    this.socketAddress = new InetSocketAddress(inetAddress, _port);
                    this.pfamily = pf;

                    // fall through below to finish setting up
                } else {
                    throw runtime.newRaiseException(runtime.getClass("SocketError"), "unknown address family: " + family.toString());
                }

            } else {

                InetAddress inetAddress;

                inetAddress = getRubyInetAddress(sockaddr);
                if (inetAddress == null) {
                    // try unpacking
                    // FIXME: inefficient to allow exception before trying unpack
                    this.socketAddress = Sockaddr.sockaddrFromBytes(runtime, sockaddr.convertToString().getBytes());
                }

                int _port;
                if (port != null) {
                    _port = SocketUtils.portToInt(port);
                } else {
                    _port = 0;
                }

                this.socketAddress = new InetSocketAddress(inetAddress, _port);

                //this.pfamily = SocketUtils.protocolFamilyFromArg(family);
                if (getInetAddress() instanceof Inet4Address) {
                    this.pfamily = PF_INET;
                } else {
                    this.pfamily = PF_INET6;
                }
            }

            this.socketType = SocketType.SOCKET;

            setSockAndProtocol(sock);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw runtime.newIOErrorFromException(ioe);
        }
    }

    private void setSockAndProtocol(IRubyObject sock) {
        setSockAndProtocol(sock == null ? null : SocketUtils.sockFromArg(sock));
    }

    private void setSockAndProtocol(Sock sock) {
        this.sock = sock;
        if (socketAddress instanceof InetSocketAddress) {
            if (this.sock == Sock.SOCK_STREAM) {
                protocol = Protocol.getProtocolByName("tcp");
            } else if (this.sock == Sock.SOCK_DGRAM) {
                protocol = Protocol.getProtocolByName("udp");
            }
        }
    }

    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        String base = "#<Addrinfo: %s>";
        String val;

        if (interfaceLink == true) {
            val = packet_inspect();
        }  else {
            val = inspect_sockaddr(context).toString();
        }

        return context.runtime.newString(String.format(base, val));
    }

    @JRubyMethod
    public IRubyObject inspect_sockaddr(ThreadContext context) {
        if (socketAddress instanceof UnixSocketAddress) {
            String path = getUnixSocketAddress().path();

            return context.runtime.newString(path.startsWith("/") ? path : "UNIX " + path);
        }

        int port = getInetSocketAddress().getPort();

        if (getInetAddress() instanceof Inet6Address) {
            String host = ipv6_ip();
            String hostPort = port == 0 ? host : "[" + host + "]:" + port;

            return context.runtime.newString(hostPort);
        }

        String portString = port == 0 ? "" : ":" + port;
        String host = getInetSocketAddress().getAddress().getHostAddress();

        return context.runtime.newString(host + portString);
    }

    @JRubyMethod(rest = true, meta = true)
    public static IRubyObject getaddrinfo(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyArray.newArray(context.runtime, SocketUtils.getaddrinfoList(context, args));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject ip(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        String host = StringSupport.checkEmbeddedNulls(context.runtime, arg.convertToString()).toString();
        try {
            InetAddress addy = SocketUtils.getRubyInetAddress(host);
            Addrinfo addrinfo = new Addrinfo(context.runtime, (RubyClass) recv, addy);
            addrinfo.protocol = Protocol.getProtocolByName("ip");
            addrinfo.pfamily = (addy instanceof Inet4Address) ? PF_INET : PF_INET6;

            return addrinfo;
        } catch (UnknownHostException uhe) {
            throw SocketUtils.sockerr(context.runtime, "host not found");
        }
    }

    @JRubyMethod(meta = true)
    public static IRubyObject tcp(ThreadContext context, IRubyObject recv, IRubyObject host, IRubyObject port) {
        Ruby runtime = context.runtime;

        Addrinfo addrinfo = new Addrinfo(runtime, (RubyClass) recv);
        addrinfo.initializeCommon(context, host, runtime.newFixnum(PF_UNSPEC.intValue()), runtime.newFixnum(Sock.SOCK_STREAM.intValue()), port);

        return addrinfo;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject udp(ThreadContext context, IRubyObject recv, IRubyObject host, IRubyObject port) {
        Ruby runtime = context.runtime;

        Addrinfo addrinfo = new Addrinfo(runtime, (RubyClass) recv);
        addrinfo.initializeCommon(context, host, runtime.newFixnum(PF_UNSPEC.intValue()), runtime.newFixnum(Sock.SOCK_DGRAM.intValue()), port);

        return addrinfo;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject unix(ThreadContext context, IRubyObject recv, IRubyObject path) {
        Addrinfo addrinfo = new Addrinfo(context.runtime, (RubyClass) recv);

        addrinfo.socketAddress = new UnixSocketAddress(new File(path.convertToString().toString()));
        addrinfo.sock = Sock.SOCK_STREAM;
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
        return context.runtime.newFixnum(getAddressFamily().intValue());
    }

    @JRubyMethod
    public IRubyObject pfamily(ThreadContext context) {
        return context.runtime.newFixnum(pfamily.intValue());
    }

    @JRubyMethod
    public IRubyObject socktype(ThreadContext context) {
      if (sock == null) {
        return context.runtime.newFixnum(0);
      }
      return context.runtime.newFixnum(sock.intValue());
    }

    @JRubyMethod
    public IRubyObject protocol(ThreadContext context) {
        return context.runtime.newFixnum(protocol.getProto());
    }

    @JRubyMethod
    public IRubyObject canonname(ThreadContext context) {
        if (socketAddress instanceof InetSocketAddress) {
            return context.runtime.newString(getInetSocketAddress().getAddress().getCanonicalHostName());
        } else if (socketAddress instanceof UnixSocketAddress) {
            return context.runtime.newString(getUnixSocketAddress().path());
        }
        throw context.runtime.newNotImplementedError("canonname not implemented for socket address: " + socketAddress);
    }

    @JRubyMethod(name = "ipv4?")
    public IRubyObject ipv4_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context, getAddressFamily() == AF_INET);
    }

    @JRubyMethod(name = "ipv6?")
    public IRubyObject ipv6_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context, getAddressFamily() == AF_INET6);
    }

    @JRubyMethod(name = "unix?")
    public IRubyObject unix_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context, getAddressFamily() == AF_UNIX);
    }

    @JRubyMethod(name = "ip?")
    public IRubyObject ip_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context, getAddressFamily() == AF_INET || getAddressFamily() == AF_INET6);
    }

    @JRubyMethod
    public IRubyObject ip_unpack(ThreadContext context) {
        RubyArray ary = RubyArray.newArray(context.runtime, 2);
        ary.append(ip_address(context));
        ary.append(ip_port(context));
        return ary;
    }

    @JRubyMethod
    public IRubyObject ip_address(ThreadContext context) {
        if (getAddressFamily() != AF_INET && getAddressFamily() != AF_INET6) {
            throw SocketUtils.sockerr(context.runtime, "need IPv4 or IPv6 address");
        }
        // TODO: (gf) for IPv6 link-local address this appends a numeric interface index (like MS-Windows), should append interface name on Linux
        String fullHost = ((InetSocketAddress) socketAddress).getAddress().getHostAddress();
        String host = (getAddressFamily() == AF_INET6) ? ipv6_ip() : fullHost;

        return context.runtime.newString(host);
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject ip_port(ThreadContext context) {
        if (getAddressFamily() != AF_INET && getAddressFamily() != AF_INET6) {
            throw SocketUtils.sockerr(context.runtime, "need IPv4 or IPv6 address");
        }
        return context.runtime.newFixnum(((InetSocketAddress) socketAddress).getPort());
    }

    @JRubyMethod(name = "ipv4_private?")
    public IRubyObject ipv4_private_p(ThreadContext context) {
        if (getAddressFamily() == AF_INET) {
            return RubyBoolean.newBoolean(context, getInet4Address().isSiteLocalAddress());
        }
        return RubyBoolean.newBoolean(context, false);
    }

    @JRubyMethod(name = "ipv4_loopback?")
    public IRubyObject ipv4_loopback_p(ThreadContext context) {
      if (getAddressFamily() == AF_INET) {
        return RubyBoolean.newBoolean(context, ((InetSocketAddress) socketAddress).getAddress().isLoopbackAddress());
      }
      return RubyBoolean.newBoolean(context, false);
    }

    @JRubyMethod(name = "ipv4_multicast?")
    public IRubyObject ipv4_multicast_p(ThreadContext context) {
        if (getAddressFamily() == AF_INET) {
            return RubyBoolean.newBoolean(context, getInet4Address().isMulticastAddress());
        }
        return context.fals;
    }

    @JRubyMethod(name = "ipv6_unspecified?")
    public IRubyObject ipv6_unspecified_p(ThreadContext context) {
        if (getAddressFamily() == AF_INET6) {
            return RubyBoolean.newBoolean(context, getInet6Address().getHostAddress().equals("::"));
        }
        return context.fals;
    }

    @JRubyMethod(name = "ipv6_loopback?")
    public IRubyObject ipv6_loopback_p(ThreadContext context) {
      if (getAddressFamily() == AF_INET6) {
        return RubyBoolean.newBoolean(context, getInetSocketAddress().getAddress().isLoopbackAddress());
      }
      return RubyBoolean.newBoolean(context, false);
    }

    @JRubyMethod(name = "ipv6_multicast?")
    public IRubyObject ipv6_multicast_p(ThreadContext context) {
        if (getAddressFamily() == AF_INET6) {
            return RubyBoolean.newBoolean(context, getInet6Address().isMulticastAddress());
        }
        return context.fals;
    }

    @JRubyMethod(name = "ipv6_linklocal?")
    public IRubyObject ipv6_linklocal_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context, getInetSocketAddress().getAddress().isLinkLocalAddress());
    }

    @JRubyMethod(name = "ipv6_sitelocal?")
    public IRubyObject ipv6_sitelocal_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context, ((InetSocketAddress) socketAddress).getAddress().isSiteLocalAddress());
    }

    @JRubyMethod(name = "ipv6_v4mapped?")
    public IRubyObject ipv6_v4mapped_p(ThreadContext context) {
        Inet6Address in6 = getInet6Address();
        return RubyBoolean.newBoolean(context, in6 != null &&
                // Java always converts mapped ipv6 addresses to ipv4 form
                in6.getHostAddress().indexOf(":") == -1);
    }

    @JRubyMethod(name = "ipv6_v4compat?")
    public IRubyObject ipv6_v4compat_p(ThreadContext context) {
        Inet6Address in6 = getInet6Address();
        return RubyBoolean.newBoolean(context, in6 != null && in6.isIPv4CompatibleAddress());
    }

    @JRubyMethod(name = "ipv6_mc_nodelocal?")
    public IRubyObject ipv6_mc_nodelocal_p(ThreadContext context) {
        Inet6Address in6 = getInet6Address();
        return RubyBoolean.newBoolean(context, in6 != null && in6.isMCNodeLocal());
    }

    @JRubyMethod(name = "ipv6_mc_linklocal?")
    public IRubyObject ipv6_mc_linklocal_p(ThreadContext context) {
        Inet6Address in6 = getInet6Address();
        return RubyBoolean.newBoolean(context, in6 != null && in6.isMCLinkLocal());
    }

    @JRubyMethod(name = "ipv6_mc_sitelocal?")
    public IRubyObject ipv6_mc_sitelocal_p(ThreadContext context) {
        Inet6Address in6 = getInet6Address();
        return RubyBoolean.newBoolean(context, in6 != null && in6.isMCSiteLocal());
    }

    @JRubyMethod(name = "ipv6_mc_orglocal?")
    public IRubyObject ipv6_mc_orglocal_p(ThreadContext context) {
        Inet6Address in6 = getInet6Address();
        return RubyBoolean.newBoolean(context, in6 != null && in6.isMCOrgLocal());
    }

    @JRubyMethod(name = "ipv6_mc_global?")
    public IRubyObject ipv6_mc_global_p(ThreadContext context) {
        Inet6Address in6 = getInet6Address();
        return RubyBoolean.newBoolean(context, in6 != null && in6.isMCGlobal());
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject ipv6_to_ipv4(ThreadContext context) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject unix_path(ThreadContext context) {
        if (getAddressFamily() != AF_UNIX) {
            throw SocketUtils.sockerr(context.runtime, "need AF_UNIX address");
        }
        return context.runtime.newString(getUnixSocketAddress().path());
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
                    throw SocketUtils.sockerr(context.runtime, "to_sockaddr: " + e.getMessage());
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

    @JRubyMethod(optional = 1)
    public IRubyObject getnameinfo(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        RubyString hostname;

        InetSocketAddress inet = getInetSocketAddress();
        if (inet != null) {
            hostname = runtime.newString(inet.getHostName());
        } else {
            UnixSocketAddress unix = getUnixSocketAddress();
            hostname = runtime.newString(unix.path());
        }

        RubyString rubyService = null;

        if (args.length > 0) {
            int flags = args[0].convertToInteger().getIntValue();
            if ((flags & NameInfo.NI_NUMERICSERV.intValue()) != 0) {
                rubyService = runtime.newString(Integer.toString(getPort()));
            }
        }

        if (rubyService == null) {
            Service service = Service.getServiceByPort(getPort(), protocol.getName());
            rubyService = runtime.newString(service.getName());
        }

        return runtime.newArray(hostname, rubyService);
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
        return context.runtime.newString(toString());
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
            if (getInetAddress() instanceof Inet4Address) return AF_INET;
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
    private Protocol protocol = Protocol.getProtocolByNumber(0);
}
