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
import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Sock;
import jnr.constants.platform.SocketLevel;
import jnr.constants.platform.SocketOption;
import jnr.netdb.Protocol;
import jnr.netdb.Service;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyInteger;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.api.Access;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.Sockaddr;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jnr.constants.platform.AddressFamily.AF_INET;
import static jnr.constants.platform.AddressFamily.AF_INET6;
import static jnr.constants.platform.IPProto.IPPROTO_TCP;
import static jnr.constants.platform.IPProto.IPPROTO_UDP;
import static jnr.constants.platform.NameInfo.NI_NUMERICHOST;
import static jnr.constants.platform.NameInfo.NI_NUMERICSERV;
import static jnr.constants.platform.ProtocolFamily.PF_INET;
import static jnr.constants.platform.ProtocolFamily.PF_INET6;
import static jnr.constants.platform.Sock.SOCK_DGRAM;
import static jnr.constants.platform.Sock.SOCK_STREAM;
import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.asInt;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Create.*;
import static org.jruby.api.Error.argumentError;
import static org.jruby.ext.socket.Addrinfo.AI_CANONNAME;

/**
 * Socket class methods for addresses, structures, and so on.
 */
public class SocketUtils {
    public static IRubyObject gethostname(ThreadContext context) {
        try {
            return newString(context, InetAddress.getLocalHost().getHostName());

        } catch (UnknownHostException e) {

            try {
                return newString(context, InetAddress.getByAddress(new byte[]{0, 0, 0, 0}).getHostName());

            } catch (UnknownHostException e2) {
                throw sockerr(context.runtime, "gethostname: name or service not known");

            }
        }
    }

    public static IRubyObject gethostbyaddr(ThreadContext context, IRubyObject[] args) {
        var ret0 = newString(context, Sockaddr.addressFromString(context.runtime, args[0].convertToString().toString()).getCanonicalHostName());
        var ret1 = newEmptyArray(context);
        var ret2 = asFixnum(context, 2); // AF_INET
        var ret3 = args[0];

        return RubyArray.newArray(context.runtime, ret0, ret1, ret2, ret3);
    }

    public static IRubyObject getservbyname(ThreadContext context, IRubyObject[] args) {
        String name = args[0].convertToString().toString();
        String proto = args.length ==  1 ? "tcp" : args[1].convertToString().toString();
        Service service = Service.getServiceByName(name, proto);
        int port;

        if (service != null) {
            port = service.getPort();
        } else {
            // MRI behavior: try to convert the name string to port directly
            try {
                port = Integer.parseInt(name.trim());
            } catch (NumberFormatException nfe) {
                throw sockerr(context.runtime, "no such service " + name + "/" + proto);
            }
        }

        return asFixnum(context, port);
    }

    @Deprecated
    public static IRubyObject pack_sockaddr_in(ThreadContext context, IRubyObject port, IRubyObject host) {
        return Sockaddr.pack_sockaddr_in(context, port, host);
    }

    @Deprecated
    public static RubyArray unpack_sockaddr_in(ThreadContext context, IRubyObject addr) {
        return Sockaddr.unpack_sockaddr_in(context, addr);
    }

    @Deprecated
    public static IRubyObject pack_sockaddr_un(ThreadContext context, IRubyObject filename) {
        String path = filename.convertToString().asJavaString();
        return Sockaddr.pack_sockaddr_un(context, path);
    }

    public static IRubyObject gethostbyname(ThreadContext context, IRubyObject hostname) {
        try {
            InetAddress addr = getRubyInetAddress(hostname.convertToString().toString());

            return RubyArray.newArray(context.runtime,
                    newString(context, addr.getCanonicalHostName()),
                    newEmptyArray(context),
                    asFixnum(context, AF_INET.longValue()),
                    newString(context, new ByteList(addr.getAddress())));
        } catch(UnknownHostException e) {
            throw sockerr(context.runtime, "gethostbyname: name or service not known");

        }
    }

