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
import org.jruby.CompatVersion;
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
import org.jruby.RubyObject;
import org.jruby.RubyRational;
import org.jruby.RubyString;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyConstant;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Numeric;
import org.jruby.util.SafeDoubleParser;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
@JRubyClass(name="BigDecimal", parent="Numeric")
public class RubyBigDecimal extends RubyNumeric {
    private static final ObjectAllocator BIGDECIMAL_ALLOCATOR = new ObjectAllocator() {
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
        RubyClass bigDecimal = runtime.defineClass("BigDecimal", runtime.getNumeric(), BIGDECIMAL_ALLOCATOR);

        runtime.getKernel().defineAnnotatedMethods(BigDecimalKernelMethods.class);

        bigDecimal.setInternalModuleVariable("vpPrecLimit", RubyFixnum.zero(runtime));
        bigDecimal.setInternalModuleVariable("vpExceptionMode", RubyFixnum.zero(runtime));
        bigDecimal.setInternalModuleVariable("vpRoundingMode", runtime.newFixnum(ROUND_HALF_UP));
        
        bigDecimal.defineAnnotatedMethods(RubyBigDecimal.class);
        bigDecimal.defineAnnotatedConstants(RubyBigDecimal.class);

        if (runtime.is1_9()) {
            RubyModule bigMath = runtime.defineModule("BigMath");
            // TODO: BigMath.exp and BigMath.pow in native code

            bigDecimal.defineConstant("NAN", newNaN(runtime));
            bigDecimal.defineConstant("INFINITY", newInfinity(runtime, 1));
        }

        return bigDecimal;
    }

