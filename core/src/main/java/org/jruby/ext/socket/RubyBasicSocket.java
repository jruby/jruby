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

import static jnr.constants.platform.IPProto.IPPROTO_TCP;
import static jnr.constants.platform.IPProto.IPPROTO_IP;
import static jnr.constants.platform.Sock.SOCK_DGRAM;
import static jnr.constants.platform.Sock.SOCK_STREAM;
import static jnr.constants.platform.TCP.TCP_NODELAY;

import java.io.EOFException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Sock;
import org.jruby.CompatVersion;
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
import org.jruby.runtime.Arity;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Pack;
import org.jruby.util.io.BadDescriptorException;
import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.ChannelStream;
import org.jruby.util.io.EncodingUtils;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.OpenFile;

import jnr.constants.platform.SocketLevel;
import jnr.constants.platform.SocketOption;
import org.jruby.util.io.Sockaddr;


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
        doNotReverseLookup = runtime.is1_9();
    }

    @JRubyMethod(name = "do_not_reverse_lookup", compat = CompatVersion.RUBY1_9)
    public IRubyObject do_not_reverse_lookup19(ThreadContext context) {
        return context.runtime.newBoolean(doNotReverseLookup);
    }

    @JRubyMethod(name = "do_not_reverse_lookup=", compat = CompatVersion.RUBY1_9)
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

    @Deprecated
    public IRubyObject recv(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 1:
                return recv(context, args[0]);
            case 2:
                return recv(context, args[0], args[1]);
            default:
                Arity.raiseArgumentError(context.runtime, args, 1, 2);
                return null; // not reached
        }
    }

    @JRubyMethod
    public IRubyObject recv(ThreadContext context, IRubyObject _length) {
        Ruby runtime = context.runtime;

        ByteList bytes = doReceive(context, RubyNumeric.fix2int(_length));

        if (bytes == null) return context.nil;

        return RubyString.newString(runtime, bytes);
    }
    
    @JRubyMethod
    public IRubyObject recv(ThreadContext context, IRubyObject _length, IRubyObject _flags) {
        // TODO: implement flags
        return recv(context, _length);
    }

    @JRubyMethod
    public IRubyObject recv_nonblock(ThreadContext context, IRubyObject _length) {
        Ruby runtime = context.runtime;

        ByteList bytes = doReceiveNonblock(context, RubyNumeric.fix2int(_length));

        if (bytes == null) {
            if (runtime.is1_9()) {
                throw runtime.newErrnoEAGAINReadableError("recvfrom(2)");
            } else {
                throw runtime.newErrnoEAGAINError("recvfrom(2)");
            }
        }

        return RubyString.newString(runtime, bytes);
    }

    @JRubyMethod
    public IRubyObject recv_nonblock(ThreadContext context, IRubyObject _length, IRubyObject _flags) {
        // TODO: implement flags
        return recv_nonblock(context, _length);
    }

    @JRubyMethod
    public IRubyObject getsockopt(ThreadContext context, IRubyObject _level, IRubyObject _opt) {
        Ruby runtime = context.runtime;

        SocketLevel level = levelFromArg(_level);
        SocketOption opt = optionFromArg(_opt);

        int value = 0;

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

                value = SocketType.forChannel(channel).getSocketOption(channel, opt);
                
                if (runtime.is1_9()) {
                    return new Option(runtime, ProtocolFamily.PF_INET, level, opt, value);
                } else {
                    return number(runtime, SocketType.forChannel(channel).getSocketOption(channel, opt));
                }

            default:
                throw runtime.newErrnoENOPROTOOPTError();
            }

        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();

        } catch(IOException e) {
            throw runtime.newErrnoENOPROTOOPTError();
        }
    }

    @JRubyMethod
    public IRubyObject setsockopt(ThreadContext context, IRubyObject _level, IRubyObject _opt, IRubyObject val) {
        Ruby runtime = context.runtime;

        SocketLevel level = levelFromArg(_level);
        SocketOption opt = optionFromArg(_opt);

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

        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();

        } catch(IOException e) {
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

            if(null == sock) {
                throw runtime.newIOError("Not Supported");
            }

            return runtime.newString(sock.toString());

        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        }
    }

    @JRubyMethod(name = "getpeereid", compat = CompatVersion.RUBY1_9, notImplemented = true)
    public IRubyObject getpeereid(ThreadContext context) {
        throw context.runtime.newNotImplementedError("getpeereid not implemented");
    }

    @JRubyMethod(compat = CompatVersion.RUBY1_9)
    public IRubyObject local_address(ThreadContext context) {
        try {
            InetSocketAddress address = getSocketAddress();

            if (address == null) {
                return context.nil;

            } else {
                return new Addrinfo(context.runtime, context.runtime.getClass("Addrinfo"), address.getAddress(), address.getPort(), SocketType.forChannel(getChannel()));
            }
        } catch (BadDescriptorException bde) {
            throw context.runtime.newErrnoEBADFError("address unavailable");
        }
    }

    @JRubyMethod(compat = CompatVersion.RUBY1_9)
    public IRubyObject remote_address(ThreadContext context) {
        try {
            InetSocketAddress address = getRemoteSocket();

            if (address == null) {
                return context.nil;

            } else {
                return new Addrinfo(context.runtime, context.runtime.getClass("Addrinfo"), address.getAddress(), address.getPort(), SocketType.forChannel(getChannel()));
            }
        } catch (BadDescriptorException bde) {
            throw context.runtime.newErrnoEBADFError("address unavailable");
        }
    }

    @JRubyMethod(optional = 1)
    public IRubyObject shutdown(ThreadContext context, IRubyObject[] args) {
        int how = 2;

        if (args.length > 0) {
            how = RubyNumeric.fix2int(args[0]);
        }

        try {
            return shutdownInternal(context, how);

        } catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError();
        }
    }

    @Override
    @JRubyMethod
    public IRubyObject close_write(ThreadContext context) {
        Ruby runtime = context.runtime;

        if (!openFile.isWritable()) {
            return runtime.getNil();
        }

        if (openFile.getPipeStream() == null && openFile.isReadable()) {
            throw runtime.newIOError("closing non-duplex IO for writing");
        }

        if (!openFile.isReadable()) {
            close();

        } else {
            // shutdown write
            try {
                shutdownInternal(context, 1);

            } catch (BadDescriptorException e) {
                throw runtime.newErrnoEBADFError();
            }
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

            } catch (BadDescriptorException e) {
                throw runtime.newErrnoEBADFError();
            }
        }

        return context.nil;
    }

    @JRubyMethod(rest = true, notImplemented = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject sendmsg(ThreadContext context, IRubyObject[] args) {
        throw context.runtime.newNotImplementedError("sendmsg is not implemented");
    }

    @JRubyMethod(rest = true, notImplemented = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject sendmsg_nonblock(ThreadContext context, IRubyObject[] args) {
        throw context.runtime.newNotImplementedError("sendmsg_nonblock is not implemented");
    }

    @JRubyMethod(rest = true, notImplemented = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject readmsg(ThreadContext context, IRubyObject[] args) {
        throw context.runtime.newNotImplementedError("readmsg is not implemented");
    }

    @JRubyMethod(rest = true, notImplemented = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject readmsg_nonblock(ThreadContext context, IRubyObject[] args) {
        throw context.runtime.newNotImplementedError("readmsg_nonblock is not implemented");
    }

    private ByteList doReceive(ThreadContext context, int length) {
        Ruby runtime = context.runtime;
        ByteBuffer buf = ByteBuffer.allocate(length);

        try {
            context.getThread().beforeBlockingCall();

            int read = openFile.getMainStreamSafe().getDescriptor().read(buf);

            if (read == 0) return null;

            return new ByteList(buf.array(), 0, buf.position());

        } catch (BadDescriptorException e) {
            throw runtime.newIOError("bad descriptor");

        } catch (IOException e) {
            // All errors to sysread should be SystemCallErrors, but on a closed stream
            // Ruby returns an IOError.  Java throws same exception for all errors so
            // we resort to this hack...
            if ("Socket not open".equals(e.getMessage())) {
                throw runtime.newIOError(e.getMessage());
            }

            throw runtime.newSystemCallError(e.getMessage());

        } finally {
            context.getThread().afterBlockingCall();
        }
    }

    public ByteList doReceiveNonblock(ThreadContext context, int length) {
        Ruby runtime = context.runtime;
        Channel channel = getChannel();

        if (!(channel instanceof SelectableChannel)) {
            if (runtime.is1_9()) {
                throw runtime.newErrnoEAGAINReadableError(channel.getClass().getName() + " does not support nonblocking");
            } else {
                throw runtime.newErrnoEAGAINError(channel.getClass().getName() + " does not support nonblocking");
            }
        }

        SelectableChannel selectable = (SelectableChannel)channel;

        synchronized (selectable.blockingLock()) {
            boolean oldBlocking = selectable.isBlocking();

            try {
                selectable.configureBlocking(false);

                try {
                    return doReceive(context, length);
                } finally {
                    selectable.configureBlocking(oldBlocking);
                }

            } catch(IOException e) {
                throw runtime.newIOErrorFromException(e);
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

            if(null == sock) {
                return Sockaddr.pack_sockaddr_in(context, 0, "0.0.0.0");

            } else {
               return Sockaddr.pack_sockaddr_in(context, sock);
            }

        } catch (BadDescriptorException e) {
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

            } catch (IOException e) {
                throw runtime.newIOError(e.getMessage());
            }

            if(openFile.getPipeStream() != null) {
                openFile.setMainStream(openFile.getPipeStream());
                openFile.setPipeStream(null);
            }

            openFile.setMode(openFile.getMode() & ~OpenFile.READABLE);

            return RubyFixnum.zero(runtime);

        case 1:
            channel = getOpenChannel();
            try {
                SocketType.forChannel(channel).shutdownOutput(channel);

            } catch (IOException e) {
                throw runtime.newIOError(e.getMessage());
            }

            openFile.setPipeStream(null);
            openFile.setMode(openFile.getMode() & ~OpenFile.WRITABLE);

            return RubyFixnum.zero(runtime);

        case 2:
            shutdownInternal(context, 0);
            shutdownInternal(context, 1);

            return RubyFixnum.zero(runtime);

            default:
            throw runtime.newArgumentError("`how' should be either 0, 1, 2");
        }
    }

    public boolean doNotReverseLookup(ThreadContext context) {
        return context.runtime.isDoNotReverseLookupEnabled() || doNotReverseLookup;
    }

    protected void initSocket(Ruby runtime, ChannelDescriptor descriptor) {
        // continue with normal initialization
        MakeOpenFile();

        try {
            openFile.setMainStream(ChannelStream.fdopen(runtime, descriptor, newModeFlags(runtime, ModeFlags.RDONLY)));
            openFile.setPipeStream(ChannelStream.fdopen(runtime, descriptor, newModeFlags(runtime, ModeFlags.WRONLY)));
            openFile.getPipeStream().setSync(true);

        } catch (org.jruby.util.io.InvalidValueException ex) {
            throw runtime.newErrnoEINVALError();
        }

        openFile.setMode(OpenFile.READWRITE | OpenFile.SYNC);

        // see rsock_init_sock in MRI; sockets are initialized to binary
        setAscii8bitBinmode();
    }
    
    private Channel getOpenChannel() throws BadDescriptorException {
        return getOpenFileChecked().getMainStreamSafe().getDescriptor().getChannel();
    }

    private int asNumber(IRubyObject val) {
        if (val instanceof RubyNumeric) {
            return RubyNumeric.fix2int(val);
        } else if (val instanceof RubyBoolean) {
            return val.isTrue() ? 1 : 0;
        }
        else {
            return stringAsNumber(val);
        }
    }

    private int stringAsNumber(IRubyObject val) {
        ByteList str = val.convertToString().getByteList();
        IRubyObject res = Pack.unpack(getRuntime(), str, FORMAT_SMALL_I).entry(0);

        if (res.isNil()) {
            throw getRuntime().newErrnoEINVALError();
        }

        return RubyNumeric.fix2int(res);
    }

    protected boolean asBoolean(IRubyObject val) {
        if (val instanceof RubyString) {
            return stringAsNumber(val) != 0;
        } else if(val instanceof RubyNumeric) {
            return RubyNumeric.fix2int(val) != 0;
        } else {
            return val.isTrue();
        }
    }

    private static IRubyObject number(Ruby runtime, int s) {
        return RubyString.newString(runtime, Pack.packInt_i(new ByteList(4), s));
    }

    protected static SocketOption optionFromArg(IRubyObject _opt) {
        SocketOption opt;
        if (_opt instanceof RubyString || _opt instanceof RubySymbol) {
            opt = SocketOption.valueOf(_opt.toString());
        } else {
            opt = SocketOption.valueOf(RubyNumeric.fix2int(_opt));
        }
        return opt;
    }

    protected static SocketLevel levelFromArg(IRubyObject _level) {
        SocketLevel level;
        if (_level instanceof RubyString || _level instanceof RubySymbol) {
            level = SocketLevel.valueOf(_level.toString());
        } else {
            level = SocketLevel.valueOf(RubyNumeric.fix2int(_level));
        }
        return level;
    }

    protected IRubyObject addrFor(ThreadContext context, InetSocketAddress addr, boolean reverse) {
        Ruby r = context.runtime;
        IRubyObject[] ret = new IRubyObject[4];
        if (addr.getAddress() instanceof Inet6Address) {
            ret[0] = r.newString("AF_INET6");
        } else {
            ret[0] = r.newString("AF_INET");
        }
        ret[1] = r.newFixnum(addr.getPort());
        String hostAddress = addr.getAddress().getHostAddress();
        if (!reverse || doNotReverseLookup(context)) {
            ret[2] = r.newString(hostAddress);
        } else {
            ret[2] = r.newString(addr.getHostName());
        }
        ret[3] = r.newString(hostAddress);
        return r.newArrayNoCopy(ret);
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

    private static final ByteList FORMAT_SMALL_I = new ByteList(ByteList.plain("i"));
    protected MulticastStateManager multicastStateManager = null;

    // By default we always reverse lookup unless do_not_reverse_lookup set.
    private boolean doNotReverseLookup = false;
}// RubyBasicSocket
