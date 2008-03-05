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

import java.io.IOException;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.Union;
import com.sun.jna.ptr.IntByReference;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.posix.util.Platform;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.ChannelStream;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class RubyUNIXSocket extends RubyBasicSocket {
    protected static LibCSocket INSTANCE = null;

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

        try {
            synchronized(RubyUNIXSocket.class) {
                if(INSTANCE != null) {
                    return true;
                }
                INSTANCE = (LibCSocket)Native.loadLibrary("c", LibCSocket.class);
                return true;
            }
        } catch(Throwable e) {
            return false;
        }
    }

    public static interface LibCSocket extends Library {
        int socketpair(int d, int type, int protocol, int[] sv);
        int socket(int domain, int type, int protocol);
        int connect(int s, sockaddr_un name, int namelen);
        int bind(int s, sockaddr_un name, int namelen);
        int listen(int s, int backlog);
        int accept(int s, sockaddr_un addr, IntByReference addrlen);

        int getsockname(int s, sockaddr_un addr, IntByReference addrlen);
        int getpeername(int s, sockaddr_un addr, IntByReference addrlen);

        int getsockopt(int s, int level, int optname, byte[] optval, IntByReference optlen);
        int setsockopt(int s, int level, int optname, byte[] optval, int optlen);

        int recv(int s, Buffer buf, int len, int flags);
        int recvfrom(int s, Buffer buf, int len, int flags, sockaddr_un from, IntByReference fromlen);

        int send(int s, Buffer msg, int len, int flags);

        int fcntl(int fd, int cmd, int arg);

        int unlink(String path);
        int close(int s);

        void perror(String arg);

        // Sockaddr_un has different structure on different platforms.
        // See JRUBY-2213 for more details.
        public static class sockaddr_un extends Structure {
            public final static int LENGTH = 106;
            public final static boolean hasLen = Platform.IS_BSD;

            public header sun_header;
            public byte[] sun_path = new byte[LENGTH - 2];

            public static final class header extends Union {
                public static final class famlen extends Structure {
                    public byte sun_len;
                    public byte sun_family;
                }
                public famlen famlen;
                public short sun_family;
                public header() {
                    setType(hasLen ? famlen.class : short.class);
                }
            }

            public void setFamily(int family) {
                if(hasLen) {
                    sun_header.famlen.sun_family = (byte) family;
                } else {
                    sun_header.sun_family = (short) family;
                }
            }

            public int getFamily() {
                if(hasLen) {
                    return sun_header.famlen.sun_family;
                } else {
                    return sun_header.sun_family;
                }
            }
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
        CallbackFactory cfact = runtime.callbackFactory(RubyUNIXSocket.class);

        rb_cUNIXSocket.defineFastMethod("initialize", cfact.getFastMethod("initialize", IRubyObject.class));
        rb_cUNIXSocket.defineFastMethod("path", cfact.getFastMethod("path"));
        rb_cUNIXSocket.defineFastMethod("addr", cfact.getFastMethod("addr"));
        rb_cUNIXSocket.defineFastMethod("peeraddr", cfact.getFastMethod("peeraddr"));
        rb_cUNIXSocket.defineFastMethod("recvfrom", cfact.getFastOptMethod("recvfrom"));
        rb_cUNIXSocket.defineFastMethod("send_io", cfact.getFastMethod("send_io", IRubyObject.class));
        rb_cUNIXSocket.defineFastMethod("recv_io", cfact.getFastOptMethod("recv_io"));
        
        rb_cUNIXSocket.getMetaClass().defineFastMethod("socketpair", cfact.getFastOptSingletonMethod("socketpair"));
        rb_cUNIXSocket.getMetaClass().defineFastMethod("pair", cfact.getFastOptSingletonMethod("socketpair"));
        rb_cUNIXSocket.getMetaClass().defineFastMethod("open", cfact.getFastSingletonMethod("open", IRubyObject.class));
    }

    public RubyUNIXSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    protected int fd;
    protected String fpath;

    protected void rb_sys_fail(String message) {
        rb_sys_fail(getRuntime(), message);
    }

    protected static void rb_sys_fail(Ruby runtime, String message) {
        int n = Native.getLastError();
        Native.setLastError(0);

        IRubyObject arg = (message != null) ? runtime.newString(message) : runtime.getNil();

        RubyClass instance = runtime.getErrno(n);
        if(instance == null) {
            instance = runtime.fastGetClass("SystemCallError");
            throw new RaiseException((RubyException)(instance.newInstance(new IRubyObject[]{arg, runtime.newFixnum(n)}, Block.NULL_BLOCK)));
        } else {
            throw new RaiseException((RubyException)(instance.newInstance(new IRubyObject[]{arg}, Block.NULL_BLOCK)));
        }
    }

    protected final static int F_GETFL = 3;
    protected final static int F_SETFL = 4;

    protected final static int O_NONBLOCK = 0x0004;

    protected void init_unixsock(IRubyObject _path, boolean server) throws Exception {
        int status;
        fd = -1;
        try {
            fd = INSTANCE.socket(RubySocket.AF_UNIX, RubySocket.SOCK_STREAM, 0);
        } catch (UnsatisfiedLinkError ule) { }
        if (fd < 0) {
            rb_sys_fail("socket(2)");
        }

        LibCSocket.sockaddr_un sockaddr = new LibCSocket.sockaddr_un();
        sockaddr.setFamily(RubySocket.AF_UNIX);

        ByteList path = _path.convertToString().getByteList();
        fpath = path.toString();

        if(sockaddr.sun_path.length <= path.realSize) {
            throw getRuntime().newArgumentError("too long unix socket path (max: " + (sockaddr.sun_path.length-1) + "bytes)");
        }

        System.arraycopy(path.bytes, path.begin, sockaddr.sun_path, 0, path.realSize);

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
            rb_sys_fail(fpath);
        }

        if(server) {
            INSTANCE.listen(fd, 5);
        }

        init_sock();

        if(server) {
            openFile.setPath(fpath);
        }
    }

    @Override
    public IRubyObject setsockopt(IRubyObject lev, IRubyObject optname, IRubyObject val) {
        int level = RubyNumeric.fix2int(lev);
        int opt = RubyNumeric.fix2int(optname);

        switch(level) {
        case RubySocket.SOL_SOCKET:
            switch(opt) {
            case RubySocket.SO_KEEPALIVE: {
                int res = INSTANCE.setsockopt(fd, level, opt, asBoolean(val) ? new byte[]{32,0,0,0} : new byte[]{0,0,0,0}, 4);
                if(res == -1) {
                    rb_sys_fail(openFile.getPath());
                }
            }
                break;
            default:
                throw getRuntime().newErrnoENOPROTOOPTError();
            }
            break;
        default:
            throw getRuntime().newErrnoENOPROTOOPTError();
        }

        return getRuntime().newFixnum(0);
    }

    protected void init_sock() throws Exception {
        ModeFlags modes = new ModeFlags(ModeFlags.RDWR);
        openFile.setMainStream(new ChannelStream(getRuntime(), new ChannelDescriptor(new UnixDomainSocketChannel(fd), getNewFileno(), modes, new java.io.FileDescriptor())));
        openFile.setPipeStream(openFile.getMainStream());
        openFile.setMode(modes.getOpenFileFlags());
        openFile.getMainStream().setSync(true);
    }

    public IRubyObject initialize(IRubyObject path) throws Exception {
        init_unixsock(path, false);
        return this;
    }

    private String unixpath(LibCSocket.sockaddr_un addr, IntByReference len) throws Exception {
        int firstZero = 0;
        for(int i=0;i<addr.sun_path.length;i++) {
            if(addr.sun_path[i] == 0) {
                firstZero = i;
                break;
            }
        }

        if(len.getValue()>0) {
            return new String(addr.sun_path, 0, firstZero, "ISO8859-1");
        } else {
            return "";
        }
    }

    private IRubyObject unixaddr(LibCSocket.sockaddr_un addr, IntByReference len) throws Exception {
        return getRuntime().newArrayNoCopy(new IRubyObject[]{getRuntime().newString("AF_UNIX"), getRuntime().newString(unixpath(addr, len))});
    }

    public IRubyObject path() throws Exception {
        if(openFile.getPath() == null) {
            LibCSocket.sockaddr_un addr = new LibCSocket.sockaddr_un();
            IntByReference len = new IntByReference(LibCSocket.sockaddr_un.LENGTH);
            if(INSTANCE.getsockname(fd, addr, len) < 0) {
                rb_sys_fail(null);
            }
            openFile.setPath(unixpath(addr, len));
        }
        return getRuntime().newString(openFile.getPath());
    }

    public IRubyObject addr() throws Exception {
        LibCSocket.sockaddr_un addr = new LibCSocket.sockaddr_un();
        IntByReference len = new IntByReference(LibCSocket.sockaddr_un.LENGTH);
        if(INSTANCE.getsockname(fd, addr, len) < 0) {
            rb_sys_fail("getsockname(2)");
        }
        return unixaddr(addr, len);
    }

    public IRubyObject peeraddr() throws Exception {
        LibCSocket.sockaddr_un addr = new LibCSocket.sockaddr_un();
        IntByReference len = new IntByReference(LibCSocket.sockaddr_un.LENGTH);
        if(INSTANCE.getpeername(fd, addr, len) < 0) {
            rb_sys_fail("getpeername(2)");
        }
        return unixaddr(addr, len);
    }

    public IRubyObject recvfrom(IRubyObject[] args) throws Exception {
        ByteBuffer str = ByteBuffer.allocateDirect(1024);
        LibCSocket.sockaddr_un buf = new LibCSocket.sockaddr_un();
        IntByReference alen = new IntByReference(LibCSocket.sockaddr_un.LENGTH);

        IRubyObject len, flg;

        int flags;

        if(Arity.checkArgumentCount(getRuntime(), args, 1, 2) == 2) {
            flg = args[1];
        } else {
            flg = getRuntime().getNil();
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
            rb_sys_fail("recvfrom(2)");
        }

        if(slen < buflen) {
            buflen = slen;
        }
        byte[] outp = new byte[buflen];
        str.get(outp);
        RubyString _str = getRuntime().newString(new ByteList(outp, 0, buflen, false));

        return getRuntime().newArrayNoCopy(new IRubyObject[]{_str, unixaddr(buf, alen)});
    }

    public IRubyObject send_io(IRubyObject path) {
        //TODO: implement, won't do this now
        return  getRuntime().getNil();
    }

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

    public static IRubyObject open(IRubyObject recv, IRubyObject path) {
        return recv.callMethod(recv.getRuntime().getCurrentContext(), "new", new IRubyObject[]{path}, Block.NULL_BLOCK);
    }

    private static int getSocketType(IRubyObject tp) {
        if(tp instanceof RubyString) {
            String str = tp.toString();
            if("SOCK_STREAM".equals(str)) {
                return RubySocket.SOCK_STREAM;
            } else if("SOCK_DGRAM".equals(str)) {
                return RubySocket.SOCK_DGRAM;
            } else if("SOCK_RAW".equals(str)) {
                return RubySocket.SOCK_RAW;
            } else {
                return -1;
            }
        }
        return RubyNumeric.fix2int(tp);
    }

    public static IRubyObject socketpair(IRubyObject recv, IRubyObject[] args) throws Exception {
        int domain = RubySocket.PF_UNIX;
        Arity.checkArgumentCount(recv.getRuntime(), args, 0, 2);
        
        int type;

        if(args.length == 0) { 
            type = RubySocket.SOCK_STREAM;
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
        int ret = INSTANCE.socketpair(domain, type, protocol, sp);
        if(ret < 0) {
            rb_sys_fail(recv.getRuntime(), "socketpair(2");
        }

        RubyUNIXSocket sock = (RubyUNIXSocket)(recv.getRuntime().fastGetClass("UNIXSocket").callMethod(recv.getRuntime().getCurrentContext(), "allocate", new IRubyObject[0]));
        sock.fd = sp[0];
        sock.init_sock();
        RubyUNIXSocket sock2 = (RubyUNIXSocket)(recv.getRuntime().fastGetClass("UNIXSocket").callMethod(recv.getRuntime().getCurrentContext(), "allocate", new IRubyObject[0]));
        sock2.fd = sp[1];
        sock2.init_sock();

        return recv.getRuntime().newArrayNoCopy(new IRubyObject[]{sock, sock2});
    }
}
