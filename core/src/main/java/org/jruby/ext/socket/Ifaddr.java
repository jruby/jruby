package org.jruby.ext.socket;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author Lucas Allan Amorim
 */
public class Ifaddr extends RubyObject {

    String name;
    Boolean isUp;
    Boolean isLoopback;
    Boolean isPointToPoint;
    Boolean isVirtual;
    Boolean isMulticast;
    byte[] hardwareAddress;
    InetAddress address;
    InetAddress broadcast;
    short networkPrefixLength;

    public static void createIfaddr(Ruby runtime) {
        RubyClass ifaddr = runtime.defineClass(
                "Socket::Ifaddr",
                runtime.getClass("Data"),
                new ObjectAllocator() {
                    public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                        return new Ifaddr(runtime, klazz);
                    }
                });
        ifaddr.defineAnnotatedMethods(Ifaddr.class);
    }

    public Ifaddr(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public Ifaddr(Ruby runtime, RubyClass metaClass, NetworkInterface ni, InterfaceAddress it) {
        super(runtime, metaClass);
        try {
            isUp = ni.isUp();
            name = ni.getDisplayName();
            isLoopback = ni.isLoopback();
            isPointToPoint = ni.isPointToPoint();
            isVirtual = ni.isVirtual();
            isMulticast = ni.supportsMulticast();
            hardwareAddress = ni.getHardwareAddress();

            address = it.getAddress();
            broadcast = it.getBroadcast();
            networkPrefixLength = it.getNetworkPrefixLength();
        } catch (SocketException ex) {
            Logger.getLogger(Ifaddr.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        return context.runtime.newString("#<Socket::Ifaddr: " + name + status() + interfaceType() + ">");
    }

    @JRubyMethod
    public IRubyObject name(ThreadContext context) {
        return context.runtime.newString(name);
    }

    @JRubyMethod
    public IRubyObject addr(ThreadContext context) {
        return new Addrinfo(context.runtime, context.runtime.getClass("Addrinfo"), address);
    }

    @JRubyMethod
    public IRubyObject broadaddr(ThreadContext context) {
        return new Addrinfo(context.runtime, context.runtime.getClass("Addrinfo"), broadcast);
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject ifindex(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject flags(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject netmask(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject dstaddr(ThreadContext context) {
        return context.nil;
    }

    private String interfaceType() {
        if (isLoopback) {
            return ",LOOPBACK";
        } else if (isPointToPoint) {
            return ",POINTOPOINT";
        } else {
            return "";
        }
    }

    private String status() {
        if (isUp) {
            return ",UP";
        } else {
            return ",DOWN";
        }

    }
}