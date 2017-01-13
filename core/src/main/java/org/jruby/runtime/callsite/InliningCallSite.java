package org.jruby.runtime.callsite;

import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.compiler.Compilable;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.CallBase;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by enebo on 11/4/15.
 */
public class InliningCallSite extends CallSite {
    protected InliningCacheEntry cache = InliningCacheEntry.NULL_CACHE;

    private IRScope callingScope;
    private CallBase call;

    public InliningCallSite(CallBase call, IRScope callingScope) {
        super(call.getName(), call.getCallType());

        this.call = call;
        this.callingScope = callingScope;
    }

    public InliningCacheEntry getCache() {
        return cache;
    }

    public String getMethodName() {
        return methodName;
    }

    public void attemptInline(RubyClass selfType) {
        callingScope.inlineMethod((Compilable) cache.method, selfType, cache.token, null, call, false);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, long fixnum) {
        return call(context, caller, self, RubyFixnum.newFixnum(context.runtime, fixnum));
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, double flote) {
        return call(context, caller, self, RubyFloat.newFloat(context.runtime, flote));
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject... args) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        InliningCacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if (cache.maybeInline(callingScope)) attemptInline(selfType);
            return cache.method.call(context, this, self, selfType, methodName, args);
        }
        return cacheAndCall(caller, selfType, args, context, self);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        InliningCacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if (cache.maybeInline(callingScope)) attemptInline(selfType);
            return cache.method.call(context, this, self, selfType, methodName, args, block);
        }
        return cacheAndCall(caller, selfType, block, args, context, self);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        return callBlock(context, caller, self, args, block);
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        try {
            return callBlock(context, caller, self, args, block);
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
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        InliningCacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if (cache.maybeInline(callingScope)) attemptInline(selfType);
            return cache.method.call(context, this, self, selfType, methodName);
        }
        return cacheAndCall(caller, selfType, context, self);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        InliningCacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if (cache.maybeInline(callingScope)) attemptInline(selfType);
            return cache.method.call(context, this, self, selfType, methodName, block);
        }
        return cacheAndCall(caller, selfType, block, context, self);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        return callBlock(context, caller, self, block);
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        try {
            return callBlock(context, caller, self, block);
        } finally {
            block.escape();
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        InliningCacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if (cache.maybeInline(callingScope)) attemptInline(selfType);
            return cache.method.call(context, this, self, selfType, methodName, arg1);
        }
        return cacheAndCall(caller, selfType, context, self, arg1);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        InliningCacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if (cache.maybeInline(callingScope)) attemptInline(selfType);
            return cache.method.call(context, this, self, selfType, methodName, arg1, block);
        }
        return cacheAndCall(caller, selfType, block, context, self, arg1);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block) {
        return callBlock(context, caller, self, arg1, block);
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block) {
        try {
            return callBlock(context, caller, self, arg1, block);
        } finally {
            block.escape();
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        InliningCacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if (cache.maybeInline(callingScope)) attemptInline(selfType);
            return cache.method.call(context, this, self, selfType, methodName, arg1, arg2);
        }
        return cacheAndCall(caller, selfType, context, self, arg1, arg2);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        InliningCacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if (cache.maybeInline(callingScope)) attemptInline(selfType);
            return cache.method.call(context, this, self, selfType, methodName, arg1, arg2, block);
        }
        return cacheAndCall(caller, selfType, block, context, self, arg1, arg2);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        return callBlock(context, caller, self, arg1, arg2, block);
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        try {
            return callBlock(context, caller, self, arg1, arg2, block);
        } finally {
            block.escape();
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        InliningCacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if (cache.maybeInline(callingScope)) attemptInline(selfType);
            return cache.method.call(context, this, self, selfType, methodName, arg1, arg2, arg3);
        }
        return cacheAndCall(caller, selfType, context, self, arg1, arg2, arg3);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        InliningCacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if (cache.maybeInline(callingScope)) attemptInline(selfType);
            return cache.method.call(context, this, self, selfType, methodName, arg1, arg2, arg3, block);
        }
        return cacheAndCall(caller, selfType, block, context, self, arg1, arg2, arg3);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        return callBlock(context, caller, self, arg1, arg2, arg3, block);
    }

    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        try {
            return callBlock(context, caller, self, arg1, arg2, arg3, block);
        } finally {
            block.escape();
        }
    }

    private InliningCacheEntry cacheAndGet(RubyClass selfType, String methodName) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        return cache = new InliningCacheEntry(entry.method, entry.token);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, IRubyObject[] args, ThreadContext context, IRubyObject self) {
        InliningCacheEntry entry = cacheAndGet(selfType, methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, args, block);
        }
        updateCache(entry);
        return method.call(context, this, self, selfType, methodName, args, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, IRubyObject[] args, ThreadContext context, IRubyObject self) {
        InliningCacheEntry entry = cacheAndGet(selfType, methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, args);
        }
        updateCache(entry);
        return method.call(context, this, self, selfType, methodName, args);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self) {
                InliningCacheEntry entry = cacheAndGet(selfType, methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method);
        }
        updateCache(entry);
        return method.call(context, this, self, selfType, methodName);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self) {
                InliningCacheEntry entry = cacheAndGet(selfType, methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, block);
        }
        updateCache(entry);
        return method.call(context, this, self, selfType, methodName, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg) {
                InliningCacheEntry entry = cacheAndGet(selfType, methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg);
        }
        updateCache(entry);
        return method.call(context, this, self, selfType, methodName, arg);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, IRubyObject arg) {
                InliningCacheEntry entry = cacheAndGet(selfType, methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg, block);
        }
        updateCache(entry);
        return method.call(context, this, self, selfType, methodName, arg, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
                InliningCacheEntry entry = cacheAndGet(selfType, methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg1, arg2);
        }
        updateCache(entry);
        return method.call(context, this, self, selfType, methodName, arg1, arg2);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
                InliningCacheEntry entry = cacheAndGet(selfType, methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg1, arg2, block);
        }
        updateCache(entry);
        return method.call(context, this, self, selfType, methodName, arg1, arg2, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
                InliningCacheEntry entry = cacheAndGet(selfType, methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg1, arg2, arg3);
        }
        updateCache(entry);
        return method.call(context, this, self, selfType, methodName, arg1, arg2, arg3);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
                InliningCacheEntry entry = cacheAndGet(selfType, methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg1, arg2, arg3, block);
        }
        updateCache(entry);
        return method.call(context, this, self, selfType, methodName, arg1, arg2, arg3, block);
    }

    protected void updateCache(InliningCacheEntry newEntry) {
        // not really working because it flags jitted methods as polymorphic
//        InliningCacheEntry oldCache = cache;
//        if (oldCache != InliningCacheEntry.NULL_CACHE && oldCache.method != newEntry.method) {
//            isPolymorphic.set(true);
//        }
        cache = newEntry;
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject[] args) {
        return Helpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, this, self, self.getMetaClass(), methodName, args, Block.NULL_BLOCK);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method) {
        return Helpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, this, self, self.getMetaClass(), methodName, Block.NULL_BLOCK);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, Block block) {
        return Helpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, this, self, self.getMetaClass(), methodName, block);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg) {
        return Helpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, this, self, self.getMetaClass(), methodName, arg, Block.NULL_BLOCK);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject[] args, Block block) {
        return Helpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, this, self, self.getMetaClass(), methodName, args, block);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg0, Block block) {
        return Helpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, this, self, self.getMetaClass(), methodName, arg0, block);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg0, IRubyObject arg1) {
        return Helpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, this, self, self.getMetaClass(), methodName, arg0, arg1, Block.NULL_BLOCK);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg0, IRubyObject arg1, Block block) {
        return Helpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, this, self, self.getMetaClass(), methodName, arg0, arg1, block);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return Helpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, this, self, self.getMetaClass(), methodName, arg0, arg1, arg2, Block.NULL_BLOCK);
    }

    protected IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return Helpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, this, self, self.getMetaClass(), methodName, arg0, arg1, arg2, block);
    }

    protected boolean methodMissing(DynamicMethod method, IRubyObject caller) {
        return method.isUndefined() || (!methodName.equals("method_missing") && !method.isCallableFrom(caller, callType));
    }

    private static RubyClass getClass(IRubyObject self) {
        // the cast in the following line is necessary due to lacking optimizations in Hotspot
        return ((RubyBasicObject) self).getMetaClass();
    }
}
