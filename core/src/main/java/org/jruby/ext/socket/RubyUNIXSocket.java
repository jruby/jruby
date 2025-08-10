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
import org.jruby.api.Access;
import org.jruby.api.Define;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
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
import static org.jruby.api.Access.fixnumClass;
import static org.jruby.api.Access.ioClass;
import static org.jruby.api.Check.checkEmbeddedNulls;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Create.*;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;


@JRubyClass(name="UNIXSocket", parent="BasicSocket")
public class RubyUNIXSocket extends RubyBasicSocket {
    static RubyClass createUNIXSocket(ThreadContext context, RubyClass BasicSocket) {
        return Define.defineClass(context, "UNIXSocket", BasicSocket, RubyUNIXSocket::new).
                defineMethods(context, RubyUNIXSocket.class);
    }

    public RubyUNIXSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject for_fd(ThreadContext context, IRubyObject recv, IRubyObject _fileno) {
        int fileno = toInt(context, _fileno);

        RubyClass klass = (RubyClass)recv;
        RubyUNIXSocket unixSocket = (RubyUNIXSocket)(Helpers.invoke(context, klass, "allocate"));
        UnixSocketChannel channel = UnixSocketChannel.fromFD(fileno);
        unixSocket.init_sock(context.runtime, channel);
        return unixSocket;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject path) {
        init_unixsock(context, path, false);

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject path(ThreadContext context) {
        return newEmptyString(context);
    }

    @JRubyMethod
    public IRubyObject addr(ThreadContext context) {
        return newArray(context, newString(context, "AF_UNIX"), newEmptyString(context));
    }

    @JRubyMethod
    public IRubyObject peeraddr(ThreadContext context) {
        final String _path = getUnixRemoteSocket(context).path();
        final RubyString path = _path == null ? newEmptyString(context) : newString(context, _path);
        return newArray(context, newString(context, "AF_UNIX"), path);
    }

    @JRubyMethod(name = "recvfrom", required = 1, optional = 1, checkArity = false)
    public IRubyObject recvfrom(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 1, 2);
        IRubyObject _length = args[0];
        IRubyObject _flags = argc == 2 ? args[1] : context.nil;
        int flags = _flags.isNil() ? 0 : toInt(context, _flags); // TODO

        return newArray(context, recv(context, _length), peeraddr(context));
    }

    @JRubyMethod
    public IRubyObject send_io(ThreadContext context, IRubyObject arg) {
        final POSIX posix = context.runtime.getPosix();
        OpenFile fptr = getOpenFileChecked();
        int fd;

        if (arg.callMethod(context, "kind_of?", ioClass(context)).isTrue()) {
          fd = ((RubyIO) arg).getOpenFileChecked().getFileno();
        } else if (arg.callMethod(context, "kind_of?", fixnumClass(context)).isTrue()) {
          fd = ((RubyFixnum) arg).asInt(context);
        } else {
          throw typeError(context, "neither IO nor file descriptor");
        }

        if (FilenoUtil.isFake(fd)) throw typeError(context, "file descriptor is not native");

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
                    throw context.runtime.newErrnoFromInt(posix.errno(), "sendmsg(2)");
                }
            }
        } finally {
            if (locked) fptr.unlock();
        }

        return context.nil;
    }

    @JRubyMethod(optional = 2, checkArity = false)
    public IRubyObject recv_io(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 0, 2);
        final POSIX posix = context.runtime.getPosix();
        OpenFile fptr = getOpenFileChecked();
        IRubyObject klass = argc > 0 ? args[0] : ioClass(context);
        IRubyObject mode = argc > 1 ? args[1] : context.nil;
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
                    throw context.runtime.newErrnoFromInt(posix.errno(), "recvmsg(2)");
                }
            }
        } finally {
            if (locked) fptr.unlock();
        }


        ByteBuffer inFdBuf = inMessage.getControls()[0].getData();
        inFdBuf.order(ByteOrder.nativeOrder());

        IRubyObject fd = asFixnum(context, inFdBuf.getInt());

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

    @JRubyMethod(name = {"socketpair", "pair"}, optional = 2, checkArity = false, meta = true)
    public static IRubyObject socketpair(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 2);

        final Ruby runtime = context.runtime;

        // TODO: type and protocol

        UnixSocketChannel[] sp;

        try {
            sp = UnixSocketChannel.pair();

            final RubyClass UNIXSocket = Access.getClass(context, "UNIXSocket");
            RubyUNIXSocket sock = (RubyUNIXSocket)(Helpers.invoke(context, UNIXSocket, "allocate"));
            sock.init_sock(runtime, sp[0], "");

            RubyUNIXSocket sock2 = (RubyUNIXSocket)(Helpers.invoke(context, UNIXSocket, "allocate"));
            sock2.init_sock(runtime, sp[1], "");

            return newArray(context, sock, sock2);

        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }
    }

    @Override
    public IRubyObject setsockopt(ThreadContext context, IRubyObject _level, IRubyObject _opt, IRubyObject val) {
        SocketLevel level = SocketUtils.levelFromArg(context, _level);
        SocketOption opt = SocketUtils.optionFromArg(context, _opt);

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

        return asFixnum(context, 0);
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

    protected void init_unixsock(ThreadContext context, IRubyObject _path, boolean server) {
        RubyString strPath = unixsockPathValue(context, _path);
        ByteList path = strPath.getByteList();
        String fpath = Helpers.decodeByteList(context.runtime, path);

        int maxSize = 103; // Max size from Darwin, lowest common value we know of
        if (fpath.length() > 103) throw argumentError(context, "too long unix socket path (max: " + maxSize + "bytes)");

        Closeable closeable = null;
        try {
            if (server) {
                UnixServerSocketChannel channel = UnixServerSocketChannel.open();
                UnixServerSocket socket = channel.socket();
                closeable = channel;

                // TODO: listen backlog

                socket.bind(new UnixSocketAddress(new File(fpath)));

                init_sock(context.runtime, channel, fpath);

            } else {
                File fpathFile = new File(fpath);

                if (!fpathFile.exists()) {
                    throw context.runtime.newErrnoENOENTError("unix socket");
                }

                UnixSocketChannel channel = UnixSocketChannel.open();
                closeable = channel;

                channel.connect(new UnixSocketAddress(fpathFile));

                init_sock(context.runtime, channel);

            }

            // initialized cleanly, clear closeable
            closeable = null;

        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        } finally {
            if (closeable != null) {
                try {closeable.close();} catch (IOException ioe2) {}
            }
        }
    }

    // MRI: unixsock_path_value
    private static RubyString unixsockPathValue(ThreadContext context, IRubyObject path) {
        return checkEmbeddedNulls(context, path.convertToString());
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
