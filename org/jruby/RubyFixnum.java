/*
 * RubyFixnum.java - Implementation of the Fixnum class.
 * Created on 04. Juli 2001, 22:53
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby;

import org.jruby.runtime.*;
import org.jruby.marshal.*;
import org.jruby.util.Asserts;

/** Implementation of the Fixnum class.
 *
 * @author jpetersen
 * @version $Revision$
 */
public class RubyFixnum extends RubyInteger implements IndexCallable {
    private long value;
    private static final int BIT_SIZE = 63;

    public RubyFixnum(Ruby ruby) {
        this(ruby, 0);
    }

    public RubyFixnum(Ruby ruby, long value) {
        super(ruby, ruby.getClasses().getFixnumClass());
        this.value = value;
    }

    private static final int M_TO_F = 1;
    private static final int M_TO_S = 2;
    private static final int M_OP_LSHIFT = 3;
    private static final int M_OP_RSHIFT = 4;
    private static final int M_OP_PLUS = 5;
    private static final int M_OP_MINUS = 6;
    private static final int M_OP_MUL = 7;
    private static final int M_OP_DIV = 8;
    private static final int M_OP_MOD = 9;
    private static final int M_OP_POW = 10;
    private static final int M_EQUAL = 11;
    private static final int M_OP_CMP = 12;
    private static final int M_OP_GT = 13;
    private static final int M_OP_GE = 14;
    private static final int M_OP_LT = 15;
    private static final int M_OP_LE = 16;
    private static final int M_OP_AND = 17;
//     private static final int M_OP_OR = 18;
//     private static final int M_OP_XOR = 19;
    private static final int M_SIZE = 20;
    private static final int M_HASH = 100;
    private static final int M_ID2NAME = 101;

    public static RubyClass createFixnumClass(Ruby ruby) {
        RubyClass fixnumClass = ruby.defineClass("Fixnum", ruby.getClasses().getIntegerClass());
        fixnumClass.includeModule(ruby.getClasses().getPrecisionModule());

        fixnumClass.defineSingletonMethod("induced_from", CallbackFactory.getSingletonMethod(RubyFixnum.class, "induced_from", RubyObject.class));

        fixnumClass.defineMethod("to_f", IndexedCallback.create(M_TO_F, 0));
        fixnumClass.defineMethod("to_s", IndexedCallback.create(M_TO_S, 0));
        fixnumClass.defineMethod("to_str", IndexedCallback.create(M_TO_S, 0));
        fixnumClass.defineMethod("taint", CallbackFactory.getSelfMethod(0));
        fixnumClass.defineMethod("freeze", CallbackFactory.getSelfMethod(0));

        fixnumClass.defineMethod("<<", IndexedCallback.create(M_OP_LSHIFT, 1));
        fixnumClass.defineMethod(">>", IndexedCallback.create(M_OP_RSHIFT, 1));

        fixnumClass.defineMethod("+", IndexedCallback.create(M_OP_PLUS, 1));
        fixnumClass.defineMethod("-", IndexedCallback.create(M_OP_MINUS, 1));

        fixnumClass.defineMethod("*", IndexedCallback.create(M_OP_MUL, 1));
        fixnumClass.defineMethod("/", IndexedCallback.create(M_OP_DIV, 1));
        fixnumClass.defineMethod("%", IndexedCallback.create(M_OP_MOD, 1));
        fixnumClass.defineMethod("**", IndexedCallback.create(M_OP_POW, 1));

        fixnumClass.defineMethod("==", IndexedCallback.create(M_EQUAL, 1));
        fixnumClass.defineMethod("eql?", IndexedCallback.create(M_EQUAL, 1));
        fixnumClass.defineMethod("equal?", IndexedCallback.create(M_EQUAL, 1));
        fixnumClass.defineMethod("<=>", IndexedCallback.create(M_OP_CMP, 1));
        fixnumClass.defineMethod(">", IndexedCallback.create(M_OP_GT, 1));
        fixnumClass.defineMethod(">=", IndexedCallback.create(M_OP_GE, 1));
        fixnumClass.defineMethod("<", IndexedCallback.create(M_OP_LT, 1));
        fixnumClass.defineMethod("<=", IndexedCallback.create(M_OP_LE, 1));
        fixnumClass.defineMethod("&", IndexedCallback.create(M_OP_AND, 1));
        fixnumClass.defineMethod("|", CallbackFactory.getMethod(RubyFixnum.class, "op_or", RubyInteger.class));
        fixnumClass.defineMethod("^", CallbackFactory.getMethod(RubyFixnum.class, "op_xor", RubyInteger.class));
        fixnumClass.defineMethod("size", IndexedCallback.create(M_SIZE, 0));
        fixnumClass.defineMethod("[]", CallbackFactory.getMethod(RubyFixnum.class, "aref", RubyInteger.class));
        fixnumClass.defineMethod("hash", IndexedCallback.create(M_HASH, 0));
        fixnumClass.defineMethod("id2name", IndexedCallback.create(M_ID2NAME, 0));

        return fixnumClass;
    }

