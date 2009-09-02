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
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
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

import com.kenai.constantine.platform.AddressFamily;
import static com.kenai.constantine.platform.AddressFamily.*;
import static com.kenai.constantine.platform.ProtocolFamily.*;
import com.kenai.constantine.platform.Sock;
import static com.kenai.constantine.platform.Sock.*;
import static com.kenai.constantine.platform.NameInfo.*;
import static com.kenai.constantine.platform.IPProto.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyIO;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.platform.Platform;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import org.jruby.util.ByteList;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.InvalidValueException;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
@JRubyClass(name="Socket", parent="BasicSocket", include="Socket::Constants")
public class RubySocket extends RubyBasicSocket {
    @JRubyClass(name="SocketError", parent="StandardError")
    public static class SocketError {}

    public static class Service implements Library {
        public void load(final Ruby runtime, boolean wrap) throws IOException {
            runtime.defineClass("SocketError", runtime.getStandardError(), runtime.getStandardError().getAllocator());
            RubyBasicSocket.createBasicSocket(runtime);
            RubySocket.createSocket(runtime);

            if(runtime.getInstanceConfig().nativeEnabled && RubyUNIXSocket.tryUnixDomainSocket()) {
                RubyUNIXSocket.createUNIXSocket(runtime);
                RubyUNIXServer.createUNIXServer(runtime);
            }

            RubyIPSocket.createIPSocket(runtime);
            RubyTCPSocket.createTCPSocket(runtime);
            RubyTCPServer.createTCPServer(runtime);
            RubyUDPSocket.createUDPSocket(runtime);
        }
    }

