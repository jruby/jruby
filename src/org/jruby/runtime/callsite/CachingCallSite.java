package org.jruby.runtime.callsite;

import org.jruby.RubyClass;
import org.jruby.RubyLocalJumpError;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.JumpException.BreakJump;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class CachingCallSite extends CallSite {
    protected volatile CacheEntry cache = CacheEntry.NULL_CACHE;
    private int misses = 0;
    private static final int MAX_MISSES = 50;
    public static volatile int totalCallSites;
    public static volatile int failedCallSites;

    public CachingCallSite(String methodName, CallType callType) {
        super(methodName, callType);
        totalCallSites++;
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject... args) {
        RubyClass selfType = pollAndGetClass(context, self);
        CacheEntry myCache = cache;
        if (myCache.typeOk(selfType)) {
            return myCache.cachedMethod.call(context, self, selfType, methodName, args);
        }
        return cacheAndCall(caller, selfType, args, context, self);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        try {
            RubyClass selfType = pollAndGetClass(context, self);
            CacheEntry myCache = cache;
            if (myCache.typeOk(selfType)) {
                return myCache.cachedMethod.call(context, self, selfType, methodName, args, block);
            }
            return cacheAndCall(caller, selfType, block, args, context, self);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        }
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        try {
            RubyClass selfType = pollAndGetClass(context, self);
            CacheEntry myCache = cache;
            if (myCache.typeOk(selfType)) {
                return myCache.cachedMethod.call(context, self, selfType, methodName, args, block);
            }
            return cacheAndCall(caller, selfType, block, args, context, self);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self) {
        RubyClass selfType = pollAndGetClass(context, self);
        CacheEntry myCache = cache;
        if (myCache.typeOk(selfType)) {
            return myCache.cachedMethod.call(context, self, selfType, methodName);
        }
        return cacheAndCall(caller, selfType, context, self);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        try {
            RubyClass selfType = pollAndGetClass(context, self);
            CacheEntry myCache = cache;
            if (myCache.typeOk(selfType)) {
                return myCache.cachedMethod.call(context, self, selfType, methodName, block);
            }
            return cacheAndCall(caller, selfType, block, context, self);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        }
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        try {
            RubyClass selfType = pollAndGetClass(context, self);
            CacheEntry myCache = cache;
            if (myCache.typeOk(selfType)) {
                return myCache.cachedMethod.call(context, self, selfType, methodName, block);
            }
            return cacheAndCall(caller, selfType, block, context, self);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1) {
        RubyClass selfType = pollAndGetClass(context, self);
        CacheEntry myCache = cache;
        if (myCache.typeOk(selfType)) {
            return myCache.cachedMethod.call(context, self, selfType, methodName, arg1);
        }
        return cacheAndCall(caller, selfType, context, self, arg1);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block) {
        RubyClass selfType = pollAndGetClass(context, self);
        try {
            CacheEntry myCache = cache;
            if (myCache.typeOk(selfType)) {
                return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, block);
            }
            return cacheAndCall(caller, selfType, block, context, self, arg1);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        }
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block) {
        try {
            RubyClass selfType = pollAndGetClass(context, self);
            CacheEntry myCache = cache;
            if (myCache.typeOk(selfType)) {
                return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, block);
            }
            return cacheAndCall(caller, selfType, block, context, self, arg1);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
        RubyClass selfType = pollAndGetClass(context, self);
        CacheEntry myCache = cache;
        if (myCache.typeOk(selfType)) {
            return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, arg2);
        }
        return cacheAndCall(caller, selfType, context, self, arg1, arg2);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        try {
            RubyClass selfType = pollAndGetClass(context, self);
            CacheEntry myCache = cache;
            if (myCache.typeOk(selfType)) {
                return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, arg2, block);
            }
            return cacheAndCall(caller, selfType, block, context, self, arg1, arg2);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        }
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        try {
            RubyClass selfType = pollAndGetClass(context, self);
            CacheEntry myCache = cache;
            if (myCache.typeOk(selfType)) {
                return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, arg2, block);
            }
            return cacheAndCall(caller, selfType, block, context, self, arg1, arg2);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        RubyClass selfType = pollAndGetClass(context, self);
        CacheEntry myCache = cache;
        if (myCache.typeOk(selfType)) {
            return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, arg2, arg3);
        }
        return cacheAndCall(caller, selfType, context, self, arg1, arg2, arg3);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        try {
            RubyClass selfType = pollAndGetClass(context, self);
            CacheEntry myCache = cache;
            if (myCache.typeOk(selfType)) {
                return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, arg2, arg3, block);
            }
            return cacheAndCall(caller, selfType, block, context, self, arg1, arg2, arg3);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        }
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        try {
            RubyClass selfType = pollAndGetClass(context, self);
            CacheEntry myCache = cache;
            if (myCache.typeOk(selfType)) {
                return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, arg2, arg3, block);
            }
            return cacheAndCall(caller, selfType, block, context, self, arg1, arg2, arg3);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            throw retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, IRubyObject[] args, ThreadContext context, IRubyObject self) {
        DynamicMethod method = selfType.searchMethod(methodName);
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, args, block);
        }
        updateCacheEntry(method, selfType);
        return method.call(context, self, selfType, methodName, args, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, IRubyObject[] args, ThreadContext context, IRubyObject self) {
        DynamicMethod method = selfType.searchMethod(methodName);
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, args);
        }
        updateCacheEntry(method, selfType);
        return method.call(context, self, selfType, methodName, args);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self) {
        DynamicMethod method = selfType.searchMethod(methodName);
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method);
        }
        updateCacheEntry(method, selfType);
        return method.call(context, self, selfType, methodName);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self) {
        DynamicMethod method = selfType.searchMethod(methodName);
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, block);
        }
        updateCacheEntry(method, selfType);
        return method.call(context, self, selfType, methodName, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg) {
        DynamicMethod method = selfType.searchMethod(methodName);
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg);
        }
        updateCacheEntry(method, selfType);
        return method.call(context, self, selfType, methodName, arg);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, IRubyObject arg) {
        DynamicMethod method = selfType.searchMethod(methodName);
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg, block);
        }
        updateCacheEntry(method, selfType);
        return method.call(context, self, selfType, methodName, arg, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
        DynamicMethod method = selfType.searchMethod(methodName);
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg1, arg2);
        }
        updateCacheEntry(method, selfType);
        return method.call(context, self, selfType, methodName, arg1, arg2);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
        DynamicMethod method = selfType.searchMethod(methodName);
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg1, arg2, block);
        }
        updateCacheEntry(method, selfType);
        return method.call(context, self, selfType, methodName, arg1, arg2, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        DynamicMethod method = selfType.searchMethod(methodName);
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg1, arg2, arg3);
        }
        updateCacheEntry(method, selfType);
        return method.call(context, self, selfType, methodName, arg1, arg2, arg3);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        DynamicMethod method = selfType.searchMethod(methodName);
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg1, arg2, arg3, block);
        }
        updateCacheEntry(method, selfType);
        return method.call(context, self, selfType, methodName, arg1, arg2, arg3, block);
    }

    private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject[] args) {
        return RuntimeHelpers.callMethodMissing(context, self, method, methodName, args, callType, Block.NULL_BLOCK);
    }

    private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method) {
        return RuntimeHelpers.callMethodMissing(context, self, method, methodName, callType, Block.NULL_BLOCK);
    }

    private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, Block block) {
        return RuntimeHelpers.callMethodMissing(context, self, method, methodName, callType, block);
    }

    private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg) {
        return RuntimeHelpers.callMethodMissing(context, self, method, methodName, arg, callType, Block.NULL_BLOCK);
    }

    private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject[] args, Block block) {
        return RuntimeHelpers.callMethodMissing(context, self, method, methodName, args, callType, block);
    }

    private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg, Block block) {
        return RuntimeHelpers.callMethodMissing(context, self, method, methodName, arg, callType, block);
    }

    private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg1, IRubyObject arg2) {
        return RuntimeHelpers.callMethodMissing(context, self, method, methodName, arg1, arg2, callType, Block.NULL_BLOCK);
    }

    private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg1, IRubyObject arg2, Block block) {
        return RuntimeHelpers.callMethodMissing(context, self, method, methodName, arg1, arg2, callType, block);
    }

    private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return RuntimeHelpers.callMethodMissing(context, self, method, methodName, arg1, arg2, arg3, callType, Block.NULL_BLOCK);
    }

    private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        return RuntimeHelpers.callMethodMissing(context, self, method, methodName, arg1, arg2, arg3, callType, block);
    }

    protected abstract boolean methodMissing(DynamicMethod method, IRubyObject caller);

    private RubyClass pollAndGetClass(ThreadContext context, IRubyObject self) {
        context.callThreadPoll();
        RubyClass selfType = self.getMetaClass();
        return selfType;
    }

    private void updateCacheEntry(DynamicMethod method, RubyClass selfType) {
        if (misses < MAX_MISSES) {
            misses++;
            if (misses >= MAX_MISSES) {
                failedCallSites++;
            }
            cache = new CacheEntry(method, selfType, methodName);
        }
    }

    public void removeCachedMethod() {
        cache = CacheEntry.NULL_CACHE;
    }

    private IRubyObject handleBreakJump(ThreadContext context, BreakJump bj) throws BreakJump {
        if (context.getFrameJumpTarget() == bj.getTarget()) {
            return (IRubyObject) bj.getValue();
        }
        throw bj;
    }

    private RaiseException retryJumpError(ThreadContext context) {
        return context.getRuntime().newLocalJumpError(RubyLocalJumpError.Reason.RETRY, context.getRuntime().getNil(), "retry outside of rescue not supported");
    }
}
