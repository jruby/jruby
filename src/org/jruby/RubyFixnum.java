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
 * Copyright (C) 2002-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2006 Antti Karanta <antti.karanta@napa.fi>
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

import java.math.BigInteger;

import org.jruby.runtime.Arity;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;

/** Implementation of the Fixnum class.
 *
 * @author jpetersen
 */
public class RubyFixnum extends RubyInteger {
    
    public static RubyClass createFixnumClass(IRuby runtime) {
        RubyClass fixnum = runtime.defineClass("Fixnum", runtime.getClass("Integer"),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyFixnum.class);

        fixnum.includeModule(runtime.getModule("Precision"));
        fixnum.defineFastSingletonMethod("induced_from", callbackFactory.getSingletonMethod(
                "induced_from", IRubyObject.class));

        fixnum.defineFastMethod("to_s", callbackFactory.getOptMethod("to_s"));

        fixnum.defineFastMethod("id2name", callbackFactory.getMethod("id2name"));
        fixnum.defineFastMethod("to_sym", callbackFactory.getMethod("to_sym"));

        fixnum.defineFastMethod("-@", callbackFactory.getMethod("uminus"));
        fixnum.defineFastMethod("+", callbackFactory.getMethod("plus", IRubyObject.class));
        fixnum.defineFastMethod("-", callbackFactory.getMethod("minus", IRubyObject.class));
        fixnum.defineFastMethod("*", callbackFactory.getMethod("mul", IRubyObject.class));
        fixnum.defineFastMethod("/", callbackFactory.getMethod("div_slash", IRubyObject.class));
        fixnum.defineFastMethod("div", callbackFactory.getMethod("div_div", IRubyObject.class));
        fixnum.defineFastMethod("%", callbackFactory.getMethod("mod", IRubyObject.class));
        fixnum.defineFastMethod("modulo", callbackFactory.getMethod("mod", IRubyObject.class));
        fixnum.defineFastMethod("divmod", callbackFactory.getMethod("divmod", IRubyObject.class));
        fixnum.defineFastMethod("quo", callbackFactory.getMethod("quo", IRubyObject.class));
        fixnum.defineFastMethod("**", callbackFactory.getMethod("pow", IRubyObject.class));

        fixnum.defineFastMethod("abs", callbackFactory.getMethod("abs"));

        fixnum.defineFastMethod("==", callbackFactory.getMethod("equal", IRubyObject.class));
        fixnum.defineFastMethod("<=>", callbackFactory.getMethod("cmp", IRubyObject.class));

        fixnum.defineFastMethod(">", callbackFactory.getMethod("gt", IRubyObject.class));
        fixnum.defineFastMethod(">=", callbackFactory.getMethod("ge", IRubyObject.class));
        fixnum.defineFastMethod("<", callbackFactory.getMethod("lt", IRubyObject.class));
        fixnum.defineFastMethod("<=", callbackFactory.getMethod("le", IRubyObject.class));

        fixnum.defineFastMethod("~", callbackFactory.getMethod("rev"));
        fixnum.defineFastMethod("&", callbackFactory.getMethod("and", IRubyObject.class));
        fixnum.defineFastMethod("|", callbackFactory.getMethod("or", IRubyObject.class));
        fixnum.defineFastMethod("^", callbackFactory.getMethod("xor", IRubyObject.class));
        fixnum.defineFastMethod("[]", callbackFactory.getMethod("aref", IRubyObject.class));
        fixnum.defineFastMethod("<<", callbackFactory.getMethod("lshift", IRubyObject.class));
        fixnum.defineFastMethod(">>", callbackFactory.getMethod("rshift", IRubyObject.class));

        fixnum.defineFastMethod("to_f", callbackFactory.getMethod("to_f"));
        fixnum.defineFastMethod("size", callbackFactory.getMethod("size"));
        fixnum.defineFastMethod("zero?", callbackFactory.getMethod("zero_p"));

        return fixnum;
    }    
    
    private long value;
    private static final int BIT_SIZE = 64;
    private static final long SIGN_BIT = (1L << (BIT_SIZE - 1));
    public static final long MAX = (1L<<(BIT_SIZE - 1)) - 1;
    public static final long MIN = -1 * MAX - 1;
    private static final long MAX_MARSHAL_FIXNUM = (1L << 30) - 1;
    
    public static final byte OP_PLUS_SWITCHVALUE = 1;
    public static final byte OP_MINUS_SWITCHVALUE = 2;
    public static final byte OP_LT_SWITCHVALUE = 3;

    public RubyFixnum(IRuby runtime) {
        this(runtime, 0);
    }

    public RubyFixnum(IRuby runtime, long value) {
        super(runtime, runtime.getFixnum());
        this.value = value;
    }
    
