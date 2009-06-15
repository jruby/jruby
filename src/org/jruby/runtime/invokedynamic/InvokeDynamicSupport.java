package org.jruby.runtime.invokedynamic;

import java.dyn.CallSite;
import java.dyn.Linkage;
import java.dyn.MethodHandle;
import java.dyn.MethodHandles;
import java.dyn.MethodType;
import org.jruby.RubyClass;
import org.jruby.RubyLocalJumpError;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import static org.jruby.util.CodegenUtils.*;
import org.objectweb.asm.MethodVisitor;

public class InvokeDynamicSupport {
    public static class JRubyCallSite extends CallSite {
        private final CallType callType;

        public JRubyCallSite(Class caller, String name, MethodType type, CallType callType) {
            super(caller, name, type);
            this.callType = callType;
        }

        public CallType callType() {
            return callType;
        }
    }

    public static CallSite bootstrap(Class caller, String name, MethodType type) {
        JRubyCallSite site;

        if (name == "call") {
            site = new JRubyCallSite(caller, name, type, CallType.NORMAL);
        } else {
            site = new JRubyCallSite(caller, name, type, CallType.FUNCTIONAL);
        }
        
        MethodType fallbackType = type.insertParameterType(0, JRubyCallSite.class);
        MethodHandle myFallback = MethodHandles.insertArguments(
                MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "fallback",
                fallbackType),
                0,
                site);
        site.setTarget(myFallback);
        return site;
    }
    
    public static void registerBootstrap(Class cls) {
        Linkage.registerBootstrapMethod(cls, BOOTSTRAP);
    }
    
    public static void installBytecode(MethodVisitor method, String classname) {
        SkinnyMethodAdapter mv = new SkinnyMethodAdapter(method);
        mv.ldc(c(classname));
        mv.invokestatic(p(Class.class), "forName", sig(Class.class, params(String.class)));
        mv.invokestatic(p(InvokeDynamicSupport.class), "registerBootstrap", sig(void.class, Class.class));
    }

    private static MethodHandle createGWT(MethodHandle test, MethodHandle target, MethodHandle fallback, CacheEntry entry, CallSite site) {
        MethodHandle myTest = MethodHandles.insertArguments(test, 0, entry);
        MethodHandle myTarget = MethodHandles.insertArguments(target, 0, entry);
        MethodHandle myFallback = MethodHandles.insertArguments(fallback, 0, site);
        MethodHandle guardWithTest = MethodHandles.guardWithTest(myTest, myTarget, myFallback);
        
        return MethodHandles.convertArguments(guardWithTest, site.type());
    }

    public static boolean test(CacheEntry entry, IRubyObject self) {
        return entry.typeOk(self.getMetaClass());
    }

    public static IRubyObject target(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name) {
        RubyClass selfClass = pollAndGetClass(context, self);
        return entry.method.call(context, self, selfClass, name);
    }

    public static IRubyObject fallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name) {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            return callMethodMissing(entry, site.callType(), context, self, name);
        }
        site.setTarget(createGWT(TEST_0, TARGET_0, FALLBACK_0, entry, site));

        return entry.method.call(context, self, selfClass, name);
    }

    public static IRubyObject target(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0) {
        RubyClass selfClass = pollAndGetClass(context, self);
        return entry.method.call(context, self, selfClass, name, arg0);
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

    public static IRubyObject target(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) {
        RubyClass selfClass = pollAndGetClass(context, self);
        return entry.method.call(context, self, selfClass, name, arg0, arg1);
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

    public static IRubyObject target(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        RubyClass selfClass = pollAndGetClass(context, self);
        return entry.method.call(context, self, selfClass, name, arg0, arg1, arg2);
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

    public static boolean test(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args) {
        return entry.typeOk(self.getMetaClass());
    }

    public static IRubyObject target(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args) {
        RubyClass selfClass = pollAndGetClass(context, self);
        return entry.method.call(context, self, selfClass, name, args);
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

    public static IRubyObject target(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, Block block) {
        try {
            RubyClass selfClass = pollAndGetClass(context, self);
            return entry.method.call(context, self, selfClass, name, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        } finally {
            block.escape();
        }
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
            throw retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject target(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, Block block) {
        try {
            RubyClass selfClass = pollAndGetClass(context, self);
            return entry.method.call(context, self, selfClass, name, arg0, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
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
            throw retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject target(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        try {
            RubyClass selfClass = pollAndGetClass(context, self);
            return entry.method.call(context, self, selfClass, name, arg0, arg1, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
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
            throw retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject target(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        try {
            RubyClass selfClass = pollAndGetClass(context, self);
            return entry.method.call(context, self, selfClass, name, arg0, arg1, arg2, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
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
            throw retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject target(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args, Block block) {
        try {
            RubyClass selfClass = pollAndGetClass(context, self);
           return entry.method.call(context, self, selfClass, name, args, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
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
            throw retryJumpError(context);
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

    private static RubyClass pollAndGetClass(ThreadContext context, IRubyObject self) {
        context.callThreadPoll();
        RubyClass selfType = self.getMetaClass();
        return selfType;
    }

    private static IRubyObject handleBreakJump(ThreadContext context, JumpException.BreakJump bj) throws JumpException.BreakJump {
        if (context.getFrameJumpTarget() == bj.getTarget()) {
            return (IRubyObject) bj.getValue();
        }
        throw bj;
    }

    private static RaiseException retryJumpError(ThreadContext context) {
        return context.getRuntime().newLocalJumpError(RubyLocalJumpError.Reason.RETRY, context.getRuntime().getNil(), "retry outside of rescue not supported");
    }

    private static final MethodType BOOTSTRAP_TYPE = MethodType.make(CallSite.class, Class.class, String.class, MethodType.class);
    private static final MethodHandle BOOTSTRAP = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "bootstrap", BOOTSTRAP_TYPE);

    private static final MethodHandle TEST = MethodHandles.dropArguments(
            MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "test",
                MethodType.make(boolean.class, CacheEntry.class, IRubyObject.class)),
            1,
            ThreadContext.class, IRubyObject.class);

    private static final MethodHandle TEST_0 = MethodHandles.dropArguments(TEST, 4, String.class);
    private static final MethodHandle TARGET_0 = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "target",
            MethodType.make(IRubyObject.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class));
    private static final MethodHandle FALLBACK_0 = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "fallback",
            MethodType.make(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class));

    private static final MethodHandle TEST_1 = MethodHandles.dropArguments(TEST, 4, String.class, IRubyObject.class);
    private static final MethodHandle TARGET_1 = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "target",
            MethodType.make(IRubyObject.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class));
    private static final MethodHandle FALLBACK_1 = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "fallback",
            MethodType.make(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class));

    private static final MethodHandle TEST_2 = MethodHandles.dropArguments(TEST, 4, String.class, IRubyObject.class, IRubyObject.class);
    private static final MethodHandle TARGET_2 = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "target",
            MethodType.make(IRubyObject.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class));
    private static final MethodHandle FALLBACK_2 = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "fallback",
            MethodType.make(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class));

    private static final MethodHandle TEST_3 = MethodHandles.dropArguments(TEST, 4, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class);
    private static final MethodHandle TARGET_3 = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "target",
            MethodType.make(IRubyObject.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
    private static final MethodHandle FALLBACK_3 = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "fallback",
            MethodType.make(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));

    private static final MethodHandle TEST_N = MethodHandles.dropArguments(TEST, 4, String.class, IRubyObject[].class);
    private static final MethodHandle TARGET_N = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "target",
            MethodType.make(IRubyObject.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class));
    private static final MethodHandle FALLBACK_N = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "fallback",
            MethodType.make(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class));

    private static final MethodHandle TEST_0_B = MethodHandles.dropArguments(TEST, 4, String.class, Block.class);
    private static final MethodHandle TARGET_0_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "target",
            MethodType.make(IRubyObject.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, Block.class));
    private static final MethodHandle FALLBACK_0_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "fallback",
            MethodType.make(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, Block.class));

    private static final MethodHandle TEST_1_B = MethodHandles.dropArguments(TEST, 4, String.class, IRubyObject.class, Block.class);
    private static final MethodHandle TARGET_1_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "target",
            MethodType.make(IRubyObject.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, Block.class));
    private static final MethodHandle FALLBACK_1_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "fallback",
            MethodType.make(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, Block.class));

    private static final MethodHandle TEST_2_B = MethodHandles.dropArguments(TEST, 4, String.class, IRubyObject.class, IRubyObject.class, Block.class);
    private static final MethodHandle TARGET_2_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "target",
            MethodType.make(IRubyObject.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));
    private static final MethodHandle FALLBACK_2_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "fallback",
            MethodType.make(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));

    private static final MethodHandle TEST_3_B = MethodHandles.dropArguments(TEST, 4, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class);
    private static final MethodHandle TARGET_3_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "target",
            MethodType.make(IRubyObject.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
    private static final MethodHandle FALLBACK_3_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "fallback",
            MethodType.make(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));

    private static final MethodHandle TEST_N_B = MethodHandles.dropArguments(TEST, 4, String.class, IRubyObject[].class, Block.class);
    private static final MethodHandle TARGET_N_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "target",
            MethodType.make(IRubyObject.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class, Block.class));
    private static final MethodHandle FALLBACK_N_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "fallback",
            MethodType.make(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class, Block.class));
}
