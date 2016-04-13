/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.numeric;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

import java.util.Locale;

@CoreClass(name = "Float")
public abstract class FloatNodes {

    @CoreMethod(names = "-@")
    public abstract static class NegNode extends CoreMethodArrayArgumentsNode {

        public NegNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public double neg(double value) {
            return -value;
        }

    }

    @CoreMethod(names = "+", required = 1)
    public abstract static class AddNode extends CoreMethodArrayArgumentsNode {

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public double add(double a, long b) {
            return a + b;
        }

        @Specialization
        public double add(double a, double b) {
            return a + b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public double add(double a, DynamicObject b) {
            return a + Layouts.BIGNUM.getValue(b).doubleValue();
        }

        // TODO (pitr 14-Sep-2015): all coerces should be replaced with a CallDispatchHeadNodes to speed up things like `5 + rational`
        @Specialization(guards = "!isRubyBignum(b)")
        public Object addCoerced(VirtualFrame frame, double a, DynamicObject b) {
            return ruby("redo_coerced :+, b", "b", b);
        }
    }

    @CoreMethod(names = "-", required = 1)
    public abstract static class SubNode extends CoreMethodArrayArgumentsNode {

        public SubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public double sub(double a, long b) {
            return a - b;
        }

        @Specialization
        public double sub(double a, double b) {
            return a - b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public double sub(double a, DynamicObject b) {
            return a - Layouts.BIGNUM.getValue(b).doubleValue();
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object subCoerced(VirtualFrame frame, double a, DynamicObject b) {
            return ruby("redo_coerced :-, b", "b", b);
        }

    }

    @CoreMethod(names = "*", required = 1)
    public abstract static class MulNode extends CoreMethodArrayArgumentsNode {

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public double mul(double a, long b) {
            return a * b;
        }

        @Specialization
        public double mul(double a, double b) {
            return a * b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public double mul(double a, DynamicObject b) {
            return a * Layouts.BIGNUM.getValue(b).doubleValue();
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object mulCoerced(VirtualFrame frame, double a, DynamicObject b) {
            return ruby("redo_coerced :*, b", "b", b);
        }

    }

    @CoreMethod(names = "**", required = 1)
    public abstract static class PowNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode complexConvertNode;
        @Child private CallDispatchHeadNode complexPowNode;

        private final ConditionProfile complexProfile = ConditionProfile.createBinaryProfile();

        public PowNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {
                "exponent == cachedExponent",
                "cachedExponent >= 0",
                "cachedExponent < 10" }, limit = "10")
        @ExplodeLoop
        public double powCached(double base, long exponent,
                @Cached("exponent") long cachedExponent) {
            double result = 1.0;
            for (int i = 0; i < cachedExponent; i++) {
                result *= base;
            }
            return result;
        }

        @Specialization(contains = "powCached")
        public double pow(double a, long b) {
            return Math.pow(a, b);
        }

        @Specialization
        public Object pow(VirtualFrame frame, double a, double b) {
            if (complexProfile.profile(a < 0 && b != Math.round(b))) {
                if (complexConvertNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    complexConvertNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
                    complexPowNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
                }

                final Object aComplex = complexConvertNode.call(frame, coreLibrary().getComplexClass(), "convert", null, a, 0);

                return complexPowNode.call(frame, aComplex, "**", null, b);
            } else {
                return Math.pow(a, b);
            }
        }

        @Specialization(guards = "isRubyBignum(b)")
        public double pow(double a, DynamicObject b) {
            return Math.pow(a, Layouts.BIGNUM.getValue(b).doubleValue());
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object powCoerced(VirtualFrame frame, double a, DynamicObject b) {
            return ruby("redo_coerced :**, b", "b", b);
        }

    }

    @CoreMethod(names = { "/", "__slash__" }, required = 1)
    public abstract static class DivNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode redoCoercedNode;

        public DivNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public double div(double a, long b) {
            return a / b;
        }

        @Specialization
        public double div(double a, double b) {
            return a / b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public double div(double a, DynamicObject b) {
            return a / Layouts.BIGNUM.getValue(b).doubleValue();
        }

        @Specialization(guards = {
                "!isInteger(b)",
                "!isLong(b)",
                "!isDouble(b)",
                "!isRubyBignum(b)" })
        public Object div(VirtualFrame frame, double a, Object b) {
            if (redoCoercedNode == null) {
                CompilerDirectives.transferToInterpreter();
                redoCoercedNode = insert(DispatchHeadNodeFactory.createMethodCallOnSelf(getContext()));
            }

            return redoCoercedNode.call(frame, a, "redo_coerced", null, getSymbol("/"), b);
        }

    }

    @CoreMethod(names = "%", required = 1)
    public abstract static class ModNode extends CoreMethodArrayArgumentsNode {

        private final ConditionProfile lessThanZeroProfile = ConditionProfile.createBinaryProfile();

        public ModNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public double mod(double a, long b) {
            return mod(a, (double) b);
        }

        @Specialization
        public double mod(double a, double b) {
            if (b == 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().zeroDivisionError(this));
            }

            double result = Math.IEEEremainder(a, b);

            if (lessThanZeroProfile.profile(b * result < 0)) {
                result += b;
            }

            return result;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public double mod(double a, DynamicObject b) {
            return mod(a, Layouts.BIGNUM.getValue(b).doubleValue());
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object modCoerced(VirtualFrame frame, double a, DynamicObject b) {
            return ruby("redo_coerced :mod, b", "b", b);
        }

    }

    @CoreMethod(names = "divmod", required = 1)
    public abstract static class DivModNode extends CoreMethodArrayArgumentsNode {

        @Child private GeneralDivModNode divModNode;

        public DivModNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            divModNode = new GeneralDivModNode(context, sourceSection);
        }

        @Specialization
        public DynamicObject divMod(double a, long b) {
            return divModNode.execute(a, b);
        }

        @Specialization
        public DynamicObject divMod(double a, double b) {
            return divModNode.execute(a, b);
        }

        @Specialization(guards = "isRubyBignum(b)")
        public DynamicObject divMod(double a, DynamicObject b) {
            return divModNode.execute(a, Layouts.BIGNUM.getValue(b));
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object divModCoerced(VirtualFrame frame, double a, DynamicObject b) {
            return ruby("redo_coerced :divmod, b", "b", b);
        }

    }

    @CoreMethod(names = "<", required = 1)
    public abstract static class LessNode extends CoreMethodArrayArgumentsNode {

        public LessNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean less(double a, long b) {
            return a < b;
        }

        @Specialization
        public boolean less(double a, double b) {
            return a < b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public boolean lessBignum(double a, DynamicObject b) {
            return a < Layouts.BIGNUM.getValue(b).doubleValue();
        }

        @Specialization(guards = {
                "!isRubyBignum(b)",
                "!isInteger(b)",
                "!isLong(b)",
                "!isDouble(b)" })
        public Object lessCoerced(VirtualFrame frame, double a, Object b) {
            return ruby("b, a = math_coerce other, :compare_error; a < b", "other", b);
        }
    }

    @CoreMethod(names = "<=", required = 1)
    public abstract static class LessEqualNode extends CoreMethodArrayArgumentsNode {

        public LessEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean lessEqual(double a, long b) {
            return a <= b;
        }

        @Specialization
        public boolean lessEqual(double a, double b) {
            return a <= b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public boolean lessEqual(double a, DynamicObject b) {
            return a <= Layouts.BIGNUM.getValue(b).doubleValue();
        }

        @Specialization(guards = {
                "!isRubyBignum(b)",
                "!isInteger(b)",
                "!isLong(b)",
                "!isDouble(b)" })
        public Object lessEqualCoerced(VirtualFrame frame, double a, Object b) {
            return ruby("b, a = math_coerce other, :compare_error; a <= b", "other", b);
        }
    }

    @CoreMethod(names = "eql?", required = 1)
    public abstract static class EqlNode extends CoreMethodArrayArgumentsNode {

        public EqlNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean eql(double a, double b) {
            return a == b;
        }

        @Specialization(guards = { "!isDouble(b)" })
        public boolean eqlGeneral(double a, Object b) {
            return false;
        }
    }

    @CoreMethod(names = { "==", "===" }, required = 1)
    public abstract static class EqualNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode fallbackCallNode;

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean equal(double a, long b) {
            return a == b;
        }

        @Specialization
        public boolean equal(double a, double b) {
            return a == b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public boolean equal(double a, DynamicObject b) {
            return a == Layouts.BIGNUM.getValue(b).doubleValue();
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object equal(VirtualFrame frame, double a, DynamicObject b) {
            if (fallbackCallNode == null) {
                CompilerDirectives.transferToInterpreter();
                fallbackCallNode = insert(DispatchHeadNodeFactory.createMethodCallOnSelf(getContext()));
            }

            return fallbackCallNode.call(frame, a, "equal_fallback", null, b);
        }
    }

    @CoreMethod(names = "<=>", required = 1)
    public abstract static class CompareNode extends CoreMethodArrayArgumentsNode {

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNaN(a)")
        public DynamicObject compareFirstNaN(double a, Object b) {
            return nil();
        }

        @Specialization(guards = "isNaN(b)")
        public DynamicObject compareSecondNaN(Object a, double b) {
            return nil();
        }

        @Specialization(guards = { "!isNaN(a)" })
        public int compare(double a, long b) {
            return Double.compare(a, b);
        }

        @Specialization(guards = { "isInfinity(a)", "isRubyBignum(b)" })
        public int compareInfinity(double a, DynamicObject b) {
            if (a < 0) {
                return -1;
            } else {
                return +1;
            }
        }

        @Specialization(guards = { "!isNaN(a)", "!isInfinity(a)", "isRubyBignum(b)" })
        public int compareBignum(double a, DynamicObject b) {
            return Double.compare(a, Layouts.BIGNUM.getValue(b).doubleValue());
        }

        @Specialization(guards = { "!isNaN(a)", "!isNaN(b)" })
        public int compare(double a, double b) {
            return Double.compare(a, b);
        }

        @Specialization(guards = { "!isNaN(a)", "!isRubyBignum(b)" })
        public DynamicObject compare(double a, DynamicObject b) {
            return nil();
        }

    }

    @CoreMethod(names = ">=", required = 1)
    public abstract static class GreaterEqualNode extends CoreMethodArrayArgumentsNode {

        public GreaterEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean greaterEqual(double a, long b) {
            return a >= b;
        }

        @Specialization
        public boolean greaterEqual(double a, double b) {
            return a >= b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public boolean greaterEqual(double a, DynamicObject b) {
            return a >= Layouts.BIGNUM.getValue(b).doubleValue();
        }

        @Specialization(guards = {
                "!isRubyBignum(b)",
                "!isInteger(b)",
                "!isLong(b)",
                "!isDouble(b)" })
        public Object greaterEqualCoerced(VirtualFrame frame, double a, Object b) {
            return ruby("b, a = math_coerce other, :compare_error; a >= b", "other", b);
        }

    }

    @CoreMethod(names = ">", required = 1)
    public abstract static class GreaterNode extends CoreMethodArrayArgumentsNode {

        public GreaterNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean greater(double a, long b) {
            return a > b;
        }

        @Specialization
        public boolean greater(double a, double b) {
            return a > b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public boolean greater(double a, DynamicObject b) {
            return a > Layouts.BIGNUM.getValue(b).doubleValue();
        }

        @Specialization(guards = {
                "!isRubyBignum(b)",
                "!isInteger(b)",
                "!isLong(b)",
                "!isDouble(b)" })
        public Object greaterCoerced(VirtualFrame frame, double a, Object b) {
            return ruby("b, a = math_coerce(other, :compare_error); a > b", "other", b);
        }
    }

    @CoreMethod(names = { "abs", "magnitude" })
    public abstract static class AbsNode extends CoreMethodArrayArgumentsNode {

        public AbsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public double abs(double n) {
            return Math.abs(n);
        }

    }

    @CoreMethod(names = "ceil")
    public abstract static class CeilNode extends CoreMethodArrayArgumentsNode {

        @Child private FixnumOrBignumNode fixnumOrBignum;

        public CeilNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fixnumOrBignum = new FixnumOrBignumNode(context, sourceSection);
        }

        @Specialization
        public Object ceil(double n) {
            return fixnumOrBignum.fixnumOrBignum(Math.ceil(n));
        }

    }

    @CoreMethod(names = "floor")
    public abstract static class FloorNode extends CoreMethodArrayArgumentsNode {

        @Child private FixnumOrBignumNode fixnumOrBignum;

        public FloorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fixnumOrBignum = new FixnumOrBignumNode(context, sourceSection);
        }

        @Specialization
        public Object floor(double n) {
            return fixnumOrBignum.fixnumOrBignum(Math.floor(n));
        }

    }

    @CoreMethod(names = "infinite?")
    public abstract static class InfiniteNode extends CoreMethodArrayArgumentsNode {

        public InfiniteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object infinite(double value) {
            if (Double.isInfinite(value)) {
                if (value < 0) {
                    return -1;
                } else {
                    return 1;
                }
            } else {
                return nil();
            }
        }

    }

    @CoreMethod(names = "nan?")
    public abstract static class NaNNode extends CoreMethodArrayArgumentsNode {

        public NaNNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean nan(double value) {
            return Double.isNaN(value);
        }

    }

    @CoreMethod(names = "round", optional = 1)
    public abstract static class RoundNode extends CoreMethodArrayArgumentsNode {

        @Child private FixnumOrBignumNode fixnumOrBignum;

        public RoundNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fixnumOrBignum = new FixnumOrBignumNode(context, sourceSection);
        }

        @Specialization(guards = "doubleInLongRange(n)")
        public long roundFittingLong(double n, NotProvided ndigits,
                                     @Cached("createBinaryProfile()") ConditionProfile positiveProfile) {
            long l = (long) n;
            if (positiveProfile.profile(n >= 0.0)) {
                if (n - l >= 0.5) {
                    l++;
                }
                return l;
            } else {
                if (l - n >= 0.5) {
                    l--;
                }
                return l;
            }
        }

        protected boolean doubleInLongRange(double n) {
            return Long.MIN_VALUE < n && n < Long.MAX_VALUE;
        }

        @Specialization
        public Object round(double n, NotProvided ndigits,
                            @Cached("createBinaryProfile()") ConditionProfile positiveProfile) {
            // Algorithm copied from JRuby - not shared as we want to branch profile it

            if (Double.isInfinite(n)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().floatDomainError("Infinity", this));
            }

            if (Double.isNaN(n)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().floatDomainError("NaN", this));
            }

            double f = n;

            if (positiveProfile.profile(f >= 0.0)) {
                f = Math.floor(f);

                if (n - f >= 0.5) {
                    f += 1.0;
                }
            } else {
                f = Math.ceil(f);

                if (f - n >= 0.5) {
                    f -= 1.0;
                }
            }

            return fixnumOrBignum.fixnumOrBignum(f);
        }

        @Specialization(guards = "wasProvided(ndigits)")
        public Object round(VirtualFrame frame, double n, Object ndigits) {
            return ruby("round_internal(ndigits)", "ndigits", ndigits);
        }

    }

    @CoreMethod(names = { "to_i", "to_int", "truncate" })
    public abstract static class ToINode extends CoreMethodArrayArgumentsNode {

        @Child private FixnumOrBignumNode fixnumOrBignum;

        public ToINode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fixnumOrBignum = new FixnumOrBignumNode(context, sourceSection);
        }

        public abstract Object executeToI(VirtualFrame frame, double value);

        @Specialization
        Object toI(double value) {
            if (Double.isInfinite(value)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().floatDomainError("Infinity", this));
            }

            if (Double.isNaN(value)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().floatDomainError("NaN", this));
            }

            return fixnumOrBignum.fixnumOrBignum(value);
        }

    }

    @CoreMethod(names = "to_f")
    public abstract static class ToFNode extends CoreMethodArrayArgumentsNode {

        public ToFNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public double toF(double value) {
            return value;
        }

    }

    @CoreMethod(names = { "to_s", "inspect" })
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject toS(double value) {
            if (Double.isInfinite(value) || Double.isNaN(value)) {
                return create7BitString(Double.toString(value), USASCIIEncoding.INSTANCE);
            }

            String str = String.format(Locale.ENGLISH, "%.15g", value);

            // If no dot, add one to show it's a floating point number
            if (str.indexOf('.') == -1) {
                assert str.indexOf('e') == -1;
                str += ".0";
            }

            final int dot = str.indexOf('.');
            assert dot != -1;

            final int e = str.indexOf('e');
            final boolean hasE = e != -1;

            // Remove trailing zeroes, but keep at least one after the dot
            final int start = hasE ? e : str.length();
            int i = start - 1; // last digit we keep, inclusive
            while (i > dot + 1 && str.charAt(i) == '0') {
                i--;
            }

            final String formatted = str.substring(0, i + 1) + str.substring(start, str.length());

            return create7BitString(formatted, USASCIIEncoding.INSTANCE);
        }

    }

}
