/*
 ***** BEGIN LICENSE BLOCK *****
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

package org.jruby.runtime.invokedynamic;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.SwitchPoint;
import java.math.BigInteger;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyEncoding;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyLocalJumpError;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.ast.executable.AbstractScript;
import org.jruby.exceptions.JumpException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.CallType;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;
import static org.jruby.util.CodegenUtils.*;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;

@SuppressWarnings("deprecation")
public class InvokeDynamicSupport {
    private static final Logger LOG = LoggerFactory.getLogger("InvokeDynamicSupport");
    
    ////////////////////////////////////////////////////////////////////////////
    // BOOTSTRAP HANDLES
    ////////////////////////////////////////////////////////////////////////////
    
    public final static String BOOTSTRAP_BARE_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class);
    public final static String BOOTSTRAP_STRING_STRING_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, String.class);
    public final static String BOOTSTRAP_STRING_STRING_INT_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, String.class, int.class);
    public final static String BOOTSTRAP_STRING_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class);
    public final static String BOOTSTRAP_STRING_CALLTYPE_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, CallType.class);
    public final static String BOOTSTRAP_LONG_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, long.class);
    public final static String BOOTSTRAP_DOUBLE_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, double.class);
    public final static String BOOTSTRAP_STRING_INT_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, int.class);
    public final static String BOOTSTRAP_STRING_LONG_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, long.class);
    public final static String BOOTSTRAP_STRING_DOUBLE_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, double.class);
    
    public static Handle getBootstrapHandle(String name, Class type, String sig) {
        return new Handle(Opcodes.H_INVOKESTATIC, p(type), name, sig);
    }
    
    public static Handle getBootstrapHandle(String name, String sig) {
        return getBootstrapHandle(name, InvokeDynamicSupport.class, sig);
    }
    
    public static Handle getInvocationHandle() {
        return getBootstrapHandle("invocationBootstrap", InvocationLinker.class, BOOTSTRAP_BARE_SIG);
    }
    
    public static Handle getConstantHandle() {
        return getBootstrapHandle("getConstantBootstrap", BOOTSTRAP_BARE_SIG);
    }
    
    public static Handle getByteListHandle() {
        return getBootstrapHandle("getByteListBootstrap", BOOTSTRAP_STRING_STRING_SIG);
    }
    
    public static Handle getRegexpHandle() {
        return getBootstrapHandle("getRegexpBootstrap", BOOTSTRAP_STRING_STRING_INT_SIG);
    }
    
    public static Handle getSymbolHandle() {
        return getBootstrapHandle("getSymbolBootstrap", BOOTSTRAP_STRING_SIG);
    }
    
    public static Handle getFixnumHandle() {
        return getBootstrapHandle("getFixnumBootstrap", BOOTSTRAP_LONG_SIG);
    }
    
    public static Handle getFloatHandle() {
        return getBootstrapHandle("getFloatBootstrap", BOOTSTRAP_DOUBLE_SIG);
    }
    
    public static Handle getStaticScopeHandle() {
        return getBootstrapHandle("getStaticScopeBootstrap", BOOTSTRAP_STRING_INT_SIG);
    }
    
    public static Handle getLoadStaticScopeHandle() {
        return getBootstrapHandle("getLoadStaticScopeBootstrap", BOOTSTRAP_BARE_SIG);
    }
    
    public static Handle getCallSiteHandle() {
        return getBootstrapHandle("getCallSiteBootstrap", BOOTSTRAP_STRING_INT_SIG);
    }
    
    public static Handle getStringHandle() {
        return getBootstrapHandle("getStringBootstrap", BOOTSTRAP_STRING_STRING_INT_SIG);
    }
    
    public static Handle getBigIntegerHandle() {
        return getBootstrapHandle("getBigIntegerBootstrap", BOOTSTRAP_STRING_SIG);
    }
    
    public static Handle getEncodingHandle() {
        return getBootstrapHandle("getEncodingBootstrap", BOOTSTRAP_STRING_SIG);
    }
    
    public static Handle getBlockBodyHandle() {
        return getBootstrapHandle("getBlockBodyBootstrap", BOOTSTRAP_STRING_SIG);
    }
    
    public static Handle getBlockBody19Handle() {
        return getBootstrapHandle("getBlockBody19Bootstrap", BOOTSTRAP_STRING_SIG);
    }
    
    public static Handle getFixnumOperatorHandle() {
        return getBootstrapHandle("fixnumOperatorBootstrap", BOOTSTRAP_STRING_LONG_SIG);
    }
    
    public static Handle getFloatOperatorHandle() {
        return getBootstrapHandle("floatOperatorBootstrap", BOOTSTRAP_STRING_DOUBLE_SIG);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // BOOTSTRAP METHODS
    ////////////////////////////////////////////////////////////////////////////
    
    public static CallSite fixnumOperatorBootstrap(Lookup lookup, String name, MethodType type, String operator, long value) throws NoSuchMethodException, IllegalAccessException {
        JRubyCallSite site = new JRubyCallSite(lookup, type, CallType.NORMAL, false, false, true);
        
        MethodHandle target = lookup.findStatic(InvokeDynamicSupport.class, "fixnumOperator", 
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, JRubyCallSite.class, String.class, long.class));
        target = insertArguments(target, 3, site, operator, value);
        
        site.setTarget(target);
        return site;
    }
    
    public static CallSite floatOperatorBootstrap(Lookup lookup, String name, MethodType type, String operator, double value) throws NoSuchMethodException, IllegalAccessException {
        JRubyCallSite site = new JRubyCallSite(lookup, type, CallType.NORMAL, false, false, true);
        
        MethodHandle target = lookup.findStatic(InvokeDynamicSupport.class, "floatOperator", 
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, JRubyCallSite.class, String.class, double.class));
        target = insertArguments(target, 3, site, operator, value);
        
        site.setTarget(target);
        return site;
    }

    public static CallSite getConstantBootstrap(Lookup lookup, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
        RubyConstantCallSite site;

        site = new RubyConstantCallSite(type, name);
        
        MethodType fallbackType = type.insertParameterTypes(0, RubyConstantCallSite.class);
        MethodHandle myFallback = insertArguments(
                lookup.findStatic(InvokeDynamicSupport.class, "constantFallback",
                fallbackType),
                0,
                site);
        site.setTarget(myFallback);
        return site;
    }

    public static CallSite getByteListBootstrap(Lookup lookup, String name, MethodType type, String asString, String encodingName) {
        byte[] bytes = RuntimeHelpers.stringToRawBytes(asString);
        Encoding encoding = EncodingDB.getEncodings().get(encodingName.getBytes()).getEncoding();
        ByteList byteList = new ByteList(bytes, encoding);
        
        return new ConstantCallSite(constant(ByteList.class, byteList));
    }
    
    public static CallSite getRegexpBootstrap(Lookup lookup, String name, MethodType type, String asString, String encodingName, int options) {
        byte[] bytes = RuntimeHelpers.stringToRawBytes(asString);
        Encoding encoding = EncodingDB.getEncodings().get(encodingName.getBytes()).getEncoding();
        ByteList byteList = new ByteList(bytes, encoding);
        
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "initRegexp",
                methodType(RubyRegexp.class, MutableCallSite.class, ThreadContext.class, ByteList.class, int.class));
        init = insertArguments(init, 2, byteList, options);
        init = insertArguments(
                init,
                0,
                site);
        site.setTarget(init);
        return site;
    }
    
    public static CallSite getSymbolBootstrap(Lookup lookup, String name, MethodType type, String symbol) {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "initSymbol",
                methodType(RubySymbol.class, MutableCallSite.class, ThreadContext.class, String.class));
        init = insertArguments(init, 2, symbol);
        init = insertArguments(
                init,
                0,
                site);
        site.setTarget(init);
        return site;
    }
    
    public static CallSite getFixnumBootstrap(Lookup lookup, String name, MethodType type, long value) {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "initFixnum",
                methodType(RubyFixnum.class, MutableCallSite.class, ThreadContext.class, long.class));
        init = insertArguments(init, 2, value);
        init = insertArguments(
                init,
                0,
                site);
        site.setTarget(init);
        return site;
    }
    
    public static CallSite getFloatBootstrap(Lookup lookup, String name, MethodType type, double value) {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "initFloat",
                methodType(RubyFloat.class, MutableCallSite.class, ThreadContext.class, double.class));
        init = insertArguments(init, 2, value);
        init = insertArguments(
                init,
                0,
                site);
        site.setTarget(init);
        return site;
    }
    
    public static CallSite getStaticScopeBootstrap(Lookup lookup, String name, MethodType type, String scopeString, int index) {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "initStaticScope",
                methodType(StaticScope.class, MutableCallSite.class, AbstractScript.class, ThreadContext.class, String.class, int.class));
        init = insertArguments(init, 3, scopeString, index);
        init = insertArguments(
                init,
                0,
                site);
        site.setTarget(init);
        return site;
    }
    
    public static CallSite getLoadStaticScopeBootstrap(Lookup lookup, String name, MethodType type, int index) {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "loadStaticScope",
                methodType(StaticScope.class, MutableCallSite.class, AbstractScript.class, int.class));
        init = insertArguments(init, 2, index);
        init = insertArguments(
                init,
                0,
                site);
        site.setTarget(init);
        return site;
    }

    public static CallSite getCallSiteBootstrap(Lookup lookup, String name, MethodType type, String callName, int callTypeChar) {
        org.jruby.runtime.CallSite callSite = null;
        switch (callTypeChar) {
            case 'N':
                callSite = MethodIndex.getCallSite(callName);
                break;
            case 'F':
                callSite = MethodIndex.getFunctionalCallSite(callName);
                break;
            case 'V':
                callSite = MethodIndex.getVariableCallSite(callName);
                break;
            case 'S':
                callSite = MethodIndex.getSuperCallSite();
                break;
        }
        
        return new ConstantCallSite(constant(org.jruby.runtime.CallSite.class, callSite));
    }
    
    public static CallSite getStringBootstrap(Lookup lookup, String name, MethodType type, String asString, String encodingName, int codeRange) {
        byte[] bytes = RuntimeHelpers.stringToRawBytes(asString);
        Encoding encoding = EncodingDB.getEncodings().get(encodingName.getBytes()).getEncoding();
        ByteList byteList = new ByteList(bytes, encoding);
        
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "newString",
                methodType(RubyString.class, ThreadContext.class, ByteList.class, int.class));
        init = insertArguments(init, 1, byteList, codeRange);
        site.setTarget(init);
        return site;
    }

    public static CallSite getBigIntegerBootstrap(Lookup lookup, String name, MethodType type, String asString) {
        BigInteger byteList = new BigInteger(asString, 16);
        
        return new ConstantCallSite(constant(BigInteger.class, byteList));
    }
    
    public static CallSite getEncodingBootstrap(Lookup lookup, String name, MethodType type, String encodingName) {
        Encoding encoding = EncodingDB.getEncodings().get(encodingName.getBytes()).getEncoding();
        
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "initEncoding",
                methodType(RubyEncoding.class, MutableCallSite.class, ThreadContext.class, Encoding.class));
        init = insertArguments(init, 2, encoding);
        init = insertArguments(
                init,
                0,
                site);
        site.setTarget(init);
        return site;
    }
    
    public static CallSite getBlockBodyBootstrap(Lookup lookup, String name, MethodType type, String descriptor) {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "initBlockBody",
                methodType(BlockBody.class, MutableCallSite.class, Object.class, ThreadContext.class, String.class));
        init = insertArguments(init, 3, descriptor);
        init = insertArguments(
                init,
                0,
                site);
        site.setTarget(init);
        return site;
    }
    
    public static CallSite getBlockBody19Bootstrap(Lookup lookup, String name, MethodType type, String descriptor) {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "initBlockBody19",
                methodType(BlockBody.class, MutableCallSite.class, Object.class, ThreadContext.class, String.class));
        init = insertArguments(init, 3, descriptor);
        init = insertArguments(
                init,
                0,
                site);
        site.setTarget(init);
        return site;
    }

    ////////////////////////////////////////////////////////////////////////////
    // INITIAL AND FALLBACK METHODS FOR POST BOOTSTRAP
    ////////////////////////////////////////////////////////////////////////////
    
    public static IRubyObject fixnumOperator(ThreadContext context, IRubyObject caller, IRubyObject self, JRubyCallSite site, String operator, long value) throws Throwable {
        String opMethod = MethodIndex.getFastFixnumOpsMethod(operator);
        String name = "fixnum_" + opMethod;
        MethodType type = methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class);
        MethodHandle target = null;
        
        if (operator.equals("+") || operator.equals("-")) {
            if (value == 1) {
                name += "_one";
                target = lookup().findStatic(InvokeDynamicSupport.class, name, type);
            } else if (value == 2) {
                name += "_two";
                target = lookup().findStatic(InvokeDynamicSupport.class, name, type);
            }
        }
        
        if (target == null) {
            type = type.insertParameterTypes(3, long.class);
            target = lookup().findStatic(InvokeDynamicSupport.class, name, type);
            target = insertArguments(target, 3, value);
        }

        MethodHandle fallback = lookup().findStatic(InvokeDynamicSupport.class, "fixnumOperatorFail", 
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, JRubyCallSite.class, String.class, RubyFixnum.class));
        fallback = insertArguments(fallback, 3, site, operator, context.runtime.newFixnum(value));
        
        MethodHandle test = lookup().findStatic(InvokeDynamicSupport.class, "fixnumTest", methodType(boolean.class, Ruby.class, IRubyObject.class));
        test = test.bindTo(context.runtime);
        test = permuteArguments(test, methodType(boolean.class, ThreadContext.class, IRubyObject.class, IRubyObject.class), new int[] {2});
        
        site.setTarget(guardWithTest(test, target, fallback));
        return (IRubyObject)site.getTarget().invokeWithArguments(context, caller, self);
    }
    
    public static boolean fixnumTest(Ruby runtime, IRubyObject self) {
        return self instanceof RubyFixnum && !runtime.isFixnumReopened();
    }
    
    public static IRubyObject fixnumOperatorFail(ThreadContext context, IRubyObject caller, IRubyObject self, JRubyCallSite site, String operator, RubyFixnum value) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, operator, value);
        } else {
            entry = selfClass.searchWithCache(operator);
            if (methodMissing(entry, site.callType(), operator, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, operator, value);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, operator, value);
        }
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
    
    public static IRubyObject floatOperator(ThreadContext context, IRubyObject caller, IRubyObject self, JRubyCallSite site, String operator, double value) throws Throwable {
        String opMethod = MethodIndex.getFastFixnumOpsMethod(operator);
        String name = "float_" + opMethod;
        MethodType type = methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class);
        MethodHandle target = null;
        
        if (target == null) {
            type = type.insertParameterTypes(3, double.class);
            target = lookup().findStatic(InvokeDynamicSupport.class, name, type);
            target = insertArguments(target, 3, value);
        }

        MethodHandle fallback = lookup().findStatic(InvokeDynamicSupport.class, "floatOperatorFail", 
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, JRubyCallSite.class, String.class, RubyFloat.class));
        fallback = insertArguments(fallback, 3, site, operator, context.runtime.newFloat(value));
        
        MethodHandle test = lookup().findStatic(InvokeDynamicSupport.class, "floatTest", methodType(boolean.class, Ruby.class, IRubyObject.class));
        test = test.bindTo(context.runtime);
        test = permuteArguments(test, methodType(boolean.class, ThreadContext.class, IRubyObject.class, IRubyObject.class), new int[] {2});
        
        site.setTarget(guardWithTest(test, target, fallback));
        return (IRubyObject)site.getTarget().invokeWithArguments(context, caller, self);
    }
    
    public static boolean floatTest(Ruby runtime, IRubyObject self) {
        return self instanceof RubyFloat && !runtime.isFloatReopened();
    }
    
    public static IRubyObject floatOperatorFail(ThreadContext context, IRubyObject caller, IRubyObject self, JRubyCallSite site, String operator, RubyFloat value) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, operator, value);
        } else {
            entry = selfClass.searchWithCache(operator);
            if (methodMissing(entry, site.callType(), operator, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, operator, value);
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

    public static IRubyObject constantFallback(RubyConstantCallSite site, 
            ThreadContext context) {
        IRubyObject value = context.getConstant(site.name());
        
        if (value != null) {
            if (RubyInstanceConfig.LOG_INDY_CONSTANTS) LOG.info("constant " + site.name() + " bound directly");
            
            MethodHandle valueHandle = constant(IRubyObject.class, value);
            valueHandle = dropArguments(valueHandle, 0, ThreadContext.class);

            MethodHandle fallback = insertArguments(
                    findStatic(InvokeDynamicSupport.class, "constantFallback",
                    methodType(IRubyObject.class, RubyConstantCallSite.class, ThreadContext.class)),
                    0,
                    site);

            SwitchPoint switchPoint = (SwitchPoint)context.runtime.getConstantInvalidator().getData();
            MethodHandle gwt = switchPoint.guardWithTest(valueHandle, fallback);
            site.setTarget(gwt);
        } else {
            value = context.getCurrentScope().getStaticScope().getModule()
                    .callMethod(context, "const_missing", context.getRuntime().fastNewSymbol(site.name()));
        }
        
        return value;
    }
    
    public static RubyRegexp initRegexp(MutableCallSite site, ThreadContext context, ByteList pattern, int options) {
        RubyRegexp regexp = RubyRegexp.newRegexp(context.runtime, pattern, RegexpOptions.fromEmbeddedOptions(options));
        regexp.setLiteral();
        site.setTarget(dropArguments(constant(RubyRegexp.class, regexp), 0, ThreadContext.class));
        return regexp;
    }
    
    public static RubySymbol initSymbol(MutableCallSite site, ThreadContext context, String symbol) {
        RubySymbol rubySymbol = context.runtime.newSymbol(symbol);
        site.setTarget(dropArguments(constant(RubySymbol.class, rubySymbol), 0, ThreadContext.class));
        return rubySymbol;
    }
    
    public static RubyFixnum initFixnum(MutableCallSite site, ThreadContext context, long value) {
        RubyFixnum rubyFixnum = context.runtime.newFixnum(value);
        site.setTarget(dropArguments(constant(RubyFixnum.class, rubyFixnum), 0, ThreadContext.class));
        return rubyFixnum;
    }
    
    public static RubyFloat initFloat(MutableCallSite site, ThreadContext context, double value) {
        RubyFloat rubyFloat = context.runtime.newFloat(value);
        site.setTarget(dropArguments(constant(RubyFloat.class, rubyFloat), 0, ThreadContext.class));
        return rubyFloat;
    }
    
    public static StaticScope initStaticScope(MutableCallSite site, AbstractScript script, ThreadContext context, String staticScope, int index) {
        StaticScope scope = script.getScope(context, staticScope, index);
        site.setTarget(dropArguments(constant(StaticScope.class, scope), 0, AbstractScript.class, ThreadContext.class));
        return scope;
    }
    
    public static StaticScope loadStaticScope(MutableCallSite site, AbstractScript script, int index) {
        StaticScope scope = script.getScope(index);
        site.setTarget(dropArguments(constant(StaticScope.class, scope), 0, AbstractScript.class));
        return scope;
    }
    
    public static RubyString newString(ThreadContext context, ByteList contents, int codeRange) {
        return RubyString.newStringShared(context.runtime, contents, codeRange);
    }
    
    public static RubyEncoding initEncoding(MutableCallSite site, ThreadContext context, Encoding encoding) {
        RubyEncoding rubyEncoding = context.runtime.getEncodingService().getEncoding(encoding);
        site.setTarget(dropArguments(constant(RubyEncoding.class, rubyEncoding), 0, ThreadContext.class));
        return rubyEncoding;
    }
    
    public static BlockBody initBlockBody(MutableCallSite site, Object scriptObject, ThreadContext context, String descriptor) {
        BlockBody body = RuntimeHelpers.createCompiledBlockBody(context, scriptObject, descriptor);
        site.setTarget(dropArguments(constant(BlockBody.class, body), 0, Object.class, ThreadContext.class));
        return body;
    }
    
    public static BlockBody initBlockBody19(MutableCallSite site, Object scriptObject, ThreadContext context, String descriptor) {
        BlockBody body = RuntimeHelpers.createCompiledBlockBody19(context, scriptObject, descriptor);
        site.setTarget(dropArguments(constant(BlockBody.class, body), 0, Object.class, ThreadContext.class));
        return body;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // method_missing support code
    ////////////////////////////////////////////////////////////////////////////

    public static boolean methodMissing(CacheEntry entry, CallType callType, String name, IRubyObject caller) {
        DynamicMethod method = entry.method;
        return method.isUndefined() || (callType == CallType.NORMAL && !name.equals("method_missing") && !method.isCallableFrom(caller, callType));
    }

    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject[] args) {
        return RuntimeHelpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, args, Block.NULL_BLOCK);
    }

    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name) {
        return RuntimeHelpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, Block.NULL_BLOCK);
    }

    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, Block block) {
        return RuntimeHelpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, block);
    }

    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg) {
        return RuntimeHelpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg, Block.NULL_BLOCK);
    }

    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject[] args, Block block) {
        return RuntimeHelpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, args, block);
    }

    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg0, Block block) {
        return RuntimeHelpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg0, block);
    }

    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) {
        return RuntimeHelpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg0, arg1, Block.NULL_BLOCK);
    }

    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        return RuntimeHelpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg0, arg1, block);
    }

    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return RuntimeHelpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg0, arg1, arg2, Block.NULL_BLOCK);
    }

    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return RuntimeHelpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg0, arg1, arg2, block);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Dispatch support methods
    ////////////////////////////////////////////////////////////////////////////

    public static RubyClass pollAndGetClass(ThreadContext context, IRubyObject self) {
        context.callThreadPoll();
        RubyClass selfType = self.getMetaClass();
        return selfType;
    }

    public static IRubyObject handleBreakJump(JumpException.BreakJump bj, ThreadContext context) throws JumpException.BreakJump {
        if (context.getFrameJumpTarget() == bj.getTarget()) {
            return (IRubyObject) bj.getValue();
        }
        throw bj;
    }

    public static IRubyObject handleBreakJump(JumpException.BreakJump bj, CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) throws JumpException.BreakJump {
        if (context.getFrameJumpTarget() == bj.getTarget()) {
            return (IRubyObject) bj.getValue();
        }
        throw bj;
    }

    public static IRubyObject handleBreakJump(ThreadContext context, JumpException.BreakJump bj) throws JumpException.BreakJump {
        if (context.getFrameJumpTarget() == bj.getTarget()) {
            return (IRubyObject) bj.getValue();
        }
        throw bj;
    }

    public static IRubyObject retryJumpError(ThreadContext context) {
        throw context.getRuntime().newLocalJumpError(RubyLocalJumpError.Reason.RETRY, context.getRuntime().getNil(), "retry outside of rescue not supported");
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Utility methods for lookup
    ////////////////////////////////////////////////////////////////////////////
    
    public static MethodHandle findStatic(Class target, String name, MethodType type) {
        try {
            return lookup().findStatic(target, name, type);
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        } catch (IllegalAccessException nae) {
            throw new RuntimeException(nae);
        }
    }
    public static MethodHandle findVirtual(Class target, String name, MethodType type) {
        try {
            return lookup().findVirtual(target, name, type);
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        } catch (IllegalAccessException nae) {
            throw new RuntimeException(nae);
        }
    }
}