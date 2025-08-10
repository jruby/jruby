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

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaSites.FiberSites;
import org.jruby.runtime.JavaSites.NumericSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.util.ByteList;
import org.jruby.util.Numeric;

import static org.jruby.RubyEnumerator.ARGS;
import static org.jruby.RubyEnumerator.FEEDVALUE;
import static org.jruby.RubyEnumerator.GENERATOR;
import static org.jruby.RubyEnumerator.LOOKAHEAD;
import static org.jruby.RubyEnumerator.METHOD;
import static org.jruby.RubyEnumerator.OBJECT;
import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.RubyEnumerator.enumeratorizeWithSize;

import static org.jruby.RubyNumeric.fixable;
import static org.jruby.RubyNumeric.floatStep;
import static org.jruby.RubyNumeric.floatStepSize;

import static org.jruby.api.Access.enumeratorClass;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.asFloat;
import static org.jruby.api.Convert.toDouble;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Convert.toInteger;
import static org.jruby.api.Convert.toLong;
import static org.jruby.api.Create.allocArray;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newEmptyArray;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.rangeError;
import static org.jruby.runtime.Helpers.hashEnd;
import static org.jruby.runtime.Helpers.hashStart;
import static org.jruby.runtime.Helpers.murmurCombine;
import static org.jruby.runtime.Helpers.safeHash;
import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;

/**
 * Implements Enumerator::ArithmeticSequence
 */
@JRubyClass(name = "Enumerator::ArithmeticSequence", parent = "Enumerator")
public class RubyArithmeticSequence extends RubyObject {
    private IRubyObject begin;
    private IRubyObject end;
    private IRubyObject step;
    private IRubyObject excludeEnd;
    private IRubyObject generatedBy;
    private String method;
    private IRubyObject [] args;

    public static RubyClass createArithmeticSequenceClass(ThreadContext context, RubyClass Enumerator, RubyModule Enumerable) {
        return Enumerator.defineClassUnder(context, "ArithmeticSequence", Enumerator, NOT_ALLOCATABLE_ALLOCATOR).
                include(context, Enumerable).
                defineMethods(context, RubyArithmeticSequence.class).
                tap(m -> m.getMetaClass().undefMethods(context, "new"));
    }

