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
 * Copyright (C) 2008 Ola Bini <ola.bini@gmail.com>
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

import jnr.constants.platform.Fcntl;
import jnr.constants.platform.OpenFlags;
import jnr.constants.platform.SocketLevel;
import jnr.constants.platform.SocketOption;
import jnr.ffi.LastError;
import jnr.posix.CmsgHdr;
import jnr.posix.MsgHdr;
import jnr.posix.POSIX;
import jnr.unixsocket.UnixServerSocket;
import jnr.unixsocket.UnixServerSocketChannel;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyIO;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;
import org.jruby.util.io.FilenoUtil;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.OpenFile;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channel;

import static com.headius.backport9.buffer.Buffers.flipBuffer;


@JRubyClass(name="UNIXSocket", parent="BasicSocket")
public class RubyUNIXSocket extends RubyBasicSocket {
    static void createUNIXSocket(Ruby runtime) {
        RubyClass rb_cUNIXSocket = runtime.defineClass("UNIXSocket", runtime.getClass("BasicSocket"), RubyUNIXSocket::new);
        runtime.getObject().setConstant("UNIXsocket", rb_cUNIXSocket);

        rb_cUNIXSocket.defineAnnotatedMethods(RubyUNIXSocket.class);
    }

    public RubyUNIXSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject for_fd(ThreadContext context, IRubyObject recv, IRubyObject _fileno) {
        Ruby runtime = context.runtime;
        int fileno = (int)_fileno.convertToInteger().getLongValue();

        RubyClass klass = (RubyClass)recv;
        RubyUNIXSocket unixSocket = (RubyUNIXSocket)(Helpers.invoke(context, klass, "allocate"));
        UnixSocketChannel channel = UnixSocketChannel.fromFD(fileno);
        unixSocket.init_sock(runtime, channel);
        return unixSocket;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject path) {
        init_unixsock(context.runtime, path, false);

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject path(ThreadContext context) {
        return RubyString.newEmptyString(context.runtime);
    }

    @JRubyMethod
    public IRubyObject addr(ThreadContext context) {
        final Ruby runtime = context.runtime;

        return runtime.newArray( runtime.newString("AF_UNIX"),  RubyString.newEmptyString(runtime) );
    }

    @JRubyMethod
    public IRubyObject peeraddr(ThreadContext context) {
        final Ruby runtime = context.runtime;
        final String _path = getUnixRemoteSocket().path();
        final RubyString path = (_path == null) ? RubyString.newEmptyString(runtime) : runtime.newString(_path);
        return runtime.newArray( runtime.newString("AF_UNIX"), path );
    }

    @JRubyMethod(name = "recvfrom", required = 1, optional = 1)
    public IRubyObject recvfrom(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        IRubyObject _length = args[0];
        IRubyObject _flags;

        if(args.length == 2) {
            _flags = args[1];
        } else {
            _flags = runtime.getNil();
        }

        // TODO
        int flags;

        _length = args[0];

        if(_flags.isNil()) {
            flags = 0;
        } else {
            flags = RubyNumeric.fix2int(_flags);
        }

        return runtime.newArray(
                recv(context, _length),
                peeraddr(context));
    }

    @JRubyMethod
    public IRubyObject send_io(ThreadContext context, IRubyObject arg) {
        final Ruby runtime = context.runtime;
        final POSIX posix = runtime.getPosix();
        OpenFile fptr = getOpenFileChecked();
        int fd;

        if (arg.callMethod(context, "kind_of?", runtime.getIO()).isTrue()) {
          fd = ((RubyIO) arg).getOpenFileChecked().getFileno();
        } else if (arg.callMethod(context, "kind_of?", runtime.getFixnum()).isTrue()) {
          fd = ((RubyFixnum) arg).getIntValue();
        } else {
          throw runtime.newTypeError("neither IO nor file descriptor");
        }

        if (FilenoUtil.isFake(fd)) {
          throw runtime.newTypeError("file descriptor is not native");
        }

        byte[] dataBytes = new byte[1];
        dataBytes[0] = 0;

        MsgHdr outMessage = posix.allocateMsgHdr();

        ByteBuffer[] outIov = new ByteBuffer[1];
        outIov[0] = ByteBuffer.allocateDirect(dataBytes.length);
        outIov[0].put(dataBytes);
        flipBuffer(outIov[0]);

        outMessage.setIov(outIov);

        CmsgHdr outControl = outMessage.allocateControl(4);
        outControl.setLevel(SocketLevel.SOL_SOCKET.intValue());
        outControl.setType(0x01);

        ByteBuffer fdBuf = ByteBuffer.allocateDirect(4);
        fdBuf.order(ByteOrder.nativeOrder());
        fdBuf.putInt(0, fd);
        outControl.setData(fdBuf);

        boolean locked = fptr.lock();
        try {
            while (posix.sendmsg(fptr.getFileno(), outMessage, 0) == -1) {
                if (!fptr.waitWritable(context)) {
                    throw runtime.newErrnoFromInt(posix.errno(), "sendmsg(2)");
                }
            }
        } finally {
            if (locked) fptr.unlock();
        }

        return runtime.getNil();
    }

    @JRubyMethod(optional = 2)
    public IRubyObject recv_io(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.runtime;
        final POSIX posix = runtime.getPosix();
        OpenFile fptr = getOpenFileChecked();

        IRubyObject klass = runtime.getIO();
        IRubyObject mode = runtime.getNil();
        if (args.length > 0) {
            klass = args[0];
        }
        if (args.length > 1) {
            mode = args[1];
        }

        MsgHdr inMessage = posix.allocateMsgHdr();
        ByteBuffer[] inIov = new ByteBuffer[1];
        inIov[0] = ByteBuffer.allocateDirect(1);
        inMessage.setIov(inIov);

        CmsgHdr inControl = inMessage.allocateControl(4);
        inControl.setLevel(SocketLevel.SOL_SOCKET.intValue());
        inControl.setType(0x01);

        ByteBuffer fdBuf = ByteBuffer.allocateDirect(4);
        fdBuf.order(ByteOrder.nativeOrder());
        fdBuf.putInt(0, -1);
        inControl.setData(fdBuf);

        boolean locked = fptr.lock();
        try {
            while (posix.recvmsg(fptr.getFileno(), inMessage, 0) == -1) {
                if (!fptr.waitReadable(context)) {
                    throw runtime.newErrnoFromInt(posix.errno(), "recvmsg(2)");
                }
            }
        } finally {
            if (locked) fptr.unlock();
        }


        ByteBuffer inFdBuf = inMessage.getControls()[0].getData();
        inFdBuf.order(ByteOrder.nativeOrder());

        IRubyObject fd = runtime.newFixnum(inFdBuf.getInt());

        if (klass.isNil()) {
            return fd;
        } else {
            if (mode.isNil()) {
              return Helpers.invoke(context, klass, "for_fd", fd);
            } else {
              return Helpers.invoke(context, klass, "for_fd", fd, mode);
            }
        }
    }

    @JRubyMethod(name = {"socketpair", "pair"}, optional = 2, meta = true)
    public static IRubyObject socketpair(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        final Ruby runtime = context.runtime;

        // TODO: type and protocol

        UnixSocketChannel[] sp;

        try {
            sp = UnixSocketChannel.pair();

            final RubyClass UNIXSocket = runtime.getClass("UNIXSocket");
            RubyUNIXSocket sock = (RubyUNIXSocket)(Helpers.invoke(context, UNIXSocket, "allocate"));
            sock.init_sock(runtime, sp[0], "");

            RubyUNIXSocket sock2 = (RubyUNIXSocket)(Helpers.invoke(context, UNIXSocket, "allocate"));
            sock2.init_sock(runtime, sp[1], "");

            return runtime.newArray(sock, sock2);

        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }
    }

    @Override
    public IRubyObject setsockopt(ThreadContext context, IRubyObject _level, IRubyObject _opt, IRubyObject val) {
        SocketLevel level = SocketUtils.levelFromArg(_level);
        SocketOption opt = SocketUtils.optionFromArg(_opt);

        switch(level) {
            case SOL_SOCKET:
                switch(opt) {
                    case SO_KEEPALIVE: {
                        // TODO: socket options
                    }
                    break;
                    default:
                        throw context.runtime.newErrnoENOPROTOOPTError();
                }
                break;
            default:
                throw context.runtime.newErrnoENOPROTOOPTError();
        }

        return context.runtime.newFixnum(0);
    }

    protected static void rb_sys_fail(Ruby runtime, String message) {
        final int n = LastError.getLastError(jnr.ffi.Runtime.getSystemRuntime());

        RubyClass instance = runtime.getErrno(n);

        if(instance == null) {
            throw runtime.newSystemCallError(message);

        } else {
            throw runtime.newErrnoFromInt(n, message);
        }
    }

    protected void init_unixsock(Ruby runtime, IRubyObject _path, boolean server) {
        RubyString strPath = unixsockPathValue(runtime, _path);
        ByteList path = strPath.getByteList();
        String fpath = Helpers.decodeByteList(runtime, path);

        int maxSize = 103; // Max size from Darwin, lowest common value we know of
        if (fpath.length() > 103) {
            throw runtime.newArgumentError("too long unix socket path (max: " + maxSize + "bytes)");
        }

        Closeable closeable = null;
        try {
            if (server) {
                UnixServerSocketChannel channel = UnixServerSocketChannel.open();
                UnixServerSocket socket = channel.socket();
                closeable = channel;

                // TODO: listen backlog

                socket.bind(new UnixSocketAddress(new File(fpath)));

                init_sock(runtime, channel, fpath);

            } else {
                File fpathFile = new File(fpath);

                if (!fpathFile.exists()) {
                    throw runtime.newErrnoENOENTError("unix socket");
                }

                UnixSocketChannel channel = UnixSocketChannel.open();
                closeable = channel;

                channel.connect(new UnixSocketAddress(fpathFile));

                init_sock(runtime, channel);

            }

            // initialized cleanly, clear closeable
            closeable = null;

        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        } finally {
            if (closeable != null) {
                try {closeable.close();} catch (IOException ioe2) {}
            }
        }
    }

    // MRI: unixsock_path_value
    private static RubyString unixsockPathValue(Ruby runtime, IRubyObject path) {
        return StringSupport.checkEmbeddedNulls(runtime, path.convertToString());
    }


    protected void init_sock(Ruby runtime, Channel channel, String path) {
        MakeOpenFile();

        ModeFlags modes = newModeFlags(runtime, ModeFlags.RDWR);

        openFile.setFD(newChannelFD(runtime, channel));
        openFile.setMode(modes.getOpenFileFlags());
        openFile.setSync(true);
        openFile.setPath(path);
    }

    protected void init_sock(Ruby runtime, Channel channel) {
        init_sock(runtime, channel, null);
    }

    //private UnixSocketChannel asUnixSocket() {
    //    return (UnixSocketChannel)getOpenFile().fd().ch;
    //}

    protected final static int F_GETFL = Fcntl.F_GETFL.intValue();
    protected final static int F_SETFL = Fcntl.F_SETFL.intValue();

    protected final static int O_NONBLOCK = OpenFlags.O_NONBLOCK.intValue();
}
