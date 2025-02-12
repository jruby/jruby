package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.Signature;
import com.headius.invokebinder.SmartBinder;
import com.headius.invokebinder.SmartHandle;
import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.RubyNil;
import org.jruby.RubyStruct;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.AliasMethod;
import org.jruby.internal.runtime.methods.AttrReaderMethod;
import org.jruby.internal.runtime.methods.AttrWriterMethod;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.HandleMethod;
import org.jruby.internal.runtime.methods.MixedModeIRMethod;
import org.jruby.internal.runtime.methods.NativeCallMethod;
import org.jruby.internal.runtime.methods.PartialDelegatingMethod;
import org.jruby.ir.JIT;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.targets.SiteTracker;
import org.jruby.java.invokers.InstanceFieldGetter;
import org.jruby.java.invokers.InstanceFieldSetter;
import org.jruby.javasupport.JavaUtil;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.invokedynamic.InvocationLinker;
import org.jruby.runtime.invokedynamic.JRubyCallSite;
import org.jruby.runtime.ivars.FieldVariableAccessor;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.util.CodegenUtils;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.SwitchPoint;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.lang.invoke.MethodHandles.constant;
import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.foldArguments;
import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;
import static org.jruby.RubySymbol.newSymbol;
import static org.jruby.api.Access.basicObjectClass;
import static org.jruby.api.Access.kernelModule;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.runtime.Helpers.arrayOf;
import static org.jruby.runtime.Helpers.constructObjectArrayHandle;
import static org.jruby.runtime.invokedynamic.JRubyCallSite.SITE_ID;

/**
* Created by headius on 10/23/14.
*/
public abstract class InvokeSite extends MutableCallSite {

    private static final Logger LOG = LoggerFactory.getLogger(InvokeSite.class);
    private static final String[] GENERIC_CALL_PERMUTE = {"context", "self", "arg.*"};

