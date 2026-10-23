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
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
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

import jnr.constants.platform.Errno;
import jnr.constants.platform.Fcntl;
import jnr.constants.platform.IPProto;
import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.SocketLevel;
import jnr.constants.platform.SocketOption;
import jnr.ffi.LibraryLoader;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.byref.IntByReference;
import jnr.posix.Timeval;
import jnr.posix.POSIX;
import jnr.unixsocket.UnixSocketAddress;
import org.jruby.FiberScheduler;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyThread;
import org.jruby.runtime.Visibility;
import jnr.constants.platform.Sock;
import org.jruby.RubyArray;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyIO;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.api.Access;
import org.jruby.api.Convert;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.fcntl.FcntlLibrary;
import org.jruby.platform.Platform;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Pack;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.BadDescriptorException;
import org.jruby.util.io.ChannelFD;
import org.jruby.util.io.OpenFile;
import org.jruby.util.io.Sockaddr;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SelectableChannel;

import static jnr.constants.platform.TCP.TCP_INFO;
import static jnr.constants.platform.TCP.TCP_KEEPCNT;
import static jnr.constants.platform.TCP.TCP_KEEPIDLE;
import static jnr.constants.platform.TCP.TCP_KEEPINTVL;
import static jnr.constants.platform.TCP.TCP_NODELAY;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Convert.toLong;
import static org.jruby.api.Create.*;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.runtime.Helpers.extractExceptionOnlyArg;
import static org.jruby.runtime.Helpers.throwErrorFromException;
import static com.headius.backport9.buffer.Buffers.flipBuffer;

/**
 * Implementation of the BasicSocket class from Ruby.
 */
@JRubyClass(name="BasicSocket", parent="IO")
public class RubyBasicSocket extends RubyIO {
    static RubyClass createBasicSocket(ThreadContext context, RubyClass IO) {
        return defineClass(context, "BasicSocket", IO, RubyBasicSocket::new).
                defineMethods(context, RubyBasicSocket.class).
                undefMethods(context, "initialize");
    }

