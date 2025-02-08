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
 * Copyright (C) 2019 The JRuby Team
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

package org.jruby.javasupport.ext;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.api.Access;
import org.jruby.ext.bigdecimal.RubyBigDecimal;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Error.nameError;
import static org.jruby.javasupport.JavaUtil.unwrapIfJavaObject;

/**
 * Java::JavaMath package extensions.
 *
 * @author kares
 */
public class JavaMath {

    public static void define(ThreadContext context) {
        JavaExtensions.put(context.runtime, java.math.BigDecimal.class,proxy -> proxy.defineMethods(context, BigDecimal.class));
    }

    @JRubyModule(name = "Java::JavaMath::BigDecimal")
    public static class BigDecimal {
        @JRubyMethod(name = "to_d") // bigdecimal/util.rb
        public static IRubyObject to_d(ThreadContext context, IRubyObject self) {
            return asRubyBigDecimal(context, unwrapIfJavaObject(self));
        }

        @JRubyMethod(name = "to_f") // override from java.lang.Number
        public static IRubyObject to_f(ThreadContext context, IRubyObject self) {
            return asRubyBigDecimal(context, unwrapIfJavaObject(self)).to_f(context);
        }

        @JRubyMethod(name = { "to_i", "to_int" }) // override from java.lang.Number
        public static IRubyObject to_i(ThreadContext context, IRubyObject self) {
            return asRubyBigDecimal(context, unwrapIfJavaObject(self)).to_int(context);
        }

        @JRubyMethod(name = "coerce") // override from java.lang.Number
        public static IRubyObject coerce(final ThreadContext context, final IRubyObject self, final IRubyObject type) {
            return newArray(context, type, asRubyBigDecimal(context, unwrapIfJavaObject(self)));
        }

        @JRubyMethod(name = "to_r")
        public static IRubyObject to_r(ThreadContext context, IRubyObject self) {
            return asRubyBigDecimal(context, unwrapIfJavaObject(self)).to_r(context);
        }

        private static RubyBigDecimal asRubyBigDecimal(ThreadContext context, final java.math.BigDecimal value) {
            final RubyClass klass = Access.getClass(context, "BigDecimal");
            if (klass == null) { // user should require 'bigdecimal'
                throw nameError(context, "uninitialized constant BigDecimal", asSymbol(context, "BigDecimal"));
            }
            return new RubyBigDecimal(context.runtime, klass, value);
        }

    }

}