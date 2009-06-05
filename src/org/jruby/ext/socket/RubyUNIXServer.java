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
 * Copyright (C) 2008 Ola Bini <ola.bini@gmail.com>
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


import com.kenai.jaffl.byref.IntByReference;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyNumeric;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.InvalidValueException;

/**
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
@JRubyClass(name="UNIXServer", parent="UNIXSocket")
public class RubyUNIXServer extends RubyUNIXSocket {
    private static ObjectAllocator UNIXSERVER_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyUNIXServer(runtime, klass);
        }
    };

    static void createUNIXServer(Ruby runtime) {
        RubyClass rb_cUNIXServer = runtime.defineClass("UNIXServer", runtime.fastGetClass("UNIXSocket"), UNIXSERVER_ALLOCATOR);
        runtime.getObject().fastSetConstant("UNIXserver", rb_cUNIXServer);
        
        rb_cUNIXServer.defineAnnotatedMethods(RubyUNIXServer.class);
    }

    public RubyUNIXServer(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject path) {
        init_unixsock(context.getRuntime(), path, true);
        return this;
    }

    @Deprecated
    public IRubyObject accept() {
        return accept(getRuntime().getCurrentContext());
    }
    @JRubyMethod
    public IRubyObject accept(ThreadContext context) {
        LibCSocket.sockaddr_un from = LibCSocket.sockaddr_un.newInstance();
        int fd2 = INSTANCE.accept(fd, from, new IntByReference(LibCSocket.sockaddr_un.LENGTH));
        if(fd2 < 0) {
            rb_sys_fail(context.getRuntime(), null);
        }

        Ruby runtime = context.getRuntime();
        RubyUNIXSocket sock = (RubyUNIXSocket)(RuntimeHelpers.invoke(context, runtime.fastGetClass("UNIXSocket"), "allocate"));
        
        sock.fd = fd2;
        sock.fpath = from.path().toString();

        sock.init_sock(context.getRuntime());

        return sock;
    }
    @Deprecated
    public IRubyObject accept_nonblock() {
        return accept_nonblock(getRuntime().getCurrentContext());
    }
    @JRubyMethod
    public IRubyObject accept_nonblock(ThreadContext context) {
        LibCSocket.sockaddr_un from = LibCSocket.sockaddr_un.newInstance();
        IntByReference fromlen = new IntByReference(LibCSocket.sockaddr_un.LENGTH);
        
        int flags = INSTANCE.fcntl(fd, RubyUNIXSocket.F_GETFL ,0);
        INSTANCE.fcntl(fd, RubyUNIXSocket.F_SETFL, flags | RubyUNIXSocket.O_NONBLOCK);

        int fd2 = INSTANCE.accept(fd, from, new IntByReference(LibCSocket.sockaddr_un.LENGTH));
        if(fd2 < 0) {
            rb_sys_fail(context.getRuntime(), null);
        }

        Ruby runtime = context.getRuntime();
        RubyUNIXSocket sock = (RubyUNIXSocket)(RuntimeHelpers.invoke(context, runtime.fastGetClass("UNIXSocket"), "allocate"));
        
        sock.fd = fd2;
        sock.fpath = from.path().toString();

        sock.init_sock(context.getRuntime());

        return sock;
    }
    @Deprecated
    public IRubyObject sysaccept() {
        return accept(getRuntime().getCurrentContext());
    }
    @JRubyMethod
    public IRubyObject sysaccept(ThreadContext context) {
        return accept(context);
    }
    @Deprecated
    public IRubyObject listen(IRubyObject log) {
        return listen(getRuntime().getCurrentContext(), log);
    }
    @JRubyMethod
    public IRubyObject listen(ThreadContext context, IRubyObject log) {
        if(INSTANCE.listen(fd, RubyNumeric.fix2int(log)) < 0) {
            rb_sys_fail(context.getRuntime(), "listen(2)");
        }
        return context.getRuntime().newFixnum(0);
    }
}// RubyUNIXServer