    public RubyArithmeticSequence(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    public RubyArithmeticSequence(Ruby runtime, RubyClass klass, IRubyObject generatedBy, String method, IRubyObject [] args, IRubyObject begin, IRubyObject end, IRubyObject step, IRubyObject excludeEnd) {
        super(runtime, klass);
        this.begin = begin;
        this.end = end;
        this.step = step;
        this.excludeEnd = excludeEnd;
        this.generatedBy = generatedBy;
        this.method = method;
        this.args = args;
        ThreadContext context = runtime.getCurrentContext();
        setInstanceVariable(OBJECT, generatedBy);
        setInstanceVariable(METHOD, newString(context, method));
        setInstanceVariable(ARGS, args != null ? RubyArray.newArrayMayCopy(runtime, args) : newEmptyArray(context));
        setInstanceVariable(GENERATOR, context.nil);
        setInstanceVariable(LOOKAHEAD, allocArray(context, 4));
        setInstanceVariable(FEEDVALUE, new RubyEnumerator.FeedValue(context));
    }

    public static RubyArithmeticSequence newArithmeticSequence(ThreadContext context, IRubyObject generatedBy, String method, IRubyObject [] args, IRubyObject begin, IRubyObject end, IRubyObject step, IRubyObject excludeEnd) {
        return new RubyArithmeticSequence(context.runtime, context.runtime.getArithmeticSequence(), generatedBy, method, args, begin, end, step, excludeEnd);
    }

    private static NumericSites sites(ThreadContext context) {
        return context.sites.Numeric;
    }

    // arith_seq_each
    @JRubyMethod
    public IRubyObject each(ThreadContext context, Block block) {
        IRubyObject c = begin;
        IRubyObject e = end;
        IRubyObject s = step;
        IRubyObject len_1, last;

        if (!block.isGiven()) {
            return this;
        }

        if (!(step instanceof RubyComplex) && floatStep(context, c, e, s, excludeEnd.isTrue(), true, block)) {
            return this;
        }

        if (end.isNil()) {
            while (true) {
                block.yield(context, c);
                c = ((RubyNumeric)c).op_plus(context, s);
            }
        }

        if (Helpers.rbEqual(context, step, asFixnum(context, 0)).isTrue()) {
            while (true) {
                block.yield(context, c);
            }
        }

        len_1 = ((RubyNumeric)((RubyNumeric)e).op_minus(context, c)).idiv(context, s);
        last = ((RubyNumeric)c).op_plus(context, Numeric.f_mul(context, s, len_1));
        if (excludeEnd.isTrue() && Helpers.rbEqual(context, last, e).isTrue()) {
            last = ((RubyNumeric)last).op_minus(context, s);
        }

        CallSite op_ge = sites(context).op_ge;
        if (Numeric.f_negative_p(context, s)) {
            while (RubyNumeric.numFuncall(context, c, op_ge, last).isTrue()) {
                block.yield(context, c);
                c = ((RubyNumeric)c).op_plus(context, s);
            }
        } else {
            while (RubyNumeric.numFuncall(context, last, op_ge, c).isTrue()) {
                block.yield(context, c);
                c = ((RubyNumeric)c).op_plus(context, s);
            }
        }

        return this;
    }

    // arith_seq_first
    @JRubyMethod
    public IRubyObject first(ThreadContext context) {
        return first(context, null);
    }

    // arith_seq_first
    @JRubyMethod
    public IRubyObject first(ThreadContext context, IRubyObject num) {
        IRubyObject b = begin, e = end, s = step;
        RubyArray ary;

        if (num == null) return firstNoNum(context, b, e, s);

        /* TODO: the following code should be extracted as arith_seq_take */
        long n = toLong(context, num);

        if (n < 0) throw argumentError(context, "attempt to take negative size");
        if (n == 0) return newEmptyArray(context);

        boolean x = excludeEnd.isTrue();

        if (b instanceof RubyFixnum bb && e.isNil() && s instanceof RubyFixnum ss) {
            long i = bb.getValue();
            long unit = ss.getValue();
            ary = allocArray(context, n);
            while (n > 0 && fixable(context.runtime, i)) {
                ary.append(context, asFixnum(context, i));
                i += unit;  /* FIXABLE + FIXABLE never overflow; */
                --n;
            }
            if (n > 0) {
                b = asFixnum(context, i);
                while (n > 0) {
                    ary.append(context, b);
                    b = ((RubyInteger)b).op_plus(context, s);
                    --n;
                }
            }

            return ary;
        } else if (b instanceof RubyFixnum bb && e instanceof RubyFixnum ee && s instanceof RubyFixnum ss) {
            long i = bb.getValue();
            long end = ee.getValue();
            long unit = ss.getValue();
            long len;

            if (unit >= 0) {
                if (!x) end += 1;

                len = end - i;
                if (len < 0) len = 0;
                ary = allocArray(context, Math.min(n, len));
                while (n > 0 && i < end) {
                    ary.append(context, asFixnum(context, i));
                    if (i + unit < i) break;
                    i += unit;
                    --n;
                }
            } else {
                if (!x) end -= 1;

                len = i - end;
                if (len < 0) len = 0;
                ary = allocArray(context, Math.min(n, len));
                while (n > 0 && i > end) {
                    ary.append(context, asFixnum(context, i));
                    if (i + unit > i) break;
                    i += unit;
                    --n;
                }
            }

            return ary;
        } else if (b instanceof RubyFloat || e instanceof RubyFloat || s instanceof RubyFloat) {
            /* generate values like ruby_float_step */

            double unit = toDouble(context, s);
            double beg = toDouble(context, b);
            double end = e.isNil() ? (unit < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY) : toDouble(context, e);
            double len = floatStepSize(beg, end, unit, x);
            long i;

            if (n > len) n = (long)len;

            if (Double.isInfinite(unit)) {
                return len > 0 ?
                        newArray(context, asFloat(context, beg)) :
                        newEmptyArray(context);
            } else if (unit == 0) {
                IRubyObject val = asFloat(context, beg);
                ary = allocArray(context, n);
                for (i = 0; i < len; ++i) {
                    ary.append(context, val);
                }
            } else {
                ary = allocArray(context, n);
                for (i = 0; i < n; ++i) {
                    double d = i * unit + beg;
                    if (unit >= 0 ? end < d : d < end) d = end;
                    ary.append(context, asFloat(context, d));
                }
            }

            return ary;
        }

        return Helpers.invokeSuper(context, this, enumeratorClass(context), "first", num, Block.NULL_BLOCK);
    }

    private IRubyObject firstNoNum(ThreadContext context, IRubyObject b, IRubyObject e, IRubyObject s) {
        if (b.isNil()) return context.nil;
        if (!e.isNil()) {
            IRubyObject zero = asFixnum(context, 0);
            CallSite op_cmp = sites(context).op_cmp;
            CallSite op_gt = sites(context).op_gt;
            CallSite op_lt = sites(context).op_lt;
            int r = RubyComparable.cmpint(context, ((RubyNumeric)step).coerceCmp(context, op_cmp, zero), s, zero);
            if (r > 0 && RubyNumeric.numFuncall(context, b, op_gt, e).isTrue()) return context.nil;
            if (r < 0 && RubyNumeric.numFuncall(context, b, op_lt, e).isTrue()) return context.nil;
        }
        return b;
    }

    // arith_seq_eq
    @JRubyMethod(name = {"==", "eql?"})
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if (!(other instanceof RubyArithmeticSequence aseqOther)) return context.fals;

        if (!Helpers.rbEqual(context, this.begin, aseqOther.begin).isTrue()) return context.fals;
        if (!Helpers.rbEqual(context, this.end, aseqOther.end).isTrue()) return context.fals;
        if (!Helpers.rbEqual(context, this.step, aseqOther.step).isTrue()) return context.fals;
        if (!Helpers.rbEqual(context, this.excludeEnd, aseqOther.excludeEnd).isTrue()) return context.fals;

        return context.tru;
    }

