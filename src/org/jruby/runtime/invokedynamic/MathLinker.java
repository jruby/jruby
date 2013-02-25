/*
 ***** BEGIN LICENSE BLOCK *****
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

package org.jruby.runtime.invokedynamic;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyInstanceConfig;
import org.jruby.runtime.CallType;
import org.jruby.runtime.MethodIndex;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
import java.lang.invoke.SwitchPoint;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class MathLinker {
    private static final Logger LOG = LoggerFactory.getLogger("MathLinker");
    
    public static CallSite fixnumOperatorBootstrap(Lookup lookup, String name, MethodType type, long value, String file, int line) throws NoSuchMethodException, IllegalAccessException {
        String[] names = name.split(":");
        String operator = JavaNameMangler.demangleMethodName(names[1]);
        JRubyCallSite site = new JRubyCallSite(lookup, type, CallType.NORMAL, file, line, operator, false, false, true);
        
        MethodHandle target = lookup.findStatic(MathLinker.class, "fixnumOperator", 
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, JRubyCallSite.class, long.class));
        target = insertArguments(target, 3, site, value);
        
        site.setTarget(target);
        return site;
    }
    
    public static CallSite fixnumBooleanBootstrap(Lookup lookup, String name, MethodType type, long value, String file, int line) throws NoSuchMethodException, IllegalAccessException {
        String[] names = name.split(":");
        String operator = JavaNameMangler.demangleMethodName(names[1]);
        JRubyCallSite site = new JRubyCallSite(lookup, type, CallType.NORMAL, file, line, operator, false, false, true);
        
        MethodHandle target = lookup.findStatic(MathLinker.class, "fixnumBoolean", 
                methodType(boolean.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, JRubyCallSite.class, long.class));
        target = insertArguments(target, 3, site, value);
        
        site.setTarget(target);
        return site;
    }
    
    public static CallSite floatOperatorBootstrap(Lookup lookup, String name, MethodType type, double value, String file, int line) throws NoSuchMethodException, IllegalAccessException {
        String[] names = name.split(":");
        String operator = JavaNameMangler.demangleMethodName(names[1]);
        JRubyCallSite site = new JRubyCallSite(lookup, type, CallType.NORMAL, file, line, operator, false, false, true);
        
        MethodHandle target = lookup.findStatic(MathLinker.class, "floatOperator", 
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, JRubyCallSite.class, double.class));
        target = insertArguments(target, 3, site, value);
        
        site.setTarget(target);
        return site;
    }
    
    public static IRubyObject fixnumOperator(ThreadContext context, IRubyObject caller, IRubyObject self, JRubyCallSite site, long value) throws Throwable {
        String operator = site.name();
        String opMethod = MethodIndex.getFastFixnumOpsMethod(operator);
        String name = "fixnum_" + opMethod;
        MethodType type = methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class);
        MethodHandle target = null;
        
        if (operator.equals("+") || operator.equals("-")) {
            if (value == 1) {
                name += "_one";
                target = lookup().findStatic(MathLinker.class, name, type);
            } else if (value == 2) {
                name += "_two";
                target = lookup().findStatic(MathLinker.class, name, type);
            }
        }
        
        if (target == null) {
            type = type.insertParameterTypes(3, long.class);
            target = lookup().findStatic(MathLinker.class, name, type);
            target = insertArguments(target, 3, value);
        }

        MethodHandle fallback = lookup().findStatic(MathLinker.class, "fixnumOperatorFail", 
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, JRubyCallSite.class, RubyFixnum.class));
        fallback = insertArguments(fallback, 3, site, context.runtime.newFixnum(value));
        
        MethodHandle test = lookup().findStatic(MathLinker.class, "fixnumTest", methodType(boolean.class, IRubyObject.class));
        test = permuteArguments(test, methodType(boolean.class, ThreadContext.class, IRubyObject.class, IRubyObject.class), new int[] {2});
        
        if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(name + "\tFixnum operation at site #" + site.siteID() + " (" + site.file() + ":" + site.line() + ") bound directly");
        
        // confirm it's a Fixnum
        target = guardWithTest(test, target, fallback);
        
        // check Fixnum reopening
        target = ((SwitchPoint)context.runtime.getFixnumInvalidator().getData())
                .guardWithTest(target, fallback);
        
        site.setTarget(target);
        
        return (IRubyObject)target.invokeWithArguments(context, caller, self);
    }
    
    public static boolean fixnumBoolean(ThreadContext context, IRubyObject caller, IRubyObject self, JRubyCallSite site, long value) throws Throwable {
        String operator = site.name();
        String opMethod = MethodIndex.getFastFixnumOpsMethod(operator);
        String name = "fixnum_boolean_" + opMethod;
        MethodType type = methodType(boolean.class, ThreadContext.class, IRubyObject.class, IRubyObject.class);
        MethodHandle target = null;
        
        if (target == null) {
            type = type.insertParameterTypes(3, long.class);
            target = lookup().findStatic(MathLinker.class, name, type);
            target = insertArguments(target, 3, value);
        }

        MethodHandle fallback = lookup().findStatic(MathLinker.class, "fixnumBooleanFail", 
                methodType(boolean.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, JRubyCallSite.class, RubyFixnum.class));
        fallback = insertArguments(fallback, 3, site, context.runtime.newFixnum(value));
        
        MethodHandle test = lookup().findStatic(MathLinker.class, "fixnumTest", methodType(boolean.class, IRubyObject.class));
        test = permuteArguments(test, methodType(boolean.class, ThreadContext.class, IRubyObject.class, IRubyObject.class), new int[] {2});
        
        if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(name + "\tFixnum boolean operation at site #" + site.siteID() + " (" + site.file() + ":" + site.line() + ") bound directly");
        
        // confirm it's a Fixnum
        target = guardWithTest(test, target, fallback);
        
        // check Fixnum reopening
        target = ((SwitchPoint)context.runtime.getFixnumInvalidator().getData())
                .guardWithTest(target, fallback);
        
        site.setTarget(target);
        
        return (Boolean)target.invokeWithArguments(context, caller, self);
    }
    
    public static boolean fixnumTest(IRubyObject self) {
        return self instanceof RubyFixnum;
    }
    
    public static IRubyObject fixnumOperatorFail(ThreadContext context, IRubyObject caller, IRubyObject self, JRubyCallSite site, RubyFixnum value) throws Throwable {
        String operator = site.name();
        RubyClass selfClass = InvokeDynamicSupport.pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, operator, value);
        } else {
            entry = selfClass.searchWithCache(operator);
            if (InvokeDynamicSupport.methodMissing(entry, site.callType(), operator, caller)) {
                return InvokeDynamicSupport.callMethodMissing(entry, site.callType(), context, self, operator, value);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, operator, value);
        }
    }
    
    public static boolean fixnumBooleanFail(ThreadContext context, IRubyObject caller, IRubyObject self, JRubyCallSite site, RubyFixnum value) throws Throwable {
        return fixnumOperatorFail(context, caller, self, site, value).isTrue();
    }

    public static IRubyObject fixnum_op_plus(ThreadContext context, IRubyObject caller, IRubyObject self, long value) throws Throwable {
        return ((RubyFixnum)self).op_plus(context, value);
    }

    public static IRubyObject fixnum_op_minus(ThreadContext context, IRubyObject caller, IRubyObject self, long value) throws Throwable {
        return ((RubyFixnum)self).op_minus(context, value);
    }

    public static IRubyObject fixnum_op_mul(ThreadContext context, IRubyObject caller, IRubyObject self, long value) throws Throwable {
        return ((RubyFixnum)self).op_mul(context, value);
    }

    public static IRubyObject fixnum_op_equal(ThreadContext context, IRubyObject caller, IRubyObject self, long value) throws Throwable {
        return ((RubyFixnum)self).op_equal(context, value);
    }

    public static IRubyObject fixnum_op_lt(ThreadContext context, IRubyObject caller, IRubyObject self, long value) throws Throwable {
        return ((RubyFixnum)self).op_lt(context, value);
    }

    public static IRubyObject fixnum_op_le(ThreadContext context, IRubyObject caller, IRubyObject self, long value) throws Throwable {
        return ((RubyFixnum)self).op_le(context, value);
    }

    public static IRubyObject fixnum_op_gt(ThreadContext context, IRubyObject caller, IRubyObject self, long value) throws Throwable {
        return ((RubyFixnum)self).op_gt(context, value);
    }

    public static IRubyObject fixnum_op_ge(ThreadContext context, IRubyObject caller, IRubyObject self, long value) throws Throwable {
        return ((RubyFixnum)self).op_ge(context, value);
    }

    public static boolean fixnum_boolean_op_equal(ThreadContext context, IRubyObject caller, IRubyObject self, long value) throws Throwable {
        return ((RubyFixnum)self).op_equal_boolean(context, value);
    }

    public static boolean fixnum_boolean_op_lt(ThreadContext context, IRubyObject caller, IRubyObject self, long value) throws Throwable {
        return ((RubyFixnum)self).op_lt_boolean(context, value);
    }

    public static boolean fixnum_boolean_op_le(ThreadContext context, IRubyObject caller, IRubyObject self, long value) throws Throwable {
        return ((RubyFixnum)self).op_le_boolean(context, value);
    }

    public static boolean fixnum_boolean_op_gt(ThreadContext context, IRubyObject caller, IRubyObject self, long value) throws Throwable {
        return ((RubyFixnum)self).op_gt_boolean(context, value);
    }

    public static boolean fixnum_boolean_op_ge(ThreadContext context, IRubyObject caller, IRubyObject self, long value) throws Throwable {
        return ((RubyFixnum)self).op_ge_boolean(context, value);
    }

    public static IRubyObject fixnum_op_cmp(ThreadContext context, IRubyObject caller, IRubyObject self, long value) throws Throwable {
        return ((RubyFixnum)self).op_cmp(context, value);
    }

    public static IRubyObject fixnum_op_and(ThreadContext context, IRubyObject caller, IRubyObject self, long value) throws Throwable {
        return ((RubyFixnum)self).op_and(context, value);
    }

    public static IRubyObject fixnum_op_or(ThreadContext context, IRubyObject caller, IRubyObject self, long value) throws Throwable {
        return ((RubyFixnum)self).op_or(context, value);
    }

    public static IRubyObject fixnum_op_xor(ThreadContext context, IRubyObject caller, IRubyObject self, long value) throws Throwable {
        return ((RubyFixnum)self).op_xor(context, value);
    }

    public static IRubyObject fixnum_op_rshift(ThreadContext context, IRubyObject caller, IRubyObject self, long value) throws Throwable {
        return ((RubyFixnum)self).op_rshift(value);
    }

    public static IRubyObject fixnum_op_lshift(ThreadContext context, IRubyObject caller, IRubyObject self, long value) throws Throwable {
        return ((RubyFixnum)self).op_lshift(value);
    }

    public static IRubyObject fixnum_op_plus_one(ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        return ((RubyFixnum)self).op_plus_one(context);
    }

    public static IRubyObject fixnum_op_minus_one(ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        return ((RubyFixnum)self).op_minus_one(context);
    }

    public static IRubyObject fixnum_op_plus_two(ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        return ((RubyFixnum)self).op_plus_two(context);
    }

    public static IRubyObject fixnum_op_minus_two(ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        return ((RubyFixnum)self).op_minus_two(context);
    }
    
    public static IRubyObject floatOperator(ThreadContext context, IRubyObject caller, IRubyObject self, JRubyCallSite site, double value) throws Throwable {
        String operator = site.name();
        String opMethod = MethodIndex.getFastFloatOpsMethod(operator);
        String name = "float_" + opMethod;
        MethodType type = methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class);
        MethodHandle target = null;
        
        if (target == null) {
            type = type.insertParameterTypes(3, double.class);
            target = lookup().findStatic(MathLinker.class, name, type);
            target = insertArguments(target, 3, value);
        }

        MethodHandle fallback = lookup().findStatic(MathLinker.class, "floatOperatorFail", 
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, JRubyCallSite.class, RubyFloat.class));
        fallback = insertArguments(fallback, 3, site, context.runtime.newFloat(value));
        
        MethodHandle test = lookup().findStatic(MathLinker.class, "floatTest", methodType(boolean.class, IRubyObject.class));
        test = permuteArguments(test, methodType(boolean.class, ThreadContext.class, IRubyObject.class, IRubyObject.class), new int[] {2});
        
        if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(name + "\tFloat operation at site #" + site.siteID() + " (" + site.file() + ":" + site.line() + ") bound directly");
        site.setTarget(guardWithTest(test, target, fallback));
        
        // confirm it's a Float
        target = guardWithTest(test, target, fallback);
        
        // check Float reopening
        target = ((SwitchPoint)context.runtime.getFloatInvalidator().getData())
                .guardWithTest(target, fallback);
        
        site.setTarget(target);
        
        return (IRubyObject)target.invokeWithArguments(context, caller, self);
    }
    
    public static boolean floatTest(IRubyObject self) {
        return self instanceof RubyFloat;
    }
    
    public static IRubyObject floatOperatorFail(ThreadContext context, IRubyObject caller, IRubyObject self, JRubyCallSite site, RubyFloat value) throws Throwable {
        String operator = site.name();
        RubyClass selfClass = InvokeDynamicSupport.pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, operator, value);
        } else {
            entry = selfClass.searchWithCache(operator);
            if (InvokeDynamicSupport.methodMissing(entry, site.callType(), operator, caller)) {
                return InvokeDynamicSupport.callMethodMissing(entry, site.callType(), context, self, operator, value);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, operator, value);
        }
    }

    public static IRubyObject float_op_plus(ThreadContext context, IRubyObject caller, IRubyObject self, double value) throws Throwable {
        return ((RubyFloat)self).op_plus(context, value);
    }

    public static IRubyObject float_op_minus(ThreadContext context, IRubyObject caller, IRubyObject self, double value) throws Throwable {
        return ((RubyFloat)self).op_minus(context, value);
    }

    public static IRubyObject float_op_mul(ThreadContext context, IRubyObject caller, IRubyObject self, double value) throws Throwable {
        return ((RubyFloat)self).op_mul(context, value);
    }

    public static IRubyObject float_op_equal(ThreadContext context, IRubyObject caller, IRubyObject self, double value) throws Throwable {
        return ((RubyFloat)self).op_equal(context, value);
    }

    public static IRubyObject float_op_lt(ThreadContext context, IRubyObject caller, IRubyObject self, double value) throws Throwable {
        return ((RubyFloat)self).op_lt(context, value);
    }

    public static IRubyObject float_op_le(ThreadContext context, IRubyObject caller, IRubyObject self, double value) throws Throwable {
        return ((RubyFloat)self).op_le(context, value);
    }

    public static IRubyObject float_op_gt(ThreadContext context, IRubyObject caller, IRubyObject self, double value) throws Throwable {
        return ((RubyFloat)self).op_gt(context, value);
    }

    public static IRubyObject float_op_ge(ThreadContext context, IRubyObject caller, IRubyObject self, double value) throws Throwable {
        return ((RubyFloat)self).op_ge(context, value);
    }

    public static IRubyObject float_op_cmp(ThreadContext context, IRubyObject caller, IRubyObject self, double value) throws Throwable {
        return ((RubyFloat)self).op_cmp(context, value);
    }
}
