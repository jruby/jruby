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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.common.RubyWarnings;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.concurrent.Callable;

import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.invokedynamic.MethodNames.OP_CMP;

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
        if (val instanceof RubyFixnum) {
            final int asInt = RubyNumeric.fix2int((RubyFixnum) val);

            if (asInt > 0) {
                return 1;
            }

            if (asInt < 0) {
                return -1;
            }

            return 0;
        }
        if (val instanceof RubyBignum) return ((RubyBignum) val).getValue().signum() == -1 ? -1 : 1;

        RubyFixnum zero = RubyFixnum.zero(context.runtime);
        
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

    /** rb_invcmp
     *
     */
    public static IRubyObject invcmp(final ThreadContext context, final IRubyObject recv, final IRubyObject other) {
        final Ruby runtime = context.runtime;
        IRubyObject result = runtime.execRecursiveOuter(new Ruby.RecursiveFunction() {
            @Override
            public IRubyObject call(IRubyObject obj, boolean recur) {
                if (recur || !other.respondsTo("<=>")) return context.runtime.getNil();
                return invokedynamic(context, other, OP_CMP, recv);
            }
        }, recv);

        if (result.isNil()) return result;
        return RubyFixnum.newFixnum(runtime, -cmpint(context, result, recv, other));
    }

    /*  ================
     *  Module Methods
     *  ================ 
     */

    /** cmp_equal (cmp_eq inlined here)
     * 
     */
    public static IRubyObject op_equal(ThreadContext context, IRubyObject recv, IRubyObject other) {
        return op_equal19(context, recv, other);
    }

    @JRubyMethod(name = "==", required = 1)
    public static IRubyObject op_equal19(ThreadContext context, IRubyObject recv, IRubyObject other) {
        return callCmpMethod(context, recv, other, context.runtime.getFalse());
    }

    private static IRubyObject callCmpMethod(ThreadContext context, IRubyObject recv, IRubyObject other, IRubyObject returnValueOnError) {
        Ruby runtime = context.runtime;
        
        if (recv == other) return runtime.getTrue();

        IRubyObject savedError = runtime.getGlobalVariables().get("$!");

        try {
            IRubyObject result = invokedynamic(context, recv, OP_CMP, other);

            // This is only to prevent throwing exceptions by cmperr - it has poor performance
            if (result.isNil()) {
                return returnValueOnError;
            }

            return RubyBoolean.newBoolean(runtime, cmpint(context, result, recv, other) == 0);
        } catch (RaiseException e) {
            if (e.getException().kind_of_p(context, runtime.getStandardError()).isTrue()) {
                cmpFailed(context);
                // clear error info resulting from failure to compare (JRUBY-3292)
                runtime.getGlobalVariables().set("$!", savedError);
                return returnValueOnError;
            } else {
                throw e;
            }
        }
    }

    private static void cmpFailed(ThreadContext context) {
        RubyWarnings warnings = context.runtime.getWarnings();

        warnings.warn("Comparable#== will no more rescue exceptions of #<=> in the next release.");
        warnings.warn("Return nil in #<=> if the comparison is inappropriate or avoid such comparison.");
    }

    /** cmp_gt
     * 
     */
    // <=> may return nil in many circumstances, e.g. 3 <=> NaN        
    @JRubyMethod(name = ">", required = 1)
    public static RubyBoolean op_gt(ThreadContext context, IRubyObject recv, IRubyObject other) {
        IRubyObject result = invokedynamic(context, recv, OP_CMP, other);
        
        if (result.isNil()) cmperr(recv, other);

        return RubyBoolean.newBoolean(context.runtime, cmpint(context, result, recv, other) > 0);
    }

    /** cmp_ge
     * 
     */
    @JRubyMethod(name = ">=", required = 1)
    public static RubyBoolean op_ge(ThreadContext context, IRubyObject recv, IRubyObject other) {
        IRubyObject result = invokedynamic(context, recv, OP_CMP, other);
        
        if (result.isNil()) cmperr(recv, other);

        return RubyBoolean.newBoolean(context.runtime, cmpint(context, result, recv, other) >= 0);
    }

    /** cmp_lt
     * 
     */
    @JRubyMethod(name = "<", required = 1)
    public static RubyBoolean op_lt(ThreadContext context, IRubyObject recv, IRubyObject other) {
        IRubyObject result = invokedynamic(context, recv, OP_CMP, other);

        if (result.isNil()) cmperr(recv, other);

        return RubyBoolean.newBoolean(context.runtime, cmpint(context, result, recv, other) < 0);
    }

    /** cmp_le
     * 
     */
    @JRubyMethod(name = "<=", required = 1)
    public static RubyBoolean op_le(ThreadContext context, IRubyObject recv, IRubyObject other) {
        IRubyObject result = invokedynamic(context, recv, OP_CMP, other);

        if (result.isNil()) cmperr(recv, other);

        return RubyBoolean.newBoolean(context.runtime, cmpint(context, result, recv, other) <= 0);
    }

    /** cmp_between
     * 
     */
    @JRubyMethod(name = "between?", required = 2)
    public static RubyBoolean between_p(ThreadContext context, IRubyObject recv, IRubyObject first, IRubyObject second) {
        return context.runtime.newBoolean(op_lt(context, recv, first).isFalse() && op_gt(context, recv, second).isFalse());
    }
}
