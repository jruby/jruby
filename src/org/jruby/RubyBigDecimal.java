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

import java.math.*;

import org.jruby.runtime.Arity;

import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubyBigDecimal extends RubyNumeric {
    private static final ObjectAllocator BIGDECIMAL_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyBigDecimal(runtime, klass);
        }
    };
    
    public static RubyClass createBigDecimal(Ruby runtime) {
        RubyClass result = runtime.defineClass("BigDecimal",runtime.getClass("Numeric"), BIGDECIMAL_ALLOCATOR);

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

        runtime.getModule("Kernel").defineModuleFunction("BigDecimal",callbackFactory.getOptSingletonMethod("newBigDecimal"));
        result.getMetaClass().defineMethod("new", callbackFactory.getOptSingletonMethod("newInstance"));
        result.getMetaClass().defineFastMethod("ver", callbackFactory.getFastSingletonMethod("ver"));
        result.getMetaClass().defineMethod("_load", callbackFactory.getSingletonMethod("_load",RubyKernel.IRUBY_OBJECT));
        result.getMetaClass().defineFastMethod("double_fig", callbackFactory.getFastSingletonMethod("double_fig"));
        result.getMetaClass().defineFastMethod("limit", callbackFactory.getFastSingletonMethod("limit", RubyKernel.IRUBY_OBJECT));
        result.getMetaClass().defineFastMethod("mode", callbackFactory.getFastSingletonMethod("mode", RubyKernel.IRUBY_OBJECT, RubyKernel.IRUBY_OBJECT));

        result.defineMethod("initialize", callbackFactory.getOptMethod("initialize"));
        result.defineFastMethod("%", callbackFactory.getFastMethod("mod",RubyKernel.IRUBY_OBJECT));
        result.defineFastMethod("modulo", callbackFactory.getFastMethod("mod",RubyKernel.IRUBY_OBJECT));
        result.defineFastMethod("*", callbackFactory.getFastOptMethod("mult"));
        result.defineFastMethod("mult", callbackFactory.getFastOptMethod("mult"));
        result.defineFastMethod("**", callbackFactory.getFastMethod("power",RubyKernel.IRUBY_OBJECT));
        result.defineFastMethod("power", callbackFactory.getFastMethod("power",RubyKernel.IRUBY_OBJECT));
        result.defineFastMethod("+", callbackFactory.getFastOptMethod("add"));
        result.defineFastMethod("add", callbackFactory.getFastOptMethod("add"));
        result.defineFastMethod("-", callbackFactory.getFastOptMethod("sub"));
        result.defineFastMethod("sub", callbackFactory.getFastOptMethod("sub"));
        result.defineFastMethod("/", callbackFactory.getFastOptMethod("div"));
        result.defineFastMethod("div", callbackFactory.getFastOptMethod("div"));
        result.defineFastMethod("quo", callbackFactory.getFastOptMethod("div"));
        result.defineFastMethod("<=>", callbackFactory.getFastMethod("spaceship",RubyKernel.IRUBY_OBJECT));
        result.defineFastMethod("==", callbackFactory.getFastMethod("eql_p",RubyKernel.IRUBY_OBJECT));
        result.defineFastMethod("===", callbackFactory.getFastMethod("eql_p",RubyKernel.IRUBY_OBJECT));
        result.defineFastMethod("eql?", callbackFactory.getFastMethod("eql_p",RubyKernel.IRUBY_OBJECT));
        result.defineFastMethod("!=", callbackFactory.getFastMethod("ne",RubyKernel.IRUBY_OBJECT));
        result.defineFastMethod("<", callbackFactory.getFastMethod("lt",RubyKernel.IRUBY_OBJECT));
        result.defineFastMethod("<=", callbackFactory.getFastMethod("le",RubyKernel.IRUBY_OBJECT));
        result.defineFastMethod(">", callbackFactory.getFastMethod("gt",RubyKernel.IRUBY_OBJECT));
        result.defineFastMethod(">=", callbackFactory.getFastMethod("ge",RubyKernel.IRUBY_OBJECT));
        result.defineFastMethod("abs", callbackFactory.getFastMethod("abs"));
        result.defineFastMethod("ceil", callbackFactory.getFastMethod("ceil",RubyKernel.IRUBY_OBJECT));
        result.defineFastMethod("coerce", callbackFactory.getFastMethod("coerce",RubyKernel.IRUBY_OBJECT));
        result.defineFastMethod("divmod", callbackFactory.getFastMethod("divmod",RubyKernel.IRUBY_OBJECT)); 
        result.defineFastMethod("exponent", callbackFactory.getFastMethod("exponent"));
        result.defineFastMethod("finite?", callbackFactory.getFastMethod("finite_p"));
        result.defineFastMethod("fix", callbackFactory.getFastMethod("fix"));
        result.defineFastMethod("floor", callbackFactory.getFastMethod("floor",RubyKernel.IRUBY_OBJECT));
        result.defineFastMethod("frac", callbackFactory.getFastMethod("frac"));
        result.defineFastMethod("infinite?", callbackFactory.getFastMethod("infinite_p"));
        result.defineFastMethod("inspect", callbackFactory.getFastMethod("inspect"));
        result.defineFastMethod("nan?", callbackFactory.getFastMethod("nan_p"));
        result.defineFastMethod("nonzero?", callbackFactory.getFastMethod("nonzero_p"));
        result.defineFastMethod("precs", callbackFactory.getFastMethod("precs"));
        result.defineFastMethod("remainder", callbackFactory.getFastMethod("remainder",RubyKernel.IRUBY_OBJECT));
        result.defineFastMethod("round", callbackFactory.getFastOptMethod("round"));
        result.defineFastMethod("sign", callbackFactory.getFastMethod("sign"));
        result.defineFastMethod("split", callbackFactory.getFastMethod("split"));
        result.defineFastMethod("sqrt", callbackFactory.getFastOptMethod("sqrt"));
        result.defineFastMethod("to_f", callbackFactory.getFastMethod("to_f"));
        result.defineFastMethod("to_i", callbackFactory.getFastMethod("to_i"));
        result.defineFastMethod("to_int", callbackFactory.getFastMethod("to_int"));
        result.defineFastMethod("to_s", callbackFactory.getFastOptMethod("to_s"));
        result.defineFastMethod("truncate", callbackFactory.getFastOptMethod("truncate"));
        result.defineFastMethod("zero?", callbackFactory.getFastMethod("zero_p"));

        result.setClassVar("VpPrecLimit", RubyFixnum.zero(runtime));

        return result;
    }

    private BigDecimal value;

    public RubyBigDecimal(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    public RubyBigDecimal(Ruby runtime, BigDecimal value) {
        super(runtime, runtime.getClass("BigDecimal"));
        this.value = value;
    }

    public static RubyBigDecimal newInstance(IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        RubyClass klass = (RubyClass)recv;
        
        RubyBigDecimal result = (RubyBigDecimal) klass.allocate();
        
        result.callInit(args, Block.NULL_BLOCK);
        
        return result;
    }

    public static RubyBigDecimal newBigDecimal(IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        return newInstance(recv.getRuntime().getClass("BigDecimal"), args, Block.NULL_BLOCK);
    }

    public static IRubyObject ver(IRubyObject recv) {
        return recv.getRuntime().newString("1.0.1");
    }

    public static IRubyObject _load(IRubyObject recv, IRubyObject p1, Block block) {
        // TODO: implement
        return recv.getRuntime().getNil();
    }

    public static IRubyObject double_fig(IRubyObject recv) {
        return recv.getRuntime().newFixnum(20);
    }
    
    public static IRubyObject limit(IRubyObject recv, IRubyObject arg1) {
        RubyModule c = (RubyModule)recv;
        IRubyObject nCur = c.getClassVar("VpPrecLimit");

        if (arg1.isNil()) {
            return nCur;
        }

        c.setClassVar("VpPrecLimit",arg1);

        return nCur;
    }

    public static IRubyObject mode(IRubyObject recv, IRubyObject mode, IRubyObject value) {
        System.err.println("unimplemented: mode");
        // TODO: implement
        return recv.getRuntime().getNil();
    }

    private RubyBigDecimal getVpValue(IRubyObject v, boolean must) {
        if(v instanceof RubyBigDecimal) {
            return (RubyBigDecimal)v;
        } else if(v instanceof RubyFixnum || v instanceof RubyBignum) {
            String s = v.toString();
            return newInstance(getRuntime().getClass("BigDecimal"),new IRubyObject[]{getRuntime().newString(s)}, Block.NULL_BLOCK);
        }
        if(must) {
            throw getRuntime().newTypeError(trueFalseNil(v.getMetaClass().getName() + " can't be coerced into BigDecimal"));
        }
        return null;
    }

    public IRubyObject initialize(IRubyObject[] args, Block unusedBlock) {
        String ss = args[0].convertToString().toString();
        
        try {
        this.value = new BigDecimal(ss);
        } catch(NumberFormatException e) {
            this.value = new BigDecimal("0");
        }

        return this;
    }

    private RubyBigDecimal setResult() {
        return setResult(0);
    }

    private RubyBigDecimal setResult(int scale) {
        int prec = RubyFixnum.fix2int(getRuntime().getClass("BigDecimal").getClassVar("VpPrecLimit"));
        int prec2 = Math.max(scale,prec);
        if(prec2 > 0 && this.value.scale() > (prec2-exp())) {
            this.value = this.value.setScale(prec2-exp(),BigDecimal.ROUND_HALF_UP);
        }
        return this;
    }

    public IRubyObject mod(IRubyObject arg) {
        System.err.println("unimplemented: mod");
        // TODO: implement
        return this;
    }

    public IRubyObject mult(IRubyObject[] args) {
        RubyBigDecimal val = getVpValue(args[0],false);
        if(val == null) {
            return callCoerced("*",args[0]);
        }

        return new RubyBigDecimal(getRuntime(),value.multiply(val.value)).setResult();
    }

    public IRubyObject power(IRubyObject arg) {
        // TODO: better implementation
        BigDecimal val = value;
        int times = RubyNumeric.fix2int(arg.convertToInteger());
        for(int i=0;i<times;i++) {
            val = val.multiply(val);
        }
        return new RubyBigDecimal(getRuntime(),val).setResult();
    }

    public IRubyObject add(IRubyObject[] args) {
        RubyBigDecimal val = getVpValue(args[0],false);
        if(val == null) {
            return callCoerced("+",args[0]);
        }
        return new RubyBigDecimal(getRuntime(),value.add(val.value)).setResult();
    }

    public IRubyObject sub(IRubyObject[] args) {
        RubyBigDecimal val = getVpValue(args[0],false);
        if(val == null) {
            return callCoerced("-",args[0]);
        }
        return new RubyBigDecimal(getRuntime(),value.subtract(val.value)).setResult();
    }

    public IRubyObject div(IRubyObject[] args) {
        int scale = 0;
        if(Arity.checkArgumentCount(getRuntime(), args,1,2) == 2) {
            scale = RubyNumeric.fix2int(args[1]);
        }

        RubyBigDecimal val = getVpValue(args[0],false);
        if(val == null) {
            return callCoerced("/",args[0]);
        }

        if(scale == 0) {
            return new RubyBigDecimal(getRuntime(),value.divide(val.value,200,BigDecimal.ROUND_HALF_UP)).setResult();
        } else {
            return new RubyBigDecimal(getRuntime(),value.divide(val.value,200,BigDecimal.ROUND_HALF_UP)).setResult(scale);
        }
    }

    private IRubyObject cmp(IRubyObject r, char op) {
        int e = 0;
        RubyBigDecimal rb = getVpValue(r,false);
        if(rb == null) {
            IRubyObject ee = callCoerced("<=>",r);
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

    public IRubyObject abs() {
        return new RubyBigDecimal(getRuntime(),value.abs()).setResult();
    }

    public IRubyObject ceil(IRubyObject arg) {
        System.err.println("unimplemented: ceil");
        // TODO: implement correctly
        return this;
    }

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

    public RubyNumeric multiplyWith(RubyInteger value) { 
        return (RubyNumeric)mult(new IRubyObject[]{value});
    }

    public RubyNumeric multiplyWith(RubyFloat value) { 
        return (RubyNumeric)mult(new IRubyObject[]{value});
    }

    public RubyNumeric multiplyWith(RubyBignum value) { 
        return (RubyNumeric)mult(new IRubyObject[]{value});
    }

    public IRubyObject divmod(IRubyObject arg) {
        System.err.println("unimplemented: divmod");
        // TODO: implement
        return getRuntime().getNil();
    }

    public IRubyObject exponent() {
        return getRuntime().newFixnum(exp());
    }

    private int exp() {
        return value.abs().unscaledValue().toString().length() - value.abs().scale();
    }

    public IRubyObject finite_p() {
        System.err.println("unimplemented: finite?");
        // TODO: implement correctly
        return getRuntime().getTrue();
    }

    public IRubyObject fix() {
        System.err.println("unimplemented: fix");
        // TODO: implement correctly
        return this;
    }

    public IRubyObject floor(IRubyObject arg) {
        System.err.println("unimplemented: floor");
        // TODO: implement correctly
        return this;
    }
 
    public IRubyObject frac() {
        System.err.println("unimplemented: frac");
        // TODO: implement correctly
        return this;
    }

    public IRubyObject infinite_p() {
        System.err.println("unimplemented: infinite?");
        // TODO: implement correctly
        return getRuntime().getFalse();
    }

    public IRubyObject inspect() {
        StringBuffer val = new StringBuffer("#<BigDecimal:").append(Integer.toHexString(System.identityHashCode(this))).append(",");
        val.append("'").append(this.callMethod(getRuntime().getCurrentContext(), MethodIndex.TO_S, "to_s")).append("'").append(",");
        int len = value.abs().unscaledValue().toString().length();
        int pow = len/4;
        val.append(len).append("(").append((pow+1)*4).append(")").append(">");
        return getRuntime().newString(val.toString());
    }

    public IRubyObject nan_p() {
        System.err.println("unimplemented: nan?");
        // TODO: implement correctly
        return getRuntime().getFalse();
    }

    public IRubyObject nonzero_p() {
        return value.signum() != 0 ? getRuntime().getTrue() : getRuntime().getFalse();
    }
 
    public IRubyObject precs() {
        System.err.println("unimplemented: precs");
        // TODO: implement
        return getRuntime().getNil();
    }

    public IRubyObject remainder(IRubyObject arg) {
        System.err.println("unimplemented: remainder");
        // TODO: implement
        return this;
    }

    public IRubyObject round(IRubyObject[] args) {
      int scale = args.length > 0 ? num2int(args[0]) : 0;
      int mode = (args.length > 1) ? javaRoundingModeFromRubyRoundingMode(args[1]) : BigDecimal.ROUND_HALF_UP;
      return new RubyBigDecimal(getRuntime(), value.setScale(scale, mode));
    }

  //this relies on the Ruby rounding enumerations == Java ones, which they (currently) all are
  private int javaRoundingModeFromRubyRoundingMode(IRubyObject arg) {
    return num2int(arg);
  }

  public IRubyObject sign() {
      System.err.println("unimplemented: sign");
      // TODO: implement correctly
      return getRuntime().newFixnum(value.signum());
  }

    public IRubyObject split() {
        System.err.println("unimplemented: split");
        // TODO: implement
        return getRuntime().getNil();
    }

    public IRubyObject sqrt(IRubyObject[] args) {
        System.err.println("unimplemented: sqrt");
        // TODO: implement correctly
        return new RubyBigDecimal(getRuntime(),new BigDecimal(Math.sqrt(value.doubleValue()))).setResult();
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

    private String removeTrailingZeroes(String in) {
        while(in.length() > 0 && in.charAt(in.length()-1)=='0') {
            in = in.substring(0,in.length()-1);
        }
        return in;
    }

    private String toSpecialString(BigDecimal abs) {
      if (abs.compareTo(BigDecimal.valueOf(0))== 0) {
        return "0.0";
      }
      //TODO: match the MRI code below!
      //TODO: refactor the overly-long branches in to_s so we can reuse the sign processing here
      return null;
//      if(VpIsNaN(a)) {
//          sprintf(psz,SZ_NaN);
//          return 1;
//      }
//
//      if(VpIsPosInf(a)) {
//          if(fPlus==1) {
//             *psz++ = ' ';
//          } else if(fPlus==2) {
//             *psz++ = '+';
//          }
//          sprintf(psz,SZ_INF);
//          return 1;
//      }
//      if(VpIsNegInf(a)) {
//          sprintf(psz,SZ_NINF);
//          return 1;
//      }
//      if(VpIsZero(a)) {
//          if(VpIsPosZero(a)) {
//              if(fPlus==1)      sprintf(psz, " 0.0");
//              else if(fPlus==2) sprintf(psz, "+0.0");
//              else              sprintf(psz, "0.0");
//          } else    sprintf(psz, "-0.0");
//          return 1;
//      }
//      return 0;

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
        BigDecimal abs = value.abs();
        String unscaled = abs.unscaledValue().toString();

        //not beautiful, but parallel to MRI's VpToSpecialString
        if (null != (out = toSpecialString(abs))) {
          return getRuntime().newString(out);
        }
        if(engineering) {
            int exponent = exp();
            int signum = value.signum();
            StringBuffer build = new StringBuffer();
            build.append(signum == -1 ? "-" : (signum == 1 ? (pos_sign ? (pos_space ? " " : "+" ) : "") : ""));
            build.append("0.");
            if(0 == groups) {
                String s = removeTrailingZeroes(unscaled);
                if("".equals(s)) {
                    build.append("0");
            } else {
                    build.append(s);
                }
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
                    System.out.println("AFTER: " + after);
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
        System.err.println("unimplemented: truncate");
        // TODO: implement
        return this;
    }

    public IRubyObject zero_p() {
        return value.signum() == 0 ? getRuntime().getTrue() : getRuntime().getFalse();
    }
}// RubyBigdecimal
