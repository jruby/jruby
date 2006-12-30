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
/**
 * $Id: $
 */
package org.jruby;

import java.math.BigDecimal;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 * @version $Revision: 1.2 $
 */
public class RubyBigDecimal extends RubyNumeric {
    public static RubyClass createBigDecimal(IRuby runtime) {
        RubyClass result = runtime.defineClass("BigDecimal",runtime.getClass("Numeric"));

        result.setConstant("ROUND_DOWN",RubyNumeric.int2fix(runtime,BigDecimal.ROUND_DOWN));
        result.setConstant("SIGN_POSITIVE_INFINITE",RubyNumeric.int2fix(runtime,3));
        result.setConstant("EXCEPTION_OVERFLOW",RubyNumeric.int2fix(runtime,1));
        result.setConstant("SIGN_POSITIVE_ZERO",RubyNumeric.int2fix(runtime,1));
        result.setConstant("EXCEPTION_ALL",RubyNumeric.int2fix(runtime,255));
        result.setConstant("ROUND_CEILING",RubyNumeric.int2fix(runtime,BigDecimal.ROUND_CEILING));
        result.setConstant("ROUND_UP",RubyNumeric.int2fix(runtime,BigDecimal.ROUND_UP));
        result.setConstant("SIGN_NEGATIVE_FINITE",RubyNumeric.int2fix(runtime,-2));
        result.setConstant("EXCEPTION_UNDERFLOW",RubyNumeric.int2fix(runtime, 4));
        result.setConstant("SIGN_NaN",RubyNumeric.int2fix(runtime, 0));
        result.setConstant("BASE",RubyNumeric.int2fix(runtime,10000));
        result.setConstant("ROUND_HALF_DOWN",RubyNumeric.int2fix(runtime,BigDecimal.ROUND_HALF_DOWN));
        result.setConstant("ROUND_MODE",RubyNumeric.int2fix(runtime,256));
        result.setConstant("SIGN_POSITIVE_FINITE",RubyNumeric.int2fix(runtime,2));
        result.setConstant("EXCEPTION_INFINITY",RubyNumeric.int2fix(runtime,1));
        result.setConstant("ROUND_HALF_EVEN",RubyNumeric.int2fix(runtime,BigDecimal.ROUND_HALF_EVEN));
        result.setConstant("ROUND_HALF_UP",RubyNumeric.int2fix(runtime,BigDecimal.ROUND_HALF_UP));
        result.setConstant("SIGN_NEGATIVE_INFINITE",RubyNumeric.int2fix(runtime,-3));
        result.setConstant("EXCEPTION_ZERODIVIDE",RubyNumeric.int2fix(runtime,1));
        result.setConstant("SIGN_NEGATIVE_ZERO",RubyNumeric.int2fix(runtime,-1));
        result.setConstant("EXCEPTION_NaN",RubyNumeric.int2fix(runtime,2));
        result.setConstant("ROUND_FLOOR",RubyNumeric.int2fix(runtime,BigDecimal.ROUND_FLOOR));

        CallbackFactory callbackFactory = runtime.callbackFactory(RubyBigDecimal.class);

        runtime.getModule("Kernel").defineModuleFunction("BigDecimal",callbackFactory.getOptSingletonMethod("newCreate"));
        result.defineFastSingletonMethod("new", callbackFactory.getOptSingletonMethod("newCreate"));
        result.defineFastSingletonMethod("ver", callbackFactory.getSingletonMethod("ver"));
        result.defineSingletonMethod("_load", callbackFactory.getSingletonMethod("_load",IRubyObject.class));
        result.defineFastSingletonMethod("double_fig", callbackFactory.getSingletonMethod("double_fig"));
        result.defineFastSingletonMethod("limit", callbackFactory.getSingletonMethod("limit",RubyFixnum.class));
        result.defineFastSingletonMethod("mode", callbackFactory.getSingletonMethod("mode",RubyFixnum.class,RubyFixnum.class));

        result.defineMethod("initialize", callbackFactory.getOptMethod("initialize"));
        result.defineFastMethod("%", callbackFactory.getMethod("mod",IRubyObject.class));
        result.defineFastMethod("modulo", callbackFactory.getMethod("mod",IRubyObject.class));
        result.defineFastMethod("*", callbackFactory.getOptMethod("mult"));
        result.defineFastMethod("mult", callbackFactory.getOptMethod("mult"));
        result.defineFastMethod("**", callbackFactory.getMethod("power",RubyInteger.class));
        result.defineFastMethod("power", callbackFactory.getMethod("power",RubyInteger.class));
        result.defineFastMethod("+", callbackFactory.getOptMethod("add"));
        result.defineFastMethod("add", callbackFactory.getOptMethod("add"));
        result.defineFastMethod("-", callbackFactory.getOptMethod("sub"));
        result.defineFastMethod("sub", callbackFactory.getOptMethod("sub"));
        result.defineFastMethod("/", callbackFactory.getOptMethod("div"));
        result.defineFastMethod("div", callbackFactory.getOptMethod("div"));
        result.defineFastMethod("quo", callbackFactory.getOptMethod("div"));
        result.defineFastMethod("<=>", callbackFactory.getMethod("spaceship",IRubyObject.class));
        result.defineFastMethod("==", callbackFactory.getMethod("eql_p",IRubyObject.class));
        result.defineFastMethod("===", callbackFactory.getMethod("eql_p",IRubyObject.class));
        result.defineFastMethod("eql?", callbackFactory.getMethod("eql_p",IRubyObject.class));
        result.defineFastMethod("!=", callbackFactory.getMethod("ne",IRubyObject.class));
        result.defineFastMethod("<", callbackFactory.getMethod("lt",IRubyObject.class));
        result.defineFastMethod("<=", callbackFactory.getMethod("le",IRubyObject.class));
        result.defineFastMethod(">", callbackFactory.getMethod("gt",IRubyObject.class));
        result.defineFastMethod(">=", callbackFactory.getMethod("ge",IRubyObject.class));
        result.defineFastMethod("abs", callbackFactory.getMethod("abs"));
        result.defineFastMethod("ceil", callbackFactory.getMethod("ceil",RubyInteger.class));
        result.defineFastMethod("coerce", callbackFactory.getMethod("coerce",IRubyObject.class));
        result.defineFastMethod("divmod", callbackFactory.getMethod("divmod",IRubyObject.class)); 
        result.defineFastMethod("exponent", callbackFactory.getMethod("exponent"));
        result.defineFastMethod("finite?", callbackFactory.getMethod("finite_p"));
        result.defineFastMethod("fix", callbackFactory.getMethod("fix"));
        result.defineFastMethod("floor", callbackFactory.getMethod("floor",RubyInteger.class));
        result.defineFastMethod("frac", callbackFactory.getMethod("frac"));
        result.defineFastMethod("infinite?", callbackFactory.getMethod("infinite_p"));
        result.defineFastMethod("inspect", callbackFactory.getMethod("inspect"));
        result.defineFastMethod("nan?", callbackFactory.getMethod("nan_p"));
        result.defineFastMethod("nonzero?", callbackFactory.getMethod("nonzero_p"));
        result.defineFastMethod("precs", callbackFactory.getMethod("precs"));
        result.defineFastMethod("remainder", callbackFactory.getMethod("remainder",IRubyObject.class));
        result.defineFastMethod("round", callbackFactory.getOptMethod("round"));
        result.defineFastMethod("sign", callbackFactory.getMethod("sign"));
        result.defineFastMethod("split", callbackFactory.getMethod("split"));
        result.defineFastMethod("sqrt", callbackFactory.getOptMethod("sqrt"));
        result.defineFastMethod("to_f", callbackFactory.getMethod("to_f"));
        result.defineFastMethod("to_i", callbackFactory.getMethod("to_i"));
        result.defineFastMethod("to_int", callbackFactory.getMethod("to_int"));
        result.defineFastMethod("to_s", callbackFactory.getOptMethod("to_s"));
        result.defineFastMethod("truncate", callbackFactory.getOptMethod("truncate"));
        result.defineFastMethod("zero?", callbackFactory.getMethod("zero_p"));

        return result;
    }