    /**
     * Ruby definition would look like:
     * <p></p>
     * def self.getaddrinfo(host, port, family = nil, socktype = nil, protocol = nil, flags = nil, reverse_lookup = nil)
     */
    public static IRubyObject getaddrinfo(final ThreadContext context, IRubyObject[] args) {
        final List<IRubyObject> list = new ArrayList<>();

        buildAddrinfoList(context, args, true, (address, port, sock, reverse, usesCanonical) -> {
            boolean is_ipv6 = address instanceof Inet6Address;
            boolean sock_stream = true;
            boolean sock_dgram = true;

            if (sock != null) {
                if (sock == SOCK_STREAM) {
                    sock_dgram = false;
                } else if (sock == SOCK_DGRAM) {
                    sock_stream = false;
                }
            }

            if (sock_dgram) {
                list.add(RubyArray.newArrayMayCopy(context.runtime,
                        newString(context, is_ipv6 ? "AF_INET6" : "AF_INET"),
                        asFixnum(context, port),
                        newString(context, getHostAddress(context, address, reverse)),
                        newString(context, address.getHostAddress()),
                        asFixnum(context, is_ipv6 ? PF_INET6.longValue() : PF_INET.longValue()),
                        asFixnum(context, SOCK_DGRAM.longValue()),
                        asFixnum(context, IPPROTO_UDP.longValue())));
            }

            if (sock_stream) {
                list.add(RubyArray.newArrayMayCopy(context.runtime,
                        newString(context, is_ipv6 ? "AF_INET6" : "AF_INET"),
                        asFixnum(context, port),
                        newString(context, getHostAddress(context, address, reverse)),
                        newString(context, address.getHostAddress()),
                        asFixnum(context, is_ipv6 ? PF_INET6.longValue() : PF_INET.longValue()),
                        asFixnum(context, SOCK_STREAM.longValue()),
                        asFixnum(context, IPPROTO_TCP.longValue())));
            }
        });

        return newArray(context, list);
    }

    public static List<Addrinfo> getaddrinfoList(ThreadContext context, IRubyObject[] args) {
        final List<Addrinfo> l = new ArrayList<Addrinfo>();

        buildAddrinfoList(context, args, false, (address, port, sock, reverse, usesCanonical) -> {
            boolean sock_stream = true;
            boolean sock_dgram = true;

            if (sock != null) {
                if (sock == SOCK_STREAM) {
                    sock_dgram = false;

                } else if (sock == SOCK_DGRAM) {
                    sock_stream = false;

                }
            }

            if (sock_dgram) {
                l.add(new Addrinfo(context.runtime, Access.getClass(context, "Addrinfo"),
                        new InetSocketAddress(address, port),
                        Sock.SOCK_DGRAM,
                        SocketType.DATAGRAM,
                        usesCanonical));
            }

            if (sock_stream) {
                l.add(new Addrinfo(context.runtime, Access.getClass(context, "Addrinfo"),
                        new InetSocketAddress(address, port),
                        Sock.SOCK_STREAM,
                        SocketType.SOCKET,
                        usesCanonical));
            }
        });

        return l;
    }

    interface AddrinfoCallback {
        void addrinfo(
                InetAddress address,
                int port,
                Sock sock,
                Boolean reverse,
                boolean usesCanonical);
    }

