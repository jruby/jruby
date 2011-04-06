package org.jruby.runtime.invokedynamic;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyLocalJumpError;
import org.jruby.RubyModule;
import org.jruby.ast.executable.AbstractScript;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.exceptions.JumpException;
import org.jruby.internal.runtime.methods.CompiledMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.Framing;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.util.SafePropertyAccessor;
import static org.jruby.util.CodegenUtils.*;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

@SuppressWarnings("deprecation")
public class InvokeDynamicSupport {
    public static class JRubyCallSite extends MutableCallSite {
        private final CallType callType;
        private final MethodType type;

        public JRubyCallSite(MethodType type, CallType callType) {
            super(type);
            this.type = type;
            this.callType = callType;
        }

        public CallType callType() {
            return callType;
        }

        public MethodType type() {
            return type;
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
    
    public final static MethodType BOOTSTRAP_SIGNATURE      = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class);
    public final static String     BOOTSTRAP_SIGNATURE_DESC = sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class);
    
    public static org.objectweb.asm.MethodHandle bootstrapHandle() {
        return new org.objectweb.asm.MethodHandle(Opcodes.MH_INVOKESTATIC, p(InvokeDynamicSupport.class), "bootstrap", BOOTSTRAP_SIGNATURE_DESC);
    }

    private static MethodHandle createGWT(MethodHandle test, MethodHandle target, MethodHandle fallback, CacheEntry entry, JRubyCallSite site) {
        if (entry.method.getNativeCall() != null) {
            DynamicMethod.NativeCall nativeCall = entry.method.getNativeCall();
            Class[] nativeSig = nativeCall.getNativeSignature();
            // if enabled, use invokedynamic for ruby to ruby calls
            if (SafePropertyAccessor.getBoolean("jruby.compile.invokedynamic.rubyDirect", true) &&
                    nativeSig.length > 0 &&
                    AbstractScript.class.isAssignableFrom(nativeSig[0]) &&
                    entry.method instanceof CompiledMethod) {
                if (entry.method.getCallConfig().framing() == Framing.None) {
                    return createRubyGWT(nativeCall, test, fallback, entry, site);
                }
            } else {
                // if enabled, use invokedynamic for ruby to native calls
                if (SafePropertyAccessor.getBoolean("jruby.compile.invokedynamic.nativeDirect", true) &&
                        getArgCount(nativeSig, nativeCall.isStatic()) != -1) {
                    if (nativeSig.length > 0 && nativeSig[0] == ThreadContext.class && nativeSig[nativeSig.length - 1] != Block.class) {
                        return createNativeGWT(nativeCall, test, fallback, entry, site);
                    }
                }
            }
        }
        MethodHandle myTest = MethodHandles.insertArguments(test, 0, entry.token);
        MethodHandle myTarget = MethodHandles.insertArguments(target, 0, entry);
        MethodHandle myFallback = MethodHandles.insertArguments(fallback, 0, site);
        MethodHandle guardWithTest = MethodHandles.guardWithTest(myTest, myTarget, myFallback);
        
        return MethodHandles.convertArguments(guardWithTest, site.type());
    }

