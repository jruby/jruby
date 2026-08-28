package org.jruby.util.io;

import jnr.constants.platform.AddressFamily;
import jnr.netdb.Service;
import jnr.unixsocket.UnixSocketAddress;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyNumeric;
import org.jruby.RubyBoolean;
import org.jruby.api.Access;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.socket.Addrinfo;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import org.jruby.RubyString;
import org.jruby.ext.socket.SocketUtils;
import org.jruby.ext.socket.SocketUtilsIPV6;
import org.jruby.runtime.Helpers;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Create.*;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;

public class Sockaddr {

    public static InetAddress addressFromString(Ruby runtime, String s) {
        try {
            byte[] bs = ByteList.plain(s);
            return InetAddress.getByAddress(bs);
        } catch(Exception e) {
            throw sockerr(runtime.getCurrentContext(), "strtoaddr: " + e.toString());
        }
    }

    public static String stringFromAddress(Ruby runtime, InetAddress as) {
        try {
            return new String(ByteList.plain(as.getAddress()));
        } catch(Exception e) {
            throw sockerr(runtime.getCurrentContext(), "addrtostr: " + e.toString());
        }
    }

    public static InetSocketAddress addressFromArg(ThreadContext context, IRubyObject arg) {
        InetSocketAddress iaddr;
        if (arg instanceof Addrinfo addrinfo) {
            if (!addrinfo.ip_p(context).isTrue()) throw typeError(context, "not an INET or INET6 address: " + addrinfo);
            iaddr = new InetSocketAddress(addrinfo.getInetAddress(), addrinfo.getPort());
        } else {
            iaddr = addressFromSockaddr_in(context, arg);
        }

        return iaddr;
    }

    public static InetSocketAddress addressFromSockaddr_in(ThreadContext context, IRubyObject arg) {
        ByteList val = arg.convertToString().getByteList();
        return addressFromSockaddr_in(context, val);
    }

    public static InetSocketAddress addressFromSockaddr_in(ThreadContext context, ByteList val) {
        RubyArray sockaddr = unpack_sockaddr_in(context, val);

        IRubyObject addr = sockaddr.pop(context);
        IRubyObject _port = sockaddr.pop(context);

        return new InetSocketAddress(
                addr.convertToString().toString(), SocketUtils.portToInt(context, _port));
    }

    public static SocketAddress addressFromSockaddr(ThreadContext context, IRubyObject arg) {
        ByteList val = arg.convertToString().getByteList();
        AddressFamily af = getAddressFamilyFromSockaddr(context, val);

        switch (af) {
            case AF_UNIX:
                return addressFromSockaddr_un(context, val);
            case AF_INET:
            case AF_INET6:
                return addressFromSockaddr_in(context, val);
            default:
                throw argumentError(context, "can't resolve socket address of wrong type");

        }
    }

    public static UnixSocketAddress addressFromSockaddr_un(ThreadContext context, IRubyObject arg) {
        ByteList val = arg.convertToString().getByteList();

        return addressFromSockaddr_un(context, val);
    }

    public static UnixSocketAddress addressFromSockaddr_un(ThreadContext context, ByteList bl) {
        RubyString pathStr = pathFromSockaddr_un(context, bl.bytes());

        return new UnixSocketAddress(new File(pathStr.toString()));
    }

    public static IRubyObject packSockaddrFromAddress(ThreadContext context, InetSocketAddress sock) {
        if (sock == null) {
            return pack_sockaddr_in(context, 0, "");
        } else {
            return pack_sockaddr_in(context, sock);
        }
    }

    public static IRubyObject pack_sockaddr_in(ThreadContext context, IRubyObject port, IRubyObject host) {
        int portNum;
        if (port.isNil()) {
            portNum = 0;
        } else if (port instanceof RubyString) {
            String portString = port.asJavaString();
            try {
                portNum = Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                Service service = Service.getServiceByName(portString, "tcp"); // FIXME: is tcp safe here?
                if (service == null) throw sockerr(context, "getaddrinfo: Servname not supported for ai_socktype");
                portNum = service.getPort();
            }
        } else {
            portNum = toInt(context, port);
        }

        final String hostStr = host.isNil() ? null : host.convertToString().toString();
        return pack_sockaddr_in(context, portNum, hostStr);
    }