    // FIXME: timeout is not actually implemented and while this original method dualed nice betwee Socket/AddrInfo they now deviate on 7th arg.
    public static void buildAddrinfoList(ThreadContext context, IRubyObject[] args, boolean processLastArgAsReverse, AddrinfoCallback callback) {
        Ruby runtime = context.runtime;
        IRubyObject host = args[0];
        IRubyObject port = args[1];
        boolean emptyHost = host.isNil() || host.convertToString().isEmpty();

        IRubyObject family = args.length > 2 ? args[2] : context.nil;
        IRubyObject socktype = args.length > 3 ? args[3] : context.nil;
        IRubyObject protocol = args.length > 4 ? args[4] : context.nil;
        IRubyObject flags = args.length > 5 ? args[5] : context.nil;
        IRubyObject reverseArg = args.length > 6 ? args[6] : context.nil;

        Boolean reverseLookup = null;
        IRubyObject timeout = context.nil;
        if (processLastArgAsReverse) {
            // The Ruby Socket.getaddrinfo function supports boolean/nil/Symbol values for the
            // reverse_lookup parameter. We need to massage all valid inputs to true/false/null.
             reverseLookup = RubyIPSocket.doReverseLookup(context, reverseArg);
        } else {
            if (reverseArg != context.nil) {
                timeout = ArgsUtil.extractKeywordArg(context, "timeout", reverseArg);
            }
        }

        AddressFamily addressFamily = family.isNil() ? null : addressFamilyFromArg(context, family);

        Sock sock = socktype.isNil() ? SOCK_STREAM : sockFromArg(context, socktype);

        if(port instanceof RubyString) {
            port = getservbyname(context, new IRubyObject[]{port});
        }

        int p = port.isNil() ? 0 : toInt(context, port);

        // TODO: implement flags
        int flag = flags.isNil() ? 0 : RubyNumeric.fix2int(flags);

        boolean displayCanonical = (flag & AI_CANONNAME) != 0;

        String hostString = null;

        // The value of 1 is for Socket::AI_PASSIVE.
        if ((flag == 1) && emptyHost) {
            // use RFC 2732 style string to ensure that we get Inet6Address
            hostString = (addressFamily == AddressFamily.AF_INET6) ? "[::]" : "0.0.0.0";
        } else {
            // get the addresses for the given host name
            hostString = emptyHost ? "localhost" : host.convertToString().toString();
        }

        try {
            InetAddress[] addrs = InetAddress.getAllByName(hostString);

            for(int i = 0; i < addrs.length; i++) {
                // filter out unrelated address families if specified
                if (addressFamily == AF_INET6 && !(addrs[i] instanceof Inet6Address)) continue;
                if (addressFamily == AF_INET && !(addrs[i] instanceof Inet4Address)) continue;
                callback.addrinfo(addrs[i], p, sock, reverseLookup, displayCanonical);
            }

        } catch(UnknownHostException e) {
            throw sockerr(runtime, "getaddrinfo: name or service not known");

        }
    }

    public static IRubyObject getnameinfo(ThreadContext context, IRubyObject[] args) {
        int flags = args.length == 2 ? RubyNumeric.num2int(args[1]) : 0;
        IRubyObject arg0 = args[0];
        String host, port;

        if (arg0 instanceof RubyArray) {
            RubyArray ary = (RubyArray) arg0;
            final int len = ary.size();

            if (len < 3 || len > 4) {
                throw argumentError(context, "array size should be 3 or 4, "+ len +" given");
            }

            // if array has 4 elements, third element is ignored
            port = ary.eltInternal(1).toString();
            host = len == 3 ? ary.eltInternal(2).toString() : ary.eltInternal(3).toString();

        } else if (arg0 instanceof RubyString) {
            String arg = ((RubyString) arg0).toString();
            Matcher m = STRING_IPV4_ADDRESS_PATTERN.matcher(arg);

            if (!m.matches()) {
                RubyArray portAndHost = Sockaddr.unpack_sockaddr_in(context, arg0);

                if (portAndHost.size() != 2) throw argumentError(context, "invalid address representation");

                port = portAndHost.eltInternal(0).toString();
                host = portAndHost.eltInternal(1).toString();

            } else if ((host = m.group(IPV4_HOST_GROUP)) == null || host.length() == 0 ||
                    (port = m.group(IPV4_PORT_GROUP)) == null || port.length() == 0) {

                throw argumentError(context, "invalid address string");
            } else {
                // Try IPv6
                try {
                    InetAddress ipv6_addr = InetAddress.getByName(host);

                    if (ipv6_addr instanceof Inet6Address) host = ipv6_addr.getHostAddress();
                } catch (UnknownHostException uhe) {
                    throw argumentError(context, "invalid address string");
                }
            }
        } else {
            throw argumentError(context, "invalid args");
        }

        InetAddress addr;

        try {
            addr = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw sockerr(context.runtime, "unknown host: "+ host);
        }

        host = (flags & NI_NUMERICHOST.intValue()) == 0 ? addr.getCanonicalHostName() : addr.getHostAddress();

        Service serv = Service.getServiceByPort(Integer.parseInt(port), null);

        if (serv != null) {
            port = (flags & NI_NUMERICSERV.intValue()) == 0 ? serv.getName() : Integer.toString(serv.getPort());
        }

        return newArray(context, newString(context, host), newString(context, port));
    }