    public RubyBasicSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
        doNotReverseLookup = runtime.isDoNotReverseLookupEnabled();
    }

    @JRubyMethod(meta = true)
    public static IRubyObject for_fd(ThreadContext context, IRubyObject _klass, IRubyObject _fileno) {
        Ruby runtime = context.runtime;
        int fileno = toInt(context, _fileno);
        RubyClass klass = (RubyClass)_klass;

        ChannelFD fd = runtime.getFilenoUtil().getWrapperFromFileno(fileno);

        RubyBasicSocket basicSocket = (RubyBasicSocket)klass.getAllocator().allocate(runtime, klass);
        basicSocket.initSocket(fd);

        return basicSocket;
    }

    @JRubyMethod(name = "do_not_reverse_lookup")
    public IRubyObject do_not_reverse_lookup(ThreadContext context) {
        return Convert.asBoolean(context, doNotReverseLookup);
    }

    @JRubyMethod(name = "do_not_reverse_lookup=")
    public IRubyObject set_do_not_reverse_lookup(ThreadContext context, IRubyObject flag) {
        doNotReverseLookup = flag.isTrue();
        return do_not_reverse_lookup(context);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject do_not_reverse_lookup(ThreadContext context, IRubyObject recv) {
        return Convert.asBoolean(context, context.runtime.isDoNotReverseLookupEnabled());
    }

    @JRubyMethod(name = "do_not_reverse_lookup=", meta = true)
    public static IRubyObject set_do_not_reverse_lookup(ThreadContext context, IRubyObject recv, IRubyObject flag) {
        context.runtime.setDoNotReverseLookupEnabled(flag.isTrue());

        return flag;
    }

    @JRubyMethod(name = "send")
    public IRubyObject send(ThreadContext context, IRubyObject _mesg, IRubyObject _flags) {
        return doSend(context, _mesg, _flags, null);
    }

    @JRubyMethod(name = "send")
    public IRubyObject send(ThreadContext context, IRubyObject _mesg, IRubyObject _flags, IRubyObject _to) {
        return doSend(context, _mesg, _flags, _to);
    }

    /**
     * send(2) with flags (MSG_OOB, ...) on the real fd of a connected socket:
     * java.nio has no way to pass send flags.
     */
    private int sendWithFlags(ThreadContext context, ByteList mesg, int flags) {
        int fd = getOpenFileChecked().fd().realFileno;
        if (fd < 0 || !UnixDomainNative.hasDgram()) {
            throw context.runtime.newNotImplementedError("send flags need native socket access (--enable-native-access)");
        }
        try {
            return (int) UnixDomainNative.send(fd, mesg.unsafeBytes(), mesg.begin(), mesg.realSize(), flags);
        } catch (UnixDomainNative.NativeException ne) {
            throw context.runtime.newErrnoFromInt(ne.errno, "send(2)");
        }
    }

    /**
     * send(2) on a connected DatagramChannel. The JDK's send()/write() return 0
     * without a syscall for an empty buffer, so a zero-length datagram -- which
     * MRI's send(2) does transmit -- goes straight to the fd via FFM.
     */
    protected int sendConnectedDatagram(ThreadContext context, DatagramChannel dgram, ByteBuffer buf) throws IOException {
        if (buf.hasRemaining()) return dgram.write(buf);

        int fd = getOpenFile().fd().realFileno;
        if (fd < 0 || !UnixDomainNative.hasDgram()) return 0; // no real fd/FFM: old behavior

        try {
            return (int) UnixDomainNative.sendTo(fd, ByteList.NULL_ARRAY, 0, 0, null);
        } catch (UnixDomainNative.NativeException ne) {
            throw context.runtime.newErrnoFromInt(ne.errno, "send(2)");
        }
    }

    private IRubyObject doSend(ThreadContext context, IRubyObject _mesg, IRubyObject _flags, IRubyObject _to) {
        final SocketAddress sockaddr;

        if (_to instanceof Addrinfo) {
            Addrinfo addr = (Addrinfo) _to;
            sockaddr = addr.getSocketAddress();
        } else if (_to == null || _to.isNil()) {
            sockaddr = null;
        } else {
            sockaddr = Sockaddr.addressFromSockaddr(context, _to);
        }

        RubyString mesg = _mesg.convertToString();
        ByteList mesgByteList = mesg.getByteList();
        ByteBuffer mesgBytes = ByteBuffer.wrap(mesgByteList.unsafeBytes(), mesgByteList.begin(), mesgByteList.realSize());

        Channel channel = getChannel();

        int written = 0;

        int msgFlags = flagsArg(context, _flags);
        if (msgFlags != 0 && sockaddr == null && nativeFlagsFd() >= 0) return asFixnum(context, sendWithFlags(context, mesgByteList, msgFlags));

        if (channel instanceof DatagramChannel dgram && sockaddr == null && !dgram.isConnected()) {
            throw context.runtime.newErrnoEDESTADDRREQError("send(2)"); // MRI; was ENOTCONN via syswrite
        }

        try {
            if (channel instanceof DatagramChannel && sockaddr != null) {
                written = ((DatagramChannel) channel).send(mesgBytes, sockaddr);

                return asFixnum(context, written);
            } else if (channel instanceof UnixFdChannel ufc && sockaddr != null) {
                // raw Socket.new(:UNIX, :DGRAM) fd -- see RubySocket#initChannelFD.
                // sockaddr here is jnr.unixsocket.UnixSocketAddress, same as
                // doBind (Sockaddr's UNIX address parsing predates this PR).
                String destPath = sockaddr instanceof UnixSocketAddress usa ? usa.path() : null;
                if (destPath == null) throw context.runtime.newErrnoENOPROTOOPTError();
                try {
                    long n = UnixDomainNative.sendTo(ufc.fd(), mesgByteList.unsafeBytes(),
                            mesgByteList.begin(), mesgByteList.realSize(), destPath);
                    return asFixnum(context, (int) n);
                } catch (UnixDomainNative.NativeException ne) {
                    throw context.runtime.newErrnoFromInt(ne.errno, "sendto(2)");
                }
            } else if (channel instanceof DatagramChannel dgram && dgram.isConnected()) {
                // Not syswrite: it skips the syscall for an empty string.
                return asFixnum(context, sendConnectedDatagram(context, dgram, mesgBytes));
            } else if (channel instanceof UnixFdChannel ufc && ufc.sockType() == UnixDomainNative.SOCK_DGRAM) {
                // Same for a connected unix dgram socket (UNIXSocket.pair(:DGRAM)):
                // a real send(2), which unlike write(2) sends a zero-length datagram.
                try {
                    long n = UnixDomainNative.sendTo(ufc.fd(), mesgByteList.unsafeBytes(),
                            mesgByteList.begin(), mesgByteList.realSize(), null);
                    return asFixnum(context, (int) n);
                } catch (UnixDomainNative.NativeException ne) {
                    throw context.runtime.newErrnoFromInt(ne.errno, "send(2)");
                }
            } else {
                return syswrite(context, _mesg);
            }
        } catch (Exception e) {
            throwErrorFromException(context.runtime, e);
            return null; // not reached
        }
    }

    @JRubyMethod
    public IRubyObject recv(ThreadContext context, IRubyObject length) {
        return recv(context, length, null, null);
    }

    @JRubyMethod(required = 1, optional = 2, checkArity = false) // (length) required = 1 handled above
    public IRubyObject recv(ThreadContext context, IRubyObject[] args) {
        // MRI: recv(maxlen, flags = 0, outbuf = nil)
        IRubyObject length; RubyString str = null; IRubyObject flags = null;

        switch (args.length) {
            case 3:
                if (!args[2].isNil()) str = args[2].convertToString();
            case 2:
                flags = args[1];
            case 1:
                length = args[0];
                break;
            default:
                throw argumentError(context, args.length, 1, 3);
        }

        return recv(context, length, str, flags);
    }

    private IRubyObject recv(ThreadContext context, IRubyObject length,
                             RubyString str, IRubyObject flags) {
        int len = toInt(context, length);
        int msgFlags = flagsArg(context, flags);

        if (msgFlags != 0 && nativeFlagsFd() >= 0) {
            ByteList bytes = recvWithFlags(context, len, msgFlags, false);
            if (bytes == NONBLOCK_EOF) return context.nil;
            return returnBytes(context, bytes, str);
        }

        final ByteBuffer buffer = ByteBuffer.allocate(len);

        ByteList bytes;

        Channel channel = getChannel();

        if (channel instanceof DatagramChannel) {
            try {
                DatagramChannel dgram = (DatagramChannel) channel;

                receiveDatagram(context, dgram, buffer);

                flipBuffer(buffer);
                bytes = new ByteList(buffer.array(), buffer.position(), buffer.limit());
            } catch (Exception e) {
                throwErrorFromException(context.runtime, e);
                return null; // not reached
            }
        } else {
            bytes = doRead(context, buffer);
            if (bytes == null) return context.nil;
        }

        return returnBytes(context, bytes, str);
    }

    /**
     * Data read into a caller-supplied buffer keeps the buffer's encoding (MRI
     * replaces only the bytes); a new string is binary.
     */
    private static IRubyObject returnBytes(ThreadContext context, ByteList bytes, IRubyObject str) {
        if (str != null && !str.isNil()) {
            RubyString buf = str.convertToString();
            org.jcodings.Encoding enc = buf.getEncoding();
            buf.setValue(bytes);
            buf.setEncoding(enc);
            return buf;
        }

        return newString(context, bytes);
    }

    // Send/recv flags go to the kernel on the real fd; without native access (Java 21, or
    // denied) or a real fd they are ignored, as JRuby always did before.
    private int nativeFlagsFd() {
        return UnixDomainNative.hasDgram() ? getOpenFileChecked().fd().realFileno : -1;
    }

    private static int flagsArg(ThreadContext context, IRubyObject flags) {
        return flags == null || flags.isNil() ? 0 : toInt(context, flags);
    }

    /**
     * recv(2) with flags (MSG_PEEK, MSG_OOB, ...) on the real fd: java.nio has
     * no way to pass recv flags. Always issued with MSG_DONTWAIT; a blocking
     * call waits through OpenFile#waitReadable so Thread#kill/raise and
     * Timeout still work. Returns null when nonBlocking and nothing is
     * available, NONBLOCK_EOF at a stream socket's EOF.
     */
    private ByteList recvWithFlags(ThreadContext context, int len, int flags, boolean nonBlocking) {
        if (len < 0) throw argumentError(context, "negative length " + len + " given");
        OpenFile fptr = getOpenFileChecked();
        int fd = fptr.fd().realFileno;
        if (fd < 0 || !UnixDomainNative.hasDgram()) {
            throw context.runtime.newNotImplementedError("recv flags need native socket access (--enable-native-access)");
        }

        while (true) {
            byte[] bytes;
            try {
                bytes = UnixDomainNative.recv(fd, len, flags | UnixDomainNative.MSG_DONTWAIT);
            } catch (UnixDomainNative.NativeException ne) {
                if (ne.errno != UnixDomainNative.EAGAIN) throw context.runtime.newErrnoFromInt(ne.errno, "recvfrom(2)");
                if (nonBlocking) return null;
                waitFd(context, fd, UnixDomainNative.POLLIN);
                continue;
            }
            if (bytes.length == 0 && len > 0 && (sockBits() & SOCK_DGRAM) == 0) return NONBLOCK_EOF;
            return new ByteList(bytes, false);
        }
    }

    // WHY: facts about this socket that never change once it is open --
    // unix vs inet, stream vs dgram, whether a real OS fd is known --
    // computed once, then read lock-free on every call that needs them, the
    // same shape as Builtins' builtinBits (plain int reads on the hot path,
    // a rare write). Previously recomputed per call: an instanceof chain plus
    // a getsockopt(SO_TYPE) syscall on every flagged recv at EOF. A racing
    // first computation writes the same value from every thread, and an int
    // write is atomic (JLS 17.7), so no lock. Not cached while a java.nio
    // socket is still unbound/unconnected: its family is only knowable from
    // its address.
    private static final int SOCK_KNOWN = 1, SOCK_UNIX = 1 << 1, SOCK_DGRAM = 1 << 2, SOCK_NATIVE_FD = 1 << 3;
    private int sockBits;

    private int sockBits() {
        int bits = sockBits;
        if (bits != 0) return bits;

        Channel channel = getChannel();
        int fd = getOpenFileChecked().fd().realFileno;
        boolean settled = true;

        bits = SOCK_KNOWN;
        if (fd >= 0) bits |= SOCK_NATIVE_FD;
        if (channel instanceof UnixFdChannel || channel instanceof jnr.unixsocket.UnixSocketChannel
                || channel instanceof jnr.unixsocket.UnixServerSocketChannel || UnixDomain.isUnixChannel(channel)) {
            bits |= SOCK_UNIX;
        } else if (channel instanceof NetworkChannel net) {
            try {
                settled = net.getLocalAddress() != null;
            } catch (IOException e) {
                settled = false;
            }
        }
        if (channel instanceof DatagramChannel
                || (fd >= 0 && !(channel instanceof NetworkChannel) && UnixDomainNative.socketType(fd) == UnixDomainNative.SOCK_DGRAM)) {
            bits |= SOCK_DGRAM;
        }

        if (settled) sockBits = bits;
        return bits;
    }

    @JRubyMethod(required = 1, optional = 3, checkArity = false)
    public IRubyObject recv_nonblock(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 1, 4);

        boolean exception = true;
        Ruby runtime = context.runtime;
        IRubyObject opts = ArgsUtil.getOptionsArg(runtime, args);
        if (opts != context.nil) {
            argc--;
            exception = extractExceptionOnlyArg(context, (RubyHash) opts);
        }

        IRubyObject length, flags, str;
        length = flags = context.nil; str = null;

        switch (argc) {
            case 3: str = args[2];
            case 2: flags = args[1];
            case 1: length = args[0];
        }

        int msgFlags = flagsArg(context, flags);
        if (msgFlags != 0 && nativeFlagsFd() >= 0) {
            ByteList bytes = recvWithFlags(context, toInt(context, length), msgFlags, true);
            if (bytes == NONBLOCK_EOF) return context.nil;
            return handleReturnBytes(context, bytes, str, "recvfrom(2)", exception);
        }

        final ByteBuffer buffer = ByteBuffer.allocate(toInt(context, length));

        ByteList bytes;

        Channel channel = getChannel();

        if (channel instanceof DatagramChannel) {
            DatagramChannel dgram = (DatagramChannel) channel;
            boolean wasBlocking = dgram.isBlocking();
            try {
                getOpenFile().setBlocking(runtime, false);

                // receive() returns null only when no datagram was waiting; a
                // zero-length datagram is real data ("" in MRI), not EAGAIN.
                if (dgram.receive(buffer) == null) {
                    bytes = null;
                } else {
                    flipBuffer(buffer);
                    bytes = new ByteList(buffer.array(), buffer.position(), buffer.limit());
                }
            } catch (Exception e) {
                // exception: false only silences "would block" (null above)
                throwErrorFromException(runtime, e);
                return context.nil; // not reached
            } finally {
                // MRI uses recvfrom(MSG_DONTWAIT): the IO's own mode is untouched
                if (wasBlocking) getOpenFile().setBlocking(runtime, true);
            }
        } else {
            bytes = doReadNonblock(context, buffer, exception);
        }

        if (bytes == NONBLOCK_EOF) return context.nil; // MRI: nil once the peer has shut down

        return handleReturnBytes(context, bytes, str, "recvfrom(2)", exception);
    }

    private IRubyObject handleReturnBytes(ThreadContext context, ByteList bytes, IRubyObject str, String exMessage, boolean exception) {
        if (bytes == null) {
            if (!exception) return Convert.asSymbol(context, "wait_readable");
            throw context.runtime.newErrnoEAGAINReadableError(exMessage);
        }

        return returnBytes(context, bytes, str);
    }

    // WHY: IO#write_nonblock flips the IO into nonblock mode and leaves it
    // there. MRI's socket write_nonblock on Linux uses send(MSG_DONTWAIT), so
    // the socket's own mode is untouched (basicsocket/write_nonblock_spec
    // "does not set the IO in nonblock mode"); restore it afterward.
    @Override
    @JRubyMethod(name = "write_nonblock")
    public IRubyObject write_nonblock(ThreadContext context, IRubyObject arg0) {
        boolean wasBlocking = getOpenFileChecked().isBlocking();
        try {
            return super.write_nonblock(context, arg0);
        } finally {
            if (Platform.IS_LINUX && wasBlocking) getOpenFileChecked().setBlocking(context.runtime, true);
        }
    }

    @Override
    @JRubyMethod(name = "write_nonblock")
    public IRubyObject write_nonblock(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        boolean wasBlocking = getOpenFileChecked().isBlocking();
        try {
            return super.write_nonblock(context, arg0, arg1);
        } finally {
            if (Platform.IS_LINUX && wasBlocking) getOpenFileChecked().setBlocking(context.runtime, true);
        }
    }

    @Override
    @JRubyMethod
    public IRubyObject read_nonblock(ThreadContext context, IRubyObject arg0) {
        Channel channel = getChannel();

        if (!(channel instanceof DatagramChannel)) return super.read_nonblock(context, arg0);
        if (!ArgsUtil.getOptionsArg(context, arg0).isNil()) throw argumentError(context, 0, 1, 3);

        return readNonblockCommon(context, (DatagramChannel) channel, arg0, context.nil, true);
    }

    @Override
    @JRubyMethod
    public IRubyObject read_nonblock(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        Channel channel = getChannel();

        if (!(channel instanceof DatagramChannel)) return super.read_nonblock(context, arg0, arg1);

        boolean exception = true;
        IRubyObject str = context.nil;
        IRubyObject maybeHash = ArgsUtil.getOptionsArg(context, arg1);

        if (maybeHash.isNil()) {
            str = arg1;
        } else {
            exception = ArgsUtil.extractKeywordArg(context, "exception", maybeHash) != context.fals;
        }

        return readNonblockCommon(context, (DatagramChannel) channel, arg0, str, exception);
    }

    @Override
    @JRubyMethod
    public IRubyObject read_nonblock(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        Channel channel = getChannel();

        if (!(channel instanceof DatagramChannel)) return super.read_nonblock(context, arg0, arg1, arg2);

        IRubyObject maybeHash = ArgsUtil.getOptionsArg(context, arg2);
        boolean exception = !maybeHash.isNil() ?
                ArgsUtil.extractKeywordArg(context, "exception", maybeHash) != context.fals : true;

        return readNonblockCommon(context, (DatagramChannel) channel, arg0, arg1, exception);
    }

    private IRubyObject readNonblockCommon(ThreadContext context, DatagramChannel channel, IRubyObject length, IRubyObject str, boolean exception) {
        final ByteBuffer buffer = ByteBuffer.allocate(toInt(context, length));

        ByteList bytes;

        try {
            DatagramChannel dgram = channel;

            getOpenFile().setBlocking(context.runtime, false);

            if (dgram.receive(buffer) == null) {
                bytes = null;
            } else if (buffer.position() == 0) {
                // MRI's read_nonblock reads a zero-length datagram as EOF
                if (!exception) return context.nil;
                throw context.runtime.newEOFError();
            } else {
                flipBuffer(buffer);
                bytes = new ByteList(buffer.array(), buffer.position(), buffer.limit());
            }
        } catch (RaiseException re) {
            throw re;
        } catch (Exception ex) {
            // exception: false only silences "would block" (null above)
            throwErrorFromException(context.runtime, ex);
            return context.nil; // not reached
        } finally {
            if (Platform.IS_LINUX) {
                // unset nonblocking to emulate recv(... MSG_DONTWAIT) leaving it blocking
                getOpenFile().setBlocking(context.runtime, true);
            }
        }

        return handleReturnBytes(context, bytes, str, "read_nonblock", exception);
    }

    /**
     * Remember the error from a non-blocking connect on this socket that failed.
     *
     * POSIX leaves that error on the socket, for getsockopt(SOL_SOCKET, SO_ERROR) to
     * report, and leaves the descriptor valid until the application closes it.
     * {@link java.nio.channels.SocketChannel#finishConnect()} instead throws and closes
     * the channel, so {@link org.jruby.util.io.SelectExecutor} hands us the error here
     * and we reproduce the POSIX behaviour for both getsockopt and close.
     *
     * See jruby/jruby#8786.
     */
    public void setConnectError(IOException connectError) {
        this.connectError = connectError;
    }

    /**
     * The errno of a non-blocking connect that failed on this socket, the way POSIX
     * getsockopt(SOL_SOCKET, SO_ERROR) would report it, or zero if there is none.
     *
     * See jruby/jruby#8786.
     */
    private int connectErrno() {
        IOException connectError = this.connectError;
        if (connectError == null) return 0;

        Errno errno = Helpers.errnoFromException(connectError);
        return errno == null ? 0 : errno.intValue();
    }

    @Override
    protected IRubyObject rbIoClose(ThreadContext context) {
        if (connectError != null) {
            OpenFile fptr = getOpenFile();

            if (fptr != null && fptr.fd() != null && !fptr.fd().ch.isOpen()) {
                // The connect failed and the JDK closed the channel with it, so the normal
                // close path would report EBADF for a descriptor POSIX still considers
                // open. Release it without raising, exactly as RubySocket.tryConnect does
                // for a connect that failed synchronously. See jruby/jruby#8786.
                fptr.cleanup(context.runtime, true);
                return context.nil;
            }
        }

        return super.rbIoClose(context);
    }

    // WHY: options go to the kernel's getsockopt(2)/setsockopt(2) on the real
    // fd when native socket access is available, as MRI does: the JDK path
    // only knows a fixed set of SOL_SOCKET options (and a few TCP ones), so
    // e.g. IP_TTL, IPV6_V6ONLY, UDP_CORK, numeric options and every option on
    // an FFM unix socket raised ENOPROTOOPT/SocketError. The JDK path stays
    // for options JRuby emulates on top of java.nio (jdkEmulatedOption) and
    // when FFM is unavailable.
    @JRubyMethod
    public IRubyObject getsockopt(ThreadContext context, IRubyObject _level, IRubyObject _opt) {
        int level = SocketUtils.levelToInt(context, _level);
        int optname = SocketUtils.optnameToInt(context, level, _opt);
        int fd = nativeSockoptFd();

        if (fd >= 0 && !jdkEmulatedOption(level, optname)) {
            try {
                byte[] raw = UnixDomainNative.getSockopt(fd, level, optname);
                return new Option(context.runtime, optionFamily(), level, optname, new ByteList(raw, false));
            } catch (UnixDomainNative.NativeException ne) {
                throw context.runtime.newErrnoFromInt(ne.errno, "getsockopt(2)");
            }
        }

        return jdkGetsockopt(context, _level, _opt);
    }

    private IRubyObject jdkGetsockopt(ThreadContext context, IRubyObject _level, IRubyObject _opt) {
        SocketLevel level = SocketUtils.levelFromArg(context, _level);
        SocketOption opt = SocketUtils.optionFromArg(context, _opt);

        try {
            Channel channel = getOpenChannel();

            switch(level) {

            case SOL_SOCKET:
                if (opt == SocketOption.__UNKNOWN_CONSTANT__) {
                    throw context.runtime.newErrnoENOPROTOOPTError();
                }

                if (opt == SocketOption.SO_ERROR) {
                    // Java exposes no equivalent socket option, so the only error we can
                    // report is one left behind by a failed non-blocking connect.
                    return new Option(context.runtime, ProtocolFamily.PF_INET, level, opt,
                            Option.packInt(connectErrno()));
                }

                int value = SocketType.forChannel(channel).getSocketOption(channel, opt);
                ByteList packedValue;

                if (opt == SocketOption.SO_LINGER) {
                    if (value == -1) {
                        // Hardcode to 0, because Java Socket API drops actual value
                        // if lingering is disabled
                        packedValue = Option.packLinger(0, 0);
                    } else {
                        packedValue = Option.packLinger(1, value);
                    }
                } else {
                    packedValue = Option.packInt(value);
                }

                return new Option(context.runtime, ProtocolFamily.PF_INET, level, opt, packedValue);

            default:
                int intLevel = toInt(context, _level);
                int intOpt = toInt(context, _opt);
                ChannelFD fd = getOpenFile().fd();
                IPProto proto = IPProto.valueOf(intLevel);
 
                switch (proto) {
                    case IPPROTO_TCP:
                        if (Platform.IS_LINUX && validTcpSockOpt(intOpt) &&
                                fd.realFileno > 0 && SOCKOPT != null) {
                            ByteBuffer buf = ByteBuffer.allocate(256);
                            IntByReference len = new IntByReference(4);
                            
                            int ret = SOCKOPT.getsockopt(fd.realFileno, intLevel, intOpt, buf, len);

                            if (ret != 0) {
                                throw context.runtime.newErrnoFromLastPOSIXErrno();
                            }
                            flipBuffer(buf);
                            ByteList bytes = new ByteList(buf.array(), buf.position(), len.getValue());

                            return new Option(context.runtime, ProtocolFamily.PF_INET, level, opt, bytes);
                        }

                        break;
                }

                throw context.runtime.newErrnoENOPROTOOPTError();
            }
        } catch (Exception e) {
            throwErrorFromException(context.runtime, e);
            return null; // not reached
        }
    }

    // One binding for both MRI forms, setsockopt(option) and
    // setsockopt(level, optname, value): two fixed-arity bindings left a
    // 2-argument call with no implementation (AbstractMethodError) instead of
    // an ArgumentError.
    @JRubyMethod(name = "setsockopt", required = 1, optional = 2, checkArity = false)
    public IRubyObject setsockopt(ThreadContext context, IRubyObject[] args) {
        return switch (args.length) {
            case 1 -> setsockopt(context, args[0]);
            case 3 -> setsockopt(context, args[0], args[1], args[2]);
            default -> throw argumentError(context, "wrong number of arguments (given " + args.length + ", expected 1 or 3)");
        };
    }

    public IRubyObject setsockopt(ThreadContext context, IRubyObject option) {
        if (!(option instanceof Option sockopt)) throw argumentError(context, option + " is not a Socket::Option");

        return setsockopt(context, sockopt.level(context), sockopt.optname(context), sockopt.data(context));
    }

    public IRubyObject setsockopt(ThreadContext context, IRubyObject _level, IRubyObject _opt, IRubyObject val) {
        int intLevel = SocketUtils.levelToInt(context, _level);
        int optname = SocketUtils.optnameToInt(context, intLevel, _opt);
        int fd = nativeSockoptFd();

        if (fd >= 0 && !jdkEmulatedOption(intLevel, optname)) {
            ByteList data = sockoptData(context, val);
            try {
                UnixDomainNative.setSockopt(fd, intLevel, optname, data.unsafeBytes(), data.begin(), data.realSize());
            } catch (UnixDomainNative.NativeException ne) {
                throw context.runtime.newErrnoFromInt(ne.errno, "setsockopt(2)");
            }
            return asFixnum(context, 0);
        }

        return jdkSetsockopt(context, _level, _opt, val);
    }

    /** The real fd for native getsockopt/setsockopt, or -1 to use the JDK path. */
    private int nativeSockoptFd() {
        if (!UnixDomainNative.hasSockopt() || (sockBits() & SOCK_NATIVE_FD) == 0) return -1;
        return getOpenFileChecked().fd().realFileno;
    }

    /**
     * Options JRuby implements on top of java.nio rather than as the plain
     * kernel option: SO_RCVTIMEO/SO_SNDTIMEO (mapped to the socket timeout),
     * SO_ERROR (connect errors java.nio consumes itself) and IP_ADD_MEMBERSHIP
     * (MulticastStateManager's DatagramChannel#join).
     */
    private static boolean jdkEmulatedOption(int level, int optname) {
        if (level == SocketLevel.SOL_SOCKET.intValue()) {
            return optname == SocketOption.SO_RCVTIMEO.intValue() || optname == SocketOption.SO_SNDTIMEO.intValue()
                    || optname == SocketOption.SO_ERROR.intValue();
        }
        return level == IPProto.IPPROTO_IP.intValue() && optname == MulticastStateManager.IP_ADD_MEMBERSHIP;
    }

    /** setsockopt value as MRI packs it: String as-is, true/false as int 1/0, Integer as int. */
    private static ByteList sockoptData(ThreadContext context, IRubyObject val) {
        if (val instanceof RubyString str) return str.getByteList();
        if (val == context.tru || val == context.fals) return Option.packInt(val.isTrue() ? 1 : 0);
        return Option.packInt(toInt(context, val));
    }

    /** The family a Socket::Option from this socket reports. */
    protected ProtocolFamily optionFamily() {
        if ((sockBits() & SOCK_UNIX) != 0) return ProtocolFamily.PF_UNIX;
        InetSocketAddress local = getInetSocketAddress();
        return local != null && local.getAddress() instanceof Inet6Address ? ProtocolFamily.PF_INET6 : ProtocolFamily.PF_INET;
    }

    private IRubyObject jdkSetsockopt(ThreadContext context, IRubyObject _level, IRubyObject _opt, IRubyObject val) {
        Ruby runtime = context.runtime;

        SocketLevel level = SocketUtils.levelFromArg(context, _level);
        SocketOption opt = SocketUtils.optionFromArg(context, _opt);

        try {
            Channel channel = getOpenChannel();
            SocketType socketType = SocketType.forChannel(channel);

            switch(level) {

            case SOL_SOCKET:
                if (opt == SocketOption.SO_LINGER) {
                    if (val instanceof RubyString) {
                        int[] linger = Option.unpackLinger(val.convertToString().getByteList());
                        socketType.setSoLinger(channel, linger[0] != 0, linger[1]);
                    } else {
                        throw runtime.newErrnoEINVALError("setsockopt(2)");
                    }
                } else {
                    socketType.setSocketOption(channel, opt, asNumber(context, val));
                }

                return asFixnum(context, 0);

            default:
                int intLevel = toInt(context, _level);
                int intOpt = toInt(context, _opt);
                ChannelFD fd = getOpenFile().fd();
                IPProto proto = IPProto.valueOf(intLevel);

                switch (proto) {
                    case IPPROTO_TCP:
                        if (TCP_NODELAY.intValue() == intOpt) {
                            socketType.setTcpNoDelay(channel, asBoolean(context, val));
                        } else if (Platform.IS_LINUX && validTcpSockOpt(intOpt) &&
                                fd.realFileno > 0 && SOCKOPT != null) {

                            ByteBuffer buf = ByteBuffer.allocate(4);
                            buf.order(ByteOrder.nativeOrder());
                            flipBuffer(buf.putInt(toInt(context, val)));
                            int ret = SOCKOPT.setsockopt(fd.realFileno, intLevel, intOpt, buf, buf.remaining());

                            if (ret != 0) {
                                throw runtime.newErrnoFromLastPOSIXErrno();
                            }
                        }

                        break;

                    case IPPROTO_IP:
                    case IPPROTO_HOPOPTS: // these both have value 0 on several platforms
                        if (MulticastStateManager.IP_ADD_MEMBERSHIP == intOpt) {
                            joinMulticastGroup(val);
                        }

                        break;

                    default:
                        throw runtime.newErrnoENOPROTOOPTError();
                }

                return RubyFixnum.zero(runtime);
            }
        } catch (Exception e) {
            throwErrorFromException(context.runtime, e);
            return null; // not reached
        }
    }

    // FIXME: copied from jnr-unixsocket
    static final String[] libnames = jnr.ffi.Platform.getNativePlatform().getOS() == jnr.ffi.Platform.OS.SOLARIS
            ? new String[] { "socket", "nsl", jnr.ffi.Platform.getNativePlatform().getStandardCLibraryName() }
            : new String[] { jnr.ffi.Platform.getNativePlatform().getStandardCLibraryName() };

    public interface LibC {
        int F_GETFL = jnr.constants.platform.Fcntl.F_GETFL.intValue();
        int F_SETFL = jnr.constants.platform.Fcntl.F_SETFL.intValue();
        int O_NONBLOCK = jnr.constants.platform.OpenFlags.O_NONBLOCK.intValue();

        int getsockopt(int s, int level, int optname, @Out ByteBuffer optval, IntByReference optlen);
        int setsockopt(int s, int level, int optname, @In ByteBuffer optval, int optlen);
        int setsockopt(int s, int level, int optname, @In Timeval optval, int optlen);
    }

    static final LibC SOCKOPT;

    static {
        LibraryLoader<LibC> loader = LibraryLoader.create(LibC.class);
        for (String libraryName : libnames) {
            loader.library(libraryName);
        }
        SOCKOPT = loader.load();
    }

    @JRubyMethod(name = "getsockname")
    public IRubyObject getsockname(ThreadContext context) {
        return getSocknameCommon(context, "getsockname");
    }

    @JRubyMethod(name = "getpeername")
    public IRubyObject getpeername(ThreadContext context) {
        InetSocketAddress sock = getInetRemoteSocket(context);

        return sock != null ?
                Sockaddr.pack_sockaddr_in(context, sock) :
                Sockaddr.pack_sockaddr_un(context, UnixDomain.pathOf(getUnixRemoteSocket(context)));
    }

    // WHY: IO#ioctl only takes an Integer/boolean argument in JRuby; socket
    // ioctls (SIOCGIFADDR, ...) take a struct in a String the kernel reads
    // and fills in. With a String argument, call ioctl(2) natively on the fd
    // and copy the result back into the String, as MRI does.
    @Override
    @JRubyMethod(name = "ioctl", required = 1, optional = 1, checkArity = false)
    public IRubyObject ioctl(ThreadContext context, IRubyObject[] args) {
        if (args.length != 2 || !(args[1] instanceof RubyString str)) return super.ioctl(context, args);

        int fd = getOpenFileChecked().fd().realFileno;
        if (fd < 0 || !UnixDomainNative.hasIoctl()) return super.ioctl(context, args);

        str.modify();
        ByteList bytes = str.getByteList();
        try {
            return asFixnum(context, UnixDomainNative.ioctl(fd, toLong(context, args[0]),
                    bytes.unsafeBytes(), bytes.begin(), bytes.realSize()));
        } catch (UnixDomainNative.NativeException ne) {
            throw context.runtime.newErrnoFromInt(ne.errno, "ioctl");
        }
    }

    /**
     * [euid, egid] of the peer, from the kernel's SO_PEERCRED (struct ucred:
     * pid, uid, gid) as MRI does on Linux.
     */
    @JRubyMethod(name = "getpeereid")
    public IRubyObject getpeereid(ThreadContext context) {
        int fd = nativeSockoptFd();
        if (fd < 0 || !Platform.IS_LINUX) throw context.runtime.newNotImplementedError("getpeereid needs native socket access on Linux");
        try {
            byte[] cred = UnixDomainNative.getSockopt(fd, SocketLevel.SOL_SOCKET.intValue(), SO_PEERCRED);
            ByteBuffer buf = ByteBuffer.wrap(cred).order(ByteOrder.nativeOrder());
            return newArray(context, asFixnum(context, buf.getInt(4)), asFixnum(context, buf.getInt(8)));
        } catch (UnixDomainNative.NativeException ne) {
            throw context.runtime.newErrnoFromInt(ne.errno, "getsockopt(2)");
        }
    }

    private static final int SO_PEERCRED = 17; // Linux <asm-generic/socket.h>

    @JRubyMethod
    public IRubyObject local_address(ThreadContext context) {
        InetSocketAddress address = getInetSocketAddress();

        if (address != null) {
            SocketType socketType = SocketType.forChannel(getChannel());
            return Addrinfo.socketAddress(context.runtime, Access.getClass(context, "Addrinfo"), address, socketType.getSocketType(), socketType);
        }

        SocketAddress unix = getUnixSocketAddress();
        if (unix == null) {
            InetSocketAddress unbound = unboundInetAddress();
            if (unbound != null) {
                SocketType socketType = SocketType.forChannel(getChannel());
                return Addrinfo.socketAddress(context.runtime, Access.getClass(context, "Addrinfo"), unbound, socketType.getSocketType(), socketType);
            }
        }
        return Addrinfo.unix(context, Access.getClass(context, "Addrinfo"), newString(context, UnixDomain.pathOf(unix)));
    }

    /**
     * The wildcard address (port 0) an inet socket reports before it has a
     * local address of its own, e.g. Socket.new(:INET, :STREAM) before
     * bind/connect; null if this isn't an inet socket. Only RubySocket can be
     * in that state, so only it overrides this.
     */
    protected InetSocketAddress unboundInetAddress() {
        return null;
    }

    @JRubyMethod
    public IRubyObject remote_address(ThreadContext context) {
        InetSocketAddress address = getInetRemoteSocket(context);

        if (address != null) {
            SocketType socketType = SocketType.forChannel(getChannel());
            return Addrinfo.socketAddress(context.runtime, Access.getClass(context, "Addrinfo"), address, socketType.getSocketType(), socketType);
        }

        SocketAddress unix = getUnixRemoteSocket(context);

         if (unix == null) throw context.runtime.newErrnoENOTCONNError();

        return Addrinfo.unix(context, Access.getClass(context, "Addrinfo"), newString(context, UnixDomain.pathOf(unix)));
    }

    @JRubyMethod(optional = 1, checkArity = false)
    public IRubyObject shutdown(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 0, 1);

        int how = 2;

        if (argc > 0) {
            String howString = null;
            if (args[0] instanceof RubyString || args[0] instanceof RubySymbol) {
                howString = args[0].asJavaString();
            } else {
                Ruby runtime = context.runtime;
                IRubyObject maybeString = TypeConverter.checkStringType(runtime, args[0]);
                if (!maybeString.isNil()) howString = maybeString.toString();
            }
            if (howString != null) {
                if (howString.equals("RD") || howString.equals("SHUT_RD")) {
                    how = 0;
                } else if (howString.equals("WR") || howString.equals("SHUT_WR")) {
                    how = 1;
                } else if (howString.equals("RDWR") || howString.equals("SHUT_RDWR")) {
                    how = 2;
                } else {
                    throw SocketUtils.sockerr(context.runtime, "'how' should be either :SHUT_RD, :SHUT_WR, :SHUT_RDWR");
                }
            } else {
                how = toInt(context, args[0]);
            }
        }

        OpenFile fptr = getOpenFileChecked();

        return shutdownInternal(context, fptr, how);
    }

    @Override
    @JRubyMethod
    public IRubyObject close_write(ThreadContext context) {
        return closeHalf(context, OpenFile.WRITABLE);
    }

    @Override
    @JRubyMethod
    public IRubyObject close_read(ThreadContext context) {
        return closeHalf(context, OpenFile.READABLE);
    }

    private IRubyObject closeHalf(ThreadContext context, int closeHalf) {
        OpenFile fptr;

        int otherHalf = closeHalf == OpenFile.READABLE ? OpenFile.WRITABLE : OpenFile.READABLE;

        fptr = getOpenFileChecked();
        if ((fptr.getMode() & otherHalf) == 0) {
            // shutdown fully
            return rbIoClose(context);
        }

        // shutdown half
        int how = closeHalf == OpenFile.READABLE ? 0 : 1;
        shutdownInternal(context, fptr, how, true);

        return context.nil;
    }

    // WHY: sendmsg/recvmsg (and _nonblock) were NotImplementedError. The
    // Ruby-facing methods are MRI's shapes in socket.rb, which normalize the
    // arguments (control messages to [level, type, data], destination to
    // packed sockaddr bytes) and build the result objects (Addrinfo,
    // Socket::AncillaryData, received IOs); these two primitives only do the
    // syscall, via FFM on the real fd. Always issued with MSG_DONTWAIT; a
    // blocking call waits through OpenFile#waitReadable/#waitWritable, so
    // Thread#kill/raise and Timeout still work.
    @JRubyMethod(name = "__sendmsg_native", required = 6, visibility = Visibility.PRIVATE)
    public IRubyObject sendmsgNative(ThreadContext context, IRubyObject[] args) {
        ByteList mesg = args[0].convertToString().getByteList();
        int flags = flagsArg(context, args[1]);
        byte[] dest = args[2].isNil() ? null : args[2].convertToString().getBytes();
        boolean nonblock = args[4].isTrue();
        boolean exception = args[5].isTrue();

        java.util.List<UnixDomainNative.Cmsg> controls = new java.util.ArrayList<>();
        for (IRubyObject entry : ((RubyArray<?>) args[3]).toJavaArray(context)) {
            RubyArray<?> c = (RubyArray<?>) entry;
            controls.add(new UnixDomainNative.Cmsg(toInt(context, c.eltOk(0)), toInt(context, c.eltOk(1)),
                    c.eltOk(2).convertToString().getBytes()));
        }

        OpenFile fptr = getOpenFileChecked();
        int fd = msgFd(context, fptr, "sendmsg");
        while (true) {
            try {
                return asFixnum(context, UnixDomainNative.sendMsg(fd, mesg.unsafeBytes(), mesg.begin(), mesg.realSize(),
                        dest, controls, flags | UnixDomainNative.MSG_DONTWAIT));
            } catch (UnixDomainNative.NativeException ne) {
                if (ne.errno != UnixDomainNative.EAGAIN) throw context.runtime.newErrnoFromInt(ne.errno, "sendmsg(2)");
                if (nonblock) {
                    if (!exception) return Convert.asSymbol(context, "wait_writable");
                    throw context.runtime.newErrnoEAGAINWritableError("sendmsg(2) would block");
                }
                waitFd(context, fd, UnixDomainNative.POLLOUT);
            }
        }
    }

    /**
     * recvmsg(2): [data, sender Addrinfo, msg_flags, [[level, type, data], ...]],
     * nil at a stream socket's EOF, or :wait_readable.
     */
    @JRubyMethod(name = "__recvmsg_native", required = 5, visibility = Visibility.PRIVATE)
    public IRubyObject recvmsgNative(ThreadContext context, IRubyObject[] args) {
        int flags = flagsArg(context, args[1]);
        int controlLen = args[2].isNil() ? 4096 : toInt(context, args[2]);
        boolean nonblock = args[3].isTrue();
        boolean exception = args[4].isTrue();
        boolean dgram = (sockBits() & SOCK_DGRAM) != 0;
        int len = args[0].isNil() ? 4096 : toInt(context, args[0]);
        if (len < 0) throw argumentError(context, "negative length " + len + " given");
        // No maxdatalen: a datagram is read whole, as MRI does -- peek with
        // MSG_TRUNC first to learn its real size.
        boolean probe = args[0].isNil() && dgram;

        OpenFile fptr = getOpenFileChecked();
        int fd = msgFd(context, fptr, "recvmsg");
        UnixDomainNative.RecvMsgResult r;
        while (true) {
            int callFlags = flags | UnixDomainNative.MSG_DONTWAIT
                    | (probe ? UnixDomainNative.MSG_PEEK | UnixDomainNative.MSG_TRUNC : 0);
            try {
                r = UnixDomainNative.recvMsg(fd, len, controlLen, callFlags);
            } catch (UnixDomainNative.NativeException ne) {
                if (ne.errno != UnixDomainNative.EAGAIN) throw context.runtime.newErrnoFromInt(ne.errno, "recvmsg(2)");
                if (nonblock) {
                    if (!exception) return Convert.asSymbol(context, "wait_readable");
                    throw context.runtime.newErrnoEAGAINReadableError("recvmsg(2) would block");
                }
                waitFd(context, fd, UnixDomainNative.POLLIN);
                continue;
            }
            if (probe) {
                probe = false;
                if (r.n() > len) len = (int) r.n();
                continue;
            }
            break;
        }

        if (r.n() == 0 && len > 0 && !dgram) return context.nil; // peer shut down (MRI: nil)

        Sock sock = dgram ? Sock.SOCK_DGRAM : Sock.SOCK_STREAM;
        RubyClass addrinfoClass = Access.getClass(context, "Addrinfo");
        Addrinfo sender;
        if (r.name().length == 0) { // e.g. a stream socket: recvmsg(2) reports no sender
            sender = new Addrinfo(context.runtime, addrinfoClass, null, sock, SocketType.forChannel(getChannel()));
        } else {
            try {
                SocketAddress from = Sockaddr.sockaddrFromBytes(context, r.name());
                SocketType socketType = SocketType.forChannel(getChannel());
                sender = from instanceof InetSocketAddress inet ?
                        Addrinfo.socketAddress(context.runtime, addrinfoClass, inet, sock, socketType) : // protocol 0, as MRI
                        new Addrinfo(context.runtime, addrinfoClass, from, sock, socketType);
            } catch (IOException e) {
                throw context.runtime.newIOErrorFromException(e);
            }
        }

        RubyArray<?> controls = newArray(context);
        for (UnixDomainNative.Cmsg c : r.controls()) {
            controls.append(context, newArray(context, asFixnum(context, c.level()), asFixnum(context, c.type()),
                    newString(context, new ByteList(c.data(), false))));
        }

        return newArray(context, newString(context, new ByteList(r.data(), false)), sender,
                asFixnum(context, r.flags()), controls);
    }

    /**
     * Blocking receive of one datagram that Thread#kill/raise/Timeout can
     * interrupt: a non-blocking DatagramChannel#receive plus waitFd. A plain
     * blocking receive left the thread "run" and unkillable until the socket
     * closed (stock JRuby too).
     */
    final SocketAddress receiveDatagram(ThreadContext context, DatagramChannel dgram, ByteBuffer buffer) throws IOException {
        int fd = getOpenFileChecked().fd().realFileno;
        if (fd < 0 || !UnixDomainNative.hasPoll()) return dgram.receive(buffer);

        while (true) {
            SocketAddress from;
            synchronized (dgram.blockingLock()) {
                boolean wasBlocking = dgram.isBlocking();
                dgram.configureBlocking(false);
                try {
                    from = dgram.receive(buffer);
                } finally {
                    dgram.configureBlocking(wasBlocking);
                }
            }
            if (from != null) return from;
            waitFd(context, fd, UnixDomainNative.POLLIN);
        }
    }

    @JRubyMethod(rest = true, notImplemented = true)
    public IRubyObject sendmsg_nonblock(ThreadContext context, IRubyObject[] args) {
        throw context.runtime.newNotImplementedError("sendmsg_nonblock is not implemented");
    }

    /**
     * Interruptible wait for an FFM socket call that got EAGAIN: poll(2) on
     * the real fd, run as a RubyThread task, so the thread shows as "sleep"
     * and Thread#kill/raise/Timeout wake it (executeTask's pollThreadEvents
     * raises them). OpenFile#waitReadable can't be used here -- for a
     * channel in blocking mode it returns at once, so the caller spun.
     * poll(2) can't be woken from Java, so it waits in slices backing off from
     * 1ms to 100ms, the same governor shape as Kestowv's Wave and
     * UnixFdChannel's SCRATCH: readiness is never delayed (poll returns the
     * moment the fd is ready), an idle wait costs ~10 wakeups/s, and an
     * interrupt is seen within one slice.
     */
    final void waitFd(ThreadContext context, int fd, short events) {
        if (!UnixDomainNative.hasPoll()) {
            if (events == UnixDomainNative.POLLOUT) getOpenFileChecked().waitWritable(context);
            else getOpenFileChecked().waitReadable(context);
            return;
        }
        UnixDomain.waitFd(context, fd, events, -1);
    }

    /** True when recvmsg/sendmsg (and recvfrom built on them) can run natively. */
    final boolean hasNativeMsg() {
        return UnixDomainNative.hasScm() && getOpenFileChecked().fd().realFileno >= 0;
    }

    /**
     * Socket#recvfrom(_nonblock) as MRI: [mesg, sender Addrinfo], nil at a
     * stream EOF, built on recvmsg(2) so it works for every socket type
     * (the old path cast every channel to DatagramChannel).
     */
    final IRubyObject recvfromNative(ThreadContext context, IRubyObject length, IRubyObject flags, IRubyObject outbuf,
                                     boolean nonblock, boolean exception) {
        IRubyObject r = recvmsgNative(context, new IRubyObject[] {
                length, flags == null ? context.nil : flags, asFixnum(context, 0),
                context.runtime.newBoolean(nonblock), context.runtime.newBoolean(exception) });
        if (!(r instanceof RubyArray<?> result)) return r; // nil (EOF) or :wait_readable
        return newArray(context, returnBytes(context, result.eltOk(0).convertToString().getByteList(), outbuf), result.eltOk(1));
    }

    private int msgFd(ThreadContext context, OpenFile fptr, String op) {
        int fd = fptr.fd().realFileno;
        if (fd < 0 || !UnixDomainNative.hasScm()) {
            throw context.runtime.newNotImplementedError(op + " needs native socket access (--enable-native-access)");
        }
        return fd;
    }

    // master's stubs: socket.rb replaces them when the native calls are there (__native_msg?)
    @JRubyMethod(rest = true, notImplemented = true)
    public IRubyObject sendmsg(ThreadContext context, IRubyObject[] args) {
        throw context.runtime.newNotImplementedError("sendmsg is not implemented");
    }

    @JRubyMethod(rest = true, notImplemented = true)
    public IRubyObject recvmsg(ThreadContext context, IRubyObject[] args) {
        throw context.runtime.newNotImplementedError("recvmsg is not implemented");
    }

    @JRubyMethod(rest = true, notImplemented = true)
    public IRubyObject recvmsg_nonblock(ThreadContext context, IRubyObject[] args) {
        throw context.runtime.newNotImplementedError("recvmsg_nonblock is not implemented");
    }

    /** True when sendmsg/recvmsg can run natively; socket.rb defines them, and AncillaryData, only then. */
    @JRubyMethod(name = "__native_msg?", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject native_msg_p(ThreadContext context, IRubyObject self) {
        return context.runtime.newBoolean(UnixDomainNative.hasScm());
    }

    protected ByteList doRead(ThreadContext context, final ByteBuffer buffer) {
        return readChannel(context, buffer) <= 0 ? null : new ByteList(buffer.array(), 0, buffer.position(), false);
    }

    /**
     * doReadNonblock's result at EOF (peer has shut down), as opposed to null
     * for "no data yet". doRead folds both into null, which made recv_nonblock
     * raise EAGAIN at EOF forever instead of returning nil.
     */
    private static final ByteList NONBLOCK_EOF = new ByteList(0);

    /** One read into buffer: bytes read, 0 if a non-blocking channel has nothing yet, -1 at EOF. */
    private int readChannel(ThreadContext context, final ByteBuffer buffer) {
        OpenFile fptr;

        fptr = getOpenFileInitialized();
        fptr.checkReadable(context);

        try {
            context.getThread().beforeBlockingCall(context);

            return openFile.readChannel().read(buffer);
        } catch (Exception e) {
            throwErrorFromException(context.runtime, e);
            return -1; // not reached
        } finally {
            context.getThread().afterBlockingCall();
        }
    }

    protected final ByteList doReadNonblock(ThreadContext context, final ByteBuffer buffer, boolean exception) {
        Channel channel = getChannel();

        // UnixFdChannel (pair()/for_fd/rawSocket) isn't a SelectableChannel,
        // so the generic path below would unconditionally treat it as
        // "doesn't support nonblocking" -- confirmed by testing: it raised
        // EAGAIN even when data was genuinely available, every time, not a
        // real would-block signal. recvFromNonBlocking does a real
        // single-attempt recvfrom(2) with the fd actually toggled into
        // OS-level O_NONBLOCK; works the same for a pair() stream socket
        // (recvfrom(2) on a connected stream socket is just a read,
        // ignoring the sender info) or a raw dgram one.
        if (channel instanceof UnixFdChannel ufc) {
            try {
                UnixDomain.DgramReceived result = UnixDomain.recvFromNonBlocking(context, ufc.fd(), buffer.remaining());
                if (result == null) return null;
                // recvfrom(2) returning 0 is EOF on a stream socket, but a real
                // zero-length datagram on a dgram one.
                if (result.data.isEmpty() && ufc.sockType() != UnixDomainNative.SOCK_DGRAM) return NONBLOCK_EOF;
                return result.data.getByteList();
            } catch (RuntimeException re) {
                throw re; // EAGAIN is result == null above; anything else is a real error
            }
        }

        if ( ! (channel instanceof SelectableChannel) ) {
            throw context.runtime.newErrnoEAGAINReadableError(channel.getClass().getName() + " does not support nonblocking");
        }

        SelectableChannel selectable = (SelectableChannel) channel;

        synchronized (selectable.blockingLock()) {
            boolean oldBlocking = selectable.isBlocking();

            try {
                selectable.configureBlocking(false);

                try {
                    int read = readChannel(context, buffer);
                    if (read < 0) return NONBLOCK_EOF;
                    return read == 0 ? null : new ByteList(buffer.array(), 0, buffer.position(), false);
                }
                finally {
                    selectable.configureBlocking(oldBlocking);
                }
            } catch (Exception e) {
                // exception: false only silences "would block" (read == 0 above);
                // e.g. ENOTCONN on an unconnected socket must still raise (MRI)
                throwErrorFromException(context.runtime, e);
                return null; // not reached
            }
        }
    }

    private void joinMulticastGroup(IRubyObject val) throws IOException, BadDescriptorException {
        Channel socketChannel = getOpenChannel();

        if(socketChannel instanceof DatagramChannel) {
            if (multicastStateManager == null) {
                multicastStateManager = new MulticastStateManager();
            }

            if (val instanceof RubyString) {
                byte [] ipaddr_buf = val.convertToString().getBytes();

                multicastStateManager.addMembership(ipaddr_buf);
            }
        }
    }

    protected InetSocketAddress getInetSocketAddress() {
        SocketAddress socketAddress = getSocketAddress();
        if (socketAddress instanceof InetSocketAddress) return (InetSocketAddress) socketAddress;
        return null;
    }

    @Deprecated(since = "10.0.0.0")
    protected InetSocketAddress getInetRemoteSocket() {
        return getInetRemoteSocket(getCurrentContext());
    }

    protected InetSocketAddress getInetRemoteSocket(ThreadContext context) {
        SocketAddress socketAddress = getRemoteSocket(context);
        return socketAddress instanceof InetSocketAddress sock ? sock : null;
    }

    protected SocketAddress getUnixSocketAddress() {
        SocketAddress socketAddress = getSocketAddress();
        return UnixDomain.isUnixAddress(socketAddress) ? socketAddress : null;
    }

    @Deprecated(since = "10.0.0.0")
    protected SocketAddress getUnixRemoteSocket() {
        return getUnixRemoteSocket(getCurrentContext());
    }

    protected SocketAddress getUnixRemoteSocket(ThreadContext context) {
        SocketAddress socketAddress = getRemoteSocket(context);
        return UnixDomain.isUnixAddress(socketAddress) ? socketAddress : null;
    }

    protected SocketAddress getSocketAddress() {
        Channel channel = getOpenChannel();

        return SocketType.forChannel(channel).getLocalSocketAddress(channel);
    }

    @Deprecated(since = "10.0.0.0")
    protected SocketAddress getRemoteSocket() {
        return getRemoteSocket(getCurrentContext());
    }

    protected SocketAddress getRemoteSocket(ThreadContext context) {
        Channel channel = getOpenChannel();

        SocketAddress address = SocketType.forChannel(channel).getRemoteSocketAddress(channel);

        if (address == null) throw context.runtime.newErrnoENOTCONNError();

        return address;
    }

    protected IRubyObject getSocknameCommon(ThreadContext context, String caller) {
        if (getInetSocketAddress() != null) {
            return Sockaddr.pack_sockaddr_in(context, getInetSocketAddress());
        }

        SocketAddress unix = getUnixSocketAddress();
        if (unix != null) {
            return Sockaddr.pack_sockaddr_un(context, UnixDomain.pathOf(unix));
        }

        InetSocketAddress unbound = unboundInetAddress();
        if (unbound != null) return Sockaddr.pack_sockaddr_in(context, unbound);

        return Sockaddr.pack_sockaddr_in(context, 0, "0.0.0.0");
    }

    private boolean validTcpSockOpt(int intOpt) {
        if (TCP_INFO.intValue() == intOpt ||
            TCP_KEEPIDLE.intValue() == intOpt ||
            TCP_KEEPINTVL.intValue() == intOpt ||
            TCP_KEEPCNT.intValue() == intOpt) {
            return true;
        } else {
            return false;
        }
    }

    // WHY: MRI's BasicSocket#shutdown is plain shutdown(2): the IO stays
    // readable/writable, a read after SHUT_RD sees EOF and a write after
    // SHUT_WR raises EPIPE. JRuby cleared the IO's READABLE/WRITABLE mode
    // (so both raised IOError "not opened for ...") and used java.nio's
    // shutdownOutput (after which writes raise ClosedChannelException, not
    // EPIPE). With a real fd, shutdown(2) goes straight to the kernel; only
    // close_read/close_write (adjustMode) also drop the IO mode, as MRI's do.
    private static IRubyObject shutdownInternal(ThreadContext context, OpenFile fptr, int how) {
        return shutdownInternal(context, fptr, how, false);
    }

    private static IRubyObject shutdownInternal(ThreadContext context, OpenFile fptr, int how, boolean adjustMode) {
        if (how < 0 || how > 2) throw argumentError(context, "'how' should be either :SHUT_RD, :SHUT_WR, :SHUT_RDWR");

        int fd = fptr.fd().realFileno;
        if (fd >= 0 && UnixDomainNative.available()) {
            if (UnixDomainNative.shutdown(fd, how) < 0 && !adjustMode) {
                int errno = UnixDomainContext.captureErrno();
                if (errno != 0) throw context.runtime.newErrnoFromInt(errno, "shutdown(2)");
            }
        } else {
            Channel channel = fptr.channel();
            try {
                if (how != 1) SocketType.forChannel(channel).shutdownInput(channel);
                if (how != 0) SocketType.forChannel(channel).shutdownOutput(channel);
            } catch (IOException e) {
                // MRI ignores errors from shutdown()
            }
        }

        if (adjustMode) {
            if (how != 1) fptr.setMode(fptr.getMode() & ~OpenFile.READABLE);
            if (how != 0) fptr.setMode(fptr.getMode() & ~OpenFile.WRITABLE);
        }

        return RubyFixnum.zero(context.runtime);
    }

    // The socket's own flag, initialized from BasicSocket.do_not_reverse_lookup
    // at creation: a per-socket setting overrides the global one (MRI).
    public boolean doNotReverseLookup(ThreadContext context) {
        return doNotReverseLookup;
    }

    protected static ChannelFD newChannelFD(Ruby runtime, Channel channel) {
        POSIX posix = runtime.getPosix();
        ChannelFD fd = new ChannelFD(channel, posix, runtime.getFilenoUtil());

        if (posix.isNative() && fd.realFileno >= 0 && !Platform.IS_WINDOWS) {
            posix.fcntlInt(fd.realFileno, Fcntl.F_SETFD, FcntlLibrary.FD_CLOEXEC);
        }

        return fd;
    }

    // WHY: MRI (Ruby 3.0+) creates every socket with O_NONBLOCK, so
    // IO#nonblock? is true until the program says otherwise. JRuby keeps a
    // java.nio socket blocking, with a select before each read -- that select
    // is what lets Thread#kill/raise interrupt a read without closing the
    // channel -- so MRI's default is reported here, not applied, until
    // IO#nonblock= (setBlocking) is called explicitly. getBlocking/setBlocking
    // are only used by io/nonblock.
    private volatile boolean reportNonblockDefault = true;

    @Override
    public boolean getBlocking() {
        return !reportNonblockDefault && super.getBlocking();
    }

    @Override
    public void setBlocking(boolean blocking) {
        reportNonblockDefault = false;
        super.setBlocking(blocking);
    }

    protected void initSocket(ChannelFD fd) {
        // continue with normal initialization
        MakeOpenFile();

        openFile.setFD(fd);
        openFile.setMode(OpenFile.READWRITE | OpenFile.SYNC);

        // see rsock_init_sock in MRI; sockets are initialized to binary
        setAscii8bitBinmode();
    }

    /**
     * Wait through the fiber scheduler for a connection, after a nonblocking accept found none pending.
     * Call it without holding any lock on this server, since io_wait runs sibling fibers that may accept on
     * or close it. Returns when the caller should try accepting again.
     */
    // MRI: rsock_s_accept on EAGAIN, which waits with rb_io_maybe_wait_readable and RUBY_IO_TIMEOUT_DEFAULT
    protected void schedulerWaitAcceptable(ThreadContext context, IRubyObject scheduler) {
        IRubyObject result = FiberScheduler.ioWaitReadable(context, scheduler, this);

        if (result == context.fals) {
            throw RubyIOTimeoutError.newIOTimeoutError(context.runtime, "Timed out waiting for IO to become readable!").toThrowable();
        }

        // no events ready, so MRI gives up with the EAGAIN it got from accept(2)
        if (result.isNil() || toInt(context, result) == 0) throw context.runtime.newErrnoFromErrno(Errno.EAGAIN, "accept(2)");
    }

    private Channel getOpenChannel() {
        return getOpenFileChecked().channel();
    }

    static RuntimeException sockerr(final Ruby runtime, final String msg, final Exception cause) {
        RuntimeException ex = SocketUtils.sockerr(runtime, msg);
        if ( cause != null ) ex.initCause(cause);
        return ex;
    }

    static boolean extractExceptionArg(ThreadContext context, IRubyObject opts) {
        return extractExceptionOnlyArg(context, opts, true);
    }

    private static int asNumber(ThreadContext context, IRubyObject val) {
        if ( val instanceof RubyNumeric) {
            return toInt(context, val);
        }
        if ( val instanceof RubyBoolean) {
            return val.isTrue() ? 1 : 0;
        }
        return stringAsNumber(context, val);
    }

    private static int stringAsNumber(ThreadContext context, IRubyObject val) {
        IRubyObject res = Pack.unpack(context, val.convertToString(), FORMAT_SMALL_I).entry(0);

        if (res == context.nil) throw context.runtime.newErrnoEINVALError();

        return toInt(context, res);
    }

    protected boolean asBoolean(ThreadContext context, IRubyObject val) {
        if (val instanceof RubyString) return stringAsNumber(context, val) != 0;
        if (val instanceof RubyNumeric) return toInt(context, val) != 0;
        return val.isTrue();
    }

    protected IRubyObject addrFor(ThreadContext context, InetSocketAddress addr, boolean reverse) {
        boolean ipV6 = addr.getAddress() instanceof Inet6Address;
        IRubyObject ret0 = newString(context, ipV6 ? "AF_INET6" : "AF_INET");
        IRubyObject ret1 = asFixnum(context, addr.getPort());
        IRubyObject ret2;
        String hostAddress = SocketUtils.ipString(addr.getAddress());
        // callers resolve the reverse_lookup argument / do_not_reverse_lookup default
        if (!reverse) {
            ret2 = newString(context, normalizeIPv6Address(ipV6, hostAddress));
        } else {
            ret2 = newString(context, addr.getHostName());
        }
        IRubyObject ret3 = newString(context, normalizeIPv6Address(ipV6, hostAddress));

        return newArray(context, ret0, ret1, ret2, ret3);
    }

    private static String normalizeIPv6Address(boolean ipV6, String hostAddress) {
        return ipV6 ? SocketUtilsIPV6.getIPV6Address(hostAddress) : hostAddress;
    }

    @Deprecated(since = "10.0.0.0")
    protected static String bindContextMessage(IRubyObject host, int port) {
        return bindContextMessage(((RubyBasicObject) host).getCurrentContext(), host, port);
    }

    protected static String bindContextMessage(ThreadContext context, IRubyObject host, int port) {
        return "bind(2) for " + host.inspect(context) + " port " + port;
    }

    @Deprecated(since = "9.4.6.0")
    @Override
    public IRubyObject read_nonblock(ThreadContext context, IRubyObject[] args) {
        return switch (args.length) {
            case 3 -> read_nonblock(context, args[0], args[1], args[2]);
            case 2 -> read_nonblock(context, args[0], args[1]);
            case 1 -> read_nonblock(context, args[0]);
            default -> throw argumentError(context, args.length, 1, 3);
        };
    }

    private static final ByteList FORMAT_SMALL_I = new ByteList(new byte[] { 'i' }, false);
    protected MulticastStateManager multicastStateManager = null;

    // By default we always reverse lookup unless do_not_reverse_lookup set.
    private boolean doNotReverseLookup = false;

    // The error from a non-blocking connect on this socket that failed, if any.
    // See setConnectError and jruby/jruby#8786.
    private volatile IOException connectError;

    protected static class ReceiveTuple {
        ReceiveTuple() {}
        ReceiveTuple(RubyString result, InetSocketAddress sender) {
            this.result = result;
            this.sender = sender;
        }

        RubyString result;
        InetSocketAddress sender;
    }

    // MRI: rsock_s_socketpair, which yields the pair and then closes the second socket and the first
    protected static IRubyObject yieldPair(ThreadContext context, RubyBasicSocket sock0, RubyBasicSocket sock1, Block block) {
        RubyArray<?> pair = newArray(context, sock0, sock1);
        if (!block.isGiven()) return pair;

        try {
            return block.yield(context, pair);
        } finally {
            try {
                closeRescued(context, sock1);
            } finally {
                closeRescued(context, sock0);
            }
        }
    }

    // MRI: ext/socket's io_close, which rescues any StandardError, unlike RubyIO.ioClose
    private static void closeRescued(ThreadContext context, IRubyObject io) {
        IRubyObject errorInfo = context.getErrorInfo();
        try {
            io.callMethod(context, "close");
        } catch (RaiseException re) {
            if (!context.runtime.getStandardError().isInstance(re.getException())) throw re;
            context.setErrorInfo(errorInfo);
        }
    }
}// RubyBasicSocket
