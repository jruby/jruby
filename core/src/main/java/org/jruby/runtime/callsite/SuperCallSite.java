package org.jruby.runtime.callsite;

import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyLocalJumpError;
import org.jruby.RubyModule;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class SuperCallSite extends CallSite {
    protected volatile SuperTuple cache = SuperTuple.NULL_CACHE;

    private static class SuperTuple {
        static final SuperTuple NULL_CACHE = new SuperTuple("", CacheEntry.NULL_CACHE);
        public final String name;
        public final CacheEntry cache;

        public SuperTuple(String name, CacheEntry cache) {
            this.name = name;
            this.cache = cache;
        }

        public boolean cacheOk(String name, RubyClass klass) {
            return this.name.equals(name) && cache.typeOk(klass);
        }
    }
    
    public SuperCallSite() {
        super("super", CallType.SUPER);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, long fixnum) {
        return call(context, caller, self, RubyFixnum.newFixnum(context.runtime, fixnum));
    }
    
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, double flote) {
        return call(context, caller, self, RubyFloat.newFloat(context.runtime, flote));
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject... args) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        SuperTuple myCache = cache;
        if (selfType != null && myCache.cacheOk(name, selfType)) {
            return myCache.cache.method.call(context, self, selfType, name, args);
        }
        return cacheAndCall(caller, selfType, args, context, self, name);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        SuperTuple myCache = cache;
        if (selfType != null && myCache.cacheOk(name, selfType)) {
            return myCache.cache.method.call(context, self, selfType, name, args, block);
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

    public IRubyObject callVarargs(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject... args) {
        switch (args.length) {
            case 0: return call(context, caller, self);
            case 1: return call(context, caller, self, args[0]);
            case 2: return call(context, caller, self, args[0], args[1]);
            case 3: return call(context, caller, self, args[0], args[1], args[2]);
            default: return call(context, caller, self, args);
        }
    }

    public IRubyObject callVarargs(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        switch (args.length) {
            case 0: return call(context, caller, self, block);
            case 1: return call(context, caller, self, args[0], block);
            case 2: return call(context, caller, self, args[0], args[1], block);
            case 3: return call(context, caller, self, args[0], args[1], args[2], block);
            default: return call(context, caller, self, args, block);
        }
    }

    public IRubyObject callVarargsIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        switch (args.length) {
            case 0: return callIter(context, caller, self, block);
            case 1: return callIter(context, caller, self, args[0], block);
            case 2: return callIter(context, caller, self, args[0], args[1], block);
            case 3: return callIter(context, caller, self, args[0], args[1], args[2], block);
            default: return callIter(context, caller, self, args, block);
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        SuperTuple myCache = cache;
        if (selfType != null && myCache.cacheOk(name, selfType)) {
            return myCache.cache.method.call(context, self, selfType, name);
        }
        return cacheAndCall(caller, selfType, context, self, name);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        SuperTuple myCache = cache;
        if (selfType != null && myCache.cacheOk(name, selfType)) {
            return myCache.cache.method.call(context, self, selfType, name, block);
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

        SuperTuple myCache = cache;
        if (selfType != null && myCache.cacheOk(name, selfType)) {
            return myCache.cache.method.call(context, self, selfType, name, arg1);
        }
        return cacheAndCall(caller, selfType, context, self, name, arg1);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        SuperTuple myCache = cache;
        if (selfType != null && myCache.cacheOk(name, selfType)) {
            return myCache.cache.method.call(context, self, selfType, name, arg1, block);
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

        SuperTuple myCache = cache;
        if (selfType != null && myCache.cacheOk(name, selfType)) {
            return myCache.cache.method.call(context, self, selfType, name, arg1, arg2);
        }
        return cacheAndCall(caller, selfType, context, self, name, arg1, arg2);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        SuperTuple myCache = cache;
        if (selfType != null && myCache.cacheOk(name, selfType)) {
            return myCache.cache.method.call(context, self, selfType, name, arg1, arg2, block);
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

        SuperTuple myCache = cache;
        if (selfType != null && myCache.cacheOk(name, selfType)) {
            return myCache.cache.method.call(context, self, selfType, name, arg1, arg2, arg3);
        }
        return cacheAndCall(caller, selfType, context, self, name, arg1, arg2, arg3);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();
        RubyClass selfType = pollAndGetClass(context, self, klazz, name);

        SuperTuple myCache = cache;
        if (selfType != null && myCache.cacheOk(name, selfType)) {
            return myCache.cache.method.call(context, self, selfType, name, arg1, arg2, arg3, block);
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
        cache = new SuperTuple(name, entry);
        return method.call(context, self, selfType, name, args, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, IRubyObject[] args, ThreadContext context, IRubyObject self, String name) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, name, method, args);
        }
        cache = new SuperTuple(name, entry);
        return method.call(context, self, selfType, name, args);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, String name) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, name, method);
        }
        cache = new SuperTuple(name, entry);
        return method.call(context, self, selfType, name);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, String name) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, name, method, block);
        }
        cache = new SuperTuple(name, entry);
        return method.call(context, self, selfType, name, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, String name, IRubyObject arg) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, name, method, arg);
        }
        cache = new SuperTuple(name, entry);
        return method.call(context, self, selfType, name, arg);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, String name, IRubyObject arg) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, name, method, arg, block);
        }
        cache = new SuperTuple(name, entry);
        return method.call(context, self, selfType, name, arg, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, name, method, arg1, arg2);
        }
        cache = new SuperTuple(name, entry);
        return method.call(context, self, selfType, name, arg1, arg2);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, name, method, arg1, arg2, block);
        }
        cache = new SuperTuple(name, entry);
        return method.call(context, self, selfType, name, arg1, arg2, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, name, method, arg1, arg2, arg3);
        }
        cache = new SuperTuple(name, entry);
        return method.call(context, self, selfType, name, arg1, arg2, arg3);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        CacheEntry entry = selfType != null ? selfType.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, name, method, arg1, arg2, arg3, block);
        }
        cache = new SuperTuple(name, entry);
        return method.call(context, self, selfType, name, arg1, arg2, arg3, block);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, String name, DynamicMethod method, IRubyObject[] args) {
        return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, args, Block.NULL_BLOCK);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, String name, DynamicMethod method) {
        return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, Block.NULL_BLOCK);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, String name, DynamicMethod method, Block block) {
        return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, block);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, String name, DynamicMethod method, IRubyObject arg) {
        return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg, Block.NULL_BLOCK);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, String name, DynamicMethod method, IRubyObject[] args, Block block) {
        return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, args, block);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, String name, DynamicMethod method, IRubyObject arg0, Block block) {
        return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg0, block);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, String name, DynamicMethod method, IRubyObject arg0, IRubyObject arg1) {
        return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg0, arg1, Block.NULL_BLOCK);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, String name, DynamicMethod method, IRubyObject arg0, IRubyObject arg1, Block block) {
        return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg0, arg1, block);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, String name, DynamicMethod method, IRubyObject arg0, IRubyObject arg1, IRubyObject arg3) {
        return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg0, arg1, arg3, Block.NULL_BLOCK);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, String name, DynamicMethod method, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg0, arg1, arg2, block);
    }

    protected boolean methodMissing(DynamicMethod method, IRubyObject caller) {
        return method.isUndefined();
    }

    protected static RubyClass pollAndGetClass(ThreadContext context, IRubyObject self, RubyModule frameClass, String frameName) {
        checkSuperDisabledOrOutOfMethod(context, frameClass, frameName);
        RubyClass superClass = Helpers.findImplementerIfNecessary(self.getMetaClass(), frameClass).getSuperClass();
        return superClass;
    }

    protected static void checkSuperDisabledOrOutOfMethod(ThreadContext context, RubyModule frameClass, String frameName) {
        if (frameClass == null) {
            if (frameName != null) {
                throw context.runtime.newNameError("superclass method '" + frameName + "' disabled", frameName);
            } else {
                throw context.runtime.newNoMethodError("super called outside of method", null, context.runtime.getNil());
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
        return context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.RETRY, context.runtime.getNil(), "retry outside of rescue not supported");
    }
}
