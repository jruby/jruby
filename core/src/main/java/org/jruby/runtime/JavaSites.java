package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.DivCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.runtime.callsite.MulCallSite;
import org.jruby.runtime.callsite.PlusCallSite;
import org.jruby.runtime.callsite.RespondToCallSite;

/**
 * A collection of all call sites used for dynamic calls from JRuby's Java code.
 */
public class JavaSites {
    public final BasicObjectSites BasicObject = new BasicObjectSites();
    public final KernelSites Kernel = new KernelSites();
    public final ObjectSites Object = new ObjectSites();
    public final ArraySites Array = new ArraySites();
    public final Array2Sites Array2 = new Array2Sites();
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
    public final WarningSites Warning = new WarningSites();
    public final ZlibSites Zlib = new ZlibSites();
    public final ArgfSites Argf = new ArgfSites();
    public final TracePointSites TracePoint = new TracePointSites();
    public final MarshalSites Marshal = new MarshalSites();
    public final PathnameSites Pathname = new PathnameSites();
    public final DateSites Date = new DateSites();
    public final TempfileSites Tempfile = new TempfileSites();
    public final RaiseExceptionSites RaiseException = new RaiseExceptionSites();
    public final ConditionVariableSites ConditionVariable = new ConditionVariableSites();
    public final FiberSites Fiber = new FiberSites();
    public final MonitorSites Monitor = new MonitorSites();

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
        public final CallSite op_equal = new FunctionalCachingCallSite("==");
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
        public final CallSite call = new FunctionalCachingCallSite("call");
        public final CallSite warn = new FunctionalCachingCallSite("warn");
    }

    public static class ArraySites {
        public final CheckedSites begin_checked = new CheckedSites("begin");
        public final CheckedSites end_checked = new CheckedSites("end");
        public final CheckedSites exclude_end_checked = new CheckedSites("exclude_end?");
        public final CheckedSites to_ary_checked = new CheckedSites("to_ary");
        public final RespondToCallSite respond_to_to_ary = new RespondToCallSite("to_ary");
        public final CallSite to_ary = new FunctionalCachingCallSite("to_ary");
        public final CallSite cmp = new FunctionalCachingCallSite("<=>");
        public final CachingCallSite op_cmp_minmax = new FunctionalCachingCallSite("<=>");
        public final CallSite op_gt_minmax = new FunctionalCachingCallSite(">");
        public final CallSite op_lt_minmax = new FunctionalCachingCallSite("<");
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
        public final CachingCallSite self_each = new FunctionalCachingCallSite("each");
    }

    public static class Array2Sites {
        public final CachingCallSite op_cmp_fixnum = new FunctionalCachingCallSite("<=>");
        public final CachingCallSite op_cmp_string = new FunctionalCachingCallSite("<=>");
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

        public final ThreadContext.RecursiveFunctionEx recursive_cmp = new ThreadContext.RecursiveFunctionEx<IRubyObject>() {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject recv, IRubyObject other, boolean recur) {
                if (recur || !respond_to_cmp.respondsTo(context, other, other)) return context.nil;
                return cmp.call(context, other, other, recv);
            }
        };
    }

    public static class HashSites {
        public final RespondToCallSite respond_to_to_hash = new RespondToCallSite("to_hash");
        public final CachingCallSite self_default = new FunctionalCachingCallSite("default");
        public final CallSite flatten_bang = new FunctionalCachingCallSite("flatten!");
        public final CallSite call = new FunctionalCachingCallSite("call");
    }

    public static class NumericSites {
        public final RespondToCallSite respond_to_coerce = new RespondToCallSite("coerce");
        public final CallSite coerce = new FunctionalCachingCallSite("coerce");
        public final CallSite op_cmp = new FunctionalCachingCallSite("<=>");
        public final CachingCallSite op_minus = new FunctionalCachingCallSite("-");
        public final CallSite op_quo = new FunctionalCachingCallSite("/");
        public final CallSite floor = new FunctionalCachingCallSite("floor");
        public final CallSite div = new FunctionalCachingCallSite("div");
        public final CachingCallSite op_times = new FunctionalCachingCallSite("*");
        public final CallSite op_mod = new FunctionalCachingCallSite("%");
        public final CallSite op_ge = new FunctionalCachingCallSite(">=");
        public final CallSite op_le = new FunctionalCachingCallSite("<=");
        public final CachingCallSite op_lt = new FunctionalCachingCallSite("<");
        public final CachingCallSite op_gt = new FunctionalCachingCallSite(">");
        public final CheckedSites op_lt_checked = new CheckedSites("<");
        public final CheckedSites op_gt_checked = new CheckedSites(">");
        public final CallSite op_uminus = new FunctionalCachingCallSite("-@");
        public final CallSite zero = new FunctionalCachingCallSite("zero?");
        public final CallSite op_equals = new FunctionalCachingCallSite("==");
        public final CachingCallSite op_plus = new FunctionalCachingCallSite("+");
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
        public final CallSite op_equal = new FunctionalCachingCallSite("==");
    }

    public static class IntegerSites {
        public final CachingCallSite op_gt = new FunctionalCachingCallSite(">");
        public final CachingCallSite op_lt = new FunctionalCachingCallSite("<");
        public final CallSite op_le = new FunctionalCachingCallSite("<=");
        public final CallSite op_plus = new FunctionalCachingCallSite("+");
        public final CallSite op_minus = new FunctionalCachingCallSite("-");
        public final CallSite op_quo = new FunctionalCachingCallSite("/");
        public final CallSite op_mod = new FunctionalCachingCallSite("%");
        public final CallSite size = new FunctionalCachingCallSite("size");
        public final CallSite op_pow = new FunctionalCachingCallSite("**");
        public final CallSite op_uminus = new FunctionalCachingCallSite("-@");
        public final CheckedSites to_i_checked = new CheckedSites("to_i");
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
        public final CachingCallSite basic_op_lt = new FunctionalCachingCallSite("<");
        public final CachingCallSite basic_op_gt = new FunctionalCachingCallSite(">");
        public final CallSite op_exp_complex = new FunctionalCachingCallSite("**");
        public final CallSite op_lt_bignum = new FunctionalCachingCallSite("<");
        public final CallSite op_exp_rational = new FunctionalCachingCallSite("**");
        public final CallSite fdiv = new FunctionalCachingCallSite("fdiv");
        public final CallSite op_uminus = new FunctionalCachingCallSite("-@");
        public final CallSite op_rshift = new FunctionalCachingCallSite(">>");
        public final CheckedSites checked_op_and = new CheckedSites("&");
        public final CheckedSites checked_op_or = new CheckedSites("|");
        public final CheckedSites checked_op_xor = new CheckedSites("^");
        public final CachingCallSite to_f = new FunctionalCachingCallSite("to_f");
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
        public final CheckedSites checked_op_and = new CheckedSites("&");
        public final CheckedSites checked_op_or = new CheckedSites("|");
        public final CheckedSites checked_op_xor = new CheckedSites("^");
        public final CallSite op_cmp = new FunctionalCachingCallSite("<=>");
        public final CallSite fdiv = new FunctionalCachingCallSite("fdiv");
        public final CachingCallSite basic_op_lt = new FunctionalCachingCallSite("<");
        public final CachingCallSite basic_op_gt = new FunctionalCachingCallSite(">");
        public final CachingCallSite to_f = new FunctionalCachingCallSite("to_f");
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
        public final CachingCallSite cmp = new FunctionalCachingCallSite("<=>");

        public final ThreadContext.RecursiveFunctionEx recursive_cmp = new ThreadContext.RecursiveFunctionEx<IRubyObject>() {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject recv, IRubyObject other, boolean recur) {
                if (recur || !respond_to_cmp.respondsTo(context, other, other)) return context.nil;
                return cmp.call(context, other, other, recv);
            }
        };

        public final RespondToCallSite respond_to_to_int = new RespondToCallSite("to_int");
        public final CachingCallSite to_int = new FunctionalCachingCallSite("to_int");
        public final CachingCallSite to_i = new FunctionalCachingCallSite("to_i");
        public final CachingCallSite to_r = new FunctionalCachingCallSite("to_r");
        public final CheckedSites checked_to_r = new CheckedSites("to_r");

        public final RespondToCallSite respond_to_divmod = new RespondToCallSite("divmod");
        public final CachingCallSite divmod = new FunctionalCachingCallSite("divmod");
    }

    public static class EnumerableSites {
        public final CheckedSites size_checked = new CheckedSites("size");
        public final CachingCallSite to_enum = new FunctionalCachingCallSite("to_enum");
        public final CachingCallSite each = new FunctionalCachingCallSite("each");
        public final CallSite zip_next = new FunctionalCachingCallSite("next");
        public final CallSite chunk_call = new FunctionalCachingCallSite("call");
        public final CallSite chunk_op_lshift = new FunctionalCachingCallSite("<<");
        public final CallSite cycle_op_mul = new MulCallSite();
        public final CallSite detect_call = new FunctionalCachingCallSite("call");
        public final CallSite sum_op_plus = new FunctionalCachingCallSite("+");
        public final CallSite each_slice_op_plus = new PlusCallSite();
        public final CallSite each_slice_op_div = new DivCallSite();
        public final CallSite each_cons_op_plus = new PlusCallSite();
        public final CallSite each_cons_op_cmp = new FunctionalCachingCallSite("<=>");
        public final CallSite none_op_eqq = new FunctionalCachingCallSite("===");
        public final CallSite one_op_eqq = new FunctionalCachingCallSite("===");
        public final CallSite all_op_eqq = new FunctionalCachingCallSite("===");
        public final CallSite any_op_eqq = new FunctionalCachingCallSite("===");
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
        public final CachingCallSite write = new FunctionalCachingCallSite("write");
        public final RespondToCallSite respond_to_read = new RespondToCallSite("read");
        public final RespondToCallSite respond_to_readpartial = new RespondToCallSite("readpartial");
        public final CallSite read = new FunctionalCachingCallSite("read");
        public final CallSite to_f = new FunctionalCachingCallSite("to_f");
        public final CallSite new_ = new FunctionalCachingCallSite("new");
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
        public final CallSite op_equal = new FunctionalCachingCallSite("==");

        public final ThreadContext.RecursiveFunctionEx<Ruby> recursive_hash = new ThreadContext.RecursiveFunctionEx<Ruby>() {
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
        public final CallSite op_eql = new FunctionalCachingCallSite("==");
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
        public final CallSite op_quo = new FunctionalCachingCallSite("quo");
        public final CallSite op_exp = new FunctionalCachingCallSite("**");
        public final CallSite op_times = new FunctionalCachingCallSite("*");
        public final CallSite op_minus = new FunctionalCachingCallSite("-");
        public final CallSite finite = new FunctionalCachingCallSite("finite?");
        public final CallSite infinite = new FunctionalCachingCallSite("infinite?");
        public final CallSite fdiv = new FunctionalCachingCallSite("fdiv");
        public final CheckedSites to_c_checked = new CheckedSites("to_c");
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
        public final RespondToCallSite respond_to_to_r = new RespondToCallSite("to_r");
        public final CachingCallSite to_f = new FunctionalCachingCallSite("to_f");
    }

    public static class RangeSites {
        public final RespondToCallSite respond_to_succ = new RespondToCallSite("succ");
        public final CheckedSites to_int_checked = new CheckedSites("to_int");
        public final RespondToCallSite respond_to_begin = new RespondToCallSite("begin");
        public final RespondToCallSite respond_to_end = new RespondToCallSite("end");
        public final CallSite begin = new FunctionalCachingCallSite("begin");
        public final CallSite end = new FunctionalCachingCallSite("end");
        public final CallSite exclude_end = new FunctionalCachingCallSite("exclude_end?");
        public final CallSite max = new FunctionalCachingCallSite("max");
        public final CallSite op_cmp = new FunctionalCachingCallSite("<=>");
        public final CallSite op_gt = new FunctionalCachingCallSite(">");
        public final CallSite op_lt = new FunctionalCachingCallSite("<");
        public final CallSite each = new FunctionalCachingCallSite("each");
    }

    public static class WarningSites {
        public final CallSite warn = new FunctionalCachingCallSite("warn");
        public final CallSite write = new FunctionalCachingCallSite("write");
    }

    public static class ZlibSites {
        public final RespondToCallSite reader_respond_to = new RespondToCallSite();
        public final RespondToCallSite writer_respond_to = new RespondToCallSite();
    }

    public static class ArgfSites {
        public final CallSite each_codepoint = new FunctionalCachingCallSite("each_codepoint");
    }

    public static class TracePointSites {
        public final CheckedSites to_sym = new CheckedSites("to_sym");
    }

    public static class MarshalSites {
        public final RespondToCallSite respond_to_binmode = new RespondToCallSite("binmode");
        public final CachingCallSite binmode = new FunctionalCachingCallSite("binmode");
        public final RespondToCallSite respond_to_read = new RespondToCallSite("read");
        public final RespondToCallSite respond_to_getc = new RespondToCallSite("getc");
        public final RespondToCallSite respond_to_write = new RespondToCallSite("write");
    }

    public static class PathnameSites {
        public final CallSite glob = new FunctionalCachingCallSite("glob");
        public final CallSite op_plus = new FunctionalCachingCallSite("+");
        public final CallSite sub = new FunctionalCachingCallSite("sub");
    }

    public static class DateSites {
        public final CallSite zone_to_diff = new FunctionalCachingCallSite("zone_to_diff");
    }

    public static class TempfileSites {
        public final CachingCallSite create = new FunctionalCachingCallSite("create");
    }

    public static class RaiseExceptionSites {
        public final CheckedSites backtrace = new CheckedSites("backtrace");
    }

    public static class ConditionVariableSites {
        public final CachingCallSite mutex_sleep = new FunctionalCachingCallSite("sleep");
    }

    public static class FiberSites {
        public final CachingCallSite peek = new FunctionalCachingCallSite("peek");
        public final CachingCallSite next = new FunctionalCachingCallSite("next");
        public final CallSite each = new FunctionalCachingCallSite("each");
    }

    public static class MonitorSites {
        public final CachingCallSite wait = new FunctionalCachingCallSite("wait");
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
