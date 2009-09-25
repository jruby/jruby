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

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/** Implementation of the Comparable module.
 *
 */
@JRubyModule(name="Comparable")
public class RubyComparable {
    public static RubyModule createComparable(Ruby runtime) {
        RubyModule comparableModule = runtime.defineModule("Comparable");
        runtime.setComparable(comparableModule);
        
        comparableModule.defineAnnotatedMethods(RubyComparable.class);

        return comparableModule;
    }

    /*  ================
     *  Utility Methods
     *  ================ 
     */

    /** rb_cmpint
     * 
     */
    public static int cmpint(ThreadContext context, IRubyObject val, IRubyObject a, IRubyObject b) {
        if (val.isNil()) cmperr(a, b);
        if (val instanceof RubyFixnum) return RubyNumeric.fix2int((RubyFixnum) val);
        if (val instanceof RubyBignum) return ((RubyBignum) val).getValue().signum() == -1 ? 1 : -1;

        RubyFixnum zero = RubyFixnum.zero(context.getRuntime());
        
        if (val.callMethod(context, ">", zero).isTrue()) return 1;
        if (val.callMethod(context, "<", zero).isTrue()) return -1;

        return 0;
    }

    /** rb_cmperr
     * 
     */
    public static IRubyObject cmperr(IRubyObject recv, IRubyObject other) {
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
    @JRubyMethod(name = "==", required = 1, compat = CompatVersion.RUBY1_8)
    public static IRubyObject op_equal(ThreadContext context, IRubyObject recv, IRubyObject other) {
        return callCmpMethod(context, recv, other, context.getRuntime().getNil());
    }

    @JRubyMethod(name = "==", required = 1, compat = CompatVersion.RUBY1_9)
    public static IRubyObject op_equal19(ThreadContext context, IRubyObject recv, IRubyObject other) {
        return callCmpMethod(context, recv, other, context.getRuntime().newBoolean(false));
    }

    private static IRubyObject callCmpMethod(ThreadContext context, IRubyObject recv, IRubyObject other, IRubyObject returnValueOnError) {
        Ruby runtime = context.getRuntime();
        
        if (recv == other) return runtime.getTrue();

        try {
            IRubyObject result = recv.callMethod(context, "<=>", other);

            return RubyBoolean.newBoolean(runtime, cmpint(context, result, recv, other) == 0);
        } catch (RaiseException e) {
            if (e.getException().kind_of_p(context, runtime.getStandardError()).isTrue()) {
                return returnValueOnError;
            } else {
                throw e;
            }
        }
    }

    /** cmp_gt
     * 
     */
    // <=> may return nil in many circumstances, e.g. 3 <=> NaN        
    @JRubyMethod(name = ">", required = 1)
    public static RubyBoolean op_gt(ThreadContext context, IRubyObject recv, IRubyObject other) {
        IRubyObject result = recv.callMethod(context, "<=>", other);
        
        if (result.isNil()) cmperr(recv, other);

        return RubyBoolean.newBoolean(context.getRuntime(), cmpint(context, result, recv, other) > 0);
    }

    /** cmp_ge
     * 
     */
    @JRubyMethod(name = ">=", required = 1)
    public static RubyBoolean op_ge(ThreadContext context, IRubyObject recv, IRubyObject other) {
        IRubyObject result = recv.callMethod(context, "<=>", other);
        
        if (result.isNil()) cmperr(recv, other);

        return RubyBoolean.newBoolean(context.getRuntime(), cmpint(context, result, recv, other) >= 0);
    }

    /** cmp_lt
     * 
     */
    @JRubyMethod(name = "<", required = 1)
    public static RubyBoolean op_lt(ThreadContext context, IRubyObject recv, IRubyObject other) {
        IRubyObject result = recv.callMethod(context, "<=>", other);

        if (result.isNil()) cmperr(recv, other);

        return RubyBoolean.newBoolean(context.getRuntime(), cmpint(context, result, recv, other) < 0);
    }

    /** cmp_le
     * 
     */
    @JRubyMethod(name = "<=", required = 1)
    public static RubyBoolean op_le(ThreadContext context, IRubyObject recv, IRubyObject other) {
        IRubyObject result = recv.callMethod(context, "<=>", other);

        if (result.isNil()) cmperr(recv, other);

        return RubyBoolean.newBoolean(context.getRuntime(), cmpint(context, result, recv, other) <= 0);
    }

    /** cmp_between
     * 
     */
    @JRubyMethod(name = "between?", required = 2)
    public static RubyBoolean between_p(ThreadContext context, IRubyObject recv, IRubyObject first, IRubyObject second) {
        return context.getRuntime().newBoolean(op_lt(context, recv, first).isFalse() && op_gt(context, recv, second).isFalse());
    }
}
