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

import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.nio.channels.ServerSocketChannel;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyNumeric;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubyTCPServer extends RubyTCPSocket {
    static void createTCPServer(Ruby runtime) {
        RubyClass rb_cTCPServer = runtime.defineClass("TCPServer", runtime.getClass("TCPSocket"), TCPSERVER_ALLOCATOR);
        CallbackFactory cfact = runtime.callbackFactory(RubyTCPServer.class);

        rb_cTCPServer.defineFastMethod("initialize", cfact.getFastMethod("initialize",IRubyObject.class, IRubyObject.class));
        rb_cTCPServer.defineFastMethod("peeraddr", cfact.getFastOptMethod("peeraddr"));
        rb_cTCPServer.defineFastMethod("getpeername", cfact.getFastOptMethod("getpeername"));
        rb_cTCPServer.defineFastMethod("accept", cfact.getFastMethod("accept"));
        rb_cTCPServer.defineFastMethod("close", cfact.getFastMethod("close"));
        rb_cTCPServer.defineFastMethod("listen", cfact.getFastMethod("listen",IRubyObject.class));
        rb_cTCPServer.getMetaClass().defineMethod("open", cfact.getOptSingletonMethod("open"));
        
        runtime.getObject().setConstant("TCPserver",rb_cTCPServer);
    }

    private static ObjectAllocator TCPSERVER_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyTCPServer(runtime, klass);
        }
    };

    public RubyTCPServer(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    private ServerSocketChannel ssc;
    private InetSocketAddress socket_address;

    public IRubyObject initialize(IRubyObject hostname, IRubyObject port) {
        if(hostname.isNil()) {
            hostname = getRuntime().newString("0.0.0.0");
        }
        String shost = hostname.convertToString().toString();
        try {
            InetAddress addr = InetAddress.getByName(shost);
            ssc = ServerSocketChannel.open();
            socket_address = new InetSocketAddress(addr, RubyNumeric.fix2int(port));
            ssc.socket().bind(socket_address);
            setChannel(ssc);
        } catch(UnknownHostException e) {
            throw sockerr(this, "initialize: name or service not known");
        } catch(BindException e) {
            //            e.printStackTrace();
            throw getRuntime().newErrnoEADDRINUSEError();
        } catch(IOException e) {
            throw sockerr(this, "initialize: name or service not known");
        }

        return this;
    }

    public IRubyObject accept() {
        RubyTCPSocket socket = new RubyTCPSocket(getRuntime(),getRuntime().getClass("TCPSocket"));
        try {
            socket.setChannel(ssc.accept());
        } catch(IOException e) {
            throw sockerr(this, "problem when accepting");
        }
        return socket;
    }

    public IRubyObject close() {
        try {
            ssc.close();
        } catch(IOException e) {
            throw sockerr(this, "problem when closing");
        }
        return getRuntime().getNil();
    }

    public IRubyObject listen(IRubyObject backlog) {
        return RubyFixnum.zero(getRuntime());
    }

    public IRubyObject peeraddr(IRubyObject[] args) {
        throw getRuntime().newNotImplementedError("not supported");
    }

    public IRubyObject getpeername(IRubyObject[] args) {
        throw getRuntime().newNotImplementedError("not supported");
    }

    public static IRubyObject open(IRubyObject recv, IRubyObject[] args, Block block) {
        ThreadContext context = recv.getRuntime().getCurrentContext();
        IRubyObject tcpServer = recv.callMethod(context,"new",args);
        if (block.isGiven()) {
            try {
                return block.yield(context, tcpServer);
            } finally {
                tcpServer.callMethod(context, "close");
            }
        } else {
            return recv.callMethod(recv.getRuntime().getCurrentContext(),"new",args);
        }
    }
}// RubyTCPServer
