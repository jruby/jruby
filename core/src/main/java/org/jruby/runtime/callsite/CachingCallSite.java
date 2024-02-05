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
        return cacheAndCall(context, caller, self, selfType, args);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, int callInfo, IRubyObject... args) {
        RubyClass selfType = getMetaClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, cache.sourceModule, methodName, callInfo, args);
        }
        return cacheAndCall(context, caller, self, selfType, callInfo, args);
    }

    public IRubyObject fcall(ThreadContext context, IRubyObject self, IRubyObject... args) {
        return call(context, self, self, args);
    }

    public IRubyObject fcall(ThreadContext context, IRubyObject self, int callInfo, IRubyObject... args) {
        return call(context, self, self, callInfo, args);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        RubyClass selfType = getMetaClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, cache.sourceModule, methodName, args, block);
        }
        return cacheAndCall(context, caller, self, selfType, args, block);
    }

    public IRubyObject fcall(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return call(context, self, self, args, block);
    }

    public IRubyObject fcall(ThreadContext context, IRubyObject self, int fcall, Block block, IRubyObject... args) {
        return call(context, self, self, fcall, block, args);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, int callInfo, Block block, IRubyObject... args) {
        RubyClass selfType = getMetaClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, cache.sourceModule, methodName, callInfo, block, args);
        }
        return cacheAndCall(context, caller, self, selfType, callInfo, block, args);
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
    public IRubyObject callIter(ThreadContext context, IRubyObject caller, IRubyObject self,
                                int callInfo, Block block, IRubyObject... args) {
        try {
            return call(context, caller, self, callInfo, block, args);
        } finally {
            block.escape();
        }
    }

    public IRubyObject fcallIter(ThreadContext context, IRubyObject self,
                                int callInfo, Block block, IRubyObject[] args) {
        try {
            return call(context, self, self, callInfo, block, args);
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

    public final IRubyObject fcallVarargs(ThreadContext context,
                                         IRubyObject self, IRubyObject... args) {
        switch (args.length) {
            case 0: return call(context, self, self);
            case 1: return call(context, self, self, args[0]);
            case 2: return call(context, self, self, args[0], args[1]);
            case 3: return call(context, self, self, args[0], args[1], args[2]);
            default: return call(context, self, self, args);
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

    public final IRubyObject fcallVarargs(ThreadContext context,
                                         IRubyObject self, IRubyObject[] args, Block block) {
        switch (args.length) {
            case 0: return call(context, self, self, block);
            case 1: return call(context, self, self, args[0], block);
            case 2: return call(context, self, self, args[0], args[1], block);
            case 3: return call(context, self, self, args[0], args[1], args[2], block);
            default: return call(context, self, self, args, block);
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

    public final IRubyObject fcallVarargsIter(ThreadContext context,
                                             IRubyObject self, IRubyObject[] args, Block block) {
        switch (args.length) {
            case 0: return callIter(context, self, self, block);
            case 1: return callIter(context, self, self, args[0], block);
            case 2: return callIter(context, self, self, args[0], args[1], block);
            case 3: return callIter(context, self, self, args[0], args[1], args[2], block);
            default: return callIter(context, self, self, args, block);
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
        return cacheAndCall(context, caller, self, selfType);
    }

    public IRubyObject fcall(ThreadContext context, IRubyObject self) {
        return call(context, self, self);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        RubyClass selfType = getMetaClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, cache.sourceModule, methodName, block);
        }
        return cacheAndCall(context, caller, self, selfType, block);
    }

    public IRubyObject fcall(ThreadContext context, IRubyObject self, Block block) {
        return call(context, self, self, block);
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

    public final IRubyObject fcallIter(ThreadContext context, IRubyObject self,
                                      Block block) {
        try {
            return call(context, self, self, block);
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
        return cacheAndCall(context, caller, self, selfType, arg1);
    }

    public IRubyObject fcall(ThreadContext context, IRubyObject self, IRubyObject arg1) {
        return call(context, self, self, arg1);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block) {
        RubyClass selfType = getMetaClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, cache.sourceModule, methodName, arg1, block);
        }
        return cacheAndCall(context, caller, self, selfType, arg1, block);
    }

    public IRubyObject fcall(ThreadContext context, IRubyObject self, IRubyObject arg1, Block block) {
        return call(context, self, self, arg1, block);
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

    public IRubyObject fcallIter(ThreadContext context, IRubyObject self,
                                IRubyObject arg1, Block block) {
        try {
            return call(context, self, self, arg1, block);
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
        return cacheAndCall(context, caller, self, selfType, arg1, arg2);
    }

    public IRubyObject fcall(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
        return call(context, self, self, arg1, arg2);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        RubyClass selfType = getMetaClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, cache.sourceModule, methodName, arg1, arg2, block);
        }
        return cacheAndCall(context, caller, self, selfType, arg1, arg2, block);
    }

    public IRubyObject fcall(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        return call(context, self, self, arg1, arg2, block);
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

    public final IRubyObject fcallIter(ThreadContext context, IRubyObject self,
                                      IRubyObject arg1, IRubyObject arg2, Block block) {
        try {
            return call(context, self, self, arg1, arg2, block);
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
        return cacheAndCall(context, caller, self, selfType, arg1, arg2, arg3);
    }

    public IRubyObject fcall(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return call(context, self, self, arg1, arg2, arg3);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        RubyClass selfType = getMetaClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            return cache.method.call(context, self, cache.sourceModule, methodName, arg1, arg2, arg3, block);
        }
        return cacheAndCall(context, caller, self, selfType, block, arg1, arg2, arg3);
    }

    public IRubyObject fcall(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        return call(context, self, self, arg1, arg2, arg3, block);
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

    public final IRubyObject fcallIter(ThreadContext context, IRubyObject self,
                                      IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        try {
            return call(context, self, self, arg1, arg2, arg3, block);
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

    protected IRubyObject cacheAndCall(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass selfType, IRubyObject[] args, Block block) {
        CacheEntry entry = populateCacheEntry(caller, selfType, context, self);
        return entry.method.call(context, self, entry.sourceModule, methodName, args, block);
    }

    protected IRubyObject cacheAndCall(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass selfType, IRubyObject[] args) {
        CacheEntry entry = populateCacheEntry(caller, selfType, context, self);
        return entry.method.call(context, self, entry.sourceModule, methodName, args);
    }

    protected IRubyObject cacheAndCall(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass selfType, int callInfo, Block block, IRubyObject... args) {
        CacheEntry entry = populateCacheEntry(caller, selfType, context, self);
        return entry.method.call(context, self, entry.sourceModule, methodName, callInfo, block, args);
    }

    protected IRubyObject cacheAndCall(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass selfType) {
        CacheEntry entry = populateCacheEntry(caller, selfType, context, self);
        return entry.method.call(context, self, entry.sourceModule, methodName);
    }

    protected IRubyObject cacheAndCall(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass selfType, int callInfo, IRubyObject[] args) {
        CacheEntry entry = populateCacheEntry(caller, selfType, context, self);
        return entry.method.call(context, self, entry.sourceModule, methodName, callInfo, args);
    }

    protected IRubyObject cacheAndCall(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass selfType, Block block) {
        CacheEntry entry = populateCacheEntry(caller, selfType, context, self);
        return entry.method.call(context, self, entry.sourceModule, methodName, block);
    }

    protected IRubyObject cacheAndCall(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass selfType, IRubyObject arg) {
        CacheEntry entry = populateCacheEntry(caller, selfType, context, self);
        return entry.method.call(context, self, entry.sourceModule, methodName, arg);
    }

    protected IRubyObject cacheAndCall(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass selfType, IRubyObject arg, Block block) {
        CacheEntry entry = populateCacheEntry(caller, selfType, context, self);
        return entry.method.call(context, self, entry.sourceModule, methodName, arg, block);
    }

    protected IRubyObject cacheAndCall(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass selfType, IRubyObject arg1, IRubyObject arg2) {
        CacheEntry entry = populateCacheEntry(caller, selfType, context, self);
        return entry.method.call(context, self, entry.sourceModule, methodName, arg1, arg2);
    }

    protected IRubyObject cacheAndCall(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass selfType, IRubyObject arg1, IRubyObject arg2, Block block) {
        CacheEntry entry = populateCacheEntry(caller, selfType, context, self);
        return entry.method.call(context, self, entry.sourceModule, methodName, arg1, arg2, block);
    }

    protected IRubyObject cacheAndCall(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass selfType, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        CacheEntry entry = populateCacheEntry(caller, selfType, context, self);
        return entry.method.call(context, self, entry.sourceModule, methodName, arg1, arg2, arg3);
    }

    protected IRubyObject cacheAndCall(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass selfType, Block block, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        CacheEntry entry = populateCacheEntry(caller, selfType, context, self);
        return entry.method.call(context, self, entry.sourceModule, methodName, arg1, arg2, arg3, block);
    }

    private CacheEntry populateCacheEntry(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            entry = Helpers.createMethodMissingEntry(context, selfType, callType, method.getVisibility(), entry.token, methodName);
        }

        entry = setCache(entry, self);
        return entry;
    }

    protected boolean methodMissing(DynamicMethod method, IRubyObject caller) {
        return method.isUndefined() || (!methodName.equals("method_missing") && !method.isCallableFrom(caller, callType));
    }

    protected static RubyClass getClass(IRubyObject self) {
        // the cast in the following line is necessary due to lacking optimizations in Hotspot
        return getMetaClass(self);
    }
}
