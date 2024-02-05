package org.jruby.runtime.callsite;

import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.AbstractIRMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.internal.runtime.methods.MixedModeIRMethod;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * Note: I originally had a totalType changes thinking that we did not want to churn through
 * repeated inlines if the type is changing a "little bit".  It should give up.  This is premature
 * as hostScope only allows a single inline and we have no deoptimization yet.  So it is a candidate
 * again once the opt/deopt system exists and we can contemplate multiple speculative optimizations.
 */

/**
 * An interesting callsite in which we will look for monomorphic behavior in case we want to inline.
 */
public class ProfilingCachingCallSite extends CachingCallSite {
    public static final Logger LOG = LoggerFactory.getLogger(ProfilingCachingCallSite.class);
    private final AtomicInteger totalMonomorphicCalls = new AtomicInteger(0);
    private final IRScope hostScope;
    private final long callSiteId;

    public ProfilingCachingCallSite(CallType callType, String methodName, IRScope scope, long callSiteId) {
        super(methodName, callType);

        this.hostScope = scope;
        this.callSiteId = callSiteId;
    }

    private void inlineCheck(ThreadContext context, IRubyObject self, CacheEntry cache) {
        // Either host has already failed to inline or the scope has decided this is not an elligble host for inlining.
        if (!hostScope.inliningAllowed()) return;

        // CompiledIRMethod* is not supported
        boolean targetIsIR = cache.method instanceof AbstractIRMethod;
        boolean siteIsIR = hostScope.compilable != null;

        AbstractIRMethod targetMethod;
        if (!targetIsIR) {
            // Hope is that this will load ruby version and next inlineCheck use the ruby version instead.
            if (self instanceof RubyFixnum && "times".equals(methodName)) {
                targetIsIR = true;
                targetMethod = new MixedModeIRMethod(context.runtime.getIRManager().loadInternalMethod(context, self, "times"), cache.method.getVisibility(), cache.method.getImplementationClass());
            } else {
                targetMethod = null;
            }
        } else {
            targetMethod = (AbstractIRMethod) cache.method;
        }

        if (targetIsIR && siteIsIR) {
            IRMethod scopeToInline = (IRMethod) (targetMethod).getIRScope();

            if (IRManager.IR_INLINER_VERBOSE) LOG.info("PROFILE: " + hostScope + " -> " + scopeToInline + " - " + totalMonomorphicCalls);

            RubyModule metaClass = self.getMetaClass();
            AbstractIRMethod hostMethod = (AbstractIRMethod) hostScope.compilable;
            if (hostMethod instanceof InterpretedIRMethod) {
                hostScope.inlineMethod(scopeToInline, metaClass, callSiteId, cache.token, false);
            } else if (hostMethod instanceof MixedModeIRMethod) {
                hostScope.inlineMethodJIT(scopeToInline, metaClass, callSiteId, cache.token, false);
            } else {
                hostScope.inlineMethodCompiled(scopeToInline, metaClass, callSiteId, cache.token, false);
            }
        }
    }

