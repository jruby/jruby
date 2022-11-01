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
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyIO;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.ext.fcntl.FcntlLibrary;
import org.jruby.platform.Platform;
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
import java.nio.channels.SelectableChannel;

import static jnr.constants.platform.TCP.TCP_INFO;
import static jnr.constants.platform.TCP.TCP_KEEPCNT;
import static jnr.constants.platform.TCP.TCP_KEEPIDLE;
import static jnr.constants.platform.TCP.TCP_KEEPINTVL;
import static jnr.constants.platform.TCP.TCP_NODELAY;
import static org.jruby.runtime.Helpers.extractExceptionOnlyArg;
import static org.jruby.runtime.Helpers.throwErrorFromException;

/**
 * Implementation of the BasicSocket class from Ruby.
 */
@JRubyClass(name="BasicSocket", parent="IO")
public class RubyBasicSocket extends RubyIO {
    static void createBasicSocket(Ruby runtime) {
        RubyClass rb_cBasicSocket = runtime.defineClass("BasicSocket", runtime.getIO(), RubyBasicSocket::new);

        rb_cBasicSocket.defineAnnotatedMethods(RubyBasicSocket.class);
        rb_cBasicSocket.undefineMethod("initialize");
    }

    public RubyBasicSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
        doNotReverseLookup = runtime.isDoNotReverseLookupEnabled();
    }

    @JRubyMethod(meta = true)
    public static IRubyObject for_fd(ThreadContext context, IRubyObject _klass, IRubyObject _fileno) {
        Ruby runtime = context.runtime;
        int fileno = (int)_fileno.convertToInteger().getLongValue();
        RubyClass klass = (RubyClass)_klass;

        ChannelFD fd = runtime.getFilenoUtil().getWrapperFromFileno(fileno);

        RubyBasicSocket basicSocket = (RubyBasicSocket)klass.getAllocator().allocate(runtime, klass);
        basicSocket.initSocket(fd);

        return basicSocket;
    }

    @JRubyMethod(name = "do_not_reverse_lookup")
    public IRubyObject do_not_reverse_lookup19(ThreadContext context) {
        return RubyBoolean.newBoolean(context, doNotReverseLookup);
    }

    @JRubyMethod(name = "do_not_reverse_lookup=")
    public IRubyObject set_do_not_reverse_lookup19(ThreadContext context, IRubyObject flag) {
        doNotReverseLookup = flag.isTrue();
        return do_not_reverse_lookup19(context);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject do_not_reverse_lookup(ThreadContext context, IRubyObject recv) {
        return RubyBoolean.newBoolean(context, context.runtime.isDoNotReverseLookupEnabled());
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

        // TODO: implement flags
        try {
            if (channel instanceof DatagramChannel && sockaddr != null) {
                written = ((DatagramChannel) channel).send(mesgBytes, sockaddr);

                return context.runtime.newFixnum(written);
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

    @JRubyMethod(required = 1, optional = 2) // (length) required = 1 handled above
    public IRubyObject recv(ThreadContext context, IRubyObject[] args) {
        IRubyObject length; RubyString str; IRubyObject flags;

        switch (args.length) {
            case 3:
                length = args[0];
                str = (RubyString) args[1];
                flags = args[2].convertToHash();
                break;
            case 2:
                length = args[0];
                flags = TypeConverter.checkHashType(context.runtime, args[1]);
                str = flags.isNil() ? (RubyString) args[1] : null;
                break;
            case 1:
                length = args[0];
                str = null; flags = null;
                break;
            default:
                length = context.nil;
                str = null; flags = null;
        }

        return recv(context, length, str, flags);
    }

    private IRubyObject recv(ThreadContext context, IRubyObject length,
                             RubyString str, IRubyObject flags) {
        // TODO: implement flags
        final ByteBuffer buffer = ByteBuffer.allocate(RubyNumeric.fix2int(length));

        ByteList bytes;

        Channel channel = getChannel();

        Ruby runtime = context.runtime;
        if (channel instanceof DatagramChannel) {
            try {
                DatagramChannel dgram = (DatagramChannel) channel;

                dgram.receive(buffer);

                buffer.flip();
                bytes = new ByteList(buffer.array(), buffer.position(), buffer.limit());
            } catch (Exception e) {
                throwErrorFromException(runtime, e);
                return null; // not reached
            }
        } else {
                bytes = doRead(context, buffer);
        }
        if (bytes == null) return context.nil;

        if (str != null) {
            str.setValue(bytes);
            return str;
        }

        return RubyString.newString(runtime, bytes);
    }

    @JRubyMethod(required = 1, optional = 3)
    public IRubyObject recv_nonblock(ThreadContext context, IRubyObject[] args) {
        int argc = args.length;
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

        // TODO: implement flags

        final ByteBuffer buffer = ByteBuffer.allocate(RubyNumeric.fix2int(length));

        ByteList bytes;

        Channel channel = getChannel();

        if (channel instanceof DatagramChannel) {
            try {
                DatagramChannel dgram = (DatagramChannel) channel;

                getOpenFile().setBlocking(runtime, false);

                dgram.receive(buffer);

                if (buffer.position() == 0) {
                    bytes = null;
                } else {
                    buffer.flip();
                    bytes = new ByteList(buffer.array(), buffer.position(), buffer.limit());
                }
            } catch (Exception e) {
                if (exception) throwErrorFromException(runtime, e);
                return context.nil;
            }
        } else {
            bytes = doReadNonblock(context, buffer, exception);
        }

        return handleReturnBytes(runtime, bytes, str, "recvfrom(2)", exception);
    }

    private IRubyObject handleReturnBytes(Ruby runtime, ByteList bytes, IRubyObject str, String exMessage, boolean exception) {
        if (bytes == null) {
            if (!exception) return runtime.newSymbol("wait_readable");
            throw runtime.newErrnoEAGAINReadableError(exMessage);
        }

        if (str != null && str != runtime.getNil()) {
            str = str.convertToString();
            ((RubyString) str).setValue(bytes);
            return str;
        }

        return RubyString.newString(runtime, bytes);
    }

    @Override
    @JRubyMethod(required = 1, optional = 2)
    public IRubyObject read_nonblock(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        Channel channel = getChannel();

        if (!(channel instanceof DatagramChannel)) {
            return super.read_nonblock(context, args);
        }

        int argc = args.length;
        boolean exception = true;
        IRubyObject str = context.nil;
        IRubyObject length;

        IRubyObject maybeHash = ArgsUtil.getOptionsArg(runtime, args);

        if (!maybeHash.isNil()) {
            exception = ArgsUtil.extractKeywordArg(context, "exception", maybeHash) != context.fals;
            argc--;
        }

        switch (argc) {
            case 3:
            case 2:
                str = args[1];
                // fall through
            case 1:
                length = args[0];
                break;
            default:
                throw runtime.newArgumentError(argc, 1, 3);
        }

        final ByteBuffer buffer = ByteBuffer.allocate(RubyNumeric.fix2int(length));

        ByteList bytes;

        try {
            DatagramChannel dgram = (DatagramChannel) channel;

            getOpenFile().setBlocking(runtime, false);

            dgram.receive(buffer);

            if (buffer.position() == 0) {
                bytes = null;
            } else {
                buffer.flip();
                bytes = new ByteList(buffer.array(), buffer.position(), buffer.limit());
            }
        } catch (Exception ex) {
            if (exception) throwErrorFromException(runtime, ex);
            return context.nil;
        } finally {
            if (Platform.IS_LINUX) {
                // unset nonblocking to emulate recv(... MSG_DONTWAIT) leaving it blocking
                getOpenFile().setBlocking(runtime, true);
            }
        }

        return handleReturnBytes(runtime, bytes, str, "read_nonblock", exception);
    }

    @JRubyMethod
    public IRubyObject getsockopt(ThreadContext context, IRubyObject _level, IRubyObject _opt) {
        Ruby runtime = context.runtime;

        SocketLevel level = SocketUtils.levelFromArg(_level);
        SocketOption opt = SocketUtils.optionFromArg(_opt);

        try {
            Channel channel = getOpenChannel();

            switch(level) {

            case SOL_SOCKET:
                if (opt == SocketOption.__UNKNOWN_CONSTANT__) {
                    throw runtime.newErrnoENOPROTOOPTError();
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

                return new Option(runtime, ProtocolFamily.PF_INET, level, opt, packedValue);

            default:
                int intLevel = _level.convertToInteger().getIntValue();
                int intOpt = _opt.convertToInteger().getIntValue();
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
                                throw runtime.newErrnoEINVALError(SOCKOPT.strerror(ret));
                            }
                            buf.flip();
                            ByteList bytes = new ByteList(buf.array(), buf.position(), len.getValue());

                            return new Option(runtime, ProtocolFamily.PF_INET, level, opt, bytes);
                        }

                        break;
                }

                throw runtime.newErrnoENOPROTOOPTError();
            }
        } catch (Exception e) {
            throwErrorFromException(context.runtime, e);
            return null; // not reached
        }
    }

    @JRubyMethod
    public IRubyObject setsockopt(ThreadContext context, IRubyObject option) {
        if (option instanceof Option) {
            Option rsockopt = (Option) option;
            return setsockopt(context, rsockopt.level(context), rsockopt.optname(context), rsockopt.data(context));
        } else {
            throw context.runtime.newArgumentError(option.toString() + " is not a Socket::Option");
        }
    }

    @JRubyMethod
    public IRubyObject setsockopt(ThreadContext context, IRubyObject _level, IRubyObject _opt, IRubyObject val) {
        Ruby runtime = context.runtime;

        SocketLevel level = SocketUtils.levelFromArg(_level);
        SocketOption opt = SocketUtils.optionFromArg(_opt);

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

                return RubyFixnum.zero(runtime);

            default:
                int intLevel = _level.convertToInteger().getIntValue();
                int intOpt = _opt.convertToInteger().getIntValue();
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
                            buf.putInt(val.convertToInteger().getIntValue()).flip();
                            int ret = SOCKOPT.setsockopt(fd.realFileno, intLevel, intOpt, buf, buf.remaining());

                            if (ret != 0) {
                                throw runtime.newErrnoEINVALError(SOCKOPT.strerror(ret));
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

        int getsockopt(int s, int level, int optname, @Out ByteBuffer optval, @Out IntByReference optlen);
        int setsockopt(int s, int level, int optname, @In ByteBuffer optval, int optlen);
        int setsockopt(int s, int level, int optname, @In Timeval optval, int optlen);
        String strerror(int error);
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
        Ruby runtime = context.runtime;

        InetSocketAddress sock = getInetRemoteSocket();

        if (sock != null) return Sockaddr.pack_sockaddr_in(context, sock);
        UnixSocketAddress unix = getUnixRemoteSocket();
        return Sockaddr.pack_sockaddr_un(context, unix.path());
    }

    @JRubyMethod(name = "getpeereid", notImplemented = true)
    public IRubyObject getpeereid(ThreadContext context) {
        throw context.runtime.newNotImplementedError("getpeereid not implemented");
    }

    @JRubyMethod
    public IRubyObject local_address(ThreadContext context) {
        Ruby runtime = context.runtime;

        InetSocketAddress address = getInetSocketAddress();

        if (address != null) {
            SocketType socketType = SocketType.forChannel(getChannel());
            return new Addrinfo(runtime, runtime.getClass("Addrinfo"), address, socketType.getSocketType(), socketType);
        }

        UnixSocketAddress unix = getUnixSocketAddress();
        return Addrinfo.unix(context, runtime.getClass("Addrinfo"), runtime.newString(unix.path()));
    }

    @JRubyMethod
    public IRubyObject remote_address(ThreadContext context) {
        Ruby runtime = context.runtime;

        InetSocketAddress address = getInetRemoteSocket();

        if (address != null) {
            SocketType socketType = SocketType.forChannel(getChannel());
            return new Addrinfo(runtime, runtime.getClass("Addrinfo"), address, socketType.getSocketType(), socketType);
        }

        UnixSocketAddress unix = getUnixRemoteSocket();

         if (unix != null) {
             return Addrinfo.unix(context, runtime.getClass("Addrinfo"), runtime.newString(unix.path()));
         }

         throw runtime.newErrnoENOTCONNError();
    }

    @JRubyMethod(optional = 1)
    public IRubyObject shutdown(ThreadContext context, IRubyObject[] args) {
        int how = 2;

        if (args.length > 0) {
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
                    throw SocketUtils.sockerr(context.runtime, "`how' should be either :SHUT_RD, :SHUT_WR, :SHUT_RDWR");
                }
            } else {
                how = RubyNumeric.fix2int(args[0]);
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
        shutdownInternal(context, fptr, how);

        return context.nil;
    }

    @JRubyMethod(rest = true, notImplemented = true)
    public IRubyObject sendmsg(ThreadContext context, IRubyObject[] args) {
        throw context.runtime.newNotImplementedError("sendmsg is not implemented");
    }

    @JRubyMethod(rest = true, notImplemented = true)
    public IRubyObject sendmsg_nonblock(ThreadContext context, IRubyObject[] args) {
        throw context.runtime.newNotImplementedError("sendmsg_nonblock is not implemented");
    }

    @JRubyMethod(rest = true, notImplemented = true)
    public IRubyObject recvmsg(ThreadContext context, IRubyObject[] args) {
        throw context.runtime.newNotImplementedError("recvmsg is not implemented");
    }

    @JRubyMethod(rest = true, notImplemented = true)
    public IRubyObject recvmsg_nonblock(ThreadContext context, IRubyObject[] args) {
        throw context.runtime.newNotImplementedError("recvmsg_nonblock is not implemented");
    }

    protected ByteList doRead(ThreadContext context, final ByteBuffer buffer) {
        OpenFile fptr;

        fptr = getOpenFileInitialized();
        fptr.checkReadable(context);

        try {
            context.getThread().beforeBlockingCall(context);

            int read = openFile.readChannel().read(buffer);

            if (read == 0) return null;

            return new ByteList(buffer.array(), 0, buffer.position(), false);
        } catch (Exception e) {
            throwErrorFromException(context.runtime, e);
            return null; // not reached
        } finally {
            context.getThread().afterBlockingCall();
        }
    }

    protected final ByteList doReadNonblock(ThreadContext context, final ByteBuffer buffer, boolean exception) {
        Channel channel = getChannel();

        if ( ! (channel instanceof SelectableChannel) ) {
            throw context.runtime.newErrnoEAGAINReadableError(channel.getClass().getName() + " does not support nonblocking");
        }

        SelectableChannel selectable = (SelectableChannel) channel;

        synchronized (selectable.blockingLock()) {
            boolean oldBlocking = selectable.isBlocking();

            try {
                selectable.configureBlocking(false);

                try {
                    return doRead(context, buffer);
                }
                finally {
                    selectable.configureBlocking(oldBlocking);
                }
            } catch (Exception e) {
                if (exception) throwErrorFromException(context.runtime, e);
                return null;
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

    protected InetSocketAddress getInetRemoteSocket() {
        SocketAddress socketAddress = getRemoteSocket();
        if (socketAddress instanceof InetSocketAddress) return (InetSocketAddress) socketAddress;
        return null;
    }

    protected UnixSocketAddress getUnixSocketAddress() {
        SocketAddress socketAddress = getSocketAddress();
        if (socketAddress instanceof UnixSocketAddress) return (UnixSocketAddress) socketAddress;
        return null;
    }

    protected UnixSocketAddress getUnixRemoteSocket() {
        SocketAddress socketAddress = getRemoteSocket();
        if (socketAddress instanceof UnixSocketAddress) return (UnixSocketAddress) socketAddress;
        return null;
    }

    protected SocketAddress getSocketAddress() {
        Channel channel = getOpenChannel();

        return SocketType.forChannel(channel).getLocalSocketAddress(channel);
    }

    protected SocketAddress getRemoteSocket() {
        Channel channel = getOpenChannel();

        SocketAddress address = SocketType.forChannel(channel).getRemoteSocketAddress(channel);

        if (address == null) throw getRuntime().newErrnoENOTCONNError();

        return address;
    }

    protected IRubyObject getSocknameCommon(ThreadContext context, String caller) {
        if (getInetSocketAddress() != null) {
            return Sockaddr.pack_sockaddr_in(context, getInetSocketAddress());
        }

        UnixSocketAddress unix = getUnixSocketAddress();
        if (unix != null) {
            return Sockaddr.pack_sockaddr_un(context, unix.path());
        }

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

    private static IRubyObject shutdownInternal(ThreadContext context, OpenFile fptr, int how) {
        Ruby runtime = context.runtime;
        Channel channel;

        switch (how) {
        case 0:
            channel = fptr.channel();
            try {
                SocketType.forChannel(channel).shutdownInput(channel);
            }
            catch (IOException e) {
                // MRI ignores errors from shutdown()
            }

            fptr.setMode(fptr.getMode() & ~OpenFile.READABLE);

            return RubyFixnum.zero(runtime);

        case 1:
            channel = fptr.channel();
            try {
                SocketType.forChannel(channel).shutdownOutput(channel);
            }
            catch (IOException e) {
                // MRI ignores errors from shutdown()
            }

            fptr.setMode(fptr.getMode() & ~OpenFile.WRITABLE);

            return RubyFixnum.zero(runtime);

        case 2:
            shutdownInternal(context, fptr, 0);
            shutdownInternal(context, fptr, 1);

            return RubyFixnum.zero(runtime);

        default:
            throw runtime.newArgumentError("`how' should be either :SHUT_RD, :SHUT_WR, :SHUT_RDWR");
        }
    }

    public boolean doNotReverseLookup(ThreadContext context) {
        return context.runtime.isDoNotReverseLookupEnabled() || doNotReverseLookup;
    }

    protected static ChannelFD newChannelFD(Ruby runtime, Channel channel) {
        POSIX posix = runtime.getPosix();
        ChannelFD fd = new ChannelFD(channel, posix, runtime.getFilenoUtil());

        if (posix.isNative() && fd.realFileno >= 0 && !Platform.IS_WINDOWS) {
            posix.fcntlInt(fd.realFileno, Fcntl.F_SETFD, FcntlLibrary.FD_CLOEXEC);
        }

        return fd;
    }

    protected void initSocket(ChannelFD fd) {
        // continue with normal initialization
        MakeOpenFile();

        openFile.setFD(fd);
        openFile.setMode(OpenFile.READWRITE | OpenFile.SYNC);

        // see rsock_init_sock in MRI; sockets are initialized to binary
        setAscii8bitBinmode();
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
            return RubyNumeric.fix2int(val);
        }
        if ( val instanceof RubyBoolean) {
            return val.isTrue() ? 1 : 0;
        }
        return stringAsNumber(context, val);
    }

    private static int stringAsNumber(ThreadContext context, IRubyObject val) {
        IRubyObject res = Pack.unpack(context, val.convertToString(), FORMAT_SMALL_I).entry(0);

        if (res == context.nil) throw context.runtime.newErrnoEINVALError();

        return RubyNumeric.fix2int(res);
    }

    protected boolean asBoolean(ThreadContext context, IRubyObject val) {
        if ( val instanceof RubyString) {
            return stringAsNumber(context, val) != 0;
        }
        if ( val instanceof RubyNumeric) {
            return RubyNumeric.fix2int(val) != 0;
        }
        return val.isTrue();
    }

    protected IRubyObject addrFor(ThreadContext context, InetSocketAddress addr, boolean reverse) {
        final Ruby runtime = context.runtime;
        IRubyObject ret0, ret1, ret2, ret3;
        if (addr.getAddress() instanceof Inet6Address) {
            ret0 = runtime.newString("AF_INET6");
        } else {
            ret0 = runtime.newString("AF_INET");
        }
        ret1 = runtime.newFixnum(addr.getPort());
        String hostAddress = addr.getAddress().getHostAddress();
        if (!reverse || doNotReverseLookup(context)) {
            ret2 = runtime.newString(hostAddress);
        } else {
            ret2 = runtime.newString(addr.getHostName());
        }
        ret3 = runtime.newString(hostAddress);
        return RubyArray.newArray(runtime, ret0, ret1, ret2, ret3);
    }

    protected static String bindContextMessage(IRubyObject host, int port) {
        return "bind(2) for " + host.inspect() + " port " + port;
    }

    @Deprecated
    public IRubyObject recv(IRubyObject[] args) {
        return recv(getRuntime().getCurrentContext(), args);
    }

    @Deprecated
    public IRubyObject getsockopt(IRubyObject lev, IRubyObject optname) {
        return getsockopt(getRuntime().getCurrentContext(), lev, optname);
    }

    @Deprecated
    public IRubyObject setsockopt(IRubyObject lev, IRubyObject optname, IRubyObject val) {
        return setsockopt(getRuntime().getCurrentContext(), lev, optname, val);
    }

    @Deprecated
    public IRubyObject getsockname() {
        return getsockname(getRuntime().getCurrentContext());
    }

    @Deprecated
    public IRubyObject getpeername() {
        return getpeername(getRuntime().getCurrentContext());
    }

    @Deprecated
    public static IRubyObject do_not_reverse_lookup(IRubyObject recv) {
        return do_not_reverse_lookup(recv.getRuntime().getCurrentContext(), recv);
    }

    @Deprecated
    public static IRubyObject set_do_not_reverse_lookup(IRubyObject recv, IRubyObject flag) {
        return set_do_not_reverse_lookup(recv.getRuntime().getCurrentContext(), recv, flag);
    }

    private static final ByteList FORMAT_SMALL_I = new ByteList(new byte[] { 'i' }, false);
    protected MulticastStateManager multicastStateManager = null;

    // By default we always reverse lookup unless do_not_reverse_lookup set.
    private boolean doNotReverseLookup = false;

    protected static class ReceiveTuple {
        ReceiveTuple() {}
        ReceiveTuple(RubyString result, InetSocketAddress sender) {
            this.result = result;
            this.sender = sender;
        }

        RubyString result;
        InetSocketAddress sender;
    }
}// RubyBasicSocket