    public static IRubyObject pack_sockaddr_in(ThreadContext context, int port, String host) {
        ByteArrayOutputStream bufS = new ByteArrayOutputStream();

        try {
            DataOutputStream ds = new DataOutputStream(bufS);

            try {
                if ( host != null && host.length() == 0 ) {
                    writeSockaddrHeader(AddressFamily.AF_INET, ds);
                    writeSockaddrPort(ds, port);
                    ds.writeInt(0);
                } else {
                    InetAddress[] addrs = InetAddress.getAllByName(host);
                    byte[] addr = addrs[0].getAddress();

                    if (addr.length == 4) {
                        writeSockaddrHeader(AddressFamily.AF_INET, ds);
                    } else {
                        writeSockaddrHeader(AddressFamily.AF_INET6, ds);
                    }

                    writeSockaddrPort(ds, port);

                    ds.write(addr, 0, addr.length);
                }
            }
            catch (UnknownHostException e) {
                throw sockerr(context, "getaddrinfo: No address associated with nodename");
            }

            writeSockaddrFooter(ds);
        }
        catch (IOException e) {
            throw sockerr(context, "pack_sockaddr_in: internal error");
        }

        return newString(context, new ByteList(bufS.toByteArray(), false));
    }

    public static IRubyObject pack_sockaddr_in(ThreadContext context, InetSocketAddress sock) {
        ByteArrayOutputStream bufS = new ByteArrayOutputStream();

        try {
            DataOutputStream ds = new DataOutputStream(bufS);

            InetAddress inet = sock.getAddress();
            if (inet instanceof Inet4Address) {
                writeSockaddrHeader(AddressFamily.AF_INET, ds);
            } else {
                writeSockaddrHeader(AddressFamily.AF_INET6, ds);
            }
            writeSockaddrPort(ds, sock);

            String host = sock.getAddress().getHostAddress();

            if ("".equals(host)) {
                ds.writeInt(0);

            } else {
                byte[] addr = sock.getAddress().getAddress();
                ds.write(addr, 0, addr.length);

            }

            writeSockaddrFooter(ds);

        } catch (IOException e) {
            throw sockerr(context, "pack_sockaddr_in: internal error");

        }

        return newString(context, new ByteList(bufS.toByteArray(), false));
    }

    public static RubyArray unpack_sockaddr_in(ThreadContext context, IRubyObject addr) {
        if (addr instanceof Addrinfo addrinfo) {
            if (((RubyBoolean)addrinfo.ip_p(context)).isFalse()) {
                throw argumentError(context, "not an AF_INET/AF_INET6 sockaddr");
            }

            return newArray(context, addrinfo.ip_port(context), addrinfo.ip_address(context));
        }

        return unpack_sockaddr_in(context, addr.convertToString().getByteList());
    }

    public static RubyArray unpack_sockaddr_in(ThreadContext context, ByteList val) {
        AddressFamily af = getAddressFamilyFromSockaddr(context, val);

        if (af != AddressFamily.AF_INET && af != AddressFamily.AF_INET6) {
            throw argumentError(context, "not an AF_INET/AF_INET6 sockaddr");
        }

        int port = ((val.get(2)&0xff) << 8) + (val.get(3)&0xff);

        final StringBuilder formatAddr = new StringBuilder();
        RubyString ip;

        if (af == AddressFamily.AF_INET) {
            formatAddr.append(val.get(4) & 0xff)
                      .append('.')
                      .append(val.get(5) & 0xff)
                      .append('.')
                      .append(val.get(6) & 0xff)
                      .append('.')
                      .append(val.get(7) & 0xff);
            ip = newString(context, formatAddr.toString());
        } else {                                    // if af == AddressFamily.AF_INET6
            for (int i = 4; i <= 19; i++) {
                if (i != 4 && i % 2 == 0) formatAddr.append(':');
                formatAddr.append(Integer.toHexString(val.get(i) & 0xff | 0x100).substring(1));
            }
            ip = newString(context, SocketUtilsIPV6.getIPV6Address(formatAddr.toString()));
        }

        return newArray(context, asFixnum(context, port), ip);
    }

    public static IRubyObject pack_sockaddr_un(ThreadContext context, String unixpath) {
        ByteBuffer buf = ByteBuffer.allocate(SOCKADDR_UN_SIZE);
        byte[] path = unixpath.getBytes();

        if (path.length > SOCKADDR_UN_PATH) {
            String errorMsg = "too long unix socket path (%d bytes given but %d bytes max)";
            throw argumentError(context, String.format(errorMsg, path.length, SOCKADDR_UN_PATH));
        }

        int afamily = AddressFamily.AF_UNIX.intValue();
        int high = (afamily & 0xff00) >> 8;
        int low = afamily & 0xff;
        buf.put((byte)high);
        buf.put((byte)low);
        buf.put(path);

        return newString(context, new ByteList(buf.array()));
    }

