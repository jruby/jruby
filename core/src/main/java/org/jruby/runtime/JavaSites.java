package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.runtime.callsite.RespondToCallSite;

import static org.jruby.runtime.invokedynamic.MethodNames.OP_CMP;

/**
 * A collection of all call sites used for dynamic calls from JRuby's Java code.
 */
public class JavaSites {
    public final BasicObjectSites BasicObject = new BasicObjectSites();
    public final KernelSites Kernel = new KernelSites();
    public final ObjectSites Object = new ObjectSites();
    public final ArraySites Array = new ArraySites();
    public final StringSites String = new StringSites();
    public final NumericSites Numeric = new NumericSites();
    public final FixnumSites Fixnum = new FixnumSites();
    public final FloatSites Float = new FloatSites();
    public final BignumSites Bignum = new BignumSites();
    public final TimeSites Time = new TimeSites();
    public final EnumerableSites Enumerable = new EnumerableSites();
    public final IOSites IO = new IOSites();
    public final TypeConverterSites TypeConverter = new TypeConverterSites();
    public final HelpersSites Helpers = new HelpersSites();
    public final IRRuntimeHelpersSites IRRuntimeHelpers = new IRRuntimeHelpersSites();
    public final BigDecimalSites BigDecimal = new BigDecimalSites();
    public final ComplexSites Complex = new ComplexSites();
    public final RationalSites Rational = new RationalSites();

    public static class BasicObjectSites {
        public final CallSite respond_to = new FunctionalCachingCallSite("respond_to?");
        public final CallSite respond_to_missing = new FunctionalCachingCallSite("respond_to_missing?");
        public final CallSite initialize_dup = new FunctionalCachingCallSite("initialize_dup");
        public final CallSite initialize_clone = new FunctionalCachingCallSite("initialize_clone");
        public final CallSite to_s = new FunctionalCachingCallSite("to_s");
        public final CheckedSites to_ary_checked = new CheckedSites("to_ary");
        public final CheckedSites to_hash_checked = new CheckedSites("to_hash");
        public final CheckedSites to_f_checked = new CheckedSites("to_f");
        public final CheckedSites to_int_checked = new CheckedSites("to_int");
        public final CheckedSites to_i_checked = new CheckedSites("to_i");
        public final CheckedSites to_str_checked = new CheckedSites("to_str");
        public final CheckedSites equals_checked = new CheckedSites("==");
        public final CheckedSites hash_checked = new CheckedSites("hash");
        public final CallSite inspect = new FunctionalCachingCallSite("inspect");
        public final CallSite match = new FunctionalCachingCallSite("=~");
        public final CallSite call = new FunctionalCachingCallSite("call");
    }

    public static class ObjectSites {
        public final CachingCallSite dig_array = new FunctionalCachingCallSite("dig");
        public final CachingCallSite dig_hash = new FunctionalCachingCallSite("dig");
        public final CachingCallSite dig_struct = new FunctionalCachingCallSite("dig");
        public final RespondToCallSite respond_to_dig = new RespondToCallSite("dig");
        public final CachingCallSite dig_misc = new FunctionalCachingCallSite("dig");
        public final CachingCallSite to_s = new FunctionalCachingCallSite("to_s");
    }

    public static class KernelSites {
        public final CheckedSites to_f_checked = new CheckedSites("to_f");
        public final CheckedSites to_s_checked = new CheckedSites("to_s");
        public final CheckedSites to_str_checked = new CheckedSites("to_str");
        public final CallSite to_str = new FunctionalCachingCallSite("to_str");
        public final CallSite getc = new FunctionalCachingCallSite("getc");
        public final CallSite gets = new FunctionalCachingCallSite("gets");
        public final CallSite putc = new FunctionalCachingCallSite("putc");
        public final CallSite puts = new FunctionalCachingCallSite("puts");
        public final CallSite initialize_copy = new FunctionalCachingCallSite("initialize_copy");
        public final CallSite convert_complex = new FunctionalCachingCallSite("convert");
        public final CallSite convert_rational = new FunctionalCachingCallSite("convert");
        public final CheckedSites to_hash_checked = new CheckedSites("to_hash");
        public final CallSite write = new FunctionalCachingCallSite("write");
    }
    public static class ArraySites {
        public final CheckedSites to_ary_checked = new CheckedSites("to_ary");
        public final RespondToCallSite respond_to_to_ary = new RespondToCallSite("to_ary");
        public final CallSite to_ary = new FunctionalCachingCallSite("to_ary");
        public final CallSite cmp = new FunctionalCachingCallSite("<=>");
    }

