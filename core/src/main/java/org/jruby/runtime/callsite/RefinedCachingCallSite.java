package org.jruby.runtime.callsite;

import org.jruby.RubyClass;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.IRScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RefinedCachingCallSite extends CachingCallSite {
    private final StaticScope scope;

    public RefinedCachingCallSite(String methodName, StaticScope scope, CallType callType) {
        super(methodName, callType);

        this.scope = scope;
    }

    public RefinedCachingCallSite(String methodName, IRScope scope, CallType callType) {
        this(methodName, scope.getStaticScope(), callType);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject... args) {
        RubyClass selfType = getClass(self);
        CacheEntry entry = selfType.searchWithRefinements(methodName, scope);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method, args);
        }

        return method.call(context, self, entry.sourceModule, methodName, args);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        RubyClass selfType = getClass(self);
        CacheEntry entry = selfType.searchWithRefinements(methodName, scope);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method, args, block);
        }

        return method.call(context, self, entry.sourceModule, methodName, args, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self) {
        RubyClass selfType = getClass(self);
        CacheEntry entry = selfType.searchWithRefinements(methodName, scope);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method);
        }

        return method.call(context, self, entry.sourceModule, methodName);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        RubyClass selfType = getClass(self);
        CacheEntry entry = selfType.searchWithRefinements(methodName, scope);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method, block);
        }

        return method.call(context, self, entry.sourceModule, methodName, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0) {
        RubyClass selfType = getClass(self);
        CacheEntry entry = selfType.searchWithRefinements(methodName, scope);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method, arg0);
        }

        return method.call(context, self, entry.sourceModule, methodName, arg0);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, Block block) {
        RubyClass selfType = getClass(self);
        CacheEntry entry = selfType.searchWithRefinements(methodName, scope);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method, arg0, block);
        }

        return method.call(context, self, entry.sourceModule, methodName, arg0, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1) {
        RubyClass selfType = getClass(self);
        CacheEntry entry = selfType.searchWithRefinements(methodName, scope);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method, arg0, arg1);
        }

        return method.call(context, self, entry.sourceModule, methodName, arg0, arg1);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) {
        RubyClass selfType = getClass(self);
        CacheEntry entry = selfType.searchWithRefinements(methodName, scope);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method, arg0, arg1, block);
        }

        return method.call(context, self, entry.sourceModule, methodName, arg0, arg1, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        RubyClass selfType = getClass(self);
        CacheEntry entry = selfType.searchWithRefinements(methodName, scope);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method, arg0, arg1, arg2);
        }

        return method.call(context, self, entry.sourceModule, methodName, arg0, arg1, arg2);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        RubyClass selfType = getClass(self);
        CacheEntry entry = selfType.searchWithRefinements(methodName, scope);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, selfType, method, arg0, arg1, arg2, block);
        }

        return method.call(context, self, entry.sourceModule, methodName, arg0, arg1, arg2, block);
    }

    protected boolean methodMissing(DynamicMethod method, IRubyObject caller) {
        // doing full "normal" MM check rather than multiple refined sites by call types
        return method.isUndefined() || (!methodName.equals("method_missing") && !method.isCallableFrom(caller, callType));
    }
}
