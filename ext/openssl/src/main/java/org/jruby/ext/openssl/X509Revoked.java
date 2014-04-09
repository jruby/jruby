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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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
package org.jruby.ext.openssl;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Visibility;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509Revoked extends RubyObject {

    private static final long serialVersionUID = -6238325248555061878L;

    private static ObjectAllocator X509REVOKED_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new X509Revoked(runtime, klass);
        }
    };

    public static void createX509Revoked(Ruby runtime, RubyModule mX509) {
        RubyClass cX509Rev = mX509.defineClassUnder("Revoked",runtime.getObject(),X509REVOKED_ALLOCATOR);
        RubyClass openSSLError = runtime.getModule("OpenSSL").getClass("OpenSSLError");
        mX509.defineClassUnder("RevokedError",openSSLError,openSSLError.getAllocator());

        cX509Rev.defineAnnotatedMethods(X509Revoked.class);
    }

    private IRubyObject serial;
    private IRubyObject extensions;
    private IRubyObject time;

    public X509Revoked(Ruby runtime, RubyClass type) {
        super(runtime,type);
    }

    @JRubyMethod(name="initialize",rest=true, visibility = Visibility.PRIVATE)
    public IRubyObject _initialize(final ThreadContext context, final IRubyObject[] args, final Block unusedBlock) {
        serial = time = context.runtime.getNil();
        extensions = context.runtime.newArray();
        return this;
    }

    @JRubyMethod
    public IRubyObject serial() {
        return this.serial;
    }

    @JRubyMethod(name="serial=")
    public IRubyObject set_serial(IRubyObject val) {
        this.serial = val;
        return val;
    }

    @JRubyMethod
    public IRubyObject time() {
        return this.time;
    }

    @JRubyMethod(name="time=")
    public IRubyObject set_time(IRubyObject val) {
        this.time = val;
        return val;
    }

    @JRubyMethod
    public IRubyObject extensions() {
        return this.extensions;
    }

    @JRubyMethod(name="extensions=")
    public IRubyObject set_extensions(IRubyObject val) {
        this.extensions = val;
        return val;
    }

    @JRubyMethod
    public IRubyObject add_extension(final ThreadContext context, final IRubyObject val) {
        this.extensions.callMethod(context, "<<", val);
        return val;
    }
}// X509Revoked
