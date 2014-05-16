/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import java.math.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.RubyFixnum;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.core.array.RubyArray;

@CoreClass(name = "Fixnum")
public abstract class FixnumNodes {

    @CoreMethod(names = "+@", maxArgs = 0)
    public abstract static class PosNode extends CoreMethodNode {

        public PosNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PosNode(PosNode prev) {
            super(prev);
        }

        @Specialization
        public int pos(int value) {
            return value;
        }

    }

    @CoreMethod(names = "-@", maxArgs = 0)
    public abstract static class NegNode extends CoreMethodNode {

        public NegNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NegNode(NegNode prev) {
            super(prev);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public int neg(int value) {
            return ExactMath.subtractExact(0, value);
        }

        @Specialization
        public BigInteger negWithOverflow(int value) {
            return BigInteger.valueOf(value).negate();
        }

    }

    @CoreMethod(names = "+", minArgs = 1, maxArgs = 1)
    public abstract static class AddNode extends CoreMethodNode {

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AddNode(AddNode prev) {
            super(prev);
        }

        @Specialization(order = 1, rewriteOn = ArithmeticException.class)
        public int add(int a, int b) {
            return ExactMath.addExact(a, b);
        }

        @Specialization(order = 2, rewriteOn = ArithmeticException.class)
        public long addWithLongOverflow(int a, int b) {
            return ExactMath.addExact((long) a, (long) b);
        }

        @Specialization(order = 3)
        public Object addWithBigIntegerOverflow(int a, int b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).add(BigInteger.valueOf(b)));
        }

        @Specialization(order = 4)
        public double add(int a, double b) {
            return a + b;
        }

        @Specialization(order = 5)
        public long add(int a, long b) {
            return ExactMath.addExact(a, b);
        }

