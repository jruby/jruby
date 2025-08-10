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
import org.jruby.runtime.CallSite;
import org.jruby.runtime.JavaSites.ComparableSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Convert.*;
import static org.jruby.api.Define.defineModule;
import static org.jruby.api.Error.argumentError;

/** Implementation of the Comparable module.
 *
 */
@JRubyModule(name="Comparable")
public class RubyComparable {
    public static RubyModule createComparable(ThreadContext context) {
        return defineModule(context, "Comparable").defineMethods(context, RubyComparable.class);
    }

    /*  ================
     *  Utility Methods
     *  ================
     */

    /** rb_cmpint
     *
     */
    public static int cmpint(ThreadContext context, CallSite op_gt, CallSite op_lt, IRubyObject val, IRubyObject a, IRubyObject b) {
        if (val == context.nil) cmperr(context, a, b);
        if (val instanceof RubyFixnum fixnum) return Integer.compare(toInt(context, fixnum), 0);
        if (val instanceof RubyBignum bignum) return bignum.signum(context) == -1 ? -1 : 1;

        var zero = RubyFixnum.zero(context.runtime);

        if (op_gt.call(context, val, val, zero).isTrue()) return 1;
        if (op_lt.call(context, val, val, zero).isTrue()) return -1;

        return 0;
    }

    public static int cmpint(ThreadContext context, IRubyObject val, IRubyObject a, IRubyObject b) {
        ComparableSites sites = sites(context);
        return cmpint(context, sites.op_gt, sites.op_lt, val, a, b);
    }

    public static int cmpAndCmpint(ThreadContext context, IRubyObject a, IRubyObject b) {
        IRubyObject cmpResult = sites(context).op_cmp.call(context, a, a, b);
        return cmpint(context, cmpResult, a, b);
    }

    public static int cmpAndCmpint(ThreadContext context, CallSite op_cmp, CallSite op_gt, CallSite op_lt, IRubyObject a, IRubyObject b) {
        IRubyObject cmpResult = op_cmp.call(context, a, a, b);
        return cmpint(context, op_gt, op_lt, cmpResult, a, b);
    }

    @Deprecated
    public static IRubyObject cmperr(IRubyObject recv, IRubyObject other) {
        return cmperr(((RubyBasicObject) recv).getCurrentContext(), recv, other);
    }

    /** rb_cmperr
     *
     */
    public static IRubyObject cmperr(ThreadContext context, IRubyObject recv, IRubyObject other) {
        IRubyObject target = other.isImmediate() || !(other.isNil() || other.isTrue() || other == context.fals) ?
                other.inspect(context) : other.getType();

        throw argumentError(context, "comparison of " + recv.getType() + " with " + target + " failed");
    }

    /** rb_invcmp
     *
     */
    public static IRubyObject invcmp(final ThreadContext context, final IRubyObject recv, final IRubyObject other) {
        return invcmp(context, RubyComparable::invcmpRecursive, recv, other);
    }

    private static IRubyObject invcmpRecursive(ThreadContext context, IRubyObject recv, IRubyObject other, boolean recur) {
        if (recur || !sites(context).respond_to_op_cmp.respondsTo(context, other, other)) return context.nil;
        return sites(context).op_cmp.call(context, other, other, recv);
    }

    /** rb_invcmp
     *
     */
    public static IRubyObject invcmp(final ThreadContext context, ThreadContext.RecursiveFunctionEx<IRubyObject> func, IRubyObject recv, IRubyObject other) {
        var result = context.safeRecurse(func, recv, other, "<=>", true);
        return result.isNil() ? context.nil : asFixnum(context, -cmpint(context, result, recv, other));
    }

    /*  ================
     *  Module Methods
     *  ================
     */

    /** cmp_equal (cmp_eq inlined here)
     *
     */
    @JRubyMethod(name = "==")
    public static IRubyObject op_equal(ThreadContext context, IRubyObject recv, IRubyObject other) {
        return callCmpMethod(context, recv, other, context.fals);
    }

