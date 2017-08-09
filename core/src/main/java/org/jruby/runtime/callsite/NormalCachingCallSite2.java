package org.jruby.runtime.callsite;

import org.jruby.RubyClass;

/**
 * Kind of a polymorphic call-site that needs to be managed manually.
 *
 * @note For now only used as a base class for mixed Fixnum/Float ops.
 */
abstract class NormalCachingCallSite2 extends NormalCachingCallSite {

    protected CacheEntry cache2 = CacheEntry.NULL_CACHE;

    public NormalCachingCallSite2(String methodName) {
        super(methodName);
    }

    public final CacheEntry getCache2() {
        return cache2;
    }

    public final CacheEntry retrieveCache2(RubyClass selfType) {
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache2;
        if (cache.typeOk(selfType)) {
            return cache;
        }
        return cacheAndGet2(selfType, methodName);
    }

    public final CacheEntry retrieveCache2(RubyClass selfType, String methodName) {
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache2;
        if (cache.typeOk(selfType)) {
            return cache;
        }
        return cacheAndGet2(selfType, methodName);
    }

    public final boolean isBuiltin2(RubyClass selfType) {
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache2;
        if (cache.typeOk(selfType)) {
            return cache.method.isBuiltin();
        }
        return cacheAndGet2(selfType, methodName).method.isBuiltin(); // false for method.isUndefined()
    }

    private CacheEntry cacheAndGet2(RubyClass selfType, String methodName) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        if (!entry.method.isUndefined()) this.cache2 = entry;
        return entry;
    }

}