        @Specialization(order = 6)
        public Object add(int a, BigInteger b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).add(b));
        }

        @Specialization(order = 7, rewriteOn = ArithmeticException.class)
        public long add(long a, int b) {
            return ExactMath.addExact(a, b);
        }

        @Specialization(order = 8, rewriteOn = ArithmeticException.class)
        public long add(long a, long b) {
            return ExactMath.addExact(a, b);
        }

        @Specialization(order = 9)
        public Object addWithBigIntegerOverflow(long a, long b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).add(BigInteger.valueOf(b)));
        }

        @Specialization(order = 10)
        public double add(long a, double b) {
            return a + b;
        }

        @Specialization(order = 11)
        public Object add(long a, BigInteger b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).add(b));
        }

    }

    @CoreMethod(names = "-", minArgs = 1, maxArgs = 1)
    public abstract static class SubNode extends CoreMethodNode {

        public SubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SubNode(SubNode prev) {
            super(prev);
        }

        @Specialization(order = 1, rewriteOn = ArithmeticException.class)
        public int sub(int a, int b) {
            return ExactMath.subtractExact(a, b);
        }

        @Specialization(order = 2, rewriteOn = ArithmeticException.class)
        public long subWithLongOverflow(int a, int b) {
            return ExactMath.subtractExact((long) a, (long) b);
        }

        @Specialization(order = 3)
        public Object subWithBigIntegerOverflow(int a, int b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).subtract(BigInteger.valueOf(b)));
        }

        @Specialization(order = 4)
        public double sub(int a, double b) {
            return a - b;
        }

        @Specialization(order = 5)
        public long sub(int a, long b) {
            return ExactMath.subtractExact(a, b);
        }

        @Specialization(order = 6)
        public Object sub(int a, BigInteger b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).subtract(b));
        }

        @Specialization(order = 7, rewriteOn = ArithmeticException.class)
        public long sub(long a, int b) {
            return ExactMath.subtractExact(a, b);
        }

        @Specialization(order = 8, rewriteOn = ArithmeticException.class)
        public long sub(long a, long b) {
            return ExactMath.subtractExact(a, b);
        }

        @Specialization(order = 9)
        public Object subWithBigIntegerOverflow(long a, long b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).subtract(BigInteger.valueOf(b)));
        }

        @Specialization(order = 10)
        public double sub(long a, double b) {
            return a - b;
        }

        @Specialization(order = 11)
        public Object sub(long a, BigInteger b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).subtract(b));
        }

    }

    @CoreMethod(names = "*", minArgs = 1, maxArgs = 1)
    public abstract static class MulNode extends CoreMethodNode {

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MulNode(MulNode prev) {
            super(prev);
        }

        @Specialization(order = 1, rewriteOn = ArithmeticException.class)
        public int mul(int a, int b) {
            return ExactMath.multiplyExact(a, b);
        }

        @Specialization(order = 2, rewriteOn = ArithmeticException.class)
        public long mulWithLong(int a, int b) {
            return ExactMath.multiplyExact((long) a, (long) b);
        }

        @Specialization(order = 3)
        public Object mulWithBigInteger(int a, int b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)));
        }

        @Specialization(order = 4, rewriteOn = ArithmeticException.class)
        public Object mul(int a, long b) {
            return ExactMath.multiplyExact(a, b);
        }

        @Specialization(order = 5)
        public Object mulWithBigInteger(int a, long b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)));
        }

        @Specialization(order = 6)
        public double mul(int a, double b) {
            return a * b;
        }

        @Specialization(order = 7)
        public Object mul(int a, BigInteger b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).multiply(b));
        }

        @Specialization(order = 8, rewriteOn = ArithmeticException.class)
        public long mul(long a, int b) {
            return ExactMath.multiplyExact(a, b);
        }

        @Specialization(order = 9)
        public Object mulWithBigInteger(long a, int b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)));
        }

        @Specialization(order = 10, rewriteOn = ArithmeticException.class)
        public Object mul(long a, long b) {
            return ExactMath.multiplyExact(a, b);
        }

        @Specialization(order = 11)
        public Object mulWithBigInteger(long a, long b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)));
        }

        @Specialization(order = 12)
        public double mul(long a, double b) {
            return a * b;
        }

        @Specialization(order = 13)
        public Object mul(long a, BigInteger b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).multiply(b));
        }

    }

    @CoreMethod(names = "**", minArgs = 1, maxArgs = 1)
    public abstract static class PowNode extends CoreMethodNode {

        @Child protected FixnumOrBignumNode fixnumOrBignum;

        public PowNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fixnumOrBignum = new FixnumOrBignumNode();
        }

        public PowNode(PowNode prev) {
            super(prev);
            fixnumOrBignum = prev.fixnumOrBignum;
        }

        @Specialization
        public Object pow(int a, int b) {
            // TODO(CS): I'd like to use CompilerDirectives.isConstant here - see if a is 2 or b is 2 for example (binary-trees.rb)
            return fixnumOrBignum.fixnumOrBignum(BigInteger.valueOf(a).pow(b));
        }

        @Specialization
        public double pow(int a, double b) {
            return Math.pow(a, b);
        }

        @Specialization
        public Object pow(int a, BigInteger b) {
            notDesignedForCompilation();

            final BigInteger bigA = BigInteger.valueOf(a);

            BigInteger result = BigInteger.ONE;

            for (BigInteger n = BigInteger.ZERO; b.compareTo(b) < 0; n = n.add(BigInteger.ONE)) {
                result = result.multiply(bigA);
            }

            return result;
        }

    }

    @CoreMethod(names = "/", minArgs = 1, maxArgs = 1)
    public abstract static class DivNode extends CoreMethodNode {

        public DivNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DivNode(DivNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public int div(int a, int b) {
            return a / b;
        }

        @Specialization(order = 2)
        public long div(int a, long b) {
            return a / b;
        }

        @Specialization(order = 3)
        public double div(int a, double b) {
            return a / b;
        }

        @Specialization(order = 4)
        public int div(@SuppressWarnings("unused") int a, @SuppressWarnings("unused") BigInteger b) {
            return 0;
        }

        @Specialization(order = 5)
        public long div(long a, int b) {
            return a / b;
        }

        @Specialization(order = 6)
        public long div(long a, long b) {
            return a / b;
        }

        @Specialization(order = 7)
        public double div(long a, double b) {
            return a / b;
        }

        @Specialization(order = 8)
        public int div(@SuppressWarnings("unused") long a, @SuppressWarnings("unused") BigInteger b) {
            return 0;
        }
    }

    @CoreMethod(names = "%", minArgs = 1, maxArgs = 1)
    public abstract static class ModNode extends CoreMethodNode {

        public ModNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ModNode(ModNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public int mod(int a, int b) {
            return a % b;
        }

        @Specialization(order = 2)
        public long mod(int a, long b) {
            return a % b;
        }

        @Specialization(order = 3)
        public Object mod(int a, BigInteger b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).mod(b));
        }

        @Specialization(order = 4)
        public long mod(long a, int b) {
            return a % b;
        }

        @Specialization(order = 5)
        public long mod(long a, long b) {
            return a % b;
        }

        @Specialization(order = 6)
        public Object mod(long a, BigInteger b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).mod(b));
        }
    }

    @CoreMethod(names = "divmod", minArgs = 1, maxArgs = 1)
    public abstract static class DivModNode extends CoreMethodNode {

        @Child protected GeneralDivModNode divModNode;

        public DivModNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            divModNode = new GeneralDivModNode(context);
        }

        public DivModNode(DivModNode prev) {
            super(prev);
            divModNode = new GeneralDivModNode(getContext());
        }

        @Specialization(order = 1)
        public RubyArray divMod(int a, int b) {
            return divModNode.execute(a, b);
        }

        @Specialization(order = 2)
        public RubyArray divMod(int a, long b) {
            return divModNode.execute(a, b);
        }

        @Specialization(order = 3)
        public RubyArray divMod(int a, BigInteger b) {
            return divModNode.execute(a, b);
        }

        @Specialization(order = 4)
        public RubyArray divMod(long a, int b) {
            return divModNode.execute(a, b);
        }

        @Specialization(order = 5)
        public RubyArray divMod(long a, long b) {
            return divModNode.execute(a, b);
        }

        @Specialization(order = 6)
        public RubyArray divMod(long a, BigInteger b) {
            return divModNode.execute(a, b);
        }

    }

    @CoreMethod(names = "<", minArgs = 1, maxArgs = 1)
    public abstract static class LessNode extends CoreMethodNode {

        public LessNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LessNode(LessNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public boolean less(int a, int b) {
            return a < b;
        }

        @Specialization(order = 2)
        public boolean less(int a, long b) {
            return a < b;
        }

        @Specialization(order = 3)
        public boolean less(int a, double b) {
            return a < b;
        }

        @Specialization(order = 4)
        public boolean less(int a, BigInteger b) {
            return BigInteger.valueOf(a).compareTo(b) < 0;
        }

        @Specialization(order = 5)
        public boolean less(long a, int b) {
            return a < b;
        }

        @Specialization(order = 6)
        public boolean less(long a, long b) {
            return a < b;
        }

        @Specialization(order = 7)
        public boolean less(long a, double b) {
            return a < b;
        }

        @Specialization(order = 8)
        public boolean less(long a, BigInteger b) {
            return BigInteger.valueOf(a).compareTo(b) < 0;
        }
    }

    @CoreMethod(names = "<=", minArgs = 1, maxArgs = 1)
    public abstract static class LessEqualNode extends CoreMethodNode {

        public LessEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LessEqualNode(LessEqualNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public boolean lessEqual(int a, int b) {
            return a <= b;
        }

        @Specialization(order = 2)
        public boolean lessEqual(int a, long b) {
            return a <= b;
        }

        @Specialization(order = 3)
        public boolean lessEqual(int a, double b) {
            return a <= b;
        }

        @Specialization(order = 4)
        public boolean lessEqual(int a, BigInteger b) {
            return BigInteger.valueOf(a).compareTo(b) <= 0;
        }

        @Specialization(order = 5)
        public boolean lessEqual(long a, int b) {
            return a <= b;
        }

        @Specialization(order = 6)
        public boolean lessEqual(long a, long b) {
            return a <= b;
        }

        @Specialization(order = 7)
        public boolean lessEqual(long a, double b) {
            return a <= b;
        }

        @Specialization(order = 8)
        public boolean lessEqual(long a, BigInteger b) {
            return BigInteger.valueOf(a).compareTo(b) <= 0;
        }
    }

    @CoreMethod(names = {"==", "==="}, minArgs = 1, maxArgs = 1)
    public abstract static class EqualNode extends CoreMethodNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EqualNode(EqualNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public boolean equal(int a, int b) {
            return a == b;
        }

        @Specialization(order = 2)
        public boolean equal(int a, long b) {
            return a == b;
        }

        @Specialization(order = 3)
        public boolean equal(int a, double b) {
            return a == b;
        }

        @Specialization(order = 4)
        public boolean equal(int a, BigInteger b) {
            return BigInteger.valueOf(a).compareTo(b) == 0;
        }

        @Specialization(order = 5)
        public boolean equal(long a, int b) {
            return a == b;
        }

        @Specialization(order = 6)
        public boolean equal(long a, long b) {
            return a == b;
        }

        @Specialization(order = 7)
        public boolean equal(long a, double b) {
            return a == b;
        }

        @Specialization(order = 8)
        public boolean equal(long a, BigInteger b) {
            return BigInteger.valueOf(a).compareTo(b) == 0;
        }
    }

    @CoreMethod(names = "<=>", minArgs = 1, maxArgs = 1)
    public abstract static class CompareNode extends CoreMethodNode {

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CompareNode(CompareNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public int compare(int a, int b) {
            return Integer.compare(a, b);
        }

        @Specialization(order = 2)
        public int compare(int a, long b) {
            return Long.compare(a, b);
        }

        @Specialization(order = 3)
        public int compare(int a, double b) {
            return Double.compare(a, b);
        }

        @Specialization(order = 4)
        public int compare(int a, BigInteger b) {
            return BigInteger.valueOf(a).compareTo(b);
        }

        @Specialization(order = 5)
        public int compare(long a, int b) {
            return Long.compare(a, b);
        }

        @Specialization(order = 6)
        public int compare(long a, long b) {
            return Long.compare(a, b);
        }

        @Specialization(order = 7)
        public int compare(long a, double b) {
            return Double.compare(a, b);
        }

        @Specialization(order = 8)
        public int compare(long a, BigInteger b) {
            return BigInteger.valueOf(a).compareTo(b);
        }
    }

    @CoreMethod(names = "!=", minArgs = 1, maxArgs = 1)
    public abstract static class NotEqualNode extends CoreMethodNode {

        public NotEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NotEqualNode(NotEqualNode prev) {
            super(prev);
        }

        @Specialization
        public boolean notEqual(int a, int b) {
            return a != b;
        }

        @Specialization
        public boolean notEqual(int a, double b) {
            return a != b;
        }

        @Specialization
        public boolean notEqual(int a, BigInteger b) {
            return BigInteger.valueOf(a).compareTo(b) != 0;
        }
    }

    @CoreMethod(names = ">=", minArgs = 1, maxArgs = 1)
    public abstract static class GreaterEqualNode extends CoreMethodNode {

        public GreaterEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GreaterEqualNode(GreaterEqualNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public boolean greaterEqual(int a, int b) {
            return a >= b;
        }

        @Specialization(order = 2)
        public boolean greaterEqual(int a, long b) {
            return a >= b;
        }

        @Specialization(order = 3)
        public boolean greaterEqual(int a, double b) {
            return a >= b;
        }

        @Specialization(order = 4)
        public boolean greaterEqual(int a, BigInteger b) {
            return BigInteger.valueOf(a).compareTo(b) >= 0;
        }

        @Specialization(order = 5)
        public boolean greaterEqual(long a, int b) {
            return a >= b;
        }

        @Specialization(order = 6)
        public boolean greaterEqual(long a, long b) {
            return a >= b;
        }

        @Specialization(order = 7)
        public boolean greaterEqual(long a, double b) {
            return a >= b;
        }

        @Specialization(order = 8)
        public boolean greaterEqual(long a, BigInteger b) {
            return BigInteger.valueOf(a).compareTo(b) >= 0;
        }
    }

    @CoreMethod(names = ">", minArgs = 1, maxArgs = 1)
    public abstract static class GreaterNode extends CoreMethodNode {

        public GreaterNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GreaterNode(GreaterNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public boolean greater(int a, int b) {
            return a > b;
        }

        @Specialization(order = 2)
        public boolean greater(int a, long b) {
            return a > b;
        }

        @Specialization(order = 3)
        public boolean greater(int a, double b) {
            return a > b;
        }

        @Specialization(order = 4)
        public boolean greater(int a, BigInteger b) {
            return BigInteger.valueOf(a).compareTo(b) > 0;
        }

        @Specialization(order = 5)
        public boolean greater(long a, int b) {
            return a > b;
        }

        @Specialization(order = 6)
        public boolean greater(long a, long b) {
            return a > b;
        }

        @Specialization(order = 7)
        public boolean greater(long a, double b) {
            return a > b;
        }

        @Specialization(order = 8)
        public boolean greater(long a, BigInteger b) {
            return BigInteger.valueOf(a).compareTo(b) > 0;
        }

    }

    @CoreMethod(names = "~", maxArgs = 0)
    public abstract static class ComplementNode extends CoreMethodNode {

        public ComplementNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ComplementNode(ComplementNode prev) {
            super(prev);
        }

        @Specialization
        public int complement(int n) {
            return ~n;
        }

    }

    @CoreMethod(names = "&", minArgs = 1, maxArgs = 1)
    public abstract static class BitAndNode extends CoreMethodNode {

        public BitAndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BitAndNode(BitAndNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public int bitAnd(int a, int b) {
            return a & b;
        }

        @Specialization(order = 2)
        public long bitAnd(int a, long b) {
            return a & b;
        }

        @Specialization(order = 3)
        public Object bitAnd(int a, BigInteger b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).and(b));
        }

        @Specialization(order = 4)
        public long bitAnd(long a, int b) {
            return a & b;
        }

        @Specialization(order = 5)
        public long bitAnd(long a, long b) {
            return a & b;
        }

        @Specialization(order = 6)
        public Object bitAnd(long a, BigInteger b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).and(b));
        }
    }

    @CoreMethod(names = "|", minArgs = 1, maxArgs = 1)
    public abstract static class BitOrNode extends CoreMethodNode {

        public BitOrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BitOrNode(BitOrNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public int bitOr(int a, int b) {
            return a | b;
        }

        @Specialization(order = 2)
        public long bitOr(int a, long b) {
            return a | b;
        }

        @Specialization(order = 3)
        public Object bitOr(int a, BigInteger b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).or(b));
        }

        @Specialization(order = 4)
        public long bitOr(long a, int b) {
            return a | b;
        }

        @Specialization(order = 5)
        public long bitOr(long a, long b) {
            return a | b;
        }

        @Specialization(order = 6)
        public Object bitOr(long a, BigInteger b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).or(b));
        }
    }

    @CoreMethod(names = "^", minArgs = 1, maxArgs = 1)
    public abstract static class BitXOrNode extends CoreMethodNode {

        public BitXOrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BitXOrNode(BitXOrNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public int bitXOr(int a, int b) {
            return a ^ b;
        }

        @Specialization(order = 2)
        public long bitXOr(int a, long b) {
            return a ^ b;
        }

        @Specialization(order = 3)
        public Object bitXOr(int a, BigInteger b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).xor(b));
        }

        @Specialization(order = 4)
        public long bitXOr(long a, int b) {
            return a ^ b;
        }

        @Specialization(order = 5)
        public long bitXOr(long a, long b) {
            return a ^ b;
        }

        @Specialization(order = 6)
        public Object bitXOr(long a, BigInteger b) {
            return RubyFixnum.fixnumOrBignum(BigInteger.valueOf(a).xor(b));
        }
    }

    @CoreMethod(names = "<<", minArgs = 1, maxArgs = 1)
    public abstract static class LeftShiftNode extends CoreMethodNode {

        @Child protected FixnumOrBignumNode fixnumOrBignum;

        private final BranchProfile bAboveZeroProfile = new BranchProfile();
        private final BranchProfile bNotAboveZeroProfile = new BranchProfile();
        private final BranchProfile useBignumProfile = new BranchProfile();

        public LeftShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fixnumOrBignum = new FixnumOrBignumNode();
        }

        public LeftShiftNode(LeftShiftNode prev) {
            super(prev);
            fixnumOrBignum = prev.fixnumOrBignum;
        }

        @Specialization
        public Object leftShift(int a, int b) {
            if (b > 0) {
                bAboveZeroProfile.enter();

                if (RubyFixnum.SIZE - Integer.numberOfLeadingZeros(a) + b > RubyFixnum.SIZE - 1) {
                    useBignumProfile.enter();
                    return fixnumOrBignum.fixnumOrBignum(BigInteger.valueOf(a).shiftLeft(b));
                } else {
                    return a << b;
                }
            } else {
                bNotAboveZeroProfile.enter();

                if (-b >= Integer.SIZE) {
                    return 0;
                } else {
                    return a >> -b;
                }
            }
        }

        @Specialization
        public Object leftShift(long a, int b) {
            notDesignedForCompilation();

            if (b > 0) {
                if (RubyFixnum.SIZE - Long.numberOfLeadingZeros(a) + b > RubyFixnum.SIZE - 1) {
                    return fixnumOrBignum.fixnumOrBignum(BigInteger.valueOf(a).shiftLeft(b));
                } else {
                    return a << b;
                }
            } else {
                if (-b >= Integer.SIZE) {
                    return 0;
                } else {
                    return a >> -b;
                }
            }
        }

    }

    @CoreMethod(names = ">>", minArgs = 1, maxArgs = 1)
    public abstract static class RightShiftNode extends CoreMethodNode {

        public RightShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RightShiftNode(RightShiftNode prev) {
            super(prev);
        }

        @Specialization
        public int rightShift(int a, int b) {
            if (b > 0) {
                return a >> b;
            } else {
                if (-b >= RubyFixnum.SIZE) {
                    return 0;
                } else {
                    return a << -b;
                }
            }
        }

        @Specialization
        public long rightShift(long a, int b) {
            if (b > 0) {
                return a >> b;
            } else {
                if (-b >= RubyFixnum.SIZE) {
                    return 0;
                } else {
                    return a << -b;
                }
            }
        }

    }

    @CoreMethod(names = "[]", minArgs = 1, maxArgs = 1)
    public abstract static class GetIndexNode extends CoreMethodNode {

        public GetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GetIndexNode(GetIndexNode prev) {
            super(prev);
        }

        @Specialization
        public int getIndex(int self, int index) {
            notDesignedForCompilation();

            if ((self & (1 << index)) == 0) {
                return 0;
            } else {
                return 1;
            }
        }

    }

    @CoreMethod(names = "abs", maxArgs = 0)
    public abstract static class AbsNode extends CoreMethodNode {

        public AbsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AbsNode(AbsNode prev) {
            super(prev);
        }

        @Specialization
        public int abs(int n) {
            return Math.abs(n);
        }

        @Specialization
        public long abs(long n) {
            return Math.abs(n);
        }

    }

    @CoreMethod(names = "chr", maxArgs = 0)
    public abstract static class ChrNode extends CoreMethodNode {

        public ChrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ChrNode(ChrNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString chr(int n) {
            notDesignedForCompilation();

            // TODO(CS): not sure about encoding here
            return getContext().makeString((char) n);
        }

    }

    @CoreMethod(names = "nonzero?", maxArgs = 0)
    public abstract static class NonZeroNode extends CoreMethodNode {

        public NonZeroNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NonZeroNode(NonZeroNode prev) {
            super(prev);
        }

        @Specialization
        public Object nonZero(int value) {
            if (value == 0) {
                return false;
            } else {
                return value;
            }
        }

    }

    @CoreMethod(names = "size", needsSelf = false, maxArgs = 0)
    public abstract static class SizeNode extends CoreMethodNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SizeNode(SizeNode prev) {
            super(prev);
        }

        @Specialization
        public int size() {
            return Integer.SIZE / Byte.SIZE;
        }

    }

    @CoreMethod(names = "step", needsBlock = true, minArgs = 2, maxArgs = 2)
    public abstract static class StepNode extends YieldingCoreMethodNode {

        public StepNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public StepNode(StepNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder step(VirtualFrame frame, int from, int to, int step, RubyProc block) {
            for (int i = from; i <= to; i += step) {
                yield(frame, block, i);
            }

            return NilPlaceholder.INSTANCE;
        }

    }

    @CoreMethod(names = "times", needsBlock = true, maxArgs = 0)
    public abstract static class TimesNode extends YieldingCoreMethodNode {

        private final BranchProfile breakProfile = new BranchProfile();
        private final BranchProfile nextProfile = new BranchProfile();
        private final BranchProfile redoProfile = new BranchProfile();

        public TimesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TimesNode(TimesNode prev) {
            super(prev);
        }

        @Specialization
        public Object times(VirtualFrame frame, int n, RubyProc block) {
            int count = 0;

            try {
                outer: for (int i = 0; i < n; i++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, i);
                            continue outer;
                        } catch (BreakException e) {
                            breakProfile.enter();
                            return e.getResult();
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    ((RubyRootNode) getRootNode()).reportLoopCountThroughBlocks(count);
                }
            }

            return n;
        }

    }

    @CoreMethod(names = {"to_i", "to_int"}, maxArgs = 0)
    public abstract static class ToINode extends CoreMethodNode {

        public ToINode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToINode(ToINode prev) {
            super(prev);
        }

        @Specialization
        public int toI(int n) {
            return n;
        }

    }

    @CoreMethod(names = "to_f", maxArgs = 0)
    public abstract static class ToFNode extends CoreMethodNode {

        public ToFNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToFNode(ToFNode prev) {
            super(prev);
        }

        @Specialization
        public double toF(int n) {
            return n;
        }

        @Specialization
        public double toF(long n) {
            return n;
        }

    }

    @CoreMethod(names = {"to_s", "inspect"}, maxArgs = 0)
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString toS(int n) {
            return getContext().makeString(Integer.toString(n));
        }

        @Specialization
        public RubyString toS(long n) {
            return getContext().makeString(Long.toString(n));
        }

    }

    @CoreMethod(names = "upto", needsBlock = true, minArgs = 1, maxArgs = 1)
    public abstract static class UpToNode extends YieldingCoreMethodNode {

        private final BranchProfile breakProfile = new BranchProfile();
        private final BranchProfile nextProfile = new BranchProfile();
        private final BranchProfile redoProfile = new BranchProfile();

        public UpToNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public UpToNode(UpToNode prev) {
            super(prev);
        }

        @Specialization
        public Object upto(VirtualFrame frame, int from, int to, RubyProc block) {
            notDesignedForCompilation();

            int count = 0;

            try {
                outer:
                for (int i = from; i <= to; i++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, i);
                            continue outer;
                        } catch (BreakException e) {
                            breakProfile.enter();
                            return e.getResult();
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    ((RubyRootNode) getRootNode()).reportLoopCountThroughBlocks(count);
                }
            }

            return NilPlaceholder.INSTANCE;
        }

    }

    @CoreMethod(names = "zero?", maxArgs = 0)
    public abstract static class ZeroNode extends CoreMethodNode {

        public ZeroNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ZeroNode(ZeroNode prev) {
            super(prev);
        }

        @Specialization
        public boolean zero(int n) {
            return n == 0;
        }

        @Specialization
        public boolean zero(long n) {
            return n == 0;
        }

    }

}
