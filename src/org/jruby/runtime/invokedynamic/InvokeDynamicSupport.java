package org.jruby.runtime.invokedynamic;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.SwitchPoint;
import java.util.Comparator;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyLocalJumpError;
import org.jruby.RubyModule;
import org.jruby.ast.executable.AbstractScript;
import org.jruby.exceptions.JumpException;
import org.jruby.internal.runtime.methods.AttrReaderMethod;
import org.jruby.internal.runtime.methods.AttrWriterMethod;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.CompiledMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.util.SafePropertyAccessor;
import static org.jruby.util.CodegenUtils.*;
import org.objectweb.asm.Opcodes;

@SuppressWarnings("deprecation")
public class InvokeDynamicSupport {
    private static final int MAX_FAIL_COUNT = SafePropertyAccessor.getInt("jruby.invokedynamic.maxfail", 2);
    private static final boolean LOG_INDY_BINDINGS = SafePropertyAccessor.getBoolean("jruby.invokedynamic.log.binding");
    private static final boolean LOG_INDY_CONSTANTS = SafePropertyAccessor.getBoolean("jruby.invokedynamic.log.constants");
    
    public static class JRubyCallSite extends MutableCallSite {
        private final CallType callType;
        private final MethodType type;
        public CacheEntry entry = CacheEntry.NULL_CACHE;
        public volatile int failCount = 0;

        public JRubyCallSite(MethodType type, CallType callType) {
            super(type);
            this.type = type;
            this.callType = callType;
        }

        public CallType callType() {
            return callType;
        }
    }
    
    public static class RubyConstantCallSite extends MutableCallSite {
        private final String name;

        public RubyConstantCallSite(MethodType type, String name) {
            super(type);
            this.name = name;
        }
        
        public String name() {
            return name;
        }
    }

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
        JRubyCallSite site;

        if (name == "call") {
            site = new JRubyCallSite(type, CallType.NORMAL);
        } else {
            site = new JRubyCallSite(type, CallType.FUNCTIONAL);
        }
        
