/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
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
import org.jruby.ast.util.ArgsUtil;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.runtime.builtin.IRubyObject.NULL_ARRAY;
import org.jruby.util.Numeric;
import org.jruby.util.SafeDoubleParser;
import org.jruby.util.StringSupport;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubyBigDecimal extends RubyNumeric {

    private static final ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        public RubyBigDecimal allocate(Ruby runtime, RubyClass klass) {
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
    public final static int EXCEPTION_OVERFLOW = 1; // Note: This is same as EXCEPTION_INFINITY in MRI now
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
        RubyClass bigDecimal = runtime.defineClass("BigDecimal", runtime.getNumeric(), ALLOCATOR);

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
        this.value = BigDecimal.ZERO;
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
        String precisionAndValue = from.convertToString().asJavaString();
        String value = precisionAndValue.substring(precisionAndValue.indexOf(":")+1);
        return newInstance(context, recv, RubyString.newString(context.runtime, value));
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
        return modeExecute(context, (RubyModule) recv, block, "vpPrecLimit");
    }

    @JRubyMethod(meta = true)
    public static IRubyObject save_exception_mode(ThreadContext context, IRubyObject recv, Block block) {
        return modeExecute(context, (RubyModule) recv, block, "vpExceptionMode");
    }

    @JRubyMethod(meta = true)
    public static IRubyObject save_rounding_mode(ThreadContext context, IRubyObject recv, Block block) {
        return modeExecute(context, (RubyModule) recv, block, "vpRoundingMode");
    }

    private static IRubyObject modeExecute(final ThreadContext context, final RubyModule BigDecimal,
        final Block block, final String intVariableName) {
        IRubyObject current = BigDecimal.searchInternalModuleVariable(intVariableName);
        try {
            return block.yieldSpecific(context);
        }
        finally {
            BigDecimal.setInternalModuleVariable(intVariableName, current);
        }
    }

    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject mode(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        // FIXME: I doubt any of the constants referenced in this method
        // are ever redefined -- should compare to the known values, rather
        // than do an expensive constant lookup.
        RubyModule c = (RubyModule)recv;

        args = Arity.scanArgs(context.runtime, args, 1, 1);

        IRubyObject mode = args[0];
        IRubyObject value = args[1];

        if (!(mode instanceof RubyFixnum)) {
            throw context.runtime.newTypeError("wrong argument type " + mode.getMetaClass() + " (expected Fixnum)");
        }

        long longMode = ((RubyFixnum)mode).getLongValue();
        if ((longMode & EXCEPTION_ALL) != 0) {
            if (value.isNil()) return c.searchInternalModuleVariable("vpExceptionMode");
            if (!(value instanceof RubyBoolean)) throw context.runtime.newArgumentError("second argument must be true or false");

            long newExceptionMode = c.searchInternalModuleVariable("vpExceptionMode").convertToInteger().getLongValue();

            boolean enable = value.isTrue();

            if ((longMode & EXCEPTION_INFINITY) != 0) {
                newExceptionMode = enable ? newExceptionMode | EXCEPTION_INFINITY : newExceptionMode &  ~(EXCEPTION_INFINITY);
            }

            if ((longMode & EXCEPTION_NaN) != 0) {
                newExceptionMode = enable ? newExceptionMode | EXCEPTION_NaN : newExceptionMode &  ~(EXCEPTION_NaN);
            }

            if ((longMode & EXCEPTION_UNDERFLOW) != 0) {
                newExceptionMode = enable ? newExceptionMode | EXCEPTION_UNDERFLOW : newExceptionMode &  ~(EXCEPTION_UNDERFLOW);
            }

            if ((longMode & EXCEPTION_ZERODIVIDE) != 0) {
                newExceptionMode = enable ? newExceptionMode | EXCEPTION_ZERODIVIDE : newExceptionMode &  ~(EXCEPTION_ZERODIVIDE);
            }

            RubyFixnum fixnumMode = RubyFixnum.newFixnum(runtime, newExceptionMode);
            c.setInternalModuleVariable("vpExceptionMode", fixnumMode);
            return fixnumMode;
        }

        if (longMode == ROUND_MODE) {
            if (value.isNil()) {
                return c.searchInternalModuleVariable("vpRoundingMode");
            }

            RoundingMode javaRoundingMode = javaRoundingModeFromRubyRoundingMode(runtime, value);
            RubyFixnum roundingMode = runtime.newFixnum(javaRoundingMode.ordinal());
            c.setInternalModuleVariable("vpRoundingMode", roundingMode);

            return c.searchInternalModuleVariable("vpRoundingMode");
        }

        throw runtime.newTypeError("first argument for BigDecimal#mode invalid");
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
        return (bigDecimalVar(runtime, "vpExceptionMode") & EXCEPTION_NaN) != 0;
    }

    private static boolean isInfinityExceptionMode(Ruby runtime) {
        return (bigDecimalVar(runtime, "vpExceptionMode") & EXCEPTION_INFINITY) != 0;
    }

    private static boolean isOverflowExceptionMode(Ruby runtime) {
        return (bigDecimalVar(runtime, "vpExceptionMode") & EXCEPTION_OVERFLOW) != 0;
    }

    private static RubyBigDecimal cannotBeCoerced(ThreadContext context, IRubyObject value, boolean must) {
        if (must) {
            throw context.runtime.newTypeError(
                errMessageType(context, value) + " can't be coerced into BigDecimal"
            );
        }
        return null;
    }

    private static String errMessageType(ThreadContext context, IRubyObject value) {
        if (value == null || value.isNil()) return "nil";
        if (value.isImmediate()) return RubyObject.inspect(context, value).toString();
        return value.getMetaClass().getBaseName();
    }

    private static RubyBigDecimal getVpValue19(ThreadContext context, IRubyObject v, boolean must) {
        long precision = (v instanceof RubyFloat || v instanceof RubyRational) ? 0 : -1;

        return getVpValueWithPrec19(context, v, precision, must);
    }

    private static RubyBigDecimal getVpRubyObjectWithPrec19Inner(ThreadContext context, RubyRational value) {
        return getVpRubyObjectWithPrec19Inner(context, value, getRoundingMode(context.runtime));
    }

    public static RubyBigDecimal getVpRubyObjectWithPrec19Inner(ThreadContext context, RubyRational value, RoundingMode roundingMode) {
        BigDecimal numerator = BigDecimal.valueOf(RubyNumeric.num2long(value.numerator(context)));
        BigDecimal denominator = BigDecimal.valueOf(RubyNumeric.num2long(value.denominator(context)));

        int len = numerator.precision() + denominator.precision();
        int pow = len / 4;
        MathContext mathContext = new MathContext((pow + 1) * 4, roundingMode);

        return new RubyBigDecimal(context.runtime, numerator.divide(denominator, mathContext));
    }

    private static RubyBigDecimal getVpValueWithPrec19(ThreadContext context, IRubyObject value, long precision, boolean must) {
        if (value instanceof RubyFloat) {
            if (precision > Long.MAX_VALUE) return cannotBeCoerced(context, value, must);
            double doubleValue = ((RubyFloat) value).getDoubleValue();

            if (Double.isInfinite(doubleValue)) {
                throw context.runtime.newFloatDomainError(doubleValue < 0 ? "-Infinity" : "Infinity");
            }
            if (Double.isNaN(doubleValue)) {
                throw context.runtime.newFloatDomainError("NaN");
            }

            return new RubyBigDecimal(context.runtime, BigDecimal.valueOf(doubleValue));
        }
        else if (value instanceof RubyRational) {
            if (precision < 0) {
                if (must) {
                    throw context.runtime.newArgumentError(value.getMetaClass().getBaseName() + " can't be coerced into BigDecimal without a precision");
                }
                return null;
            }

            return getVpRubyObjectWithPrec19Inner(context, (RubyRational) value);
        }

        return getVpValue(context, value, must);
    }

    private static RubyBigDecimal getVpValue(ThreadContext context, IRubyObject value, boolean must) {
        if (value instanceof RubyBigDecimal) return (RubyBigDecimal) value;
        if (value instanceof RubyFixnum || value instanceof RubyBignum) {
            // Converted to a String because some values -inf cannot happen from Java libs
            return newInstance(context, context.runtime.getClass("BigDecimal"), value.asString());
        }
        if ((value instanceof RubyRational) || (value instanceof RubyFloat)) {
            return newInstance(context, context.runtime.getClass("BigDecimal"), value, RubyFixnum.newFixnum(context.runtime, RubyFloat.DIG));
        }
        return cannotBeCoerced(context, value, must);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject induced_from(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return getVpValue(context, arg, true);
    }

    private static RubyBigDecimal newInstance(Ruby runtime, IRubyObject recv, RubyBigDecimal arg) {
        return new RubyBigDecimal(runtime, (RubyClass) recv, arg);
    }

    private static RubyBigDecimal newInstance(Ruby runtime, IRubyObject recv, RubyFixnum arg, MathContext mathContext) {
        return new RubyBigDecimal(runtime, (RubyClass) recv, new BigDecimal(arg.getLongValue(), mathContext));
    }

    private static RubyBigDecimal newInstance(ThreadContext context, RubyRational arg, MathContext mathContext) {
        BigDecimal num = new BigDecimal(arg.numerator(context).convertToInteger().getLongValue());
        BigDecimal den = new BigDecimal(arg.denominator(context).convertToInteger().getLongValue());
        BigDecimal value;
        try {
          value = num.divide(den, mathContext);
        } catch (ArithmeticException e){
          value = num.divide(den, MathContext.DECIMAL64);
        };

        return new RubyBigDecimal(context.runtime, value);
    }

    private static RubyBigDecimal newInstance(Ruby runtime, IRubyObject recv, RubyFloat arg, MathContext mathContext) {
        // precision can be no more than float digits
        if (mathContext.getPrecision() > RubyFloat.DIG + 1) throw runtime.newArgumentError("precision too large");

        double dblVal = arg.getDoubleValue();

        if(Double.isNaN(dblVal)) throw runtime.newFloatDomainError("NaN");
        if(Double.isInfinite(dblVal)) return newInfinity(runtime, dblVal == Double.POSITIVE_INFINITY ? 1 : -1);

        return new RubyBigDecimal(runtime, (RubyClass) recv, new BigDecimal(dblVal, mathContext));
    }

    private static RubyBigDecimal newInstance(Ruby runtime, IRubyObject recv, RubyBignum arg, MathContext mathContext) {
        return new RubyBigDecimal(runtime, (RubyClass) recv, new BigDecimal(arg.getBigIntegerValue(), mathContext));
    }

    private final static Pattern NUMBER_PATTERN = Pattern.compile("^([+-]?\\d*\\.?\\d*([eE]?)([+-]?\\d*)).*");

    private static RubyBigDecimal newInstance(ThreadContext context, IRubyObject recv, IRubyObject arg, MathContext mathContext) {
        String strValue = arg.convertToString().toString().trim();

        int sign = 1;
        switch ( strValue.length() > 0 ? strValue.charAt(0) : ' ' ) {
            case '_' : return newZero(context.runtime, 1); // leading "_" are not allowed
            case 'N' :
                if ( "NaN".equals(strValue) ) return newNaN(context.runtime);
                break;
            case 'I' :
                if ( "Infinity".equals(strValue) ) return newInfinity(context.runtime, 1);
                break;
            case '-' :
                if ( "-Infinity".equals(strValue) ) return newInfinity(context.runtime, -1);
                sign = -1;
                break;
            case '+' :
                if ( "+Infinity".equals(strValue) ) return newInfinity(context.runtime, +1);
                break;
        }

        // Convert String to Java understandable format (for BigDecimal).
        strValue = strValue.replaceFirst("[dD]", "E");                  // 1. MRI allows d and D as exponent separators
        strValue = strValue.replaceAll("_", "");                        // 2. MRI allows underscores anywhere

        Matcher matcher = NUMBER_PATTERN.matcher(strValue);
        strValue = matcher.replaceFirst("$1");                          // 3. MRI ignores the trailing junk

        String exp = matcher.group(2); int idx;
        if ( exp != null && ! exp.isEmpty() ) {
            String expValue = matcher.group(3);
            if (expValue.isEmpty() || expValue.equals("-") || expValue.equals("+")) {
                strValue = strValue.concat("0");                        // 4. MRI allows 1E, 1E-, 1E+
            }
            else if (isExponentOutOfRange(expValue)) {
                // Handle infinity (Integer.MIN_VALUE + 1) < expValue < Integer.MAX_VALUE
                return newInfinity(context.runtime, sign);
            }
        }
        else if ( ( idx = matcher.start(3) ) > 0 ) {
            strValue = strValue.substring(0, idx); // ignored tail junk e.g. "5-6" -> "-6"
        }

        BigDecimal decimal;
        try {
            decimal = new BigDecimal(strValue, mathContext);
        }
        catch (NumberFormatException e) {
            if (isOverflowExceptionMode(context.runtime)) throw context.runtime.newFloatDomainError("exponent overflow");

            decimal = new BigDecimal(0);
        }

        // MRI behavior: -0 and +0 are two different things
        if (decimal.signum() == 0) return newZero(context.runtime, sign);

        return new RubyBigDecimal(context.runtime, (RubyClass) recv, decimal);
    }

    private static boolean isExponentOutOfRange(final String expValue) {
        int num = 0;
        int sign = 1;
        final int len = expValue.length();
        final char ch = expValue.charAt(0);
        if (ch == '-') {
          sign = -1;
        } else if (ch != '+') {
            num = '0' - ch;
        }
        int i = 1;
        final int max = (sign == 1) ? -Integer.MAX_VALUE : Integer.MIN_VALUE + 1;
        final int multmax = max / 10;
        while (i < len) {
            int d = expValue.charAt(i++) - '0';
            if (num < multmax) {
                return true;
            }
            num *= 10;
            if (num < (max + d)) {
                return true;
            }
            num -= d;
        }
        return false;
    }

    @Deprecated
    public static RubyBigDecimal newInstance(IRubyObject recv, IRubyObject[] args) {
        final ThreadContext context = recv.getRuntime().getCurrentContext();
        switch (args.length) {
            case 1: return newInstance(context, recv, args[0]);
            case 2: return newInstance(context, recv, args[0], args[1]);
        }
        throw new IllegalArgumentException("unexpected argument count: " + args.length);
    }

    @JRubyMethod(name = "new", meta = true)
    public static RubyBigDecimal newInstance(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        if (arg instanceof RubyBigDecimal) return newInstance(context.runtime, recv, (RubyBigDecimal) arg);
        if (arg instanceof RubyRational) throw context.runtime.newArgumentError("can't omit precision for a Rational.");
        if (arg instanceof RubyFloat) throw context.runtime.newArgumentError("can't omit precision for a Float.");
        if (arg instanceof RubyFixnum) return newInstance(context.runtime, recv, (RubyFixnum) arg, MathContext.UNLIMITED);
        if (arg instanceof RubyBignum) return newInstance(context.runtime, recv, (RubyBignum) arg, MathContext.UNLIMITED);
        return newInstance(context, recv, arg, MathContext.UNLIMITED);
    }

    @JRubyMethod(name = "new", meta = true)
    public static RubyBigDecimal newInstance(ThreadContext context, IRubyObject recv, IRubyObject arg, IRubyObject mathArg) {
        int digits = (int) mathArg.convertToInteger().getLongValue();
        if (digits < 0) throw context.runtime.newArgumentError("argument must be positive");

        MathContext mathContext = new MathContext(digits);

        if (arg instanceof RubyBigDecimal) return newInstance(context.runtime, recv, (RubyBigDecimal) arg);
        if (arg instanceof RubyFloat) return newInstance(context.runtime, recv, (RubyFloat) arg, mathContext);
        if (arg instanceof RubyRational) return newInstance(context, (RubyRational) arg, mathContext);
        if (arg instanceof RubyFixnum) return newInstance(context.runtime, recv, (RubyFixnum) arg, mathContext);
        if (arg instanceof RubyBignum) return newInstance(context.runtime, recv, (RubyBignum) arg, mathContext);

        return newInstance(context, recv, arg, MathContext.UNLIMITED);
    }

    private static RubyBigDecimal newZero(final Ruby runtime, final int sign) {
        return new RubyBigDecimal(runtime, BigDecimal.ZERO, 0, sign < 0 ? -1 : 1);
    }

    private static RubyBigDecimal newNaN(Ruby runtime) {
        if ( isNaNExceptionMode(runtime) ) {
            throw runtime.newFloatDomainError("Computation results to 'NaN'(Not a Number)");
        }
        return new RubyBigDecimal(runtime, BigDecimal.ZERO, true);
    }

    private static RubyBigDecimal newInfinity(final Ruby runtime, final int sign) {
        if ( isInfinityExceptionMode(runtime) ) {
            throw runtime.newFloatDomainError("Computation results to 'Infinity'");
        }
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

    @JRubyMethod(name = {"%", "modulo"}, required = 1)
    public IRubyObject op_mod(ThreadContext context, IRubyObject other) {
        // TODO: full-precision divmod is 1000x slower than MRI!
        RubyBigDecimal val = getVpValue19(context, other, false);

        if (val == null) return callCoerced(context, sites(context).op_mod, other, true);
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

    @Deprecated
    public IRubyObject op_mod19(ThreadContext context, IRubyObject arg) {
        return op_mod(context, arg);
    }

    @Override
    @JRubyMethod(name = "remainder", required = 1)
    public IRubyObject remainder(ThreadContext context, IRubyObject arg) {
        return remainderInternal(context, getVpValue19(context, arg, false), arg);
    }

    @Deprecated
    public IRubyObject remainder19(ThreadContext context, IRubyObject arg) {
        return remainder(context, arg);
    }

    private IRubyObject remainderInternal(ThreadContext context, RubyBigDecimal val, IRubyObject arg) {
        // TODO: full-precision remainder is 1000x slower than MRI!
        if (isInfinity() || isNaN()) return newNaN(context.runtime);
        if (val == null) return callCoerced(context, sites(context).remainder, arg, true);
        if (val.isInfinity() || val.isNaN() || val.isZero()) return newNaN(context.runtime);

        // Java and MRI definitions of remainder are the same.
        return new RubyBigDecimal(context.runtime, value.remainder(val.value)).setResult();
    }

    @JRubyMethod(name = "*", required = 1)
    public IRubyObject op_mul(ThreadContext context, IRubyObject arg) {
        return mult2(context, arg, vpPrecLimit(context.runtime));
    }

    @Deprecated
    public IRubyObject op_mul19(ThreadContext context, IRubyObject arg) {
        return op_mul(context, arg);
    }

    @JRubyMethod(name = "mult", required = 2)
    public IRubyObject mult2(ThreadContext context, IRubyObject b, IRubyObject n) {
        RubyBigDecimal val = getVpValue19(context, b, false);
        if (val == null) { // TODO: what about n arg?
            return callCoerced(context, sites(context).op_times, b, true);
        }
        return multInternal(context.runtime, val, n);
    }

    private RubyBigDecimal multInternal(final Ruby runtime, RubyBigDecimal val, IRubyObject n) {
        int digits = RubyNumeric.fix2int(n);

        if (isNaN() || val.isNaN()) return newNaN(runtime);
        if ((isInfinity() && val.isZero()) || (isZero() && val.isInfinity())) return newNaN(runtime);

        if ( isZero() || val.isZero() ) {
            int sign1 = isZero()? zeroSign : value.signum();
            int sign2 = val.isZero() ?  val.zeroSign : val.value.signum();
            return newZero(runtime, sign1 * sign2);
        }

        if ( isInfinity() || val.isInfinity() ) {
            int sign1 = isInfinity() ? infinitySign : value.signum();
            int sign2 = val.isInfinity() ? val.infinitySign : val.value.signum();
            return newInfinity(runtime, sign1 * sign2);
        }

        BigDecimal res = value.multiply(val.value);
        // FIXME: rounding mode should not be hard-coded. See #mode.
        if (res.precision() > digits) res = res.round(new MathContext(digits,  RoundingMode.HALF_UP));

        return new RubyBigDecimal(runtime, res).setResult();
    }

    @Deprecated
    public IRubyObject mult219(ThreadContext context, IRubyObject b, IRubyObject n) {
        return mult2(context, b, n);
    }

    // Calculate appropriate zero or infinity depending on exponent...
    private RubyBigDecimal newPowOfInfinity(ThreadContext context, IRubyObject exp) {
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

    private static IRubyObject vpPrecLimit(final Ruby runtime) {
        return runtime.getClass("BigDecimal").searchInternalModuleVariable("vpPrecLimit");
    }

    // @Deprecated
    public IRubyObject op_pow(IRubyObject arg) {
        return op_pow(getRuntime().getCurrentContext(), arg);
    }

    @JRubyMethod(name = {"**", "power"}, required = 1)
    public RubyBigDecimal op_pow(final ThreadContext context, IRubyObject exp) {
        final Ruby runtime = context.runtime;

        if ( ! (exp instanceof RubyNumeric) ) {
            throw context.runtime.newTypeError("wrong argument type " + exp.getMetaClass() + " (expected scalar Numeric)");
        }

        if (isNaN()) return newNaN(runtime);

        if (isInfinity()) return newPowOfInfinity(context, exp);

        final int times; final double rem; // exp's decimal part
        // when pow is not an integer we're play the oldest trick :
        // X pow (T+R) = X pow T * X pow R
        if ( ! ( exp instanceof RubyInteger ) ) {
            BigDecimal expVal = BigDecimal.valueOf( ((RubyNumeric) exp).getDoubleValue() );
            BigDecimal[] divAndRem = expVal.divideAndRemainder(BigDecimal.ONE);
            times = divAndRem[0].intValueExact(); rem = divAndRem[1].doubleValue();
        }
        else {
            times = RubyNumeric.fix2int(exp); rem = 0;
        }

        BigDecimal pow;
        if ( times < 0 ) {
            if (isZero()) return newInfinity(context.runtime, value.signum());
            pow = powNegative(times);
        }
        else {
            pow = value.pow(times);
        }

        if ( rem > 0 ) {
            // TODO of course this assumes we fit into double (and we loose some precision)
            double remPow = Math.pow(value.doubleValue(), rem);
            pow = pow.multiply( BigDecimal.valueOf(remPow) );
        }

        return new RubyBigDecimal(runtime, pow);
    }

    @Deprecated
    public IRubyObject op_pow19(IRubyObject exp) {
        return op_pow(getRuntime().getCurrentContext(), exp);
    }

    @Deprecated
    public RubyBigDecimal op_pow19(ThreadContext context, IRubyObject exp) {
        return op_pow(context, exp);
    }

    private BigDecimal powNegative(final int times) {
        // Note: MRI has a very non-trivial way of calculating the precision,
        // so we use very simple approximation here:
        int precision = (-times + 4) * (getAllDigits().length() + 4);
        return value.pow(times, new MathContext(precision, RoundingMode.HALF_UP));
    }

    @JRubyMethod(name = "+")
    public IRubyObject op_plus(ThreadContext context, IRubyObject b) {
        return addInternal(context, getVpValue19(context, b, false), b, vpPrecLimit(context.runtime));
    }

    @Deprecated
    public IRubyObject op_plus19(ThreadContext context, IRubyObject b) {
        return op_plus(context, b);
    }

    @JRubyMethod(name = "add")
    public IRubyObject add2(ThreadContext context, IRubyObject b, IRubyObject digits) {
        return addInternal(context, getVpValue19(context, b, false), b, digits);
    }

    @Deprecated
    public IRubyObject add219(ThreadContext context, IRubyObject b, IRubyObject digits) {
        return add2(context, b, digits);
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
            return callCoerced(context, sites(context).op_plus, b, true);
        }

        RubyBigDecimal res = handleAddSpecialValues(context, val);
        if ( res != null ) return res;

        RoundingMode roundMode = getRoundingMode(runtime);
        return new RubyBigDecimal(runtime, value.add(
                val.value, new MathContext(prec, roundMode))); // TODO: why this: .setResult();
    }

    private static int getPositiveInt(ThreadContext context, IRubyObject arg) {
        final Ruby runtime = context.runtime;

        if ( arg instanceof RubyFixnum ) {
            int value = RubyNumeric.fix2int(arg);
            if (value < 0) {
                throw runtime.newArgumentError("argument must be positive");
            }
            return value;
        }
        throw runtime.newTypeError(arg, runtime.getFixnum());
    }

    private RubyBigDecimal handleAddSpecialValues(ThreadContext context, RubyBigDecimal val) {
        if (isNaN() || val.isNaN) {
            return newNaN(context.runtime);
        }

        int sign = infinitySign * val.infinitySign;
        if (sign > 0) {
            return isInfinity() ? this : val;
        }
        if (sign < 0) {
            return newNaN(context.runtime);
        }
        if (sign == 0) {
            sign = infinitySign + val.infinitySign;
            if (sign != 0) {
                return newInfinity(context.runtime, sign);
            }
        }
        return null;
    }

    @Override
    @JRubyMethod(name = "+@")
    public IRubyObject op_uplus() {
        return this;
    }

    @Override
    @JRubyMethod(name = "-@")
    public IRubyObject op_uminus(ThreadContext context) {
        if (isNaN()) return newNaN(context.runtime);
        if (isInfinity()) return newInfinity(context.runtime, -infinitySign);
        if (isZero()) return newZero(context.runtime, -zeroSign);

        return new RubyBigDecimal(context.runtime, value.negate());
    }

    @JRubyMethod(name = "-", required = 1)
    public IRubyObject op_minus(ThreadContext context, IRubyObject b) {
        return subInternal(context, getVpValue19(context, b, true), b);
    }

    @Deprecated
    public IRubyObject op_minus19(ThreadContext context, IRubyObject b) {
        return op_minus(context, b);
    }

    @JRubyMethod(name = "sub", required = 2)
    public IRubyObject sub2(ThreadContext context, IRubyObject b, IRubyObject n) {
        // FIXME: Missing handling of n
        return subInternal(context, getVpValue19(context, b, false), b);
    }

    @Deprecated
    public IRubyObject sub219(ThreadContext context, IRubyObject b, IRubyObject n) {
        return sub2(context, b, n);
    }

    private IRubyObject subInternal(ThreadContext context, RubyBigDecimal val, IRubyObject b) {
        if (val == null) return callCoerced(context, sites(context).op_minus, b);

        RubyBigDecimal res = handleMinusSpecialValues(context, val);

        return res != null ? res : new RubyBigDecimal(context.runtime, value.subtract(val.value)).setResult();
    }

    private RubyBigDecimal handleMinusSpecialValues(ThreadContext context, RubyBigDecimal val) {
        if (isNaN() || val.isNaN()) {
            return newNaN(context.runtime);
        }

        int sign = infinitySign * val.infinitySign;
        if (sign > 0) {
            return newNaN(context.runtime);
        }
        if (sign < 0) {
            return this;
        }
        if (sign == 0) {
            if (isInfinity()) {
                return this;
            }
            if (val.isInfinity()) {
                return newInfinity(context.runtime, val.infinitySign * -1);
            }
            sign = infinitySign + val.infinitySign;
            if (sign != 0) {
                return newInfinity(context.runtime, sign);
            }
        }
        return null;
    }

    @JRubyMethod(name = {"/", "quo"})
    public IRubyObject op_quo(ThreadContext context, IRubyObject other) {
        RubyBigDecimal val = getVpValue19(context, other, false);
        if (val == null) return callCoerced(context, sites(context).op_quo, other, true);

        if (isNaN() || val.isNaN()) return newNaN(context.runtime);

        // regular division with some default precision
        // proper algorithm to set the precision
        // the precision is multiple of 4
        // and the precision is larger than len * 2
        int len = value.precision() + val.value.precision();
        int pow = len / 4;
        int precision = (pow + 1) * 4 * 2;

        return divWithScale(context, val, precision);
    }

    @Deprecated
    public IRubyObject op_quo19(ThreadContext context, IRubyObject other) {
        return op_quo(context, other);
    }

    @Deprecated
    public IRubyObject op_quo20(ThreadContext context, IRubyObject other) {
        return op_quo(context, other);
    }

    @JRubyMethod(name = "div")
    public IRubyObject op_div(ThreadContext context, IRubyObject r) {
        RubyBigDecimal val = getVpValue19(context, r, false);
        if (val == null) return callCoerced(context, sites(context).div, r, true);

        if (isNaN() || val.isNaN()) throw context.runtime.newFloatDomainError("Computation results to 'NaN'");
        if (isInfinity()) { // NOTE: MRI is inconsistent with div(r, d) impl
            if (val.isInfinity()) {
                throw context.runtime.newFloatDomainError("Computation results to 'NaN'(Not a Number)");
            }
            throw context.runtime.newFloatDomainError("Computation results to 'Infinity'");
        }
        if (val.isInfinity()) return newZero(context.runtime, value.signum() * val.infinitySign);

        if (val.isZero()) throw context.runtime.newZeroDivisionError();

        return new RubyBigDecimal(context.runtime, this.value.divideToIntegralValue(val.value)).setResult();
    }

    @Deprecated
    public final IRubyObject op_div19(ThreadContext context, IRubyObject r) {
        return op_div(context, r);
    }

    @JRubyMethod(name = "div")
    public IRubyObject op_div(ThreadContext context, IRubyObject other, IRubyObject digits) {
        RubyBigDecimal val = getVpValue(context, other, false);
        if (val == null) return callCoerced(context, sites(context).div, other, true);

        if (isNaN() || val.isNaN()) {
            throw context.runtime.newFloatDomainError("Computation results to 'NaN'");
        }
        return divWithScale(context, val, RubyNumeric.fix2int(digits));
    }

    private IRubyObject divWithScale(ThreadContext context, RubyBigDecimal val, int scale) {
        if (isInfinity()) {
            if (val.isInfinity()) return newNaN(context.runtime);
            return newInfinity(context.runtime, infinitySign * val.value.signum());
        }
        if (val.isInfinity()) return newZero(context.runtime, value.signum() * val.infinitySign);

        if (val.isZero()) {
            if (isZero()) return newNaN(context.runtime);
            int sign1 = isInfinity() ? infinitySign : value.signum();
            return newInfinity(context.runtime, sign1 * val.zeroSign);
        }
        if (isZero()) return newZero(context.runtime, zeroSign * val.value.signum());

        // MRI behavior: "If digits is 0, the result is the same as the / operator."
        if (scale == 0) return op_quo(context, val);

        MathContext mathContext = new MathContext(scale, getRoundingMode(context.runtime));
        return new RubyBigDecimal(context.runtime, value.divide(val.value, mathContext)).setResult(scale);
    }

    @Deprecated
    public final IRubyObject op_div19(ThreadContext context, IRubyObject other, IRubyObject digits) {
        return op_div(context, other, digits);
    }

    private IRubyObject cmp(ThreadContext context, final IRubyObject arg, final char op) {
        final int e;
        RubyBigDecimal rb = getVpValue(context, arg, false);
        if (rb == null) {
            IRubyObject cmp = callCoerced(context, sites(context).op_cmp, arg, false);
            if ( cmp.isNil() ) { // arg.coerce failed
                if (op == '*') return context.nil;
                if (op == '=' || isNaN()) return context.runtime.getFalse();
                throw context.runtime.newArgumentError("comparison of BigDecimal with "+ errMessageType(context, arg) +" failed");
            }
            e = RubyNumeric.fix2int(cmp);
        } else {
            if (isNaN() || rb.isNaN()) return (op == '*') ? context.nil : context.runtime.getFalse();

            e = infinitySign != 0 || rb.infinitySign != 0 ? infinitySign - rb.infinitySign : value.compareTo(rb.value);
        }
        switch(op) {
            case '*': return context.runtime.newFixnum(e);
            case '=': return context.runtime.newBoolean(e == 0);
            case '!': return context.runtime.newBoolean(e != 0);
            case 'G': return context.runtime.newBoolean(e >= 0);
            case '>': return context.runtime.newBoolean(e >  0);
            case 'L': return context.runtime.newBoolean(e <= 0);
            case '<': return context.runtime.newBoolean(e <  0);
        }
        return context.nil;
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

        if (value.scale() <= n) return this; // no rounding necessary

        return new RubyBigDecimal(context.runtime, value.setScale(n, RoundingMode.CEILING));
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

    // FIXME: Do we really need this Java inheritance for coerce?
    @Override
    public IRubyObject coerce(IRubyObject other) {
        return coerce(getRuntime().getCurrentContext(), other);
    }

    @JRubyMethod
    public RubyArray coerce(ThreadContext context, IRubyObject other) {
        return context.runtime.newArray(getVpValue(context, other, true), this);
    }

    @Override
    public double getDoubleValue() { return SafeDoubleParser.doubleValue(value); }

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
    @JRubyMethod(name = "divmod")
    public IRubyObject divmod(ThreadContext context, IRubyObject other) {
        // TODO: full-precision divmod is 1000x slower than MRI!
        Ruby runtime = context.runtime;
        RubyBigDecimal val = getVpValue19(context, other, false);

        if (val == null) return callCoerced(context, sites(context).divmod, other, true);
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
        return toStringImpl(context.runtime, null);
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
        return RubyArray.newArray(context.runtime,
                context.runtime.newFixnum(getSignificantDigits().length()),
                context.runtime.newFixnum(((getAllDigits().length() / 4) + 1) * 4));
    }

    @JRubyMethod(name = "round", optional = 2)
    public IRubyObject round(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        // Special treatment for BigDecimal::NAN and BigDecimal::INFINITY
        //
        // If round is called without any argument, we should raise a
        // FloatDomainError. Otherwise, we don't have to call round ;
        // we can simply return the number itself.
        if (args.length == 0 && isInfinity()) {
            StringBuilder message = new StringBuilder("Computation results to ");
            message.append('\'').append(callMethod(context, "to_s")).append('\'');

            throw runtime.newFloatDomainError(message.toString());
        } else {
            if (isNaN()) return newNaN(runtime);
            if (isInfinity()) {
                return newInfinity(runtime, infinitySign);
            }
        }

        RoundingMode mode = getRoundingMode(runtime);
        int scale = 0;

        int argc = args.length;
        switch (argc) {
            case 2:
                mode = javaRoundingModeFromRubyRoundingMode(runtime, args[1]);
                scale = num2int(args[0]);
            case 1:
                if (ArgsUtil.getOptionsArg(runtime, args[0]).isNil()) {
                    scale = num2int(args[0]);
                } else {
                    mode = javaRoundingModeFromRubyRoundingMode(runtime, args[0]);
                }
        }

        // JRUBY-914: Java 1.4 BigDecimal does not allow a negative scale, so we have to simulate it
        final RubyBigDecimal bigDecimal;
        if (scale < 0) {
          // shift the decimal point just to the right of the digit to be rounded to (divide by 10**(abs(scale)))
          // -1 -> 10's digit, -2 -> 100's digit, etc.
          BigDecimal normalized = value.movePointRight(scale);
          // ...round to that digit
          BigDecimal rounded = normalized.setScale(0, mode);
          // ...and shift the result back to the left (multiply by 10**(abs(scale)))
          bigDecimal = new RubyBigDecimal(runtime, rounded.movePointLeft(scale));
        } else {
          bigDecimal = new RubyBigDecimal(runtime, value.setScale(scale, mode));
        }

        return args.length == 0 ? bigDecimal.to_int() : bigDecimal;
    }

    public IRubyObject round(ThreadContext context, IRubyObject scale, IRubyObject mode) {
        return round(context, new IRubyObject[]{scale, mode});
    }

    //this relies on the Ruby rounding enumerations == Java ones, which they (currently) all are
    private static RoundingMode javaRoundingModeFromRubyRoundingMode(Ruby runtime, IRubyObject arg) {
        IRubyObject opts = ArgsUtil.getOptionsArg(runtime, arg);
        if (!opts.isNil()) {
            arg = ArgsUtil.extractKeywordArg(runtime.getCurrentContext(), "half", opts);
            if (arg.isNil()) {
                return getRoundingMode(runtime);
            }
            String roundingMode = arg.asJavaString();
            switch (roundingMode) {
                case "up":
                    return RoundingMode.HALF_UP;
                case "down" :
                    return RoundingMode.HALF_DOWN;
                case "even" :
                    return RoundingMode.HALF_EVEN;
                default :
                    throw runtime.newArgumentError("invalid rounding mode: " + roundingMode);
            }
        }
        if (arg.isNil()) {
            return getRoundingMode(runtime);
        }
        if (arg instanceof RubySymbol) {
            String roundingMode = arg.asJavaString();
            switch (roundingMode) {
                case "up" :
                    return RoundingMode.UP;
                case "down" :
                case "truncate" :
                    return RoundingMode.DOWN;
                case "half_up" :
                case "default" :
                    return RoundingMode.HALF_UP;
                case "half_down" :
                    return RoundingMode.HALF_DOWN;
                case "half_even" :
                case "even" :
                case "banker" :
                    return RoundingMode.HALF_EVEN;
                case "ceiling" :
                case "ceil" :
                    return RoundingMode.CEILING;
                case "floor" :
                    return RoundingMode.FLOOR;
                default :
                    throw runtime.newArgumentError("invalid rounding mode: " + roundingMode);
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
        return RubyArray.newArray(context.runtime,
                signValue(context.runtime),
                context.runtime.newString(splitDigits()),
                context.runtime.newFixnum(10),
                exponent());
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

    public final IRubyObject to_int() {
        return to_int(getRuntime());
    }

    @Override
    @JRubyMethod(name = {"to_i", "to_int"})
    public IRubyObject to_int(ThreadContext context) {
        return to_int(context.runtime);
    }

    final RubyInteger to_int(Ruby runtime) {
        checkFloatDomain();
        try {
            return RubyNumeric.int2fix(runtime, value.longValueExact());
        } catch (ArithmeticException ex) {
            return RubyBignum.bignorm(runtime, value.toBigInteger());
        }
    }

    @JRubyMethod(name = "to_r")
    public IRubyObject to_r(ThreadContext context) {
        checkFloatDomain();

        int scale = value.scale();
        BigInteger numerator = value.scaleByPowerOfTen(scale).toBigInteger();
        BigInteger denominator = BigInteger.TEN.pow(scale);

        return RubyRational.newInstance(context, context.runtime.getRational(), RubyBignum.newBignum(context.runtime, numerator), RubyBignum.newBignum(context.runtime, denominator));
    }

    @Deprecated // not-used
    public IRubyObject to_int19() {
        return to_int();
    }

    private static String removeTrailingZeroes(final String str) {
        int l = str.length();
        while (l > 0 && str.charAt(l-1) == '0') l--;
        return str.substring(0, l);
    }

    public static boolean formatHasLeadingPlus(String format) {
        return format.length() > 0 && format.charAt(0) == '+';
    }

    public static boolean formatHasLeadingSpace(String format) {
        return format.length() > 0 && format.charAt(0) == ' ';
    }

    public static boolean formatHasFloatingPointNotation(String format) {
        return format.length() > 0 && format.charAt(format.length()-1) == 'F';
    }

    private static final Pattern FRACTIONAL_DIGIT_GROUPS = Pattern.compile("(\\+| )?(\\d+)(E|F)?");

    public static int formatFractionalDigitGroups(String format) {
        Matcher match = FRACTIONAL_DIGIT_GROUPS.matcher(format);
        return match.matches() ? Integer.parseInt(match.group(2)) : 0;
    }

    private static boolean posSpace(String arg) {
        return arg == null ? false : formatHasLeadingSpace(arg);
    }

    private static boolean posSign(String arg) {
        return arg == null ? false : (formatHasLeadingPlus(arg) || posSpace(arg));
    }

    private static int groups(String arg) {
        return arg == null ? 0 : formatFractionalDigitGroups(arg);
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

    private static String sign(String arg, int signum) {
        return signum == -1 ? "-" : (signum == 1 ? (posSign(arg) ? (posSpace(arg) ? " " : "+") : "") : "");
    }

    private CharSequence engineeringValue(final String arg) {
        final String s = removeTrailingZeroes(unscaledValue());

        StringBuilder build = new StringBuilder();
        build.append( sign(arg, value.signum()) ).append("0.");

        final int groups = groups(arg);
        if (groups == 0) {
            build.append(s.isEmpty() ? "0" : s);
        } else {
            final int len = s.length();
            String sep = "";
            for (int index = 0; index < len; index += groups) {
                int next = index + groups;
                build.append(sep).append(s.substring(index, next > len ? len : next));
                sep = " ";
            }
        }
        build.append('e').append(getExponent());
        return build;
    }

    private CharSequence floatingPointValue(final String arg) {
        List<String> values = StringSupport.split(value.abs().stripTrailingZeros().toPlainString(), '.');
        final String whole = values.size() > 0 ? values.get(0) : "0";
        final String after = values.size() > 1 ? values.get(1) : "0";

        StringBuilder build = new StringBuilder();
        build.append( sign(arg, value.signum()) );

        final int groups = groups(arg);
        if (groups == 0) {
            build.append(whole);
            if (after != null) build.append('.').append(after);
        } else {
            int index = 0, len = whole.length();
            String sep = "";
            while (index < len) {
                int next = index + groups;
                if (next > len) next = len;

                build.append(sep).append(whole.substring(index, next));
                sep = " ";
                index += groups;
            }
            if (after != null) {
                build.append('.');
                index = 0; len = after.length();
                sep = "";
                while (index < len) {
                    int next = index + groups;
                    if (next > len) next = len;

                    build.append(sep).append(after.substring(index, next));
                    sep = " ";
                    index += groups;
                }
            }
        }
        return build;
    }

    @JRubyMethod
    public RubyString to_s(ThreadContext context) {
        return toStringImpl(context.runtime, null);
    }

    @JRubyMethod
    public RubyString to_s(ThreadContext context, IRubyObject arg) {
        return toStringImpl(context.runtime, arg.isNil() ? null : arg.toString());
    }

    private RubyString toStringImpl(final Ruby runtime, String arg) {
        if ( isNaN() ) return runtime.newString("NaN");
        if ( isInfinity() ) return runtime.newString(infinityString());
        if ( isZero() ) return runtime.newString(zeroSign < 0 ? "-0.0" : "0.0");

        boolean asEngineering = arg == null || ! formatHasFloatingPointNotation(arg);

        return RubyString.newString(runtime,
            ( asEngineering ? engineeringValue(arg) : floatingPointValue(arg) )
        );
    }

    @Deprecated
    public IRubyObject to_s(IRubyObject[] args) {
        return toStringImpl(getRuntime(), args.length == 0 ? null : (args[0].isNil() ? null : args[0].toString()));
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

        if (precision > 0) {
            return new RubyBigDecimal(getRuntime(),
                value.round(new MathContext(precision, RoundingMode.DOWN)));
        }

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
     * @link http://oldblog.novaloka.nl/blogger.xs4all.nl/novaloka/archive/2007/09/15/295396.html
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

    private static JavaSites.BigDecimalSites sites(ThreadContext context) {
        return context.sites.BigDecimal;
    }
}
