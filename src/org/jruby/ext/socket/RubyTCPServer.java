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

import java.io.FileDescriptor;
import java.io.IOException;

import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import java.nio.channels.ServerSocketChannel;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyIO;
import org.jruby.RubyInteger;
import org.jruby.RubyNumeric;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.InvalidValueException;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubyTCPServer extends RubyTCPSocket {
    static void createTCPServer(Ruby runtime) {
        RubyClass rb_cTCPServer = runtime.defineClass("TCPServer", runtime.fastGetClass("TCPSocket"), TCPSERVER_ALLOCATOR);

        rb_cTCPServer.defineAnnotatedMethods(RubyTCPServer.class);
        
        runtime.getObject().fastSetConstant("TCPserver",rb_cTCPServer);
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

    @JRubyMethod(name = "initialize", required = 1, optional = 1, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(IRubyObject[] args) {
        IRubyObject hostname = args[0];
        IRubyObject port = args.length > 1 ? args[1] : getRuntime().getNil();
        
        if(hostname.isNil()) {
            hostname = getRuntime().newString("0.0.0.0");
        } else if (hostname instanceof RubyFixnum) {
            // numeric host, use it for port
            port = hostname;
            hostname = getRuntime().newString("0.0.0.0");
        }
        
        String shost = hostname.convertToString().toString();
        try {
            InetAddress addr = InetAddress.getByName(shost);
            ssc = ServerSocketChannel.open();
            
            int portInt;
            if (port instanceof RubyInteger) {
                portInt = RubyNumeric.fix2int(port);
            } else {
                IRubyObject portString = port.convertToString();
                IRubyObject portInteger = portString.convertToInteger(MethodIndex.TO_I, "to_i");
                portInt = RubyNumeric.fix2int(portInteger);

                if (portInt <= 0) {
                    portInt = RubyNumeric.fix2int(RubySocket.getservbyname(getRuntime().getObject(), new IRubyObject[] {portString}));
                }
            }
            
            socket_address = new InetSocketAddress(addr, portInt);
            ssc.socket().bind(socket_address);
            initSocket(new ChannelDescriptor(ssc, RubyIO.getNewFileno(), new ModeFlags(ModeFlags.RDWR), new FileDescriptor()));
        } catch (InvalidValueException ex) {
            throw getRuntime().newErrnoEINVALError();
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

    @JRubyMethod(name = "accept")
    public IRubyObject accept() {
        RubyTCPSocket socket = new RubyTCPSocket(getRuntime(),getRuntime().fastGetClass("TCPSocket"));
        ThreadContext context = getRuntime().getCurrentContext();
        
        try {
            ssc.configureBlocking(false);
            
            while (true) {
                boolean ready = context.getThread().selectForAccept(this);
                if (!ready) {
                    // we were woken up without being selected...poll for thread events and go back to sleep
                    getRuntime().getCurrentContext().pollThreadEvents();
                } else {
                    try {
                        // otherwise one key has been selected (ours) so we get the channel and hand it off
                        socket.initSocket(new ChannelDescriptor(ssc.accept(), RubyIO.getNewFileno(), new ModeFlags(ModeFlags.RDWR), new FileDescriptor()));
                    } catch (InvalidValueException ex) {
                        throw getRuntime().newErrnoEINVALError();
                    }
                    return socket;
                }
            }
        } catch(IOException e) {
            throw sockerr(this, "problem when accepting");
        }
    }

    @JRubyMethod(name = "accept_nonblock")
    public IRubyObject accept_nonblock() {
        RubyTCPSocket socket = new RubyTCPSocket(getRuntime(),getRuntime().fastGetClass("TCPSocket"));
        Selector selector = null;
        try {
            ssc.configureBlocking(false);
            selector = Selector.open();
            SelectionKey key = ssc.register(selector, SelectionKey.OP_ACCEPT);
            
            int selected = selector.selectNow();
            if (selected == 0) {
                // no connection immediately accepted, let them try again
                throw getRuntime().newErrnoEAGAINError("Resource temporarily unavailable");
            } else {
                try {
                    // otherwise one key has been selected (ours) so we get the channel and hand it off
                    socket.initSocket(new ChannelDescriptor(ssc.accept(), RubyIO.getNewFileno(), new ModeFlags(ModeFlags.RDWR), new FileDescriptor()));
                } catch (InvalidValueException ex) {
                    throw getRuntime().newErrnoEINVALError();
                }
                return socket;
            }
        } catch(IOException e) {
            throw sockerr(this, "problem when accepting");
        } finally {
            try {
                if (selector != null) selector.close();
            } catch (IOException ioe) {
            }
        }
    }

    @JRubyMethod(name = "listen", required = 1)
    public IRubyObject listen(IRubyObject backlog) {
        return RubyFixnum.zero(getRuntime());
    }

    @JRubyMethod(name = "peeraddt", rest = true)
    public IRubyObject peeraddr(IRubyObject[] args) {
        throw getRuntime().newNotImplementedError("not supported");
    }

    @JRubyMethod(name = "getpeername", rest = true)
    public IRubyObject getpeername(IRubyObject[] args) {
        throw getRuntime().newNotImplementedError("not supported");
    }

    @JRubyMethod(name = "open", rest = true, frame = true, meta = true)
    public static IRubyObject open(IRubyObject recv, IRubyObject[] args, Block block) {
        ThreadContext context = recv.getRuntime().getCurrentContext();
        IRubyObject tcpServer = recv.callMethod(context, "new", args);
        
        if (!block.isGiven()) return tcpServer;
        
        try {
            return block.yield(context, tcpServer);
        } finally {
            tcpServer.callMethod(context, "close");
        }
    }
}