    public RubyObject callIndexed(int index, RubyObject[] args) {
        switch (index) {
        case M_TO_F:
            return to_f();
        case M_TO_S:
            return to_s();
        case M_OP_LSHIFT:
            return op_lshift(args[0]);
        case M_OP_RSHIFT:
            return op_rshift(args[0]);
        case M_OP_PLUS:
            return op_plus(args[0]);
        case M_OP_MINUS:
            return op_minus(args[0]);
        case M_OP_MUL:
            return op_mul(args[0]);
        case M_OP_DIV:
            return op_div(args[0]);
        case M_OP_MOD:
            return op_mod(args[0]);
        case M_OP_POW:
            return op_pow(args[0]);
        case M_EQUAL:
            return equal(args[0]);
        case M_OP_CMP:
            return op_cmp(args[0]);
        case M_OP_GT:
            return op_gt(args[0]);
        case M_OP_GE:
            return op_ge(args[0]);
        case M_OP_LT:
            return op_lt(args[0]);
        case M_OP_LE:
            return op_le(args[0]);
        case M_OP_AND:
            return op_and(args[0]);
        case M_SIZE:
            return size();
        case M_HASH:
            return hash();
        case M_ID2NAME:
            return id2name();
        }
        Asserts.assertNotReached();
        return null;
    }

    public Class getJavaClass() {
        return Long.TYPE;
    }

    public double getDoubleValue() {
        return (double) value;
    }

    public long getLongValue() {
        return value;
    }

    public static RubyFixnum zero(Ruby ruby) {
        return newFixnum(ruby, 0);
    }

    public static RubyFixnum one(Ruby ruby) {
        return newFixnum(ruby, 1);
    }

    public static RubyFixnum minus_one(Ruby ruby) {
        return newFixnum(ruby, -1);
    }

    protected final int compareValue(final RubyNumeric other) {
        if (other instanceof RubyBignum) {
            return -((RubyBignum) other).compareValue(this);
        } else if (other instanceof RubyFloat) {
            final double otherVal = other.getDoubleValue();
            return value > otherVal ? 1 : value < otherVal ? -1 : 0;
        } else {
            final long otherVal = other.getLongValue();
            return value > otherVal ? 1 : value < otherVal ? -1 : 0;
        }
    }

    public RubyFixnum hash() {
        return newFixnum((((int) value) ^ (int) (value >> 32)));
    }

    // Methods of the Fixnum Class (fix_*):

    public static RubyFixnum newFixnum(Ruby ruby, long value) {
        RubyFixnum fixnum;
        if (value >= 0 && value < ruby.fixnumCache.length) {
            fixnum = ruby.fixnumCache[(int) value];
            if (fixnum == null) {
                fixnum = new RubyFixnum(ruby, value);
                ruby.fixnumCache[(int) value] = fixnum;
            }
        } else {
            fixnum = new RubyFixnum(ruby, value);
        }
        return fixnum;
    }

    public RubyFixnum newFixnum(long value) {
        return newFixnum(ruby, value);
    }

    public static RubyInteger induced_from(Ruby ruby, RubyObject recv, RubyObject number) {
        if (number instanceof RubyFixnum) {
            return (RubyFixnum) number;
        } else if (number instanceof RubyFloat) {
            return ((RubyFloat) number).to_i();
        } else if (number instanceof RubyBignum) {
            return RubyFixnum.newFixnum(ruby, ((RubyBignum) number).getLongValue());
        }
        return (RubyFixnum) number.convertToType("Fixnum", "to_int", true);
    }

