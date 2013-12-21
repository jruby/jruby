package org.jruby.ext.socket;

import java.lang.reflect.Field;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
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

    private String name;
    private Boolean isUp;
    private Boolean isLoopback;
    private Boolean isPointToPoint;
    private InetAddress address;
    private InetAddress broadcast;
    private InterfaceAddress interfaceAddress;
    private String netmask;
    private int index;
    private String flagStatus;

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

    public Ifaddr(Ruby runtime, RubyClass metaClass, NetworkInterface ni, InterfaceAddress it) throws Exception {
        super(runtime, metaClass);
        isUp = ni.isUp();
        name = ni.getDisplayName();
        isLoopback = ni.isLoopback();
        isPointToPoint = ni.isPointToPoint();
        address = it.getAddress();
        broadcast = it.getBroadcast();
        interfaceAddress = it;

        setNetmask(it);
        setIndex(ni);
        setFlags(ni);

    }

    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        return context.runtime.newString("#<Socket::Ifaddr: " + name + " " + flagStatus + ">");
    }

    @JRubyMethod
    public IRubyObject name(ThreadContext context) {
        return context.runtime.newString(name);
    }

    @JRubyMethod
    public IRubyObject addr(ThreadContext context) {
        if (address == null) {
            return context.nil;
        }
        return new Addrinfo(context.runtime, context.runtime.getClass("Addrinfo"), address);
    }

    @JRubyMethod
    public IRubyObject broadaddr(ThreadContext context) {
        if (broadcast == null) {
            return context.nil;
        }
        return new Addrinfo(context.runtime, context.runtime.getClass("Addrinfo"), broadcast);
    }

    @JRubyMethod
    public IRubyObject ifindex(ThreadContext context) {
        return context.runtime.newFixnum(index);
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject flags(ThreadContext context) {
        throw SocketUtils.sockerr(context.runtime, "flags not implemented yet");
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject netmask(ThreadContext context) throws UnknownHostException {
        if (netmask == null) {
            return context.nil;
        }
        return new Addrinfo(context.runtime, context.runtime.getClass("Addrinfo"), InetAddress.getByName(netmask));
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject dstaddr(ThreadContext context) {
        throw SocketUtils.sockerr(context.runtime, "dstaddr not implemented yet");
    }

    private void setNetmask(InterfaceAddress it) throws Exception {
        if (it.getNetworkPrefixLength() != 0 && address instanceof Inet4Address) {
            String subnet = ipAddress() + "/" + it.getNetworkPrefixLength();
            SubnetUtils utils = new SubnetUtils(subnet);
            netmask = utils.getInfo().getNetmask();

        } else if ((it.getNetworkPrefixLength() != 0 && address instanceof Inet6Address)) {
            String subnet = ipAddress() + "/" + it.getNetworkPrefixLength();
        }
    }

    private void setIndex(NetworkInterface ni) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field field = ni.getClass().getDeclaredField("index");
        field.setAccessible(true);
        index = (Integer) field.get(ni);
    }

    private void setFlags(NetworkInterface nif) throws SocketException {
        flagStatus = nif.isUp() ? "UP" : "DOWN";
        if (nif.isLoopback()) {
            flagStatus += ",LOOPBACK";
        }
        if (nif.isPointToPoint()) {
            flagStatus += ",PTP";
        }
        if (nif.isVirtual()) {
            flagStatus += ",VIRTUAL";
        }
        if (nif.supportsMulticast()) {
            flagStatus += ",MULTICAST";
        }
        flagStatus += ",MTU=" + nif.getMTU();
        byte[] mac = nif.getHardwareAddress();
        if (mac != null) {
            flagStatus += ",HWADDR=";
            for (int i = 0; i < mac.length; ++i) {
                if (i > 0) {
                    flagStatus += ":";
                }
                flagStatus += String.format("%02x", mac[i]);
            }
        }
        if (!ipAddress().equals("")) {
            flagStatus += " " + ipAddress();
        }
        if (broadcast != null) {
            flagStatus += " broadcast=" + getBroadcastAsString();
        }
        if (netmask != null) {
            flagStatus += " netmask=" + netmask;
        }
    }

    private String ipAddress() {
        if (address instanceof Inet4Address) {
            return address.toString().substring(1, address.toString().length());
        } else if ((address instanceof Inet6Address)) {
            return address.toString().substring(1, address.toString().length() - 3);
        }
        return "";
    }
    
    private String getBroadcastAsString(){
        if (broadcast == null) {
            return "";
        }
        return broadcast.toString().substring(1, broadcast.toString().length());
    }
}
