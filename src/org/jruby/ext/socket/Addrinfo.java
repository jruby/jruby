package org.jruby.ext.socket;

import jnr.constants.platform.AddressFamily;
import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Sock;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Addrinfo extends RubyObject {
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

    public Addrinfo(Ruby runtime, RubyClass cls, InetAddress inetAddress) {
        super(runtime, cls);
        this.inetAddress = inetAddress;

        this.sock = Sock.SOCK_STREAM;
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

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject _sockaddr) {
        initializeCommon(context.runtime, _sockaddr, null, null, null);

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject _sockaddr, IRubyObject _family) {
        initializeCommon(context.runtime, _sockaddr, _family, null, null);

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject _sockaddr, IRubyObject _family, IRubyObject _socktype) {
        initializeCommon(context.runtime, _sockaddr, _family, _socktype, null);

        return context.nil;
    }

    @JRubyMethod(required = 1, optional = 4)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 1: return initialize(context, args[0]);
            case 2: return initialize(context, args[0], args[1]);
            case 3: return initialize(context, args[0], args[1], args[2]);
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
                this.port = (int)port.convertToInteger().getLongValue();
            }
        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }
    }

    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        // TODO: MRI also shows hostname, but we don't want to reverse every time...
        String portString = port == 0 ? "" : ":" + port;
        return context.runtime.newString("#<Addrinfo: " + inetAddress.getHostAddress() +  portString + ">");
    }

    @JRubyMethod
    public IRubyObject inspect_sockaddr(ThreadContext context) {
        String portString = port == 0 ? "" : ":" + port;
        return context.runtime.newString(inetAddress.getHostAddress() + portString);
    }

    @JRubyMethod(rest = true, meta = true)
    public static IRubyObject getaddrinfo(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject ip(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        String host = arg.convertToString().toString();

        try {
            InetAddress addy = InetAddress.getByName(host);
            return new Addrinfo(context.runtime, (RubyClass)recv, addy);
        } catch (UnknownHostException uhe) {
            throw SocketUtils.sockerr(context.runtime, "host not found");
        }
    }

    @JRubyMethod(meta = true)
    public static IRubyObject tcp(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        Addrinfo addrinfo = new Addrinfo(context.runtime, (RubyClass)recv);

        addrinfo.initializeCommon(context.runtime, arg0, null, null, arg1);

        return addrinfo;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject udp(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        return ((RubyClass)recv).newInstance(context, arg0, arg1, Block.NULL_BLOCK);
    }

    @JRubyMethod(rest = true, meta = true, notImplemented = true)
    public static IRubyObject unix(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return ((RubyClass)recv).newInstance(context, args, Block.NULL_BLOCK);
    }

    @JRubyMethod
    public IRubyObject afamily(ThreadContext context) {
        return context.runtime.newFixnum(pfamily.intValue());
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject pfamily(ThreadContext context) {
        return context.runtime.newFixnum(afamily.intValue());
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject socktype(ThreadContext context) {
        return context.runtime.newFixnum(sock.intValue());
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject protocol(ThreadContext context) {
        return context.runtime.newFixnum(port);
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
        byte[] bytes = inetAddress.getAddress();
        RubyArray ary = RubyArray.newArray(context.runtime, bytes.length);
        for (byte bite : bytes) ary.append(context.runtime.newFixnum(bite));
        return ary;
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject ip_address(ThreadContext context) {
        return context.runtime.newString(inetAddress.getHostAddress());
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject ip_port(ThreadContext context) {
        return context.runtime.newFixnum(port);
    }

    @JRubyMethod(name = "ipv4_private?", notImplemented = true)
    public IRubyObject ipv4_private_p(ThreadContext context) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod(name = "ipv4_loopback?")
    public IRubyObject ipv4_loopback_p(ThreadContext context) {
        return context.runtime.newBoolean(inetAddress.isLoopbackAddress());
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
        return context.runtime.newBoolean(inetAddress.isLoopbackAddress());
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
        if (!(inetAddress instanceof Inet6Address)) return context.runtime.getFalse();
        return context.runtime.newBoolean(((Inet6Address)inetAddress).isIPv4CompatibleAddress());
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

    @JRubyMethod(name = {"to_sockaddr", "to_s"}, notImplemented = true)
    public IRubyObject to_sockaddr(ThreadContext context) {
        // unimplemented
        return context.nil;
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

    private InetAddress inetAddress;
    private int port;
    private ProtocolFamily pfamily;
    private AddressFamily afamily;
    private Sock sock;
    private SocketType socketType;
}
