/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jruby.*;
import org.jruby.anno.JRubyConstant;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.common.IRubyWarnings;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Numeric;
import org.jruby.util.SafeDoubleParser;
import org.jruby.util.StringSupport;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubyBigDecimal extends RubyNumeric {

    @JRubyConstant
    public final static int ROUND_DOWN = RoundingMode.DOWN.ordinal();
    @JRubyConstant
    public final static int ROUND_CEILING = RoundingMode.CEILING.ordinal();
    @JRubyConstant
    public final static int ROUND_UP = RoundingMode.UP.ordinal();
    @JRubyConstant
    public final static int ROUND_HALF_DOWN = RoundingMode.HALF_DOWN.ordinal();
    @JRubyConstant
    public final static int ROUND_HALF_EVEN = RoundingMode.HALF_EVEN.ordinal();
    @JRubyConstant
    public final static int ROUND_HALF_UP = RoundingMode.HALF_UP.ordinal();
    @JRubyConstant
    public final static int ROUND_FLOOR = RoundingMode.FLOOR.ordinal();

    @JRubyConstant
    public final static int SIGN_POSITIVE_INFINITE = 3;
    @JRubyConstant
    public final static int SIGN_POSITIVE_ZERO = 1;
    @JRubyConstant
    public final static int SIGN_NEGATIVE_FINITE = -2;
    @JRubyConstant
    public final static int SIGN_NaN = 0;
    @JRubyConstant
    public final static int BASE = 10000;
    @JRubyConstant
    public final static int ROUND_MODE = 256;
    @JRubyConstant
    public final static int SIGN_POSITIVE_FINITE = 2;
    @JRubyConstant
    public final static int SIGN_NEGATIVE_INFINITE = -3;
    @JRubyConstant
    public final static int SIGN_NEGATIVE_ZERO = -1;

    @JRubyConstant
    public final static int EXCEPTION_INFINITY = 1;
    @JRubyConstant
    public final static int EXCEPTION_OVERFLOW = 1; // Note: This is same as EXCEPTION_INFINITY in MRI now
    @JRubyConstant
    public final static int EXCEPTION_NaN = 2;
    @JRubyConstant
    public final static int EXCEPTION_UNDERFLOW = 4;
    @JRubyConstant
    public final static int EXCEPTION_ZERODIVIDE = 16;
    @JRubyConstant
    public final static int EXCEPTION_ALL = 255;

    private static final ByteList VERSION = ByteList.create("1.3.4");

    // (MRI-like) internals

    private static final short VP_DOUBLE_FIG = 16;

    // #elif SIZEOF_BDIGITS >= 4
    // # define RMPD_COMPONENT_FIGURES 9
    private static final short RMPD_COMPONENT_FIGURES = 9;
    // rmpd_component_figures(void) { return RMPD_COMPONENT_FIGURES; }
    // #define VpBaseFig() rmpd_component_figures()
    private static final short BASE_FIG = RMPD_COMPONENT_FIGURES;

    // Static constants
    private static final double SQRT_10 = 3.162277660168379332;
    private static final long NEGATIVE_ZERO_LONG_BITS = Double.doubleToLongBits(-0.0);

    public static RubyClass createBigDecimal(Ruby runtime) {
        RubyClass bigDecimal = runtime.defineClass("BigDecimal", runtime.getNumeric(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

        bigDecimal.setConstant("VERSION", RubyString.newStringShared(runtime, VERSION));

        runtime.getKernel().defineAnnotatedMethods(BigDecimalKernelMethods.class);

        bigDecimal.setInternalModuleVariable("vpPrecLimit", RubyFixnum.zero(runtime));
        bigDecimal.setInternalModuleVariable("vpExceptionMode", RubyFixnum.zero(runtime));
        bigDecimal.setInternalModuleVariable("vpRoundingMode", runtime.newFixnum(ROUND_HALF_UP));

        bigDecimal.defineAnnotatedMethods(RubyBigDecimal.class);
        bigDecimal.defineAnnotatedConstants(RubyBigDecimal.class);

        bigDecimal.getSingletonClass().undefineMethod("allocate");

        //RubyModule bigMath = runtime.defineModule("BigMath");
        // NOTE: BigMath.exp and BigMath.pow should be implemented as native
        // for now @see jruby/bigdecimal.rb

        bigDecimal.defineConstant("NAN", newNaN(runtime));
        bigDecimal.defineConstant("INFINITY", newInfinity(runtime, 1));
        
        bigDecimal.setReifiedClass(RubyBigDecimal.class);

        return bigDecimal;
    }

    private boolean isNaN;
    private int infinitySign; // 0 (for finite), -1, +1
    private int zeroSign; // -1, +1 (if zero, otherwise 0)
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
        this.flags |= FROZEN_F;
    }

    public RubyBigDecimal(Ruby runtime, BigDecimal value) {
        super(runtime, runtime.getClass("BigDecimal"));
        this.isNaN = false;
        this.infinitySign = 0;
        this.zeroSign = 0;
        this.value = value;
        this.flags |= FROZEN_F;
    }

    public RubyBigDecimal(Ruby runtime, RubyClass klass, BigDecimal value) {
        super(runtime, klass);
        this.isNaN = false;
        this.infinitySign = 0;
        this.zeroSign = 0;
        this.value = value;
        this.flags |= FROZEN_F;
    }

    public RubyBigDecimal(Ruby runtime, BigDecimal value, int infinitySign) {
        this(runtime, value, infinitySign, 0);
    }

    public RubyBigDecimal(Ruby runtime, BigDecimal value, int infinitySign, int zeroSign) {
        super(runtime, runtime.getClass("BigDecimal"));
        this.isNaN = false;
        this.infinitySign = infinitySign;
        this.zeroSign = zeroSign;
        this.value = value;
        this.flags |= FROZEN_F;
    }

    public RubyBigDecimal(Ruby runtime, BigDecimal value, boolean isNan) {
        super(runtime, runtime.getClass("BigDecimal"));
        this.isNaN = isNan;
        this.infinitySign = 0;
        this.zeroSign = 0;
        this.value = value;
        this.flags |= FROZEN_F;
    }

    RubyBigDecimal(Ruby runtime, RubyClass klass, BigDecimal value, int zeroSign, int infinitySign, boolean isNaN) {
        super(runtime, klass);
        this.isNaN = isNaN;
        this.infinitySign = infinitySign;
        this.zeroSign = zeroSign;
        this.value = value;
        this.flags |= FROZEN_F;
    }

    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.BIGDECIMAL;
    }

    public static class BigDecimalKernelMethods {
        @JRubyMethod(name = "BigDecimal", module = true, visibility = Visibility.PRIVATE) // required = 1, optional = 1
        public static IRubyObject newBigDecimal(ThreadContext context, IRubyObject recv, IRubyObject arg) {
            return newInstance(context, context.runtime.getClass("BigDecimal"), arg, true, true);
        }

        @JRubyMethod(name = "BigDecimal", module = true, visibility = Visibility.PRIVATE) // required = 1, optional = 1
        public static IRubyObject newBigDecimal(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
            Ruby runtime = context.runtime;
            RubyClass bigDecimal = runtime.getClass("BigDecimal");

            IRubyObject maybeOpts = ArgsUtil.getOptionsArg(runtime, arg1, false);

            if (maybeOpts.isNil()) {
                return newInstance(context, bigDecimal, arg0, arg1, true, true);
            }

            IRubyObject exObj = ArgsUtil.extractKeywordArg(context, "exception", maybeOpts);

            boolean exception = exObj.isNil() ? true : exObj.isTrue();

            return newInstance(context, bigDecimal, arg0, true, exception);
        }

        @JRubyMethod(name = "BigDecimal", module = true, visibility = Visibility.PRIVATE) // required = 1, optional = 1
        public static IRubyObject newBigDecimal(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, IRubyObject opts) {
            Ruby runtime = context.runtime;

            IRubyObject maybeOpts = ArgsUtil.getOptionsArg(runtime, opts, false);

            if (maybeOpts.isNil()) {
                throw runtime.newArgumentError(3, 1, 2);
            }

            IRubyObject exObj = ArgsUtil.extractKeywordArg(context, "exception", maybeOpts);

            boolean exception = exObj.isNil() ? true : exObj.isTrue();

            return newInstance(context, context.runtime.getClass("BigDecimal"), arg0, arg1, true, exception);
        }
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
        String value = precisionAndValue.substring(precisionAndValue.indexOf(':') + 1);
        return newInstance(context, recv, RubyString.newString(context.runtime, value));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject double_fig(ThreadContext context, IRubyObject recv) {
        return context.runtime.newFixnum(VP_DOUBLE_FIG);
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

        if (arg == context.nil) return old;
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

    @JRubyMethod(meta = true)
    public static IRubyObject interpret_loosely(ThreadContext context, IRubyObject recv, IRubyObject str) {
        return newInstance(context, recv, str, false, true);
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
            if (value == context.nil) {
                return c.searchInternalModuleVariable("vpRoundingMode");
            }

            RoundingMode javaRoundingMode = javaRoundingModeFromRubyRoundingMode(context, value);
            RubyFixnum roundingMode = runtime.newFixnum(javaRoundingMode.ordinal());
            c.setInternalModuleVariable("vpRoundingMode", roundingMode);

            return roundingMode;
        }

        throw runtime.newTypeError("first argument for BigDecimal#mode invalid");
    }

    // The Fixnum cast should be fine because these are internal variables and user code cannot change them.
    private static long bigDecimalVar(Ruby runtime, String variableName) {
        return ((RubyFixnum) runtime.getClass("BigDecimal").searchInternalModuleVariable(variableName)).getLongValue();
    }

    private static RoundingMode getRoundingMode(Ruby runtime) {
        return RoundingMode.values()[(int) bigDecimalVar(runtime, "vpRoundingMode")];
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

    private static boolean isUnderflowExceptionMode(Ruby runtime) {
        return (bigDecimalVar(runtime, "vpExceptionMode") & EXCEPTION_UNDERFLOW) != 0;
    }

    private static boolean isZeroDivideExceptionMode(Ruby runtime) {
        return (bigDecimalVar(runtime, "vpExceptionMode") & EXCEPTION_ZERODIVIDE) != 0;
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
        if (value == null || value == context.nil) return "nil";
        if (value.isImmediate()) return RubyObject.inspect(context, value).toString();
        return value.getMetaClass().getBaseName();
    }

    private static BigDecimal toBigDecimal(final RubyInteger value) {
        if (value instanceof RubyFixnum) {
            return BigDecimal.valueOf(RubyNumeric.num2long(value));
        }
        return new BigDecimal(value.getBigIntegerValue());
    }

    private static RubyBigDecimal getVpRubyObjectWithPrecInner(ThreadContext context, RubyRational value, RoundingMode mode) {
        BigDecimal numerator = toBigDecimal(value.getNumerator());
        BigDecimal denominator = toBigDecimal(value.getDenominator());

        int len = numerator.precision() + denominator.precision(); // 0
        int pow = len / 4; // 0
        MathContext mathContext = new MathContext((pow + 1) * 4, mode);

        return new RubyBigDecimal(context.runtime, numerator.divide(denominator, mathContext));
    }

    private RubyBigDecimal getVpValueWithPrec(ThreadContext context, IRubyObject value, boolean must) {
        if (value instanceof RubyFloat) {
            double doubleValue = ((RubyFloat) value).getDoubleValue();

            if (Double.isInfinite(doubleValue)) {
                throw context.runtime.newFloatDomainError(doubleValue < 0 ? "-Infinity" : "Infinity");
            }
            if (Double.isNaN(doubleValue)) {
                throw context.runtime.newFloatDomainError("NaN");
            }

            MathContext mathContext = new MathContext(RubyFloat.DIG + 1, getRoundingMode(context.runtime));
            return new RubyBigDecimal(context.runtime, new BigDecimal(doubleValue, mathContext));
        }
        if (value instanceof RubyRational) {
            return div2Impl(context, ((RubyRational) value).getNumerator(), ((RubyRational) value).getDenominator(), this.value.precision() * BASE_FIG);
        }

        return getVpValue(context, value, must);
    }

    private static RubyBigDecimal getVpValue(ThreadContext context, IRubyObject value, boolean must) {
        switch (((RubyBasicObject) value).getNativeClassIndex()) {
            case BIGDECIMAL:
                return (RubyBigDecimal) value;
            case FIXNUM:
                return newInstance(context.runtime, context.runtime.getClass("BigDecimal"), (RubyFixnum) value, MathContext.UNLIMITED);
            case BIGNUM:
                return newInstance(context.runtime, context.runtime.getClass("BigDecimal"), (RubyBignum) value, MathContext.UNLIMITED);
            case FLOAT:
                return newInstance(context.runtime, context.runtime.getClass("BigDecimal"), (RubyFloat) value, new MathContext(RubyFloat.DIG));
            case RATIONAL:
                return newInstance(context, (RubyRational) value, new MathContext(RubyFloat.DIG));
        }
        return cannotBeCoerced(context, value, must);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject induced_from(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return getVpValue(context, arg, true);
    }

    private static RubyBigDecimal newInstance(Ruby runtime, IRubyObject recv, RubyBigDecimal arg) {
        return new RubyBigDecimal(runtime, (RubyClass) recv, arg.value, arg.zeroSign, arg.infinitySign, arg.isNaN);
    }

    private static RubyBigDecimal newInstance(Ruby runtime, IRubyObject recv, RubyFixnum arg, MathContext mathContext) {
        final long value = arg.getLongValue();
        if (value == 0) return newZero(runtime, 1);
        return new RubyBigDecimal(runtime, (RubyClass) recv, new BigDecimal(value, mathContext));
    }

    private static RubyBigDecimal newInstance(ThreadContext context, RubyRational arg, MathContext mathContext) {
        if (arg.getNumerator().isZero()) return newZero(context.runtime, 1);

        BigDecimal num = toBigDecimal(arg.getNumerator());
        BigDecimal den = toBigDecimal(arg.getDenominator());
        BigDecimal value;
        try {
            value = num.divide(den, mathContext);
        } catch (ArithmeticException e){
            value = num.divide(den, MathContext.DECIMAL64);
        }

        return new RubyBigDecimal(context.runtime, value);
    }

    private static RubyBigDecimal newInstance(Ruby runtime, IRubyObject recv, RubyFloat arg, MathContext mathContext) {
        // precision can be no more than float digits
        if (mathContext.getPrecision() > RubyFloat.DIG + 1) throw runtime.newArgumentError("precision too large");

        RubyBigDecimal res = newFloatSpecialCases(runtime, arg);
        if (res != null) return res;

        return new RubyBigDecimal(runtime, (RubyClass) recv, new BigDecimal(arg.getDoubleValue(), mathContext));
    }

    private static RubyBigDecimal newFloatSpecialCases(Ruby runtime, RubyFloat val) {
        if (val.isNaN()) return newNaN(runtime);
        if (val.isInfinite()) return newInfinity(runtime, val.getDoubleValue() == Double.POSITIVE_INFINITY ? 1 : -1);
        if (val.isZero()) return newZero(runtime, Double.doubleToLongBits(val.getDoubleValue()) == NEGATIVE_ZERO_LONG_BITS ? -1 : 1);
        return null;
    }

    private static RubyBigDecimal newInstance(Ruby runtime, IRubyObject recv, RubyBignum arg, MathContext mathContext) {
        final BigInteger value = arg.getBigIntegerValue();
        if (value.equals(BigInteger.ZERO)) return newZero(runtime, 1);
        return new RubyBigDecimal(runtime, (RubyClass) recv, new BigDecimal(value, mathContext));
    }

    private static IRubyObject newInstance(ThreadContext context, RubyClass recv, RubyString arg, MathContext mathContext, boolean strict, boolean exception) {
        // Convert String to Java understandable format (for BigDecimal).

        char[] str = arg.decodeString().toCharArray();
        int s = 0; int e = str.length - 1;

        if (e == 0) {
            switch (str[0]) {
                case '0':
                    return newZero(context.runtime, 1);
                case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    return new RubyBigDecimal(context.runtime, recv, BigDecimal.valueOf(str[0] - '0'));
            }
        }

        // 0. toString().trim() :
        while (s <= e && str[s] <= ' ') s++; // l-trim
        while (s <= e && str[e] <= ' ') e--; // r-trim

        int sign = 1;
        switch ( s <= e ? str[s] : ' ' ) {
            case '_' :
                if (!strict) return context.nil;
                throw invalidArgumentError(context, arg);
            case 'N' :
                if ( contentEquals("NaN", str, s, e) ) return newNaN(context.runtime);
                break;
            case 'I' :
                if ( contentEquals("Infinity", str, s, e) ) return newInfinity(context.runtime, 1);
                break;
            case '-' :
                if ( contentEquals("-Infinity", str, s, e) ) return newInfinity(context.runtime, -1);
                sign = -1;
                break;
            case '+' :
                if ( contentEquals("+Infinity", str, s, e) ) return newInfinity(context.runtime, +1);
                break;
        }

        int i = s; int off = 0; boolean dD = false;
        int exp = -1; int lastSign = -1;
        boolean dotFound = false;
        // 1. MRI allows d and D as exponent separators
        // 2. MRI allows underscores anywhere
        loop: while (i + off <= e) {
            switch (str[i + off]) {
                case 'd': case 'D': // replaceFirst("[dD]", "E")
                    if (dD) {
                        e = i - 1; continue; // not first - (trailing) junk
                    }
                    else {
                        str[i] = 'E'; dD = true;
                        if (exp == -1) exp = i;
                        else {
                            e = i - 1; continue; // (trailing) junk - DONE
                        }
                        i++; continue;
                    }
                case '_':
                    if ((i + off) == e || Character.isDigit(str[i + off + 1])) {
                        str[i] = str[i + off];
                        off++;
                        continue;
                    }
                    if (!strict) {
                        e = i + off - 1;
                        break loop;
                    }
                    throw invalidArgumentError(context, arg);
                // 3. MRI ignores the trailing junk
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    break;
                case '.':
                    // MRI allows multiple dots(e.g. "1.2.3".to_d)
                    if (dotFound == true) {
                        e = i + off - 1;
                        break loop;
                    } else {
                        dotFound = true;
                    }

                    // checking next character
                    if ((i + off + 1) <= e && !Character.isDigit(str[i + off + 1])) {
                        e = i + off - 1;
                        break loop;
                    }
                    break;
                case '-': case '+':
                    lastSign = i; break;
                case 'e': case 'E':
                    if (exp == -1) exp = i;
                    else {
                        e = i - 1; continue; // (trailing) junk - DONE
                    }

                    // checking next character
                    if (i + off + 1 <= e ) {
                        char c = str[i + off + 1];
                        if (( c == '-') || (c == '+') || Character.isDigit(c)) {

                        } else {
                            e = i + off - 1;
                            break loop;
                        }
                    }

                    break;
                default : // (trailing) junk - DONE
                    e = i - 1; continue;
            }
            str[i] = str[i+off];
            i++;
        }

        // scan remaining chars and error if too much junk left
        i = e + 1;
        if (i < str.length) {
            while (i < str.length && Character.isWhitespace(str[i++]));

            if (i < str.length && strict) {
                throw invalidArgumentError(context, arg);
            }
        }

        e -= off;

        if ( exp != -1 ) {
            if (exp == e || (exp + 1 == e && (str[exp + 1] == '-' || str[exp + 1] == '+'))) {
                if (!strict) return newZero(context.runtime, 1);
                throw invalidArgumentError(context, arg);
            }
            else if (isExponentOutOfRange(str, exp + 1, e)) {
                // Handle infinity (Integer.MIN_VALUE + 1) < expValue < Integer.MAX_VALUE
                // checking the sign of exponent part.
                if (isZeroBase(str, s, exp) || str[exp + 1] == '-') return newZero(context.runtime, sign);
                return newInfinity(context.runtime, sign);
            }
        }
        else if ( lastSign > s ) {
            e = lastSign - 1; // ignored tail junk e.g. "5-6" -> "-6"
        }

        BigDecimal decimal;
        try {
            decimal = new BigDecimal(str, s, e - s + 1, mathContext);
        }
        catch (ArithmeticException ex) {
            return checkOverUnderFlow(context.runtime, ex, false, strict, exception);
        }
        catch (NumberFormatException ex) {
            return handleInvalidArgument(context, arg, strict, exception);
        }

        // MRI behavior: -0 and +0 are two different things
        if (decimal.signum() == 0) return newZero(context.runtime, sign);

        return new RubyBigDecimal(context.runtime, recv, decimal);
    }

    private static IRubyObject handleInvalidArgument(ThreadContext context, RubyString arg, boolean strict, boolean exception) {
        if (!strict) {
            return newZero(context.runtime, 1);
        }
        if (!exception) {
            return context.nil;
        }
        throw invalidArgumentError(context, arg);
    }

    private static RaiseException invalidArgumentError(ThreadContext context, RubyString arg) {
        return context.runtime.newArgumentError("invalid value for BigDecimal(): \"" + arg + "\"");
    }

    private static boolean contentEquals(final String str1, final char[] str2, final int s2, final int e2) {
        final int len = str1.length();
        if (len == e2 - s2 + 1) {
            for (int i=0; i<len; i++) {
                if (str1.charAt(i) != str2[s2+i]) return false;
            }
            return true;
        }
        return false;
    }

    private static boolean isZeroBase(final char[] str, final int off, final int end) {
        for (int i=off; i<end; i++) {
            if (str[i] != '0') return false;
        }
        return true;
    }

    private static boolean isExponentOutOfRange(final char[] str, final int off, final int end) {
        int num = 0;
        int sign = 1;
        final char ch0 = str[off];
        if (ch0 == '-') {
            sign = -1;
        } else if (ch0 != '+') {
            num = '0' - ch0;
        }
        int i = off + 1;
        final int max = (sign == 1) ? -Integer.MAX_VALUE : Integer.MIN_VALUE + 1;
        final int multmax = max / 10;
        while (i <= end) {
            int d = str[i++] - '0';
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

    @Deprecated // no to be used in user-lang
    @JRubyMethod(name = "new", meta = true)
    public static IRubyObject new_(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        context.runtime.getWarnings().warn(IRubyWarnings.ID.DEPRECATED_METHOD, "BigDecimal.new is deprecated; use BigDecimal() method instead.");
        return BigDecimalKernelMethods.newBigDecimal(context, recv, arg);
    }

    @Deprecated // no to be used in user-lang
    @JRubyMethod(name = "new", meta = true)
    public static IRubyObject new_(ThreadContext context, IRubyObject recv, IRubyObject arg, IRubyObject mathArg) {
        context.runtime.getWarnings().warn(IRubyWarnings.ID.DEPRECATED_METHOD, "BigDecimal.new is deprecated; use BigDecimal() method instead.");
        return BigDecimalKernelMethods.newBigDecimal(context, recv, arg, mathArg);
    }

    @Deprecated // no to be used in user-lang
    @JRubyMethod(name = "new", meta = true)
    public static IRubyObject new_(ThreadContext context, IRubyObject recv, IRubyObject arg, IRubyObject mathArg, IRubyObject opts) {
        context.runtime.getWarnings().warn(IRubyWarnings.ID.DEPRECATED_METHOD, "BigDecimal.new is deprecated; use BigDecimal() method instead.");
        return BigDecimalKernelMethods.newBigDecimal(context, recv, arg, mathArg, opts);
    }

    public static RubyBigDecimal newInstance(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return (RubyBigDecimal) newInstance(context, recv, arg, true, true);
    }

    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject arg, boolean strict, boolean exception) {
        switch (((RubyBasicObject) arg).getNativeClassIndex()) {
            case RATIONAL:
                return handleMissingPrecision(context, "Rational", strict, exception);
            case FLOAT:
                RubyBigDecimal res = newFloatSpecialCases(context.runtime, (RubyFloat) arg);
                if (res != null) return res;
                return handleMissingPrecision(context, "Float", strict, exception);
            case FIXNUM:
                return newInstance(context.runtime, recv, (RubyFixnum) arg, MathContext.UNLIMITED);
            case BIGNUM:
                return newInstance(context.runtime, recv, (RubyBignum) arg, MathContext.UNLIMITED);
            case BIGDECIMAL:
                return newInstance(context.runtime, recv, (RubyBigDecimal) arg);
        }

        IRubyObject maybeString = arg.checkStringType();

        if (maybeString.isNil()) {
            if (!strict) return newZero(context.runtime, 1);
            if (!exception) return context.nil;
            throw context.runtime.newTypeError("no implicit conversion of " + arg.inspect() + "into to String");
        }
        return newInstance(context, (RubyClass) recv, maybeString.convertToString(), MathContext.UNLIMITED, strict, exception);
    }

    private static IRubyObject handleMissingPrecision(ThreadContext context, String name, boolean strict, boolean exception) {
        if (!strict) return newZero(context.runtime, 1);
        if (!exception) return context.nil;
        throw context.runtime.newArgumentError("can't omit precision for a " + name + ".");
    }

    public static RubyBigDecimal newInstance(ThreadContext context, IRubyObject recv, IRubyObject arg, IRubyObject mathArg) {
        return (RubyBigDecimal) newInstance(context, recv, arg, mathArg, true, true);
    }

    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject arg, IRubyObject mathArg, boolean strict, boolean exception) {
        int digits = (int) mathArg.convertToInteger().getLongValue();
        if (digits < 0) {
            if (!strict) return newZero(context.runtime, 1);
            if (!exception) return context.nil;
            throw context.runtime.newArgumentError("argument must be positive");
        }

        MathContext mathContext = new MathContext(digits);

        switch (((RubyBasicObject) arg).getNativeClassIndex()) {
            case RATIONAL:
                return newInstance(context, (RubyRational) arg, mathContext);
            case FLOAT:
                return newInstance(context.runtime, recv, (RubyFloat) arg, mathContext);
            case FIXNUM:
                return newInstance(context.runtime, recv, (RubyFixnum) arg, mathContext);
            case BIGNUM:
                return newInstance(context.runtime, recv, (RubyBignum) arg, mathContext);
            case BIGDECIMAL:
                return newInstance(context.runtime, recv, (RubyBigDecimal) arg);
        }
        return newInstance(context, (RubyClass) recv, arg.convertToString(), MathContext.UNLIMITED, strict, exception);
    }

    private static RubyBigDecimal newZero(final Ruby runtime, final int sign) {
        return new RubyBigDecimal(runtime, BigDecimal.ZERO, 0, sign < 0 ? -1 : 1);
    }

    private static RubyBigDecimal newNaN(final Ruby runtime) {
        if ( isNaNExceptionMode(runtime) ) {
            throw newNaNFloatDomainError(runtime);
        }
        return new RubyBigDecimal(runtime, BigDecimal.ZERO, true);
    }

    private static RaiseException newNaNFloatDomainError(final Ruby runtime) {
        return runtime.newFloatDomainError("Computation results to 'NaN'(Not a Number)");
    }

    private static RubyBigDecimal newInfinity(final Ruby runtime, final int sign) {
        if ( isInfinityExceptionMode(runtime) ) {
            throw newInfinityFloatDomainError(runtime, sign);
        }
        return new RubyBigDecimal(runtime, BigDecimal.ZERO, sign < 0 ? -1 : 1, 0);
    }

    private static RaiseException newInfinityFloatDomainError(final Ruby runtime, final int sign) {
        return runtime.newFloatDomainError("Computation results to " + (sign < 0 ? "'-Infinity'" : "'Infinity'"));
    }

    private RubyBigDecimal setResult() {
        return setResult(0);
    }

    private RubyBigDecimal setResult(int prec) {
        if (prec == 0) prec = getPrecLimit(getRuntime());
        int exponent;
        if (prec > 0 && this.value.scale() > (prec - (exponent = getExponent()))) {
            this.value = this.value.setScale(prec - exponent, RoundingMode.HALF_UP);
            this.absStripTrailingZeros = null;
        }
        return this;
    }

    private transient BigDecimal absStripTrailingZeros; // cached re-used 'normalized' value

    private BigDecimal absStripTrailingZeros() {
        BigDecimal absStripTrailingZeros = this.absStripTrailingZeros;
        if (absStripTrailingZeros == null) {
            return this.absStripTrailingZeros = value.abs().stripTrailingZeros();
        }
        return absStripTrailingZeros;
    }

    @Override
    @JRubyMethod
    public RubyFixnum hash() {
        return getRuntime().newFixnum(absStripTrailingZeros().hashCode() * value.signum());
    }

    @Override
    @JRubyMethod(name = "initialize_copy", visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(IRubyObject original) {
        if (this == original) return this;

        checkFrozen();

        if (!(original instanceof RubyBigDecimal)) {
            throw getRuntime().newTypeError("wrong argument class");
        }

        RubyBigDecimal orig = (RubyBigDecimal) original;

        this.isNaN = orig.isNaN;
        this.infinitySign = orig.infinitySign;
        this.zeroSign = orig.zeroSign;
        this.value = orig.value;

        return this;
    }

    @JRubyMethod(name = {"%", "modulo"}, required = 1)
    public IRubyObject op_mod(ThreadContext context, IRubyObject other) {
        RubyBigDecimal val = getVpValueWithPrec(context, other, false);

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
        return remainderInternal(context, getVpValueWithPrec(context, arg, false), arg);
    }

    @Deprecated
    public IRubyObject remainder19(ThreadContext context, IRubyObject arg) {
        return remainder(context, arg);
    }

    private IRubyObject remainderInternal(ThreadContext context, RubyBigDecimal val, IRubyObject arg) {
        if (isInfinity() || isNaN()) return newNaN(context.runtime);
        if (val == null) return callCoerced(context, sites(context).remainder, arg, true);
        if (val.isInfinity() || val.isNaN() || val.isZero()) return newNaN(context.runtime);

        // Java and MRI definitions of remainder are the same.
        return new RubyBigDecimal(context.runtime, value.remainder(val.value)).setResult();
    }

    @JRubyMethod(name = "*", required = 1)
    public IRubyObject op_mul(ThreadContext context, IRubyObject arg) {
        RubyBigDecimal val = getVpValueWithPrec(context, arg, false);
        if (val == null) return callCoerced(context, sites(context).op_times, arg, true);
        return multImpl(context.runtime, val);
    }

    @Deprecated
    public IRubyObject op_mul19(ThreadContext context, IRubyObject arg) {
        return op_mul(context, arg);
    }

    @JRubyMethod(name = "mult", required = 2)
    public IRubyObject mult2(ThreadContext context, IRubyObject b, IRubyObject n) {
        final int mx = getPrecisionInt(context.runtime, n);
        if (mx == 0) return op_mul(context, b);

        RubyBigDecimal val = getVpValueWithPrec(context, b, false);
        if (val == null) { // TODO: what about n arg?
            return callCoerced(context, sites(context).op_times, b, true);
        }

        return multImpl(context.runtime, val).setResult(mx);
    }

    private RubyBigDecimal multImpl(final Ruby runtime, RubyBigDecimal val) {
        if ( isNaN() || val.isNaN() ) return newNaN(runtime);

        if ( isZero() || val.isZero() ) {
            if ((isInfinity() && val.isZero()) || (isZero() && val.isInfinity())) return newNaN(runtime);

            int sign1 = isZero()? zeroSign : value.signum();
            int sign2 = val.isZero() ?  val.zeroSign : val.value.signum();
            return newZero(runtime, sign1 * sign2);
        }

        if ( isInfinity() || val.isInfinity() ) {
            int sign1 = isInfinity() ? infinitySign : value.signum();
            int sign2 = val.isInfinity() ? val.infinitySign : val.value.signum();
            return newInfinity(runtime, sign1 * sign2);
        }

        int mx = value.precision() + val.value.precision();

        MathContext mathContext = new MathContext(mx, getRoundingMode(runtime));
        BigDecimal result;
        try {
            result = value.multiply(val.value, mathContext);
        }
        catch (ArithmeticException ex) {
            return (RubyBigDecimal) checkOverUnderFlow(runtime, ex, false, true, true);
        }
        return new RubyBigDecimal(runtime, result).setResult();
    }

    private static IRubyObject checkOverUnderFlow(final Ruby runtime, final ArithmeticException ex, boolean nullDefault, boolean strict, boolean exception) {
        String message = ex.getMessage();
        if (message == null) message = "";
        message = message.toLowerCase(Locale.ENGLISH);
        if (message.contains("underflow")) {
            if (isUnderflowExceptionMode(runtime)) {
                return handleFloatDomainError(runtime, message, strict, exception);
            }
            return newZero(runtime, 1);
        }
        if (message.contains("overflow")) {
            if (isOverflowExceptionMode(runtime)) {
                return handleFloatDomainError(runtime, message, strict, exception);
            }
            return newInfinity(runtime, 1); // TODO sign?
        }
        if (nullDefault) return null;
        return handleFloatDomainError(runtime, message, strict, exception);
    }

    private static IRubyObject handleFloatDomainError(Ruby runtime, String message, boolean strict, boolean exception) {
        if (!strict) return newZero(runtime, 1);
        if (!exception) return runtime.getNil();
        throw runtime.newFloatDomainError(message);
    }

    @Deprecated
    public IRubyObject mult219(ThreadContext context, IRubyObject b, IRubyObject n) {
        return mult2(context, b, n);
    }

    // Calculate appropriate zero or infinity depending on exponent...
    private RubyBigDecimal newPowOfInfinity(ThreadContext context, RubyNumeric exp) {
        if (Numeric.f_negative_p(context, exp)) {
            if (infinitySign >= 0) return newZero(context.runtime, 0);

            // (-Infinity) ** (-even_integer) -> +0 AND (-Infinity) ** (-odd_integer) -> -0
            if (Numeric.f_integer_p(context, exp)) return newZero(context.runtime, isEven(exp) ? 1 : -1);

            return newZero(context.runtime, -1); // (-Infinity) ** (-non_integer) -> -0
        }

        if (infinitySign >= 0) return newInfinity(context.runtime, 1);

        if (Numeric.f_integer_p(context, exp)) return newInfinity(context.runtime, isEven(exp) ? 1 : -1);

        throw context.runtime.newMathDomainError("a non-integral exponent for a negative base");
    }

    // Calculate appropriate zero or infinity depending on exponent
    private RubyBigDecimal newPowOfZero(ThreadContext context, RubyNumeric exp) {
        if (Numeric.f_negative_p(context, exp)) {
            /* (+0) ** (-num)  -> Infinity */
            if (zeroSign >= 0) return newInfinity(context.runtime, 1);

            // (-0) ** (-even_integer) -> +Infinity  AND (-0) ** (-odd_integer) -> -Infinity
            if (Numeric.f_integer_p(context, exp)) return newInfinity(context.runtime, isEven(exp) ? 1 : -1);

            return newInfinity(context.runtime, -1); // (-0) ** (-non_integer) -> Infinity
        }

        if (Numeric.f_zero_p(context, exp)) return new RubyBigDecimal(context.runtime, BigDecimal.ONE);

        return newZero(context.runtime, 1);
    }

    private static IRubyObject vpPrecLimit(final Ruby runtime) {
        return runtime.getClass("BigDecimal").searchInternalModuleVariable("vpPrecLimit");
    }

    private static int getPrecLimit(final Ruby runtime) {
        return RubyNumeric.fix2int(vpPrecLimit(runtime));
    }

    @Deprecated
    public IRubyObject op_pow(IRubyObject arg) {
        return op_pow(getRuntime().getCurrentContext(), arg);
    }

    @JRubyMethod(name = {"**", "power"}, required = 1)
    public RubyBigDecimal op_pow(final ThreadContext context, IRubyObject exp) {
        final Ruby runtime = context.runtime;

        if (isNaN()) return newNaN(runtime);

        if ( ! (exp instanceof RubyNumeric) ) {
            throw context.runtime.newTypeError("wrong argument type " + exp.getMetaClass() + " (expected scalar Numeric)");
        } else if (exp instanceof RubyFixnum) {

        } else if (exp instanceof RubyBignum) {

        } else if (exp instanceof RubyFloat) {
            double d = RubyNumeric.num2dbl(context, exp);
            if (d == Math.round(d)) {
                if (RubyNumeric.fixable(runtime, d)) {
                    exp = RubyFixnum.newFixnum(runtime, (long)d);
                } else {
                    exp = RubyBignum.newBignorm(runtime, d);
                }
            }
        } else if (exp instanceof RubyRational) {
            if (Numeric.f_zero_p(context, Numeric.f_numerator(context, exp))) {

            } else if (Numeric.f_one_p(context, Numeric.f_denominator(context, exp))) {
                exp = Numeric.f_numerator(context, exp);
            }
        } else if (exp instanceof RubyBigDecimal) {
            IRubyObject zero = RubyNumeric.int2fix(runtime, 0);
            IRubyObject rounded = ((RubyBigDecimal)exp).round(context, new IRubyObject[]{zero});
            if (((RubyBigDecimal)exp).eql_p(context, rounded).isTrue()) {
                exp = ((RubyBigDecimal)exp).to_int();
            }
        }

        if (isZero()) return newPowOfZero(context, (RubyNumeric) exp);

        if (Numeric.f_zero_p(context, exp)) return new RubyBigDecimal(context.runtime, BigDecimal.ONE);

        if (isInfinity()) return newPowOfInfinity(context, (RubyNumeric) exp);

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
        return addInternal(context, getVpValueWithPrec(context, b, false), b, vpPrecLimit(context.runtime));
    }

    @Deprecated
    public IRubyObject op_plus19(ThreadContext context, IRubyObject b) {
        return op_plus(context, b);
    }

    @JRubyMethod(name = "add")
    public IRubyObject add2(ThreadContext context, IRubyObject b, IRubyObject digits) {
        return addInternal(context, getVpValueWithPrec(context, b, false), b, digits);
    }

    @Deprecated
    public IRubyObject add219(ThreadContext context, IRubyObject b, IRubyObject digits) {
        return add2(context, b, digits);
    }

    private IRubyObject addInternal(ThreadContext context, RubyBigDecimal val, IRubyObject b, IRubyObject digits) {
        if (val == null) {
            // TODO:
            // MRI behavior: Call "+" or "add", depending on the call.
            // But this leads to exceptions when Floats are added. See:
            // http://blade.nagaokaut.ac.jp/cgi-bin/scat.rb/ruby/ruby-core/17374
            // return callCoerced(context, op, b, true); -- this is MRI behavior.
            // We'll use ours for now, thus providing an ability to add Floats.
            return callCoerced(context, sites(context).op_plus, b, true);
        }

        RubyBigDecimal res = addSpecialCases(context, val);
        if ( res != null ) return res;

        final Ruby runtime = context.runtime;
        int prec = getPositiveInt(context, digits);

        MathContext mathContext = new MathContext(prec, getRoundingMode(runtime));
        return new RubyBigDecimal(runtime, value.add(val.value, mathContext)).setResult(prec);
    }

    private static int getPositiveInt(ThreadContext context, IRubyObject arg) {
        if ( arg instanceof RubyFixnum ) {
            int value = RubyNumeric.fix2int(arg);
            if (value < 0) {
                throw context.runtime.newArgumentError("argument must be positive");
            }
            return value;
        }
        throw context.runtime.newTypeError(arg, context.runtime.getFixnum());
    }

    private RubyBigDecimal addSpecialCases(ThreadContext context, RubyBigDecimal val) {
        if (isNaN() || val.isNaN) {
            return newNaN(context.runtime);
        }
        if (isZero()) {
            if (val.isZero()) {
                return newZero(context.runtime, zeroSign == val.zeroSign ? zeroSign : 1);
            }
            if (val.isInfinity()) {
                return newInfinity(context.runtime, val.infinitySign);
            }
            return new RubyBigDecimal(context.runtime, val.value);
        }

        int sign = infinitySign * val.infinitySign;

        if (sign > 0) return newInfinity(context.runtime, infinitySign);
        if (sign < 0) return newNaN(context.runtime);
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
        return subInternal(context, getVpValueWithPrec(context, b, false), b, 0);
    }

    @Deprecated
    public IRubyObject op_minus19(ThreadContext context, IRubyObject b) {
        return op_minus(context, b);
    }

    @JRubyMethod(name = "sub", required = 2)
    public IRubyObject sub2(ThreadContext context, IRubyObject b, IRubyObject n) {
        return subInternal(context, getVpValueWithPrec(context, b, false), b, getPositiveInt(context, n));
    }

    @Deprecated
    public IRubyObject sub219(ThreadContext context, IRubyObject b, IRubyObject n) {
        return sub2(context, b, n);
    }

    private IRubyObject subInternal(ThreadContext context, RubyBigDecimal val, IRubyObject b, int prec) {
        if (val == null) return callCoerced(context, sites(context).op_minus, b, true);
        RubyBigDecimal res = subSpecialCases(context, val);
        return res != null ? res : new RubyBigDecimal(context.runtime, value.subtract(val.value)).setResult(prec);
    }

    private RubyBigDecimal subSpecialCases(ThreadContext context, RubyBigDecimal val) {
        if (isNaN() || val.isNaN()) {
            return newNaN(context.runtime);
        }
        if (isZero()) {
            if (val.isZero()) return newZero(context.runtime, zeroSign * val.zeroSign);
            if (val.isInfinity()) return newInfinity(context.runtime, val.infinitySign * -1);
            return new RubyBigDecimal(context.runtime, val.value.negate());
        }

        int sign = infinitySign * val.infinitySign;

        if (sign > 0) return newNaN(context.runtime);
        if (sign < 0) return newInfinity(context.runtime, infinitySign);
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
        RubyBigDecimal val = getVpValueWithPrec(context, other, false);
        if (val == null) return callCoerced(context, sites(context).op_quo, other, true);

        if (isNaN() || val.isNaN()) return newNaN(context.runtime);

        RubyBigDecimal div = divSpecialCases(context, val);
        if (div != null) return div;

        return quoImpl(context, val);
    }

    private RubyBigDecimal quoImpl(ThreadContext context, RubyBigDecimal that) {
        int mx = this.value.precision();
        int mxb = that.value.precision();
        if (mx < mxb) mx = mxb;
        mx = (mx + 1) * BASE_FIG;

        final int limit = getPrecLimit(context.runtime);
        if (limit > 0 && limit < mx) mx = limit;

        MathContext mathContext = new MathContext(mx, getRoundingMode(context.runtime));
        return new RubyBigDecimal(context.runtime, divide(this.value, that.value, mathContext)).setResult(limit);
    }

    // NOTE: base on Android's
    // https://android.googlesource.com/platform/libcore/+/refs/heads/master/luni/src/main/java/java/math/BigDecimal.java
    private static BigDecimal divide(BigDecimal target, BigDecimal divisor, MathContext mc) {
        assert mc.getPrecision() != 0; // NOTE: handled by divSpecialCases
        /* Calculating how many zeros must be append to 'dividend'
         * to obtain a  quotient with at least 'mc.precision()' digits */
        long trailingZeros = (long) mc.getPrecision() + 1L + divisor.precision() - target.precision();
        long diffScale = (long) target.scale() - divisor.scale();
        long newScale = diffScale; // scale of the final quotient
        BigInteger quotAndRem[] = { target.unscaledValue() };
        final BigInteger divScaled = divisor.unscaledValue();
        if (trailingZeros > 0) {
            // To append trailing zeros at end of dividend
            quotAndRem[0] = quotAndRem[0].multiply(Multiplication.powerOf10(trailingZeros));
            newScale += trailingZeros;
        }
        quotAndRem = quotAndRem[0].divideAndRemainder(divScaled);
        BigInteger integerQuot = quotAndRem[0];
        // Calculating the exact quotient with at least 'mc.precision()' digits
        if (quotAndRem[1].signum() != 0) {
            // Checking if:   2 * remainder >= divisor ?
            int compRem = shiftLeftOneBit(quotAndRem[1]).compareTo(divScaled);
            // quot := quot * 10 + r;     with 'r' in {-6,-5,-4, 0,+4,+5,+6}
            integerQuot = integerQuot.multiply(BigInteger.TEN)
                    .add(BigInteger.valueOf(quotAndRem[0].signum() * (5 + compRem)));
            newScale++;
        } // else BigDecimal() will scale 'down'
        return new BigDecimal(integerQuot, safeLongToInt(newScale), mc);
    }


    private static BigInteger shiftLeftOneBit(BigInteger i) { return i.shiftLeft(1); }

    private static int safeLongToInt(long longValue) {
        if (longValue < Integer.MIN_VALUE || longValue > Integer.MAX_VALUE) {
            throw new ArithmeticException("Out of int range: " + longValue);
        }
        return (int) longValue;
    }

    private static RubyBigDecimal div2Impl(ThreadContext context, RubyNumeric a, RubyNumeric b, final int ix) {
        RubyBigDecimal thiz = getVpValue(context, a, true);
        RubyBigDecimal that = getVpValue(context, b, true);

        if (thiz.isNaN() || that.isNaN()) return newNaN(context.runtime);

        RubyBigDecimal div = thiz.divSpecialCases(context, that);
        if (div != null) return div;

        int mx = thiz.value.precision() + that.value.precision() + 2;

        MathContext mathContext = new MathContext((mx * 2 + 2) * BASE_FIG, getRoundingMode(context.runtime));
        return new RubyBigDecimal(context.runtime, thiz.value.divide(that.value, mathContext)).setResult(ix);
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
    public IRubyObject op_div(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyRational) return idiv(context, (RubyRational) other);

        RubyBigDecimal val = getVpValue(context, other, false);
        if (val == null) return callCoerced(context, sites(context).div, other, true);

        if (isNaN() || val.isNaN()) throw newNaNFloatDomainError(context.runtime);
        if (isInfinity()) { // NOTE: MRI is inconsistent with div(other, d) impl
            if (val.isInfinity()) throw newNaNFloatDomainError(context.runtime);
            throw newInfinityFloatDomainError(context.runtime, infinitySign);
        }
        if (val.isZero()) throw context.runtime.newZeroDivisionError();

        if (val.isInfinity()) return RubyFixnum.zero(context.runtime);

        BigDecimal result = this.value.divideToIntegralValue(val.value);
        return toInteger(context.runtime, result);
    }

    private RubyInteger idiv(ThreadContext context, RubyRational val) {
        if (isNaN()) throw newNaNFloatDomainError(context.runtime);
        if (isInfinity()) { // NOTE: MRI is inconsistent with div(other, d) impl
            throw newInfinityFloatDomainError(context.runtime, infinitySign);
        }
        if (val.isZero()) throw context.runtime.newZeroDivisionError();

        BigDecimal result = this.value.multiply(toBigDecimal(val.getDenominator()))
                                      .divideToIntegralValue(toBigDecimal(val.getNumerator()));
        return toInteger(context.runtime, result);
    }

    private static final BigDecimal MAX_FIX = BigDecimal.valueOf(RubyFixnum.MAX);
    private static final BigDecimal MIN_FIX = BigDecimal.valueOf(RubyFixnum.MIN);

    private static RubyInteger toInteger(final Ruby runtime, final BigDecimal result) {
        if (result.compareTo(MAX_FIX) <= 0 && result.compareTo(MIN_FIX) >= 0) {
            return RubyFixnum.newFixnum(runtime, result.longValue());
        }
        return RubyBignum.newBignum(runtime, result.toBigInteger());
    }

    @JRubyMethod(name = "div")
    public IRubyObject op_div(ThreadContext context, IRubyObject other, IRubyObject digits) {
        RubyBigDecimal val = getVpValue(context, other, false);
        if (val == null) return callCoerced(context, sites(context).div, other, true);

        if (isNaN() || val.isNaN()) return newNaN(context.runtime);

        RubyBigDecimal div = divSpecialCases(context, val);
        if (div != null) return div;

        final int scale = RubyNumeric.fix2int(digits);
        // MRI behavior: "If digits is 0, the result is the same as the / operator."
        if (scale == 0) return quoImpl(context, val);

        MathContext mathContext = new MathContext(scale, getRoundingMode(context.runtime));
        return new RubyBigDecimal(context.runtime, value.divide(val.value, mathContext)).setResult(scale);
    }

    private RubyBigDecimal divSpecialCases(ThreadContext context, RubyBigDecimal val) {
        if (isInfinity()) {
            if (val.isInfinity()) return newNaN(context.runtime);
            return newInfinity(context.runtime, infinitySign * val.value.signum());
        }
        if (val.isInfinity()) return newZero(context.runtime, value.signum() * val.infinitySign);

        if (val.isZero()) {
            if (isZero()) return newNaN(context.runtime);
            if (isZeroDivideExceptionMode(context.runtime)) {
                throw context.runtime.newFloatDomainError("Divide by zero");
            }
            int sign1 = isInfinity() ? infinitySign : value.signum();
            return newInfinity(context.runtime, sign1 * val.zeroSign);
        }
        if (isZero()) return newZero(context.runtime, zeroSign * val.value.signum());

        return null;
    }

    @Deprecated
    public final IRubyObject op_div19(ThreadContext context, IRubyObject r) {
        return op_div(context, r);
    }

    @Deprecated
    public final IRubyObject op_div19(ThreadContext context, IRubyObject other, IRubyObject digits) {
        return op_div(context, other, digits);
    }

    private IRubyObject cmp(ThreadContext context, final IRubyObject arg, final char op) {
        final int e;
        RubyBigDecimal rb = getVpValue(context, arg, false);
        if (rb == null) {
            String id = "!=";
            switch (op) {
                case '*':
                    if (falsyEqlCheck(context, arg)) return context.nil;
                    return callCoerced(context, sites(context).op_cmp, arg, false);
                case '=': {
                    if (falsyEqlCheck(context, arg)) return context.fals;
                    IRubyObject res = callCoerced(context, sites(context).op_eql, arg, false);
                    return RubyBoolean.newBoolean(context, res != context.nil && res != context.fals);
                }
                case '!':
                    if (falsyEqlCheck(context, arg)) return context.tru;
                    /* id = "!="; */ break;
                case 'G': id = ">="; break;
                case 'L': id = "<="; break;
                case '<': id =  "<"; break;
                case '>': id =  ">"; break;
            }

            IRubyObject cmp = callCoerced(context, id, arg);
            if (cmp == context.nil) { // arg.coerce failed
                throw context.runtime.newArgumentError("comparison of BigDecimal with "+ errMessageType(context, arg) +" failed");
            }
            return cmp;
        }

        if (isNaN() || rb.isNaN()) return (op == '*') ? context.nil : context.fals;
        e = infinitySign != 0 || rb.infinitySign != 0 ? infinitySign - rb.infinitySign : value.compareTo(rb.value);

        switch (op) {
            case '*': return context.runtime.newFixnum(e);
            case '=': return RubyBoolean.newBoolean(context, e == 0);
            case '!': return RubyBoolean.newBoolean(context, e != 0);
            case 'G': return RubyBoolean.newBoolean(context, e >= 0);
            case '>': return RubyBoolean.newBoolean(context, e >  0);
            case 'L': return RubyBoolean.newBoolean(context, e <= 0);
            case '<': return RubyBoolean.newBoolean(context, e <  0);
        }
        return context.nil;
    }

    // NOTE: otherwise `BD == nil` etc gets ___reeeaaally___ slow (due exception throwing)
    private static boolean falsyEqlCheck(final ThreadContext context, final IRubyObject arg) {
        return arg == context.nil || arg == context.fals || arg == context.tru;
    }

    @Override
    @JRubyMethod(name = "<=>", required = 1)
    public IRubyObject op_cmp(ThreadContext context, IRubyObject arg) {
        return cmp(context, arg, '*');
    }

    // NOTE: do not use BigDecimal#equals since ZERO.equals(new BD('0.0')) -> false
    @Override
    @JRubyMethod(name = {"eql?", "=="}, required = 1)
    public IRubyObject eql_p(ThreadContext context, IRubyObject arg) {
        return cmp(context, arg, '=');
    }

    @Override
    @JRubyMethod(name = "===", required = 1) // same as == (eql?)
    public IRubyObject op_eqq(ThreadContext context, IRubyObject arg) {
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
        final Ruby runtime = context.runtime;

        RubyBigDecimal val = getVpValueWithPrec(context, other, false);
        if (val == null) return callCoerced(context, sites(context).divmod, other, true);

        if (isNaN() || val.isNaN() || isInfinity() && val.isInfinity()) return RubyArray.newArray(runtime, newNaN(runtime), newNaN(runtime));
        if (val.isZero()) throw runtime.newZeroDivisionError();
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

    private RubyBigDecimal floorNaNInfinityCheck(ThreadContext context) {
        if (isNaN()) throw newNaNFloatDomainError(context.runtime);
        if (isInfinity()) throw newInfinityFloatDomainError(context.runtime, infinitySign);
        return null;
    }

    private RubyBigDecimal floorImpl(ThreadContext context, int n) {
        return value.scale() > n ? new RubyBigDecimal(context.runtime, value.setScale(n, RoundingMode.FLOOR)) : this;
    }

    @JRubyMethod
    public IRubyObject floor(ThreadContext context) {
        RubyBigDecimal res = floorNaNInfinityCheck(context);
        if (res != null) return res;
        return floorImpl(context, 0).to_int(context.runtime);
    }

    @JRubyMethod
    public IRubyObject floor(ThreadContext context, IRubyObject arg) {
        RubyBigDecimal res = floorNaNInfinityCheck(context);
        if (res != null) return res;
        return floorImpl(context, RubyNumeric.fix2int(arg));
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
        return infinitySign == 0 ? context.nil : context.runtime.newFixnum(infinitySign);
    }

    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        return toStringImpl(context.runtime, null);
    }

    @JRubyMethod(name = "nan?")
    public IRubyObject nan_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context, isNaN());
    }

    @Override
    @JRubyMethod(name = "nonzero?")
    public IRubyObject nonzero_p(ThreadContext context) {
        return isZero() ? context.nil : this;
    }

    @Deprecated
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
        if (isNaN()) {
            if (args.length == 0) {
                throw newNaNFloatDomainError(runtime);
            }
            return newNaN(runtime);
        }
        if (isInfinity()) {
            if (args.length == 0) {
                throw newInfinityFloatDomainError(runtime, infinitySign);
            }
            return newInfinity(runtime, infinitySign);
        }

        RoundingMode mode = getRoundingMode(runtime);
        int scale = 0;

        int argc = args.length;
        switch (argc) {
            case 2:
                mode = javaRoundingModeFromRubyRoundingMode(context, args[1]);
                scale = num2int(args[0]);
            case 1:
                if (ArgsUtil.getOptionsArg(runtime, args[0]) == context.nil) {
                    scale = num2int(args[0]);
                } else {
                    mode = javaRoundingModeFromRubyRoundingMode(context, args[0]);
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

        return args.length == 0 ? bigDecimal.to_int(runtime) : bigDecimal;
    }

    public IRubyObject round(ThreadContext context, IRubyObject scale, IRubyObject mode) {
        return round(context, new IRubyObject[]{scale, mode});
    }

    //this relies on the Ruby rounding enumerations == Java ones, which they (currently) all are
    private static RoundingMode javaRoundingModeFromRubyRoundingMode(ThreadContext context, IRubyObject arg) {
        if (arg == context.nil) return getRoundingMode(context.runtime);
        IRubyObject opts = ArgsUtil.getOptionsArg(context.runtime, arg);
        if (opts != context.nil) {
            arg = ArgsUtil.extractKeywordArg(context, (RubyHash) opts, "half");
            if (arg == null || arg == context.nil) return getRoundingMode(context.runtime);
            String roundingMode = arg.asJavaString();
            switch (roundingMode) {
                case "up":
                    return RoundingMode.HALF_UP;
                case "down" :
                    return RoundingMode.HALF_DOWN;
                case "even" :
                    return RoundingMode.HALF_EVEN;
                default :
                    throw context.runtime.newArgumentError("invalid rounding mode: " + roundingMode);
            }
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
                    throw context.runtime.newArgumentError("invalid rounding mode: " + roundingMode);
            }
        } else {
            int ordinal = num2int(arg);
            RoundingMode[] values = RoundingMode.values();
            if (ordinal < 0 || ordinal >= values.length) {
                throw context.runtime.newArgumentError("invalid rounding mode");
            }
            return values[ordinal];
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
        return absStripTrailingZeros().unscaledValue().toString();
    }

    private String getAllDigits() {
        return value.unscaledValue().abs().toString();
    }

    private int getExponent() {
        if (isZero() || isNaN() || isInfinity()) return 0;

        BigDecimal val = absStripTrailingZeros();
        return val.precision() - val.scale();
    }

    @JRubyMethod
    public IRubyObject sqrt(IRubyObject arg) {
        Ruby runtime = getRuntime();
        if (isNaN()) throw runtime.newFloatDomainError("sqrt of NaN");
        if ((isInfinity() && infinitySign < 0) || value.signum() < 0) throw runtime.newFloatDomainError("sqrt of negative value");
        if (isInfinity() && infinitySign > 0) return newInfinity(runtime, 1);

        // NOTE: MRI's sqrt precision is limited by 100, but we allow values more than 100.
        int n = getPrecisionInt(runtime, arg) + VP_DOUBLE_FIG + BASE_FIG;
        //int mx = value.precision() * (BASE_FIG + 1);
        //if (mx <= n) mx = n;

        return new RubyBigDecimal(runtime, bigSqrt(value, new MathContext(n, RoundingMode.HALF_UP))).setResult();
    }

    // MRI: GetPrecisionInt(VALUE v)
    private static int getPrecisionInt(final Ruby runtime, final IRubyObject v) {
        int n = RubyNumeric.num2int(v);
        if (n < 0) throw runtime.newArgumentError("negative precision");
        return n;
    }

    @JRubyMethod
    public IRubyObject to_f() { return toFloat(getRuntime(), true); }

    private RubyFloat toFloat(final Ruby runtime, final boolean checkFlow) {
        if (isNaN()) return RubyFloat.newFloat(runtime, Double.NaN);
        if (isInfinity()) return RubyFloat.newFloat(runtime, infinitySign < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
        if (isZero()) return RubyFloat.newFloat(runtime, zeroSign < 0 ? -0.0 : 0.0);

        if (-value.scale() <= RubyFloat.MAX_10_EXP) {
            return RubyFloat.newFloat(runtime, SafeDoubleParser.doubleValue(value));
        }

        switch (value.signum()) {
            case -1:
                if (checkFlow && isOverflowExceptionMode(runtime)) {
                    throw runtime.newFloatDomainError("BigDecimal to Float conversion");
                }
                return RubyFloat.newFloat(getRuntime(), Double.NEGATIVE_INFINITY);
            case 0:
                if (checkFlow && isUnderflowExceptionMode(runtime)) {
                    throw runtime.newFloatDomainError("BigDecimal to Float conversion");
                }
                return RubyFloat.newFloat(getRuntime(), 0);
            case 1:
                if (checkFlow && isOverflowExceptionMode(runtime)) {
                    throw runtime.newFloatDomainError("BigDecimal to Float conversion");
                }
                return RubyFloat.newFloat(getRuntime(), Double.POSITIVE_INFINITY);
            default :
                throw new AssertionError("invalid signum: " + value.signum() + " for BigDecimal " + this);
        }
    }

    @Override
    public RubyFloat convertToFloat() {
        return toFloat(getRuntime(), false);
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
            return RubyFixnum.newFixnum(runtime, value.longValueExact());
        } catch (ArithmeticException ex) {
            return RubyBignum.bignorm(runtime, value.toBigInteger());
        }
    }

    @Override
    public RubyInteger convertToInteger() {
        return to_int(getRuntime());
    }

    @JRubyMethod(name = "to_r")
    public IRubyObject to_r(ThreadContext context) {
        checkFloatDomain();

        int scale = value.scale();
        BigInteger numerator = value.scaleByPowerOfTen(scale).toBigInteger();
        BigInteger denominator = BigInteger.TEN.pow(scale);

        return RubyRational.newInstance(context, RubyBignum.newBignum(context.runtime, numerator), RubyBignum.newBignum(context.runtime, denominator));
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

    @Override
    public final boolean isZero() {
        return !isNaN() && !isInfinity() && (value.signum() == 0);
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

    private static StringBuilder appendSign(final StringBuilder buff, final String str, int signum) {
        if (signum == -1) buff.append('-');
        else if (signum == 1) {
            if (posSign(str)) {
                buff.append(posSpace(str) ? ' ' : '+');
            }
        }
        return buff;
    }

    private CharSequence engineeringValue(final String arg) {
        final String s = removeTrailingZeroes(unscaledValue());

        StringBuilder build = new StringBuilder();
        appendSign(build, arg, value.signum()).append('0').append('.');

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
        List<String> values = StringSupport.split(absStripTrailingZeros().toPlainString(), '.');
        final String whole = values.size() > 0 ? values.get(0) : "0";
        final String after = values.size() > 1 ? values.get(1) : "0";

        StringBuilder build = new StringBuilder();
        appendSign(build, arg, value.signum());

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

    @Override
    public IRubyObject to_s() {
        return toStringImpl(getRuntime(), null);
    }

    @JRubyMethod
    public RubyString to_s(ThreadContext context) {
        return toStringImpl(context.runtime, null);
    }

    @JRubyMethod
    public RubyString to_s(ThreadContext context, IRubyObject arg) {
        return toStringImpl(context.runtime, arg == context.nil ? null : arg.toString());
    }

    private RubyString toStringImpl(final Ruby runtime, String arg) {
        if ( isNaN() ) return runtime.newString("NaN");
        if ( isInfinity() ) {
            if ( arg != null && infinitySign >= 0) {
                if ( formatHasLeadingSpace(arg) ) return runtime.newString(" Infinity");
                if ( formatHasLeadingPlus(arg) ) return runtime.newString("+Infinity");
            }
            return runtime.newString(infinityString(infinitySign));
        }
        if ( isZero() ) {
            if ( zeroSign < 0 ) {
                return runtime.newString("-0.0");
            } else {
                if ( arg != null && formatHasLeadingSpace(arg) ) return runtime.newString(" 0.0");
                if ( arg != null && formatHasLeadingPlus(arg) ) return runtime.newString("+0.0");
                return runtime.newString("0.0");
            }
        }

        boolean asEngineering = arg == null || ! formatHasFloatingPointNotation(arg);

        return RubyString.newString(runtime, ( asEngineering ? engineeringValue(arg) : floatingPointValue(arg) ));
    }

    @Override
    public String toString() {
        if ( isNaN() ) return "NaN";
        if ( isInfinity() ) return infinityString(infinitySign);
        if ( isZero() ) return zeroSign < 0 ? "-0.0" : "0.0";

        return engineeringValue(null).toString();
    }

    @Deprecated
    public IRubyObject to_s(IRubyObject[] args) {
        return toStringImpl(getRuntime(), args.length == 0 ? null : (args[0].isNil() ? null : args[0].toString()));
    }

    // Note: #fix has only no-arg form, but truncate allows optional parameter.

    @JRubyMethod
    public IRubyObject fix() {
        return truncateInternal(getRuntime(), 0);
    }

    private RubyBigDecimal truncateInternal(final Ruby runtime, int arg) {
        if (isNaN()) return newNaN(runtime);
        if (isInfinity()) return newInfinity(runtime, infinitySign);

        int precision = value.precision() - value.scale() + arg;

        if (precision > 0) {
            return new RubyBigDecimal(runtime, value.round(new MathContext(precision, RoundingMode.DOWN)));
        }

        return newZero(runtime, this.zeroSign);
    }

    @JRubyMethod
    public IRubyObject truncate(ThreadContext context) {
        return truncateInternal(context.runtime, 0).to_int(context.runtime);
    }

    @JRubyMethod
    public IRubyObject truncate(ThreadContext context, IRubyObject arg) {
        return truncateInternal(context.runtime, RubyNumeric.fix2int(arg));
    }

    @Override
    @JRubyMethod(name = "zero?")
    public IRubyObject zero_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context, isZero());
    }

    @Deprecated
    public IRubyObject zero_p() {
        return getRuntime().newBoolean(isZero());
    }

    @Override
    public <T> T toJava(Class<T> target) {
        if (target == BigDecimal.class || target == Number.class) {
            return (T) value;
        }
        return super.toJava(target);
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

      final BigDecimal TWO = BigDecimal.valueOf(2);

      // Initial v - the reciprocal
      BigDecimal v = BigDecimal.ONE.divide(TWO.multiply(x), nMC);        // v0 = 1/(2*x)

      // Collect iteration precisions beforehand
      ArrayList<Integer> nPrecs = new ArrayList<Integer>();

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
        if (isInfinity()) throw getRuntime().newFloatDomainError(infinityString(infinitySign));
    }

    static String infinityString(final int infinitySign) {
        return infinitySign == -1 ? "-Infinity" : "Infinity";
    }

    private static boolean isEven(final RubyNumeric x) {
        if (x instanceof RubyFixnum) return (((RubyFixnum) x).getLongValue() & 1) == 0;
        if (x instanceof RubyBignum) {
            return ((RubyBignum) x).getBigIntegerValue().testBit(0) == false; // 0-th bit -> 0
        }
        return false;
    }

    @Deprecated
    public static IRubyObject ver(ThreadContext context, IRubyObject recv) {
        context.runtime.getWarnings().warn(IRubyWarnings.ID.DEPRECATED_METHOD, "BigDecimal.ver is deprecated; use BigDecimal::VERSION instead");
        return RubyString.newStringShared(context.runtime, VERSION);
    }

    @Deprecated // no longer used
    public RubyBigDecimal(Ruby runtime, RubyBigDecimal rbd) {
        this(runtime, runtime.getClass("BigDecimal"), rbd);
    }

    @Deprecated // no longer used
    public RubyBigDecimal(Ruby runtime, RubyClass klass, RubyBigDecimal rbd) {
        super(runtime, klass);
        this.isNaN = rbd.isNaN;
        this.infinitySign = rbd.infinitySign;
        this.zeroSign = rbd.zeroSign;
        this.value = rbd.value;
        this.flags |= FROZEN_F;
    }

    private static JavaSites.BigDecimalSites sites(ThreadContext context) {
        return context.sites.BigDecimal;
    }
}
