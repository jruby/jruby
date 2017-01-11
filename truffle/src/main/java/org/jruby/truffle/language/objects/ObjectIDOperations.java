/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.numeric.BignumOperations;
import org.jruby.truffle.language.objects.shared.SharedObjects;

import java.math.BigInteger;

/**
 * <pre>
 * Object IDs distribution
 *
 * We try to respect MRI scheme when it makes sense (Fixnum for the moment).
 * Have a look at include/ruby/ruby.h below ruby_special_consts.
 *
 * Encoding for Fixnum (long):
 * ... 0000 = false
 * ... 0010 = true
 * ... 0100 = nil
 *
 * ... xxx1 = Fixnum of value (id-1)/2 if -2^62 <= value < 2^62
 * ... xxx0 = BasicObject generated id (for id > 4)
 *
 * Encoding for Bignum:
 * ... 0001 | 64-bit long = Fixnum if value < -2^62 or value >= 2^62
 * ... 0010 | 64-bit raw double bits = Float
 * </pre>
 */
public abstract class ObjectIDOperations {

    public static final long FALSE = 0;
    public static final long TRUE = 2;
    public static final long NIL = 4;
    public static final long FIRST_OBJECT_ID = 6;

    private static final BigInteger LARGE_FIXNUM_FLAG = BigInteger.ONE.shiftLeft(64);
    private static final BigInteger FLOAT_FLAG = BigInteger.ONE.shiftLeft(65);

    private static final long SMALL_FIXNUM_MIN = -(1L << 62);
    private static final long SMALL_FIXNUM_MAX = (1L << 62) - 1;

    // primitive => ID

    public static boolean isSmallFixnum(long fixnum) {
        // TODO: optimize
        return SMALL_FIXNUM_MIN <= fixnum && fixnum <= SMALL_FIXNUM_MAX;
    }

    public static long smallFixnumToIDOverflow(long fixnum) throws ArithmeticException {
        return Math.addExact(Math.multiplyExact(fixnum, 2), 1);
    }

    public static long smallFixnumToID(long fixnum) {
        assert isSmallFixnum(fixnum);
        return fixnum * 2 + 1;
    }

    public static DynamicObject largeFixnumToID(RubyContext context, long fixnum) {
        assert !isSmallFixnum(fixnum);
        BigInteger big = unsignedBigInteger(fixnum);
        return BignumOperations.createBignum(context, big.or(LARGE_FIXNUM_FLAG));
    }

    public static DynamicObject floatToID(RubyContext context, double value) {
        long bits = Double.doubleToRawLongBits(value);
        BigInteger big = unsignedBigInteger(bits);
        return BignumOperations.createBignum(context, big.or(FLOAT_FLAG));
    }

    // ID => primitive

    public static boolean isSmallFixnumID(long id) {
        return id % 2 != 0;
    }

    public static long toFixnum(long id) {
        return (id - 1) / 2;
    }

    public static boolean isLargeFixnumID(BigInteger id) {
        return !id.and(LARGE_FIXNUM_FLAG).equals(BigInteger.ZERO);
    }

    public static boolean isFloatID(BigInteger id) {
        return !id.and(FLOAT_FLAG).equals(BigInteger.ZERO);
    }

    public static boolean isBasicObjectID(long id) {
        return id >= FIRST_OBJECT_ID && id % 2 == 0;
    }

    private static BigInteger unsignedBigInteger(long value) {
        BigInteger big = BigInteger.valueOf(value);
        if (value < 0) {
            big = new BigInteger(1, big.toByteArray()); // consider as unsigned
        }
        return big;
    }

    @CompilerDirectives.TruffleBoundary
    public static long verySlowGetObjectID(RubyContext context, DynamicObject object) {
        // TODO(CS): we should specialise on reading this in the #object_id method and anywhere else it's used
        Property property = object.getShape().getProperty(Layouts.OBJECT_ID_IDENTIFIER);

        if (property != null) {
            return (long) property.get(object, false);
        }

        final long objectID = context.getObjectSpaceManager().getNextObjectID();

        if (SharedObjects.isShared(object)) {
            synchronized (object) {
                // no need for a write barrier here, objectID is a long.
                object.define(Layouts.OBJECT_ID_IDENTIFIER, objectID);
            }
        } else {
            object.define(Layouts.OBJECT_ID_IDENTIFIER, objectID);
        }

        return objectID;
    }
}
