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

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;

/** Implementation of the Fixnum class.
 *
 * @author jpetersen
 */
public class RubyFixnum extends RubyInteger {
    private long value;
    private static final int BIT_SIZE = 64;
    public static final long MAX = (1L<<(BIT_SIZE - 2)) - 1;
    public static final long MIN = -1 * MAX - 1;
    private static final long MAX_MARSHAL_FIXNUM = (1L << 30) - 1;

    public RubyFixnum(IRuby runtime) {
        this(runtime, 0);
    }

    public RubyFixnum(IRuby runtime, long value) {
        super(runtime, runtime.getClass("Fixnum"));
        this.value = value;
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

    public static RubyFixnum zero(IRuby runtime) {
        return runtime.newFixnum(0);
    }

    public static RubyFixnum one(IRuby runtime) {
        return runtime.newFixnum(1);
    }

    public static RubyFixnum minus_one(IRuby runtime) {
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

    public static RubyFixnum newFixnum(IRuby runtime, long value) {
        RubyFixnum fixnum;
        if (value >= 0 && value < runtime.getFixnumCache().length) {
            fixnum = runtime.getFixnumCache()[(int) value];
            if (fixnum == null) {
                fixnum = new RubyFixnum(runtime, value);
                runtime.getFixnumCache()[(int) value] = fixnum;
            }
        } else {
            fixnum = new RubyFixnum(runtime, value);
        }
        return fixnum;
    }

    public RubyFixnum newFixnum(long newValue) {
        return getRuntime().newFixnum(newValue);
    }

    public boolean singletonMethodsAllowed() {
        return false;
    }

    public RubyNumeric multiplyWith(RubyFixnum other) {
        long otherValue = other.getLongValue();
        if (otherValue == 0) {
            return RubyFixnum.zero(getRuntime());
        }
        long result = value * otherValue;
        if (result > MAX || result < MIN || result / otherValue != value) {
            return (RubyNumeric) RubyBignum.newBignum(getRuntime(), getLongValue()).op_mul(other);
        }
		return newFixnum(result);
    }

    public RubyNumeric multiplyWith(RubyInteger other) {
        return other.multiplyWith(this);
    }

    public RubyNumeric multiplyWith(RubyFloat other) {
       return other.multiplyWith(RubyFloat.newFloat(getRuntime(), getLongValue()));
    }

    public RubyNumeric quo(IRubyObject other) {
        return new RubyFloat(getRuntime(), ((RubyNumeric) op_div(other)).getDoubleValue());
    }
    
    public IRubyObject op_and(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
            return newFixnum(value & ((RubyNumeric) other).getTruncatedLongValue());
    	}
    	
    	return callCoerced("&", other);
    }

    public IRubyObject op_div(IRubyObject other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_div(other);
        } else if (other instanceof RubyBignum) {
            return RubyBignum.newBignum(getRuntime(), getLongValue()).op_div(other);
        } else if (other instanceof RubyNumeric) {
            // Java / and % are not the same as ruby
            long x = getLongValue();
            long y = ((RubyNumeric) other).getLongValue();
            
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
        
        return callCoerced("/", other);
    }

    public IRubyObject op_lshift(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
            long width = ((RubyNumeric) other).getLongValue();
            if (width < 0) {
                return op_rshift(((RubyNumeric) other).op_uminus());
            }
            if (value > 0) {
                if (width >= BIT_SIZE - 2 || value >> (BIT_SIZE - width) > 0) {
                    RubyBignum bigValue = 
                        RubyBignum.newBignum(getRuntime(), RubyBignum.bigIntValue(this));
                
                    return bigValue.op_lshift(other);
	            }
            } else {
	            if (width >= BIT_SIZE - 1 || value >> (BIT_SIZE - width) < -1) {
                    RubyBignum bigValue = 
                        RubyBignum.newBignum(getRuntime(), RubyBignum.bigIntValue(this));
                
                    return bigValue.op_lshift(other);
                }
            }

            return newFixnum(value << width);
    	}
    	
    	return callCoerced("<<", other);
    }

