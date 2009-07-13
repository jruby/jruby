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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * Copyright (C) 2009 Joseph LaFata <joe@quibb.org>
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyConstant;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

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
    public final static int SIGN_POSITIVE_INFINITE=3;
    @JRubyConstant
    public final static int EXCEPTION_OVERFLOW=8;
    @JRubyConstant
    public final static int SIGN_POSITIVE_ZERO=1;
    @JRubyConstant
    public final static int EXCEPTION_ALL=255;
    @JRubyConstant
    public final static int SIGN_NEGATIVE_FINITE=-2;
    @JRubyConstant
    public final static int EXCEPTION_UNDERFLOW=4;
    @JRubyConstant
    public final static int SIGN_NaN=0;
    @JRubyConstant
    public final static int BASE=10000;
    @JRubyConstant
    public final static int ROUND_MODE=256;
    @JRubyConstant
    public final static int SIGN_POSITIVE_FINITE=2;
    @JRubyConstant
    public final static int EXCEPTION_INFINITY=1;
    @JRubyConstant
    public final static int SIGN_NEGATIVE_INFINITE=-3;
    @JRubyConstant
    public final static int EXCEPTION_ZERODIVIDE=1;
    @JRubyConstant
    public final static int SIGN_NEGATIVE_ZERO=-1;
    @JRubyConstant
    public final static int EXCEPTION_NaN=2;
    
    // Static constants
    private static final BigDecimal TWO = new BigDecimal(2);
    private static final double SQRT_10 = 3.162277660168379332;
    
    public static RubyClass createBigDecimal(Ruby runtime) {
        RubyClass result = runtime.defineClass("BigDecimal",runtime.getNumeric(), BIGDECIMAL_ALLOCATOR);

        runtime.getKernel().defineAnnotatedMethods(BigDecimalKernelMethods.class);

        result.setInternalModuleVariable("vpPrecLimit", RubyFixnum.zero(runtime));
        result.setInternalModuleVariable("vpExceptionMode", RubyFixnum.zero(runtime));
        result.setInternalModuleVariable("vpRoundingMode", runtime.newFixnum(ROUND_HALF_UP));
        
        result.defineAnnotatedMethods(RubyBigDecimal.class);
        result.defineAnnotatedConstants(RubyBigDecimal.class);

        return result;
    }

    private boolean isNaN = false;
    private int infinitySign = 0;
    private int zeroSign = 0;
    private BigDecimal value;

    public BigDecimal getValue() {
        return value;
    }

    public RubyBigDecimal(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    public RubyBigDecimal(Ruby runtime, BigDecimal value) {
        super(runtime, runtime.fastGetClass("BigDecimal"));
        this.value = value;
    }
    
    public static class BigDecimalKernelMethods {
        @JRubyMethod(name = "BigDecimal", rest = true, module = true, visibility = Visibility.PRIVATE)
        public static IRubyObject newBigDecimal(IRubyObject recv, IRubyObject[] args) {
            return RubyBigDecimal.newBigDecimal(recv, args, Block.NULL_BLOCK);
        }
    }

    public static RubyBigDecimal newBigDecimal(IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        return newInstance(recv.getRuntime().fastGetClass("BigDecimal"), args);
    }

    @JRubyMethod(name = "ver", meta = true)
    public static IRubyObject ver(IRubyObject recv) {
        return recv.getRuntime().newString("1.0.1");
    }

    @JRubyMethod(name = "_dump", optional = 1, frame = true)
    public IRubyObject dump(IRubyObject[] args, Block unusedBlock) {
        RubyString precision = RubyString.newUnicodeString(args[0].getRuntime(), "0:");
        RubyString str = this.asString();
        return precision.append(str);
    }
        
    @JRubyMethod(name = "_load", required = 1, frame = true, meta = true)
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

    @JRubyMethod(name = "mode", required = 1, optional = 1, meta = true)
    public static IRubyObject mode(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        // FIXME: I doubt any of the constants referenced in this method
        // are ever redefined -- should compare to the known values, rather
        // than do an expensive constant lookup.
        Ruby runtime = recv.getRuntime();
        RubyClass clazz = runtime.fastGetClass("BigDecimal");
        RubyModule c = (RubyModule)recv;
        
        args = Arity.scanArgs(runtime, args, 1, 1);
        
        IRubyObject mode = args[0];
        IRubyObject value = args[1];
        
        if (!(mode instanceof RubyFixnum)) {
            throw runtime.newTypeError("wrong argument type " + mode.getMetaClass() + " (expected Fixnum)");
        }
        
        long longMode = ((RubyFixnum)mode).getLongValue();
        long EXCEPTION_ALL = ((RubyFixnum)clazz.fastGetConstant("EXCEPTION_ALL")).getLongValue();
        if ((longMode & EXCEPTION_ALL) != 0) {     
            if (value.isNil()) {
                return c.searchInternalModuleVariable("vpExceptionMode");
            }
            if (!(value.isNil()) && !(value instanceof RubyBoolean)) {
                throw runtime.newTypeError("second argument must be true or false");
            }

            RubyFixnum currentExceptionMode = (RubyFixnum)c.searchInternalModuleVariable("vpExceptionMode");
            RubyFixnum newExceptionMode = new RubyFixnum(runtime, currentExceptionMode.getLongValue());
            
            RubyFixnum EXCEPTION_INFINITY = (RubyFixnum)clazz.fastGetConstant("EXCEPTION_INFINITY");
            if ((longMode & EXCEPTION_INFINITY.getLongValue()) != 0) {
                newExceptionMode = (value.isTrue()) ? (RubyFixnum)currentExceptionMode.callCoerced(context, "|", EXCEPTION_INFINITY)
                        : (RubyFixnum)currentExceptionMode.callCoerced(context, "&", new RubyFixnum(runtime, ~(EXCEPTION_INFINITY).getLongValue()));
            }
            
            RubyFixnum EXCEPTION_NaN = (RubyFixnum)clazz.fastGetConstant("EXCEPTION_NaN");
            if ((longMode & EXCEPTION_NaN.getLongValue()) != 0) {
                newExceptionMode = (value.isTrue()) ? (RubyFixnum)currentExceptionMode.callCoerced(context, "|", EXCEPTION_NaN)
                        : (RubyFixnum)currentExceptionMode.callCoerced(context, "&", new RubyFixnum(runtime, ~(EXCEPTION_NaN).getLongValue()));
            }
            
            RubyFixnum EXCEPTION_UNDERFLOW = (RubyFixnum)clazz.fastGetConstant("EXCEPTION_UNDERFLOW");
            if ((longMode & EXCEPTION_UNDERFLOW.getLongValue()) != 0) {
                newExceptionMode = (value.isTrue()) ? (RubyFixnum)currentExceptionMode.callCoerced(context, "|", EXCEPTION_UNDERFLOW)
                        : (RubyFixnum)currentExceptionMode.callCoerced(context, "&", new RubyFixnum(runtime, ~(EXCEPTION_UNDERFLOW).getLongValue()));
            }
            RubyFixnum EXCEPTION_OVERFLOW = (RubyFixnum)clazz.fastGetConstant("EXCEPTION_OVERFLOW");
            if ((longMode & EXCEPTION_OVERFLOW.getLongValue()) != 0) {
                newExceptionMode = (value.isTrue()) ? (RubyFixnum)currentExceptionMode.callCoerced(context, "|", EXCEPTION_OVERFLOW)
                        : (RubyFixnum)currentExceptionMode.callCoerced(context, "&", new RubyFixnum(runtime, ~(EXCEPTION_OVERFLOW).getLongValue()));
            }
            c.setInternalModuleVariable("vpExceptionMode", newExceptionMode);
            return newExceptionMode;
        }
        
        long ROUND_MODE = ((RubyFixnum)clazz.fastGetConstant("ROUND_MODE")).getLongValue();
        if (longMode == ROUND_MODE) {
            if (value.isNil()) {
                return c.searchInternalModuleVariable("vpRoundingMode");
            }
            if (!(value instanceof RubyFixnum)) {
                throw runtime.newTypeError("wrong argument type " + mode.getMetaClass() + " (expected Fixnum)");
            }
            
            RubyFixnum roundingMode = (RubyFixnum)value;
            if (roundingMode == clazz.fastGetConstant("ROUND_UP") ||
                    roundingMode == clazz.fastGetConstant("ROUND_DOWN") ||
                    roundingMode == clazz.fastGetConstant("ROUND_FLOOR") ||
                    roundingMode == clazz.fastGetConstant("ROUND_CEILING") ||
                    roundingMode == clazz.fastGetConstant("ROUND_HALF_UP") ||
                    roundingMode == clazz.fastGetConstant("ROUND_HALF_DOWN") ||
                    roundingMode == clazz.fastGetConstant("ROUND_HALF_EVEN")) {
                c.setInternalModuleVariable("vpRoundingMode", roundingMode);
            } else {
                throw runtime.newTypeError("invalid rounding mode");
            }
            return c.searchInternalModuleVariable("vpRoundingMode");
        }
        throw runtime.newTypeError("first argument for BigDecimal#mode invalid");
    }

    private RoundingMode getRoundingMode(Ruby runtime) {
        RubyFixnum roundingMode = (RubyFixnum)runtime.fastGetClass("BigDecimal")
                .searchInternalModuleVariable("vpRoundingMode");
        return RoundingMode.valueOf((int)roundingMode.getLongValue());
    }

    private static boolean isNaNExceptionMode(Ruby runtime) {
        RubyFixnum currentExceptionMode = (RubyFixnum)runtime.fastGetClass("BigDecimal")
                .searchInternalModuleVariable("vpExceptionMode");
        RubyFixnum EXCEPTION_NaN = (RubyFixnum)runtime.fastGetClass("BigDecimal")
                .fastGetConstant("EXCEPTION_NaN");
        return (currentExceptionMode.getLongValue() & EXCEPTION_NaN.getLongValue()) != 0;
    }

    private static boolean isInfinityExceptionMode(Ruby runtime) {
        RubyFixnum currentExceptionMode = (RubyFixnum)runtime.fastGetClass("BigDecimal")
                .searchInternalModuleVariable("vpExceptionMode");
        RubyFixnum EXCEPTION_INFINITY = (RubyFixnum)runtime.fastGetClass("BigDecimal")
                .fastGetConstant("EXCEPTION_INFINITY");
        return (currentExceptionMode.getLongValue() & EXCEPTION_INFINITY.getLongValue()) != 0;
    }
    
    private static boolean isOverflowExceptionMode(Ruby runtime) {
        RubyFixnum currentExceptionMode = (RubyFixnum)runtime.fastGetClass("BigDecimal")
                .searchInternalModuleVariable("vpExceptionMode");
        RubyFixnum EXCEPTION_OVERFLOW = (RubyFixnum)runtime.fastGetClass("BigDecimal")
                .fastGetConstant("EXCEPTION_OVERFLOW");
        return (currentExceptionMode.getLongValue() & EXCEPTION_OVERFLOW.getLongValue()) != 0;
    }

    private static RubyBigDecimal getVpValue(IRubyObject v, boolean must) {
        if(v instanceof RubyBigDecimal) {
            return (RubyBigDecimal)v;
        } else if(v instanceof RubyFixnum || v instanceof RubyBignum) {
            String s = v.toString();
            return newInstance(v.getRuntime().fastGetClass("BigDecimal"),new IRubyObject[]{v.getRuntime().newString(s)});
        }
        if(must) {
            String err;
            if (v.isImmediate()) {
                ThreadContext context = v.getRuntime().getCurrentContext();
                err = RubyObject.inspect(context, v).toString();
            } else {
                err = v.getMetaClass().getBaseName();
            }
            throw v.getRuntime().newTypeError(err + " can't be coerced into BigDecimal");
        }
        return null;
    }

    @JRubyMethod(name = "induced_from", required = 1, frame = true, meta = true)
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
        if (args.length == 0) { 
            decimal = new BigDecimal(0);
        } else {
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
                decimal = new BigDecimal(strValue);
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
        return new RubyBigDecimal(runtime, decimal);
    }

    private static RubyBigDecimal newZero(Ruby runtime, int sign) {
        RubyBigDecimal rbd =  new RubyBigDecimal(runtime, BigDecimal.ZERO);
        if (sign < 0) {
            rbd.zeroSign = -1;
        } else {
            rbd.zeroSign = 1;
        }
        return rbd;
    }

    private static RubyBigDecimal newNaN(Ruby runtime) {
        if (isNaNExceptionMode(runtime)) {
            throw runtime.newFloatDomainError("Computation results to 'NaN'(Not a Number)");
        }
        RubyBigDecimal rbd =  new RubyBigDecimal(runtime, BigDecimal.ZERO);
        rbd.isNaN = true;
        return rbd;
    }
    
    private static RubyBigDecimal newInfinity(Ruby runtime, int sign) {
        RubyBigDecimal rbd =  new RubyBigDecimal(runtime, BigDecimal.ZERO);
        if (isInfinityExceptionMode(runtime)) {
            throw runtime.newFloatDomainError("Computation results to 'Infinity'");
        }
        if (sign < 0) {
            rbd.infinitySign = -1;
        } else {
            rbd.infinitySign = 1;
        }
        return rbd;
    }

    private RubyBigDecimal setResult() {
        return setResult(0);
    }

    private RubyBigDecimal setResult(int scale) {
        int prec = RubyFixnum.fix2int(getRuntime().fastGetClass("BigDecimal").searchInternalModuleVariable("vpPrecLimit"));
        int prec2 = Math.max(scale,prec);
        if(prec2 > 0 && this.value.scale() > (prec2-getExponent())) {
            this.value = this.value.setScale(prec2-getExponent(),BigDecimal.ROUND_HALF_UP);
        }
        return this;
    }
    
    @JRubyMethod(name = "hash")
    public RubyFixnum hash() {
        return getRuntime().newFixnum(value.hashCode());
    }

    @JRubyMethod(name = {"%", "modulo"}, required = 1)
    public IRubyObject op_mod(ThreadContext context, IRubyObject arg) {
        // TODO: full-precision remainder is 1000x slower than MRI!
        Ruby runtime = context.getRuntime();
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

    @JRubyMethod(name = "remainder", required = 1)
    public IRubyObject remainder(ThreadContext context, IRubyObject arg) {
        // TODO: full-precision remainder is 1000x slower than MRI!
        Ruby runtime = context.getRuntime();
        if (isInfinity() || isNaN()) {
            return newNaN(runtime);
        }
        RubyBigDecimal val = getVpValue(arg,false);
        if (val == null) {
            return callCoerced(context, "remainder", arg, true);
        }
        if (val.isInfinity() || val.isNaN() || val.isZero()) {
            return newNaN(runtime);
        }

        // Java and MRI definitions of remainder are the same.
        return new RubyBigDecimal(runtime, value.remainder(val.value)).setResult();
    }

    @JRubyMethod(name = "*", required = 1)
    public IRubyObject op_mul(ThreadContext context, IRubyObject arg) {
        return mult2(context, arg, getRuntime().fastGetClass("BigDecimal")
                .searchInternalModuleVariable("vpPrecLimit"));
    }

    @JRubyMethod(name = "mult", required = 2)
    public IRubyObject mult2(ThreadContext context, IRubyObject b, IRubyObject n) {
        Ruby runtime = context.getRuntime();

        RubyBigDecimal val = getVpValue(b,false);
        if(val == null) {
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
    
    @JRubyMethod(name = {"**", "power"}, required = 1)
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

    @JRubyMethod(name = "+", required = 1, frame=true)
    public IRubyObject op_plus(ThreadContext context, IRubyObject b) {
        return addInternal(context, b, "add", getRuntime().fastGetClass("BigDecimal")
                .searchInternalModuleVariable("vpPrecLimit"));
    }

    @JRubyMethod(name = "add", required = 2, frame=true)
    public IRubyObject add2(ThreadContext context, IRubyObject b, IRubyObject digits) {
        return addInternal(context, b, "add", digits);
    }

    private IRubyObject addInternal(ThreadContext context, IRubyObject b, String op, IRubyObject digits) {
        Ruby runtime = context.getRuntime();
        int prec = getPositiveInt(context, digits);

        RubyBigDecimal val = getVpValue(b, false);
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
        Ruby runtime = context.getRuntime();

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
        // TODO: don't calculate the same value 3 times
        if (infinitySign * val.infinitySign > 0) {
            return isInfinity() ? this : val;
        }
        if (infinitySign * val.infinitySign < 0) {
            return newNaN(getRuntime());
        }
        if (infinitySign * val.infinitySign == 0) {
            int sign = infinitySign + val.infinitySign;
            if (sign != 0) {
                return newInfinity(getRuntime(), sign);
            }
        }
        return null;
    }

    @JRubyMethod(name = "+@")
    public IRubyObject op_uplus() {
        return this;
    }
    
    @JRubyMethod(name = "-", required = 1)
    public IRubyObject op_minus(ThreadContext context, IRubyObject arg) {
        RubyBigDecimal val = getVpValue(arg, false);
        if(val == null) {
            return callCoerced(context, "-", arg);
        }
        RubyBigDecimal res = handleMinusSpecialValues(val);
        if (res != null) {
            return res;
        }
        return new RubyBigDecimal(getRuntime(),value.subtract(val.value)).setResult();
    }

    @JRubyMethod(name = "sub", required = 2)
    public IRubyObject sub2(ThreadContext context, IRubyObject b, IRubyObject n) {
        RubyBigDecimal val = getVpValue(b, false);
        if(val == null) {
            return callCoerced(context, "-", b);
        }
        RubyBigDecimal res = handleMinusSpecialValues(val);
        if (res != null) {
            return res;
        }

        return new RubyBigDecimal(getRuntime(),value.subtract(val.value)).setResult();
    }

    private RubyBigDecimal handleMinusSpecialValues(RubyBigDecimal val) {
        if (isNaN() || val.isNaN()) {
            return newNaN(getRuntime());
        }
        
        // TODO: 3 times calculate the same value below
        if (infinitySign * val.infinitySign > 0) {
            return newNaN(getRuntime());
        }
        if (infinitySign * val.infinitySign < 0) {
                return this;
        }
        if (infinitySign * val.infinitySign == 0) {
            if (isInfinity()) {
                return this;
            }
            if (val.isInfinity()) {
                return newInfinity(getRuntime(), val.infinitySign * -1);
            }
            int sign = infinitySign + val.infinitySign;
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

    @JRubyMethod(name = {"/", "quo"})
    public IRubyObject op_quo(ThreadContext context, IRubyObject other) {
        // regular division with some default precision
        // TODO: proper algorithm to set the precision
        return op_div(context, other, getRuntime().newFixnum(200));
    }

    @JRubyMethod(name = "div")
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

    @JRubyMethod(name = "div")
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

    private IRubyObject cmp(ThreadContext context, IRubyObject r, char op) {
        int e = 0;
        RubyBigDecimal rb = getVpValue(r,false);
        if(rb == null) {
            IRubyObject ee = callCoerced(context, "<=>",r);
            if(ee.isNil()) {
                return getRuntime().getNil();
            }
            e = RubyNumeric.fix2int(ee);
        } else {
            if (isNaN() | rb.isNaN()) {
                return getRuntime().getNil();
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

    @JRubyMethod(name = "<=>", required = 1)
    public IRubyObject op_cmp(ThreadContext context, IRubyObject arg) {
        return cmp(context, arg,'*');
    }

    @JRubyMethod(name = {"eql?", "==", "==="}, required = 1)
    public IRubyObject eql_p(ThreadContext context, IRubyObject arg) {
        return cmp(context, arg,'=');
    }

    @JRubyMethod(name = "<", required = 1)
    public IRubyObject op_lt(ThreadContext context, IRubyObject arg) {
        return cmp(context, arg,'<');
    }

    @JRubyMethod(name = "<=", required = 1)
    public IRubyObject op_le(ThreadContext context, IRubyObject arg) {
        return cmp(context, arg,'L');
    }

    @JRubyMethod(name = ">", required = 1)
    public IRubyObject op_gt(ThreadContext context, IRubyObject arg) {
        return cmp(context, arg,'>');
    }

    @JRubyMethod(name = ">=", required = 1)
    public IRubyObject op_ge(ThreadContext context, IRubyObject arg) {
        return cmp(context, arg,'G');
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

    @JRubyMethod(name = "coerce", required = 1)
    @Override
    public IRubyObject coerce(IRubyObject other) {
        IRubyObject obj;
        if(other instanceof RubyFloat) {
            obj = getRuntime().newArray(other,to_f());
        } else {
            obj = getRuntime().newArray(getVpValue(other, true),this);
        }
        return obj;
    }

    @Override
    public double getDoubleValue() { return value.doubleValue(); }
    
    @Override
    public long getLongValue() { return value.longValue(); }

    @Override
    public BigInteger getBigIntegerValue() {
        return value.toBigInteger();
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

    @JRubyMethod(name = "divmod", required = 1)
    public IRubyObject divmod(ThreadContext context, IRubyObject other) {
        // TODO: full-precision divmod is 1000x slower than MRI!
        Ruby runtime = context.getRuntime();
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

    @JRubyMethod(name = "round", optional = 2)
    public IRubyObject round(IRubyObject[] args) {
        int scale = args.length > 0 ? num2int(args[0]) : 0;
        int mode = (args.length > 1) ? javaRoundingModeFromRubyRoundingMode(args[1]) : BigDecimal.ROUND_HALF_UP;
        // JRUBY-914: Java 1.4 BigDecimal does not allow a negative scale, so we have to simulate it
        if (scale < 0) {
          // shift the decimal point just to the right of the digit to be rounded to (divide by 10**(abs(scale)))
          // -1 -> 10's digit, -2 -> 100's digit, etc.
          BigDecimal normalized = value.movePointRight(scale);
          // ...round to that digit
          BigDecimal rounded = normalized.setScale(0,mode);
          // ...and shift the result back to the left (multiply by 10**(abs(scale)))
          return new RubyBigDecimal(getRuntime(), rounded.movePointLeft(scale));
        } else {
          return new RubyBigDecimal(getRuntime(), value.setScale(scale, mode));
        }
    }

    //this relies on the Ruby rounding enumerations == Java ones, which they (currently) all are
    private int javaRoundingModeFromRubyRoundingMode(IRubyObject arg) {
      return num2int(arg);
    }
    
    @JRubyMethod(name = "sign")
    public IRubyObject sign() {
        if (isNaN()) {
            return getMetaClass().fastGetConstant("SIGN_NaN");
        }

        if (isInfinity()) {
            if (infinitySign < 0) {
                return getMetaClass().fastGetConstant("SIGN_NEGATIVE_INFINITE");
            } else {
                return getMetaClass().fastGetConstant("SIGN_POSITIVE_INFINITE");
            }
        }

        if (isZero()) {
            if (zeroSign < 0) {
                return getMetaClass().fastGetConstant("SIGN_NEGATIVE_ZERO");
            } else {
                return getMetaClass().fastGetConstant("SIGN_POSITIVE_ZERO");
            }
        }
        
        if (value.signum() < 0) {
            return getMetaClass().fastGetConstant("SIGN_NEGATIVE_FINITE");
        } else {
            return getMetaClass().fastGetConstant("SIGN_POSITIVE_FINITE");
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
        // TODO: no need to calculate every time.
        if (isZero()) {
            return 0;
        }
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
        if (-value.scale() < RubyFloat.MIN_10_EXP) {
            return RubyFloat.newFloat(getRuntime(), 0);
        }
        return RubyFloat.newFloat(getRuntime(), value.doubleValue());
    }

    @JRubyMethod(name = {"to_i", "to_int"})
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

    private String removeTrailingZeroes(String in) {
        while(in.length() > 0 && in.charAt(in.length()-1)=='0') {
            in = in.substring(0,in.length()-1);
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

    private boolean isNaN() {
        return isNaN;
    }

    private boolean isInfinity() {
        return infinitySign != 0;
    }

    private String unscaledValue() {
        return value.abs().unscaledValue().toString();
    }

    private IRubyObject engineeringValue(String arg) {
        int exponent = getExponent();
        int signum = value.signum();
        StringBuilder build = new StringBuilder();
        build.append(signum == -1 ? "-" : (signum == 1 ? (posSign(arg) ? (posSpace(arg) ? " " : "+") : "") : ""));
        build.append("0.");
        if (0 == groups(arg)) {
            String s = removeTrailingZeroes(unscaledValue());
            if ("".equals(s)) {
                build.append("0");
            } else {
                build.append(s);
            }
        } else {
            int index = 0;
            String sep = "";
            while (index < unscaledValue().length()) {
                int next = index + groups(arg);
                if (next > unscaledValue().length()) {
                    next = unscaledValue().length();
                }
                build.append(sep).append(unscaledValue().substring(index, next));
                sep = " ";
                index += groups(arg);
            }
        }
        build.append("E").append(exponent);
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
        if(asEngineering(arg)) {
            return engineeringValue(arg);
        } else {
            return floatingPointValue(arg);
        }
    }

    // Note: #fix has only no-arg form, but truncate allows optional parameter.

    @JRubyMethod
    public IRubyObject fix() {
        return truncate(RubyFixnum.zero(getRuntime()));
    }

    @JRubyMethod
    public IRubyObject truncate() {
        return truncate(RubyFixnum.zero(getRuntime()));
    }

    @JRubyMethod
    public IRubyObject truncate(IRubyObject arg) {
        if (isNaN) {
            return newNaN(getRuntime());
        }
        if (isInfinity()) {
            return newInfinity(getRuntime(), infinitySign);
        }

        int n = RubyNumeric.fix2int(arg);
        
        int precision = value.precision() - value.scale() + n;
        
        if (precision > 0) {
            return new RubyBigDecimal(getRuntime(),
                    value.round(new MathContext(precision, RoundingMode.DOWN)));
        } else {
            // TODO: proper sign
            return new RubyBigDecimal(getRuntime(), BigDecimal.ZERO);
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
     */
    public static BigDecimal bigSqrt(BigDecimal squarD, MathContext rootMC) {
       // General number and precision checking
      int sign = squarD.signum();
      if (sign == -1) {
          throw new ArithmeticException("Square root of a negative number: " + squarD);
      } else if(sign == 0) {
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

      double root = Math.sqrt(bi.doubleValue());
      BigDecimal halfBack = new BigDecimal(BigInteger.ONE.shiftLeft(shift/2));

      int scale = squarD.scale();
      if (scale % 2 == 1) {
          root *= SQRT_10;                        // 5 -> 2, -5 -> -3 need half a scale more..
      }
      scale = (int) Math.floor(scale/2.);         // ..where 100 -> 10 shifts the scale

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
}// RubyBigdecimal
