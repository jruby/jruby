/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2007 Miguel Covarrubias <mlcovarrubias@gmail.com>
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
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.Convert;

/** 
 * Implementation of the Fixnum class.
 */
public class RubyFixnum extends RubyInteger {
    
    public static RubyClass createFixnumClass(Ruby runtime) {
        RubyClass fixnum = runtime.defineClass("Fixnum", runtime.getInteger(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setFixnum(fixnum);
        fixnum.index = ClassIndex.FIXNUM;
        fixnum.kindOf = new RubyModule.KindOf() {
                public boolean isKindOf(IRubyObject obj, RubyModule type) {
                    return obj instanceof RubyFixnum;
                }
            };
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyFixnum.class);

        fixnum.includeModule(runtime.getPrecision());
        
        fixnum.defineAnnotatedMethods(RubyFixnum.class);
        
        fixnum.dispatcher = callbackFactory.createDispatcher(fixnum);
        
        for (int i = 0; i < runtime.fixnumCache.length; i++) {
            runtime.fixnumCache[i] = new RubyFixnum(runtime, fixnum, i - 128);
        }

        return fixnum;
    }    
    
    private final long value;
    private static final int BIT_SIZE = 64;
    public static final long SIGN_BIT = (1L << (BIT_SIZE - 1));
    public static final long MAX = (1L<<(BIT_SIZE - 1)) - 1;
    public static final long MIN = -1 * MAX - 1;
    public static final long MAX_MARSHAL_FIXNUM = (1L << 30) - 1; // 0x3fff_ffff
    public static final long MIN_MARSHAL_FIXNUM = - (1L << 30);   // -0x4000_0000

    public RubyFixnum(Ruby runtime) {
        this(runtime, 0);
    }

    public RubyFixnum(Ruby runtime, long value) {
        super(runtime, runtime.getFixnum(), false);
        this.value = value;
    }
    
    private RubyFixnum(Ruby runtime, RubyClass klazz, long value) {
        super(runtime, klazz, false);
        this.value = value;
    }
    
    public int getNativeTypeIndex() {
        return ClassIndex.FIXNUM;
    }
    
    /** 
     * short circuit for Fixnum key comparison
     */
    public final boolean eql(IRubyObject other) {
        return other instanceof RubyFixnum && value == ((RubyFixnum)other).value;
    }
    
    public boolean isImmediate() {
    	return true;
    }
    
    public RubyClass getSingletonClass() {
        throw getRuntime().newTypeError("can't define singleton");
    }

    public Class<?> getJavaClass() {
        return Long.TYPE;
    }

    public double getDoubleValue() {
        return value;
    }

    public long getLongValue() {
        return value;
    }

    public static RubyFixnum newFixnum(Ruby runtime, long value) {
        final int offset = 128;
        if (value <= 127 && value >= -128) {
            return runtime.fixnumCache[(int) value + offset];
        }
        return new RubyFixnum(runtime, value);
    }

    public RubyFixnum newFixnum(long newValue) {
        return newFixnum(getRuntime(), newValue);
    }

    public static RubyFixnum zero(Ruby runtime) {
        return newFixnum(runtime, 0);
    }

    public static RubyFixnum one(Ruby runtime) {
        return newFixnum(runtime, 1);
    }

    public static RubyFixnum minus_one(Ruby runtime) {
        return newFixnum(runtime, -1);
    }

    public RubyFixnum hash() {
        return newFixnum(hashCode());
    }

    public final int hashCode() {
        return (int)(value ^ value >>> 32);
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        
        if (other instanceof RubyFixnum) { 
            RubyFixnum num = (RubyFixnum)other;
            
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
    @JRubyMethod(name = "to_s", optional = 1)
    public RubyString to_s(IRubyObject[] args) {
        int base = args.length == 0 ? 10 : num2int(args[0]);
        if (base < 2 || base > 36) {
            throw getRuntime().newArgumentError("illegal radix " + base);
            }
        return getRuntime().newString(Convert.longToByteList(value, base));
        }

    /** fix_id2name
     * 
     */
    @JRubyMethod(name = "id2name")
    public IRubyObject id2name() {
        RubySymbol symbol = RubySymbol.getSymbolLong(getRuntime(), value);
        
        if (symbol != null) return getRuntime().newString(symbol.asSymbol());

        return getRuntime().getNil();
    }

    /** fix_to_sym
     * 
     */
    @JRubyMethod(name = "to_sym")
    public IRubyObject to_sym() {
        RubySymbol symbol = RubySymbol.getSymbolLong(getRuntime(), value);
        
        return symbol != null ? symbol : getRuntime().getNil(); 
    }

    /** fix_uminus
     * 
     */
    @JRubyMethod(name = "-@")
    public IRubyObject op_uminus() {
        if (value == MIN) { // a gotcha
            return RubyBignum.newBignum(getRuntime(), BigInteger.valueOf(value).negate());
        }
        return RubyFixnum.newFixnum(getRuntime(), -value);
        }

    /** fix_plus
     * 
     */
    @JRubyMethod(name = "+", required = 1)
    public IRubyObject op_plus(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            long otherValue = ((RubyFixnum) other).value;
            long result = value + otherValue;
            if ((~(value ^ otherValue) & (value ^ result) & SIGN_BIT) != 0) {
                return RubyBignum.newBignum(getRuntime(), value).op_plus(other);
            }
		return newFixnum(result);
    }
        if (other instanceof RubyBignum) {
            return ((RubyBignum) other).op_plus(this);
    }
        if (other instanceof RubyFloat) {
            return getRuntime().newFloat((double) value + ((RubyFloat) other).getDoubleValue());
        }
        return coerceBin("+", other);
    }

    /** fix_minus
     * 
     */
    @JRubyMethod(name = "-", required = 1)
    public IRubyObject op_minus(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            long otherValue = ((RubyFixnum) other).value;
            long result = value - otherValue;
            if ((~(value ^ ~otherValue) & (value ^ result) & SIGN_BIT) != 0) {
                return RubyBignum.newBignum(getRuntime(), value).op_minus(other);
    }
            return newFixnum(result);
        } else if (other instanceof RubyBignum) {
            return RubyBignum.newBignum(getRuntime(), value).op_minus(other);
        } else if (other instanceof RubyFloat) {
            return getRuntime().newFloat((double) value - ((RubyFloat) other).getDoubleValue());
        }
        return coerceBin("-", other);
    }

    /** fix_mul
     * 
     */
    @JRubyMethod(name = "*", required = 1)
    public IRubyObject op_mul(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            long otherValue = ((RubyFixnum) other).value;
            if (value == 0) {
                return RubyFixnum.zero(getRuntime());
            }
            long result = value * otherValue;
            IRubyObject r = newFixnum(getRuntime(),result);
            if(RubyNumeric.fix2long(r) != result || result/value != otherValue) {
                return (RubyNumeric) RubyBignum.newBignum(getRuntime(), value).op_mul(other);
            }
            return r;
        } else if (other instanceof RubyBignum) {
            return ((RubyBignum) other).op_mul(this);
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
    @JRubyMethod(name = "div", required = 1)
    public IRubyObject div_div(IRubyObject other) {
        return idiv(other, "div");
    }
    	
    @JRubyMethod(name = "/", required = 1)
    public IRubyObject op_div(IRubyObject other) {
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
    @JRubyMethod(name = {"%", "modulo"}, required = 1)
    public IRubyObject op_mod(IRubyObject other) {
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
    @JRubyMethod(name = "divmod", required = 1)
    public IRubyObject divmod(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            long x = value;
            long y = ((RubyFixnum) other).value;
            final Ruby runtime = getRuntime();

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
    @JRubyMethod(name = "quo", required = 1)
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
    @JRubyMethod(name = "**", required = 1)
    public IRubyObject op_pow(IRubyObject other) {
        if(other instanceof RubyFixnum) {
            long b = ((RubyFixnum) other).value;
            if (b == 0) {
                return RubyFixnum.one(getRuntime());
            }
            if (b == 1) {
                return this;
            }
            if (b > 0) {
                return RubyBignum.newBignum(getRuntime(), value).op_pow(other);
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
    @JRubyMethod(name = "abs")
    public IRubyObject abs() {
        if (value < 0) {
            return RubyFixnum.newFixnum(getRuntime(), -value);
        }
        return this;
    }
            
    /** fix_equal
     * 
     */
    @JRubyMethod(name = "==", required = 1)
    public IRubyObject op_equal(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return RubyBoolean.newBoolean(getRuntime(), value == ((RubyFixnum) other).value);
        }
        return super.op_equal(other);
            }

    /** fix_cmp
     * 
     */
    @JRubyMethod(name = "<=>", required = 1)
    public IRubyObject op_cmp(IRubyObject other) {
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
    @JRubyMethod(name = ">", required = 1)
    public IRubyObject op_gt(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return RubyBoolean.newBoolean(getRuntime(), value > ((RubyFixnum) other).value);
    }
        return coerceRelOp(">", other);
    }

    /** fix_ge
     * 
     */
    @JRubyMethod(name = ">=", required = 1)
    public IRubyObject op_ge(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return RubyBoolean.newBoolean(getRuntime(), value >= ((RubyFixnum) other).value);
            }
        return coerceRelOp(">=", other);
    }

    /** fix_lt
     * 
     */
    @JRubyMethod(name = "<", required = 1)
    public IRubyObject op_lt(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return RubyBoolean.newBoolean(getRuntime(), value < ((RubyFixnum) other).value);
        }
        return coerceRelOp("<", other);
    }
        
    /** fix_le
     * 
     */
    @JRubyMethod(name = "<=", required = 1)
    public IRubyObject op_le(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return RubyBoolean.newBoolean(getRuntime(), value <= ((RubyFixnum) other).value);
    }
        return coerceRelOp("<=", other);
    }

    /** fix_rev
     * 
     */
    @JRubyMethod(name = "~")
    public IRubyObject op_neg() {
        return newFixnum(~value);
    	}
    	
    /** fix_and
     * 
     */
    @JRubyMethod(name = "&", required = 1)
    public IRubyObject op_and(IRubyObject other) {
        if (other instanceof RubyBignum) {
            return ((RubyBignum) other).op_and(this);
    }
        return RubyFixnum.newFixnum(getRuntime(), value & num2long(other));
    }

    /** fix_or 
     * 
     */
    @JRubyMethod(name = "|", required = 1)
    public IRubyObject op_or(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return newFixnum(value | ((RubyFixnum) other).value);
        }
        if (other instanceof RubyBignum) {
            return ((RubyBignum) other).op_or(this);
        }
        if (other instanceof RubyNumeric) {
            return newFixnum(value | ((RubyNumeric) other).getLongValue());
        }
        
        return op_or(RubyFixnum.newFixnum(getRuntime(), num2long(other)));
    }

    /** fix_xor 
     * 
     */
    @JRubyMethod(name = "^", required = 1)
    public IRubyObject op_xor(IRubyObject other) {
        if(other instanceof RubyFixnum) {
            return newFixnum(value ^ ((RubyFixnum) other).value);
            }
        if (other instanceof RubyBignum) {
            return ((RubyBignum) other).op_xor(this);
        }
        if (other instanceof RubyNumeric) {
            return newFixnum(value ^ ((RubyNumeric) other).getLongValue());
        }

        return op_xor(RubyFixnum.newFixnum(getRuntime(), num2long(other)));
        }

    /** fix_aref 
     * 
     */
    @JRubyMethod(name = "[]", required = 1)
    public IRubyObject op_aref(IRubyObject other) {
        if(other instanceof RubyBignum) {
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
    @JRubyMethod(name = "<<", required = 1)
    public IRubyObject op_lshift(IRubyObject other) {
        long width = num2long(other);

            if (width < 0) {
            return op_rshift(RubyFixnum.newFixnum(getRuntime(), -width));
		    }
    	
        if (width == 0) {
            return this;
    }

        if (width > BIT_SIZE - 1 || ((~0L << BIT_SIZE - width - 1) & value) != 0) {
            return RubyBignum.newBignum(getRuntime(), value).op_lshift(other);
        }
        
        return newFixnum(value << width);
    }

    /** fix_rshift 
     * 
     */
    @JRubyMethod(name = ">>", required = 1)
    public IRubyObject op_rshift(IRubyObject other) {
        long width = num2long(other);

        if (width < 0) {
            return op_lshift(RubyFixnum.newFixnum(getRuntime(), -width));
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
    @JRubyMethod(name = "to_f")
    public IRubyObject to_f() {
        return RubyFloat.newFloat(getRuntime(), (double) value);
    }

    /** fix_size 
     * 
     */
    @JRubyMethod(name = "size")
    public IRubyObject size() {
        return newFixnum((long) ((BIT_SIZE + 7) / 8));
        }

    /** fix_zero_p 
     * 
     */
    @JRubyMethod(name = "zero?")
    public IRubyObject zero_p() {
        return RubyBoolean.newBoolean(getRuntime(), value == 0);
    }

    @JRubyMethod(name = "id")
    public IRubyObject id() {
        if (value <= Long.MAX_VALUE / 2 && value >= Long.MIN_VALUE / 2)
            return newFixnum(2 * value + 1);
        return super.id();
    }

    public IRubyObject taint() {
        return this;
    }

    public IRubyObject freeze() {
        return this;
    }
    
    // Piece of mri rb_to_id
    public String asSymbol() {
        getRuntime().getWarnings().warn("do not use Fixnums as Symbols");
        
        // FIXME: I think this chunk is equivalent to MRI id2name (and not our public method 
        // id2name).  Make into method if used more than once.  
        RubySymbol symbol = RubySymbol.getSymbolLong(getRuntime(), value);
        
        if (symbol != null) {
            throw getRuntime().newArgumentError("" + value + " is not a symbol");
        }
        
        return symbol.asSymbol();
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
    @JRubyMethod(name = "induced_from", required = 1, meta = true)
    public static IRubyObject induced_from(IRubyObject recv, IRubyObject other) {
        return RubyNumeric.num2fix(other);
    }
}