    private BigDecimal value;

    public RubyBigDecimal(IRuby runtime) {
        this(runtime, new BigDecimal("0"));
    }

    public RubyBigDecimal(IRuby runtime, BigDecimal value) {
        super(runtime,runtime.getClass("BigDecimal"));
        this.value = value;
    }

    public static RubyBigDecimal newCreate(IRubyObject recv, IRubyObject[] args) {
        RubyBigDecimal result = new RubyBigDecimal(recv.getRuntime());
        result.callInit(args);
        return result;
    }

    public static IRubyObject ver(IRubyObject recv) {
        return recv.getRuntime().newString("1.0.1");
    }

    public static IRubyObject _load(IRubyObject recv, IRubyObject p1) {
        // TODO: implement
        return null;
    }

    public static IRubyObject double_fig(IRubyObject recv) {
        return recv.getRuntime().newFixnum(20);
    }
    
    public static IRubyObject limit(IRubyObject recv, RubyFixnum p1) {
        // TODO: implement
        return null;
    }

    public static IRubyObject mode(IRubyObject recv, RubyFixnum mode, RubyFixnum value) {
        // TODO: implement
        return null;
    }

    public IRubyObject initialize(IRubyObject[] args) {
        String ss = args[0].toString();
        if(ss.trim().equals("")) {
            ss = "0";
        }
        this.value = new BigDecimal(ss);
        return this;
    }

