package org.jruby.util.io;

import jnr.constants.platform.AddressFamily;
import jnr.unixsocket.UnixSocketAddress;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyNumeric;
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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import org.jruby.RubyString;
import org.jruby.ext.socket.SocketUtils;
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

    public static InetSocketAddress addressFromSockaddr_in(ThreadContext context, IRubyObject arg) {
        RubyArray sockaddr = (RubyArray) unpack_sockaddr_in(context, arg);

        IRubyObject addr = sockaddr.pop(context);
        IRubyObject _port = sockaddr.pop(context);
        int port = SocketUtils.portToInt(_port);

        return new InetSocketAddress(
                addr.convertToString().toString(), port);
    }

    public static InetSocketAddress addressFromArg(ThreadContext context, IRubyObject arg) {
        InetSocketAddress iaddr;
        if (arg instanceof Addrinfo) {
            Addrinfo addrinfo = (Addrinfo)arg;
            iaddr = new InetSocketAddress(addrinfo.getInetAddress(), addrinfo.getPort());
        } else {
            iaddr = addressFromSockaddr_in(context, arg);
        }

        return iaddr;
    }

    public static UnixSocketAddress addressFromSockaddr_un(ThreadContext context, IRubyObject arg) {
        ByteList bl = arg.convertToString().getByteList();
        byte[] raw = bl.bytes();

        int end = 2;
        for (; end < raw.length; end++) {
            if (raw[end] == 0) break;
        }

        ByteList path = new ByteList(raw, 2, end, false);
        String pathStr = Helpers.decodeByteList(context.runtime, path);

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

            writeSockaddrHeader(AddressFamily.AF_INET, ds);
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
        ByteList val = addr.convertToString().getByteList();

        AddressFamily af = getAddressFamilyFromSockaddr(runtime, val);

        if (af != AddressFamily.AF_INET &&
                af != AddressFamily.AF_INET6) {
            throw runtime.newArgumentError("can't resolve socket address of wrong type");
        }

        int port = ((val.get(2)&0xff) << 8) + (val.get(3)&0xff);

        final StringBuilder formatAddr = new StringBuilder();

        if (af == AddressFamily.AF_INET) {
            formatAddr.append(val.get(4) & 0xff)
                      .append('.')
                      .append(val.get(5) & 0xff)
                      .append('.')
                      .append(val.get(6) & 0xff)
                      .append('.')
                      .append(val.get(7) & 0xff);

        } else {                                    // if af == AddressFamily.AF_INET6
            for (int i = 4; i <= 19; i++) {
                if (i != 4 && i % 2 == 0) formatAddr.append(':');
                formatAddr.append(Integer.toHexString(val.get(i) & 0xff | 0x100).substring(1));
            }
        }

        return RubyArray.newArray(runtime, runtime.newFixnum(port), RubyString.newString(runtime, formatAddr));
    }

    public static IRubyObject pack_sockaddr_un(ThreadContext context, IRubyObject filename) {
        ByteList str = filename.convertToString().getByteList();

        AddressFamily af = AddressFamily.AF_UNIX;
        int high = (af.intValue() & 0xff00) >> 8;
        int low = af.intValue() & 0xff;

        ByteList bl = new ByteList();
        bl.append((byte)high);
        bl.append((byte)low);
        bl.append(str);

        for(int i=str.length();i<104;i++) {
            bl.append((byte)0);
        }

        return context.runtime.newString(bl);
    }

    public static IRubyObject unpack_sockaddr_un(ThreadContext context, IRubyObject addr) {
        final Ruby runtime = context.runtime;
        ByteList val = addr.convertToString().getByteList();

        AddressFamily af = getAddressFamilyFromSockaddr(runtime, val);

        if (af != AddressFamily.AF_UNIX) {
            throw runtime.newArgumentError("not an AF_UNIX sockaddr");
        }

        String filename = Sockaddr.addressFromSockaddr_un(context, addr).path();
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
}