    static { // enable DEBUG output
        if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) LOG.setDebugEnable(true);
    }
    private static final boolean LOG_BINDING = LOG.isDebugEnabled();
    static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public final Signature signature;
    public final Signature fullSignature;
    public final int arity;
    protected final String methodName;
    final MethodHandle fallback;
    private final SiteTracker tracker = new SiteTracker();
    private final long siteID = SITE_ID.getAndIncrement();
    public final int argOffset;
    public final boolean functional;
    protected final String file;
    protected final int line;
    protected final int flags;
    private boolean boundOnce;
    private boolean literalClosure;
    protected CacheEntry cache = CacheEntry.NULL_CACHE;

    public static boolean testType(RubyClass original, IRubyObject self) {
        // naive test
        return original == RubyBasicObject.getMetaClass(self);
    }

    MethodHandle buildIndyHandle(CacheEntry entry) {
        MethodHandle mh = null;
        Signature siteToDyncall = signature.insertArgs(argOffset, arrayOf("class", "name"), arrayOf(RubyModule.class, String.class));
        DynamicMethod method = entry.method;

        if (method instanceof HandleMethod) {
            HandleMethod handleMethod = (HandleMethod)method;
            boolean blockGiven = signature.lastArgType() == Block.class;

            if (arity >= 0) {
                mh = handleMethod.getHandle(arity);
                if (mh != null) {
                    if (!blockGiven) mh = insertArguments(mh, mh.type().parameterCount() - 1, Block.NULL_BLOCK);
                    if (!functional) mh = dropArguments(mh, 1, IRubyObject.class);
                } else {
                    mh = handleMethod.getHandle(-1);
                    if (!functional) mh = dropArguments(mh, 1, IRubyObject.class);
                    if (arity == 0) {
                        if (!blockGiven) {
                            mh = insertArguments(mh, mh.type().parameterCount() - 2, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
                        } else {
                            mh = insertArguments(mh, mh.type().parameterCount() - 2, (Object)IRubyObject.NULL_ARRAY);
                        }
                    } else {
                        // bundle up varargs
                        if (!blockGiven) mh = insertArguments(mh, mh.type().parameterCount() - 1, Block.NULL_BLOCK);

                        mh = SmartBinder.from(lookup(), siteToDyncall)
                                .collect("args", "arg.*", Helpers.constructObjectArrayHandle(arity))
                                .invoke(mh)
                                .handle();
                    }
                }
            } else {
                mh = handleMethod.getHandle(-1);
                if (mh != null) {
                    if (!functional) mh = dropArguments(mh, 1, IRubyObject.class);
                    if (!blockGiven) mh = insertArguments(mh, mh.type().parameterCount() - 1, Block.NULL_BLOCK);

                    mh = SmartBinder.from(lookup(), siteToDyncall)
                            .invoke(mh)
                            .handle();
                }
            }

            if (mh != null) {
                mh = insertArguments(mh, argOffset, entry.sourceModule, name());

                if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                    LOG.info(name() + "\tbound directly to handle " + Bootstrap.logMethod(method));
                }
            }
        }

        return mh;
    }

    MethodHandle buildGenericHandle(CacheEntry entry) {
        SmartBinder binder;
        DynamicMethod method = entry.method;

        binder = SmartBinder.from(signature);

        binder = permuteForGenericCall(binder, method, GENERIC_CALL_PERMUTE);


        binder = binder
                .insert(2, new String[]{"rubyClass", "name"}, new Class[]{RubyModule.class, String.class}, entry.sourceModule, name())
                .insert(0, "method", DynamicMethod.class, method);

        if (arity > 3) {
            binder = binder.collect("args", "arg.*", constructObjectArrayHandle(arity));
        }

        if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
            LOG.info(name() + "\tbound indirectly " + method + ", " + Bootstrap.logMethod(method));
        }

        return binder.invokeVirtualQuiet(LOOKUP, "call").handle();
    }

    private static SmartBinder permuteForGenericCall(SmartBinder binder, DynamicMethod method, String... basePermutes) {
        if (methodWantsBlock(method)) {
            binder = binder.permute(arrayOf(basePermutes, "block", String[]::new));
        } else {
            binder = binder.permute(basePermutes);
        }
        return binder;
    }

    private static boolean methodWantsBlock(DynamicMethod method) {
        // only include block if native signature receives block, whatever its arity
        boolean wantsBlock = true;
        if (method instanceof NativeCallMethod) {
            DynamicMethod.NativeCall nativeCall = ((NativeCallMethod) method).getNativeCall();
            // if it is a non-JI native call and does not want block, drop it
            // JI calls may lazily convert blocks to an interface type (jruby/jruby#7246)
            if (nativeCall != null && !nativeCall.isJava()) {
                Class[] nativeSignature = nativeCall.getNativeSignature();

                // no args or last arg not a block, do no pass block
                if (nativeSignature.length == 0 || nativeSignature[nativeSignature.length - 1] != Block.class) {
                    wantsBlock = false;
                }
            }
        }
        return wantsBlock;
    }

    static MethodHandle buildMethodMissingHandle(InvokeSite site, CacheEntry entry, IRubyObject self) {
        SmartBinder binder;
        DynamicMethod method = entry.method;

        if (site.arity >= 0) {
            binder = SmartBinder.from(site.signature);

            binder = permuteForGenericCall(binder, method, GENERIC_CALL_PERMUTE)
                    .insert(2,
                            new String[]{"rubyClass", "name", "argName"}
                            , new Class[]{RubyModule.class, String.class, IRubyObject.class},
                            entry.sourceModule,
                            site.name(),
                            newSymbol(self.getRuntime(), site.methodName))
                    .insert(0, "method", DynamicMethod.class, method)
                    .collect("args", "arg.*", Helpers.constructObjectArrayHandle(site.arity + 1));
        } else {
            SmartHandle fold = SmartBinder.from(
                    site.signature
                            .permute("context", "self", "args", "block")
                            .changeReturn(IRubyObject[].class))
                    .permute("args")
                    .insert(0, "argName", IRubyObject.class, newSymbol(self.getRuntime(), site.methodName))
                    .invokeStaticQuiet(LOOKUP, Helpers.class, "arrayOf");

            binder = SmartBinder.from(site.signature);

            binder = permuteForGenericCall(binder, method, "context", "self", "args")
                    .fold("args2", fold);
            binder = permuteForGenericCall(binder, method, "context", "self", "args2")
                    .insert(2,
                            new String[]{"rubyClass", "name"}
                            , new Class[]{RubyModule.class, String.class},
                            entry.sourceModule,
                            site.name())
                    .insert(0, "method", DynamicMethod.class, method);
        }

        if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
            LOG.info(site.name() + "\tbound to method_missing for " + method + ", " + Bootstrap.logMethod(method));
        }

        return binder.invokeVirtualQuiet(LOOKUP, "call").handle();
    }

    MethodHandle buildAttrHandle(CacheEntry entry, IRubyObject self) {
        DynamicMethod method = entry.method;

        if (method instanceof AttrReaderMethod && arity == 0) {
            AttrReaderMethod attrReader = (AttrReaderMethod) method;
            String varName = attrReader.getVariableName();

            // we getVariableAccessorForWrite here so it is eagerly created and we don't cache the DUMMY
            VariableAccessor accessor = self.getType().getVariableAccessorForWrite(varName);

            // Ruby to attr reader
            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                if (accessor instanceof FieldVariableAccessor) {
                    LOG.info(name() + "\tbound as field attr reader " + Bootstrap.logMethod(method) + ":" + ((AttrReaderMethod)method).getVariableName());
                } else {
                    LOG.info(name() + "\tbound as attr reader " + Bootstrap.logMethod(method) + ":" + ((AttrReaderMethod)method).getVariableName());
                }
            }

            return createAttrReaderHandle(self, self.getType(), accessor);
        } else if (method instanceof AttrWriterMethod && arity == 1) {
            AttrWriterMethod attrReader = (AttrWriterMethod)method;
            String varName = attrReader.getVariableName();

            // we getVariableAccessorForWrite here so it is eagerly created and we don't cache the DUMMY
            VariableAccessor accessor = self.getType().getVariableAccessorForWrite(varName);

            // Ruby to attr reader
            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                if (accessor instanceof FieldVariableAccessor) {
                    LOG.info(name() + "\tbound as field attr writer " + Bootstrap.logMethod(method) + ":" + ((AttrWriterMethod) method).getVariableName());
                } else {
                    LOG.info(name() + "\tbound as attr writer " + Bootstrap.logMethod(method) + ":" + ((AttrWriterMethod) method).getVariableName());
                }
            }

            return createAttrWriterHandle(self, self.getType(), accessor);
        }

        return null;
    }

    private MethodHandle createAttrReaderHandle(IRubyObject self, RubyClass cls, VariableAccessor accessor) {
        MethodHandle filter = cls.getClassRuntime().getNullToNilHandle();

        MethodHandle getValue;

        SmartBinder binder = SmartBinder.from(signature).permute("self");

        if (accessor instanceof FieldVariableAccessor) {
            MethodHandle getter = ((FieldVariableAccessor)accessor).getGetter();
            getValue = binder
                    .filterReturn(filter)
                    .cast(Object.class, self.getClass())
                    .invoke(getter).handle();
        } else {
            getValue = binder
                    .filterReturn(filter)
                    .cast(Object.class, Object.class)
                    .prepend("accessor", accessor)
                    .invokeVirtualQuiet(LOOKUP, "get").handle();
        }

        // NOTE: Must not cache the fully-bound handle in the method, since it's specific to this class

        return getValue;
    }

    private MethodHandle createAttrWriterHandle(IRubyObject self, RubyClass cls, VariableAccessor accessor) {
        MethodHandle filter = Binder
                .from(IRubyObject.class, Object.class)
                .drop(0)
                .constant(cls.getRuntime().getNil());

        MethodHandle setValue;

        SmartBinder binder = SmartBinder.from(signature).permute("self", "arg0");

        if (accessor instanceof FieldVariableAccessor) {
            MethodHandle setter = ((FieldVariableAccessor)accessor).getSetter();
            setValue = binder
                    .filterReturn(filter)
                    .cast(void.class, self.getClass(), Object.class)
                    .invoke(setter).handle();
        } else {
            setValue = binder
                    .filterReturn(filter)
                    .cast(void.class, Object.class, Object.class)
                    .prepend("accessor", accessor)
                    .invokeVirtualQuiet(LOOKUP, "set").handle();
        }

        return setValue;
    }

    MethodHandle buildJittedHandle(CacheEntry entry, boolean blockGiven) {
        MethodHandle mh = null;
        SmartBinder binder;
        CompiledIRMethod compiledIRMethod = null;
        DynamicMethod method = entry.method;
        RubyModule sourceModule = entry.sourceModule;

        if (method instanceof CompiledIRMethod) {
            compiledIRMethod = (CompiledIRMethod)method;
        } else if (method instanceof MixedModeIRMethod) {
            DynamicMethod actualMethod = ((MixedModeIRMethod)method).getActualMethod();
            if (actualMethod instanceof CompiledIRMethod) {
                compiledIRMethod = (CompiledIRMethod) actualMethod;
            } else {
                if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                    LOG.info(name() + "\tfailed direct binding due to unjitted method " + Bootstrap.logMethod(method));
                }
            }
        }

        if (compiledIRMethod != null) {

            // attempt IR direct binding
            // TODO: this will have to expand when we start specializing arities

            binder = SmartBinder.from(signature)
                    .permute("context", "self", "arg.*", "block");

            if (arity == -1) {
                // already [], nothing to do
                mh = (MethodHandle)compiledIRMethod.getHandle();
            } else if (arity == 0) {
                MethodHandle specific;
                if ((specific = compiledIRMethod.getHandleFor(arity)) != null) {
                    mh = specific;
                } else {
                    mh = (MethodHandle)compiledIRMethod.getHandle();
                    binder = binder.insert(2, "args", IRubyObject.NULL_ARRAY);
                }
            } else {
                MethodHandle specific;
                if ((specific = compiledIRMethod.getHandleFor(arity)) != null) {
                    mh = specific;
                } else {
                    mh = (MethodHandle) compiledIRMethod.getHandle();
                    binder = binder.collect("args", "arg.*", Helpers.constructObjectArrayHandle(arity));
                }
            }

            if (!blockGiven) {
                binder = binder.append("block", Block.class, Block.NULL_BLOCK);
            }

            binder = binder
                    .insert(1, "scope", StaticScope.class, compiledIRMethod.getStaticScope())
                    .append("class", RubyModule.class, sourceModule)
                    .append("frameName", String.class, name());

            mh = binder.invoke(mh).handle();

            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                LOG.info(name() + "\tbound directly to jitted method " + Bootstrap.logMethod(method));
            }
        }

        return mh;
    }

    MethodHandle buildNativeHandle(CacheEntry entry, boolean blockGiven) {
        MethodHandle mh = null;
        SmartBinder binder = null;
        DynamicMethod method = entry.method;

        if (method instanceof NativeCallMethod && ((NativeCallMethod) method).getNativeCall() != null) {
            NativeCallMethod nativeMethod = (NativeCallMethod)method;
            DynamicMethod.NativeCall nativeCall = nativeMethod.getNativeCall();

            if (nativeCall.isJava()) {
                return JavaBootstrap.createJavaHandle(this, method);
            } else {
                // always try to bind exact arg count; fallback to other paths
                DynamicMethod.NativeCall exactNativeCall = buildExactNativeCall(nativeCall, arity);
                if (exactNativeCall != null) {
                    binder = SmartBinder.from(lookup(), signature);
                    nativeCall = exactNativeCall;
                } else {
                    int nativeArgCount = getArgCount(nativeCall.getNativeSignature(), nativeCall.isStatic());

                    if (nativeArgCount >= 0) { // native methods only support arity 3
                        // if non-Java, must:
                        // * exactly match arities or both are [] boxed
                        // * 3 or fewer arguments
                        if (nativeArgCount == arity) {
                            // nothing to do
                            binder = SmartBinder.from(lookup(), signature);
                        } else {
                            // arity mismatch...leave null and use DynamicMethod.call below
                            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                                LOG.info(name() + "\tdid not match the primary arity for a native method " + Bootstrap.logMethod(method));
                            }
                        }
                    } else {
                        // varargs
                        if (arity == -1) {
                            // ok, already passing []
                            binder = SmartBinder.from(lookup(), signature);
                        } else if (arity == 0) {
                            // no args, insert dummy
                            binder = SmartBinder.from(lookup(), signature)
                                    .insert(argOffset, "args", IRubyObject.NULL_ARRAY);
                        } else {
                            // 1 or more args, collect into []
                            binder = SmartBinder.from(lookup(), signature)
                                    .collect("args", "arg.*", Helpers.constructObjectArrayHandle(arity));
                        }
                    }
                }

                if (binder != null) {

                    // clean up non-arguments, ordering, types
                    if (!nativeCall.hasContext()) {
                        binder = binder.drop("context");
                    }

                    if (nativeCall.hasBlock() && !blockGiven) {
                        binder = binder.append("block", Block.NULL_BLOCK);
                    } else if (!nativeCall.hasBlock() && blockGiven) {
                        binder = binder.drop("block");
                    }

                    if (nativeCall.isStatic()) {
                        mh = binder
                                .permute("context", "self", "arg.*", "block") // filter caller
                                .cast(nativeCall.getNativeReturn(), nativeCall.getNativeSignature())
                                .invokeStaticQuiet(LOOKUP, nativeCall.getNativeTarget(), nativeCall.getNativeName())
                                .handle();
                    } else {
                        mh = binder
                                .permute("self", "context", "arg.*", "block") // filter caller, move self
                                .castArg("self", nativeCall.getNativeTarget())
                                .castVirtual(nativeCall.getNativeReturn(), nativeCall.getNativeTarget(), nativeCall.getNativeSignature())
                                .invokeVirtualQuiet(LOOKUP, nativeCall.getNativeName())
                                .handle();
                    }

                    if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                        LOG.info(name() + "\tbound directly to JVM method " + Bootstrap.logMethod(method));
                    }

                    JRubyMethod anno = nativeCall.getMethod().getAnnotation(JRubyMethod.class);
                    if (anno != null && anno.frame()) {
                        mh = InvocationLinker.wrapWithFrameOnly(signature, entry.sourceModule, name(), mh);
                    }
                }
            }
        }

        return mh;
    }

    private static DynamicMethod.NativeCall buildExactNativeCall(DynamicMethod.NativeCall nativeCall, int arity) {
        Class[] args = nativeCall.getNativeSignature();

        int rubyArgCount = args.length;
        boolean hasContext = false;
        boolean hasBlock = false;
        if (nativeCall.isStatic()) {
            if (args.length > 1 && args[0] == ThreadContext.class) {
                rubyArgCount--;
                hasContext = true;
            }

            // remove self object
            assert args.length >= 1;
            rubyArgCount--;

            if (args.length > 1 && args[args.length - 1] == Block.class) {
                rubyArgCount--;
                hasBlock = true;
            }

            if (rubyArgCount == 1) {
                if (hasContext && args[2] == IRubyObject[].class) {
                    return null;
                } else if (args[1] == IRubyObject[].class) {
                    return null;
                }
            }
        } else {
            if (args.length > 0 && args[0] == ThreadContext.class) {
                rubyArgCount--;
                hasContext = true;
            }

            if (args.length > 0 && args[args.length - 1] == Block.class) {
                rubyArgCount--;
                hasBlock = true;
            }

            if (rubyArgCount == 1) {
                if (hasContext && args[1] == IRubyObject[].class) {
                    return null;
                } else if (args[0] == IRubyObject[].class) {
                    return null;
                }
            }
        }

        // rebuild with requested arity
        Class[] params = null;

        if (nativeCall.isStatic()) {
            if (hasContext) {
                if (hasBlock) {
                    if (arity == -1) {
                        params = CodegenUtils.params(ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class);
                    } else if (arity <= 3) {
                        params = CodegenUtils.params(ThreadContext.class, IRubyObject.class, IRubyObject.class, arity, Block.class);
                    }
                } else {
                    if (arity == -1) {
                        params = CodegenUtils.params(ThreadContext.class, IRubyObject.class, IRubyObject[].class);
                    } else if (arity <= 3) {
                        params = CodegenUtils.params(ThreadContext.class, IRubyObject.class, IRubyObject.class, arity);
                    }
                }
            } else {
                if (hasBlock) {
                    if (arity == -1) {
                        params = CodegenUtils.params(IRubyObject.class, IRubyObject[].class, Block.class);
                    } else if (arity <= 3) {
                        params = CodegenUtils.params(IRubyObject.class, IRubyObject.class, arity, Block.class);
                    }
                } else {
                    if (arity == -1) {
                        params = CodegenUtils.params(IRubyObject.class, IRubyObject[].class);
                    } else if (arity <= 3) {
                        params = CodegenUtils.params(IRubyObject.class, IRubyObject.class, arity);
                    }
                }
            }
        } else {
            if (hasContext) {
                if (hasBlock) {
                    if (arity == -1) {
                        params = CodegenUtils.params(ThreadContext.class, IRubyObject[].class, Block.class);
                    } else if (arity <= 3) {
                        params = CodegenUtils.params(ThreadContext.class, IRubyObject.class, arity, Block.class);
                    }
                } else {
                    if (arity == -1) {
                        params = CodegenUtils.params(ThreadContext.class, IRubyObject[].class);
                    } else if (arity <= 3) {
                        params = CodegenUtils.params(ThreadContext.class, IRubyObject.class, arity);
                    }
                }
            } else {
                if (hasBlock) {
                    if (arity == -1) {
                        params = CodegenUtils.params(IRubyObject[].class, Block.class);
                    } else if (arity <= 3) {
                        params = CodegenUtils.params(IRubyObject.class, arity, Block.class);
                    }
                } else {
                    if (arity == -1) {
                        params = CodegenUtils.params(IRubyObject[].class);
                    } else if (arity <= 3) {
                        params = CodegenUtils.params(IRubyObject.class, arity);
                    }
                }
            }
        }

        if (params != null) {
            try {
                if (nativeCall.isStatic()) {
                    lookup().findStatic(nativeCall.getNativeTarget(), nativeCall.getNativeName(), methodType(nativeCall.getNativeReturn(), params));
                } else {
                    lookup().findVirtual(nativeCall.getNativeTarget(), nativeCall.getNativeName(), methodType(nativeCall.getNativeReturn(), params));
                }

                return new DynamicMethod.NativeCall(nativeCall.getNativeTarget(), nativeCall.getNativeName(), nativeCall.getNativeReturn(), params, nativeCall.isStatic(), false);
            } catch (NoSuchMethodException | IllegalAccessException e) {
            }
        }

        return null;
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

    public String name() {
        return methodName;
    }

    public final CallType callType;

    public InvokeSite(MethodType type, String name, CallType callType, boolean literalClosure, int flags, String file, int line) {
        super(type);
        this.methodName = name;
        this.callType = callType;
        this.literalClosure = literalClosure;
        this.file = file;
        this.line = line;
        this.flags = flags;

        Signature startSig;

        if (callType == CallType.SUPER) {
            // super calls receive current class argument, so offsets and signature are different
            startSig = JRubyCallSite.STANDARD_SUPER_SIG;
            functional = false;
            argOffset = 4;
        } else if (callType == CallType.FUNCTIONAL || callType == CallType.VARIABLE) {
            startSig = JRubyCallSite.STANDARD_FSITE_SIG;
            functional = true;
            argOffset = 2;
        } else {
            startSig = JRubyCallSite.STANDARD_SITE_SIG;
            functional = false;
            argOffset = 3;
        }

        int arity;
        if (type.parameterType(type.parameterCount() - 1) == Block.class) {
            arity = type.parameterCount() - (argOffset + 1);

            if (arity == 1 && type.parameterType(argOffset) == IRubyObject[].class) {
                arity = -1;
                startSig = startSig.appendArg("args", IRubyObject[].class);
            } else {
                for (int i = 0; i < arity; i++) {
                    startSig = startSig.appendArg("arg" + i, IRubyObject.class);
                }
            }
            startSig = startSig.appendArg("block", Block.class);
            fullSignature = signature = startSig;
        } else {
            arity = type.parameterCount() - argOffset;

            if (arity == 1 && type.parameterType(argOffset) == IRubyObject[].class) {
                arity = -1;
                startSig = startSig.appendArg("args", IRubyObject[].class);
            } else {
                for (int i = 0; i < arity; i++) {
                    startSig = startSig.appendArg("arg" + i, IRubyObject.class);
                }
            }
            signature = startSig;
            fullSignature = startSig.appendArg("block", Block.class);
        }

        this.arity = arity;

        this.fallback = prepareBinder(true).invokeVirtualQuiet(LOOKUP, "invoke");
    }

    public static CallSite bootstrap(InvokeSite site, MethodHandles.Lookup lookup) {
        site.setInitialTarget(site.fallback);

        return site;
    }

    public IRubyObject invoke(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        SwitchPoint switchPoint = (SwitchPoint) selfClass.getInvalidator().getData();
        String methodName = this.methodName;
        CacheEntry entry = selfClass.searchWithCache(methodName);
        MethodHandle mh;
        boolean passSymbol = false;

        if (methodMissing(entry, caller)) {
            entry = methodMissingEntry(context, selfClass, methodName, entry);
            // only pass symbol below if we be calling a user-defined method_missing (default ones do it for us)
            passSymbol = !(entry.method instanceof RubyKernel.MethodMissingMethod ||
                    entry.method instanceof Helpers.MethodMissingWrapper);
            mh = buildGenericHandle(entry);
        } else {
            mh = getHandle(context, self, entry);
        }

        finishBinding(entry, mh, self, selfClass, switchPoint);

        return performIndirectCall(context, self, args, block, methodName, passSymbol, entry);
    }

    public IRubyObject invoke(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        SwitchPoint switchPoint = (SwitchPoint) selfClass.getInvalidator().getData();
        String methodName = this.methodName;
        CacheEntry entry = selfClass.searchWithCache(methodName);
        MethodHandle mh;
        boolean passSymbol = false;

        if (methodMissing(entry)) {
            entry = methodMissingEntry(context, selfClass, methodName, entry);
            // only pass symbol below if we be calling a user-defined method_missing (default ones do it for us)
            passSymbol = !(entry.method instanceof RubyKernel.MethodMissingMethod ||
                    entry.method instanceof Helpers.MethodMissingWrapper);
            mh = buildGenericHandle(entry);
        } else {
            mh = getHandle(context, self, entry);
        }

        finishBinding(entry, mh, self, selfClass, switchPoint);

        return performIndirectCall(context, self, args, block, methodName, passSymbol, entry);
    }

    private CacheEntry methodMissingEntry(ThreadContext context, RubyClass selfClass, String methodName, CacheEntry entry) {
        // Test thresholds so we don't do this forever (#4596)
        if (testThresholds(selfClass) == CacheAction.FAIL) {
            logFail();
            bindToFail();
        } else {
            logMethodMissing();
        }
        Visibility visibility = entry.method.getVisibility();
        return Helpers.createMethodMissingEntry(context, selfClass, callType, visibility, entry.token, methodName);
    }

    private void finishBinding(CacheEntry entry, MethodHandle mh, IRubyObject self, RubyClass selfClass, SwitchPoint switchPoint) {
        if (literalClosure) {
            mh = Binder.from(mh.type())
                    .tryFinally(getBlockEscape(signature))
                    .invoke(mh);
        }

        SmartHandle callInfoWrapper;
        SmartBinder baseBinder = SmartBinder.from(signature.changeReturn(void.class)).permute("context");
        if (flags == 0) {
            callInfoWrapper = baseBinder.invokeStaticQuiet(LOOKUP, ThreadContext.class, "clearCallInfo");
        } else {
            callInfoWrapper = baseBinder.append("flags", flags).invokeStaticQuiet(LOOKUP, IRRuntimeHelpers.class, "setCallInfo");
        }
        mh = foldArguments(mh, callInfoWrapper.handle());

        updateInvocationTarget(mh, self, selfClass, entry.method, switchPoint);
    }

    private IRubyObject performIndirectCall(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block, String methodName, boolean passSymbol, CacheEntry entry) {
        RubyModule sourceModule = entry.sourceModule;
        DynamicMethod method = entry.method;

        IRRuntimeHelpers.setCallInfo(context, flags);

        if (literalClosure) {
            try {
                if (passSymbol) {
                    return method.call(context, self, sourceModule, "method_missing", Helpers.arrayOf(asSymbol(context,methodName), args), block);
                } else {
                    return method.call(context, self, sourceModule, methodName, args, block);
                }
            } finally {
                block.escape();
            }
        }

        return method.call(context, self, sourceModule, methodName,
                passSymbol ? Helpers.arrayOf(asSymbol(context, methodName), args) : args, block);
    }

    private static final MethodHandle ESCAPE_BLOCK = Binder.from(void.class, Block.class).invokeVirtualQuiet(LOOKUP, "escape");
    private static final Map<Signature, MethodHandle> BLOCK_ESCAPES = Collections.synchronizedMap(new HashMap<Signature, MethodHandle>());

    private static MethodHandle getBlockEscape(Signature signature) {
        Signature voidSignature = signature.changeReturn(void.class);
        MethodHandle escape = BLOCK_ESCAPES.get(voidSignature);
        if (escape == null) {
            escape = SmartBinder.from(voidSignature)
                    .permute("block")
                    .invoke(ESCAPE_BLOCK)
                    .handle();
            BLOCK_ESCAPES.put(voidSignature, escape);
        }
        return escape;
    }

    /**
     * Failover version uses a monomorphic cache and DynamicMethod.call, as in non-indy.
     */
    public IRubyObject fail(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String name = methodName;
        CacheEntry entry = cache;

        IRRuntimeHelpers.setCallInfo(context, flags);

        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, entry.sourceModule, name, args, block);
        }

        entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, caller)) {
            return callMethodMissing(entry, callType, context, self, selfClass, name, args, block);
        }

        cache = entry;

        return entry.method.call(context, self, entry.sourceModule, name, args, block);
    }

    /**
     * Failover version uses a monomorphic cache and DynamicMethod.call, as in non-indy.
     */
    public IRubyObject failf(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String name = methodName;
        CacheEntry entry = cache;

        IRRuntimeHelpers.setCallInfo(context, flags);

        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, entry.sourceModule, name, args, block);
        }

        entry = selfClass.searchWithCache(name);

        if (methodMissing(entry)) {
            return callMethodMissing(entry, callType, context, self, selfClass, name, args, block);
        }

        cache = entry;

        return entry.method.call(context, self, entry.sourceModule, name, args, block);
    }

    /**
     * Failover version uses a monomorphic cache and DynamicMethod.call, as in non-indy.
     */
    public IRubyObject fail(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) throws Throwable {
        return fail(context, caller, self, IRubyObject.NULL_ARRAY, block);
    }

    /**
     * Failover version uses a monomorphic cache and DynamicMethod.call, as in non-indy.
     */
    public IRubyObject failf(ThreadContext context, IRubyObject self, Block block) throws Throwable {
        return failf(context, self, IRubyObject.NULL_ARRAY, block);
    }

    /**
     * Failover version uses a monomorphic cache and DynamicMethod.call, as in non-indy.
     */
    public IRubyObject fail(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String name = methodName;
        CacheEntry entry = cache;

        IRRuntimeHelpers.setCallInfo(context, flags);

        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, entry.sourceModule, name, arg0, block);
        }

        entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, caller)) {
            return callMethodMissing(entry, callType, context, self, selfClass, name, arg0, block);
        }

        cache = entry;

        return entry.method.call(context, self, entry.sourceModule, name, arg0, block);
    }

    /**
     * Failover version uses a monomorphic cache and DynamicMethod.call, as in non-indy.
     */
    public IRubyObject failf(ThreadContext context, IRubyObject self, IRubyObject arg0, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String name = methodName;
        CacheEntry entry = cache;

        IRRuntimeHelpers.setCallInfo(context, flags);

        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, entry.sourceModule, name, arg0, block);
        }

        entry = selfClass.searchWithCache(name);

        if (methodMissing(entry)) {
            return callMethodMissing(entry, callType, context, self, selfClass, name, arg0, block);
        }

        cache = entry;

        return entry.method.call(context, self, entry.sourceModule, name, arg0, block);
    }

    /**
     * Failover version uses a monomorphic cache and DynamicMethod.call, as in non-indy.
     */
    public IRubyObject fail(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String name = methodName;
        CacheEntry entry = cache;

        IRRuntimeHelpers.setCallInfo(context, flags);

        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, entry.sourceModule, name, arg0, arg1, block);
        }

        entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, caller)) {
            return callMethodMissing(entry, callType, context, self, selfClass, name, arg0, arg1, block);
        }

        cache = entry;

        return entry.method.call(context, self, entry.sourceModule, name, arg0, arg1, block);
    }

    /**
     * Failover version uses a monomorphic cache and DynamicMethod.call, as in non-indy.
     */
    public IRubyObject failf(ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String name = methodName;
        CacheEntry entry = cache;

        IRRuntimeHelpers.setCallInfo(context, flags);

        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, entry.sourceModule, name, arg0, arg1, block);
        }

        entry = selfClass.searchWithCache(name);

        if (methodMissing(entry)) {
            return callMethodMissing(entry, callType, context, self, selfClass, name, arg0, arg1, block);
        }

        cache = entry;

        return entry.method.call(context, self, entry.sourceModule, name, arg0, arg1, block);
    }

    /**
     * Failover version uses a monomorphic cache and DynamicMethod.call, as in non-indy.
     */
    public IRubyObject fail(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String name = methodName;
        CacheEntry entry = cache;

        IRRuntimeHelpers.setCallInfo(context, flags);

        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, entry.sourceModule, name, arg0, arg1, arg2, block);
        }

        entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, caller)) {
            return callMethodMissing(entry, callType, context, self, selfClass, name, arg0, arg1, arg2, block);
        }

        cache = entry;

        return entry.method.call(context, self, entry.sourceModule, name, arg0, arg1, arg2, block);
    }

    /**
     * Failover version uses a monomorphic cache and DynamicMethod.call, as in non-indy.
     */
    public IRubyObject failf(ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        String name = methodName;
        CacheEntry entry = cache;

        IRRuntimeHelpers.setCallInfo(context, flags);

        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, entry.sourceModule, name, arg0, arg1, arg2, block);
        }

        entry = selfClass.searchWithCache(name);

        if (methodMissing(entry)) {
            return callMethodMissing(entry, callType, context, self, selfClass, name, arg0, arg1, arg2, block);
        }

        cache = entry;

        return entry.method.call(context, self, entry.sourceModule, name, arg0, arg1, arg2, block);
    }

    /**
     * Prepare a binder for this call site's target, forcing varargs if specified
     *
     * @param varargs whether to only call an arg-boxed variable arity path
     * @return the prepared binder
     */
    public Binder prepareBinder(boolean varargs) {
        SmartBinder binder = SmartBinder.from(signature);

        if (varargs || arity > 3) {
            // we know we want to call varargs path always, so prepare args[] here
            if (arity == -1) {
                // do nothing, already have IRubyObject[] in args
            } else if (arity == 0) {
                binder = binder.insert(argOffset, "args", IRubyObject.NULL_ARRAY);
            } else {
                binder = binder
                        .collect("args", "arg[0-9]+", Helpers.constructObjectArrayHandle(arity));
            }
        }

        // add block if needed
        if (signature.lastArgType() != Block.class) {
            binder = binder.append("block", Block.NULL_BLOCK);
        }

        // bind to site
        binder = binder.insert(0, "site", this);

        return binder.binder();
    }

    protected MethodHandle getHandle(ThreadContext context, IRubyObject self, CacheEntry entry) throws Throwable {
        boolean blockGiven = signature.lastArgType() == Block.class;

        MethodHandle mh = buildNewInstanceHandle(entry, self);
        if (mh == null) mh = buildNotEqualHandle(context, entry, self);
        if (mh == null) mh = buildNativeHandle(entry, blockGiven);
        if (mh == null) mh = buildJavaFieldHandle(entry, self);
        if (mh == null) mh = buildIndyHandle(entry);
        if (mh == null) mh = buildJittedHandle(entry, blockGiven);
        if (mh == null) mh = buildAttrHandle(entry, self);
        if (mh == null) mh = buildAliasHandle(context, entry, self);
        if (mh == null) mh = buildStructHandle(entry);
        if (mh == null) mh = buildGenericHandle(entry);

        assert mh != null : "we should have a method handle of some sort by now";

        return mh;
    }

    MethodHandle buildJavaFieldHandle(CacheEntry entry, IRubyObject self) throws Throwable {
        DynamicMethod method = entry.method;

        if (method instanceof InstanceFieldGetter) {
            // only matching arity
            if (arity != 0 || signature.lastArgType() == Block.class) return null;

            Field field = ((InstanceFieldGetter) method).getField();

            // only IRubyObject subs for now
            if (!IRubyObject.class.isAssignableFrom(field.getType())) return null;

            MethodHandle fieldHandle = (MethodHandle) method.getHandle();

            if (fieldHandle != null) {
                return fieldHandle;
            }

            fieldHandle = LOOKUP.unreflectGetter(field);

            MethodHandle filter = self.getRuntime().getNullToNilHandle();

            MethodHandle receiverConverter = Binder
                    .from(field.getDeclaringClass(), IRubyObject.class)
                    .cast(Object.class, IRubyObject.class)
                    .invokeStaticQuiet(lookup(), JavaUtil.class, "objectFromJavaProxy");

            fieldHandle = Binder
                    .from(type())
                    .permute(2)
                    .filter(0, receiverConverter)
                    .filterReturn(filter)
                    .cast(fieldHandle.type())
                    .invoke(fieldHandle);

            method.setHandle(fieldHandle);

            return fieldHandle;
        } else if (method instanceof InstanceFieldSetter) {
            // only matching arity
            if (arity != 1 || signature.lastArgType() == Block.class) return null;

            Field field = ((InstanceFieldSetter) method).getField();

            // only IRubyObject subs for now
            if (!IRubyObject.class.isAssignableFrom(field.getType())) return null;

            MethodHandle fieldHandle = (MethodHandle) method.getHandle();

            if (fieldHandle != null) {
                return fieldHandle;
            }

            fieldHandle = LOOKUP.unreflectSetter(field);

            MethodHandle receiverConverter = Binder
                    .from(field.getDeclaringClass(), IRubyObject.class)
                    .cast(Object.class, IRubyObject.class)
                    .invokeStaticQuiet(lookup(), JavaUtil.class, "objectFromJavaProxy");

            fieldHandle = Binder
                    .from(type())
                    .permute(2, 3)
                    .filter(0, receiverConverter)
                    .filterReturn(constant(IRubyObject.class, self.getRuntime().getNil()))
                    .cast(fieldHandle.type())
                    .invoke(fieldHandle);

            method.setHandle(fieldHandle);

            return fieldHandle;
        }

        return null;
    }

    MethodHandle buildNewInstanceHandle(CacheEntry entry, IRubyObject self) {
        MethodHandle mh = null;
        DynamicMethod method = entry.method;

        if (method == self.getRuntime().getBaseNewMethod()) {
            RubyClass recvClass = (RubyClass) self;

            // Bind a second site as a dynamic invoker to guard against changes in new object's type
            MethodType type = type();
            if (!functional) type = type.dropParameterTypes(1, 2);
            CallSite initSite = SelfInvokeSite.bootstrap(LOOKUP, "callFunctional:initialize", type, literalClosure ? 1 : 0, flags, file, line);
            MethodHandle initHandle = initSite.dynamicInvoker();
            if (!functional) initHandle = MethodHandles.dropArguments(initHandle, 1, IRubyObject.class);

            MethodHandle allocFilter = Binder.from(IRubyObject.class, IRubyObject.class)
                    .cast(IRubyObject.class, RubyClass.class)
                    .insert(0, new Class[] {ObjectAllocator.class, Ruby.class}, recvClass.getAllocator(), self.getRuntime())
                    .invokeVirtualQuiet(LOOKUP, "allocate");

            mh = SmartBinder.from(LOOKUP, signature)
                    .filter("self", allocFilter)
                    .fold("dummy", initHandle)
                    .permute("self")
                    .identity()
                    .handle();

            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                LOG.info(name() + "\tbound as new instance creation " + Bootstrap.logMethod(method));
            }
        }

        return mh;
    }

    MethodHandle buildNotEqualHandle(ThreadContext context, CacheEntry entry, IRubyObject self) {
        MethodHandle mh = null;
        DynamicMethod method = entry.method;

        if (method.isBuiltin()) {
            CallSite equalSite;

            // FIXME: poor test for built-in != and !~
            MethodType type = type();
            if (!functional) type = type.dropParameterTypes(1, 2);
            String negatedCall;
            if (method.getImplementationClass() == basicObjectClass(context) && name().equals("!=")) {
                negatedCall = "callFunctional:==";
            } else if (method.getImplementationClass() == kernelModule(context) && name().equals("!~")) {
                negatedCall = "callFunctional:=~";
            } else {
                // unknown negatable call for us
                return null;
            }
            equalSite = SelfInvokeSite.bootstrap(LOOKUP, negatedCall, type, literalClosure ? 1 : 0, flags, file, line);

            if (equalSite != null) {
                // Bind a second site as a dynamic invoker to guard against changes in new object's type
                MethodHandle equalHandle = equalSite.dynamicInvoker();
                if (!functional) equalHandle = MethodHandles.dropArguments(equalHandle, 1, IRubyObject.class);

                MethodHandle filter = insertArguments(NEGATE, 1, context.nil, context.tru, context.fals);
                mh = MethodHandles.filterReturnValue(equalHandle, filter);

                if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                    LOG.info(name() + "\tbound as specialized " + name() + ":" + Bootstrap.logMethod(method));
                }
            }
        }

        return mh;
    }

    public static final MethodHandle NEGATE = Binder.from(IRubyObject.class, IRubyObject.class, RubyNil.class, RubyBoolean.True.class, RubyBoolean.False.class).invokeStaticQuiet(LOOKUP, InvokeSite.class, "negate");

    public static IRubyObject negate(IRubyObject object, RubyNil nil, RubyBoolean.True tru, RubyBoolean.False fals) {
        return object == nil || object == fals ? tru : fals;
    }

    MethodHandle buildAliasHandle(ThreadContext context, CacheEntry entry, IRubyObject self) throws Throwable {
        MethodHandle mh = null;
        DynamicMethod method = entry.method;

        if (method instanceof PartialDelegatingMethod delegate) {
            DynamicMethod innerMethod = delegate.getRealMethod();
            mh = getHandle(context, self, new CacheEntry(innerMethod, entry.sourceModule, entry.token));
        } else if (method instanceof AliasMethod alias) {
            DynamicMethod innerMethod = alias.getRealMethod();
            String name = alias.getName();

            // Use a second site to mimic invocation from AliasMethod
            MethodType type = type();
            if (!functional) type = type.dropParameterTypes(1, 2);
            InvokeSite innerSite = (InvokeSite) SelfInvokeSite.bootstrap(LOOKUP, "callFunctional:" + name, type, literalClosure ? 1 : 0, flags, file, line);
            mh = innerSite.getHandle(context, self, new CacheEntry(innerMethod, entry.sourceModule, entry.token));
            if (!functional) mh = MethodHandles.dropArguments(mh, 1, IRubyObject.class);

            alias.setHandle(mh);

            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                LOG.info(name() + "\tbound directly through alias to " + Bootstrap.logMethod(method));
            }
        }

        return mh;
    }

    MethodHandle buildStructHandle(CacheEntry entry) throws Throwable {
        MethodHandle mh = null;
        DynamicMethod method = entry.method;

        if (method instanceof RubyStruct.Accessor) {
            if (arity == 0) {
                RubyStruct.Accessor accessor = (RubyStruct.Accessor) method;
                int index = accessor.getIndex();

                mh = SmartBinder.from(signature)
                        .cast(signature.replaceArg("self", "self", RubyStruct.class))
                        .permute("self")
                        .append("index", index)
                        .invokeVirtualQuiet(LOOKUP, "get").handle();

                method.setHandle(mh);

                if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                    LOG.info(name() + "\tbound directly as Struct accessor " + Bootstrap.logMethod(method));
                }
            } else {
                if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                    LOG.info(name() + "\tcalled struct accessor with arity > 0 " + Bootstrap.logMethod(method));
                }
            }
        } else if (method instanceof RubyStruct.Mutator) {
            if (arity == 1) {
                RubyStruct.Mutator mutator = (RubyStruct.Mutator) method;
                int index = mutator.getIndex();

                mh = SmartBinder.from(signature)
                        .cast(signature.replaceArg("self", "self", RubyStruct.class))
                        .permute("self", "arg0")
                        .append("index", index)
                        .invokeVirtualQuiet(LOOKUP, "set").handle();

                method.setHandle(mh);

                if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                    LOG.info(name() + "\tbound directly as Struct mutator " + Bootstrap.logMethod(method));
                }
            } else {
                if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                    LOG.info(name() + "\tcalled struct mutator with arity > 1 " + Bootstrap.logMethod(method));
                }
            }
        }

        return mh;
    }

    /**
     * Update the given call site using the new target, wrapping with appropriate
     * bind and argument-juggling logic. Return a handle suitable for invoking
     * with the site's original method type.
     */
    protected MethodHandle updateInvocationTarget(MethodHandle target, IRubyObject self, RubyModule testClass, DynamicMethod method, SwitchPoint switchPoint) {
        MethodHandle fallback;
        MethodHandle gwt;

        CacheAction cacheAction = testThresholds(testClass);
        switch (cacheAction) {
            case FAIL:
                logFail();
                // bind to specific-arity fail method if available
                return bindToFail();
            case PIC:
                // stack it up into a PIC
                logPic(method);
                fallback = getTarget();
                break;
            case REBIND:
            case BIND:
                // wipe out site with this new type and method
                logBind(cacheAction);
                fallback = this.fallback;
                break;
            default:
                throw new RuntimeException("invalid cache action: " + cacheAction);
        }

        // Continue with logic for PIC, BIND, and REBIND

        SmartHandle test = testTarget(self, testClass);

        gwt = MethodHandles.guardWithTest(test.handle(), target, fallback);

        // wrap in switchpoint for mutation invalidation
        gwt = switchPoint.guardWithTest(gwt, fallback);

        setTarget(gwt);

        tracker.addType(testClass.id);

        return target;
    }

    protected SmartHandle testTarget(IRubyObject self, RubyModule testClass) {
        if (self instanceof RubySymbol ||
                self instanceof RubyFixnum ||
                self instanceof RubyFloat ||
                self instanceof RubyNil ||
                self instanceof RubyBoolean.True ||
                self instanceof RubyBoolean.False) {

            return SmartBinder
                    .from(signature.asFold(boolean.class))
                    .permute("self")
                    .insert(1, "selfJavaType", self.getClass())
                    .cast(boolean.class, Object.class, Class.class)
                    .invoke(TEST_CLASS);

        } else {

            return SmartBinder
                    .from(signature.changeReturn(boolean.class))
                    .permute("self")
                    .insert(0, "selfClass", RubyClass.class, testClass)
                    .invokeStaticQuiet(LOOKUP, InvokeSite.class, "testType");
        }
    }

    private void logMethodMissing() {
        if (LOG_BINDING) {
            LOG.debug(methodName + "\ttriggered site #" + siteID + " method_missing (" + file + ":" + line + ")");
        }
    }

    private void logBind(CacheAction action) {
        if (LOG_BINDING) {
            LOG.debug(methodName + "\ttriggered site #" + siteID + ' ' + action + " (" + file + ":" + line + ")");
        }
    }

    private void logPic(DynamicMethod method) {
        if (LOG_BINDING) {
            LOG.debug(methodName + "\tsite #" + siteID + " added to PIC " + logMethod(method));
        }
    }

    private void logFail() {
        if (LOG_BINDING) {
            if (tracker.clearCount() > Options.INVOKEDYNAMIC_MAXFAIL.load()) {
                LOG.info(methodName + "\tat site #" + siteID + " failed more than " + Options.INVOKEDYNAMIC_MAXFAIL.load() + " times; bailing out (" + file + ":" + line + ")");
            } else if (tracker.seenTypesCount() + 1 > Options.INVOKEDYNAMIC_MAXPOLY.load()) {
                LOG.info(methodName + "\tat site #" + siteID + " encountered more than " + Options.INVOKEDYNAMIC_MAXPOLY.load() + " types; bailing out (" + file + ":" + line + ")");
            }
        }
    }

    private MethodHandle bindToFail() {
        MethodHandle target;
        setTarget(target = prepareBinder(false).invokeVirtualQuiet(LOOKUP, functional ? "failf" : "fail"));
        return target;
    }

    enum CacheAction { FAIL, BIND, REBIND, PIC }

    CacheAction testThresholds(RubyModule testClass) {
        if (tracker.clearCount() > Options.INVOKEDYNAMIC_MAXFAIL.load() ||
                (!tracker.hasSeenType(testClass.id) && tracker.seenTypesCount() + 1 > Options.INVOKEDYNAMIC_MAXPOLY.load())) {
            // Thresholds exceeded
            return CacheAction.FAIL;
        } else {
            // if we've cached no types, and the site is bound and we haven't seen this new type...
            if (tracker.seenTypesCount() > 0 && getTarget() != null && !tracker.hasSeenType(testClass.id)) {
                // stack it up into a PIC
                return CacheAction.PIC;
            } else {
                // wipe out site with this new type and method
                tracker.clearTypes();
                return boundOnce ? CacheAction.REBIND : CacheAction.BIND;
            }
        }
    }

    public static RubyClass pollAndGetClass(ThreadContext context, IRubyObject self) {
        context.callThreadPoll();
        return RubyBasicObject.getMetaClass(self);
    }

    @Override
    public void setTarget(MethodHandle target) {
        super.setTarget(target);
        boundOnce = true;
    }

    public void setInitialTarget(MethodHandle target) {
        super.setTarget(target);
    }

    public boolean methodMissing(CacheEntry entry, IRubyObject caller) {
        DynamicMethod method = entry.method;

        return method.isUndefined() || (!methodName.equals("method_missing") && !method.isCallableFrom(caller, callType));
    }

    public boolean methodMissing(CacheEntry entry) {
        DynamicMethod method = entry.method;

        return method.isUndefined();
    }

    /**
     * Variable arity method_missing invocation. Arity zero also passes through here.
     */
    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self,
                                         RubyClass selfClass, String name, IRubyObject[] args, Block block) {
        return Helpers.callMethodMissing(context, self, selfClass, entry.method.getVisibility(), name, callType, args, block);
    }

    /**
     * Arity one method_missing invocation
     */
    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self,
                                         RubyClass selfClass, String name, IRubyObject arg0, Block block) {
        return Helpers.callMethodMissing(context, self, selfClass, entry.method.getVisibility(), name, callType, arg0, block);
    }


    /**
     * Arity two method_missing invocation
     */
    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self,
                                         RubyClass selfClass, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        return Helpers.callMethodMissing(context, self, selfClass, entry.method.getVisibility(), name, callType, arg0, arg1, block);
    }


    /**
     * Arity three method_missing invocation
     */
    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self,
                                         RubyClass selfClass, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return Helpers.callMethodMissing(context, self, selfClass, entry.method.getVisibility(), name, callType, arg0, arg1, arg2, block);
    }

    private static String logMethod(DynamicMethod method) {
        return "[#" + method.getSerialNumber() + ' ' + method.getImplementationClass() + ']';
    }

    @JIT
    public static boolean testClass(Object object, Class clazz) {
        return object.getClass() == clazz;
    }

    public String toString() {
        return getClass().getName() + "[name=" + name() + ",arity=" + arity + ",type=" + type() + ",file=" + file + ",line=" + line + ']';
    }

    private static final MethodHandle TEST_CLASS = Binder
            .from(boolean.class, Object.class, Class.class)
            .invokeStaticQuiet(LOOKUP, InvokeSite.class, "testClass");
}