    public IRubyObject mod(IRubyObject arg) {
        // TODO: implement
        return this;
    }

    public IRubyObject mult(IRubyObject[] args) {
        // TODO: better implementation
        RubyBigDecimal val = null;
        if(args[0] instanceof RubyBigDecimal) {
            val = (RubyBigDecimal)args[0];
        } else {
            val = (RubyBigDecimal)args[0].callMethod(getRuntime().getCurrentContext(), "to_d");
        }
        return new RubyBigDecimal(getRuntime(),value.multiply(val.value));
    }

    public IRubyObject power(RubyInteger arg) {
        // TODO: MUCH better implementation
        BigDecimal val = value;
        for(int i=0,j=RubyNumeric.fix2int(arg);i<j;i++) {
            val = val.multiply(val);
        }
        return new RubyBigDecimal(getRuntime(),val);
    }

    public IRubyObject add(IRubyObject[] args) {
        // TODO: better implementation
        RubyBigDecimal val = null;
        if(args[0] instanceof RubyBigDecimal) {
            val = (RubyBigDecimal)args[0];
        } else {
            val = (RubyBigDecimal)args[0].callMethod(getRuntime().getCurrentContext(), "to_d");
        }
        return new RubyBigDecimal(getRuntime(),value.add(val.value));
    }

    public IRubyObject sub(IRubyObject[] args) {
        // TODO: better implementation
        RubyBigDecimal val = null;
        if(args[0] instanceof RubyBigDecimal) {
            val = (RubyBigDecimal)args[0];
        } else {
            val = (RubyBigDecimal)args[0].callMethod(getRuntime().getCurrentContext(), "to_d");
        }
        return new RubyBigDecimal(getRuntime(),value.subtract(val.value));
    }

    public IRubyObject div(IRubyObject[] args) {
        // TODO: better implementation
        RubyBigDecimal val = null;
        if(args[0] instanceof RubyBigDecimal) {
            val = (RubyBigDecimal)args[0];
        } else {
            val = (RubyBigDecimal)args[0].callMethod(getRuntime().getCurrentContext(), "to_d");
        }
        return new RubyBigDecimal(getRuntime(),value.divide(val.value,BigDecimal.ROUND_HALF_EVEN));
    }

