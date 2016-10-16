package org.jruby.util.io;

import jnr.constants.platform.AddressFamily;
import jnr.unixsocket.UnixSocketAddress;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyNumeric;
import org.jruby.RubyBoolean;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.socket.Addrinfo;
import org.jruby.platform.Platform;
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
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import org.jruby.RubyString;
import org.jruby.ext.socket.SocketUtils;
import org.jruby.ext.socket.SocketUtilsIPV6;
import org.jruby.runtime.Helpers;

public class Sockaddr {

    public static InetAddress addressFromString(Ruby runtime, String s) {
        try {
            byte[] bs = ByteList.plain(s);
            return InetAddress.getByAddress(bs);
        } catch(Exception e) {
            throw sockerr(runtime, "strtoaddr: " + e.toString());
        }
    }

    public static String stringFromAddress(Ruby runtime, InetAddress as) {
        try {
            return new String(ByteList.plain(as.getAddress()));
        } catch(Exception e) {
            throw sockerr(runtime, "addrtostr: " + e.toString());
        }
    }

    public static InetSocketAddress addressFromArg(ThreadContext context, IRubyObject arg) {
        InetSocketAddress iaddr;
        if (arg instanceof Addrinfo) {
            Addrinfo addrinfo = (Addrinfo)arg;
            if (!addrinfo.ip_p(context).isTrue()) {
                throw context.runtime.newTypeError("not an INET or INET6 address: " + addrinfo);
            }
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
        RubyArray sockaddr = (RubyArray) unpack_sockaddr_in(context, val);

        IRubyObject addr = sockaddr.pop(context);
        IRubyObject _port = sockaddr.pop(context);
        int port = SocketUtils.portToInt(_port);

        return new InetSocketAddress(
                addr.convertToString().toString(), port);
    }

    public static SocketAddress addressFromSockaddr(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;

        ByteList val = arg.convertToString().getByteList();

        AddressFamily af = getAddressFamilyFromSockaddr(runtime, val);

        switch (af) {
            case AF_UNIX:
                return addressFromSockaddr_un(context, val);
            case AF_INET:
            case AF_INET6:
                return addressFromSockaddr_in(context, val);
            default:
                throw runtime.newArgumentError("can't resolve socket address of wrong type");

        }
    }

    public static UnixSocketAddress addressFromSockaddr_un(ThreadContext context, IRubyObject arg) {
        ByteList val = arg.convertToString().getByteList();

        return addressFromSockaddr_un(context, val);
    }

    public static UnixSocketAddress addressFromSockaddr_un(ThreadContext context, ByteList bl) {
        String pathStr = pathFromSockaddr_un(context, bl.bytes());

        return new UnixSocketAddress(new File(pathStr));
    }

    public static IRubyObject packSockaddrFromAddress(ThreadContext context, InetSocketAddress sock) {
        if (sock == null) {
            return pack_sockaddr_in(context, 0, "");
        } else {
            return pack_sockaddr_in(context, sock);
        }
    }

    public static IRubyObject pack_sockaddr_in(ThreadContext context, IRubyObject port, IRubyObject host) {
        final int portNum;
        if ( ! port.isNil() ) {
            portNum = port instanceof RubyString ?
                    Integer.parseInt(port.convertToString().toString()) :
                        RubyNumeric.fix2int(port);
        }
        else {
            portNum = 0;
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
                throw sockerr(context.runtime, "getaddrinfo: No address associated with nodename");
            }

            writeSockaddrFooter(ds);
        }
        catch (IOException e) {
            throw sockerr(context.runtime, "pack_sockaddr_in: internal error");
        }

        return context.runtime.newString(new ByteList(bufS.toByteArray(),
                false));
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

            if(host != null && "".equals(host)) {
                ds.writeInt(0);

            } else {
                byte[] addr = sock.getAddress().getAddress();
                ds.write(addr, 0, addr.length);

            }

            writeSockaddrFooter(ds);

        } catch (IOException e) {
            throw sockerr(context.runtime, "pack_sockaddr_in: internal error");

        }

