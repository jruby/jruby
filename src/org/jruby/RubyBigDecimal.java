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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyConstant;

import org.jruby.runtime.Arity;

import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
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
    public final static int EXCEPTION_OVERFLOW=1;
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
    
    public static RubyClass createBigDecimal(Ruby runtime) {
        RubyClass result = runtime.defineClass("BigDecimal",runtime.getNumeric(), BIGDECIMAL_ALLOCATOR);

        CallbackFactory callbackFactory = runtime.callbackFactory(RubyBigDecimal.class);

        runtime.getKernel().defineModuleFunction("BigDecimal",callbackFactory.getOptSingletonMethod("newBigDecimal"));

        result.setInternalModuleVariable("vpPrecLimit", RubyFixnum.zero(runtime));
        result.setInternalModuleVariable("vpExceptionMode", RubyFixnum.zero(runtime));
        result.setInternalModuleVariable("vpRoundingMode", RubyFixnum.zero(runtime));
        
        result.defineAnnotatedMethods(RubyBigDecimal.class);
        result.defineAnnotatedConstants(RubyBigDecimal.class);
        result.dispatcher = callbackFactory.createDispatcher(result);

        return result;
    }

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

    private RubyBigDecimal getVpValue(IRubyObject v, boolean must) {
        if(v instanceof RubyBigDecimal) {
            return (RubyBigDecimal)v;
        } else if(v instanceof RubyFixnum || v instanceof RubyBignum) {
            String s = v.toString();
            return newInstance(getRuntime().fastGetClass("BigDecimal"),new IRubyObject[]{getRuntime().newString(s)});
        }
        if(must) {
            String err;
            if (isImmediate()) {
                ThreadContext context = getRuntime().getCurrentContext();
                err = RubyString.objAsString(context, callMethod(context, "inspect")).toString();
            } else {
                err = getMetaClass().getBaseName();
            }
            throw getRuntime().newTypeError(err + " can't be coerced into BigDecimal");
        }
        return null;
    }

    @JRubyMethod(name = "new", required = 1, optional = 1, meta = true)
    public static RubyBigDecimal newInstance(IRubyObject recv, IRubyObject[] args) {
        BigDecimal decimal;
        if (args.length == 0) { 
            decimal = new BigDecimal(0);
        } else {
            try {
                decimal = new BigDecimal(args[0].convertToString().toString());
            } catch(NumberFormatException e) {
                decimal = new BigDecimal(0);
            }
        }
        return new RubyBigDecimal(recv.getRuntime(), decimal);
    }

    private RubyBigDecimal setResult() {
        return setResult(0);
    }

    private RubyBigDecimal setResult(int scale) {
        int prec = RubyFixnum.fix2int(getRuntime().fastGetClass("BigDecimal").searchInternalModuleVariable("vpPrecLimit"));
        int prec2 = Math.max(scale,prec);
        if(prec2 > 0 && this.value.scale() > (prec2-exp())) {
            this.value = this.value.setScale(prec2-exp(),BigDecimal.ROUND_HALF_UP);
        }
        return this;
    }
    
    @JRubyMethod(name = "hash")
    public RubyFixnum hash() {
        return getRuntime().newFixnum(value.hashCode());
    }

    @JRubyMethod(name = {"%", "modulo"}, required = 1)
    public IRubyObject op_mod(IRubyObject arg) {
        RubyBigDecimal val = getVpValue(arg,false);
        return new RubyBigDecimal(getRuntime(),this.value.divideAndRemainder(val.value)[1]).setResult();
    }

    @JRubyMethod(name = "*", required = 1)
    public IRubyObject op_mul(ThreadContext context, IRubyObject arg) {
        RubyBigDecimal val = getVpValue(arg, false);
        if(val == null) {
            return callCoerced(context, "*", arg);
        }

        return new RubyBigDecimal(getRuntime(),value.multiply(val.value)).setResult();
    }

    @JRubyMethod(name = "mult", required = 2)
    public IRubyObject mult2(ThreadContext context, IRubyObject b, IRubyObject n) {
        RubyBigDecimal val = getVpValue(b,false);
        if(val == null) {
            return callCoerced(context, "*",b);
        }

        return new RubyBigDecimal(getRuntime(),value.multiply(val.value)).setResult();
    }
    
    @JRubyMethod(name = {"**", "power"}, required = 1)
    public IRubyObject op_pow(IRubyObject arg) {
        if (!(arg instanceof RubyFixnum)) {
            throw getRuntime().newTypeError("wrong argument type " + arg.getMetaClass() + " (expected Fixnum)");
        }
        
        BigDecimal val = value;
        
        int times = RubyNumeric.fix2int(arg.convertToInteger());
        int sign = 0;
        if (times < 0) {
            sign = -1;
            times = -times;
        }
        
        BigDecimal result = BigDecimal.ONE;
        while (times > 0) {
            if (times % 2 != 0) {
                result = result.multiply(val);
                times -= 1;
            }
            val = val.multiply(val);
            times /= 2;
        }
        
        if (sign == -1) {
            result = BigDecimal.ONE.divide(result);
        }
        
        return new RubyBigDecimal(getRuntime(),result).setResult();
    }

    @JRubyMethod(name = "+", required = 1)
    public IRubyObject op_plus(ThreadContext context, IRubyObject arg) {
        RubyBigDecimal val = getVpValue(arg, false);
        if(val == null) {
            return callCoerced(context, "+", arg);
        }
        return new RubyBigDecimal(getRuntime(),value.add(val.value)).setResult();
    }

    @JRubyMethod(name = "add", required = 2)
    public IRubyObject add2(ThreadContext context, IRubyObject b, IRubyObject n) {
        RubyBigDecimal val = getVpValue(b, false);
        if(val == null) {
            return callCoerced(context, "+", b);
        }
        return new RubyBigDecimal(getRuntime(),value.add(val.value)).setResult();
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
        return new RubyBigDecimal(getRuntime(),value.subtract(val.value)).setResult();
    }

    @JRubyMethod(name = "sub", required = 2)
    public IRubyObject sub2(ThreadContext context, IRubyObject b, IRubyObject n) {
        RubyBigDecimal val = getVpValue(b, false);
        if(val == null) {
            return callCoerced(context, "-", b);
        }
        return new RubyBigDecimal(getRuntime(),value.subtract(val.value)).setResult();
    }
    
    @JRubyMethod(name = "-@")
    public IRubyObject op_uminus() {
        return new RubyBigDecimal(getRuntime(), value.negate());
    }
    
    @JRubyMethod(name = {"/", "div", "quo"}, required = 1, optional = 1)
    public IRubyObject op_div(ThreadContext context, IRubyObject[] args) {
        int scale = 0;
        if(args.length == 2) {
            scale = RubyNumeric.fix2int(args[1]);
        }

        RubyBigDecimal val = getVpValue(args[0],false);
        if(val == null) {
            return callCoerced(context, "/",args[0]);
        }

        if(scale == 0) {
            return new RubyBigDecimal(getRuntime(),value.divide(val.value,200,BigDecimal.ROUND_HALF_UP)).setResult();
        } else {
            return new RubyBigDecimal(getRuntime(),value.divide(val.value,200,BigDecimal.ROUND_HALF_UP)).setResult(scale);
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
            e = value.compareTo(rb.value);
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

    @JRubyMethod(name = "eql?", required = 1)
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
        return new RubyBigDecimal(getRuntime(),value.abs()).setResult();
    }

    @JRubyMethod(name = "ceil", optional = 1)
    public IRubyObject ceil(IRubyObject[] args) {
        throw new RuntimeException("BigDecimal#ceil is not implemented");
    }

    @JRubyMethod(name = "coerce", required = 1)
    public IRubyObject coerce(IRubyObject other) {
        IRubyObject obj;
        if(other instanceof RubyFloat) {
            obj = getRuntime().newArray(other,to_f());
        } else {
            obj = getRuntime().newArray(getVpValue(other, true),this);
        }
        return obj;
    }

    public double getDoubleValue() { return value.doubleValue(); }
    public long getLongValue() { return value.longValue(); }

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
    public IRubyObject divmod(IRubyObject arg) {
        System.err.println("unimplemented: divmod");
        // TODO: implement
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "exponent")
    public IRubyObject exponent() {
        return getRuntime().newFixnum(exp());
    }

    private int exp() {
        return value.abs().unscaledValue().toString().length() - value.abs().scale();
    }

    @JRubyMethod(name = "finite?")
    public IRubyObject finite_p() {
        System.err.println("unimplemented: finite?");
        // TODO: implement correctly
        return getRuntime().getTrue();
    }

    @JRubyMethod(name = "fix")
    public IRubyObject fix() {
        System.err.println("unimplemented: fix");
        // TODO: implement correctly
        return this;
    }

    @JRubyMethod(name = "floor", optional = 1)
    public IRubyObject floor(IRubyObject[]args) {
        throw new RuntimeException("BigDecimal#floor is not implemented");
    }
 
    @JRubyMethod(name = "frac")
    public IRubyObject frac() {
        System.err.println("unimplemented: frac");
        // TODO: implement correctly
        return this;
    }

    @JRubyMethod(name = "infinite?")
    public IRubyObject infinite_p() {
        System.err.println("unimplemented: infinite?");
        // TODO: implement correctly
        return getRuntime().getFalse();
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        StringBuffer val = new StringBuffer("#<BigDecimal:").append(Integer.toHexString(System.identityHashCode(this))).append(",");
        val.append("'").append(this.callMethod(context, MethodIndex.TO_S, "to_s")).append("'").append(",");
        int len = value.abs().unscaledValue().toString().length();
        int pow = len/4;
        val.append(len).append("(").append((pow+1)*4).append(")").append(">");
        return getRuntime().newString(val.toString());
    }

    @JRubyMethod(name = "nan?")
    public IRubyObject nan_p() {
        System.err.println("unimplemented: nan?");
        // TODO: implement correctly
        return getRuntime().getFalse();
    }

    @JRubyMethod(name = "nonzero?")
    public IRubyObject nonzero_p() {
        return value.signum() != 0 ? getRuntime().getTrue() : getRuntime().getFalse();
    }
 
    @JRubyMethod(name = "precs")
    public IRubyObject precs() {
        System.err.println("unimplemented: precs");
        // TODO: implement
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "remainder", required = 1)
    public IRubyObject remainder(IRubyObject arg) {
        System.err.println("unimplemented: remainder");
        // TODO: implement
        return this;
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
        System.err.println("unimplemented: sign");
        // TODO: implement correctly
        return getRuntime().newFixnum(value.signum());
    }

    @JRubyMethod(name = "split")
    public IRubyObject split() {
        System.err.println("unimplemented: split");
        // TODO: implement
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "sqrt", required = 1)
    public IRubyObject sqrt(IRubyObject arg) {
        System.err.println("unimplemented: sqrt");
        // TODO: implement correctly
        return new RubyBigDecimal(getRuntime(),new BigDecimal(Math.sqrt(value.doubleValue()))).setResult();
    }

    @JRubyMethod(name = "to_f")
    public IRubyObject to_f() {
        return RubyFloat.newFloat(getRuntime(),value.doubleValue());
    }

    @JRubyMethod(name = "to_i")
    public IRubyObject to_i() {
        return RubyNumeric.int2fix(getRuntime(),value.longValue());
    }

    @JRubyMethod(name = "to_int")
    public IRubyObject to_int() {
        // TODO: implement to handle infinity and stuff
        return RubyNumeric.int2fix(getRuntime(),value.longValue());
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
        return value.abs().compareTo(BigDecimal.ZERO) == 0;
    }

    private String unscaledValue() {
        return value.abs().unscaledValue().toString();
    }

    private IRubyObject engineeringValue(String arg) {
        int exponent = exp();
        int signum = value.signum();
        StringBuffer build = new StringBuffer();
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
        String values[] = value.abs().toString().split("\\.");
        String whole = "0";
        if (values.length > 0) {
            whole = values[0];
        }
        String after = "0";
        if (values.length > 1) {
            after = values[1];
        }
        int signum = value.signum();
        StringBuffer build = new StringBuffer();
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
        if (isZero()) {
          return getRuntime().newString("0.0");
        }        
        if(asEngineering(arg)) {
            return engineeringValue(arg);
        } else {
            return floatingPointValue(arg);
        }
    }

    @JRubyMethod(name = "truncate", optional = 1)
    public IRubyObject truncate(IRubyObject[] args) {
        throw new RuntimeException("BigDecimal#truncate is not implemented");
    }

    @JRubyMethod(name = "zero?")
    public IRubyObject zero_p() {
        return value.signum() == 0 ? getRuntime().getTrue() : getRuntime().getFalse();
    }
}// RubyBigdecimal