    public static IRubyObject ip_address_list(ThreadContext context) {
        try {
            var list = newArray(context);
            RubyClass addrInfoCls = Access.getClass(context, "Addrinfo");

            for (Enumeration<NetworkInterface> networkIfcs = NetworkInterface.getNetworkInterfaces(); networkIfcs.hasMoreElements() ; ) {
                for (Enumeration<InetAddress> addresses = networkIfcs.nextElement().getInetAddresses(); addresses.hasMoreElements() ; ) {
                    list.append(context, new Addrinfo(context.runtime, addrInfoCls, addresses.nextElement()));
                }
            }

            return list;
        } catch (SocketException se) {
            throw sockerr(context.runtime, se.getLocalizedMessage());
        }
    }

    @Deprecated
    public static InetAddress[] getRubyInetAddresses(ByteList address) throws UnknownHostException {
        // switched to String because the ByteLists were not comparing properly in 1.9 mode (encoding?
        // FIXME: Need to properly decode this string (see Helpers.decodeByteList)
        String addressString = Helpers.byteListToString(address);
        return getRubyInetAddresses(addressString);
    }

    public static InetAddress[] getRubyInetAddresses(String addressString) throws UnknownHostException {
        InetAddress specialAddress = specialAddress(addressString);
        if (specialAddress != null) {
            return new InetAddress[] {specialAddress};
        } else {
            return InetAddress.getAllByName(addressString);
        }
    }

    public static InetAddress getRubyInetAddress(String addressString) throws UnknownHostException {
        InetAddress specialAddress = specialAddress(addressString);
        if (specialAddress != null) {
            return specialAddress;
        } else {
            return InetAddress.getByName(addressString);
        }
    }

    public static InetAddress getRubyInetAddress(String host, String node) throws UnknownHostException {
        InetAddress specialAddress = specialAddress(host);
        if (specialAddress != null) {
            return specialAddress;
        } else {
            return InetAddress.getByAddress(host, InetAddress.getByName(node).getAddress());
        }
    }

    public static InetAddress getRubyInetAddress(byte[] addressBytes) throws UnknownHostException {
        return InetAddress.getByAddress(addressBytes);
    }

    private static InetAddress specialAddress(String addressString) throws UnknownHostException {
        if (addressString.equals(BROADCAST)) {
            return InetAddress.getByAddress(INADDR_BROADCAST);
        } else if (addressString.equals(ANY)) {
            return InetAddress.getByAddress(INADDR_ANY);
        } else {
            return null;
        }
    }

    public static final String IP_V4_MAPPED_ADDRESS_PREFIX = "::ffff:";
    private static final String ipv6LocalHost = "::1";

    public static boolean isIPV4MappedAddressPrefix(String address) {
        return address.startsWith(IP_V4_MAPPED_ADDRESS_PREFIX);
    }

    public static IRubyObject getaddress(ThreadContext context, IRubyObject hostname) {
        try {
            String hostnameString = hostname.convertToString().toString();
            InetAddress address = InetAddress.getByName(hostnameString);

            if (isIPV4MappedAddressPrefix(hostnameString)) {
                // See https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/Inet6Address.html#special-ipv6-address-heading
                // IPv4 mapped IPv6 addresses will always return an Inet4Address. When given an IPv6 address to
                // IPSocket.getaddress, ruby will return the IPv6 address. This is not the case in Java.
                return RubyString.newInternalFromJavaExternal(
                        context.runtime, IP_V4_MAPPED_ADDRESS_PREFIX + address.getHostAddress());
            } else if (hostnameString.equals(ipv6LocalHost)) {
                // Ruby will return "::1" for the local host IPv6 address.
                // Java will return the full IPv6 address of 0:0:0:0:0:0:0:1.
                return RubyString.newInternalFromJavaExternal(context.runtime, ipv6LocalHost);
            } else {
                return RubyString.newInternalFromJavaExternal(context.runtime, address.getHostAddress());
            }
        } catch(UnknownHostException e) {
            throw sockerr(context.runtime, "getaddress: name or service not known");
        }
    }

