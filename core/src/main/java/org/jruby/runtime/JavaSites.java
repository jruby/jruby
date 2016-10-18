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
    public final HashSites Hash = new HashSites();
    public final NumericSites Numeric = new NumericSites();
    public final IntegerSites Integer = new IntegerSites();
    public final FixnumSites Fixnum = new FixnumSites();
    public final FloatSites Float = new FloatSites();
    public final BignumSites Bignum = new BignumSites();
    public final TimeSites Time = new TimeSites();
    public final EnumerableSites Enumerable = new EnumerableSites();
    public final ComparableSites Comparable = new ComparableSites();
    public final IOSites IO = new IOSites();
    public final FileSites File = new FileSites();
    public final TypeConverterSites TypeConverter = new TypeConverterSites();
    public final HelpersSites Helpers = new HelpersSites();
    public final IRRuntimeHelpersSites IRRuntimeHelpers = new IRRuntimeHelpersSites();
    public final BigDecimalSites BigDecimal = new BigDecimalSites();
    public final ComplexSites Complex = new ComplexSites();
    public final RationalSites Rational = new RationalSites();
    public final RangeSites Range = new RangeSites();
    public final ZlibSites Zlib = new ZlibSites();

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
        public final CachingCallSite op_cmp_minmax = new FunctionalCachingCallSite("<=>");
        public final CallSite op_gt_minmax = new FunctionalCachingCallSite(">");
        public final CallSite op_lt_minmax = new FunctionalCachingCallSite("<");
        public final RespondToCallSite respond_to_begin = new RespondToCallSite("begin");
        public final RespondToCallSite respond_to_end = new RespondToCallSite("end");
        public final CallSite begin = new FunctionalCachingCallSite("begin");
        public final CallSite end = new FunctionalCachingCallSite("end");
        public final CallSite exclude_end = new FunctionalCachingCallSite("exclude_end?");
        public final CallSite to_enum = new FunctionalCachingCallSite("to_enum");
        public final CallSite op_times = new FunctionalCachingCallSite("*");
        public final CallSite op_quo = new FunctionalCachingCallSite("/");
        public final CallSite op_exp = new FunctionalCachingCallSite("**");
        public final CallSite call = new FunctionalCachingCallSite("call");
        public final CallSite sort_by = new FunctionalCachingCallSite("sort_by");
        public final CallSite op_equal = new FunctionalCachingCallSite("==");
        public final CallSite eql = new FunctionalCachingCallSite("eql?");
        public final CallSite op_cmp_bsearch = new FunctionalCachingCallSite("<=>");
        public final CallSite op_cmp_sort = new FunctionalCachingCallSite("<=>");
        public final CallSite op_gt_sort = new FunctionalCachingCallSite(">");
        public final CallSite op_lt_sort = new FunctionalCachingCallSite("<");
    }

    public static class StringSites {
        public final CheckedSites to_str_checked = new CheckedSites("to_str");
        public final RespondToCallSite respond_to_cmp = new RespondToCallSite("<=>");
        public final RespondToCallSite respond_to_to_str = new RespondToCallSite("to_str");
        public final CallSite equals = new FunctionalCachingCallSite("==");
        public final CachingCallSite cmp = new FunctionalCachingCallSite("<=>");
        public final CallSite hash = new FunctionalCachingCallSite("hash");
        public final CallSite to_s = new FunctionalCachingCallSite("to_s");
        public final CallSite op_match = new FunctionalCachingCallSite("=~");
        public final CallSite match = new FunctionalCachingCallSite("match");
        public final RespondToCallSite respond_to_begin = new RespondToCallSite("begin");
        public final RespondToCallSite respond_to_end = new RespondToCallSite("end");
        public final CallSite begin = new FunctionalCachingCallSite("begin");
        public final CallSite end = new FunctionalCachingCallSite("end");
        public final CallSite exclude_end = new FunctionalCachingCallSite("exclude_end?");
        public final CallSite op_lt = new FunctionalCachingCallSite("<");
        public final CallSite op_le = new FunctionalCachingCallSite("<=");
        public final CallSite succ = new FunctionalCachingCallSite("succ");
        public final CallSite op_plus = new FunctionalCachingCallSite("+");
        public final CallSite op_minus = new FunctionalCachingCallSite("-");
        public final CallSite op_lshift = new FunctionalCachingCallSite("<<");
        public final CallSite op_and = new FunctionalCachingCallSite("&");
        public final CheckedSites to_hash_checked = new CheckedSites("to_hash");

        public final Ruby.RecursiveFunctionEx recursive_cmp = new Ruby.RecursiveFunctionEx<IRubyObject>() {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject recv, IRubyObject other, boolean recur) {
                if (recur || !respond_to_cmp.respondsTo(context, other, other)) return context.nil;
                return cmp.call(context, other, other, recv);
            }
        };
    }

    public static class HashSites {
        public final RespondToCallSite respond_to_to_hash = new RespondToCallSite("to_hash");
        public final CallSite default_ = new FunctionalCachingCallSite("default");
        public final CallSite flatten_bang = new FunctionalCachingCallSite("flatten!");
        public final CallSite call = new FunctionalCachingCallSite("call");
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
        public final CallSite zero = new FunctionalCachingCallSite("zero?");
        public final CallSite op_equals = new FunctionalCachingCallSite("==");
        public final CallSite op_plus = new FunctionalCachingCallSite("+");
        public final CallSite numerator = new FunctionalCachingCallSite("numerator");
        public final CallSite denominator = new FunctionalCachingCallSite("denominator");
        public final CallSite op_xor = new FunctionalCachingCallSite("^");
        public final CallSite abs = new FunctionalCachingCallSite("abs");
        public final CallSite abs2 = new FunctionalCachingCallSite("abs2");
        public final CallSite arg = new FunctionalCachingCallSite("arg");
        public final CallSite conjugate = new FunctionalCachingCallSite("conjugate");
        public final CallSite exact = new FunctionalCachingCallSite("exact?");
        public final CallSite polar = new FunctionalCachingCallSite("polar");
        public final CallSite real = new FunctionalCachingCallSite("real?");
        public final CallSite integer = new FunctionalCachingCallSite("integer?");
        public final CallSite divmod = new FunctionalCachingCallSite("divmod");
        public final CallSite inspect = new FunctionalCachingCallSite("inspect");
        public final CallSite to_f = new FunctionalCachingCallSite("to_f");
        public final CallSite to_i = new FunctionalCachingCallSite("to_i");
        public final CallSite to_r = new FunctionalCachingCallSite("to_r");
        public final CallSite to_s = new FunctionalCachingCallSite("to_s");
        public final CallSite truncate = new FunctionalCachingCallSite("truncate");
        public final CallSite op_exp = new FunctionalCachingCallSite("**");
        public final CallSite quo = new FunctionalCachingCallSite("quo");
        public final CallSite op_lshift = new FunctionalCachingCallSite("<<");
        public final CallSite op_rshift = new FunctionalCachingCallSite(">>");
        public final CallSite size = new FunctionalCachingCallSite("size");
        public final CallSite ceil = new FunctionalCachingCallSite("ceil");
    }

    public static class IntegerSites {
        public final CallSite op_gt = new FunctionalCachingCallSite(">");
        public final CallSite op_lt = new FunctionalCachingCallSite("<");
        public final CallSite op_le = new FunctionalCachingCallSite("<=");
        public final CallSite op_plus = new FunctionalCachingCallSite("+");
        public final CallSite op_minus = new FunctionalCachingCallSite("-");
        public final CallSite op_quo = new FunctionalCachingCallSite("/");
        public final CallSite op_mod = new FunctionalCachingCallSite("%");
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
        public final CallSite op_exp_complex = new FunctionalCachingCallSite("**");
        public final CallSite op_lt_bignum = new FunctionalCachingCallSite("<");
        public final CallSite op_exp_rational = new FunctionalCachingCallSite("**");
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
        public final CallSite op_equal = new FunctionalCachingCallSite("==");
        public final RespondToCallSite respond_to_infinite = new RespondToCallSite("infinite?");
        public final CallSite infinite = new FunctionalCachingCallSite("infinite?");
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

    public static class ComparableSites {
        public final RespondToCallSite respond_to_op_cmp = new RespondToCallSite("<=>");
        public final CallSite op_cmp = new FunctionalCachingCallSite("<=>");
        public final CallSite op_lt = new FunctionalCachingCallSite("<");
        public final CallSite op_gt = new FunctionalCachingCallSite(">");
    }

    public static class IOSites {
        public final CheckedSites closed_checked = new CheckedSites("closed?");
        public final CheckedSites close_checked = new CheckedSites("close");
        public final CheckedSites to_path_checked1 = new CheckedSites("to_path");
        public final CheckedSites to_path_checked2 = new CheckedSites("to_path");
        public final RespondToCallSite respond_to_write = new RespondToCallSite("write");
        public final CallSite write = new FunctionalCachingCallSite("write");
        public final RespondToCallSite respond_to_read = new RespondToCallSite("read");
        public final CallSite read = new FunctionalCachingCallSite("read");
        public final CallSite to_f = new FunctionalCachingCallSite("to_f");
        public final CallSite new_ = new FunctionalCachingCallSite("new");
        public final RespondToCallSite respond_to_to_int = new RespondToCallSite("to_int");
        public final RespondToCallSite respond_to_to_io = new RespondToCallSite("to_io");
        public final RespondToCallSite respond_to_to_hash = new RespondToCallSite("to_hash");
    }

    public static class FileSites {
        public final CallSite to_path = new FunctionalCachingCallSite("to_path");
        public final RespondToCallSite respond_to_to_path = new RespondToCallSite("to_path");
        public final CheckedSites to_time_checked = new CheckedSites("to_time");
        public final CheckedSites to_int_checked = new CheckedSites("to_int");
        public final CheckedSites to_hash_checked = new CheckedSites("to_hash");
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

    public static class RangeSites {
        public final RespondToCallSite respond_to_succ = new RespondToCallSite("succ");
        public final CheckedSites to_int_checked = new CheckedSites("to_int");
    }

    public static class ZlibSites {
        public final RespondToCallSite reader_respond_to = new RespondToCallSite();
        public final RespondToCallSite writer_respond_to = new RespondToCallSite();
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
