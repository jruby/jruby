/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.api.utilities.ConditionProfile;

import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;

@CoreClass(name = "Float")
public abstract class FloatNodes {

    @CoreMethod(names = "-@")
    public abstract static class NegNode extends CoreMethodNode {

        public NegNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NegNode(NegNode prev) {
            super(prev);
        }

        @Specialization
        public double neg(double value) {
            return -value;
        }

    }

    @CoreMethod(names = "+", required = 1)
    public abstract static class AddNode extends CoreMethodNode {

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AddNode(AddNode prev) {
            super(prev);
        }

        @Specialization
        public double add(double a, int b) {
            return a + b;
        }

        @Specialization
        public double add(double a, long b) {
            return a + b;
        }

        @Specialization
        public double add(double a, double b) {
            return a + b;
        }

        @Specialization
        public double add(double a, RubyBignum b) {
            return a + b.bigIntegerValue().doubleValue();
        }

    }

    @CoreMethod(names = "-", required = 1)
    public abstract static class SubNode extends CoreMethodNode {

        public SubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SubNode(SubNode prev) {
            super(prev);
        }

        @Specialization
        public double sub(double a, int b) {
            return a - b;
        }

        @Specialization
        public double sub(double a, long b) {
            return a - b;
        }

        @Specialization
        public double sub(double a, double b) {
            return a - b;
        }

        @Specialization
        public double sub(double a, RubyBignum b) {
            return a - b.bigIntegerValue().doubleValue();
        }

    }

    @CoreMethod(names = "*", required = 1)
    public abstract static class MulNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode rationalConvertNode;
        @Child private CallDispatchHeadNode rationalPowNode;

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MulNode(MulNode prev) {
            super(prev);
            rationalConvertNode = prev.rationalConvertNode;
            rationalPowNode = prev.rationalPowNode;
        }

        @Specialization
        public double mul(double a, int b) {
            return a * b;
        }

        @Specialization
        public double mul(double a, long b) {
            return a * b;
        }

        @Specialization
        public double mul(double a, double b) {
            return a * b;
        }

        @Specialization
        public double mul(double a, RubyBignum b) {
            return a * b.bigIntegerValue().doubleValue();
        }

        @Specialization(guards = "isRational(b)")
        public Object mul(VirtualFrame frame, double a, RubyBasicObject b) {
            if (rationalConvertNode == null) {
                CompilerDirectives.transferToInterpreter();
                rationalConvertNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
                rationalPowNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            final Object aRational = rationalConvertNode.call(frame, getContext().getCoreLibrary().getRationalClass(), "convert", null, a, 1);

            return rationalPowNode.call(frame, aRational, "*", null, b);
        }

    }

    @CoreMethod(names = "**", required = 1)
    public abstract static class PowNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode complexConvertNode;
        @Child private CallDispatchHeadNode complexPowNode;

        @Child private CallDispatchHeadNode rationalPowNode;

        private final ConditionProfile complexProfile = ConditionProfile.createBinaryProfile();

        public PowNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PowNode(PowNode prev) {
            super(prev);
            rationalPowNode = prev.rationalPowNode;
        }

        @Specialization
        public double pow(double a, int b) {
            return Math.pow(a, b);
        }

        @Specialization
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

                final Object aComplex = complexConvertNode.call(frame, getContext().getCoreLibrary().getComplexClass(), "convert", null, a, 0);

