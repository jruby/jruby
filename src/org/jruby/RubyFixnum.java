/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 David Corbin <dcorbin@users.sourceforge.net>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import org.jruby.runtime.Arity;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.ReflectionCallback;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;

/** Implementation of the Fixnum class.
 *
 * @author jpetersen
 * @version $Revision$
 */
public class RubyFixnum extends RubyInteger {
    private long value;
    private static final int BIT_SIZE = 64;
    public static final long MAX = (1L<<(BIT_SIZE - 2)) - 1;
    public static final long MIN = -1 * MAX - 1;
    private static final long MAX_MARSHAL_FIXNUM = (1L << 30) - 1;

    public RubyFixnum(Ruby runtime) {
        this(runtime, 0);
    }

    public RubyFixnum(Ruby runtime, long value) {
        super(runtime, runtime.getClass("Fixnum"));
        this.value = value;
    }

    public static RubyClass createFixnumClass(Ruby runtime) {
        RubyClass fixnumClass = runtime.defineClass("Fixnum", runtime.getClasses().getIntegerClass());
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyFixnum.class);

        fixnumClass.defineMethod("quo", callbackFactory.getMethod("quo", RubyNumeric.class));
        fixnumClass.defineMethod("to_f", callbackFactory.getMethod("to_f"));
        fixnumClass.defineMethod("to_i", callbackFactory.getMethod("to_i"));
        fixnumClass.defineMethod("to_s", callbackFactory.getMethod("to_s"));
        fixnumClass.defineMethod("taint", callbackFactory.getMethod("taint"));
        fixnumClass.defineMethod("freeze", callbackFactory.getMethod("freeze"));
        fixnumClass.defineMethod("<<", callbackFactory.getMethod("op_lshift", RubyNumeric.class));
        fixnumClass.defineMethod(">>", callbackFactory.getMethod("op_rshift", RubyNumeric.class));
        fixnumClass.defineMethod("+", callbackFactory.getMethod("op_plus", IRubyObject.class));
        fixnumClass.defineMethod("-", callbackFactory.getMethod("op_minus", RubyNumeric.class));
        fixnumClass.defineMethod("*", callbackFactory.getMethod("op_mul", RubyNumeric.class));
        fixnumClass.defineMethod("/", callbackFactory.getMethod("op_div", RubyNumeric.class));
        fixnumClass.defineAlias("div", "/");
        fixnumClass.defineMethod("%", callbackFactory.getMethod("op_mod", RubyNumeric.class));
        fixnumClass.defineMethod("**", callbackFactory.getMethod("op_pow", RubyNumeric.class));
        fixnumClass.defineMethod("&", callbackFactory.getMethod("op_and", RubyNumeric.class));
        fixnumClass.defineMethod("|", callbackFactory.getMethod("op_or", RubyNumeric.class));
        fixnumClass.defineMethod("^", callbackFactory.getMethod("op_xor", RubyNumeric.class));
        fixnumClass.defineMethod("size", callbackFactory.getMethod("size"));
        fixnumClass.defineMethod("[]", new ReflectionCallback(RubyFixnum.class, "aref", Arity.fixed(1)));
        fixnumClass.defineMethod("hash", callbackFactory.getMethod("hash"));
        fixnumClass.defineMethod("id2name", callbackFactory.getMethod("id2name"));
        fixnumClass.defineMethod("~", callbackFactory.getMethod("invert"));
        fixnumClass.defineMethod("id", callbackFactory.getMethod("id"));

        fixnumClass.defineSingletonMethod("induced_from", callbackFactory.getSingletonMethod("induced_from", IRubyObject.class));