    public static RuntimeException sockerr(Ruby runtime, String msg) {
        return RaiseException.from(runtime, Access.getClass(runtime.getCurrentContext(), "SocketError"), msg);
    }

    public static RuntimeException sockerr_with_trace(Ruby runtime, String msg, StackTraceElement[] trace) {
        String eol = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();
        sb.append(msg);
        for (int i = 0, il = trace.length; i < il; i++) {
            sb.append(eol).append(trace[i].toString());
        }
        return RaiseException.from(runtime, Access.getClass(runtime.getCurrentContext(), "SocketError"), sb.toString());
    }

    public static int getPortFrom(ThreadContext context, IRubyObject _port) {
        int port;
        if (_port instanceof RubyInteger) {
            port = RubyNumeric.fix2int(_port);
        } else {
            IRubyObject portString = _port.convertToString();
            IRubyObject portInteger = portString.convertToInteger( "to_i");
            port = RubyNumeric.fix2int(portInteger);

            if (port <= 0) {
                port = RubyNumeric.fix2int(RubySocket.getservbyname(context, objectClass(context), new IRubyObject[]{portString}));
            }
        }

        return port;
    }

    private static String getHostAddress(ThreadContext context, InetAddress addr, Boolean reverse) {
        String ret;
        if (reverse == null) {
            ret = context.runtime.isDoNotReverseLookupEnabled() ?
                   addr.getHostAddress() : addr.getCanonicalHostName();
        } else if (reverse) {
            ret = addr.getCanonicalHostName();
        } else {
            ret = addr.getHostAddress();
        }
        return ret;
    }

    private static final Pattern STRING_IPV4_ADDRESS_PATTERN = Pattern.compile("((.*)\\/)?([\\.0-9]+)(:([0-9]+))?");

    private static final int IPV4_HOST_GROUP = 3;
    private static final int IPV4_PORT_GROUP = 5;

    private static final String BROADCAST = "<broadcast>";
    private static final byte[] INADDR_BROADCAST = new byte[] {-1,-1,-1,-1}; // 255.255.255.255
    private static final String ANY = "<any>";
    private static final byte[] INADDR_ANY = new byte[] {0,0,0,0}; // 0.0.0.0

    // MRI: address family part of rsock_family_to_int
    static AddressFamily addressFamilyFromArg(ThreadContext context, IRubyObject domain) {
        IRubyObject maybeString = TypeConverter.checkStringType(context.runtime, domain);

        if (!maybeString.isNil()) {
            domain = maybeString;
        }

        try {
            if (domain instanceof RubyString || domain instanceof RubySymbol) {
                String domainString = domain.toString();
                if (domainString.startsWith("AF_")) return AddressFamily.valueOf(domainString);
                if (domainString.startsWith("PF_"))
                    return AddressFamily.valueOf(ProtocolFamily.valueOf(domainString).intValue());
                return AddressFamily.valueOf("AF_" + domainString);
            }

            int domainInt = RubyNumeric.fix2int(domain);
            return AddressFamily.valueOf(domainInt);
        } catch (IllegalArgumentException iae) {
            throw SocketUtils.sockerr(context.runtime, "invalid address family: " + domain);
        }
    }

    static Sock sockFromArg(ThreadContext context, IRubyObject type) {
        IRubyObject maybeString = TypeConverter.checkStringType(context.runtime, type);

        if (!maybeString.isNil()) {
            type = maybeString;
        }

        try {
            if(type instanceof RubyString || type instanceof RubySymbol) {
                String typeString = type.toString();
                if (typeString.startsWith("SOCK_")) return Sock.valueOf(typeString.toString());
                return Sock.valueOf("SOCK_" + typeString);
            }

            int typeInt = RubyNumeric.fix2int(type);
            return Sock.valueOf(typeInt);
        } catch (IllegalArgumentException iae) {
            throw SocketUtils.sockerr(context.runtime, "invalid socket type: " + type);
        }
    }