    public IRubyObject op_minus(IRubyObject other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_minus(other);
        } else if (other instanceof RubyBignum) {
            return RubyBignum.newBignum(getRuntime(), value).op_minus(other);
        } else {
            long otherValue = ((RubyNumeric) other).getLongValue();
            long result = value - otherValue;
            if ((value <= 0 && otherValue >= 0 && (result > 0 || result < -MAX)) || 
		(value >= 0 && otherValue <= 0 && (result < 0 || result > MAX))) {
                return RubyBignum.newBignum(getRuntime(), value).op_minus(other);
            }
            return newFixnum(result);
        }
    }

    public IRubyObject op_mod(IRubyObject other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_mod(other);
        } else if (other instanceof RubyBignum) {
            return RubyBignum.newBignum(getRuntime(), getLongValue()).op_mod(other);
        } else if (other instanceof RubyNumeric) {
	        // Java / and % are not the same as ruby
            long x = getLongValue();
            long y = ((RubyNumeric) other).getLongValue();
            long mod = x % y;

            if (mod < 0 && y > 0 || mod > 0 && y < 0) {
                mod += y;
            }

            return getRuntime().newFixnum(mod);
        }
        
        return (RubyNumeric) callCoerced("%", other);
    }

    public IRubyObject op_mul(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
        	return ((RubyNumeric) other).multiplyWith(this);
    	}
    	
    	return callCoerced("*", other);
    }

    public IRubyObject op_or(IRubyObject other) {
        if (other instanceof RubyBignum) {
            return (RubyInteger) other.callMethod("|", this);
        } else if (other instanceof RubyNumeric) {
            return newFixnum(value | ((RubyNumeric) other).getLongValue());
        }
        
        return (RubyInteger) callCoerced("|", other);
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
                return RubyBignum.newBignum(getRuntime(), value).op_plus(other);
            }
            return newFixnum(result);
        }
        return callCoerced("+", other);
    }

    public IRubyObject op_pow(IRubyObject other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_pow(other);
        } else if (other instanceof RubyNumeric) {
            long longValue = ((RubyNumeric) other).getLongValue();
            
		    if (longValue == 0) {
		        return getRuntime().newFixnum(1);
		    } else if (longValue == 1) {
		        return this;
		    } else if (longValue > 1) {
		        return RubyBignum.newBignum(getRuntime(), getLongValue()).op_pow(other);
		    } else {
		        return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_pow(other);
		    }
        }
        
        return callCoerced("**", other);
    }

    public IRubyObject op_rshift(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
            long width = ((RubyNumeric) other).getLongValue();
            if (width < 0) {
			    return op_lshift(((RubyNumeric) other).op_uminus());
		    }
            return newFixnum(value >> width);
    	}
    	
    	return callCoerced(">>", other);
    }

    public IRubyObject op_xor(IRubyObject other) {
        if (other instanceof RubyBignum) {
            return (RubyInteger) other.callMethod("^", this);
        } else if (other instanceof RubyNumeric) {
            return newFixnum(value ^ ((RubyNumeric) other).getLongValue());
        }
        
        return callCoerced("^", other);
    }

    public RubyString to_s(IRubyObject[] args) {
    	checkArgumentCount(args, 0, 1);

    	int radix = args.length == 0 ? 10 : (int) args[0].convertToInteger().getLongValue();
        
        return getRuntime().newString(Long.toString(getLongValue(), radix));
    }
    
    public RubyFloat to_f() {
        return RubyFloat.newFloat(getRuntime(), getDoubleValue());
    }

    public RubyFixnum size() {
        return newFixnum((long) Math.ceil(BIT_SIZE / 8.0));
    }

    public RubyFixnum aref(IRubyObject other) {
        long position = other.convertToInteger().getLongValue();

        // Seems mighty expensive to keep creating over and over again.
        // How else can this be done though?
        if (position > BIT_SIZE) {
            return RubyBignum.newBignum(getRuntime(), value).aref(other);
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
            getRuntime().getCurrentContext().yield(newFixnum(i));
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