        return fixnumClass;
    }
    
    public boolean isImmediate() {
    	return true;
    }

    public Class getJavaClass() {
        return Long.TYPE;
    }

    public double getDoubleValue() {
        return value;
    }

    public long getLongValue() {
        return value;
    }

    public static RubyFixnum zero(Ruby runtime) {
        return runtime.newFixnum(0);
    }

    public static RubyFixnum one(Ruby runtime) {
        return runtime.newFixnum(1);
    }

    public static RubyFixnum minus_one(Ruby runtime) {
        return runtime.newFixnum(-1);
    }

    protected int compareValue(RubyNumeric other) {
        if (other instanceof RubyBignum) {
            return -other.compareValue(this);
        } else if (other instanceof RubyFloat) {
            final double otherVal = other.getDoubleValue();
            return value > otherVal ? 1 : value < otherVal ? -1 : 0;
        } else {
            final long otherVal = other.getLongValue();
            return value > otherVal ? 1 : value < otherVal ? -1 : 0;
        }
    }

    public RubyFixnum hash() {
        return newFixnum((int) value ^ (int) (value >> 32));
    }

    // Methods of the Fixnum Class (fix_*):

    public static RubyFixnum newFixnum(Ruby runtime, long value) {
        RubyFixnum fixnum;
        if (value >= 0 && value < runtime.fixnumCache.length) {
            fixnum = runtime.fixnumCache[(int) value];
            if (fixnum == null) {
                fixnum = new RubyFixnum(runtime, value);
                runtime.fixnumCache[(int) value] = fixnum;
            }
        } else {
            fixnum = new RubyFixnum(runtime, value);
        }
        return fixnum;
    }

    public RubyFixnum newFixnum(long value) {
        return getRuntime().newFixnum(value);
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

    public IRubyObject op_plus(IRubyObject other) {
        if (other instanceof RubyFloat) {
            return getRuntime().newFloat(getDoubleValue() + ((RubyFloat)other).getDoubleValue());
        } else if (other instanceof RubyFixnum) {
            long otherValue = ((RubyFixnum)other).getLongValue();
            long result = value + otherValue;
            if (other instanceof RubyBignum ||
                (value < 0 && otherValue < 0 && (result > 0 || result < -MAX)) || 
                (value > 0 && otherValue > 0 && (result < 0 || result > MAX))) {
                return RubyBignum.newBignum(getRuntime(), value).op_plus((RubyFixnum)other);
            }
            return newFixnum(result);
        }
        return callCoerced("+", other);
    }

    public RubyNumeric op_minus(RubyNumeric other) {
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

    public RubyNumeric op_mul(RubyNumeric other) {
        return other.multiplyWith(this);
    }

    public RubyNumeric multiplyWith(RubyFixnum other) {
        long otherValue = other.getLongValue();
        if (otherValue == 0) {
            return RubyFixnum.zero(getRuntime());
        }
        long result = value * otherValue;
        if (result > MAX || result < MIN || result / otherValue != value) {
            return RubyBignum.newBignum(getRuntime(), getLongValue()).op_mul(other);
        }
		return newFixnum(result);
    }

    public RubyNumeric multiplyWith(RubyInteger other) {
        return other.multiplyWith(this);
    }

    public RubyNumeric multiplyWith(RubyFloat other) {
       return other.multiplyWith(RubyFloat.newFloat(getRuntime(), getLongValue()));
    }

    public RubyNumeric quo(RubyNumeric other) {
        return new RubyFloat(getRuntime(), op_div(other).getDoubleValue());
    }
    
    public RubyNumeric op_div(RubyNumeric other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_div(other);
        } else if (other instanceof RubyBignum) {
            return RubyBignum.newBignum(getRuntime(), getLongValue()).op_div(other);
        }
        
        // Java / and % are not the same as ruby
        long x = getLongValue();
        long y = other.getLongValue();
        long div = x / y;
        long mod = x % y;

        if (mod < 0 && y > 0 || mod > 0 && y < 0) {
            div -= 1;
        }

        return getRuntime().newFixnum(div);
    }

    public RubyNumeric op_mod(RubyNumeric other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_mod(other);
        } else if (other instanceof RubyBignum) {
            return RubyBignum.newBignum(getRuntime(), getLongValue()).op_mod(other);
        } 
        
	    // Java / and % are not the same as ruby
        long x = getLongValue();
        long y = other.getLongValue();
        long mod = x % y;

        if (mod < 0 && y > 0 || mod > 0 && y < 0) {
            mod += y;
        }

        return getRuntime().newFixnum(mod);
    }

    public RubyNumeric op_pow(RubyNumeric other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_pow(other);
        }
		if (other.getLongValue() == 0) {
		    return getRuntime().newFixnum(1);
		} else if (other.getLongValue() == 1) {
		    return this;
		} else if (other.getLongValue() > 1) {
		    return RubyBignum.newBignum(getRuntime(), getLongValue()).op_pow(other);
		} else {
		    return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_pow(other);
		}
    }

    public RubyString to_s() {
        return getRuntime().newString(String.valueOf(getLongValue()));
    }

    public RubyFloat to_f() {
        return RubyFloat.newFloat(getRuntime(), getDoubleValue());
    }

    public RubyInteger op_lshift(RubyNumeric other) {
        long width = other.getLongValue();
        if (width < 0) {
			return op_rshift(other.op_uminus());
		}
        if (value > 0) {
	    if (width >= BIT_SIZE - 2 ||
		value >> (BIT_SIZE - width) > 0) {
		RubyBignum lBigValue = 
		    RubyBignum.newBignum(getRuntime(), 
					 RubyBignum.bigIntValue(this));
		return lBigValue.op_lshift(other);
	    }
	} else {
	    if (width >= BIT_SIZE - 1 ||
		value >> (BIT_SIZE - width) < -1) {
		RubyBignum lBigValue = 
		    RubyBignum.newBignum(getRuntime(), 
					 RubyBignum.bigIntValue(this));
		return lBigValue.op_lshift(other);
	    }
	}

        return newFixnum(value << width);
    }

    public RubyInteger op_rshift(RubyNumeric other) {
        long width = other.getLongValue();
        if (width < 0) {
			return op_lshift(other.op_uminus());
		}
        return newFixnum(value >> width);
    }

    public RubyNumeric op_and(RubyNumeric other) {
        return newFixnum(value & other.getTruncatedLongValue());
    }

    public RubyInteger op_or(RubyNumeric other) {
        if (other instanceof RubyBignum) {
            return (RubyInteger) other.callMethod("|", this);
        }
        return newFixnum(value | other.getLongValue());
    }

    public RubyInteger op_xor(RubyNumeric other) {
        if (other instanceof RubyBignum) {
            return (RubyInteger) other.callMethod("^", this);
        }
        return newFixnum(value ^ other.getLongValue());
    }

    public RubyFixnum size() {
        return newFixnum((long) Math.ceil(BIT_SIZE / 8.0));
    }

    public RubyFixnum aref(IRubyObject other) {
        long position = num2long(other);

        // Seems mighty expensive to keep creating over and over again.
        // How else can this be done though?
        if (position > BIT_SIZE) {
            RubyBignum bignum = RubyBignum.newBignum(getRuntime(), value);

            return bignum.aref(other);
        }

        return newFixnum((value & 1L << position) == 0 ? 0 : 1);
    }

    public IRubyObject id2name() {
        String symbol = RubySymbol.getSymbol(getRuntime(), value);
        if (symbol != null) {
            return getRuntime().newString(symbol);
        }
        return getRuntime().getNil();
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
            output.dumpObject(RubyBignum.newBignum(getRuntime(), value));
        }
    }

    public static RubyFixnum unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        return input.getRuntime().newFixnum(input.unmarshalInt());
    }
}