        return context.runtime.newString(new ByteList(bufS.toByteArray(),
                false));
    }

    public static RubyArray unpack_sockaddr_in(ThreadContext context, IRubyObject addr) {
        final Ruby runtime = context.runtime;

        if (addr instanceof Addrinfo) {
            Addrinfo addrinfo = (Addrinfo)addr;

            if (((RubyBoolean)addrinfo.ip_p(context)).isFalse()) {
                throw runtime.newArgumentError("not an AF_INET/AF_INET6 sockaddr");
            }

            return RubyArray.newArray(runtime, addrinfo.ip_port(context),
                                      addrinfo.ip_address(context));
        }

        ByteList val = addr.convertToString().getByteList();

        return unpack_sockaddr_in(context, val);
    }

    public static RubyArray unpack_sockaddr_in(ThreadContext context, ByteList val) {
        final Ruby runtime = context.runtime;

        AddressFamily af = getAddressFamilyFromSockaddr(runtime, val);

        if (af != AddressFamily.AF_INET &&
            af != AddressFamily.AF_INET6) {
            throw runtime.newArgumentError("not an AF_INET/AF_INET6 sockaddr");
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
            ip = RubyString.newString(runtime, formatAddr);
        } else {                                    // if af == AddressFamily.AF_INET6
            for (int i = 4; i <= 19; i++) {
                if (i != 4 && i % 2 == 0) formatAddr.append(':');
                formatAddr.append(Integer.toHexString(val.get(i) & 0xff | 0x100).substring(1));
            }
            ip = RubyString.newString(runtime,
                                      SocketUtilsIPV6.getIPV6Address(formatAddr.toString()));
        }

        return RubyArray.newArray(runtime, runtime.newFixnum(port), ip);
    }

    public static IRubyObject pack_sockaddr_un(ThreadContext context, String unixpath) {
        final Ruby runtime = context.runtime;

        ByteBuffer buf = ByteBuffer.allocate(SOCKADDR_UN_SIZE);

        byte[] path = unixpath.getBytes();

        if (path.length > SOCKADDR_UN_PATH) {
            String errorMsg = "too long unix socket path (%d bytes given but %d bytes max)";
            String formattedErrorMsg = String.format(errorMsg, path.length, SOCKADDR_UN_PATH);
            throw runtime.newArgumentError(formattedErrorMsg);
        }

        int afamily = AddressFamily.AF_UNIX.intValue();
        int high = (afamily & 0xff00) >> 8;
        int low = afamily & 0xff;
        buf.put((byte)high);
        buf.put((byte)low);
        buf.put(path);

        return RubyString.newString(runtime, buf.array());
    }

    public static IRubyObject unpack_sockaddr_un(ThreadContext context, IRubyObject addr) {
        final Ruby runtime = context.runtime;

        if (addr instanceof Addrinfo) {
            Addrinfo addrinfo = (Addrinfo)addr;

            if (((RubyBoolean)addrinfo.unix_p(context)).isFalse()) {
                throw runtime.newArgumentError("not an AF_UNIX sockaddr");
            }
            return addrinfo.unix_path(context);
        }

        ByteList val = addr.convertToString().getByteList();
        AddressFamily af = getAddressFamilyFromSockaddr(runtime, val);

        if (af != AddressFamily.AF_UNIX) {
            throw runtime.newArgumentError("not an AF_UNIX sockaddr");
        }

        String filename = pathFromSockaddr_un(context, val.bytes());
        return context.runtime.newString(filename);
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

    public static AddressFamily getAddressFamilyFromSockaddr(Ruby runtime, ByteList val) {
        if (val.length() < 2) {
            throw runtime.newArgumentError("too short sockaddr");
        }

        int high = val.get(0) & 0xff;
        int low = val.get(1) & 0xff;

        return AddressFamily.valueOf((high << 8) + low);
    }

    private static RuntimeException sockerr(Ruby runtime, String msg) {
        return new RaiseException(runtime, runtime.getClass("SocketError"), msg, true);
    }

    public static SocketAddress sockaddrFromBytes(Ruby runtime, byte[] val) throws IOException {
        AddressFamily afamily = AddressFamily.valueOf(uint16(val[0], val[1]));

        if (afamily == null || afamily == AddressFamily.__UNKNOWN_CONSTANT__) {
            throw runtime.newArgumentError("can't resolve socket address of wrong type");
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
                throw runtime.newArgumentError("can't resolve socket address of wrong type");
        }
    }

    private static int uint16(byte high, byte low) {
        return ((high & 0xFF) << 8) + (low & 0xFF);
    }

    private static String pathFromSockaddr_un(ThreadContext context, byte[] raw) {
        int end = 2;
        for (; end < raw.length; end++) {
            if (raw[end] == 0) break;
        }

        return new String(raw, 2, (end - 2));
    }

    // sizeof(sockaddr_un) on Linux
    // 2 bytes sun_family + 108 bytes sun_path
    private static final int SOCKADDR_UN_PATH = 108;
    private static final int SOCKADDR_UN_SIZE = 2 + SOCKADDR_UN_PATH;
}
