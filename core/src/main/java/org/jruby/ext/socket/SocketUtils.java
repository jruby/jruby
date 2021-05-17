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
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyInteger;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
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

/**
 * Socket class methods for addresses, structures, and so on.
 */
public class SocketUtils {
    public static IRubyObject gethostname(ThreadContext context) {
        Ruby runtime = context.runtime;

        try {
            return RubyString.newString(context.runtime, InetAddress.getLocalHost().getHostName());

        } catch(UnknownHostException e) {

            try {
                return RubyString.newString(context.runtime, InetAddress.getByAddress(new byte[]{0, 0, 0, 0}).getHostName());

            } catch(UnknownHostException e2) {
                throw sockerr(runtime, "gethostname: name or service not known");

            }
        }
    }

    public static IRubyObject gethostbyaddr(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        IRubyObject ret0, ret1, ret2, ret3;

        ret0 = runtime.newString(Sockaddr.addressFromString(runtime, args[0].convertToString().toString()).getCanonicalHostName());
        ret1 = runtime.newArray();
        ret2 = runtime.newFixnum(2); // AF_INET
        ret3 = args[0];

        return RubyArray.newArray(runtime, ret0, ret1, ret2, ret3);
    }

    public static IRubyObject getservbyname(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
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
                throw sockerr(runtime, "no such service " + name + "/" + proto);

            }

        }

        return runtime.newFixnum(port);
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
        Ruby runtime = context.runtime;

        try {
            InetAddress addr = getRubyInetAddress(hostname.convertToString().toString());
            IRubyObject ret0, ret1, ret2, ret3;

            ret0 = runtime.newString(addr.getCanonicalHostName());
            ret1 = runtime.newArray();
            ret2 = runtime.newFixnum(AF_INET);
            ret3 = runtime.newString(new ByteList(addr.getAddress()));
            return RubyArray.newArray(runtime, ret0, ret1, ret2, ret3);

        } catch(UnknownHostException e) {
            throw sockerr(runtime, "gethostbyname: name or service not known");

        }
    }

    /**
     * Ruby definition would look like:
     *
     * def self.getaddrinfo(host, port, family = nil, socktype = nil, protocol = nil, flags = nil, reverse_lookup = nil)
     */
    public static IRubyObject getaddrinfo(final ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.runtime;
        final List<IRubyObject> l = new ArrayList<IRubyObject>();

        buildAddrinfoList(context, args, new AddrinfoCallback() {
            @Override
            public void addrinfo(InetAddress address, int port, Sock sock, Boolean reverse) {
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

                IRubyObject[] c;

                if (sock_dgram) {
                    c = new IRubyObject[7];
                    c[0] = runtime.newString(is_ipv6 ? "AF_INET6" : "AF_INET");
                    c[1] = runtime.newFixnum(port);
                    c[2] = runtime.newString(getHostAddress(context, address, reverse));
                    c[3] = runtime.newString(address.getHostAddress());
                    c[4] = runtime.newFixnum(is_ipv6 ? PF_INET6 : PF_INET);
                    c[5] = runtime.newFixnum(SOCK_DGRAM);
                    c[6] = runtime.newFixnum(IPPROTO_UDP);
                    l.add(RubyArray.newArrayMayCopy(runtime, c));
                }

                if (sock_stream) {
                    c = new IRubyObject[7];
                    c[0] = runtime.newString(is_ipv6 ? "AF_INET6" : "AF_INET");
                    c[1] = runtime.newFixnum(port);
                    c[2] = runtime.newString(getHostAddress(context, address, reverse));
                    c[3] = runtime.newString(address.getHostAddress());
                    c[4] = runtime.newFixnum(is_ipv6 ? PF_INET6 : PF_INET);
                    c[5] = runtime.newFixnum(SOCK_STREAM);
                    c[6] = runtime.newFixnum(IPPROTO_TCP);
                    l.add(RubyArray.newArrayMayCopy(runtime, c));
                }
            }
        });

        return runtime.newArray(l);
    }

    public static List<Addrinfo> getaddrinfoList(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.runtime;
        final List<Addrinfo> l = new ArrayList<Addrinfo>();

        buildAddrinfoList(context, args, new AddrinfoCallback() {
            @Override
            public void addrinfo(InetAddress address, int port, Sock sock, Boolean reverse) {
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
                    l.add(new Addrinfo(runtime, runtime.getClass("Addrinfo"),
                            new InetSocketAddress(address, port),
                            Sock.SOCK_DGRAM,
                            SocketType.DATAGRAM));
                }

                if (sock_stream) {
                    l.add(new Addrinfo(runtime, runtime.getClass("Addrinfo"),
                            new InetSocketAddress(address, port),
                            Sock.SOCK_STREAM,
                            SocketType.SOCKET));
                }
            }
        });

        return l;
    }

    interface AddrinfoCallback {
        void addrinfo(
                InetAddress address,
                int port,
                Sock sock,
                Boolean reverse);
    }

    public static void buildAddrinfoList(ThreadContext context, IRubyObject[] args, AddrinfoCallback callback) {
        Ruby runtime = context.runtime;
        IRubyObject host = args[0];
        IRubyObject port = args[1];
        boolean emptyHost = host.isNil() || host.convertToString().isEmpty();

        IRubyObject family = args.length > 2 ? args[2] : context.nil;
        IRubyObject socktype = args.length > 3 ? args[3] : context.nil;
        IRubyObject protocol = args.length > 4 ? args[4] : context.nil;
        IRubyObject flags = args.length > 5 ? args[5] : context.nil;
        IRubyObject reverseArg = args.length > 6 ? args[6] : context.nil;

        // The Ruby Socket.getaddrinfo function supports boolean/nil/Symbol values for the
        // reverse_lookup parameter. We need to massage all valid inputs to true/false/null.
        Boolean reverseLookup = RubyIPSocket.doReverseLookup(context, reverseArg);

        AddressFamily addressFamily = family.isNil() ? null : addressFamilyFromArg(family);

        Sock sock = socktype.isNil() ? SOCK_STREAM : sockFromArg(socktype);

        if(port instanceof RubyString) {
            port = getservbyname(context, new IRubyObject[]{port});
        }

        int p = port.isNil() ? 0 : (int)port.convertToInteger().getLongValue();

        // TODO: implement flags
        int flag = flags.isNil() ? 0 : RubyNumeric.fix2int(flags);

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
                callback.addrinfo(addrs[i], p, sock, reverseLookup);
            }

        } catch(UnknownHostException e) {
            throw sockerr(runtime, "getaddrinfo: name or service not known");

        }
    }

    public static IRubyObject getnameinfo(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        int flags = args.length == 2 ? RubyNumeric.num2int(args[1]) : 0;
        IRubyObject arg0 = args[0];
        String host, port;

        if (arg0 instanceof RubyArray) {
            RubyArray ary = (RubyArray) arg0;
            final int len = ary.size();

            if (len < 3 || len > 4) {
                throw runtime.newArgumentError("array size should be 3 or 4, "+ len +" given");
            }

            // if array has 4 elements, third element is ignored
            port = ary.eltInternal(1).toString();
            host = len == 3 ? ary.eltInternal(2).toString() : ary.eltInternal(3).toString();

        } else if (arg0 instanceof RubyString) {
            String arg = ((RubyString) arg0).toString();
            Matcher m = STRING_IPV4_ADDRESS_PATTERN.matcher(arg);

            if (!m.matches()) {
                RubyArray portAndHost = Sockaddr.unpack_sockaddr_in(context, arg0);

                if (portAndHost.size() != 2) {
                    throw runtime.newArgumentError("invalid address representation");
                }

                port = portAndHost.eltInternal(0).toString();
                host = portAndHost.eltInternal(1).toString();

            } else if ((host = m.group(IPV4_HOST_GROUP)) == null || host.length() == 0 ||
                    (port = m.group(IPV4_PORT_GROUP)) == null || port.length() == 0) {

                throw runtime.newArgumentError("invalid address string");

            } else {

                // Try IPv6
                try {
                    InetAddress ipv6_addr = InetAddress.getByName(host);

                    if (ipv6_addr instanceof Inet6Address) {
                        host = ipv6_addr.getHostAddress();
                    }

                } catch (UnknownHostException uhe) {
                    throw runtime.newArgumentError("invalid address string");

                }
            }

        } else {
            throw runtime.newArgumentError("invalid args");

        }

        InetAddress addr;

        try {
            addr = InetAddress.getByName(host);

        } catch (UnknownHostException e) {
            throw sockerr(runtime, "unknown host: "+ host);

        }

        if ((flags & NI_NUMERICHOST.intValue()) == 0) {
            host = addr.getCanonicalHostName();

        } else {
            host = addr.getHostAddress();

        }

        Service serv = Service.getServiceByPort(Integer.parseInt(port), null);

        if (serv != null) {

            if ((flags & NI_NUMERICSERV.intValue()) == 0) {
                port = serv.getName();

            } else {
                port = Integer.toString(serv.getPort());

            }

        }

        return runtime.newArray(runtime.newString(host), runtime.newString(port));

    }

    public static IRubyObject ip_address_list(ThreadContext context) {
        Ruby runtime = context.runtime;

        try {
            RubyArray list = RubyArray.newArray(runtime);
            RubyClass addrInfoCls = runtime.getClass("Addrinfo");

            for (Enumeration<NetworkInterface> networkIfcs = NetworkInterface.getNetworkInterfaces(); networkIfcs.hasMoreElements() ; ) {
                for (Enumeration<InetAddress> addresses = networkIfcs.nextElement().getInetAddresses(); addresses.hasMoreElements() ; ) {
                    list.append(new Addrinfo(runtime, addrInfoCls, addresses.nextElement()));
                }
            }

            return list;
        } catch (SocketException se) {
            throw sockerr(runtime, se.getLocalizedMessage());
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

    public static IRubyObject getaddress(ThreadContext context, IRubyObject hostname) {
        try {
            return RubyString.newInternalFromJavaExternal(context.runtime, InetAddress.getByName(hostname.convertToString().toString()).getHostAddress());
        } catch(UnknownHostException e) {
            throw sockerr(context.runtime, "getaddress: name or service not known");
        }
    }

    public static RuntimeException sockerr(Ruby runtime, String msg) {
        return RaiseException.from(runtime, runtime.getClass("SocketError"), msg);
    }

    public static RuntimeException sockerr_with_trace(Ruby runtime, String msg, StackTraceElement[] trace) {
        String eol = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();
        sb.append(msg);
        for (int i = 0, il = trace.length; i < il; i++) {
            sb.append(eol).append(trace[i].toString());
        }
        return RaiseException.from(runtime, runtime.getClass("SocketError"), sb.toString());
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
                port = RubyNumeric.fix2int(RubySocket.getservbyname(
                        context, context.runtime.getObject(), new IRubyObject[]{portString}));
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
    static AddressFamily addressFamilyFromArg(IRubyObject domain) {
        IRubyObject maybeString = TypeConverter.checkStringType(domain.getRuntime(), domain);

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
            throw SocketUtils.sockerr(domain.getRuntime(), "invalid address family: " + domain);
        }
    }

    static Sock sockFromArg(IRubyObject type) {
        IRubyObject maybeString = TypeConverter.checkStringType(type.getRuntime(), type);

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
            throw SocketUtils.sockerr(type.getRuntime(), "invalid socket type: " + type);
        }
    }

    // MRI: protocol family part of rsock_family_to_int
    static ProtocolFamily protocolFamilyFromArg(IRubyObject protocol) {
        IRubyObject maybeString = TypeConverter.checkStringType(protocol.getRuntime(), protocol);

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
            throw SocketUtils.sockerr(protocol.getRuntime(), "invalid protocol family: " + protocol);
        }
    }

    static Protocol protocolFromArg(IRubyObject protocol) {
        IRubyObject maybeString = TypeConverter.checkStringType(protocol.getRuntime(), protocol);

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
            throw SocketUtils.sockerr(protocol.getRuntime(), "invalid protocol: " + protocol);
        }
    }

    static SocketLevel levelFromArg(IRubyObject level) {
        IRubyObject maybeString = TypeConverter.checkStringType(level.getRuntime(), level);

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
            throw SocketUtils.sockerr(level.getRuntime(), "invalid socket level: " + level);
        }
    }

    static SocketOption optionFromArg(IRubyObject opt) {
        IRubyObject maybeString = TypeConverter.checkStringType(opt.getRuntime(), opt);

        if (!maybeString.isNil()) {
            opt = maybeString;
        }

        try {
            if (opt instanceof RubyString || opt instanceof RubySymbol) {
                String optString = opt.toString();
                if (optString.startsWith("SO_")) return SocketOption.valueOf(optString);
                return SocketOption.valueOf("SO_" + optString);
            }

            return SocketOption.valueOf(RubyNumeric.fix2int(opt));
        } catch (IllegalArgumentException iae) {
            throw SocketUtils.sockerr(opt.getRuntime(), "invalid socket option: " + opt);
        }
    }

    public static int portToInt(IRubyObject port) {
        if (port.isNil()) return 0;

        Ruby runtime = port.getRuntime();

        IRubyObject maybeStr = TypeConverter.checkStringType(runtime, port);
        if (!maybeStr.isNil()) {
            RubyString portStr = maybeStr.convertToString();
            Service serv = Service.getServiceByName(portStr.toString(), null);

            if (serv != null) return serv.getPort();

            return RubyNumeric.fix2int(portStr.to_i());
        }
        return RubyNumeric.fix2int(port);
    }
}
