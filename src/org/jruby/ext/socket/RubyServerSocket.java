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

import jnr.constants.platform.AddressFamily;
import jnr.constants.platform.Sock;
import jnr.netdb.Service;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.io.BadDescriptorException;
import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.Sockaddr;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jnr.constants.platform.AddressFamily.AF_INET;
import static jnr.constants.platform.AddressFamily.AF_INET6;
import static jnr.constants.platform.IPProto.IPPROTO_TCP;
import static jnr.constants.platform.IPProto.IPPROTO_UDP;
import static jnr.constants.platform.NameInfo.NI_NUMERICHOST;
import static jnr.constants.platform.NameInfo.NI_NUMERICSERV;
import static jnr.constants.platform.ProtocolFamily.PF_INET;
import static jnr.constants.platform.ProtocolFamily.PF_INET6;
import static jnr.constants.platform.Sock.SOCK_DGRAM;
import static jnr.constants.platform.Sock.SOCK_STREAM;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
@JRubyClass(name="Socket", parent="BasicSocket", include="Socket::Constants")
public class RubyServerSocket extends RubySocket {
    static void createServerSocket(Ruby runtime) {
        RubyClass rb_cSocket = runtime.defineClass("ServerSocket", runtime.getClass("Socket"), SERVER_SOCKET_ALLOCATOR);

        rb_cSocket.defineAnnotatedMethods(RubyServerSocket.class);
    }

    private static ObjectAllocator SERVER_SOCKET_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyServerSocket(runtime, klass);
        }
    };

    public RubyServerSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(name = "listen")
    public IRubyObject listen(ThreadContext context, IRubyObject backlog) {
        return context.getRuntime().newFixnum(0);
    }

    @JRubyMethod()
    public IRubyObject connect_nonblock(ThreadContext context, IRubyObject arg) {
        throw sockerr(context.runtime, "server socket cannot connect");
    }

    @JRubyMethod()
    public IRubyObject connect(ThreadContext context, IRubyObject arg) {
        throw sockerr(context.runtime, "server socket cannot connect");
    }

    @JRubyMethod()
    public IRubyObject accept(ThreadContext context) {
        Ruby runtime = context.runtime;
        ServerSocketChannel channel = (ServerSocketChannel)getChannel();

        try {
            SocketChannel socket = channel.accept();
            RubySocket rubySocket = new RubySocket(runtime, runtime.getClass("Socket"));
            rubySocket.initFromServer(runtime, this, socket);

            return rubySocket;
            
        } catch (IOException ioe) {
            throw sockerr(runtime, "bind(2): name or service not known");

        }
    }

    protected ChannelDescriptor initChannel(Ruby runtime) {
        Channel channel;

        try {
            if(soType == Sock.SOCK_STREAM) {
                channel = ServerSocketChannel.open();

            } else {
                throw runtime.newArgumentError("unsupported server socket type `" + soType + "'");

            }

            ModeFlags modeFlags = newModeFlags(runtime, ModeFlags.RDWR);

            return new ChannelDescriptor(channel, modeFlags);

        } catch(IOException e) {
            throw sockerr(runtime, "initialize: " + e.toString());

        }
    }
}// RubySocket
