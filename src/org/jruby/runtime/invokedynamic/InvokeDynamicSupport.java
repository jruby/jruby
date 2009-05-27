package org.jruby.runtime.invokedynamic;

import java.dyn.CallSite;
import java.dyn.Linkage;
import java.dyn.MethodHandle;
import java.dyn.MethodHandles;
import java.dyn.MethodType;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import static org.jruby.util.CodegenUtils.*;
import org.objectweb.asm.MethodVisitor;

public class InvokeDynamicSupport {
    public static CallSite bootstrap(Class caller, String name, MethodType type) {
        CallSite site = new CallSite(caller, name, type);
        MethodType fallbackType = type.insertParameterType(0, CallSite.class);
        MethodHandle myFallback = MethodHandles.insertArgument(
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
        MethodHandle myTest = MethodHandles.insertArgument(test, 0, entry);
        MethodHandle myTarget = MethodHandles.insertArgument(target, 0, entry);
        MethodHandle myFallback = MethodHandles.insertArgument(fallback, 0, site);
        MethodHandle guardWithTest = MethodHandles.guardWithTest(myTest, myTarget, myFallback);
        return MethodHandles.convertArguments(guardWithTest, site.type());
    }

    public static boolean test(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name) {
        return entry.typeOk(self.getMetaClass());
    }

    public static IRubyObject target(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name) {
        return entry.method.call(context, self, self.getMetaClass(), name);
    }

    public static IRubyObject fallback(CallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name) {
        CacheEntry newEntry = self.getMetaClass().searchWithCache(name);
        site.setTarget(createGWT(TEST_0, TARGET_0, FALLBACK_0, newEntry, site));

        return newEntry.method.call(context, self, self.getMetaClass(), name);
    }

    public static boolean test(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0) {
        return entry.typeOk(self.getMetaClass());
    }

    public static IRubyObject target(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0) {
        return entry.method.call(context, self, self.getMetaClass(), name, arg0);
    }

    public static IRubyObject fallback(CallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0) {
        CacheEntry newEntry = self.getMetaClass().searchWithCache(name);
        site.setTarget(createGWT(TEST_1, TARGET_1, FALLBACK_1, newEntry, site));

        return newEntry.method.call(context, self, self.getMetaClass(), name, arg0);
    }

    public static boolean test(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) {
        return entry.typeOk(self.getMetaClass());
    }

    public static IRubyObject target(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) {
        return entry.method.call(context, self, self.getMetaClass(), name, arg0, arg1);
    }

    public static IRubyObject fallback(CallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) {
        CacheEntry newEntry = self.getMetaClass().searchWithCache(name);
        site.setTarget(createGWT(TEST_2, TARGET_2, FALLBACK_2, newEntry, site));

        return newEntry.method.call(context, self, self.getMetaClass(), name, arg0, arg1);
    }

    public static boolean test(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return entry.typeOk(self.getMetaClass());
    }

    public static IRubyObject target(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return entry.method.call(context, self, self.getMetaClass(), name, arg0, arg1, arg2);
    }

    public static IRubyObject fallback(CallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        CacheEntry newEntry = self.getMetaClass().searchWithCache(name);
        site.setTarget(createGWT(TEST_3, TARGET_3, FALLBACK_3, newEntry, site));

        return newEntry.method.call(context, self, self.getMetaClass(), name, arg0, arg1, arg2);
    }

    public static boolean test(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args) {
        return entry.typeOk(self.getMetaClass());
    }

    public static IRubyObject target(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args) {
        return entry.method.call(context, self, self.getMetaClass(), name, args);
    }

    public static IRubyObject fallback(CallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args) {
        CacheEntry newEntry = self.getMetaClass().searchWithCache(name);
        site.setTarget(createGWT(TEST_N, TARGET_N, FALLBACK_N, newEntry, site));

        return newEntry.method.call(context, self, self.getMetaClass(), name, args);
    }

    public static boolean test(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, Block block) {
        return entry.typeOk(self.getMetaClass());
    }

    public static IRubyObject target(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, Block block) {
        return entry.method.call(context, self, self.getMetaClass(), name, block);
    }

    public static IRubyObject fallback(CallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, Block block) {
        CacheEntry newEntry = self.getMetaClass().searchWithCache(name);
        site.setTarget(createGWT(TEST_0_B, TARGET_0_B, FALLBACK_0_B, newEntry, site));

        return newEntry.method.call(context, self, self.getMetaClass(), name, block);
    }

    public static boolean test(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, Block block) {
        return entry.typeOk(self.getMetaClass());
    }

    public static IRubyObject target(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, Block block) {
        return entry.method.call(context, self, self.getMetaClass(), name, arg0, block);
    }

    public static IRubyObject fallback(CallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, Block block) {
        CacheEntry newEntry = self.getMetaClass().searchWithCache(name);
        site.setTarget(createGWT(TEST_1_B, TARGET_1_B, FALLBACK_1_B, newEntry, site));

        return newEntry.method.call(context, self, self.getMetaClass(), name, arg0, block);
    }

    public static boolean test(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        return entry.typeOk(self.getMetaClass());
    }

    public static IRubyObject target(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        return entry.method.call(context, self, self.getMetaClass(), name, arg0, arg1, block);
    }

    public static IRubyObject fallback(CallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        CacheEntry newEntry = self.getMetaClass().searchWithCache(name);
        site.setTarget(createGWT(TEST_2_B, TARGET_2_B, FALLBACK_2_B, newEntry, site));

        return newEntry.method.call(context, self, self.getMetaClass(), name, arg0, arg1, block);
    }

    public static boolean test(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return entry.typeOk(self.getMetaClass());
    }

    public static IRubyObject target(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return entry.method.call(context, self, self.getMetaClass(), name, arg0, arg1, arg2, block);
    }

    public static IRubyObject fallback(CallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        CacheEntry newEntry = self.getMetaClass().searchWithCache(name);
        site.setTarget(createGWT(TEST_3_B, TARGET_3_B, FALLBACK_3_B, newEntry, site));

        return newEntry.method.call(context, self, self.getMetaClass(), name, arg0, arg1, arg2, block);
    }

    public static boolean test(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args, Block block) {
        return entry.typeOk(self.getMetaClass());
    }

    public static IRubyObject target(CacheEntry entry, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args, Block block) {
        return entry.method.call(context, self, self.getMetaClass(), name, args, block);
    }

    public static IRubyObject fallback(CallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args, Block block) {
        CacheEntry newEntry = self.getMetaClass().searchWithCache(name);
        site.setTarget(createGWT(TEST_N_B, TARGET_N_B, FALLBACK_N_B, newEntry, site));

        return newEntry.method.call(context, self, self.getMetaClass(), name, args, block);
    }

    private static final MethodType BOOTSTRAP_TYPE = MethodType.make(CallSite.class, Class.class, String.class, MethodType.class);
    private static final MethodHandle BOOTSTRAP = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "bootstrap", BOOTSTRAP_TYPE);

