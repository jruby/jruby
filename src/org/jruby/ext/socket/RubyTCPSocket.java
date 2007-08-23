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

import java.io.IOException;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.nio.channels.SocketChannel;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubyTCPSocket extends RubyIPSocket {
    static void createTCPSocket(Ruby runtime) {
        RubyClass rb_cTCPSocket = runtime.defineClass("TCPSocket", runtime.getClass("IPSocket"), TCPSOCKET_ALLOCATOR);
        CallbackFactory cfact = runtime.callbackFactory(RubyTCPSocket.class);

        rb_cTCPSocket.includeModule(runtime.getClass("Socket").getConstant("Constants"));

        rb_cTCPSocket.defineFastMethod("initialize", cfact.getFastMethod("initialize",IRubyObject.class, IRubyObject.class));
        rb_cTCPSocket.defineFastMethod("setsockopt", cfact.getFastOptMethod("setsockopt"));
        rb_cTCPSocket.getMetaClass().defineFastMethod("gethostbyname", cfact.getFastSingletonMethod("gethostbyname", IRubyObject.class));
        rb_cTCPSocket.getMetaClass().defineMethod("open", cfact.getOptSingletonMethod("open"));

        runtime.getObject().setConstant("TCPsocket",rb_cTCPSocket);
    }

    private static ObjectAllocator TCPSOCKET_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyTCPSocket(runtime, klass);
        }
    };

    public RubyTCPSocket(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    public IRubyObject initialize(IRubyObject arg1, IRubyObject port) {
        if (port instanceof RubyString) {
            port = RubyNumeric.str2inum(getRuntime(), (RubyString)port, 0, true);
        }
        int porti = RubyNumeric.fix2int(port);
        try {
            InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName(arg1.convertToString().toString()),porti);
            SocketChannel channel = SocketChannel.open(addr);
            channel.finishConnect();
            setChannel(channel);
        } catch(ConnectException e) {
            throw getRuntime().newErrnoECONNREFUSEDError();
        } catch(UnknownHostException e) {
            throw sockerr(this, "initialize: name or service not known");
        } catch(IOException e) {
            throw sockerr(this, "initialize: name or service not known");
        }
        return this;
    }

    public IRubyObject setsockopt(IRubyObject[] args) {
        // Stubbed out
        return getRuntime().getNil();
    }

    public static IRubyObject open(IRubyObject recv, IRubyObject[] args, Block block) {
        RubyTCPSocket sock = (RubyTCPSocket)recv.callMethod(recv.getRuntime().getCurrentContext(),"new",args);
        if (!block.isGiven()) return sock;

        try {
            return block.yield(recv.getRuntime().getCurrentContext(), sock);
        } finally {
            if (sock.isOpen()) sock.close();
        }
    }

    public static IRubyObject gethostbyname(IRubyObject recv, IRubyObject hostname) {
        try {
            IRubyObject[] ret = new IRubyObject[4];
            Ruby r = recv.getRuntime();
            InetAddress addr = InetAddress.getByName(hostname.convertToString().toString());
            ret[0] = r.newString(addr.getCanonicalHostName());
            ret[1] = r.newArray();
            ret[2] = r.newFixnum(2); //AF_INET
            ret[3] = r.newString(addr.getHostAddress());
            return r.newArrayNoCopy(ret);
        } catch(UnknownHostException e) {
            throw sockerr(recv, "gethostbyname: name or service not known");
        }
    }
}// RubyTCPSocket
