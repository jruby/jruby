/*
 * RubyFixnum.java - Implementation of the Fixnum class.
 * Created on 04. Juli 2001, 22:53
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Copyright (C) 2002 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Thomas E Enebo <enebo@acm.org>
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

import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.IndexCallable;
import org.jruby.internal.runtime.builtin.definitions.FixnumDefinition;

/** Implementation of the Fixnum class.
 *
 * @author jpetersen
 * @version $Revision$
 */
public class RubyFixnum extends RubyInteger implements IndexCallable {
    private long value;
    private static final int BIT_SIZE = 64;
    public static final long MAX = (1L<<(BIT_SIZE - 2)) - 1;
    public static final long MIN = -1 * MAX - 1;
    private static final long MAX_MARSHAL_FIXNUM = (1L << 30) - 1;

    public RubyFixnum(Ruby ruby) {
        this(ruby, 0);
    }

    public RubyFixnum(Ruby ruby, long value) {
        super(ruby, ruby.getClass("Fixnum"));
        this.value = value;
    }

    public static RubyClass createFixnumClass(Ruby runtime) {
        return new FixnumDefinition(runtime).getType();
    }

    public IRubyObject callIndexed(int index, IRubyObject[] args) {
        switch (index) {
        case FixnumDefinition.AREF:
            return aref(args[0]);
        case FixnumDefinition.TO_F:
            return to_f();
        case FixnumDefinition.TO_I:
            return to_i();
        case FixnumDefinition.TO_S:
            return to_s();
        case FixnumDefinition.OP_LSHIFT:
            return op_lshift(args[0]);
        case FixnumDefinition.OP_RSHIFT:
            return op_rshift(args[0]);
        case FixnumDefinition.OP_PLUS:
            return op_plus(args[0]);
        case FixnumDefinition.OP_MINUS:
            return op_minus(args[0]);
        case FixnumDefinition.OP_MUL:
            return op_mul(args[0]);
        case FixnumDefinition.OP_DIV:
            return op_div(args[0]);
        case FixnumDefinition.OP_MOD:
            return op_mod(args[0]);
        case FixnumDefinition.OP_POW:
            return op_pow(args[0]);
        case FixnumDefinition.VERYEQUAL:
            return veryEqual(args[0]);
        case FixnumDefinition.EQUAL:
            return equal(args[0]);
        case FixnumDefinition.OP_CMP:
            return op_cmp(args[0]);
        case FixnumDefinition.OP_GT:
            return op_gt(args[0]);
        case FixnumDefinition.OP_GE:
            return op_ge(args[0]);
        case FixnumDefinition.OP_LT:
            return op_lt(args[0]);
        case FixnumDefinition.OP_LE:
            return op_le(args[0]);
        case FixnumDefinition.OP_AND:
            return op_and(args[0]);
        case FixnumDefinition.OP_OR:
            return op_or(args[0]);
        case FixnumDefinition.OP_XOR:
            return op_xor(args[0]);
        case FixnumDefinition.SIZE:
            return size();
        case FixnumDefinition.HASH:
            return hash();
        case FixnumDefinition.ID2NAME:
            return id2name();
        case FixnumDefinition.INVERT:
            return invert();
        case FixnumDefinition.ID:
            return id();
        case FixnumDefinition.TAINT:
            return taint();
        case FixnumDefinition.FREEZE:
            return freeze();
        }
        return super.callIndexed(index, args);
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

    protected int compareValue(RubyNumeric other) {
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
        return newFixnum(runtime, value);
    }

    public boolean singletonMethodsAllowed() {
        return false;
    }

    public static RubyInteger induced_from(IRubyObject recv, 
					   IRubyObject number) {
	// For undocumented reasons ruby allows Symbol as parm for Fixnum.
	if (number instanceof RubySymbol) {
            return (RubyInteger) number.callMethod("to_i");
	} 

	return RubyInteger.induced_from(recv, number);
    }

    public RubyNumeric op_plus(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return ((RubyFloat)other).op_plus(this);
        } else if (other instanceof RubyBignum) {
            return ((RubyBignum)other).op_plus(this);
        } else {
            long otherValue = other.getLongValue();
            long result = value + otherValue;
            if ((value < 0 && otherValue < 0 && (result > 0 || result < -MAX)) || 
                (value > 0 && otherValue > 0 && (result < 0 || result > MAX))) {
                return RubyBignum.newBignum(getRuntime(), value).op_plus(other);
            }
            return newFixnum(result);
        }
    }