    private static IRubyObject callCmpMethod(final ThreadContext context, final IRubyObject recv, final IRubyObject other, IRubyObject returnValueOnError) {
        if (recv == other) return context.tru;

        var result = context.safeRecurse(
                (ctx, obj, self, recur) -> recur ? ctx.nil : sites(ctx).op_cmp.call(ctx, self, self, obj),
                other, recv, "<=>", true);

        // This is only to prevent throwing exceptions by cmperr - it has poor performance
        if (result.isNil()) return returnValueOnError;

        return asBoolean(context, cmpint(context, result, recv, other) == 0);
    }

    /** cmp_gt
     *
     */
    // <=> may return nil in many circumstances, e.g. 3 <=> NaN
    @JRubyMethod(name = ">")
    public static RubyBoolean op_gt(ThreadContext context, IRubyObject recv, IRubyObject other) {
        var result = sites(context).op_cmp.call(context, recv, recv, other);

        if (result.isNil()) cmperr(context, recv, other);

        return asBoolean(context, cmpint(context, result, recv, other) > 0);
    }

    /** cmp_ge
     *
     */
    @JRubyMethod(name = ">=")
    public static RubyBoolean op_ge(ThreadContext context, IRubyObject recv, IRubyObject other) {
        var result = sites(context).op_cmp.call(context, recv, recv, other);

        if (result.isNil()) cmperr(context, recv, other);

        return asBoolean(context, cmpint(context, result, recv, other) >= 0);
    }

    /** cmp_lt
     *
     */
    @JRubyMethod(name = "<")
    public static RubyBoolean op_lt(ThreadContext context, IRubyObject recv, IRubyObject other) {
        return op_lt(context, sites(context).op_cmp, recv, other);
    }

    public static RubyBoolean op_lt(ThreadContext context, CallSite cmp, IRubyObject recv, IRubyObject other) {
        var result = cmp.call(context, recv, recv, other);

        if (result.isNil()) cmperr(context, recv, other);

        return asBoolean(context, cmpint(context, result, recv, other) < 0);
    }

    /** cmp_le
     *
     */
    @JRubyMethod(name = "<=")
    public static RubyBoolean op_le(ThreadContext context, IRubyObject recv, IRubyObject other) {
        var result = sites(context).op_cmp.call(context, recv, recv, other);

        if (result.isNil()) cmperr(context, recv, other);

        return asBoolean(context, cmpint(context, result, recv, other) <= 0);
    }

    /** cmp_between
     *
     */
    @JRubyMethod(name = "between?")
    public static RubyBoolean between_p(ThreadContext context, IRubyObject recv, IRubyObject first, IRubyObject second) {
        return asBoolean(context, op_lt(context, recv, first).isFalse() && op_gt(context, recv, second).isFalse());
    }

    @JRubyMethod(name = "clamp")
    public static IRubyObject clamp(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        var range = castAsRange(context, arg);
        var min = range.begin(context);
        var max = range.end(context);

        if (!max.isNil() && range.isExcludeEnd()) throw argumentError(context, "cannot clamp with an exclusive range");

        return clamp(context, recv, min, max);
    }

    @JRubyMethod(name = "clamp")
    public static IRubyObject clamp(ThreadContext context, IRubyObject recv, IRubyObject min, IRubyObject max) {
        ComparableSites sites = sites(context);
        CallSite op_gt = sites.op_gt;
        CallSite op_lt = sites.op_lt;
        CallSite op_cmp = sites.op_cmp;

        if (!min.isNil() && !max.isNil() && cmpAndCmpint(context, op_cmp, op_gt, op_lt, min, max) > 0) {
            throw argumentError(context, "min argument must be less than or equal to max argument");
        }

        if (!min.isNil()) {
            int c = cmpAndCmpint(context, op_cmp, op_gt, op_lt, recv, min);
            if (c == 0) return recv;
            if (c < 0) return min;
        }

        if (!max.isNil()) {
            int c = cmpAndCmpint(context, op_cmp, op_gt, op_lt, recv, max);
            if (c > 0) return max;
        }

        return recv;
    }

    private static ComparableSites sites(ThreadContext context) {
        return context.sites.Comparable;
    }

}
