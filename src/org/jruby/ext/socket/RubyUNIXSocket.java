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
 * Copyright (C) 2008 Ola Bini <ola.bini@gmail.com>
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

import com.kenai.constantine.platform.Fcntl;
import com.kenai.constantine.platform.OpenFlags;
import com.kenai.constantine.platform.SocketLevel;
import com.kenai.constantine.platform.SocketOption;
import com.kenai.jaffl.LastError;
import com.kenai.jaffl.annotations.In;
import com.kenai.jaffl.annotations.Out;
import com.kenai.jaffl.annotations.Transient;
import com.kenai.jaffl.byref.IntByReference;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import static com.kenai.constantine.platform.AddressFamily.*;
import static com.kenai.constantine.platform.ProtocolFamily.*;
import static com.kenai.constantine.platform.Sock.*;


import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.posix.util.Platform;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.ChannelStream;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.ByteList;
import org.jruby.util.io.InvalidValueException;

/**
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
@JRubyClass(name="UNIXSocket", parent="BasicSocket")
public class RubyUNIXSocket extends RubyBasicSocket {
    protected static volatile LibCSocket INSTANCE = null;

    /**
     * Implements reading and writing to a socket fd using recv and send
     */
    private static class UnixDomainSocketChannel implements ReadableByteChannel, WritableByteChannel {
        private final int fd;
        private boolean open = true;

        public UnixDomainSocketChannel(int fd) {
            this.fd = fd;
        }

        public void close() throws IOException {
            open = false;
        }

        public boolean isOpen() {
            return open;
        }

        public int read(ByteBuffer dst) throws IOException {
            int max = dst.remaining();

            int v = INSTANCE.recv(fd, dst, max, 0);

            if(v != -1) {
                dst.position(dst.position()+v);
            }

            return v;
        }

        public int write(ByteBuffer src) throws IOException {
            int max = src.remaining();
            int v = INSTANCE.send(fd, src, max, 0);

            if(v != -1) {
                src.position(src.position()+v);
            }

            return v;
        }
    }

    public static boolean tryUnixDomainSocket() {
        if(INSTANCE != null) {
            return true;
        }

        if (Platform.IS_WINDOWS) {
            return false; // no UNIXSockets on Windows
        }

        try {
            synchronized(RubyUNIXSocket.class) {
                if(INSTANCE != null) {
                    return true;
                }

                String[] libnames = Platform.IS_SOLARIS
                        ? new String[] { "socket", "nsl", "c" }
                        : new String[] { "c" };

                INSTANCE = (LibCSocket)com.kenai.jaffl.Library.loadLibrary(LibCSocket.class, libnames);
                return true;
            }
        } catch(Throwable e) {
            return false;
        }
    }

    public static interface LibCSocket {
        int socketpair(int d, int type, int protocol, int[] sv);
        int socket(int domain, int type, int protocol);
        int connect(int s, @In @Transient sockaddr_un name, int namelen);
        int bind(int s, @Transient sockaddr_un name, int namelen);
        int listen(int s, int backlog);
        int accept(int s, @Transient sockaddr_un addr, IntByReference addrlen);

        int getsockname(int s, @Out @Transient sockaddr_un addr, IntByReference addrlen);
        int getpeername(int s, @Out @Transient sockaddr_un addr, IntByReference addrlen);

        int getsockopt(int s, int level, int optname, @Out byte[] optval, IntByReference optlen);
        int setsockopt(int s, int level, int optname, @In byte[] optval, int optlen);

        int recv(int s, @Out ByteBuffer buf, int len, int flags);
        int recvfrom(int s, @Out ByteBuffer buf, int len, int flags, @Out @Transient sockaddr_un from, IntByReference fromlen);

        int send(int s, @In ByteBuffer msg, int len, int flags);

        int fcntl(int fd, int cmd, int arg);

        int unlink(String path);
        int close(int s);

        void perror(String arg);

        // Sockaddr_un has different structure on different platforms.
        // See JRUBY-2213 for more details.
        public static abstract class sockaddr_un extends com.kenai.jaffl.struct.Struct {
            public final static int LENGTH = 106;
            public abstract void setFamily(int family);
            public abstract int getFamily();
            public abstract UTF8String path();
            
            public static final sockaddr_un newInstance() {
                return Platform.IS_BSD ? new LibCSocket.BSDSockAddrUnix() : new DefaultSockAddrUnix();
            }
        }

        public static final class BSDSockAddrUnix extends sockaddr_un {
            public final Signed8 sun_len = new Signed8();
            public final Signed8 sun_family = new Signed8();
            public final UTF8String sun_path = new UTF8String(LENGTH - 2);
            public final void setFamily(int family) {
                sun_family.set((byte) family);
            }
            public final int getFamily() {
                return sun_family.get();
            }
            public final UTF8String path() { return sun_path; }
        }
        
        public static final class DefaultSockAddrUnix extends sockaddr_un {
            public final Signed16 sun_family = new Signed16();
            public final UTF8String sun_path = new UTF8String(LENGTH - 2);

            public final void setFamily(int family) {
                sun_family.set((short) family);
            }
            public final int getFamily() {
                return sun_family.get();
            }
            public final UTF8String path() { return sun_path; }
        }

    }
    
    private static ObjectAllocator UNIXSOCKET_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyUNIXSocket(runtime, klass);
        }
    };

    static void createUNIXSocket(Ruby runtime) {
        RubyClass rb_cUNIXSocket = runtime.defineClass("UNIXSocket", runtime.fastGetClass("BasicSocket"), UNIXSOCKET_ALLOCATOR);
        runtime.getObject().fastSetConstant("UNIXsocket", rb_cUNIXSocket);
        
        rb_cUNIXSocket.defineAnnotatedMethods(RubyUNIXSocket.class);
    }

    public RubyUNIXSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    protected int fd;
    protected String fpath;

    protected static void rb_sys_fail(Ruby runtime, String message) {
        final int n = LastError.getLastError();

        IRubyObject arg = (message != null) ? runtime.newString(message) : runtime.getNil();

        RubyClass instance = runtime.getErrno(n);
        if(instance == null) {
            instance = runtime.getSystemCallError();
            throw new RaiseException((RubyException)(instance.newInstance(runtime.getCurrentContext(), new IRubyObject[]{arg, runtime.newFixnum(n)}, Block.NULL_BLOCK)));
        } else {
            throw new RaiseException((RubyException)(instance.newInstance(runtime.getCurrentContext(), new IRubyObject[]{arg}, Block.NULL_BLOCK)));
        }
    }

    protected final static int F_GETFL = Fcntl.F_GETFL.value();
    protected final static int F_SETFL = Fcntl.F_SETFL.value();

    protected final static int O_NONBLOCK = OpenFlags.O_NONBLOCK.value();

    protected void init_unixsock(Ruby runtime, IRubyObject _path, boolean server) {
        int status;
        fd = -1;
        try {
            fd = INSTANCE.socket(AF_UNIX.value(), SOCK_STREAM.value(), 0);
        } catch (UnsatisfiedLinkError ule) { }
        if (fd < 0) {
            rb_sys_fail(runtime, "socket(2)");
        }

        LibCSocket.sockaddr_un sockaddr = LibCSocket.sockaddr_un.newInstance();
        sockaddr.setFamily(AF_UNIX.value());

        ByteList path = _path.convertToString().getByteList();
        fpath = path.toString();

        if(sockaddr.path().length() <= path.realSize) {
            throw runtime.newArgumentError("too long unix socket path (max: " + (sockaddr.path().length()-1) + "bytes)");
        }

        sockaddr.path().set(fpath);

        if(server) {
            status = INSTANCE.bind(fd, sockaddr, LibCSocket.sockaddr_un.LENGTH);
        } else {
            try {
                status = INSTANCE.connect(fd, sockaddr, LibCSocket.sockaddr_un.LENGTH);
            } catch(RuntimeException e) {
                INSTANCE.close(fd);
                throw e;
            }
        }

        if(status < 0) {
            INSTANCE.close(fd);
            rb_sys_fail(runtime, fpath);
        }

        if(server) {
            INSTANCE.listen(fd, 5);
        }

        init_sock(runtime);

        if(server) {
            openFile.setPath(fpath);
        }
    }

    @Override
    public IRubyObject setsockopt(ThreadContext context, IRubyObject lev, IRubyObject optname, IRubyObject val) {
        int level = RubyNumeric.fix2int(lev);
        int opt = RubyNumeric.fix2int(optname);

        switch(SocketLevel.valueOf(level)) {
        case SOL_SOCKET:
            switch(SocketOption.valueOf(opt)) {
            case SO_KEEPALIVE: {
                int res = INSTANCE.setsockopt(fd, level, opt, asBoolean(val) ? new byte[]{32,0,0,0} : new byte[]{0,0,0,0}, 4);
                if(res == -1) {
                    rb_sys_fail(context.getRuntime(), openFile.getPath());
                }
            }
                break;
            default:
                throw context.getRuntime().newErrnoENOPROTOOPTError();
            }
            break;
        default:
            throw context.getRuntime().newErrnoENOPROTOOPTError();
        }

        return context.getRuntime().newFixnum(0);
    }

    protected void init_sock(Ruby runtime) {
        try {
            ModeFlags modes = new ModeFlags(ModeFlags.RDWR);
            openFile.setMainStream(new ChannelStream(runtime, new ChannelDescriptor(new UnixDomainSocketChannel(fd), getNewFileno(), modes, new java.io.FileDescriptor())));
            openFile.setPipeStream(openFile.getMainStream());
            openFile.setMode(modes.getOpenFileFlags());
            openFile.getMainStream().setSync(true);
        } catch (InvalidValueException ive) {
            throw runtime.newErrnoEINVALError();
        }
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject path) {
        init_unixsock(context.getRuntime(), path, false);
        return this;
    }

    private String unixpath(LibCSocket.sockaddr_un addr, IntByReference len) {
        if (len.getValue() > 2) {
            // There is something valid in the sun_path component
            return addr.path().toString();
        } else {
            return "";
        }
    }

    private IRubyObject unixaddr(Ruby runtime, LibCSocket.sockaddr_un addr, IntByReference len) {
        return runtime.newArrayNoCopy(new IRubyObject[]{runtime.newString("AF_UNIX"), runtime.newString(unixpath(addr, len))});
    }
    @Deprecated
    public IRubyObject path() {
        return path(getRuntime().getCurrentContext());
    }
    @JRubyMethod
    public IRubyObject path(ThreadContext context) {
        if(openFile.getPath() == null) {
            LibCSocket.sockaddr_un addr = LibCSocket.sockaddr_un.newInstance();
            IntByReference len = new IntByReference(LibCSocket.sockaddr_un.LENGTH);
            if(INSTANCE.getsockname(fd, addr, len) < 0) {
                rb_sys_fail(context.getRuntime(), null);
            }
            openFile.setPath(unixpath(addr, len));
        }
        return context.getRuntime().newString(openFile.getPath());
    }

    @Deprecated
    public IRubyObject addr() {
        return addr(getRuntime().getCurrentContext());
    }
    @JRubyMethod
    public IRubyObject addr(ThreadContext context) {
        LibCSocket.sockaddr_un addr = LibCSocket.sockaddr_un.newInstance();
        IntByReference len = new IntByReference(LibCSocket.sockaddr_un.LENGTH);
        if(INSTANCE.getsockname(fd, addr, len) < 0) {
            rb_sys_fail(context.getRuntime(), "getsockname(2)");
        }
        return unixaddr(context.getRuntime(), addr, len);
    }
    @Deprecated
    public IRubyObject peeraddr() {
        return peeraddr(getRuntime().getCurrentContext());
    }
    @JRubyMethod
    public IRubyObject peeraddr(ThreadContext context) {
        LibCSocket.sockaddr_un addr = LibCSocket.sockaddr_un.newInstance();
        IntByReference len = new IntByReference(LibCSocket.sockaddr_un.LENGTH);
        if(INSTANCE.getpeername(fd, addr, len) < 0) {
            rb_sys_fail(context.getRuntime(), "getpeername(2)");
        }
        return unixaddr(context.getRuntime(), addr, len);
    }
    @Deprecated
    public IRubyObject recvfrom(IRubyObject[] args) {
        return recvfrom(getRuntime().getCurrentContext(), args);
    }

    @JRubyMethod(name = "recvfrom", required = 1, optional = 1)
    public IRubyObject recvfrom(ThreadContext context, IRubyObject[] args) {
        ByteBuffer str = ByteBuffer.allocateDirect(1024);
        LibCSocket.sockaddr_un buf = LibCSocket.sockaddr_un.newInstance();
        IntByReference alen = new IntByReference(LibCSocket.sockaddr_un.LENGTH);

        IRubyObject len, flg;

        int flags;

        if(Arity.checkArgumentCount(context.getRuntime(), args, 1, 2) == 2) {
            flg = args[1];
        } else {
            flg = context.getRuntime().getNil();
        }

        len = args[0];

        if(flg.isNil()) {
            flags = 0;
        } else {
            flags = RubyNumeric.fix2int(flg);
        }

        int buflen = RubyNumeric.fix2int(len);
        int slen = INSTANCE.recvfrom(fd, str, buflen, flags, buf, alen);
        if(slen < 0) {
            rb_sys_fail(context.getRuntime(), "recvfrom(2)");
        }

        if(slen < buflen) {
            buflen = slen;
        }
        byte[] outp = new byte[buflen];
        str.get(outp);
        RubyString _str = context.getRuntime().newString(new ByteList(outp, 0, buflen, false));

        return context.getRuntime().newArrayNoCopy(new IRubyObject[]{_str, unixaddr(context.getRuntime(), buf, alen)});
    }

    @JRubyMethod
    public IRubyObject send_io(IRubyObject path) {
        //TODO: implement, won't do this now
        return  getRuntime().getNil();
    }

    @JRubyMethod(rest = true)
    public IRubyObject recv_io(IRubyObject[] args) {
        //TODO: implement, won't do this now
        return  getRuntime().getNil();
    }

    @Override
    public IRubyObject close() {
        super.close();
        INSTANCE.close(fd);
        return getRuntime().getNil();
    }
    @Deprecated
    public static IRubyObject open(IRubyObject recv, IRubyObject path) {
        return open(recv.getRuntime().getCurrentContext(), recv, path);
    }
    @JRubyMethod(meta = true)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject path) {
        return RuntimeHelpers.invoke(context, recv, "new", path);
    }

    private static int getSocketType(IRubyObject tp) {
        if(tp instanceof RubyString) {
            String str = tp.toString();
            if("SOCK_STREAM".equals(str)) {
                return SOCK_STREAM.value();
            } else if("SOCK_DGRAM".equals(str)) {
                return SOCK_DGRAM.value();
            } else if("SOCK_RAW".equals(str)) {
                return SOCK_RAW.value();
            } else {
                return -1;
            }
        }
        return RubyNumeric.fix2int(tp);
    }
    @Deprecated
    public static IRubyObject socketpair(IRubyObject recv, IRubyObject[] args) {
        return socketpair(recv.getRuntime().getCurrentContext(), recv, args);
    }
    @JRubyMethod(name = {"socketpair", "pair"}, optional = 2, meta = true)
    public static IRubyObject socketpair(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        int domain = PF_UNIX.value();
        Ruby runtime = context.getRuntime();
        Arity.checkArgumentCount(runtime, args, 0, 2);
        
        int type;

        if(args.length == 0) { 
            type = SOCK_STREAM.value();
        } else {
            type = getSocketType(args[0]);
        }

        int protocol;

        if(args.length <= 1) {
            protocol = 0;
        } else {
            protocol = RubyNumeric.fix2int(args[1]);
        }

        int[] sp = new int[2];
        int ret = -1;
        try {
            ret = INSTANCE.socketpair(domain, type, protocol, sp);
        } catch (UnsatisfiedLinkError ule) { }
        if (ret < 0) {
            rb_sys_fail(runtime, "socketpair(2)");
        }

        RubyUNIXSocket sock = (RubyUNIXSocket)(RuntimeHelpers.invoke(context, runtime.fastGetClass("UNIXSocket"), "allocate"));
        sock.fd = sp[0];
        sock.init_sock(runtime);
        RubyUNIXSocket sock2 = (RubyUNIXSocket)(RuntimeHelpers.invoke(context, runtime.fastGetClass("UNIXSocket"), "allocate"));
        sock2.fd = sp[1];
        sock2.init_sock(runtime);

        return runtime.newArrayNoCopy(new IRubyObject[]{sock, sock2});
    }
}
