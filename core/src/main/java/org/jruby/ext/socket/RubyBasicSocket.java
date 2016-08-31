/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectableChannel;

import jnr.constants.platform.Fcntl;
import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Sock;
import jnr.constants.platform.SocketLevel;
import jnr.constants.platform.SocketOption;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyIO;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.ext.fcntl.FcntlLibrary;
import org.jruby.platform.Platform;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Pack;
import org.jruby.util.io.BadDescriptorException;
import org.jruby.util.io.ChannelFD;
import org.jruby.util.io.OpenFile;

import org.jruby.util.TypeConverter;
import org.jruby.util.io.Sockaddr;

import static jnr.constants.platform.IPProto.IPPROTO_TCP;
import static jnr.constants.platform.IPProto.IPPROTO_IP;
import static jnr.constants.platform.TCP.TCP_NODELAY;

/**
 * Implementation of the BasicSocket class from Ruby.
 */
@JRubyClass(name="BasicSocket", parent="IO")
public class RubyBasicSocket extends RubyIO {
    static void createBasicSocket(Ruby runtime) {
        RubyClass rb_cBasicSocket = runtime.defineClass("BasicSocket", runtime.getIO(), BASICSOCKET_ALLOCATOR);

        rb_cBasicSocket.defineAnnotatedMethods(RubyBasicSocket.class);
    }

