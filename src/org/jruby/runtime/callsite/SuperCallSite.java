package org.jruby.runtime.callsite;

import org.jruby.RubyClass;
import org.jruby.RubyLocalJumpError;
import org.jruby.RubyModule;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class SuperCallSite extends CallSite {
    protected volatile CacheEntry cache = CacheEntry.NULL_CACHE;
    // This can go away if we determine that the following is officially a bug:
    // http://redmine.ruby-lang.org/issues/show/1151
    protected volatile String lastName;
    
    public SuperCallSite() {
        super("super", CallType.SUPER);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject... args) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        CacheEntry myCache = cache;
        if (selfType != null && myCache.typeOk(selfType)) {
            return myCache.method.call(context, self, selfType, name, args);
        }
        return cacheAndCall(caller, selfType, args, context, self, name);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        CacheEntry myCache = cache;
        if (selfType != null && myCache.typeOk(selfType)) {
            return myCache.method.call(context, self, selfType, name, args, block);
        }
        return cacheAndCall(caller, selfType, block, args, context, self, name);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        try {
            return callBlock(context, caller, self, args, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        }
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        try {
            return callBlock(context, caller, self, args, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        CacheEntry myCache = cache;
        if (selfType != null && myCache.typeOk(selfType)) {
            return myCache.method.call(context, self, selfType, name);
        }
        return cacheAndCall(caller, selfType, context, self, name);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        CacheEntry myCache = cache;
        if (selfType != null && myCache.typeOk(selfType)) {
            return myCache.method.call(context, self, selfType, name, block);
        }
        return cacheAndCall(caller, selfType, block, context, self, name);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        try {
            return callBlock(context, caller, self, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        }
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        try {
            return callBlock(context, caller, self, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        CacheEntry myCache = cache;
        if (selfType != null && myCache.typeOk(selfType)) {
            return myCache.method.call(context, self, selfType, name, arg1);
        }
        return cacheAndCall(caller, selfType, context, self, name, arg1);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        CacheEntry myCache = cache;
        if (selfType != null && myCache.typeOk(selfType)) {
            return myCache.method.call(context, self, selfType, name, arg1, block);
        }
        return cacheAndCall(caller, selfType, block, context, self, name, arg1);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block) {
        try {
            return callBlock(context, caller, self, arg1, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        }
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block) {
        try {
            return callBlock(context, caller, self, arg1, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        CacheEntry myCache = cache;
        if (selfType != null && myCache.typeOk(selfType)) {
            return myCache.method.call(context, self, selfType, name, arg1, arg2);
        }
        return cacheAndCall(caller, selfType, context, self, name, arg1, arg2);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        CacheEntry myCache = cache;
        if (selfType != null && myCache.typeOk(selfType)) {
            return myCache.method.call(context, self, selfType, name, arg1, arg2, block);
        }
        return cacheAndCall(caller, selfType, block, context, self, name, arg1, arg2);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        try {
            return callBlock(context, caller, self, arg1, arg2, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        }
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        try {
            return callBlock(context, caller, self, arg1, arg2, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        CacheEntry myCache = cache;
        if (selfType != null && myCache.typeOk(selfType)) {
            return myCache.method.call(context, self, selfType, name, arg1, arg2, arg3);
        }
        return cacheAndCall(caller, selfType, context, self, name, arg1, arg2, arg3);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        CacheEntry myCache = cache;
        if (selfType != null && myCache.typeOk(selfType)) {
            return myCache.method.call(context, self, selfType, name, arg1, arg2, arg3, block);
        }
        return cacheAndCall(caller, selfType, block, context, self, name, arg1, arg2, arg3);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        try {
            return callBlock(context, caller, self, arg1, arg2, arg3, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        }
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        try {
            return callBlock(context, caller, self, arg1, arg2, arg3, block);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        } finally {
            block.escape();
        }
    }
    
    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, IRubyObject[] args, ThreadContext context, IRubyObject self, String name) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, name, method, args, block);
        }
        cache = entry;
        return method.call(context, self, selfType, name, args, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, IRubyObject[] args, ThreadContext context, IRubyObject self, String name) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, name, method, args);
        }
        cache = entry;
        return method.call(context, self, selfType, name, args);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, String name) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, name, method);
        }
        cache = entry;
        return method.call(context, self, selfType, name);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, String name) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, name, method, block);
        }
        cache = entry;
        return method.call(context, self, selfType, name, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, String name, IRubyObject arg) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, name, method, arg);
        }
        cache = entry;
        return method.call(context, self, selfType, name, arg);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, String name, IRubyObject arg) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, name, method, arg, block);
        }
        cache = entry;
        return method.call(context, self, selfType, name, arg, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, name, method, arg1, arg2);
        }
        cache = entry;
        return method.call(context, self, selfType, name, arg1, arg2);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, name, method, arg1, arg2, block);
        }
        cache = entry;
        return method.call(context, self, selfType, name, arg1, arg2, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, name, method, arg1, arg2, arg3);
        }
        cache = entry;
        return method.call(context, self, selfType, name, arg1, arg2, arg3);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, name, method, arg1, arg2, arg3, block);
        }
        cache = entry;
        return method.call(context, self, selfType, name, arg1, arg2, arg3, block);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, String name, DynamicMethod method, IRubyObject[] args) {
        return RuntimeHelpers.callMethodMissing(context, self, method, name, args, callType, Block.NULL_BLOCK);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, String name, DynamicMethod method) {
        return RuntimeHelpers.callMethodMissing(context, self, method, name, callType, Block.NULL_BLOCK);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, String name, DynamicMethod method, Block block) {
        return RuntimeHelpers.callMethodMissing(context, self, method, name, callType, block);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, String name, DynamicMethod method, IRubyObject arg) {
        return RuntimeHelpers.callMethodMissing(context, self, method, name, arg, callType, Block.NULL_BLOCK);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, String name, DynamicMethod method, IRubyObject[] args, Block block) {
        return RuntimeHelpers.callMethodMissing(context, self, method, name, args, callType, block);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, String name, DynamicMethod method, IRubyObject arg, Block block) {
        return RuntimeHelpers.callMethodMissing(context, self, method, name, arg, callType, block);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, String name, DynamicMethod method, IRubyObject arg1, IRubyObject arg2) {
        return RuntimeHelpers.callMethodMissing(context, self, method, name, arg1, arg2, callType, Block.NULL_BLOCK);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, String name, DynamicMethod method, IRubyObject arg1, IRubyObject arg2, Block block) {
        return RuntimeHelpers.callMethodMissing(context, self, method, name, arg1, arg2, callType, block);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, String name, DynamicMethod method, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return RuntimeHelpers.callMethodMissing(context, self, method, name, arg1, arg2, arg3, callType, Block.NULL_BLOCK);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, String name, DynamicMethod method, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        return RuntimeHelpers.callMethodMissing(context, self, method, name, arg1, arg2, arg3, callType, block);
    }

    protected boolean methodMissing(DynamicMethod method, IRubyObject caller) {
        return method.isUndefined();
    }

    protected static RubyClass pollAndGetClass(ThreadContext context, IRubyObject self, RubyModule frameClass, String frameName) {
        checkSuperDisabledOrOutOfMethod(context, frameClass, frameName);
        RubyClass superClass = RuntimeHelpers.findImplementerIfNecessary(self.getMetaClass(), frameClass).getSuperClass();
        return superClass;
    }

    protected static void checkSuperDisabledOrOutOfMethod(ThreadContext context, RubyModule frameClass, String frameName) {
        if (frameClass == null) {
            if (frameName != null) {
                throw context.getRuntime().newNameError("superclass method '" + frameName + "' disabled", frameName);
            } else {
                throw context.getRuntime().newNoMethodError("super called outside of method", null, context.getRuntime().getNil());
            }
        }
    }

    protected static IRubyObject handleBreakJump(ThreadContext context, JumpException.BreakJump bj) throws JumpException.BreakJump {
        if (context.getFrameJumpTarget() == bj.getTarget()) {
            return (IRubyObject) bj.getValue();
        }
        throw bj;
    }

    protected static RaiseException retryJumpError(ThreadContext context) {
        return context.getRuntime().newLocalJumpError(RubyLocalJumpError.Reason.RETRY, context.getRuntime().getNil(), "retry outside of rescue not supported");
    }
}
