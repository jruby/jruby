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

import static jnr.constants.platform.IPProto.IPPROTO_TCP;
import static jnr.constants.platform.IPProto.IPPROTO_IP;
import static jnr.constants.platform.Sock.SOCK_DGRAM;
import static jnr.constants.platform.Sock.SOCK_STREAM;
import static jnr.constants.platform.TCP.TCP_NODELAY;

import java.io.EOFException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

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
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Pack;
import org.jruby.util.io.BadDescriptorException;
import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.ChannelStream;
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

    @JRubyMethod(compat = CompatVersion.RUBY1_9)
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

    @JRubyMethod(name = "send", rest = true)
    public IRubyObject write_send(ThreadContext context, IRubyObject[] args) {
        return syswrite(context, args[0]);
    }

    @JRubyMethod(rest = true)
    public IRubyObject recv(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        OpenFile openFile = getOpenFileChecked();
        
        try {
            context.getThread().beforeBlockingCall();
            
            return RubyString.newString(runtime, openFile.getMainStreamSafe().read(RubyNumeric.fix2int(args[0])));
            
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
            
        } catch (EOFException e) {
            // recv returns nil on EOF
            return runtime.getNil();
            
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

    @JRubyMethod
    public IRubyObject getsockopt(ThreadContext context, IRubyObject lev, IRubyObject optname) {
        Ruby runtime = context.getRuntime();
        int level = RubyNumeric.fix2int(lev);
        int opt = RubyNumeric.fix2int(optname);

        try {
            switch(SocketLevel.valueOf(level)) {

            case SOL_IP:
            case SOL_SOCKET:
            case SOL_TCP:
            case SOL_UDP:

                switch(SocketOption.valueOf(opt)) {

                case SO_BROADCAST:
                    return getBroadcast(runtime);

                case SO_KEEPALIVE:
                    return getKeepAlive(runtime);

                case SO_LINGER:
                    return getLinger(runtime);

                case SO_OOBINLINE:
                    return getOOBInline(runtime);

                case SO_RCVBUF:
                    return getRcvBuf(runtime);

                case SO_REUSEADDR:
                    return getReuseAddr(runtime);

                case SO_SNDBUF:
                    return getSndBuf(runtime);

                case SO_RCVTIMEO:
                case SO_SNDTIMEO:
                    return getTimeout(runtime);

                case SO_TYPE:
                    return getSoType(runtime);

                    // Can't support the rest with Java
                case SO_RCVLOWAT:
                    return number(runtime, 1);

                case SO_SNDLOWAT:
                    return number(runtime, 2048);

                case SO_DEBUG:
                case SO_ERROR:
                case SO_DONTROUTE:
                case SO_TIMESTAMP:
                    return trueFalse(runtime, false);

                default:
                    throw runtime.newErrnoENOPROTOOPTError();
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
    public IRubyObject setsockopt(ThreadContext context, IRubyObject lev, IRubyObject optname, IRubyObject val) {
        Ruby runtime = context.runtime;
        int level = RubyNumeric.fix2int(lev);
        int opt = RubyNumeric.fix2int(optname);

        try {
            switch(SocketLevel.valueOf(level)) {

            case SOL_IP:
            case SOL_SOCKET:
            case SOL_TCP:
            case SOL_UDP:

                switch(SocketOption.valueOf(opt)) {

                case SO_BROADCAST:

                    setBroadcast(val);
                    break;
                case SO_KEEPALIVE:

                    setKeepAlive(val);
                    break;
                case SO_LINGER:

                    setLinger(val);
                    break;
                case SO_OOBINLINE:
                    setOOBInline(val);
                    break;

                case SO_RCVBUF:
                    setRcvBuf(val);
                    break;

                case SO_REUSEADDR:
                    setReuseAddr(val);
                    break;

                case SO_SNDBUF:
                    setSndBuf(val);
                    break;

                case SO_RCVTIMEO:
                case SO_SNDTIMEO:
                    setTimeout(val);
                    break;

                    // Can't support the rest with Java
                case SO_TYPE:
                case SO_RCVLOWAT:
                case SO_SNDLOWAT:
                case SO_DEBUG:
                case SO_ERROR:
                case SO_DONTROUTE:
                case SO_TIMESTAMP:
                    break;

                default:
                    if (IPPROTO_TCP.intValue() == level && TCP_NODELAY.intValue() == opt) {
                        setTcpNoDelay(val);

                    } else if (IPPROTO_IP.intValue() == level) {
                        if (MulticastStateManager.IP_ADD_MEMBERSHIP == opt) {
                            joinMulticastGroup(val);
                        }

                    } else {
                        throw runtime.newErrnoENOPROTOOPTError();
                    }
                }

                break;

            default:
                if (IPPROTO_TCP.intValue() == level && TCP_NODELAY.intValue() == opt) {
                    setTcpNoDelay(val);

                } else if (IPPROTO_IP.intValue() == level) {
                    if (MulticastStateManager.IP_ADD_MEMBERSHIP == opt) {
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

    @JRubyMethod(name = "__getsockname")
    public IRubyObject getsockname_u(ThreadContext context) {
        return getSocknameCommon(context, "__getsockname");
    }

    @JRubyMethod(name = {"getpeername", "__getpeername"})
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
    public IRubyObject close_write(ThreadContext context) {
        Ruby runtime = context.getRuntime();

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
    public IRubyObject close_read(ThreadContext context) {
        Ruby runtime = context.getRuntime();

        if (!openFile.isOpen()) {
            throw context.getRuntime().newIOError("not opened for reading");
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

    protected InetSocketAddress getSocketAddress() throws BadDescriptorException {
        Channel channel = getOpenChannel();

        return (InetSocketAddress)SocketType.forChannel(channel).getLocalSocketAddress(channel);
    }

    protected InetSocketAddress getRemoteSocket() throws BadDescriptorException {
        Channel channel = getOpenChannel();

        return (InetSocketAddress)SocketType.forChannel(channel).getRemoteSocketAddress(channel);
    }

    private Socket asSocket() throws BadDescriptorException {
        Channel socketChannel = getOpenChannel();

        if(!(socketChannel instanceof SocketChannel)) {
            throw getRuntime().newErrnoENOPROTOOPTError();
        }

        return ((SocketChannel)socketChannel).socket();
    }

    private IRubyObject getBroadcast(Ruby runtime) throws IOException, BadDescriptorException {
        Channel channel = getOpenChannel();

        return trueFalse(runtime, SocketType.forChannel(channel).getBroadcast(channel));
    }

    private void setBroadcast(IRubyObject val) throws IOException, BadDescriptorException {
        Channel channel = getOpenChannel();

        SocketType.forChannel(channel).setBroadcast(channel, asBoolean(val));
    }

    private void setKeepAlive(IRubyObject val) throws IOException, BadDescriptorException {
        Channel channel = getOpenChannel();

        SocketType.forChannel(channel).setKeepAlive(channel, asBoolean(val));
    }

    private void setTcpNoDelay(IRubyObject val) throws IOException, BadDescriptorException {
        Channel channel = getOpenChannel();

        SocketType.forChannel(channel).setTcpNoDelay(channel, asBoolean(val));
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

    private void setReuseAddr(IRubyObject val) throws IOException, BadDescriptorException {
        Channel channel = getOpenChannel();

        SocketType.forChannel(channel).setReuseAddress(channel, asBoolean(val));
    }

    private void setRcvBuf(IRubyObject val) throws IOException, BadDescriptorException {
        Channel channel = getOpenChannel();

        SocketType.forChannel(channel).setReceiveBufferSize(channel, asNumber(val));
    }

    private void setTimeout(IRubyObject val) throws IOException, BadDescriptorException {
        Channel channel = getOpenChannel();

        SocketType.forChannel(channel).setSoTimeout(channel, asNumber(val));
    }

    private void setSndBuf(IRubyObject val) throws IOException, BadDescriptorException {
        try {
            Channel channel = getOpenChannel();

            SocketType.forChannel(channel).setSendBufferSize(channel, asNumber(val));

        } catch (IllegalArgumentException iae) {
            throw getRuntime().newErrnoEINVALError(iae.getMessage());
        }
    }

    private void setLinger(IRubyObject val) throws IOException, BadDescriptorException {
        Channel channel = getOpenChannel();

        boolean linger;
        int timeout;

        // wacky much, MRI?
        if(val instanceof RubyBoolean && !val.isTrue()) {
            linger = false;
            timeout = 0;
        } else {
            linger = true;
            timeout = asNumber(val);
            if(timeout == -1) {
                linger = false;
                timeout = 0;
            }
        }

        SocketType.forChannel(channel).setSoLinger(channel, linger, timeout);
    }

    private void setOOBInline(IRubyObject val) throws IOException, BadDescriptorException {
        Channel channel = getOpenChannel();

        SocketType.forChannel(channel).setOOBInline(channel, asBoolean(val));
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

    private IRubyObject getKeepAlive(Ruby runtime) throws IOException, BadDescriptorException {
        Channel channel = getOpenChannel();

        return trueFalse(runtime, SocketType.forChannel(channel).getKeepAlive(channel));
    }

    private IRubyObject getLinger(Ruby runtime) throws IOException, BadDescriptorException {
        Channel channel = getOpenChannel();

        int linger = SocketType.forChannel(channel).getSoLinger(channel);

        if (linger < 0) {
            linger = 0;
        }

        return number(runtime, linger);
    }

    private IRubyObject getOOBInline(Ruby runtime) throws IOException, BadDescriptorException {
        Channel channel = getOpenChannel();

        return trueFalse(runtime, SocketType.forChannel(channel).getOOBInline(channel));
    }

    private IRubyObject getRcvBuf(Ruby runtime) throws IOException, BadDescriptorException {
        Channel channel = getOpenChannel();

        return number(runtime, SocketType.forChannel(channel).getReceiveBufferSize(channel));
    }

    private IRubyObject getSndBuf(Ruby runtime) throws IOException, BadDescriptorException {
        Channel channel = getOpenChannel();

        return number(runtime, SocketType.forChannel(channel).getSendBufferSize(channel));
    }

    private IRubyObject getReuseAddr(Ruby runtime) throws IOException, BadDescriptorException {
        Channel channel = getOpenChannel();

        return trueFalse(runtime, SocketType.forChannel(channel).getReuseAddress(channel));
    }

    private IRubyObject getTimeout(Ruby runtime) throws IOException, BadDescriptorException {
        Channel channel = getOpenChannel();

        return number(runtime, SocketType.forChannel(channel).getSoTimeout(channel));
    }

    protected Sock getDefaultSocketType() {
        return Sock.SOCK_STREAM;
    }

    private IRubyObject getSoType(Ruby runtime) throws IOException, BadDescriptorException {
        Channel channel = getOpenChannel();

        return number(runtime,  SocketType.forChannel(channel).getSocketType().intValue());
    }

    private IRubyObject trueFalse(Ruby runtime, boolean val) {
        return number(runtime, val ? 1 : 0);
    }

    private static IRubyObject number(Ruby runtime, int s) {
        RubyArray array = runtime.newArray(runtime.newFixnum(s));

        return Pack.pack(runtime, array, FORMAT_SMALL_I);
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
        Channel socketChannel;

        switch (how) {
        case 0:
            socketChannel = getOpenChannel();

            try {
                if (socketChannel instanceof SocketChannel) {
                    asSocket().shutdownInput();

                } else if (socketChannel instanceof Shutdownable) {
                    ((Shutdownable)socketChannel).shutdownInput();
                }

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
            socketChannel = getOpenChannel();
            try {
                if (socketChannel instanceof SocketChannel) {
                    asSocket().shutdownOutput();

                } else if (socketChannel instanceof Shutdownable) {
                    ((Shutdownable)socketChannel).shutdownOutput();
                }

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

    protected boolean doNotReverseLookup(ThreadContext context) {
        return context.runtime.isDoNotReverseLookupEnabled() || doNotReverseLookup;
    }

    protected void initSocket(Ruby runtime, ChannelDescriptor descriptor) {
        // continue with normal initialization
        openFile = new OpenFile();

        try {
            openFile.setMainStream(ChannelStream.fdopen(runtime, descriptor, newModeFlags(runtime, ModeFlags.RDONLY)));
            openFile.setPipeStream(ChannelStream.fdopen(runtime, descriptor, newModeFlags(runtime, ModeFlags.WRONLY)));
            openFile.getPipeStream().setSync(true);

        } catch (org.jruby.util.io.InvalidValueException ex) {
            throw runtime.newErrnoEINVALError();
        }

        openFile.setMode(OpenFile.READWRITE | OpenFile.SYNC);
    }
    
    private Channel getOpenChannel() throws BadDescriptorException {
        return getOpenFileChecked().getMainStreamSafe().getDescriptor().getChannel();
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
