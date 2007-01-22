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
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2006 Thomas E Enebo <enebo@acm.org>
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
package org.jruby;

import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/** Implementation of the Comparable module.
 *
 */
public class RubyComparable {

    public static RubyModule createComparable(IRuby runtime) {
        RubyModule comparableModule = runtime.defineModule("Comparable");
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyComparable.class);
        comparableModule.defineFastMethod("==", callbackFactory.getSingletonMethod("equal", IRubyObject.class));
        comparableModule.defineFastMethod(">", callbackFactory.getSingletonMethod("op_gt", IRubyObject.class));
        comparableModule.defineFastMethod(">=", callbackFactory.getSingletonMethod("op_ge", IRubyObject.class));
        comparableModule.defineFastMethod("<", callbackFactory.getSingletonMethod("op_lt", IRubyObject.class));
        comparableModule.defineFastMethod("<=", callbackFactory.getSingletonMethod("op_le", IRubyObject.class));
        comparableModule.defineFastMethod("between?", callbackFactory.getSingletonMethod("between_p",
                IRubyObject.class, IRubyObject.class));

        return comparableModule;
    }

    /*  ================
     *  Utility Methods
     *  ================ 
     */

    /** rb_cmpint
     * 
     */
    public static long cmpint(IRubyObject val, IRubyObject a, IRubyObject b) {
        if (val.isNil()) {
            cmperr(a, b);
        }
        if (val instanceof RubyFixnum) {
            return ((RubyFixnum) val).getLongValue();
        }
        if (val instanceof RubyBignum) {
            if (((RubyBignum) val).getValue().signum() == -1) {
                return 1;
            }
            return -1;
        }

        final IRuby runtime = val.getRuntime();
        final ThreadContext tc = runtime.getCurrentContext();
        final RubyFixnum zero = RubyFixnum.one(runtime);
        if (val.callMethod(tc, ">", zero).isTrue()) {
            return 1;
        }
        if (val.callMethod(tc, "<", zero).isTrue()) {
            return -1;
        }
        return 0;
    }

    /** rb_cmperr
     * 
     */
    public static void cmperr(IRubyObject recv, IRubyObject other) {
        IRubyObject target;
        if (other.isImmediate() || !(other.isNil() || other.isTrue() || other == recv.getRuntime().getFalse())) {
            target = other.inspect();
        } else {
            target = other.getType();
        }

        throw recv.getRuntime().newArgumentError("comparison of " + recv.getType() + " with " + target + " failed");
    }

    /*  ================
     *  Module Methods
     *  ================ 
     */

    /** cmp_equal (cmp_eq inlined here)
     * 
     */
    public static IRubyObject equal(IRubyObject recv, IRubyObject other) {
        if (recv == other) {
            return recv.getRuntime().getTrue();
        }
        IRuby runtime = recv.getRuntime();
        IRubyObject result = null;
        try {
            result = recv.callMethod(runtime.getCurrentContext(), "<=>", other);
        } catch (RaiseException e) {
            return recv.getRuntime().getFalse();
        }

        if (result.isNil()) {
            return result;
        }

        return RubyBoolean.newBoolean(runtime, cmpint(result, recv, other) == 0);
    }

    /** cmp_gt
     * 
     */
    // <=> may return nil in many circumstances, e.g. 3 <=> NaN        
    public static RubyBoolean op_gt(IRubyObject recv, IRubyObject other) {
        final IRuby runtime = recv.getRuntime();
        IRubyObject result = recv.callMethod(runtime.getCurrentContext(), "<=>", other);

        if (result.isNil()) {
            cmperr(recv, other);
        }

        return RubyBoolean.newBoolean(runtime, cmpint(result, recv, other) > 0);
    }

    /** cmp_ge
     * 
     */
    public static RubyBoolean op_ge(IRubyObject recv, IRubyObject other) {
        final IRuby runtime = recv.getRuntime();
        IRubyObject result = recv.callMethod(runtime.getCurrentContext(), "<=>", other);

        if (result.isNil()) {
            cmperr(recv, other);
        }

        return RubyBoolean.newBoolean(runtime, cmpint(result, recv, other) >= 0);
    }

    /** cmp_lt
     * 
     */
    public static RubyBoolean op_lt(IRubyObject recv, IRubyObject other) {
        final IRuby runtime = recv.getRuntime();
        IRubyObject result = recv.callMethod(runtime.getCurrentContext(), "<=>", other);

        if (result.isNil()) {
            cmperr(recv, other);
        }

        return RubyBoolean.newBoolean(runtime, cmpint(result, recv, other) < 0);
    }

    /** cmp_le
     * 
     */
    public static RubyBoolean op_le(IRubyObject recv, IRubyObject other) {
        final IRuby runtime = recv.getRuntime();
        IRubyObject result = recv.callMethod(runtime.getCurrentContext(), "<=>", other);

        if (result.isNil()) {
            cmperr(recv, other);
        }

        return RubyBoolean.newBoolean(runtime, cmpint(result, recv, other) <= 0);
    }

    /** cmp_between
     * 
     */
    public static RubyBoolean between_p(IRubyObject recv, IRubyObject first, IRubyObject second) {

        return recv.getRuntime().newBoolean(op_lt(recv, first).isFalse() && op_gt(recv, second).isFalse());
    }
}
