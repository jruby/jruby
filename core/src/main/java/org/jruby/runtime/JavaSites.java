package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubySymbol;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.runtime.callsite.RespondToCallSite;

/**
 * A collection of all call sites used for dynamic calls from JRuby's Java code.
 */
public class JavaSites {
    public final BasicObjectSites BasicObject;
    public final KernelSites Kernel;
    public final ObjectSites Object;
    public final ArraySites Array;
    public final StringSites String;
    public final HashSites Hash;
    public final NumericSites Numeric;
    public final IntegerSites Integer;
    public final FixnumSites Fixnum;
    public final FloatSites Float;
    public final BignumSites Bignum;
    public final TimeSites Time;
    public final EnumerableSites Enumerable;
    public final ComparableSites Comparable;
    public final IOSites IO;
    public final FileSites File;
    public final TypeConverterSites TypeConverter;
    public final HelpersSites Helpers;
    public final IRRuntimeHelpersSites IRRuntimeHelpers;
    public final BigDecimalSites BigDecimal;
    public final ComplexSites Complex;
    public final RationalSites Rational;
    public final RangeSites Range;
    public final WarningSites Warning;
    public final ZlibSites Zlib;

    public JavaSites(Ruby runtime) {
        BasicObject = new BasicObjectSites(runtime);
        Kernel = new KernelSites(runtime);
        Object = new ObjectSites(runtime);
        Array = new ArraySites(runtime);
        String = new StringSites(runtime);
        Hash = new HashSites(runtime);
        Numeric = new NumericSites(runtime);
        Integer = new IntegerSites(runtime);
        Fixnum = new FixnumSites(runtime);
        Float = new FloatSites(runtime);
        Bignum = new BignumSites(runtime);
        Time = new TimeSites(runtime);
        Enumerable = new EnumerableSites(runtime);
        Comparable = new ComparableSites(runtime);
        IO = new IOSites(runtime);
        File = new FileSites(runtime);
        TypeConverter = new TypeConverterSites(runtime);
        Helpers = new HelpersSites(runtime);
        IRRuntimeHelpers = new IRRuntimeHelpersSites(runtime);
        BigDecimal = new BigDecimalSites(runtime);
        Complex = new ComplexSites(runtime);
        Rational = new RationalSites(runtime);
        Range = new RangeSites(runtime);
        Warning = new WarningSites(runtime);
        Zlib = new ZlibSites(runtime);
    }

    public static class BasicObjectSites {
        public final CallSite respond_to;
        public final CallSite respond_to_missing;
        public final CallSite initialize_dup;
        public final CallSite initialize_clone;
        public final CallSite to_s;
        public final CheckedSites to_ary_checked;
        public final CheckedSites to_hash_checked;
        public final CheckedSites to_f_checked;
        public final CheckedSites to_int_checked;
        public final CheckedSites to_i_checked;
        public final CheckedSites to_str_checked;
        public final CheckedSites equals_checked;
        public final CheckedSites hash_checked;
        public final CallSite inspect;
        public final CallSite match;
        public final CallSite call;

        public BasicObjectSites(Ruby runtime) {
            respond_to = new FunctionalCachingCallSite(runtime.newSymbol("respond_to?"));
            respond_to_missing = new FunctionalCachingCallSite(runtime.newSymbol("respond_to_missing?"));
            initialize_dup = new FunctionalCachingCallSite(runtime.newSymbol("initialize_dup"));
            initialize_clone = new FunctionalCachingCallSite(runtime.newSymbol("initialize_clone"));
            to_s = new FunctionalCachingCallSite(runtime.newSymbol("to_s"));
            to_ary_checked = new CheckedSites(runtime, runtime.newSymbol("to_ary"));
            to_hash_checked = new CheckedSites(runtime, runtime.newSymbol("to_hash"));
            to_f_checked = new CheckedSites(runtime, runtime.newSymbol("to_f"));
            to_int_checked = new CheckedSites(runtime, runtime.newSymbol("to_int"));
            to_i_checked = new CheckedSites(runtime, runtime.newSymbol("to_i"));
            to_str_checked = new CheckedSites(runtime, runtime.newSymbol("to_str"));
            equals_checked = new CheckedSites(runtime, runtime.newSymbol("=="));
            hash_checked = new CheckedSites(runtime, runtime.newSymbol("hash"));
            inspect = new FunctionalCachingCallSite(runtime.newSymbol("inspect"));
            match = new FunctionalCachingCallSite(runtime.newSymbol("=~"));
            call = new FunctionalCachingCallSite(runtime.newSymbol("call"));
        }
    }

    public static class ObjectSites {
        public final CachingCallSite dig_array;
        public final CachingCallSite dig_hash;
        public final CachingCallSite dig_struct;
        public final RespondToCallSite respond_to_dig;
        public final CachingCallSite dig_misc;
        public final CachingCallSite to_s;

