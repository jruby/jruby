/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2007 Damian Steer <pldms@mac.com>
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

import jnr.constants.platform.AddressFamily;
import jnr.netdb.Service;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.io.Sockaddr;

import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.ProtocolFamily;
import java.net.SocketException;
import java.net.StandardProtocolFamily;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.UnsupportedAddressTypeException;

import static org.jruby.runtime.Helpers.extractExceptionOnlyArg;

/**
 * @author <a href="mailto:pldms@mac.com">Damian Steer</a>
 */
@JRubyClass(name="UDPSocket", parent="IPSocket")
public class RubyUDPSocket extends RubyIPSocket {

    public static final double RECV_BUFFER_COPY_SCALE = 1.5;

    static void createUDPSocket(Ruby runtime) {
        RubyClass rb_cUDPSocket = runtime.defineClass("UDPSocket", runtime.getClass("IPSocket"), RubyUDPSocket::new);

        rb_cUDPSocket.includeModule(runtime.getClass("Socket").getConstant("Constants"));

        rb_cUDPSocket.defineAnnotatedMethods(RubyUDPSocket.class);

        runtime.getObject().setConstant("UDPsocket", rb_cUDPSocket);
    }

    public RubyUDPSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @Override
    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context) {
        return initialize(context, StandardProtocolFamily.INET);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject _family) {
        AddressFamily family = SocketUtils.addressFamilyFromArg(_family);

        if (family == AddressFamily.AF_INET) {
            explicitFamily = Inet4Address.class;
            return initialize(context, StandardProtocolFamily.INET);
        } else if (family == AddressFamily.AF_INET6) {
            explicitFamily = Inet6Address.class;
            return initialize(context, StandardProtocolFamily.INET6);
        }

        throw context.runtime.newErrnoEAFNOSUPPORTError("invalid family for UDPSocket: " + _family);
    }

    public IRubyObject initialize(ThreadContext context, ProtocolFamily family) {
        Ruby runtime = context.runtime;

        try {
            this.family = family;
            DatagramChannel channel = DatagramChannel.open(family);
            initSocket(newChannelFD(runtime, channel));
        } catch (ConnectException e) {
            throw runtime.newErrnoECONNREFUSEDError();
        } catch (UnknownHostException e) {
            throw SocketUtils.sockerr(runtime, "initialize: name or service not known");
        } catch (UnsupportedOperationException uoe) {
            if (uoe.getMessage().contains("IPv6 not available")) {
                throw runtime.newErrnoEAFNOSUPPORTError("socket(2) - udp");
            }
            throw sockerr(runtime, "UnsupportedOperationException: " + uoe.getLocalizedMessage(), uoe);
        } catch (IOException e) {
            throw sockerr(runtime, "initialize: name or service not known", e);
        }

        return this;
    }

    @JRubyMethod
    public IRubyObject bind(ThreadContext context, IRubyObject host, IRubyObject _port) {
        final Ruby runtime = context.runtime;

        final InetSocketAddress addr;
        final int port = SocketUtils.portToInt(_port);
        try {
            final Channel channel = getChannel();

            if ( host.isNil() ||
                 ( (host instanceof RubyString) && ((RubyString) host).isEmpty() ) ) {
                // host is nil or the empty string, bind to INADDR_ANY
                addr = new InetSocketAddress(port);
            }
            else if (host instanceof RubyFixnum) {
                // passing in something like INADDR_ANY
                int intAddr;
                if (host instanceof RubyInteger) {
                    intAddr = RubyNumeric.fix2int(host);
                } else if (host instanceof RubyString) {
                    intAddr = ((RubyString)host).to_i().convertToInteger().getIntValue();
                } else {
                    throw runtime.newTypeError(host, runtime.getInteger());
                }
                RubyModule Socket = runtime.getModule("Socket");
                if (intAddr == RubyNumeric.fix2int(Socket.getConstant("INADDR_ANY"))) {
                    addr = new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port);
                }
                else {
                    if (multicastStateManager == null) {
                        throw runtime.newNotImplementedError("bind with host: " + intAddr);
                    }
                    else addr = null;
                }
            }
            else {
                addr = new InetSocketAddress(InetAddress.getByName(host.convertToString().toString()), port);
            }

            if (multicastStateManager == null) {
                ((DatagramChannel) channel).bind(addr);
            } else {
                multicastStateManager.rebindToPort(port);
            }

            return RubyFixnum.zero(runtime);
        }
        catch (UnsupportedAddressTypeException e) {
            // This may not be the appropriate message for all such exceptions
            ProtocolFamily family = this.family == null ? StandardProtocolFamily.INET : this.family;
            throw SocketUtils.sockerr(runtime, "bind: unsupported address " + host.inspect() + " for protocol family " + family);
        }
        catch (UnknownHostException e) {
            throw SocketUtils.sockerr(runtime, "bind: name or service not known");
        }
        catch (BindException e) {
            throw runtime.newErrnoFromBindException(e, bindContextMessage(host, port));
        }
        catch (AlreadyBoundException e) {
            throw runtime.newErrnoEINVALError(bindContextMessage(host, port));
        }
        catch (SocketException e) {
            final String message = e.getMessage();
            if ( message != null ) {
                switch ( message ) {
                    case "Permission denied" :
                        throw runtime.newErrnoEACCESError(bindContextMessage(host, port));
                }
            }
            throw sockerr(runtime, "bind: name or service not known", e);
        }
        catch (IOException e) {
            throw sockerr(runtime, "bind: name or service not known", e);
        }
    }

    @JRubyMethod
    public IRubyObject connect(ThreadContext context, IRubyObject _host, IRubyObject port) {
        Ruby runtime = context.runtime;

        try {
            String host = _host.isNil() ? "localhost" : _host.convertToString().toString();
            InetAddress[] addrs = InetAddress.getAllByName(host);

            for (int i = 0; i < addrs.length; i++) {
                InetAddress a = addrs[i];

                // If an explicit family is specified, don't try all addresses
                if (explicitFamily != null && !explicitFamily.isInstance(a)) continue;

                try {
                    InetSocketAddress addr = new InetSocketAddress(addrs[i], SocketUtils.portToInt(port));

                    ((DatagramChannel) this.getChannel()).connect(addr);

                    return RubyFixnum.zero(runtime);
                } catch (NoRouteToHostException nrthe) {
                    if (i+1 < addrs.length) continue;
                    throw nrthe;
                }
            }
        }
        catch (UnknownHostException e) {
            throw SocketUtils.sockerr(runtime, "connect: name or service not known");
        }
        catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        }
        catch (IllegalArgumentException e) {
            throw SocketUtils.sockerr(runtime, e.getLocalizedMessage());
        }

        // should not get here
        return context.nil;
    }

    private DatagramChannel getDatagramChannel() {
        return (DatagramChannel) getChannel();
    }

    @JRubyMethod(required = 1, optional = 3)
    public IRubyObject recvfrom_nonblock(ThreadContext context, IRubyObject[] args) {
        return recvfrom_nonblock(this, context, args);
    }

    public static IRubyObject recvfrom_nonblock(RubyBasicSocket socket, ThreadContext context, IRubyObject[] args) {
        int argc = args.length;
        boolean exception = true;
        IRubyObject opts = ArgsUtil.getOptionsArg(context.runtime, args);
        if (opts != context.nil) {
            argc--;
            exception = extractExceptionOnlyArg(context, (RubyHash) opts);
        }

        IRubyObject length, flags, str;
        length = flags = str = context.nil;

        switch (argc) {
            case 3: str = args[2];
            case 2: flags = args[1];
            case 1: length = args[0];
        }

        return recvfrom_nonblock(socket, context, length, flags, str, exception);
    }

    private static IRubyObject recvfrom_nonblock(RubyBasicSocket socket, ThreadContext context,
                                                 IRubyObject length, IRubyObject flags, IRubyObject str, boolean exception) {
        final Ruby runtime = context.runtime;

        try {
            ReceiveTuple tuple = doReceiveNonblockTuple(socket, runtime, RubyNumeric.fix2int(length));

            if (tuple == null) {
                if (!exception) return context.runtime.newSymbol("wait_readable");
                throw context.runtime.newErrnoEAGAINReadableError("recvfrom(2)");
            }

            // TODO: make this efficient
            if (str != null && !str.isNil()) {
                str = str.convertToString();
                ((RubyString) str).setValue(tuple.result.getByteList());
            }
            else {
                str = tuple.result;
            }

            IRubyObject addressArray = socket.addrFor(context, tuple.sender, false);

            return runtime.newArray(str, addressArray);
        }
        catch (UnknownHostException e) {
            throw SocketUtils.sockerr(runtime, "recvfrom: name or service not known");
        }
        catch (PortUnreachableException e) {
            throw runtime.newErrnoECONNREFUSEDError();
        }
        catch (IOException e) { // SocketException
            throw runtime.newIOErrorFromException(e);
        }
        catch (RaiseException e) { throw e; }
        catch (Exception e) {
            throw sockerr(runtime, e.getLocalizedMessage(), e);
        }
    }

    @JRubyMethod
    public IRubyObject send(ThreadContext context, IRubyObject _mesg, IRubyObject _flags) {
        // TODO: implement flags
        final Ruby runtime = context.runtime;

        try {
            int written;

            RubyString data = _mesg.convertToString();
            ByteList dataBL = data.getByteList();
            ByteBuffer buf = ByteBuffer.wrap(dataBL.unsafeBytes(), dataBL.begin(), dataBL.realSize());

            written = ((DatagramChannel) this.getChannel()).write(buf);

            return runtime.newFixnum(written);
        }
        catch (NotYetConnectedException e) {
            throw runtime.newErrnoEDESTADDRREQError("send(2)");
        }
        catch (UnknownHostException e) {
            throw SocketUtils.sockerr(runtime, "send: name or service not known");
        }
        catch (IOException e) { // SocketException
            throw runtime.newIOErrorFromException(e);
        }
        catch (RaiseException e) { throw e; }
        catch (Exception e) {
            throw sockerr(runtime, e.getLocalizedMessage(), e);
        }
    }

    @JRubyMethod(required = 2, optional = 2)
    public IRubyObject send(ThreadContext context, IRubyObject[] args) {
        // TODO: implement flags
        Ruby runtime = context.runtime;
        IRubyObject _mesg = args[0];
        IRubyObject _flags = args[1];

        try {
            int written;

            if (args.length == 2) {
                return send(context, _mesg, _flags);
            }

            InetAddress[] addrs;
            int port;
            if (args.length == 3) {
                InetSocketAddress sockAddress;
                IRubyObject sockaddr = args[2];
                if (sockaddr instanceof Addrinfo) {
                    sockAddress = ((Addrinfo) sockaddr).getInetSocketAddress();
                    if (sockAddress == null) {
                        throw SocketUtils.sockerr(runtime, "need AF_INET or AF_INET6 address");
                    }
                } else {
                    sockAddress = Sockaddr.addressFromSockaddr_in(context, sockaddr);
                }
                addrs = new InetAddress[] {sockAddress.getAddress()};
                port = sockAddress.getPort();
            } else { // args.length >= 4
                IRubyObject _host = args[2];
                IRubyObject _port = args[3];

                RubyString nameStr = _host.convertToString();

                if (_port instanceof RubyString) {

                    Service service = Service.getServiceByName(_port.asJavaString(), "udp");

                    if (service != null) {
                        port = service.getPort();
                    } else {
                        port = (int) _port.convertToInteger("to_i").getLongValue();
                    }

                } else {
                    port = (int) _port.convertToInteger().getLongValue();
                }

                addrs = SocketUtils.getRubyInetAddresses(nameStr.toString());
            }

            RubyString data = _mesg.convertToString();
            ByteBuffer buf = ByteBuffer.wrap(data.getBytes());

            byte[] buf2 = data.getBytes();
            DatagramPacket sendDP;

            for (int i = 0; i < addrs.length; i++) {
                InetAddress inetAddress = addrs[i];
                InetSocketAddress addr = new InetSocketAddress(inetAddress, port);

                try {
                    if (this.multicastStateManager == null) {
                        written = ((DatagramChannel) this.getChannel()).send(buf, addr);

                    } else {
                        sendDP = new DatagramPacket(buf2, buf2.length, addr);
                        multicastStateManager.rebindToPort(addr.getPort());
                        MulticastSocket ms = this.multicastStateManager.getMulticastSocket();

                        ms.send(sendDP);
                        written = sendDP.getLength();
                    }

                    return runtime.newFixnum(written);
                } catch (NoRouteToHostException nrthe) {
                    if (i+1 < addrs.length) {
                        continue;
                    }
                    throw nrthe;
                }
            }
        } catch (UnknownHostException e) {
            throw SocketUtils.sockerr(runtime, "send: name or service not known");
        } catch (IOException e) { // SocketException
            final String message = e.getMessage();
            if (message != null) {
                switch(message) {
                case "Message too large": // Alpine Linux
                case "Message too long":
                    throw runtime.newErrnoEMSGSIZEError();
                }
            }
            throw runtime.newIOErrorFromException(e);
        }
        catch (RaiseException e) { throw e; }
        catch (Exception e) {
            throw sockerr(runtime, e.getLocalizedMessage(), e);
        }

        // should not get here
        return context.nil;
    }

    @JRubyMethod(rest = true, meta = true)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        RubyUDPSocket sock = (RubyUDPSocket) recv.callMethod(context, "new", args);

        if (!block.isGiven()) {
            return sock;
        }

        try {
            return block.yield(context, sock);

        } finally {
            if (sock.openFile.isOpen()) {
                sock.close();
            }
        }
    }

    /**
     * Overrides IPSocket#recvfrom
     */
    @Override
    public IRubyObject recvfrom(ThreadContext context, IRubyObject length) {
        return recvfrom(this, context, length);
    }

    public static IRubyObject recvfrom(RubyBasicSocket socket, ThreadContext context, IRubyObject length) {
        final Ruby runtime = context.runtime;

        try {
            ReceiveTuple tuple = doReceiveTuple(socket, runtime, false, RubyNumeric.fix2int(length));

            IRubyObject addressArray = socket.addrFor(context, tuple.sender, false);

            return runtime.newArray(tuple.result, addressArray);
        }
        catch (UnknownHostException e) {
            throw SocketUtils.sockerr(runtime, "recvfrom: name or service not known");
        }
        catch (PortUnreachableException e) {
            throw runtime.newErrnoECONNREFUSEDError();
        }
        catch (IOException e) { // SocketException
            throw runtime.newIOErrorFromException(e);
        }
        catch (RaiseException e) { throw e; }
        catch (Exception e) {
            throw sockerr(runtime, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Overrides IPSocket#recvfrom
     */
    @Override
    public IRubyObject recvfrom(ThreadContext context, IRubyObject _length, IRubyObject _flags) {
        // TODO: handle flags
        return recvfrom(context, _length);
    }

    /**
     * Overrides BasicSocket#recv
     */
    @Override
    public IRubyObject recv(ThreadContext context, IRubyObject length) {
        final Ruby runtime = context.runtime;

        try {
            return doReceive(this, runtime, false, RubyNumeric.fix2int(length), null);
        }
        catch (PortUnreachableException e) {
            throw runtime.newErrnoECONNREFUSEDError();
        }
        catch (IOException e) { // SocketException
            throw runtime.newIOErrorFromException(e);
        }
        catch (RaiseException e) { throw e; }
        catch (Exception e) {
            throw sockerr(runtime, e.getLocalizedMessage(), e);
        }
    }

    private static ReceiveTuple doReceiveTuple(RubyBasicSocket socket, final Ruby runtime, final boolean non_block, int length) throws IOException {
        ReceiveTuple tuple = new ReceiveTuple();

        final IRubyObject result;
        if (socket.multicastStateManager == null) {
            result = doReceive(socket, runtime, non_block, length, tuple);
        } else {
            result = doReceiveMulticast(socket, runtime, non_block, length, tuple);
        }
        return result == null ? null : tuple; // need to return null for non_block (if op would block)
    }

    private static ReceiveTuple doReceiveNonblockTuple(RubyBasicSocket socket, Ruby runtime, int length) throws IOException {
        DatagramChannel channel = (DatagramChannel) socket.getChannel();

        synchronized (channel.blockingLock()) {
            boolean oldBlocking = channel.isBlocking();

            channel.configureBlocking(false);

            try {
                return doReceiveTuple(socket, runtime, true, length);
            }
            finally {
                channel.configureBlocking(oldBlocking);
            }
        }
    }

    private static IRubyObject doReceive(RubyBasicSocket socket, final Ruby runtime, final boolean non_block,
                                         int length) throws IOException {
        return doReceive(socket, runtime, non_block, length, null);
    }

    protected static IRubyObject doReceive(RubyBasicSocket socket, final Ruby runtime, final boolean non_block,
                                           int length, ReceiveTuple tuple) throws IOException {
        DatagramChannel channel = (DatagramChannel) socket.getChannel();

        ByteBuffer buf = ByteBuffer.allocate(length);

        InetSocketAddress sender = (InetSocketAddress) channel.receive(buf);

        if (sender == null) {
            if ( non_block ) { // non-blocking receive
                return null; // :wait_readable or "recvfrom(2) would block"
            }
            else { // see JRUBY-4678
                throw runtime.newErrnoECONNRESETError();
            }
        }

        // return a string from the buffer, copying if the buffer size is > 1.5 * data size
        ByteList bl = new ByteList(buf.array(), 0, buf.position(), buf.limit() > buf.position() * RECV_BUFFER_COPY_SCALE);
        RubyString result = runtime.newString(bl);

        if (tuple != null) {
            tuple.result = result;
            tuple.sender = sender;
        }

        return result;
    }

    private static IRubyObject doReceiveMulticast(RubyBasicSocket socket, final Ruby runtime, final boolean non_block,
                                                  int length, ReceiveTuple tuple) throws IOException {
        DatagramPacket recv = new DatagramPacket(new byte[length], length);

        try {
            socket.multicastStateManager.getMulticastSocket().receive(recv);
        } catch (IllegalBlockingModeException e) {
            if (non_block) return null; // :wait_readable or raise WaitReadable

            throw runtime.newErrnoEAGAINReadableError("multicast UDP does not support nonblocking");
        }

        InetSocketAddress sender = (InetSocketAddress) recv.getSocketAddress();

        if (sender == null) throw runtime.newErrnoECONNRESETError();         // see JRUBY-4678

        RubyString result = runtime.newString(new ByteList(recv.getData(), recv.getOffset(), recv.getLength(), false));

        if (tuple != null) {
            tuple.result = result;
            tuple.sender = sender;
        }

        return result;
    }

    private volatile Class<? extends InetAddress> explicitFamily;
    private volatile ProtocolFamily family;

    @Deprecated
    public IRubyObject bind(IRubyObject host, IRubyObject port) {
        return bind(getRuntime().getCurrentContext(), host, port);
    }

    @Deprecated
    public IRubyObject connect(IRubyObject host, IRubyObject port) {
        return connect(getRuntime().getCurrentContext(), host, port);
    }

    @Deprecated
    public IRubyObject recvfrom(IRubyObject[] args) {
        return recvfrom(getRuntime().getCurrentContext(), args);
    }

    @Deprecated
    public IRubyObject send(IRubyObject[] args) {
        return send(getRuntime().getCurrentContext(), args);
    }

    @Deprecated
    public static IRubyObject open(IRubyObject recv, IRubyObject[] args, Block block) {
        return open(recv.getRuntime().getCurrentContext(), recv, args, block);
    }
}// RubyUDPSocket