    @JRubyMethod(name = "hash")
    public RubyFixnum hash(ThreadContext context) {
        long hash = hashStart(context.runtime, safeHash(context, excludeEnd).getValue());

        hash = murmurCombine(hash, safeHash(context, begin).getValue());
        hash = murmurCombine(hash, safeHash(context, end).getValue());
        hash = murmurCombine(hash, safeHash(context, step).getValue());
        hash = hashEnd(hash);

        return asFixnum(context, hash);
    }

    @JRubyMethod
    public RubyString inspect(ThreadContext context) {
        ByteList str = new ByteList();
        boolean isRange = generatedBy instanceof RubyRange;
        str.append('(');
        if (isRange) {
            str.append('(');
        }
        str.append(RubyObject.inspect(context, generatedBy).getByteList());
        if (isRange) {
            str.append(')');
        }
        str.append('.').append(method.getBytes());
        if (args != null) {
            int argc = args.length;

            if (argc > 0) {
                IRubyObject kwds = context.nil;

                str.append('(');

                if (args[argc - 1] instanceof RubyHash) {
                    boolean allKey = true;
                    IRubyObject [] keys = ((RubyHash)args[argc - 1]).keys().toJavaArray(context);
                    for (IRubyObject key : keys) {
                        if (!(key instanceof RubySymbol)) {
                            allKey = false;
                            break;
                        }
                    }
                    if (allKey) {
                        kwds = args[--argc];
                    }
                }

                int counter = 0;
                while (argc > 0) {
                    str.append(RubyObject.inspect(context, args[counter]).getByteList());
                    str.append(',').append(' ');
                    argc--;
                    counter++;
                }
                
                if (!kwds.isNil()) {
                    IRubyObject [] keys = ((RubyHash)kwds).keys().toJavaArray(context);
                    for (IRubyObject key : keys) {
                        IRubyObject value = ((RubyHash)kwds).fastARef(key);
                        str.append(((RubySymbol)key).getBytes());
                        str.append(':').append(' ');
                        str.append(RubyObject.inspect(context, value).getByteList());
                        str.append(',').append(' ');
                    }
                }

                str = new ByteList(str, 0, str.length() - 2); /* drop the last ", " */
                str.append(')');
            }
        }

        str.append(')');

        return RubyString.newStringLight(context.runtime, str);
    }
    
    @JRubyMethod
    public IRubyObject begin(ThreadContext context) {
        return begin;
    }

    @JRubyMethod
    public IRubyObject end(ThreadContext context) {
        return end;
    }

    @JRubyMethod
    public IRubyObject step(ThreadContext context) {
        return step;
    }

    @JRubyMethod(name = "exclude_end?")
    public IRubyObject exclude_end(ThreadContext context) {
        return excludeEnd;
    }

    // arith_seq_last
    @JRubyMethod
    public IRubyObject last(ThreadContext context) {
        return last(context, null);
    }

    // arith_seq_last
    @JRubyMethod
    public IRubyObject last(ThreadContext context, IRubyObject num) {
        var b = (RubyNumeric) begin;
        if (end.isNil()) throw rangeError(context, "cannot get the last element of endless arithmetic sequence");
        var e = (RubyNumeric) end;
        IRubyObject s = step;

        var len_1 = (RubyNumeric) ((RubyNumeric) e.op_minus(context, b)).idiv(context, s);
        if (Numeric.f_negative_p(context, len_1)) return num == null ? context.nil : newEmptyArray(context);

        var last = (RubyNumeric) b.op_plus(context, Numeric.f_mul(context, s, len_1));
        boolean last_is_adjusted = excludeEnd.isTrue();
        if (last_is_adjusted && Helpers.rbEqual(context, last, e).isTrue()) {
            last = (RubyNumeric) last.op_minus(context, s);
        }

        if (num == null) return last;

        var len = last_is_adjusted ? len_1 : (RubyNumeric) len_1.op_plus(context, asFixnum(context, 1));
        RubyNumeric nv = !(num instanceof RubyInteger numm) ? toInteger(context, num) : numm;

        if (RubyNumeric.numFuncall(context, nv, sites(context).op_gt, len).isTrue()) nv = len;

        long n = toLong(context, nv);
        if (n < 0) throw argumentError(context, "negative array size");

        var ary = allocArray(context, n);
        var i = (RubyNumeric) last.op_minus(context, Numeric.f_mul(context, s, nv));
        while (n > 0) {
            i = (RubyNumeric) i.op_plus(context, s);
            ary.append(context, i);
            --n;
        }

        return ary;
    }

