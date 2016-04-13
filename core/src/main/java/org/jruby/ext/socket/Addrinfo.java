package org.jruby.ext.socket;

import jnr.constants.platform.AddressFamily;
import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Sock;
import jnr.netdb.Protocol;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Visibility;
import org.jruby.util.ByteList;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.net.SocketException;
import org.jruby.exceptions.RaiseException;

import org.jruby.util.io.Sockaddr;

public class Addrinfo extends RubyObject {

    // TODO: (gf) these constants should be in their respective .h files
    final short ARPHRD_ETHER    =   1;  // ethernet hatype (if_arp.h)
    final short ARPHRD_LOOPBACK	= 772;	// loopback hatype (if_arp.h) 
    final short AF_PACKET       =  17;  // packet socket (socket.h)
    final byte  PACKET_HOST     =   0;  // host packet type (if_packet.h)

    public static void createAddrinfo(Ruby runtime) {
        RubyClass addrinfo = runtime.defineClass(
                "Addrinfo",
                runtime.getClass("Data"),
                new ObjectAllocator() {
                    public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                        return new Addrinfo(runtime, klazz);
                    }
                });

        addrinfo.defineAnnotatedMethods(Addrinfo.class);
    }

    public Addrinfo(Ruby runtime, RubyClass cls) {
        super(runtime, cls);
    }

    public Addrinfo(Ruby runtime, RubyClass cls, NetworkInterface networkInterface, InetAddress inetAddress) {
        super(runtime, cls);
        this.networkInterface = networkInterface;
        this.interfaceLink = false;
        this.inetAddress = inetAddress;
        this.interfaceName = networkInterface.getName();
        this.pfamily = inetAddress instanceof Inet4Address ? ProtocolFamily.PF_INET : ProtocolFamily.PF_INET6;
        this.afamily = inetAddress instanceof Inet4Address ? AddressFamily.AF_INET : AddressFamily.AF_INET6;
        this.socketType = SocketType.SOCKET;
    }

    public Addrinfo(Ruby runtime, RubyClass cls, NetworkInterface networkInterface, boolean isBroadcast) {
        super(runtime, cls);
        this.networkInterface = networkInterface;
        this.interfaceLink = true;
        this.isBroadcast = isBroadcast;
        this.interfaceName = networkInterface.getName();
        this.afamily = AddressFamily.AF_UNSPEC;  // TODO: (gf) should be AF_PACKET (17) when available
        this.pfamily = ProtocolFamily.PF_UNSPEC; // TODO: (gf) should be PF_PACKET (17) when available
        this.socketType = SocketType.SOCKET;
    }

    public Addrinfo(Ruby runtime, RubyClass cls, InetAddress inetAddress) {
        super(runtime, cls);
        this.inetAddress = inetAddress;
        this.pfamily = inetAddress instanceof Inet4Address ? ProtocolFamily.PF_INET : ProtocolFamily.PF_INET6;
        this.afamily = inetAddress instanceof Inet4Address ? AddressFamily.AF_INET : AddressFamily.AF_INET6;
        this.socketType = SocketType.SOCKET;
    }

    public Addrinfo(Ruby runtime, RubyClass cls, InetAddress inetAddress, int port) {
        super(runtime, cls);
        this.inetAddress = inetAddress;
        this.port = port;
        this.sock = Sock.SOCK_STREAM;
        this.pfamily = inetAddress instanceof Inet4Address ? ProtocolFamily.PF_INET : ProtocolFamily.PF_INET6;
        this.afamily = inetAddress instanceof Inet4Address ? AddressFamily.AF_INET : AddressFamily.AF_INET6;
        this.socketType = SocketType.SOCKET;
    }

    public Addrinfo(Ruby runtime, RubyClass cls, InetAddress inetAddress, int port, SocketType socketType) {
        super(runtime, cls);
        this.inetAddress = inetAddress;
        this.port = port;
        this.socketType = socketType;
        this.sock = Sock.SOCK_STREAM;
        this.pfamily = inetAddress instanceof Inet4Address ? ProtocolFamily.PF_INET : ProtocolFamily.PF_INET6;
        this.afamily = inetAddress instanceof Inet4Address ? AddressFamily.AF_INET : AddressFamily.AF_INET6;
        this.socketType = SocketType.SOCKET;
    }

    public InetAddress getInetAddress() {
        return inetAddress;
    }

    public int getPort() {
        return port;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject _sockaddr) {
        initializeCommon(context.runtime, _sockaddr, null, null, null);
        return context.nil;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject _sockaddr, IRubyObject _family) {
        initializeCommon(context.runtime, _sockaddr, _family, null, null);
        return context.nil;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject _sockaddr, IRubyObject _family, IRubyObject _socktype) {
        initializeCommon(context.runtime, _sockaddr, _family, _socktype, null);
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

        initializeCommon(context.runtime, _sockaddr, _family, _socktype, _protocol);

        return context.nil;
    }

    private void initializeCommon(Ruby runtime, IRubyObject sockaddr, IRubyObject family, IRubyObject sock, IRubyObject port) {
        try {
            inetAddress = SocketUtils.getRubyInetAddress(sockaddr.convertToString().getByteList());

            if (family == null) {
                this.pfamily = inetAddress instanceof Inet4Address ? ProtocolFamily.PF_INET : ProtocolFamily.PF_INET6;
                this.afamily = inetAddress instanceof Inet4Address ? AddressFamily.AF_INET : AddressFamily.AF_INET6;
                this.socketType = SocketType.SOCKET;
            } else {
                this.pfamily = SocketUtils.protocolFamilyFromArg(family);

                if (this.pfamily == ProtocolFamily.__UNKNOWN_CONSTANT__) {
                    throw runtime.newErrnoENOPROTOOPTError();
                }

                this.afamily = SocketUtils.addressFamilyFromArg(family);

                if (this.afamily == AddressFamily.__UNKNOWN_CONSTANT__) {
                    throw runtime.newErrnoENOPROTOOPTError();
                }

                this.socketType = SocketType.SOCKET;
            }

            if (sock == null) {
                this.sock = Sock.SOCK_STREAM;
            } else {
                this.sock = SocketUtils.sockFromArg(sock);
            }

            if (port == null) {
                this.port = 0;
            } else {
                this.port = (int) port.convertToInteger().getLongValue();
            }

            protocol = Protocol.getProtocolByName("tcp");
        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }
    }

    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        if (interfaceLink == true) {
            return context.runtime.newString("#<Addrinfo: " + packet_inspect() + ">");
        } else {
            // TODO: MRI also shows hostname, but we don't want to reverse every time...
            String portString = port == 0 ? "" : ":" + port;
            return context.runtime.newString("#<Addrinfo: " + inetAddress.getHostAddress() + portString + ">");
        }
    }

    @JRubyMethod
    public IRubyObject inspect_sockaddr(ThreadContext context) {
        String portString = port == 0 ? "" : ":" + port;
        return context.runtime.newString(inetAddress.getHostAddress() + portString);
    }

    @JRubyMethod(rest = true, meta = true)
    public static IRubyObject getaddrinfo(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyArray.newArray(context.runtime, SocketUtils.getaddrinfoList(context, args));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject ip(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        String host = arg.convertToString().toString();
        try {
            InetAddress addy = InetAddress.getByName(host);
            return new Addrinfo(context.runtime, (RubyClass) recv, addy);
        } catch (UnknownHostException uhe) {
            throw SocketUtils.sockerr(context.runtime, "host not found");
        }
    }

    @JRubyMethod(meta = true)
    public static IRubyObject tcp(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        Addrinfo addrinfo = new Addrinfo(context.runtime, (RubyClass) recv);
        addrinfo.initializeCommon(context.runtime, arg0, null, null, arg1);
        return addrinfo;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject udp(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        return ((RubyClass) recv).newInstance(context, arg0, arg1, Block.NULL_BLOCK);
    }

    @JRubyMethod(rest = true, meta = true, notImplemented = true)
    public static IRubyObject unix(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return ((RubyClass) recv).newInstance(context, args, Block.NULL_BLOCK);
    }

    @JRubyMethod
    public IRubyObject afamily(ThreadContext context) {
        return context.runtime.newFixnum(afamily.intValue());
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
        return context.runtime.newString(inetAddress.getCanonicalHostName());
    }

    @JRubyMethod(name = "ipv4?")
    public IRubyObject ipv4_p(ThreadContext context) {
        return context.runtime.newBoolean(pfamily == ProtocolFamily.PF_INET);
    }

    @JRubyMethod(name = "ipv6?")
    public IRubyObject ipv6_p(ThreadContext context) {
        return context.runtime.newBoolean(pfamily == ProtocolFamily.PF_INET6);
    }

    @JRubyMethod(name = "unix?")
    public IRubyObject unix_p(ThreadContext context) {
        return context.runtime.newBoolean(pfamily == ProtocolFamily.PF_UNIX);
    }

    @JRubyMethod(name = "ip?", notImplemented = true)
    public IRubyObject ip_p(ThreadContext context) {
        return context.runtime.newBoolean(pfamily == ProtocolFamily.PF_INET || pfamily == ProtocolFamily.PF_INET6);
    }

    @JRubyMethod
    public IRubyObject ip_unpack(ThreadContext context) {
        RubyArray ary = RubyArray.newArray(context.runtime, 2);
        ary.append(ip_address(context));
        ary.append(ip_port(context));
        return ary;
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject ip_address(ThreadContext context) {
        if (interfaceLink == true) {
            throw SocketUtils.sockerr(context.runtime, "need IPv4 or IPv6 address");
        }
        // TODO: (gf) for IPv6 link-local address this appends a numeric interface index (like MS-Windows), should append interface name on Linux 
        return context.runtime.newString(inetAddress.getHostAddress());
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject ip_port(ThreadContext context) {
        if (interfaceLink ==  true) {
             throw SocketUtils.sockerr(context.runtime, "need IPv4 or IPv6 address");
       }
        return context.runtime.newFixnum(port);
    }

    @JRubyMethod(name = "ipv4_private?", notImplemented = true)
    public IRubyObject ipv4_private_p(ThreadContext context) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod(name = "ipv4_loopback?")
    public IRubyObject ipv4_loopback_p(ThreadContext context) {
      if (afamily == AddressFamily.AF_INET) {
        return context.runtime.newBoolean(inetAddress.isLoopbackAddress());
      }
      return context.runtime.newBoolean(false);
    }

    @JRubyMethod(name = "ipv4_multicast?")
    public IRubyObject ipv4_multicast_p(ThreadContext context) {
        return context.runtime.newBoolean(inetAddress.isMulticastAddress());
    }

    @JRubyMethod(name = "ipv6_unspecified?", notImplemented = true)
    public IRubyObject ipv6_unspecified_p(ThreadContext context) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod(name = "ipv6_loopback?")
    public IRubyObject ipv6_loopback_p(ThreadContext context) {
      if (afamily == AddressFamily.AF_INET6) {
        return context.runtime.newBoolean(inetAddress.isLoopbackAddress());
      }
      return context.runtime.newBoolean(false);
    }

    @JRubyMethod(name = "ipv6_multicast?")
    public IRubyObject ipv6_multicast_p(ThreadContext context) {
        return context.runtime.newBoolean(inetAddress.isMulticastAddress());
    }

    @JRubyMethod(name = "ipv6_linklocal?")
    public IRubyObject ipv6_linklocal_p(ThreadContext context) {
        return context.runtime.newBoolean(inetAddress.isLinkLocalAddress());
    }

    @JRubyMethod(name = "ipv6_sitelocal?")
    public IRubyObject ipv6_sitelocal_p(ThreadContext context) {
        return context.runtime.newBoolean(inetAddress.isSiteLocalAddress());
    }

    @JRubyMethod(name = "ipv6_v4mapped?", notImplemented = true)
    public IRubyObject ipv6_v4mapped_p(ThreadContext context) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod(name = "ipv6_v4compat?")
    public IRubyObject ipv6_v4compat_p(ThreadContext context) {
        if (!(inetAddress instanceof Inet6Address)) {
            return context.runtime.getFalse();
        }
        return context.runtime.newBoolean(((Inet6Address) inetAddress).isIPv4CompatibleAddress());
    }

    @JRubyMethod(name = "ipv6_mc_nodelocal?")
    public IRubyObject ipv6_mc_nodelocal_p(ThreadContext context) {
        return context.runtime.newBoolean(inetAddress.isMCNodeLocal());
    }

    @JRubyMethod(name = "ipv6_mc_linklocal?")
    public IRubyObject ipv6_mc_linklocal_p(ThreadContext context) {
        return context.runtime.newBoolean(inetAddress.isMCLinkLocal());
    }

    @JRubyMethod(name = "ipv6_mc_sitelocal?")
    public IRubyObject ipv6_mc_sitelocal_p(ThreadContext context) {
        return context.runtime.newBoolean(inetAddress.isMCSiteLocal());
    }

    @JRubyMethod(name = "ipv6_mc_orglocal?")
    public IRubyObject ipv6_mc_orglocal_p(ThreadContext context) {
        return context.runtime.newBoolean(inetAddress.isMCOrgLocal());
    }

    @JRubyMethod(name = "ipv6_mc_global?")
    public IRubyObject ipv6_mc_global_p(ThreadContext context) {
        return context.runtime.newBoolean(inetAddress.isMCGlobal());
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject ipv6_to_ipv4(ThreadContext context) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject unix_path(ThreadContext context) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod(name = {"to_sockaddr", "to_s"})
    public IRubyObject to_sockaddr(ThreadContext context) {
      if (afamily == AddressFamily.AF_INET || afamily == AddressFamily.AF_INET6) {
        return Sockaddr.pack_sockaddr_in(context,port,inetAddress.getHostAddress());
      }
      if (afamily == AddressFamily.AF_UNSPEC) {
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
    
    private static RuntimeException sockerr(Ruby runtime, String msg) {
        return new RaiseException(runtime, runtime.getClass("SocketError"), msg, true);
    }
    
    @JRubyMethod(rest = true, notImplemented = true)
    public IRubyObject getnameinfo(ThreadContext context, IRubyObject[] args) {
        // unimplemented
        return context.nil;
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
    
    public String toString(){  
        return inetAddress.getHostAddress() + ":" + port;
    }

    private InetAddress inetAddress;
    private int port;
    private ProtocolFamily pfamily;
    private AddressFamily afamily;
    private Sock sock;
    private SocketType socketType;
    private String interfaceName;
    private boolean interfaceLink;
    private NetworkInterface networkInterface;
    private boolean isBroadcast;
    private Protocol protocol = Protocol.getProtocolByNumber(0);
}