    public IRubyObject callMethod(ThreadContext context, RubyModule rubyclass, byte switchvalue, String name,
            IRubyObject[] args, CallType callType) {
        switch (switchvalue) {
            case OP_PLUS_SWITCHVALUE:
                Arity.singleArgument().checkArity(context.getRuntime(), args);
                return plus(args[0]);
            case OP_MINUS_SWITCHVALUE:
                Arity.singleArgument().checkArity(context.getRuntime(), args);
                return minus(args[0]);
            case OP_LT_SWITCHVALUE:
                Arity.singleArgument().checkArity(context.getRuntime(), args);
                return lt(args[0]);
            case 0:
            default:
                return super.callMethod(context, rubyclass, name, args, callType);
        }
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

    public static RubyFixnum newFixnum(IRuby runtime, long value) {
        RubyFixnum fixnum;
        RubyFixnum[] fixnumCache = runtime.getFixnumCache();

        if (value >= 0 && value < fixnumCache.length) {
            fixnum = fixnumCache[(int) value];
            if (fixnum == null) {
                fixnum = new RubyFixnum(runtime, value);
                fixnumCache[(int) value] = fixnum;
            }
        } else {
            fixnum = new RubyFixnum(runtime, value);
        }
        return fixnum;
    }

    public RubyFixnum newFixnum(long newValue) {
        return newFixnum(getRuntime(), newValue);
    }

    public static RubyFixnum zero(IRuby runtime) {
        return newFixnum(runtime, 0);
    }

    public static RubyFixnum one(IRuby runtime) {
        return newFixnum(runtime, 1);
    }

    public static RubyFixnum minus_one(IRuby runtime) {
        return newFixnum(runtime, -1);
    }

    public RubyFixnum hash() {
        return newFixnum(hashCode());
    }

    public int hashCode() {
        return (int) value ^ (int) (value >> 32);
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (other instanceof RubyFixnum) {
            RubyFixnum num = (RubyFixnum) other;

            if (num.value == value) {
                return true;
            }
        }

        return false;
    }

    /*  ================
     *  Instance Methods
     *  ================ 
     */

    /** fix_to_s
     * 
     */
    public RubyString to_s(IRubyObject[] args) {
        checkArgumentCount(args, 0, 1);

        int base = args.length == 0 ? 10 : num2int(args[0]);
        if (base < 2 || base > 36) {
            throw getRuntime().newArgumentError("illegal radix " + base);
            }
        return getRuntime().newString(Long.toString(value, base));
        }

    /** fix_id2name
     * 
     */
    public IRubyObject id2name() {
        String symbol = RubySymbol.getSymbol(getRuntime(), value);
        if (symbol != null) {
            return getRuntime().newString(symbol);
    }
        return getRuntime().getNil();
    }

    /** fix_to_sym
     * 
     */
    public IRubyObject to_sym() {
        String symbol = RubySymbol.getSymbol(getRuntime(), value);
        if (symbol != null) {
            return RubySymbol.newSymbol(getRuntime(), symbol);
    }
        return getRuntime().getNil();
    }

    /** fix_uminus
     * 
     */
    public IRubyObject uminus() {
        if (value == MIN) { // a gotcha
            return RubyBignum.newBignum(getRuntime(), BigInteger.valueOf(value).negate());
        }
        return RubyFixnum.newFixnum(getRuntime(), -value);
    }

    /** fix_plus
     * 
     */
    public IRubyObject plus(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            long otherValue = ((RubyFixnum) other).value;
            long result = value + otherValue;
            if ((~(value ^ otherValue) & (value ^ result) & SIGN_BIT) != 0) {
                return RubyBignum.newBignum(getRuntime(), value).plus(other);
            }
            return newFixnum(result);
        }
        if (other instanceof RubyBignum) {
            return ((RubyBignum) other).plus(this);
        }
        if (other instanceof RubyFloat) {
            return getRuntime().newFloat((double) value + ((RubyFloat) other).getDoubleValue());
        }
        return coerceBin("+", other);
    }

