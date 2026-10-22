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
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.anno.CRuby;
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
import static jnr.constants.platform.AddressFamily.AF_UNSPEC;
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
        // gethostname(2) as MRI: java.net only has getLocalHost(), which needs
        // the host's own name to resolve, and fell back to "0.0.0.0"
        String name = UnixDomainNative.hostname();
        if (name != null) return newString(context, name);

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

    /**
     * An address as MRI prints it: dotted IPv4, or RFC 5952 compressed IPv6
     * ("::1", not java.net's "0:0:0:0:0:0:0:1"), keeping any "%scope".
     */
    public static String ipString(InetAddress address) {
        String host = address.getHostAddress();
        if (!(address instanceof Inet6Address)) return host;
        int scope = host.indexOf('%');
        String bare = scope < 0 ? host : host.substring(0, scope);
        return SocketUtilsIPV6.getIPV6Address(bare) + (scope < 0 ? "" : host.substring(scope));
    }

    // WHY: jnr-netdb's native getservbyname_r/getservbyport_r throw
    // RuntimeException("..._r failed") for a name or port that isn't in the
    // services database, instead of returning null -- so every "unknown
    // service" path (Socket.getservbyname, TCPServer.new("0", "nope"), a
    // non-numeric port String) surfaced a raw Java exception, not
    // SocketError, and a String port that wasn't a service never fell back
    // to its numeric value.
    public static Service serviceByName(String name, String proto) {
        try {
            return Service.getServiceByName(name, proto);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static Service serviceByPort(int port, String proto) {
        try {
            return Service.getServiceByPort(port, proto);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Socket::ResolutionError (Ruby 3.3+): a SocketError carrying getaddrinfo's EAI_* code. */
    public static RaiseException resolutionError(ThreadContext context, String message, int eaiCode) {
        org.jruby.RubyModule socket = (org.jruby.RubyModule) context.runtime.getClassFromPath("Socket");
        IRubyObject klass = socket.getConstantNoConstMissing("ResolutionError");
        if (klass == null) return (RaiseException) sockerr(context.runtime, message);
        IRubyObject error = klass.callMethod(context, "new", new IRubyObject[] { newString(context, message), asFixnum(context, eaiCode) });
        return ((org.jruby.RubyException) error).toThrowable();
    }

    // Linux <netdb.h>
    static final int EAI_NONAME = -2, EAI_FAMILY = -6;

    /** Socket.getservbyport(port, protocol = "tcp") */
    public static IRubyObject getservbyport(ThreadContext context, IRubyObject[] args) {
        int port = toInt(context, args[0]);
        String proto = args.length > 1 && !args[1].isNil() ? args[1].convertToString().toString() : "tcp";
        Service service = port > 0 && port <= 0xffff ? serviceByPort(port, proto) : null;
        if (service == null) throw sockerr(context.runtime, "no such service for port " + port + "/" + proto);
        return newString(context, service.getName());
    }

    /**
     * Socket.gethostbyaddr(packed_address, family = nil): the family follows
     * the address length (4: AF_INET, 16: AF_INET6); an explicit family that
     * doesn't fit the address is "host not found", as MRI's gethostbyaddr(3).
     */
    public static IRubyObject gethostbyaddr(ThreadContext context, IRubyObject[] args) {
        byte[] packed = args[0].convertToString().getBytes();
        AddressFamily family = packed.length == 16 ? AF_INET6 : AF_INET;
        if (args.length > 1 && !args[1].isNil()) {
            AddressFamily requested = addressFamilyFromArg(context, args[1]);
            if (requested != family || (packed.length != 4 && packed.length != 16)) throw sockerr(context.runtime, "host not found");
        }
        try {
            InetAddress addr = InetAddress.getByAddress(packed);
            return RubyArray.newArray(context.runtime,
                    newString(context, addr.getCanonicalHostName()),
                    newEmptyArray(context),
                    asFixnum(context, family.longValue()),
                    args[0]);
        } catch (UnknownHostException e) {
            throw sockerr(context.runtime, "host not found");
        }
    }

    public static IRubyObject getservbyname(ThreadContext context, IRubyObject[] args) {
        String name = args[0].convertToString().toString();
        String proto = args.length ==  1 ? "tcp" : args[1].convertToString().toString();
        Service service = serviceByName(name, proto);
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

    public static IRubyObject gethostbyname(ThreadContext context, IRubyObject hostname) {
        try {
            String name = hostname.convertToString().toString();
            InetAddress addr = getRubyInetAddress(name);

            // MRI (gethostbyname(3)): a literal address comes back as given,
            // a name as its canonical name; the family follows the address
            boolean literal = name.indexOf(':') >= 0 || STRING_IPV4_ADDRESS_PATTERN.matcher(name).matches();
            return RubyArray.newArray(context.runtime,
                    newString(context, literal ? name : addr.getCanonicalHostName()),
                    newEmptyArray(context),
                    asFixnum(context, (addr instanceof Inet6Address ? AF_INET6 : AF_INET).longValue()),
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
                        newString(context, ipString(address)),
                        asFixnum(context, is_ipv6 ? PF_INET6.longValue() : PF_INET.longValue()),
                        asFixnum(context, SOCK_DGRAM.longValue()),
                        asFixnum(context, IPPROTO_UDP.longValue())));
            }

            if (sock_stream) {
                list.add(RubyArray.newArrayMayCopy(context.runtime,
                        newString(context, is_ipv6 ? "AF_INET6" : "AF_INET"),
                        asFixnum(context, port),
                        newString(context, getHostAddress(context, address, reverse)),
                        newString(context, ipString(address)),
                        asFixnum(context, is_ipv6 ? PF_INET6.longValue() : PF_INET.longValue()),
                        asFixnum(context, SOCK_STREAM.longValue()),
                        asFixnum(context, IPPROTO_TCP.longValue())));
            }
        });

        return newArray(context, list);
    }

    public static List<Addrinfo> getaddrinfoList(ThreadContext context, IRubyObject[] args) {
        final List<Addrinfo> l = new ArrayList<Addrinfo>();
        // an explicit protocol argument is kept (MRI), not replaced by the socktype's default
        final IRubyObject protocolArg = args.length > 4 ? args[4] : context.nil;
        final Protocol protocol = protocolArg.isNil() || !(protocolArg instanceof RubyInteger) ? null
                : Protocol.getProtocolByNumber(toInt(context, protocolArg));

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
                Addrinfo info = new Addrinfo(context.runtime, Access.getClass(context, "Addrinfo"),
                        new InetSocketAddress(address, port),
                        Sock.SOCK_DGRAM,
                        SocketType.DATAGRAM,
                        usesCanonical);
                if (protocol != null) info.setProtocol(protocol);
                l.add(info);
            }

            if (sock_stream) {
                Addrinfo info = new Addrinfo(context.runtime, Access.getClass(context, "Addrinfo"),
                        new InetSocketAddress(address, port),
                        Sock.SOCK_STREAM,
                        SocketType.SOCKET,
                        usesCanonical);
                if (protocol != null) info.setProtocol(protocol);
                l.add(info);
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
        if (addressFamily != null && addressFamily != AF_INET && addressFamily != AF_INET6 && addressFamily != AF_UNSPEC) {
            throw resolutionError(context, "getaddrinfo: ai_family not supported", EAI_FAMILY);
        }

        Sock sock = socktype.isNil() ? SOCK_STREAM : sockFromArg(context, socktype);

        if(port instanceof RubyString) {
            port = getservbyname(context, new IRubyObject[]{port});
        }

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
            // get the addresses for the given host name; no host means the
            // loopback address of the requested family (MRI), and "localhost"
            // may not resolve to ::1 at all
            hostString = !emptyHost ? host.convertToString().toString() :
                    addressFamily == AF_INET6 ? "::1" : addressFamily == AF_INET ? "127.0.0.1" : "localhost";
        }

        // AI_NUMERICHOST: the host must be an address literal; never resolve a name
        if ((flag & AI_NUMERICHOST) != 0 && !emptyHost && !isNumericHost(hostString)) {
            throw sockerr(runtime, "getaddrinfo: name or service not known");
        }

        try {
            InetAddress[] addrs = getAllByName(context, host, hostString, emptyHost);

            for(int i = 0; i < addrs.length; i++) {
                // filter out unrelated address families if specified
                if (addressFamily == AF_INET6 && !(addrs[i] instanceof Inet6Address)) continue;
                if (addressFamily == AF_INET && !(addrs[i] instanceof Inet4Address)) continue;
                callback.addrinfo(addrs[i], p, sock, reverseLookup, displayCanonical);
            }

        } catch(UnknownHostException e) {
            throw resolutionError(context, "getaddrinfo: Name or service not known", EAI_NONAME);
        }
    }

    // A host that is not numeric goes to the fiber scheduler's address_resolve
    @CRuby(value = "rsock_getaddrinfo", file = "ext/socket/raddrinfo.c")
    private static InetAddress[] getAllByName(ThreadContext context, IRubyObject host, String hostString, boolean emptyHost) throws UnknownHostException {
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

            // [family, port, hostname, numeric_address]: the numeric address
            // wins when present, else the hostname (MRI)
            String family = ary.eltInternal(0).toString();
            if (!family.equals("AF_INET") && !family.equals("AF_INET6") && !family.equals("AF_UNSPEC")
                    && !family.equals("INET") && !family.equals("INET6")
                    && !(ary.eltInternal(0) instanceof RubyInteger)) {
                throw resolutionError(context, "getnameinfo: ai_family not supported", EAI_FAMILY);
            }
            port = ary.eltInternal(1).toString();
            IRubyObject numeric = len == 4 ? ary.eltInternal(3) : context.nil;
            host = (numeric.isNil() ? ary.eltInternal(2) : numeric).toString();

        } else if (arg0 instanceof RubyString argS) {
            String arg = argS.toString();
            Matcher m = STRING_IPV4_ADDRESS_PATTERN.matcher(arg);

            if (!m.matches()) {
                RubyArray portAndHost;
                try {
                    portAndHost = Sockaddr.unpack_sockaddr_in(context, arg0);
                } catch (RaiseException e) {
                    if (e.getException() instanceof org.jruby.RubyArgumentError) throw sockerr(context.runtime, "getnameinfo: " + e.getMessage());
                    throw e;
                }

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

                    if (ipv6_addr instanceof Inet6Address) host = ipString(ipv6_addr);
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

        host = (flags & NI_NUMERICHOST.intValue()) == 0 ? addr.getCanonicalHostName() : ipString(addr);

        Service serv = serviceByPort(Integer.parseInt(port), null);

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
                return RubyString.newInternalFromJavaExternal(context.runtime, ipString(address));
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
        // nil or "" is "any port" (0), as MRI's getaddrinfo with no service
        if (_port == null || _port.isNil()) return 0;
        if (_port instanceof RubyString str && str.isEmpty()) return 0;
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
                   ipString(addr) : addr.getCanonicalHostName();
        } else if (reverse) {
            ret = addr.getCanonicalHostName();
        } else {
            ret = ipString(addr);
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

    /**
     * A sockopt level argument to its int value, as MRI's rsock_level_arg: an
     * Integer, or a Symbol/String naming SOL_SOCKET or an IPPROTO_* constant,
     * with or without prefix (:SOCKET, :IP, :IPV6, "IPPROTO_TCP", ...).
     */
    static int levelToInt(ThreadContext context, IRubyObject level) {
        String name = nameArg(context, level);
        if (name == null) return toInt(context, level);

        Integer value = socketConstant(context, name);
        if (value == null && name.equals("SOCKET")) value = socketConstant(context, "SOL_SOCKET");
        if (value == null) value = socketConstant(context, "IPPROTO_" + name);
        if (value == null) throw sockerr(context.runtime, "unknown protocol level: " + name);
        return value;
    }

    /**
     * A sockopt name argument to its int value for the given level, as MRI's
     * rsock_optname_to_int: an Integer, or a Symbol/String resolved in that
     * level's own constant namespace (SO_ for SOL_SOCKET, IP_, IPV6_, TCP_,
     * UDP_), with or without prefix. Resolving every name as SO_* made e.g.
     * (:IP, :TTL) fail, and an IP_TTL integer read back as SO_REUSEADDR.
     */
    static int optnameToInt(ThreadContext context, int level, IRubyObject opt) {
        String name = nameArg(context, opt);
        if (name == null) return toInt(context, opt);

        String prefix = optnamePrefix(level);
        Integer value = prefix == null ? null : socketConstant(context, name.startsWith(prefix) ? name : prefix + name);
        if (value == null) throw sockerr(context.runtime, "unknown " + levelDescription(prefix) + " option name: " + name);
        return value;
    }

    private static String levelDescription(String prefix) {
        if (prefix == null) return "socket level";
        return switch (prefix) {
            case "SO_" -> "socket level";
            case "IPV6_" -> "IPv6 level";
            default -> prefix.substring(0, prefix.length() - 1) + " level";
        };
    }

    /** The constant-name prefix of a sockopt level's option names, or null. */
    static String optnamePrefix(int level) {
        if (level == SocketLevel.SOL_SOCKET.intValue()) return "SO_";
        if (level == jnr.constants.platform.IPProto.IPPROTO_IP.intValue()) return "IP_";
        if (level == jnr.constants.platform.IPProto.IPPROTO_IPV6.intValue()) return "IPV6_";
        if (level == IPPROTO_TCP.intValue()) return "TCP_";
        if (level == IPPROTO_UDP.intValue()) return "UDP_";
        return null;
    }

    private static String nameArg(ThreadContext context, IRubyObject arg) {
        IRubyObject maybeString = TypeConverter.checkStringType(context.runtime, arg);
        if (!maybeString.isNil()) return maybeString.toString();
        return arg instanceof RubySymbol ? arg.toString() : null;
    }

    private static Integer socketConstant(ThreadContext context, String name) {
        RubyModule constants = (RubyModule) context.runtime.getClassFromPath("Socket::Constants");
        IRubyObject value = constants.getConstantNoConstMissing(name);
        return value == null ? null : toInt(context, value);
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
            Service serv = serviceByName(portStr.toString(), null);

            if (serv != null) return serv.getPort();

            return ((RubyInteger) portStr.to_i(context)).asInt(context);
        }
        return toInt(context, port);
    }
}