    private static final MethodHandle TEST_0 = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "test",
            MethodType.make(boolean.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class));
    private static final MethodHandle TARGET_0 = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "target",
            MethodType.make(IRubyObject.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class));
    private static final MethodHandle FALLBACK_0 = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "fallback",
            MethodType.make(IRubyObject.class, CallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class));

    private static final MethodHandle TEST_1 = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "test",
            MethodType.make(boolean.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class));
    private static final MethodHandle TARGET_1 = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "target",
            MethodType.make(IRubyObject.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class));
    private static final MethodHandle FALLBACK_1 = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "fallback",
            MethodType.make(IRubyObject.class, CallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class));

    private static final MethodHandle TEST_2 = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "test",
            MethodType.make(boolean.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class));
    private static final MethodHandle TARGET_2 = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "target",
            MethodType.make(IRubyObject.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class));
    private static final MethodHandle FALLBACK_2 = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "fallback",
            MethodType.make(IRubyObject.class, CallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class));

    private static final MethodHandle TEST_3 = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "test",
            MethodType.make(boolean.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
    private static final MethodHandle TARGET_3 = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "target",
            MethodType.make(IRubyObject.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
    private static final MethodHandle FALLBACK_3 = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "fallback",
            MethodType.make(IRubyObject.class, CallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));

    private static final MethodHandle TEST_N = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "test",
            MethodType.make(boolean.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class));
    private static final MethodHandle TARGET_N = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "target",
            MethodType.make(IRubyObject.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class));
    private static final MethodHandle FALLBACK_N = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "fallback",
            MethodType.make(IRubyObject.class, CallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class));

    private static final MethodHandle TEST_0_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "test",
            MethodType.make(boolean.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, Block.class));
    private static final MethodHandle TARGET_0_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "target",
            MethodType.make(IRubyObject.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, Block.class));
    private static final MethodHandle FALLBACK_0_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "fallback",
            MethodType.make(IRubyObject.class, CallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, Block.class));

    private static final MethodHandle TEST_1_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "test",
            MethodType.make(boolean.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, Block.class));
    private static final MethodHandle TARGET_1_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "target",
            MethodType.make(IRubyObject.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, Block.class));
    private static final MethodHandle FALLBACK_1_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "fallback",
            MethodType.make(IRubyObject.class, CallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, Block.class));

    private static final MethodHandle TEST_2_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "test",
            MethodType.make(boolean.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));
    private static final MethodHandle TARGET_2_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "target",
            MethodType.make(IRubyObject.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));
    private static final MethodHandle FALLBACK_2_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "fallback",
            MethodType.make(IRubyObject.class, CallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));

    private static final MethodHandle TEST_3_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "test",
            MethodType.make(boolean.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
    private static final MethodHandle TARGET_3_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "target",
            MethodType.make(IRubyObject.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
    private static final MethodHandle FALLBACK_3_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "fallback",
            MethodType.make(IRubyObject.class, CallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));

    private static final MethodHandle TEST_N_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "test",
            MethodType.make(boolean.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class, Block.class));
    private static final MethodHandle TARGET_N_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "target",
            MethodType.make(IRubyObject.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class, Block.class));
    private static final MethodHandle FALLBACK_N_B = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "fallback",
            MethodType.make(IRubyObject.class, CallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class, Block.class));
}