    private final boolean isNaN;
    private final int infinitySign;
    private final int zeroSign;
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
        @JRubyMethod(name = "BigDecimal", rest = true, module = true, visibility = Visibility.PRIVATE)
        public static IRubyObject newBigDecimal(IRubyObject recv, IRubyObject[] args) {
            return RubyBigDecimal.newBigDecimal(recv, args, Block.NULL_BLOCK);
        }
    }

    public static RubyBigDecimal newBigDecimal(IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        return newInstance(recv.getRuntime().getClass("BigDecimal"), args);
    }

    @JRubyMethod(name = "ver", meta = true)
    public static IRubyObject ver(IRubyObject recv) {
        return recv.getRuntime().newString("1.0.1");
    }

    @JRubyMethod(name = "_dump", optional = 1)
    public IRubyObject dump(IRubyObject[] args, Block unusedBlock) {
        RubyString precision = RubyString.newUnicodeString(getRuntime(), "0:");

        return precision.append(asString());
    }
        
    @JRubyMethod(name = "_load", required = 1, meta = true)
    public static RubyBigDecimal load(IRubyObject recv, IRubyObject from, Block block) {
        RubyBigDecimal rubyBigDecimal = (RubyBigDecimal) (((RubyClass)recv).allocate());
        String precisionAndValue = from.convertToString().asJavaString();
        String value = precisionAndValue.substring(precisionAndValue.indexOf(":")+1);
        rubyBigDecimal.value = new BigDecimal(value);
        return rubyBigDecimal;
    }

    @JRubyMethod(name = "double_fig", meta = true)
    public static IRubyObject double_fig(IRubyObject recv) {
        return recv.getRuntime().newFixnum(20);
    }
    
    @JRubyMethod(name = "limit", optional = 1, meta = true)
    public static IRubyObject limit(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        RubyModule c = (RubyModule)recv;
        IRubyObject nCur = c.searchInternalModuleVariable("vpPrecLimit");

        if (args.length > 0) {
            IRubyObject arg = args[0];
            if (!arg.isNil()) {
                if (!(arg instanceof RubyFixnum)) {
                    throw runtime.newTypeError(arg, runtime.getFixnum());
                }
                if (0 > ((RubyFixnum)arg).getLongValue()) {
                    throw runtime.newArgumentError("argument must be positive");
                }
                c.setInternalModuleVariable("vpPrecLimit", arg);
            }
        }

        return nCur;
    }

    @JRubyMethod(name = "save_limit", meta = true)
    public static IRubyObject save_limit(ThreadContext context, IRubyObject recv, Block block) {
        RubyModule c = (RubyModule)recv;
        IRubyObject nCur = c.searchInternalModuleVariable("vpPrecLimit");
        IRubyObject ret;

        try {
            ret = block.yieldSpecific(context);
        } finally {
            c.setInternalModuleVariable("vpPrecLimit", nCur);
        }

        return ret;
    }

    @JRubyMethod(name = "save_exception_mode", meta = true)
    public static IRubyObject save_exception_mode(ThreadContext context, IRubyObject recv, Block block) {
        RubyModule c = (RubyModule)recv;
        IRubyObject nCur = c.searchInternalModuleVariable("vpExceptionMode");
        IRubyObject ret;

        try {
            ret = block.yieldSpecific(context);
        } finally {
            c.setInternalModuleVariable("vpExceptionMode", nCur);
        }

        return ret;
    }

    @JRubyMethod(name = "save_rounding_mode", meta = true)
    public static IRubyObject save_rounding_mode(ThreadContext context, IRubyObject recv, Block block) {
        RubyModule c = (RubyModule)recv;
        IRubyObject nCur = c.searchInternalModuleVariable("vpRoundingMode");
        IRubyObject ret;

        try {
            ret = block.yieldSpecific(context);
        } finally {
            c.setInternalModuleVariable("vpRoundingMode", nCur);
        }

        return ret;
    }

    @JRubyMethod(name = "mode", required = 1, optional = 1, meta = true)
    public static IRubyObject mode(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        // FIXME: I doubt any of the constants referenced in this method
        // are ever redefined -- should compare to the known values, rather
        // than do an expensive constant lookup.
        Ruby runtime = recv.getRuntime();
        RubyClass clazz = runtime.getClass("BigDecimal");
        RubyModule c = (RubyModule)recv;
        
        args = Arity.scanArgs(runtime, args, 1, 1);
        
        IRubyObject mode = args[0];
        IRubyObject value = args[1];
        
        if (!(mode instanceof RubyFixnum)) {
            throw runtime.newTypeError("wrong argument type " + mode.getMetaClass() + " (expected Fixnum)");
        }
        
        long longMode = ((RubyFixnum)mode).getLongValue();
        long EXCEPTION_ALL = ((RubyFixnum)clazz.getConstant("EXCEPTION_ALL")).getLongValue();
        if ((longMode & EXCEPTION_ALL) != 0) {     
            if (value.isNil()) {
                return c.searchInternalModuleVariable("vpExceptionMode");
            }
            if (!(value.isNil()) && !(value instanceof RubyBoolean)) {
                throw runtime.newTypeError("second argument must be true or false");
            }

            RubyFixnum currentExceptionMode = (RubyFixnum)c.searchInternalModuleVariable("vpExceptionMode");
            RubyFixnum newExceptionMode = new RubyFixnum(runtime, currentExceptionMode.getLongValue());
            
            RubyFixnum EXCEPTION_INFINITY = (RubyFixnum)clazz.getConstant("EXCEPTION_INFINITY");
            if ((longMode & EXCEPTION_INFINITY.getLongValue()) != 0) {
                newExceptionMode = (value.isTrue()) ? (RubyFixnum)currentExceptionMode.callCoerced(context, "|", EXCEPTION_INFINITY)
                        : (RubyFixnum)currentExceptionMode.callCoerced(context, "&", new RubyFixnum(runtime, ~(EXCEPTION_INFINITY).getLongValue()));
            }
            
            RubyFixnum EXCEPTION_NaN = (RubyFixnum)clazz.getConstant("EXCEPTION_NaN");
            if ((longMode & EXCEPTION_NaN.getLongValue()) != 0) {
                newExceptionMode = (value.isTrue()) ? (RubyFixnum)currentExceptionMode.callCoerced(context, "|", EXCEPTION_NaN)
                        : (RubyFixnum)currentExceptionMode.callCoerced(context, "&", new RubyFixnum(runtime, ~(EXCEPTION_NaN).getLongValue()));
            }
            
            RubyFixnum EXCEPTION_UNDERFLOW = (RubyFixnum)clazz.getConstant("EXCEPTION_UNDERFLOW");
            if ((longMode & EXCEPTION_UNDERFLOW.getLongValue()) != 0) {
                newExceptionMode = (value.isTrue()) ? (RubyFixnum)currentExceptionMode.callCoerced(context, "|", EXCEPTION_UNDERFLOW)
                        : (RubyFixnum)currentExceptionMode.callCoerced(context, "&", new RubyFixnum(runtime, ~(EXCEPTION_UNDERFLOW).getLongValue()));
            }
            RubyFixnum EXCEPTION_OVERFLOW = (RubyFixnum)clazz.getConstant("EXCEPTION_OVERFLOW");
            if ((longMode & EXCEPTION_OVERFLOW.getLongValue()) != 0) {
                newExceptionMode = (value.isTrue()) ? (RubyFixnum)currentExceptionMode.callCoerced(context, "|", EXCEPTION_OVERFLOW)
                        : (RubyFixnum)currentExceptionMode.callCoerced(context, "&", new RubyFixnum(runtime, ~(EXCEPTION_OVERFLOW).getLongValue()));
            }
            c.setInternalModuleVariable("vpExceptionMode", newExceptionMode);
            return newExceptionMode;
        }
        
        long ROUND_MODE = ((RubyFixnum)clazz.getConstant("ROUND_MODE")).getLongValue();
        if (longMode == ROUND_MODE) {
            if (value.isNil()) {
                return c.searchInternalModuleVariable("vpRoundingMode");
            }
            if (!(value instanceof RubyFixnum)) {
                throw runtime.newTypeError("wrong argument type " + mode.getMetaClass() + " (expected Fixnum)");
            }
            
            RubyFixnum roundingMode = (RubyFixnum)value;
            if (roundingMode == clazz.getConstant("ROUND_UP") ||
                    roundingMode == clazz.getConstant("ROUND_DOWN") ||
                    roundingMode == clazz.getConstant("ROUND_FLOOR") ||
                    roundingMode == clazz.getConstant("ROUND_CEILING") ||
                    roundingMode == clazz.getConstant("ROUND_HALF_UP") ||
                    roundingMode == clazz.getConstant("ROUND_HALF_DOWN") ||
                    roundingMode == clazz.getConstant("ROUND_HALF_EVEN")) {
                c.setInternalModuleVariable("vpRoundingMode", roundingMode);
            } else {
                throw runtime.newTypeError("invalid rounding mode");
            }
            return c.searchInternalModuleVariable("vpRoundingMode");
        }
        throw runtime.newTypeError("first argument for BigDecimal#mode invalid");
    }

    private static RoundingMode getRoundingMode(Ruby runtime) {
        RubyFixnum roundingMode = (RubyFixnum)runtime.getClass("BigDecimal")
                .searchInternalModuleVariable("vpRoundingMode");
        return RoundingMode.valueOf((int)roundingMode.getLongValue());
    }

    private static boolean isNaNExceptionMode(Ruby runtime) {
        RubyFixnum currentExceptionMode = (RubyFixnum)runtime.getClass("BigDecimal")
                .searchInternalModuleVariable("vpExceptionMode");
        RubyFixnum EXCEPTION_NaN = (RubyFixnum)runtime.getClass("BigDecimal")
                .getConstant("EXCEPTION_NaN");
        return (currentExceptionMode.getLongValue() & EXCEPTION_NaN.getLongValue()) != 0;
    }

    private static boolean isInfinityExceptionMode(Ruby runtime) {
        RubyFixnum currentExceptionMode = (RubyFixnum)runtime.getClass("BigDecimal")
                .searchInternalModuleVariable("vpExceptionMode");
        RubyFixnum EXCEPTION_INFINITY = (RubyFixnum)runtime.getClass("BigDecimal")
                .getConstant("EXCEPTION_INFINITY");
        return (currentExceptionMode.getLongValue() & EXCEPTION_INFINITY.getLongValue()) != 0;
    }
    
    private static boolean isOverflowExceptionMode(Ruby runtime) {
        RubyFixnum currentExceptionMode = (RubyFixnum)runtime.getClass("BigDecimal")
                .searchInternalModuleVariable("vpExceptionMode");
        RubyFixnum EXCEPTION_OVERFLOW = (RubyFixnum)runtime.getClass("BigDecimal")
                .getConstant("EXCEPTION_OVERFLOW");
        return (currentExceptionMode.getLongValue() & EXCEPTION_OVERFLOW.getLongValue()) != 0;
    }
    
    private static RubyBigDecimal cannotBeCoerced(ThreadContext context, IRubyObject v, boolean must) {
        if (must) {
            String err;
            
            if (v.isImmediate()) {
                err = RubyObject.inspect(context, v).toString();
            } else {
                err = v.getMetaClass().getBaseName();
            }
            
            throw context.runtime.newTypeError(err + " can't be coerced into BigDecimal");
        }
        
        return null;
    }
    
    private static RubyBigDecimal unableToCoerceWithoutPrec(ThreadContext context, IRubyObject v, boolean must) {
        if (must) {
            throw context.runtime.newArgumentError(v.getMetaClass().getBaseName() + " can't be coerced into BigDecimal without a precision");
        }
        
        return null;
    }
    
    private static RubyBigDecimal getVpValue19(ThreadContext context, IRubyObject v, boolean must) {
        long precision;
        if (v instanceof RubyFloat) {
            precision = 0;
        } else if (v instanceof RubyRational) {
            precision = 0;
        } else {
            precision = -1;
        }
        return getVpValueWithPrec19(context, v, precision, must);
    }
    
    private static IRubyObject getVpRubyObjectWithPrec19Inner(ThreadContext context, RubyRational r, long precision, boolean must) {
        long numerator = RubyNumeric.num2long(r.numerator(context));
        long denominator = RubyNumeric.num2long(r.denominator(context));
            
        return new RubyBigDecimal(context.runtime, 
                BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), getRoundingMode(context.runtime)));
    }
    
    private static RubyBigDecimal getVpValueWithPrec19(ThreadContext context, IRubyObject value, long precision, boolean must) {
        while (true) {
            if (value instanceof RubyFloat) {
                if (precision > Long.MAX_VALUE) cannotBeCoerced(context, value, must);
            
                RubyFloat f = (RubyFloat)value;
                value = new RubyBigDecimal(context.runtime, BigDecimal.valueOf(f.getDoubleValue()));
                continue;
            } else if (value instanceof RubyRational) {
                if (precision < 0) return unableToCoerceWithoutPrec(context, value, must);
                
                value = getVpRubyObjectWithPrec19Inner(context, (RubyRational) value, precision, must);
                continue;
            } else if (value instanceof RubyBigDecimal) {
                return (RubyBigDecimal) value;
            } else if (value instanceof RubyFixnum || value instanceof RubyBignum) {
                String s = value.toString();
                return newInstance(value.getRuntime().getClass("BigDecimal"), new IRubyObject[]{value.getRuntime().newString(s)});            
            } 
            
            return cannotBeCoerced(context, value, must);            
        }
    }

    private static RubyBigDecimal getVpValue(IRubyObject v, boolean must) {
        if (v instanceof RubyBigDecimal) {
            return (RubyBigDecimal)v;
        } else if (v instanceof RubyFixnum || v instanceof RubyBignum) {
            String s = v.toString();
            return newInstance(v.getRuntime().getClass("BigDecimal"), new IRubyObject[]{v.getRuntime().newString(s)});
        }

        return cannotBeCoerced(v.getRuntime().getCurrentContext(), v, must);
    }

    @JRubyMethod(name = "induced_from", required = 1, meta = true)
    public static IRubyObject induced_from(IRubyObject recv, IRubyObject arg) {
        return getVpValue(arg, true);
    }

    private final static Pattern INFINITY_PATTERN = Pattern.compile("^([+-])?Infinity$");
    private final static Pattern NUMBER_PATTERN
            = Pattern.compile("^([+-]?\\d*\\.?\\d*([eE][+-]?)?\\d*).*");
    
    @JRubyMethod(name = "new", required = 1, optional = 1, meta = true)
    public static RubyBigDecimal newInstance(IRubyObject recv, IRubyObject[] args) {
        BigDecimal decimal;
        Ruby runtime = recv.getRuntime();
        ThreadContext ctx = runtime.getCurrentContext();

        if (args.length == 0) { 
            decimal = new BigDecimal(0);
        } else {
            MathContext context = MathContext.UNLIMITED;

            if (args.length == 2) {
                int digits = (int)args[1].convertToInteger().getLongValue();
                if (digits < 0) {
                    throw runtime.newArgumentError("argument must be positive");
                }
                context = new MathContext(digits);
            }

            if (runtime.is1_9()) {
                if (args[0] instanceof RubyBigDecimal) {
                    return new RubyBigDecimal(runtime, (RubyClass)recv, ((RubyBigDecimal)args[0]));
                } else if (args[0] instanceof RubyFloat || args[0] instanceof RubyRational) {
                    if (args.length != 2) {
                        // float input must be accompanied by precision
                        throw runtime.newArgumentError("can't omit precision for a rational");
                    }

                    if (args[0] instanceof RubyFloat) {
                        // precision can be no more than float digits
                        if (context.getPrecision() > RubyFloat.DIG + 1) {
                            throw runtime.newArgumentError("precision too large");
                        }
                        return new RubyBigDecimal(runtime, (RubyClass)recv, new BigDecimal(((RubyFloat)args[0]).getDoubleValue(), context));
                    } else {
                        RubyRational rat = (RubyRational)args[0];

                        BigDecimal num = new BigDecimal(rat.numerator(ctx).convertToInteger().getLongValue());
                        BigDecimal den = new BigDecimal(rat.denominator(ctx).convertToInteger().getLongValue());

                        BigDecimal value = num.divide(den, context);

                        return new RubyBigDecimal(runtime, value);
                    }
                } else if (args[0] instanceof RubyFixnum) {
                    return new RubyBigDecimal(runtime, (RubyClass)recv, new BigDecimal(((RubyFixnum)args[0]).getLongValue(), context));
                } else if (args[0] instanceof RubyBignum) {
                    return new RubyBigDecimal(runtime, (RubyClass)recv, new BigDecimal(((RubyBignum)args[0]).getBigIntegerValue(), context));
                }
                // fall through to String coercion below
            }

            String strValue = args[0].convertToString().toString();
            strValue = strValue.trim();
            if ("NaN".equals(strValue)) {
                return newNaN(runtime);
            }

            Matcher m = INFINITY_PATTERN.matcher(strValue);
            if (m.matches()) {
                int sign = 1;
                String signGroup = m.group(1);
                if ("-".equals(signGroup)) {
                    sign = -1;
                }
                return newInfinity(runtime, sign);
            }

            // Clean-up string representation so that it could be understood
            // by Java's BigDecimal. Not terribly efficient for now.
            // 1. MRI allows d and D as exponent separators
            strValue = strValue.replaceFirst("[dD]", "E");
            // 2. MRI allows underscores anywhere
            strValue = strValue.replaceAll("_", "");
            // 3. MRI ignores the trailing junk
            strValue = NUMBER_PATTERN.matcher(strValue).replaceFirst("$1");

            try {
                decimal = new BigDecimal(strValue, context);
            } catch(NumberFormatException e) {
                if (isOverflowExceptionMode(runtime)) {
                    throw runtime.newFloatDomainError("exponent overflow");
                }
                decimal = new BigDecimal(0);
            }
            if (decimal.signum() == 0) {
                // MRI behavior: -0 and +0 are two different things
                if (strValue.matches("^\\s*-.*")) {
                    return newZero(runtime, -1);
                } else {
                    return newZero(runtime, 1);
                }
            }
        }
        return new RubyBigDecimal(runtime, (RubyClass)recv, decimal);
    }

    private static RubyBigDecimal newZero(Ruby runtime, int sign) {
        int zeroSign;
        if (sign < 0) {
            zeroSign = -1;
        } else {
            zeroSign = 1;
        }
        RubyBigDecimal rbd = new RubyBigDecimal(runtime, BigDecimal.ZERO, 0, zeroSign);
        return rbd;
    }

    private static RubyBigDecimal newNaN(Ruby runtime) {
        if (isNaNExceptionMode(runtime)) throw runtime.newFloatDomainError("Computation results to 'NaN'(Not a Number)");

        RubyBigDecimal rbd = new RubyBigDecimal(runtime, BigDecimal.ZERO, true);
        return rbd;
    }
    
    private static RubyBigDecimal newInfinity(Ruby runtime, int sign) {
        int infinitySign;
        if (sign < 0) {
            infinitySign = -1;
        } else {
            infinitySign = 1;
        }
        RubyBigDecimal rbd =  new RubyBigDecimal(runtime, BigDecimal.ZERO, infinitySign);
        if (isInfinityExceptionMode(runtime)) throw runtime.newFloatDomainError("Computation results to 'Infinity'");

        return rbd;
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
    @JRubyMethod(name = "hash")
    public RubyFixnum hash() {
        return getRuntime().newFixnum(value.stripTrailingZeros().hashCode());
    }

    @JRubyMethod(name = {"%", "modulo"}, required = 1, compat = CompatVersion.RUBY1_8)
    public IRubyObject op_mod(ThreadContext context, IRubyObject arg) {
        // TODO: full-precision remainder is 1000x slower than MRI!
        Ruby runtime = context.runtime;
        if (isInfinity() || isNaN()) {
            return newNaN(runtime);
        }
        RubyBigDecimal val = getVpValue(arg, false);
        if (val == null) {
            return callCoerced(context, "%", arg, true);
        }
        if (val.isInfinity() || val.isNaN() || val.isZero()) {
            return newNaN(runtime);
        }

        // Java and MRI definitions of modulo are different.
        BigDecimal modulo = value.remainder(val.value);
        if (modulo.signum() * val.value.signum() < 0) {
            modulo = modulo.add(val.value);
        }

        return new RubyBigDecimal(runtime, modulo).setResult();
    }

    @JRubyMethod(name = {"%", "modulo"}, required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject op_mod19(ThreadContext context, IRubyObject other) {
        // TODO: full-precision divmod is 1000x slower than MRI!
        Ruby runtime = context.runtime;
        RubyBigDecimal val = getVpValue19(context, other, false);
        if (val == null) {
            return callCoerced(context, "%", other, true);
        }
        if (isNaN() || val.isNaN() || (isInfinity() && val.isInfinity())) {
            return newNaN(runtime);
        }
        if (val.isZero()) {
            throw context.runtime.newZeroDivisionError();
        }
        if (isInfinity()) {
            return newNaN(runtime);
        }
        if (val.isInfinity()) {
            return this;
        }
        if (isZero()) {
            return newZero(runtime, value.signum());
        }

        // Java and MRI definitions of modulo are different.
        BigDecimal modulo = value.remainder(val.value);
        if (modulo.signum() * val.value.signum() < 0) {
            modulo = modulo.add(val.value);
        }

        return new RubyBigDecimal(runtime, modulo).setResult();
    }

    @Override
    @JRubyMethod(name = "remainder", required = 1, compat = CompatVersion.RUBY1_8)
    public IRubyObject remainder(ThreadContext context, IRubyObject arg) {
        RubyBigDecimal val = getVpValue(arg, false);
        return remainderInternal(context, val, arg);
    }

    @JRubyMethod(name = "remainder", required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject remainder19(ThreadContext context, IRubyObject arg) {
        RubyBigDecimal val = getVpValue19(context, arg, false);
        return remainderInternal(context, val, arg);
    }

    private IRubyObject remainderInternal(ThreadContext context, RubyBigDecimal val, IRubyObject arg) {
     // TODO: full-precision remainder is 1000x slower than MRI!
        Ruby runtime = context.runtime;
        if (isInfinity() || isNaN()) {
            return newNaN(runtime);
        }

        if (val == null) {
            return callCoerced(context, "remainder", arg, true);
        }
        if (val.isInfinity() || val.isNaN() || val.isZero()) {
            return newNaN(runtime);
        }

        // Java and MRI definitions of remainder are the same.
        return new RubyBigDecimal(runtime, value.remainder(val.value)).setResult();
    }

    @JRubyMethod(name = "*", required = 1, compat = CompatVersion.RUBY1_8)
    public IRubyObject op_mul(ThreadContext context, IRubyObject arg) {
        return mult2(context, arg, getRuntime().getClass("BigDecimal")
                .searchInternalModuleVariable("vpPrecLimit"));
    }

    @JRubyMethod(name = "*", required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject op_mul19(ThreadContext context, IRubyObject arg) {
        return mult219(context, arg, getRuntime().getClass("BigDecimal")
                .searchInternalModuleVariable("vpPrecLimit"));
    }

    @JRubyMethod(name = "mult", required = 2, compat = CompatVersion.RUBY1_8)
    public IRubyObject mult2(ThreadContext context, IRubyObject b, IRubyObject n) {
        RubyBigDecimal val = getVpValue(b, false);
        return multInternal(context, val, b, n);
    }

    @JRubyMethod(name = "mult", required = 2, compat = CompatVersion.RUBY1_9)
    public IRubyObject mult219(ThreadContext context, IRubyObject b, IRubyObject n) {
        RubyBigDecimal val = getVpValue19(context, b, false);
        return multInternal(context, val, b, n);
    }

    private IRubyObject multInternal(ThreadContext context, RubyBigDecimal val, IRubyObject b, IRubyObject n) {
        Ruby runtime = context.runtime;

        if (val == null) {
            // TODO: what about n arg?
            return callCoerced(context, "*", b);
        }

        int digits = RubyNumeric.fix2int(n);

        if (isNaN() || val.isNaN()) {
            return newNaN(runtime);
        }

        if  ((isInfinity() && val.isZero()) || (isZero() && val.isInfinity())) {
            return newNaN(runtime);
        }

        if (isZero() || val.isZero()) {
            int sign1 = isZero()? zeroSign : value.signum();
            int sign2 = val.isZero() ?  val.zeroSign : val.value.signum();
            return newZero(runtime, sign1 * sign2);
        }

        if (isInfinity() || val.isInfinity()) {
            int sign1 = isInfinity() ? infinitySign : value.signum();
            int sign2 = val.isInfinity() ? val.infinitySign : val.value.signum();
            return newInfinity(runtime, sign1 * sign2);
        }

        BigDecimal res = value.multiply(val.value);
        if (res.precision() > digits) {
            // TODO: rounding mode should not be hard-coded. See #mode.
            res = res.round(new MathContext(digits,  RoundingMode.HALF_UP));
        }
        return new RubyBigDecimal(runtime, res).setResult();
    }

    @JRubyMethod(name = {"**", "power"}, required = 1, compat = CompatVersion.RUBY1_8)
    public IRubyObject op_pow(IRubyObject arg) {
        if (!(arg instanceof RubyFixnum)) {
            throw getRuntime().newTypeError("wrong argument type " + arg.getMetaClass() + " (expected Fixnum)");
        }

        if (isNaN() || isInfinity()) {
            return newNaN(getRuntime());
        }

        int times = RubyNumeric.fix2int(arg.convertToInteger());

        if (times < 0) {
            if (isZero()) {
                return newInfinity(getRuntime(), value.signum());
            }

            // Note: MRI has a very non-trivial way of calculating the precision,
            // so we use very simple approximation here:
            int precision = (-times + 4) * (getAllDigits().length() + 4);

            return new RubyBigDecimal(getRuntime(),
                    value.pow(times, new MathContext(precision, RoundingMode.HALF_UP)));
        } else {
            return new RubyBigDecimal(getRuntime(), value.pow(times));
        }
    }

    @JRubyMethod(name = {"**", "power"}, required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject op_pow19(IRubyObject exp) {
        if (!(exp instanceof RubyFixnum)) {
            throw getRuntime().newTypeError("wrong argument type " + exp.getMetaClass() + " (expected Fixnum)");
        }
        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();

        if (isNaN()) {
            return newNaN(runtime);
        }
        if (isInfinity()) {
            if (Numeric.f_negative_p(context, exp)) {
                if (infinitySign < 0) {
                    if (Numeric.f_integer_p(context, exp).isTrue()) {
                        if (is_even(exp)) {
                            /* (-Infinity) ** (-even_integer) -> +0 */
                            return newZero(runtime, 1);
                        } else {
                            /* (-Infinity) ** (-odd_integer) -> -0 */
                            return newZero(runtime, -1);
                        }
                    } else {
                        /* (-Infinity) ** (-non_integer) -> -0 */
                        return newZero(runtime, -1);
                    }
                } else {
                    return newZero(runtime, 0);

                }
            } else {
                if (infinitySign < 0) {
                    if (Numeric.f_integer_p(context, exp).isTrue()) {
                        if (is_even(exp)) {
                            return newInfinity(runtime, 1);
                        } else {
                            return newInfinity(runtime, -1);
                        }
                    } else {
                        throw runtime.newMathDomainError("a non-integral exponent for a negative base");
                    }
                } else {
                    return newInfinity(runtime, 1);
                }
            }
        }

        int times = RubyNumeric.fix2int(exp.convertToInteger());

        if (times < 0) {
            if (isZero()) {
                return newInfinity(getRuntime(), value.signum());
            }

            // Note: MRI has a very non-trivial way of calculating the precision,
            // so we use very simple approximation here:
            int precision = (-times + 4) * (getAllDigits().length() + 4);

            return new RubyBigDecimal(getRuntime(),
                    value.pow(times, new MathContext(precision, RoundingMode.HALF_UP)));
        } else {
            return new RubyBigDecimal(getRuntime(), value.pow(times));
        }
    }

    @JRubyMethod(name = "+", compat = CompatVersion.RUBY1_8)
    public IRubyObject op_plus(ThreadContext context, IRubyObject b) {
        RubyBigDecimal val = getVpValue(b, false);
        return addInternal(context, val, b, getRuntime().getClass("BigDecimal")
                .searchInternalModuleVariable("vpPrecLimit"));
    }

    @JRubyMethod(name = "+", compat = CompatVersion.RUBY1_9)
    public IRubyObject op_plus19(ThreadContext context, IRubyObject b) {
        RubyBigDecimal val = getVpValue19(context, b, false);
        return addInternal(context, val, b, getRuntime().getClass("BigDecimal")
                .searchInternalModuleVariable("vpPrecLimit"));
    }

    @JRubyMethod(name = "add", compat = CompatVersion.RUBY1_8)
    public IRubyObject add2(ThreadContext context, IRubyObject b, IRubyObject digits) {
        RubyBigDecimal val = getVpValue(b, false);
        return addInternal(context, val, b, digits);
    }

    @JRubyMethod(name = "add", compat = CompatVersion.RUBY1_9)
    public IRubyObject add219(ThreadContext context, IRubyObject b, IRubyObject digits) {
        RubyBigDecimal val = getVpValue19(context, b, false);
        return addInternal(context, val, b, digits);
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
        if (res != null) {
            return res;
        }
        RoundingMode roundMode = getRoundingMode(runtime);
        return new RubyBigDecimal(runtime, value.add(
                val.value, new MathContext(prec, roundMode))); // TODO: why this: .setResult();
    }

    private int getPositiveInt(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;

        if (arg instanceof RubyFixnum) {
            int value = RubyNumeric.fix2int(arg);
            if (value < 0) {
                throw runtime.newArgumentError("argument must be positive");
            }
            return value;
        } else {
            throw runtime.newTypeError(arg, runtime.getFixnum());
        }
    }

    private RubyBigDecimal handleAddSpecialValues(RubyBigDecimal val) {
        if (isNaN() || val.isNaN) {
            return newNaN(getRuntime());
        }

        int sign = infinitySign * val.infinitySign;
        if (sign > 0) {
            return isInfinity() ? this : val;
        }
        if (sign < 0) {
            return newNaN(getRuntime());
        }
        if (sign == 0) {
            sign = infinitySign + val.infinitySign;
            if (sign != 0) {
                return newInfinity(getRuntime(), sign);
            }
        }
        return null;
    }

    @Override
    @JRubyMethod(name = "+@")
    public IRubyObject op_uplus() {
        return this;
    }
    
    @JRubyMethod(name = "-", required = 1, compat = CompatVersion.RUBY1_8)
    public IRubyObject op_minus(ThreadContext context, IRubyObject b) {
        RubyBigDecimal val = getVpValue(b, false);
        return subInternal(context, val, b);
    }

    @JRubyMethod(name = "-", required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject op_minus19(ThreadContext context, IRubyObject b) {
        return subInternal(context, getVpValue19(context, b, true), b);
    }

    @JRubyMethod(name = "sub", required = 2, compat = CompatVersion.RUBY1_8)
    public IRubyObject sub2(ThreadContext context, IRubyObject b, IRubyObject n) {
        RubyBigDecimal val = getVpValue(b, false);
        return subInternal(context, val, b);
    }

    @JRubyMethod(name = "sub", required = 2, compat = CompatVersion.RUBY1_9)
    public IRubyObject sub219(ThreadContext context, IRubyObject b, IRubyObject n) {
        RubyBigDecimal val = getVpValue19(context, b, false);
        return subInternal(context, val, b);
    }

    private IRubyObject subInternal(ThreadContext context, RubyBigDecimal val, IRubyObject b) {
        if (val == null) {
            return callCoerced(context, "-", b);
        }
        RubyBigDecimal res = handleMinusSpecialValues(val);
        if (res != null) {
            return res;
        }
        return new RubyBigDecimal(getRuntime(), value.subtract(val.value)).setResult();
    }

    private RubyBigDecimal handleMinusSpecialValues(RubyBigDecimal val) {
        if (isNaN() || val.isNaN()) {
            return newNaN(getRuntime());
        }

        int sign = infinitySign * val.infinitySign;
        if (sign > 0) {
            return newNaN(getRuntime());
        }
        if (sign < 0) {
            return this;
        }
        if (sign == 0) {
            if (isInfinity()) {
                return this;
            }
            if (val.isInfinity()) {
                return newInfinity(getRuntime(), val.infinitySign * -1);
            }
            sign = infinitySign + val.infinitySign;
            if (sign != 0) {
                return newInfinity(getRuntime(), sign);
            }
        }
        return null;
    }

    @JRubyMethod(name = "-@")
    public IRubyObject op_uminus() {
        Ruby runtime = getRuntime();
        if (isNaN()) {
            return newNaN(runtime);
        }
        if (isInfinity()) {
            return newInfinity(runtime, -infinitySign);
        }
        if (isZero()) {
            return newZero(runtime, -zeroSign);
        }
        return new RubyBigDecimal(getRuntime(), value.negate());
    }

    @JRubyMethod(name = {"/", "quo"}, compat = CompatVersion.RUBY1_8)
    public IRubyObject op_quo(ThreadContext context, IRubyObject other) {
        // regular division with some default precision
        // TODO: proper algorithm to set the precision
        return convertDivResult(context, other, 
                op_div(context, other, getRuntime().newFixnum(200)));
    }

    @JRubyMethod(name = {"/", "quo"}, compat = CompatVersion.RUBY1_9)
    public IRubyObject op_quo19(ThreadContext context, IRubyObject other) {   
        return op_quo19_20(context, other);
    }
    
    @JRubyMethod(name = {"/", "quo"}, compat = CompatVersion.RUBY2_0)
    public IRubyObject op_quo20(ThreadContext context, IRubyObject other) {
        return op_quo19_20(context, other);
    }
    
    private IRubyObject op_quo19_20(ThreadContext context, IRubyObject other) {
        RubyObject preciseOther = getVpValue19(context, other, true);
        // regular division with some default precision
        // TODO: proper algorithm to set the precision
        return op_div(context, preciseOther, getRuntime().newFixnum(200));
    }
    
    private IRubyObject convertDivResult(ThreadContext context, IRubyObject other, IRubyObject result) {
        if (other instanceof RubyFloat && result instanceof RubyBigDecimal) 
            result = ((RubyBigDecimal)result).convertToFloat();
        return result;        
    }
    
    @JRubyMethod(name = "div", compat = CompatVersion.RUBY1_8)
    public IRubyObject op_div(ThreadContext context, IRubyObject other) {
        // integer division
        RubyBigDecimal val = getVpValue(other, false);
        if (val == null) {
            return callCoerced(context, "div", other);
        }

        if (isNaN() || val.isZero() || val.isNaN()) {
            return newNaN(getRuntime());
        }

        if (isInfinity() || val.isInfinity()) {
            return newNaN(getRuntime());
        }

        return new RubyBigDecimal(getRuntime(),
                this.value.divideToIntegralValue(val.value)).setResult();
    }

    @JRubyMethod(name = "div", compat = CompatVersion.RUBY1_9)
    public IRubyObject op_div19(ThreadContext context, IRubyObject r) {
        RubyBigDecimal val = getVpValue19(context, r, true);
        
        if (val == null) return cannotBeCoerced(context, val, true);

        if (isNaN() || val.isNaN()) {
            throw context.runtime.newFloatDomainError("Computation results to 'NaN'");
        }

        if (isInfinity() && val.isOne()) {
            throw context.runtime.newFloatDomainError("Computation results to 'Infinity'");
        }
        
        if (val.isInfinity()) {
            return newZero(getRuntime(), val.infinitySign);
        }

        if (isZero() || val.isZero()) {
            throw context.runtime.newZeroDivisionError();
        }

        return op_div(context, r);
    }

    @JRubyMethod(name = "div", compat = CompatVersion.RUBY1_8)
    public IRubyObject op_div(ThreadContext context, IRubyObject other, IRubyObject digits) {
        // TODO: take BigDecimal.mode into account.

        int scale = RubyNumeric.fix2int(digits);

        RubyBigDecimal val = getVpValue(other, false);
        if (val == null) {
            return callCoerced(context, "/", other);
        }

        if (isNaN() || (isZero() && val.isZero()) || val.isNaN()) {
            return newNaN(getRuntime());
        }

        if (val.isZero()) {
            int sign1 = isInfinity() ? infinitySign : value.signum();
            return newInfinity(getRuntime(), sign1 * val.zeroSign);
        }

        if (isInfinity() && !val.isInfinity()) {
            return newInfinity(getRuntime(), infinitySign * val.value.signum());
        }

        if (!isInfinity() && val.isInfinity()) {
            return newZero(getRuntime(), value.signum() * val.infinitySign);
        }

        if (isInfinity() && val.isInfinity()) {
            return newNaN(getRuntime());
        }

        if (isZero()) {
            return newZero(getRuntime(), zeroSign * val.value.signum());
        }

        if (scale == 0) {
            // MRI behavior: "If digits is 0, the result is the same as the / operator."
            return op_quo(context, other);
        } else {
            // TODO: better algorithm to set precision needed
            int prec = Math.max(200, scale);
            return new RubyBigDecimal(getRuntime(),
                    value.divide(val.value, new MathContext(prec, RoundingMode.HALF_UP))).setResult(scale);
        }
    }
    
    @JRubyMethod(name = "div", compat = CompatVersion.RUBY1_9)
    public IRubyObject op_div19(ThreadContext context, IRubyObject other, IRubyObject digits) {
        RubyBigDecimal val = getVpValue(other, false);
        if (isNaN() || val.isNaN()) {
            throw context.runtime.newFloatDomainError("Computation results to 'NaN'");
        }
        
        return op_div(context, other, digits);
    }

    private IRubyObject cmp(ThreadContext context, IRubyObject r, char op) {
        int e = 0;
        RubyBigDecimal rb = getVpValue(r, false);
        if (rb == null) {
            IRubyObject ee = callCoerced(context, "<=>", r);
            if (ee.isNil()) {
                if (op == '*') {
                    return getRuntime().getNil();
                } else if (op == '=' || isNaN()){
                    return getRuntime().getFalse();
                } else {
                    throw getRuntime().newArgumentError("nil could not be coerced into a BigDecmil");
                }
            }
            e = RubyNumeric.fix2int(ee);
        } else {
            if (isNaN() | rb.isNaN()) {
                return (op == '*') ? getRuntime().getNil() : getRuntime().getFalse();
            }
            if (infinitySign != 0 || rb.infinitySign != 0) {
                e = infinitySign - rb.infinitySign;
            } else {
                e = value.compareTo(rb.value);
            }
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

    @JRubyMethod(name = "abs")
    public IRubyObject abs() {
        Ruby runtime = getRuntime();
        if (isNaN) {
            return newNaN(runtime);
        }
        if (isInfinity()) {
            return newInfinity(runtime, 1);
        }
        return new RubyBigDecimal(getRuntime(), value.abs()).setResult();
    }

    @JRubyMethod(name = "ceil", optional = 1)
    public IRubyObject ceil(IRubyObject[] args) {
        if (isNaN) {
            return newNaN(getRuntime());
        }
        if (isInfinity()) {
            return newInfinity(getRuntime(), infinitySign);
        }

        int n = 0;
        if (args.length > 0) {
            n = RubyNumeric.fix2int(args[0]);
        }
        
        if (value.scale() > n) { // rounding neccessary
            return new RubyBigDecimal(getRuntime(),
                    value.setScale(n, RoundingMode.CEILING));
        } else {
            return this;
        }
    }

    @JRubyMethod(name = "ceil", optional = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject ceil19(IRubyObject[] args) {
        checkFloatDomain();
        
        if (args.length == 0) {
            BigInteger ceil = value.setScale(0, RoundingMode.CEILING).toBigInteger();
            if (ceil.compareTo(BigInteger.valueOf((long)ceil.intValue())) == 0) {
                // it fits in Fixnum
                return RubyInteger.int2fix(getRuntime(), ceil.intValue());
            }
            return RubyBignum.newBignum(getRuntime(), ceil);
        }

        return ceil(args);
    }

    @JRubyMethod(name = "coerce", required = 1)
    @Override
    public IRubyObject coerce(IRubyObject other) {
        IRubyObject obj;
        if (other instanceof RubyFloat) {
            obj = getRuntime().newArray(other, to_f());
        } else {
            obj = getRuntime().newArray(getVpValue(other, true), this);
        }
        return obj;
    }

    @Override
    public double getDoubleValue() { return SafeDoubleParser.doubleValue(value); }
    
    @Override
    public long getLongValue() { return value.longValue(); }

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
    @JRubyMethod(name = "divmod", required = 1, compat = CompatVersion.RUBY1_8)
    public IRubyObject divmod(ThreadContext context, IRubyObject other) {
        // TODO: full-precision divmod is 1000x slower than MRI!
        Ruby runtime = context.runtime;
        if (isInfinity() || isNaN()) {
            return RubyArray.newArray(runtime, newNaN(runtime), newNaN(runtime));
        }
        RubyBigDecimal val = getVpValue(other, false);
        if (val == null) {
            return callCoerced(context, "divmod", other, true);
        }
        if (val.isInfinity() || val.isNaN() || val.isZero()) {
            return RubyArray.newArray(runtime, newNaN(runtime), newNaN(runtime));
        }

        // Java and MRI definitions of divmod are different.        
        BigDecimal[] divmod = value.divideAndRemainder(val.value);

        BigDecimal div = divmod[0];
        BigDecimal mod = divmod[1];

        if (mod.signum() * val.value.signum() < 0) {
            div = div.subtract(BigDecimal.ONE);
            mod = mod.add(val.value);
        }

        return RubyArray.newArray(runtime,
                new RubyBigDecimal(runtime, div),
                new RubyBigDecimal(runtime, mod));
    }
    
    @Override
    @JRubyMethod(name = "divmod", required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject divmod19(ThreadContext context, IRubyObject other) {
        // TODO: full-precision divmod is 1000x slower than MRI!
        Ruby runtime = context.runtime;
        RubyBigDecimal val = getVpValue19(context, other, false);
        if (val == null) {
            return callCoerced(context, "divmod", other, true);
        }
        if (isNaN() || val.isNaN() || (isInfinity() && val.isInfinity())) {
            return RubyArray.newArray(runtime, newNaN(runtime), newNaN(runtime));
        }
        if (val.isZero()) {
            throw context.runtime.newZeroDivisionError();
        }
        if (isInfinity()) {
            int sign = (infinitySign == val.value.signum()) ? 1 : -1;
            return RubyArray.newArray(runtime, newInfinity(runtime, sign), newNaN(runtime));
        }
        if (val.isInfinity()) {
            return RubyArray.newArray(runtime, newZero(runtime, val.value.signum()), this);
        }
        if (isZero()) {
            return RubyArray.newArray(runtime, newZero(runtime,
                    value.signum()), newZero(runtime, value.signum()));
        }

        // Java and MRI definitions of divmod are different.
        BigDecimal[] divmod = value.divideAndRemainder(val.value);

        BigDecimal div = divmod[0];
        BigDecimal mod = divmod[1];

        if (mod.signum() * val.value.signum() < 0) {
            div = div.subtract(BigDecimal.ONE);
            mod = mod.add(val.value);
        }

        return RubyArray.newArray(runtime,
                new RubyBigDecimal(runtime, div),
                new RubyBigDecimal(runtime, mod));
    }
    
    @JRubyMethod(name = "exponent")
    public IRubyObject exponent() {
        return getRuntime().newFixnum(getExponent());
    }

    @JRubyMethod(name = "finite?")
    public IRubyObject finite_p() {
        if (isNaN()) {
            return getRuntime().getFalse();
        }
        return getRuntime().newBoolean(!isInfinity());
    }

    @JRubyMethod(name = "floor", optional = 1)
    public IRubyObject floor(IRubyObject[]args) {
        if (isNaN) {
            return newNaN(getRuntime());
        }
        if (isInfinity()) {
            return newInfinity(getRuntime(), infinitySign);
        }

        int n = 0;
        if (args.length > 0) {
            n = RubyNumeric.fix2int(args[0]);
        }

        if (value.scale() > n) { // rounding neccessary
            return new RubyBigDecimal(getRuntime(),
                    value.setScale(n, RoundingMode.FLOOR));
        } else {
            return this;
        }
    }

    @JRubyMethod(name = "floor", optional =1, compat = CompatVersion.RUBY1_9)
    public IRubyObject floor19(IRubyObject[] args) {
        if (isNaN || isInfinity()) {
            throw getRuntime().newFloatDomainError("Computation results to '" + to_s(args).asJavaString() + "'");
        }
        return floor(args);
    }
 
    @JRubyMethod(name = "frac")
    public IRubyObject frac() {
        if (isNaN) {
            return newNaN(getRuntime());
        }
        if (isInfinity()) {
            return newInfinity(getRuntime(), infinitySign);
        }
        if (value.scale() > 0 && value.precision() < value.scale()) {
            return new RubyBigDecimal(getRuntime(), value);
        }

        BigDecimal val = value.subtract(((RubyBigDecimal)fix()).value);
        return new RubyBigDecimal(getRuntime(), val);
    }

    @JRubyMethod(name = "infinite?")
    public IRubyObject infinite_p() {
        if (infinitySign == 0) {
            return getRuntime().getNil();
        }
        return getRuntime().newFixnum(infinitySign);
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        StringBuilder val = new StringBuilder("#<BigDecimal:").append(Integer.toHexString(System.identityHashCode(this))).append(",");
        val.append("'").append(this.callMethod(context, "to_s")).append("'").append(",");

        val.append(getSignificantDigits().length()).append("(");

        int len = getAllDigits().length();
        int pow = len / 4;
        val.append((pow + 1) * 4).append(")").append(">");

        return getRuntime().newString(val.toString());
    }

    @JRubyMethod(name = "nan?")
    public IRubyObject nan_p() {
        return getRuntime().newBoolean(isNaN);
    }

    @JRubyMethod(name = "nonzero?")
    public IRubyObject nonzero_p() {
        return isZero() ? getRuntime().getNil() : this;
    }
 
    @JRubyMethod(name = "precs")
    public IRubyObject precs() {
        final Ruby runtime = getRuntime();
        final IRubyObject[] array = new IRubyObject[2];

        array[0] = runtime.newFixnum(getSignificantDigits().length());

        int len = getAllDigits().length();
        int pow = len / 4;
        array[1] = runtime.newFixnum((pow + 1) * 4);

        return RubyArray.newArrayNoCopy(runtime, array);
    }
    
    @Deprecated
    public IRubyObject round(IRubyObject[] args) {
        return round(getRuntime().getCurrentContext(), args);
    }

    @JRubyMethod(name = "round", optional = 2)
    public IRubyObject round(ThreadContext context, IRubyObject[] args) {
        int scale = args.length > 0 ? num2int(args[0]) : 0;
        RoundingMode mode = (args.length > 1) ? javaRoundingModeFromRubyRoundingMode(args[1]) : getRoundingMode(context.runtime);
        // JRUBY-914: Java 1.4 BigDecimal does not allow a negative scale, so we have to simulate it
        if (scale < 0) {
          // shift the decimal point just to the right of the digit to be rounded to (divide by 10**(abs(scale)))
          // -1 -> 10's digit, -2 -> 100's digit, etc.
          BigDecimal normalized = value.movePointRight(scale);
          // ...round to that digit
          BigDecimal rounded = normalized.setScale(0, mode);
          // ...and shift the result back to the left (multiply by 10**(abs(scale)))
          return new RubyBigDecimal(getRuntime(), rounded.movePointLeft(scale));
        } else {
          return new RubyBigDecimal(getRuntime(), value.setScale(scale, mode));
        }
    }

    //this relies on the Ruby rounding enumerations == Java ones, which they (currently) all are
    private RoundingMode javaRoundingModeFromRubyRoundingMode(IRubyObject arg) {
      return RoundingMode.valueOf(num2int(arg));
    }
    
    @JRubyMethod(name = "sign")
    public IRubyObject sign() {
        if (isNaN()) {
            return getMetaClass().getConstant("SIGN_NaN");
        }

        if (isInfinity()) {
            if (infinitySign < 0) {
                return getMetaClass().getConstant("SIGN_NEGATIVE_INFINITE");
            } else {
                return getMetaClass().getConstant("SIGN_POSITIVE_INFINITE");
            }
        }

        if (isZero()) {
            if (zeroSign < 0) {
                return getMetaClass().getConstant("SIGN_NEGATIVE_ZERO");
            } else {
                return getMetaClass().getConstant("SIGN_POSITIVE_ZERO");
            }
        }
        
        if (value.signum() < 0) {
            return getMetaClass().getConstant("SIGN_NEGATIVE_FINITE");
        } else {
            return getMetaClass().getConstant("SIGN_POSITIVE_FINITE");
        }
    }

    @JRubyMethod(name = "split")
    public RubyArray split() {
        final Ruby runtime = getRuntime();
        final IRubyObject[] array = new IRubyObject[4];

        // sign
        final RubyFixnum sign;
        if (isNaN) {
            sign = RubyFixnum.zero(runtime);
        } else if (isInfinity()) {
            sign = runtime.newFixnum(infinitySign);
        } else if (isZero()){
            sign = runtime.newFixnum(zeroSign);
        } else {
            sign = runtime.newFixnum(value.signum());
        }
        array[0] = sign;

        // significant digits and exponent
        final RubyString digits;
        final RubyFixnum exp;
        if (isNaN()) {
            digits = runtime.newString("NaN");
            exp = RubyFixnum.zero(runtime);
        } else if (isInfinity()) {
            digits = runtime.newString("Infinity");
            exp = RubyFixnum.zero(runtime);
        } else if (isZero()){
            digits = runtime.newString("0");
            exp = RubyFixnum.zero(runtime);
        } else {
            // normalize the value
            digits = runtime.newString(getSignificantDigits());
            exp = runtime.newFixnum(getExponent());
        }
        array[1] = digits;
        array[3] = exp;

        // base
        array[2] = runtime.newFixnum(10);

        return RubyArray.newArrayNoCopy(runtime, array);
    }

    // it doesn't handle special cases
    private String getSignificantDigits() {
        // TODO: no need to calculate every time.
        BigDecimal val = value.abs().stripTrailingZeros();
        return val.unscaledValue().toString();
    }

    private String getAllDigits() {
        // TODO: no need to calculate every time.
        BigDecimal val = value.abs();
        return val.unscaledValue().toString();
    }    

    // it doesn't handle special cases
    private int getExponent() {
        if (isZero() || isNaN() || isInfinity()) return 0;

        BigDecimal val = value.abs().stripTrailingZeros();
        return val.precision() - val.scale();
    }

    @JRubyMethod(name = "sqrt", required = 1)
    public IRubyObject sqrt(IRubyObject arg) {
        Ruby runtime = getRuntime();
        if (isNaN()) {
            throw runtime.newFloatDomainError("(VpSqrt) SQRT(NaN value)");
        }
        if ((isInfinity() && infinitySign < 0) || value.signum() < 0) {
            throw runtime.newFloatDomainError("(VpSqrt) SQRT(negative value)");
        }
        if (isInfinity() && infinitySign > 0) {
            return newInfinity(runtime, 1);
        }

        // NOTE: MRI's sqrt precision is limited by 100,
        // but we allow values more than 100.
        int n = RubyNumeric.fix2int(arg);
        if (n < 0) {
            throw runtime.newArgumentError("argument must be positive");
        }

        n += 4; // just in case, add a bit of extra precision

        return new RubyBigDecimal(getRuntime(),
                bigSqrt(this.value, new MathContext(n, RoundingMode.HALF_UP))).setResult();
    }

    @JRubyMethod(name = "to_f")
    public IRubyObject to_f() {
        if (isNaN()) {
            return RubyFloat.newFloat(getRuntime(), Double.NaN);
        }
        if (isInfinity()) {
            return RubyFloat.newFloat(getRuntime(),
                    infinitySign < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
        }
        if (isZero()) {
            return RubyFloat.newFloat(getRuntime(),
                    zeroSign < 0 ? -0.0 : 0.0);
        }
        if (-value.scale() > RubyFloat.MAX_10_EXP) {
            switch (value.signum()) {
            case -1:
                return RubyFloat.newFloat(getRuntime(), Double.NEGATIVE_INFINITY);
            case 0:
                return RubyFloat.newFloat(getRuntime(), 0);
            case 1:
                return RubyFloat.newFloat(getRuntime(), Double.POSITIVE_INFINITY);
            default:
                // eh?!
            }
        }
        return RubyFloat.newFloat(getRuntime(), SafeDoubleParser.doubleValue(value));
    }

    @JRubyMethod(name = {"to_i", "to_int"}, compat = CompatVersion.RUBY1_8)
    public IRubyObject to_int() {
        if (isNaN() || infinitySign != 0) {
            return getRuntime().getNil();
        }
        try {
            return RubyNumeric.int2fix(getRuntime(), value.longValueExact());
        } catch (ArithmeticException ae) {
            return RubyBignum.bignorm(getRuntime(), value.toBigInteger());            
        }
    }

    @JRubyMethod(name = {"to_i", "to_int"}, compat = CompatVersion.RUBY1_9)
    public IRubyObject to_int19() {
        checkFloatDomain();
        return to_int();
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
        int groups = 0;
        Pattern p = Pattern.compile("(\\+| )?(\\d+)(E|F)?");
        Matcher m = p.matcher(format);
        if (m.matches()) {
            groups = Integer.parseInt(m.group(2));
        }
        return groups;
    }
    
    private boolean hasArg(IRubyObject[] args) {
        return args.length != 0 && !args[0].isNil();
    }

    private String format(IRubyObject[] args) {
        return args[0].toString();
    }

    private String firstArgument(IRubyObject[] args) {
        if (hasArg(args)) {
            return format(args);
        }
        return null;
    }

    private boolean posSpace(String arg) {
        if (null != arg) {
            return formatHasLeadingSpace(arg);
        }
        return false;
    }

    private boolean posSign(String arg) {
        if (null != arg) {
            return formatHasLeadingPlus(arg) || posSpace(arg);
        }
        return false;
    }

    private boolean asEngineering(String arg) {
        if (null != arg) {
            return !formatHasFloatingPointNotation(arg);
        }
        return true;
    }

    private int groups(String arg) {
        if (null != arg) {
            return formatFractionalDigitGroups(arg);
        }
        return 0;
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
        String whole = "0";
        if (values.length > 0) {
            whole = values[0];
        }
        String after = "0";
        if (values.length > 1) {
            after = values[1];
        }
        int signum = value.signum();
        StringBuilder build = new StringBuilder();
        build.append(signum == -1 ? "-" : (signum == 1 ? (posSign(arg) ? (posSpace(arg) ? " " : "+") : "") : ""));
        if (groups(arg) == 0) {
            build.append(whole);
            if (null != after) {
                build.append(".").append(after);
            }
        } else {
            int index = 0;
            String sep = "";
            while (index < whole.length()) {
                int next = index + groups(arg);
                if (next > whole.length()) {
                    next = whole.length();
                }
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
                    if (next > after.length()) {
                        next = after.length();
                    }
                    build.append(sep).append(after.substring(index, next));
                    sep = " ";
                    index += groups(arg);
                }
            }
        }
        return getRuntime().newString(build.toString());
    }
            
    @JRubyMethod(name = "to_s", optional = 1)
    public IRubyObject to_s(IRubyObject[] args) {
        String arg = firstArgument(args);
        if (isNaN()) {
            return getRuntime().newString("NaN");
        }
        if (infinitySign != 0) {
            if (infinitySign == -1) {
                return getRuntime().newString("-Infinity");
            } else {
                return getRuntime().newString("Infinity");
            }
        }
        if (isZero()) {
            String zero = "0.0";
            if (zeroSign < 0) {
                zero = "-" + zero;
            }
            return getRuntime().newString(zero);
        }
        if (asEngineering(arg)) {
            return engineeringValue(arg);
        } else {
            return floatingPointValue(arg);
        }
    }

    // Note: #fix has only no-arg form, but truncate allows optional parameter.

    @JRubyMethod
    public IRubyObject fix() {
        IRubyObject[] ary = { RubyFixnum.zero(getRuntime()) };
        return truncate(ary);
    }

    @JRubyMethod(name = "truncate", optional = 1, compat = CompatVersion.RUBY1_8)
    public IRubyObject truncate(IRubyObject[] args) {
        if (args.length == 0) {
            IRubyObject[] ary = { RubyFixnum.zero(getRuntime()) };
            return truncate(ary);
        }
        
        if (isNaN) {
            return newNaN(getRuntime());
        }
        if (isInfinity()) {
            return newInfinity(getRuntime(), infinitySign);
        }

        int n = RubyNumeric.fix2int(args[0]);
        
        int precision = value.precision() - value.scale() + n;
        
        if (precision > 0) {
            return new RubyBigDecimal(getRuntime(),
                    value.round(new MathContext(precision, RoundingMode.DOWN)));
        } else {
            // TODO: proper sign
            return new RubyBigDecimal(getRuntime(), BigDecimal.ZERO);
        }
    }

    @JRubyMethod(name = "truncate", optional = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject truncate19(IRubyObject[] args) {
        if (args.length == 0) {
            return ((RubyBigDecimal) truncate(args)).to_int19();
        } else {
            return truncate(args);
        }
            
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
      if (sign == -1) {
          throw new ArithmeticException("Square root of a negative number: " + squarD);
      } else if (sign == 0) {
          return squarD.round(rootMC);
      }

      int prec = rootMC.getPrecision();           // the requested precision
      if (prec == 0) {
          throw new IllegalArgumentException("Most roots won't have infinite precision = 0");
      }

      // Initial precision is that of double numbers 2^63/2 ~ 4E18
      int BITS = 62;                              // 63-1 an even number of number bits
      int nInit = 16;                             // precision seems 16 to 18 digits
      MathContext nMC = new MathContext(18, RoundingMode.HALF_DOWN);

      // Iteration variables, for the square root x and the reciprocal v
      BigDecimal x = null, e = null;              // initial x:  x0 ~ sqrt()
      BigDecimal v = null, g = null;              // initial v:  v0 = 1/(2*x)

      // Estimate the square root with the foremost 62 bits of squarD
      BigInteger bi = squarD.unscaledValue();     // bi and scale are a tandem
      int biLen = bi.bitLength();
      int shift = Math.max(0, biLen - BITS + (biLen%2 == 0 ? 0 : 1));   // even shift..
      bi = bi.shiftRight(shift);                  // ..floors to 62 or 63 bit BigInteger

      double root = Math.sqrt(SafeDoubleParser.doubleValue(bi));
      BigDecimal halfBack = new BigDecimal(BigInteger.ONE.shiftLeft(shift/2));

      int scale = squarD.scale();
      if (scale % 2 == 1) {
          root *= SQRT_10;                        // 5 -> 2, -5 -> -3 need half a scale more..
      }
      scale = (int) Math.ceil(scale/2.);         // ..where 100 -> 10 shifts the scale

      // Initial x - use double root - multiply by halfBack to unshift - set new scale
      x = new BigDecimal(root, nMC);
      x = x.multiply(halfBack, nMC);              // x0 ~ sqrt()
      if (scale != 0) {
          x = x.movePointLeft(scale);
      }

      if (prec < nInit) {                // for prec 15 root x0 must surely be OK
          return x.round(rootMC);        // return small prec roots without iterations
      }

      // Initial v - the reciprocal
      v = BigDecimal.ONE.divide(TWO.multiply(x), nMC);        // v0 = 1/(2*x)

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
          nMC = new MathContext(nPrecs.get(i), (i%2 == 1) ? RoundingMode.HALF_UP : 
                                                          RoundingMode.HALF_DOWN);

          // Next x                                        // e = d - x^2
          e = squarD.subtract(x.multiply(x, nMC), nMC);
          if (i != 0) {
              x = x.add(e.multiply(v, nMC));               // x += e*v     ~ sqrt()
          } else {
              x = x.add(e.multiply(v, rootMC), rootMC);    // root x is ready!
              break;
          }

          // Next v                                        // g = 1 - 2*x*v
          g = BigDecimal.ONE.subtract(TWO.multiply(x).multiply(v, nMC));

          v = v.add(g.multiply(v, nMC));                   // v += g*v     ~ 1/2/sqrt()
      }

      return x;                      // return sqrt(squarD) with precision of rootMC
    }

    private void checkFloatDomain() {
        if (isNaN) {
            throw this.getRuntime().newFloatDomainError("NaN");
        }
        if (infinitySign != 0) {
            if (infinitySign == -1) {
                throw getRuntime().newFloatDomainError("-Infinity");
            } else {
                throw getRuntime().newFloatDomainError("Infinity");
            }
        }
    }
    
    private boolean is_even(IRubyObject x) {
        if (x instanceof RubyFixnum) {
            return RubyNumeric.fix2long((RubyFixnum) x) % 2 == 0;
        }
        if (x instanceof RubyBignum) {
            return RubyBignum.big2long((RubyBignum) x) % 2 == 0;
        }
        return false;
    }
}// RubyBigdecimal
