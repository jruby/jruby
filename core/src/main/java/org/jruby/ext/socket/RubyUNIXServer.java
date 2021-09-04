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
 * Copyright (C) 2008 Ola Bini <ola.bini@gmail.com>
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


import jnr.unixsocket.UnixServerSocketChannel;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

@JRubyClass(name="UNIXServer", parent="UNIXSocket")
public class RubyUNIXServer extends RubyUNIXSocket {
    static void createUNIXServer(Ruby runtime) {
        RubyClass rb_cUNIXServer = runtime.defineClass("UNIXServer", runtime.getClass("UNIXSocket"), RubyUNIXServer::new);

        runtime.getObject().setConstant("UNIXserver", rb_cUNIXServer);
        
        rb_cUNIXServer.defineAnnotatedMethods(RubyUNIXServer.class);
    }

    public RubyUNIXServer(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject path) {
        init_unixsock(context.runtime, path, true);

        return this;
    }

    @JRubyMethod
    public IRubyObject accept(ThreadContext context) {
        Ruby runtime = context.runtime;

        try {

            while (true) { // select loop to allow interrupting
                boolean ready = context.getThread().select(this, SelectionKey.OP_ACCEPT);

                if (!ready) {
                    // we were woken up without being selected...poll for thread events and go back to sleep
                    context.pollThreadEvents();

                } else {
                    UnixSocketChannel socketChannel = asUnixServer().accept();

                    RubyUNIXSocket sock = (RubyUNIXSocket)(Helpers.invoke(context, runtime.getClass("UNIXSocket"), "allocate"));

                    sock.init_sock(context.runtime, socketChannel, "");

                    return sock;
                }
            }

        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        }
    }

    @JRubyMethod
    public IRubyObject accept_nonblock(ThreadContext context) {
        return accept_nonblock(context, context.runtime, true);
    }

    @JRubyMethod
    public IRubyObject accept_nonblock(ThreadContext context, IRubyObject opts) {
        return accept_nonblock(context, context.runtime, extractExceptionArg(context, opts));
    }

    public IRubyObject accept_nonblock(ThreadContext context, Ruby runtime, boolean ex) {
        SelectableChannel selectable = (SelectableChannel)getChannel();

        synchronized (selectable.blockingLock()) {
            boolean oldBlocking = selectable.isBlocking();

            try {
                selectable.configureBlocking(false);

                try {
                    UnixSocketChannel socketChannel = ((UnixServerSocketChannel) selectable).accept();

                    if (socketChannel == null) {
                        if (!ex) return runtime.newSymbol("wait_readable");
                        throw runtime.newErrnoEAGAINReadableError("accept(2) would block");
                    }

                    RubyUNIXSocket sock = (RubyUNIXSocket)(Helpers.invoke(context, runtime.getClass("UNIXSocket"), "allocate"));

                    sock.init_sock(context.runtime, socketChannel, "");

                    return sock;

                } finally {
                    selectable.configureBlocking(oldBlocking);
                }

            } catch (IOException ioe) {
                if (ioe.getMessage().equals("accept failed: Resource temporarily unavailable")) {
                    if (!ex) return runtime.newSymbol("wait_readable");
                    throw runtime.newErrnoEAGAINReadableError("accept");
                }

                throw context.runtime.newIOErrorFromException(ioe);
            }
        }
    }

    @JRubyMethod
    public IRubyObject listen(ThreadContext context, IRubyObject log) {
        // TODO listen backlog
        return context.runtime.newFixnum(0);
    }

    @JRubyMethod
    public IRubyObject sysaccept(ThreadContext context) {
        RubyUNIXSocket socket = (RubyUNIXSocket) accept(context);
        return context.runtime.newFixnum(((UnixSocketChannel) socket.getChannel()).getFD());
    }

    @JRubyMethod
    public IRubyObject path(ThreadContext context) {
        return context.runtime.newString(openFile.getPath());
    }

    @JRubyMethod
    public IRubyObject addr(ThreadContext context) {
        Ruby runtime = context.runtime;

        return runtime.newArray(
                runtime.newString("AF_UNIX"),
                runtime.newString(openFile.getPath()));
    }

    @JRubyMethod
    public IRubyObject peeraddr(ThreadContext context) {
        throw context.runtime.newErrnoENOTCONNError();
    }

    @Override
    protected UnixSocketAddress getUnixSocketAddress() {
        SocketAddress socketAddress = ((UnixServerSocketChannel)getChannel()).getLocalSocketAddress();
        if (socketAddress instanceof UnixSocketAddress) return (UnixSocketAddress) socketAddress;
        return null;
    }

    @Override
    protected UnixSocketAddress getUnixRemoteSocket() {
        SocketAddress socketAddress = ((UnixServerSocketChannel)getChannel()).getLocalSocketAddress();
        if (socketAddress instanceof UnixSocketAddress) return (UnixSocketAddress) socketAddress;
        return null;
    }

    private UnixServerSocketChannel asUnixServer() {
        return (UnixServerSocketChannel)getChannel();
    }
}// RubyUNIXServer