                return complexPowNode.call(frame, aComplex, "**", null, b);
            } else {
                return Math.pow(a, b);
            }
        }

        @Specialization
        public double pow(double a, RubyBignum b) {
            return Math.pow(a, b.bigIntegerValue().doubleValue());
        }

        @Specialization(guards = "isRational(b)")
        public Object pow(VirtualFrame frame, double a, RubyBasicObject b) {
            if (rationalPowNode == null) {
                CompilerDirectives.transferToInterpreter();
                rationalPowNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
            }

            return rationalPowNode.call(frame, a, "pow_rational", null, b);
        }

    }

    @CoreMethod(names = {"/", "__slash__"}, required = 1)
    public abstract static class DivNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode redoCoercedNode;

        public DivNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DivNode(DivNode prev) {
            super(prev);
            redoCoercedNode = prev.redoCoercedNode;
        }

        @Specialization
        public double div(double a, int b) {
            return a / b;
        }

        @Specialization
        public double div(double a, long b) {
            return a / b;
        }

        @Specialization
        public double div(double a, double b) {
            return a / b;
        }

        @Specialization
        public double div(double a, RubyBignum b) {
            return a / b.bigIntegerValue().doubleValue();
        }

        @Specialization(guards = {
                "!isInteger(b)",
                "!isLong(b)",
                "!isDouble(b)",
                "!isRubyBignum(b)"})
        public Object div(VirtualFrame frame, double a, Object b) {
            if (redoCoercedNode == null) {
                CompilerDirectives.transferToInterpreter();
                redoCoercedNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
            }

            return redoCoercedNode.call(frame, a, "redo_coerced", null, getContext().getSymbolTable().getSymbol("/"), b);
        }

    }

    @CoreMethod(names = "%", required = 1)
    public abstract static class ModNode extends CoreMethodNode {

        private ConditionProfile lessThanZeroProfile = ConditionProfile.createBinaryProfile();

        public ModNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ModNode(ModNode prev) {
            super(prev);
        }

        @Specialization
        public double mod(double a, int b) {
            return mod(a, (double) b);
        }

        @Specialization
        public double mod(double a, long b) {
            return mod(a, (double) b);
        }

        @Specialization
        public double mod(double a, double b) {
            if (b == 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().zeroDivisionError(this));
            }

            double result = Math.IEEEremainder(a, b);

            if (lessThanZeroProfile.profile(b * result < 0)) {
                result += b;
            }

            return result;
        }

        @Specialization
        public double mod(double a, RubyBignum b) {
            return mod(a, b.bigIntegerValue().doubleValue());
        }

    }

    @CoreMethod(names = "divmod", required = 1)
    public abstract static class DivModNode extends CoreMethodNode {

        @Child private GeneralDivModNode divModNode;

        public DivModNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            divModNode = new GeneralDivModNode(context, sourceSection);
        }

        public DivModNode(DivModNode prev) {
            super(prev);
            divModNode = prev.divModNode;
        }

        @Specialization
        public RubyArray divMod(double a, int b) {
            return divModNode.execute(a, b);
        }

        @Specialization
        public RubyArray divMod(double a, long b) {
            return divModNode.execute(a, b);
        }

        @Specialization
        public RubyArray divMod(double a, double b) {
            return divModNode.execute(a, b);
        }

        @Specialization
        public RubyArray divMod(double a, RubyBignum b) {
            return divModNode.execute(a, b);
        }

    }

    @CoreMethod(names = "<", required = 1)
    public abstract static class LessNode extends CoreMethodNode {

        public LessNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LessNode(LessNode prev) {
            super(prev);
        }

        @Specialization
        public boolean less(double a, int b) {
            return a < b;
        }

        @Specialization
        public boolean less(double a, long b) {
            return a < b;
        }

        @Specialization
        public boolean less(double a, double b) {
            return a < b;
        }

        @Specialization
        public boolean less(double a, RubyBignum b) {
            return a < b.bigIntegerValue().doubleValue();
        }

        @Specialization(guards = "!isRubyBignum(other)")
        public boolean less(double a, RubyBasicObject other) {
            throw new RaiseException(new RubyException(
                    getContext().getCoreLibrary().getArgumentErrorClass(),
                    getContext().makeString(String.format("comparison of Float with %s failed", other.getLogicalClass().getName())),
                    RubyCallStack.getBacktrace(this)
            ));
        }
    }

    @CoreMethod(names = "<=", required = 1)
    public abstract static class LessEqualNode extends CoreMethodNode {

        public LessEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LessEqualNode(LessEqualNode prev) {
            super(prev);
        }

        @Specialization
        public boolean lessEqual(double a, int b) {
            return a <= b;
        }

        @Specialization
        public boolean lessEqual(double a, long b) {
            return a <= b;
        }

        @Specialization
        public boolean lessEqual(double a, double b) {
            return a <= b;
        }

        @Specialization
        public boolean lessEqual(double a, RubyBignum b) {
            return a <= b.bigIntegerValue().doubleValue();
        }

        @Specialization(guards = "!isRubyBignum(other)")
        public boolean less(double a, RubyBasicObject other) {
            throw new RaiseException(new RubyException(
                    getContext().getCoreLibrary().getArgumentErrorClass(),
                    getContext().makeString(String.format("comparison of Float with %s failed", other.getLogicalClass().getName())),
                    RubyCallStack.getBacktrace(this)
            ));
        }
    }

    @CoreMethod(names = { "==", "===" }, required = 1)
    public abstract static class EqualNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode fallbackCallNode;

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EqualNode(EqualNode prev) {
            super(prev);
        }

        @Specialization
        public boolean equal(double a, int b) {
            return a == b;
        }

        @Specialization
        public boolean equal(double a, long b) {
            return a == b;
        }

        @Specialization
        public boolean equal(double a, double b) {
            return a == b;
        }

        @Specialization
        public boolean equal(double a, RubyBignum b) {
            return a == b.bigIntegerValue().doubleValue();
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object equal(VirtualFrame frame, double a, RubyBasicObject b) {
            if (fallbackCallNode == null) {
                CompilerDirectives.transferToInterpreter();
                fallbackCallNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
            }

            return fallbackCallNode.call(frame, a, "equal_fallback", null, b);
        }
    }

    @CoreMethod(names = "<=>", required = 1)
    public abstract static class CompareNode extends CoreMethodNode {

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CompareNode(CompareNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNaN(a)")
        public RubyNilClass compareFirstNaN(double a, Object b) {
            return nil();
        }

        @Specialization(guards = "isNaN(b)")
        public RubyNilClass compareSecondNaN(Object a, double b) {
            return nil();
        }

        @Specialization(guards = {"!isNaN(a)"})
        public int compare(double a, int b) {
            return Double.compare(a, b);
        }

        @Specialization(guards = {"!isNaN(a)"})
        public int compare(double a, long b) {
            return Double.compare(a, b);
        }

        @Specialization(guards = "isInfinity(a)")
        public int compareInfinity(double a, RubyBignum b) {
            if (a < 0) {
                return -1;
            } else {
                return +1;
            }
        }

        @Specialization(guards = {"!isNaN(a)", "!isInfinity(a)"})
        public int compare(double a, RubyBignum b) {
            return Double.compare(a, b.bigIntegerValue().doubleValue());
        }

        @Specialization(guards = {"!isNaN(a)", "!isNaN(b)"})
        public int compare(double a, double b) {
            return Double.compare(a, b);
        }

        @Specialization(guards = {"!isNaN(a)", "!isRubyBignum(b)"})
        public RubyNilClass compare(double a, RubyBasicObject b) {
            return nil();
        }

    }

    @CoreMethod(names = ">=", required = 1)
    public abstract static class GreaterEqualNode extends CoreMethodNode {

        public GreaterEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GreaterEqualNode(GreaterEqualNode prev) {
            super(prev);
        }

        @Specialization
        public boolean greaterEqual(double a, int b) {
            return a >= b;
        }

        @Specialization
        public boolean greaterEqual(double a, long b) {
            return a >= b;
        }

        @Specialization
        public boolean greaterEqual(double a, double b) {
            return a >= b;
        }

        @Specialization
        public boolean greaterEqual(double a, RubyBignum b) {
            return a >= b.bigIntegerValue().doubleValue();
        }

        @Specialization(guards = "!isRubyBignum(other)")
        public boolean less(double a, RubyBasicObject other) {
            throw new RaiseException(new RubyException(
                    getContext().getCoreLibrary().getArgumentErrorClass(),
                    getContext().makeString(String.format("comparison of Float with %s failed", other.getLogicalClass().getName())),
                    RubyCallStack.getBacktrace(this)
            ));
        }
    }

    @CoreMethod(names = ">", required = 1)
    public abstract static class GreaterNode extends CoreMethodNode {

        public GreaterNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GreaterNode(GreaterNode prev) {
            super(prev);
        }

        @Specialization
        public boolean equal(double a, int b) {
            return a > b;
        }

        @Specialization
        public boolean equal(double a, long b) {
            return a > b;
        }

        @Specialization
        public boolean equal(double a, double b) {
            return a > b;
        }

        @Specialization
        public boolean equal(double a, RubyBignum b) {
            return a > b.bigIntegerValue().doubleValue();
        }

        @Specialization(guards = "!isRubyBignum(other)")
        public boolean less(double a, RubyBasicObject other) {
            throw new RaiseException(new RubyException(
                    getContext().getCoreLibrary().getArgumentErrorClass(),
                    getContext().makeString(String.format("comparison of Float with %s failed", other.getLogicalClass().getName())),
                    RubyCallStack.getBacktrace(this)
            ));
        }
    }

    @CoreMethod(names = { "abs", "magnitude" })
    public abstract static class AbsNode extends CoreMethodNode {

        public AbsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AbsNode(AbsNode prev) {
            super(prev);
        }

        @Specialization
        public double abs(double n) {
            return Math.abs(n);
        }

    }

    @CoreMethod(names = "ceil")
    public abstract static class CeilNode extends CoreMethodNode {

        @Child private FixnumOrBignumNode fixnumOrBignum;

        public CeilNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fixnumOrBignum = new FixnumOrBignumNode(context, sourceSection);
        }

        public CeilNode(CeilNode prev) {
            super(prev);
            fixnumOrBignum = prev.fixnumOrBignum;
        }

        @Specialization
        public Object ceil(double n) {
            return fixnumOrBignum.fixnumOrBignum(Math.ceil(n));
        }

    }

    @CoreMethod(names = "floor")
    public abstract static class FloorNode extends CoreMethodNode {

        @Child private FixnumOrBignumNode fixnumOrBignum;

        public FloorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fixnumOrBignum = new FixnumOrBignumNode(context, sourceSection);
        }

        public FloorNode(FloorNode prev) {
            super(prev);
            fixnumOrBignum = prev.fixnumOrBignum;
        }

        @Specialization
        public Object floor(double n) {
            return fixnumOrBignum.fixnumOrBignum(Math.floor(n));
        }

    }

    @CoreMethod(names = "infinite?")
    public abstract static class InfiniteNode extends CoreMethodNode {

        public InfiniteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InfiniteNode(InfiniteNode prev) {
            super(prev);
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
    public abstract static class NaNNode extends CoreMethodNode {

        public NaNNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NaNNode(NaNNode prev) {
            super(prev);
        }

        @Specialization
        public boolean nan(double value) {
            return Double.isNaN(value);
        }

    }

    @CoreMethod(names = "round")
    public abstract static class RoundNode extends CoreMethodNode {

        @Child private FixnumOrBignumNode fixnumOrBignum;

        private final BranchProfile greaterZero = BranchProfile.create();
        private final BranchProfile lessZero = BranchProfile.create();

        public RoundNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fixnumOrBignum = new FixnumOrBignumNode(context, sourceSection);
        }

        public RoundNode(RoundNode prev) {
            super(prev);
            fixnumOrBignum = prev.fixnumOrBignum;
        }

        @Specialization
        public Object round(double n) {
            // Algorithm copied from JRuby - not shared as we want to branch profile it

            if (Double.isInfinite(n)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().floatDomainError("Infinity", this));
            }

            if (Double.isNaN(n)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().floatDomainError("NaN", this));
            }

            double f = n;

            if (f > 0.0) {
                greaterZero.enter();

                f = Math.floor(f);

                if (n - f >= 0.5) {
                    f += 1.0;
                }
            } else if (f < 0.0) {
                lessZero.enter();

                f = Math.ceil(f);

                if (f - n >= 0.5) {
                    f -= 1.0;
                }
            }

            return fixnumOrBignum.fixnumOrBignum(f);
        }

    }

    @CoreMethod(names = { "to_i", "to_int", "truncate" })
    public abstract static class ToINode extends CoreMethodNode {

        @Child private FixnumOrBignumNode fixnumOrBignum;

        public ToINode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fixnumOrBignum = new FixnumOrBignumNode(context, sourceSection);
        }

        public ToINode(ToINode prev) {
            super(prev);
            fixnumOrBignum = prev.fixnumOrBignum;
        }

        @Specialization
        public Object toI(double value) {
            if (Double.isInfinite(value)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().floatDomainError("Infinity", this));
            }

            if (Double.isNaN(value)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().floatDomainError("NaN", this));
            }

            return fixnumOrBignum.fixnumOrBignum(value);
        }

    }

    @CoreMethod(names = "to_f")
    public abstract static class ToFNode extends CoreMethodNode {

        public ToFNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToFNode(ToFNode prev) {
            super(prev);
        }

        @Specialization
        public double toF(double value) {
            return value;
        }

    }

    @CoreMethod(names = {"to_s", "inspect"})
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyString toS(double value) {
            return getContext().makeString(Double.toString(value));
        }

    }

}