    private static ObjectAllocator SOCKET_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubySocket(runtime, klass);
        }
    };
    
    public static final int MSG_OOB = 0x1;
    public static final int MSG_PEEK = 0x2;
    public static final int MSG_DONTROUTE = 0x4;

    @JRubyModule(name="Socket::Constants")
    public static class Constants {}

    static void createSocket(Ruby runtime) {
        RubyClass rb_cSocket = runtime.defineClass("Socket", runtime.fastGetClass("BasicSocket"), SOCKET_ALLOCATOR);
        
        RubyModule rb_mConstants = rb_cSocket.defineModuleUnder("Constants");
        // we don't have to define any that we don't support; see socket.c

        runtime.loadConstantSet(rb_mConstants, com.kenai.constantine.platform.Sock.class);
        runtime.loadConstantSet(rb_mConstants, com.kenai.constantine.platform.SocketOption.class);
        runtime.loadConstantSet(rb_mConstants, com.kenai.constantine.platform.SocketLevel.class);
        runtime.loadConstantSet(rb_mConstants, com.kenai.constantine.platform.ProtocolFamily.class);
        runtime.loadConstantSet(rb_mConstants, com.kenai.constantine.platform.AddressFamily.class);
        runtime.loadConstantSet(rb_mConstants, com.kenai.constantine.platform.INAddr.class);
        runtime.loadConstantSet(rb_mConstants, com.kenai.constantine.platform.IPProto.class);
        runtime.loadConstantSet(rb_mConstants, com.kenai.constantine.platform.Shutdown.class);
        runtime.loadConstantSet(rb_mConstants, com.kenai.constantine.platform.TCP.class);
        runtime.loadConstantSet(rb_mConstants, com.kenai.constantine.platform.NameInfo.class);

        // mandatory constants we haven't implemented
        rb_mConstants.fastSetConstant("MSG_OOB", runtime.newFixnum(MSG_OOB));
        rb_mConstants.fastSetConstant("MSG_PEEK", runtime.newFixnum(MSG_PEEK));
        rb_mConstants.fastSetConstant("MSG_DONTROUTE", runtime.newFixnum(MSG_DONTROUTE));
        
        // constants webrick crashes without
        rb_mConstants.fastSetConstant("AI_PASSIVE", runtime.newFixnum(1));
        
        // More constants needed by specs
        rb_mConstants.fastSetConstant("IP_MULTICAST_TTL", runtime.newFixnum(10));
        rb_mConstants.fastSetConstant("IP_MULTICAST_LOOP", runtime.newFixnum(11));
        rb_mConstants.fastSetConstant("IP_ADD_MEMBERSHIP", runtime.newFixnum(12));
        rb_mConstants.fastSetConstant("IP_MAX_MEMBERSHIPS", runtime.newFixnum(20));
        rb_mConstants.fastSetConstant("IP_DEFAULT_MULTICAST_LOOP", runtime.newFixnum(1));
        rb_mConstants.fastSetConstant("IP_DEFAULT_MULTICAST_TTL", runtime.newFixnum(1));

        rb_cSocket.includeModule(rb_mConstants);

        rb_cSocket.defineAnnotatedMethods(RubySocket.class);
    }
    
    public RubySocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    protected int getSoTypeDefault() {
        return soType;
    }

    private int soDomain;
    private int soType;
    private int soProtocol;
    @Deprecated
    public static IRubyObject for_fd(IRubyObject socketClass, IRubyObject fd) {
        return for_fd(socketClass.getRuntime().getCurrentContext(), socketClass, fd);
    }
    @JRubyMethod(frame = true, meta = true)
    public static IRubyObject for_fd(ThreadContext context, IRubyObject socketClass, IRubyObject fd) {
        Ruby ruby = context.getRuntime();
        if (fd instanceof RubyFixnum) {
            RubySocket socket = (RubySocket)((RubyClass)socketClass).allocate();
            
            // normal file descriptor..try to work with it
            ChannelDescriptor descriptor = socket.getDescriptorByFileno((int)((RubyFixnum)fd).getLongValue());
            
            if (descriptor == null) {
                throw ruby.newErrnoEBADFError();
            }
            
            Channel mainChannel = descriptor.getChannel();

            if (mainChannel instanceof SocketChannel) {
                // ok, it's a socket...set values accordingly
                // just using AF_INET since we can't tell from SocketChannel...
                socket.soDomain = AddressFamily.AF_INET.value();
                socket.soType = Sock.SOCK_STREAM.value();
                socket.soProtocol = 0;
            } else if (mainChannel instanceof DatagramChannel) {
                // datagram, set accordingly
                // again, AF_INET
                socket.soDomain = AddressFamily.AF_INET.value();
                socket.soType = Sock.SOCK_DGRAM.value();
                socket.soProtocol = 0;
            } else {
                throw context.getRuntime().newErrnoENOTSOCKError("can't Socket.new/for_fd against a non-socket");
            }

            socket.initSocket(ruby, descriptor);
            
            return socket;
        } else {
            throw context.getRuntime().newTypeError(fd, context.getRuntime().getFixnum());
        }
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject domain, IRubyObject type, IRubyObject protocol) {
        try {
            if(domain instanceof RubyString) {
                String domainString = domain.toString();
                if(domainString.equals("AF_INET")) {
                    soDomain = AF_INET.value();
                } else if(domainString.equals("PF_INET")) {
                    soDomain = PF_INET.value();
                } else {
                    throw sockerr(context.getRuntime(), "unknown socket domain " + domainString);
                }
            } else {
                soDomain = RubyNumeric.fix2int(domain);
            }
        
            if(type instanceof RubyString) {
                String typeString = type.toString();
                if(typeString.equals("SOCK_STREAM")) {
                    soType = SOCK_STREAM.value();
                } else if(typeString.equals("SOCK_DGRAM")) {
                    soType = SOCK_DGRAM.value();
                } else {
                    throw sockerr(context.getRuntime(), "unknown socket type " + typeString);
                }
            } else {
                soType = RubyNumeric.fix2int(type);
            }

            soProtocol = RubyNumeric.fix2int(protocol);
        
            Channel channel = null;
            if(soType == Sock.SOCK_STREAM.value()) {
                channel = SocketChannel.open();
            } else if(soType == Sock.SOCK_DGRAM.value()) {
                channel = DatagramChannel.open();
            }
            
            initSocket(context.getRuntime(), new ChannelDescriptor(channel, RubyIO.getNewFileno(), new ModeFlags(ModeFlags.RDWR), new FileDescriptor()));
        } catch (InvalidValueException ex) {
            throw context.getRuntime().newErrnoEINVALError();
        } catch(IOException e) {
            throw sockerr(context.getRuntime(), "initialize: " + e.toString());
        }

        return this;
    }

    private static RuntimeException sockerr(Ruby runtime, String msg) {
        return new RaiseException(runtime, runtime.fastGetClass("SocketError"), msg, true);
    }

    @Deprecated
    public static IRubyObject gethostname(IRubyObject recv) {
        return gethostname(recv.getRuntime().getCurrentContext(), recv);
    }
    @JRubyMethod(meta = true)
    public static IRubyObject gethostname(ThreadContext context, IRubyObject recv) {
        try {
            return context.getRuntime().newString(InetAddress.getLocalHost().getHostName());
        } catch(UnknownHostException e) {
            try {
                return context.getRuntime().newString(InetAddress.getByAddress(new byte[]{0,0,0,0}).getHostName());
            } catch(UnknownHostException e2) {
                throw sockerr(context.getRuntime(), "gethostname: name or service not known");
            }
        }
    }

    private static InetAddress intoAddress(Ruby runtime, String s) {
        try {
            byte[] bs = ByteList.plain(s);
            return InetAddress.getByAddress(bs);
        } catch(Exception e) {
            throw sockerr(runtime, "strtoaddr: " + e.toString());
        }
    }

    private static String intoString(Ruby runtime, InetAddress as) {
        try {
            return new String(ByteList.plain(as.getAddress()));
        } catch(Exception e) {
            throw sockerr(runtime, "addrtostr: " + e.toString());
        }
    }
    @Deprecated
    public static IRubyObject gethostbyaddr(IRubyObject recv, IRubyObject[] args) {
        return gethostbyaddr(recv.getRuntime().getCurrentContext(), recv, args);
    }
    @JRubyMethod(required = 1, rest = true, meta = true)
    public static IRubyObject gethostbyaddr(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        IRubyObject[] ret = new IRubyObject[4];
        ret[0] = runtime.newString(intoAddress(runtime,args[0].convertToString().toString()).getCanonicalHostName());
        ret[1] = runtime.newArray();
        ret[2] = runtime.newFixnum(2); // AF_INET
        ret[3] = args[0];
        return runtime.newArrayNoCopy(ret);
    }
    @Deprecated
    public static IRubyObject getservbyname(IRubyObject recv, IRubyObject[] args) {
        return getservbyname(recv.getRuntime().getCurrentContext(), recv, args);
    }
    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject getservbyname(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        int argc = Arity.checkArgumentCount(runtime, args, 1, 2);
        String name = args[0].convertToString().toString();
        String service = argc == 1 ? "tcp" : args[1].convertToString().toString();
        Integer port = IANA.serviceToPort.get(name + "/" + service);
        if(port == null) {
            throw sockerr(runtime, "no such service " + name + "/" + service);
        }
        return runtime.newFixnum(port.intValue());
    }

    @Deprecated
    public static IRubyObject pack_sockaddr_un(IRubyObject recv, IRubyObject filename) {
        return pack_sockaddr_un(recv.getRuntime().getCurrentContext(), recv, filename);
    }
    @JRubyMethod(name = {"pack_sockaddr_un", "sockaddr_un"}, meta = true)
    public static IRubyObject pack_sockaddr_un(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        StringBuilder sb = new StringBuilder();
        sb.append((char)0);
        sb.append((char)1);
        String str = filename.convertToString().toString();
        sb.append(str);
        for(int i=str.length();i<104;i++) {
            sb.append((char)0);
        }
        return context.getRuntime().newString(sb.toString());
    }

    @Deprecated
    public static IRubyObject pack_sockaddr_in(IRubyObject recv, IRubyObject port, IRubyObject host) {
        return pack_sockaddr_in(recv.getRuntime().getCurrentContext(), recv, port, host);
    }
    @JRubyMethod(name = {"pack_sockaddr_in", "sockaddr_in"}, meta = true)
    public static IRubyObject pack_sockaddr_in(ThreadContext context, IRubyObject recv, IRubyObject port, IRubyObject host) {
	return pack_sockaddr_in(context, recv,
				RubyNumeric.fix2int(port),
				host.isNil()
				? null
				: host.convertToString().toString());
    }
    public static IRubyObject pack_sockaddr_in(ThreadContext context, IRubyObject recv, int iport, String host) {
	ByteArrayOutputStream bufS = new ByteArrayOutputStream();
	try {
	    DataOutputStream ds = new DataOutputStream(bufS);
	    if(Platform.IS_BSD) {
		ds.write(16);
		ds.write(2);
	    } else {
		ds.write(2);
		ds.write(0);
	    }

	    ds.write(iport >> 8);
	    ds.write(iport);

	    try {
		if(host != null && "".equals(host)) {
		    ds.writeInt(0);
		} else {
		    InetAddress[] addrs = InetAddress.getAllByName(host);
		    byte[] addr = addrs[0].getAddress();
		    ds.write(addr, 0, addr.length);
		}
	    } catch (UnknownHostException e) {
		throw sockerr(context.getRuntime(), "getaddrinfo: No address associated with nodename");
	    }

	    ds.writeInt(0);
	    ds.writeInt(0);
	} catch (IOException e) {
	    throw sockerr(context.getRuntime(), "pack_sockaddr_in: internal error");
	}

        return context.getRuntime().newString(new ByteList(bufS.toByteArray(),
							   false));
    }
    @Deprecated
    public static IRubyObject unpack_sockaddr_in(IRubyObject recv, IRubyObject addr) {
        return unpack_sockaddr_in(recv.getRuntime().getCurrentContext(), recv, addr);
    }
    @JRubyMethod(meta = true)
    public static IRubyObject unpack_sockaddr_in(ThreadContext context, IRubyObject recv, IRubyObject addr) {
        String val = addr.convertToString().toString();
        if((Platform.IS_BSD && val.charAt(0) != 16 && val.charAt(1) != 2) || (!Platform.IS_BSD && val.charAt(0) != 2)) {
            throw context.getRuntime().newArgumentError("can't resolve socket address of wrong type");
        }
        
        int port = (val.charAt(2) << 8) + (val.charAt(3));
        StringBuilder sb = new StringBuilder();
        sb.append((int)val.charAt(4));
        sb.append(".");
        sb.append((int)val.charAt(5));
        sb.append(".");
        sb.append((int)val.charAt(6));
        sb.append(".");
        sb.append((int)val.charAt(7));

        IRubyObject[] result = new IRubyObject[]{
            context.getRuntime().newFixnum(port),
            context.getRuntime().newString(sb.toString())};

        return context.getRuntime().newArrayNoCopy(result);
    }

    private static final ByteList BROADCAST = new ByteList("<broadcast>".getBytes());
    private static final byte[] INADDR_BROADCAST = new byte[] {-1,-1,-1,-1}; // 255.255.255.255
    private static final ByteList ANY = new ByteList("<any>".getBytes());
    private static final byte[] INADDR_ANY = new byte[] {0,0,0,0}; // 0.0.0.0

    public static InetAddress getRubyInetAddress(ByteList address) throws UnknownHostException {
        if (address.equal(BROADCAST)) {
            return InetAddress.getByAddress(INADDR_BROADCAST);
        } else if (address.equal(ANY)) {
            return InetAddress.getByAddress(INADDR_ANY);
        } else {
            return InetAddress.getByName(address.toString());
        }
    }

    @Deprecated
    public static IRubyObject gethostbyname(IRubyObject recv, IRubyObject hostname) {
        return gethostbyname(recv.getRuntime().getCurrentContext(), recv, hostname);
    }
    @JRubyMethod(meta = true)
    public static IRubyObject gethostbyname(ThreadContext context, IRubyObject recv, IRubyObject hostname) {
        try {
            InetAddress addr = getRubyInetAddress(hostname.convertToString().getByteList());
            Ruby runtime = context.getRuntime();
            IRubyObject[] ret = new IRubyObject[4];
            ret[0] = runtime.newString(addr.getCanonicalHostName());
            ret[1] = runtime.newArray();
            ret[2] = runtime.newFixnum(2); // AF_INET
            ret[3] = runtime.newString(new ByteList(addr.getAddress()));
            return runtime.newArrayNoCopy(ret);
        } catch(UnknownHostException e) {
            throw sockerr(context.getRuntime(), "gethostbyname: name or service not known");
        }
    }

    @Deprecated
    public static IRubyObject getaddrinfo(IRubyObject recv, IRubyObject[] args) {
        return getaddrinfo(recv.getRuntime().getCurrentContext(), recv, args);
    }
    //def self.getaddrinfo(host, port, family = nil, socktype = nil, protocol = nil, flags = nil)
    @JRubyMethod(required = 2, optional = 4, meta = true)
    public static IRubyObject getaddrinfo(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        args = Arity.scanArgs(context.getRuntime(),args,2,4);
        try {
            Ruby r = context.getRuntime();
            IRubyObject host = args[0];
            IRubyObject port = args[1];
            boolean emptyHost = host.isNil() || host.convertToString().isEmpty();

            if(port instanceof RubyString) {
                port = getservbyname(context, recv, new IRubyObject[]{port});
            }

            //IRubyObject family = args[2];
            IRubyObject socktype = args[3];
            //IRubyObject protocol = args[4];
            IRubyObject flags = args[5];

            boolean sock_stream = true;
            boolean sock_dgram = true;
            if(!socktype.isNil()) {
                int val = RubyNumeric.fix2int(socktype);
                if(val == SOCK_STREAM.value()) {
                    sock_dgram = false;
                } else if(val == SOCK_DGRAM.value()) {
                    sock_stream = false;
                }
            }

            // When Socket::AI_PASSIVE and host is nil, return 'any' address. 
            InetAddress[] addrs = null; 
            if(!flags.isNil()) {
                // The value of 1 is for Socket::AI_PASSIVE.
                int flag = RubyNumeric.fix2int(flags);
                if ((flag == 1) && emptyHost ) {
                    addrs = InetAddress.getAllByName("0.0.0.0");
                }

            }

            if (addrs == null)
                addrs = InetAddress.getAllByName(emptyHost ? null : host.convertToString().toString());

            List<IRubyObject> l = new ArrayList<IRubyObject>();
            for(int i = 0; i < addrs.length; i++) {
                IRubyObject[] c;
                if(sock_dgram) {
                    c = new IRubyObject[7];
                    c[0] = r.newString("AF_INET");
                    c[1] = port;
                    c[2] = r.newString(addrs[i].getCanonicalHostName());
                    c[3] = r.newString(addrs[i].getHostAddress());
                    c[4] = r.newFixnum(PF_INET);
                    c[5] = r.newFixnum(SOCK_DGRAM);
                    c[6] = r.newFixnum(IPPROTO_UDP);
                    l.add(r.newArrayNoCopy(c));
                }
                if(sock_stream) {
                    c = new IRubyObject[7];
                    c[0] = r.newString("AF_INET");
                    c[1] = port;
                    c[2] = r.newString(addrs[i].getCanonicalHostName());
                    c[3] = r.newString(addrs[i].getHostAddress());
                    c[4] = r.newFixnum(PF_INET);
                    c[5] = r.newFixnum(SOCK_STREAM);
                    c[6] = r.newFixnum(IPPROTO_TCP);
                    l.add(r.newArrayNoCopy(c));
                }
            }
            return r.newArray(l);
        } catch(UnknownHostException e) {
            throw sockerr(context.getRuntime(), "getaddrinfo: name or service not known");
        }
    }

    // FIXME: may need to broaden for IPV6 IP address strings
    private static final Pattern STRING_ADDRESS_PATTERN =
        Pattern.compile("((.*)\\/)?([\\.0-9]+)(:([0-9]+))?");
    
    private static final int HOST_GROUP = 3;
    private static final int PORT_GROUP = 5;
    
    @Deprecated
    public static IRubyObject getnameinfo(IRubyObject recv, IRubyObject[] args) {
        return getnameinfo(recv.getRuntime().getCurrentContext(), recv, args);
    }
    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject getnameinfo(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        int argc = Arity.checkArgumentCount(runtime, args, 1, 2);
        int flags = argc == 2 ? RubyNumeric.num2int(args[1]) : 0;
        IRubyObject arg0 = args[0];

        String host, port;
        if (arg0 instanceof RubyArray) {
            List list = ((RubyArray)arg0).getList();
            int len = list.size();
            if (len < 3 || len > 4) {
                throw runtime.newArgumentError("array size should be 3 or 4, "+len+" given");
            }
            // TODO: validate port as numeric
            host = list.get(2).toString();
            port = list.get(1).toString();
        } else if (arg0 instanceof RubyString) {
            String arg = ((RubyString)arg0).toString();
            Matcher m = STRING_ADDRESS_PATTERN.matcher(arg);
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
                }
                else {
                    throw runtime.newArgumentError("invalid address string");
                }
            } else if ((host = m.group(HOST_GROUP)) == null || host.length() == 0 ||
                    (port = m.group(PORT_GROUP)) == null || port.length() == 0) {
                throw runtime.newArgumentError("invalid address string");
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
        if ((flags & NI_NUMERICHOST.value()) == 0) {
            host = addr.getCanonicalHostName();
        } else {
            host = addr.getHostAddress();
        }
        if ((flags & NI_NUMERICSERV.value()) == 0) {
            String serv = IANA.portToService.get(Integer.parseInt(port));
            if (serv != null) {
                port = serv.substring(0, serv.indexOf('/') );
            }
        }
        return runtime.newArray(runtime.newString(host), runtime.newString(port));

    }
}// RubySocket
