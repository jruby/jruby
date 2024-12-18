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

import org.jruby.api.Access;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Numeric;
import org.jruby.util.SafeDoubleParser;
import org.jruby.util.StringSupport;

import static org.jruby.api.Access.kernelModule;
import static org.jruby.api.Access.getModule;
import static org.jruby.api.Convert.*;
import static org.jruby.api.Create.*;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;
import static org.jruby.api.Warn.warningDeprecated;
import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubyBigDecimal extends RubyNumeric {

    @JRubyConstant
    public final static int ROUND_DOWN = 2;
    @JRubyConstant
    public final static int ROUND_CEILING = 5;
    @JRubyConstant
    public final static int ROUND_UP = 1;
    @JRubyConstant
    public final static int ROUND_HALF_DOWN = 4;
    @JRubyConstant
    public final static int ROUND_HALF_EVEN = 7;
    @JRubyConstant
    public final static int ROUND_HALF_UP = 3;
    @JRubyConstant
    public final static int ROUND_FLOOR = 6;

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

    private static final ByteList VERSION = ByteList.create("3.1.4");

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

    public static RubyClass createBigDecimal(ThreadContext context) {
        var runtime = context.runtime;
        RubyClass bigDecimal = defineClass(context, "BigDecimal", runtime.getNumeric(), NOT_ALLOCATABLE_ALLOCATOR).
                reifiedClass(RubyBigDecimal.class).
                defineMethods(context, RubyBigDecimal.class).
                defineConstants(context, RubyBigDecimal.class).
                defineConstant(context, "VERSION", RubyString.newStringShared(runtime, VERSION));

        kernelModule(context).defineMethods(context, BigDecimalKernelMethods.class);

        bigDecimal.setInternalModuleVariable("vpPrecLimit", asFixnum(context, 0));
        bigDecimal.setInternalModuleVariable("vpExceptionMode", asFixnum(context, 0));
        bigDecimal.setInternalModuleVariable("vpRoundingMode", asFixnum(context, ROUND_HALF_UP));

        //RubyModule bigMath = runtime.defineModule("BigMath");
        // NOTE: BigMath.exp and BigMath.pow should be implemented as native
        // for now @see jruby/bigdecimal.rb

        RubyBigDecimal POSITIVE_ZERO = new RubyBigDecimal(runtime, BigDecimal.ZERO, 0, 1);
        RubyBigDecimal NEGATIVE_ZERO = new RubyBigDecimal(runtime, BigDecimal.ZERO, 0, -1);
        RubyBigDecimal NAN = new RubyBigDecimal(runtime, BigDecimal.ZERO, true);
        RubyBigDecimal POSITIVE_INFINITY = new RubyBigDecimal(runtime, BigDecimal.ZERO, 1, 0);
        RubyBigDecimal NEGATIVE_INFINITY = new RubyBigDecimal(runtime, BigDecimal.ZERO, -1, 0);

        bigDecimal.defineConstant(context, "POSITIVE_ZERO", POSITIVE_ZERO, true).
                defineConstant(context, "NEGATIVE_ZERO", NEGATIVE_ZERO, true).
                defineConstant(context, "NAN", NAN).
                defineConstant(context, "INFINITY", POSITIVE_INFINITY).
                defineConstant(context, "NEGATIVE_INFINITY", NEGATIVE_INFINITY, true);

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
        super(runtime, Access.getClass(runtime.getCurrentContext(), "BigDecimal"));
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
        super(runtime, Access.getClass(runtime.getCurrentContext(), "BigDecimal"));
        this.isNaN = false;
        this.infinitySign = infinitySign;
        this.zeroSign = zeroSign;
        this.value = value;
        this.flags |= FROZEN_F;
    }

    public RubyBigDecimal(Ruby runtime, BigDecimal value, boolean isNan) {
        super(runtime, Access.getClass(runtime.getCurrentContext(), "BigDecimal"));
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
            return newBigDecimal(context, recv, arg, asFixnum(context, Integer.MAX_VALUE));
        }

        @JRubyMethod(name = "BigDecimal", module = true, visibility = Visibility.PRIVATE) // required = 1, optional = 1
        public static IRubyObject newBigDecimal(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
            RubyClass bigDecimal = Access.getClass(context, "BigDecimal");
            IRubyObject maybeOpts = ArgsUtil.getOptionsArg(context.runtime, arg1, false);

            if (maybeOpts.isNil()) return newInstance(context, bigDecimal, arg0, arg1, true, true);

            IRubyObject exObj = ArgsUtil.extractKeywordArg(context, "exception", maybeOpts);
            boolean exception = exObj.isNil() || exObj.isTrue();

            return newInstance(context, bigDecimal, arg0, asFixnum(context, Integer.MAX_VALUE), true, exception);
        }

        @JRubyMethod(name = "BigDecimal", module = true, visibility = Visibility.PRIVATE) // required = 1, optional = 1
        public static IRubyObject newBigDecimal(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, IRubyObject opts) {
            IRubyObject maybeOpts = ArgsUtil.getOptionsArg(context.runtime, opts, false);

            if (maybeOpts.isNil()) throw argumentError(context, 3, 1, 2);

            IRubyObject exObj = ArgsUtil.extractKeywordArg(context, "exception", maybeOpts);
            boolean exception = exObj.isNil() || exObj.isTrue();

            return newInstance(context, Access.getClass(context, "BigDecimal"), arg0, arg1, true, exception);
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
        long m = 0;
        // First get max prec
        for (int i = 0; i != precisionAndValue.length() && precisionAndValue.charAt(i) != ':'; i++) {
            if (!Character.isDigit(precisionAndValue.charAt(i))) {
                throw typeError(context, "load failed: invalid character in the marshaled string");
            }
            m = m * 10 + (precisionAndValue.charAt(i) - '0');
        }
        String value = precisionAndValue.substring(precisionAndValue.indexOf(':') + 1);
        return (RubyBigDecimal) newInstance(context, recv, newString(context, value), asFixnum(context, m), true, true);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject double_fig(ThreadContext context, IRubyObject recv) {
        return asFixnum(context, VP_DOUBLE_FIG);
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
        if (castAsFixnum(context, arg).getLongValue() < 0) throw argumentError(context, "argument must be positive");

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
        return newInstance(context, recv, str, asFixnum(context, Integer.MAX_VALUE), false, false );
    }

    private static IRubyObject modeExecute(final ThreadContext context, final RubyModule BigDecimal,
        final Block block, final String intVariableName) {
        IRubyObject current = BigDecimal.searchInternalModuleVariable(intVariableName);
        try {
            return block.yieldSpecific(context);
        } finally {
            BigDecimal.setInternalModuleVariable(intVariableName, current);
        }
    }

    @JRubyMethod(required = 1, optional = 1, checkArity = false, meta = true)
    public static IRubyObject mode(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        // FIXME: I doubt any of the constants referenced in this method
        // are ever redefined -- should compare to the known values, rather
        // than do an expensive constant lookup.
        RubyModule c = (RubyModule)recv;

        args = Arity.scanArgs(context, args, 1, 1);

        long mode = castAsFixnum(context, args[0]).getLongValue();
        IRubyObject value = args[1];

        if ((mode & EXCEPTION_ALL) != 0) {
            if (value.isNil()) return c.searchInternalModuleVariable("vpExceptionMode");
            if (!(value instanceof RubyBoolean)) throw argumentError(context, "second argument must be true or false");

            long newExceptionMode = c.searchInternalModuleVariable("vpExceptionMode").convertToInteger().getLongValue();

            boolean enable = value.isTrue();

            if ((mode & EXCEPTION_INFINITY) != 0) {
                newExceptionMode = enable ? newExceptionMode | EXCEPTION_INFINITY : newExceptionMode &  ~(EXCEPTION_INFINITY);
            }

            if ((mode & EXCEPTION_NaN) != 0) {
                newExceptionMode = enable ? newExceptionMode | EXCEPTION_NaN : newExceptionMode &  ~(EXCEPTION_NaN);
            }

            if ((mode & EXCEPTION_UNDERFLOW) != 0) {
                newExceptionMode = enable ? newExceptionMode | EXCEPTION_UNDERFLOW : newExceptionMode &  ~(EXCEPTION_UNDERFLOW);
            }

            if ((mode & EXCEPTION_ZERODIVIDE) != 0) {
                newExceptionMode = enable ? newExceptionMode | EXCEPTION_ZERODIVIDE : newExceptionMode &  ~(EXCEPTION_ZERODIVIDE);
            }

            RubyFixnum fixnumMode = asFixnum(context, newExceptionMode);
            c.setInternalModuleVariable("vpExceptionMode", fixnumMode);
            return fixnumMode;
        }

        if (mode == ROUND_MODE) {
            if (value == context.nil) return c.searchInternalModuleVariable("vpRoundingMode");

            RoundingMode javaRoundingMode = javaRoundingModeFromRubyRoundingMode(context, value);
            RubyFixnum roundingMode = asFixnum(context, rubyRoundingModeFromJavaRoundingMode(context, javaRoundingMode));
            c.setInternalModuleVariable("vpRoundingMode", roundingMode);

            return roundingMode;
        }

        throw typeError(context, "first argument for BigDecimal#mode invalid");
    }

    // The Fixnum cast should be fine because these are internal variables and user code cannot change them.
    private static long bigDecimalVar(ThreadContext context, String variableName) {
        return ((RubyFixnum) Access.getClass(context, "BigDecimal").searchInternalModuleVariable(variableName)).getLongValue();
    }

    private static RoundingMode getRoundingMode(ThreadContext context) {
        IRubyObject mode = Access.getClass(context, "BigDecimal").searchInternalModuleVariable("vpRoundingMode");
        return javaRoundingModeFromRubyRoundingMode(context, mode);
    }

    private static boolean isNaNExceptionMode(ThreadContext context) {
        return (bigDecimalVar(context, "vpExceptionMode") & EXCEPTION_NaN) != 0;
    }

    private static boolean isInfinityExceptionMode(ThreadContext context) {
        return (bigDecimalVar(context, "vpExceptionMode") & EXCEPTION_INFINITY) != 0;
    }

    private static boolean isOverflowExceptionMode(ThreadContext context) {
        return (bigDecimalVar(context, "vpExceptionMode") & EXCEPTION_OVERFLOW) != 0;
    }

    private static boolean isUnderflowExceptionMode(ThreadContext context) {
        return (bigDecimalVar(context, "vpExceptionMode") & EXCEPTION_UNDERFLOW) != 0;
    }

    private static boolean isZeroDivideExceptionMode(ThreadContext context) {
        return (bigDecimalVar(context, "vpExceptionMode") & EXCEPTION_ZERODIVIDE) != 0;
    }

    private static RubyBigDecimal cannotBeCoerced(ThreadContext context, IRubyObject value, boolean must) {
        if (must) throw typeError(context, errMessageType(context, value) + " can't be coerced into BigDecimal");
        return null;
    }

    private static String errMessageType(ThreadContext context, IRubyObject value) {
        if (value == null || value == context.nil) return "nil";
        return value.isImmediate() ? RubyObject.inspect(context, value).toString() : value.getMetaClass().getBaseName();
    }

    private static BigDecimal toBigDecimal(ThreadContext context, final RubyInteger value) {
        return value instanceof RubyFixnum ?
            BigDecimal.valueOf(numericToLong(context, value)) : new BigDecimal(value.getBigIntegerValue());
    }

    private static RubyBigDecimal getVpRubyObjectWithPrecInner(ThreadContext context, RubyRational value, RoundingMode mode) {
        BigDecimal numerator = toBigDecimal(context, value.getNumerator());
        BigDecimal denominator = toBigDecimal(context, value.getDenominator());

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

            MathContext mathContext = new MathContext(RubyFloat.DIG + 1, getRoundingMode(context));
            // uses value.toString to prevent a precision error
            // e.g. new BigDecimal(64.4) -> 64.400000000000005684341886080801486968994140625
            //      new BigDecimal("64.4") -> 64.4
            return new RubyBigDecimal(context.runtime, new BigDecimal(value.toString(), mathContext));
        }
        if (value instanceof RubyRational) {
            return div2Impl(context, ((RubyRational) value).getNumerator(), ((RubyRational) value).getDenominator(), getPrec(context) * BASE_FIG);
        }

        return getVpValue(context, value, must);
    }

    private static RubyBigDecimal getVpValue(ThreadContext context, IRubyObject value, boolean must) {
        return switch (((RubyBasicObject) value).getNativeClassIndex()) {
            case BIGDECIMAL -> (RubyBigDecimal) value;
            case FIXNUM -> newInstance(context, Access.getClass(context, "BigDecimal"), (RubyFixnum) value, MathContext.UNLIMITED);
            case BIGNUM -> newInstance(context, Access.getClass(context, "BigDecimal"), (RubyBignum) value, MathContext.UNLIMITED);
            case FLOAT -> newInstance(context, Access.getClass(context, "BigDecimal"), (RubyFloat) value, new MathContext(RubyFloat.DIG));
            case RATIONAL -> newInstance(context, Access.getClass(context, "BigDecimal"), (RubyRational) value, new MathContext(RubyFloat.DIG));
            default -> cannotBeCoerced(context, value, must);
        };
    }

    @JRubyMethod(meta = true)
    public static IRubyObject induced_from(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return getVpValue(context, arg, true);
    }

    private static RubyBigDecimal newInstance(Ruby runtime, IRubyObject recv, RubyBigDecimal arg) {
        return new RubyBigDecimal(runtime, (RubyClass) recv, arg.value, arg.zeroSign, arg.infinitySign, arg.isNaN);
    }

    private static RubyBigDecimal newInstance(ThreadContext context, IRubyObject recv, RubyFixnum arg, MathContext mathContext) {
        final long value = arg.getLongValue();
        if (value == 0) return getZero(context, 1);
        return new RubyBigDecimal(context.runtime, (RubyClass) recv, new BigDecimal(value, mathContext));
    }

    private static RubyBigDecimal newInstance(ThreadContext context, IRubyObject recv, RubyRational arg, MathContext mathContext) {
        if (arg.getNumerator().isZero(context)) return getZero(context, 1);

        BigDecimal num = toBigDecimal(context, arg.getNumerator());
        BigDecimal den = toBigDecimal(context, arg.getDenominator());
        BigDecimal value;
        try {
            value = num.divide(den, mathContext);
        } catch (ArithmeticException e){
            value = num.divide(den, MathContext.DECIMAL64);
        }

        return new RubyBigDecimal(context.runtime, (RubyClass) recv, value);
    }

    private static RubyBigDecimal newInstance(ThreadContext context, IRubyObject recv, RubyFloat arg, MathContext mathContext) {
        // precision can be no more than float digits
        if (mathContext.getPrecision() > RubyFloat.DIG + 1) throw argumentError(context, "precision too large");

        RubyBigDecimal res = newFloatSpecialCases(context, arg);
        if (res != null) return res;

        return new RubyBigDecimal(context.runtime, (RubyClass) recv, new BigDecimal(arg.toString(), mathContext));
    }

    private static RubyBigDecimal newFloatSpecialCases(ThreadContext context, RubyFloat val) {
        if (val.isNaN()) return getNaN(context);
        if (val.isInfinite()) return getInfinity(context, val.getDoubleValue() == Double.POSITIVE_INFINITY ? 1 : -1);
        if (val.isZero(context)) return getZero(context, Double.doubleToLongBits(val.getDoubleValue()) == NEGATIVE_ZERO_LONG_BITS ? -1 : 1);
        return null;
    }

    private static RubyBigDecimal newInstance(ThreadContext context, IRubyObject recv, RubyBignum arg, MathContext mathContext) {
        final BigInteger value = arg.getBigIntegerValue();
        if (value.equals(BigInteger.ZERO)) return getZero(context, 1);
        return new RubyBigDecimal(context.runtime, (RubyClass) recv, new BigDecimal(value, mathContext));
    }

    private static IRubyObject newInstance(ThreadContext context, RubyClass recv, RubyString arg, MathContext mathContext, boolean strict, boolean exception) {
        // Convert String to Java understandable format (for BigDecimal).

        char[] str = arg.decodeString().toCharArray();
        int s = 0; int e = str.length - 1;

        if (e == 0) {
            switch (str[0]) {
                case '0':
                    return getZero(context, 1);
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
                if ( contentEquals("NaN", str, s, e) ) return getNaN(context);
                break;
            case 'I' :
                if ( contentEquals("Infinity", str, s, e) ) return getInfinity(context, 1);
                break;
            case '-' :
                if ( contentEquals("-Infinity", str, s, e) ) return getInfinity(context, -1);
                sign = -1;
                break;
            case '+' :
                if ( contentEquals("+Infinity", str, s, e) ) return getInfinity(context, +1);
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
                    if (dotFound) {
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

            if (i < str.length && strict) throw invalidArgumentError(context, arg);
        }

        e -= off;

        if ( exp != -1 ) {
            if (exp == e || (exp + 1 == e && (str[exp + 1] == '-' || str[exp + 1] == '+'))) {
                if (!strict) {
                    // exponential loosely
                    if (exp == e) e--; // e.g. "1e".to_d
                    if (exp + 1 == e) e-=2; // e.g. "1e+".to_d
                } else {
                    throw invalidArgumentError(context, arg);
                }
            }
            else if (isExponentOutOfRange(str, exp + 1, e)) {
                // Handle infinity (Integer.MIN_VALUE + 1) < expValue < Integer.MAX_VALUE
                // checking the sign of exponent part.
                if (isZeroBase(str, s, exp) || str[exp + 1] == '-') return getZero(context, sign);
                return getInfinity(context, sign);
            }
        } else if ( lastSign > s ) {
            e = lastSign - 1; // ignored tail junk e.g. "5-6" -> "-6"
        }

        BigDecimal decimal;
        try {
            decimal = new BigDecimal(str, s, e - s + 1, mathContext);
        } catch (ArithmeticException ex) {
            return checkOverUnderFlow(context, ex, false, strict, exception);
        } catch (NumberFormatException ex) {
            return handleInvalidArgument(context, arg, strict, exception);
        }

        // MRI behavior: -0 and +0 are two different things
        if (decimal.signum() == 0) return getZero(context, sign);

        return new RubyBigDecimal(context.runtime, recv, decimal);
    }

    private static IRubyObject handleInvalidArgument(ThreadContext context, RubyString arg, boolean strict, boolean exception) {
        if (!strict) return getZero(context, 1);
        if (!exception) return context.nil;

        throw invalidArgumentError(context, arg);
    }

    private static RaiseException invalidArgumentError(ThreadContext context, RubyString arg) {
        return argumentError(context, "invalid value for BigDecimal(): \"" + arg + "\"");
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

    private static IRubyObject handleMissingPrecision(ThreadContext context, String name, boolean strict, boolean exception) {
        if (!strict) return getZero(context, 1);
        if (!exception) return context.nil;
        throw argumentError(context, "can't omit precision for a " + name + ".");
    }

    // Left for arjdbc (being phased out from 61.3 forward and newer points of newer arjdbc versions)
    @Deprecated
    public static RubyBigDecimal newInstance(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return (RubyBigDecimal) newInstance(context, recv, arg, true, true);
    }

    // Left for arjdbc (being phased out from 61.3 forward and newer points of newer arjdbc versions)
    @Deprecated
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject arg, boolean strict, boolean exception) {
        switch (((RubyBasicObject) arg).getNativeClassIndex()) {
            case RATIONAL:
                return handleMissingPrecision(context, "Rational", strict, exception);
            case FLOAT:
                RubyBigDecimal res = newFloatSpecialCases(context, (RubyFloat) arg);
                if (res != null) return res;
                return handleMissingPrecision(context, "Float", strict, exception);
            case FIXNUM:
                return newInstance(context, recv, (RubyFixnum) arg, MathContext.UNLIMITED);
            case BIGNUM:
                return newInstance(context, recv, (RubyBignum) arg, MathContext.UNLIMITED);
            case BIGDECIMAL:
                return arg;
            case COMPLEX:
                RubyComplex c = (RubyComplex) arg;
                if (!((RubyNumeric)c.image(context)).isZero(context)) {
                    throw argumentError(context, "Unable to make a BigDecimal from non-zero imaginary number");
                }
        }

        IRubyObject maybeString = arg.checkStringType();

        if (maybeString.isNil()) {
            if (!strict) return getZero(context, 1);
            if (!exception) return context.nil;
            throw typeError(context, "no implicit conversion of " + arg.inspect(context) + "into to String");
        }
        return newInstance(context, (RubyClass) recv, maybeString.convertToString(), MathContext.UNLIMITED, strict, exception);
    }

    public static RubyBigDecimal newInstance(ThreadContext context, IRubyObject recv, IRubyObject arg, IRubyObject mathArg) {
        return (RubyBigDecimal) newInstance(context, recv, arg, mathArg, true, true);
    }

    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject arg, IRubyObject mathArg, boolean strict, boolean exception) {
        if (arg.isNil() || arg instanceof RubyBoolean) {
            if (!exception) return context.nil;
            throw typeError(context, "can't convert " + arg.inspect(context) + " into BigDecimal");
        }

        int digits = (int) mathArg.convertToInteger().getLongValue();
        if (digits < 0) {
            if (!strict) return getZero(context, 1);
            if (!exception) return context.nil;
            throw argumentError(context, "argument must be positive");
        }

        MathContext mathContext = new MathContext(digits, getRoundingMode(context));

        switch (((RubyBasicObject) arg).getNativeClassIndex()) {
            case RATIONAL:
                // mri uses SIZE_MAX
                if (digits == Integer.MAX_VALUE) {
                    return handleMissingPrecision(context, "Rational", strict, exception);
                }
                return newInstance(context, recv, (RubyRational) arg, mathContext);
            case FLOAT:
                RubyBigDecimal res = newFloatSpecialCases(context, (RubyFloat) arg);
                if (res != null) return res;
                // mri uses SIZE_MAX
                if (digits == Integer.MAX_VALUE) {
                    return handleMissingPrecision(context, "Float", strict, exception);
                }
                return newInstance(context, recv, (RubyFloat) arg, mathContext);
            case FIXNUM:
                return newInstance(context, recv, (RubyFixnum) arg, mathContext);
            case BIGNUM:
                return newInstance(context, recv, (RubyBignum) arg, mathContext);
            case BIGDECIMAL:
                // mri uses SIZE_MAX
                if (digits == Integer.MAX_VALUE) {
                    return arg;
                }
                return newInstance(context.runtime, recv, (RubyBigDecimal) arg);
            case COMPLEX:
                RubyComplex c = (RubyComplex) arg;
                if (!((RubyNumeric)c.image(context)).isZero(context)) {
                    throw argumentError(context, "Unable to make a BigDecimal from non-zero imaginary number");
                }
                return newInstance(context, recv, c.real(context), mathArg, strict, exception);
        }

        IRubyObject maybeString = arg.checkStringType();

        if (maybeString.isNil()) {
            if (!exception) return context.nil;
            throw typeError(context, "can't convert " + arg.getMetaClass() + " into BigDecimal");
        }

        return newInstance(context, (RubyClass) recv, (RubyString) maybeString, MathContext.UNLIMITED, strict, exception);
    }

    private static RubyBigDecimal getZero(ThreadContext context, final int sign) {
        String constantName = sign < 0 ? "NEGATIVE_ZERO" : "POSITIVE_ZERO";
        return (RubyBigDecimal) Access.getClass(context, "BigDecimal").getConstant(constantName);
    }

    private static RubyBigDecimal getNaN(ThreadContext context) {
        if ( isNaNExceptionMode(context) ) throw newNaNFloatDomainError(context);

        return (RubyBigDecimal) Access.getClass(context, "BigDecimal").getConstant("NAN");
    }

    private static RaiseException newNaNFloatDomainError(ThreadContext context) {
        return context.runtime.newFloatDomainError("Computation results to 'NaN'(Not a Number)");
    }

    private enum InfinityErrorMsgType {To, In}

    private static RubyBigDecimal getInfinity(ThreadContext context, final int sign) {
        return getInfinity(context, sign, InfinityErrorMsgType.To);
    }

    private static RubyBigDecimal getInfinity(ThreadContext context, final int sign, final InfinityErrorMsgType type) {
        if (isInfinityExceptionMode(context)) throw newInfinityFloatDomainError(context, sign, type);

        String constantName = sign < 0 ? "NEGATIVE_INFINITY" : "INFINITY";
        return (RubyBigDecimal) Access.getClass(context, "BigDecimal").getConstant(constantName);
    }

    private static RaiseException newInfinityFloatDomainError(ThreadContext context, final int sign) {
        return newInfinityFloatDomainError(context, sign, InfinityErrorMsgType.To);
    }

    private static RaiseException newInfinityFloatDomainError(ThreadContext context, final int sign, final InfinityErrorMsgType type) {
        if (type == InfinityErrorMsgType.To) {
            return context.runtime.newFloatDomainError("Computation results to " + (sign < 0 ? "'-Infinity'" : "'Infinity'"));
        } else {
            return context.runtime.newFloatDomainError("Computation results in " + (sign < 0 ? "'-Infinity'" : "'Infinity'"));
        }
    }

    private RubyBigDecimal setResult(ThreadContext context) {
        return setResult(context, 0);
    }

    private RubyBigDecimal setResult(ThreadContext context, int prec) {
        if (prec == 0) prec = getPrecLimit(context);
        int exponent;
        if (prec > 0 && this.value.scale() > (prec - (exponent = getExponent(context)))) {
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

    @JRubyMethod
    public RubyFixnum hash(ThreadContext context) {
        return asFixnum(context, absStripTrailingZeros().hashCode() * value.signum());
    }

    @JRubyMethod(name = "initialize_copy", visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject original) {
        if (this == original) return this;

        checkFrozen();

        if (original instanceof RubyBigDecimal orig) {
            this.isNaN = orig.isNaN;
            this.infinitySign = orig.infinitySign;
            this.zeroSign = orig.zeroSign;
            this.value = orig.value;

            return this;
        }

        throw typeError(context, "wrong argument class");
    }

    @JRubyMethod(name = {"%", "modulo"})
    public IRubyObject op_mod(ThreadContext context, IRubyObject other) {
        RubyBigDecimal val = getVpValueWithPrec(context, other, false);

        if (val == null) return callCoerced(context, sites(context).op_mod, other, true);
        if (isNaN() || val.isNaN() || isInfinity() && val.isInfinity()) return getNaN(context);
        if (val.isZero(context)) throw context.runtime.newZeroDivisionError();
        if (isInfinity()) return getNaN(context);
        if (val.isInfinity()) return this;
        if (isZero(context)) return getZero(context, value.signum());

        // Java and MRI definitions of modulo are different.
        BigDecimal modulo = value.remainder(val.value);
        if (modulo.signum() * val.value.signum() < 0) modulo = modulo.add(val.value);

        return new RubyBigDecimal(context.runtime, modulo).setResult(context);
    }

    @Override
    @JRubyMethod(name = "remainder")
    public IRubyObject remainder(ThreadContext context, IRubyObject arg) {
        return remainderInternal(context, getVpValueWithPrec(context, arg, false), arg);
    }

    private IRubyObject remainderInternal(ThreadContext context, RubyBigDecimal val, IRubyObject arg) {
        if (isInfinity() || isNaN()) return getNaN(context);
        if (val == null) return callCoerced(context, sites(context).remainder, arg, true);
        if (val.isInfinity()) return this;
        if (val.isNaN() || val.isZero(context)) return getNaN(context);

        // Java and MRI definitions of remainder are the same.
        return new RubyBigDecimal(context.runtime, value.remainder(val.value)).setResult(context);
    }

    @JRubyMethod(name = "*")
    public IRubyObject op_mul(ThreadContext context, IRubyObject arg) {
        RubyBigDecimal val = getVpValueWithPrec(context, arg, false);
        if (val == null) return callCoerced(context, sites(context).op_times, arg, true);
        return multImpl(context, val);
    }

    @JRubyMethod(name = "mult")
    public IRubyObject mult2(ThreadContext context, IRubyObject b, IRubyObject n) {
        final int mx = getPrecisionInt(context, n);
        if (mx == 0) return op_mul(context, b);

        RubyBigDecimal val = getVpValueWithPrec(context, b, false);
        if (val == null) { // TODO: what about n arg?
            return callCoerced(context, sites(context).op_times, b, true);
        }

        return multImpl(context, val).setResult(context, mx);
    }

    private RubyBigDecimal multImpl(ThreadContext context, RubyBigDecimal val) {
        if ( isNaN() || val.isNaN() ) return getNaN(context);

        var isZero = isZero(context);
        if (isZero || val.isZero(context)) {
            if ((isInfinity() && val.isZero(context)) || (isZero && val.isInfinity())) return getNaN(context);

            int sign1 = isZero ? zeroSign : value.signum();
            int sign2 = val.isZero(context) ?  val.zeroSign : val.value.signum();
            return getZero(context, sign1 * sign2);
        }

        if ( isInfinity() || val.isInfinity() ) {
            int sign1 = isInfinity() ? infinitySign : value.signum();
            int sign2 = val.isInfinity() ? val.infinitySign : val.value.signum();
            return getInfinity(context, sign1 * sign2);
        }

        int mx = value.precision() + val.value.precision();

        MathContext mathContext = new MathContext(mx, getRoundingMode(context));
        BigDecimal result;
        try {
            result = value.multiply(val.value, mathContext);
        }
        catch (ArithmeticException ex) {
            return (RubyBigDecimal) checkOverUnderFlow(context, ex, false, true, true);
        }
        return new RubyBigDecimal(context.runtime, result).setResult(context);
    }

    private static IRubyObject checkOverUnderFlow(ThreadContext context, final ArithmeticException ex,
                                                  boolean nullDefault, boolean strict, boolean exception) {
        String message = ex.getMessage();
        if (message == null) message = "";
        message = message.toLowerCase(Locale.ENGLISH);
        if (message.contains("underflow")) {
            return isUnderflowExceptionMode(context) ?
                    handleFloatDomainError(context, message, strict, exception) :
                    getZero(context, 1);
        }
        if (message.contains("overflow")) {
            return isOverflowExceptionMode(context) ?
                    handleFloatDomainError(context, message, strict, exception) :
                    getInfinity(context, 1); // TODO sign?
        }

        return nullDefault ? null : handleFloatDomainError(context, message, strict, exception);
    }

    private static IRubyObject handleFloatDomainError(ThreadContext context, String message, boolean strict, boolean exception) {
        if (!strict) return getZero(context, 1);
        if (!exception) return context.nil;
        throw context.runtime.newFloatDomainError(message);
    }

    // Calculate appropriate zero or infinity depending on exponent...
    private RubyBigDecimal newPowOfInfinity(ThreadContext context, RubyNumeric exp) {
        if (Numeric.f_negative_p(context, exp)) {
            if (infinitySign >= 0) return getZero(context, 0);

            // (-Infinity) ** (-even_integer) -> +0 AND (-Infinity) ** (-odd_integer) -> -0
            if (Numeric.f_integer_p(context, exp)) return getZero(context, isEven(exp) ? 1 : -1);

            return getZero(context, -1); // (-Infinity) ** (-non_integer) -> -0
        }

        if (infinitySign >= 0) return getInfinity(context, 1);

        if (Numeric.f_integer_p(context, exp)) return getInfinity(context, isEven(exp) ? 1 : -1);

        throw context.runtime.newMathDomainError("a non-integral exponent for a negative base");
    }

    // Calculate appropriate zero or infinity depending on exponent
    private RubyBigDecimal newPowOfZero(ThreadContext context, RubyNumeric exp) {
        if (Numeric.f_negative_p(context, exp)) {
            /* (+0) ** (-num)  -> Infinity */
            if (zeroSign >= 0) return getInfinity(context, 1);

            // (-0) ** (-even_integer) -> +Infinity  AND (-0) ** (-odd_integer) -> -Infinity
            if (Numeric.f_integer_p(context, exp)) return getInfinity(context, isEven(exp) ? 1 : -1);

            return getInfinity(context, -1); // (-0) ** (-non_integer) -> Infinity
        }

        return Numeric.f_zero_p(context, exp) ?
                new RubyBigDecimal(context.runtime, BigDecimal.ONE) :
                getZero(context, 1);
    }

    private static IRubyObject vpPrecLimit(ThreadContext context) {
        return Access.getClass(context, "BigDecimal").searchInternalModuleVariable("vpPrecLimit");
    }

    private static int getPrecLimit(ThreadContext context) {
        return RubyNumeric.fix2int(vpPrecLimit(context));
    }

    @JRubyMethod(name = "**")
    public IRubyObject op_pow(ThreadContext context, IRubyObject exp) {
        return op_power(context, exp, context.nil);
    }

    @JRubyMethod(name = "power")
    public IRubyObject op_power(ThreadContext context, IRubyObject exp) {
        return op_power(context, exp, context.nil);
    }

    @JRubyMethod(name = "power")
    public IRubyObject op_power(ThreadContext context, IRubyObject exp, IRubyObject prec) {
        if (isNaN()) return getNaN(context);
        if (!(exp instanceof RubyNumeric)) throw typeError(context, exp, "scalar Numeric");

        if (exp instanceof RubyBignum || exp instanceof RubyFixnum) {
        } else if (exp instanceof RubyFloat floatExp) {
            double d = floatExp.getValue();
            if (d == Math.round(d)) {
                exp = RubyNumeric.fixable(context.runtime, d) ?
                        asFixnum(context, (long) d) : RubyBignum.newBignorm(context.runtime, d);
            }
        } else if (exp instanceof RubyRational) {
            if (Numeric.f_zero_p(context, Numeric.f_numerator(context, exp))) {

            } else if (Numeric.f_one_p(context, Numeric.f_denominator(context, exp))) {
                exp = Numeric.f_numerator(context, exp);
            }
        } else if (exp instanceof RubyBigDecimal bigExp) {
            IRubyObject zero = RubyNumeric.int2fix(context.runtime, 0);
            IRubyObject rounded = bigExp.round(context, new IRubyObject[]{zero});
            if (bigExp.eql_p(context, rounded).isTrue()) {
                exp = bigExp.to_int();
            }
        }

        if (isZero(context)) return newPowOfZero(context, (RubyNumeric) exp);

        if (Numeric.f_zero_p(context, exp)) return new RubyBigDecimal(context.runtime, BigDecimal.ONE);

        if (isInfinity()) return newPowOfInfinity(context, (RubyNumeric) exp);

        final int times;
        final double rem; // exp's decimal part
        final int nx = prec.isNil() ? getPrec(context) * BASE_FIG : num2int(prec);
        if (exp instanceof RubyBigDecimal) {
            RubyBigDecimal bdExp = (RubyBigDecimal)exp;
            int ny = bdExp.getPrec(context) * BASE_FIG;
            return bigdecimal_power_by_bigdecimal(context, exp, prec.isNil() ? nx + ny : nx);
        } else if ( exp instanceof RubyFloat ) {
            int ny = VP_DOUBLE_FIG;
            return bigdecimal_power_by_bigdecimal(context, exp, prec.isNil() ? nx + ny : nx);
        } else if ( exp instanceof RubyRational) {
            int ny = nx;
            return bigdecimal_power_by_bigdecimal(context, exp, prec.isNil() ? nx + ny : nx);
        } else if (exp instanceof RubyBignum) {
            BigDecimal absValue = value.abs();
            if (absValue.equals(BigDecimal.ONE)) {
                return new RubyBigDecimal(context.runtime, BigDecimal.ONE, 0, 1) ;
            } else if (absValue.compareTo(BigDecimal.ONE) == -1) {
                if (Numeric.f_negative_p(context, exp)) {
                    return getInfinity(context, (isEven((RubyBignum)exp) ? 1 : -1 ) * value.signum());
                } else if (Numeric.f_negative_p(context, this) && isEven((RubyBignum)exp)) {
                    return getZero(context, -1);
                } else {
                    return getZero(context, 1);
                }
            } else {
                if (!Numeric.f_negative_p(context, exp)) {
                    return getInfinity(context, (isEven((RubyBignum)exp) ? 1 : -1 ) * value.signum());
                } else if(value.signum() == -1 && isEven((RubyBignum)exp)) {
                    return getZero(context, -1);
                } else {
                    return getZero(context, 1);
                }
            }
        } else if ( ! ( exp instanceof RubyInteger ) ) {
            // when pow is not an integer we're play the oldest trick :
            // X pow (T+R) = X pow T * X pow R
            BigDecimal expVal = BigDecimal.valueOf( ((RubyNumeric) exp).getDoubleValue() );
            BigDecimal[] divAndRem = expVal.divideAndRemainder(BigDecimal.ONE);
            times = divAndRem[0].intValueExact(); rem = divAndRem[1].doubleValue();
        } else {
            times = RubyNumeric.fix2int(exp); rem = 0;
        }

        BigDecimal pow;
        if ( times < 0 ) {
            if (isZero(context)) return getInfinity(context, value.signum());
            pow = powNegative(context, times);
        } else {
            pow = value.pow(times);
        }

        if ( rem > 0 ) {
            // TODO of course this assumes we fit into double (and we loose some precision)
            double remPow = Math.pow(value.doubleValue(), rem);
            pow = pow.multiply( BigDecimal.valueOf(remPow) );
        }

        if (!prec.isNil()) pow = pow.setScale(nx, getRoundingMode(context));

        return new RubyBigDecimal(context.runtime, pow);
    }

    // mri bigdecimal_power_by_bigdecimal
    private IRubyObject bigdecimal_power_by_bigdecimal(ThreadContext context, IRubyObject exp, int precision) {
        RubyModule bigMath = getModule(context, "BigMath");
        RubyBigDecimal log_x = (RubyBigDecimal) bigMath.callMethod(context, "log", new IRubyObject[]{this, asFixnum(context, precision + 1)});
        RubyBigDecimal multipled = (RubyBigDecimal) log_x.mult2(context, exp, asFixnum(context, precision + 1));
        return bigMath.callMethod(context, "exp", new IRubyObject[]{multipled, asFixnum(context, precision)});
    }

    private BigDecimal powNegative(ThreadContext context, final int times) {
        // Note: MRI has a very non-trivial way of calculating the precision,
        // so we use approximation here :
        int mp = getPrec(context) * (BASE_FIG + 1) * (-times + 1);
        int precision = (mp + 8) / BASE_FIG * BASE_FIG;
        BigDecimal pow = value.pow(times, new MathContext(precision * 2, RoundingMode.DOWN));
        // calculates exponent of pow
        BigDecimal tmp = pow.abs().stripTrailingZeros();
        int exponent = tmp.precision() - tmp.scale();
        // adjusts precision using exponent. it seems like to match MRI behavior.
        if (exponent < 0) {
            precision += Math.abs(exponent) / BASE_FIG * BASE_FIG;
        } else {
            precision -= (exponent + 8) / BASE_FIG * BASE_FIG;
        }

        return pow.setScale(precision, RoundingMode.DOWN);
    }

    @JRubyMethod(name = "+")
    public IRubyObject op_plus(ThreadContext context, IRubyObject b) {
        return addInternal(context, getVpValueWithPrec(context, b, false), b, vpPrecLimit(context));
    }

    @JRubyMethod(name = "add")
    public IRubyObject add2(ThreadContext context, IRubyObject b, IRubyObject digits) {
        return addInternal(context, getVpValueWithPrec(context, b, false), b, digits);
    }

    private IRubyObject addInternal(ThreadContext context, RubyBigDecimal val, IRubyObject b, IRubyObject digits) {
        int prec = getPositiveInt(context, digits);
        if (val == null) {
            // TODO:
            // MRI behavior: Call "+" or "add", depending on the call.
            // But this leads to exceptions when Floats are added. See:
            // http://blade.nagaokaut.ac.jp/cgi-bin/scat.rb/ruby/ruby-core/17374
            // return callCoerced(context, op, b, true); -- this is MRI behavior.
            // We'll use ours for now, thus providing an ability to add Floats.
            RubyBigDecimal ret = (RubyBigDecimal) callCoerced(context, sites(context).op_plus, b, true);
            if (ret != null) {
                ret.setResult(context, prec);
            }
            return ret;
        }

        RubyBigDecimal res = addSpecialCases(context, val);
        if ( res != null ) return res;
        MathContext mathContext = new MathContext(prec, getRoundingMode(context));
        return new RubyBigDecimal(context.runtime, value.add(val.value, mathContext)).setResult(context, prec);
    }

    private static int getPositiveInt(ThreadContext context, IRubyObject arg) {
        int value = castAsFixnum(context, arg).getIntValue();
        if (value < 0) throw argumentError(context, "argument must be positive");
        return value;
    }

    private RubyBigDecimal addSpecialCases(ThreadContext context, RubyBigDecimal val) {
        if (isNaN() || val.isNaN) return getNaN(context);

        if (isZero(context)) {
            if (val.isZero(context)) return getZero(context, zeroSign == val.zeroSign ? zeroSign : 1);
            if (val.isInfinity()) return getInfinity(context, val.infinitySign);
            return new RubyBigDecimal(context.runtime, val.value);
        }

        int sign = infinitySign * val.infinitySign;

        if (sign > 0) return getInfinity(context, infinitySign);
        if (sign < 0) return getNaN(context);
        if (sign == 0) {
            sign = infinitySign + val.infinitySign;
            if (sign != 0) return getInfinity(context, sign);
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
        if (isNaN()) return getNaN(context);
        if (isInfinity()) return getInfinity(context, -infinitySign);
        if (isZero(context)) return getZero(context, -zeroSign);

        return new RubyBigDecimal(context.runtime, value.negate());
    }

    @JRubyMethod(name = "-")
    public IRubyObject op_minus(ThreadContext context, IRubyObject b) {
        return subInternal(context, getVpValueWithPrec(context, b, false), b, 0);
    }

    @JRubyMethod(name = "sub")
    public IRubyObject sub2(ThreadContext context, IRubyObject b, IRubyObject n) {
        return subInternal(context, getVpValueWithPrec(context, b, false), b, getPositiveInt(context, n));
    }

    private IRubyObject subInternal(ThreadContext context, RubyBigDecimal val, IRubyObject b, int prec) {
        if (val == null) return ((RubyBigDecimal)callCoerced(context, sites(context).op_minus, b, true)).setResult(context, prec);
        RubyBigDecimal res = subSpecialCases(context, val);
        return res != null ? res : new RubyBigDecimal(context.runtime, value.subtract(val.value)).setResult(context, prec);
    }

    private RubyBigDecimal subSpecialCases(ThreadContext context, RubyBigDecimal val) {
        if (isNaN() || val.isNaN()) return getNaN(context);

        if (isZero(context)) {
            if (val.isZero(context)) return getZero(context, zeroSign * val.zeroSign);
            if (val.isInfinity()) return getInfinity(context, val.infinitySign * -1);
            return new RubyBigDecimal(context.runtime, val.value.negate());
        }

        int sign = infinitySign * val.infinitySign;

        if (sign > 0) return getNaN(context);
        if (sign < 0) return getInfinity(context, infinitySign);
        if (sign == 0) {
            if (isInfinity()) return this;
            if (val.isInfinity()) return getInfinity(context, val.infinitySign * -1);
            sign = infinitySign + val.infinitySign;
            if (sign != 0) return getInfinity(context, sign);
        }
        return null;
    }

    // mri : BigDecimal_div
    @JRubyMethod(name = "/")
    public IRubyObject op_divide(ThreadContext context, IRubyObject other) {
        RubyBigDecimal val = getVpValueWithPrec(context, other, false);
        if (val == null) return callCoerced(context, sites(context).op_quo, other, true);

        if (isNaN() || val.isNaN()) return getNaN(context);

        RubyBigDecimal div = divSpecialCases(context, val);
        if (div != null) return div;

        return quoImpl(context, val);
    }

    // mri : BigDecimal_quo
    @JRubyMethod(name = "quo")
    public IRubyObject op_quo(ThreadContext context, IRubyObject other) {
        return op_divide(context, other);
    }

     // mri : BigDecimal_quo
    @JRubyMethod(name = "quo")
    public IRubyObject op_quo(ThreadContext context, IRubyObject object, IRubyObject digits) {
        int n = num2int(digits);
        if (n > 0) {
            return op_div(context, object, digits);
        } else {
            return op_divide(context, object);
        }
    }

    private RubyBigDecimal quoImpl(ThreadContext context, RubyBigDecimal that) {
        int mx = this.getPrecisionScale()[0];
        int mxb = that.getPrecisionScale()[0];
        if (mx < mxb) mx = mxb;
        mx *= 2;
        if (VP_DOUBLE_FIG * 2> mx) {
            mx = VP_DOUBLE_FIG * 2;
        }

        final int limit = getPrecLimit(context);
        if (limit > 0 && limit < mx) mx = limit;
        mx = (mx + 8) / BASE_FIG * BASE_FIG;

        RoundingMode mode = getRoundingMode(context);
        MathContext mathContext = new MathContext(mx * 2, mode);
        BigDecimal ret = divide(this.value, that.value, mathContext).setScale(mx, mode);
        return new RubyBigDecimal(context.runtime, ret).setResult(context, limit);
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

        if (thiz.isNaN() || that.isNaN()) return getNaN(context);

        RubyBigDecimal div = thiz.divSpecialCases(context, that);
        if (div != null) return div;

        int mx = thiz.value.precision() + that.value.precision() + 2;

        MathContext mathContext = new MathContext((mx * 2 + 2) * BASE_FIG, getRoundingMode(context));
        return new RubyBigDecimal(context.runtime, thiz.value.divide(that.value, mathContext)).setResult(context, ix);
    }

    // mri : BigDecimal_div3
    @JRubyMethod(name = "div")
    public IRubyObject op_div(ThreadContext context, IRubyObject other) {
        return op_div(context, other, context.nil);
    }

    private RubyInteger idiv(ThreadContext context, RubyRational val) {
        if (isNaN()) throw newNaNFloatDomainError(context);
        if (isInfinity()) { // NOTE: MRI is inconsistent with div(other, d) impl
            throw newInfinityFloatDomainError(context, infinitySign);
        }
        if (val.isZero(context)) throw context.runtime.newZeroDivisionError();

        BigDecimal result = this.value.multiply(toBigDecimal(context, val.getDenominator()))
                                      .divideToIntegralValue(toBigDecimal(context, val.getNumerator()));
        return toInteger(context, result);
    }

    private static final BigDecimal MAX_FIX = BigDecimal.valueOf(RubyFixnum.MAX);
    private static final BigDecimal MIN_FIX = BigDecimal.valueOf(RubyFixnum.MIN);

    private static RubyInteger toInteger(ThreadContext context, final BigDecimal result) {
        return result.compareTo(MAX_FIX) <= 0 && result.compareTo(MIN_FIX) >= 0 ?
                asFixnum(context, result.longValue()) :
                RubyBignum.newBignum(context.runtime, result.toBigInteger());
    }

    // mri : BigDecimal_div2
    @JRubyMethod(name = "div")
    public IRubyObject op_div(ThreadContext context, IRubyObject other, IRubyObject digits) {
        RubyBigDecimal val = getVpValue(context, other, false);
        if (digits.isNil()) {
            if (val == null) return callCoerced(context, sites(context).div, other, true);

            if (isNaN() || val.isNaN()) throw newNaNFloatDomainError(context);
            if (isInfinity()) { // NOTE: MRI is inconsistent with div(other, d) impl
                if (val.isInfinity()) throw newNaNFloatDomainError(context);
                throw newInfinityFloatDomainError(context, infinitySign);
            }
            if (val.isZero(context)) throw context.runtime.newZeroDivisionError();

            if (val.isInfinity()) return RubyFixnum.zero(context.runtime);
            return toInteger(context, this.value.divideToIntegralValue(val.value));
        }

        final int scale = RubyNumeric.fix2int(digits);

        // MRI behavior: "If digits is 0, the result is the same as the / operator."
        if (scale == 0) return op_divide(context, other);

        val = getVpValue(context, other, true);
        if (isNaN() || val.isNaN()) return getNaN(context);

        RubyBigDecimal div = divSpecialCases(context, val);
        if (div != null) return div;

        MathContext mathContext = new MathContext(scale, getRoundingMode(context));
        return new RubyBigDecimal(context.runtime, value.divide(val.value, mathContext)).setResult(context, scale);
    }

    private RubyBigDecimal divSpecialCases(ThreadContext context, RubyBigDecimal val) {
        if (isInfinity()) {
            return val.isInfinity() ? getNaN(context) : getInfinity(context, infinitySign * val.value.signum());
        }
        if (val.isInfinity()) return getZero(context, value.signum() * val.infinitySign);

        if (val.isZero(context)) {
            if (isZero(context)) return getNaN(context);
            if (isZeroDivideExceptionMode(context)) throw context.runtime.newFloatDomainError("Divide by zero");

            int sign1 = isInfinity() ? infinitySign : value.signum();
            return getInfinity(context, sign1 * val.zeroSign, InfinityErrorMsgType.In);
        }
        if (isZero(context)) return getZero(context, zeroSign * val.value.signum());

        return null;
    }

    private IRubyObject cmp(ThreadContext context, final IRubyObject arg, final char op) {
        final int e;
        RubyBigDecimal rb = arg instanceof RubyRational ?
                getVpValueWithPrec(context, arg, false) : getVpValue(context, arg, false);

        if (rb == null) {
            String id = "!=";
            switch (op) {
                case '*':
                    if (falsyEqlCheck(context, arg)) return context.nil;
                    return callCoerced(context, sites(context).op_cmp, arg, false);
                case '=': {
                    if (falsyEqlCheck(context, arg)) return context.fals;
                    IRubyObject res = callCoerced(context, sites(context).op_eql, arg, false);
                    return asBoolean(context, res != context.nil && res != context.fals);
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
                throw argumentError(context, "comparison of BigDecimal with "+ errMessageType(context, arg) +" failed");
            }
            return cmp;
        }

        if (isNaN() || rb.isNaN()) return (op == '*') ? context.nil : context.fals;
        e = infinitySign != 0 || rb.infinitySign != 0 ? infinitySign - rb.infinitySign : value.compareTo(rb.value);

        switch (op) {
            case '*': return asFixnum(context, e);
            case '=': return asBoolean(context, e == 0);
            case '!': return asBoolean(context, e != 0);
            case 'G': return asBoolean(context, e >= 0);
            case '>': return asBoolean(context, e >  0);
            case 'L': return asBoolean(context, e <= 0);
            case '<': return asBoolean(context, e <  0);
        }
        return context.nil;
    }

    // NOTE: otherwise `BD == nil` etc gets ___reeeaaally___ slow (due exception throwing)
    private static boolean falsyEqlCheck(final ThreadContext context, final IRubyObject arg) {
        return arg == context.nil || arg == context.fals || arg == context.tru;
    }

    @Override
    @JRubyMethod(name = "<=>")
    public IRubyObject op_cmp(ThreadContext context, IRubyObject arg) {
        return cmp(context, arg, '*');
    }

    // NOTE: do not use BigDecimal#equals since ZERO.equals(new BD('0.0')) -> false
    @Override
    @JRubyMethod(name = {"eql?", "=="})
    public IRubyObject eql_p(ThreadContext context, IRubyObject arg) {
        return cmp(context, arg, '=');
    }

    @Override
    @JRubyMethod(name = "===") // same as == (eql?)
    public IRubyObject op_eqq(ThreadContext context, IRubyObject arg) {
        return cmp(context, arg, '=');
    }

    @JRubyMethod(name = "<")
    public IRubyObject op_lt(ThreadContext context, IRubyObject arg) {
        return cmp(context, arg, '<');
    }

    @JRubyMethod(name = "<=")
    public IRubyObject op_le(ThreadContext context, IRubyObject arg) {
        return cmp(context, arg, 'L');
    }

    @JRubyMethod(name = ">")
    public IRubyObject op_gt(ThreadContext context, IRubyObject arg) {
        return cmp(context, arg, '>');
    }

    @JRubyMethod(name = ">=")
    public IRubyObject op_ge(ThreadContext context, IRubyObject arg) {
        return cmp(context, arg, 'G');
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject abs() {
        return abs(getCurrentContext());
    }

    @JRubyMethod
    public IRubyObject abs(ThreadContext context) {
        if (isNaN()) return getNaN(context);
        if (isInfinity()) return getInfinity(context, 1);

        return new RubyBigDecimal(context.runtime, value.abs()).setResult(context);
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

    @JRubyMethod
    public RubyArray coerce(ThreadContext context, IRubyObject other) {
        return newArray(context, getVpValue(context, other, true), this);
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

        if (isNaN() || val.isNaN() || isInfinity() && val.isInfinity()) {
            return newArray(context, getNaN(context), getNaN(context));
        }
        if (val.isZero(context)) throw runtime.newZeroDivisionError();
        if (isInfinity()) {
            int sign = (infinitySign == val.value.signum()) ? 1 : -1;
            return newArray(context, getInfinity(context, sign), getNaN(context));
        }
        if (val.isInfinity()) return newArray(context, getZero(context, val.value.signum()), this);
        if (isZero(context)) return newArray(context, getZero(context, value.signum()), getZero(context, value.signum()));

        // Java and MRI definitions of divmod are different.
        BigDecimal[] divmod = value.divideAndRemainder(val.value);

        BigDecimal div = divmod[0];
        BigDecimal mod = divmod[1];

        if (mod.signum() * val.value.signum() < 0) {
            div = div.subtract(BigDecimal.ONE);
            mod = mod.add(val.value);
        }

        return newArray(context, new RubyBigDecimal(runtime, div), new RubyBigDecimal(runtime, mod));
    }

    @Deprecated
    public IRubyObject exponent() {
        return exponent(getCurrentContext());
    }

    @JRubyMethod
    public IRubyObject exponent(ThreadContext context) {
        return asFixnum(context, getExponent(context));
    }

    @JRubyMethod(name = "finite?")
    public IRubyObject finite_p(ThreadContext context) {
        return asBoolean(context, !isNaN() && !isInfinity());
    }

    @Deprecated
    public IRubyObject finite_p() {
        return finite_p(getCurrentContext());
    }

    private RubyBigDecimal floorNaNInfinityCheck(ThreadContext context) {
        if (isNaN()) throw newNaNFloatDomainError(context);
        if (isInfinity()) throw newInfinityFloatDomainError(context, infinitySign);
        return null;
    }

    private RubyBigDecimal floorImpl(ThreadContext context, int n) {
        return value.scale() > n ? new RubyBigDecimal(context.runtime, value.setScale(n, RoundingMode.FLOOR)) : this;
    }

    @JRubyMethod
    public IRubyObject floor(ThreadContext context) {
        RubyBigDecimal res = floorNaNInfinityCheck(context);
        return res != null ? res : floorImpl(context, 0).to_int(context);
    }

    @JRubyMethod
    public IRubyObject floor(ThreadContext context, IRubyObject arg) {
        RubyBigDecimal res = floorNaNInfinityCheck(context);
        return res != null ? res : floorImpl(context, RubyNumeric.fix2int(arg));
     }

    @JRubyMethod
    public IRubyObject frac(ThreadContext context) {
        if (isNaN()) return getNaN(context);
        if (isInfinity()) return getInfinity(context, infinitySign);

        return value.scale() > 0 && value.precision() < value.scale() ?
                new RubyBigDecimal(context.runtime, value) :
                new RubyBigDecimal(context.runtime, value.subtract(((RubyBigDecimal)fix()).value));
    }

    @JRubyMethod(name = "infinite?")
    public IRubyObject infinite_p(ThreadContext context) {
        return infinitySign == 0 ? context.nil : asFixnum(context, infinitySign);
    }

    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        return toStringImpl(context, null);
    }

    @JRubyMethod(name = "nan?")
    public IRubyObject nan_p(ThreadContext context) {
        return asBoolean(context, isNaN());
    }

    @Override
    @JRubyMethod(name = "nonzero?")
    public IRubyObject nonzero_p(ThreadContext context) {
        return isZero(context) ? context.nil : this;
    }

    @Deprecated
    public IRubyObject nonzero_p() {
        return nonzero_p(getCurrentContext());
    }

    @JRubyMethod
    public  IRubyObject n_significant_digits(ThreadContext context) {
        return value.equals(BigDecimal.ZERO) ?
                RubyFixnum.zero(context.runtime) :
                asFixnum(context, value.stripTrailingZeros().precision());
    }

    @JRubyMethod
    public IRubyObject precision(ThreadContext context) {
        return precision_scale(context).aref(context, RubyFixnum.zero(context.runtime));
    }

    @JRubyMethod
    public IRubyObject scale(ThreadContext context) {
        return precision_scale(context).aref(context, RubyFixnum.one(context.runtime));
    }

    @JRubyMethod
    public RubyArray precision_scale(ThreadContext context) {
        int [] ary = getPrecisionScale();
        return newArray(context, asFixnum(context, ary[0]), asFixnum(context, ary[1]));
    }

    private int [] getPrecisionScale() {
        int precision = 0;
        int scale = 0;
        String plainString = value.toPlainString();

        if (value.equals(BigDecimal.ZERO)) {
            // special case
        } else {
            if (!plainString.contains(".")) {
                // only integer
                precision = plainString.replace("-", "").length();
            } else {
                // integer and fraction
                String [] params = plainString.split("\\.");
                if (new BigDecimal(params[1]).equals(BigDecimal.ZERO)) {
                    // special case
                    precision = params[0].replace("-", "").length();
                } else {
                    precision = Math.max(value.precision(), value.scale());
                    scale = value.scale();
                }
            }
        }
        return new int [] {precision, scale};
    }

    // mri x->Prec
    private int getPrec(ThreadContext context) {
        // precision > scale (exponent > 0)
        // e.g. "123.456789" is represented as "0. 000000123 456789 * 1000000000 ^ 1" in MRI BigDecimal internal..
        // so, in this case Prec becomes 2.
        int [] precisionScale = getPrecisionScale();
        if (precisionScale[0] > precisionScale[1]) {
            return (precisionScale[0] + (BASE_FIG - (precisionScale[0] - precisionScale[1])) + 8) / BASE_FIG;
        }
        // precision = scale
        // e.g. "0.000123456789" is represented as "0 .000123456 789 * 1000000000 ^ 0" in MRI BigDecimal internal.
        // so, in this case Prec becomes 2.
        if (getExponent(context) > -9) {
            return (value.toString().length() - 2 + 8) / BASE_FIG;
        }
        // e.g. "0.0000000000123456789" is represented as "0 .012345678 9 * 1000000000 ^ -1" in MRI BigDecimal internal.
        // so, in this case Prec becomes 2.
        return (value.unscaledValue().toString().length() + 8) / BASE_FIG;
    }

    @Deprecated
    @JRubyMethod
    public IRubyObject precs(ThreadContext context) {
        warningDeprecated(context, "BigDecimal#precs is deprecated and will be removed in the future; use BigDecimal#precision instead.");
        return newArray(context,
                asFixnum(context, getSignificantDigits().length()),
                asFixnum(context, ((getAllDigits().length() / 4) + 1) * 4));
    }

    @JRubyMethod(name = "round", optional = 2, checkArity = false)
    public IRubyObject round(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 0, 2);

        // Special treatment for BigDecimal::NAN and BigDecimal::INFINITY
        //
        // If round is called without any argument, we should raise a
        // FloatDomainError. Otherwise, we don't have to call round ;
        // we can simply return the number itself.
        if (isNaN()) {
            if (argc == 0) throw newNaNFloatDomainError(context);

            return getNaN(context);
        }
        if (isInfinity()) {
            if (argc == 0) {
                throw newInfinityFloatDomainError(context, infinitySign);
            }
            return getInfinity(context, infinitySign);
        }

        RoundingMode mode = getRoundingMode(context);
        int scale = 0;
        boolean roundToInt = false;

        switch (argc) {
            case 2:
                mode = javaRoundingModeFromRubyRoundingMode(context, args[1]);
                scale = num2int(args[0]);
                break;
            case 1:
                if (ArgsUtil.getOptionsArg(context, args[0]) == context.nil) {
                    scale = num2int(args[0]);
                    if (scale < 1) roundToInt = true;
                } else {
                    mode = javaRoundingModeFromRubyRoundingMode(context, args[0]);
                }
                break;
            case 0:
                roundToInt = true;
                break;
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
            bigDecimal = new RubyBigDecimal(context.runtime, rounded.movePointLeft(scale));
        } else {
            bigDecimal = new RubyBigDecimal(context.runtime, value.setScale(scale, mode));
        }

        return roundToInt ? bigDecimal.to_int(context) : bigDecimal;
    }

    public IRubyObject round(ThreadContext context, IRubyObject scale, IRubyObject mode) {
        return round(context, new IRubyObject[]{scale, mode});
    }

    private static int rubyRoundingModeFromJavaRoundingMode(ThreadContext context, RoundingMode mode) {
        if (mode.equals(RoundingMode.UP)) {
            return ROUND_UP;
        } else if (mode.equals(RoundingMode.DOWN)) {
            return ROUND_DOWN;
        } else if (mode.equals(RoundingMode.HALF_UP)) {
            return ROUND_HALF_UP;
        } else if (mode.equals(RoundingMode.HALF_DOWN)) {
            return ROUND_HALF_DOWN;
        } else if (mode.equals(RoundingMode.CEILING)) {
            return ROUND_CEILING;
        } else if (mode.equals(RoundingMode.FLOOR)) {
            return ROUND_FLOOR;
        } else if (mode.equals(RoundingMode.HALF_EVEN)) {
            return ROUND_HALF_EVEN;
        } else {
            throw argumentError(context, "invalid rounding mode");
        }
    }

    //this relies on the Ruby rounding enumerations == Java ones, which they (currently) all are
    private static RoundingMode javaRoundingModeFromRubyRoundingMode(ThreadContext context, IRubyObject arg) {
        if (arg == context.nil) return getRoundingMode(context);
        IRubyObject opts = ArgsUtil.getOptionsArg(context, arg);
        if (opts != context.nil) {
            arg = ArgsUtil.extractKeywordArg(context, (RubyHash) opts, "half");
            if (arg == null || arg == context.nil) return getRoundingMode(context);
            String roundingMode = arg instanceof RubySymbol ? arg.asJavaString() : arg.toString();

            return switch (roundingMode) {
                case "up" -> RoundingMode.HALF_UP;
                case "down" -> RoundingMode.HALF_DOWN;
                case "even" -> RoundingMode.HALF_EVEN;
                default -> throw argumentError(context, "invalid rounding mode (" + roundingMode + ")");
            };
        }
        if (arg instanceof RubySymbol) {
            String roundingMode = arg.asJavaString();
            return switch (roundingMode) {
                case "up" -> RoundingMode.UP;
                case "down", "truncate" -> RoundingMode.DOWN;
                case "half_up", "default" -> RoundingMode.HALF_UP;
                case "half_down" -> RoundingMode.HALF_DOWN;
                case "half_even", "even", "banker" -> RoundingMode.HALF_EVEN;
                case "ceiling", "ceil" -> RoundingMode.CEILING;
                case "floor" -> RoundingMode.FLOOR;
                default -> throw argumentError(context, "invalid rounding mode (" + roundingMode + ")");
            };
        } else {
            return switch (num2int(arg)) {
                case ROUND_UP -> RoundingMode.UP;
                case ROUND_DOWN -> RoundingMode.DOWN;
                case ROUND_HALF_UP -> RoundingMode.HALF_UP;
                case ROUND_HALF_DOWN -> RoundingMode.HALF_DOWN;
                case ROUND_CEILING -> RoundingMode.CEILING;
                case ROUND_FLOOR -> RoundingMode.FLOOR;
                case ROUND_HALF_EVEN -> RoundingMode.HALF_EVEN;
                default -> throw argumentError(context, "invalid rounding mode");
            };
        }
    }

    /**
     * @return
     * @deprecated Use {@link org.jruby.ext.bigdecimal.RubyBigDecimal#sign(ThreadContext)} instead.
     */
    public IRubyObject sign() {
        return sign(getCurrentContext());
    }

    @JRubyMethod
    public IRubyObject sign(ThreadContext context) {
        if (isNaN()) return getMetaClass().getConstant("SIGN_NaN");
        if (isInfinity()) return getMetaClass().getConstant(infinitySign < 0 ? "SIGN_NEGATIVE_INFINITE" : "SIGN_POSITIVE_INFINITE");
        if (isZero(context)) return getMetaClass().getConstant(zeroSign < 0 ? "SIGN_NEGATIVE_ZERO" : "SIGN_POSITIVE_ZERO");

        return getMetaClass().getConstant(value.signum() < 0 ? "SIGN_NEGATIVE_FINITE" : "SIGN_POSITIVE_FINITE");
    }

    private RubyFixnum signValue(ThreadContext context) {
        if (isNaN()) return RubyFixnum.zero(context.runtime);
        if (isInfinity()) return asFixnum(context, infinitySign);
        if (isZero(context)) return asFixnum(context, zeroSign);

        return asFixnum(context, value.signum());
    }

    @JRubyMethod
    public RubyArray split(ThreadContext context) {
        return RubyArray.newArray(context.runtime,
                signValue(context), newString(context, splitDigits(context)), asFixnum(context, 10), exponent());
    }

    private String splitDigits(ThreadContext context) {
        if (isNaN()) return "NaN";
        if (isInfinity()) return "Infinity";
        if (isZero(context)) return "0";

        return getSignificantDigits();
    }

    // it doesn't handle special cases
    private String getSignificantDigits() {
        return absStripTrailingZeros().unscaledValue().toString();
    }

    private String getAllDigits() {
        return value.unscaledValue().abs().toString();
    }

    private int getExponent(ThreadContext context) {
        if (isZero(context) || isNaN() || isInfinity()) return 0;

        BigDecimal val = absStripTrailingZeros();
        return val.precision() - val.scale();
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject sqrt(IRubyObject arg) {
        return sqrt(getCurrentContext(), arg);
    }

    @JRubyMethod
    public IRubyObject sqrt(ThreadContext context, IRubyObject arg) {
        if (isNaN()) throw context.runtime.newFloatDomainError("sqrt of 'NaN'(Not a Number)");
        if ((isInfinity() && infinitySign < 0) || value.signum() < 0) {
            throw context.runtime.newFloatDomainError("sqrt of negative value");
        }
        if (isInfinity() && infinitySign > 0) return getInfinity(context, 1);

        int n = RubyNumeric.num2int(precision(context)) * (getPrecisionInt(context, arg) + 1);

        return new RubyBigDecimal(context.runtime, bigSqrt(value, new MathContext(n, RoundingMode.HALF_UP))).setResult(context);
    }

    // MRI: GetPrecisionInt(VALUE v)
    private static int getPrecisionInt(ThreadContext context, final IRubyObject v) {
        int n = RubyNumeric.num2int(v);
        if (n < 0) throw argumentError(context, "negative precision");
        return n;
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject to_f() {
        return toFloat(getCurrentContext(), true);
    }

    @JRubyMethod
    public IRubyObject to_f(ThreadContext context) {
        return toFloat(context, true);
    }

    private RubyFloat toFloat(ThreadContext context, final boolean checkFlow) {
        if (isNaN()) return asFloat(context, Double.NaN);
        if (isInfinity()) return asFloat(context, infinitySign < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
        if (isZero(context)) return asFloat(context, zeroSign < 0 ? -0.0 : 0.0);

        int exponent = getExponent(context);
        if (exponent > RubyFloat.MAX_10_EXP + VP_DOUBLE_FIG) {
            if (checkFlow && isOverflowExceptionMode(context)) {
                throw context.runtime.newFloatDomainError("BigDecimal to Float conversion");
            }
            return asFloat(context, value.signum() > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
        }
        if (exponent < RubyFloat.MIN_10_EXP - VP_DOUBLE_FIG) {
            if (checkFlow && isUnderflowExceptionMode(context)) {
                throw context.runtime.newFloatDomainError("BigDecimal to Float conversion");
            }
            return asFloat(context, 0);
        }

        return asFloat(context, SafeDoubleParser.doubleValue(value));
    }

    @Override
    public RubyFloat convertToFloat() {
        return toFloat(getRuntime().getCurrentContext(), false);
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public final IRubyObject to_int() {
        return to_int(getCurrentContext());
    }

    @Override
    @JRubyMethod(name = {"to_i", "to_int"})
    public IRubyObject to_int(ThreadContext context) {
        checkFloatDomain();
        try {
            return asFixnum(context, value.longValueExact());
        } catch (ArithmeticException ex) {
            return RubyBignum.bignorm(context.runtime, value.toBigInteger());
        }
    }

    @Deprecated(since = "10.0", forRemoval = true)
    final RubyInteger to_int(Ruby runtime) {
        return (RubyInteger) to_int(getCurrentContext());
    }

    @Override
    @Deprecated
    public RubyInteger convertToInteger() {
        return (RubyInteger) to_int(getCurrentContext());
    }

    @JRubyMethod(name = "to_r")
    public IRubyObject to_r(ThreadContext context) {
        checkFloatDomain();

        int scale = Math.abs(value.scale());
        BigInteger numerator = value.scaleByPowerOfTen(scale).toBigInteger();
        BigInteger denominator = BigInteger.TEN.pow(scale);

        return RubyRational.newInstance(context, RubyBignum.newBignum(context.runtime, numerator), RubyBignum.newBignum(context.runtime, denominator));
    }

    private static String removeTrailingZeroes(final String str) {
        int l = str.length();
        while (l > 0 && str.charAt(l-1) == '0') l--;
        return str.substring(0, l);
    }

    public static boolean formatHasLeadingPlus(String format) {
        return !format.isEmpty() && format.charAt(0) == '+';
    }

    public static boolean formatHasLeadingSpace(String format) {
        return !format.isEmpty() && format.charAt(0) == ' ';
    }

    public static boolean formatHasFloatingPointNotation(String format) {
        return !format.isEmpty() && (format.charAt(format.length()-1) == 'F' || format.charAt(format.length()-1) == 'f');
    }

    private static final Pattern FRACTIONAL_DIGIT_GROUPS = Pattern.compile("(\\+| )?(\\d+)(E|F|f)?");

    public static int formatFractionalDigitGroups(String format) {
        Matcher match = FRACTIONAL_DIGIT_GROUPS.matcher(format);
        return match.matches() ? Integer.parseInt(match.group(2)) : 0;
    }

    private static boolean posSpace(String arg) {
        return arg != null && formatHasLeadingSpace(arg);
    }

    private static boolean posSign(String arg) {
        return arg != null && (formatHasLeadingPlus(arg) || posSpace(arg));
    }

    private static int groups(String arg) {
        return arg == null ? 0 : formatFractionalDigitGroups(arg);
    }

    @Override
    public final boolean isZero(ThreadContext context) {
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

    private CharSequence engineeringValue(ThreadContext context, final String arg) {
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
        build.append('e').append(getExponent(context));
        return build;
    }

    private CharSequence floatingPointValue(final String arg) {
        List<String> values = StringSupport.split(absStripTrailingZeros().toPlainString(), '.');
        final String whole = !values.isEmpty() ? values.get(0) : "0";
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
    @JRubyMethod
    public RubyString to_s(ThreadContext context) {
        return toStringImpl(context, null);
    }

    @JRubyMethod
    public RubyString to_s(ThreadContext context, IRubyObject arg) {
        return toStringImpl(context, arg == context.nil ? null : arg.toString());
    }

    private RubyString toStringImpl(ThreadContext context, String arg) {
        if ( isNaN() ) return RubyString.newUSASCIIString(context.runtime, "NaN");
        if ( isInfinity() ) {
            if ( arg != null && infinitySign >= 0) {
                if ( formatHasLeadingSpace(arg) ) return RubyString.newUSASCIIString(context.runtime, " Infinity");
                if ( formatHasLeadingPlus(arg) ) return RubyString.newUSASCIIString(context.runtime, "+Infinity");
            }
            return RubyString.newUSASCIIString(context.runtime, infinityString(infinitySign));
        }
        if (isZero(context)) {
            if (zeroSign < 0) return RubyString.newUSASCIIString(context.runtime, "-0.0");
            if (arg != null && formatHasLeadingSpace(arg)) return RubyString.newUSASCIIString(context.runtime, " 0.0");
            if (arg != null && formatHasLeadingPlus(arg)) return RubyString.newUSASCIIString(context.runtime, "+0.0");
            return RubyString.newUSASCIIString(context.runtime, "0.0");
        }

        boolean asEngineering = arg == null || ! formatHasFloatingPointNotation(arg);

        return RubyString.newUSASCIIString(context.runtime, ( asEngineering ? engineeringValue(context, arg) : floatingPointValue(arg) ).toString());
    }

    @Override
    public String toString() {
        var context = getRuntime().getCurrentContext();
        if (isNaN()) return "NaN";
        if (isInfinity()) return infinityString(infinitySign);
        if (isZero(context)) return zeroSign < 0 ? "-0.0" : "0.0";

        return engineeringValue(context, null).toString();
    }

    @Deprecated
    public IRubyObject to_s(IRubyObject[] args) {
        return toStringImpl(getCurrentContext(), args.length == 0 ? null : (args[0].isNil() ? null : args[0].toString()));
    }

    // Note: #fix has only no-arg form, but truncate allows optional parameter.
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject fix() {
        return fix(getCurrentContext());
    }

    @JRubyMethod
    public IRubyObject fix(ThreadContext context) {
        return truncateInternal(context, 0);
    }

    private RubyBigDecimal truncateInternal(ThreadContext context, int arg) {
        if (isNaN()) return getNaN(context);
        if (isInfinity()) return getInfinity(context, infinitySign);

        int precision = value.precision() - value.scale() + arg;

        return precision > 0 ?
                new RubyBigDecimal(context.runtime, value.round(new MathContext(precision, RoundingMode.DOWN))) :
                getZero(context, this.zeroSign);
    }

    @JRubyMethod
    public IRubyObject truncate(ThreadContext context) {
        return truncateInternal(context, 0).to_int(context);
    }

    @JRubyMethod
    public IRubyObject truncate(ThreadContext context, IRubyObject arg) {
        return truncateInternal(context, RubyNumeric.fix2int(arg));
    }

    @Override
    @JRubyMethod(name = "zero?")
    public IRubyObject zero_p(ThreadContext context) {
        return asBoolean(context, isZero(context));
    }

    @Deprecated
    public IRubyObject zero_p() {
        return zero_p(getCurrentContext());
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
     * @see <a href="https://web.archive.org/web/20101025040915/http://oldblog.novaloka.nl/blogger.xs4all.nl/novaloka/archive/2007/09/15/295396.html">Java BigDecimal sqrt() method - Square Root by Coupled Newton Iteration</a>
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
      ArrayList<Integer> nPrecs = new ArrayList<>();

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

    @Deprecated // no longer used
    public RubyBigDecimal(Ruby runtime, RubyBigDecimal rbd) {
        this(runtime, Access.getClass(runtime.getCurrentContext(), "BigDecimal"), rbd);
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
