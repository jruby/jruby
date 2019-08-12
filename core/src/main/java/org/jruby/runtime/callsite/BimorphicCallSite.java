package org.jruby.runtime.callsite;

import org.jruby.RubyClass;
import org.jruby.runtime.CallType;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.RubyBasicObject.getMetaClass;

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

    protected CacheEntry setSecondaryCache(CacheEntry entry, IRubyObject self) {
        return secondaryCache = entry;
    }

    public final CacheEntry retrieveSecondaryCache(IRubyObject self) {
        RubyClass selfType = getMetaClass(self);
        CacheEntry cache = this.secondaryCache;
        if (cache.typeOk(selfType)) {
            return cache;
        }
        return cacheAndGetSecondary(self, selfType, methodName);
    }

    public boolean isSecondaryBuiltin(IRubyObject self) {
        RubyClass selfType = getMetaClass(self);
        CacheEntry cache = this.secondaryCache;
        if (cache.typeOk(selfType)) {
            return cache.method.isBuiltin();
        }
        return cacheAndGetSecondary(self, selfType, methodName).method.isBuiltin(); // false for method.isUndefined()
    }

    private CacheEntry cacheAndGetSecondary(IRubyObject self, RubyClass selfType, String methodName) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        if (!entry.method.isUndefined()) entry = setSecondaryCache(entry, self);
        return entry;
    }

}