    public static IRubyObject unpack_sockaddr_un(ThreadContext context, IRubyObject addr) {
        if (addr instanceof Addrinfo) {
            Addrinfo addrinfo = (Addrinfo)addr;

            if (((RubyBoolean)addrinfo.unix_p(context)).isFalse()) {
                throw argumentError(context, "not an AF_UNIX sockaddr");
            }
            return addrinfo.unix_path(context);
        }

        ByteList val = addr.convertToString().getByteList();
        AddressFamily af = getAddressFamilyFromSockaddr(context, val);

        if (af != AddressFamily.AF_UNIX) throw argumentError(context, "not an AF_UNIX sockaddr");

        return pathFromSockaddr_un(context, val.bytes());
    }

    public static void writeSockaddrHeader(AddressFamily family, DataOutputStream ds) throws IOException {
        int value = family.intValue();
        int high = (value & 0xff00) >> 8;
        int low = value & 0xff;

        ds.write((byte)high);
        ds.write((byte)low);
    }

    public static void writeSockaddrFooter(DataOutputStream ds) throws IOException {
        ds.writeInt(0);
        ds.writeInt(0);
    }

    public static void writeSockaddrPort(DataOutputStream ds, InetSocketAddress sockaddr) throws IOException {
        writeSockaddrPort(ds, sockaddr.getPort());
    }

    public static void writeSockaddrPort(DataOutputStream ds, int port) throws IOException {
        ds.write(port >> 8);
        ds.write(port);
    }

    public static AddressFamily getAddressFamilyFromSockaddr(ThreadContext context, ByteList val) {
        if (val.length() < 2) throw argumentError(context, "too short sockaddr");

        int high = val.get(0) & 0xff;
        int low = val.get(1) & 0xff;

        return AddressFamily.valueOf((high << 8) + low);
    }

    @Deprecated(since = "10.0.0.0")
    public static AddressFamily getAddressFamilyFromSockaddr(Ruby runtime, ByteList val) {
        return getAddressFamilyFromSockaddr(runtime.getCurrentContext(), val);
    }

    private static RuntimeException sockerr(ThreadContext context, String msg) {
        return RaiseException.from(context.runtime, Access.getClass(context, "SocketError"), msg);
    }

    @Deprecated(since = "10.0.0.0")
    public static SocketAddress sockaddrFromBytes(Ruby runtime, byte[] val) throws IOException {
        return sockaddrFromBytes(runtime.getCurrentContext(), val);
    }

    public static SocketAddress sockaddrFromBytes(ThreadContext context, byte[] val) throws IOException {
        AddressFamily afamily = AddressFamily.valueOf(uint16(val[0], val[1]));

        if (afamily == null || afamily == AddressFamily.__UNKNOWN_CONSTANT__) {
            throw argumentError(context, "can't resolve socket address of wrong type");
        }

        int port;
        switch (afamily) {
            case AF_INET:
                port = uint16(val[2], val[3]);
                Inet4Address inet4Address = (Inet4Address)InetAddress.getByAddress(Helpers.subseq(val, 4, 4));
                return new InetSocketAddress(inet4Address, port);
            case AF_INET6:
                port = uint16(val[2], val[3]);
                Inet6Address inet6Address = (Inet6Address)InetAddress.getByAddress(Helpers.subseq(val, 4, 16));
                return new InetSocketAddress(inet6Address, port);
            case AF_UNIX:
                String path = new String(val, 2, val.length - 2);
                return new UnixSocketAddress(new File(path));
            default:
                throw argumentError(context, "can't resolve socket address of wrong type");
        }
    }

    private static int uint16(byte high, byte low) {
        return ((high & 0xFF) << 8) + (low & 0xFF);
    }

    private static RubyString pathFromSockaddr_un(ThreadContext context, byte[] raw) {
        int end = 2;
        for (; end < raw.length; end++) {
            if (raw[end] == 0) break;
        }

        return newString(context, raw, 2, (end - 2));
    }

    // sizeof(sockaddr_un) on Linux
    // 2 bytes sun_family + 108 bytes sun_path
    private static final int SOCKADDR_UN_PATH = 108;
    private static final int SOCKADDR_UN_SIZE = 2 + SOCKADDR_UN_PATH;
}