    private static ObjectAllocator BASICSOCKET_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyBasicSocket(runtime, klass);
        }
    };

    public RubyBasicSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
        doNotReverseLookup = true;
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
        return context.runtime.newBoolean(doNotReverseLookup);
    }

    @JRubyMethod(name = "do_not_reverse_lookup=")
    public IRubyObject set_do_not_reverse_lookup19(ThreadContext context, IRubyObject flag) {
        doNotReverseLookup = flag.isTrue();
        return do_not_reverse_lookup19(context);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject do_not_reverse_lookup(ThreadContext context, IRubyObject recv) {
        return context.runtime.newBoolean(context.runtime.isDoNotReverseLookupEnabled());
    }

    @JRubyMethod(name = "do_not_reverse_lookup=", meta = true)
    public static IRubyObject set_do_not_reverse_lookup(ThreadContext context, IRubyObject recv, IRubyObject flag) {
        context.runtime.setDoNotReverseLookupEnabled(flag.isTrue());

        return flag;
    }

    @JRubyMethod(name = "send")
    public IRubyObject send(ThreadContext context, IRubyObject _mesg, IRubyObject _flags) {
        // TODO: implement flags
        return syswrite(context, _mesg);
    }

    @JRubyMethod(name = "send")
    public IRubyObject send(ThreadContext context, IRubyObject _mesg, IRubyObject _flags, IRubyObject _to) {
        // TODO: implement flags and to
        return send(context, _mesg, _flags);
    }

    @JRubyMethod
    public IRubyObject recv(ThreadContext context, IRubyObject length) {
        return recv(context, length, null, null);
    }

    @JRubyMethod(required = 2, optional = 1) // (length) required = 1 handled above
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

    @Deprecated
    public IRubyObject recv(ThreadContext context, IRubyObject length, IRubyObject flags) {
        return recv(context, new IRubyObject[] { length, flags });
    }

    private IRubyObject recv(ThreadContext context, IRubyObject length,
        RubyString str, IRubyObject flags) {
        // TODO: implement flags
        final ByteBuffer buffer = ByteBuffer.allocate(RubyNumeric.fix2int(length));

        ByteList bytes = doReceive(context, buffer);

        if (bytes == null) return context.nil;

        if (str != null) {
            str.setValue(bytes);
            return str;
        }
        return RubyString.newString(context.runtime, bytes);
    }

    @JRubyMethod
    public IRubyObject recv_nonblock(ThreadContext context, IRubyObject length) {
        return recv_nonblock(context, length, context.nil, /* str */ null, true);
    }

    @JRubyMethod(required = 1, optional = 3) // (length) required = 1 handled above
    public IRubyObject recv_nonblock(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        int argc = args.length;
        IRubyObject opts = ArgsUtil.getOptionsArg(runtime, args);
        if (!opts.isNil()) argc--;

        IRubyObject length, flags, str;
        length = flags = context.nil; str = null;

        switch (argc) {
            case 3: str = args[2];
            case 2: flags = args[1];
            case 1: length = args[0];
        }

        boolean exception = ArgsUtil.extractKeywordArg(context, "exception", opts) != runtime.getFalse();

        return recv_nonblock(context, length, flags, str, exception);
    }

    protected final IRubyObject recv_nonblock(ThreadContext context,
        IRubyObject length,  IRubyObject flags, IRubyObject str, final boolean exception) {
        // TODO: implement flags
        final ByteBuffer buffer = ByteBuffer.allocate(RubyNumeric.fix2int(length));

        ByteList bytes = doReceiveNonblock(context, buffer);

        if (bytes == null) {
            if (!exception) return context.runtime.newSymbol("wait_readable");
            throw context.runtime.newErrnoEAGAINReadableError("recvfrom(2)");
        }

        if (str != null && !str.isNil()) {
            str = str.convertToString();
            ((RubyString) str).setValue(bytes);
            return str;
        }
        return RubyString.newString(context.runtime, bytes);
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
            case SOL_IP:
            case SOL_TCP:
            case SOL_UDP:

                if (opt == SocketOption.__UNKNOWN_CONSTANT__) {
                    throw runtime.newErrnoENOPROTOOPTError();
                }

                int value = SocketType.forChannel(channel).getSocketOption(channel, opt);
                ByteList packedValue = Option.packInt(value);
                return new Option(runtime, ProtocolFamily.PF_INET, level, opt, packedValue);

            default:
                throw runtime.newErrnoENOPROTOOPTError();
            }
        }
        catch (IOException e) {
            throw runtime.newErrnoENOPROTOOPTError();
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

            case SOL_IP:
            case SOL_SOCKET:
            case SOL_TCP:
            case SOL_UDP:

                if (opt == SocketOption.SO_LINGER) {
                    if(val instanceof RubyBoolean && !val.isTrue()) {
                        socketType.setSoLinger(channel, false, 0);
                    } else {
                        int num = asNumber(val);
                        if(num == -1) {
                            socketType.setSoLinger(channel, false, 0);
                        } else {
                            socketType.setSoLinger(channel, true, num);
                        }
                    }

                } else {
                    socketType.setSocketOption(channel, opt, asNumber(val));
                }

                break;

            default:
                int intLevel = (int)_level.convertToInteger().getLongValue();
                int intOpt = (int)_opt.convertToInteger().getLongValue();
                if (IPPROTO_TCP.intValue() == intLevel && TCP_NODELAY.intValue() == intOpt) {
                    socketType.setTcpNoDelay(channel, asBoolean(val));

                } else if (IPPROTO_IP.intValue() == intLevel) {
                    if (MulticastStateManager.IP_ADD_MEMBERSHIP == intOpt) {
                        joinMulticastGroup(val);
                    }

                } else {
                    throw runtime.newErrnoENOPROTOOPTError();
                }
            }
        }
        catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        }
        catch(IOException e) {
            throw runtime.newErrnoENOPROTOOPTError();
        }
        return runtime.newFixnum(0);
    }

    @JRubyMethod(name = "getsockname")
    public IRubyObject getsockname(ThreadContext context) {
        return getSocknameCommon(context, "getsockname");
    }

    @JRubyMethod(name = "getpeername")
    public IRubyObject getpeername(ThreadContext context) {
        Ruby runtime = context.runtime;

        try {
            SocketAddress sock = getRemoteSocket();

            if (sock == null) {
                throw runtime.newIOError("Not Supported");
            }

            return runtime.newString(sock.toString());
        }
        catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        }
    }

    @JRubyMethod(name = "getpeereid", notImplemented = true)
    public IRubyObject getpeereid(ThreadContext context) {
        throw context.runtime.newNotImplementedError("getpeereid not implemented");
    }

    @JRubyMethod
    public IRubyObject local_address(ThreadContext context) {
        try {
            InetSocketAddress address = getSocketAddress();

            if (address == null) return context.nil;

            return new Addrinfo(context.runtime, context.runtime.getClass("Addrinfo"), address.getAddress(), address.getPort(), SocketType.forChannel(getChannel()));
        }
        catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError("address unavailable");
        }
    }

    @JRubyMethod
    public IRubyObject remote_address(ThreadContext context) {
        try {
            InetSocketAddress address = getRemoteSocket();

            if (address == null) return context.nil;

            return new Addrinfo(context.runtime, context.runtime.getClass("Addrinfo"), address.getAddress(), address.getPort(), SocketType.forChannel(getChannel()));
        }
        catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError("address unavailable");
        }
    }

    @JRubyMethod(optional = 1)
    public IRubyObject shutdown(ThreadContext context, IRubyObject[] args) {
        int how = 2;

        if (args.length > 0) {
            String howString = null;
            if (args[0] instanceof RubyString) {
                howString = ((RubyString) args[0]).asJavaString();
            } else if (args[0] instanceof RubySymbol) {
                howString = ((RubySymbol) args[0]).asJavaString();
            }

            if (howString != null) {
                if (howString.equals("RD") || howString.equals("SHUT_RD")) {
                    how = 0;
                } else if (howString.equals("WR") || howString.equals("SHUT_WR")) {
                    how = 1;
                } else if (howString.equals("RDWR") || howString.equals("SHUT_RDWR")) {
                    how = 2;
                }
            } else {
                how = RubyNumeric.fix2int(args[0]);
            }
        }

        try {
            return shutdownInternal(context, how);
        }
        catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError();
        }
    }

    @Override
    @JRubyMethod
    public IRubyObject close_write(ThreadContext context) {
        Ruby runtime = context.runtime;
        OpenFile fptr;

//        if (rb_safe_level() >= 4 && !OBJ_TAINTED(sock)) {
//            rb_raise(rb_eSecurityError, "Insecure: can't close socket");
//        }
        fptr = getOpenFileChecked();
        if ((fptr.getMode() & OpenFile.READABLE) == 0) {
            return rbIoClose(runtime);
        }
        // shutdown write
        try {
            shutdownInternal(context, 1);
        }
        catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        }

        return context.nil;
    }

    @Override
    @JRubyMethod
    public IRubyObject close_read(ThreadContext context) {
        Ruby runtime = context.runtime;

        if (!openFile.isOpen()) {
            throw context.runtime.newIOError("not opened for reading");
        }

        if (!openFile.isWritable()) {
            close();

        } else {
            // shutdown read
            try {
                shutdownInternal(context, 0);
            }
            catch (BadDescriptorException e) {
                throw runtime.newErrnoEBADFError();
            }
        }

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
    public IRubyObject readmsg(ThreadContext context, IRubyObject[] args) {
        throw context.runtime.newNotImplementedError("readmsg is not implemented");
    }

    @JRubyMethod(rest = true, notImplemented = true)
    public IRubyObject readmsg_nonblock(ThreadContext context, IRubyObject[] args) {
        throw context.runtime.newNotImplementedError("readmsg_nonblock is not implemented");
    }

    private ByteList doReceive(ThreadContext context, final ByteBuffer buffer) {
        OpenFile fptr;

        fptr = getOpenFile();
        fptr.checkReadable(context);

        try {
            context.getThread().beforeBlockingCall();

            int read = openFile.readChannel().read(buffer);

            if (read == 0) return null;

            return new ByteList(buffer.array(), 0, buffer.position(), false);
        }
        catch (IOException e) {
            // All errors to sysread should be SystemCallErrors, but on a closed stream
            // Ruby returns an IOError.  Java throws same exception for all errors so
            // we resort to this hack...
            if ( "Socket not open".equals(e.getMessage()) ) {
                throw context.runtime.newIOError(e.getMessage());
            }
            throw context.runtime.newSystemCallError(e.getMessage());
        }
        finally {
            context.getThread().afterBlockingCall();
        }
    }

    protected final ByteList doReceiveNonblock(ThreadContext context, final ByteBuffer buffer) {
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
                    return doReceive(context, buffer);
                }
                finally {
                    selectable.configureBlocking(oldBlocking);
                }
            }
            catch (IOException e) {
                throw context.runtime.newIOErrorFromException(e);
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

    protected InetSocketAddress getSocketAddress() throws BadDescriptorException {
        Channel channel = getOpenChannel();

        return (InetSocketAddress)SocketType.forChannel(channel).getLocalSocketAddress(channel);
    }

    protected InetSocketAddress getRemoteSocket() throws BadDescriptorException {
        Channel channel = getOpenChannel();

        return (InetSocketAddress)SocketType.forChannel(channel).getRemoteSocketAddress(channel);
    }

    protected Sock getDefaultSocketType() {
        return Sock.SOCK_STREAM;
    }

    protected IRubyObject getSocknameCommon(ThreadContext context, String caller) {
        try {
            InetSocketAddress sock = getSocketAddress();

            if (sock == null) {
                return Sockaddr.pack_sockaddr_in(context, 0, "0.0.0.0");
            }

            return Sockaddr.pack_sockaddr_in(context, sock);
        }
        catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError();
        }
    }

    private IRubyObject shutdownInternal(ThreadContext context, int how) throws BadDescriptorException {
        Ruby runtime = context.runtime;
        Channel channel;

        switch (how) {
        case 0:
            channel = getOpenChannel();
            try {
                SocketType.forChannel(channel).shutdownInput(channel);
            }
            catch (IOException e) {
                // MRI ignores errors from shutdown()
            }

            openFile.setMode(openFile.getMode() & ~OpenFile.READABLE);

            return RubyFixnum.zero(runtime);

        case 1:
            channel = getOpenChannel();
            try {
                SocketType.forChannel(channel).shutdownOutput(channel);
            }
            catch (IOException e) {
                // MRI ignores errors from shutdown()
            }

            openFile.setMode(openFile.getMode() & ~OpenFile.WRITABLE);

            return RubyFixnum.zero(runtime);

        case 2:
            shutdownInternal(context, 0);
            shutdownInternal(context, 1);

            return RubyFixnum.zero(runtime);

        default:
            throw runtime.newArgumentError("`how' should be either :SHUT_RD, :SHUT_WR, :SHUT_RDWR");
        }
    }

    public boolean doNotReverseLookup(ThreadContext context) {
        return context.runtime.isDoNotReverseLookupEnabled() || doNotReverseLookup;
    }

    protected static ChannelFD newChannelFD(Ruby runtime, Channel channel) {
        ChannelFD fd = new ChannelFD(channel, runtime.getPosix(), runtime.getFilenoUtil());

        if (runtime.getPosix().isNative() && fd.realFileno >= 0 && !Platform.IS_WINDOWS) {
            runtime.getPosix().fcntlInt(fd.realFileno, Fcntl.F_SETFD, FcntlLibrary.FD_CLOEXEC);
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

    private static int asNumber(IRubyObject val) {
        if ( val instanceof RubyNumeric ) {
            return RubyNumeric.fix2int(val);
        }
        if ( val instanceof RubyBoolean ) {
            return val.isTrue() ? 1 : 0;
        }
        return stringAsNumber(val);
    }

    private static int stringAsNumber(IRubyObject val) {
        final Ruby runtime = val.getRuntime();
        ByteList str = val.convertToString().getByteList();
        IRubyObject res = Pack.unpack(runtime, str, FORMAT_SMALL_I).entry(0);

        if ( res.isNil() ) throw runtime.newErrnoEINVALError();

        return RubyNumeric.fix2int(res);
    }

    protected boolean asBoolean(IRubyObject val) {
        if ( val instanceof RubyString ) {
            return stringAsNumber(val) != 0;
        }
        if ( val instanceof RubyNumeric ) {
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
}// RubyBasicSocket
