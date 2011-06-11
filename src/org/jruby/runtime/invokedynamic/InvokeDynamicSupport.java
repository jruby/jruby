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
import java.lang.invoke.MethodHandles;
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

@SuppressWarnings("deprecation")
public class InvokeDynamicSupport {
    ////////////////////////////////////////////////////////////////////////////
    // BOOTSTRAP HANDLES
    ////////////////////////////////////////////////////////////////////////////
    
    public final static String BOOTSTRAP_BARE_SIG = sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class);
    public final static String BOOTSTRAP_STRING_STRING_SIG = sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, String.class);
    public final static String BOOTSTRAP_STRING_STRING_INT_SIG = sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, String.class, int.class);
    public final static String BOOTSTRAP_STRING_SIG = sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class);
    public final static String BOOTSTRAP_STRING_CALLTYPE_SIG = sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, CallType.class);
    public final static String BOOTSTRAP_LONG_SIG = sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, long.class);
    public final static String BOOTSTRAP_DOUBLE_SIG = sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, double.class);
    public final static String BOOTSTRAP_STRING_INT_SIG = sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, int.class);
    
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
    
    ////////////////////////////////////////////////////////////////////////////
    // BOOTSTRAP METHODS
    ////////////////////////////////////////////////////////////////////////////
    
    public static CallSite invocationBootstrap(MethodHandles.Lookup lookup, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
        JRubyCallSite site;

        if (name.equals("call")) {
            site = new JRubyCallSite(type, CallType.NORMAL, false);
        } else if (name.equals("fcall")) {
            site = new JRubyCallSite(type, CallType.FUNCTIONAL, false);
        } else if (name.equals("attrAssign")) {
            // This needs to change based on receiver, but it's not a big deal
            site = new JRubyCallSite(type, CallType.VARIABLE, true);
        } else {
            throw new RuntimeException("wrong invokedynamic target: " + name);
        }
        
        MethodType fallbackType = type.insertParameterTypes(0, JRubyCallSite.class);
        MethodHandle myFallback = MethodHandles.insertArguments(
                lookup.findStatic(InvokeDynamicSupport.class, "invocationFallback",
                fallbackType),
                0,
                site);
        site.setTarget(myFallback);
        return site;
    }

    public static CallSite getConstantBootstrap(MethodHandles.Lookup lookup, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
        RubyConstantCallSite site;

        site = new RubyConstantCallSite(type, name);
        
        MethodType fallbackType = type.insertParameterTypes(0, RubyConstantCallSite.class);
        MethodHandle myFallback = MethodHandles.insertArguments(
                lookup.findStatic(InvokeDynamicSupport.class, "constantFallback",
                fallbackType),
                0,
                site);
        site.setTarget(myFallback);
        return site;
    }

    public static CallSite getByteListBootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String asString, String encodingName) {
        byte[] bytes = RuntimeHelpers.stringToRawBytes(asString);
        Encoding encoding = EncodingDB.getEncodings().get(encodingName.getBytes()).getEncoding();
        ByteList byteList = new ByteList(bytes, encoding);
        
        return new ConstantCallSite(MethodHandles.constant(ByteList.class, byteList));
    }
    
    public static CallSite getRegexpBootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String asString, String encodingName, int options) {
        byte[] bytes = RuntimeHelpers.stringToRawBytes(asString);
        Encoding encoding = EncodingDB.getEncodings().get(encodingName.getBytes()).getEncoding();
        ByteList byteList = new ByteList(bytes, encoding);
        
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "initRegexp",
                MethodType.methodType(RubyRegexp.class, MutableCallSite.class, ThreadContext.class, ByteList.class, int.class));
        init = MethodHandles.insertArguments(init, 2, byteList, options);
        init = MethodHandles.insertArguments(
                init,
                0,
                site);
        site.setTarget(init);
        return site;
    }
    
    public static CallSite getSymbolBootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String symbol) {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "initSymbol",
                MethodType.methodType(RubySymbol.class, MutableCallSite.class, ThreadContext.class, String.class));
        init = MethodHandles.insertArguments(init, 2, symbol);
        init = MethodHandles.insertArguments(
                init,
                0,
                site);
        site.setTarget(init);
        return site;
    }
    
    public static CallSite getFixnumBootstrap(MethodHandles.Lookup lookup, String name, MethodType type, long value) {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "initFixnum",
                MethodType.methodType(RubyFixnum.class, MutableCallSite.class, ThreadContext.class, long.class));
        init = MethodHandles.insertArguments(init, 2, value);
        init = MethodHandles.insertArguments(
                init,
                0,
                site);
        site.setTarget(init);
        return site;
    }
    
    public static CallSite getFloatBootstrap(MethodHandles.Lookup lookup, String name, MethodType type, double value) {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "initFloat",
                MethodType.methodType(RubyFloat.class, MutableCallSite.class, ThreadContext.class, double.class));
        init = MethodHandles.insertArguments(init, 2, value);
        init = MethodHandles.insertArguments(
                init,
                0,
                site);
        site.setTarget(init);
        return site;
    }
    
    public static CallSite getStaticScopeBootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String staticScope) {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "initStaticScope",
                MethodType.methodType(StaticScope.class, MutableCallSite.class, ThreadContext.class, String.class));
        init = MethodHandles.insertArguments(init, 2, staticScope);
        init = MethodHandles.insertArguments(
                init,
                0,
                site);
        site.setTarget(init);
        return site;
    }

    public static CallSite getCallSiteBootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String callName, int callTypeChar) {
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
        
        return new ConstantCallSite(MethodHandles.constant(org.jruby.runtime.CallSite.class, callSite));
    }
    
    public static CallSite getStringBootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String asString, String encodingName, int codeRange) {
        byte[] bytes = RuntimeHelpers.stringToRawBytes(asString);
        Encoding encoding = EncodingDB.getEncodings().get(encodingName.getBytes()).getEncoding();
        ByteList byteList = new ByteList(bytes, encoding);
        
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "newString",
                MethodType.methodType(RubyString.class, ThreadContext.class, ByteList.class, int.class));
        init = MethodHandles.insertArguments(init, 1, byteList, codeRange);
        site.setTarget(init);
        return site;
    }

    public static CallSite getBigIntegerBootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String asString) {
        BigInteger byteList = new BigInteger(asString, 16);
        
        return new ConstantCallSite(MethodHandles.constant(BigInteger.class, byteList));
    }
    
    public static CallSite getEncodingBootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String encodingName) {
        Encoding encoding = EncodingDB.getEncodings().get(encodingName.getBytes()).getEncoding();
        
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "initEncoding",
                MethodType.methodType(RubyEncoding.class, MutableCallSite.class, ThreadContext.class, Encoding.class));
        init = MethodHandles.insertArguments(init, 2, encoding);
        init = MethodHandles.insertArguments(
                init,
                0,
                site);
        site.setTarget(init);
        return site;
    }
    
    public static CallSite getBlockBodyBootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String descriptor) {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "initBlockBody",
                MethodType.methodType(BlockBody.class, MutableCallSite.class, Object.class, ThreadContext.class, String.class));
        init = MethodHandles.insertArguments(init, 3, descriptor);
        init = MethodHandles.insertArguments(
                init,
                0,
                site);
        site.setTarget(init);
        return site;
    }
    
    public static CallSite getBlockBody19Bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String descriptor) {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "initBlockBody19",
                MethodType.methodType(BlockBody.class, MutableCallSite.class, Object.class, ThreadContext.class, String.class));
        init = MethodHandles.insertArguments(init, 3, descriptor);
        init = MethodHandles.insertArguments(
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
        
        MethodHandle target = getTarget(site, name, entry, 0);
        
        if (target == null || ++site.failCount > RubyInstanceConfig.MAX_FAIL_COUNT) {
            target = createFail(FAIL_0, site);
        } else {
            target = postProcess(site, target);
            if (site.getTarget() != null) {
                target = createGWT(TEST_0, target, site.getTarget(), entry, site, false);
            } else {
                target = createGWT(TEST_0, target, FALLBACK_0, entry, site);
            }
        }
        
        site.setTarget(target);

        return (IRubyObject)target.invokeExact(context, caller, self, name);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            return callMethodMissing(entry, site.callType(), context, self, name, arg0);
        }
        
        MethodHandle target = getTarget(site, name, entry, 1);
        
        if (target == null || ++site.failCount > RubyInstanceConfig.MAX_FAIL_COUNT) {
            target = createFail(FAIL_1, site);
        } else {
            target = postProcess(site, target);
            if (site.getTarget() != null) {
                target = createGWT(TEST_1, target, site.getTarget(), entry, site, false);
            } else {
                target = createGWT(TEST_1, target, FALLBACK_1, entry, site);
            }
        }
        
        site.setTarget(target);

        return (IRubyObject)target.invokeExact(context, caller, self, name, arg0);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1);
        }
        
        MethodHandle target = getTarget(site, name, entry, 2);
        
        if (target == null || ++site.failCount > RubyInstanceConfig.MAX_FAIL_COUNT) {
            target = createFail(FAIL_2, site);
        } else {
            target = postProcess(site, target);
            if (site.getTarget() != null) {
                target = createGWT(TEST_2, target, site.getTarget(), entry, site, false);
            } else {
                target = createGWT(TEST_2, target, FALLBACK_2, entry, site);
            }
        }
        
        site.setTarget(target);

        return (IRubyObject)target.invokeExact(context, caller, self, name, arg0, arg1);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, arg2);
        }
        
        MethodHandle target = getTarget(site, name, entry, 3);
        
        if (target == null || ++site.failCount > RubyInstanceConfig.MAX_FAIL_COUNT) {
            target = createFail(FAIL_3, site);
        } else {
            target = postProcess(site, target);
            if (site.getTarget() != null) {
                target = createGWT(TEST_3, target, site.getTarget(), entry, site, false);
            } else {
                target = createGWT(TEST_3, target, FALLBACK_3, entry, site);
            }
        }
        
        site.setTarget(target);

        return (IRubyObject)target.invokeExact(context, caller, self, name, arg0, arg1, arg2);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            return callMethodMissing(entry, site.callType(), context, self, name, args);
        }
        
        MethodHandle target = getTarget(site, name, entry, -1);
        
        if (target == null || ++site.failCount > RubyInstanceConfig.MAX_FAIL_COUNT) {
            target = createFail(FAIL_N, site);
        } else {
            target = postProcess(site, target);
            if (site.getTarget() != null) {
                target = createGWT(TEST_N, target, site.getTarget(), entry, site, false);
            } else {
                target = createGWT(TEST_N, target, FALLBACK_N, entry, site);
            }
        }
        
        site.setTarget(target);

        return (IRubyObject)target.invokeExact(context, caller, self, name, args);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);

        try {
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, block);
            }

            MethodHandle target = getTarget(site, name, entry, 0);

            if (target == null || ++site.failCount > RubyInstanceConfig.MAX_FAIL_COUNT) {
                target = createFail(FAIL_0_B, site);
            } else {
                target = postProcess(site, target);
                if (site.getTarget() != null) {
                    target = createGWT(TEST_0_B, target, site.getTarget(), entry, site, false);
                } else {
                    target = createGWT(TEST_0_B, target, FALLBACK_0_B, entry, site);
                }
            }

            site.setTarget(target);

            return (IRubyObject) target.invokeExact(context, caller, self, name, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            return retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);

        try {
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, block);
            }
            
            MethodHandle target = getTarget(site, name, entry, 1);

            if (target == null || ++site.failCount > RubyInstanceConfig.MAX_FAIL_COUNT) {
                target = createFail(FAIL_1_B, site);
            } else {
                target = postProcess(site, target);
                if (site.getTarget() != null) {
                    target = createGWT(TEST_1_B, target, site.getTarget(), entry, site, false);
                } else {
                    target = createGWT(TEST_1_B, target, FALLBACK_1_B, entry, site);
                }
            }

            site.setTarget(target);

            return (IRubyObject) target.invokeExact(context, caller, self, name, arg0, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            return retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);

        try {
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, block);
            }
            
            MethodHandle target = getTarget(site, name, entry, 2);

            if (target == null || ++site.failCount > RubyInstanceConfig.MAX_FAIL_COUNT) {
                target = createFail(FAIL_2_B, site);
            } else {
                target = postProcess(site, target);
                if (site.getTarget() != null) {
                    target = createGWT(TEST_2_B, target, site.getTarget(), entry, site, false);
                } else {
                    target = createGWT(TEST_2_B, target, FALLBACK_2_B, entry, site);
                }
            }

            site.setTarget(target);

            return (IRubyObject) target.invokeExact(context, caller, self, name, arg0, arg1, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            return retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);

        try {
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, arg2, block);
            }

            MethodHandle target = getTarget(site, name, entry, 3);

            if (target == null || ++site.failCount > RubyInstanceConfig.MAX_FAIL_COUNT) {
                target = createFail(FAIL_3_B, site);
            } else {
                target = postProcess(site, target);
                if (site.getTarget() != null) {
                    target = createGWT(TEST_3_B, target, site.getTarget(), entry, site, false);
                } else {
                    target = createGWT(TEST_3_B, target, FALLBACK_3_B, entry, site);
                }
            }

            site.setTarget(target);

            return (IRubyObject) target.invokeExact(context, caller, self, name, arg0, arg1, arg2, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            return retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);

        try {
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, args, block);
            }
            
            MethodHandle target = getTarget(site, name, entry, -1);

            if (target == null || ++site.failCount > RubyInstanceConfig.MAX_FAIL_COUNT) {
                target = createFail(FAIL_N_B, site);
            } else {
                target = postProcess(site, target);
                if (site.getTarget() != null) {
                    target = createGWT(TEST_N_B, target, site.getTarget(), entry, site, false);
                } else {
                    target = createGWT(TEST_N_B, target, FALLBACK_N_B, entry, site);
                }
            }

            site.setTarget(target);

            return (IRubyObject) target.invokeExact(context, caller, self, name, args, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            return retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject constantFallback(RubyConstantCallSite site, 
            ThreadContext context) {
        IRubyObject value = context.getConstant(site.name());
        
        if (value != null) {
            if (RubyInstanceConfig.LOG_INDY_CONSTANTS) System.out.println("binding constant " + site.name() + " with invokedynamic");
            
            MethodHandle valueHandle = MethodHandles.constant(IRubyObject.class, value);
            valueHandle = MethodHandles.dropArguments(valueHandle, 0, ThreadContext.class);

            MethodHandle fallback = MethodHandles.insertArguments(
                    findStatic(InvokeDynamicSupport.class, "constantFallback",
                    MethodType.methodType(IRubyObject.class, RubyConstantCallSite.class, ThreadContext.class)),
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
        site.setTarget(MethodHandles.dropArguments(MethodHandles.constant(RubyRegexp.class, regexp), 0, ThreadContext.class));
        return regexp;
    }
    
    public static RubySymbol initSymbol(MutableCallSite site, ThreadContext context, String symbol) {
        RubySymbol rubySymbol = context.runtime.newSymbol(symbol);
        site.setTarget(MethodHandles.dropArguments(MethodHandles.constant(RubySymbol.class, rubySymbol), 0, ThreadContext.class));
        return rubySymbol;
    }
    
    public static RubyFixnum initFixnum(MutableCallSite site, ThreadContext context, long value) {
        RubyFixnum rubyFixnum = context.runtime.newFixnum(value);
        site.setTarget(MethodHandles.dropArguments(MethodHandles.constant(RubyFixnum.class, rubyFixnum), 0, ThreadContext.class));
        return rubyFixnum;
    }
    
    public static RubyFloat initFloat(MutableCallSite site, ThreadContext context, double value) {
        RubyFloat rubyFloat = context.runtime.newFloat(value);
        site.setTarget(MethodHandles.dropArguments(MethodHandles.constant(RubyFloat.class, rubyFloat), 0, ThreadContext.class));
        return rubyFloat;
    }
    
    public static StaticScope initStaticScope(MutableCallSite site, ThreadContext context, String staticScope) {
        String[] scopeData = staticScope.split(",");
        String[] varNames = scopeData[0].split(";");
        for (int i = 0; i < varNames.length; i++) {
            varNames[i] = varNames[i].intern();
        }
        StaticScope scope = new LocalStaticScope(context.getCurrentScope().getStaticScope(), varNames);
        site.setTarget(MethodHandles.dropArguments(MethodHandles.constant(StaticScope.class, scope), 0, ThreadContext.class));
        return scope;
    }
    
    public static RubyString newString(ThreadContext context, ByteList contents, int codeRange) {
        return RubyString.newStringShared(context.runtime, contents, codeRange);
    }
    
    public static RubyEncoding initEncoding(MutableCallSite site, ThreadContext context, Encoding encoding) {
        RubyEncoding rubyEncoding = context.runtime.getEncodingService().getEncoding(encoding);
        site.setTarget(MethodHandles.dropArguments(MethodHandles.constant(RubyEncoding.class, rubyEncoding), 0, ThreadContext.class));
        return rubyEncoding;
    }
    
    public static BlockBody initBlockBody(MutableCallSite site, Object scriptObject, ThreadContext context, String descriptor) {
        BlockBody body = RuntimeHelpers.createCompiledBlockBody(context, scriptObject, descriptor);
        site.setTarget(MethodHandles.dropArguments(MethodHandles.constant(BlockBody.class, body), 0, Object.class, ThreadContext.class));
        return body;
    }
    
    public static BlockBody initBlockBody19(MutableCallSite site, Object scriptObject, ThreadContext context, String descriptor) {
        BlockBody body = RuntimeHelpers.createCompiledBlockBody19(context, scriptObject, descriptor);
        site.setTarget(MethodHandles.dropArguments(MethodHandles.constant(BlockBody.class, body), 0, Object.class, ThreadContext.class));
        return body;
    }

    private static MethodHandle createGWT(MethodHandle test, MethodHandle target, MethodHandle fallback, CacheEntry entry, JRubyCallSite site) {
        return createGWT(test, target, fallback, entry, site, true);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // INVOCATION SUPPORT METHODS
    ////////////////////////////////////////////////////////////////////////////

    private static MethodHandle createFail(MethodHandle fail, JRubyCallSite site) {
        MethodHandle myFail = MethodHandles.insertArguments(fail, 0, site);
        myFail = postProcess(site, myFail);
        return myFail;
    }

    private static MethodHandle createGWT(MethodHandle test, MethodHandle target, MethodHandle fallback, CacheEntry entry, JRubyCallSite site, boolean curryFallback) {
        MethodHandle myTest = MethodHandles.insertArguments(test, 0, entry.token);
        MethodHandle myFallback = curryFallback ? MethodHandles.insertArguments(fallback, 0, site) : fallback;
        MethodHandle guardWithTest = MethodHandles.guardWithTest(myTest, target, myFallback);
        
        return guardWithTest;
    }
    
    private static MethodHandle getTarget(JRubyCallSite site, String name, CacheEntry entry, int arity) {
        // only direct invoke if no block passed (for now) and if no frame/scope are required
        if (site.type().parameterArray()[site.type().parameterCount() - 1] != Block.class &&
                entry.method.getCallConfig() == CallConfiguration.FrameNoneScopeNone) {
            if (entry.method instanceof AttrReaderMethod || entry.method instanceof AttrWriterMethod) {
                if (RubyInstanceConfig.LOG_INDY_BINDINGS) System.out.println("binding attr target: " + name);
                
                return getAttrTarget(entry.method);
            }

            DynamicMethod.NativeCall nativeCall = entry.method.getNativeCall();
            
            if (nativeCall != null) {
                if (!nativeCall.isJava()

                        // incoming is IRubyObject[], outgoing is not; mismatch
                        && getArgCount(nativeCall.getNativeSignature(), nativeCall.isStatic()) == 4
                        && site.type().parameterArray()[site.type().parameterCount() - 1] != IRubyObject[].class

                        // outgoing is IRubyObject[], incoming is not; mismatch
                        || getArgCount(nativeCall.getNativeSignature(), nativeCall.isStatic()) != 4
                        && site.type().parameterArray()[site.type().parameterCount() - 1] != IRubyObject[].class

                        // incoming and outgoing arg count mismatch
                        || site.type().parameterCount() - 4 != getArgCount(nativeCall.getNativeSignature(), nativeCall.isStatic())) {

                    // fall back on DynamicMethod.call for now

                } else if (nativeCall.isJava()
                        && !(nativeCall.getNativeSignature().length == 0 && site.type().parameterCount() == 4)) {
                    // only no-arg Java methods are bound directly right now

                } else {
                    MethodHandle nativeTarget = handleForMethod(site, entry.method);

                    if (nativeTarget != null) return nativeTarget;
                }
            }
        }
        
        // if indirect indy-bound methods (via DynamicMethod.call) are disabled, fail permanently
        if (!RubyInstanceConfig.INVOKEDYNAMIC_INDIRECT) {
            return null;
        }
        
        // no direct native path, use DynamicMethod.call target provided
        if (RubyInstanceConfig.LOG_INDY_BINDINGS) System.out.println("binding " + name + " as DynamicMethod.call");
        
        return MethodHandles.insertArguments(getDynamicMethodTarget(site.type(), arity), 0, entry);
    }
    
    private static MethodHandle handleForMethod(JRubyCallSite site, DynamicMethod method) {
        MethodHandle nativeTarget = null;
        
        if (method.getHandle() != null) {
            nativeTarget = (MethodHandle)method.getHandle();
        } else {
            if (method.getNativeCall() != null) {
                DynamicMethod.NativeCall nativeCall = method.getNativeCall();
                
                if (nativeCall.isJava() && RubyInstanceConfig.INVOKEDYNAMIC_JAVA) {
                    // Ruby to Java
                    if (RubyInstanceConfig.LOG_INDY_BINDINGS) System.out.println("binding java target: " + nativeCall);
                    nativeTarget = createJavaHandle(method);
                } else if (method instanceof CompiledMethod) {
                    // Ruby to Ruby
                    if (RubyInstanceConfig.LOG_INDY_BINDINGS) System.out.println("binding ruby target: " + nativeCall);
                    nativeTarget = createRubyHandle(site, method);
                } else {
                    // Ruby to Core
                    if (RubyInstanceConfig.LOG_INDY_BINDINGS) System.out.println("binding native target: " + nativeCall);
                    nativeTarget = createNativeHandle(method);
                }
                        
                // add NULL_BLOCK if needed
                if (nativeTarget != null
                        && site.type().parameterCount() > 0
                        && site.type().parameterArray()[site.type().parameterCount() - 1] != Block.class
                        && nativeTarget.type().parameterCount() > 0
                        && nativeTarget.type().parameterArray()[nativeTarget.type().parameterCount() - 1] == Block.class) {

                    nativeTarget = MethodHandles.insertArguments(nativeTarget, nativeTarget.type().parameterCount() - 1, Block.NULL_BLOCK);
                }
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
    
    private static MethodHandle postProcess(JRubyCallSite site, MethodHandle target) {
        // if it's an attr assignment, need to return n-1th argument
        if (site.isAttrAssign()) {
            // return given argument
            MethodHandle newTarget = MethodHandles.identity(IRubyObject.class);
            
            // if args are IRubyObject[].class, yank out n-1th
            if (site.type().parameterArray()[site.type().parameterCount() - 1] == IRubyObject[].class) {
                newTarget = MethodHandles.filterArguments(newTarget, 0, findStatic(InvokeDynamicSupport.class, "getLast", MethodType.methodType(IRubyObject.class, IRubyObject[].class))); 
            }
            
            // drop standard preamble args plus extra args
            newTarget = MethodHandles.dropArguments(newTarget, 0, IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class);
            
            // drop extra arguments, if any
            MethodType dropped = target.type().dropParameterTypes(0, 4);
            if (dropped.parameterCount() > 1) {
                Class[] drops = new Class[dropped.parameterCount() - 1];
                Arrays.fill(drops, IRubyObject.class);
                newTarget = MethodHandles.dropArguments(newTarget, 5, drops);
            }
            
            // fold using target
            return MethodHandles.foldArguments(newTarget, target);
        } else {
            return target;
        }
    }

    private static int getRubyArgCount(Class[] args) {
        return args.length - 4;
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

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, Block block) throws Throwable {
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

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) throws Throwable {
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

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) throws Throwable {
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

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args, Block block) throws Throwable {
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
            nativeTarget = findStatic(nativeCall.getNativeTarget(), nativeCall.getNativeName(), MethodType.methodType(nativeCall.getNativeReturn(), nativeCall.getNativeSignature()));
            
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
                    nativeTarget = MethodHandles.explicitCastArguments(nativeTarget, MethodType.methodType(long.class));
                    returnFilter = MethodHandles.insertArguments(
                            findStatic(RubyFixnum.class, "newFixnum", MethodType.methodType(RubyFixnum.class, Ruby.class, long.class)),
                            0,
                            runtime);
                } else if (nativeCall.getNativeReturn() == float.class ||
                        nativeCall.getNativeReturn() == double.class ||
                        nativeCall.getNativeReturn() == Float.class ||
                        nativeCall.getNativeReturn() == Double.class) {
                    nativeTarget = MethodHandles.explicitCastArguments(nativeTarget, MethodType.methodType(double.class));
                    returnFilter = MethodHandles.insertArguments(
                            findStatic(RubyFloat.class, "newFloat", MethodType.methodType(RubyFloat.class, Ruby.class, double.class)),
                            0,
                            runtime);
                } else if (nativeCall.getNativeReturn() == boolean.class ||
                        nativeCall.getNativeReturn() == Boolean.class) {
                    nativeTarget = MethodHandles.explicitCastArguments(nativeTarget, MethodType.methodType(boolean.class));
                    returnFilter = MethodHandles.insertArguments(
                            findStatic(RubyBoolean.class, "newBoolean", MethodType.methodType(RubyBoolean.class, Ruby.class, boolean.class)),
                            0,
                            runtime);
                } else if (CharSequence.class.isAssignableFrom(nativeCall.getNativeReturn())) {
                    nativeTarget = MethodHandles.explicitCastArguments(nativeTarget, MethodType.methodType(CharSequence.class));
                    returnFilter = MethodHandles.insertArguments(
                            findStatic(RubyString.class, "newUnicodeString", MethodType.methodType(RubyString.class, Ruby.class, CharSequence.class)),
                            0,
                            runtime);
                } else if (nativeCall.getNativeReturn() == void.class) {
                    returnFilter = MethodHandles.constant(IRubyObject.class, runtime.getNil());
                }

                // we can handle this; do remaining transforms and return
                if (returnFilter != null) {
                    nativeTarget = MethodHandles.filterReturnValue(nativeTarget, returnFilter);
                    nativeTarget = MethodHandles.explicitCastArguments(nativeTarget, MethodType.methodType(IRubyObject.class));
                    nativeTarget = MethodHandles.dropArguments(nativeTarget, 0, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class);
                    
                    method.setHandle(nativeTarget);
                    return nativeTarget;
                }
            }
        } else {
            nativeTarget = findVirtual(nativeCall.getNativeTarget(), nativeCall.getNativeName(), MethodType.methodType(nativeCall.getNativeReturn(), nativeCall.getNativeSignature()));
            
            if (nativeCall.getNativeSignature().length == 0) {
                // convert target
                nativeTarget = MethodHandles.filterArguments(
                        nativeTarget,
                        0,
                        MethodHandles.explicitCastArguments(
                                findStatic(JavaUtil.class, "objectFromJavaProxy", MethodType.methodType(Object.class, IRubyObject.class)),
                                MethodType.methodType(nativeCall.getNativeTarget(), IRubyObject.class)));
                
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
                    nativeTarget = MethodHandles.explicitCastArguments(nativeTarget, MethodType.methodType(long.class, IRubyObject.class));
                    returnFilter = MethodHandles.insertArguments(
                            findStatic(RubyFixnum.class, "newFixnum", MethodType.methodType(RubyFixnum.class, Ruby.class, long.class)),
                            0,
                            runtime);
                } else if (nativeCall.getNativeReturn() == float.class ||
                        nativeCall.getNativeReturn() == double.class ||
                        nativeCall.getNativeReturn() == Float.class ||
                        nativeCall.getNativeReturn() == Double.class) {
                    nativeTarget = MethodHandles.explicitCastArguments(nativeTarget, MethodType.methodType(double.class, IRubyObject.class));
                    returnFilter = MethodHandles.insertArguments(
                            findStatic(RubyFloat.class, "newFloat", MethodType.methodType(RubyFloat.class, Ruby.class, double.class)),
                            0,
                            runtime);
                } else if (nativeCall.getNativeReturn() == boolean.class ||
                        nativeCall.getNativeReturn() == Boolean.class) {
                    nativeTarget = MethodHandles.explicitCastArguments(nativeTarget, MethodType.methodType(boolean.class, IRubyObject.class));
                    returnFilter = MethodHandles.insertArguments(
                            findStatic(RubyBoolean.class, "newBoolean", MethodType.methodType(RubyBoolean.class, Ruby.class, boolean.class)),
                            0,
                            runtime);
                } else if (CharSequence.class.isAssignableFrom(nativeCall.getNativeReturn())) {
                    nativeTarget = MethodHandles.explicitCastArguments(nativeTarget, MethodType.methodType(CharSequence.class, IRubyObject.class));
                    returnFilter = MethodHandles.insertArguments(
                            findStatic(RubyString.class, "newUnicodeString", MethodType.methodType(RubyString.class, Ruby.class, CharSequence.class)),
                            0,
                            runtime);
                } else if (nativeCall.getNativeReturn() == void.class) {
                    returnFilter = MethodHandles.constant(IRubyObject.class, runtime.getNil());
                }

                // we can handle this; do remaining transforms and return
                if (returnFilter != null) {
                    nativeTarget = MethodHandles.filterReturnValue(nativeTarget, returnFilter);
                    nativeTarget = MethodHandles.explicitCastArguments(nativeTarget, MethodType.methodType(IRubyObject.class, IRubyObject.class));
                    nativeTarget = MethodHandles.permuteArguments(
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

    private static MethodHandle createNativeHandle(DynamicMethod method) {
        MethodHandle nativeTarget = null;
        
        if (method.getCallConfig() == CallConfiguration.FrameNoneScopeNone) {
            DynamicMethod.NativeCall nativeCall = method.getNativeCall();
            Class[] nativeSig = nativeCall.getNativeSignature();
            boolean isStatic = nativeCall.isStatic();
            
            try {
                if (isStatic) {
                    nativeTarget = MethodHandles.lookup().findStatic(
                            nativeCall.getNativeTarget(),
                            nativeCall.getNativeName(),
                            MethodType.methodType(nativeCall.getNativeReturn(),
                            nativeCall.getNativeSignature()));
                } else {
                    nativeTarget = MethodHandles.lookup().findVirtual(
                            nativeCall.getNativeTarget(),
                            nativeCall.getNativeName(),
                            MethodType.methodType(nativeCall.getNativeReturn(),
                            nativeCall.getNativeSignature()));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
            if (getArgCount(nativeSig, nativeCall.isStatic()) != -1) {
                int argCount = getArgCount(nativeCall.getNativeSignature(), isStatic);
                MethodType inboundType = STANDARD_NATIVE_TYPES_BLOCK[argCount];
                
                if (nativeSig.length > 0) {
                    int[] permute;
                    MethodType convert;
                    if (nativeSig[0] == ThreadContext.class) {
                        if (nativeSig[nativeSig.length - 1] == Block.class) {
                            convert = isStatic ? TARGET_TC_SELF_ARGS_BLOCK[argCount] : TARGET_SELF_TC_ARGS_BLOCK[argCount];
                            permute = isStatic ? TC_SELF_ARGS_BLOCK_PERMUTES[argCount] : SELF_TC_ARGS_BLOCK_PERMUTES[argCount];
                        } else {
                            convert = isStatic ? TARGET_TC_SELF_ARGS[argCount] : TARGET_SELF_TC_ARGS[argCount];
                            permute = isStatic ? TC_SELF_ARGS_PERMUTES[argCount] : SELF_TC_ARGS_PERMUTES[argCount];
                        }
                    } else {
                        if (nativeSig[nativeSig.length - 1] == Block.class) {
                            convert = TARGET_SELF_ARGS_BLOCK[argCount];
                            permute = SELF_ARGS_BLOCK_PERMUTES[argCount];
                        } else {
                            convert = TARGET_SELF_ARGS[argCount];
                            permute = SELF_ARGS_PERMUTES[argCount];
                        }
                    }

                    nativeTarget = MethodHandles.explicitCastArguments(nativeTarget, convert);
                    nativeTarget = MethodHandles.permuteArguments(nativeTarget, inboundType, permute);
                    method.setHandle(nativeTarget);
                    return nativeTarget;
                }
            }
        }
        
        // can't build native handle for it
        return null;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Dispatch via direct handle to Ruby method
    ////////////////////////////////////////////////////////////////////////////

    private static MethodHandle createRubyHandle(JRubyCallSite site, DynamicMethod method) {
        DynamicMethod.NativeCall nativeCall = method.getNativeCall();
        MethodHandle nativeTarget;
        
        try {
            nativeTarget = MethodHandles.lookup().findStatic(
                    nativeCall.getNativeTarget(),
                    nativeCall.getNativeName(),
                    MethodType.methodType(nativeCall.getNativeReturn(),
                    nativeCall.getNativeSignature()));
            CompiledMethod cm = (CompiledMethod)method;
            nativeTarget = MethodHandles.insertArguments(nativeTarget, 0, cm.getScriptObject());
            nativeTarget = MethodHandles.insertArguments(nativeTarget, nativeTarget.type().parameterCount() - 1, Block.NULL_BLOCK);
            
            // juggle args into correct places
            int argCount = getRubyArgCount(nativeCall.getNativeSignature());
            switch (argCount) {
                case 0:
                    nativeTarget = MethodHandles.permuteArguments(nativeTarget, site.type(), new int[] {0, 2});
                    break;
                case -1:
                case 1:
                    nativeTarget = MethodHandles.permuteArguments(nativeTarget, site.type(), new int[] {0, 2, 4});
                    break;
                case 2:
                    nativeTarget = MethodHandles.permuteArguments(nativeTarget, site.type(), new int[] {0, 2, 4, 5});
                    break;
                case 3:
                    nativeTarget = MethodHandles.permuteArguments(nativeTarget, site.type(), new int[] {0, 2, 4, 5, 6});
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

    private static MethodHandle getAttrTarget(DynamicMethod method) {
        MethodHandle target = (MethodHandle)method.getHandle();
        if (target != null) return target;
        
        try {
            if (method instanceof AttrReaderMethod) {
                AttrReaderMethod reader = (AttrReaderMethod)method;
                target = MethodHandles.lookup().findVirtual(
                        AttrReaderMethod.class,
                        "call",
                        MethodType.methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class));
                target = MethodHandles.explicitCastArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class));
                target = MethodHandles.permuteArguments(
                        target,
                        MethodType.methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class),
                        new int[] {0,2,4,1,5});
                // IRubyObject, DynamicMethod, RubyClass, ThreadContext, IRubyObject, IRubyObject, String
                target = MethodHandles.insertArguments(target, 0, reader);
                // IRubyObject, RubyClass, ThreadContext, IRubyObject, IRubyObject, String
                target = MethodHandles.foldArguments(target, PGC2_0);
                // IRubyObject, ThreadContext, IRubyObject, IRubyObject, String
            } else {
                AttrWriterMethod writer = (AttrWriterMethod)method;
                target = MethodHandles.lookup().findVirtual(
                        AttrWriterMethod.class,
                        "call",
                        MethodType.methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class));
                target = MethodHandles.explicitCastArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class));
                target = MethodHandles.permuteArguments(
                        target,
                        MethodType.methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class),
                        new int[] {0,2,4,1,5,6});
                // IRubyObject, DynamicMethod, RubyClass, ThreadContext, IRubyObject, IRubyObject, String
                target = MethodHandles.insertArguments(target, 0, writer);
                // IRubyObject, RubyClass, ThreadContext, IRubyObject, IRubyObject, String
                target = MethodHandles.foldArguments(target, PGC2_1);
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
        MethodHandle getMethod = findStatic(InvokeDynamicSupport.class, "getMethod", MethodType.methodType(DynamicMethod.class, CacheEntry.class));
        getMethod = MethodHandles.dropArguments(getMethod, 0, RubyClass.class);
        getMethod = MethodHandles.dropArguments(getMethod, 2, ThreadContext.class, IRubyObject.class, IRubyObject.class);
        GETMETHOD = getMethod;
    }

    public static final DynamicMethod getMethod(CacheEntry entry) {
        return entry.method;
    }

    private static final MethodHandle PGC = MethodHandles.dropArguments(
            MethodHandles.dropArguments(
                findStatic(InvokeDynamicSupport.class, "pollAndGetClass",
                    MethodType.methodType(RubyClass.class, ThreadContext.class, IRubyObject.class)),
                1,
                IRubyObject.class),
            0,
            CacheEntry.class);

    private static final MethodHandle PGC2 = MethodHandles.dropArguments(
            findStatic(InvokeDynamicSupport.class, "pollAndGetClass",
                MethodType.methodType(RubyClass.class, ThreadContext.class, IRubyObject.class)),
            1,
            IRubyObject.class);

    private static final MethodHandle TEST = MethodHandles.dropArguments(
            findStatic(InvokeDynamicSupport.class, "test",
                MethodType.methodType(boolean.class, int.class, IRubyObject.class)),
            1,
            ThreadContext.class, IRubyObject.class);

    private static MethodHandle dropNameAndArgs(MethodHandle original, int index, int count, boolean block) {
        switch (count) {
        case -1:
            if (block) {
                return MethodHandles.dropArguments(original, index, String.class, IRubyObject[].class, Block.class);
            } else {
                return MethodHandles.dropArguments(original, index, String.class, IRubyObject[].class);
            }
        case 0:
            if (block) {
                return MethodHandles.dropArguments(original, index, String.class, Block.class);
            } else {
                return MethodHandles.dropArguments(original, index, String.class);
            }
        case 1:
            if (block) {
                return MethodHandles.dropArguments(original, index, String.class, IRubyObject.class, Block.class);
            } else {
                return MethodHandles.dropArguments(original, index, String.class, IRubyObject.class);
            }
        case 2:
            if (block) {
                return MethodHandles.dropArguments(original, index, String.class, IRubyObject.class, IRubyObject.class, Block.class);
            } else {
                return MethodHandles.dropArguments(original, index, String.class, IRubyObject.class, IRubyObject.class);
            }
        case 3:
            if (block) {
                return MethodHandles.dropArguments(original, index, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class);
            } else {
                return MethodHandles.dropArguments(original, index, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class);
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
            MethodType.methodType(void.class, ThreadContext.class, RubyModule.class, String.class, IRubyObject.class, Block.class, StaticScope.class);
    
    private static final MethodHandle PRE_METHOD_FRAME_AND_SCOPE =
            findStatic(InvokeDynamicSupport.class, "preMethodFrameAndScope", PRE_METHOD_TYPE);
    private static final MethodHandle POST_METHOD_FRAME_AND_SCOPE =
            findVirtual(ThreadContext.class, "postMethodFrameAndScope", MethodType.methodType(void.class));
    
    private static final MethodHandle PRE_METHOD_FRAME_AND_DUMMY_SCOPE =
            findStatic(InvokeDynamicSupport.class, "preMethodFrameAndDummyScope", PRE_METHOD_TYPE);
    private static final MethodHandle FRAME_FULL_SCOPE_DUMMY_POST = POST_METHOD_FRAME_AND_SCOPE;
    
    private static final MethodHandle PRE_METHOD_FRAME_ONLY =
            findStatic(InvokeDynamicSupport.class, "preMethodFrameOnly", PRE_METHOD_TYPE);
    private static final MethodHandle POST_METHOD_FRAME_ONLY =
            findVirtual(ThreadContext.class, "postMethodFrameOnly", MethodType.methodType(void.class));
    
    private static final MethodHandle PRE_METHOD_SCOPE_ONLY =
            findStatic(InvokeDynamicSupport.class, "preMethodScopeOnly", PRE_METHOD_TYPE);
    private static final MethodHandle POST_METHOD_SCOPE_ONLY = 
            findVirtual(ThreadContext.class, "postMethodScopeOnly", MethodType.methodType(void.class));
    
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
                MethodType.methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class));
        target = MethodHandles.explicitCastArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class));
        target = MethodHandles.permuteArguments(
                target,
                MethodType.methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class),
                new int[] {0,3,5,1,6});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String
        target = MethodHandles.foldArguments(target, GETMETHOD_0);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String
        target = MethodHandles.foldArguments(target, PGC_0);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String
        TARGET_0 = target;
    }
    private static final MethodHandle FALLBACK_0 = findStatic(InvokeDynamicSupport.class, "invocationFallback",
            MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class));
    private static final MethodHandle FAIL_0 = findStatic(InvokeDynamicSupport.class, "fail",
            MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class));

    private static final MethodHandle PGC_1 = dropNameAndArgs(PGC, 4, 1, false);
    private static final MethodHandle PGC2_1 = dropNameAndArgs(PGC2, 3, 1, false);
    private static final MethodHandle GETMETHOD_1 = dropNameAndArgs(GETMETHOD, 5, 1, false);
    private static final MethodHandle TEST_1 = dropNameAndArgs(TEST, 4, 1, false);
    private static final MethodHandle TARGET_1;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                MethodType.methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class));
        // IRubyObject, DynamicMethod, ThreadContext, IRubyObject, RubyModule, String, IRubyObject
        target = MethodHandles.explicitCastArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class));
        // IRubyObject, DynamicMethod, ThreadContext, IRubyObject, RubyClass, String, IRubyObject
        target = MethodHandles.permuteArguments(
                target,
                MethodType.methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class),
                new int[] {0,3,5,1,6,7});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, IRubyObject
        target = MethodHandles.foldArguments(target, GETMETHOD_1);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, IRubyObject
        target = MethodHandles.foldArguments(target, PGC_1);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, IRubyObject
        TARGET_1 = target;
    }
    private static final MethodHandle FALLBACK_1 = findStatic(InvokeDynamicSupport.class, "invocationFallback",
            MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class));
    private static final MethodHandle FAIL_1 = findStatic(InvokeDynamicSupport.class, "fail",
            MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class));

    private static final MethodHandle PGC_2 = dropNameAndArgs(PGC, 4, 2, false);
    private static final MethodHandle GETMETHOD_2 = dropNameAndArgs(GETMETHOD, 5, 2, false);
    private static final MethodHandle TEST_2 = dropNameAndArgs(TEST, 4, 2, false);
    private static final MethodHandle TARGET_2;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                MethodType.methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class));
        target = MethodHandles.explicitCastArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class, IRubyObject.class));
        target = MethodHandles.permuteArguments(
                target,
                MethodType.methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class),
                new int[] {0,3,5,1,6,7,8});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = MethodHandles.foldArguments(target, GETMETHOD_2);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = MethodHandles.foldArguments(target, PGC_2);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        TARGET_2 = target;
    }
    private static final MethodHandle FALLBACK_2 = findStatic(InvokeDynamicSupport.class, "invocationFallback",
            MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class));
    private static final MethodHandle FAIL_2 = findStatic(InvokeDynamicSupport.class, "fail",
            MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class));

    private static final MethodHandle PGC_3 = dropNameAndArgs(PGC, 4, 3, false);
    private static final MethodHandle GETMETHOD_3 = dropNameAndArgs(GETMETHOD, 5, 3, false);
    private static final MethodHandle TEST_3 = dropNameAndArgs(TEST, 4, 3, false);
    private static final MethodHandle TARGET_3;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                MethodType.methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
        target = MethodHandles.explicitCastArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
        target = MethodHandles.permuteArguments(
                target,
                MethodType.methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class),
                new int[] {0,3,5,1,6,7,8,9});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = MethodHandles.foldArguments(target, GETMETHOD_3);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = MethodHandles.foldArguments(target, PGC_3);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        TARGET_3 = target;
    }
    private static final MethodHandle FALLBACK_3 = findStatic(InvokeDynamicSupport.class, "invocationFallback",
            MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
    private static final MethodHandle FAIL_3 = findStatic(InvokeDynamicSupport.class, "fail",
            MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));

    private static final MethodHandle PGC_N = dropNameAndArgs(PGC, 4, -1, false);
    private static final MethodHandle GETMETHOD_N = dropNameAndArgs(GETMETHOD, 5, -1, false);
    private static final MethodHandle TEST_N = dropNameAndArgs(TEST, 4, -1, false);
    private static final MethodHandle TARGET_N;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                MethodType.methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject[].class));
        target = MethodHandles.explicitCastArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject[].class));
        target = MethodHandles.permuteArguments(
                target,
                MethodType.methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class),
                new int[] {0,3,5,1,6,7});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = MethodHandles.foldArguments(target, GETMETHOD_N);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = MethodHandles.foldArguments(target, PGC_N);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        TARGET_N = target;
    }
    private static final MethodHandle FALLBACK_N = findStatic(InvokeDynamicSupport.class, "invocationFallback",
            MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class));
    private static final MethodHandle FAIL_N = findStatic(InvokeDynamicSupport.class, "fail",
            MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class));

    private static final MethodHandle BREAKJUMP;
    static {
        MethodHandle breakJump = findStatic(
                InvokeDynamicSupport.class,
                "handleBreakJump",
                MethodType.methodType(IRubyObject.class, JumpException.BreakJump.class, ThreadContext.class));
        // BreakJump, ThreadContext
        breakJump = MethodHandles.permuteArguments(
                breakJump,
                MethodType.methodType(IRubyObject.class, JumpException.BreakJump.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class),
                new int[] {0,2});
        // BreakJump, CacheEntry, ThreadContext, IRubyObject, IRubyObject
        BREAKJUMP = breakJump;
    }

    private static final MethodHandle RETRYJUMP;
    static {
        MethodHandle retryJump = findStatic(
                InvokeDynamicSupport.class,
                "retryJumpError",
                MethodType.methodType(IRubyObject.class, ThreadContext.class));
        // ThreadContext
        retryJump = MethodHandles.permuteArguments(
                retryJump,
                MethodType.methodType(IRubyObject.class, JumpException.RetryJump.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class),
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
                MethodType.methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, Block.class));
        target = MethodHandles.explicitCastArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, Block.class));
        target = MethodHandles.permuteArguments(
                target,
                MethodType.methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, Block.class),
                new int[] {0,3,5,1,6,7});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = MethodHandles.foldArguments(target, GETMETHOD_0_B);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = MethodHandles.foldArguments(target, PGC_0_B);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args

        MethodHandle breakJump = dropNameAndArgs(BREAKJUMP, 5, 0, true);
        MethodHandle retryJump = dropNameAndArgs(RETRYJUMP, 5, 0, true);
        target = MethodHandles.catchException(target, JumpException.BreakJump.class, breakJump);
        target = MethodHandles.catchException(target, JumpException.RetryJump.class, retryJump);

        TARGET_0_B = target;
    }
    private static final MethodHandle FALLBACK_0_B = findStatic(InvokeDynamicSupport.class, "invocationFallback",
            MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, Block.class));
    private static final MethodHandle FAIL_0_B = findStatic(InvokeDynamicSupport.class, "fail",
            MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, Block.class));

    private static final MethodHandle PGC_1_B = dropNameAndArgs(PGC, 4, 1, true);
    private static final MethodHandle GETMETHOD_1_B = dropNameAndArgs(GETMETHOD, 5, 1, true);
    private static final MethodHandle TEST_1_B = dropNameAndArgs(TEST, 4, 1, true);
    private static final MethodHandle TARGET_1_B;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                MethodType.methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, Block.class));
        target = MethodHandles.explicitCastArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class, Block.class));
        target = MethodHandles.permuteArguments(
                target,
                MethodType.methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, Block.class),
                new int[] {0,3,5,1,6,7,8});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = MethodHandles.foldArguments(target, GETMETHOD_1_B);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = MethodHandles.foldArguments(target, PGC_1_B);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args

        MethodHandle breakJump = dropNameAndArgs(BREAKJUMP, 5, 1, true);
        MethodHandle retryJump = dropNameAndArgs(RETRYJUMP, 5, 1, true);
        target = MethodHandles.catchException(target, JumpException.BreakJump.class, breakJump);
        target = MethodHandles.catchException(target, JumpException.RetryJump.class, retryJump);

        TARGET_1_B = target;
    }
    private static final MethodHandle FALLBACK_1_B = findStatic(InvokeDynamicSupport.class, "invocationFallback",
            MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, Block.class));
    private static final MethodHandle FAIL_1_B = findStatic(InvokeDynamicSupport.class, "fail",
            MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, Block.class));

    private static final MethodHandle PGC_2_B = dropNameAndArgs(PGC, 4, 2, true);
    private static final MethodHandle GETMETHOD_2_B = dropNameAndArgs(GETMETHOD, 5, 2, true);
    private static final MethodHandle TEST_2_B = dropNameAndArgs(TEST, 4, 2, true);
    private static final MethodHandle TARGET_2_B;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                MethodType.methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));
        target = MethodHandles.explicitCastArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));
        target = MethodHandles.permuteArguments(
                target,
                MethodType.methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, Block.class),
                new int[] {0,3,5,1,6,7,8,9});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = MethodHandles.foldArguments(target, GETMETHOD_2_B);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = MethodHandles.foldArguments(target, PGC_2_B);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args

        MethodHandle breakJump = dropNameAndArgs(BREAKJUMP, 5, 2, true);
        MethodHandle retryJump = dropNameAndArgs(RETRYJUMP, 5, 2, true);
        target = MethodHandles.catchException(target, JumpException.BreakJump.class, breakJump);
        target = MethodHandles.catchException(target, JumpException.RetryJump.class, retryJump);

        TARGET_2_B = target;
    }
    private static final MethodHandle FALLBACK_2_B = findStatic(InvokeDynamicSupport.class, "invocationFallback",
            MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));
    private static final MethodHandle FAIL_2_B = findStatic(InvokeDynamicSupport.class, "fail",
            MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));

    private static final MethodHandle PGC_3_B = dropNameAndArgs(PGC, 4, 3, true);
    private static final MethodHandle GETMETHOD_3_B = dropNameAndArgs(GETMETHOD, 5, 3, true);
    private static final MethodHandle TEST_3_B = dropNameAndArgs(TEST, 4, 3, true);
    private static final MethodHandle TARGET_3_B;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                MethodType.methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
        target = MethodHandles.explicitCastArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
        target = MethodHandles.permuteArguments(
                target,
                MethodType.methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class),
                new int[] {0,3,5,1,6,7,8,9,10});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = MethodHandles.foldArguments(target, GETMETHOD_3_B);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = MethodHandles.foldArguments(target, PGC_3_B);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args

        MethodHandle breakJump = findStatic(InvokeDynamicSupport.class, "handleBreakJump", MethodType.methodType(IRubyObject.class, JumpException.BreakJump.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
        MethodHandle retryJump = dropNameAndArgs(RETRYJUMP, 5, 3, true);
        target = MethodHandles.catchException(target, JumpException.BreakJump.class, breakJump);
        target = MethodHandles.catchException(target, JumpException.RetryJump.class, retryJump);
        
        TARGET_3_B = target;
    }
    private static final MethodHandle FALLBACK_3_B = findStatic(InvokeDynamicSupport.class, "invocationFallback",
            MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
    private static final MethodHandle FAIL_3_B = findStatic(InvokeDynamicSupport.class, "fail",
            MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));

    private static final MethodHandle PGC_N_B = dropNameAndArgs(PGC, 4, -1, true);
    private static final MethodHandle GETMETHOD_N_B = dropNameAndArgs(GETMETHOD, 5, -1, true);
    private static final MethodHandle TEST_N_B = dropNameAndArgs(TEST, 4, -1, true);
    private static final MethodHandle TARGET_N_B;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                MethodType.methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject[].class, Block.class));
        target = MethodHandles.explicitCastArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject[].class, Block.class));
        target = MethodHandles.permuteArguments(
                target,
                MethodType.methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class, Block.class),
                new int[] {0,3,5,1,6,7,8});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = MethodHandles.foldArguments(target, GETMETHOD_N_B);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = MethodHandles.foldArguments(target, PGC_N_B);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args

        MethodHandle breakJump = dropNameAndArgs(BREAKJUMP, 5, -1, true);
        MethodHandle retryJump = dropNameAndArgs(RETRYJUMP, 5, -1, true);
        target = MethodHandles.catchException(target, JumpException.BreakJump.class, breakJump);
        target = MethodHandles.catchException(target, JumpException.RetryJump.class, retryJump);
        
        TARGET_N_B = target;
    }
    private static final MethodHandle FALLBACK_N_B = findStatic(InvokeDynamicSupport.class, "invocationFallback",
                    MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class, Block.class));
    private static final MethodHandle FAIL_N_B = findStatic(InvokeDynamicSupport.class, "fail",
                    MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class, Block.class));
    
    ////////////////////////////////////////////////////////////////////////////
    // Utility methods for lookup
    ////////////////////////////////////////////////////////////////////////////
    
    private static MethodHandle findStatic(Class target, String name, MethodType type) {
        try {
            return MethodHandles.lookup().findStatic(target, name, type);
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        } catch (IllegalAccessException nae) {
            throw new RuntimeException(nae);
        }
    }
    private static MethodHandle findVirtual(Class target, String name, MethodType type) {
        try {
            return MethodHandles.lookup().findVirtual(target, name, type);
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        } catch (IllegalAccessException nae) {
            throw new RuntimeException(nae);
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Support method types and permutations
    ////////////////////////////////////////////////////////////////////////////
    
    private static final MethodType STANDARD_NATIVE_TYPE = MethodType.methodType(
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
    
    private static final MethodType TARGET_SELF = MethodType.methodType(
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
    
    private static final MethodType TARGET_TC_SELF = MethodType.methodType(
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