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

import org.jruby.runtime.callsite.GeCallSite;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.util.ByteList;
import org.jruby.util.Numeric;

import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.RubyEnumerator.SizeFn;

import static org.jruby.RubyNumeric.floatStep;
import static org.jruby.RubyNumeric.floatStepSize;
import static org.jruby.RubyNumeric.dbl2num;
import static org.jruby.RubyNumeric.int2fix;
import static org.jruby.RubyNumeric.num2dbl;

import static org.jruby.runtime.Helpers.hashEnd;
import static org.jruby.runtime.Helpers.hashStart;
import static org.jruby.runtime.Helpers.murmurCombine;
import static org.jruby.runtime.Helpers.safeHash;

/**
 * Implements Enumerator::ArithmeticSequence
 */
@JRubyClass(name = "Enumerator::ArithmeticSequence")
public class RubyArithmeticSequence extends RubyObject {

    private IRubyObject begin;
    private IRubyObject end;
    private IRubyObject step;
    private IRubyObject excludeEnd;
    private IRubyObject generatedBy;
    private String method;
    private IRubyObject [] args;

    public static RubyClass createArithmeticSequenceClass(Ruby runtime, RubyClass enumeratorModule) {
        RubyClass sequencec = runtime.defineClassUnder("ArithmeticSequence", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new RubyArithmeticSequence(runtime, klazz);
            }
        }, enumeratorModule);

        sequencec.includeModule(runtime.getEnumerable());
        sequencec.defineAnnotatedMethods(RubyArithmeticSequence.class);

        RubyClass seqMetaClass = sequencec.getMetaClass();
        seqMetaClass.undefineMethod("new");

        return sequencec;
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
    }

    public static RubyArithmeticSequence newArithmeticSequence(ThreadContext context, IRubyObject generatedBy, String method, IRubyObject [] args, IRubyObject begin, IRubyObject end, IRubyObject step, IRubyObject excludeEnd) {
        return new RubyArithmeticSequence(context.runtime, context.runtime.getArithmeticSequence(), generatedBy, method, args, begin, end, step, excludeEnd);
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
            //FIXME : unreachable statement
            //return this;
        }

        if (Helpers.rbEqual(context, step, int2fix(context.runtime, 0)).isTrue()) {
            while (true) {
                block.yield(context, c);
            }
            //FIXME : unreachable statement
            //return this;
        }

        len_1 = ((RubyNumeric)((RubyNumeric)e).op_minus(context, c)).idiv(context, s);
        last = ((RubyNumeric)c).op_plus(context, Numeric.f_mul(context, s, len_1));
        if (excludeEnd.isTrue() && Helpers.rbEqual(context, last, e).isTrue()) {
            last = ((RubyNumeric)last).op_minus(context, s);
        }

        if (Numeric.f_negative_p(context, s)) {
            while (RubyNumeric.numFuncall(context, c, new GeCallSite(), last).isTrue()) {
                block.yield(context, c);
                c = ((RubyNumeric)c).op_plus(context, s);
            }
        } else {
            while (RubyNumeric.numFuncall(context, last, new GeCallSite(), c).isTrue()) {
                block.yield(context, c);
                c = ((RubyNumeric)c).op_plus(context, s);
            }
        }

        return this;
    }

    // arith_seq_eq
    @JRubyMethod(name = "==")
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if (!(other instanceof RubyArithmeticSequence)) {
            return context.fals;
        }

        RubyArithmeticSequence aseqOther = (RubyArithmeticSequence)other;

        if (!Helpers.rbEqual(context, this.begin, aseqOther.begin).isTrue()) {
            return context.fals;
        }

        if (!Helpers.rbEqual(context, this.end, aseqOther.end).isTrue()) {
            return context.fals;
        }

        if (!Helpers.rbEqual(context, this.step, aseqOther.step).isTrue()) {
            return context.fals;
        }

        if (!Helpers.rbEqual(context, this.excludeEnd, aseqOther.excludeEnd).isTrue()) {
            return context.fals;
        }

        return context.tru;
    }

    @Override
    public RubyFixnum hash() {
        return hash(metaClass.runtime.getCurrentContext());
    }

    @JRubyMethod(name = "hash")
    public RubyFixnum hash(ThreadContext context) {
        Ruby runtime = context.runtime;

        IRubyObject v;

        v = safeHash(context, excludeEnd);
        long hash = hashStart(runtime, v.convertToInteger().getLongValue());

        v = safeHash(context, begin);
        hash = murmurCombine(hash, v.convertToInteger().getLongValue());

        v = safeHash(context, end);
        hash = murmurCombine(hash, v.convertToInteger().getLongValue());

        v = safeHash(context, step);
        hash = murmurCombine(hash, v.convertToInteger().getLongValue());
        hash = hashEnd(hash);

        return runtime.newFixnum(hash);
    }

    @Override
    public final IRubyObject inspect() {
        return inspect(getRuntime().getCurrentContext());
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
                    IRubyObject [] keys = ((RubyHash)args[argc - 1]).keys().toJavaArray();
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

                while (argc > 0) {
                    str.append(RubyObject.inspect(context, args[args.length - argc]).getByteList());
                    str.append(',').append(' ');
                    --argc;
                }
                
                if (!kwds.isNil()) {
                    IRubyObject [] keys = ((RubyHash)kwds).keys().toJavaArray();
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

    @JRubyMethod
    public IRubyObject size(ThreadContext context) {
        Ruby runtime = context.runtime;
        IRubyObject len_1, len, last;
        int x;

        if (begin instanceof RubyFloat || end instanceof RubyFloat || step instanceof RubyFloat) {
            double ee, n;

            if (end.isNil()) {
                if (Numeric.f_negative_p(context, step)) {
                    ee = Double.NEGATIVE_INFINITY;
                } else {
                    ee = Double.POSITIVE_INFINITY;
                }
            } else {
                ee = num2dbl(end);
            }

            n = floatStepSize(num2dbl(begin), ee, num2dbl(step), excludeEnd.isTrue());
            if (Double.isInfinite(n)) {
                return dbl2num(runtime, n);
            }
            if (RubyNumeric.posFixable(n)) {
                return int2fix(runtime, (long)n);
            }

            return RubyBignum.newBignorm(runtime, n);
        }

        if (end.isNil()) {
            return dbl2num(runtime, Double.POSITIVE_INFINITY);
        }

        if(!(step instanceof RubyNumeric)) {
            step = step.convertToInteger();
        }

        if (Helpers.rbEqual(context, step, int2fix(runtime, 0)).isTrue()) {
            return dbl2num(runtime, Double.POSITIVE_INFINITY);
        }

        len_1 = ((RubyNumeric)((RubyNumeric)end).op_minus(context, begin)).idiv(context, step);
        if (Numeric.f_negative_p(context, len_1)) {
            return int2fix(runtime, 0);
        }

        last = ((RubyNumeric)begin).op_plus(context, Numeric.f_mul(context, step, len_1));
        if (excludeEnd.isTrue() && Helpers.rbEqual(context, last, end).isTrue()) {
            len = len_1;
        } else {
            len = ((RubyNumeric)len_1).op_plus(context, int2fix(runtime, 1));
        }

        return len;
    }
}