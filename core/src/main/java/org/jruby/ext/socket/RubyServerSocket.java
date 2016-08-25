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

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.ObjectAllocator;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
@Deprecated
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

    @Deprecated
    @JRubyMethod()
    public IRubyObject bind(ThreadContext context, IRubyObject addr) {
        return bind(context, addr, RubyFixnum.zero(context.runtime));
    }

    @Deprecated
    @JRubyMethod()
    public IRubyObject bind(ThreadContext context, IRubyObject addr, IRubyObject backlog) {
        this.backlog = backlog.convertToInteger().getIntValue();

        return super.bind(context, addr);
    }
}// RubySocket
