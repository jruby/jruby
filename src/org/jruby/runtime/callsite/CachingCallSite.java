package org.jruby.runtime.callsite;

import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyLocalJumpError;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.JumpException.BreakJump;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class CachingCallSite extends CallSite {
    protected volatile CacheEntry cache = CacheEntry.NULL_CACHE;
    public static volatile int totalCallSites;
    public static volatile int failedCallSites;

    public CachingCallSite(String methodName, CallType callType) {
        super(methodName, callType);
        totalCallSites++;
    }

    public CacheEntry getCache() {
        return cache;
    }

    public boolean isOptimizable() {
        if (getCache() != CacheEntry.NULL_CACHE) {
            return true;
        }
        return false;
    }

    public int getCachedClassIndex() {
        CacheEntry cacheEntry = getCache();
        if (cacheEntry != CacheEntry.NULL_CACHE) {
            return cacheEntry.method.getImplementationClass().index;
        }
        return ClassIndex.NO_INDEX;
    }

    public String getMethodName() {
        return methodName;
    }

    public long getCachedMethodSerial() {
        CacheEntry cacheEntry = getCache();
        if (cacheEntry != CacheEntry.NULL_CACHE) {
            return cacheEntry.method.getSerialNumber();
        }
        return -1;
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, long fixnum) {
        return call(context, caller, self, RubyFixnum.newFixnum(context.getRuntime(), fixnum));
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject... args) {
        RubyClass selfType = pollAndGetClass(context, self);
        CacheEntry myCache = cache;
        if (myCache.typeOk(selfType)) {
            return myCache.method.call(context, self, selfType, methodName, args);
        }
        return cacheAndCall(caller, selfType, args, context, self);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        RubyClass selfType = pollAndGetClass(context, self);
        CacheEntry myCache = cache;
        if (myCache.typeOk(selfType)) {
            return myCache.method.call(context, self, selfType, methodName, args, block);
        }
        return cacheAndCall(caller, selfType, block, args, context, self);
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
        RubyClass selfType = pollAndGetClass(context, self);
        CacheEntry myCache = cache;
        if (myCache.typeOk(selfType)) {
            return myCache.method.call(context, self, selfType, methodName);
        }
        return cacheAndCall(caller, selfType, context, self);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        RubyClass selfType = pollAndGetClass(context, self);
        CacheEntry myCache = cache;
        if (myCache.typeOk(selfType)) {
            return myCache.method.call(context, self, selfType, methodName, block);
        }
        return cacheAndCall(caller, selfType, block, context, self);
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
        RubyClass selfType = pollAndGetClass(context, self);
        CacheEntry myCache = cache;
        if (myCache.typeOk(selfType)) {
            return myCache.method.call(context, self, selfType, methodName, arg1);
        }
        return cacheAndCall(caller, selfType, context, self, arg1);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block) {
        RubyClass selfType = pollAndGetClass(context, self);
        CacheEntry myCache = cache;
        if (myCache.typeOk(selfType)) {
            return myCache.method.call(context, self, selfType, methodName, arg1, block);
        }
        return cacheAndCall(caller, selfType, block, context, self, arg1);
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
        RubyClass selfType = pollAndGetClass(context, self);
        CacheEntry myCache = cache;
        if (myCache.typeOk(selfType)) {
            return myCache.method.call(context, self, selfType, methodName, arg1, arg2);
        }
        return cacheAndCall(caller, selfType, context, self, arg1, arg2);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        RubyClass selfType = pollAndGetClass(context, self);
        CacheEntry myCache = cache;
        if (myCache.typeOk(selfType)) {
            return myCache.method.call(context, self, selfType, methodName, arg1, arg2, block);
        }
        return cacheAndCall(caller, selfType, block, context, self, arg1, arg2);
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
        RubyClass selfType = pollAndGetClass(context, self);
        CacheEntry myCache = cache;
        if (myCache.typeOk(selfType)) {
            return myCache.method.call(context, self, selfType, methodName, arg1, arg2, arg3);
        }
        return cacheAndCall(caller, selfType, context, self, arg1, arg2, arg3);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        RubyClass selfType = pollAndGetClass(context, self);
        CacheEntry myCache = cache;
        if (myCache.typeOk(selfType)) {
            return myCache.method.call(context, self, selfType, methodName, arg1, arg2, arg3, block);
        }
        return cacheAndCall(caller, selfType, block, context, self, arg1, arg2, arg3);
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
    
    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, IRubyObject[] args, ThreadContext context, IRubyObject self) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, args, block);
        }
        cache = entry;
        return method.call(context, self, selfType, methodName, args, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, IRubyObject[] args, ThreadContext context, IRubyObject self) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, args);
        }
        cache = entry;
        return method.call(context, self, selfType, methodName, args);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method);
        }
        cache = entry;
        return method.call(context, self, selfType, methodName);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, block);
        }
        cache = entry;
        return method.call(context, self, selfType, methodName, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg);
        }
        cache = entry;
        return method.call(context, self, selfType, methodName, arg);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, IRubyObject arg) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg, block);
        }
        cache = entry;
        return method.call(context, self, selfType, methodName, arg, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg1, arg2);
        }
        cache = entry;
        return method.call(context, self, selfType, methodName, arg1, arg2);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg1, arg2, block);
        }
        cache = entry;
        return method.call(context, self, selfType, methodName, arg1, arg2, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg1, arg2, arg3);
        }
        cache = entry;
        return method.call(context, self, selfType, methodName, arg1, arg2, arg3);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg1, arg2, arg3, block);
        }
        cache = entry;
        return method.call(context, self, selfType, methodName, arg1, arg2, arg3, block);
    }

    private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject[] args) {
        return RuntimeHelpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, self, self.getMetaClass(), methodName, args, Block.NULL_BLOCK);
    }

    private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method) {
        return RuntimeHelpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, self, self.getMetaClass(), methodName, Block.NULL_BLOCK);
    }

    private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, Block block) {
        return RuntimeHelpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, self, self.getMetaClass(), methodName, block);
    }

    private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg) {
        return RuntimeHelpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, self, self.getMetaClass(), methodName, arg, Block.NULL_BLOCK);
    }

    private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject[] args, Block block) {
        return RuntimeHelpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, self, self.getMetaClass(), methodName, args, block);
    }

    private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg0, Block block) {
        return RuntimeHelpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, self, self.getMetaClass(), methodName, arg0, block);
    }

    private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg0, IRubyObject arg1) {
        return RuntimeHelpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, self, self.getMetaClass(), methodName, arg0, arg1, Block.NULL_BLOCK);
    }

    private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg0, IRubyObject arg1, Block block) {
        return RuntimeHelpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, self, self.getMetaClass(), methodName, arg0, arg1, block);
    }

    private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return RuntimeHelpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, self, self.getMetaClass(), methodName, arg0, arg1, arg2, Block.NULL_BLOCK);
    }

    private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return RuntimeHelpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, self, self.getMetaClass(), methodName, arg0, arg1, arg2, block);
    }

    protected abstract boolean methodMissing(DynamicMethod method, IRubyObject caller);

    private static RubyClass pollAndGetClass(ThreadContext context, IRubyObject self) {
        context.callThreadPoll();
        RubyClass selfType = self.getMetaClass();
        return selfType;
    }

    private static IRubyObject handleBreakJump(ThreadContext context, BreakJump bj) throws BreakJump {
        if (context.getFrameJumpTarget() == bj.getTarget()) {
            return (IRubyObject) bj.getValue();
        }
        throw bj;
    }

    private static RaiseException retryJumpError(ThreadContext context) {
        return context.getRuntime().newLocalJumpError(RubyLocalJumpError.Reason.RETRY, context.getRuntime().getNil(), "retry outside of rescue not supported");
    }
}
