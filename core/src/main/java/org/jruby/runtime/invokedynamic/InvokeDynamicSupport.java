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

import org.jruby.runtime.ivars.VariableAccessor;
import com.headius.invokebinder.Binder;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.SwitchPoint;
import java.math.BigInteger;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jruby.*;
import org.jruby.ast.executable.AbstractScript;
import org.jruby.exceptions.JumpException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Helpers;
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
import org.jruby.internal.runtime.GlobalVariable;
import org.jruby.runtime.ivars.FieldVariableAccessor;
import org.jruby.runtime.opto.Invalidator;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.cli.Options;

@SuppressWarnings("deprecation")
public class InvokeDynamicSupport {
    private static final Logger LOG = LoggerFactory.getLogger("InvokeDynamicSupport");
    
    ////////////////////////////////////////////////////////////////////////////
    // BOOTSTRAP HANDLES
    ////////////////////////////////////////////////////////////////////////////
    
    public final static String BOOTSTRAP_BARE_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class);
    public final static String BOOTSTRAP_INT_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, int.class);
    public final static String BOOTSTRAP_STRING_STRING_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, String.class);
    public final static String BOOTSTRAP_STRING_STRING_INT_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, String.class, int.class);
    public final static String BOOTSTRAP_STRING_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class);
    public final static String BOOTSTRAP_STRING_CALLTYPE_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, CallType.class);
    public final static String BOOTSTRAP_LONG_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, long.class);
    public final static String BOOTSTRAP_DOUBLE_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, double.class);
    public final static String BOOTSTRAP_LONG_STRING_INT_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, long.class, String.class, int.class);
    public final static String BOOTSTRAP_DOUBLE_STRING_INT_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, double.class, String.class, int.class);
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
        return getBootstrapHandle("invocationBootstrap", InvocationLinker.class, BOOTSTRAP_STRING_INT_SIG);
    }
    
    public static Handle getConstantHandle() {
        return getBootstrapHandle("getConstantBootstrap", BOOTSTRAP_INT_SIG);
    }
    
    public static Handle getConstantBooleanHandle() {
        return getBootstrapHandle("getConstantBooleanBootstrap", BOOTSTRAP_INT_SIG);
    }
    
    public static Handle getByteListHandle() {
        return getBootstrapHandle("getByteListBootstrap", BOOTSTRAP_STRING_STRING_SIG);
    }
    
    public static Handle getRegexpHandle() {
        return getBootstrapHandle("getRegexpBootstrap", BOOTSTRAP_STRING_STRING_INT_SIG);
    }
    
    public static Handle getSymbolHandle() {
        return getBootstrapHandle("getSymbolBootstrap", BOOTSTRAP_STRING_STRING_SIG);
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
        return getBootstrapHandle("getLoadStaticScopeBootstrap", BOOTSTRAP_INT_SIG);
    }
    
    public static Handle getCallSiteHandle() {
        return getBootstrapHandle("getCallSiteBootstrap", BOOTSTRAP_STRING_INT_SIG);
    }
    
    public static Handle getStringHandle() {
        return getBootstrapHandle("getStringBootstrap", BOOTSTRAP_STRING_STRING_INT_SIG);
    }
    
    public static Handle getFrozenStringHandle() {
        return getBootstrapHandle("getFrozenStringBootstrap", BOOTSTRAP_STRING_STRING_INT_SIG);
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
        return getBootstrapHandle("fixnumOperatorBootstrap", MathLinker.class, BOOTSTRAP_LONG_STRING_INT_SIG);
    }
    
    public static Handle getFixnumBooleanHandle() {
        return getBootstrapHandle("fixnumBooleanBootstrap", MathLinker.class, BOOTSTRAP_LONG_STRING_INT_SIG);
    }
    
    public static Handle getFloatOperatorHandle() {
        return getBootstrapHandle("floatOperatorBootstrap", MathLinker.class, BOOTSTRAP_DOUBLE_STRING_INT_SIG);
    }
    
    public static Handle getVariableHandle() {
        return getBootstrapHandle("variableBootstrap", BOOTSTRAP_STRING_INT_SIG);
    }
    
    public static Handle getContextFieldHandle() {
        return getBootstrapHandle("contextFieldBootstrap", BOOTSTRAP_BARE_SIG);
    }
    
    public static Handle getGlobalHandle() {
        return getBootstrapHandle("globalBootstrap", BOOTSTRAP_STRING_INT_SIG);
    }
    
    public static Handle getGlobalBooleanHandle() {
        return getBootstrapHandle("globalBooleanBootstrap", BOOTSTRAP_STRING_INT_SIG);
    }
    
    public static Handle getLoadBooleanHandle() {
        return getBootstrapHandle("loadBooleanBootstrap", BOOTSTRAP_BARE_SIG);
    }
    
    public static Handle checkpointHandle() {
        return getBootstrapHandle("checkpointBootstrap", BOOTSTRAP_BARE_SIG);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // BOOTSTRAP METHODS
    ////////////////////////////////////////////////////////////////////////////
    
    // <editor-fold desc="bootstraps">
    
    public static CallSite contextFieldBootstrap(Lookup lookup, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
        MutableCallSite site = new MutableCallSite(type);
        
        if (name.equals("nil")) {
            site.setTarget(Binder.from(type).insert(0, site).invokeStatic(lookup, InvokeDynamicSupport.class, "loadNil"));
        } else if (name.equals("runtime")) {
            site.setTarget(Binder.from(type).insert(0, site).invokeStatic(lookup, InvokeDynamicSupport.class, "loadRuntime"));
        }
        
        return site;
    }
    
    public static IRubyObject loadNil(MutableCallSite site, ThreadContext context) throws Throwable {
        site.setTarget(Binder.from(IRubyObject.class, ThreadContext.class).drop(0).constant(context.nil));
        
        return context.nil;
    }
    
    public static Ruby loadRuntime(MutableCallSite site, ThreadContext context) throws Throwable {
        site.setTarget(Binder.from(Ruby.class, ThreadContext.class).drop(0).constant(context.runtime));
        
        return context.runtime;
    }

    public static CallSite getConstantBootstrap(Lookup lookup, String name, MethodType type, int scopeIndex) throws NoSuchMethodException, IllegalAccessException {
        RubyConstantCallSite site;

        site = new RubyConstantCallSite(type, name);
        
        MethodType fallbackType = methodType(IRubyObject.class, RubyConstantCallSite.class, AbstractScript.class, ThreadContext.class, int.class);
        MethodHandle myFallback = insertArguments(
                lookup.findStatic(InvokeDynamicSupport.class, "constantFallback",
                fallbackType),
                0,
                site);
        myFallback = insertArguments(myFallback, 2, scopeIndex);
        site.setTarget(myFallback);
        return site;
    }

    public static CallSite getConstantBooleanBootstrap(Lookup lookup, String name, MethodType type, int scopeIndex) throws NoSuchMethodException, IllegalAccessException {
        RubyConstantCallSite site;

        site = new RubyConstantCallSite(type, name);
        
        MethodType fallbackType = methodType(boolean.class, RubyConstantCallSite.class, AbstractScript.class, ThreadContext.class, int.class);
        MethodHandle myFallback = insertArguments(
                lookup.findStatic(InvokeDynamicSupport.class, "constantBooleanFallback",
                fallbackType),
                0,
                site);
        myFallback = insertArguments(myFallback, 2, scopeIndex);
        site.setTarget(myFallback);
        return site;
    }

    public static CallSite getByteListBootstrap(Lookup lookup, String name, MethodType type, String asString, String encodingName) {
        byte[] bytes = Helpers.stringToRawBytes(asString);
        Encoding encoding = EncodingDB.getEncodings().get(encodingName.getBytes()).getEncoding();
        ByteList byteList = new ByteList(bytes, encoding);
        
        return new ConstantCallSite(constant(ByteList.class, byteList));
    }
    
    public static CallSite getRegexpBootstrap(Lookup lookup, String name, MethodType type, String asString, String encodingName, int options) {
        byte[] bytes = Helpers.stringToRawBytes(asString);
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
    
    public static CallSite getSymbolBootstrap(Lookup lookup, String name, MethodType type, String symbol, String encodingName) {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "initSymbol",
                methodType(RubySymbol.class, MutableCallSite.class, ThreadContext.class, String.class, Encoding.class));

        Encoding encoding = null;
        if (encodingName != null) {
            encoding = EncodingDB.getEncodings().get(encodingName.getBytes()).getEncoding();
        }

        init = insertArguments(init, 2, symbol, encoding);
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
                methodType(StaticScope.class, MutableCallSite.class, AbstractScript.class, ThreadContext.class, StaticScope.class, String.class, int.class));
        init = insertArguments(init, 4, scopeString, index);
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
        byte[] bytes = Helpers.stringToRawBytes(asString);
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
    
    public static CallSite getFrozenStringBootstrap(Lookup lookup, String name, MethodType type, String asString, String encodingName, int codeRange) {
        byte[] bytes = Helpers.stringToRawBytes(asString);
        Encoding encoding = EncodingDB.getEncodings().get(encodingName.getBytes()).getEncoding();
        ByteList byteList = new ByteList(bytes, encoding);
        
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle init = findStatic(
                InvokeDynamicSupport.class,
                "newFrozenString",
                methodType(RubyString.class, ThreadContext.class, MutableCallSite.class, ByteList.class, int.class));
        init = insertArguments(init, 1, site, byteList, codeRange);
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
                methodType(BlockBody.class, MutableCallSite.class, Object.class, ThreadContext.class, StaticScope.class, String.class));
        init = insertArguments(init, 4, descriptor);
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
                methodType(BlockBody.class, MutableCallSite.class, Object.class, ThreadContext.class, StaticScope.class, String.class));
        init = insertArguments(init, 4, descriptor);
        init = insertArguments(
                init,
                0,
                site);
        site.setTarget(init);
        return site;
    }

    public static CallSite variableBootstrap(Lookup lookup, String name, MethodType type, String file, int line) throws Throwable {
        String[] names = name.split(":");
        String operation = names[0];
        String varName = names[1];
        VariableSite site = new VariableSite(type, varName, file, line);
        MethodHandle handle;
        
        if (operation.equals("get")) {
            handle = lookup.findStatic(InvokeDynamicSupport.class, "getVariableFallback", methodType(IRubyObject.class, VariableSite.class, IRubyObject.class));
        } else if (operation.equals("set")) {
            handle = lookup.findStatic(InvokeDynamicSupport.class, "setVariableFallback", methodType(IRubyObject.class, VariableSite.class, IRubyObject.class, IRubyObject.class));
        } else {
            throw new RuntimeException("invalid variable access type");
        }
        
        handle = handle.bindTo(site);
        site.setTarget(handle);
        
        return site;
    }
    
    public static IRubyObject getVariableFallback(VariableSite site, IRubyObject self) throws Throwable {
        RubyClass realClass = self.getMetaClass().getRealClass();
        VariableAccessor accessor = realClass.getVariableAccessorForRead(site.name());
        
        // produce nil if the variable has not been initialize
        MethodHandle nullToNil = findStatic(Helpers.class, "nullToNil", methodType(IRubyObject.class, IRubyObject.class, IRubyObject.class));
        nullToNil = insertArguments(nullToNil, 1, self.getRuntime().getNil());
        nullToNil = explicitCastArguments(nullToNil, methodType(IRubyObject.class, Object.class));
        
        // get variable value and filter with nullToNil
        MethodHandle getValue;
        boolean direct = false;
        
        if (accessor instanceof FieldVariableAccessor) {
            direct = true;
            int offset = ((FieldVariableAccessor)accessor).getOffset();
            Class cls = REIFIED_OBJECT_CLASSES[offset];
            getValue = lookup().findGetter(cls, "var" + offset, Object.class);
            getValue = explicitCastArguments(getValue, methodType(Object.class, IRubyObject.class));
        } else {
            getValue = findStatic(VariableAccessor.class, "getVariable", methodType(Object.class, RubyBasicObject.class, int.class));
            getValue = explicitCastArguments(getValue, methodType(Object.class, IRubyObject.class, int.class));
            getValue = insertArguments(getValue, 1, accessor.getIndex());
        }
        
        getValue = filterReturnValue(getValue, nullToNil);
        
        // prepare fallback
        MethodHandle fallback = null;
        if (site.chainCount() + 1 > Options.INVOKEDYNAMIC_MAXPOLY.load()) {
            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) LOG.info(site.name() + "\tqet on type " + self.getMetaClass().id + " failed (polymorphic)" + extractSourceInfo(site));
            fallback = findStatic(InvokeDynamicSupport.class, "getVariableFail", methodType(IRubyObject.class, VariableSite.class, IRubyObject.class));
            fallback = fallback.bindTo(site);
            site.setTarget(fallback);
            return (IRubyObject)fallback.invokeWithArguments(self);
        } else {
            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                if (direct) {
                    LOG.info(site.name() + "\tget field on type " + self.getMetaClass().id + " added to PIC" + extractSourceInfo(site));
                } else {
                    LOG.info(site.name() + "\tget on type " + self.getMetaClass().id + " added to PIC" + extractSourceInfo(site));
                }
            }
            fallback = site.getTarget();
            site.incrementChainCount();
        }
        
        // prepare test
        MethodHandle test = findStatic(InvocationLinker.class, "testRealClass", methodType(boolean.class, int.class, IRubyObject.class));
        test = insertArguments(test, 0, accessor.getClassId());
        
        getValue = guardWithTest(test, getValue, fallback);
        
        if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) LOG.info(site.name() + "\tget on class " + self.getMetaClass().id + " bound directly" + extractSourceInfo(site));
        site.setTarget(getValue);
        
        return (IRubyObject)getValue.invokeWithArguments(self);
    }

    public static IRubyObject getVariableFail(VariableSite site, IRubyObject self) throws Throwable {
        return site.getVariable(self);
    }
    
    public static final Class[] REIFIED_OBJECT_CLASSES = {
        RubyObjectVar0.class,
        RubyObjectVar1.class,
        RubyObjectVar2.class,
        RubyObjectVar3.class,
        RubyObjectVar4.class,
        RubyObjectVar5.class,
        RubyObjectVar6.class,
        RubyObjectVar7.class,
        RubyObjectVar8.class,
        RubyObjectVar9.class,
    };
    
    public static IRubyObject setVariableFallback(VariableSite site, IRubyObject self, IRubyObject value) throws Throwable {
        RubyClass realClass = self.getMetaClass().getRealClass();
        VariableAccessor accessor = realClass.getVariableAccessorForWrite(site.name());

        // return provided value
        MethodHandle returnValue = identity(IRubyObject.class);
        returnValue = dropArguments(returnValue, 0, IRubyObject.class);

        // set variable value and fold by returning value
        MethodHandle setValue;
        boolean direct = false;
        
        if (accessor instanceof FieldVariableAccessor) {
            direct = true;
            int offset = ((FieldVariableAccessor)accessor).getOffset();
            Class cls = REIFIED_OBJECT_CLASSES[offset];
            setValue = findStatic(cls, "setVariableChecked", methodType(void.class, cls, Object.class));
            setValue = explicitCastArguments(setValue, methodType(void.class, IRubyObject.class, IRubyObject.class));
        } else {
            setValue = findStatic(accessor.getClass(), "setVariableChecked", methodType(void.class, RubyBasicObject.class, RubyClass.class, int.class, Object.class));
            setValue = explicitCastArguments(setValue, methodType(void.class, IRubyObject.class, RubyClass.class, int.class, IRubyObject.class));
            setValue = insertArguments(setValue, 1, realClass, accessor.getIndex());
        }
        
        setValue = foldArguments(returnValue, setValue);

        // prepare fallback
        MethodHandle fallback = null;
        if (site.chainCount() + 1 > Options.INVOKEDYNAMIC_MAXPOLY.load()) {
            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) LOG.info(site.name() + "\tset on type " + self.getMetaClass().id + " failed (polymorphic)" + extractSourceInfo(site));
            fallback = findStatic(InvokeDynamicSupport.class, "setVariableFail", methodType(IRubyObject.class, VariableSite.class, IRubyObject.class, IRubyObject.class));
            fallback = fallback.bindTo(site);
            site.setTarget(fallback);
            return (IRubyObject)fallback.invokeWithArguments(self, value);
        } else {
            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                if (direct) {
                    LOG.info(site.name() + "\tset field on type " + self.getMetaClass().id + " added to PIC" + extractSourceInfo(site));
                } else {
                    LOG.info(site.name() + "\tset on type " + self.getMetaClass().id + " added to PIC" + extractSourceInfo(site));
                }
            }
            fallback = site.getTarget();
            site.incrementChainCount();
        }

        // prepare test
        MethodHandle test = findStatic(InvocationLinker.class, "testRealClass", methodType(boolean.class, int.class, IRubyObject.class));
        test = insertArguments(test, 0, accessor.getClassId());
        test = dropArguments(test, 1, IRubyObject.class);

        setValue = guardWithTest(test, setValue, fallback);

        if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) LOG.info(site.name() + "\tset on class " + self.getMetaClass().id + " bound directly" + extractSourceInfo(site));
        site.setTarget(setValue);

        return (IRubyObject)setValue.invokeWithArguments(self, value);
    }

    public static IRubyObject setVariableFail(VariableSite site, IRubyObject self, IRubyObject value) throws Throwable {
        return site.setVariable(self, value);
    }

    public static CallSite globalBootstrap(Lookup lookup, String name, MethodType type, String file, int line) throws Throwable {
        String[] names = name.split(":");
        String operation = names[0];
        String varName = JavaNameMangler.demangleMethodName(names[1]);
        GlobalSite site = new GlobalSite(type, varName, file, line);
        MethodHandle handle;
        
        if (operation.equals("get")) {
            handle = lookup.findStatic(InvokeDynamicSupport.class, "getGlobalFallback", methodType(IRubyObject.class, GlobalSite.class, ThreadContext.class));
        } else {
            throw new RuntimeException("invalid variable access type");
        }
        
        handle = handle.bindTo(site);
        site.setTarget(handle);
        
        return site;
    }

    public static CallSite globalBooleanBootstrap(Lookup lookup, String name, MethodType type, String file, int line) throws Throwable {
        String[] names = name.split(":");
        String operation = names[0];
        String varName = JavaNameMangler.demangleMethodName(names[1]);
        GlobalSite site = new GlobalSite(type, varName, file, line);
        MethodHandle handle;
        
        if (operation.equals("getBoolean")) {
            handle = lookup.findStatic(InvokeDynamicSupport.class, "getGlobalBooleanFallback", methodType(boolean.class, GlobalSite.class, ThreadContext.class));
        } else {
            throw new RuntimeException("invalid variable access type");
        }
        
        handle = handle.bindTo(site);
        site.setTarget(handle);
        
        return site;
    }

    public static CallSite loadBooleanBootstrap(Lookup lookup, String name, MethodType type) throws Throwable {
        String[] names = name.split(":");
        String operation = names[0];
        boolean value = Boolean.parseBoolean(names[1]);
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle handle;
        
        if (operation.equals("loadBoolean")) {
            handle = lookup.findStatic(InvokeDynamicSupport.class, "loadBoolean", methodType(RubyBoolean.class, MutableCallSite.class, boolean.class, ThreadContext.class));
        } else {
            throw new RuntimeException("invalid variable access type");
        }
        
        handle = insertArguments(handle, 0, site, value);
        site.setTarget(handle);
        
        return site;
    }
    
    public static IRubyObject getGlobalFallback(GlobalSite site, ThreadContext context) throws Throwable {
        Ruby runtime = context.runtime;
        GlobalVariable variable = runtime.getGlobalVariables().getVariable(site.name());
        
        if (site.failures() > Options.INVOKEDYNAMIC_GLOBAL_MAXFAIL.load() ||
                variable.getScope() != GlobalVariable.Scope.GLOBAL) {
            
            // use uncached logic forever
            if (Options.INVOKEDYNAMIC_LOG_GLOBALS.load()) LOG.info("global " + site.name() + " (" + site.file() + ":" + site.line() + ") rebound > " + Options.INVOKEDYNAMIC_GLOBAL_MAXFAIL.load() + " times, reverting to simple lookup");
            
            MethodHandle uncached = lookup().findStatic(InvokeDynamicSupport.class, "getGlobalUncached", methodType(IRubyObject.class, GlobalVariable.class));
            uncached = uncached.bindTo(variable);
            uncached = dropArguments(uncached, 0, ThreadContext.class);
            site.setTarget(uncached);
            return (IRubyObject)uncached.invokeWithArguments(context);
        }
        
        Invalidator invalidator = variable.getInvalidator();
        IRubyObject value = variable.getAccessor().getValue();
        
        MethodHandle target = constant(IRubyObject.class, value);
        target = dropArguments(target, 0, ThreadContext.class);
        MethodHandle fallback = lookup().findStatic(InvokeDynamicSupport.class, "getGlobalFallback", methodType(IRubyObject.class, GlobalSite.class, ThreadContext.class));
        fallback = fallback.bindTo(site);
        
        target = ((SwitchPoint)invalidator.getData()).guardWithTest(target, fallback);
        
        site.setTarget(target);
        
        if (Options.INVOKEDYNAMIC_LOG_GLOBALS.load()) LOG.info("global " + site.name() + " (" + site.file() + ":" + site.line() + ") cached");
        
        return value;
    }
    
    public static IRubyObject getGlobalUncached(GlobalVariable variable) throws Throwable {
        return variable.getAccessor().getValue();
    }
    
    public static boolean getGlobalBooleanFallback(GlobalSite site, ThreadContext context) throws Throwable {
        Ruby runtime = context.runtime;
        GlobalVariable variable = runtime.getGlobalVariables().getVariable(site.name());
        
        if (site.failures() > Options.INVOKEDYNAMIC_GLOBAL_MAXFAIL.load() ||
                variable.getScope() != GlobalVariable.Scope.GLOBAL) {
            
            // use uncached logic forever
            if (Options.INVOKEDYNAMIC_LOG_GLOBALS.load()) LOG.info("global " + site.name() + " (" + site.file() + ":" + site.line() + ") rebound > " + Options.INVOKEDYNAMIC_GLOBAL_MAXFAIL.load() + " times, reverting to simple lookup");

            MethodHandle uncached = lookup().findStatic(InvokeDynamicSupport.class, "getGlobalBooleanUncached", methodType(boolean.class, GlobalVariable.class));
            uncached = uncached.bindTo(variable);
            uncached = dropArguments(uncached, 0, ThreadContext.class);
            site.setTarget(uncached);
            return (Boolean)uncached.invokeWithArguments(context);
        }
        
        Invalidator invalidator = variable.getInvalidator();
        boolean value = variable.getAccessor().getValue().isTrue();
        
        MethodHandle target = constant(boolean.class, value);
        target = dropArguments(target, 0, ThreadContext.class);
        MethodHandle fallback = lookup().findStatic(InvokeDynamicSupport.class, "getGlobalBooleanFallback", methodType(boolean.class, GlobalSite.class, ThreadContext.class));
        fallback = fallback.bindTo(site);
        
        target = ((SwitchPoint)invalidator.getData()).guardWithTest(target, fallback);
        
        site.setTarget(target);
        
        if (Options.INVOKEDYNAMIC_LOG_GLOBALS.load()) LOG.info("global " + site.name() + " (" + site.file() + ":" + site.line() + ") cached as boolean");
        
        return value;
    }
    
    public static boolean getGlobalBooleanUncached(GlobalVariable variable) throws Throwable {
        return variable.getAccessor().getValue().isTrue();
    }

    public static CallSite checkpointBootstrap(Lookup lookup, String name, MethodType type) throws Throwable {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle handle = lookup.findStatic(InvokeDynamicSupport.class, "checkpointFallback", methodType(void.class, MutableCallSite.class, ThreadContext.class));
        
        handle = handle.bindTo(site);
        site.setTarget(handle);
        
        return site;
    }
    
    public static void checkpointFallback(MutableCallSite site, ThreadContext context) throws Throwable {
        Ruby runtime = context.runtime;
        Invalidator invalidator = runtime.getCheckpointInvalidator();
        
        MethodHandle target = Binder
                .from(void.class, ThreadContext.class)
                .nop();
        MethodHandle fallback = lookup().findStatic(InvokeDynamicSupport.class, "checkpointFallback", methodType(void.class, MutableCallSite.class, ThreadContext.class));
        fallback = fallback.bindTo(site);
        
        target = ((SwitchPoint)invalidator.getData()).guardWithTest(target, fallback);
        
        site.setTarget(target);
    }
    
    // </editor-fold>
    
    ////////////////////////////////////////////////////////////////////////////
    // INITIAL AND FALLBACK METHODS FOR POST BOOTSTRAP
    ////////////////////////////////////////////////////////////////////////////
    
    public static IRubyObject constantFallback(RubyConstantCallSite site, 
            AbstractScript script, ThreadContext context, int scopeIndex) {
        SwitchPoint switchPoint = (SwitchPoint)context.runtime.getConstantInvalidator(site.name()).getData();
        StaticScope scope = script.getScope(scopeIndex);
        IRubyObject value = scope.getConstant(site.name());
        
        if (value != null) {
            if (Options.INVOKEDYNAMIC_LOG_CONSTANTS.load()) LOG.info("constant " + site.name() + " bound directly");
            
            MethodHandle valueHandle = constant(IRubyObject.class, value);
            valueHandle = dropArguments(valueHandle, 0, AbstractScript.class, ThreadContext.class);

            MethodHandle fallback = insertArguments(
                    findStatic(InvokeDynamicSupport.class, "constantFallback",
                    methodType(IRubyObject.class, RubyConstantCallSite.class, AbstractScript.class, ThreadContext.class, int.class)),
                    0,
                    site);
            fallback = insertArguments(fallback, 2, scopeIndex);

            MethodHandle gwt = switchPoint.guardWithTest(valueHandle, fallback);
            site.setTarget(gwt);
        } else {
            value = scope.getModule()
                    .callMethod(context, "const_missing", context.runtime.newSymbol(site.name()));
        }
        
        return value;
    }

    public static boolean constantBooleanFallback(RubyConstantCallSite site, 
            AbstractScript script, ThreadContext context, int scopeIndex) {
        SwitchPoint switchPoint = (SwitchPoint)context.runtime.getConstantInvalidator(site.name()).getData();
        StaticScope scope = script.getScope(scopeIndex);
        IRubyObject value = scope.getConstant(site.name());
        
        if (value != null) {
            if (Options.INVOKEDYNAMIC_LOG_CONSTANTS.load()) LOG.info("constant " + site.name() + " bound directly");
            
            MethodHandle valueHandle = constant(boolean.class, value.isTrue());
            valueHandle = dropArguments(valueHandle, 0, AbstractScript.class, ThreadContext.class);

            MethodHandle fallback = insertArguments(
                    findStatic(InvokeDynamicSupport.class, "constantBooleanFallback",
                    methodType(boolean.class, RubyConstantCallSite.class, AbstractScript.class, ThreadContext.class, int.class)),
                    0,
                    site);
            fallback = insertArguments(fallback, 2, scopeIndex);

            MethodHandle gwt = switchPoint.guardWithTest(valueHandle, fallback);
            site.setTarget(gwt);
        } else {
            value = scope.getModule()
                    .callMethod(context, "const_missing", context.runtime.newSymbol(site.name()));
        }
        
        boolean booleanValue = value.isTrue();
        
        return booleanValue;
    }
    
    public static RubyRegexp initRegexp(MutableCallSite site, ThreadContext context, ByteList pattern, int options) {
        RubyRegexp regexp = RubyRegexp.newRegexp(context.runtime, pattern, RegexpOptions.fromEmbeddedOptions(options));
        regexp.setLiteral();
        site.setTarget(dropArguments(constant(RubyRegexp.class, regexp), 0, ThreadContext.class));
        return regexp;
    }
    
    public static RubySymbol initSymbol(MutableCallSite site, ThreadContext context, String symbol, Encoding encoding) {
        RubySymbol rubySymbol = context.runtime.newSymbol(symbol);
        if (encoding != null) rubySymbol.associateEncoding(encoding);
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
    
    public static StaticScope initStaticScope(MutableCallSite site, AbstractScript script, ThreadContext context, StaticScope parent, String staticScope, int index) {
        StaticScope scope = script.getScope(context, parent, staticScope, index);
        site.setTarget(dropArguments(constant(StaticScope.class, scope), 0, AbstractScript.class, ThreadContext.class, StaticScope.class));
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
    
    public static RubyString newFrozenString(ThreadContext context, MutableCallSite site, ByteList contents, int codeRange) {
        RubyString string = context.runtime.freezeAndDedupString(RubyString.newStringShared(context.runtime, contents, codeRange));
        site.setTarget(dropArguments(constant(RubyString.class, string), 0, ThreadContext.class));
        return string;
    }
    
    public static RubyEncoding initEncoding(MutableCallSite site, ThreadContext context, Encoding encoding) {
        RubyEncoding rubyEncoding = context.runtime.getEncodingService().getEncoding(encoding);
        site.setTarget(dropArguments(constant(RubyEncoding.class, rubyEncoding), 0, ThreadContext.class));
        return rubyEncoding;
    }
    
    public static RubyBoolean loadBoolean(MutableCallSite site, boolean value, ThreadContext context) {
        RubyBoolean rubyBoolean = context.runtime.newBoolean(value);
        site.setTarget(dropArguments(constant(RubyBoolean.class, rubyBoolean), 0, ThreadContext.class));
        return rubyBoolean;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // method_missing support code
    ////////////////////////////////////////////////////////////////////////////

    public static boolean methodMissing(CacheEntry entry, CallType callType, String name, IRubyObject caller) {
        DynamicMethod method = entry.method;
        return method.isUndefined() || (callType == CallType.NORMAL && !name.equals("method_missing") && !method.isCallableFrom(caller, callType));
    }

    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject[] args) {
        return Helpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, args, Block.NULL_BLOCK);
    }

    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name) {
        return Helpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, Block.NULL_BLOCK);
    }

    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, Block block) {
        return Helpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, block);
    }

    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg) {
        return Helpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg, Block.NULL_BLOCK);
    }

    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject[] args, Block block) {
        return Helpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, args, block);
    }

    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg0, Block block) {
        return Helpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg0, block);
    }

    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) {
        return Helpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg0, arg1, Block.NULL_BLOCK);
    }

    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        return Helpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg0, arg1, block);
    }

    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return Helpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg0, arg1, arg2, Block.NULL_BLOCK);
    }

    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return Helpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg0, arg1, arg2, block);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Dispatch support methods
    ////////////////////////////////////////////////////////////////////////////

    public static RubyClass pollAndGetClass(ThreadContext context, IRubyObject self) {
        context.callThreadPoll();
        RubyClass selfType = ((RubyBasicObject)self).getMetaClass();
        return selfType;
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

    private static String extractSourceInfo(VariableSite site) {
        return " (" + site.file() + ":" + site.line() + ")";
    }
}