    /** fix_minus
     * 
     */
    public IRubyObject minus(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            long otherValue = ((RubyFixnum) other).value;
            long result = value - otherValue;
            if ((~(value ^ ~otherValue) & (value ^ result) & SIGN_BIT) != 0) {
                return RubyBignum.newBignum(getRuntime(), value).minus(other);
            }
            return newFixnum(result);
        } else if (other instanceof RubyBignum) {
            return RubyBignum.newBignum(getRuntime(), value).minus(other);
        } else if (other instanceof RubyFloat) {
            return getRuntime().newFloat((double) value - ((RubyFloat) other).getDoubleValue());
        }
        return coerceBin("-", other);
    }

    /** fix_mul
     * 
     */
    public IRubyObject mul(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            long otherValue = ((RubyFixnum) other).value;
            if (value == 0) {
                return RubyFixnum.zero(getRuntime());
            }
            long result = value * otherValue;
            IRubyObject r = newFixnum(getRuntime(),result);
            if(RubyNumeric.fix2long(r) != result || result/value != otherValue) {
                return (RubyNumeric) RubyBignum.newBignum(getRuntime(), value).mul(other);
            }
            return r;
        } else if (other instanceof RubyBignum) {
            return ((RubyBignum) other).mul(this);
        } else if (other instanceof RubyFloat) {
            return getRuntime().newFloat((double) value * ((RubyFloat) other).getDoubleValue());
        }
        return coerceBin("*", other);
    }

    /** fix_div
     * here is terrible MRI gotcha:
     * 1.div 3.0 -> 0
     * 1 / 3.0   -> 0.3333333333333333
     * 
     * MRI is also able to do it in one place by looking at current frame in rb_num_coerce_bin:
     * rb_funcall(x, ruby_frame->orig_func, 1, y);
     * 
     * also note that RubyFloat doesn't override Numeric.div
     */
    public IRubyObject div_div(IRubyObject other) {
        return idiv(other, "div");
    }

    public IRubyObject div_slash(IRubyObject other) {
        return idiv(other, "/");
    }

    public IRubyObject idiv(IRubyObject other, String method) {
        if (other instanceof RubyFixnum) {
            long x = value;
            long y = ((RubyFixnum) other).value;

            if (y == 0) {
                throw getRuntime().newZeroDivisionError();
            }

            long div = x / y;
            long mod = x % y;

            if (mod < 0 && y > 0 || mod > 0 && y < 0) {
                div -= 1;
    	}
            return getRuntime().newFixnum(div);
    }
        return coerceBin(method, other);
    }

    /** fix_mod
     * 
     */
    public IRubyObject mod(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            // Java / and % are not the same as ruby
            long x = value;
            long y = ((RubyFixnum) other).value;

            if (y == 0) {
            	throw getRuntime().newZeroDivisionError();
            }

            long mod = x % y;

            if (mod < 0 && y > 0 || mod > 0 && y < 0) {
                mod += y;
            }

            return getRuntime().newFixnum(mod);
        }
        return coerceBin("%", other);
    }

    /** fix_divmod
     * 
     */
    public IRubyObject divmod(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            long x = value;
            long y = ((RubyFixnum) other).value;
            final IRuby runtime = getRuntime();

            if (y == 0) {
                throw runtime.newZeroDivisionError();
            }

            long div = x / y;
            long mod = x % y;

            if (mod < 0 && y > 0 || mod > 0 && y < 0) {
                div -= 1;
                mod += y;
            }

            IRubyObject fixDiv = RubyFixnum.newFixnum(getRuntime(), div);
            IRubyObject fixMod = RubyFixnum.newFixnum(getRuntime(), mod);

            return RubyArray.newArray(runtime, fixDiv, fixMod);

    }
        return coerceBin("divmod", other);
    }

    /** fix_quo
     * 
     */
    public IRubyObject quo(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return RubyFloat.newFloat(getRuntime(), (double) value
                    / (double) ((RubyFixnum) other).value);
            }
        return coerceBin("quo", other);
	            }

    /** fix_pow 
     * 
     */
    public IRubyObject pow(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            long b = ((RubyFixnum) other).value;
            if (b == 0) {
                return RubyFixnum.one(getRuntime());
                }
            if (b == 1) {
                return this;
            }
            if (b > 0) {
                return RubyBignum.newBignum(getRuntime(), value).pow(other);
            }
            return RubyFloat.newFloat(getRuntime(), Math.pow(value, b));
        } else if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), Math.pow(value, ((RubyFloat) other)
                    .getDoubleValue()));
        }
        return coerceBin("**", other);
    }

    /** fix_abs
     * 
     */
    public IRubyObject abs() {
        if (value < 0) {
            return RubyFixnum.newFixnum(getRuntime(), -value);
    	}
        return this;
    }

    /** fix_equal
     * 
     */
    public IRubyObject equal(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return RubyBoolean.newBoolean(getRuntime(), value == ((RubyFixnum) other).value);
            }
        return super.equal(other);
            }

    /** fix_cmp
     * 
     */
    public IRubyObject cmp(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            long otherValue = ((RubyFixnum) other).value;
            if (value == otherValue) {
                return RubyFixnum.zero(getRuntime());
        }
            if (value > otherValue) {
                return RubyFixnum.one(getRuntime());
            }
            return RubyFixnum.minus_one(getRuntime());
        }
        return coerceCmp("<=>", other);
    }

    /** fix_gt
     * 
     */
    public IRubyObject gt(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return RubyBoolean.newBoolean(getRuntime(), value > ((RubyFixnum) other).value);
    }
        return coerceRelOp(">", other);
    }

    /** fix_ge
     * 
     */
    public IRubyObject ge(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return RubyBoolean.newBoolean(getRuntime(), value >= ((RubyFixnum) other).value);
        }
        return coerceRelOp(">=", other);
    }

    /** fix_lt
     * 
     */
    public IRubyObject lt(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return RubyBoolean.newBoolean(getRuntime(), value < ((RubyFixnum) other).value);
            }
        return coerceRelOp("<", other);
    }

    /** fix_le
     * 
     */
    public IRubyObject le(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return RubyBoolean.newBoolean(getRuntime(), value <= ((RubyFixnum) other).value);
        }
        return coerceRelOp("<=", other);
    }

    /** fix_rev
     * 
     */
    public IRubyObject rev() {
        return newFixnum(~value);
    	}

    /** fix_and
     * 
     */
    public IRubyObject and(IRubyObject other) {
        if (other instanceof RubyBignum) {
            return ((RubyBignum) other).and(this);
    }
        return RubyFixnum.newFixnum(getRuntime(), value & num2long(other));
    }

    /** fix_or 
     * 
     */
    public IRubyObject or(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return newFixnum(value | ((RubyFixnum) other).value);
        }
        if (other instanceof RubyBignum) {
            return ((RubyBignum) other).or(this);
        }
        if (other instanceof RubyNumeric) {
            return newFixnum(value | ((RubyNumeric) other).getLongValue());
        }

        return or(RubyFixnum.newFixnum(getRuntime(), num2long(other)));
    }

    /** fix_xor 
     * 
     */
    public IRubyObject xor(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return newFixnum(value ^ ((RubyFixnum) other).value);
            }
        if (other instanceof RubyBignum) {
            return ((RubyBignum) other).xor(this);
        }
        if (other instanceof RubyNumeric) {
            return newFixnum(value ^ ((RubyNumeric) other).getLongValue());
        }

        return xor(RubyFixnum.newFixnum(getRuntime(), num2long(other)));
        }

    /** fix_aref 
     * 
     */
    public IRubyObject aref(IRubyObject other) {
        if (other instanceof RubyBignum) {
            RubyBignum big = (RubyBignum) other;
            RubyObject tryFix = RubyBignum.bignorm(getRuntime(), big.getValue());
            if (!(tryFix instanceof RubyFixnum)) {
                if (big.getValue().signum() == 0 || value >= 0) {
                    return RubyFixnum.zero(getRuntime());
                }
                return RubyFixnum.one(getRuntime());
            }
        }

        long otherValue = num2long(other);

        if (otherValue < 0) {
            return RubyFixnum.zero(getRuntime());
        }

        if (BIT_SIZE - 1 < otherValue) {
            if (value < 0) {
                return RubyFixnum.one(getRuntime());
            }
            return RubyFixnum.zero(getRuntime());
        }

        return (value & (1L << otherValue)) == 0 ? RubyFixnum.zero(getRuntime()) : RubyFixnum.one(getRuntime());
    }

    /** fix_lshift 
     * 
     */
    public IRubyObject lshift(IRubyObject other) {
        long width = num2long(other);

        if (width < 0) {
            return rshift(RubyFixnum.newFixnum(getRuntime(), -width));
        }

        if (width == 0) {
            return this;
        }

        if (width > BIT_SIZE - 1 || ((~0L << BIT_SIZE - width - 1) & value) != 0) {
            return RubyBignum.newBignum(getRuntime(), value).lshift(other);
        }

        return newFixnum(value << width);
    }

    /** fix_rshift 
     * 
     */
    public IRubyObject rshift(IRubyObject other) {
        long width = num2long(other);

        if (width < 0) {
            return lshift(RubyFixnum.newFixnum(getRuntime(), -width));
        }

        if (width == 0) {
            return this;
        }

        if (width >= BIT_SIZE - 1) {
            if (value < 0) {
                return RubyFixnum.minus_one(getRuntime());
        }
            return RubyFixnum.zero(getRuntime());
        }

        return newFixnum(value >> width);
    }

    /** fix_to_f 
     * 
     */
    public IRubyObject to_f() {
        return RubyFloat.newFloat(getRuntime(), (double) value);
        }

    /** fix_size 
     * 
     */
    public IRubyObject size() {
        return newFixnum((long) ((BIT_SIZE + 7) / 8));
    }

    /** fix_zero_p 
     * 
     */
    public IRubyObject zero_p() {
        return RubyBoolean.newBoolean(getRuntime(), value == 0);
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

    public IRubyObject rbClone() {
        throw getRuntime().newTypeError("can't clone Fixnum");
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

    /*  ================
     *  Singleton Methods
     *  ================ 
     */

    /** rb_fix_induced_from
     * 
     */

    public static IRubyObject induced_from(IRubyObject recv, IRubyObject other) {
        return RubyNumeric.num2fix(other);
    }
}