        public ObjectSites(Ruby runtime) {
            RubySymbol dig = runtime.newSymbol("dig");

            dig_array = new FunctionalCachingCallSite(dig);
            dig_hash = new FunctionalCachingCallSite(dig);
            dig_struct = new FunctionalCachingCallSite(dig);
            respond_to_dig = new RespondToCallSite(dig);
            dig_misc = new FunctionalCachingCallSite(dig);
            to_s = new FunctionalCachingCallSite(runtime.newSymbol("to_s"));
        }
    }

    public static class KernelSites {
        public final CheckedSites to_f_checked;
        public final CheckedSites to_s_checked;
        public final CheckedSites to_str_checked;
        public final CallSite to_str;
        public final CallSite getc;
        public final CallSite gets;
        public final CallSite putc;
        public final CallSite puts;
        public final CallSite initialize_copy;
        public final CallSite convert_complex;
        public final CallSite convert_rational;
        public final CheckedSites to_hash_checked;
        public final CallSite write;
        public final CallSite call;

        public KernelSites(Ruby runtime) {
            to_f_checked = new CheckedSites(runtime, runtime.newSymbol("to_f"));
            to_s_checked = new CheckedSites(runtime, runtime.newSymbol("to_s"));
            to_str_checked = new CheckedSites(runtime, runtime.newSymbol("to_str"));
            to_str = new FunctionalCachingCallSite(runtime.newSymbol("to_str"));
            getc = new FunctionalCachingCallSite(runtime.newSymbol("getc"));
            gets = new FunctionalCachingCallSite(runtime.newSymbol("gets"));
            putc = new FunctionalCachingCallSite(runtime.newSymbol("putc"));
            puts = new FunctionalCachingCallSite(runtime.newSymbol("puts"));
            initialize_copy = new FunctionalCachingCallSite(runtime.newSymbol("initialize_copy"));
            convert_complex = new FunctionalCachingCallSite(runtime.newSymbol("convert"));
            convert_rational = new FunctionalCachingCallSite(runtime.newSymbol("convert"));
            to_hash_checked = new CheckedSites(runtime, runtime.newSymbol("to_hash"));
            write = new FunctionalCachingCallSite(runtime.newSymbol("write"));
            call = new FunctionalCachingCallSite(runtime.newSymbol("call"));
        }
    }

    public static class ArraySites {
        public final CheckedSites to_ary_checked;
        public final RespondToCallSite respond_to_to_ary;
        public final CallSite to_ary;
        public final CallSite cmp;
        public final CachingCallSite op_cmp_minmax;
        public final CallSite op_gt_minmax;
        public final CallSite op_lt_minmax;
        public final RespondToCallSite respond_to_begin;
        public final RespondToCallSite respond_to_end;
        public final CallSite begin;
        public final CallSite end;
        public final CallSite exclude_end;
        public final CallSite to_enum;
        public final CallSite op_times;
        public final CallSite op_quo;
        public final CallSite op_exp;
        public final CallSite call;
        public final CallSite sort_by;
        public final CallSite op_equal;
        public final CallSite eql;
        public final CallSite op_cmp_bsearch;
        public final CallSite op_cmp_sort;
        public final CallSite op_gt_sort;
        public final CallSite op_lt_sort;

        public ArraySites(Ruby runtime) {
            to_ary_checked = new CheckedSites(runtime, runtime.newSymbol("to_ary"));
            respond_to_to_ary = new RespondToCallSite(runtime.newSymbol("to_ary"));
            to_ary = new FunctionalCachingCallSite(runtime.newSymbol("to_ary"));
            cmp = new FunctionalCachingCallSite(runtime.newSymbol("<=>"));
            op_cmp_minmax = new FunctionalCachingCallSite(runtime.newSymbol("<=>"));
            op_gt_minmax = new FunctionalCachingCallSite(runtime.newSymbol(">"));
            op_lt_minmax = new FunctionalCachingCallSite(runtime.newSymbol("<"));
            respond_to_begin = new RespondToCallSite(runtime.newSymbol("begin"));
            respond_to_end = new RespondToCallSite(runtime.newSymbol("end"));
            begin = new FunctionalCachingCallSite(runtime.newSymbol("begin"));
            end = new FunctionalCachingCallSite(runtime.newSymbol("end"));
            exclude_end = new FunctionalCachingCallSite(runtime.newSymbol("exclude_end?"));
            to_enum = new FunctionalCachingCallSite(runtime.newSymbol("to_enum"));
            op_times = new FunctionalCachingCallSite(runtime.newSymbol("*"));
            op_quo = new FunctionalCachingCallSite(runtime.newSymbol("/"));
            op_exp = new FunctionalCachingCallSite(runtime.newSymbol("**"));
            call = new FunctionalCachingCallSite(runtime.newSymbol("call"));
            sort_by = new FunctionalCachingCallSite(runtime.newSymbol("sort_by"));
            op_equal = new FunctionalCachingCallSite(runtime.newSymbol("=="));
            eql = new FunctionalCachingCallSite(runtime.newSymbol("eql?"));
            op_cmp_bsearch = new FunctionalCachingCallSite(runtime.newSymbol("<=>"));
            op_cmp_sort = new FunctionalCachingCallSite(runtime.newSymbol("<=>"));
            op_gt_sort = new FunctionalCachingCallSite(runtime.newSymbol(">"));
            op_lt_sort = new FunctionalCachingCallSite(runtime.newSymbol("<"));
        }
    }