    @JRubyMethod
    public IRubyObject size(ThreadContext context) {
        if (begin instanceof RubyFloat || end instanceof RubyFloat || step instanceof RubyFloat) {
            double ee = end.isNil() ?
                    (Numeric.f_negative_p(context, step) ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY) :
                    toDouble(context, end);

            double n = floatStepSize(toDouble(context, begin), ee, toDouble(context, step), excludeEnd.isTrue());
            if (Double.isInfinite(n)) return asFloat(context, n);
            if (RubyNumeric.posFixable(n)) return asFixnum(context, (long)n);

            return RubyBignum.newBignorm(context.runtime, n);
        }

        if (end.isNil()) return asFloat(context, Double.POSITIVE_INFINITY);

        if (!(step instanceof RubyNumeric)) step = toInteger(context, step);

        if (Helpers.rbEqual(context, step, asFixnum(context, 0)).isTrue()) {
            return asFloat(context, Double.POSITIVE_INFINITY);
        }

        IRubyObject len_1 = ((RubyNumeric)((RubyNumeric)end).op_minus(context, begin)).idiv(context, step);
        if (Numeric.f_negative_p(context, len_1)) return asFixnum(context, 0);

        IRubyObject last = ((RubyNumeric)begin).op_plus(context, Numeric.f_mul(context, step, len_1));
        IRubyObject len = excludeEnd.isTrue() && Helpers.rbEqual(context, last, end).isTrue() ?
                len_1 : ((RubyNumeric)len_1).op_plus(context, asFixnum(context, 1));

        return len;
    }

    /**
     * A size method suitable for lambda method reference implementation of
     * {@link RubyEnumerator.SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see RubyEnumerator.SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject size(ThreadContext context, RubyArithmeticSequence self, IRubyObject[] args) {
        return self.size(context);
    }

    private static FiberSites fiberSites(ThreadContext context) {
        return context.sites.Fiber;
    }

    @JRubyMethod(name = "each_cons")
    public IRubyObject each_cons(ThreadContext context, IRubyObject arg, final Block block) {
        int size = (int) toLong(context, arg);
        if (size <= 0) throw argumentError(context, "invalid size");
        return block.isGiven() ?
                RubyEnumerable.each_consCommon(context, this, size, block) :
                enumeratorize(context.runtime, this, "each_cons", arg);
    }

    @JRubyMethod(name = "each_slice")
    public IRubyObject each_slice(ThreadContext context, IRubyObject arg, final Block block) {
        int size = (int) toLong(context, arg);
        if (size <= 0) throw argumentError(context, "invalid size");

        return block.isGiven() ?
                RubyEnumerable.each_sliceCommon(context, this, size, block) :
                enumeratorizeWithSize(context, this, "each_slice", new IRubyObject[]{arg}, RubyArithmeticSequence::size);
    }

    @JRubyMethod
    public IRubyObject each_with_object(final ThreadContext context, IRubyObject arg, Block block) {
        return block.isGiven() ?
                RubyEnumerable.each_with_objectCommon(context, this, block, arg) :
                enumeratorizeWithSize(context, this, "each_with_object", new IRubyObject[]{arg}, RubyArithmeticSequence::size);
    }

    @JRubyMethod(name = "with_index")
    public IRubyObject with_index(ThreadContext context, final Block block) {
        return with_index(context, context.nil, block);
    }

    @JRubyMethod(name = "with_index")
    public IRubyObject with_index(ThreadContext context, IRubyObject arg, final Block block) {
        final int index = arg.isNil() ? 0 : toInt(context, arg);

        if (!block.isGiven()) {
            return arg.isNil() ?
                    enumeratorizeWithSize(context, this, "with_index", RubyArithmeticSequence::size) :
                    enumeratorizeWithSize(context, this, "with_index", new IRubyObject[]{asFixnum(context, index)}, RubyArithmeticSequence::size);
        }

        return RubyEnumerable.callEach(context, fiberSites(context).each, this, new RubyEnumerable.EachWithIndex(block, index));
    }
}