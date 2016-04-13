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

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.NotYetConnectedException;

import jnr.netdb.Service;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:pldms@mac.com">Damian Steer</a>
 */
@JRubyClass(name="UDPSocket", parent="IPSocket")
public class RubyUDPSocket extends RubyIPSocket {

    static void createUDPSocket(Ruby runtime) {
        RubyClass rb_cUDPSocket = runtime.defineClass("UDPSocket", runtime.getClass("IPSocket"), UDPSOCKET_ALLOCATOR);

        rb_cUDPSocket.includeModule(runtime.getClass("Socket").getConstant("Constants"));

        rb_cUDPSocket.defineAnnotatedMethods(RubyUDPSocket.class);

        runtime.getObject().setConstant("UDPsocket", rb_cUDPSocket);
    }

    private static ObjectAllocator UDPSOCKET_ALLOCATOR = new ObjectAllocator() {

        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyUDPSocket(runtime, klass);
        }
    };

    public RubyUDPSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @Override
    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context) {
        Ruby runtime = context.runtime;

        try {
            DatagramChannel channel = DatagramChannel.open();
            initSocket(newChannelFD(runtime, channel));
        }
        catch (ConnectException e) {
            throw runtime.newErrnoECONNREFUSEDError();
        }
        catch (UnknownHostException e) {
            throw SocketUtils.sockerr(runtime, "initialize: name or service not known");
        }
        catch (IOException e) {
            throw sockerr(runtime, "initialize: name or service not known", e);
        }

        return this;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject protocol) {
        // we basically ignore protocol. let someone report it...
        return initialize(context);
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
                final int intAddr = RubyNumeric.fix2int(host);
                final RubyModule Socket = runtime.getModule("Socket");
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
                ((DatagramChannel) channel).socket().bind(addr);
            } else {
                multicastStateManager.rebindToPort(port);
            }

            return RubyFixnum.zero(runtime);
        }
        catch (UnknownHostException e) {
            throw SocketUtils.sockerr(runtime, "bind: name or service not known");
        }
        catch (SocketException e) {
            final String message = e.getMessage();
            if ( message != null ) {
                switch ( message ) {
                    case "Permission denied" :
                        throw runtime.newErrnoEACCESError("bind(2) for " + host.inspect() + " port " + port);
                }
            }
            throw sockerr(runtime, "bind: name or service not known", e);
        }
        catch (IOException e) {
            throw sockerr(runtime, "bind: name or service not known", e);
        }
        //catch (Error e) {
        //    // Workaround for a bug in Sun's JDK 1.5.x, see
        //    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6303753
        //    if (e.getCause() instanceof SocketException) {
        //        throw SocketUtils.sockerr(runtime, "bind: name or service not known");
        //    } else {
        //        throw e;
        //    }
        //}
    }

    @JRubyMethod
    public IRubyObject connect(ThreadContext context, IRubyObject host, IRubyObject port) {
        Ruby runtime = context.runtime;

        try {
            InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName(host.convertToString().toString()), SocketUtils.portToInt(port));

            ((DatagramChannel) this.getChannel()).connect(addr);

            return RubyFixnum.zero(runtime);
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
    }

    @JRubyMethod
    public IRubyObject recvfrom_nonblock(ThreadContext context, IRubyObject length) {
        return recv_nonblock(context, length, context.nil, context.nil, false);
    }

    @JRubyMethod(required = 1, optional = 3) // (length) required = 1 handled above
    public IRubyObject recvfrom_nonblock(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        int argc = args.length;
        IRubyObject opts = ArgsUtil.getOptionsArg(context.runtime, args);
        if (!opts.isNil()) argc--;

        IRubyObject length = context.nil;
        IRubyObject flags = length;
        IRubyObject str = length;

        switch (argc) {
            case 3:
                str = args[3];
            case 2:
                flags = args[2];
            case 1:
                length = args[1];
        }

        boolean exception = ArgsUtil.extractKeywordArg(context, "exception", opts) != runtime.getFalse();

        return recvfrom_nonblock(context, length, flags, str, exception);
    }

    public IRubyObject recvfrom_nonblock(ThreadContext context, IRubyObject _length, IRubyObject _flags, IRubyObject str, boolean ex) {
        final Ruby runtime = context.runtime;

        try {
            int length = RubyNumeric.fix2int(_length);

            ReceiveTuple tuple = doReceiveNonblockTuple(runtime, length, ex);

            // TODO: make this efficient
            if (!str.isNil()) {
                tuple.result = str.convertToString().replace19(tuple.result);
            }

            IRubyObject addressArray = addrFor(context, tuple.sender, false);

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

    @JRubyMethod
    public IRubyObject send(ThreadContext context, IRubyObject _mesg, IRubyObject _flags, IRubyObject _to) {
        return send(context, _mesg, _flags);
    }

    @JRubyMethod(required = 2, optional = 2)
    public IRubyObject send(ThreadContext context, IRubyObject[] args) {
        // TODO: implement flags
        Ruby runtime = context.runtime;
        IRubyObject _mesg = args[0];
        IRubyObject _flags = args[1];

        try {
            int written;

            if (args.length == 2 || args.length == 3) {
                return send(context, _mesg, _flags);
            }

            IRubyObject _host = args[2];
            IRubyObject _port = args[3];

            RubyString nameStr = _host.convertToString();
            RubyString data = _mesg.convertToString();
            ByteList dataBL = data.getByteList();
            ByteBuffer buf = ByteBuffer.wrap(dataBL.unsafeBytes(), dataBL.begin(), dataBL.realSize());

            byte[] buf2 = data.getBytes();
            DatagramPacket sendDP;

            int port;
            if (_port instanceof RubyString) {

                Service service = Service.getServiceByName(_port.asJavaString(), "udp");

                if (service != null) {
                    port = service.getPort();
                } else {
                    port = (int)_port.convertToInteger("to_i").getLongValue();
                }

            } else {
                port = (int)_port.convertToInteger().getLongValue();
            }

            InetAddress address = SocketUtils.getRubyInetAddress(nameStr.getByteList());
            InetSocketAddress addr = new InetSocketAddress(address, port);

            if (this.multicastStateManager == null) {
                written = ((DatagramChannel) this.getChannel()).send(buf, addr);

            } else {
                sendDP = new DatagramPacket(buf2, buf2.length, address, port);
                multicastStateManager.rebindToPort(port);
                MulticastSocket ms = this.multicastStateManager.getMulticastSocket();

                ms.send(sendDP);
                written = sendDP.getLength();
            }

            return runtime.newFixnum(written);

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
    public IRubyObject recvfrom(ThreadContext context, IRubyObject _length) {
        final Ruby runtime = context.runtime;

        try {
            int length = RubyNumeric.fix2int(_length);

            ReceiveTuple tuple = doReceiveTuple(runtime, length, true);

            IRubyObject addressArray = addrFor(context, tuple.sender, false);

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
    public IRubyObject recv(ThreadContext context, IRubyObject _length) {
        final Ruby runtime = context.runtime;

        try {
            return doReceive(runtime, RubyNumeric.fix2int(_length));
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
     * Overrides BasicSocket#recv
     */
    @Override
    public IRubyObject recv(ThreadContext context, IRubyObject _length, IRubyObject _flags) {
        // TODO: implement flags
        return recv(context, _length);
    }

    private ReceiveTuple doReceiveTuple(Ruby runtime, int length, boolean ex) throws IOException {
        ReceiveTuple tuple = new ReceiveTuple();

        if (this.multicastStateManager == null) {
            doReceive(runtime, length, ex, tuple);
        } else {
            doReceiveMulticast(runtime, length, ex, tuple);
        }

        return tuple;
    }

    private ReceiveTuple doReceiveNonblockTuple(Ruby runtime, int length, boolean ex) throws IOException {
        DatagramChannel channel = (DatagramChannel)getChannel();

        synchronized (channel.blockingLock()) {
            boolean oldBlocking = channel.isBlocking();

            channel.configureBlocking(false);

            try {
                return doReceiveTuple(runtime, length, ex);
            }
            finally {
                channel.configureBlocking(oldBlocking);
            }
        }
    }

    private static class ReceiveTuple {
        ReceiveTuple() {}
        ReceiveTuple(RubyString result, InetSocketAddress sender) {
            this.result = result;
            this.sender = sender;
        }

        RubyString result;
        InetSocketAddress sender;
    }

    private IRubyObject doReceive(Ruby runtime, int length) throws IOException {
        return doReceive(runtime, length, true, null);
    }

    private IRubyObject doReceive(Ruby runtime, int length, boolean ex, ReceiveTuple tuple) throws IOException {
        DatagramChannel channel = (DatagramChannel)getChannel();

        ByteBuffer buf = ByteBuffer.allocate(length);

        InetSocketAddress sender = (InetSocketAddress)channel.receive(buf);

        if (sender == null) {
            // noblocking receive
            if (!ex) return runtime.newSymbol("wait_readable");
            throw runtime.newErrnoEAGAINReadableError("recvfrom(2) would block");
        }

        // see JRUBY-4678
        if (sender == null) {
            throw runtime.newErrnoECONNRESETError();
        }

        RubyString result = runtime.newString(new ByteList(buf.array(), 0, buf.position()));

        if (tuple != null) {
            tuple.result = result;
            tuple.sender = sender;
        }

        return result;
    }

    private IRubyObject doReceiveMulticast(Ruby runtime, int length, boolean ex, ReceiveTuple tuple) throws IOException {
        byte[] buf2 = new byte[length];
        ByteBuffer recv = ByteBuffer.wrap(buf2);
        SocketAddress address;

        DatagramChannel channel = this.multicastStateManager.getMulticastSocket().getChannel();

        address = channel.receive(recv);

        if (address == null) {
            if (!ex) return runtime.newSymbol("wait_readable");
            throw runtime.newErrnoEAGAINReadableError("multicast UDP does not support nonblocking");
        }

        InetSocketAddress sender = (InetSocketAddress) address;

        // see JRUBY-4678
        if (sender == null) {
            throw runtime.newErrnoECONNRESETError();
        }

        recv.flip();
        RubyString result = runtime.newString(new ByteList(recv.array(), recv.position(), recv.limit()));

        if (tuple != null) {
            tuple.result = result;
            tuple.sender = sender;
        }

        return result;
    }

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

