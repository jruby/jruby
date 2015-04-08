package org.jruby.runtime.callsite;

import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

public class RefinedCachingCallSite extends CachingCallSite {
    public RefinedCachingCallSite(String methodName, CallType callType) {
        super(methodName, callType);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject... args) {
        RubyClass selfType = getClass(self);
        StaticScope refinedScope = context.getCurrentStaticScope();
        Map<RubyClass, RubyModule> refinements;
        RubyModule refinement;
        DynamicMethod method = null;

        while (refinedScope != null &&
                (
                        (refinements = refinedScope.getModule().getRefinements()).isEmpty() ||
                                (refinement = refinements.get(selfType)) == null ||
                                (method = refinement.searchMethod(methodName)).isUndefined())
                ) {
            refinedScope = refinedScope.getPreviousCRefScope();
        }

        if (refinedScope == null) {
            return super.call(context, caller, self, args);
        }

        return method.call(context, self, selfType, methodName, args);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
        RubyClass selfType = getClass(self);
        StaticScope refinedScope = context.getCurrentStaticScope();
        Map<RubyClass, RubyModule> refinements;
        RubyModule refinement;
        DynamicMethod method = null;

        while (refinedScope != null &&
                (
                        (refinements = refinedScope.getModule().getRefinements()).isEmpty() ||
                                (refinement = refinements.get(selfType)) == null ||
                                (method = refinement.searchMethod(methodName)).isUndefined())
                ) {
            refinedScope = refinedScope.getPreviousCRefScope();
        }

        if (refinedScope == null) {
            return super.call(context, caller, self);
        }

        return method.call(context, self, selfType, methodName, args, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self) {
        RubyClass selfType = getClass(self);
        StaticScope refinedScope = context.getCurrentStaticScope();
        Map<RubyClass, RubyModule> refinements;
        RubyModule refinement;
        DynamicMethod method = null;

        while (refinedScope != null &&
                (
                        (refinements = refinedScope.getModule().getRefinements()).isEmpty() ||
                                (refinement = refinements.get(selfType)) == null ||
                                (method = refinement.searchMethod(methodName)).isUndefined())
                ) {
            refinedScope = refinedScope.getPreviousCRefScope();
        }

        if (refinedScope == null) {
            return super.call(context, caller, self);
        }

        return method.call(context, self, selfType, methodName);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
        RubyClass selfType = getClass(self);
        StaticScope refinedScope = context.getCurrentStaticScope();
        Map<RubyClass, RubyModule> refinements;
        RubyModule refinement;
        DynamicMethod method = null;

        while (refinedScope != null &&
                (
                        (refinements = refinedScope.getModule().getRefinements()).isEmpty() ||
                                (refinement = refinements.get(selfType)) == null ||
                                (method = refinement.searchMethod(methodName)).isUndefined())
                ) {
            refinedScope = refinedScope.getPreviousCRefScope();
        }

        if (refinedScope == null) {
            return super.call(context, caller, self, block);
        }

        return method.call(context, self, selfType, methodName, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0) {
        RubyClass selfType = getClass(self);
        StaticScope refinedScope = context.getCurrentStaticScope();
        Map<RubyClass, RubyModule> refinements;
        RubyModule refinement;
        DynamicMethod method = null;

        while (refinedScope != null &&
                (
                        (refinements = refinedScope.getModule().getRefinements()).isEmpty() ||
                                (refinement = refinements.get(selfType)) == null ||
                                (method = refinement.searchMethod(methodName)).isUndefined())
                ) {
            refinedScope = refinedScope.getPreviousCRefScope();
        }

        if (refinedScope == null) {
            return super.call(context, caller, self, arg0);
        }

        return method.call(context, self, selfType, methodName, arg0);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, Block block) {
        RubyClass selfType = getClass(self);
        StaticScope refinedScope = context.getCurrentStaticScope();
        Map<RubyClass, RubyModule> refinements;
        RubyModule refinement;
        DynamicMethod method = null;

        while (refinedScope != null &&
                (
                        (refinements = refinedScope.getModule().getRefinements()).isEmpty() ||
                                (refinement = refinements.get(selfType)) == null ||
                                (method = refinement.searchMethod(methodName)).isUndefined())
                ) {
            refinedScope = refinedScope.getPreviousCRefScope();
        }

        if (refinedScope == null) {
            return super.call(context, caller, self, arg0, block);
        }

        return method.call(context, self, selfType, methodName, arg0, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1) {
        RubyClass selfType = getClass(self);
        StaticScope refinedScope = context.getCurrentStaticScope();
        Map<RubyClass, RubyModule> refinements;
        RubyModule refinement;
        DynamicMethod method = null;

        while (refinedScope != null &&
                (
                        (refinements = refinedScope.getModule().getRefinements()).isEmpty() ||
                                (refinement = refinements.get(selfType)) == null ||
                                (method = refinement.searchMethod(methodName)).isUndefined())
                ) {
            refinedScope = refinedScope.getPreviousCRefScope();
        }

        if (refinedScope == null) {
            return super.call(context, caller, self, arg0, arg1);
        }

        return method.call(context, self, selfType, methodName);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) {
        RubyClass selfType = getClass(self);
        StaticScope refinedScope = context.getCurrentStaticScope();
        Map<RubyClass, RubyModule> refinements;
        RubyModule refinement;
        DynamicMethod method = null;

        while (refinedScope != null &&
                (
                        (refinements = refinedScope.getModule().getRefinements()).isEmpty() ||
                                (refinement = refinements.get(selfType)) == null ||
                                (method = refinement.searchMethod(methodName)).isUndefined())
                ) {
            refinedScope = refinedScope.getPreviousCRefScope();
        }

        if (refinedScope == null) {
            return super.call(context, caller, self, arg0, arg1, block);
        }

        return method.call(context, self, selfType, methodName, arg0, arg1, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        RubyClass selfType = getClass(self);
        StaticScope refinedScope = context.getCurrentStaticScope();
        Map<RubyClass, RubyModule> refinements;
        RubyModule refinement;
        DynamicMethod method = null;

        while (refinedScope != null &&
                (
                        (refinements = refinedScope.getModule().getRefinements()).isEmpty() ||
                                (refinement = refinements.get(selfType)) == null ||
                                (method = refinement.searchMethod(methodName)).isUndefined())
                ) {
            refinedScope = refinedScope.getPreviousCRefScope();
        }

        if (refinedScope == null) {
            return super.call(context, caller, self, arg0, arg1, arg2);
        }

        return method.call(context, self, selfType, methodName, arg0, arg1, arg2);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        RubyClass selfType = getClass(self);
        StaticScope refinedScope = context.getCurrentStaticScope();
        Map<RubyClass, RubyModule> refinements;
        RubyModule refinement;
        DynamicMethod method = null;

        while (refinedScope != null &&
                (
                        (refinements = refinedScope.getModule().getRefinements()).isEmpty() ||
                                (refinement = refinements.get(selfType)) == null ||
                                (method = refinement.searchMethod(methodName)).isUndefined())
                ) {
            refinedScope = refinedScope.getPreviousCRefScope();
        }

        if (refinedScope == null) {
            return super.call(context, caller, self, arg0, arg1, arg2, block);
        }

        return method.call(context, self, selfType, methodName, arg0, arg1, arg2);
    }

    protected boolean methodMissing(DynamicMethod method, IRubyObject caller) {
        // doing full "normal" MM check rather than multiple refined sites by call types
        return method.isUndefined() || (!methodName.equals("method_missing") && !method.isCallableFrom(caller, callType));
    }

    private static RubyClass getClass(IRubyObject self) {
        // the cast in the following line is necessary due to lacking optimizations in Hotspot
        return ((RubyBasicObject) self).getMetaClass();
    }
}
