package org.jruby.runtime.callsite;

import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.internal.runtime.AbstractIRMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.IRScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;


/* ENEBO:
  I wanted to completely decouple of CachingCallSite so not influence it's performance but current JIT assumes it
  as the base type for a JIT'd method.  Notes:

  1. isBuiltin or retrieveCache usage.  We want mono calls to these but I think in this case none of these sites use
  blocks.  If so then there may be an issue if there is a second cache object in this type (since we will stop using
  the one in cachingcallsite.  Let's try this and see where we land.  Ultimately, this may lead to some refactoring.
  2. Lots of duplicated code here.  I think from JIT perspective these types will always be a single type so things
  should still inline ok.

 */

/**
 * An interesting callsite which we will look for monomorphic behavior.
 */
public class ProfilingCachingCallSite extends CachingCallSite {
    protected CacheEntry cache = CacheEntry.NULL_CACHE;
    public volatile int totalMonomorphicCalls = 0;
    public volatile int totalTypeChanges = -1;
    private final IRScope scope;

    public ProfilingCachingCallSite(String methodName, IRScope scope) {
        super(methodName, CallType.NORMAL);

        this.scope = scope;
    }

    protected boolean methodMissing(DynamicMethod method, IRubyObject caller) {
        return method.isUndefined() || (!methodName.equals("method_missing") && !method.isCallableFrom(caller, callType));
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
        CacheEntry cache = this.cache;  // This must be retrieved *once* to avoid racing with other threads.

        if (cache.typeOk(selfType)) {
            if ((totalMonomorphicCalls++ % 10) == 0) printCallsiteData(context, caller, self, args, cache);
            return cache.method.call(context, self, selfType, methodName, args);
        } else {
            totalMonomorphicCalls = 1;
            return cacheAndCall(caller, selfType, args, context, self);
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        RubyClass selfType = getClass(self);
        CacheEntry cache = this.cache; // This must be retrieved *once* to avoid racing with other threads.

        if (cache.typeOk(selfType)) {
            if ((totalMonomorphicCalls++ % 10) == 0) printCallsiteData(context, caller, self, args, cache);
            return cache.method.call(context, self, selfType, methodName, args, block);
        } else {
            totalMonomorphicCalls = 1;
            return cacheAndCall(caller, selfType, block, args, context, self);
        }
    }

    private void printCallsiteData(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, CacheEntry cache) {
        if (cache.method instanceof AbstractIRMethod) { // we only care about mono calls to IR
            System.err.println("PROFILE: " + scope + " -> " + self.getMetaClass().rubyName() + "#" + methodName + " - " + totalMonomorphicCalls);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if ((totalMonomorphicCalls++ % 10) == 0) printCallsiteData(context, caller, self, IRubyObject.NULL_ARRAY, cache);
            return cache.method.call(context, self, selfType, methodName);
        } else {
            totalMonomorphicCalls = 1;
            return cacheAndCall(caller, selfType, context, self);
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if ((totalMonomorphicCalls++ % 10) == 0) printCallsiteData(context, caller, self, IRubyObject.NULL_ARRAY, cache);
            return cache.method.call(context, self, selfType, methodName, block);
        } else {
            totalMonomorphicCalls = 1;
            return cacheAndCall(caller, selfType, block, context, self);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if ((totalMonomorphicCalls++ % 10) == 0) printCallsiteData(context, caller, self, new IRubyObject[] { arg1 }, cache);
            return cache.method.call(context, self, selfType, methodName, arg1);
        } else {
            totalMonomorphicCalls = 1;
            return cacheAndCall(caller, selfType, context, self, arg1);
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if ((totalMonomorphicCalls++ % 10) == 0) printCallsiteData(context, caller, self, new IRubyObject[] { arg1 }, cache);
            return cache.method.call(context, self, selfType, methodName, arg1, block);
        } else {
            totalMonomorphicCalls = 1;
            return cacheAndCall(caller, selfType, block, context, self, arg1);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if ((totalMonomorphicCalls++ % 10) == 0) printCallsiteData(context, caller, self, new IRubyObject[] { arg1, arg2 }, cache);
            return cache.method.call(context, self, selfType, methodName, arg1, arg2);
        } else {
            totalMonomorphicCalls = 1;
            return cacheAndCall(caller, selfType, context, self, arg1, arg2);
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if ((totalMonomorphicCalls++ % 10) == 0) printCallsiteData(context, caller, self, new IRubyObject[] { arg1, arg2 }, cache);
            return cache.method.call(context, self, selfType, methodName, arg1, arg2, block);
        } else {
            totalMonomorphicCalls = 1;
            return cacheAndCall(caller, selfType, block, context, self, arg1, arg2);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if ((totalMonomorphicCalls++ % 10) == 0) printCallsiteData(context, caller, self, new IRubyObject[] { arg1, arg2, arg3 }, cache);
            return cache.method.call(context, self, selfType, methodName, arg1, arg2, arg3);
        } else {
            totalMonomorphicCalls = 1;
            return cacheAndCall(caller, selfType, context, self, arg1, arg2, arg3);
        }
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if ((totalMonomorphicCalls++ % 10) == 0) printCallsiteData(context, caller, self, new IRubyObject[] { arg1, arg2, arg3 }, cache);
            return cache.method.call(context, self, selfType, methodName, arg1, arg2, arg3, block);
        } else {
            totalMonomorphicCalls = 1;
            return cacheAndCall(caller, selfType, block, context, self, arg1, arg2, arg3);
        }
    }

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block,
                                     IRubyObject[] args, ThreadContext context, IRubyObject self) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        totalTypeChanges++;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, args, block);
        } else {
            cache = entry;
            return method.call(context, self, selfType, methodName, args, block);
        }
    }

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType,
                                     IRubyObject[] args, ThreadContext context, IRubyObject self) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        totalTypeChanges++;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, args);
        } else {
            cache = entry;
            return method.call(context, self, selfType, methodName, args);
        }
    }

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType,
                                     ThreadContext context, IRubyObject self) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        totalTypeChanges++;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method);
        } else {
            cache = entry;
            return method.call(context, self, selfType, methodName);
        }
    }

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block,
                                     ThreadContext context, IRubyObject self) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        totalTypeChanges++;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, block);
        } else {
            cache = entry;
            return method.call(context, self, selfType, methodName, block);
        }
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        totalTypeChanges++;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg);
        } else {
            cache = entry;
            return method.call(context, self, selfType, methodName, arg);
        }
    }

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block,
                                     ThreadContext context, IRubyObject self, IRubyObject arg) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        totalTypeChanges++;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg, block);
        } else {
            cache = entry;
            return method.call(context, self, selfType, methodName, arg, block);
        }
    }

    protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        totalTypeChanges++;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg1, arg2);
        } else {
            cache = entry;
            return method.call(context, self, selfType, methodName, arg1, arg2);
        }
    }

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block,
                                     ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        totalTypeChanges++;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg1, arg2, block);
        } else {
            cache = entry;
            return method.call(context, self, selfType, methodName, arg1, arg2, block);
        }
    }

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType,
                                     ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2,
                                     IRubyObject arg3) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        totalTypeChanges++;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg1, arg2, arg3);
        } else {
            cache = entry;
            return method.call(context, self, selfType, methodName, arg1, arg2, arg3);
        }
    }

    private IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block,
                                     ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2,
                                     IRubyObject arg3) {
        CacheEntry entry = selfType.searchWithCache(methodName);
        DynamicMethod method = entry.method;
        totalTypeChanges++;
        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg1, arg2, arg3, block);
        } else {
            cache = entry;
            return method.call(context, self, selfType, methodName, arg1, arg2, arg3, block);
        }
    }
}
