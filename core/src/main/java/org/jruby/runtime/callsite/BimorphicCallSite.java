package org.jruby.runtime.callsite;

import org.jruby.RubyClass;
import org.jruby.runtime.CallType;

/**
 * A bi-morphic call-site.
 *
 * @note used as a base for mixed Fixnum/Float ops
 */
abstract class BimorphicCallSite extends CachingCallSite {

    protected CacheEntry secondaryCache = CacheEntry.NULL_CACHE;

    public BimorphicCallSite(String methodName) {
        super(methodName, CallType.NORMAL);
    }

    public final CacheEntry getSecondaryCache() {
        return secondaryCache;
    }

    public final CacheEntry retrieveSecondaryCache(RubyClass selfType) {
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.secondaryCache;
        if (cache.typeOk(selfType)) {
            return cache;
        }
        return cacheAndGetSecondary(selfType, methodName);
    }

    public final CacheEntry retrieveSecondaryCache(RubyClass selfType, String methodName) {
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.secondaryCache;
        if (cache.typeOk(selfType)) {
            return cache;
        }
        return cacheAndGetSecondary(selfType, methodName);
    }

    public final boolean isSecondaryBuiltin(RubyClass selfType) {
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.secondaryCache;
        if (cache.typeOk(selfType)) {
            return cache.method.isBuiltin();
        }
        return cacheAndGetSecondary(selfType, methodName).method.isBuiltin(); // false for method.isUndefined()
    }

    private CacheEntry cacheAndGetSecondary(RubyClass selfType, String methodName) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        if (!entry.method.isUndefined()) this.secondaryCache = entry;
        return entry;
    }

}