    public RubyNumeric op_minus(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_minus(other);
        } else if (other instanceof RubyBignum) {
            return RubyBignum.newBignum(getRuntime(), value).op_minus(other);
        } else {
            long otherValue = other.getLongValue();
            long result = value - otherValue;
            if ((value <= 0 && otherValue >= 0 && (result > 0 || result < -MAX)) || 
		(value >= 0 && otherValue <= 0 && (result < 0 || result > MAX))) {
                return RubyBignum.newBignum(getRuntime(), value).op_minus(other);
            }
            return newFixnum(result);
        }
    }

    public RubyNumeric op_mul(IRubyObject num) {
        return numericValue(num).multiplyWith(this);
    }

    public RubyNumeric multiplyWith(RubyFixnum other) {
        long otherValue = other.getLongValue();
        if (otherValue == 0) {
            return RubyFixnum.zero(getRuntime());
        }
        long result = value * otherValue;
        if (result > MAX || result < MIN || result / otherValue != value) {
            return RubyBignum.newBignum(getRuntime(), getLongValue()).op_mul(other);
        } else {
            return newFixnum(result);
        }
    }

    public RubyNumeric multiplyWith(RubyInteger other) {
        return other.multiplyWith(this);
    }

    public RubyNumeric multiplyWith(RubyFloat other) {
       return other.multiplyWith(RubyFloat.newFloat(runtime, getLongValue()));
    }

    public RubyNumeric op_div(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_div(other);
        } else if (other instanceof RubyBignum) {
            return RubyBignum.newBignum(getRuntime(), getLongValue()).op_div(other);
        } else {
	    // Java / and % are not the same as ruby
	    long x = getLongValue();
	    long y = other.getLongValue();
	    long div = x / y;
	    long mod = x % y;

	    if ((mod < 0 && y > 0) || (mod > 0 && y < 0)) {
		div -= 1;
	    }

            return newFixnum(getRuntime(), div);
        }
    }

    public RubyNumeric op_mod(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_mod(other);
        } else if (other instanceof RubyBignum) {
            return RubyBignum.newBignum(getRuntime(), getLongValue()).op_mod(other);
        } else {
	    // Java / and % are not the same as ruby
	    long x = getLongValue();
	    long y = other.getLongValue();
	    long div = x / y;
	    long mod = x % y;

	    if ((mod < 0 && y > 0) || (mod > 0 && y < 0)) {
		mod += y;
	    }

            return newFixnum(getRuntime(), mod);
        }
    }

    public RubyNumeric op_pow(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_pow(other);
        } else {
            if (other.getLongValue() == 0) {
                return newFixnum(getRuntime(), 1);
            } else if (other.getLongValue() == 1) {
                return this;
            } else if (other.getLongValue() > 1) {
                return RubyBignum.newBignum(getRuntime(), getLongValue()).op_pow(other);
            } else {
                return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_pow(other);
            }
        }
    }

    public RubyBoolean veryEqual(IRubyObject other) {
        if (other instanceof RubyFixnum == false) {
            return getRuntime().getFalse();
        } 
          
	return RubyBoolean.newBoolean(getRuntime(), 
				      compareValue((RubyNumeric) other) == 0);
    }

    public RubyBoolean equal(IRubyObject other) {
        if (other instanceof RubyNumeric == false) {
            return getRuntime().getFalse();
        } 
          
	return RubyBoolean.newBoolean(getRuntime(), 
				      compareValue((RubyNumeric) other) == 0);
    }

    public RubyNumeric op_cmp(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFixnum.newFixnum(getRuntime(), compareValue(other));
    }

    public RubyBoolean op_gt(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.newBoolean(getRuntime(), compareValue(other) > 0);
    }

    public RubyBoolean op_ge(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.newBoolean(getRuntime(), compareValue(other) >= 0);
    }

    public RubyBoolean op_lt(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.newBoolean(getRuntime(), compareValue(other) < 0);
    }

    public RubyBoolean op_le(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.newBoolean(getRuntime(), compareValue(other) <= 0);
    }

    public RubyString to_s() {
        return RubyString.newString(getRuntime(), String.valueOf(getLongValue()));
    }

    public RubyFloat to_f() {
        return RubyFloat.newFloat(getRuntime(), getDoubleValue());
    }

    public RubyInteger op_lshift(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        long width = other.getLongValue();
        if (width < 0)
            return op_rshift(other.op_uminus());
        if (value > 0) {
	    if (width >= BIT_SIZE - 2 ||
		value >> (BIT_SIZE - width) > 0) {
		RubyBignum lBigValue = 
		    RubyBignum.newBignum(runtime, 
					 RubyBignum.bigIntValue(this));
		return lBigValue.op_lshift(other);
	    }
	} else {
	    if (width >= BIT_SIZE - 1 ||
		value >> (BIT_SIZE - width) < -1) {
		RubyBignum lBigValue = 
		    RubyBignum.newBignum(runtime, 
					 RubyBignum.bigIntValue(this));
		return lBigValue.op_lshift(other);
	    }
	}

        return newFixnum(value << width);
    }

    public RubyInteger op_rshift(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        long width = other.getLongValue();
        if (width < 0)
            return op_lshift(other.op_uminus());
        return newFixnum(value >> width);
    }

    public RubyNumeric op_and(IRubyObject other) {
        RubyNumeric otherNumeric = numericValue(other);
        long otherLong = otherNumeric.getTruncatedLongValue();
        return newFixnum(value & otherLong);
    }

    public RubyInteger op_or(IRubyObject other) {
        if (other instanceof RubyBignum) {
            return (RubyInteger) other.callMethod("|", this);
        }
        RubyNumeric otherNumeric = numericValue(other);
        return newFixnum(value | otherNumeric.getLongValue());
    }

    public RubyInteger op_xor(IRubyObject other) {
        if (other instanceof RubyBignum) {
            return (RubyInteger) other.callMethod("^", this);
        }
        RubyNumeric otherNumeric = numericValue(other);
        return newFixnum(value ^ otherNumeric.getLongValue());
    }

    public RubyFixnum size() {
        return newFixnum((long) Math.ceil(BIT_SIZE / 8.0));
    }

    public RubyFixnum aref(IRubyObject other) {
        RubyNumeric numericPosition = numericValue(other);
	long position = numericPosition.getLongValue();

	// Seems mighty expensive to keep creating over and over again.
	// How else can this be done though?
	if (position > BIT_SIZE) {
	    RubyBignum bignum = RubyBignum.newBignum(runtime, value);
	    return bignum.aref(numericPosition);
	}

        return newFixnum((value & (1L << position)) == 0 ? 0 : 1);
    }

    public RubyString id2name() {
        return (RubyString) RubySymbol.getSymbol(runtime, value).to_s();
    }

    public RubyFixnum invert() {
        return newFixnum(~value);
    }

    public RubyFixnum id() {
        return newFixnum(value * 2 + 1);
    }

    public IRubyObject taint() {
        return this;
    }

    public IRubyObject freeze() {
        return this;
    }

    public IRubyObject times() {
        for (long i = 0; i < value; i++) {
            getRuntime().yield(newFixnum(i));
        }
        return this;
    }

    public void marshalTo(MarshalStream output) throws java.io.IOException {
        if (value <= MAX_MARSHAL_FIXNUM) {
            output.write('i');
            output.dumpInt((int) value);
        } else {
            output.dumpObject(RubyBignum.newBignum(runtime, value));
        }
    }

    public static RubyFixnum unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        return RubyFixnum.newFixnum(input.getRuntime(),
                                    input.unmarshalInt());
    }
}