    private IRubyObject cmp(IRubyObject r, char op) {
        int e = 0;
        if(!(r instanceof RubyBigDecimal)) {
            e = RubyNumeric.fix2int(callCoerced("<=>",r));
        } else {
            RubyBigDecimal rb = (RubyBigDecimal)r;
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

    public IRubyObject spaceship(IRubyObject arg) {
        return cmp(arg,'*');
    }

    public IRubyObject eql_p(IRubyObject arg) {
        return cmp(arg,'=');
    }

    public IRubyObject ne(IRubyObject arg) {
        return cmp(arg,'!');
    }

    public IRubyObject lt(IRubyObject arg) {
        return cmp(arg,'<');
    }

    public IRubyObject le(IRubyObject arg) {
        return cmp(arg,'L');
    }

    public IRubyObject gt(IRubyObject arg) {
        return cmp(arg,'>');
    }

    public IRubyObject ge(IRubyObject arg) {
        return cmp(arg,'G');
    }

    public RubyNumeric abs() {
        return new RubyBigDecimal(getRuntime(),value.abs());
    }

    public IRubyObject ceil(RubyInteger arg) {
        // TODO: implement correctly
        return this;
    }

    public IRubyObject coerce(IRubyObject other) {
        IRubyObject obj;
        if(other instanceof RubyFloat) {
            obj = getRuntime().newArray(other,to_f());
        } else {
            obj = getRuntime().newArray(newCreate(other,new IRubyObject[]{other.callMethod(getRuntime().getCurrentContext(),"to_s")}),this);
        }
        return obj;
    }

    public double getDoubleValue() { return value.doubleValue(); }
    public long getLongValue() { return value.longValue(); }

    public IRubyObject divmod(IRubyObject arg) {
        // TODO: implement
        return getRuntime().getNil();
    }

    public IRubyObject exponent() {
        BigDecimal abs = value.abs();
        String unscaled = abs.unscaledValue().toString();
        int exponent = abs.toString().indexOf('.');
        return getRuntime().newFixnum(exponent);
    }

    public IRubyObject finite_p() {
        // TODO: implement correctly
        return getRuntime().getTrue();
    }

    public IRubyObject fix() {
        // TODO: implement correctly
        return this;
    }

    public IRubyObject floor(RubyInteger arg) {
        // TODO: implement correctly
        return this;
    }
 
    public IRubyObject frac() {
        // TODO: implement correctly
        return this;
    }

    public IRubyObject infinite_p() {
        // TODO: implement correctly
        return getRuntime().getFalse();
    }

    public IRubyObject inspect() {
        StringBuffer val = new StringBuffer("#<BigDecimal:").append(Integer.toHexString(System.identityHashCode(this))).append(",");
        val.append("'").append(this.callMethod(getRuntime().getCurrentContext(), "to_s")).append("'").append(",");
        int len = value.abs().unscaledValue().toString().length();
        int pow = len/4;
        val.append(len).append("(").append((pow+1)*4).append(")").append(">");
        return getRuntime().newString(val.toString());
    }

    public IRubyObject nan_p() {
        // TODO: implement correctly
        return getRuntime().getFalse();
    }

    public IRubyObject nonzero_p() {
        return value.signum() != 0 ? getRuntime().getTrue() : getRuntime().getFalse();
    }
 
    public IRubyObject precs() {
        // TODO: implement
        return getRuntime().getNil();
    }

    public IRubyObject remainder(IRubyObject arg) {
        // TODO: implement
        return this;
    }

    public IRubyObject round(IRubyObject[] args) {
        // TODO: implement
        return this;
    }

    public IRubyObject sign() {
        // TODO: implement correctly
        return getRuntime().newFixnum(value.signum());
    }

    public IRubyObject split() {
        // TODO: implement
        return getRuntime().getNil();
    }

    public IRubyObject sqrt(IRubyObject[] args) {
        // TODO: implement correctly
        return new RubyBigDecimal(getRuntime(),new BigDecimal(Math.sqrt(value.doubleValue())));
    }

    public IRubyObject to_f() {
        return RubyFloat.newFloat(getRuntime(),value.doubleValue());
    }

    public IRubyObject to_i() {
        return RubyNumeric.int2fix(getRuntime(),value.longValue());
    }

    public IRubyObject to_int() {
        // TODO: implement to handle infinity and stuff
        return RubyNumeric.int2fix(getRuntime(),value.longValue());
    }

    public IRubyObject to_s(IRubyObject[] args) {
        boolean engineering = true;
        boolean pos_sign = false;
        boolean pos_space = false;
        int groups = 0;

        if(args.length != 0 && !args[0].isNil()) {
            String format = args[0].toString();
            int start = 0;
            int end = format.length();
            if(format.length() > 0 && format.charAt(0) == '+') {
                pos_sign = true;
                start++;
            } else if(format.length() > 0 && format.charAt(0) == ' ') {
                pos_sign = true;
                pos_space = true;
                start++;
            }
            if(format.length() > 0 && format.charAt(format.length()-1) == 'F') {
                engineering = false;
                end--;
            } else if(format.length() > 0 && format.charAt(format.length()-1) == 'E') {
                engineering = true;
                end--;
            }
            String nums = format.substring(start,end);
            if(nums.length()>0) {
                groups = Integer.parseInt(nums);
            }
        }

        String out = null;
        if(engineering) {
            BigDecimal abs = value.abs();
            String unscaled = abs.unscaledValue().toString();
            int exponent = abs.toString().indexOf('.');
            if(-1 == exponent) {
                exponent = abs.toString().length();
            }
            int signum = value.signum();
            StringBuffer build = new StringBuffer();
            build.append(signum == -1 ? "-" : (signum == 1 ? (pos_sign ? (pos_space ? " " : "+" ) : "") : ""));
            build.append("0.");
            if(0 == groups) {
                build.append(unscaled);
            } else {
                int index = 0;
                String sep = "";
                while(index < unscaled.length()) {
                    int next = index+groups;
                    if(next > unscaled.length()) {
                        next = unscaled.length();
                    }
                    build.append(sep).append(unscaled.substring(index,next));
                    sep = " ";
                    index += groups;
                }
            }
            build.append("E").append(exponent);
            out = build.toString();
        } else {
            BigDecimal abs = value.abs();
            String unscaled = abs.unscaledValue().toString();
            int ix = abs.toString().indexOf('.');
            String whole = unscaled;
            String after = null;
            if(ix != -1) {
                whole = unscaled.substring(0,ix);
                after = unscaled.substring(ix);
            }
            int signum = value.signum();
            StringBuffer build = new StringBuffer();
            build.append(signum == -1 ? "-" : (signum == 1 ? (pos_sign ? (pos_space ? " " : "+" ) : "") : ""));
            if(0 == groups) {
                build.append(whole);
                if(null != after) {
                    build.append(".").append(after);
                }
            } else {
                int index = 0;
                String sep = "";
                while(index < whole.length()) {
                    int next = index+groups;
                    if(next > whole.length()) {
                        next = whole.length();
                    }
                    build.append(sep).append(whole.substring(index,next));
                    sep = " ";
                    index += groups;
                }
                if(null != after) {
                    build.append(".");
                    index = 0;
                    sep = "";
                    while(index < after.length()) {
                        int next = index+groups;
                        if(next > after.length()) {
                            next = after.length();
                        }
                        build.append(sep).append(after.substring(index,next));
                        sep = " ";
                        index += groups;
                    }
                }
            }
            out = build.toString();
        }

        return getRuntime().newString(out);
    }

    public IRubyObject truncate(IRubyObject[] args) {
        // TODO: implement
        return this;
    }

    public RubyBoolean zero_p() {
        return value.signum() == 0 ? getRuntime().getTrue() : getRuntime().getFalse();
    }
}// RubyBigdecimal
