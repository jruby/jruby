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
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyString;

@CoreClass(name = "Bignum")
public abstract class BignumNodes {

    public static abstract class BignumCoreMethodNode extends CoreMethodNode {

        @Child private FixnumOrBignumNode fixnumOrBignum;

        public BignumCoreMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fixnumOrBignum = new FixnumOrBignumNode(context, sourceSection);
        }

        public BignumCoreMethodNode(BignumCoreMethodNode prev) {
            super(prev);
            fixnumOrBignum = prev.fixnumOrBignum;
        }

        public Object fixnumOrBignum(RubyBignum value) {
            return fixnumOrBignum.fixnumOrBignum(value);
        }

    }

    @CoreMethod(names = "-@")
    public abstract static class NegNode extends BignumCoreMethodNode {

        public NegNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NegNode(NegNode prev) {
            super(prev);
        }

        @Specialization
        public RubyBignum neg(RubyBignum value) {
            return value.negate();
        }

    }

    @CoreMethod(names = "+", required = 1)
    public abstract static class AddNode extends BignumCoreMethodNode {

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AddNode(AddNode prev) {
            super(prev);
        }

        @Specialization
        public Object add(RubyBignum a, int b) {
            return fixnumOrBignum(a.add(b));
        }

        @Specialization
        public Object add(RubyBignum a, long b) {
            return fixnumOrBignum(a.add(b));
        }

        @Specialization
        public double add(RubyBignum a, double b) {
            return a.doubleValue() + b;
        }

        @Specialization
        public Object add(RubyBignum a, RubyBignum b) {
            return fixnumOrBignum(a.add(b));
        }

    }

    @CoreMethod(names = "-", required = 1)
    public abstract static class SubNode extends BignumCoreMethodNode {

        public SubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SubNode(SubNode prev) {
            super(prev);
        }

        @Specialization
        public Object sub(RubyBignum a, int b) {
            return fixnumOrBignum(a.subtract(b));
        }

        @Specialization
        public Object sub(RubyBignum a, long b) {
            return fixnumOrBignum(a.subtract(b));
        }

        @Specialization
        public double sub(RubyBignum a, double b) {
            return a.doubleValue() - b;
        }

        @Specialization
        public Object sub(RubyBignum a, RubyBignum b) {
            return fixnumOrBignum(a.subtract(b));
        }

    }

    @CoreMethod(names = "*", required = 1)
    public abstract static class MulNode extends BignumCoreMethodNode {

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MulNode(MulNode prev) {
            super(prev);
        }

        @Specialization
        public Object mul(RubyBignum a, int b) {
            return fixnumOrBignum(a.multiply(b));
        }

        @Specialization
        public Object mul(RubyBignum a, long b) {
            return fixnumOrBignum(a.multiply(b));
        }

        @Specialization
        public double mul(RubyBignum a, double b) {
            return a.doubleValue() * b;
        }

        @Specialization
        public Object mul(RubyBignum a, RubyBignum b) {
            return fixnumOrBignum(a.multiply(b));
        }

    }

    @CoreMethod(names = {"/", "__slash__"}, required = 1)
    public abstract static class DivNode extends BignumCoreMethodNode {

        public DivNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DivNode(DivNode prev) {
            super(prev);
        }

        @Specialization
        public Object div(RubyBignum a, int b) {
            return fixnumOrBignum(a.divide(b));
        }

        @Specialization
        public Object div(RubyBignum a, long b) {
            return fixnumOrBignum(a.divide(b));
        }

        @Specialization
        public double div(RubyBignum a, double b) {
            return a.doubleValue() / b;
        }

        @Specialization
        public Object div(RubyBignum a, RubyBignum b) {
            return fixnumOrBignum(a.divide(b));
        }

    }

    @CoreMethod(names = "%", required = 1)
    public abstract static class ModNode extends BignumCoreMethodNode {

        public ModNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ModNode(ModNode prev) {
            super(prev);
        }

        @Specialization
        public Object mod(RubyBignum a, int b) {
            return fixnumOrBignum(a.mod(b));
        }

        @Specialization
        public Object mod(RubyBignum a, long b) {
            return fixnumOrBignum(a.mod(b));
        }

        @Specialization
        public Object mod(RubyBignum a, RubyBignum b) {
            return fixnumOrBignum(a.mod(b));
        }

    }

    @CoreMethod(names = "<", required = 1)
    public abstract static class LessNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode rationalConvertNode;
        @Child private CallDispatchHeadNode rationalLessNode;

        public LessNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LessNode(LessNode prev) {
            super(prev);
            rationalConvertNode = prev.rationalConvertNode;
            rationalLessNode = prev.rationalLessNode;
        }

        @Specialization
        public boolean less(RubyBignum a, int b) {
            return a.compare(b) < 0;
        }

        @Specialization
        public boolean less(RubyBignum a, long b) {
            return a.compare(b) < 0;
        }

        @Specialization
        public boolean less(RubyBignum a, double b) {
            return a.compare(b) < 0;
        }

        @Specialization
        public boolean less(RubyBignum a, RubyBignum b) {
            return a.compare(b) < 0;
        }

        @Specialization(guards = "isRational(b)")
        public Object pow(VirtualFrame frame, Object a, RubyBasicObject b) {
            if (rationalConvertNode == null) {
                CompilerDirectives.transferToInterpreter();
                rationalConvertNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
                rationalLessNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            final Object aRational = rationalConvertNode.call(frame, getContext().getCoreLibrary().getRationalClass(), "convert", null, a, 1);

            return rationalLessNode.call(frame, aRational, "<", null, b);
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
        public boolean lessEqual(RubyBignum a, int b) {
            return a.compare(b) <= 0;
        }

        @Specialization
        public boolean lessEqual(RubyBignum a, long b) {
            return a.compare(b) <= 0;
        }

        @Specialization
        public boolean lessEqual(RubyBignum a, double b) {
            return a.compare(b) <= 0;
        }

        @Specialization
        public boolean lessEqual(RubyBignum a, RubyBignum b) {
            return a.compare(b) <= 0;
        }
    }

    @CoreMethod(names = {"==", "eql?"}, required = 1)
    public abstract static class EqualNode extends CoreMethodNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EqualNode(EqualNode prev) {
            super(prev);
        }

        @Specialization
        public boolean equal(RubyBignum a, int b) {
            return a.isEqualTo(b);
        }

        @Specialization
        public boolean equal(RubyBignum a, long b) {
            return a.isEqualTo(b);
        }

        @Specialization
        public boolean equal(RubyBignum a, double b) {
            return a.doubleValue() == b;
        }

        @Specialization
        public boolean equal(RubyBignum a, RubyBignum b) {
            return a.isEqualTo(b);
        }
    }

    @CoreMethod(names = "<=>", required = 1)
    public abstract static class CompareNode extends CoreMethodNode {

        private final ConditionProfile negativeInfinityProfile = ConditionProfile.createBinaryProfile();

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CompareNode(CompareNode prev) {
            super(prev);
        }

        @Specialization
        public int compare(RubyBignum a, int b) {
            return a.compare(b);
        }

        @Specialization
        public int compare(RubyBignum a, long b) {
            return a.compare(b);
        }

        @Specialization
        public int compare(RubyBignum a, double b) {
            if (negativeInfinityProfile.profile(Double.isInfinite(b) && b < 0)) {
                return 1;
            } else {
                return Double.compare(a.doubleValue(), b);
            }
        }

        @Specialization
        public int compare(RubyBignum a, RubyBignum b) {
            return a.compare(b);
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
        public boolean greaterEqual(RubyBignum a, int b) {
            return a.compare(b) >= 0;
        }

        @Specialization
        public boolean greaterEqual(RubyBignum a, long b) {
            return a.compare(b) >= 0;
        }

        @Specialization
        public boolean greaterEqual(RubyBignum a, double b) {
            return a.compare(b) >= 0;
        }

        @Specialization
        public boolean greaterEqual(RubyBignum a, RubyBignum b) {
            return a.compare(b) >= 0;
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
        public boolean greater(RubyBignum a, int b) {
            return a.compare(b) > 0;
        }

        @Specialization
        public boolean greater(RubyBignum a, long b) {
            return a.compare(b) > 0;
        }

        @Specialization
        public boolean greater(RubyBignum a, double b) {
            return a.compare(b) > 0;
        }

        @Specialization
        public boolean greater(RubyBignum a, RubyBignum b) {
            return a.compare(b) > 0;
        }
    }

    @CoreMethod(names = "&", required = 1)
    public abstract static class BitAndNode extends BignumCoreMethodNode {

        public BitAndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BitAndNode(BitAndNode prev) {
            super(prev);
        }

        @Specialization
        public Object bitAnd(RubyBignum a, int b) {
            return fixnumOrBignum(a.and(b));
        }

        @Specialization
        public Object bitAnd(RubyBignum a, long b) {
            return fixnumOrBignum(a.and(b));
        }

        @Specialization
        public Object bitAnd(RubyBignum a, RubyBignum b) {
            return fixnumOrBignum(a.and(b));
        }
    }

    @CoreMethod(names = "|", required = 1)
    public abstract static class BitOrNode extends BignumCoreMethodNode {

        public BitOrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BitOrNode(BitOrNode prev) {
            super(prev);
        }

        @Specialization
        public Object bitOr(RubyBignum a, int b) {
            return fixnumOrBignum(a.or(b));
        }

        @Specialization
        public Object bitOr(RubyBignum a, long b) {
            return fixnumOrBignum(a.or(b));
        }

        @Specialization
        public Object bitOr(RubyBignum a, RubyBignum b) {
            return fixnumOrBignum(a.or(a));
        }
    }

    @CoreMethod(names = "^", required = 1)
    public abstract static class BitXOrNode extends BignumCoreMethodNode {

        public BitXOrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BitXOrNode(BitXOrNode prev) {
            super(prev);
        }

        @Specialization
        public Object bitXOr(RubyBignum a, int b) {
            return fixnumOrBignum(a.xor(b));
        }

        @Specialization
        public Object bitXOr(RubyBignum a, long b) {
            return fixnumOrBignum(a.xor(b));
        }

        @Specialization
        public Object bitXOr(RubyBignum a, RubyBignum b) {
            return fixnumOrBignum(a.xor(b));
        }
    }

    @CoreMethod(names = "<<", required = 1)
    public abstract static class LeftShiftNode extends BignumCoreMethodNode {

        private final BranchProfile bLessThanZero = BranchProfile.create();

        public LeftShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LeftShiftNode(LeftShiftNode prev) {
            super(prev);
        }

        @Specialization
        public Object leftShift(RubyBignum a, int b) {
            if (b >= 0) {
                return fixnumOrBignum(a.shiftLeft(b));
            } else {
                bLessThanZero.enter();
                return fixnumOrBignum(a.shiftRight(-b));
            }
        }

    }

    @CoreMethod(names = ">>", required = 1)
    public abstract static class RightShiftNode extends BignumCoreMethodNode {

        private final BranchProfile bLessThanZero = BranchProfile.create();

        public RightShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RightShiftNode(RightShiftNode prev) {
            super(prev);
        }

        @Specialization
        public Object leftShift(RubyBignum a, int b) {
            if (b >= 0) {
                return fixnumOrBignum(a.shiftRight(b));
            } else {
                bLessThanZero.enter();
                return fixnumOrBignum(a.shiftLeft(-b));
            }
        }

    }

    @CoreMethod(names = { "abs", "magnitude" })
    public abstract static class AbsNode extends BignumCoreMethodNode {

        public AbsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AbsNode(AbsNode prev) {
            super(prev);
        }

        @Specialization
        public RubyBignum abs(RubyBignum value) {
            return value.abs();
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
        public RubyArray divMod(RubyBignum a, int b) {
            return divModNode.execute(a, b);
        }

        @Specialization
        public RubyArray divMod(RubyBignum a, long b) {
            return divModNode.execute(a, b);
        }

        @Specialization
        public RubyArray divMod(RubyBignum a, RubyBignum b) {
            return divModNode.execute(a, b);
        }

    }

    @CoreMethod(names = "even?")
    public abstract static class EvenNode extends BignumCoreMethodNode {

        public EvenNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EvenNode(EvenNode prev) {
            super(prev);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public boolean even(RubyBignum value) {
            return value.bigIntegerValue().getLowestSetBit() != 0;
        }

    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodNode {

        public HashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public HashNode(HashNode prev) {
            super(prev);
        }

        @Specialization
        public int hash(RubyBignum self) {
            return self.bigIntegerValue().hashCode();
        }

    }

    @CoreMethod(names = "size")
    public abstract static class SizeNode extends CoreMethodNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SizeNode(SizeNode prev) {
            super(prev);
        }

        @Specialization
        public int size(RubyBignum value) {
            return value.bigIntegerValue().bitLength();
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
        public double toF(RubyBignum value) {
            return value.doubleValue();
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

        @Specialization
        public RubyString toS(RubyBignum value) {
            return getContext().makeString(value.toString());
        }

    }

}
