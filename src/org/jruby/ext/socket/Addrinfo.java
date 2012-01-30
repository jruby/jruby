package org.jruby.ext.socket;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

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
    }

    @JRubyMethod(rest = true, notImplemented = true)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject inspect(ThreadContext context) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject inspect_sockaddr(ThreadContext context) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod(rest = true)
    public static IRubyObject getaddrinfo(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod
    public static IRubyObject ip(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        String host = arg.convertToString().toString();

        try {
            InetAddress addy = InetAddress.getByName(host);
            return new Addrinfo(context.runtime, (RubyClass)recv, addy);
        } catch (UnknownHostException uhe) {
            throw RubySocket.sockerr(context.runtime, "host not found");
        }
    }

    @JRubyMethod(notImplemented = true)
    public static IRubyObject tcp(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod(notImplemented = true)
    public static IRubyObject udp(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod(rest = true, notImplemented = true)
    public static IRubyObject unix(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject afamily(ThreadContext context) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject pfamily(ThreadContext context) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject socktype(ThreadContext context) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject protocol(ThreadContext context) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject canonname(ThreadContext context) {
        return context.runtime.newString(inetAddress.getCanonicalHostName());
    }

    @JRubyMethod(name = "ipv4?")
    public IRubyObject ipv4_p(ThreadContext context) {
        return context.runtime.newBoolean(inetAddress instanceof Inet4Address);
    }

    @JRubyMethod(name = "ipv6?")
    public IRubyObject ipv6_p(ThreadContext context) {
        return context.runtime.newBoolean(inetAddress instanceof Inet6Address);
    }

    @JRubyMethod(name = "unix?", notImplemented = true)
    public IRubyObject unix_p(ThreadContext context) {
        // unimplemented
        return context.nil;
    }

    @JRubyMethod(name = "ip?", notImplemented = true)
    public IRubyObject ip_p(ThreadContext context) {
        // unimplemented
        return context.nil;
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
        // unimplemented
        return context.nil;
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
}
