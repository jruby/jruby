package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.runtime.callsite.RespondToCallSite;

import static org.jruby.runtime.invokedynamic.MethodNames.OP_CMP;

/**
 * A collection of all call sites used for dynamic calls from JRuby's Java code.
 */
public class JavaCallSites {
    public final CallSite BO_respond_to = new FunctionalCachingCallSite("respond_to?");
    public final CallSite BO_initialize_dup = new FunctionalCachingCallSite("initialize_dup");
    public final CallSite BO_initialize_clone = new FunctionalCachingCallSite("initialize_clone");

    public final RespondToCallSite STR_respond_to_to_str = new RespondToCallSite("to_str");
    public final RespondToCallSite STR_respond_to_cmp = new RespondToCallSite("<=>");
    public final CallSite STR_to_str = new FunctionalCachingCallSite("to_str");
    public final CallSite STR_equals = new FunctionalCachingCallSite("==");
    public final CallSite STR_cmp = new FunctionalCachingCallSite("<=>");
    public final CallSite STR_hash = new FunctionalCachingCallSite("hash");

    public final RespondToCallSite TIME_respond_to_cmp = new RespondToCallSite("<=>");
    public final CallSite TIME_cmp = new FunctionalCachingCallSite("<=>");

    public final Ruby.RecursiveFunctionEx STR_recursive_cmp = new Ruby.RecursiveFunctionEx<IRubyObject>() {
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject recv, IRubyObject other, boolean recur) {
            if (recur || !context.sites.STR_respond_to_cmp.respondsTo(context, other, other)) return context.nil;
            return context.sites.STR_cmp.call(context, other, other, recv);
        }
    };

    public final Ruby.RecursiveFunctionEx TIME_recursive_cmp = new Ruby.RecursiveFunctionEx<IRubyObject>() {
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject recv, IRubyObject other, boolean recur) {
            if (recur || !context.sites.TIME_respond_to_cmp.respondsTo(context, other, other)) return context.nil;
            return context.sites.TIME_cmp.call(context, other, other, recv);
        }
    };
}
