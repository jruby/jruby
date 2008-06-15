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
import java.util.HashMap;
import java.util.Map;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.java.MiniJava;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.Convert;
import org.jruby.util.TypeCoercer;

/** 
 * Implementation of the Fixnum class.
 */
@JRubyClass(name="Fixnum", parent="Integer", include="Precision")
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

        fixnum.includeModule(runtime.getPrecision());
        
        fixnum.defineAnnotatedMethods(RubyFixnum.class);
        
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

    private static IRubyObject fixCoerce(IRubyObject x) {
        do {
            x = x.convertToInteger();
        } while (!(x instanceof RubyFixnum) && !(x instanceof RubyBignum));
        return x;
    }
    
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

    private static final int CACHE_OFFSET = 128;
    
    public static RubyFixnum newFixnum(Ruby runtime, long value) {
        if (isInCacheRange(value)) {
            return runtime.fixnumCache[(int) value + CACHE_OFFSET];
        }
        return new RubyFixnum(runtime, value);
    }
    
    private static boolean isInCacheRange(long value) {
        return value <= 127 && value >= -128;
    }

    public RubyFixnum newFixnum(long newValue) {
        return newFixnum(getRuntime(), newValue);
    }

    public static RubyFixnum zero(Ruby runtime) {
        return runtime.fixnumCache[CACHE_OFFSET];
    }

    public static RubyFixnum one(Ruby runtime) {
        return runtime.fixnumCache[CACHE_OFFSET + 1];
    }
    
    public static RubyFixnum two(Ruby runtime) {
        return runtime.fixnumCache[CACHE_OFFSET + 2];
    }
    
    public static RubyFixnum three(Ruby runtime) {
        return runtime.fixnumCache[CACHE_OFFSET + 3];
    }
    
    public static RubyFixnum four(Ruby runtime) {
        return runtime.fixnumCache[CACHE_OFFSET + 4];
    }
    
    public static RubyFixnum five(Ruby runtime) {
        return runtime.fixnumCache[CACHE_OFFSET + 5];
    }

    public static RubyFixnum minus_one(Ruby runtime) {
        return runtime.fixnumCache[CACHE_OFFSET - 1];
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
    @JRubyMethod(optional = 1)
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
    @JRubyMethod
    public IRubyObject id2name() {
        RubySymbol symbol = RubySymbol.getSymbolLong(getRuntime(), value);
        
        if (symbol != null) return getRuntime().newString(symbol.asJavaString());

        return getRuntime().getNil();
    }

    /** fix_to_sym
     * 
     */
    @JRubyMethod
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
    @JRubyMethod(name = "+")
    public IRubyObject op_plus(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return addFixnum(context, (RubyFixnum)other);
        }
        return addOther(context, other);
    }
    
    private IRubyObject addFixnum(ThreadContext context, RubyFixnum other) {
        long otherValue = other.value;
        long result = value + otherValue;
        if ((~(value ^ otherValue) & (value ^ result) & SIGN_BIT) != 0) {
            return addAsBignum(context, other);
        }
        return newFixnum(result);
    }
    
    private IRubyObject addAsBignum(ThreadContext context, RubyFixnum other) {
        return RubyBignum.newBignum(getRuntime(), value).op_plus(context, other);
    }
    
    private IRubyObject addOther(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyBignum) {
            return ((RubyBignum) other).op_plus(context, this);
        }
        if (other instanceof RubyFloat) {
            return getRuntime().newFloat((double) value + ((RubyFloat) other).getDoubleValue());
        }
        return coerceBin(context, "+", other);
    }

    /** fix_minus
     * 
     */
    @JRubyMethod(name = "-")
    public IRubyObject op_minus(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return subtractFixnum(context, (RubyFixnum)other);
        }
        return subtractOther(context, other);
    }
    
    private IRubyObject subtractFixnum(ThreadContext context, RubyFixnum other) {
        long otherValue = other.value;
        long result = value - otherValue;
        if ((~(value ^ ~otherValue) & (value ^ result) & SIGN_BIT) != 0) {
            return subtractAsBignum(context, other);
        }
        return newFixnum(result);
    }
    
    private IRubyObject subtractAsBignum(ThreadContext context, RubyFixnum other) {
        return RubyBignum.newBignum(getRuntime(), value).op_minus(context, other);
    }
    
    private IRubyObject subtractOther(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyBignum) {
            return RubyBignum.newBignum(getRuntime(), value).op_minus(context, other);
        } else if (other instanceof RubyFloat) {
            return getRuntime().newFloat((double) value - ((RubyFloat) other).getDoubleValue());
        }
        return coerceBin(context, "-", other);
    }

    /** fix_mul
     * 
     */
    @JRubyMethod(name = "*")
    public IRubyObject op_mul(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            long otherValue = ((RubyFixnum) other).value;
            if (value == 0) {
                return RubyFixnum.zero(getRuntime());
            }
            long result = value * otherValue;
            IRubyObject r = newFixnum(getRuntime(),result);
            if(RubyNumeric.fix2long(r) != result || result/value != otherValue) {
                return (RubyNumeric) RubyBignum.newBignum(getRuntime(), value).op_mul(context, other);
            }
            return r;
        } else if (other instanceof RubyBignum) {
            return ((RubyBignum) other).op_mul(context, this);
        } else if (other instanceof RubyFloat) {
            return getRuntime().newFloat((double) value * ((RubyFloat) other).getDoubleValue());
        }
        return coerceBin(context, "*", other);
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
    @JRubyMethod(name = "div")
    public IRubyObject div_div(ThreadContext context, IRubyObject other) {
        return idiv(context, other, "div");
    }
    	
    @JRubyMethod(name = "/")
    public IRubyObject op_div(ThreadContext context, IRubyObject other) {
        return idiv(context, other, "/");
    }

    @JRubyMethod(name = {"odd?"})
    public RubyBoolean odd_p() {
        if(value%2 != 0) {
            return getRuntime().getTrue();
        }
        return getRuntime().getFalse();
    }

    @JRubyMethod(name = {"even?"})
    public RubyBoolean even_p() {
        if(value%2 == 0) {
            return getRuntime().getTrue();
        }
        return getRuntime().getFalse();
    }

    @JRubyMethod
    public IRubyObject pred() {
        return getRuntime().newFixnum(value-1);
    }

    public IRubyObject idiv(ThreadContext context, IRubyObject other, String method) {
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
        return coerceBin(context, method, other);
    }
        
    /** fix_mod
     * 
     */
    @JRubyMethod(name = {"%", "modulo"})
    public IRubyObject op_mod(ThreadContext context, IRubyObject other) {
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
        return coerceBin(context, "%", other);
    }
                
    /** fix_divmod
     * 
     */
    @JRubyMethod
    public IRubyObject divmod(ThreadContext context, IRubyObject other) {
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
        return coerceBin(context, "divmod", other);
    }
    	
    /** fix_quo
     * 
     */
    @JRubyMethod
    public IRubyObject quo(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return RubyFloat.newFloat(getRuntime(), (double) value / (double) ((RubyFixnum) other).value);
        }
        return coerceBin(context, "quo", other);
    }

    /** fix_pow 
     * 
     */
    @JRubyMethod(name = "**")
    public IRubyObject op_pow(ThreadContext context, IRubyObject other) {
        if(other instanceof RubyFixnum) {
            long b = ((RubyFixnum) other).value;
            if (b == 0) {
                return RubyFixnum.one(getRuntime());
            }
            if (b == 1) {
                return this;
            }
            if (b > 0) {
                return RubyBignum.newBignum(getRuntime(), value).op_pow(context, other);
            }
            return RubyFloat.newFloat(getRuntime(), Math.pow(value, b));
        } else if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), Math.pow(value, ((RubyFloat) other)
                    .getDoubleValue()));
        }
        return coerceBin(context, "**", other);
    }
            
    /** fix_abs
     * 
     */
    @JRubyMethod
    public IRubyObject abs() {
        if (value < 0) {
            // A gotcha for Long.MIN_VALUE: value = -value
            if (value == Long.MIN_VALUE) {
                return RubyBignum.newBignum(
                        getRuntime(), BigInteger.valueOf(value).negate());
            }
            return RubyFixnum.newFixnum(getRuntime(), -value);
        }
        return this;
    }
            
    /** fix_equal
     * 
     */
    @JRubyMethod(name = "==")
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return RubyBoolean.newBoolean(getRuntime(), value == ((RubyFixnum) other).value);
        }
        return super.op_num_equal(context, other);
    }

    /** fix_cmp
     * 
     */
    @JRubyMethod(name = "<=>")
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return compareFixnum(context, (RubyFixnum)other);
        }
        return coerceCmp(context, "<=>", other);
    }
    
    private IRubyObject compareFixnum(ThreadContext context, RubyFixnum other) {
        long otherValue = ((RubyFixnum) other).value;
        if (value == otherValue) {
            return RubyFixnum.zero(getRuntime());
        }
        if (value > otherValue) {
            return RubyFixnum.one(getRuntime());
        }
        return RubyFixnum.minus_one(getRuntime());
    }

    /** fix_gt
     * 
     */
    @JRubyMethod(name = ">")
    public IRubyObject op_gt(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return RubyBoolean.newBoolean(getRuntime(), value > ((RubyFixnum) other).value);
    }
        return coerceRelOp(context, ">", other);
    }

    /** fix_ge
     * 
     */
    @JRubyMethod(name = ">=")
    public IRubyObject op_ge(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return RubyBoolean.newBoolean(getRuntime(), value >= ((RubyFixnum) other).value);
            }
        return coerceRelOp(context, ">=", other);
    }

    /** fix_lt
     * 
     */
    @JRubyMethod(name = "<")
    public IRubyObject op_lt(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return RubyBoolean.newBoolean(getRuntime(), value < ((RubyFixnum) other).value);
        }
        return coerceRelOp(context, "<", other);
    }
        
    /** fix_le
     * 
     */
    @JRubyMethod(name = "<=")
    public IRubyObject op_le(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return RubyBoolean.newBoolean(getRuntime(), value <= ((RubyFixnum) other).value);
    }
        return coerceRelOp(context, "<=", other);
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
    @JRubyMethod(name = "&")
    public IRubyObject op_and(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum || (other = fixCoerce(other)) instanceof RubyFixnum) {
            return newFixnum(value & ((RubyFixnum) other).value);
        }
        return ((RubyBignum) other).op_and(context, this);
    }

    /** fix_or 
     * 
     */
    @JRubyMethod(name = "|")
    public IRubyObject op_or(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum || (other = fixCoerce(other)) instanceof RubyFixnum) {
            return newFixnum(value | ((RubyFixnum) other).value);
        }
        return ((RubyBignum) other).op_or(context, this);
    }

    /** fix_xor 
     * 
     */
    @JRubyMethod(name = "^")
    public IRubyObject op_xor(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum || (other = fixCoerce(other)) instanceof RubyFixnum) {
            return newFixnum(value ^ ((RubyFixnum) other).value);
        }
        return ((RubyBignum) other).op_xor(context, this); 
    }

    /** fix_aref 
     * 
     */
    @JRubyMethod(name = "[]")
    public IRubyObject op_aref(IRubyObject other) {
        if(!(other instanceof RubyFixnum) && !((other = fixCoerce(other)) instanceof RubyFixnum)) {
            RubyBignum big = (RubyBignum) other;
            RubyObject tryFix = RubyBignum.bignorm(getRuntime(), big.getValue());
            if (!(tryFix instanceof RubyFixnum)) {
                return big.getValue().signum() == 0 || value >= 0 ? RubyFixnum.zero(getRuntime()) : RubyFixnum.one(getRuntime());
            }
        }

        long otherValue = fix2long(other);

        if (otherValue < 0) return RubyFixnum.zero(getRuntime());

        if (BIT_SIZE - 1 < otherValue) {
            return value < 0 ? RubyFixnum.one(getRuntime()) : RubyFixnum.zero(getRuntime());
        }

        return (value & (1L << otherValue)) == 0 ? RubyFixnum.zero(getRuntime()) : RubyFixnum.one(getRuntime());
    }

    /** fix_lshift 
     * 
     */
    @JRubyMethod(name = "<<")
    public IRubyObject op_lshift(IRubyObject other) {
        if (!(other instanceof RubyFixnum)) return RubyBignum.newBignum(getRuntime(), value).op_lshift(other);

        long width = ((RubyFixnum)other).getLongValue();

        return width < 0 ? rshift(-width) : lshift(width); 
    }
    
    private IRubyObject lshift(long width) {
        if (width > BIT_SIZE - 1 || ((~0L << BIT_SIZE - width - 1) & value) != 0) {
            return RubyBignum.newBignum(getRuntime(), value).op_lshift(RubyFixnum.newFixnum(getRuntime(), width));
        }
        return RubyFixnum.newFixnum(getRuntime(), value << width);
    }

    /** fix_rshift 
     * 
     */
    @JRubyMethod(name = ">>")
    public IRubyObject op_rshift(IRubyObject other) {
        if (!(other instanceof RubyFixnum)) return RubyBignum.newBignum(getRuntime(), value).op_rshift(other);

        long width = ((RubyFixnum)other).getLongValue();

        if (width == 0) return this;

        return width < 0 ? lshift(-width) : rshift(width);  
    }
    
    private IRubyObject rshift(long width) { 
        if (width >= BIT_SIZE - 1) {
            return value < 0 ? RubyFixnum.minus_one(getRuntime()) : RubyFixnum.zero(getRuntime()); 
        }
        return RubyFixnum.newFixnum(getRuntime(), value >> width);
    }

    /** fix_to_f 
     * 
     */
    @JRubyMethod
    public IRubyObject to_f() {
        return RubyFloat.newFloat(getRuntime(), (double) value);
    }

    /** fix_size 
     * 
     */
    @JRubyMethod
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

    @JRubyMethod
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
    public String asJavaString() {
        getRuntime().getWarnings().warn(ID.FIXNUMS_NOT_SYMBOLS, "do not use Fixnums as Symbols");
        
        // FIXME: I think this chunk is equivalent to MRI id2name (and not our public method 
        // id2name).  Make into method if used more than once.  
        RubySymbol symbol = RubySymbol.getSymbolLong(getRuntime(), value);
        
        if (symbol == null) {
            throw getRuntime().newArgumentError("" + value + " is not a symbol");
        }
        
        return symbol.asJavaString();
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
    @JRubyMethod(meta = true)
    public static IRubyObject induced_from(IRubyObject recv, IRubyObject other) {
        return RubyNumeric.num2fix(other);
    }

    @Override
    public IRubyObject to_java() {
        return MiniJava.javaToRuby(getRuntime(), Long.valueOf(value));
    }

    @Override
    public IRubyObject as(Class javaClass) {
        return MiniJava.javaToRuby(getRuntime(), coerceToJavaType(getRuntime(), this, javaClass));
    }
    
    private static Object coerceToJavaType(Ruby ruby, RubyFixnum self, Class javaClass) {
        if (!Number.class.isAssignableFrom(javaClass)) {
            throw ruby.newTypeError(javaClass.getCanonicalName() + " is not a numeric type");
        }
        
        TypeCoercer coercer = JAVA_COERCERS.get(javaClass);
        
        if (coercer == null) {
            throw ruby.newTypeError("Cannot coerce Fixnum to " + javaClass.getCanonicalName());
        }
        
        return coercer.coerce(self);
    }
    
    private static final Map<Class, TypeCoercer> JAVA_COERCERS = new HashMap<Class, TypeCoercer>();
    
    static {
        TypeCoercer intCoercer = new TypeCoercer() {
            public Object coerce(IRubyObject self) {
                RubyFixnum fixnum = (RubyFixnum)self;
                
                if (fixnum.value > Integer.MAX_VALUE) {
                    throw self.getRuntime().newRangeError("Fixnum " + fixnum.value + " is too large for Java int");
                }
                
                return Integer.valueOf((int)fixnum.value);
            }
        };
        JAVA_COERCERS.put(int.class, intCoercer);
        JAVA_COERCERS.put(Integer.class, intCoercer);
    }
}
