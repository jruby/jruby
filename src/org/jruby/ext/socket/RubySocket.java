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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubySocket extends RubyBasicSocket {

    public static class Service implements Library {
        public void load(final Ruby runtime) throws IOException {
            runtime.defineClass("SocketError",runtime.fastGetClass("StandardError"), runtime.fastGetClass("StandardError").getAllocator());
            RubyBasicSocket.createBasicSocket(runtime);
            RubySocket.createSocket(runtime);
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

    public static final int NI_DGRAM = 16;
    public static final int NI_MAXHOST = 1025;
    public static final int NI_MAXSERV = 32;
    public static final int NI_NAMEREQD = 4;
    public static final int NI_NOFQDN = 1;
    public static final int NI_NUMERICHOST = 2;
    public static final int NI_NUMERICSERV = 8;

    static void createSocket(Ruby runtime) {
        RubyClass rb_cSocket = runtime.defineClass("Socket", runtime.fastGetClass("BasicSocket"), SOCKET_ALLOCATOR);
        CallbackFactory cfact = runtime.callbackFactory(RubySocket.class);
        
        RubyModule rb_mConstants = rb_cSocket.defineModuleUnder("Constants");
        // we don't have to define any that we don't support; see socket.c
        
        rb_mConstants.fastSetConstant("SOCK_STREAM", runtime.newFixnum(1));
        rb_mConstants.fastSetConstant("SOCK_DGRAM", runtime.newFixnum(2));
        rb_mConstants.fastSetConstant("PF_UNSPEC", runtime.newFixnum(0));
        rb_mConstants.fastSetConstant("AF_UNSPEC", runtime.newFixnum(0));
        rb_mConstants.fastSetConstant("PF_INET", runtime.newFixnum(2));
        rb_mConstants.fastSetConstant("AF_INET", runtime.newFixnum(2));
        // mandatory constants we haven't implemented
        rb_mConstants.fastSetConstant("MSG_OOB", runtime.newFixnum(0x01));
        rb_mConstants.fastSetConstant("SOL_SOCKET", runtime.newFixnum(1));
        rb_mConstants.fastSetConstant("SOL_IP", runtime.newFixnum(0));
        rb_mConstants.fastSetConstant("SOL_TCP", runtime.newFixnum(6));
        rb_mConstants.fastSetConstant("SOL_UDP", runtime.newFixnum(17));
        rb_mConstants.fastSetConstant("IPPROTO_IP", runtime.newFixnum(0));
        rb_mConstants.fastSetConstant("IPPROTO_ICMP", runtime.newFixnum(1));
        rb_mConstants.fastSetConstant("IPPROTO_TCP", runtime.newFixnum(6));
        rb_mConstants.fastSetConstant("IPPROTO_UDP", runtime.newFixnum(17));
        //  IPPROTO_RAW = 255
        rb_mConstants.fastSetConstant("INADDR_ANY", runtime.newFixnum(0x00000000));
        rb_mConstants.fastSetConstant("INADDR_BROADCAST", runtime.newFixnum(0xffffffff));
        rb_mConstants.fastSetConstant("INADDR_LOOPBACK", runtime.newFixnum(0x7f000001));
        rb_mConstants.fastSetConstant("INADDR_UNSPEC_GROUP", runtime.newFixnum(0xe0000000));
        rb_mConstants.fastSetConstant("INADDR_ALLHOSTS_GROUP", runtime.newFixnum(0xe0000001));
        rb_mConstants.fastSetConstant("INADDR_MAX_LOCAL_GROUP", runtime.newFixnum(0xe00000ff));
        rb_mConstants.fastSetConstant("INADDR_NONE", runtime.newFixnum(0xffffffff));
        rb_mConstants.fastSetConstant("SO_REUSEADDR", runtime.newFixnum(2));
        rb_mConstants.fastSetConstant("SHUT_RD", runtime.newFixnum(0));
        rb_mConstants.fastSetConstant("SHUT_WR", runtime.newFixnum(1));
        rb_mConstants.fastSetConstant("SHUT_RDWR", runtime.newFixnum(2));
    
        // constants webrick crashes without
        rb_mConstants.fastSetConstant("AI_PASSIVE", runtime.newFixnum(1));

        // constants Rails > 1.1.4 ActiveRecord's default mysql adapter dies without during scaffold generation
        rb_mConstants.fastSetConstant("SO_KEEPALIVE", runtime.newFixnum(9));
    
        // drb needs defined
        rb_mConstants.fastSetConstant("TCP_NODELAY", runtime.newFixnum(1));

        // flags/limits used by Net::SSH
        rb_mConstants.fastSetConstant("NI_DGRAM", runtime.newFixnum(NI_DGRAM));
        rb_mConstants.fastSetConstant("NI_MAXHOST", runtime.newFixnum(NI_MAXHOST));
        rb_mConstants.fastSetConstant("NI_MAXSERV", runtime.newFixnum(NI_MAXSERV));
        rb_mConstants.fastSetConstant("NI_NAMEREQD", runtime.newFixnum(NI_NAMEREQD));
        rb_mConstants.fastSetConstant("NI_NOFQDN", runtime.newFixnum(NI_NOFQDN));
        rb_mConstants.fastSetConstant("NI_NUMERICHOST", runtime.newFixnum(NI_NUMERICHOST));
        rb_mConstants.fastSetConstant("NI_NUMERICSERV", runtime.newFixnum(NI_NUMERICSERV));
       
        
        rb_cSocket.includeModule(rb_mConstants);

        rb_cSocket.getMetaClass().defineFastMethod("gethostname", cfact.getFastSingletonMethod("gethostname"));
        rb_cSocket.getMetaClass().defineFastMethod("gethostbyaddr", cfact.getFastOptSingletonMethod("gethostbyaddr"));
        rb_cSocket.getMetaClass().defineFastMethod("gethostbyname", cfact.getFastSingletonMethod("gethostbyname", IRubyObject.class));
        rb_cSocket.getMetaClass().defineFastMethod("getaddrinfo", cfact.getFastOptSingletonMethod("getaddrinfo"));
        rb_cSocket.getMetaClass().defineFastMethod("getnameinfo", cfact.getFastOptSingletonMethod("getnameinfo"));
    }
    
    public RubySocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    private static RuntimeException sockerr(IRubyObject recv, String msg) {
        return new RaiseException(recv.getRuntime(), recv.getRuntime().fastGetClass("SocketError"), msg, true);
    }

    public static IRubyObject gethostname(IRubyObject recv) {
        try {
            return recv.getRuntime().newString(InetAddress.getLocalHost().getHostName());
        } catch(UnknownHostException e) {
            try {
                return recv.getRuntime().newString(InetAddress.getByAddress(new byte[]{0,0,0,0}).getHostName());
            } catch(UnknownHostException e2) {
                throw sockerr(recv, "gethostname: name or service not known");
            }
        }
    }

    private static InetAddress intoAddress(IRubyObject recv, String s) {
        try {
            byte[] bs = ByteList.plain(s);
            return InetAddress.getByAddress(bs);
        } catch(Exception e) {
            throw sockerr(recv, "strtoaddr: " + e.toString());
        }
    }

    private static String intoString(IRubyObject recv, InetAddress as) {
        try {
            return new String(ByteList.plain(as.getAddress()));
        } catch(Exception e) {
            throw sockerr(recv, "addrtostr: " + e.toString());
        }
    }

    public static IRubyObject gethostbyaddr(IRubyObject recv, IRubyObject[] args) {
        Arity.checkArgumentCount(recv.getRuntime(), args,1,2);
        Ruby runtime = recv.getRuntime();
        IRubyObject[] ret = new IRubyObject[4];
        ret[0] = runtime.newString(intoAddress(recv,args[0].convertToString().toString()).getCanonicalHostName());
        ret[1] = runtime.newArray();
        ret[2] = runtime.newFixnum(2); // AF_INET
        ret[3] = args[0];
        return runtime.newArrayNoCopy(ret);
    }

    public static IRubyObject gethostbyname(IRubyObject recv, IRubyObject hostname) {
        try {
            InetAddress addr = InetAddress.getByName(hostname.convertToString().toString());
            Ruby runtime = recv.getRuntime();
            IRubyObject[] ret = new IRubyObject[4];
            ret[0] = runtime.newString(addr.getCanonicalHostName());
            ret[1] = runtime.newArray();
            ret[2] = runtime.newFixnum(2); // AF_INET
            ret[3] = runtime.newString(intoString(recv,addr));
            return runtime.newArrayNoCopy(ret);
        } catch(UnknownHostException e) {
            throw sockerr(recv, "gethostbyname: name or service not known");
        }
    }

    //def self.getaddrinfo(host, port, family = nil, socktype = nil, protocol = nil, flags = nil)
    public static IRubyObject getaddrinfo(IRubyObject recv, IRubyObject[] args) {
        args = Arity.scanArgs(recv.getRuntime(),args,2,4);
        try {
            Ruby r = recv.getRuntime();
            IRubyObject host = args[0];
            IRubyObject port = args[1];
            //IRubyObject family = args[2];
            IRubyObject socktype = args[3];
            //IRubyObject protocol = args[4];
            //IRubyObject flags = args[5];
            boolean sock_stream = true;
            boolean sock_dgram = true;
            if(!socktype.isNil()) {
                int val = RubyNumeric.fix2int(socktype);
                if(val == 1) {
                    sock_dgram = false;
                } else if(val == 2) {
                    sock_stream = false;
                }
            }
            InetAddress[] addrs = InetAddress.getAllByName(host.isNil() ? null : host.convertToString().toString());
            List l = new ArrayList();
            for(int i=0;i<addrs.length;i++) {
                IRubyObject[] c;
                if(sock_stream) {
                    c = new IRubyObject[7];
                    c[0] = r.newString("AF_INET");
                    c[1] = port;
                    c[2] = r.newString(addrs[i].getCanonicalHostName());
                    c[3] = r.newString(addrs[i].getHostAddress());
                    c[4] = r.newFixnum(2); // PF_INET
                    c[5] = r.newFixnum(1); // SOCK_STREAM
                    c[6] = r.newFixnum(6); // Protocol TCP
                    l.add(r.newArrayNoCopy(c));
                }
                if(sock_dgram) {
                    c = new IRubyObject[7];
                    c[0] = r.newString("AF_INET");
                    c[1] = port;
                    c[2] = r.newString(addrs[i].getCanonicalHostName());
                    c[3] = r.newString(addrs[i].getHostAddress());
                    c[4] = r.newFixnum(2); // PF_INET
                    c[5] = r.newFixnum(2); // SOCK_DRGRAM
                    c[6] = r.newFixnum(17); // Protocol UDP
                    l.add(r.newArrayNoCopy(c));
                }
            }
            return r.newArray(l);
        } catch(UnknownHostException e) {
            throw sockerr(recv, "getaddrinfo: name or service not known");
        }
    }

    // FIXME: may need to broaden for IPV6 IP address strings
    private static final Pattern STRING_ADDRESS_PATTERN =
        Pattern.compile("((.*)\\/)?([\\.0-9]+)(:([0-9]+))?");
    
    private static final int HOST_GROUP = 3;
    private static final int PORT_GROUP = 5;
    
    public static IRubyObject getnameinfo(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
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
                throw runtime.newArgumentError("invalid address string");
            }
            if ((host = m.group(HOST_GROUP)) == null || host.length() == 0 ||
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
            throw sockerr(recv, "unknown host: "+ host);
        }
        if ((flags & NI_NUMERICHOST) == 0) {
            host = addr.getCanonicalHostName();
        } else {
            host = addr.getHostAddress();
        }
        return runtime.newArray(runtime.newString(host), runtime.newString(port));

    }
}// RubySocket