    // MRI: protocol family part of rsock_family_to_int
    static ProtocolFamily protocolFamilyFromArg(ThreadContext context, IRubyObject protocol) {
        IRubyObject maybeString = TypeConverter.checkStringType(context.runtime, protocol);

        if (!maybeString.isNil()) {
            protocol = maybeString;
        }

        try {
            if (protocol instanceof RubyString || protocol instanceof RubySymbol) {
                String protocolString = protocol.toString();
                if (protocolString.startsWith("PF_")) return ProtocolFamily.valueOf(protocolString);
                if (protocolString.startsWith("AF_")) return ProtocolFamily.valueOf(AddressFamily.valueOf(protocolString).intValue());
                return ProtocolFamily.valueOf("PF_" + protocolString);
            }

            int protocolInt = RubyNumeric.fix2int(protocol);
            return ProtocolFamily.valueOf(protocolInt);
        } catch (IllegalArgumentException iae) {
            throw SocketUtils.sockerr(context.runtime, "invalid protocol family: " + protocol);
        }
    }

    static Protocol protocolFromArg(ThreadContext context, IRubyObject protocol) {
        IRubyObject maybeString = TypeConverter.checkStringType(context.runtime, protocol);

        if (!maybeString.isNil()) {
            protocol = maybeString;
        }

        try {
            if(protocol instanceof RubyString || protocol instanceof RubySymbol) {
                String protocolString = protocol.toString();
                return Protocol.getProtocolByName(protocolString);
            }

            int protocolInt = RubyNumeric.fix2int(protocol);
            return Protocol.getProtocolByNumber(protocolInt);
        } catch (IllegalArgumentException iae) {
            throw SocketUtils.sockerr(context.runtime, "invalid protocol: " + protocol);
        }
    }

    static SocketLevel levelFromArg(ThreadContext context, IRubyObject level) {
        IRubyObject maybeString = TypeConverter.checkStringType(context.runtime, level);

        if (!maybeString.isNil()) {
            level = maybeString;
        }

        try {
            if (level instanceof RubyString || level instanceof RubySymbol) {
                String levelString = level.toString();
                if(levelString.startsWith("SOL_")) return SocketLevel.valueOf(levelString);
                return SocketLevel.valueOf("SOL_" + levelString);
            }

            return SocketLevel.valueOf(RubyNumeric.fix2int(level));
        } catch (IllegalArgumentException iae) {
            throw SocketUtils.sockerr(context.runtime, "invalid socket level: " + level);
        }
    }

    static SocketOption optionFromArg(ThreadContext context, IRubyObject opt) {
        IRubyObject maybeString = TypeConverter.checkStringType(context.runtime, opt);

        if (!maybeString.isNil()) opt = maybeString;

        try {
            if (opt instanceof RubyString || opt instanceof RubySymbol) {
                String optString = opt.toString();
                if (optString.startsWith("SO_")) return SocketOption.valueOf(optString);
                return SocketOption.valueOf("SO_" + optString);
            }

            return SocketOption.valueOf(RubyNumeric.fix2int(opt));
        } catch (IllegalArgumentException iae) {
            throw SocketUtils.sockerr(context.runtime, "invalid socket option: " + opt);
        }
    }

    @Deprecated(since = "10.0")
    public static int portToInt(IRubyObject port) {
        return portToInt(((RubyBasicObject) port).getCurrentContext(), port);
    }

    public static int portToInt(ThreadContext context, IRubyObject port) {
        if (port.isNil()) return 0;

        IRubyObject maybeStr = TypeConverter.checkStringType(context.runtime, port);
        if (!maybeStr.isNil()) {
            RubyString portStr = maybeStr.convertToString();
            Service serv = Service.getServiceByName(portStr.toString(), null);

            if (serv != null) return serv.getPort();

            return asInt(context, (RubyInteger) portStr.to_i(context));
        }
        return RubyNumeric.fix2int(port);
    }
}
