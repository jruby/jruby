package org.jruby.runtime.callsite;

import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.RubyBasicObject.getMetaClass;

public abstract class CachingCallSite extends CallSite {

    protected CacheEntry cache = CacheEntry.NULL_CACHE;
    @Deprecated
    protected CacheEntry builtinCache = CacheEntry.NULL_CACHE;

    public CachingCallSite(String methodName, CallType callType) {
        super(methodName, callType);
    }

    public final CacheEntry getCache() {
        return cache;
    }

    protected CacheEntry setCache(CacheEntry entry, IRubyObject self) {
        return cache = entry;
    }

    public final boolean isOptimizable() {
        return cache != CacheEntry.NULL_CACHE;
    }

    public final int getCachedClassIndex() {
        CacheEntry cache = this.cache;
        if (cache != CacheEntry.NULL_CACHE) {
            return cache.method.getImplementationClass().getClassIndex().ordinal();
        }
        return ClassIndex.NO_INDEX.ordinal();
    }

    public final String getMethodName() {
        return methodName;
    }

    public final long getCachedMethodSerial() {
        CacheEntry cache = this.cache;
        if (cache != CacheEntry.NULL_CACHE) {
            return cache.method.getSerialNumber();
        }
        return -1;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, long fixnum) {
        return call(context, caller, self, RubyFixnum.newFixnum(context.runtime, fixnum));
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, double flote) {
        return call(context, caller, self, RubyFloat.newFloat(context.runtime, flote));
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject... args) {
        RubyClass selfType = getMetaClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, cache.sourceModule, methodName, args);
        }
        return cacheAndCall(caller, selfType, args, context, self);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        RubyClass selfType = getMetaClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, cache.sourceModule, methodName, args, block);
        }
        return cacheAndCall(caller, selfType, block, args, context, self);
    }

    @Override
    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self,
        IRubyObject[] args, Block block) {
        try {
            return call(context, caller, self, args, block);
        } finally {
            block.escape();
        }
    }

    @Override
    public final IRubyObject callVarargs(ThreadContext context, IRubyObject caller,
        IRubyObject self, IRubyObject... args) {
        switch (args.length) {
            case 0: return call(context, caller, self);
            case 1: return call(context, caller, self, args[0]);
            case 2: return call(context, caller, self, args[0], args[1]);
            case 3: return call(context, caller, self, args[0], args[1], args[2]);
            default: return call(context, caller, self, args);
        }
    }

    @Override
    public final IRubyObject callVarargs(ThreadContext context, IRubyObject caller,
        IRubyObject self, IRubyObject[] args, Block block) {
        switch (args.length) {
            case 0: return call(context, caller, self, block);
            case 1: return call(context, caller, self, args[0], block);
            case 2: return call(context, caller, self, args[0], args[1], block);
            case 3: return call(context, caller, self, args[0], args[1], args[2], block);
            default: return call(context, caller, self, args, block);
        }
    }

    @Override
    public final IRubyObject callVarargsIter(ThreadContext context, IRubyObject caller,
        IRubyObject self, IRubyObject[] args, Block block) {
        switch (args.length) {
            case 0: return callIter(context, caller, self, block);
            case 1: return callIter(context, caller, self, args[0], block);
            case 2: return callIter(context, caller, self, args[0], args[1], block);
            case 3: return callIter(context, caller, self, args[0], args[1], args[2], block);
            default: return callIter(context, caller, self, args, block);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self) {
        RubyClass selfType = getMetaClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, cache.sourceModule, methodName);
        }
        return cacheAndCall(caller, selfType, context, self);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        RubyClass selfType = getMetaClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, cache.sourceModule, methodName, block);
        }
        return cacheAndCall(caller, selfType, block, context, self);
    }

    @Override
    public final IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self,
        Block block) {
        try {
            return call(context, caller, self, block);
        } finally {
            block.escape();
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1) {
        RubyClass selfType = getMetaClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, cache.sourceModule, methodName, arg1);
        }
        return cacheAndCall(caller, selfType, context, self, arg1);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block) {
        RubyClass selfType = getMetaClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, cache.sourceModule, methodName, arg1, block);
        }
        return cacheAndCall(caller, selfType, block, context, self, arg1);
    }

    @Override
    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self,
        IRubyObject arg1, Block block) {
        try {
            return call(context, caller, self, arg1, block);
        } finally {
            block.escape();
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
        RubyClass selfType = getMetaClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, cache.sourceModule, methodName, arg1, arg2);
        }
        return cacheAndCall(caller, selfType, context, self, arg1, arg2);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        RubyClass selfType = getMetaClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, cache.sourceModule, methodName, arg1, arg2, block);
        }
        return cacheAndCall(caller, selfType, block, context, self, arg1, arg2);
    }

    @Override
    public final IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self,
        IRubyObject arg1, IRubyObject arg2, Block block) {
        try {
            return call(context, caller, self, arg1, arg2, block);
        } finally {
            block.escape();
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        RubyClass selfType = getMetaClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, cache.sourceModule, methodName, arg1, arg2, arg3);
        }
        return cacheAndCall(caller, selfType, context, self, arg1, arg2, arg3);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        RubyClass selfType = getMetaClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, cache.sourceModule, methodName, arg1, arg2, arg3, block);
        }
        return cacheAndCall(caller, selfType, block, context, self, arg1, arg2, arg3);
    }

    @Override
    public final IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self,
        IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        try {
            return call(context, caller, self, arg1, arg2, arg3, block);
        } finally {
            block.escape();
        }
    }

    public final CacheEntry retrieveCache(IRubyObject self) {
        RubyClass selfType = getMetaClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache;
        }
        return cacheAndGet(self, selfType, methodName);
    }

    // For use directly by classes (e.g. RubyClass) where the metaclass is the caller.
    public final CacheEntry retrieveCache(RubyClass selfType) {
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache;
        }
        return cacheAndGet(selfType, methodName);
    }

    @Deprecated
    public final CacheEntry retrieveCache(RubyClass selfType, String methodName) {
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache;
        }
        return cacheAndGet(selfType, methodName);
    }

    public boolean isBuiltin(IRubyObject self) {
        RubyClass selfType = getMetaClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.isBuiltin();
        }
        return cacheAndGet(self, selfType, methodName).method.isBuiltin(); // false for method.isUndefined()
    }

    // For use directly by classes (e.g. RubyClass) where the metaclass is the caller.
    public final boolean isBuiltin(RubyClass selfType) {
        return retrieveCache(selfType).method.isBuiltin();
    }

    @Deprecated
    private CacheEntry cacheAndGet(RubyClass selfType, String methodName) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        if (!entry.method.isUndefined()) {
            this.cache = entry;
            if (entry.method.isBuiltin()) builtinCache = entry;
        }
        return entry;
    }

    private CacheEntry cacheAndGet(IRubyObject self, RubyClass selfType, String methodName) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        if (!entry.method.isUndefined()) entry = setCache(entry, self);
        return entry;
    }

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block,
        IRubyObject[] args, ThreadContext context, IRubyObject self) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method, args, block);
        }
        entry = setCache(entry, self);
        return method.call(context, self, entry.sourceModule, methodName, args, block);
    }

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType,
        IRubyObject[] args, ThreadContext context, IRubyObject self) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method, args);
        }
        entry = setCache(entry, self);
        return method.call(context, self, entry.sourceModule, methodName, args);
    }

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType,
        ThreadContext context, IRubyObject self) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method);
        }
        entry = setCache(entry, self);
        return method.call(context, self, entry.sourceModule, methodName);
    }

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block,
        ThreadContext context, IRubyObject self) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method, block);
        }
        entry = setCache(entry, self);
        return method.call(context, self, entry.sourceModule, methodName, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method, arg);
        }
        entry = setCache(entry, self);
        return method.call(context, self, entry.sourceModule, methodName, arg);
    }

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block,
        ThreadContext context, IRubyObject self, IRubyObject arg) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method, arg, block);
        }
        entry = setCache(entry, self);
        return method.call(context, self, entry.sourceModule, methodName, arg, block);
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method, arg1, arg2);
        }
        entry = setCache(entry, self);
        return method.call(context, self, entry.sourceModule, methodName, arg1, arg2);
    }

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block,
        ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method, arg1, arg2, block);
        }
        entry = setCache(entry, self);
        return method.call(context, self, entry.sourceModule, methodName, arg1, arg2, block);
    }

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType,
        ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method, arg1, arg2, arg3);
        }
        entry = setCache(entry, self);
        return method.call(context, self, entry.sourceModule, methodName, arg1, arg2, arg3);
    }

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block,
        ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method, arg1, arg2, arg3, block);
        }
        entry = setCache(entry, self);
        return method.call(context, self, entry.sourceModule, methodName, arg1, arg2, arg3, block);
    }

    protected final IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass selfType,
                                                  DynamicMethod method, IRubyObject[] args) {
        return Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType).call(context, self, selfType, methodName, args, Block.NULL_BLOCK);
    }

    protected final IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass selfType,
                                                  DynamicMethod method) {
        return Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType).call(context, self, selfType, methodName, Block.NULL_BLOCK);
    }

    protected final IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass selfType,
                                                  DynamicMethod method, Block block) {
        return Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType).call(context, self, selfType, methodName, block);
    }

    protected final IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass selfType,
                                                  DynamicMethod method, IRubyObject arg) {
        return Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType).call(context, self, selfType, methodName, arg, Block.NULL_BLOCK);
    }

    protected final IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass selfType,
                                                  DynamicMethod method, IRubyObject[] args, Block block) {
        return Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType).call(context, self, selfType, methodName, args, block);
    }

    protected final IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass selfType,
                                                  DynamicMethod method, IRubyObject arg0, Block block) {
        return Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType).call(context, self, selfType, methodName, arg0, block);
    }

    protected final IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass selfType,
                                                  DynamicMethod method, IRubyObject arg0, IRubyObject arg1) {
        return Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType).call(context, self, selfType, methodName, arg0, arg1, Block.NULL_BLOCK);
    }

    protected final IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass selfType,
                                                  DynamicMethod method, IRubyObject arg0, IRubyObject arg1, Block block) {
        return Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType).call(context, self, selfType, methodName, arg0, arg1, block);
    }

    protected final IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass selfType,
                                                  DynamicMethod method, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType).call(context, self, selfType, methodName, arg0, arg1, arg2, Block.NULL_BLOCK);
    }

    protected final IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass selfType,
                                                  DynamicMethod method, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType).call(context, self, self.getMetaClass(), methodName, arg0, arg1, arg2, block);
    }

    protected boolean methodMissing(DynamicMethod method, IRubyObject caller) {
        return method.isUndefined() || (!methodName.equals("method_missing") && !method.isCallableFrom(caller, callType));
    }

    protected static RubyClass getClass(IRubyObject self) {
        // the cast in the following line is necessary due to lacking optimizations in Hotspot
        return getMetaClass(self);
    }
}