    @Override
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
            if ((totalMonomorphicCalls.incrementAndGet() % IRManager.IR_INLINER_THRESHOLD) == 0) inlineCheck(context, self, cache);
            return cache.method.call(context, self, cache.sourceModule, methodName, args);
        } else {
            totalMonomorphicCalls.set(1);
            return cacheAndCall(context, caller, self, selfType, args);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, int callInfo, IRubyObject... args) {
        RubyClass selfType = getClass(self);
        CacheEntry cache = this.cache;  // This must be retrieved *once* to avoid racing with other threads.

        if (cache.typeOk(selfType)) {
            if ((totalMonomorphicCalls.incrementAndGet() % IRManager.IR_INLINER_THRESHOLD) == 0) inlineCheck(context, self, cache);
            return cache.method.call(context, self, cache.sourceModule, methodName, callInfo, args);
        } else {
            totalMonomorphicCalls.set(1);
            return cacheAndCall(context, caller, self, selfType, callInfo, args);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        RubyClass selfType = getClass(self);
        CacheEntry cache = this.cache; // This must be retrieved *once* to avoid racing with other threads.

        if (cache.typeOk(selfType)) {
            if ((totalMonomorphicCalls.incrementAndGet() % IRManager.IR_INLINER_THRESHOLD) == 0) inlineCheck(context, self, cache);
            return cache.method.call(context, self, cache.sourceModule, methodName, args, block);
        } else {
            totalMonomorphicCalls.set(1);
            return cacheAndCall(context, caller, self, selfType, args, block);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, int callInfo, Block block, IRubyObject... args) {
        RubyClass selfType = getClass(self);
        CacheEntry cache = this.cache; // This must be retrieved *once* to avoid racing with other threads.

        if (cache.typeOk(selfType)) {
            if ((totalMonomorphicCalls.incrementAndGet() % IRManager.IR_INLINER_THRESHOLD) == 0) inlineCheck(context, self, cache);
            return cache.method.call(context, self, cache.sourceModule, methodName, callInfo, block, args);
        } else {
            totalMonomorphicCalls.set(1);
            return cacheAndCall(context, caller, self, selfType, callInfo, block, args);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if ((totalMonomorphicCalls.incrementAndGet() % IRManager.IR_INLINER_THRESHOLD) == 0) inlineCheck(context, self, cache);
            return cache.method.call(context, self, cache.sourceModule, methodName);
        } else {
            totalMonomorphicCalls.set(1);
            return cacheAndCall(context, caller, self, selfType);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if ((totalMonomorphicCalls.incrementAndGet() % IRManager.IR_INLINER_THRESHOLD) == 0) inlineCheck(context, self, cache);
            return cache.method.call(context, self, cache.sourceModule, methodName, block);
        } else {
            totalMonomorphicCalls.set(1);
            return cacheAndCall(context, caller, self, selfType, block);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if ((totalMonomorphicCalls.incrementAndGet() % IRManager.IR_INLINER_THRESHOLD) == 0) inlineCheck(context, self, cache);
            return cache.method.call(context, self, cache.sourceModule, methodName, arg1);
        } else {
            totalMonomorphicCalls.set(1);
            return cacheAndCall(context, caller, self, selfType, arg1);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if ((totalMonomorphicCalls.incrementAndGet() % IRManager.IR_INLINER_THRESHOLD) == 0) inlineCheck(context, self, cache);
            return cache.method.call(context, self, cache.sourceModule, methodName, arg1, block);
        } else {
            totalMonomorphicCalls.set(1);
            return cacheAndCall(context, caller, self, selfType, arg1, block);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if ((totalMonomorphicCalls.incrementAndGet() % IRManager.IR_INLINER_THRESHOLD) == 0) inlineCheck(context, self, cache);
            return cache.method.call(context, self, cache.sourceModule, methodName, arg1, arg2);
        } else {
            totalMonomorphicCalls.set(1);
            return cacheAndCall(context, caller, self, selfType, arg1, arg2);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if ((totalMonomorphicCalls.incrementAndGet() % IRManager.IR_INLINER_THRESHOLD) == 0) inlineCheck(context, self, cache);
            return cache.method.call(context, self, cache.sourceModule, methodName, arg1, arg2, block);
        } else {
            totalMonomorphicCalls.set(1);
            return cacheAndCall(context, caller, self, selfType, arg1, arg2, block);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if ((totalMonomorphicCalls.incrementAndGet() % IRManager.IR_INLINER_THRESHOLD) == 0) inlineCheck(context, self, cache);
            return cache.method.call(context, self, cache.sourceModule, methodName, arg1, arg2, arg3);
        } else {
            totalMonomorphicCalls.set(1);
            return cacheAndCall(context, caller, self, selfType, arg1, arg2, arg3);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        RubyClass selfType = getClass(self);
        // This must be retrieved *once* to avoid racing with other threads.
        CacheEntry cache = this.cache;
        if (cache.typeOk(selfType)) {
            if ((totalMonomorphicCalls.incrementAndGet() % IRManager.IR_INLINER_THRESHOLD) == 0) inlineCheck(context, self, cache);
            return cache.method.call(context, self, cache.sourceModule, methodName, arg1, arg2, arg3, block);
        } else {
            totalMonomorphicCalls.set(1);
            return cacheAndCall(context, caller, self, selfType, block, arg1, arg2, arg3);
        }
    }
}
