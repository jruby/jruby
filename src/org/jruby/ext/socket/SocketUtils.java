/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.ext.socket;

import jnr.netdb.Service;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyFixnum;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.io.Sockaddr;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    @JRubyMethod(meta = true)
    public static IRubyObject gethostname(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.runtime;

        try {
            return runtime.newString(InetAddress.getLocalHost().getHostName());

        } catch(UnknownHostException e) {

            try {
                return runtime.newString(InetAddress.getByAddress(new byte[]{0,0,0,0}).getHostName());

            } catch(UnknownHostException e2) {
                throw RubySocket.sockerr(runtime, "gethostname: name or service not known");

            }
        }
    }

    @JRubyMethod(required = 1, rest = true, meta = true)
    public static IRubyObject gethostbyaddr(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        IRubyObject[] ret = new IRubyObject[4];

        ret[0] = runtime.newString(Sockaddr.addressFromString(runtime, args[0].convertToString().toString()).getCanonicalHostName());
        ret[1] = runtime.newArray();
        ret[2] = runtime.newFixnum(2); // AF_INET
        ret[3] = args[0];

        return runtime.newArrayNoCopy(ret);
    }

    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject getservbyname(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
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
                throw RubySocket.sockerr(runtime, "no such service " + name + "/" + proto);

            }

        }

        return runtime.newFixnum(port);
    }

    @JRubyMethod(name = {"pack_sockaddr_in", "sockaddr_in"}, meta = true)
    public static IRubyObject pack_sockaddr_in(ThreadContext context, IRubyObject recv, IRubyObject port, IRubyObject host) {
        int portNum = port instanceof RubyString ?
                Integer.parseInt(port.convertToString().toString()) :
                RubyNumeric.fix2int(port);

        return Sockaddr.pack_sockaddr_in(
                context,
                portNum,
                host.isNil() ? null : host.convertToString().toString());
    }

    @JRubyMethod(meta = true)
    public static IRubyObject unpack_sockaddr_in(ThreadContext context, IRubyObject recv, IRubyObject addr) {
        return Sockaddr.unpack_sockaddr_in(context, addr);
    }

    @JRubyMethod(name = {"pack_sockaddr_un", "sockaddr_un"}, meta = true)
    public static IRubyObject pack_sockaddr_un(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        String str = filename.convertToString().toString();

        StringBuilder sb = new StringBuilder()
                .append((char)0)
                .append((char) 1)
                .append(str);

        for(int i=str.length();i<104;i++) {
            sb.append((char)0);
        }

        return context.runtime.newString(sb.toString());
    }

    @JRubyMethod(meta = true)
    public static IRubyObject gethostbyname(ThreadContext context, IRubyObject recv, IRubyObject hostname) {
        Ruby runtime = context.runtime;

        try {
            InetAddress addr = getRubyInetAddress(hostname.convertToString().getByteList());
            IRubyObject[] ret = new IRubyObject[4];

            ret[0] = runtime.newString(addr.getCanonicalHostName());
            ret[1] = runtime.newArray();
            ret[2] = runtime.newFixnum(2); // AF_INET
            ret[3] = runtime.newString(new ByteList(addr.getAddress()));
            return runtime.newArrayNoCopy(ret);

        } catch(UnknownHostException e) {
            throw RubySocket.sockerr(runtime, "gethostbyname: name or service not known");

        }
    }

    public static InetAddress getRubyInetAddress(ByteList address) throws UnknownHostException {
        if (address.equal(BROADCAST)) {
            return InetAddress.getByAddress(INADDR_BROADCAST);

        } else if (address.equal(ANY)) {
            return InetAddress.getByAddress(INADDR_ANY);

        } else {
            return InetAddress.getByName(address.toString());

        }
    }

    /**
     * Ruby definition would look like:
     *
     * def self.getaddrinfo(host, port, family = nil, socktype = nil, protocol = nil, flags = nil)
     */
    @JRubyMethod(required = 2, optional = 4, meta = true)
    public static IRubyObject getaddrinfo(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        IRubyObject host = args[0];
        IRubyObject port = args[1];
        boolean emptyHost = host.isNil() || host.convertToString().isEmpty();

        try {
            if(port instanceof RubyString) {
                port = getservbyname(context, recv, new IRubyObject[]{port});
            }

            IRubyObject family = args.length > 2 ? args[2] : context.nil;
            IRubyObject socktype = args.length > 3 ? args[3] : context.nil;
            //IRubyObject protocol = args[4];
            IRubyObject flags = args.length > 5 ? args[5] : context.nil;

            boolean is_ipv6 = (! family.isNil()) && (RubyNumeric.fix2int(family) & AF_INET6.intValue()) == AF_INET6.intValue();
            boolean sock_stream = true;
            boolean sock_dgram = true;

            if(!socktype.isNil()) {
                int val = RubyNumeric.fix2int(socktype);

                if(val == SOCK_STREAM.intValue()) {
                    sock_dgram = false;

                } else if(val == SOCK_DGRAM.intValue()) {
                    sock_stream = false;

                }
            }

            // When Socket::AI_PASSIVE and host is nil, return 'any' address.
            InetAddress[] addrs = null;

            if(!flags.isNil() && RubyFixnum.fix2int(flags) > 0) {
                // The value of 1 is for Socket::AI_PASSIVE.
                int flag = RubyNumeric.fix2int(flags);

                if ((flag == 1) && emptyHost ) {
                    // use RFC 2732 style string to ensure that we get Inet6Address
                    addrs = InetAddress.getAllByName(is_ipv6 ? "[::]" : "0.0.0.0");
                }

            }

            if (addrs == null) {
                addrs = InetAddress.getAllByName(emptyHost ? (is_ipv6 ? "[::1]" : null) : host.convertToString().toString());
            }

            List<IRubyObject> l = new ArrayList<IRubyObject>();

            for(int i = 0; i < addrs.length; i++) {
                IRubyObject[] c;

                if(sock_dgram) {
                    c = new IRubyObject[7];
                    c[0] = runtime.newString(is_ipv6 ? "AF_INET6" : "AF_INET");
                    c[1] = port;
                    c[2] = runtime.newString(getHostAddress(recv, addrs[i]));
                    c[3] = runtime.newString(addrs[i].getHostAddress());
                    c[4] = runtime.newFixnum(is_ipv6 ? PF_INET6 : PF_INET);
                    c[5] = runtime.newFixnum(SOCK_DGRAM);
                    c[6] = runtime.newFixnum(IPPROTO_UDP);
                    l.add(runtime.newArrayNoCopy(c));
                }

                if(sock_stream) {
                    c = new IRubyObject[7];
                    c[0] = runtime.newString(is_ipv6 ? "AF_INET6" : "AF_INET");
                    c[1] = port;
                    c[2] = runtime.newString(getHostAddress(recv, addrs[i]));
                    c[3] = runtime.newString(addrs[i].getHostAddress());
                    c[4] = runtime.newFixnum(is_ipv6 ? PF_INET6 : PF_INET);
                    c[5] = runtime.newFixnum(SOCK_STREAM);
                    c[6] = runtime.newFixnum(IPPROTO_TCP);
                    l.add(runtime.newArrayNoCopy(c));
                }
            }

            return runtime.newArray(l);

        } catch(UnknownHostException e) {
            throw RubySocket.sockerr(runtime, "getaddrinfo: name or service not known");

        }
    }

    private static String getHostAddress(IRubyObject recv, InetAddress addr) {
        return RubyBasicSocket.do_not_reverse_lookup(recv).isTrue() ? addr.getHostAddress() : addr.getCanonicalHostName();
    }

    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject getnameinfo(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        int flags = args.length == 2 ? RubyNumeric.num2int(args[1]) : 0;
        IRubyObject arg0 = args[0];
        String host, port;

        if (arg0 instanceof RubyArray) {
            List list = ((RubyArray)arg0).getList();
            int len = list.size();

            if (len < 3 || len > 4) {
                throw runtime.newArgumentError("array size should be 3 or 4, "+len+" given");
            }

            // if array has 4 elements, third element is ignored
            host = list.size() == 3 ? list.get(2).toString() : list.get(3).toString();
            port = list.get(1).toString();

        } else if (arg0 instanceof RubyString) {
            String arg = ((RubyString)arg0).toString();
            Matcher m = STRING_IPV4_ADDRESS_PATTERN.matcher(arg);

            if (!m.matches()) {
                IRubyObject obj = unpack_sockaddr_in(context, recv, arg0);

                if (obj instanceof RubyArray) {
                    List list = ((RubyArray)obj).getList();
                    int len = list.size();

                    if (len != 2) {
                        throw runtime.newArgumentError("invalid address representation");
                    }

                    host = list.get(1).toString();
                    port = list.get(0).toString();

                } else {
                    throw runtime.newArgumentError("invalid address string");

                }

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
            throw RubySocket.sockerr(runtime, "unknown host: "+ host);

        }

        if ((flags & NI_NUMERICHOST.intValue()) == 0) {
            host = addr.getCanonicalHostName();

        } else {
            host = addr.getHostAddress();

        }

        jnr.netdb.Service serv = jnr.netdb.Service.getServiceByPort(Integer.parseInt(port), null);

        if (serv != null) {

            if ((flags & NI_NUMERICSERV.intValue()) == 0) {
                port = serv.getName();

            } else {
                port = Integer.toString(serv.getPort());

            }

        }

        return runtime.newArray(runtime.newString(host), runtime.newString(port));

    }

    private static final Pattern STRING_IPV4_ADDRESS_PATTERN =
            Pattern.compile("((.*)\\/)?([\\.0-9]+)(:([0-9]+))?");

    private static final int IPV4_HOST_GROUP = 3;
    private static final int IPV4_PORT_GROUP = 5;

    private static final ByteList BROADCAST = new ByteList("<broadcast>".getBytes());
    private static final byte[] INADDR_BROADCAST = new byte[] {-1,-1,-1,-1}; // 255.255.255.255
    private static final ByteList ANY = new ByteList("<any>".getBytes());
    private static final byte[] INADDR_ANY = new byte[] {0,0,0,0}; // 0.0.0.0
}
