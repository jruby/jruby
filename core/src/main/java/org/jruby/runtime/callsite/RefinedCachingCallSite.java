package org.jruby.runtime.callsite;

import org.jruby.RubyClass;
import org.jruby.RubySymbol;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RefinedCachingCallSite extends CachingCallSite {
    private final RubySymbol methodMissing;

    public RefinedCachingCallSite(RubySymbol methodName, CallType callType) {
        super(methodName, callType);

        methodMissing = methodName.getRuntime().newSymbol("method_missing");
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject... args) {
        RubyClass selfType = getClass(self);
        DynamicMethod method = selfType.searchWithRefinements(methodName, context.getCurrentStaticScope());

        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, args);
        }

        // FIXME: Broken for mbc strings which are not Java charsets
        return method.call(context, self, selfType, methodName.asJavaString(), args);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        RubyClass selfType = getClass(self);
        DynamicMethod method = selfType.searchWithRefinements(methodName, context.getCurrentStaticScope());

        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, args, block);
        }

        // FIXME: Broken for mbc strings which are not Java charsets
        return method.call(context, self, selfType, methodName.asJavaString(), args, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self) {
        RubyClass selfType = getClass(self);
        DynamicMethod method = selfType.searchWithRefinements(methodName, context.getCurrentStaticScope());

        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method);
        }

        // FIXME: Broken for mbc strings which are not Java charsets
        return method.call(context, self, selfType, methodName.asJavaString());
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        RubyClass selfType = getClass(self);
        DynamicMethod method = selfType.searchWithRefinements(methodName, context.getCurrentStaticScope());

        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, block);
        }

        // FIXME: Broken for mbc strings which are not Java charsets
        return method.call(context, self, selfType, methodName.asJavaString(), block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0) {
        RubyClass selfType = getClass(self);
        DynamicMethod method = selfType.searchWithRefinements(methodName, context.getCurrentStaticScope());

        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg0);
        }

        // FIXME: Broken for mbc strings which are not Java charsets
        return method.call(context, self, selfType, methodName.asJavaString(), arg0);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, Block block) {
        RubyClass selfType = getClass(self);
        DynamicMethod method = selfType.searchWithRefinements(methodName, context.getCurrentStaticScope());

        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg0, block);
        }

        // FIXME: Broken for mbc strings which are not Java charsets
        return method.call(context, self, selfType, methodName.asJavaString(), arg0, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1) {
        RubyClass selfType = getClass(self);
        DynamicMethod method = selfType.searchWithRefinements(methodName, context.getCurrentStaticScope());

        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg0, arg1);
        }

        // FIXME: Broken for mbc strings which are not Java charsets
        return method.call(context, self, selfType, methodName.asJavaString(), arg0, arg1);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) {
        RubyClass selfType = getClass(self);
        DynamicMethod method = selfType.searchWithRefinements(methodName, context.getCurrentStaticScope());

        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg0, arg1, block);
        }

        // FIXME: Broken for mbc strings which are not Java charsets
        return method.call(context, self, selfType, methodName.asJavaString(), arg0, arg1, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        RubyClass selfType = getClass(self);
        DynamicMethod method = selfType.searchWithRefinements(methodName, context.getCurrentStaticScope());

        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg0, arg1, arg2);
        }

        // FIXME: Broken for mbc strings which are not Java charsets
        return method.call(context, self, selfType, methodName.asJavaString(), arg0, arg1, arg2);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        RubyClass selfType = getClass(self);
        DynamicMethod method = selfType.searchWithRefinements(methodName, context.getCurrentStaticScope());

        if (methodMissing(method, caller)) {
            return callMethodMissing(context, self, method, arg0, arg1, arg2, block);
        }

        // FIXME: Broken for mbc strings which are not Java charsets
        return method.call(context, self, selfType, methodName.asJavaString(), arg0, arg1, arg2);
    }

    protected boolean methodMissing(DynamicMethod method, IRubyObject caller) {
        // doing full "normal" MM check rather than multiple refined sites by call types
        return method.isUndefined() || (!methodName.equals(methodMissing) && !method.isCallableFrom(caller, callType));
    }
}