    private static MethodHandle createNativeGWT(DynamicMethod.NativeCall nativeCall, MethodHandle test, MethodHandle fallback, CacheEntry entry, JRubyCallSite site) {
        try {
            boolean isStatic = nativeCall.isStatic();
            MethodHandle nativeTarget;
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
            int argCount = getArgCount(nativeCall.getNativeSignature(), nativeCall.isStatic());
            switch (argCount) {
                case 0:
                    nativeTarget = MethodHandles.permuteArguments(nativeTarget, site.type(), isStatic ? new int[] {0, 2} : new int[] {2, 0});
                    break;
                case -1:
                case 1:
                    nativeTarget = MethodHandles.permuteArguments(nativeTarget, site.type(), isStatic ? new int[] {0, 2, 4} : new int[] {2, 0, 4});
                    break;
                case 2:
                    nativeTarget = MethodHandles.permuteArguments(nativeTarget, site.type(), isStatic ? new int[] {0, 2, 4, 5} : new int[] {2, 0, 4, 5});
                    break;
                case 3:
                    nativeTarget = MethodHandles.permuteArguments(nativeTarget, site.type(), isStatic ? new int[] {0, 2, 4, 5, 6} : new int[] {2, 0, 4, 5, 6});
                    break;
                default:
                    throw new RuntimeException("unknown arg count: " + argCount);
            }
            MethodHandle myFallback = MethodHandles.insertArguments(fallback, 0, site);
            MethodHandle myTest = MethodHandles.insertArguments(test, 0, entry.token);
            MethodHandle gwt = MethodHandles.guardWithTest(myTest, nativeTarget, myFallback);
            return MethodHandles.convertArguments(gwt, site.type());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static MethodHandle createRubyGWT(DynamicMethod.NativeCall nativeCall, MethodHandle test, MethodHandle fallback, CacheEntry entry, JRubyCallSite site) {
        try {
            MethodHandle nativeTarget;
            nativeTarget = MethodHandles.lookup().findStatic(
                    nativeCall.getNativeTarget(),
                    nativeCall.getNativeName(),
                    MethodType.methodType(nativeCall.getNativeReturn(),
                    nativeCall.getNativeSignature()));
            CompiledMethod cm = (CompiledMethod)entry.method;
            nativeTarget = MethodHandles.insertArguments(nativeTarget, 0, cm.getScriptObject());
            nativeTarget = MethodHandles.insertArguments(nativeTarget, nativeTarget.type().parameterCount() - 1, Block.NULL_BLOCK);
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
            MethodHandle myFallback = MethodHandles.insertArguments(fallback, 0, site);
            MethodHandle myTest = MethodHandles.insertArguments(test, 0, entry.token);
            MethodHandle gwt = MethodHandles.guardWithTest(myTest, nativeTarget, myFallback);
            return MethodHandles.convertArguments(gwt, site.type());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        site.setTarget(createGWT(TEST_0, TARGET_0, FALLBACK_0, entry, site));

        return entry.method.call(context, self, selfClass, name);
    }

    public static IRubyObject fallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0) {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            return callMethodMissing(entry, site.callType(), context, self, name, arg0);
        }
        site.setTarget(createGWT(TEST_1, TARGET_1, FALLBACK_1, entry, site));

        return entry.method.call(context, self, selfClass, name, arg0);
    }

    public static IRubyObject fallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1);
        }
        site.setTarget(createGWT(TEST_2, TARGET_2, FALLBACK_2, entry, site));

        return entry.method.call(context, self, selfClass, name, arg0, arg1);
    }

    public static IRubyObject fallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, arg2);
        }
        site.setTarget(createGWT(TEST_3, TARGET_3, FALLBACK_3, entry, site));

        return entry.method.call(context, self, selfClass, name, arg0, arg1, arg2);
    }

    public static IRubyObject fallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args) {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            return callMethodMissing(entry, site.callType(), context, self, name, args);
        }
        site.setTarget(createGWT(TEST_N, TARGET_N, FALLBACK_N, entry, site));

        return entry.method.call(context, self, selfClass, name, args);
    }

    public static IRubyObject fallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, Block block) {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);

        try {
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, block);
            }
            site.setTarget(createGWT(TEST_0_B, TARGET_0_B, FALLBACK_0_B, entry, site));
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
            site.setTarget(createGWT(TEST_1_B, TARGET_1_B, FALLBACK_1_B, entry, site));
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
            site.setTarget(createGWT(TEST_2_B, TARGET_2_B, FALLBACK_2_B, entry, site));
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
            site.setTarget(createGWT(TEST_3_B, TARGET_3_B, FALLBACK_3_B, entry, site));
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
            site.setTarget(createGWT(TEST_N_B, TARGET_N_B, FALLBACK_N_B, entry, site));
            return entry.method.call(context, self, selfClass, name, args, block);
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

    private static final MethodHandle PGC_0 = dropNameAndArgs(PGC, 4, 0, false);
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

    private static final MethodHandle PGC_1 = dropNameAndArgs(PGC, 4, 1, false);
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

        MethodHandle breakJump = dropNameAndArgs(BREAKJUMP, 5, 3, true);
        MethodHandle retryJump = dropNameAndArgs(RETRYJUMP, 5, 3, true);
        target = MethodHandles.catchException(target, JumpException.BreakJump.class, breakJump);
        target = MethodHandles.catchException(target, JumpException.RetryJump.class, retryJump);
        
        TARGET_3_B = target;
    }
    private static final MethodHandle FALLBACK_3_B = findStatic(InvokeDynamicSupport.class, "fallback",
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