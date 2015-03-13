package org.jruby.ir.targets;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.Signature;
import com.headius.invokebinder.SmartBinder;
import com.headius.invokebinder.SmartHandle;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jruby.*;
import org.jruby.common.IRubyWarnings;
import org.jruby.internal.runtime.methods.*;
import org.jruby.ir.JIT;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.invokedynamic.InvocationLinker;
import org.jruby.runtime.invokedynamic.VariableSite;
import org.jruby.runtime.ivars.FieldVariableAccessor;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.runtime.opto.OptoFactory;
import org.jruby.util.ByteList;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.*;

import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.methodType;
import static org.jruby.runtime.Helpers.arrayOf;
import static org.jruby.runtime.invokedynamic.InvokeDynamicSupport.*;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class Bootstrap {
    private static final Logger LOG = LoggerFactory.getLogger("Bootstrap");
    static final Lookup LOOKUP = MethodHandles.lookup();

    public static CallSite string(Lookup lookup, String name, MethodType type, String value, String encodingName) {
        Encoding encoding;
        EncodingDB.Entry entry = EncodingDB.getEncodings().get(encodingName.getBytes());
        if (entry == null) entry = EncodingDB.getAliases().get(encodingName.getBytes());
        if (entry == null) throw new RuntimeException("could not find encoding: " + encodingName);
        encoding = entry.getEncoding();
        ByteList byteList = new ByteList(value.getBytes(RubyEncoding.ISO), encoding);
        MutableCallSite site = new MutableCallSite(type);
        Binder binder = Binder
                .from(RubyString.class, ThreadContext.class)
                .insert(0, arrayOf(MutableCallSite.class, ByteList.class), site, byteList);
        if (name.equals("frozen")) {
            site.setTarget(binder.invokeStaticQuiet(lookup, Bootstrap.class, "frozenString"));
        } else {
            site.setTarget(binder.invokeStaticQuiet(lookup, Bootstrap.class, "string"));
        }

        return site;
    }

    public static CallSite bytelist(Lookup lookup, String name, MethodType type, String value, String encodingName) {
        Encoding encoding;
        EncodingDB.Entry entry = EncodingDB.getEncodings().get(encodingName.getBytes());
        if (entry == null) entry = EncodingDB.getAliases().get(encodingName.getBytes());
        if (entry == null) throw new RuntimeException("could not find encoding: " + encodingName);
        encoding = entry.getEncoding();
        ByteList byteList = new ByteList(value.getBytes(RubyEncoding.ISO), encoding);
        return new ConstantCallSite(constant(ByteList.class, byteList));
    }

    public static CallSite array(Lookup lookup, String name, MethodType type) {
        MethodHandle handle = Binder
                .from(type)
                .collect(1, IRubyObject[].class)
                .invokeStaticQuiet(LOOKUP, Bootstrap.class, "array");
        CallSite site = new ConstantCallSite(handle);
        return site;
    }

    public static CallSite hash(Lookup lookup, String name, MethodType type) {
        MethodHandle handle = Binder
                .from(lookup, type)
                .collect(1, IRubyObject[].class)
                .invokeStaticQuiet(LOOKUP, Bootstrap.class, "hash");
        CallSite site = new ConstantCallSite(handle);
        return site;
    }

    public static CallSite kwargsHash(Lookup lookup, String name, MethodType type) {
        MethodHandle handle = Binder
                .from(lookup, type)
                .collect(2, IRubyObject[].class)
                .invokeStaticQuiet(LOOKUP, Bootstrap.class, "kwargsHash");
        CallSite site = new ConstantCallSite(handle);
        return site;
    }

    public static CallSite ivar(Lookup lookup, String name, MethodType type) throws Throwable {
        String[] names = name.split(":");
        String operation = names[0];
        String varName = names[1];
        VariableSite site = new VariableSite(type, varName, "noname", 0);
        MethodHandle handle;

        handle = lookup.findStatic(Bootstrap.class, operation, type.insertParameterTypes(0, VariableSite.class));

        handle = handle.bindTo(site);
        site.setTarget(handle.asType(site.type()));

        return site;
    }

    public static CallSite searchConst(Lookup lookup, String name, MethodType type, int noPrivateConsts) {
        MutableCallSite site = new MutableCallSite(type);
        String[] bits = name.split(":");
        String constName = bits[1];

        MethodHandle handle = Binder
                .from(lookup, type)
                .append(site, constName.intern())
                .append(noPrivateConsts == 0 ? false : true)
                .invokeStaticQuiet(LOOKUP, Bootstrap.class, bits[0]);

        site.setTarget(handle);

        return site;
    }

    public static Handle string() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "string", sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, String.class));
    }

    public static Handle bytelist() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "bytelist", sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, String.class));
    }

    public static Handle array() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "array", sig(CallSite.class, Lookup.class, String.class, MethodType.class));
    }

    public static Handle hash() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "hash", sig(CallSite.class, Lookup.class, String.class, MethodType.class));
    }

    public static Handle kwargsHash() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "kwargsHash", sig(CallSite.class, Lookup.class, String.class, MethodType.class));
    }

    public static Handle invokeSuper() {
        return SuperInvokeSite.BOOTSTRAP;
    }

    public static Handle ivar() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "ivar", sig(CallSite.class, Lookup.class, String.class, MethodType.class));
    }

    public static Handle searchConst() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "searchConst", sig(CallSite.class, Lookup.class, String.class, MethodType.class, int.class));
    }

    public static RubyString string(MutableCallSite site, ByteList value, ThreadContext context) throws Throwable {
        MethodHandle handle = SmartBinder
                .from(STRING_SIGNATURE)
                .invoke(NEW_STRING_SHARED_HANDLE.apply("byteList", value))
                .handle();

        site.setTarget(handle);

        return RubyString.newStringShared(context.runtime, value);
    }

    public static RubyString frozenString(MutableCallSite site, ByteList value, ThreadContext context) throws Throwable {
        RubyString frozen = context.runtime.freezeAndDedupString(RubyString.newStringShared(context.runtime, value));
        MethodHandle handle = Binder.from(RubyString.class, ThreadContext.class)
                .dropAll()
                .constant(frozen);

        site.setTarget(handle);

        return frozen;
    }

    private static final Signature STRING_SIGNATURE = Signature.from(RubyString.class, arrayOf(ThreadContext.class), "context");
    private static final Signature NEW_STRING_SHARED_SIGNATURE = Signature.from(RubyString.class, arrayOf(ThreadContext.class, ByteList.class), "context", "byteList");

    private static final SmartHandle NEW_STRING_SHARED_HANDLE =
            SmartBinder.from(NEW_STRING_SHARED_SIGNATURE)
                    .invokeStaticQuiet(MethodHandles.lookup(), Bootstrap.class, "newStringShared");
    @JIT
    private static RubyString newStringShared(ThreadContext context, ByteList byteList) {
        return RubyString.newStringShared(context.runtime, byteList);
    }

    public static IRubyObject array(ThreadContext context, IRubyObject[] elts) {
        return RubyArray.newArrayNoCopy(context.runtime, elts);
    }

    public static Handle contextValue() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "contextValue", sig(CallSite.class, Lookup.class, String.class, MethodType.class));
    }

    public static Handle contextValueString() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "contextValueString", sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class));
    }

    public static CallSite contextValue(Lookup lookup, String name, MethodType type) {
        MutableCallSite site = new MutableCallSite(type);
        site.setTarget(Binder.from(type).append(site).invokeStaticQuiet(lookup, Bootstrap.class, name));
        return site;
    }

    public static CallSite contextValueString(Lookup lookup, String name, MethodType type, String str) {
        MutableCallSite site = new MutableCallSite(type);
        site.setTarget(Binder.from(type).append(site, str).invokeStaticQuiet(lookup, Bootstrap.class, name));
        return site;
    }

    public static IRubyObject nil(ThreadContext context, MutableCallSite site) {
        MethodHandle constant = (MethodHandle)((RubyNil)context.nil).constant();
        if (constant == null) constant = (MethodHandle)OptoFactory.newConstantWrapper(IRubyObject.class, context.nil);

        site.setTarget(constant);

        return context.nil;
    }

    public static IRubyObject True(ThreadContext context, MutableCallSite site) {
        MethodHandle constant = (MethodHandle)context.runtime.getTrue().constant();
        if (constant == null) constant = (MethodHandle)OptoFactory.newConstantWrapper(IRubyObject.class, context.runtime.getTrue());

        site.setTarget(constant);

        return context.runtime.getTrue();
    }

    public static IRubyObject False(ThreadContext context, MutableCallSite site) {
        MethodHandle constant = (MethodHandle)context.runtime.getFalse().constant();
        if (constant == null) constant = (MethodHandle)OptoFactory.newConstantWrapper(IRubyObject.class, context.runtime.getFalse());

        site.setTarget(constant);

        return context.runtime.getFalse();
    }

    public static Ruby runtime(ThreadContext context, MutableCallSite site) {
        MethodHandle constant = (MethodHandle)context.runtime.constant();
        if (constant == null) constant = (MethodHandle)OptoFactory.newConstantWrapper(Ruby.class, context.runtime);

        site.setTarget(constant);

        return context.runtime;
    }

    public static RubyEncoding encoding(ThreadContext context, MutableCallSite site, String name) {
        RubyEncoding rubyEncoding = IRRuntimeHelpers.retrieveEncoding(context, name);

        MethodHandle constant = (MethodHandle)rubyEncoding.constant();
        if (constant == null) constant = (MethodHandle)OptoFactory.newConstantWrapper(RubyEncoding.class, rubyEncoding);

        site.setTarget(constant);

        return rubyEncoding;
    }

    public static IRubyObject hash(ThreadContext context, IRubyObject[] pairs) {
        Ruby runtime = context.runtime;
        RubyHash hash = RubyHash.newHash(runtime);
        for (int i = 0; i < pairs.length;) {
            hash.fastASetCheckString(runtime, pairs[i++], pairs[i++]);
        }
        return hash;
    }

    public static IRubyObject kwargsHash(ThreadContext context, RubyHash hash, IRubyObject[] pairs) {
        return IRRuntimeHelpers.dupKwargsHashAndPopulateFromArray(context, hash, pairs);
    }

    static MethodHandle buildGenericHandle(InvokeSite site, DynamicMethod method, RubyClass dispatchClass) {
        SmartBinder binder;

        binder = SmartBinder.from(site.signature)
                .permute("context", "self", "arg.*", "block")
                .insert(2, new String[]{"rubyClass", "name"}, new Class[]{RubyModule.class, String.class}, dispatchClass, site.name())
                .insert(0, "method", DynamicMethod.class, method);

        if (site.arity > 3) {
            binder = binder.collect("args", "arg.*");
        }

        return binder.invokeVirtualQuiet(LOOKUP, "call").handle();
    }

    static MethodHandle buildJittedHandle(InvokeSite site, DynamicMethod method, boolean blockGiven) {
        MethodHandle mh = null;
        SmartBinder binder;
        CompiledIRMethod compiledIRMethod = null;

        if (method instanceof CompiledIRMethod) {
            compiledIRMethod = (CompiledIRMethod)method;
        } else if (method instanceof MixedModeIRMethod) {
            DynamicMethod actualMethod = ((MixedModeIRMethod)method).getActualMethod();
            if (actualMethod instanceof CompiledIRMethod) {
                compiledIRMethod = (CompiledIRMethod)actualMethod;
            }
        }

        if (compiledIRMethod != null) {
            // attempt IR direct binding
            // TODO: this will have to expand when we start specializing arities

            binder = SmartBinder.from(site.signature)
                    .permute("context", "self", "arg.*", "block");

            if (site.arity == -1) {
                // already [], nothing to do
                mh = (MethodHandle)compiledIRMethod.getHandle();
            } else if (site.arity == 0) {
                MethodHandle specific;
                if ((specific = compiledIRMethod.getHandleFor(site.arity)) != null) {
                    mh = specific;
                } else {
                    mh = (MethodHandle)compiledIRMethod.getHandle();
                    binder = binder.insert(2, "args", IRubyObject.NULL_ARRAY);
                }
            } else {
                MethodHandle specific;
                if ((specific = compiledIRMethod.getHandleFor(site.arity)) != null) {
                    mh = specific;
                } else {
                    mh = (MethodHandle) compiledIRMethod.getHandle();
                    binder = binder.collect("args", "arg.*");
                }
            }

            if (!blockGiven) {
                binder = binder.append("block", Block.class, Block.NULL_BLOCK);
            }

            binder = binder
                    .insert(1, "scope", StaticScope.class, compiledIRMethod.getStaticScope())
                    .append("class", RubyModule.class, compiledIRMethod.getImplementationClass())
                    .append("frameName", String.class, site.name());

            mh = binder.invoke(mh).handle();
        }

        return mh;
    }

    static MethodHandle buildNativeHandle(InvokeSite site, DynamicMethod method, boolean blockGiven) {
        MethodHandle mh = null;
        SmartBinder binder = null;

        if (method.getNativeCall() != null) {

            int nativeArgCount = getNativeArgCount(method, method.getNativeCall());

            DynamicMethod.NativeCall nc = method.getNativeCall();

            if (nc.isJava()) {
                // not supported yet, use DynamicMethod.call
            } else {
                if (nativeArgCount >= 0) { // native methods only support arity 3
                    if (nativeArgCount == site.arity) {
                        // nothing to do
                        binder = SmartBinder.from(lookup(), site.signature);
                    } else {
                        // arity mismatch...leave null and use DynamicMethod.call below
                    }
                } else {
                    // varargs
                    if (site.arity == -1) {
                        // ok, already passing []
                        binder = SmartBinder.from(lookup(), site.signature);
                    } else if (site.arity == 0) {
                        // no args, insert dummy
                        binder = SmartBinder.from(lookup(), site.signature)
                                .insert(2, "args", IRubyObject.NULL_ARRAY);
                    } else {
                        // 1 or more args, collect into []
                        binder = SmartBinder.from(lookup(), site.signature)
                                .collect("args", "arg.*");
                    }
                }

                if (binder != null) {

                    // clean up non-arguments, ordering, types
                    if (!nc.hasContext()) {
                        binder = binder.drop("context");
                    }

                    if (nc.hasBlock() && !blockGiven) {
                        binder = binder.append("block", Block.NULL_BLOCK);
                    } else if (!nc.hasBlock() && blockGiven) {
                        binder = binder.drop("block");
                    }

                    if (nc.isStatic()) {
                        mh = binder
                                .permute("context", "self", "arg.*", "block") // filter caller
                                .cast(nc.getNativeReturn(), nc.getNativeSignature())
                                .invokeStaticQuiet(LOOKUP, nc.getNativeTarget(), nc.getNativeName())
                                .handle();
                    } else {
                        mh = binder
                                .permute("self", "context", "arg.*", "block") // filter caller, move self
                                .castArg("self", nc.getNativeTarget())
                                .castVirtual(nc.getNativeReturn(), nc.getNativeTarget(), nc.getNativeSignature())
                                .invokeVirtualQuiet(LOOKUP, nc.getNativeName())
                                .handle();
                    }
                }
            }
        }

        return mh;
    }

    public static int getNativeArgCount(DynamicMethod method, DynamicMethod.NativeCall nativeCall) {
        // if non-Java, must:
        // * exactly match arities or both are [] boxed
        // * 3 or fewer arguments
        return getArgCount(nativeCall.getNativeSignature(), nativeCall.isStatic());
    }

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
                    length = -1;
                } else if (args[1] == IRubyObject[].class) {
                    length = -1;
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
                    length = -1;
                } else if (args[0] == IRubyObject[].class) {
                    length = -1;
                }
            }
        }
        return length;
    }

    public static IRubyObject ivarGet(VariableSite site, IRubyObject self) throws Throwable {
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
            fallback = findStatic(Bootstrap.class, "ivarGetFail", methodType(IRubyObject.class, VariableSite.class, IRubyObject.class));
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

        return (IRubyObject)getValue.invokeExact(self);
    }

    public static IRubyObject ivarGetFail(VariableSite site, IRubyObject self) throws Throwable {
        return site.getVariable(self);
    }

    public static void ivarSet(VariableSite site, IRubyObject self, IRubyObject value) throws Throwable {
        RubyClass realClass = self.getMetaClass().getRealClass();
        VariableAccessor accessor = realClass.getVariableAccessorForWrite(site.name());

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

        // prepare fallback
        MethodHandle fallback = null;
        if (site.chainCount() + 1 > Options.INVOKEDYNAMIC_MAXPOLY.load()) {
            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) LOG.info(site.name() + "\tset on type " + self.getMetaClass().id + " failed (polymorphic)" + extractSourceInfo(site));
            fallback = findStatic(Bootstrap.class, "ivarSetFail", methodType(void.class, VariableSite.class, IRubyObject.class, IRubyObject.class));
            fallback = fallback.bindTo(site);
            site.setTarget(fallback);
            fallback.invokeExact(self, value);
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

        setValue.invokeExact(self, value);
    }

    public static void ivarSetFail(VariableSite site, IRubyObject self, IRubyObject value) throws Throwable {
        site.setVariable(self, value);
    }

    private static MethodHandle findStatic(Class target, String name, MethodType type) {
        return findStatic(lookup(), target, name, type);
    }

    private static MethodHandle findStatic(Lookup lookup, Class target, String name, MethodType type) {
        try {
            return lookup.findStatic(target, name, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean testType(RubyClass original, IRubyObject self) {
        // naive test
        return ((RubyBasicObject)self).getMetaClass() == original;
    }

    ///////////////////////////////////////////////////////////////////////////
    // constant lookup

    public static IRubyObject searchConst(ThreadContext context, StaticScope staticScope, MutableCallSite site, String constName, boolean noPrivateConsts) throws Throwable {

        // Lexical lookup
        Ruby runtime = context.getRuntime();
        RubyModule object = runtime.getObject();
        IRubyObject constant = (staticScope == null) ? object.getConstant(constName) : staticScope.getConstantInner(constName);

        // Inheritance lookup
        RubyModule module = null;
        if (constant == null) {
            // SSS FIXME: Is this null check case correct?
            module = staticScope == null ? object : staticScope.getModule();
            constant = noPrivateConsts ? module.getConstantFromNoConstMissing(constName, false) : module.getConstantNoConstMissing(constName);
        }

        // Call const_missing or cache
        if (constant == null) {
            return module.callMethod(context, "const_missing", context.runtime.fastNewSymbol(constName));
        }

        SwitchPoint switchPoint = (SwitchPoint)runtime.getConstantInvalidator(constName).getData();

        // bind constant until invalidated
        MethodHandle target = Binder.from(site.type())
                .drop(0, 2)
                .constant(constant);
        MethodHandle fallback = Binder.from(site.type())
                .append(site, constName)
                .append(noPrivateConsts)
                .invokeStatic(LOOKUP, Bootstrap.class, "searchConst");

        site.setTarget(switchPoint.guardWithTest(target, fallback));

        return constant;
    }

    public static IRubyObject inheritanceSearchConst(ThreadContext context, IRubyObject cmVal, MutableCallSite site, String constName, boolean noPrivateConsts) throws Throwable {
        Ruby runtime = context.runtime;
        RubyModule module;

        if (cmVal instanceof RubyModule) {
            module = (RubyModule) cmVal;
        } else {
            throw runtime.newTypeError(cmVal + " is not a type/class");
        }

        IRubyObject constant = noPrivateConsts ? module.getConstantFromNoConstMissing(constName, false) : module.getConstantNoConstMissing(constName);

        if (constant == null) {
            constant = UndefinedValue.UNDEFINED;
        }

        // This caching does not take into consideration sites that have many different module targets.
        //
        if (true) {
            return constant;
        }

        SwitchPoint switchPoint = (SwitchPoint)runtime.getConstantInvalidator(constName).getData();

        // bind constant until invalidated
        MethodHandle target = Binder.from(site.type())
                .drop(0, 2)
                .constant(constant);
        MethodHandle fallback = Binder.from(site.type())
                .append(site, constName)
                .append(noPrivateConsts)
                .invokeStatic(LOOKUP, Bootstrap.class, "inheritanceSearchConst");

        // test that module is same as before
        MethodHandle test = Binder.from(site.type().changeReturnType(boolean.class))
                .drop(0, 1)
                .insert(1, module.id)
                .invokeStaticQuiet(LOOKUP, Bootstrap.class, "testArg0ModuleMatch");
        target = guardWithTest(test, target, fallback);
        site.setTarget(switchPoint.guardWithTest(target, fallback));

        if (Options.INVOKEDYNAMIC_LOG_CONSTANTS.load()) {
            LOG.info(constName + "\tretrieved and cached from type " + cmVal.getMetaClass());// + " added to PIC" + extractSourceInfo(site));
        }

        return constant;
    }

    public static IRubyObject lexicalSearchConst(ThreadContext context, StaticScope scope, MutableCallSite site, String constName, boolean noPrivateConsts) throws Throwable {
        Ruby runtime = context.runtime;

        IRubyObject constant = scope.getConstantInner(constName);

        if (constant == null) {
            constant = UndefinedValue.UNDEFINED;
        }

        SwitchPoint switchPoint = (SwitchPoint)runtime.getConstantInvalidator(constName).getData();

        // bind constant until invalidated
        MethodHandle target = Binder.from(site.type())
                .drop(0, 2)
                .constant(constant);
        MethodHandle fallback = Binder.from(site.type())
                .append(site, constName)
                .append(noPrivateConsts)
                .invokeStatic(LOOKUP, Bootstrap.class, "lexicalSearchConst");

        site.setTarget(switchPoint.guardWithTest(target, fallback));

        return constant;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fixnum binding

    public static IRubyObject instVarNullToNil(IRubyObject value, IRubyObject nil, String name) {
        if (value == null) {
            Ruby runtime = nil.getRuntime();
            if (runtime.isVerbose()) {
                nil.getRuntime().getWarnings().warning(IRubyWarnings.ID.IVAR_NOT_INITIALIZED, "instance variable " + name + " not initialized");
            }
            return nil;
        }
        return value;
    }

    public static boolean testArg0ModuleMatch(IRubyObject arg0, int id) {
        System.out.println("testing " + arg0 + " for match (id = " + ((RubyModule)arg0).id + "): " + (arg0 instanceof RubyModule && ((RubyModule)arg0).id == id));
        return arg0 instanceof RubyModule && ((RubyModule)arg0).id == id;
    }

    private static String extractSourceInfo(VariableSite site) {
        return " (" + site.file() + ":" + site.line() + ")";
    }
}
