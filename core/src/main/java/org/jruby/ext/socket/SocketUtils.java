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
import org.jruby.FiberScheduler;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyInteger;
import org.jruby.RubyNil;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.CRuby;
import org.jruby.api.Access;
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
import static jnr.constants.platform.AddressFamily.AF_UNSPEC;
import static org.jruby.ext.socket.ResolutionError.newResolutionError;
import static jnr.constants.platform.AddressFamily.AF_INET6;
import static jnr.constants.platform.IPProto.IPPROTO_TCP;
import static jnr.constants.platform.IPProto.IPPROTO_UDP;
import static jnr.constants.platform.NameInfo.NI_NUMERICHOST;
import static jnr.constants.platform.NameInfo.NI_NUMERICSERV;
import static jnr.constants.platform.ProtocolFamily.PF_INET;
import static jnr.constants.platform.ProtocolFamily.PF_INET6;
import static jnr.constants.platform.Sock.SOCK_DGRAM;
import static jnr.constants.platform.Sock.SOCK_RAW;
import static jnr.constants.platform.Sock.SOCK_STREAM;
import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newEmptyArray;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Error.argumentError;
import static org.jruby.ext.socket.Addrinfo.AI_CANONNAME;
import static org.jruby.ext.socket.Addrinfo.AI_NUMERICHOST;

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
                port = Integer.parseInt(name.stripLeading());
            } catch (NumberFormatException nfe) {
                throw sockerr(context.runtime, "no such service " + name + "/" + proto);
            }
        }

        return asFixnum(context, port);
    }

    public static IRubyObject gethostbyname(ThreadContext context, IRubyObject hostname) {
        try {
            InetAddress addr = getRubyInetAddress(context, hostname.convertToString().toString());

            return RubyArray.newArray(context.runtime,
                    newString(context, addr.getCanonicalHostName()),
                    newEmptyArray(context),
                    asFixnum(context, AF_INET.longValue()),
                    newString(context, new ByteList(addr.getAddress())));
        } catch(UnknownHostException e) {
            throw ResolutionError.newNameNotKnown(context);

        }
    }

    /**
     * Ruby definition would look like:
     * <p></p>
     * def self.getaddrinfo(host, port, family = nil, socktype = nil, protocol = nil, flags = nil, reverse_lookup = nil)
     */
    public static IRubyObject getaddrinfo(final ThreadContext context, IRubyObject[] args) {
        final List<IRubyObject> list = new ArrayList<>();

        buildAddrinfoList(context, args, true, (address, port, sock, protocol, reverse, usesCanonical) -> {
            boolean is_ipv6 = address instanceof Inet6Address;

            list.add(RubyArray.newArrayMayCopy(context.runtime,
                    newString(context, is_ipv6 ? "AF_INET6" : "AF_INET"),
                    asFixnum(context, port),
                    newString(context, getHostAddress(context, address, reverse)),
                    newString(context, address.getHostAddress()),
                    asFixnum(context, is_ipv6 ? PF_INET6.longValue() : PF_INET.longValue()),
                    asFixnum(context, sock.longValue()),
                    asFixnum(context, protocol)));
        });

        return newArray(context, list);
    }

    public static List<Addrinfo> getaddrinfoList(ThreadContext context, IRubyObject[] args) {
        final List<Addrinfo> l = new ArrayList<Addrinfo>();

        buildAddrinfoList(context, args, false, (address, port, sock, protocol, reverse, usesCanonical) -> {
            l.add(new Addrinfo(context.runtime, Access.getClass(context, "Addrinfo"),
                    new InetSocketAddress(address, port),
                    sock,
                    sock == SOCK_DGRAM ? SocketType.DATAGRAM : SocketType.SOCKET,
                    usesCanonical,
                    protocol));
        });

        return l;
    }

    interface AddrinfoCallback {
        void addrinfo(
                InetAddress address,
                int port,
                Sock sock,
                int protocol,
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
        if (processLastArgAsReverse) {
            // The Ruby Socket.getaddrinfo function supports boolean/nil/Symbol values for the
            // reverse_lookup parameter. We need to massage all valid inputs to true/false/null.
             reverseLookup = RubyIPSocket.doReverseLookup(context, reverseArg);
        }

        AddressFamily addressFamily = family.isNil() ? null : addressFamilyFromArg(context, family);

        // like getaddrinfo(3), which only resolves IP addresses and needs a host or a service
        if (addressFamily != null && addressFamily != AF_INET && addressFamily != AF_INET6 && addressFamily != AF_UNSPEC) {
            throw newResolutionError(context, "getaddrinfo: ai_family not supported", "EAI_FAMILY");
        }
        if (host.isNil() && port.isNil()) {
            throw ResolutionError.newNameNotKnown(context);
        }

        boolean serviceName = port instanceof RubyString && !NUMERIC_SERVICE_PATTERN.matcher(port.toString()).matches();

        if(port instanceof RubyString) {
            port = getservbyname(context, new IRubyObject[]{port});
        }

        List<SocktypeProtocol> types = socktypeProtocols(context, socktype, protocol, serviceName);

        int p = port.isNil() ? 0 : toInt(context, port);

        // TODO: implement flags
        int flag = flags.isNil() ? 0 : toInt(context, flags);

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

        // AI_NUMERICHOST: the host must be an address literal; never resolve a name
        if ((flag & AI_NUMERICHOST) != 0 && !emptyHost && !isNumericHost(hostString)) {
            throw ResolutionError.newNameNotKnown(context);
        }

        try {
            InetAddress[] addrs = getAllByName(context, host, hostString, emptyHost);

            for(int i = 0; i < addrs.length; i++) {
                // filter out unrelated address families if specified
                if (addressFamily == AF_INET6 && !(addrs[i] instanceof Inet6Address)) continue;
                if (addressFamily == AF_INET && !(addrs[i] instanceof Inet4Address)) continue;
                for (SocktypeProtocol type : types) {
                    callback.addrinfo(addrs[i], p, type.sock(), type.protocol(), reverseLookup, displayCanonical);
                }
            }

        } catch(UnknownHostException e) {
            throw ResolutionError.newNameNotKnown(context);
        }
    }

    private record SocktypeProtocol(Sock sock, int protocol) {}

    private static final List<SocktypeProtocol> IP_SOCKTYPES = List.of(
            new SocktypeProtocol(SOCK_STREAM, IPPROTO_TCP.intValue()),
            new SocktypeProtocol(SOCK_DGRAM, IPPROTO_UDP.intValue()),
            new SocktypeProtocol(SOCK_RAW, 0));

    private static final Pattern NUMERIC_SERVICE_PATTERN = Pattern.compile("\\s*\\+?[0-9]+");

    private static List<SocktypeProtocol> socktypeProtocols(ThreadContext context, IRubyObject socktype, IRubyObject protocol, boolean serviceName) {
        int requestedType = switch (socktype) {
            case RubyNil nil -> 0;
            case RubyInteger type -> toInt(context, type);
            default -> sockFromArg(context, socktype).intValue();
        };
        int requestedProtocol = protocol.isNil() ? 0 : toInt(context, protocol);
        List<SocktypeProtocol> types = new ArrayList<>(IP_SOCKTYPES.size());

        for (SocktypeProtocol type : IP_SOCKTYPES) {
            if (requestedType != 0 && requestedType != type.sock().intValue()) continue;

            if (type.sock() == SOCK_RAW) {
                // SOCK_RAW takes any protocol but has no services
                if (!serviceName) types.add(new SocktypeProtocol(SOCK_RAW, requestedProtocol));
            } else if (requestedProtocol == 0 || requestedProtocol == type.protocol()) {
                types.add(type);
            }
        }

        if (types.isEmpty()) {
            if (serviceName && (requestedType == 0 || requestedType == SOCK_RAW.intValue())) {
                throw newResolutionError(context, "getaddrinfo: Servname not supported for ai_socktype", "EAI_SERVICE");
            }
            throw newResolutionError(context, "getaddrinfo: ai_socktype not supported", "EAI_SOCKTYPE");
        }

        return types;
    }

    // "<any>", "<broadcast>" and numeric hosts are used as they are; anything else goes to the fiber
    // scheduler's address_resolve, if it has one, before the JDK's resolver
    @CRuby(value = "rsock_getaddrinfo", file = "ext/socket/raddrinfo.c")
    private static InetAddress[] getAllByName(ThreadContext context, IRubyObject host, String hostString, boolean emptyHost) throws UnknownHostException {
        if (!emptyHost && specialAddress(hostString) instanceof InetAddress special) return new InetAddress[] { special };
        if (emptyHost || isNumericHost(hostString)) return InetAddress.getAllByName(hostString);

        IRubyObject scheduler = FiberScheduler.current(context);
        if (scheduler != null) {
            // null when the scheduler has no address_resolve, which leaves resolving to us
            IRubyObject resolved = FiberScheduler.addressResolve(context, scheduler, host);
            if (resolved != null) return schedulerAddresses(hostString, resolved);
        }

        return InetAddress.getAllByName(hostString);
    }

    // Keeps the numeric addresses and fails if none are left
    @CRuby(value = "rb_scheduler_getaddrinfo", file = "ext/socket/raddrinfo.c")
    private static InetAddress[] schedulerAddresses(String hostString, IRubyObject resolved) throws UnknownHostException {
        List<InetAddress> addresses = new ArrayList<>();

        if (!resolved.isNil()) {
            RubyArray<?> ips = resolved.convertToArray();

            for (int i = 0; i < ips.size(); i++) {
                String ip = ips.eltOk(i).convertToString().toString();
                if (!isNumericHost(ip)) continue;

                try {
                    addresses.add(InetAddress.getByName(ip));
                } catch (UnknownHostException e) {
                    // not an address after all
                }
            }
        }

        if (addresses.isEmpty()) throw new UnknownHostException(hostString);

        return addresses.toArray(new InetAddress[0]);
    }

    private static final Pattern NUMERIC_IPV4_HOST_PATTERN = Pattern.compile("[0-9]+(\\.[0-9]+){0,3}");

    // An IPv6 literal (InetAddress parses those without a lookup) or a decimal
    // IPv4 literal; anything else would make InetAddress.getAllByName query DNS.
    private static boolean isNumericHost(String host) {
        return host.contains(":") || NUMERIC_IPV4_HOST_PATTERN.matcher(host).matches();
    }

    public static IRubyObject getnameinfo(ThreadContext context, IRubyObject[] args) {
        int flags = args.length == 2 ? toInt(context, args[1]) : 0;
        IRubyObject arg0 = args[0];
        String host, port;

        if (arg0 instanceof RubyArray ary) {
            final int len = ary.size();

            if (len < 3 || len > 4) {
                throw argumentError(context, "array size should be 3 or 4, "+ len +" given");
            }

            // CRuby resolves the array with getaddrinfo(3), which only handles IP families
            IRubyObject family = ary.eltInternal(0);
            AddressFamily addressFamily = family.isNil() ? null : addressFamilyFromArg(context, family);
            if (addressFamily != null && addressFamily != AF_INET && addressFamily != AF_INET6 && addressFamily != AF_UNSPEC) {
                throw newResolutionError(context, "getaddrinfo: ai_family not supported", "EAI_FAMILY");
            }

            // if array has 4 elements, third element is ignored
            IRubyObject portArg = ary.eltInternal(1);
            IRubyObject hostArg = len == 3 ? ary.eltInternal(2) : ary.eltInternal(3);
            port = portArg.toString();
            host = hostArg.toString();
            if (port.indexOf('\0') >= 0 || host.indexOf('\0') >= 0) throw argumentError(context, "string contains null byte");

        } else if (arg0 instanceof RubyString argS) {
            String arg = argS.toString();
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
            addr = getRubyInetAddress(context, host);
        } catch (UnknownHostException e) {
            throw ResolutionError.newNameNotKnown(context);
        }

        host = (flags & NI_NUMERICHOST.intValue()) == 0 ? addr.getCanonicalHostName() : addr.getHostAddress();

        // the port may also be given as a service name, such as "http"
        int portNumber;
        try {
            portNumber = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            Service named = Service.getServiceByName(port, null);
            if (named == null) throw ResolutionError.newNameNotKnown(context);
            portNumber = named.getPort();
            port = Integer.toString(portNumber);
        }

        Service serv = Service.getServiceByPort(portNumber, null);

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

    /**
     * Resolve a host the way the socket library does, through the fiber scheduler's address_resolve when there is one.
     */
    @CRuby(value = "rsock_addrinfo", file = "ext/socket/raddrinfo.c")
    public static InetAddress[] getRubyInetAddresses(ThreadContext context, String host) throws UnknownHostException {
        return getAllByName(context, newString(context, host), host, false);
    }

    /**
     * The first address {@link #getRubyInetAddresses(ThreadContext, String)} finds for a host.
     */
    public static InetAddress getRubyInetAddress(ThreadContext context, String host) throws UnknownHostException {
        return getRubyInetAddresses(context, host)[0];
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
            InetAddress address = getRubyInetAddress(context, hostnameString);

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
            throw ResolutionError.newNameNotKnown(context);
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
            port = toInt(context, _port);
        } else {
            IRubyObject portString = _port.convertToString();
            IRubyObject portInteger = portString.convertToInteger( "to_i");
            port = toInt(context, portInteger);

            if (port <= 0) {
                port = toInt(context, RubySocket.getservbyname(context, objectClass(context), new IRubyObject[]{portString}));
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

            int domainInt = toInt(context, domain);
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

            int typeInt = toInt(context, type);
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

            int protocolInt = toInt(context, protocol);
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

            int protocolInt = toInt(context, protocol);
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

            return SocketLevel.valueOf(toInt(context, level));
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

            return SocketOption.valueOf(toInt(context, opt));
        } catch (IllegalArgumentException iae) {
            throw SocketUtils.sockerr(context.runtime, "invalid socket option: " + opt);
        }
    }

    @Deprecated(since = "10.0.0.0")
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

            return ((RubyInteger) portStr.to_i(context)).asInt(context);
        }
        return toInt(context, port);
    }
}
