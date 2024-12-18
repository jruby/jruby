package org.jruby.ext.socket;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Access;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

import jnr.constants.platform.InterfaceInfo;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.newString;

/**
 *
 * @author Lucas Allan Amorim
 */
public class Ifaddr extends RubyObject {

    private String name;
    private boolean isUp;
    private boolean isLoopback;
    private boolean isPointToPoint;
    private InetAddress address;
    private InetAddress broadcast;
    private InterfaceAddress interfaceAddress;
    private NetworkInterface networkInterface;
    private boolean isLink;
    private String netmask;
    private int index;
    private int flags;
    private String flagStatus;
    private Addrinfo addr;

    public static void createIfaddr(ThreadContext context, RubyClass Object, RubyClass Socket) {
        Socket.defineClassUnder(context, "Ifaddr", Object, Ifaddr::new).defineMethods(context, Ifaddr.class);
    }

    public Ifaddr(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public Ifaddr(Ruby runtime, RubyClass metaClass, NetworkInterface ni, InterfaceAddress it) throws SocketException {
        super(runtime, metaClass);
        this.name = ni.getDisplayName();
        this.isLoopback = ni.isLoopback();
        this.isUp = ni.isUp();
        this.isPointToPoint = ni.isPointToPoint();
        this.index = ni.getIndex();
        this.networkInterface = ni;
        this.isLink = false;
        this.flags = 0;
        this.address = it.getAddress();
        this.broadcast = it.getBroadcast();
        this.interfaceAddress = it;
        setAddr(runtime.getCurrentContext());
        setNetmask(it);
        setInspectString(ni);
        setInterfaceFlags(ni);
    }

    public Ifaddr(Ruby runtime, RubyClass metaClass, NetworkInterface ni) throws SocketException {
        super(runtime, metaClass);
        this.name = ni.getDisplayName();
        this.isLoopback = ni.isLoopback();
        this.isUp = ni.isUp();
        this.isPointToPoint = ni.isPointToPoint();
        this.index = ni.getIndex();
        this.flags = 0;
        this.networkInterface = ni;
        this.isLink = true;
        setAddr(runtime.getCurrentContext());
        setInspectString(ni);
        setInterfaceFlags(ni);
    }

    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        return newString(context, "#<Socket::Ifaddr: " + name + " " + flagStatus + ">");
    }

    @JRubyMethod
    public IRubyObject name(ThreadContext context) {
        return newString(context, name);
    }

    @JRubyMethod
    public IRubyObject addr(ThreadContext context) {
        return addr;
    }

    @JRubyMethod
    public IRubyObject broadaddr(ThreadContext context) {
        if (broadcast != null && isLink == false) {
          return new Addrinfo(context.runtime, Access.getClass(context, "Addrinfo"), broadcast);
        }
        try {
          if (isLink == true && networkInterface.isLoopback() == false) {
            return new Addrinfo(context.runtime, Access.getClass(context, "Addrinfo"), networkInterface, true);
          }
        } catch (SocketException e) {
        }
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject ifindex(ThreadContext context) {
        return asFixnum(context, index);
    }

    @JRubyMethod
    public IRubyObject flags(ThreadContext context) {
        return asFixnum(context, flags);
    }

    @JRubyMethod
    public IRubyObject netmask(ThreadContext context) {
        if (netmask == null) return context.nil;

        try {
            return new Addrinfo(context.runtime, Access.getClass(context, "Addrinfo"), InetAddress.getByName(netmask));
        } catch (UnknownHostException uhe) {
            throw context.runtime.newIOErrorFromException(uhe);
        }
    }

    @JRubyMethod(notImplemented = true)
    public IRubyObject dstaddr(ThreadContext context) {
        // not implemented yet
        return context.nil;
    }

    private void setAddr(ThreadContext context) {
        if (address != null && isLink == false) {
          addr = new Addrinfo(context.runtime, Access.getClass(context, "Addrinfo"), address);
        }
        if (isLink == true) {
          addr = new Addrinfo(context.runtime, Access.getClass(context, "Addrinfo"), networkInterface, false);
        }
    }

    private void setNetmask(InterfaceAddress it) {
        if ( ( isLoopback || ( it.getNetworkPrefixLength() != 0 ) ) && ( address instanceof Inet4Address) ) {
            String subnet = ipAddress() + "/" + it.getNetworkPrefixLength();
            if ( isLoopback ) {
                subnet = ipAddress() + "/8"; // because getNetworkPrefixLength() incorrectly returns 0 for IPv4 loopback
            }
            SubnetUtils utils = new SubnetUtils(subnet);
            netmask = utils.getInfo().getNetmask();
        } else if ( (it.getNetworkPrefixLength() != 0 ) && ( address instanceof Inet6Address) ) {
            netmask = SocketUtilsIPV6.getIPV6NetMask(ipAddress() + "/" + it.getNetworkPrefixLength());
        }
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
            return address.toString().substring(1);
        }
        if ((address instanceof Inet6Address)) {
            return address.toString().substring(1).split("%")[0];
        }
        return "";
    }

    private String getBroadcastAsString() {
        if (broadcast == null) return "";
        return broadcast.toString().substring(1);
    }

    private void setInterfaceFlags(NetworkInterface nif) throws SocketException {
        if (isUp) {
            flags |= InterfaceInfo.IFF_UP.value();
        }
        if (nif.supportsMulticast()) {
            flags |= InterfaceInfo.IFF_MULTICAST.value();
        }
        if (isLoopback) {
            flags |= InterfaceInfo.IFF_LOOPBACK.value();
        }
        if (isPointToPoint) {
            flags |= InterfaceInfo.IFF_POINTOPOINT.value();
        }
    }

}
