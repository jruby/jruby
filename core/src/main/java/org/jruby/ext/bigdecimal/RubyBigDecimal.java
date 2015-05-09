/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * Copyright (C) 2009 Joseph LaFata <joe@quibb.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.bigdecimal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import static org.jruby.RubyNumeric.num2int;
import org.jruby.RubyObject;
import org.jruby.RubyRational;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyConstant;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.runtime.builtin.IRubyObject.NULL_ARRAY;
import org.jruby.util.Numeric;
import org.jruby.util.SafeDoubleParser;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
@JRubyClass(name="BigDecimal", parent="Numeric")
public class RubyBigDecimal extends RubyNumeric {
    private static final ObjectAllocator BIGDECIMAL_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyBigDecimal(runtime, klass);
        }
    };

    @JRubyConstant
    public final static int ROUND_DOWN = BigDecimal.ROUND_DOWN;
    @JRubyConstant
    public final static int ROUND_CEILING = BigDecimal.ROUND_CEILING;
    @JRubyConstant
    public final static int ROUND_UP = BigDecimal.ROUND_UP;
    @JRubyConstant
    public final static int ROUND_HALF_DOWN = BigDecimal.ROUND_HALF_DOWN;
    @JRubyConstant
    public final static int ROUND_HALF_EVEN = BigDecimal.ROUND_HALF_EVEN;
    @JRubyConstant
    public final static int ROUND_HALF_UP = BigDecimal.ROUND_HALF_UP;
    @JRubyConstant
    public final static int ROUND_FLOOR = BigDecimal.ROUND_FLOOR;

    @JRubyConstant
    public final static int SIGN_POSITIVE_INFINITE = 3;
    @JRubyConstant
    public final static int EXCEPTION_OVERFLOW = 8;
    @JRubyConstant
    public final static int SIGN_POSITIVE_ZERO = 1;
    @JRubyConstant
    public final static int EXCEPTION_ALL = 255;
    @JRubyConstant
    public final static int SIGN_NEGATIVE_FINITE = -2;
    @JRubyConstant
    public final static int EXCEPTION_UNDERFLOW = 4;
    @JRubyConstant
    public final static int SIGN_NaN = 0;
    @JRubyConstant
    public final static int BASE = 10000;
    @JRubyConstant
    public final static int ROUND_MODE = 256;
    @JRubyConstant
    public final static int SIGN_POSITIVE_FINITE = 2;
    @JRubyConstant
    public final static int EXCEPTION_INFINITY = 1;
    @JRubyConstant
    public final static int SIGN_NEGATIVE_INFINITE = -3;
    @JRubyConstant
    public final static int EXCEPTION_ZERODIVIDE = 1;
    @JRubyConstant
    public final static int SIGN_NEGATIVE_ZERO = -1;
    @JRubyConstant
    public final static int EXCEPTION_NaN = 2;
    
    // Static constants
    private static final BigDecimal TWO = new BigDecimal(2);
    private static final double SQRT_10 = 3.162277660168379332;
    
    public static RubyClass createBigDecimal(Ruby runtime) {
        try {
        RubyClass bigDecimal = runtime.defineClass("BigDecimal", runtime.getNumeric(), BIGDECIMAL_ALLOCATOR);

        runtime.getKernel().defineAnnotatedMethods(BigDecimalKernelMethods.class);

        bigDecimal.setInternalModuleVariable("vpPrecLimit", RubyFixnum.zero(runtime));
        bigDecimal.setInternalModuleVariable("vpExceptionMode", RubyFixnum.zero(runtime));
        bigDecimal.setInternalModuleVariable("vpRoundingMode", runtime.newFixnum(ROUND_HALF_UP));
        
        bigDecimal.defineAnnotatedMethods(RubyBigDecimal.class);
        bigDecimal.defineAnnotatedConstants(RubyBigDecimal.class);

        RubyModule bigMath = runtime.defineModule("BigMath");
        // TODO: BigMath.exp and BigMath.pow in native code

        bigDecimal.defineConstant("NAN", newNaN(runtime));
        bigDecimal.defineConstant("INFINITY", newInfinity(runtime, 1));

        return bigDecimal;
        } catch (Exception e) {
            System.out.println("E:" + e);
            e.printStackTrace();
        }
        return null;
    }

    private boolean isNaN;
    private int infinitySign;
    private int zeroSign;
    private BigDecimal value;

    public BigDecimal getValue() {
        return value;
    }

    public RubyBigDecimal(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
        this.isNaN = false;
        this.infinitySign = 0;
        this.zeroSign = 0;
    }

    public RubyBigDecimal(Ruby runtime, BigDecimal value) {
        super(runtime, runtime.getClass("BigDecimal"));
        this.isNaN = false;
        this.infinitySign = 0;
        this.zeroSign = 0;
        this.value = value;
    }

    public RubyBigDecimal(Ruby runtime, RubyClass klass, BigDecimal value) {
        super(runtime, klass);
        this.isNaN = false;
        this.infinitySign = 0;
        this.zeroSign = 0;
        this.value = value;
    }

    public RubyBigDecimal(Ruby runtime, BigDecimal value, int infinitySign) {
        super(runtime, runtime.getClass("BigDecimal"));
        this.isNaN = false;
        this.infinitySign = infinitySign;
        this.zeroSign = 0;
        this.value = value;
    }

    public RubyBigDecimal(Ruby runtime, BigDecimal value, int infinitySign, int zeroSign) {
        super(runtime, runtime.getClass("BigDecimal"));
        this.isNaN = false;
        this.infinitySign = infinitySign;
        this.zeroSign = zeroSign;
        this.value = value;
    }

    public RubyBigDecimal(Ruby runtime, BigDecimal value, boolean isNan) {
        super(runtime, runtime.getClass("BigDecimal"));
        this.isNaN = isNan;
        this.infinitySign = 0;
        this.zeroSign = 0;
        this.value = value;
    }

    public RubyBigDecimal(Ruby runtime, RubyBigDecimal rbd) {
        this(runtime, runtime.getClass("BigDecimal"), rbd);
    }

    public RubyBigDecimal(Ruby runtime, RubyClass klass, RubyBigDecimal rbd) {
        super(runtime, klass);
        this.isNaN = rbd.isNaN;
        this.infinitySign = rbd.infinitySign;
        this.zeroSign = rbd.zeroSign;
        this.value = rbd.value;
    }
    
    public static class BigDecimalKernelMethods {
        @JRubyMethod(name = "BigDecimal", required = 1, optional = 1, module = true, visibility = Visibility.PRIVATE)
        public static IRubyObject newBigDecimal(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
            if (args.length == 1) return newInstance(context, context.runtime.getClass("BigDecimal"), args[0]);

            return newInstance(context, context.runtime.getClass("BigDecimal"), args[0], args[1]);
        }
    }

    @JRubyMethod(meta = true)
    public static IRubyObject ver(ThreadContext context, IRubyObject recv) {
        return context.runtime.newString("1.0.1");
    }

    @JRubyMethod
    public IRubyObject _dump(ThreadContext context) {
        return RubyString.newUnicodeString(context.runtime, "0:").append(asString());
    }
    
    @JRubyMethod
    public IRubyObject _dump(ThreadContext context, IRubyObject unused) {
        return RubyString.newUnicodeString(context.runtime, "0:").append(asString());
    }    
        
    @JRubyMethod(meta = true)
    public static RubyBigDecimal _load(ThreadContext context, IRubyObject recv, IRubyObject from) {
        RubyBigDecimal instance = (RubyBigDecimal) (((RubyClass) recv).allocate());
        String precisionAndValue = from.convertToString().asJavaString();
        String value = precisionAndValue.substring(precisionAndValue.indexOf(":")+1);
        instance.value = new BigDecimal(value);
        return instance;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject double_fig(ThreadContext context, IRubyObject recv) {
        return context.runtime.newFixnum(20);
    }
    
    /**
     * Retrieve vpPrecLimit.
     */
    @JRubyMethod(meta = true)
    public static IRubyObject limit(ThreadContext context, IRubyObject recv) {
        return ((RubyModule) recv).searchInternalModuleVariable("vpPrecLimit");
    }

    /**
     * Set new vpPrecLimit if Fixnum and return the old value.
     */
    @JRubyMethod(meta = true)
    public static IRubyObject limit(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        IRubyObject old = limit(context, recv);

        if (arg.isNil()) return old;
        if (!(arg instanceof RubyFixnum)) throw context.runtime.newTypeError(arg, context.runtime.getFixnum());
        if (0 > ((RubyFixnum)arg).getLongValue()) throw context.runtime.newArgumentError("argument must be positive");

        ((RubyModule) recv).setInternalModuleVariable("vpPrecLimit", arg);

        return old;
    }
        
    @JRubyMethod(meta = true)
    public static IRubyObject save_limit(ThreadContext context, IRubyObject recv, Block block) {
        return modeExecute("vpPrecLimit", context, (RubyModule) recv, block);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject save_exception_mode(ThreadContext context, IRubyObject recv, Block block) {
        return modeExecute("vpExceptionMode", context, (RubyModule) recv, block);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject save_rounding_mode(ThreadContext context, IRubyObject recv, Block block) {
        return modeExecute("vpRoundingMode", context, (RubyModule) recv, block);
    }
    
    private static IRubyObject modeExecute(String modeName, ThreadContext context, RubyModule mod, Block block) {
        IRubyObject savedMode = mod.searchInternalModuleVariable(modeName);

        try {
            return block.yieldSpecific(context);
        } finally {
            mod.setInternalModuleVariable(modeName, savedMode);
        }        
    }

    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject mode(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        // FIXME: I doubt any of the constants referenced in this method
        // are ever redefined -- should compare to the known values, rather
        // than do an expensive constant lookup.
        RubyClass clazz = context.runtime.getClass("BigDecimal");
        RubyModule c = (RubyModule)recv;
        
        args = Arity.scanArgs(context.runtime, args, 1, 1);
        
        IRubyObject mode = args[0];
        IRubyObject value = args[1];
        
        if (!(mode instanceof RubyFixnum)) {
            throw context.runtime.newTypeError("wrong argument type " + mode.getMetaClass() + " (expected Fixnum)");
        }
        
        long longMode = ((RubyFixnum)mode).getLongValue();
        long _EXCEPTION_ALL =  bigDecimalConst(context.runtime, "EXCEPTION_ALL");
        if ((longMode & _EXCEPTION_ALL) != 0) {     
            if (value.isNil()) return c.searchInternalModuleVariable("vpExceptionMode");
            if (!(value instanceof RubyBoolean)) throw context.runtime.newArgumentError("second argument must be true or false");

            RubyFixnum currentExceptionMode = (RubyFixnum)c.searchInternalModuleVariable("vpExceptionMode");
            RubyFixnum newExceptionMode = new RubyFixnum(context.runtime, currentExceptionMode.getLongValue());
            
            RubyFixnum _EXCEPTION_INFINITY = (RubyFixnum)clazz.getConstant("EXCEPTION_INFINITY");
            if ((longMode & _EXCEPTION_INFINITY.getLongValue()) != 0) {
                newExceptionMode = (value.isTrue()) ? (RubyFixnum)currentExceptionMode.callCoerced(context, "|", _EXCEPTION_INFINITY)
                        : (RubyFixnum)currentExceptionMode.callCoerced(context, "&", new RubyFixnum(context.runtime, ~(_EXCEPTION_INFINITY).getLongValue()));
            }
            
            RubyFixnum _EXCEPTION_NaN = (RubyFixnum)clazz.getConstant("EXCEPTION_NaN");
            if ((longMode & _EXCEPTION_NaN.getLongValue()) != 0) {
                newExceptionMode = (value.isTrue()) ? (RubyFixnum)currentExceptionMode.callCoerced(context, "|", _EXCEPTION_NaN)
                        : (RubyFixnum)currentExceptionMode.callCoerced(context, "&", new RubyFixnum(context.runtime, ~(_EXCEPTION_NaN).getLongValue()));
            }
            
            RubyFixnum _EXCEPTION_UNDERFLOW = (RubyFixnum)clazz.getConstant("EXCEPTION_UNDERFLOW");
            if ((longMode & _EXCEPTION_UNDERFLOW.getLongValue()) != 0) {
                newExceptionMode = (value.isTrue()) ? (RubyFixnum)currentExceptionMode.callCoerced(context, "|", _EXCEPTION_UNDERFLOW)
                        : (RubyFixnum)currentExceptionMode.callCoerced(context, "&", new RubyFixnum(context.runtime, ~(_EXCEPTION_UNDERFLOW).getLongValue()));
            }
            RubyFixnum _EXCEPTION_OVERFLOW = (RubyFixnum)clazz.getConstant("EXCEPTION_OVERFLOW");
            if ((longMode & _EXCEPTION_OVERFLOW.getLongValue()) != 0) {
                newExceptionMode = (value.isTrue()) ? (RubyFixnum)currentExceptionMode.callCoerced(context, "|", _EXCEPTION_OVERFLOW)
                        : (RubyFixnum)currentExceptionMode.callCoerced(context, "&", new RubyFixnum(context.runtime, ~(_EXCEPTION_OVERFLOW).getLongValue()));
            }
            c.setInternalModuleVariable("vpExceptionMode", newExceptionMode);
            return newExceptionMode;
        }
        
        long ROUND_MODE = ((RubyFixnum)clazz.getConstant("ROUND_MODE")).getLongValue();
        if (longMode == ROUND_MODE) {
            if (value.isNil()) {
                return c.searchInternalModuleVariable("vpRoundingMode");
            }

            RoundingMode javaRoundingMode = javaRoundingModeFromRubyRoundingMode(context.runtime, value);
            RubyFixnum roundingMode = context.runtime.newFixnum(javaRoundingMode.ordinal());
            c.setInternalModuleVariable("vpRoundingMode", roundingMode);

            return c.searchInternalModuleVariable("vpRoundingMode");
        }
        throw context.runtime.newTypeError("first argument for BigDecimal#mode invalid");
    }
    
    private static RubyModule bigDecimal(Ruby runtime) {
        return runtime.getClass("BigDecimal");
    }
    
    // The Fixnum cast should be fine because these are internal variables and user code cannot change them.
    private static long bigDecimalVar(Ruby runtime, String variableName) {
        return ((RubyFixnum) bigDecimal(runtime).searchInternalModuleVariable(variableName)).getLongValue();
    }
    
    // FIXME: Old code also blindly casts here.  We can CCE here.
    private static long bigDecimalConst(Ruby runtime, String constantName) {
        return ((RubyFixnum) bigDecimal(runtime).getConstant(constantName)).getLongValue();
    }     

    private static RoundingMode getRoundingMode(Ruby runtime) {
        return RoundingMode.valueOf((int) bigDecimalVar(runtime, "vpRoundingMode"));
    }

    private static boolean isNaNExceptionMode(Ruby runtime) {
        return (bigDecimalVar(runtime, "vpExceptionMode") & bigDecimalConst(runtime, "EXCEPTION_NaN")) != 0;
    }

    private static boolean isInfinityExceptionMode(Ruby runtime) {
        return (bigDecimalVar(runtime, "vpExceptionMode") & bigDecimalConst(runtime, "EXCEPTION_INFINITY")) != 0;
    }
    
    private static boolean isOverflowExceptionMode(Ruby runtime) {
        return (bigDecimalVar(runtime, "vpExceptionMode") & bigDecimalConst(runtime, "EXCEPTION_OVERFLOW")) != 0;
    }
    
    private static RubyBigDecimal cannotBeCoerced(ThreadContext context, IRubyObject v, boolean must) {
        if (must) {
            String err;

            if (v == null) {
                err = "nil";
            } else if (v.isImmediate()) {
                err = RubyObject.inspect(context, v).toString();
            } else {
                err = v.getMetaClass().getBaseName();
            }

            throw context.runtime.newTypeError(err + " can't be coerced into BigDecimal");
        }

        return null;
    }
    
    private static RubyBigDecimal unableToCoerceWithoutPrec(ThreadContext context, IRubyObject v, boolean must) {
        if (!must) return null;

        throw context.runtime.newArgumentError(v.getMetaClass().getBaseName() + 
                " can't be coerced into BigDecimal without a precision");
    }
    
    private static RubyBigDecimal getVpValue19(ThreadContext context, IRubyObject v, boolean must) {
        long precision = (v instanceof RubyFloat || v instanceof RubyRational) ? 0 : -1;

        return getVpValueWithPrec19(context, v, precision, must);
    }
    
    private static RubyBigDecimal getVpRubyObjectWithPrec19Inner(ThreadContext context, RubyRational r) {
        BigDecimal numerator = BigDecimal.valueOf(RubyNumeric.num2long(r.numerator(context)));
        BigDecimal denominator = BigDecimal.valueOf(RubyNumeric.num2long(r.denominator(context)));

        int len = numerator.precision() + denominator.precision();
        int pow = len / 4;
        MathContext mathContext = new MathContext((pow + 1) * 4, getRoundingMode(context.runtime));

        return new RubyBigDecimal(context.runtime, numerator.divide(denominator, mathContext));
    }
    
    private static RubyBigDecimal getVpValueWithPrec19(ThreadContext context, IRubyObject value, long precision, boolean must) {
        if (value instanceof RubyFloat) {
            if (precision > Long.MAX_VALUE) cannotBeCoerced(context, value, must);

            return new RubyBigDecimal(context.runtime, BigDecimal.valueOf(((RubyFloat)value).getDoubleValue()));
        } else if (value instanceof RubyRational) {
            if (precision < 0) return unableToCoerceWithoutPrec(context, value, must);
                
            return getVpRubyObjectWithPrec19Inner(context, (RubyRational) value);
        } 
            
        return getVpValue(context, value, must);
    }

    private static RubyBigDecimal getVpValue(ThreadContext context, IRubyObject v, boolean must) {
        if (v instanceof RubyBigDecimal) return (RubyBigDecimal) v;
        if (v instanceof RubyFixnum || v instanceof RubyBignum) {
            // Converted to a String because some values -inf cannot happen from Java libs
            return newInstance(context, bigDecimal(context.runtime), context.runtime.newString(v.toString()));
        }

        return cannotBeCoerced(v.getRuntime().getCurrentContext(), v, must);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject induced_from(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return getVpValue(context, arg, true);
    }

    private final static Pattern INFINITY_PATTERN = Pattern.compile("^([+-])?Infinity$");
    private final static Pattern NUMBER_PATTERN = Pattern.compile("^([+-]?\\d*\\.?\\d*([eE][+-]?)?\\d*).*");
    
    private static RubyBigDecimal newInstance(ThreadContext context, IRubyObject recv, RubyBigDecimal arg) {
        return new RubyBigDecimal(context.runtime, (RubyClass) recv, arg);
    }
    
    private static RubyBigDecimal newInstance(ThreadContext context, IRubyObject recv, RubyFixnum arg, MathContext mathContext) {
        return new RubyBigDecimal(context.runtime, (RubyClass) recv, new BigDecimal(((RubyFixnum) arg).getLongValue(), mathContext));
    }
    
    private static RubyBigDecimal newInstance(ThreadContext context, RubyRational arg, MathContext mathContext) {
        BigDecimal num = new BigDecimal(arg.numerator(context).convertToInteger().getLongValue());
        BigDecimal den = new BigDecimal(arg.denominator(context).convertToInteger().getLongValue());
        BigDecimal value = num.divide(den, mathContext);

        return new RubyBigDecimal(context.runtime, value);
    }
    
    private static RubyBigDecimal newInstance(ThreadContext context, IRubyObject recv, RubyFloat arg, MathContext mathContext) {
        // precision can be no more than float digits
        if (mathContext.getPrecision() > RubyFloat.DIG + 1) throw context.runtime.newArgumentError("precision too large");

        return new RubyBigDecimal(context.runtime, (RubyClass) recv, new BigDecimal(arg.getDoubleValue(), mathContext));
    }    
    
    private static RubyBigDecimal newInstance(ThreadContext context, IRubyObject recv, RubyBignum arg, MathContext mathContext) {
        return new RubyBigDecimal(context.runtime, (RubyClass) recv, new BigDecimal(arg.getBigIntegerValue(), mathContext));
    }
        
    private static RubyBigDecimal newInstance(ThreadContext context, IRubyObject recv, IRubyObject arg, MathContext mathContext) {
        String strValue = arg.convertToString().toString().trim();
        if ("NaN".equals(strValue)) return newNaN(context.runtime);

        Matcher m = INFINITY_PATTERN.matcher(strValue);
        if (m.matches()) return newInfinity(context.runtime, "-".equals(m.group(1)) ? -1 : 1);

        // Convert String to Java understandable format (for BigDecimal).
        strValue = strValue.replaceFirst("[dD]", "E");                  // 1. MRI allows d and D as exponent separators
        strValue = strValue.replaceAll("_", "");                        // 2. MRI allows underscores anywhere
        strValue = NUMBER_PATTERN.matcher(strValue).replaceFirst("$1"); // 3. MRI ignores the trailing junk

        BigDecimal decimal;
        try {
            decimal = new BigDecimal(strValue, mathContext);
        } catch (NumberFormatException e) {
            if (isOverflowExceptionMode(context.runtime)) throw context.runtime.newFloatDomainError("exponent overflow");

            decimal = new BigDecimal(0);
        }

        // MRI behavior: -0 and +0 are two different things
        if (decimal.signum() == 0) return newZero(context.runtime, strValue.matches("^\\s*-.*") ? -1 : 1);

        return new RubyBigDecimal(context.runtime, (RubyClass)recv, decimal);    
    }
    
    @JRubyMethod(name = "new", meta = true)
    public static RubyBigDecimal newInstance(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        if (arg instanceof RubyBigDecimal) return newInstance(context, recv, (RubyBigDecimal) arg);
        if (arg instanceof RubyFloat || arg instanceof RubyRational) throw context.runtime.newArgumentError("can't omit precision for a rational");
        if (arg instanceof RubyFixnum) return newInstance(context, recv, (RubyFixnum) arg, MathContext.UNLIMITED);
        if (arg instanceof RubyBignum) return newInstance(context, recv, (RubyBignum) arg, MathContext.UNLIMITED);

        return newInstance(context, recv, arg, MathContext.UNLIMITED);
    }
    
    @JRubyMethod(name = "new", meta = true)
    public static RubyBigDecimal newInstance(ThreadContext context, IRubyObject recv, IRubyObject arg, IRubyObject mathArg) {
        int digits = (int) mathArg.convertToInteger().getLongValue();
        if (digits < 0) throw context.runtime.newArgumentError("argument must be positive");

        MathContext mathContext = new MathContext(digits);

        if (arg instanceof RubyBigDecimal) return newInstance(context, recv, (RubyBigDecimal) arg);
        if (arg instanceof RubyFloat) return newInstance(context, recv, (RubyFloat) arg, mathContext);
        if (arg instanceof RubyRational) return newInstance(context, (RubyRational) arg, mathContext);
        if (arg instanceof RubyFixnum) return newInstance(context, recv, (RubyFixnum) arg, mathContext);
        if (arg instanceof RubyBignum) return newInstance(context, recv, (RubyBignum) arg, mathContext);

        return newInstance(context, recv, arg, MathContext.UNLIMITED);
    }

    private static RubyBigDecimal newZero(Ruby runtime, int sign) {
        return new RubyBigDecimal(runtime, BigDecimal.ZERO, 0, sign < 0 ? -1 : 1);
    }

    private static RubyBigDecimal newNaN(Ruby runtime) {
        if (isNaNExceptionMode(runtime)) throw runtime.newFloatDomainError("Computation results to 'NaN'(Not a Number)");

        return new RubyBigDecimal(runtime, BigDecimal.ZERO, true);
    }
    
    private static RubyBigDecimal newInfinity(Ruby runtime, int sign) {
        if (isInfinityExceptionMode(runtime)) throw runtime.newFloatDomainError("Computation results to 'Infinity'");

        return new RubyBigDecimal(runtime, BigDecimal.ZERO, sign < 0 ? -1 : 1);
    }

    private RubyBigDecimal setResult() {
        return setResult(0);
    }

    private RubyBigDecimal setResult(int scale) {
        int prec = RubyFixnum.fix2int(getRuntime().getClass("BigDecimal").searchInternalModuleVariable("vpPrecLimit"));
        int prec2 = Math.max(scale, prec);
        if (prec2 > 0 && this.value.scale() > (prec2-getExponent())) {
            this.value = this.value.setScale(prec2-getExponent(), BigDecimal.ROUND_HALF_UP);
        }
        return this;
    }
    
    @Override
    @JRubyMethod
    public RubyFixnum hash() {
        return getRuntime().newFixnum(value.stripTrailingZeros().hashCode());
    }

    @Override
    @JRubyMethod(name = "initialize_copy", visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(IRubyObject original) {
        if (this == original) return this;

        checkFrozen();

        if (!(original instanceof RubyBigDecimal)) {
            throw getRuntime().newTypeError("wrong argument class");
        }

        RubyBigDecimal origRbd = (RubyBigDecimal)original;

        this.isNaN = origRbd.isNaN;
        this.infinitySign = origRbd.infinitySign;
        this.zeroSign = origRbd.zeroSign;
        this.value = origRbd.value;

        return this;
    }

    public IRubyObject op_mod(ThreadContext context, IRubyObject arg) {
        return op_mod19(context, arg);
    }

    @JRubyMethod(name = {"%", "modulo"}, required = 1)
    public IRubyObject op_mod19(ThreadContext context, IRubyObject other) {
        // TODO: full-precision divmod is 1000x slower than MRI!
        RubyBigDecimal val = getVpValue19(context, other, false);
        
        if (val == null) return callCoerced(context, "%", other, true);
        if (isNaN() || val.isNaN() || isInfinity() && val.isInfinity()) return newNaN(context.runtime);
        if (val.isZero()) throw context.runtime.newZeroDivisionError();
        if (isInfinity()) return newNaN(context.runtime);
        if (val.isInfinity()) return this;
        if (isZero()) return newZero(context.runtime, value.signum());

        // Java and MRI definitions of modulo are different.
        BigDecimal modulo = value.remainder(val.value);
        if (modulo.signum() * val.value.signum() < 0) modulo = modulo.add(val.value);

        return new RubyBigDecimal(context.runtime, modulo).setResult();
    }

    @Override
    public IRubyObject remainder(ThreadContext context, IRubyObject arg) {
        return remainder19(context, arg);
    }

    @JRubyMethod(name = "remainder", required = 1)
    public IRubyObject remainder19(ThreadContext context, IRubyObject arg) {
        return remainderInternal(context, getVpValue19(context, arg, false), arg);
    }

    private IRubyObject remainderInternal(ThreadContext context, RubyBigDecimal val, IRubyObject arg) {
        // TODO: full-precision remainder is 1000x slower than MRI!
        if (isInfinity() || isNaN()) return newNaN(context.runtime);
        if (val == null) return callCoerced(context, "remainder", arg, true);
        if (val.isInfinity() || val.isNaN() || val.isZero()) return newNaN(context.runtime);

        // Java and MRI definitions of remainder are the same.
        return new RubyBigDecimal(context.runtime, value.remainder(val.value)).setResult();
    }

    public IRubyObject op_mul(ThreadContext context, IRubyObject arg) {
        return op_mul19(context, arg);
    }

    @JRubyMethod(name = "*", required = 1)
    public IRubyObject op_mul19(ThreadContext context, IRubyObject arg) {
        return mult219(context, arg, bigDecimal(context.runtime).searchInternalModuleVariable("vpPrecLimit"));
    }

    public IRubyObject mult2(ThreadContext context, IRubyObject b, IRubyObject n) {
        return mult219(context, b, n);
    }

    @JRubyMethod(name = "mult", required = 2)
    public IRubyObject mult219(ThreadContext context, IRubyObject b, IRubyObject n) {
        RubyBigDecimal val = getVpValue19(context, b, false);
        if (val == null) return cannotBeCoerced(context, b, true);

        return multInternal(context, val, b, n);
    }

    private IRubyObject multInternal(ThreadContext context, RubyBigDecimal val, IRubyObject b, IRubyObject n) {
        // TODO: what about n arg?
        if (val == null) return callCoerced(context, "*", b);

        int digits = RubyNumeric.fix2int(n);

        if (isNaN() || val.isNaN()) return newNaN(context.runtime);
        if ((isInfinity() && val.isZero()) || (isZero() && val.isInfinity())) return newNaN(context.runtime);

        if (isZero() || val.isZero()) {
            int sign1 = isZero()? zeroSign : value.signum();
            int sign2 = val.isZero() ?  val.zeroSign : val.value.signum();
            return newZero(context.runtime, sign1 * sign2);
        }

        if (isInfinity() || val.isInfinity()) {
            int sign1 = isInfinity() ? infinitySign : value.signum();
            int sign2 = val.isInfinity() ? val.infinitySign : val.value.signum();
            return newInfinity(context.runtime, sign1 * sign2);
        }

        BigDecimal res = value.multiply(val.value);
        // FIXME: rounding mode should not be hard-coded. See #mode.
        if (res.precision() > digits) res = res.round(new MathContext(digits,  RoundingMode.HALF_UP));

        return new RubyBigDecimal(context.runtime, res).setResult();
    }
    
    // Calculate appropriate zero or infinity depending on exponent...
    private IRubyObject newPowOfInfinity(ThreadContext context, IRubyObject exp) {
        if (Numeric.f_negative_p(context, exp)) {
            if (infinitySign >= 0) return newZero(context.runtime, 0);

            // (-Infinity) ** (-even_integer) -> +0 AND (-Infinity) ** (-odd_integer) -> -0
            if (Numeric.f_integer_p(context, exp).isTrue()) return newZero(context.runtime, is_even(exp) ? 1 : -1);

            return newZero(context.runtime, -1); // (-Infinity) ** (-non_integer) -> -0
        }
             
        if (infinitySign >= 0) return newInfinity(context.runtime, 1);

        if (Numeric.f_integer_p(context, exp).isTrue()) return newInfinity(context.runtime, is_even(exp) ? 1 : -1);

        throw context.runtime.newMathDomainError("a non-integral exponent for a negative base");
    }
    
    @JRubyMethod(name = {"**", "power"}, required = 1)
    public IRubyObject op_pow(ThreadContext context, IRubyObject exp) {
        if (!(exp instanceof RubyFixnum)) throw getRuntime().newTypeError("wrong argument type " + 
                exp.getMetaClass() + " (expected Fixnum)");

        if (isNaN()) return newNaN(context.runtime);
        if (isInfinity()) return newPowOfInfinity(context, exp);

        int times = RubyNumeric.fix2int(exp.convertToInteger());

        if (times < 0) {
            if (isZero()) return newInfinity(getRuntime(), value.signum());

            // Note: MRI has a very non-trivial way of calculating the precision,
            // so we use very simple approximation here:
            int precision = (-times + 4) * (getAllDigits().length() + 4);

            return new RubyBigDecimal(getRuntime(),
                    value.pow(times, new MathContext(precision, RoundingMode.HALF_UP)));
        }
         
        return new RubyBigDecimal(getRuntime(), value.pow(times));
    }

    public IRubyObject op_plus(ThreadContext context, IRubyObject b) {
        return op_plus19(context, b);
    }

    @JRubyMethod(name = "+")
    public IRubyObject op_plus19(ThreadContext context, IRubyObject b) {
        return addInternal(context, getVpValue19(context, b, false), b, 
                bigDecimal(context.runtime).searchInternalModuleVariable("vpPrecLimit"));
    }

    public IRubyObject add2(ThreadContext context, IRubyObject b, IRubyObject digits) {
        return add219(context, b, digits);
    }

    @JRubyMethod(name = "add")
    public IRubyObject add219(ThreadContext context, IRubyObject b, IRubyObject digits) {
        return addInternal(context, getVpValue19(context, b, false), b, digits);
    }

    private IRubyObject addInternal(ThreadContext context, RubyBigDecimal val, IRubyObject b, IRubyObject digits) {
        Ruby runtime = context.runtime;
        int prec = getPositiveInt(context, digits);

        if (val == null) {
            // TODO:
            // MRI behavior: Call "+" or "add", depending on the call.
            // But this leads to exceptions when Floats are added. See:
            // http://blade.nagaokaut.ac.jp/cgi-bin/scat.rb/ruby/ruby-core/17374
            // return callCoerced(context, op, b, true); -- this is MRI behavior.
            // We'll use ours for now, thus providing an ability to add Floats.
            return callCoerced(context, "+", b, true);
        }

        RubyBigDecimal res = handleAddSpecialValues(val);
        if (res != null) return res;

        RoundingMode roundMode = getRoundingMode(runtime);
        return new RubyBigDecimal(runtime, value.add(
                val.value, new MathContext(prec, roundMode))); // TODO: why this: .setResult();
    }

    private int getPositiveInt(ThreadContext context, IRubyObject arg) {
        if (!(arg instanceof RubyFixnum)) throw context.runtime.newTypeError(arg, context.runtime.getFixnum());

        int _value = RubyNumeric.fix2int(arg);
        if (_value < 0) throw context.runtime.newArgumentError("argument must be positive");
        return _value;
    }

    private RubyBigDecimal handleAddSpecialValues(RubyBigDecimal val) {
        if (isNaN() || val.isNaN) return newNaN(getRuntime());

        int sign = infinitySign * val.infinitySign;
        
        if (sign > 0) return isInfinity() ? this : val;
        if (sign < 0) return newNaN(getRuntime());

        // sign == 0
        sign = infinitySign + val.infinitySign;
        
        if (sign != 0) return newInfinity(getRuntime(), sign);

        return null;
    }

    @Override
    @JRubyMethod(name = "+@")
    public IRubyObject op_uplus() {
        return this;
    }
    
    public IRubyObject op_minus(ThreadContext context, IRubyObject b) {
        return op_minus19(context, b);
    }

    @JRubyMethod(name = "-", required = 1)
    public IRubyObject op_minus19(ThreadContext context, IRubyObject b) {
        return subInternal(context, getVpValue19(context, b, true), b);
    }

    public IRubyObject sub2(ThreadContext context, IRubyObject b, IRubyObject n) {
        return sub219(context, b, n);
    }

    @JRubyMethod(name = "sub", required = 2)
    public IRubyObject sub219(ThreadContext context, IRubyObject b, IRubyObject n) {
        // FIXME: Missing handling of n
        return subInternal(context, getVpValue19(context, b, false), b);
    }

    private IRubyObject subInternal(ThreadContext context, RubyBigDecimal val, IRubyObject b) {
        if (val == null) return callCoerced(context, "-", b);

        RubyBigDecimal res = handleMinusSpecialValues(val);

        return res != null ? res : new RubyBigDecimal(getRuntime(), value.subtract(val.value)).setResult();
    }

    private RubyBigDecimal handleMinusSpecialValues(RubyBigDecimal val) {
        if (isNaN() || val.isNaN()) return newNaN(getRuntime());

        int sign = infinitySign * val.infinitySign;
        
        if (sign > 0) return newNaN(getRuntime());
        if (sign < 0) return this;

        // sign == 0
        if (isInfinity()) return this;
        if (val.isInfinity()) return newInfinity(getRuntime(), val.infinitySign * -1);

        sign = infinitySign + val.infinitySign;
            
        if (sign != 0) return newInfinity(getRuntime(), sign);

        return null;
    }

    @JRubyMethod(name = "-@")
    @Override
    public IRubyObject op_uminus(ThreadContext context) {
        if (isNaN()) return newNaN(context.runtime);
        if (isInfinity()) return newInfinity(context.runtime, -infinitySign);
        if (isZero()) return newZero(context.runtime, -zeroSign);

        return new RubyBigDecimal(getRuntime(), value.negate());
    }

    public IRubyObject op_quo(ThreadContext context, IRubyObject other) {
        return op_quo20(context, other);
    }

    public IRubyObject op_quo19(ThreadContext context, IRubyObject other) {   
        return op_quo19_20(context, other);
    }
    
    @JRubyMethod(name = {"/", "quo"})
    public IRubyObject op_quo20(ThreadContext context, IRubyObject other) {
        return op_quo19_20(context, other);
    }
    
    private IRubyObject op_quo19_20(ThreadContext context, IRubyObject other) {
        RubyBigDecimal preciseOther = getVpValue19(context, other, true);
        // regular division with some default precision
        // proper algorithm to set the precision
        // the precision is multiple of 4
        // and the precision is larger than len * 2
        int len = value.precision() + preciseOther.value.precision();
        int pow = len / 4;
        int precision = (pow + 1) * 4 * 2;

        return op_div(context, preciseOther, getRuntime().newFixnum(precision));
    }
    
    public IRubyObject op_div(ThreadContext context, IRubyObject other) {
        // integer division
        RubyBigDecimal val = getVpValue(context, other, false);
        if (val == null) return callCoerced(context, "div", other);
        if (isNaN() || val.isZero() || val.isNaN()) return newNaN(context.runtime);
        if (isInfinity() || val.isInfinity()) return newNaN(context.runtime);

        return new RubyBigDecimal(context.runtime,
                this.value.divideToIntegralValue(val.value)).setResult();
    }

    @JRubyMethod(name = "div")
    public IRubyObject op_div19(ThreadContext context, IRubyObject r) {
        RubyBigDecimal val = getVpValue19(context, r, true);

        if (val == null) return cannotBeCoerced(context, val, true);
        if (isNaN() || val.isNaN()) throw context.runtime.newFloatDomainError("Computation results to 'NaN'");
        if (isInfinity() && val.isOne()) throw context.runtime.newFloatDomainError("Computation results to 'Infinity'");
        
        if (val.isInfinity()) return newZero(getRuntime(), val.infinitySign);

        if (isZero() || val.isZero()) throw context.runtime.newZeroDivisionError();

        return op_div(context, r);
    }

    public IRubyObject op_div(ThreadContext context, IRubyObject other, IRubyObject digits) {
        // TODO: take BigDecimal.mode into account.

        int scale = RubyNumeric.fix2int(digits);

        RubyBigDecimal val = getVpValue(context, other, false);
        if (val == null) return callCoerced(context, "/", other);
        if (isNaN() || (isZero() && val.isZero()) || val.isNaN()) return newNaN(getRuntime());

        if (val.isZero()) {
            int sign1 = isInfinity() ? infinitySign : value.signum();
            return newInfinity(context.runtime, sign1 * val.zeroSign);
        }

        if (isInfinity() && !val.isInfinity()) return newInfinity(context.runtime, infinitySign * val.value.signum());
        if (!isInfinity() && val.isInfinity()) return newZero(context.runtime, value.signum() * val.infinitySign);
        if (isInfinity() && val.isInfinity()) return newNaN(context.runtime);
        if (isZero()) return newZero(context.runtime, zeroSign * val.value.signum());

        // MRI behavior: "If digits is 0, the result is the same as the / operator."
        if (scale == 0) return op_quo(context, other);

        if (isZero()) {
            return newZero(getRuntime(), zeroSign * val.value.signum());
        }

        if (scale == 0) {
            // MRI behavior: "If digits is 0, the result is the same as the / operator."
            return op_quo(context, other);
        } else {
            MathContext mathContext = new MathContext(scale, getRoundingMode(context.runtime));
            return new RubyBigDecimal(getRuntime(),
                    value.divide(val.value, mathContext)).setResult(scale);
        }
    }
    
    @JRubyMethod(name = "div")
    public IRubyObject op_div19(ThreadContext context, IRubyObject other, IRubyObject digits) {
        RubyBigDecimal val = getVpValue(context, other, false);
        if (val == null) return cannotBeCoerced(context, val, true);

        if (isNaN() || val.isNaN()) {
            throw context.runtime.newFloatDomainError("Computation results to 'NaN'");
        }
        
        return op_div(context, other, digits);
    }

    private IRubyObject cmp(ThreadContext context, IRubyObject r, char op) {
        int e;
        RubyBigDecimal rb = getVpValue(context, r, false);
        if (rb == null) {
            IRubyObject ee = callCoerced(context, "<=>", r);
            if (ee.isNil()) {
                if (op == '*') return getRuntime().getNil();
                if (op == '=' || isNaN()) return getRuntime().getFalse();
                    
                throw getRuntime().newArgumentError("nil could not be coerced into a BigDecmil");
            }
            e = RubyNumeric.fix2int(ee);
        } else {
            if (isNaN() || rb.isNaN()) return op == '*' ? getRuntime().getNil() : getRuntime().getFalse();

            e = infinitySign != 0 || rb.infinitySign != 0 ?
                    infinitySign - rb.infinitySign : value.compareTo(rb.value);
        }
        switch(op) {
        case '*': return getRuntime().newFixnum(e);
        case '=': return (e==0)?getRuntime().getTrue():getRuntime().getFalse();
        case '!': return (e!=0)?getRuntime().getTrue():getRuntime().getFalse();
        case 'G': return (e>=0)?getRuntime().getTrue():getRuntime().getFalse();
        case '>': return (e> 0)?getRuntime().getTrue():getRuntime().getFalse();
        case 'L': return (e<=0)?getRuntime().getTrue():getRuntime().getFalse();
        case '<': return (e< 0)?getRuntime().getTrue():getRuntime().getFalse();
        }
        return getRuntime().getNil();
    }

    @Override
    @JRubyMethod(name = "<=>", required = 1)
    public IRubyObject op_cmp(ThreadContext context, IRubyObject arg) {
        return cmp(context, arg, '*');
    }

    @Override
    @JRubyMethod(name = {"eql?", "==", "==="}, required = 1)
    public IRubyObject eql_p(ThreadContext context, IRubyObject arg) {
        return cmp(context, arg, '=');
    }

    @JRubyMethod(name = "<", required = 1)
    public IRubyObject op_lt(ThreadContext context, IRubyObject arg) {
        return cmp(context, arg, '<');
    }

    @JRubyMethod(name = "<=", required = 1)
    public IRubyObject op_le(ThreadContext context, IRubyObject arg) {
        return cmp(context, arg, 'L');
    }

    @JRubyMethod(name = ">", required = 1)
    public IRubyObject op_gt(ThreadContext context, IRubyObject arg) {
        return cmp(context, arg, '>');
    }

    @JRubyMethod(name = ">=", required = 1)
    public IRubyObject op_ge(ThreadContext context, IRubyObject arg) {
        return cmp(context, arg, 'G');
    }

    @JRubyMethod
    public IRubyObject abs() {
        if (isNaN()) return newNaN(getRuntime());
        if (isInfinity()) return newInfinity(getRuntime(), 1);

        return new RubyBigDecimal(getRuntime(), value.abs()).setResult();
    }

    @JRubyMethod
    public IRubyObject ceil(ThreadContext context, IRubyObject arg) {
        checkFloatDomain();

        int n = RubyNumeric.fix2int(arg);
        
        if (value.scale() <= n) return this; // no rounding neccessary
        
        return new RubyBigDecimal(getRuntime(), value.setScale(n, RoundingMode.CEILING));
    }
    
    @JRubyMethod
    public IRubyObject ceil(ThreadContext context) {
        checkFloatDomain();

        BigInteger ceil = value.setScale(0, RoundingMode.CEILING).toBigInteger();
        
        if (ceil.compareTo(BigInteger.valueOf((long) ceil.intValue())) == 0) { // It fits in Fixnum
            return RubyInteger.int2fix(context.runtime, ceil.intValue());
        }

        return RubyBignum.newBignum(context.runtime, ceil);
    }

    // FIXME: Do we really need this Java inheritence for coerce?
    @Override
    public IRubyObject coerce(IRubyObject other) {
        return coerce(getRuntime().getCurrentContext(), other);
    }

    @JRubyMethod
    public IRubyObject coerce(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFloat) return getRuntime().newArray(other, to_f());

        return getRuntime().newArray(getVpValue(context, other, true), this);
    }

    @Override
    public double getDoubleValue() {
        return SafeDoubleParser.doubleValue(value);
    }
    
    @Override
    public long getLongValue() {
        return value.longValue();
    }

    @Override
    public int getIntValue() {
        return value.intValue();
    }

    @Override
    public BigInteger getBigIntegerValue() {
        return value.toBigInteger();
    }

    public BigDecimal getBigDecimalValue() {
        return value;
    }

    public RubyNumeric multiplyWith(ThreadContext context, RubyInteger value) { 
        return (RubyNumeric)op_mul(context, value);
    }

    public RubyNumeric multiplyWith(ThreadContext context, RubyFloat value) { 
        return (RubyNumeric)op_mul(context, value);
    }

    public RubyNumeric multiplyWith(ThreadContext context, RubyBignum value) { 
        return (RubyNumeric)op_mul(context, value);
    }

    @Override
    public IRubyObject divmod(ThreadContext context, IRubyObject other) {
        return divmod19(context, other);
    }
    
    @Override
    @JRubyMethod(name = "divmod")
    public IRubyObject divmod19(ThreadContext context, IRubyObject other) {
        // TODO: full-precision divmod is 1000x slower than MRI!
        Ruby runtime = context.runtime;
        RubyBigDecimal val = getVpValue19(context, other, false);

        if (val == null) return callCoerced(context, "divmod", other, true);
        if (isNaN() || val.isNaN() || isInfinity() && val.isInfinity()) return RubyArray.newArray(runtime, newNaN(runtime), newNaN(runtime));
        if (val.isZero()) throw context.runtime.newZeroDivisionError();
        if (isInfinity()) {
            int sign = (infinitySign == val.value.signum()) ? 1 : -1;
            return RubyArray.newArray(runtime, newInfinity(runtime, sign), newNaN(runtime));
        }
        if (val.isInfinity()) return RubyArray.newArray(runtime, newZero(runtime, val.value.signum()), this);
        if (isZero()) return RubyArray.newArray(runtime, newZero(runtime, value.signum()), newZero(runtime, value.signum()));

        // Java and MRI definitions of divmod are different.
        BigDecimal[] divmod = value.divideAndRemainder(val.value);

        BigDecimal div = divmod[0];
        BigDecimal mod = divmod[1];

        if (mod.signum() * val.value.signum() < 0) {
            div = div.subtract(BigDecimal.ONE);
            mod = mod.add(val.value);
        }

        return RubyArray.newArray(runtime, new RubyBigDecimal(runtime, div), new RubyBigDecimal(runtime, mod));
    }
    
    @JRubyMethod
    public IRubyObject exponent() {
        return getRuntime().newFixnum(getExponent());
    }

    @JRubyMethod(name = "finite?")
    public IRubyObject finite_p() {
        return getRuntime().newBoolean(!isNaN() && !isInfinity());
    }

    private void floorNaNInfinityCheck(Ruby runtime) {
        if (isNaN() || isInfinity()) {
            throw runtime.newFloatDomainError("Computation results to '" + to_s(NULL_ARRAY).asJavaString() + "'");
        }        
    }
    
    private RubyBigDecimal floorInternal(ThreadContext context, int n) {
        return value.scale() > n ? new RubyBigDecimal(context.runtime, value.setScale(n, RoundingMode.FLOOR)) : this;
    }
    
    @JRubyMethod public IRubyObject floor(ThreadContext context) {
        floorNaNInfinityCheck(context.runtime);
        return floorInternal(context, 0).to_int();
    }
    
    @JRubyMethod public IRubyObject floor(ThreadContext context, IRubyObject arg) {
        floorNaNInfinityCheck(context.runtime);
        return floorInternal(context, RubyNumeric.fix2int(arg));
     }    
 
    @JRubyMethod
    public IRubyObject frac(ThreadContext context) {
        if (isNaN()) return newNaN(context.runtime);
        if (isInfinity()) return newInfinity(context.runtime, infinitySign);

        if (value.scale() > 0 && value.precision() < value.scale()) return new RubyBigDecimal(context.runtime, value);

        return new RubyBigDecimal(context.runtime, value.subtract(((RubyBigDecimal)fix()).value));
    }

    @JRubyMethod(name = "infinite?")
    public IRubyObject infinite_p(ThreadContext context) {
        return infinitySign == 0 ? context.runtime.getNil() : context.runtime.newFixnum(infinitySign);
    }

    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        StringBuilder val = new StringBuilder("#<BigDecimal:");

        val.append(Integer.toHexString(System.identityHashCode(this))).append(",");
        val.append("'").append(callMethod(context, "to_s")).append("'").append(",");
        val.append(getSignificantDigits().length()).append("(");
        val.append(((getAllDigits().length() / 4) + 1) * 4).append(")").append(">");

        return getRuntime().newString(val.toString());
    }

    @JRubyMethod(name = "nan?")
    public IRubyObject nan_p(ThreadContext context) {
        return context.runtime.newBoolean(isNaN());
    }

    @JRubyMethod(name = "nonzero?")
    public IRubyObject nonzero_p() {
        return isZero() ? getRuntime().getNil() : this;
    }
 
    @JRubyMethod
    public IRubyObject precs(ThreadContext context) {
        return RubyArray.newArrayNoCopy(context.runtime, 
                new IRubyObject[] {context.runtime.newFixnum(getSignificantDigits().length()), 
                                   context.runtime.newFixnum(((getAllDigits().length() / 4) + 1) * 4)});
    }
    
    @JRubyMethod(name = "round", optional = 2)
    public IRubyObject round(ThreadContext context, IRubyObject[] args) {
        int scale = args.length > 0 ? num2int(args[0]) : 0;

        // Special treatment for BigDecimal::NAN and BigDecimal::INFINITY
        //
        // If round is called without any argument, we should raise a
        // FloatDomainError. Otherwise, we don't have to call round ;
        // we can simply return the number itself.
        if (scale == 0 && (isNaN() || isInfinity())) {
            StringBuilder message = new StringBuilder("Computation results to ");
            message.append("'").append(callMethod(context, "to_s")).append("'");

            // To be consistent with MRI's output
            if (isNaN()) message.append("(Not a Number)");

            throw getRuntime().newFloatDomainError(message.toString());
        } else {
            if (isNaN()) {
                return newNaN(context.runtime);
            } else if (isInfinity()) {
                return newInfinity(context.runtime, infinitySign);
            }
        }

        RoundingMode mode = (args.length > 1) ? javaRoundingModeFromRubyRoundingMode(context.runtime, args[1]) : getRoundingMode(context.runtime);
        // JRUBY-914: Java 1.4 BigDecimal does not allow a negative scale, so we have to simulate it
        RubyBigDecimal bigDecimal = null;
        if (scale < 0) {
          // shift the decimal point just to the right of the digit to be rounded to (divide by 10**(abs(scale)))
          // -1 -> 10's digit, -2 -> 100's digit, etc.
          BigDecimal normalized = value.movePointRight(scale);
          // ...round to that digit
          BigDecimal rounded = normalized.setScale(0, mode);
          // ...and shift the result back to the left (multiply by 10**(abs(scale)))
          bigDecimal = new RubyBigDecimal(getRuntime(), rounded.movePointLeft(scale));
        } else {
          bigDecimal = new RubyBigDecimal(getRuntime(), value.setScale(scale, mode));
        }
        if (args.length == 0) {
            return bigDecimal.to_int();
        } else {
            return bigDecimal;
        }
    }
    
    public IRubyObject round(ThreadContext context, IRubyObject scale, IRubyObject mode) {
        return round(context, new IRubyObject[]{scale, mode});
    }    

    //this relies on the Ruby rounding enumerations == Java ones, which they (currently) all are
    private static RoundingMode javaRoundingModeFromRubyRoundingMode(Ruby runtime, IRubyObject arg) {
        if (arg instanceof RubySymbol) {
            RubySymbol roundingModeSymbol = (RubySymbol) arg;
            String roundingModeString = roundingModeSymbol.asJavaString();
            if (roundingModeString.equals("up")) {
                return RoundingMode.UP;
            } else if (roundingModeString.equals("down") || roundingModeString.equals("truncate")) {
                return RoundingMode.DOWN;
            } else if (roundingModeString.equals("half_up") || roundingModeString.equals("default")) {
                return RoundingMode.HALF_UP;
            } else if (roundingModeString.equals("half_down")) {
                return RoundingMode.HALF_DOWN;
            } else if (roundingModeString.equals("half_even") || roundingModeString.equals("banker")) {
                return RoundingMode.HALF_EVEN;
            } else if (roundingModeString.equals("ceiling") || roundingModeString.equals("ceil")) {
                return RoundingMode.CEILING;
            } else if (roundingModeString.equals("floor")) {
                return RoundingMode.FLOOR;
            } else {
                throw runtime.newArgumentError("invalid rounding mode");
            }
        } else {
            try {
                return RoundingMode.valueOf(num2int(arg));
            } catch (IllegalArgumentException iae) {
                throw runtime.newArgumentError("invalid rounding mode");
            }
        }
    }
    
    @JRubyMethod
    public IRubyObject sign() {
        if (isNaN()) return getMetaClass().getConstant("SIGN_NaN");
        if (isInfinity()) return getMetaClass().getConstant(infinitySign < 0 ? "SIGN_NEGATIVE_INFINITE" : "SIGN_POSITIVE_INFINITE");
        if (isZero()) return getMetaClass().getConstant(zeroSign < 0 ? "SIGN_NEGATIVE_ZERO" : "SIGN_POSITIVE_ZERO");
        
        return getMetaClass().getConstant(value.signum() < 0 ? "SIGN_NEGATIVE_FINITE" : "SIGN_POSITIVE_FINITE");
    }
    
    private RubyFixnum signValue(Ruby runtime) {
        if (isNaN()) return RubyFixnum.zero(runtime);
        if (isInfinity()) return runtime.newFixnum(infinitySign);
        if (isZero()) return runtime.newFixnum(zeroSign);

        return runtime.newFixnum(value.signum());
    } 

    @JRubyMethod
    public RubyArray split(ThreadContext context) {
        return RubyArray.newArrayNoCopy(context.runtime, new IRubyObject[] {
            signValue(context.runtime), context.runtime.newString(splitDigits()), 
            context.runtime.newFixnum(10), exponent()
        });
    }
    
    private String splitDigits() {
        if (isNaN()) return "NaN";
        if (isInfinity()) return "Infinity";
        if (isZero()) return "0";

        return getSignificantDigits();
    }
    
    // it doesn't handle special cases
    private String getSignificantDigits() {
        // TODO: no need to calculate every time.
        return value.abs().stripTrailingZeros().unscaledValue().toString();
    }

    private String getAllDigits() {
        // TODO: no need to calculate every time.
        return value.abs().unscaledValue().toString();
    }    

    private int getExponent() {
        if (isZero() || isNaN() || isInfinity()) return 0;

        BigDecimal val = value.abs().stripTrailingZeros();
        return val.precision() - val.scale();
    }

    @JRubyMethod
    public IRubyObject sqrt(IRubyObject arg) {
        Ruby runtime = getRuntime();
        if (isNaN()) throw runtime.newFloatDomainError("(VpSqrt) SQRT(NaN value)");
        if ((isInfinity() && infinitySign < 0) || value.signum() < 0) throw runtime.newFloatDomainError("(VpSqrt) SQRT(negative value)");
        if (isInfinity() && infinitySign > 0) return newInfinity(runtime, 1);

        // NOTE: MRI's sqrt precision is limited by 100,
        // but we allow values more than 100.
        int n = RubyNumeric.fix2int(arg);
        if (n < 0) throw runtime.newArgumentError("argument must be positive");

        n += 4; // just in case, add a bit of extra precision

        return new RubyBigDecimal(runtime, bigSqrt(value, new MathContext(n, RoundingMode.HALF_UP))).setResult();
    }

    @JRubyMethod
    public IRubyObject to_f() {
        if (isNaN()) return RubyFloat.newFloat(getRuntime(), Double.NaN);
        if (isInfinity()) return RubyFloat.newFloat(getRuntime(), infinitySign < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
        if (isZero()) return RubyFloat.newFloat(getRuntime(), zeroSign < 0 ? -0.0 : 0.0);
        if (-value.scale() <= RubyFloat.MAX_10_EXP) return RubyFloat.newFloat(getRuntime(), SafeDoubleParser.doubleValue(value));

        switch (value.signum()) {
            case -1: return RubyFloat.newFloat(getRuntime(), Double.NEGATIVE_INFINITY);
            case 0: return RubyFloat.newFloat(getRuntime(), 0);
            case 1: return RubyFloat.newFloat(getRuntime(), Double.POSITIVE_INFINITY);
        }

        throw getRuntime().newArgumentError("signum of this rational is invalid: " + value.signum());
    }

    @JRubyMethod(name = {"to_i", "to_int"})
    public IRubyObject to_int() {
        checkFloatDomain();
    
        try {
            return RubyNumeric.int2fix(getRuntime(), value.longValueExact());
        } catch (ArithmeticException ae) {
            return RubyBignum.bignorm(getRuntime(), value.toBigInteger());            
        }
    }
    
    private String removeTrailingZeroes(String in) {
        while(in.length() > 0 && in.charAt(in.length()-1)=='0') {
            in = in.substring(0, in.length()-1);
        }
        return in;
    }
  
    public static boolean formatHasLeadingPlus(String format) {
        return format.startsWith("+");
    }

    public static boolean formatHasLeadingSpace(String format) {
        return format.startsWith(" ");
    }

    public static boolean formatHasFloatingPointNotation(String format) {
        return format.endsWith("F");
    }

    public static int formatFractionalDigitGroups(String format) {
        Matcher m = Pattern.compile("(\\+| )?(\\d+)(E|F)?").matcher(format);
        
        return m.matches() ? Integer.parseInt(m.group(2)) : 0;
    }
    
    private boolean hasArg(IRubyObject[] args) {
        return args.length != 0 && !args[0].isNil();
    }

    private String format(IRubyObject[] args) {
        return args[0].toString();
    }

    private String firstArgument(IRubyObject[] args) {
        return hasArg(args) ? format(args) : null;
    }

    private boolean posSpace(String arg) {
        return arg != null && formatHasLeadingSpace(arg);
    }

    private boolean posSign(String arg) {
        return arg != null && (formatHasLeadingPlus(arg) || posSpace(arg));
    }

    private boolean asEngineering(String arg) {
        return arg == null || !formatHasFloatingPointNotation(arg);
    }

    private int groups(String arg) {
        return arg != null ? formatFractionalDigitGroups(arg) : 0;
    }

    private boolean isZero() {
        return !isNaN() && !isInfinity() && (value.signum() == 0);
    }

    private boolean isOne() {
        return value.abs().compareTo(BigDecimal.ONE) == 0;
    }

    private boolean isNaN() {
        return isNaN;
    }

    private boolean isInfinity() {
        return infinitySign != 0;
    }

    private String unscaledValue() {
        return value.abs().unscaledValue().toString();
    }
    
    private String sign(String arg, int signum) {
        return signum == -1 ? "-" : (signum == 1 ? (posSign(arg) ? (posSpace(arg) ? " " : "+") : "") : "");
    }

    private IRubyObject engineeringValue(String arg) {
        StringBuilder build = new StringBuilder().append(sign(arg, value.signum())).append("0.");
        String s = removeTrailingZeroes(unscaledValue());
        
        if (groups(arg) == 0) {
            build.append("".equals(s) ? "0" : s);
        } else {
            int length = s.length();
            String sep = "";

            for (int index = 0; index < length; index += groups(arg)) {
                int next = index + groups(arg);
                build.append(sep).append(s.substring(index, next > length ? length : next));
                sep = " ";
            }
        }
        build.append("E").append(getExponent());
        return getRuntime().newString(build.toString());
    }

    private IRubyObject floatingPointValue(String arg) {
        String values[] = value.abs().stripTrailingZeros().toPlainString().split("\\.");
        String whole = values.length > 0 ? values[0] : "0";
        String after = values.length > 1 ? values[1] : "0";
        StringBuilder build = new StringBuilder().append(sign(arg, value.signum()));
        
        if (groups(arg) == 0) {
            build.append(whole);
            if (after != null) build.append(".").append(after);
        } else {
            int index = 0;
            String sep = "";
            while (index < whole.length()) {
                int next = index + groups(arg);
                if (next > whole.length()) next = whole.length();

                build.append(sep).append(whole.substring(index, next));
                sep = " ";
                index += groups(arg);
            }
            if (null != after) {
                build.append(".");
                index = 0;
                sep = "";
                while (index < after.length()) {
                    int next = index + groups(arg);
                    if (next > after.length()) next = after.length();

                    build.append(sep).append(after.substring(index, next));
                    sep = " ";
                    index += groups(arg);
                }
            }
        }
        return getRuntime().newString(build.toString());
    }
            
    @JRubyMethod(optional = 1)
    public IRubyObject to_s(IRubyObject[] args) {
        if (isNaN()) return getRuntime().newString("NaN");
        if (isInfinity()) return getRuntime().newString(infinityString());
        if (isZero()) return getRuntime().newString(zeroSign < 0 ? "-0.0" : "0.0");
        
        String arg = firstArgument(args);

        return asEngineering(arg) ? engineeringValue(arg) : floatingPointValue(arg);
    } 

    // Note: #fix has only no-arg form, but truncate allows optional parameter.

    @JRubyMethod
    public IRubyObject fix() {
        return truncateInternal(0);
    }

    private RubyBigDecimal truncateInternal(int arg) {
        if (isNaN()) return newNaN(getRuntime());
        if (isInfinity()) return newInfinity(getRuntime(), infinitySign);

        int precision = value.precision() - value.scale() + arg;
        
        if (precision > 0) return new RubyBigDecimal(getRuntime(),
                value.round(new MathContext(precision, RoundingMode.DOWN)));

        return new RubyBigDecimal(getRuntime(), BigDecimal.ZERO); // FIXME: proper sign
    }

    @JRubyMethod
    public IRubyObject truncate(ThreadContext context) {
        return truncateInternal(0).to_int();
    }
    
    @JRubyMethod
    public IRubyObject truncate(ThreadContext context, IRubyObject arg) {
        return truncateInternal(RubyNumeric.fix2int(arg));
    }    

    @JRubyMethod(name = "zero?")
    public IRubyObject zero_p() {
         return getRuntime().newBoolean(isZero());
    }

    /**
     * Returns the correctly rounded square root of a positive
     * BigDecimal. This method performs the fast <i>Square Root by
     * Coupled Newton Iteration</i> algorithm by Timm Ahrendt, from
     * the book "Pi, unleashed" by Jörg Arndt in a neat loop.
     * <p>
     * The code is based on Frans Lelieveld's code , used here with
     * permission.
     *
     * @param squarD The number to get the root from.
     * @param rootMC Precision and rounding mode.
     * @return the root of the argument number
     * @throws ArithmeticException
     *                 if the argument number is negative
     * @throws IllegalArgumentException
     *                 if rootMC has precision 0
     * @see http://oldblog.novaloka.nl/blogger.xs4all.nl/novaloka/archive/2007/09/15/295396.html
     */
    public static BigDecimal bigSqrt(BigDecimal squarD, MathContext rootMC) {
       // General number and precision checking
      int sign = squarD.signum();
      if (sign == -1) throw new ArithmeticException("Square root of a negative number: " + squarD);
      if (sign == 0) return squarD.round(rootMC);

      int prec = rootMC.getPrecision();           // the requested precision
      if (prec == 0) throw new IllegalArgumentException("Most roots won't have infinite precision = 0");

      // Initial precision is that of double numbers 2^63/2 ~ 4E18
      int BITS = 62;                              // 63-1 an even number of number bits
      int nInit = 16;                             // precision seems 16 to 18 digits
      MathContext nMC = new MathContext(18, RoundingMode.HALF_DOWN);

      // Estimate the square root with the foremost 62 bits of squarD
      BigInteger bi = squarD.unscaledValue();     // bi and scale are a tandem
      int biLen = bi.bitLength();
      int shift = Math.max(0, biLen - BITS + (biLen%2 == 0 ? 0 : 1));   // even shift..
      bi = bi.shiftRight(shift);                  // ..floors to 62 or 63 bit BigInteger

      double root = Math.sqrt(SafeDoubleParser.doubleValue(bi));
      BigDecimal halfBack = new BigDecimal(BigInteger.ONE.shiftLeft(shift/2));

      int scale = squarD.scale();
      if (scale % 2 == 1) root *= SQRT_10; // 5 -> 2, -5 -> -3 need half a scale more..

      scale = (int) Math.ceil(scale/2.);         // ..where 100 -> 10 shifts the scale

      // Initial x - use double root - multiply by halfBack to unshift - set new scale
      BigDecimal x = new BigDecimal(root, nMC);
      x = x.multiply(halfBack, nMC);              // x0 ~ sqrt()
      if (scale != 0) x = x.movePointLeft(scale);

      if (prec < nInit) {                // for prec 15 root x0 must surely be OK
          return x.round(rootMC);        // return small prec roots without iterations
      }

      // Initial v - the reciprocal
      BigDecimal v = BigDecimal.ONE.divide(TWO.multiply(x), nMC);        // v0 = 1/(2*x)

      // Collect iteration precisions beforehand
      List<Integer> nPrecs = new ArrayList<Integer>();

      assert nInit > 3 : "Never ending loop!";                // assume nInit = 16 <= prec

      // Let m be the exact digits precision in an earlier! loop
      for (int m = prec + 1; m > nInit; m = m/2 + (m > 100 ? 1 : 2)) {
          nPrecs.add(m);
      }

      // The loop of "Square Root by Coupled Newton Iteration"
      for (int i = nPrecs.size() - 1; i > -1; i--) {
          // Increase precision - next iteration supplies n exact digits
          nMC = new MathContext(nPrecs.get(i), i%2 == 1 ? RoundingMode.HALF_UP : RoundingMode.HALF_DOWN);

          // Next x                                        // e = d - x^2
          BigDecimal e = squarD.subtract(x.multiply(x, nMC), nMC);
          if (i != 0) {
              x = x.add(e.multiply(v, nMC));               // x += e*v     ~ sqrt()
          } else {
              x = x.add(e.multiply(v, rootMC), rootMC);    // root x is ready!
              break;
          }

          // Next v                                        // g = 1 - 2*x*v
          BigDecimal g = BigDecimal.ONE.subtract(TWO.multiply(x).multiply(v, nMC));

          v = v.add(g.multiply(v, nMC));                   // v += g*v     ~ 1/2/sqrt()
      }

      return x;                      // return sqrt(squarD) with precision of rootMC
    }

    private void checkFloatDomain() {
        if (isNaN()) throw this.getRuntime().newFloatDomainError("NaN");
        if (isInfinity()) throw getRuntime().newFloatDomainError(infinityString());
    }
    
    private String infinityString() {
        return infinitySign == -1 ? "-Infinity" : "Infinity";
    }
    
    private boolean is_even(IRubyObject x) {
        if (x instanceof RubyFixnum) return RubyNumeric.fix2long((RubyFixnum) x) % 2 == 0;
        if (x instanceof RubyBignum) return RubyBignum.big2long((RubyBignum) x) % 2 == 0;

        return false;
    }
}
