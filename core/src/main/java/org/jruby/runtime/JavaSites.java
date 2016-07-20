package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.runtime.callsite.RespondToCallSite;

import static org.jruby.runtime.invokedynamic.MethodNames.OP_CMP;

/**
 * A collection of all call sites used for dynamic calls from JRuby's Java code.
 */
public class JavaSites {
    public final CallSite BO_respond_to = new FunctionalCachingCallSite("respond_to?");
    public final CallSite BO_respond_to_missing = new FunctionalCachingCallSite("respond_to_missing?");
    public final CallSite BO_initialize_dup = new FunctionalCachingCallSite("initialize_dup");
    public final CallSite BO_initialize_clone = new FunctionalCachingCallSite("initialize_clone");
    public final CallSite BO_to_s = new FunctionalCachingCallSite("to_s");
    public final CheckedSites BO_to_ary_checked = new CheckedSites("to_ary");
    public final CheckedSites BO_to_hash_checked = new CheckedSites("to_hash");
    public final CheckedSites BO_to_f_checked = new CheckedSites("to_f");
    public final CheckedSites BO_to_int_checked = new CheckedSites("to_int");
    public final CheckedSites BO_to_i_checked = new CheckedSites("to_i");
    public final CheckedSites BO_to_str_checked = new CheckedSites("to_str");
    public final CheckedSites BO_equals_checked = new CheckedSites("==");
    public final CheckedSites BO_hash_checked = new CheckedSites("hash");
    public final CallSite BO_inspect = new FunctionalCachingCallSite("inspect");
    public final CallSite BO_match = new FunctionalCachingCallSite("=~");
    public final CallSite BO_call = new FunctionalCachingCallSite("call");

    public final CachingCallSite O_dig_array = new FunctionalCachingCallSite("dig");
    public final CachingCallSite O_dig_hash = new FunctionalCachingCallSite("dig");
    public final CachingCallSite O_dig_struct = new FunctionalCachingCallSite("dig");
    public final RespondToCallSite O_respond_to_dig = new RespondToCallSite("dig");
    public final CachingCallSite O_dig_misc = new FunctionalCachingCallSite("dig");
    public final CachingCallSite O_to_s = new FunctionalCachingCallSite("to_s");


    public final CheckedSites K_to_f_checked = new CheckedSites("to_f");
    public final CheckedSites K_to_s_checked = new CheckedSites("to_s");
    public final CheckedSites K_to_str_checked = new CheckedSites("to_str");
    public final CallSite K_to_str = new FunctionalCachingCallSite("to_str");
    public final CallSite K_getc = new FunctionalCachingCallSite("getc");
    public final CallSite K_gets = new FunctionalCachingCallSite("gets");
    public final CallSite K_putc = new FunctionalCachingCallSite("putc");
    public final CallSite K_puts = new FunctionalCachingCallSite("puts");
    public final CallSite K_initialize_copy = new FunctionalCachingCallSite("initialize_copy");
    public final CallSite K_convert_complex = new FunctionalCachingCallSite("convert");
    public final CallSite K_convert_rational = new FunctionalCachingCallSite("convert");
    public final CheckedSites K_to_hash_checked = new CheckedSites("to_hash");
    public final CallSite K_write = new FunctionalCachingCallSite("write");

    public final CheckedSites ARY_to_ary_checked = new CheckedSites("to_ary");

    public final CheckedSites STR_to_str_checked = new CheckedSites("to_str");
    public final RespondToCallSite STR_respond_to_cmp = new RespondToCallSite("<=>");
    public final RespondToCallSite STR_respond_to_to_str = new RespondToCallSite("to_str");
    public final CallSite STR_equals = new FunctionalCachingCallSite("==");
    public final CallSite STR_cmp = new FunctionalCachingCallSite("<=>");
    public final CallSite STR_hash = new FunctionalCachingCallSite("hash");

    public final RespondToCallSite TIME_respond_to_cmp = new RespondToCallSite("<=>");
    public final CallSite TIME_cmp = new FunctionalCachingCallSite("<=>");

    public final CheckedSites ENUMBL_size_checked = new CheckedSites("size");

    public final CheckedSites IO_closed_checked = new CheckedSites("closed?");
    public final CheckedSites IO_close_checked = new CheckedSites("close");
    public final RespondToCallSite IO_respond_to_to_path = new RespondToCallSite("to_path");
    public final CheckedSites IO_to_path_checked1 = new CheckedSites("to_path");
    public final CheckedSites IO_to_path_checked2 = new CheckedSites("to_path");

    public final CheckedSites TCONV_to_ary_checked = new CheckedSites("to_ary");
    public final CheckedSites TCONV_to_a_checked = new CheckedSites("to_a");

    public final CheckedSites IRHLP_to_a_checked = new CheckedSites("to_a");

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

    public class CheckedSites {
        public final RespondToCallSite respond_to_X;
        public final CachingCallSite respond_to_missing = new FunctionalCachingCallSite("respond_to_missing?");
        public final CachingCallSite method_missing = new FunctionalCachingCallSite("method_missing");
        public final CachingCallSite site;
        public final String methodName;

        public CheckedSites(String x) {
            respond_to_X = new RespondToCallSite(x);
            site = new FunctionalCachingCallSite(x);
            methodName = x;
        }
    }
}