        MethodType fallbackType = type.insertParameterTypes(0, JRubyCallSite.class);
        MethodHandle myFallback = MethodHandles.insertArguments(
                lookup.findStatic(InvokeDynamicSupport.class, "fallback",
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

    public static IRubyObject constantFallback(RubyConstantCallSite site, 
            ThreadContext context) {
        IRubyObject value = context.getConstant(site.name());
        
        if (value != null) {
            if (LOG_INDY_CONSTANTS) System.out.println("binding constant " + site.name() + " with invokedynamic");
            
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
    
    public final static MethodType BOOTSTRAP_SIGNATURE      = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class);
    public final static String     BOOTSTRAP_SIGNATURE_DESC = sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class);
    public final static String     GETCONSTANT_SIGNATURE_DESC = sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class);
    
    public static org.objectweb.asm.MethodHandle bootstrapHandle() {
        return new org.objectweb.asm.MethodHandle(Opcodes.MH_INVOKESTATIC, p(InvokeDynamicSupport.class), "bootstrap", BOOTSTRAP_SIGNATURE_DESC);
    }
    
    public static org.objectweb.asm.MethodHandle getConstantHandle() {
        return new org.objectweb.asm.MethodHandle(Opcodes.MH_INVOKESTATIC, p(InvokeDynamicSupport.class), "getConstantBootstrap", GETCONSTANT_SIGNATURE_DESC);
    }

    private static MethodHandle createGWT(String name, MethodHandle test, MethodHandle target, MethodHandle fallback, CacheEntry entry, JRubyCallSite site) {
        return createGWT(name, test, target, fallback, entry, site, true);
    }

    private static MethodHandle createGWT(String name, MethodHandle test, MethodHandle target, MethodHandle fallback, CacheEntry entry, JRubyCallSite site, boolean curryFallback) {
        // only direct invoke if no block passed (for now) and if no frame/scope are required
        if (site.type().parameterArray()[site.type().parameterCount() - 1] != Block.class &&
                entry.method.getCallConfig() == CallConfiguration.FrameNoneScopeNone) {
            if (entry.method instanceof AttrReaderMethod || entry.method instanceof AttrWriterMethod) {
                return createAttrGWT(entry, test, fallback, site, curryFallback);
            }
            
            MethodHandle nativeTarget = handleForMethod(entry.method);
            if (nativeTarget != null) {
                DynamicMethod.NativeCall nativeCall = entry.method.getNativeCall();
                
                Comparator comp = new Comparator<Object>() {
                    public int compare(Object t, Object t1) {
                        return t == t1 ? 0 : 1;
                    }
                };
                
                if (getArgCount(nativeCall.getNativeSignature(), nativeCall.isStatic()) == 4 &&
                        site.type().parameterArray()[site.type().parameterCount() - 1] != IRubyObject[].class) {
                    // mismatch call site to target IRubyObject[] args; call back on DynamicMethod.call for now
                } else {
                    if (entry.method instanceof CompiledMethod) {
                        return createRubyGWT(entry.token, nativeTarget, nativeCall, test, fallback, site, curryFallback);
                    } else {
                        if (site.type().parameterArray()[site.type().parameterCount() - 1] != Block.class
                                && nativeTarget.type().parameterArray()[nativeTarget.type().parameterCount() - 1] == Block.class) {
                            nativeTarget = MethodHandles.insertArguments(nativeTarget, nativeTarget.type().parameterCount() - 1, Block.NULL_BLOCK);
                        }
                        return createNativeGWT(entry.token, nativeTarget, nativeCall, test, fallback, site, curryFallback);
                    }
                }
            }
        }
        
        // no direct native path, use DynamicMethod.call target provided
        if (LOG_INDY_BINDINGS) System.out.println("binding " + name + " as DynamicMethod.call");
        
        MethodHandle myTest = MethodHandles.insertArguments(test, 0, entry.token);
        MethodHandle myTarget = MethodHandles.insertArguments(target, 0, entry);
        MethodHandle myFallback = curryFallback ? MethodHandles.insertArguments(fallback, 0, site) : fallback;
        MethodHandle guardWithTest = MethodHandles.guardWithTest(myTest, myTarget, myFallback);
        
        return MethodHandles.convertArguments(guardWithTest, site.type());
    }
    
    private static MethodHandle handleForMethod(DynamicMethod method) {
        MethodHandle nativeTarget = null;
        
        if (method.getHandle() != null) {
            nativeTarget = (MethodHandle)method.getHandle();
        } else {
            if (method.getNativeCall() != null) {
                DynamicMethod.NativeCall nativeCall = method.getNativeCall();
                Class[] nativeSig = nativeCall.getNativeSignature();
                // use invokedynamic for ruby to ruby calls
                if (nativeSig.length > 0 && AbstractScript.class.isAssignableFrom(nativeSig[0])) {
                    if (method instanceof CompiledMethod) {
                        return createRubyHandle(method);
                    }
                }

                // use invokedynamic for ruby to native calls
                nativeTarget = createNativeHandle(method);
            }
        }
        
        return nativeTarget;
    }

    private static MethodHandle createFail(MethodHandle fail, JRubyCallSite site) {
        MethodHandle myFail = MethodHandles.insertArguments(fail, 0, site);
        return myFail;
    }
    
    private static final MethodType STANDARD_NATIVE_TYPE_0 = MethodType.methodType(
            IRubyObject.class, // return value
            ThreadContext.class, //context
            IRubyObject.class, // caller
            IRubyObject.class, // self
            String.class, // method name
            Block.class // block
            );
    private static final MethodType STANDARD_NATIVE_TYPE_1 = MethodType.methodType(
            IRubyObject.class, // return value
            ThreadContext.class, //context
            IRubyObject.class, // caller
            IRubyObject.class, // self
            String.class, // method name
            IRubyObject.class, // arg0
            Block.class // block
            );
    private static final MethodType STANDARD_NATIVE_TYPE_2 = MethodType.methodType(
            IRubyObject.class, // return value
            ThreadContext.class, //context
            IRubyObject.class, // caller
            IRubyObject.class, // self
            String.class, // method name
            IRubyObject.class, // arg0
            IRubyObject.class, // arg1
            Block.class // block
            );
    private static final MethodType STANDARD_NATIVE_TYPE_3 = MethodType.methodType(
            IRubyObject.class, // return value
            ThreadContext.class, //context
            IRubyObject.class, // caller
            IRubyObject.class, // self
            String.class, // method name
            IRubyObject.class, // arg0
            IRubyObject.class, // arg1
            IRubyObject.class, // arg2
            Block.class // block
            );
    private static final MethodType STANDARD_NATIVE_TYPE_N = MethodType.methodType(
            IRubyObject.class, // return value
            ThreadContext.class, //context
            IRubyObject.class, // caller
            IRubyObject.class, // self
            String.class, // method name
            IRubyObject[].class, // args
            Block.class // block
            );
    private static final MethodType[] STANDARD_NATIVE_TYPES = {
        STANDARD_NATIVE_TYPE_0,
        STANDARD_NATIVE_TYPE_1,
        STANDARD_NATIVE_TYPE_2,
        STANDARD_NATIVE_TYPE_3,
        STANDARD_NATIVE_TYPE_N,
    };
    
    private static final MethodType TARGET_SELF_TC = MethodType.methodType(
            IRubyObject.class, // return value
            IRubyObject.class, // self
            ThreadContext.class //context
            );
    private static final MethodType TARGET_SELF_TC_1ARG = MethodType.methodType(
            IRubyObject.class, // return value
            IRubyObject.class, // self
            ThreadContext.class, //context
            IRubyObject.class
            );
    private static final MethodType TARGET_SELF_TC_2ARG = MethodType.methodType(
            IRubyObject.class, // return value
            IRubyObject.class, // self
            ThreadContext.class, //context
            IRubyObject.class,
            IRubyObject.class
            );
    private static final MethodType TARGET_SELF_TC_3ARG = MethodType.methodType(
            IRubyObject.class, // return value
            IRubyObject.class, // self
            ThreadContext.class, //context
            IRubyObject.class,
            IRubyObject.class,
            IRubyObject.class
            );
    private static final MethodType TARGET_SELF_TC_NARG = MethodType.methodType(
            IRubyObject.class, // return value
            IRubyObject.class, // self
            ThreadContext.class, //context
            IRubyObject[].class
            );
    private static final MethodType[] TARGET_SELF_TC_ARGS = {
        TARGET_SELF_TC,
        TARGET_SELF_TC_1ARG,
        TARGET_SELF_TC_2ARG,
        TARGET_SELF_TC_3ARG,
        TARGET_SELF_TC_NARG,
    };
    
    private static final MethodType TARGET_SELF_TC_BLOCK = MethodType.methodType(
            IRubyObject.class, // return value
            IRubyObject.class, // self
            ThreadContext.class, //context
            Block.class
            );
    private static final MethodType TARGET_SELF_TC_1ARG_BLOCK = MethodType.methodType(
            IRubyObject.class, // return value
            IRubyObject.class, // self
            ThreadContext.class, //context
            IRubyObject.class,
            Block.class
            );
    private static final MethodType TARGET_SELF_TC_2ARG_BLOCK = MethodType.methodType(
            IRubyObject.class, // return value
            IRubyObject.class, // self
            ThreadContext.class, //context
            IRubyObject.class,
            IRubyObject.class,
            Block.class
            );
    private static final MethodType TARGET_SELF_TC_3ARG_BLOCK = MethodType.methodType(
            IRubyObject.class, // return value
            IRubyObject.class, // self
            ThreadContext.class, //context
            IRubyObject.class,
            IRubyObject.class,
            IRubyObject.class,
            Block.class
            );
    private static final MethodType TARGET_SELF_TC_NARG_BLOCK = MethodType.methodType(
            IRubyObject.class, // return value
            IRubyObject.class, // self
            ThreadContext.class, //context
            IRubyObject[].class,
            Block.class
            );
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
    private static final MethodType TARGET_TC_SELF_1ARG = MethodType.methodType(
            IRubyObject.class, // return value
            ThreadContext.class, //context
            IRubyObject.class, // self
            IRubyObject.class
            );
    private static final MethodType TARGET_TC_SELF_2ARG = MethodType.methodType(
            IRubyObject.class, // return value
            ThreadContext.class, //context
            IRubyObject.class, // self
            IRubyObject.class,
            IRubyObject.class
            );
    private static final MethodType TARGET_TC_SELF_3ARG = MethodType.methodType(
            IRubyObject.class, // return value
            ThreadContext.class, //context
            IRubyObject.class, // self
            IRubyObject.class,
            IRubyObject.class,
            IRubyObject.class
            );
    private static final MethodType TARGET_TC_SELF_NARG = MethodType.methodType(
            IRubyObject.class, // return value
            ThreadContext.class, //context
            IRubyObject.class, // self
            IRubyObject[].class
            );
    private static final MethodType[] TARGET_TC_SELF_ARGS = {
        TARGET_TC_SELF,
        TARGET_TC_SELF_1ARG,
        TARGET_TC_SELF_2ARG,
        TARGET_TC_SELF_3ARG,
        TARGET_TC_SELF_NARG,
    };
    
    private static final MethodType TARGET_TC_SELF_BLOCK = MethodType.methodType(
            IRubyObject.class, // return value
            ThreadContext.class, //context
            IRubyObject.class, // self
            Block.class
            );
    private static final MethodType TARGET_TC_SELF_1ARG_BLOCK = MethodType.methodType(
            IRubyObject.class, // return value
            ThreadContext.class, //context
            IRubyObject.class, // self
            IRubyObject.class,
            Block.class
            );
    private static final MethodType TARGET_TC_SELF_2ARG_BLOCK = MethodType.methodType(
            IRubyObject.class, // return value
            ThreadContext.class, //context
            IRubyObject.class, // self
            IRubyObject.class,
            IRubyObject.class,
            Block.class
            );
    private static final MethodType TARGET_TC_SELF_3ARG_BLOCK = MethodType.methodType(
            IRubyObject.class, // return value
            ThreadContext.class, //context
            IRubyObject.class, // self
            IRubyObject.class,
            IRubyObject.class,
            IRubyObject.class,
            Block.class
            );
    private static final MethodType TARGET_TC_SELF_NARG_BLOCK = MethodType.methodType(
            IRubyObject.class, // return value
            ThreadContext.class, //context
            IRubyObject.class, // self
            IRubyObject[].class,
            Block.class
            );
    private static final MethodType[] TARGET_TC_SELF_ARGS_BLOCK = {
        TARGET_TC_SELF_BLOCK,
        TARGET_TC_SELF_1ARG_BLOCK,
        TARGET_TC_SELF_2ARG_BLOCK,
        TARGET_TC_SELF_3ARG_BLOCK,
        TARGET_TC_SELF_NARG_BLOCK,
    };
    
    
    private static final MethodType TARGET_SELF = MethodType.methodType(
            IRubyObject.class, // return value
            IRubyObject.class // self
            );
    private static final MethodType TARGET_SELF_1ARG = MethodType.methodType(
            IRubyObject.class, // return value
            IRubyObject.class, // self
            IRubyObject.class
            );
    private static final MethodType TARGET_SELF_2ARG = MethodType.methodType(
            IRubyObject.class, // return value
            IRubyObject.class, // self
            IRubyObject.class,
            IRubyObject.class
            );
    private static final MethodType TARGET_SELF_3ARG = MethodType.methodType(
            IRubyObject.class, // return value
            IRubyObject.class, // self
            IRubyObject.class,
            IRubyObject.class,
            IRubyObject.class
            );
    private static final MethodType TARGET_SELF_NARG = MethodType.methodType(
            IRubyObject.class, // return value
            IRubyObject.class, // self
            IRubyObject[].class
            );
    private static final MethodType[] TARGET_SELF_ARGS = {
        TARGET_SELF,
        TARGET_SELF_1ARG,
        TARGET_SELF_2ARG,
        TARGET_SELF_3ARG,
        TARGET_SELF_NARG,
    };
    
    private static final MethodType TARGET_SELF_BLOCK = MethodType.methodType(
            IRubyObject.class, // return value
            IRubyObject.class, // self
            Block.class
            );
    private static final MethodType TARGET_SELF_1ARG_BLOCK = MethodType.methodType(
            IRubyObject.class, // return value
            IRubyObject.class, // self
            IRubyObject.class,
            Block.class
            );
    private static final MethodType TARGET_SELF_2ARG_BLOCK = MethodType.methodType(
            IRubyObject.class, // return value
            IRubyObject.class, // self
            IRubyObject.class,
            IRubyObject.class,
            Block.class
            );
    private static final MethodType TARGET_SELF_3ARG_BLOCK = MethodType.methodType(
            IRubyObject.class, // return value
            IRubyObject.class, // self
            IRubyObject.class,
            IRubyObject.class,
            IRubyObject.class,
            Block.class
            );
    private static final MethodType TARGET_SELF_NARG_BLOCK = MethodType.methodType(
            IRubyObject.class, // return value
            IRubyObject.class, // self
            IRubyObject[].class,
            Block.class
            );
    private static final MethodType[] TARGET_SELF_ARGS_BLOCK = {
        TARGET_SELF_BLOCK,
        TARGET_SELF_1ARG_BLOCK,
        TARGET_SELF_2ARG_BLOCK,
        TARGET_SELF_3ARG_BLOCK,
        TARGET_SELF_NARG_BLOCK,
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
                MethodType inboundType = STANDARD_NATIVE_TYPES[argCount];
                
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

                    nativeTarget = MethodHandles.convertArguments(nativeTarget, convert);
                    nativeTarget = MethodHandles.permuteArguments(nativeTarget, inboundType, permute);
                    method.setHandle(nativeTarget);
                    return nativeTarget;
                }
            }
        }
        
        // can't build native handle for it
        return null;
    }

    private static MethodHandle createRubyHandle(DynamicMethod method) {
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
            method.setHandle(nativeTarget);
            return nativeTarget;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static MethodHandle creatAttrHandle(DynamicMethod method) {
        MethodHandle target = (MethodHandle)method.getHandle();
        if (target != null) return target;
        
        try {
            if (method instanceof AttrReaderMethod) {
                AttrReaderMethod reader = (AttrReaderMethod)method;
                target = MethodHandles.lookup().findVirtual(
                        AttrReaderMethod.class,
                        "call",
                        MethodType.methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class));
                target = MethodHandles.convertArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class));
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
                target = MethodHandles.convertArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class));
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

    private static MethodHandle createAttrGWT(
            CacheEntry entry,
            MethodHandle test,
            MethodHandle fallback,
            JRubyCallSite site,
            boolean curryFallback) {
        
//        if (LOG_INDY_BINDINGS) System.out.println("binding attr target: " + site.name());
        
        MethodHandle target = creatAttrHandle(entry.method);
        
        MethodHandle myTest = MethodHandles.insertArguments(test, 0, entry.token);
        MethodHandle myTarget = target;
        MethodHandle myFallback = curryFallback ? MethodHandles.insertArguments(fallback, 0, site) : fallback;
        MethodHandle guardWithTest = MethodHandles.guardWithTest(myTest, myTarget, myFallback);
        
        return MethodHandles.convertArguments(guardWithTest, site.type());
    }

    private static MethodHandle createNativeGWT(
            int token,
            MethodHandle nativeTarget,
            DynamicMethod.NativeCall nativeCall,
            MethodHandle test,
            MethodHandle fallback,
            JRubyCallSite site,
            boolean curryFallback) {
        
        if (LOG_INDY_BINDINGS) System.out.println("binding native target: " + nativeCall);
        
        MethodHandle myFallback = curryFallback ? MethodHandles.insertArguments(fallback, 0, site) : fallback;
        MethodHandle myTest = MethodHandles.insertArguments(test, 0, token);
        MethodHandle gwt = MethodHandles.guardWithTest(myTest, nativeTarget, myFallback);
        return MethodHandles.convertArguments(gwt, site.type());
    }

    private static MethodHandle createRubyGWT(
            int token,
            MethodHandle nativeTarget,
            DynamicMethod.NativeCall nativeCall,
            MethodHandle test,
            MethodHandle fallback,
            JRubyCallSite site,
            boolean curryFallback) {
        
        if (LOG_INDY_BINDINGS) System.out.println("binding ruby target: " + nativeCall);
        
        try {
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        
        // wire up GWT + test and fallback
        MethodHandle myFallback = curryFallback ? MethodHandles.insertArguments(fallback, 0, site) : fallback;
        MethodHandle myTest = MethodHandles.insertArguments(test, 0, token);
        MethodHandle gwt = MethodHandles.guardWithTest(myTest, nativeTarget, myFallback);
        
        // final arg conversion to match call site
        return MethodHandles.convertArguments(gwt, site.type());
    }

    private static int getArgCount(Class[] args, boolean isStatic) {
        int length = args.length;
        boolean hasContext = false;
        if (isStatic) {
            if (args.length > 1 && args[0] == ThreadContext.class) {
                length--;
                hasContext = true;
            }

            if (args.length > 1 && args[args.length - 1] == Block.class) {
                length--;
            }

            // all static bound methods receive self arg
            length--;
            
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
        return args.length - 4;
    }

    public static boolean test(int token, IRubyObject self) {
        return token == ((RubyBasicObject)self).getMetaClass().getCacheToken();
    }

    public static IRubyObject fallback(JRubyCallSite site, 
            ThreadContext context,
            IRubyObject caller,
            IRubyObject self,
            String name) {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            return callMethodMissing(entry, site.callType(), context, self, name);
        }
        
        if (++site.failCount > MAX_FAIL_COUNT) {
            if (LOG_INDY_BINDINGS) System.out.println("failing over to inline cache for '" + name + "' call");
            site.setTarget(createFail(FAIL_0, site));
        } else {
//            if (entry.method instanceof AttrReaderMethod) {
//                MethodHandle reader = findVirtual(IRubyObject.class, "getVariable", MethodType.methodType(Object.class, int.class));
//                reader = MethodHandles.insertArguments(reader, 1, selfClass.getVariableAccessorForRead(name).getIndex());
//                reader = MethodHandles.permuteArguments(
//                        reader,
//                        MethodType.methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class),
//                        new int[]{2});
//                if (LOG_INDY_BINDINGS) System.out.println("binding " + name + " as attribute");
//
//                MethodHandle myTest = MethodHandles.insertArguments(TEST_0, 0, entry.token);
//                MethodHandle myFallback = MethodHandles.insertArguments(FALLBACK_0, 0, site);
//                MethodHandle guardWithTest = MethodHandles.guardWithTest(myTest, reader, myFallback);
//
//                site.setTarget(MethodHandles.convertArguments(guardWithTest, site.type()));
//            } else {
                if (site.getTarget() != null) {
                    site.setTarget(createGWT(name, TEST_0, TARGET_0, site.getTarget(), entry, site, false));
                } else {
                    site.setTarget(createGWT(name, TEST_0, TARGET_0, FALLBACK_0, entry, site));
                }
//            }
        }

        return entry.method.call(context, self, selfClass, name);
    }

    public static IRubyObject fallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0) {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            return callMethodMissing(entry, site.callType(), context, self, name, arg0);
        }
        if (++site.failCount > MAX_FAIL_COUNT) {
            site.setTarget(createFail(FAIL_1, site));
        } else {
            if (site.getTarget() != null) {
                site.setTarget(createGWT(name, TEST_1, TARGET_1, site.getTarget(), entry, site, false));
            } else {
                site.setTarget(createGWT(name, TEST_1, TARGET_1, FALLBACK_1, entry, site));
            }
        }

        return entry.method.call(context, self, selfClass, name, arg0);
    }

    public static IRubyObject fallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1);
        }
        if (++site.failCount > MAX_FAIL_COUNT) {
            site.setTarget(createFail(FAIL_2, site));
        } else {
            if (site.getTarget() != null) {
                site.setTarget(createGWT(name, TEST_2, TARGET_2, site.getTarget(), entry, site, false));
            } else {
                site.setTarget(createGWT(name, TEST_2, TARGET_2, FALLBACK_2, entry, site));
            }
        }

        return entry.method.call(context, self, selfClass, name, arg0, arg1);
    }

    public static IRubyObject fallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, arg2);
        }
        if (++site.failCount > MAX_FAIL_COUNT) {
            site.setTarget(createFail(FAIL_3, site));
        } else {
            if (site.getTarget() != null) {
                site.setTarget(createGWT(name, TEST_3, TARGET_3, site.getTarget(), entry, site, false));
            } else {
                site.setTarget(createGWT(name, TEST_3, TARGET_3, FALLBACK_3, entry, site));
            }
        }

        return entry.method.call(context, self, selfClass, name, arg0, arg1, arg2);
    }

    public static IRubyObject fallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args) {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            return callMethodMissing(entry, site.callType(), context, self, name, args);
        }
        if (++site.failCount > MAX_FAIL_COUNT) {
            site.setTarget(createFail(FAIL_N, site));
        } else {
            if (site.getTarget() != null) {
                site.setTarget(createGWT(name, TEST_N, TARGET_N, site.getTarget(), entry, site, false));
            } else {
                site.setTarget(createGWT(name, TEST_N, TARGET_N, FALLBACK_N, entry, site));
            }
        }

        return entry.method.call(context, self, selfClass, name, args);
    }

    public static IRubyObject fallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, Block block) {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);

        try {
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, block);
            }
            if (++site.failCount > MAX_FAIL_COUNT) {
                site.setTarget(createFail(FAIL_0_B, site));
            } else {
                if (site.getTarget() != null) {
                    site.setTarget(createGWT(name, TEST_0_B, TARGET_0_B, site.getTarget(), entry, site, false));
                } else {
                    site.setTarget(createGWT(name, TEST_0_B, TARGET_0_B, FALLBACK_0_B, entry, site));
                }
            }
            return entry.method.call(context, self, selfClass, name, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            return retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject fallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, Block block) {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);

        try {
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, block);
            }
            if (++site.failCount > MAX_FAIL_COUNT) {
                site.setTarget(createFail(FAIL_1_B, site));
            } else {
                if (site.getTarget() != null) {
                    site.setTarget(createGWT(name, TEST_1_B, TARGET_1_B, site.getTarget(), entry, site, false));
                } else {
                    site.setTarget(createGWT(name, TEST_1_B, TARGET_1_B, FALLBACK_1_B, entry, site));
                }
            }
            return entry.method.call(context, self, selfClass, name, arg0, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            return retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject fallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);

        try {
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, block);
            }
            if (++site.failCount > MAX_FAIL_COUNT) {
                site.setTarget(createFail(FAIL_2_B, site));
            } else {
                if (site.getTarget() != null) {
                    site.setTarget(createGWT(name, TEST_2_B, TARGET_2_B, site.getTarget(), entry, site, false));
                } else {
                    site.setTarget(createGWT(name, TEST_2_B, TARGET_2_B, FALLBACK_2_B, entry, site));
                }
            }
            return entry.method.call(context, self, selfClass, name, arg0, arg1, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            return retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject fallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);

        try {
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, arg2, block);
            }
            if (++site.failCount > MAX_FAIL_COUNT) {
                site.setTarget(createFail(FAIL_3_B, site));
            } else {
                if (site.getTarget() != null) {
                    site.setTarget(createGWT(name, TEST_3_B, TARGET_3_B, site.getTarget(), entry, site, false));
                } else {
                    site.setTarget(createGWT(name, TEST_3_B, TARGET_3_B, FALLBACK_3_B, entry, site));
                }
            }
            return entry.method.call(context, self, selfClass, name, arg0, arg1, arg2, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            return retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject fallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args, Block block) {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);

        try {
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, args, block);
            }
            if (++site.failCount >= MAX_FAIL_COUNT) {
                site.setTarget(createFail(FAIL_N_B, site));
            } else {
                if (site.getTarget() != null) {
                    site.setTarget(createGWT(name, TEST_N_B, TARGET_N_B, site.getTarget(), entry, site, false));
                } else {
                    site.setTarget(createGWT(name, TEST_N_B, TARGET_N_B, FALLBACK_N_B, entry, site));
                }
            }
            return entry.method.call(context, self, selfClass, name, args, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            return retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject fail(JRubyCallSite site, 
            ThreadContext context,
            IRubyObject caller,
            IRubyObject self,
            String name) {
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

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0) {
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

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) {
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

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
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

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args) {
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

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, Block block) {
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

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, Block block) {
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

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
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

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
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

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args, Block block) {
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
    
//    private static MethodHandle wrapCallFramedScoped(MethodHandle target, MethodHandle pre, MethodHandle post) {
//        return MethodHandles.foldArguments(target, );
//    }

    private static final MethodHandle PGC_0 = dropNameAndArgs(PGC, 4, 0, false);
    private static final MethodHandle PGC2_0 = dropNameAndArgs(PGC2, 3, 0, false);
    private static final MethodHandle GETMETHOD_0 = dropNameAndArgs(GETMETHOD, 5, 0, false);
    private static final MethodHandle TEST_0 = dropNameAndArgs(TEST, 4, 0, false);
    private static final MethodHandle TARGET_0;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                MethodType.methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class));
        target = MethodHandles.convertArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class));
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
    private static final MethodHandle FALLBACK_0 = findStatic(InvokeDynamicSupport.class, "fallback",
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
        target = MethodHandles.convertArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class));
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
    private static final MethodHandle FALLBACK_1 = findStatic(InvokeDynamicSupport.class, "fallback",
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
        target = MethodHandles.convertArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class, IRubyObject.class));
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
    private static final MethodHandle FALLBACK_2 = findStatic(InvokeDynamicSupport.class, "fallback",
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
        target = MethodHandles.convertArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
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
    private static final MethodHandle FALLBACK_3 = findStatic(InvokeDynamicSupport.class, "fallback",
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
        target = MethodHandles.convertArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject[].class));
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
    private static final MethodHandle FALLBACK_N = findStatic(InvokeDynamicSupport.class, "fallback",
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
        target = MethodHandles.convertArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, Block.class));
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
    private static final MethodHandle FALLBACK_0_B = findStatic(InvokeDynamicSupport.class, "fallback",
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
        target = MethodHandles.convertArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class, Block.class));
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
    private static final MethodHandle FALLBACK_1_B = findStatic(InvokeDynamicSupport.class, "fallback",
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
        target = MethodHandles.convertArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));
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
    private static final MethodHandle FALLBACK_2_B = findStatic(InvokeDynamicSupport.class, "fallback",
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
        target = MethodHandles.convertArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
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
    private static final MethodHandle FALLBACK_3_B = findStatic(InvokeDynamicSupport.class, "fallback",
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
        target = MethodHandles.convertArguments(target, MethodType.methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject[].class, Block.class));
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
    private static final MethodHandle FALLBACK_N_B = findStatic(InvokeDynamicSupport.class, "fallback",
                    MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class, Block.class));
    private static final MethodHandle FAIL_N_B = findStatic(InvokeDynamicSupport.class, "fail",
                    MethodType.methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class, Block.class));
    
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
}


//    public static IRubyObject target(DynamicMethod method, RubyClass selfClass, ThreadContext context, IRubyObject self, String name, IRubyObject[] args, Block block) {
//        try {
//            return method.call(context, self, selfClass, name, args, block);
//        } catch (JumpException.BreakJump bj) {
//            return handleBreakJump(context, bj);
//        } catch (JumpException.RetryJump rj) {
//            throw retryJumpError(context);
//        } finally {
//            block.escape();
//        }
//    }