    public static class StringSites {
        public final CheckedSites to_str_checked = new CheckedSites("to_str");
        public final RespondToCallSite respond_to_cmp = new RespondToCallSite("<=>");
        public final RespondToCallSite respond_to_to_str = new RespondToCallSite("to_str");
        public final CallSite equals = new FunctionalCachingCallSite("==");
        public final CallSite cmp = new FunctionalCachingCallSite("<=>");
        public final CallSite hash = new FunctionalCachingCallSite("hash");

        public final Ruby.RecursiveFunctionEx recursive_cmp = new Ruby.RecursiveFunctionEx<IRubyObject>() {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject recv, IRubyObject other, boolean recur) {
                if (recur || !respond_to_cmp.respondsTo(context, other, other)) return context.nil;
                return cmp.call(context, other, other, recv);
            }
        };
    }

    public static class NumericSites {
        public final RespondToCallSite respond_to_coerce = new RespondToCallSite("coerce");
        public final CallSite coerce = new FunctionalCachingCallSite("coerce");
        public final CallSite op_cmp = new FunctionalCachingCallSite("<=>");
        public final CallSite op_minus = new FunctionalCachingCallSite("-");
        public final CallSite op_quo = new FunctionalCachingCallSite("/");
        public final CallSite floor = new FunctionalCachingCallSite("floor");
        public final CallSite div = new FunctionalCachingCallSite("div");
        public final CallSite op_times = new FunctionalCachingCallSite("*");
        public final CallSite op_mod = new FunctionalCachingCallSite("%");
        public final CallSite op_lt = new FunctionalCachingCallSite("<");
        public final CallSite op_gt = new FunctionalCachingCallSite(">");
        public final CallSite op_uminus = new FunctionalCachingCallSite("-@");
        public final CallSite to_i = new FunctionalCachingCallSite("to_i");
        public final CallSite zero = new FunctionalCachingCallSite("zero?");
        public final CallSite op_equals = new FunctionalCachingCallSite("==");
        public final CallSite op_plus = new FunctionalCachingCallSite("+");
        public final CallSite numerator = new FunctionalCachingCallSite("numerator");
        public final CallSite denominator = new FunctionalCachingCallSite("denominator");
    }

    public static class FixnumSites {
        public final CallSite op_plus = new FunctionalCachingCallSite("+");
        public final CallSite divmod = new FunctionalCachingCallSite("divmod");
        public final CallSite div = new FunctionalCachingCallSite("div");
        public final CallSite op_quo = new FunctionalCachingCallSite("/");
        public final CallSite op_times = new FunctionalCachingCallSite("*");
        public final CallSite op_mod = new FunctionalCachingCallSite("%");
        public final CallSite op_exp = new FunctionalCachingCallSite("**");
        public final CallSite quo = new FunctionalCachingCallSite("quo");
        public final CallSite op_minus = new FunctionalCachingCallSite("-");
        public final CallSite op_cmp = new FunctionalCachingCallSite("<=>");
        public final CallSite op_ge = new FunctionalCachingCallSite(">=");
        public final CallSite op_le = new FunctionalCachingCallSite("<=");
        public final CallSite op_gt = new FunctionalCachingCallSite(">");
        public final CallSite op_lt = new FunctionalCachingCallSite("<");
    }

    public static class BignumSites {
        public final CallSite op_plus = new FunctionalCachingCallSite("+");
        public final CallSite op_minus = new FunctionalCachingCallSite("-");
        public final CallSite divmod = new FunctionalCachingCallSite("divmod");
        public final CallSite op_quo = new FunctionalCachingCallSite("/");
        public final CallSite div = new FunctionalCachingCallSite("div");
        public final CallSite op_mod = new FunctionalCachingCallSite("%");
        public final CallSite op_exp = new FunctionalCachingCallSite("**");
        public final CallSite op_times = new FunctionalCachingCallSite("*");
        public final CallSite quo = new FunctionalCachingCallSite("quo");
        public final CallSite remainder = new FunctionalCachingCallSite("remainder");
        public final CallSite op_and = new FunctionalCachingCallSite("&");
        public final CallSite op_or = new FunctionalCachingCallSite("|");
        public final CallSite op_xor = new FunctionalCachingCallSite("^");
        public final CallSite op_cmp = new FunctionalCachingCallSite("<=>");
    }

    public static class FloatSites {
        public final CallSite divmod = new FunctionalCachingCallSite("divmod");
        public final CallSite op_quo = new FunctionalCachingCallSite("/");
        public final CallSite op_minus = new FunctionalCachingCallSite("-");
        public final CallSite op_mod = new FunctionalCachingCallSite("%");
        public final CallSite op_times = new FunctionalCachingCallSite("*");
        public final CallSite op_plus = new FunctionalCachingCallSite("+");
        public final CallSite op_exp = new FunctionalCachingCallSite("**");
        public final CallSite op_cmp = new FunctionalCachingCallSite("<=>");
        public final CallSite op_ge = new FunctionalCachingCallSite(">=");
        public final CallSite op_le = new FunctionalCachingCallSite("<=");
        public final CallSite op_gt = new FunctionalCachingCallSite(">");
        public final CallSite op_lt = new FunctionalCachingCallSite("<");
    }

    public static class TimeSites {
        public final RespondToCallSite respond_to_cmp = new RespondToCallSite("<=>");
        public final CallSite cmp = new FunctionalCachingCallSite("<=>");

        public final Ruby.RecursiveFunctionEx recursive_cmp = new Ruby.RecursiveFunctionEx<IRubyObject>() {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject recv, IRubyObject other, boolean recur) {
                if (recur || !respond_to_cmp.respondsTo(context, other, other)) return context.nil;
                return cmp.call(context, other, other, recv);
            }
        };
    }

    public static class EnumerableSites {
        public final CheckedSites size_checked = new CheckedSites("size");
    }

    public static class IOSites {
        public final CheckedSites closed_checked = new CheckedSites("closed?");
        public final CheckedSites close_checked = new CheckedSites("close");
        public final CheckedSites to_path_checked1 = new CheckedSites("to_path");
        public final CheckedSites to_path_checked2 = new CheckedSites("to_path");
        public final CallSite write = new FunctionalCachingCallSite("write");
    }

    public static class TypeConverterSites {
        public final CheckedSites to_f_checked = new CheckedSites("to_f");
        public final CheckedSites to_int_checked = new CheckedSites("to_int");
        public final CheckedSites to_i_checked = new CheckedSites("to_i");
        public final CheckedSites to_ary_checked = new CheckedSites("to_ary");
        public final CheckedSites to_a_checked = new CheckedSites("to_a");
    }

    public static class HelpersSites {
        public final CallSite hash = new FunctionalCachingCallSite("hash");

        public final Ruby.RecursiveFunctionEx<Ruby> recursive_hash = new Ruby.RecursiveFunctionEx<Ruby>() {
            public IRubyObject call(ThreadContext context, Ruby runtime, IRubyObject obj, boolean recur) {
                if (recur) return RubyFixnum.zero(runtime);
                return hash.call(context, obj, obj);
            }
        };
    }

    public static class IRRuntimeHelpersSites {
        public final CheckedSites to_a_checked = new CheckedSites("to_a");
    }

    public static class BigDecimalSites {
        public final CallSite op_plus = new FunctionalCachingCallSite("+");
        public final CallSite op_cmp = new FunctionalCachingCallSite("<=>");
        public final CallSite divmod = new FunctionalCachingCallSite("divmod");
        public final CallSite op_times = new FunctionalCachingCallSite("*");
        public final CallSite div = new FunctionalCachingCallSite("div");
        public final CallSite op_mod = new FunctionalCachingCallSite("%");
        public final CallSite op_quo = new FunctionalCachingCallSite("/");
        public final CallSite remainder = new FunctionalCachingCallSite("remainder");
        public final CallSite op_or = new FunctionalCachingCallSite("|");
        public final CallSite op_and = new FunctionalCachingCallSite("&");
        public final CallSite op_minus = new FunctionalCachingCallSite("-");
    }

    public static class ComplexSites {
        public final CallSite op_plus = new FunctionalCachingCallSite("+");
        public final CallSite op_quo = new FunctionalCachingCallSite("/");
        public final CallSite op_exp = new FunctionalCachingCallSite("**");
        public final CallSite op_times = new FunctionalCachingCallSite("*");
        public final CallSite op_minus = new FunctionalCachingCallSite("-");
    }

    public static class RationalSites {
        public final CallSite op_plus = new FunctionalCachingCallSite("+");
        public final CallSite op_minus = new FunctionalCachingCallSite("-");
        public final CallSite divmod = new FunctionalCachingCallSite("divmod");
        public final CallSite op_quo = new FunctionalCachingCallSite("/");
        public final CallSite div = new FunctionalCachingCallSite("div");
        public final CallSite mod = new FunctionalCachingCallSite("mod");
        public final CallSite op_exp = new FunctionalCachingCallSite("**");
        public final CallSite op_times = new FunctionalCachingCallSite("*");
        public final CallSite quo = new FunctionalCachingCallSite("quo");
        public final CallSite remainder = new FunctionalCachingCallSite("remainder");
        public final CallSite op_cmp = new FunctionalCachingCallSite("<=>");
        public final CheckedSites to_r_checked = new CheckedSites("to_r");
    }

    public static class CheckedSites {
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
