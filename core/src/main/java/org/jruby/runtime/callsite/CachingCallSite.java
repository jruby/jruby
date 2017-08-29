package org.jruby.runtime.callsite;

import org.jruby.RubyBasicObject;
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

public abstract class CachingCallSite extends CallSite {
    protected CacheEntry cache = CacheEntry.NULL_CACHE;
    //public static volatile int totalCallSites;
    //private AtomicBoolean isPolymorphic = new AtomicBoolean(false);

    public CachingCallSite(String methodName, CallType callType) {
        super(methodName, callType);
        //totalCallSites++;
    }

    public final CacheEntry getCache() {
        return cache;
    }

    public final boolean isOptimizable() {
        return cache != CacheEntry.NULL_CACHE;// && !isPolymorphic.get();
    }

    public final int getCachedClassIndex() {
        CacheEntry cacheEntry = cache;
        if (cacheEntry != CacheEntry.NULL_CACHE) {
            return cacheEntry.method.getImplementationClass().getClassIndex().ordinal();
        }
        return ClassIndex.NO_INDEX.ordinal();
    }

    public final String getMethodName() {
        return methodName;
    }

    public final long getCachedMethodSerial() {
        CacheEntry cacheEntry = cache;
        if (cacheEntry != CacheEntry.NULL_CACHE) {
            return cacheEntry.method.getSerialNumber();
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
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, selfType, methodName, args);
        }
        return cacheAndCall(caller, selfType, args, context, self);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, selfType, methodName, args, block);
        }
        return cacheAndCall(caller, selfType, block, args, context, self);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        return callBlock(context, caller, self, args, block);
    }

    @Override
    public final IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self,
        IRubyObject[] args, Block block) {
        try {
            return callBlock(context, caller, self, args, block);
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
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, selfType, methodName);
        }
        return cacheAndCall(caller, selfType, context, self);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, selfType, methodName, block);
        }
        return cacheAndCall(caller, selfType, block, context, self);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        return callBlock(context, caller, self, block);
    }

    @Override
    public final IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self,
        Block block) {
        try {
            return callBlock(context, caller, self, block);
        } finally {
            block.escape();
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, selfType, methodName, arg1);
        }
        return cacheAndCall(caller, selfType, context, self, arg1);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, selfType, methodName, arg1, block);
        }
        return cacheAndCall(caller, selfType, block, context, self, arg1);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block) {
        return callBlock(context, caller, self, arg1, block);
    }

    @Override
    public final IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self,
        IRubyObject arg1, Block block) {
        try {
            return callBlock(context, caller, self, arg1, block);
        } finally {
            block.escape();
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, selfType, methodName, arg1, arg2);
        }
        return cacheAndCall(caller, selfType, context, self, arg1, arg2);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, selfType, methodName, arg1, arg2, block);
        }
        return cacheAndCall(caller, selfType, block, context, self, arg1, arg2);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        return callBlock(context, caller, self, arg1, arg2, block);
    }

    @Override
    public final IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self,
        IRubyObject arg1, IRubyObject arg2, Block block) {
        try {
            return callBlock(context, caller, self, arg1, arg2, block);
        } finally {
            block.escape();
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, selfType, methodName, arg1, arg2, arg3);
        }
        return cacheAndCall(caller, selfType, context, self, arg1, arg2, arg3);
    }

    private IRubyObject callBlock(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, selfType, methodName, arg1, arg2, arg3, block);
        }
        return cacheAndCall(caller, selfType, block, context, self, arg1, arg2, arg3);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        return callBlock(context, caller, self, arg1, arg2, arg3, block);
    }

    @Override
    public final IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self,
        IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        try {
            return callBlock(context, caller, self, arg1, arg2, arg3, block);
        } finally {
            block.escape();
        }
    }

    public final CacheEntry retrieveCache(RubyClass selfType) {
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache;
        }
        return cacheAndGet(selfType, methodName);
    }

    public final CacheEntry retrieveCache(RubyClass selfType, String methodName) {
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache;
        }
        return cacheAndGet(selfType, methodName);
    }

    public final boolean isBuiltin(RubyClass selfType) {
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.isBuiltin();
        }
        return cacheAndGetBuiltin(selfType, methodName);
    }

    private CacheEntry cacheAndGet(RubyClass selfType, String methodName) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        if (!entry.method.isUndefined()) cache = entry;
        return entry;
    }

    private static boolean cacheAndGetBuiltin(RubyClass selfType, String methodName) {
        final DynamicMethod method = selfType.searchWithCache(methodName).method;
        return method.isUndefined() && method.isBuiltin();
    }
    
    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block,
        IRubyObject[] args, ThreadContext context, IRubyObject self) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, args, block);
        }
        cache = entry;
        return method.call(context, self, selfType, methodName, args, block);
    }

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType,
        IRubyObject[] args, ThreadContext context, IRubyObject self) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, args);
        }
        cache = entry;
        return method.call(context, self, selfType, methodName, args);
    }

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType,
        ThreadContext context, IRubyObject self) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method);
        }
        cache = entry;
        return method.call(context, self, selfType, methodName);
    }

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block,
        ThreadContext context, IRubyObject self) {
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

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block,
        ThreadContext context, IRubyObject self, IRubyObject arg) {
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

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block,
        ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg1, arg2, block);
        }
        cache = entry;
        return method.call(context, self, selfType, methodName, arg1, arg2, block);
    }

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType,
        ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2,
        IRubyObject arg3) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg1, arg2, arg3);
        }
        cache = entry;
        return method.call(context, self, selfType, methodName, arg1, arg2, arg3);
    }

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block,
        ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2,
        IRubyObject arg3) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg1, arg2, arg3, block);
        }
        cache = entry;
        return method.call(context, self, selfType, methodName, arg1, arg2, arg3, block);
    }

    protected final IRubyObject callMethodMissing(ThreadContext context, IRubyObject self,
        DynamicMethod method, IRubyObject[] args) {
        return Helpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, self, self.getMetaClass(), methodName, args, Block.NULL_BLOCK);
    }

    protected final IRubyObject callMethodMissing(ThreadContext context, IRubyObject self,
        DynamicMethod method) {
        return Helpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, self, self.getMetaClass(), methodName, Block.NULL_BLOCK);
    }

    protected final IRubyObject callMethodMissing(ThreadContext context, IRubyObject self,
        DynamicMethod method, Block block) {
        return Helpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, self, self.getMetaClass(), methodName, block);
    }

    protected final IRubyObject callMethodMissing(ThreadContext context, IRubyObject self,
        DynamicMethod method, IRubyObject arg) {
        return Helpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, self, self.getMetaClass(), methodName, arg, Block.NULL_BLOCK);
    }

    protected final IRubyObject callMethodMissing(ThreadContext context, IRubyObject self,
        DynamicMethod method, IRubyObject[] args, Block block) {
        return Helpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, self, self.getMetaClass(), methodName, args, block);
    }

    protected final IRubyObject callMethodMissing(ThreadContext context, IRubyObject self,
        DynamicMethod method, IRubyObject arg0, Block block) {
        return Helpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, self, self.getMetaClass(), methodName, arg0, block);
    }

    protected final IRubyObject callMethodMissing(ThreadContext context, IRubyObject self,
        DynamicMethod method, IRubyObject arg0, IRubyObject arg1) {
        return Helpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, self, self.getMetaClass(), methodName, arg0, arg1, Block.NULL_BLOCK);
    }

    protected final IRubyObject callMethodMissing(ThreadContext context, IRubyObject self,
        DynamicMethod method, IRubyObject arg0, IRubyObject arg1, Block block) {
        return Helpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, self, self.getMetaClass(), methodName, arg0, arg1, block);
    }

    protected final IRubyObject callMethodMissing(ThreadContext context, IRubyObject self,
        DynamicMethod method, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return Helpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, self, self.getMetaClass(), methodName, arg0, arg1, arg2, Block.NULL_BLOCK);
    }

    protected final IRubyObject callMethodMissing(ThreadContext context, IRubyObject self,
        DynamicMethod method, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return Helpers.selectMethodMissing(context, self, method.getVisibility(), methodName, callType).call(context, self, self.getMetaClass(), methodName, arg0, arg1, arg2, block);
    }

    protected abstract boolean methodMissing(DynamicMethod method, IRubyObject caller);

    protected static RubyClass getClass(IRubyObject self) {
        // the cast in the following line is necessary due to lacking optimizations in Hotspot
        return ((RubyBasicObject) self).getMetaClass();
    }
}
