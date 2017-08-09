package org.jruby.ir.targets;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.Signature;
import com.headius.invokebinder.SmartBinder;
import com.headius.invokebinder.SmartHandle;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.common.IRubyWarnings;
import org.jruby.internal.runtime.GlobalVariable;
import org.jruby.internal.runtime.methods.*;
import org.jruby.ir.IRScope;
import org.jruby.ir.JIT;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.CompiledIRBlockBody;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.invokedynamic.GlobalSite;
import org.jruby.runtime.invokedynamic.InvocationLinker;
import org.jruby.runtime.invokedynamic.MathLinker;
import org.jruby.runtime.invokedynamic.VariableSite;
import org.jruby.runtime.ivars.FieldVariableAccessor;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.runtime.opto.Invalidator;
import org.jruby.runtime.opto.OptoFactory;
import org.jruby.util.ByteList;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.*;

import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.methodType;
import static org.jruby.runtime.Helpers.arrayOf;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class Bootstrap {
    public final static String BOOTSTRAP_BARE_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class);
    public final static String BOOTSTRAP_LONG_STRING_INT_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, long.class, int.class, String.class, int.class);
    public final static String BOOTSTRAP_DOUBLE_STRING_INT_SIG = sig(CallSite.class, Lookup.class, String.class, MethodType.class, double.class, int.class, String.class, int.class);
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
    private static final Logger LOG = LoggerFactory.getLogger(Bootstrap.class);
    static final Lookup LOOKUP = MethodHandles.lookup();

    public static CallSite string(Lookup lookup, String name, MethodType type, String value, String encodingName, int cr) {
        MutableCallSite site = new MutableCallSite(type);
        Binder binder = Binder
                .from(RubyString.class, ThreadContext.class)
                .insert(0, arrayOf(MutableCallSite.class, ByteList.class, int.class), site, bytelist(value, encodingName), cr);
        site.setTarget(binder.invokeStaticQuiet(lookup, Bootstrap.class, "string"));

        return site;
    }

    public static CallSite fstring(Lookup lookup, String name, MethodType type, String value, String encodingName, int cr, String file, int line) {
        MutableCallSite site = new MutableCallSite(type);
        Binder binder = Binder
                .from(RubyString.class, ThreadContext.class)
                .insert(0, arrayOf(MutableCallSite.class, ByteList.class, int.class, String.class, int.class), site, bytelist(value, encodingName), cr, file, line);
        site.setTarget(binder.invokeStaticQuiet(lookup, Bootstrap.class, "frozenString"));

        return site;
    }

    public static CallSite bytelist(Lookup lookup, String name, MethodType type, String value, String encodingName) {
        return new ConstantCallSite(constant(ByteList.class, bytelist(value, encodingName)));
    }

    private static ByteList bytelist(String value, String encodingName) {
        Encoding encoding;
        EncodingDB.Entry entry = EncodingDB.getEncodings().get(encodingName.getBytes());
        if (entry == null) entry = EncodingDB.getAliases().get(encodingName.getBytes());
        if (entry == null) throw new RuntimeException("could not find encoding: " + encodingName);
        encoding = entry.getEncoding();
        ByteList byteList = new ByteList(value.getBytes(RubyEncoding.ISO), encoding);
        return byteList;
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

    public static Handle string() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "string", sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, String.class, int.class));
    }

    public static Handle fstring() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "fstring", sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, String.class, int.class, String.class, int.class));
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

    public static Handle global() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "globalBootstrap", sig(CallSite.class, Lookup.class, String.class, MethodType.class, String.class, int.class));
    }

    public static RubyString string(MutableCallSite site, ByteList value, int cr, ThreadContext context) throws Throwable {
        MethodHandle handle = SmartBinder
                .from(STRING_SIGNATURE)
                .invoke(NEW_STRING_SHARED_HANDLE.apply("byteList", value))
                .handle();

        site.setTarget(handle);

        return RubyString.newStringShared(context.runtime, value, cr);
    }

    public static RubyString frozenString(MutableCallSite site, ByteList value, int cr, String file, int line, ThreadContext context) throws Throwable {
        RubyString frozen = IRRuntimeHelpers.newFrozenString(context, value, cr, file, line);
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
        return RubyArray.newArrayMayCopy(context.runtime, elts);
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

    static MethodHandle buildIndyHandle(InvokeSite site, DynamicMethod method, RubyModule implClass) {
        MethodHandle mh = null;
        Signature siteToDyncall = site.signature.insertArgs(3, arrayOf("class", "name"), arrayOf(RubyModule.class, String.class));

        if (method instanceof HandleMethod) {
            HandleMethod handleMethod = (HandleMethod)method;
            boolean blockGiven = site.signature.lastArgType() == Block.class;

            if (site.arity >= 0) {
                mh = handleMethod.getHandle(site.arity);
                if (mh != null) {
                    if (!blockGiven) mh = insertArguments(mh, mh.type().parameterCount() - 1, Block.NULL_BLOCK);
                    mh = dropArguments(mh, 1, IRubyObject.class);
                } else {
                    mh = handleMethod.getHandle(-1);
                    mh = dropArguments(mh, 1, IRubyObject.class);
                    if (site.arity == 0) {
                        if (!blockGiven) {
                            mh = insertArguments(mh, mh.type().parameterCount() - 2, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
                        } else {
                            mh = insertArguments(mh, mh.type().parameterCount() - 2, (Object)IRubyObject.NULL_ARRAY);
                        }
                    } else {
                        // bundle up varargs
                        if (!blockGiven) mh = insertArguments(mh, mh.type().parameterCount() - 1, Block.NULL_BLOCK);

                        mh = SmartBinder.from(lookup(), siteToDyncall)
                                .collect("args", "arg.*")
                                .invoke(mh)
                                .handle();
                    }
                }
            } else {
                mh = handleMethod.getHandle(-1);
                if (mh != null) {
                    mh = dropArguments(mh, 1, IRubyObject.class);
                    if (!blockGiven) mh = insertArguments(mh, mh.type().parameterCount() - 1, Block.NULL_BLOCK);

                    mh = SmartBinder.from(lookup(), siteToDyncall)
                            .invoke(mh)
                            .handle();
                }
            }

            if (mh != null) {
                mh = insertArguments(mh, 3, implClass, site.name());
            }
        }

        return mh;
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

    static MethodHandle buildAttrHandle(InvokeSite site, DynamicMethod method, IRubyObject self, RubyClass dispatchClass) {
        if (method instanceof AttrReaderMethod && site.arity == 0) {
            AttrReaderMethod attrReader = (AttrReaderMethod) method;
            String varName = attrReader.getVariableName();

            // we getVariableAccessorForWrite here so it is eagerly created and we don't cache the DUMMY
            VariableAccessor accessor = dispatchClass.getRealClass().getVariableAccessorForWrite(varName);

            // Ruby to attr reader
            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                if (accessor instanceof FieldVariableAccessor) {
                    LOG.info(site.name() + "\tbound as field attr reader " + logMethod(method) + ":" + ((AttrReaderMethod)method).getVariableName());
                } else {
                    LOG.info(site.name() + "\tbound as attr reader " + logMethod(method) + ":" + ((AttrReaderMethod)method).getVariableName());
                }
            }

            return createAttrReaderHandle(site, self, dispatchClass.getRealClass(), accessor);
        } else if (method instanceof AttrWriterMethod && site.arity == 1) {
            AttrWriterMethod attrReader = (AttrWriterMethod)method;
            String varName = attrReader.getVariableName();

            // we getVariableAccessorForWrite here so it is eagerly created and we don't cache the DUMMY
            VariableAccessor accessor = dispatchClass.getRealClass().getVariableAccessorForWrite(varName);

            // Ruby to attr reader
            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                if (accessor instanceof FieldVariableAccessor) {
                    LOG.info(site.name() + "\tbound as field attr writer " + logMethod(method) + ":" + ((AttrWriterMethod) method).getVariableName());
                } else {
                    LOG.info(site.name() + "\tbound as attr writer " + logMethod(method) + ":" + ((AttrWriterMethod) method).getVariableName());
                }
            }

            return createAttrWriterHandle(site, self, dispatchClass.getRealClass(), accessor);
        }

        return null;
    }

    private static MethodHandle createAttrReaderHandle(InvokeSite site, IRubyObject self, RubyClass cls, VariableAccessor accessor) {
        MethodHandle nativeTarget;

        MethodHandle filter = Binder
                .from(IRubyObject.class, IRubyObject.class)
                .insert(1, cls.getRuntime().getNil())
                .cast(IRubyObject.class, IRubyObject.class, IRubyObject.class)
                .invokeStaticQuiet(lookup(), Bootstrap.class, "valueOrNil");

        MethodHandle getValue;

        if (accessor instanceof FieldVariableAccessor) {
            int offset = ((FieldVariableAccessor)accessor).getOffset();
            getValue = Binder.from(site.type())
                    .drop(0, 2)
                    .filterReturn(filter)
                    .cast(methodType(Object.class, self.getClass()))
                    .getFieldQuiet(LOOKUP, "var" + offset);
        } else {
            getValue = Binder.from(site.type())
                    .drop(0, 2)
                    .filterReturn(filter)
                    .cast(methodType(Object.class, Object.class))
                    .prepend(accessor)
                    .invokeVirtualQuiet(LOOKUP, "get");
        }

        // NOTE: Must not cache the fully-bound handle in the method, since it's specific to this class

        return getValue;
    }

    public static IRubyObject valueOrNil(IRubyObject value, IRubyObject nil) {
        return value == null ? nil : value;
    }

    private static MethodHandle createAttrWriterHandle(InvokeSite site, IRubyObject self, RubyClass cls, VariableAccessor accessor) {
        MethodHandle nativeTarget;

        MethodHandle filter = Binder
                .from(IRubyObject.class, Object.class)
                .drop(0)
                .constant(cls.getRuntime().getNil());

        MethodHandle setValue;

        if (accessor instanceof FieldVariableAccessor) {
            int offset = ((FieldVariableAccessor)accessor).getOffset();
            setValue = Binder.from(site.type())
                    .drop(0, 2)
                    .filterReturn(filter)
                    .cast(methodType(void.class, self.getClass(), Object.class))
                    .invokeVirtualQuiet(LOOKUP, "setVariable" + offset);
        } else {
            setValue = Binder.from(site.type())
                    .drop(0, 2)
                    .filterReturn(filter)
                    .cast(methodType(void.class, Object.class, Object.class))
                    .prepend(accessor)
                    .invokeVirtualQuiet(LOOKUP, "set");
        }

        return setValue;
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

            // Temporary fix for missing kwargs dup+splitting logic from frobnicate, called by CompiledIRMethod but
            // skipped by indy's direct binding.
            if (compiledIRMethod.hasKwargs()) return null;

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

        if (method instanceof NativeCallMethod && ((NativeCallMethod) method).getNativeCall() != null) {
            NativeCallMethod nativeMethod = (NativeCallMethod)method;
            DynamicMethod.NativeCall nativeCall = nativeMethod.getNativeCall();

            DynamicMethod.NativeCall nc = nativeCall;

            if (nc.isJava()) {
                // not supported yet, use DynamicMethod.call
            } else {
                int nativeArgCount = getNativeArgCount(method, nativeCall);

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

                JRubyMethod anno = nativeCall.getMethod().getAnnotation(JRubyMethod.class);
                if (anno != null && anno.frame()) {
                    mh = InvocationLinker.wrapWithFrameOnly(site.signature, method.getImplementationClass(), site.name(), mh);
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
        MethodHandle nullToNil = self.getRuntime().getNullToNilHandle();

        // get variable value and filter with nullToNil
        MethodHandle getValue;
        boolean direct = false;

        if (accessor instanceof FieldVariableAccessor) {
            direct = true;
            int offset = ((FieldVariableAccessor)accessor).getOffset();
            getValue = lookup().findGetter(self.getClass(), "var" + offset, Object.class);
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
        MethodHandle test = findStatic(Bootstrap.class, "testRealClass", methodType(boolean.class, int.class, IRubyObject.class));
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
            setValue = findVirtual(self.getClass(), "setVariable" + offset, methodType(void.class, Object.class));
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
        MethodHandle test = findStatic(Bootstrap.class, "testRealClass", methodType(boolean.class, int.class, IRubyObject.class));
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

    private static MethodHandle findVirtual(Class target, String name, MethodType type) {
        return findVirtual(lookup(), target, name, type);
    }

    private static MethodHandle findVirtual(Lookup lookup, Class target, String name, MethodType type) {
        try {
            return lookup.findVirtual(target, name, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean testRealClass(int id, IRubyObject self) {
        return id == ((RubyBasicObject)self).getMetaClass().getRealClass().id;
    }

    public static boolean testType(RubyClass original, IRubyObject self) {
        // naive test
        return ((RubyBasicObject)self).getMetaClass() == original;
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

    public static boolean testModuleMatch(ThreadContext context, IRubyObject arg0, int id) {
        return arg0 instanceof RubyModule && ((RubyModule)arg0).id == id;
    }

    private static String extractSourceInfo(VariableSite site) {
        return " (" + site.file() + ":" + site.line() + ")";
    }

    public static Handle getFixnumOperatorHandle() {
        return getBootstrapHandle("fixnumOperatorBootstrap", MathLinker.class, BOOTSTRAP_LONG_STRING_INT_SIG);
    }

    public static Handle getFloatOperatorHandle() {
        return getBootstrapHandle("floatOperatorBootstrap", MathLinker.class, BOOTSTRAP_DOUBLE_STRING_INT_SIG);
    }

    public static Handle checkpointHandle() {
        return getBootstrapHandle("checkpointBootstrap", BOOTSTRAP_BARE_SIG);
    }

    public static Handle getBootstrapHandle(String name, String sig) {
        return getBootstrapHandle(name, Bootstrap.class, sig);
    }

    public static Handle getBootstrapHandle(String name, Class type, String sig) {
        return new Handle(Opcodes.H_INVOKESTATIC, p(type), name, sig);
    }

    public static CallSite checkpointBootstrap(Lookup lookup, String name, MethodType type) throws Throwable {
        MutableCallSite site = new MutableCallSite(type);
        MethodHandle handle = lookup.findStatic(Bootstrap.class, "checkpointFallback", methodType(void.class, MutableCallSite.class, ThreadContext.class));

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
        MethodHandle fallback = lookup().findStatic(Bootstrap.class, "checkpointFallback", methodType(void.class, MutableCallSite.class, ThreadContext.class));
        fallback = fallback.bindTo(site);

        target = ((SwitchPoint)invalidator.getData()).guardWithTest(target, fallback);

        site.setTarget(target);
    }

    public static CallSite globalBootstrap(Lookup lookup, String name, MethodType type, String file, int line) throws Throwable {
        String[] names = name.split(":");
        String operation = names[0];
        String varName = JavaNameMangler.demangleMethodName(names[1]);
        GlobalSite site = new GlobalSite(type, varName, file, line);
        MethodHandle handle;

        if (operation.equals("get")) {
            handle = lookup.findStatic(Bootstrap.class, "getGlobalFallback", methodType(IRubyObject.class, GlobalSite.class, ThreadContext.class));
        } else {
            handle = lookup.findStatic(Bootstrap.class, "setGlobalFallback", methodType(void.class, GlobalSite.class, IRubyObject.class, ThreadContext.class));
        }

        handle = handle.bindTo(site);
        site.setTarget(handle);

        return site;
    }

    public static IRubyObject getGlobalFallback(GlobalSite site, ThreadContext context) throws Throwable {
        Ruby runtime = context.runtime;
        GlobalVariable variable = runtime.getGlobalVariables().getVariable(site.name());

        if (site.failures() > Options.INVOKEDYNAMIC_GLOBAL_MAXFAIL.load() ||
                variable.getScope() != GlobalVariable.Scope.GLOBAL ||
                RubyGlobal.UNCACHED_GLOBALS.contains(site.name())) {

            // use uncached logic forever
            if (Options.INVOKEDYNAMIC_LOG_GLOBALS.load()) LOG.info("global " + site.name() + " (" + site.file() + ":" + site.line() + ") uncacheable or rebound > " + Options.INVOKEDYNAMIC_GLOBAL_MAXFAIL.load() + " times, reverting to simple lookup");

            MethodHandle uncached = lookup().findStatic(Bootstrap.class, "getGlobalUncached", methodType(IRubyObject.class, GlobalVariable.class));
            uncached = uncached.bindTo(variable);
            uncached = dropArguments(uncached, 0, ThreadContext.class);
            site.setTarget(uncached);
            return (IRubyObject)uncached.invokeWithArguments(context);
        }

        Invalidator invalidator = variable.getInvalidator();
        IRubyObject value = variable.getAccessor().getValue();

        MethodHandle target = constant(IRubyObject.class, value);
        target = dropArguments(target, 0, ThreadContext.class);
        MethodHandle fallback = lookup().findStatic(Bootstrap.class, "getGlobalFallback", methodType(IRubyObject.class, GlobalSite.class, ThreadContext.class));
        fallback = fallback.bindTo(site);

        target = ((SwitchPoint)invalidator.getData()).guardWithTest(target, fallback);

        site.setTarget(target);

//        if (Options.INVOKEDYNAMIC_LOG_GLOBALS.load()) LOG.info("global " + site.name() + " (" + site.file() + ":" + site.line() + ") cached");

        return value;
    }

    public static IRubyObject getGlobalUncached(GlobalVariable variable) throws Throwable {
        return variable.getAccessor().getValue();
    }

    public static void setGlobalFallback(GlobalSite site, IRubyObject value, ThreadContext context) throws Throwable {
        Ruby runtime = context.runtime;
        GlobalVariable variable = runtime.getGlobalVariables().getVariable(site.name());
        MethodHandle uncached = lookup().findStatic(Bootstrap.class, "setGlobalUncached", methodType(void.class, GlobalVariable.class, IRubyObject.class));
        uncached = uncached.bindTo(variable);
        uncached = dropArguments(uncached, 1, ThreadContext.class);
        site.setTarget(uncached);
        uncached.invokeWithArguments(value, context);
    }

    public static void setGlobalUncached(GlobalVariable variable, IRubyObject value) throws Throwable {
        // FIXME: duplicated logic from GlobalVariables.set
        variable.getAccessor().setValue(value);
        variable.trace(value);
        variable.invalidate();
    }

    public static Handle prepareBlock() {
        return new Handle(Opcodes.H_INVOKESTATIC, p(Bootstrap.class), "prepareBlock", sig(CallSite.class, Lookup.class, String.class, MethodType.class, MethodHandle.class, MethodHandle.class, long.class));
    }

    public static CallSite prepareBlock(Lookup lookup, String name, MethodType type, MethodHandle bodyHandle, MethodHandle scopeHandle, long encodedSignature) throws Throwable {
        IRScope scope = (IRScope)scopeHandle.invokeExact();

        CompiledIRBlockBody body = new CompiledIRBlockBody(bodyHandle, scope, encodedSignature);

        Binder binder = Binder.from(type);

        binder = binder.fold(FRAME_SCOPE_BINDING);

        // This optimization can't happen until we can see into the method we're calling to know if it reifies the block
        if (false) {
            if (scope.needsBinding()) {
                if (scope.needsFrame()) {
                    binder = binder.fold(FRAME_SCOPE_BINDING);
                } else {
                    binder = binder.fold(SCOPE_BINDING);
                }
            } else {
                if (scope.needsFrame()) {
                    binder = binder.fold(FRAME_BINDING);
                } else {
                    binder = binder.fold(SELF_BINDING);
                }
            }
        }

        MethodHandle blockMaker = binder.drop(1, 3)
                .append(body)
                .invoke(CONSTRUCT_BLOCK);

        return new ConstantCallSite(blockMaker);
    }

    private static String logMethod(DynamicMethod method) {
        return "[#" + method.getSerialNumber() + " " + method.getImplementationClass() + "]";
    }

    private static final Binder BINDING_MAKER_BINDER = Binder.from(Binding.class, ThreadContext.class, IRubyObject.class, DynamicScope.class);

    private static final MethodHandle FRAME_SCOPE_BINDING = BINDING_MAKER_BINDER.invokeStaticQuiet(LOOKUP, Bootstrap.class, "frameScopeBinding");
    public static Binding frameScopeBinding(ThreadContext context, IRubyObject self, DynamicScope scope) {
        Frame frame = context.getCurrentFrame().capture();
        return new Binding(self, frame, frame.getVisibility(), scope);
    }

    private static final MethodHandle FRAME_BINDING = BINDING_MAKER_BINDER.invokeStaticQuiet(LOOKUP, Bootstrap.class, "frameBinding");
    public static Binding frameBinding(ThreadContext context, IRubyObject self, DynamicScope scope) {
        Frame frame = context.getCurrentFrame().capture();
        return new Binding(self, frame, frame.getVisibility());
    }

    private static final MethodHandle SCOPE_BINDING = BINDING_MAKER_BINDER.invokeStaticQuiet(LOOKUP, Bootstrap.class, "scopeBinding");
    public static Binding scopeBinding(ThreadContext context, IRubyObject self, DynamicScope scope) {
        return new Binding(self, scope);
    }

    private static final MethodHandle SELF_BINDING = BINDING_MAKER_BINDER.invokeStaticQuiet(LOOKUP, Bootstrap.class, "selfBinding");
    public static Binding selfBinding(ThreadContext context, IRubyObject self, DynamicScope scope) {
        return new Binding(self);
    }

    private static final MethodHandle CONSTRUCT_BLOCK = Binder.from(Block.class, Binding.class, CompiledIRBlockBody.class).invokeStaticQuiet(LOOKUP, Bootstrap.class, "constructBlock");
    public static Block constructBlock(Binding binding, CompiledIRBlockBody body) throws Throwable {
        return new Block(body, binding);
    }
}
