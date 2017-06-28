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
    private NetworkInterface networkInterface;
    private boolean isLink;
    private String netmask;
    private int index;
    private String flagStatus;
    private Addrinfo addr;

    public static void createIfaddr(Ruby runtime) {
        RubyClass ifaddr = runtime.getClass("Socket").defineClassUnder(
                "Ifaddr",
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
        this.isUp = ni.isUp();
        this.name = ni.getDisplayName();
        this.isLoopback = ni.isLoopback();
        this.isPointToPoint = ni.isPointToPoint();
        this.networkInterface = ni;
        this.isLink = false;
        this.address = it.getAddress();
        this.broadcast = it.getBroadcast();
        this.interfaceAddress = it;
        setAddr(runtime);
        setNetmask(it);
        setIndex(ni);
        setInspectString(ni);
    }

    public Ifaddr(Ruby runtime, RubyClass metaClass, NetworkInterface ni) throws Exception {
        super(runtime, metaClass);
        this.isUp = ni.isUp();
        this.name = ni.getDisplayName();
        this.isLoopback = ni.isLoopback();
        this.isPointToPoint = ni.isPointToPoint();
        this.networkInterface = ni;
        this.isLink = true;
        setAddr(runtime);
        setIndex(ni);
        setInspectString(ni);
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
        return addr;
    }

    @JRubyMethod
    public IRubyObject broadaddr(ThreadContext context) {
        if (broadcast != null && isLink == false) {
          return new Addrinfo(context.runtime, context.runtime.getClass("Addrinfo"), broadcast);
        }
        try {
          if (isLink == true && networkInterface.isLoopback() == false) {
            return new Addrinfo(context.runtime, context.runtime.getClass("Addrinfo"), networkInterface, true);
          }
        } catch (SocketException e) {
        }
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject ifindex(ThreadContext context) {
        return context.runtime.newFixnum(index);
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject flags(ThreadContext context) {
        // not implemented yet
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject netmask(ThreadContext context) throws UnknownHostException {
        if (netmask == null) {
            return context.nil;
        }
        return new Addrinfo(context.runtime, context.runtime.getClass("Addrinfo"), InetAddress.getByName(netmask));
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject dstaddr(ThreadContext context) {
        // not implemented yet
        return context.nil;
    }

    private void setAddr(Ruby runtime) {
        if (address != null && isLink == false) {
          addr = new Addrinfo(runtime, runtime.getClass("Addrinfo"), address);
        }
        if (isLink == true) {
          addr = new Addrinfo(runtime, runtime.getClass("Addrinfo"), networkInterface, false);
        }
    }

    private void setNetmask(InterfaceAddress it) throws Exception {
      if ( ( isLoopback || ( it.getNetworkPrefixLength() != 0 ) ) && ( address instanceof Inet4Address) ) {
        String subnet = ipAddress() + "/" + it.getNetworkPrefixLength();
        if ( isLoopback ) {
          subnet = ipAddress() + "/8";   // because getNetworkPrefixLength() incorrectly returns 0 for IPv4 loopback
        }
        SubnetUtils utils = new SubnetUtils(subnet);
        netmask = utils.getInfo().getNetmask();
      } else if ( (it.getNetworkPrefixLength() != 0 ) && ( address instanceof Inet6Address) ) {
        netmask = SocketUtilsIPV6.getIPV6NetMask(ipAddress() + "/" + it.getNetworkPrefixLength());
      }
    }

    private void setIndex(NetworkInterface ni) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field field = ni.getClass().getDeclaredField("index");
        field.setAccessible(true);
        index = (Integer) field.get(ni);
    }

    private void setInspectString(NetworkInterface nif) throws SocketException {
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
        if (isLink == true) {
            flagStatus += " " + addr.packet_inspect();
        } else {
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
    }

    private String ipAddress() {
        if (address instanceof Inet4Address) {
            final String addr = address.toString();
            return addr.substring(1, addr.length());
        } else if ((address instanceof Inet6Address)) {
            final String addr = address.toString();
            return addr.substring(1, addr.length()).split("%")[0];
        }
        return "";
    }

    private String getBroadcastAsString() {
        if (broadcast == null) return "";
        final String brdc = broadcast.toString();
        return brdc.substring(1, brdc.length());
    }

}
