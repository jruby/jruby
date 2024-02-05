package org.jruby.runtime.callsite;

import org.jruby.RubyClass;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.IRScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.Helpers;
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
            method = Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType);
        }

        return method.call(context, self, entry.sourceModule, methodName, args);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, int callInfo, IRubyObject... args) {
        RubyClass selfType = getClass(self);
        CacheEntry entry = selfType.searchWithRefinements(methodName, scope);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            method = Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType);
        }

        return method.call(context, self, entry.sourceModule, methodName, callInfo, args);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        RubyClass selfType = getClass(self);
        CacheEntry entry = selfType.searchWithRefinements(methodName, scope);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            method = Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType);
        }

        return method.call(context, self, entry.sourceModule, methodName, args, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, int callInfo, Block block, IRubyObject[] args) {
        RubyClass selfType = getClass(self);
        CacheEntry entry = selfType.searchWithRefinements(methodName, scope);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            method = Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType);
        }

        return method.call(context, self, entry.sourceModule, methodName, callInfo, block, args);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self) {
        RubyClass selfType = getClass(self);
        CacheEntry entry = selfType.searchWithRefinements(methodName, scope);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            method = Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType);
        }

        return method.call(context, self, entry.sourceModule, methodName);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        RubyClass selfType = getClass(self);
        CacheEntry entry = selfType.searchWithRefinements(methodName, scope);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            method = Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType);
        }

        return method.call(context, self, entry.sourceModule, methodName, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0) {
        RubyClass selfType = getClass(self);
        CacheEntry entry = selfType.searchWithRefinements(methodName, scope);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            method = Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType);
        }

        return method.call(context, self, entry.sourceModule, methodName, arg0);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, Block block) {
        RubyClass selfType = getClass(self);
        CacheEntry entry = selfType.searchWithRefinements(methodName, scope);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            method = Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType);
        }

        return method.call(context, self, entry.sourceModule, methodName, arg0, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1) {
        RubyClass selfType = getClass(self);
        CacheEntry entry = selfType.searchWithRefinements(methodName, scope);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            method = Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType);
        }

        return method.call(context, self, entry.sourceModule, methodName, arg0, arg1);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) {
        RubyClass selfType = getClass(self);
        CacheEntry entry = selfType.searchWithRefinements(methodName, scope);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            method = Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType);
        }

        return method.call(context, self, entry.sourceModule, methodName, arg0, arg1, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        RubyClass selfType = getClass(self);
        CacheEntry entry = selfType.searchWithRefinements(methodName, scope);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            method = Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType);
        }

        return method.call(context, self, entry.sourceModule, methodName, arg0, arg1, arg2);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        RubyClass selfType = getClass(self);
        CacheEntry entry = selfType.searchWithRefinements(methodName, scope);
        DynamicMethod method = entry.method;

        if (methodMissing(method, caller)) {
            method = Helpers.selectMethodMissing(context, selfType, method.getVisibility(), methodName, callType);
        }

        return method.call(context, self, entry.sourceModule, methodName, arg0, arg1, arg2, block);
    }

    @Override
    protected boolean methodMissing(DynamicMethod method, IRubyObject caller) {
        // doing full "normal" MM check rather than multiple refined sites by call types
        return method.isUndefined() || (!methodName.equals("method_missing") && !method.isCallableFrom(caller, callType));
    }
}