    public final RubyNumeric op_plus(final RubyObject num) {
        final RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return ((RubyFloat)other).op_plus(this);
        } else if (other instanceof RubyBignum) {
            return ((RubyBignum)other).op_plus(this);
        } else {
            final long otherValue = other.getLongValue();
            final long result = value + otherValue;
            if ((value < 0 && otherValue < 0 && result > 0) || (value > 0 && otherValue > 0 && result < 0)) {
                return RubyBignum.newBignum(getRuby(), value).op_plus(other);
            }
            return newFixnum(result);
        }
    }

    public final RubyNumeric op_minus(final RubyObject num) {
        final RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuby(), getDoubleValue()).op_minus(other);
        } else if (other instanceof RubyBignum) {
            return RubyBignum.newBignum(getRuby(), value).op_minus(other);
        } else {
            final long otherValue = other.getLongValue();
            final long result = value - otherValue;
            if ((value < 0 && otherValue > 0 && result > 0) || (value > 0 && otherValue < 0 && result < 0)) {
                return RubyBignum.newBignum(getRuby(), value).op_minus(other);
            }
            return newFixnum(result);
        }
    }

    public RubyNumeric op_mul(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuby(), getDoubleValue()).op_mul(other);
        } else if (other instanceof RubyBignum) {
            return RubyBignum.newBignum(getRuby(), getLongValue()).op_mul(other);
        } else {
            long otherValue = other.getLongValue();
            long result = value * otherValue;
            if (result / otherValue == value) {
                return newFixnum(result);
            } else {
                return RubyBignum.newBignum(getRuby(), getLongValue()).op_mul(other);
            }
        }
    }

    public RubyNumeric op_div(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuby(), getDoubleValue()).op_div(other);
        } else if (other instanceof RubyBignum) {
            return RubyBignum.newBignum(getRuby(), getLongValue()).op_div(other);
        } else {
            return newFixnum(getRuby(), getLongValue() / other.getLongValue());
        }
    }

    public RubyNumeric op_mod(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuby(), getDoubleValue()).op_mod(other);
        } else if (other instanceof RubyBignum) {
            return RubyBignum.newBignum(getRuby(), getLongValue()).op_mod(other);
        } else {
            return newFixnum(getRuby(), getLongValue() % other.getLongValue());
        }
    }

    public RubyNumeric op_pow(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuby(), getDoubleValue()).op_pow(other);
        } else {
            if (other.getLongValue() == 0) {
                return newFixnum(getRuby(), 1);
            } else if (other.getLongValue() == 1) {
                return this;
            } else if (other.getLongValue() > 1) {
                return RubyBignum.newBignum(getRuby(), getLongValue()).op_pow(other);
            } else {
                return RubyFloat.newFloat(getRuby(), getDoubleValue()).op_pow(other);
            }
        }
    }

    public RubyBoolean equal(RubyObject other) {
        if (!(other instanceof RubyNumeric)) {
            return getRuby().getFalse();
        } else {
            return RubyBoolean.newBoolean(getRuby(), compareValue((RubyNumeric) other) == 0);
        }
    }

    public RubyNumeric op_cmp(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFixnum.newFixnum(getRuby(), compareValue(other));
    }

    public RubyBoolean op_gt(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.newBoolean(getRuby(), compareValue(other) > 0);
    }

    public RubyBoolean op_ge(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.newBoolean(getRuby(), compareValue(other) >= 0);
    }

    public final RubyBoolean op_lt(final RubyObject num) {
        final RubyNumeric other = numericValue(num);
        return RubyBoolean.newBoolean(getRuby(), compareValue(other) < 0);
    }

    public RubyBoolean op_le(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.newBoolean(getRuby(), compareValue(other) <= 0);
    }

    public RubyString to_s() {
        return RubyString.newString(getRuby(), String.valueOf(getLongValue()));
    }

    public RubyFloat to_f() {
        return RubyFloat.newFloat(getRuby(), getDoubleValue());
    }

    public RubyInteger op_lshift(RubyObject num) {
        RubyNumeric other = numericValue(num);
        long width = other.getLongValue();
        if (width < 0)
            return op_rshift(other.op_uminus());
        if (width > BIT_SIZE || value >>> (BIT_SIZE - width) > 0) {
            RubyBignum lBigValue = new RubyBignum(ruby, RubyBignum.bigIntValue(this));
            return lBigValue.op_lshift(other);
        }
        return newFixnum(value << width);
    }

    public RubyInteger op_rshift(RubyObject num) {
        RubyNumeric other = numericValue(num);
        long width = other.getLongValue();
        if (width < 0)
            return op_lshift(other.op_uminus());
        return newFixnum(value >>> width);
    }

    public RubyNumeric op_and(RubyObject other) {
        RubyNumeric otherNumeric = numericValue(other);
        long otherLong = otherNumeric.getTruncatedLongValue();
        return newFixnum(value & otherLong);
    }

    public RubyInteger op_or(RubyInteger other) {
        if (other instanceof RubyBignum) {
            return (RubyInteger) other.funcall("|", this);
        }
        return newFixnum(value | other.getLongValue());
    }

    public RubyInteger op_xor(RubyInteger other) {
        if (other instanceof RubyBignum) {
            return (RubyInteger) other.funcall("^", this);
        }
        return newFixnum(value ^ other.getLongValue());
    }

    public RubyFixnum size() {
        return newFixnum(4);
    }

    public RubyFixnum aref(RubyInteger pos) {
        long mask = 1 << pos.getLongValue();
        return newFixnum((value & mask) == 0 ? 0 : 1);
    }

    public RubySymbol id2name() {
        return RubySymbol.getSymbol(ruby, value);
    }

    public void marshalTo(MarshalStream output) throws java.io.IOException {
        if (value <= Integer.MAX_VALUE) {
            output.write('i');
            output.dumpInt((int) value);
        } else {
            output.dumpObject(RubyBignum.newBignum(ruby, value));
        }
    }

    public static RubyFixnum unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        return RubyFixnum.newFixnum(input.getRuby(),
                                    input.unmarshalInt());
    }
}
