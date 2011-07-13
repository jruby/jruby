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
import java.util.Arrays;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyEncoding;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyLocalJumpError;
import org.jruby.RubyModule;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.exceptions.JumpException;
import org.jruby.internal.runtime.methods.AttrReaderMethod;
import org.jruby.internal.runtime.methods.AttrWriterMethod;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.CompiledMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.parser.LocalStaticScope;
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
    
    public static org.objectweb.asm.MethodHandle getBootstrapHandle(String name, String sig) {
        return new org.objectweb.asm.MethodHandle(Opcodes.MH_INVOKESTATIC, p(InvokeDynamicSupport.class), name, sig);
    }
    public static org.objectweb.asm.MethodHandle getInvocationHandle() {
        return getBootstrapHandle("invocationBootstrap", BOOTSTRAP_BARE_SIG);
    }
    
    public static org.objectweb.asm.MethodHandle getConstantHandle() {
        return getBootstrapHandle("getConstantBootstrap", BOOTSTRAP_BARE_SIG);
    }
    
    public static org.objectweb.asm.MethodHandle getByteListHandle() {
        return getBootstrapHandle("getByteListBootstrap", BOOTSTRAP_STRING_STRING_SIG);
    }
    
    public static org.objectweb.asm.MethodHandle getRegexpHandle() {
        return getBootstrapHandle("getRegexpBootstrap", BOOTSTRAP_STRING_STRING_INT_SIG);
    }
    
    public static org.objectweb.asm.MethodHandle getSymbolHandle() {
        return getBootstrapHandle("getSymbolBootstrap", BOOTSTRAP_STRING_SIG);
    }
    
    public static org.objectweb.asm.MethodHandle getFixnumHandle() {
        return getBootstrapHandle("getFixnumBootstrap", BOOTSTRAP_LONG_SIG);
    }
    
    public static org.objectweb.asm.MethodHandle getFloatHandle() {
        return getBootstrapHandle("getFloatBootstrap", BOOTSTRAP_DOUBLE_SIG);
    }
    
    public static org.objectweb.asm.MethodHandle getStaticScopeHandle() {
        return getBootstrapHandle("getStaticScopeBootstrap", BOOTSTRAP_STRING_SIG);
    }
    
    public static org.objectweb.asm.MethodHandle getCallSiteHandle() {
        return getBootstrapHandle("getCallSiteBootstrap", BOOTSTRAP_STRING_INT_SIG);
    }
    
    public static org.objectweb.asm.MethodHandle getStringHandle() {
        return getBootstrapHandle("getStringBootstrap", BOOTSTRAP_STRING_STRING_INT_SIG);
    }
    
    public static org.objectweb.asm.MethodHandle getBigIntegerHandle() {
        return getBootstrapHandle("getBigIntegerBootstrap", BOOTSTRAP_STRING_SIG);
    }
    
    public static org.objectweb.asm.MethodHandle getEncodingHandle() {
        return getBootstrapHandle("getEncodingBootstrap", BOOTSTRAP_STRING_SIG);
    }
    
    public static org.objectweb.asm.MethodHandle getBlockBodyHandle() {
        return getBootstrapHandle("getBlockBodyBootstrap", BOOTSTRAP_STRING_SIG);
    }
    
    public static org.objectweb.asm.MethodHandle getBlockBody19Handle() {
        return getBootstrapHandle("getBlockBody19Bootstrap", BOOTSTRAP_STRING_SIG);
    }
    
    public static org.objectweb.asm.MethodHandle getFixnumOperatorHandle() {
        return getBootstrapHandle("fixnumOperatorBootstrap", BOOTSTRAP_STRING_LONG_SIG);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // BOOTSTRAP METHODS
    ////////////////////////////////////////////////////////////////////////////
    
    public static CallSite invocationBootstrap(Lookup lookup, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
        CallSite site;

        if (name.equals("yieldSpecific")) {
            site = new MutableCallSite(type);
            MethodHandle target = lookup.findStatic(InvokeDynamicSupport.class, "yieldSpecificFallback", type.insertParameterTypes(0, MutableCallSite.class));
            target = insertArguments(target, 0, site);
            site.setTarget(target);
            return site;
        } else if (name.equals("call")) {
            site = new JRubyCallSite(lookup, type, CallType.NORMAL, false, false, true);
        } else if (name.equals("fcall")) {
            site = new JRubyCallSite(lookup, type, CallType.FUNCTIONAL, false, false, true);
        } else if (name.equals("callIter")) {
            site = new JRubyCallSite(lookup, type, CallType.NORMAL, false, true, true);
        } else if (name.equals("fcallIter")) {
            site = new JRubyCallSite(lookup, type, CallType.FUNCTIONAL, false, true, true);
        } else if (name.equals("attrAssign")) {
            site = new JRubyCallSite(lookup, type, CallType.NORMAL, true, false, false);
        } else if (name.equals("attrAssignSelf")) {
            site = new JRubyCallSite(lookup, type, CallType.VARIABLE, true, false, false);
        } else if (name.equals("attrAssignExpr")) {
            site = new JRubyCallSite(lookup, type, CallType.NORMAL, true, false, true);
        } else if (name.equals("attrAssignSelfExpr")) {
            site = new JRubyCallSite(lookup, type, CallType.VARIABLE, true, false, true);
        } else {
            throw new RuntimeException("wrong invokedynamic target: " + name);
        }
        
        MethodType fallbackType = type.insertParameterTypes(0, JRubyCallSite.class);
        MethodHandle myFallback = insertArguments(
                lookup.findStatic(InvokeDynamicSupport.class, "invocationFallback",
                fallbackType),
                0,
                site);
        site.setTarget(myFallback);
        return site;
    }
    
    public static CallSite fixnumOperatorBootstrap(Lookup lookup, String name, MethodType type, String operator, long value) throws NoSuchMethodException, IllegalAccessException {
        CallSite site = new JRubyCallSite(lookup, type, CallType.NORMAL, false, false, true);
        String opMethod = MethodIndex.getFastOpsMethod(operator);
        name = "fixnum_" + opMethod;
        type = type.insertParameterTypes(0, MutableCallSite.class);
        MethodHandle fallback = null;
        if (operator.equals("+") || operator.equals("-")) {
            if (value == 1) {
                name += "_one";
                fallback = lookup.findStatic(InvokeDynamicSupport.class, name, type);
                fallback = insertArguments(fallback, 0, site);
            } else if (value == 2) {
                name += "_two";
                fallback = lookup.findStatic(InvokeDynamicSupport.class, name, type);
                fallback = insertArguments(fallback, 0, site);
            }
        }
        
        if (fallback == null) {
            type = type.insertParameterTypes(0, long.class);
            fallback = lookup.findStatic(InvokeDynamicSupport.class, name, type);
            fallback = insertArguments(fallback, 0, value, site);
        }
        
        site.setTarget(fallback);
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
    
    public static CallSite getStaticScopeBootstrap(Lookup lookup, String name, MethodType type, String staticScope) {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "initStaticScope",
                methodType(StaticScope.class, MutableCallSite.class, ThreadContext.class, String.class));
        init = insertArguments(init, 2, staticScope);
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
    
    public static IRubyObject invocationFallback(JRubyCallSite site, 
            ThreadContext context,
            IRubyObject caller,
            IRubyObject self,
            String name) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        
        if (methodMissing(entry, site.callType(), name, caller)) {
            return callMethodMissing(entry, site.callType(), context, self, name);
        }
        
        MethodHandle target = getTarget(site, selfClass, name, entry, 0);
        
        if (target == null || ++site.failCount > RubyInstanceConfig.MAX_FAIL_COUNT) {
            site.setTarget(target = createFail(FAIL_0, site, name, entry.method));
        } else {
            target = postProcess(site, target);
            if (site.getTarget() != null) {
                site.setTarget(createGWT(TEST_0, target, site.getTarget(), entry, site, false));
            } else {
                site.setTarget(createGWT(TEST_0, target, FALLBACK_0, entry, site));
            }
        }

        return (IRubyObject)target.invokeWithArguments(context, caller, self, name);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            return callMethodMissing(entry, site.callType(), context, self, name, arg0);
        }
        
        MethodHandle target = getTarget(site, selfClass, name, entry, 1);
        
        if (target == null || ++site.failCount > RubyInstanceConfig.MAX_FAIL_COUNT) {
            site.setTarget(target = createFail(FAIL_1, site, name, entry.method));
        } else {
            target = postProcess(site, target);
            if (site.getTarget() != null) {
                site.setTarget(createGWT(TEST_1, target, site.getTarget(), entry, site, false));
            } else {
                site.setTarget(createGWT(TEST_1, target, FALLBACK_1, entry, site));
            }
        }

        return (IRubyObject)target.invokeWithArguments(context, caller, self, name, arg0);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1);
        }
        
        MethodHandle target = getTarget(site, selfClass, name, entry, 2);
        
        if (target == null || ++site.failCount > RubyInstanceConfig.MAX_FAIL_COUNT) {
            site.setTarget(target = createFail(FAIL_2, site, name, entry.method));
        } else {
            target = postProcess(site, target);
            if (site.getTarget() != null) {
                site.setTarget(createGWT(TEST_2, target, site.getTarget(), entry, site, false));
            } else {
                site.setTarget(createGWT(TEST_2, target, FALLBACK_2, entry, site));
            }
        }

        return (IRubyObject)target.invokeWithArguments(context, caller, self, name, arg0, arg1);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, arg2);
        }
        
        MethodHandle target = getTarget(site, selfClass, name, entry, 3);
        
        if (target == null || ++site.failCount > RubyInstanceConfig.MAX_FAIL_COUNT) {
            site.setTarget(target = createFail(FAIL_3, site, name, entry.method));
        } else {
            target = postProcess(site, target);
            if (site.getTarget() != null) {
                site.setTarget(createGWT(TEST_3, target, site.getTarget(), entry, site, false));
            } else {
                site.setTarget(createGWT(TEST_3, target, FALLBACK_3, entry, site));
            }
        }

        return (IRubyObject)target.invokeWithArguments(context, caller, self, name, arg0, arg1, arg2);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            return callMethodMissing(entry, site.callType(), context, self, name, args);
        }
        
        MethodHandle target = getTarget(site, selfClass, name, entry, -1);
        
        if (target == null || ++site.failCount > RubyInstanceConfig.MAX_FAIL_COUNT) {
            site.setTarget(target = createFail(FAIL_N, site, name, entry.method));
        } else {
            target = postProcess(site, target);
            if (site.getTarget() != null) {
                site.setTarget(createGWT(TEST_N, target, site.getTarget(), entry, site, false));
            } else {
                site.setTarget(createGWT(TEST_N, target, FALLBACK_N, entry, site));
            }
        }

        return (IRubyObject)target.invokeWithArguments(context, caller, self, name, args);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, site.callType(), name, caller)) {
            try {
                return callMethodMissing(entry, site.callType(), context, self, name, block);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                return retryJumpError(context);
            } finally {
                if (site.isIterator()) block.escape();
            }
        }

        MethodHandle target = getTarget(site, selfClass, name, entry, 0);

        if (target == null || ++site.failCount > RubyInstanceConfig.MAX_FAIL_COUNT) {
            site.setTarget(target = createFail(FAIL_0_B, site, name, entry.method));
        } else {
            target = postProcess(site, target);
            if (site.getTarget() != null) {
                site.setTarget(createGWT(TEST_0_B, target, site.getTarget(), entry, site, false));
            } else {
                site.setTarget(createGWT(TEST_0_B, target, FALLBACK_0_B, entry, site));
            }
        }

        return (IRubyObject) target.invokeWithArguments(context, caller, self, name, block);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, site.callType(), name, caller)) {
            try {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, block);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                return retryJumpError(context);
            } finally {
                if (site.isIterator()) block.escape();
            }
        }

        MethodHandle target = getTarget(site, selfClass, name, entry, 1);

        if (target == null || ++site.failCount > RubyInstanceConfig.MAX_FAIL_COUNT) {
            site.setTarget(target = createFail(FAIL_1_B, site, name, entry.method));
        } else {
            target = postProcess(site, target);
            if (site.getTarget() != null) {
                site.setTarget(createGWT(TEST_1_B, target, site.getTarget(), entry, site, false));
            } else {
                site.setTarget(createGWT(TEST_1_B, target, FALLBACK_1_B, entry, site));
            }
        }

        return (IRubyObject) target.invokeWithArguments(context, caller, self, name, arg0, block);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, site.callType(), name, caller)) {
            try {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, block);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                return retryJumpError(context);
            } finally {
                if (site.isIterator()) block.escape();
            }
        }

        MethodHandle target = getTarget(site, selfClass, name, entry, 2);

        if (target == null || ++site.failCount > RubyInstanceConfig.MAX_FAIL_COUNT) {
            site.setTarget(target = createFail(FAIL_2_B, site, name, entry.method));
        } else {
            target = postProcess(site, target);
            if (site.getTarget() != null) {
                site.setTarget(createGWT(TEST_2_B, target, site.getTarget(), entry, site, false));
            } else {
                site.setTarget(createGWT(TEST_2_B, target, FALLBACK_2_B, entry, site));
            }
        }

        return (IRubyObject) target.invokeWithArguments(context, caller, self, name, arg0, arg1, block);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, site.callType(), name, caller)) {
            try {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, arg2, block);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                return retryJumpError(context);
            } finally {
                if (site.isIterator()) block.escape();
            }
        }

        MethodHandle target = getTarget(site, selfClass, name, entry, 3);

        if (target == null || ++site.failCount > RubyInstanceConfig.MAX_FAIL_COUNT) {
            site.setTarget(target = createFail(FAIL_3_B, site, name, entry.method));
        } else {
            target = postProcess(site, target);
            if (site.getTarget() != null) {
                site.setTarget(createGWT(TEST_3_B, target, site.getTarget(), entry, site, false));
            } else {
                site.setTarget(createGWT(TEST_3_B, target, FALLBACK_3_B, entry, site));
            }
        }

        return (IRubyObject) target.invokeWithArguments(context, caller, self, name, arg0, arg1, arg2, block);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, site.callType(), name, caller)) {
            try {
                return callMethodMissing(entry, site.callType(), context, self, name, args, block);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                return retryJumpError(context);
            } finally {
                if (site.isIterator()) block.escape();
            }
        }

        MethodHandle target = getTarget(site, selfClass, name, entry, -1);

        if (target == null || ++site.failCount > RubyInstanceConfig.MAX_FAIL_COUNT) {
            site.setTarget(target = createFail(FAIL_N_B, site, name, entry.method));
        } else {
            target = postProcess(site, target);
            if (site.getTarget() != null) {
                site.setTarget(createGWT(TEST_N_B, target, site.getTarget(), entry, site, false));
            } else {
                site.setTarget(createGWT(TEST_N_B, target, FALLBACK_N_B, entry, site));
            }
        }

        return (IRubyObject) target.invokeWithArguments(context, caller, self, name, args, block);
    }

    public static IRubyObject fixnum_op_plus(long value, MutableCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        return ((RubyFixnum)self).op_plus(context, value);
    }

    public static IRubyObject fixnum_op_minus(long value, MutableCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        return ((RubyFixnum)self).op_minus(context, value);
    }

    public static IRubyObject fixnum_op_mul(long value, MutableCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        return ((RubyFixnum)self).op_mul(context, value);
    }

    public static IRubyObject fixnum_op_lt(long value, MutableCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        return ((RubyFixnum)self).op_lt(context, value);
    }

    public static IRubyObject fixnum_op_le(long value, MutableCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        return ((RubyFixnum)self).op_le(context, value);
    }

    public static IRubyObject fixnum_op_gt(long value, MutableCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        return ((RubyFixnum)self).op_gt(context, value);
    }

    public static IRubyObject fixnum_op_ge(long value, MutableCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        return ((RubyFixnum)self).op_ge(context, value);
    }

    public static IRubyObject fixnum_op_cmp(long value, MutableCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        return ((RubyFixnum)self).op_cmp(context, value);
    }

    public static IRubyObject fixnum_op_and(long value, MutableCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        return ((RubyFixnum)self).op_and(context, value);
    }

    public static IRubyObject fixnum_op_or(long value, MutableCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        return ((RubyFixnum)self).op_or(context, value);
    }

    public static IRubyObject fixnum_op_xor(long value, MutableCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        return ((RubyFixnum)self).op_xor(context, value);
    }

    public static IRubyObject fixnum_op_rshift(long value, MutableCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        return ((RubyFixnum)self).op_rshift(value);
    }

    public static IRubyObject fixnum_op_lshift(long value, MutableCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        return ((RubyFixnum)self).op_lshift(value);
    }

    public static IRubyObject fixnum_op_plus_one(MutableCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        return ((RubyFixnum)self).op_plus_one(context);
    }

    public static IRubyObject fixnum_op_minus_one(MutableCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        return ((RubyFixnum)self).op_minus_one(context);
    }

    public static IRubyObject fixnum_op_plus_two(MutableCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        return ((RubyFixnum)self).op_plus_two(context);
    }

    public static IRubyObject fixnum_op_minus_two(MutableCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self) throws Throwable {
        return ((RubyFixnum)self).op_minus_two(context);
    }
    
    public static IRubyObject yieldSpecificFallback(
            MutableCallSite site,
            Block block,
            ThreadContext context) throws Throwable {
        return block.yieldSpecific(context);
    }
    
    public static IRubyObject yieldSpecificFallback(
            MutableCallSite site,
            Block block,
            ThreadContext context,
            IRubyObject arg0) throws Throwable {
        return block.yieldSpecific(context, arg0);
    }
    
    public static IRubyObject yieldSpecificFallback(
            MutableCallSite site,
            Block block,
            ThreadContext context,
            IRubyObject arg0,
            IRubyObject arg1) throws Throwable {
        return block.yieldSpecific(context, arg0, arg1);
    }
    
    public static IRubyObject yieldSpecificFallback(
            MutableCallSite site,
            Block block,
            ThreadContext context,
            IRubyObject arg0,
            IRubyObject arg1,
            IRubyObject arg2) throws Throwable {
        return block.yieldSpecific(context, arg0, arg1, arg2);
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
    
    public static StaticScope initStaticScope(MutableCallSite site, ThreadContext context, String staticScope) {
        String[] scopeData = staticScope.split(",");
        String[] varNames = scopeData[0].split(";");
        for (int i = 0; i < varNames.length; i++) {
            varNames[i] = varNames[i].intern();
        }
        StaticScope scope = new LocalStaticScope(context.getCurrentScope().getStaticScope(), varNames);
        site.setTarget(dropArguments(constant(StaticScope.class, scope), 0, ThreadContext.class));
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

    private static MethodHandle createGWT(MethodHandle test, MethodHandle target, MethodHandle fallback, CacheEntry entry, JRubyCallSite site) {
        return createGWT(test, target, fallback, entry, site, true);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // INVOCATION SUPPORT METHODS
    ////////////////////////////////////////////////////////////////////////////

    private static MethodHandle createFail(MethodHandle fail, JRubyCallSite site, String name, DynamicMethod method) {
        if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(name + "\tbound to inline cache (failed #" + method.getSerialNumber() + ")");
        
        MethodHandle myFail = insertArguments(fail, 0, site);
        myFail = postProcess(site, myFail);
        return myFail;
    }

    private static MethodHandle createGWT(MethodHandle test, MethodHandle target, MethodHandle fallback, CacheEntry entry, JRubyCallSite site, boolean curryFallback) {
        MethodHandle myTest = insertArguments(test, 0, entry.token);
        MethodHandle myFallback = curryFallback ? insertArguments(fallback, 0, site) : fallback;
        MethodHandle guardWithTest = guardWithTest(myTest, target, myFallback);
        
        return guardWithTest;
    }
    
    private static class IndirectBindingException extends RuntimeException {
        public IndirectBindingException(String reason) {
            super(reason);
        }
    }
    
    private static MethodHandle tryDispatchDirect(JRubyCallSite site, String name, RubyClass cls, DynamicMethod method) {
        DynamicMethod.NativeCall nativeCall = method.getNativeCall();
        
        MethodType trimmed = site.type().dropParameterTypes(2, 4);
        int siteArgCount = getArgCount(trimmed.parameterArray(), true);
        
        if (method instanceof AttrReaderMethod) {
            // attr reader
            if (siteArgCount != 0) {
                throw new IndirectBindingException("attr reader with > 0 args");
            }
        } else if (method instanceof AttrWriterMethod) {
            // attr writer
            if (siteArgCount != 1) {
                throw new IndirectBindingException("attr writer with > 1 args");
            }
        } else if (nativeCall != null) {
            // has an explicit native call path
            
            // if frame/scope required, can't dispatch direct
            if (method.getCallConfig() != CallConfiguration.FrameNoneScopeNone) {
                throw new IndirectBindingException("frame or scope required");
            }
            
            if (nativeCall.isJava()) {
                // if Java, must be no-arg invocation
                if (nativeCall.getNativeSignature().length != 0 || siteArgCount != 0) {
                    throw new IndirectBindingException("Java call or receiver with > 0 args");
                }
            } else {
                // if non-Java, must:
                // * exactly match arities
                // * 3 or fewer arguments
                
                int nativeArgCount = (method instanceof CompiledMethod)
                        ? getRubyArgCount(nativeCall.getNativeSignature())
                        : getArgCount(nativeCall.getNativeSignature(), nativeCall.isStatic());
                
                // match arity and arity is not 4 (IRubyObject[].class)
                if (nativeArgCount == 4) {
                    throw new IndirectBindingException("target args > 4 or rest/optional");
                }
                
                if (nativeArgCount != siteArgCount) {
                    throw new IndirectBindingException("arity mismatch at call site");
                }
            }
        } else {
            throw new IndirectBindingException("no direct path available");
        }
        
        return handleForMethod(site, name, cls, method);
    }
    
    private static MethodHandle getTarget(JRubyCallSite site, RubyClass cls, String name, CacheEntry entry, int arity) {
        IndirectBindingException ibe;
        try {
            return tryDispatchDirect(site, name, cls, entry.method);
        } catch (IndirectBindingException _ibe) {
            ibe = _ibe;
            // proceed with indirect, if enabled
        }
        
        // if indirect indy-bound methods (via DynamicMethod.call) are disabled, bail out
        if (!RubyInstanceConfig.INVOKEDYNAMIC_INDIRECT) {
            if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(name + "\tfailed to bind to #" + entry.method.getSerialNumber() + ": " + ibe.getMessage());
            return null;
        }
        
        // no direct native path, use DynamicMethod.call
        if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(name + "\tbound indirectly to #" + entry.method.getSerialNumber() + ": " + ibe.getMessage());
        
        return insertArguments(getDynamicMethodTarget(site.type(), arity), 0, entry);
    }
    
    private static MethodHandle handleForMethod(JRubyCallSite site, String name, RubyClass cls, DynamicMethod method) {
        MethodHandle nativeTarget = null;
        
        if (method.getHandle() != null) {
            nativeTarget = (MethodHandle)method.getHandle();
        } else {
            if (method instanceof AttrReaderMethod) {
                // Ruby to attr reader
                if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(name + "\tbound as attr reader #" + method.getSerialNumber() + ":" + ((AttrReaderMethod)method).getVariableName());
                nativeTarget = createAttrReaderHandle(site, cls, method);
            } else if (method instanceof AttrWriterMethod) {
                // Ruby to attr writer
                if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(name + "\tbound as attr writer #" + method.getSerialNumber() + ":" + ((AttrWriterMethod)method).getVariableName());
                nativeTarget = createAttrWriterHandle(site, cls, method);
            } else if (method.getNativeCall() != null) {
                DynamicMethod.NativeCall nativeCall = method.getNativeCall();
                
                if (nativeCall.isJava() && RubyInstanceConfig.INVOKEDYNAMIC_JAVA) {
                    // Ruby to Java
                    if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(name + "\tbound to Java method #" + method.getSerialNumber() + ": " + nativeCall);
                    nativeTarget = createJavaHandle(method);
                } else if (method instanceof CompiledMethod) {
                    // Ruby to Ruby
                    if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(name + "\tbound to Ruby method #" + method.getSerialNumber() + ": " + nativeCall);
                    nativeTarget = createRubyHandle(site, method);
                } else {
                    // Ruby to Core
                    if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(name + "\tbound to native method #" + method.getSerialNumber() + ": " + nativeCall);
                    nativeTarget = createNativeHandle(site, method);
                }
            }
        }
                        
        // add NULL_BLOCK if needed
        if (nativeTarget != null) {
            if (
                    site.type().parameterCount() > 0
                    && site.type().parameterArray()[site.type().parameterCount() - 1] != Block.class
                    && nativeTarget.type().parameterCount() > 0
                    && nativeTarget.type().parameterType(nativeTarget.type().parameterCount() - 1) == Block.class) {
                nativeTarget = insertArguments(nativeTarget, nativeTarget.type().parameterCount() - 1, Block.NULL_BLOCK);
            } else if (
                    site.type().parameterCount() > 0
                    && site.type().parameterArray()[site.type().parameterCount() - 1] == Block.class
                    && nativeTarget.type().parameterCount() > 0
                    && nativeTarget.type().parameterType(nativeTarget.type().parameterCount() - 1) != Block.class) {
                // drop block if not used
                nativeTarget = dropArguments(nativeTarget, nativeTarget.type().parameterCount(), Block.class);
            }
        }
        
        return nativeTarget;
    }

    public static boolean test(int token, IRubyObject self) {
        return token == ((RubyBasicObject)self).getMetaClass().getCacheToken();
    }
    
    private static IRubyObject getLast(IRubyObject[] args) {
        return args[args.length - 1];
    }
    
    private static final MethodHandle BLOCK_ESCAPE = findStatic(InvokeDynamicSupport.class, "blockEscape", methodType(IRubyObject.class, IRubyObject.class, Block.class));
    protected static IRubyObject blockEscape(IRubyObject retval, Block block) {
        block.escape();
        return retval;
    }
    
    private static final MethodHandle BLOCK_ESCAPE_EXCEPTION = findStatic(InvokeDynamicSupport.class, "blockEscapeException", methodType(IRubyObject.class, Throwable.class, Block.class));
    protected static IRubyObject blockEscapeException(Throwable throwable, Block block) throws Throwable {
        block.escape();
        throw throwable;
    }
    
    private static final MethodHandle HANDLE_BREAK_JUMP = findStatic(InvokeDynamicSupport.class, "handleBreakJump", methodType(IRubyObject.class, JumpException.BreakJump.class, ThreadContext.class));
//    
//    private static IRubyObject handleRetryJump(JumpException.RetryJump bj, ThreadContext context) {
//        block.escape();
//        throw context.getRuntime().newLocalJumpError(RubyLocalJumpError.Reason.RETRY, context.getRuntime().getNil(), "retry outside of rescue not supported");
//    }
//    private static final MethodHandle HANDLE_RETRY_JUMP = findStatic(InvokeDynamicSupport.class, "handleRetryJump", methodType(IRubyObject.class, JumpException.BreakJump.class, ThreadContext.class));
    
    private static MethodHandle postProcess(JRubyCallSite site, MethodHandle target) {
        if (site.isIterator()) {
            // wrap with iter logic for break, retry, and block escape
            MethodHandle breakHandler = permuteArguments(
                    HANDLE_BREAK_JUMP,
                    site.type().insertParameterTypes(0, JumpException.BreakJump.class),
                    new int[] {0, 1});
//            MethodHandle retryHandler = permuteArguments(
//                    HANDLE_RETRY_JUMP,
//                    site.type().insertParameterTypes(0, JumpException.RetryJump.class),
//                    new int[] {0, 1, site.type().parameterCount()});
//            MethodHandle blockEscape = permuteArguments(
//                    breakHandler,
//                    site.type().insertParameterTypes(0, Throwable.class),
//                    new int[] {0, site.type().parameterCount()});
            target = catchException(target, JumpException.BreakJump.class, breakHandler);
//            target = catchException(target, JumpException.RetryJump.class, retryHandler);
//            target = catchException(target, Throwable.class, retryHandler);
            target = catchException(
                    target,
                    Throwable.class,
                    permuteArguments(BLOCK_ESCAPE_EXCEPTION, site.type().insertParameterTypes(0, Throwable.class), new int[] {0, site.type().parameterCount()}));
            target = foldArguments(
                    permuteArguments(BLOCK_ESCAPE, site.type().insertParameterTypes(0, IRubyObject.class), new int[] {0, site.type().parameterCount()}),
                    target);
        }
        
        // if it's an attr assignment as an expression, need to return n-1th argument
        if (site.isAttrAssign() && site.isExpression()) {
            // return given argument
            MethodHandle newTarget = identity(IRubyObject.class);
            
            // if args are IRubyObject[].class, yank out n-1th
            if (site.type().parameterArray()[site.type().parameterCount() - 1] == IRubyObject[].class) {
                newTarget = filterArguments(newTarget, 0, findStatic(InvokeDynamicSupport.class, "getLast", methodType(IRubyObject.class, IRubyObject[].class))); 
            }
            
            // drop standard preamble args plus extra args
            newTarget = dropArguments(newTarget, 0, IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class);
            
            // drop extra arguments, if any
            MethodType dropped = target.type().dropParameterTypes(0, 4);
            if (dropped.parameterCount() > 1) {
                Class[] drops = new Class[dropped.parameterCount() - 1];
                Arrays.fill(drops, IRubyObject.class);
                newTarget = dropArguments(newTarget, 5, drops);
            }
            
            // fold using target
            target = foldArguments(newTarget, target);
        }
        
        return target;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Inline-caching failure paths, for megamorphic call sites
    ////////////////////////////////////////////////////////////////////////////

    public static IRubyObject fail(JRubyCallSite site, 
            ThreadContext context,
            IRubyObject caller,
            IRubyObject self,
            String name) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name);
        } else {
            entry = selfClass.searchWithCache(name);
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, name);
        }
    }

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, arg0);
        } else {
            entry = selfClass.searchWithCache(name);
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, name, arg0);
        }
    }

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, arg0, arg1);
        } else {
            entry = selfClass.searchWithCache(name);
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, name, arg0, arg1);
        }
    }

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, arg0, arg1, arg2);
        } else {
            entry = selfClass.searchWithCache(name);
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, arg2);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, name, arg0, arg1, arg2);
        }
    }

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, args);
        } else {
            entry = selfClass.searchWithCache(name);
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, args);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, name, args);
        }
    }

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, block);
        } else {
            entry = selfClass.searchWithCache(name);
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, block);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, name, block);
        }
    }

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, arg0, block);
        } else {
            entry = selfClass.searchWithCache(name);
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, block);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, name, arg0, block);
        }
    }

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, arg0, arg1, block);
        } else {
            entry = selfClass.searchWithCache(name);
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, block);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, name, arg0, arg1, block);
        }
    }

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, arg0, arg1, arg2, block);
        } else {
            entry = selfClass.searchWithCache(name);
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, arg2, block);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, name, arg0, arg1, arg2, block);
        }
    }

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;

        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, args, block);
        } else {
            entry = selfClass.searchWithCache(name);
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, args, block);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, name, args, block);
        }
    }

    public static IRubyObject failIter(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        try {
            if (entry.typeOk(selfClass)) {
                return entry.method.call(context, self, selfClass, name, block);
            } else {
                entry = selfClass.searchWithCache(name);
                if (methodMissing(entry, site.callType(), name, caller)) {
                    return callMethodMissing(entry, site.callType(), context, self, name, block);
                }
                site.entry = entry;
                return entry.method.call(context, self, selfClass, name, block);
            }
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            return retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject failIter(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        try {
            if (entry.typeOk(selfClass)) {
                return entry.method.call(context, self, selfClass, name, arg0, block);
            } else {
                entry = selfClass.searchWithCache(name);
                if (methodMissing(entry, site.callType(), name, caller)) {
                    return callMethodMissing(entry, site.callType(), context, self, name, arg0, block);
                }
                site.entry = entry;
                return entry.method.call(context, self, selfClass, name, arg0, block);
            }
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            return retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject failIter(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        try {
            if (entry.typeOk(selfClass)) {
                return entry.method.call(context, self, selfClass, name, arg0, arg1, block);
            } else {
                entry = selfClass.searchWithCache(name);
                if (methodMissing(entry, site.callType(), name, caller)) {
                    return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, block);
                }
                site.entry = entry;
                return entry.method.call(context, self, selfClass, name, arg0, arg1, block);
            }
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            return retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject failIter(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        try {
            if (entry.typeOk(selfClass)) {
                return entry.method.call(context, self, selfClass, name, arg0, arg1, arg2, block);
            } else {
                entry = selfClass.searchWithCache(name);
                if (methodMissing(entry, site.callType(), name, caller)) {
                    return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, arg2, block);
                }
                site.entry = entry;
                return entry.method.call(context, self, selfClass, name, arg0, arg1, arg2, block);
            }
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            return retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject failIter(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        try {
            if (entry.typeOk(selfClass)) {
                return entry.method.call(context, self, selfClass, name, args, block);
            } else {
                entry = selfClass.searchWithCache(name);
                if (methodMissing(entry, site.callType(), name, caller)) {
                    return callMethodMissing(entry, site.callType(), context, self, name, args, block);
                }
                site.entry = entry;
                return entry.method.call(context, self, selfClass, name, args, block);
            }
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            return retryJumpError(context);
        } finally {
            block.escape();
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Dispatch via DynamicMethod#call
    ////////////////////////////////////////////////////////////////////////////
    
    private static MethodHandle getDynamicMethodTarget(MethodType callType, int arity) {
        MethodHandle target;
        Class lastParam = callType.parameterType(callType.parameterCount() - 1);
        boolean block = lastParam == Block.class;
        switch (arity) {
            case 0:
                target = block ? TARGET_0_B : TARGET_0;
                break;
            case 1:
                target = block ? TARGET_1_B : TARGET_1;
                break;
            case 2:
                target = block ? TARGET_2_B : TARGET_2;
                break;
            case 3:
                target = block ? TARGET_3_B : TARGET_3;
                break;
            default:
                target = block ? TARGET_N_B : TARGET_N;
        }
        
        return target;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Dispatch from Ruby to Java via Java integration
    ////////////////////////////////////////////////////////////////////////////
    
    private static MethodHandle createJavaHandle(DynamicMethod method) {
        MethodHandle nativeTarget = null;
        MethodHandle returnFilter = null;
        
        Ruby runtime = method.getImplementationClass().getRuntime();
        DynamicMethod.NativeCall nativeCall = method.getNativeCall();
        
        if (nativeCall.isStatic()) {
            nativeTarget = findStatic(nativeCall.getNativeTarget(), nativeCall.getNativeName(), methodType(nativeCall.getNativeReturn(), nativeCall.getNativeSignature()));
            
            if (nativeCall.getNativeSignature().length == 0) {
                // handle return value
                if (nativeCall.getNativeReturn() == byte.class ||
                        nativeCall.getNativeReturn() == short.class ||
                        nativeCall.getNativeReturn() == char.class ||
                        nativeCall.getNativeReturn() == int.class ||
                        nativeCall.getNativeReturn() == long.class ||
                        nativeCall.getNativeReturn() == Byte.class ||
                        nativeCall.getNativeReturn() == Short.class ||
                        nativeCall.getNativeReturn() == Character.class ||
                        nativeCall.getNativeReturn() == Integer.class ||
                        nativeCall.getNativeReturn() == Long.class) {
                    nativeTarget = explicitCastArguments(nativeTarget, methodType(long.class));
                    returnFilter = insertArguments(
                            findStatic(RubyFixnum.class, "newFixnum", methodType(RubyFixnum.class, Ruby.class, long.class)),
                            0,
                            runtime);
                } else if (nativeCall.getNativeReturn() == float.class ||
                        nativeCall.getNativeReturn() == double.class ||
                        nativeCall.getNativeReturn() == Float.class ||
                        nativeCall.getNativeReturn() == Double.class) {
                    nativeTarget = explicitCastArguments(nativeTarget, methodType(double.class));
                    returnFilter = insertArguments(
                            findStatic(RubyFloat.class, "newFloat", methodType(RubyFloat.class, Ruby.class, double.class)),
                            0,
                            runtime);
                } else if (nativeCall.getNativeReturn() == boolean.class ||
                        nativeCall.getNativeReturn() == Boolean.class) {
                    nativeTarget = explicitCastArguments(nativeTarget, methodType(boolean.class));
                    returnFilter = insertArguments(
                            findStatic(RubyBoolean.class, "newBoolean", methodType(RubyBoolean.class, Ruby.class, boolean.class)),
                            0,
                            runtime);
                } else if (CharSequence.class.isAssignableFrom(nativeCall.getNativeReturn())) {
                    nativeTarget = explicitCastArguments(nativeTarget, methodType(CharSequence.class));
                    returnFilter = insertArguments(
                            findStatic(RubyString.class, "newUnicodeString", methodType(RubyString.class, Ruby.class, CharSequence.class)),
                            0,
                            runtime);
                } else if (nativeCall.getNativeReturn() == void.class) {
                    returnFilter = constant(IRubyObject.class, runtime.getNil());
                }

                // we can handle this; do remaining transforms and return
                if (returnFilter != null) {
                    nativeTarget = filterReturnValue(nativeTarget, returnFilter);
                    nativeTarget = explicitCastArguments(nativeTarget, methodType(IRubyObject.class));
                    nativeTarget = dropArguments(nativeTarget, 0, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class);
                    
                    method.setHandle(nativeTarget);
                    return nativeTarget;
                }
            }
        } else {
            nativeTarget = findVirtual(nativeCall.getNativeTarget(), nativeCall.getNativeName(), methodType(nativeCall.getNativeReturn(), nativeCall.getNativeSignature()));
            
            if (nativeCall.getNativeSignature().length == 0) {
                // convert target
                nativeTarget = filterArguments(
                        nativeTarget,
                        0,
                        explicitCastArguments(
                                findStatic(JavaUtil.class, "objectFromJavaProxy", methodType(Object.class, IRubyObject.class)),
                                methodType(nativeCall.getNativeTarget(), IRubyObject.class)));
                
                // handle return value
                if (nativeCall.getNativeReturn() == byte.class ||
                        nativeCall.getNativeReturn() == short.class ||
                        nativeCall.getNativeReturn() == char.class ||
                        nativeCall.getNativeReturn() == int.class ||
                        nativeCall.getNativeReturn() == long.class ||
                        nativeCall.getNativeReturn() == Byte.class ||
                        nativeCall.getNativeReturn() == Short.class ||
                        nativeCall.getNativeReturn() == Character.class ||
                        nativeCall.getNativeReturn() == Integer.class ||
                        nativeCall.getNativeReturn() == Long.class) {
                    nativeTarget = explicitCastArguments(nativeTarget, methodType(long.class, IRubyObject.class));
                    returnFilter = insertArguments(
                            findStatic(RubyFixnum.class, "newFixnum", methodType(RubyFixnum.class, Ruby.class, long.class)),
                            0,
                            runtime);
                } else if (nativeCall.getNativeReturn() == float.class ||
                        nativeCall.getNativeReturn() == double.class ||
                        nativeCall.getNativeReturn() == Float.class ||
                        nativeCall.getNativeReturn() == Double.class) {
                    nativeTarget = explicitCastArguments(nativeTarget, methodType(double.class, IRubyObject.class));
                    returnFilter = insertArguments(
                            findStatic(RubyFloat.class, "newFloat", methodType(RubyFloat.class, Ruby.class, double.class)),
                            0,
                            runtime);
                } else if (nativeCall.getNativeReturn() == boolean.class ||
                        nativeCall.getNativeReturn() == Boolean.class) {
                    nativeTarget = explicitCastArguments(nativeTarget, methodType(boolean.class, IRubyObject.class));
                    returnFilter = insertArguments(
                            findStatic(RubyBoolean.class, "newBoolean", methodType(RubyBoolean.class, Ruby.class, boolean.class)),
                            0,
                            runtime);
                } else if (CharSequence.class.isAssignableFrom(nativeCall.getNativeReturn())) {
                    nativeTarget = explicitCastArguments(nativeTarget, methodType(CharSequence.class, IRubyObject.class));
                    returnFilter = insertArguments(
                            findStatic(RubyString.class, "newUnicodeString", methodType(RubyString.class, Ruby.class, CharSequence.class)),
                            0,
                            runtime);
                } else if (nativeCall.getNativeReturn() == void.class) {
                    returnFilter = constant(IRubyObject.class, runtime.getNil());
                }

                // we can handle this; do remaining transforms and return
                if (returnFilter != null) {
                    nativeTarget = filterReturnValue(nativeTarget, returnFilter);
                    nativeTarget = explicitCastArguments(nativeTarget, methodType(IRubyObject.class, IRubyObject.class));
                    nativeTarget = permuteArguments(
                            nativeTarget,
                            STANDARD_NATIVE_TYPE_BLOCK,
                            SELF_PERMUTE);
                    
                    method.setHandle(nativeTarget);
                    return nativeTarget;
                }
            }
        }
        
        return null;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Dispatch via direct handle to native core method
    ////////////////////////////////////////////////////////////////////////////

    private static MethodHandle createNativeHandle(JRubyCallSite site, DynamicMethod method) {
        MethodHandle nativeTarget = null;
        
        if (method.getCallConfig() == CallConfiguration.FrameNoneScopeNone) {
            DynamicMethod.NativeCall nativeCall = method.getNativeCall();
            Class[] nativeSig = nativeCall.getNativeSignature();
            boolean isStatic = nativeCall.isStatic();
            
            try {
                if (isStatic) {
                    nativeTarget = site.lookup().findStatic(
                            nativeCall.getNativeTarget(),
                            nativeCall.getNativeName(),
                            methodType(nativeCall.getNativeReturn(),
                            nativeCall.getNativeSignature()));
                } else {
                    nativeTarget = site.lookup().findVirtual(
                            nativeCall.getNativeTarget(),
                            nativeCall.getNativeName(),
                            methodType(nativeCall.getNativeReturn(),
                            nativeCall.getNativeSignature()));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
            int argCount = getArgCount(nativeCall.getNativeSignature(), isStatic);
            MethodType inboundType = STANDARD_NATIVE_TYPES_BLOCK[argCount];

            int[] permute;
            MethodType convert;
            if (nativeSig.length > 0 && nativeSig[0] == ThreadContext.class) {
                if (nativeSig[nativeSig.length - 1] == Block.class) {
                    convert = isStatic ? TARGET_TC_SELF_ARGS_BLOCK[argCount] : TARGET_SELF_TC_ARGS_BLOCK[argCount];
                    permute = isStatic ? TC_SELF_ARGS_BLOCK_PERMUTES[argCount] : SELF_TC_ARGS_BLOCK_PERMUTES[argCount];
                } else {
                    convert = isStatic ? TARGET_TC_SELF_ARGS[argCount] : TARGET_SELF_TC_ARGS[argCount];
                    permute = isStatic ? TC_SELF_ARGS_PERMUTES[argCount] : SELF_TC_ARGS_PERMUTES[argCount];
                }
            } else {
                if (nativeSig.length > 0 && nativeSig[nativeSig.length - 1] == Block.class) {
                    convert = TARGET_SELF_ARGS_BLOCK[argCount];
                    permute = SELF_ARGS_BLOCK_PERMUTES[argCount];
                } else {
                    convert = TARGET_SELF_ARGS[argCount];
                    permute = SELF_ARGS_PERMUTES[argCount];
                }
            }

            nativeTarget = explicitCastArguments(nativeTarget, convert);
            nativeTarget = permuteArguments(nativeTarget, inboundType, permute);
            method.setHandle(nativeTarget);
            return nativeTarget;
        }
        
        // can't build native handle for it
        return null;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Dispatch to attribute accessors
    ////////////////////////////////////////////////////////////////////////////

    private static MethodHandle createAttrReaderHandle(JRubyCallSite site, RubyClass cls, DynamicMethod method) {
        MethodHandle nativeTarget = null;
        AttrReaderMethod attrReader = (AttrReaderMethod)method;
        String varName = attrReader.getVariableName();
        
        RubyClass.VariableAccessor accessor = cls.getRealClass().getVariableAccessorForRead(varName);
        
        MethodHandle target = findVirtual(IRubyObject.class, "getVariable", methodType(Object.class, int.class));
        target = insertArguments(target, 1, accessor.getIndex());
        target = explicitCastArguments(target, methodType(IRubyObject.class, IRubyObject.class));
        MethodHandle filter = findStatic(InvokeDynamicSupport.class, "valueOrNil", methodType(IRubyObject.class, IRubyObject.class, IRubyObject.class));
        filter = insertArguments(filter, 1, cls.getRuntime().getNil());
        target = filterReturnValue(target, filter);
        target = permuteArguments(target, site.type(), new int[] {2});
        
        return target;
    }
    
    protected static IRubyObject valueOrNil(IRubyObject value, IRubyObject nil) {
        return value == null ? nil : value;
    }

    private static MethodHandle createAttrWriterHandle(JRubyCallSite site, RubyClass cls, DynamicMethod method) {
        MethodHandle nativeTarget = null;
        AttrWriterMethod attrWriter = (AttrWriterMethod)method;
        String varName = attrWriter.getVariableName();
        
        RubyClass.VariableAccessor accessor = cls.getRealClass().getVariableAccessorForWrite(varName);
        
        MethodHandle target = findVirtual(IRubyObject.class, "setVariable", methodType(void.class, int.class, Object.class));
        target = insertArguments(target, 1, accessor.getIndex());
        target = explicitCastArguments(target, methodType(void.class, IRubyObject.class, IRubyObject.class));
        target = filterReturnValue(target, constant(IRubyObject.class, cls.getRuntime().getNil()));
        target = permuteArguments(target, site.type(), new int[] {2, 4});
        
        return target;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Dispatch via direct handle to Ruby method
    ////////////////////////////////////////////////////////////////////////////

    private static MethodHandle createRubyHandle(JRubyCallSite site, DynamicMethod method) {
        DynamicMethod.NativeCall nativeCall = method.getNativeCall();
        MethodHandle nativeTarget;
        
        try {
            nativeTarget = site.lookup().findStatic(
                    nativeCall.getNativeTarget(),
                    nativeCall.getNativeName(),
                    methodType(nativeCall.getNativeReturn(),
                    nativeCall.getNativeSignature()));
            CompiledMethod cm = (CompiledMethod)method;
            nativeTarget = insertArguments(nativeTarget, 0, cm.getScriptObject());
            
            // juggle args into correct places
            int argCount = getRubyArgCount(nativeCall.getNativeSignature());
            switch (argCount) {
                case 0:
                    nativeTarget = permuteArguments(nativeTarget, STANDARD_NATIVE_TYPE_BLOCK, new int[] {0, 2, 4});
                    break;
                case -1:
                case 1:
                    nativeTarget = permuteArguments(nativeTarget, STANDARD_NATIVE_TYPE_1ARG_BLOCK, new int[] {0, 2, 4, 5});
                    break;
                case 2:
                    nativeTarget = permuteArguments(nativeTarget, STANDARD_NATIVE_TYPE_2ARG_BLOCK, new int[] {0, 2, 4, 5, 6});
                    break;
                case 3:
                    nativeTarget = permuteArguments(nativeTarget, STANDARD_NATIVE_TYPE_3ARG_BLOCK, new int[] {0, 2, 4, 5, 6, 7});
                    break;
                default:
                    throw new RuntimeException("unknown arg count: " + argCount);
            }
            
            method.setHandle(nativeTarget);
            return nativeTarget;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Dispatch via DynamicMethod.call to attribute access method
    ////////////////////////////////////////////////////////////////////////////

    private static MethodHandle getAttrTarget(JRubyCallSite site, DynamicMethod method) {
        MethodHandle target = (MethodHandle)method.getHandle();
        if (target != null) return target;
        
        try {
            if (method instanceof AttrReaderMethod) {
                AttrReaderMethod reader = (AttrReaderMethod)method;
                target = site.lookup().findVirtual(
                        AttrReaderMethod.class,
                        "call",
                        methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class));
                target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class));
                target = permuteArguments(
                        target,
                        methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class),
                        new int[] {0,2,4,1,5});
                // IRubyObject, DynamicMethod, RubyClass, ThreadContext, IRubyObject, IRubyObject, String
                target = insertArguments(target, 0, reader);
                // IRubyObject, RubyClass, ThreadContext, IRubyObject, IRubyObject, String
                target = foldArguments(target, PGC2_0);
                // IRubyObject, ThreadContext, IRubyObject, IRubyObject, String
            } else {
                AttrWriterMethod writer = (AttrWriterMethod)method;
                target = site.lookup().findVirtual(
                        AttrWriterMethod.class,
                        "call",
                        methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class));
                target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class));
                target = permuteArguments(
                        target,
                        methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class),
                        new int[] {0,2,4,1,5,6});
                // IRubyObject, DynamicMethod, RubyClass, ThreadContext, IRubyObject, IRubyObject, String
                target = insertArguments(target, 0, writer);
                // IRubyObject, RubyClass, ThreadContext, IRubyObject, IRubyObject, String
                target = foldArguments(target, PGC2_1);
                // IRubyObject, ThreadContext, IRubyObject, IRubyObject, String
            }
            method.setHandle(target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // method_missing support code
    ////////////////////////////////////////////////////////////////////////////

    protected static boolean methodMissing(CacheEntry entry, CallType callType, String name, IRubyObject caller) {
        DynamicMethod method = entry.method;
        return method.isUndefined() || (callType == CallType.NORMAL && !name.equals("method_missing") && !method.isCallableFrom(caller, callType));
    }

    private static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject[] args) {
        return RuntimeHelpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, args, Block.NULL_BLOCK);
    }

    private static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name) {
        return RuntimeHelpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, Block.NULL_BLOCK);
    }

    private static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, Block block) {
        return RuntimeHelpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, block);
    }

    private static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg) {
        return RuntimeHelpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg, Block.NULL_BLOCK);
    }

    private static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject[] args, Block block) {
        return RuntimeHelpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, args, block);
    }

    private static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg0, Block block) {
        return RuntimeHelpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg0, block);
    }

    private static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) {
        return RuntimeHelpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg0, arg1, Block.NULL_BLOCK);
    }

    private static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        return RuntimeHelpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg0, arg1, block);
    }

    private static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return RuntimeHelpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg0, arg1, arg2, Block.NULL_BLOCK);
    }

    private static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return RuntimeHelpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg0, arg1, arg2, block);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Additional support code
    ////////////////////////////////////////////////////////////////////////////

    private static int getArgCount(Class[] args, boolean isStatic) {
        int length = args.length;
        boolean hasContext = false;
        if (isStatic) {
            if (args.length > 1 && args[0] == ThreadContext.class) {
                length--;
                hasContext = true;
            }
            
            // remove self object
            assert args.length >= 1;
            length--;

            if (args.length > 1 && args[args.length - 1] == Block.class) {
                length--;
            }
            
            if (length == 1) {
                if (hasContext && args[2] == IRubyObject[].class) {
                    length = 4;
                } else if (args[1] == IRubyObject[].class) {
                    length = 4;
                }
            }
        } else {
            if (args.length > 0 && args[0] == ThreadContext.class) {
                length--;
                hasContext = true;
            }

            if (args.length > 0 && args[args.length - 1] == Block.class) {
                length--;
            }

            if (length == 1) {
                if (hasContext && args[1] == IRubyObject[].class) {
                    length = 4;
                } else if (args[0] == IRubyObject[].class) {
                    length = 4;
                }
            }
        }
        return length;
    }

    private static int getRubyArgCount(Class[] args) {
        int length = args.length;
        boolean hasContext = false;
        
        // remove script object
        length--;
        
        if (args.length > 2 && args[1] == ThreadContext.class) {
            length--;
            hasContext = true;
        }

        // remove self object
        assert args.length >= 2;
        length--;

        if (args.length > 2 && args[args.length - 1] == Block.class) {
            length--;
        }

        if (length == 1) {
            if (hasContext && args[3] == IRubyObject[].class) {
                length = 4;
            } else if (args[2] == IRubyObject[].class) {
                length = 4;
            }
        }

        return length;
    }

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

    private static IRubyObject handleBreakJump(ThreadContext context, JumpException.BreakJump bj) throws JumpException.BreakJump {
        if (context.getFrameJumpTarget() == bj.getTarget()) {
            return (IRubyObject) bj.getValue();
        }
        throw bj;
    }

    public static IRubyObject retryJumpError(ThreadContext context) {
        throw context.getRuntime().newLocalJumpError(RubyLocalJumpError.Reason.RETRY, context.getRuntime().getNil(), "retry outside of rescue not supported");
    }

    private static final MethodHandle GETMETHOD;
    static {
        MethodHandle getMethod = findStatic(InvokeDynamicSupport.class, "getMethod", methodType(DynamicMethod.class, CacheEntry.class));
        getMethod = dropArguments(getMethod, 0, RubyClass.class);
        getMethod = dropArguments(getMethod, 2, ThreadContext.class, IRubyObject.class, IRubyObject.class);
        GETMETHOD = getMethod;
    }

    public static DynamicMethod getMethod(CacheEntry entry) {
        return entry.method;
    }

    private static final MethodHandle PGC = dropArguments(
            dropArguments(
                findStatic(InvokeDynamicSupport.class, "pollAndGetClass",
                    methodType(RubyClass.class, ThreadContext.class, IRubyObject.class)),
                1,
                IRubyObject.class),
            0,
            CacheEntry.class);

    private static final MethodHandle PGC2 = dropArguments(
            findStatic(InvokeDynamicSupport.class, "pollAndGetClass",
                methodType(RubyClass.class, ThreadContext.class, IRubyObject.class)),
            1,
            IRubyObject.class);

    private static final MethodHandle TEST = dropArguments(
            findStatic(InvokeDynamicSupport.class, "test",
                methodType(boolean.class, int.class, IRubyObject.class)),
            1,
            ThreadContext.class, IRubyObject.class);

    private static MethodHandle dropNameAndArgs(MethodHandle original, int index, int count, boolean block) {
        switch (count) {
        case -1:
            if (block) {
                return dropArguments(original, index, String.class, IRubyObject[].class, Block.class);
            } else {
                return dropArguments(original, index, String.class, IRubyObject[].class);
            }
        case 0:
            if (block) {
                return dropArguments(original, index, String.class, Block.class);
            } else {
                return dropArguments(original, index, String.class);
            }
        case 1:
            if (block) {
                return dropArguments(original, index, String.class, IRubyObject.class, Block.class);
            } else {
                return dropArguments(original, index, String.class, IRubyObject.class);
            }
        case 2:
            if (block) {
                return dropArguments(original, index, String.class, IRubyObject.class, IRubyObject.class, Block.class);
            } else {
                return dropArguments(original, index, String.class, IRubyObject.class, IRubyObject.class);
            }
        case 3:
            if (block) {
                return dropArguments(original, index, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class);
            } else {
                return dropArguments(original, index, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class);
            }
        default:
            throw new RuntimeException("Invalid arg count (" + count + ") while preparing method handle:\n\t" + original);
        }
    }
    
    // call pre/post logic handles
    private static void preMethodFrameAndScope(
            ThreadContext context,
            RubyModule clazz,
            String name,
            IRubyObject self,
            Block block, 
            StaticScope staticScope) {
        context.preMethodFrameAndScope(clazz, name, self, block, staticScope);
    }
    private static void preMethodFrameAndDummyScope(
            ThreadContext context,
            RubyModule clazz,
            String name,
            IRubyObject self,
            Block block, 
            StaticScope staticScope) {
        context.preMethodFrameAndDummyScope(clazz, name, self, block, staticScope);
    }
    private static void preMethodFrameOnly(
            ThreadContext context,
            RubyModule clazz,
            String name,
            IRubyObject self,
            Block block, 
            StaticScope staticScope) {
        context.preMethodFrameOnly(clazz, name, self, block);
    }
    private static void preMethodScopeOnly(
            ThreadContext context,
            RubyModule clazz,
            String name,
            IRubyObject self,
            Block block, 
            StaticScope staticScope) {
        context.preMethodScopeOnly(clazz, staticScope);
    }
    
    private static final MethodType PRE_METHOD_TYPE =
            methodType(void.class, ThreadContext.class, RubyModule.class, String.class, IRubyObject.class, Block.class, StaticScope.class);
    
    private static final MethodHandle PRE_METHOD_FRAME_AND_SCOPE =
            findStatic(InvokeDynamicSupport.class, "preMethodFrameAndScope", PRE_METHOD_TYPE);
    private static final MethodHandle POST_METHOD_FRAME_AND_SCOPE =
            findVirtual(ThreadContext.class, "postMethodFrameAndScope", methodType(void.class));
    
    private static final MethodHandle PRE_METHOD_FRAME_AND_DUMMY_SCOPE =
            findStatic(InvokeDynamicSupport.class, "preMethodFrameAndDummyScope", PRE_METHOD_TYPE);
    private static final MethodHandle FRAME_FULL_SCOPE_DUMMY_POST = POST_METHOD_FRAME_AND_SCOPE;
    
    private static final MethodHandle PRE_METHOD_FRAME_ONLY =
            findStatic(InvokeDynamicSupport.class, "preMethodFrameOnly", PRE_METHOD_TYPE);
    private static final MethodHandle POST_METHOD_FRAME_ONLY =
            findVirtual(ThreadContext.class, "postMethodFrameOnly", methodType(void.class));
    
    private static final MethodHandle PRE_METHOD_SCOPE_ONLY =
            findStatic(InvokeDynamicSupport.class, "preMethodScopeOnly", PRE_METHOD_TYPE);
    private static final MethodHandle POST_METHOD_SCOPE_ONLY = 
            findVirtual(ThreadContext.class, "postMethodScopeOnly", methodType(void.class));
    
    ////////////////////////////////////////////////////////////////////////////
    // Support handles for DynamicMethod.call paths
    ////////////////////////////////////////////////////////////////////////////

    private static final MethodHandle PGC_0 = dropNameAndArgs(PGC, 4, 0, false);
    private static final MethodHandle PGC2_0 = dropNameAndArgs(PGC2, 3, 0, false);
    private static final MethodHandle GETMETHOD_0 = dropNameAndArgs(GETMETHOD, 5, 0, false);
    private static final MethodHandle TEST_0 = dropNameAndArgs(TEST, 4, 0, false);
    private static final MethodHandle TARGET_0;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class));
        target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class));
        target = permuteArguments(
                target,
                methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class),
                new int[] {0,3,5,1,6});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String
        target = foldArguments(target, GETMETHOD_0);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String
        target = foldArguments(target, PGC_0);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String
        TARGET_0 = target;
    }
    private static final MethodHandle FALLBACK_0 = findStatic(InvokeDynamicSupport.class, "invocationFallback",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class));
    private static final MethodHandle FAIL_0 = findStatic(InvokeDynamicSupport.class, "fail",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class));

    private static final MethodHandle PGC_1 = dropNameAndArgs(PGC, 4, 1, false);
    private static final MethodHandle PGC2_1 = dropNameAndArgs(PGC2, 3, 1, false);
    private static final MethodHandle GETMETHOD_1 = dropNameAndArgs(GETMETHOD, 5, 1, false);
    private static final MethodHandle TEST_1 = dropNameAndArgs(TEST, 4, 1, false);
    private static final MethodHandle TARGET_1;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class));
        // IRubyObject, DynamicMethod, ThreadContext, IRubyObject, RubyModule, String, IRubyObject
        target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class));
        // IRubyObject, DynamicMethod, ThreadContext, IRubyObject, RubyClass, String, IRubyObject
        target = permuteArguments(
                target,
                methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class),
                new int[] {0,3,5,1,6,7});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, IRubyObject
        target = foldArguments(target, GETMETHOD_1);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, IRubyObject
        target = foldArguments(target, PGC_1);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, IRubyObject
        TARGET_1 = target;
    }
    private static final MethodHandle FALLBACK_1 = findStatic(InvokeDynamicSupport.class, "invocationFallback",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class));
    private static final MethodHandle FAIL_1 = findStatic(InvokeDynamicSupport.class, "fail",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class));

    private static final MethodHandle PGC_2 = dropNameAndArgs(PGC, 4, 2, false);
    private static final MethodHandle GETMETHOD_2 = dropNameAndArgs(GETMETHOD, 5, 2, false);
    private static final MethodHandle TEST_2 = dropNameAndArgs(TEST, 4, 2, false);
    private static final MethodHandle TARGET_2;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class));
        target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class, IRubyObject.class));
        target = permuteArguments(
                target,
                methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class),
                new int[] {0,3,5,1,6,7,8});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, GETMETHOD_2);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, PGC_2);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        TARGET_2 = target;
    }
    private static final MethodHandle FALLBACK_2 = findStatic(InvokeDynamicSupport.class, "invocationFallback",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class));
    private static final MethodHandle FAIL_2 = findStatic(InvokeDynamicSupport.class, "fail",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class));

    private static final MethodHandle PGC_3 = dropNameAndArgs(PGC, 4, 3, false);
    private static final MethodHandle GETMETHOD_3 = dropNameAndArgs(GETMETHOD, 5, 3, false);
    private static final MethodHandle TEST_3 = dropNameAndArgs(TEST, 4, 3, false);
    private static final MethodHandle TARGET_3;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
        target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
        target = permuteArguments(
                target,
                methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class),
                new int[] {0,3,5,1,6,7,8,9});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, GETMETHOD_3);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, PGC_3);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        TARGET_3 = target;
    }
    private static final MethodHandle FALLBACK_3 = findStatic(InvokeDynamicSupport.class, "invocationFallback",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
    private static final MethodHandle FAIL_3 = findStatic(InvokeDynamicSupport.class, "fail",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));

    private static final MethodHandle PGC_N = dropNameAndArgs(PGC, 4, -1, false);
    private static final MethodHandle GETMETHOD_N = dropNameAndArgs(GETMETHOD, 5, -1, false);
    private static final MethodHandle TEST_N = dropNameAndArgs(TEST, 4, -1, false);
    private static final MethodHandle TARGET_N;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject[].class));
        target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject[].class));
        target = permuteArguments(
                target,
                methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class),
                new int[] {0,3,5,1,6,7});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, GETMETHOD_N);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, PGC_N);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        TARGET_N = target;
    }
    private static final MethodHandle FALLBACK_N = findStatic(InvokeDynamicSupport.class, "invocationFallback",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class));
    private static final MethodHandle FAIL_N = findStatic(InvokeDynamicSupport.class, "fail",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class));

    private static final MethodHandle BREAKJUMP;
    static {
        MethodHandle breakJump = findStatic(
                InvokeDynamicSupport.class,
                "handleBreakJump",
                methodType(IRubyObject.class, JumpException.BreakJump.class, ThreadContext.class));
        // BreakJump, ThreadContext
        breakJump = permuteArguments(
                breakJump,
                methodType(IRubyObject.class, JumpException.BreakJump.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class),
                new int[] {0,2});
        // BreakJump, CacheEntry, ThreadContext, IRubyObject, IRubyObject
        BREAKJUMP = breakJump;
    }

    private static final MethodHandle RETRYJUMP;
    static {
        MethodHandle retryJump = findStatic(
                InvokeDynamicSupport.class,
                "retryJumpError",
                methodType(IRubyObject.class, ThreadContext.class));
        // ThreadContext
        retryJump = permuteArguments(
                retryJump,
                methodType(IRubyObject.class, JumpException.RetryJump.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class),
                new int[] {2});
        // RetryJump, CacheEntry, ThreadContext, IRubyObject, IRubyObject
        RETRYJUMP = retryJump;
    }

    private static final MethodHandle PGC_0_B = dropNameAndArgs(PGC, 4, 0, true);
    private static final MethodHandle GETMETHOD_0_B = dropNameAndArgs(GETMETHOD, 5, 0, true);
    private static final MethodHandle TEST_0_B = dropNameAndArgs(TEST, 4, 0, true);
    private static final MethodHandle TARGET_0_B;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, Block.class));
        target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, Block.class));
        target = permuteArguments(
                target,
                methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, Block.class),
                new int[] {0,3,5,1,6,7});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, GETMETHOD_0_B);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, PGC_0_B);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args

        TARGET_0_B = target;
    }
    private static final MethodHandle FALLBACK_0_B = findStatic(InvokeDynamicSupport.class, "invocationFallback",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, Block.class));
    private static final MethodHandle FAIL_0_B = findStatic(InvokeDynamicSupport.class, "fail",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, Block.class));
    private static final MethodHandle FAIL_ITER_0_B = findStatic(InvokeDynamicSupport.class, "failIter",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, Block.class));

    private static final MethodHandle PGC_1_B = dropNameAndArgs(PGC, 4, 1, true);
    private static final MethodHandle GETMETHOD_1_B = dropNameAndArgs(GETMETHOD, 5, 1, true);
    private static final MethodHandle TEST_1_B = dropNameAndArgs(TEST, 4, 1, true);
    private static final MethodHandle TARGET_1_B;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, Block.class));
        target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class, Block.class));
        target = permuteArguments(
                target,
                methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, Block.class),
                new int[] {0,3,5,1,6,7,8});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, GETMETHOD_1_B);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, PGC_1_B);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args

        TARGET_1_B = target;
    }
    private static final MethodHandle FALLBACK_1_B = findStatic(InvokeDynamicSupport.class, "invocationFallback",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, Block.class));
    private static final MethodHandle FAIL_1_B = findStatic(InvokeDynamicSupport.class, "fail",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, Block.class));
    private static final MethodHandle FAIL_ITER_1_B = findStatic(InvokeDynamicSupport.class, "failIter",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, Block.class));

    private static final MethodHandle PGC_2_B = dropNameAndArgs(PGC, 4, 2, true);
    private static final MethodHandle GETMETHOD_2_B = dropNameAndArgs(GETMETHOD, 5, 2, true);
    private static final MethodHandle TEST_2_B = dropNameAndArgs(TEST, 4, 2, true);
    private static final MethodHandle TARGET_2_B;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));
        target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));
        target = permuteArguments(
                target,
                methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, Block.class),
                new int[] {0,3,5,1,6,7,8,9});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, GETMETHOD_2_B);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, PGC_2_B);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args

        TARGET_2_B = target;
    }
    private static final MethodHandle FALLBACK_2_B = findStatic(InvokeDynamicSupport.class, "invocationFallback",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));
    private static final MethodHandle FAIL_2_B = findStatic(InvokeDynamicSupport.class, "fail",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));
    private static final MethodHandle FAIL_ITER_2_B = findStatic(InvokeDynamicSupport.class, "failIter",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));

    private static final MethodHandle PGC_3_B = dropNameAndArgs(PGC, 4, 3, true);
    private static final MethodHandle GETMETHOD_3_B = dropNameAndArgs(GETMETHOD, 5, 3, true);
    private static final MethodHandle TEST_3_B = dropNameAndArgs(TEST, 4, 3, true);
    private static final MethodHandle TARGET_3_B;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
        target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
        target = permuteArguments(
                target,
                methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class),
                new int[] {0,3,5,1,6,7,8,9,10});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, GETMETHOD_3_B);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, PGC_3_B);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        
        TARGET_3_B = target;
    }
    private static final MethodHandle FALLBACK_3_B = findStatic(InvokeDynamicSupport.class, "invocationFallback",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
    private static final MethodHandle FAIL_3_B = findStatic(InvokeDynamicSupport.class, "fail",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
    private static final MethodHandle FAIL_ITER_3_B = findStatic(InvokeDynamicSupport.class, "failIter",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));

    private static final MethodHandle PGC_N_B = dropNameAndArgs(PGC, 4, -1, true);
    private static final MethodHandle GETMETHOD_N_B = dropNameAndArgs(GETMETHOD, 5, -1, true);
    private static final MethodHandle TEST_N_B = dropNameAndArgs(TEST, 4, -1, true);
    private static final MethodHandle TARGET_N_B;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject[].class, Block.class));
        target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject[].class, Block.class));
        target = permuteArguments(
                target,
                methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class, Block.class),
                new int[] {0,3,5,1,6,7,8});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, GETMETHOD_N_B);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, PGC_N_B);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        
        TARGET_N_B = target;
    }
    private static final MethodHandle FALLBACK_N_B = findStatic(InvokeDynamicSupport.class, "invocationFallback",
                    methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class, Block.class));
    private static final MethodHandle FAIL_N_B = findStatic(InvokeDynamicSupport.class, "fail",
                    methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class, Block.class));
    private static final MethodHandle FAIL_ITER_N_B = findStatic(InvokeDynamicSupport.class, "failIter",
                    methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class, Block.class));
    
    ////////////////////////////////////////////////////////////////////////////
    // Utility methods for lookup
    ////////////////////////////////////////////////////////////////////////////
    
    private static MethodHandle findStatic(Class target, String name, MethodType type) {
        try {
            return lookup().findStatic(target, name, type);
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        } catch (IllegalAccessException nae) {
            throw new RuntimeException(nae);
        }
    }
    private static MethodHandle findVirtual(Class target, String name, MethodType type) {
        try {
            return lookup().findVirtual(target, name, type);
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        } catch (IllegalAccessException nae) {
            throw new RuntimeException(nae);
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Support method types and permutations
    ////////////////////////////////////////////////////////////////////////////
    
    private static final MethodType STANDARD_NATIVE_TYPE = methodType(
            IRubyObject.class, // return value
            ThreadContext.class, //context
            IRubyObject.class, // caller
            IRubyObject.class, // self
            String.class // method name
            );
    private static final MethodType STANDARD_NATIVE_TYPE_1ARG = STANDARD_NATIVE_TYPE.appendParameterTypes(IRubyObject.class);
    private static final MethodType STANDARD_NATIVE_TYPE_2ARG = STANDARD_NATIVE_TYPE_1ARG.appendParameterTypes(IRubyObject.class);
    private static final MethodType STANDARD_NATIVE_TYPE_3ARG = STANDARD_NATIVE_TYPE_2ARG.appendParameterTypes(IRubyObject.class);
    private static final MethodType STANDARD_NATIVE_TYPE_NARG = STANDARD_NATIVE_TYPE.appendParameterTypes(IRubyObject[].class);
    private static final MethodType[] STANDARD_NATIVE_TYPES = {
        STANDARD_NATIVE_TYPE,
        STANDARD_NATIVE_TYPE_1ARG,
        STANDARD_NATIVE_TYPE_2ARG,
        STANDARD_NATIVE_TYPE_3ARG,
        STANDARD_NATIVE_TYPE_NARG,
    };
    
    private static final MethodType STANDARD_NATIVE_TYPE_BLOCK = STANDARD_NATIVE_TYPE.appendParameterTypes(Block.class);
    private static final MethodType STANDARD_NATIVE_TYPE_1ARG_BLOCK = STANDARD_NATIVE_TYPE_1ARG.appendParameterTypes(Block.class);
    private static final MethodType STANDARD_NATIVE_TYPE_2ARG_BLOCK = STANDARD_NATIVE_TYPE_2ARG.appendParameterTypes(Block.class);
    private static final MethodType STANDARD_NATIVE_TYPE_3ARG_BLOCK = STANDARD_NATIVE_TYPE_3ARG.appendParameterTypes(Block.class);
    private static final MethodType STANDARD_NATIVE_TYPE_NARG_BLOCK = STANDARD_NATIVE_TYPE_NARG.appendParameterTypes(Block.class);
    private static final MethodType[] STANDARD_NATIVE_TYPES_BLOCK = {
        STANDARD_NATIVE_TYPE_BLOCK,
        STANDARD_NATIVE_TYPE_1ARG_BLOCK,
        STANDARD_NATIVE_TYPE_2ARG_BLOCK,
        STANDARD_NATIVE_TYPE_3ARG_BLOCK,
        STANDARD_NATIVE_TYPE_NARG_BLOCK,
    };
    
    private static final MethodType TARGET_SELF = methodType(
            IRubyObject.class, // return value
            IRubyObject.class // self
            );
    private static final MethodType TARGET_SELF_1ARG = TARGET_SELF.appendParameterTypes(IRubyObject.class);
    private static final MethodType TARGET_SELF_2ARG = TARGET_SELF_1ARG.appendParameterTypes(IRubyObject.class);
    private static final MethodType TARGET_SELF_3ARG = TARGET_SELF_2ARG.appendParameterTypes(IRubyObject.class);
    private static final MethodType TARGET_SELF_NARG = TARGET_SELF.appendParameterTypes(IRubyObject[].class);
    private static final MethodType[] TARGET_SELF_ARGS = {
        TARGET_SELF,
        TARGET_SELF_1ARG,
        TARGET_SELF_2ARG,
        TARGET_SELF_3ARG,
        TARGET_SELF_NARG,
    };
    
    private static final MethodType TARGET_SELF_BLOCK = TARGET_SELF.appendParameterTypes(Block.class);
    private static final MethodType TARGET_SELF_1ARG_BLOCK = TARGET_SELF_1ARG.appendParameterTypes(Block.class);
    private static final MethodType TARGET_SELF_2ARG_BLOCK = TARGET_SELF_2ARG.appendParameterTypes(Block.class);
    private static final MethodType TARGET_SELF_3ARG_BLOCK = TARGET_SELF_3ARG.appendParameterTypes(Block.class);
    private static final MethodType TARGET_SELF_NARG_BLOCK = TARGET_SELF_NARG.appendParameterTypes(Block.class);
    private static final MethodType[] TARGET_SELF_ARGS_BLOCK = {
        TARGET_SELF_BLOCK,
        TARGET_SELF_1ARG_BLOCK,
        TARGET_SELF_2ARG_BLOCK,
        TARGET_SELF_3ARG_BLOCK,
        TARGET_SELF_NARG_BLOCK,
    };
    
    private static final MethodType TARGET_SELF_TC = TARGET_SELF.appendParameterTypes(ThreadContext.class);
    private static final MethodType TARGET_SELF_TC_1ARG = TARGET_SELF_TC.appendParameterTypes(IRubyObject.class);
    private static final MethodType TARGET_SELF_TC_2ARG = TARGET_SELF_TC_1ARG.appendParameterTypes(IRubyObject.class);
    private static final MethodType TARGET_SELF_TC_3ARG = TARGET_SELF_TC_2ARG.appendParameterTypes(IRubyObject.class);
    private static final MethodType TARGET_SELF_TC_NARG = TARGET_SELF_TC.appendParameterTypes(IRubyObject[].class);
    private static final MethodType[] TARGET_SELF_TC_ARGS = {
        TARGET_SELF_TC,
        TARGET_SELF_TC_1ARG,
        TARGET_SELF_TC_2ARG,
        TARGET_SELF_TC_3ARG,
        TARGET_SELF_TC_NARG,
    };
    
    private static final MethodType TARGET_SELF_TC_BLOCK = TARGET_SELF_TC.appendParameterTypes(Block.class);
    private static final MethodType TARGET_SELF_TC_1ARG_BLOCK = TARGET_SELF_TC_1ARG.appendParameterTypes(Block.class);
    private static final MethodType TARGET_SELF_TC_2ARG_BLOCK = TARGET_SELF_TC_2ARG.appendParameterTypes(Block.class);
    private static final MethodType TARGET_SELF_TC_3ARG_BLOCK = TARGET_SELF_TC_3ARG.appendParameterTypes(Block.class);
    private static final MethodType TARGET_SELF_TC_NARG_BLOCK = TARGET_SELF_TC_NARG.appendParameterTypes(Block.class);
    private static final MethodType[] TARGET_SELF_TC_ARGS_BLOCK = {
        TARGET_SELF_TC_BLOCK,
        TARGET_SELF_TC_1ARG_BLOCK,
        TARGET_SELF_TC_2ARG_BLOCK,
        TARGET_SELF_TC_3ARG_BLOCK,
        TARGET_SELF_TC_NARG_BLOCK,
    };
    
    private static final MethodType TARGET_TC_SELF = methodType(
            IRubyObject.class, // return value
            ThreadContext.class, //context
            IRubyObject.class // self
            );
    private static final MethodType TARGET_TC_SELF_1ARG = TARGET_TC_SELF.appendParameterTypes(IRubyObject.class);
    private static final MethodType TARGET_TC_SELF_2ARG = TARGET_TC_SELF_1ARG.appendParameterTypes(IRubyObject.class);
    private static final MethodType TARGET_TC_SELF_3ARG = TARGET_TC_SELF_2ARG.appendParameterTypes(IRubyObject.class);
    private static final MethodType TARGET_TC_SELF_NARG = TARGET_TC_SELF.appendParameterTypes(IRubyObject[].class);
    private static final MethodType[] TARGET_TC_SELF_ARGS = {
        TARGET_TC_SELF,
        TARGET_TC_SELF_1ARG,
        TARGET_TC_SELF_2ARG,
        TARGET_TC_SELF_3ARG,
        TARGET_TC_SELF_NARG,
    };
    
    private static final MethodType TARGET_TC_SELF_BLOCK = TARGET_TC_SELF.appendParameterTypes(Block.class);
    private static final MethodType TARGET_TC_SELF_1ARG_BLOCK = TARGET_TC_SELF_1ARG.appendParameterTypes(Block.class);
    private static final MethodType TARGET_TC_SELF_2ARG_BLOCK = TARGET_TC_SELF_2ARG.appendParameterTypes(Block.class);
    private static final MethodType TARGET_TC_SELF_3ARG_BLOCK = TARGET_TC_SELF_3ARG.appendParameterTypes(Block.class);
    private static final MethodType TARGET_TC_SELF_NARG_BLOCK = TARGET_TC_SELF_NARG.appendParameterTypes(Block.class);
    private static final MethodType[] TARGET_TC_SELF_ARGS_BLOCK = {
        TARGET_TC_SELF_BLOCK,
        TARGET_TC_SELF_1ARG_BLOCK,
        TARGET_TC_SELF_2ARG_BLOCK,
        TARGET_TC_SELF_3ARG_BLOCK,
        TARGET_TC_SELF_NARG_BLOCK
    };
    
    private static final int[] SELF_TC_PERMUTE = {2, 0};
    private static final int[] SELF_TC_1ARG_PERMUTE = {2, 0, 4};
    private static final int[] SELF_TC_2ARG_PERMUTE = {2, 0, 4, 5};
    private static final int[] SELF_TC_3ARG_PERMUTE = {2, 0, 4, 5, 6};
    private static final int[] SELF_TC_NARG_PERMUTE = {2, 0, 4};
    private static final int[][] SELF_TC_ARGS_PERMUTES = {
        SELF_TC_PERMUTE,
        SELF_TC_1ARG_PERMUTE,
        SELF_TC_2ARG_PERMUTE,
        SELF_TC_3ARG_PERMUTE,
        SELF_TC_NARG_PERMUTE
    };
    private static final int[] SELF_PERMUTE = {2};
    private static final int[] SELF_1ARG_PERMUTE = {2, 4};
    private static final int[] SELF_2ARG_PERMUTE = {2, 4, 5};
    private static final int[] SELF_3ARG_PERMUTE = {2, 4, 5, 6};
    private static final int[] SELF_NARG_PERMUTE = {2, 4};
    private static final int[][] SELF_ARGS_PERMUTES = {
        SELF_PERMUTE,
        SELF_1ARG_PERMUTE,
        SELF_2ARG_PERMUTE,
        SELF_3ARG_PERMUTE,
        SELF_NARG_PERMUTE
    };
    private static final int[] SELF_TC_BLOCK_PERMUTE = {2, 0, 4};
    private static final int[] SELF_TC_1ARG_BLOCK_PERMUTE = {2, 0, 4, 5};
    private static final int[] SELF_TC_2ARG_BLOCK_PERMUTE = {2, 0, 4, 5, 6};
    private static final int[] SELF_TC_3ARG_BLOCK_PERMUTE = {2, 0, 4, 5, 6, 7};
    private static final int[] SELF_TC_NARG_BLOCK_PERMUTE = {2, 0, 4, 5};
    private static final int[][] SELF_TC_ARGS_BLOCK_PERMUTES = {
        SELF_TC_BLOCK_PERMUTE,
        SELF_TC_1ARG_BLOCK_PERMUTE,
        SELF_TC_2ARG_BLOCK_PERMUTE,
        SELF_TC_3ARG_BLOCK_PERMUTE,
        SELF_TC_NARG_BLOCK_PERMUTE
    };
    private static final int[] SELF_BLOCK_PERMUTE = {2, 4};
    private static final int[] SELF_1ARG_BLOCK_PERMUTE = {2, 4, 5};
    private static final int[] SELF_2ARG_BLOCK_PERMUTE = {2, 4, 5, 6};
    private static final int[] SELF_3ARG_BLOCK_PERMUTE = {2, 4, 5, 6, 7};
    private static final int[] SELF_NARG_BLOCK_PERMUTE = {2, 4, 5};
    private static final int[][] SELF_ARGS_BLOCK_PERMUTES = {
        SELF_BLOCK_PERMUTE,
        SELF_1ARG_BLOCK_PERMUTE,
        SELF_2ARG_BLOCK_PERMUTE,
        SELF_3ARG_BLOCK_PERMUTE,
        SELF_NARG_BLOCK_PERMUTE
    };
    private static final int[] TC_SELF_PERMUTE = {0, 2};
    private static final int[] TC_SELF_1ARG_PERMUTE = {0, 2, 4};
    private static final int[] TC_SELF_2ARG_PERMUTE = {0, 2, 4, 5};
    private static final int[] TC_SELF_3ARG_PERMUTE = {0, 2, 4, 5, 6};
    private static final int[] TC_SELF_NARG_PERMUTE = {0, 2, 4};
    private static final int[][] TC_SELF_ARGS_PERMUTES = {
        TC_SELF_PERMUTE,
        TC_SELF_1ARG_PERMUTE,
        TC_SELF_2ARG_PERMUTE,
        TC_SELF_3ARG_PERMUTE,
        TC_SELF_NARG_PERMUTE,
    };
    private static final int[] TC_SELF_BLOCK_PERMUTE = {0, 2, 4};
    private static final int[] TC_SELF_1ARG_BLOCK_PERMUTE = {0, 2, 4, 5};
    private static final int[] TC_SELF_2ARG_BLOCK_PERMUTE = {0, 2, 4, 5, 6};
    private static final int[] TC_SELF_3ARG_BLOCK_PERMUTE = {0, 2, 4, 5, 6, 7};
    private static final int[] TC_SELF_NARG_BLOCK_PERMUTE = {0, 2, 4, 5};
    private static final int[][] TC_SELF_ARGS_BLOCK_PERMUTES = {
        TC_SELF_BLOCK_PERMUTE,
        TC_SELF_1ARG_BLOCK_PERMUTE,
        TC_SELF_2ARG_BLOCK_PERMUTE,
        TC_SELF_3ARG_BLOCK_PERMUTE,
        TC_SELF_NARG_BLOCK_PERMUTE,
    };
}