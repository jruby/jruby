/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyFixnum;
import org.jruby.truffle.runtime.core.array.ArrayStore;
import org.jruby.truffle.runtime.core.array.FixnumImmutablePairArrayStore;
import org.jruby.truffle.runtime.core.array.ObjectImmutablePairArrayStore;
import org.jruby.truffle.runtime.core.array.RubyArray;

import java.math.BigInteger;

public class GeneralDivModNode extends Node {

    private final RubyContext context;

    private final BranchProfile bZeroProfile = new BranchProfile();
    private final BranchProfile bMinusOneProfile = new BranchProfile();
    private final BranchProfile bigIntegerFixnumProfile = new BranchProfile();
    private final BranchProfile useFixnumPairProfile = new BranchProfile();
    private final BranchProfile useObjectPairProfile = new BranchProfile();

    public GeneralDivModNode(RubyContext context) {
        assert context != null;
        this.context = context;
    }

    public RubyArray execute(int a, int b) {
        return divMod(a, b);
    }

    public RubyArray execute(int a, long b) {
        return divMod(a, b);
    }

    public RubyArray execute(int a, BigInteger b) {
        return divMod(BigInteger.valueOf(a), b);
    }

    public RubyArray execute(long a, int b) {
        return divMod(a, b);
    }

    public RubyArray execute(long a, long b) {
        return divMod(a, b);
    }

    public RubyArray execute(long a, BigInteger b) {
        return divMod(BigInteger.valueOf(a), b);
    }

    public RubyArray execute(BigInteger a, int b) {
        return divMod(a, BigInteger.valueOf(b));
    }

    public RubyArray execute(BigInteger a, long b) {
        return divMod(a, BigInteger.valueOf(b));
    }

    public RubyArray execute(BigInteger a, BigInteger b) {
        return divMod(a, b);
    }

    /*
     * div-mod algorithms copied from org.jruby.RubyFixnum and org.jruby.RubyBignum. See license and contributors there.
     */

    private RubyArray divMod(long a, long b) {
        if (b == 0) {
            bZeroProfile.enter();
            throw new ArithmeticException("divide by zero");
        }

        long mod;
        Object integerDiv;

        if (b == -1) {
            bMinusOneProfile.enter();

            if (a == Long.MIN_VALUE) {
                integerDiv = BigInteger.valueOf(a).negate();
            } else {
                integerDiv = -a;
            }
            mod = 0;
        } else {
            long div = a / b;
            mod = a - b * div;
            if (mod < 0 && b > 0 || mod > 0 && b < 0) {
                div -= 1;
                mod += b;
            }
            integerDiv = div;
        }

        final ArrayStore store;

        if (integerDiv instanceof Long && ((long) integerDiv) >= Integer.MIN_VALUE && ((long) integerDiv) <= Integer.MAX_VALUE && mod >= Integer.MIN_VALUE && mod <= Integer.MAX_VALUE) {
            useFixnumPairProfile.enter();
            store = new FixnumImmutablePairArrayStore((int) (long) integerDiv, (int) mod);
        } else {
            useObjectPairProfile.enter();
            store = new ObjectImmutablePairArrayStore(integerDiv, mod);
        }

        return new RubyArray(context.getCoreLibrary().getArrayClass(), store);
    }

    private RubyArray divMod(BigInteger a, BigInteger b) {
        if (b.signum() == 0) {
            bZeroProfile.enter();
            throw new ArithmeticException("divide by zero");
        }

        final BigInteger[] bigIntegerResults = a.divideAndRemainder(b);

        if ((a.signum() * b.signum()) == -1 && bigIntegerResults[1].signum() != 0) {
            bigIntegerFixnumProfile.enter();
            bigIntegerResults[0] = bigIntegerResults[0].subtract(BigInteger.ONE);
            bigIntegerResults[1] = b.add(bigIntegerResults[1]);
        }

        final ArrayStore store = new ObjectImmutablePairArrayStore(RubyFixnum.fixnumOrBignum(bigIntegerResults[0]), RubyFixnum.fixnumOrBignum(bigIntegerResults[1]));
        return new RubyArray(context.getCoreLibrary().getArrayClass(), store);
    }

}