    public static class StringSites {
        public final CheckedSites to_str_checked;
        public final RespondToCallSite respond_to_cmp;
        public final RespondToCallSite respond_to_to_str;
        public final CallSite equals;
        public final CachingCallSite cmp;
        public final CallSite hash;
        public final CallSite to_s;
        public final CallSite op_match;
        public final CallSite match;
        public final CallSite match_p;
        public final RespondToCallSite respond_to_begin;
        public final RespondToCallSite respond_to_end;
        public final CallSite begin;
        public final CallSite end;
        public final CallSite exclude_end;
        public final CallSite op_lt;
        public final CallSite op_le;
        public final CallSite succ;
        public final CallSite op_plus;
        public final CallSite op_minus;
        public final CallSite op_lshift;
        public final CallSite op_and;
        public final CheckedSites to_hash_checked;

        public final ThreadContext.RecursiveFunctionEx recursive_cmp = new ThreadContext.RecursiveFunctionEx<IRubyObject>() {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject recv, IRubyObject other, boolean recur) {
                if (recur || !respond_to_cmp.respondsTo(context, other, other)) return context.nil;
                return cmp.call(context, other, other, recv);
            }
        };

        public StringSites (Ruby runtime) {
            to_str_checked = new CheckedSites(runtime, runtime.newSymbol("to_str"));
            respond_to_cmp = new RespondToCallSite(runtime.newSymbol("<=>"));
            respond_to_to_str = new RespondToCallSite(runtime.newSymbol("to_str"));
            equals = new FunctionalCachingCallSite(runtime.newSymbol("=="));
            cmp = new FunctionalCachingCallSite(runtime.newSymbol("<=>"));
            hash = new FunctionalCachingCallSite(runtime.newSymbol("hash"));
            to_s = new FunctionalCachingCallSite(runtime.newSymbol("to_s"));
            op_match = new FunctionalCachingCallSite(runtime.newSymbol("=~"));
            match = new FunctionalCachingCallSite(runtime.newSymbol("match"));
            match_p = new FunctionalCachingCallSite(runtime.newSymbol("match?"));
            respond_to_begin = new RespondToCallSite(runtime.newSymbol("begin"));
            respond_to_end = new RespondToCallSite(runtime.newSymbol("end"));
            begin = new FunctionalCachingCallSite(runtime.newSymbol("begin"));
            end = new FunctionalCachingCallSite(runtime.newSymbol("end"));
            exclude_end = new FunctionalCachingCallSite(runtime.newSymbol("exclude_end?"));
            op_lt = new FunctionalCachingCallSite(runtime.newSymbol("<"));
            op_le = new FunctionalCachingCallSite(runtime.newSymbol("<="));
            succ = new FunctionalCachingCallSite(runtime.newSymbol("succ"));
            op_plus = new FunctionalCachingCallSite(runtime.newSymbol("+"));
            op_minus = new FunctionalCachingCallSite(runtime.newSymbol("-"));
            op_lshift = new FunctionalCachingCallSite(runtime.newSymbol("<<"));
            op_and = new FunctionalCachingCallSite(runtime.newSymbol("&"));
            to_hash_checked = new CheckedSites(runtime, runtime.newSymbol("to_hash"));
        }
    }

    public static class HashSites {
        public final RespondToCallSite respond_to_to_hash;
        public final CallSite default_;
        public final CallSite flatten_bang;
        public final CallSite call;

        public HashSites(Ruby runtime) {
            respond_to_to_hash = new RespondToCallSite(runtime.newSymbol("to_hash"));
            default_ = new FunctionalCachingCallSite(runtime.newSymbol("default"));
            flatten_bang = new FunctionalCachingCallSite(runtime.newSymbol("flatten!"));
            call = new FunctionalCachingCallSite(runtime.newSymbol("call"));
        }
    }

    public static class NumericSites {
        public final RespondToCallSite respond_to_coerce;
        public final CallSite coerce;
        public final CallSite op_cmp;
        public final CallSite op_minus;
        public final CallSite op_quo;
        public final CallSite floor;
        public final CallSite div;
        public final CallSite op_times;
        public final CallSite op_mod;
        public final CachingCallSite op_lt;
        public final CallSite op_gt;
        public final CheckedSites op_lt_checked;
        public final CheckedSites op_gt_checked;
        public final CallSite op_uminus;
        public final CallSite zero;
        public final CallSite op_equals;
        public final CallSite op_plus;
        public final CallSite numerator;
        public final CallSite denominator;
        public final CallSite op_xor;
        public final CallSite abs;
        public final CallSite abs2;
        public final CallSite arg;
        public final CallSite conjugate;
        public final CallSite exact;
        public final CallSite polar;
        public final CallSite real;
        public final CallSite integer;
        public final CallSite divmod;
        public final CallSite inspect;
        public final CallSite to_f;
        public final CallSite to_i;
        public final CallSite to_r;
        public final CallSite to_s;
        public final CallSite truncate;
        public final CallSite op_exp;
        public final CallSite quo;
        public final CallSite op_lshift;
        public final CallSite op_rshift;
        public final CallSite size;
        public final CallSite ceil;

        public NumericSites(Ruby runtime) {
            respond_to_coerce = new RespondToCallSite(runtime.newSymbol("coerce"));
            coerce = new FunctionalCachingCallSite(runtime.newSymbol("coerce"));
            op_cmp = new FunctionalCachingCallSite(runtime.newSymbol("<=>"));
            op_minus = new FunctionalCachingCallSite(runtime.newSymbol("-"));
            op_quo = new FunctionalCachingCallSite(runtime.newSymbol("/"));
            floor = new FunctionalCachingCallSite(runtime.newSymbol("floor"));
            div = new FunctionalCachingCallSite(runtime.newSymbol("div"));
            op_times = new FunctionalCachingCallSite(runtime.newSymbol("*"));
            op_mod = new FunctionalCachingCallSite(runtime.newSymbol("%"));
            op_lt = new FunctionalCachingCallSite(runtime.newSymbol("<"));
            op_gt = new FunctionalCachingCallSite(runtime.newSymbol(">"));
            op_lt_checked = new CheckedSites(runtime, runtime.newSymbol("<"));
            op_gt_checked = new CheckedSites(runtime, runtime.newSymbol(">"));
            op_uminus = new FunctionalCachingCallSite(runtime.newSymbol("-@"));
            zero = new FunctionalCachingCallSite(runtime.newSymbol("zero?"));
            op_equals = new FunctionalCachingCallSite(runtime.newSymbol("=="));
            op_plus = new FunctionalCachingCallSite(runtime.newSymbol("+"));
            numerator = new FunctionalCachingCallSite(runtime.newSymbol("numerator"));
            denominator = new FunctionalCachingCallSite(runtime.newSymbol("denominator"));
            op_xor = new FunctionalCachingCallSite(runtime.newSymbol("^"));
            abs = new FunctionalCachingCallSite(runtime.newSymbol("abs"));
            abs2 = new FunctionalCachingCallSite(runtime.newSymbol("abs2"));
            arg = new FunctionalCachingCallSite(runtime.newSymbol("arg"));
            conjugate = new FunctionalCachingCallSite(runtime.newSymbol("conjugate"));
            exact = new FunctionalCachingCallSite(runtime.newSymbol("exact?"));
            polar = new FunctionalCachingCallSite(runtime.newSymbol("polar"));
            real = new FunctionalCachingCallSite(runtime.newSymbol("real?"));
            integer = new FunctionalCachingCallSite(runtime.newSymbol("integer?"));
            divmod = new FunctionalCachingCallSite(runtime.newSymbol("divmod"));
            inspect = new FunctionalCachingCallSite(runtime.newSymbol("inspect"));
            to_f = new FunctionalCachingCallSite(runtime.newSymbol("to_f"));
            to_i = new FunctionalCachingCallSite(runtime.newSymbol("to_i"));
            to_r = new FunctionalCachingCallSite(runtime.newSymbol("to_r"));
            to_s = new FunctionalCachingCallSite(runtime.newSymbol("to_s"));
            truncate = new FunctionalCachingCallSite(runtime.newSymbol("truncate"));
            op_exp = new FunctionalCachingCallSite(runtime.newSymbol("**"));
            quo = new FunctionalCachingCallSite(runtime.newSymbol("quo"));
            op_lshift = new FunctionalCachingCallSite(runtime.newSymbol("<<"));
            op_rshift = new FunctionalCachingCallSite(runtime.newSymbol(">>"));
            size = new FunctionalCachingCallSite(runtime.newSymbol("size"));
            ceil = new FunctionalCachingCallSite(runtime.newSymbol("ceil"));
        }
    }

    public static class IntegerSites {
        public final CallSite op_gt;
        public final CallSite op_lt;
        public final CallSite op_le;
        public final CallSite op_plus;
        public final CallSite op_minus;
        public final CallSite op_quo;
        public final CallSite op_mod;
        public final CallSite size;

        public IntegerSites(Ruby runtime) {
            op_gt = new FunctionalCachingCallSite(runtime.newSymbol(">"));
            op_lt = new FunctionalCachingCallSite(runtime.newSymbol("<"));
            op_le = new FunctionalCachingCallSite(runtime.newSymbol("<="));
            op_plus = new FunctionalCachingCallSite(runtime.newSymbol("+"));
            op_minus = new FunctionalCachingCallSite(runtime.newSymbol("-"));
            op_quo = new FunctionalCachingCallSite(runtime.newSymbol("/"));
            op_mod = new FunctionalCachingCallSite(runtime.newSymbol("%"));
            size = new FunctionalCachingCallSite(runtime.newSymbol("size"));
        }
    }

    public static class FixnumSites {
        public final CallSite op_plus;
        public final CallSite divmod;
        public final CallSite div;
        public final CallSite op_quo;
        public final CallSite op_times;
        public final CallSite op_mod;
        public final CallSite op_exp;
        public final CallSite quo;
        public final CallSite op_minus;
        public final CallSite op_cmp;
        public final CallSite op_ge;
        public final CallSite op_le;
        public final CallSite op_gt;
        public final CallSite op_lt;
        public final CachingCallSite basic_op_lt;
        public final CachingCallSite basic_op_gt;
        public final CallSite op_exp_complex;
        public final CallSite op_lt_bignum;
        public final CallSite op_exp_rational;
        public final CallSite fdiv;
        public final CheckedSites checked_op_and;
        public final CheckedSites checked_op_or;
        public final CheckedSites checked_op_xor;

        public FixnumSites(Ruby runtime) {
            op_plus = new FunctionalCachingCallSite(runtime.newSymbol("+"));
            divmod = new FunctionalCachingCallSite(runtime.newSymbol("divmod"));
            div = new FunctionalCachingCallSite(runtime.newSymbol("div"));
            op_quo = new FunctionalCachingCallSite(runtime.newSymbol("/"));
            op_times = new FunctionalCachingCallSite(runtime.newSymbol("*"));
            op_mod = new FunctionalCachingCallSite(runtime.newSymbol("%"));
            op_exp = new FunctionalCachingCallSite(runtime.newSymbol("**"));
            quo = new FunctionalCachingCallSite(runtime.newSymbol("quo"));
            op_minus = new FunctionalCachingCallSite(runtime.newSymbol("-"));
            op_cmp = new FunctionalCachingCallSite(runtime.newSymbol("<=>"));
            op_ge = new FunctionalCachingCallSite(runtime.newSymbol(">="));
            op_le = new FunctionalCachingCallSite(runtime.newSymbol("<="));
            op_gt = new FunctionalCachingCallSite(runtime.newSymbol(">"));
            op_lt = new FunctionalCachingCallSite(runtime.newSymbol("<"));
            basic_op_lt = new FunctionalCachingCallSite(runtime.newSymbol("<"));
            basic_op_gt = new FunctionalCachingCallSite(runtime.newSymbol(">"));
            op_exp_complex = new FunctionalCachingCallSite(runtime.newSymbol("**"));
            op_lt_bignum = new FunctionalCachingCallSite(runtime.newSymbol("<"));
            op_exp_rational = new FunctionalCachingCallSite(runtime.newSymbol("**"));
            fdiv = new FunctionalCachingCallSite(runtime.newSymbol("fdiv"));
            checked_op_and = new CheckedSites(runtime, runtime.newSymbol("&"));
            checked_op_or = new CheckedSites(runtime, runtime.newSymbol("|"));
            checked_op_xor = new CheckedSites(runtime, runtime.newSymbol("^"));
        }
    }

    public static class BignumSites {
        public final CallSite op_plus;
        public final CallSite op_minus;
        public final CallSite divmod;
        public final CallSite op_quo;
        public final CallSite div;
        public final CallSite op_mod;
        public final CallSite op_exp;
        public final CallSite op_times;
        public final CallSite quo;
        public final CallSite remainder;
        public final CheckedSites checked_op_and;
        public final CheckedSites checked_op_or;
        public final CheckedSites checked_op_xor;
        public final CallSite op_cmp;
        public final CallSite fdiv;
        public final CachingCallSite basic_op_lt;
        public final CachingCallSite basic_op_gt;

        public BignumSites(Ruby runtime) {
            op_plus = new FunctionalCachingCallSite(runtime.newSymbol("+"));
            op_minus = new FunctionalCachingCallSite(runtime.newSymbol("-"));
            divmod = new FunctionalCachingCallSite(runtime.newSymbol("divmod"));
            op_quo = new FunctionalCachingCallSite(runtime.newSymbol("/"));
            div = new FunctionalCachingCallSite(runtime.newSymbol("div"));
            op_mod = new FunctionalCachingCallSite(runtime.newSymbol("%"));
            op_exp = new FunctionalCachingCallSite(runtime.newSymbol("**"));
            op_times = new FunctionalCachingCallSite(runtime.newSymbol("*"));
            quo = new FunctionalCachingCallSite(runtime.newSymbol("quo"));
            remainder = new FunctionalCachingCallSite(runtime.newSymbol("remainder"));
            checked_op_and = new CheckedSites(runtime, runtime.newSymbol("&"));
            checked_op_or = new CheckedSites(runtime, runtime.newSymbol("|"));
            checked_op_xor = new CheckedSites(runtime, runtime.newSymbol("^"));
            op_cmp = new FunctionalCachingCallSite(runtime.newSymbol("<=>"));
            fdiv = new FunctionalCachingCallSite(runtime.newSymbol("fdiv"));
            basic_op_lt = new FunctionalCachingCallSite(runtime.newSymbol("<"));
            basic_op_gt = new FunctionalCachingCallSite(runtime.newSymbol(">"));
        }
    }

    public static class FloatSites {
        public final CallSite divmod;
        public final CallSite op_quo;
        public final CallSite op_minus;
        public final CallSite op_mod;
        public final CallSite op_times;
        public final CallSite op_plus;
        public final CallSite op_exp;
        public final CallSite op_cmp;
        public final CallSite op_ge;
        public final CallSite op_le;
        public final CallSite op_gt;
        public final CallSite op_lt;
        public final CallSite op_equal;
        public final RespondToCallSite respond_to_infinite;
        public final CallSite infinite;

        public FloatSites(Ruby runtime) {
            divmod = new FunctionalCachingCallSite(runtime.newSymbol("divmod"));
            op_quo = new FunctionalCachingCallSite(runtime.newSymbol("/"));
            op_minus = new FunctionalCachingCallSite(runtime.newSymbol("-"));
            op_mod = new FunctionalCachingCallSite(runtime.newSymbol("%"));
            op_times = new FunctionalCachingCallSite(runtime.newSymbol("*"));
            op_plus = new FunctionalCachingCallSite(runtime.newSymbol("+"));
            op_exp = new FunctionalCachingCallSite(runtime.newSymbol("**"));
            op_cmp = new FunctionalCachingCallSite(runtime.newSymbol("<=>"));
            op_ge = new FunctionalCachingCallSite(runtime.newSymbol(">="));
            op_le = new FunctionalCachingCallSite(runtime.newSymbol("<="));
            op_gt = new FunctionalCachingCallSite(runtime.newSymbol(">"));
            op_lt = new FunctionalCachingCallSite(runtime.newSymbol("<"));
            op_equal = new FunctionalCachingCallSite(runtime.newSymbol("=="));
            respond_to_infinite = new RespondToCallSite(runtime.newSymbol("infinite?"));
            infinite = new FunctionalCachingCallSite(runtime.newSymbol("infinite?"));
        }
    }

    public static class TimeSites {
        public final RespondToCallSite respond_to_cmp;
        public final CallSite cmp;

        public final ThreadContext.RecursiveFunctionEx recursive_cmp = new ThreadContext.RecursiveFunctionEx<IRubyObject>() {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject recv, IRubyObject other, boolean recur) {
                if (recur || !respond_to_cmp.respondsTo(context, other, other)) return context.nil;
                return cmp.call(context, other, other, recv);
            }
        };

        public TimeSites(Ruby runtime) {
            respond_to_cmp = new RespondToCallSite(runtime.newSymbol("<=>"));
            cmp = new FunctionalCachingCallSite(runtime.newSymbol("<=>"));
        }
    }

    public static class EnumerableSites {
        public final CheckedSites size_checked;

        public EnumerableSites(Ruby runtime) {
            size_checked = new CheckedSites(runtime, runtime.newSymbol("size"));
        }
    }

    public static class ComparableSites {
        public final RespondToCallSite respond_to_op_cmp;
        public final CallSite op_cmp;
        public final CallSite op_lt;
        public final CallSite op_gt;

        public ComparableSites(Ruby runtime) {
            respond_to_op_cmp = new RespondToCallSite(runtime.newSymbol("<=>"));
            op_cmp = new FunctionalCachingCallSite(runtime.newSymbol("<=>"));
            op_lt = new FunctionalCachingCallSite(runtime.newSymbol("<"));
            op_gt = new FunctionalCachingCallSite(runtime.newSymbol(">"));
        }
    }

    public static class IOSites {
        public final CheckedSites closed_checked;
        public final CheckedSites close_checked;
        public final CheckedSites to_path_checked1;
        public final CheckedSites to_path_checked2;
        public final RespondToCallSite respond_to_write;
        public final CallSite write;
        public final RespondToCallSite respond_to_read;
        public final CallSite read;
        public final CallSite to_f;
        public final CallSite new_;
        public final RespondToCallSite respond_to_to_int;
        public final RespondToCallSite respond_to_to_io;
        public final RespondToCallSite respond_to_to_hash;

        public IOSites(Ruby runtime) {
            closed_checked = new CheckedSites(runtime, runtime.newSymbol("closed?"));
            close_checked = new CheckedSites(runtime, runtime.newSymbol("close"));
            to_path_checked1 = new CheckedSites(runtime, runtime.newSymbol("to_path"));
            to_path_checked2 = new CheckedSites(runtime, runtime.newSymbol("to_path"));
            respond_to_write = new RespondToCallSite(runtime.newSymbol("write"));
            write = new FunctionalCachingCallSite(runtime.newSymbol("write"));
            respond_to_read = new RespondToCallSite(runtime.newSymbol("read"));
            read = new FunctionalCachingCallSite(runtime.newSymbol("read"));
            to_f = new FunctionalCachingCallSite(runtime.newSymbol("to_f"));
            new_ = new FunctionalCachingCallSite(runtime.newSymbol("new"));
            respond_to_to_int = new RespondToCallSite(runtime.newSymbol("to_int"));
            respond_to_to_io = new RespondToCallSite(runtime.newSymbol("to_io"));
            respond_to_to_hash = new RespondToCallSite(runtime.newSymbol("to_hash"));
        }
    }

    public static class FileSites {
        public final CallSite to_path;
        public final RespondToCallSite respond_to_to_path;
        public final CheckedSites to_time_checked;
        public final CheckedSites to_int_checked;
        public final CheckedSites to_hash_checked;

        public FileSites(Ruby runtime) {
            to_path = new FunctionalCachingCallSite(runtime.newSymbol("to_path"));
            respond_to_to_path = new RespondToCallSite(runtime.newSymbol("to_path"));
            to_time_checked = new CheckedSites(runtime, runtime.newSymbol("to_time"));
            to_int_checked = new CheckedSites(runtime, runtime.newSymbol("to_int"));
            to_hash_checked = new CheckedSites(runtime, runtime.newSymbol("to_hash"));
        }
    }

    public static class TypeConverterSites {
        public final CheckedSites to_f_checked;
        public final CheckedSites to_int_checked;
        public final CheckedSites to_i_checked;
        public final CheckedSites to_ary_checked;
        public final CheckedSites to_a_checked;

        public TypeConverterSites(Ruby runtime) {
            to_f_checked = new CheckedSites(runtime, runtime.newSymbol("to_f"));
            to_int_checked = new CheckedSites(runtime, runtime.newSymbol("to_int"));
            to_i_checked = new CheckedSites(runtime, runtime.newSymbol("to_i"));
            to_ary_checked = new CheckedSites(runtime, runtime.newSymbol("to_ary"));
            to_a_checked = new CheckedSites(runtime, runtime.newSymbol("to_a"));
        }
    }

    public static class HelpersSites {
        public final CallSite hash;

        public final ThreadContext.RecursiveFunctionEx<Ruby> recursive_hash = new ThreadContext.RecursiveFunctionEx<Ruby>() {
            public IRubyObject call(ThreadContext context, Ruby runtime, IRubyObject obj, boolean recur) {
                if (recur) return RubyFixnum.zero(runtime);
                return hash.call(context, obj, obj);
            }
        };

        public HelpersSites(Ruby runtime) {
            hash = new FunctionalCachingCallSite(runtime.newSymbol("hash"));
        }
    }

    public static class IRRuntimeHelpersSites {
        public final CheckedSites to_a_checked;

        public IRRuntimeHelpersSites(Ruby runtime) {
            to_a_checked = new CheckedSites(runtime, runtime.newSymbol("to_a"));
        }
    }

    public static class BigDecimalSites {
        public final CallSite op_plus;
        public final CallSite op_cmp;
        public final CallSite divmod;
        public final CallSite op_times;
        public final CallSite div;
        public final CallSite op_mod;
        public final CallSite op_quo;
        public final CallSite remainder;
        public final CallSite op_or;
        public final CallSite op_and;
        public final CallSite op_minus;

        public BigDecimalSites(Ruby runtime) {
            op_plus = new FunctionalCachingCallSite(runtime.newSymbol("+"));
            op_cmp = new FunctionalCachingCallSite(runtime.newSymbol("<=>"));
            divmod = new FunctionalCachingCallSite(runtime.newSymbol("divmod"));
            op_times = new FunctionalCachingCallSite(runtime.newSymbol("*"));
            div = new FunctionalCachingCallSite(runtime.newSymbol("div"));
            op_mod = new FunctionalCachingCallSite(runtime.newSymbol("%"));
            op_quo = new FunctionalCachingCallSite(runtime.newSymbol("/"));
            remainder = new FunctionalCachingCallSite(runtime.newSymbol("remainder"));
            op_or = new FunctionalCachingCallSite(runtime.newSymbol("|"));
            op_and = new FunctionalCachingCallSite(runtime.newSymbol("&"));
            op_minus = new FunctionalCachingCallSite(runtime.newSymbol("-"));
        }
    }

    public static class ComplexSites {
        public final CallSite op_plus;
        public final CallSite op_quo;
        public final CallSite op_exp;
        public final CallSite op_times;
        public final CallSite op_minus;
        public final CallSite finite;
        public final CallSite infinite;
        public final RespondToCallSite respond_to_to_c;

        public ComplexSites(Ruby runtime) {
            op_plus = new FunctionalCachingCallSite(runtime.newSymbol("+"));
            op_quo = new FunctionalCachingCallSite(runtime.newSymbol("/"));
            op_exp = new FunctionalCachingCallSite(runtime.newSymbol("**"));
            op_times = new FunctionalCachingCallSite(runtime.newSymbol("*"));
            op_minus = new FunctionalCachingCallSite(runtime.newSymbol("-"));
            finite = new FunctionalCachingCallSite(runtime.newSymbol("finite?"));
            infinite = new FunctionalCachingCallSite(runtime.newSymbol("infinite?"));
            respond_to_to_c = new RespondToCallSite(runtime.newSymbol("to_c"));
        }
    }

    public static class RationalSites {
        public final CallSite op_plus;
        public final CallSite op_minus;
        public final CallSite divmod;
        public final CallSite op_quo;
        public final CallSite div;
        public final CallSite mod;
        public final CallSite op_exp;
        public final CallSite op_times;
        public final CallSite quo;
        public final CallSite remainder;
        public final CallSite op_cmp;
        public final CheckedSites to_r_checked;
        public final RespondToCallSite respond_to_to_r;

        public RationalSites(Ruby runtime) {
            op_plus = new FunctionalCachingCallSite(runtime.newSymbol("+"));
            op_minus = new FunctionalCachingCallSite(runtime.newSymbol("-"));
            divmod = new FunctionalCachingCallSite(runtime.newSymbol("divmod"));
            op_quo = new FunctionalCachingCallSite(runtime.newSymbol("/"));
            div = new FunctionalCachingCallSite(runtime.newSymbol("div"));
            mod = new FunctionalCachingCallSite(runtime.newSymbol("mod"));
            op_exp = new FunctionalCachingCallSite(runtime.newSymbol("**"));
            op_times = new FunctionalCachingCallSite(runtime.newSymbol("*"));
            quo = new FunctionalCachingCallSite(runtime.newSymbol("quo"));
            remainder = new FunctionalCachingCallSite(runtime.newSymbol("remainder"));
            op_cmp = new FunctionalCachingCallSite(runtime.newSymbol("<=>"));
            to_r_checked = new CheckedSites(runtime, runtime.newSymbol("to_r"));
            respond_to_to_r = new RespondToCallSite(runtime.newSymbol("to_r"));
        }
    }

    public static class RangeSites {
        public final RespondToCallSite respond_to_succ;
        public final CheckedSites to_int_checked;
        public final RespondToCallSite respond_to_begin;
        public final RespondToCallSite respond_to_end;
        public final CallSite begin;
        public final CallSite end;
        public final CallSite exclude_end;

        public RangeSites(Ruby runtime) {
            respond_to_succ = new RespondToCallSite(runtime.newSymbol("succ"));
            to_int_checked = new CheckedSites(runtime, runtime.newSymbol("to_int"));
            respond_to_begin = new RespondToCallSite(runtime.newSymbol("begin"));
            respond_to_end = new RespondToCallSite(runtime.newSymbol("end"));
            begin = new FunctionalCachingCallSite(runtime.newSymbol("begin"));
            end = new FunctionalCachingCallSite(runtime.newSymbol("end"));
            exclude_end = new FunctionalCachingCallSite(runtime.newSymbol("exclude_end?"));
        }
    }

    public static class WarningSites {
        public final CheckedSites to_int_checked;
        public final CallSite warn;
        public final CallSite write;

        public WarningSites(Ruby runtime) {
            to_int_checked = new CheckedSites(runtime, runtime.newSymbol("to_str"));
            warn = new FunctionalCachingCallSite(runtime.newSymbol("warn"));
            write = new FunctionalCachingCallSite(runtime.newSymbol("write"));
        }
    }

    public static class ZlibSites {
        public final RespondToCallSite reader_respond_to;
        public final RespondToCallSite writer_respond_to;

        public ZlibSites(Ruby runtime) {
            reader_respond_to = new RespondToCallSite(runtime);
            writer_respond_to = new RespondToCallSite(runtime);
        }
    }

    public static class CheckedSites {
        public final RespondToCallSite respond_to_X;
        public final CachingCallSite respond_to_missing;
        public final CachingCallSite method_missing;
        public final CachingCallSite site;
        public final RubySymbol methodName;

        public CheckedSites(Ruby runtime, RubySymbol x) {
            respond_to_X = new RespondToCallSite(x);
            respond_to_missing = new FunctionalCachingCallSite(runtime.newSymbol("respond_to_missing?"));
            method_missing = new FunctionalCachingCallSite(runtime.newSymbol("method_missing"));
            site = new FunctionalCachingCallSite(x);
            methodName = x;
        }
    }
}
