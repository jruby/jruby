/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Don Schwartz <schwardo@users.sourceforge.net>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2008 Joseph LaFata <joe@quibb.org>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import org.jcodings.specific.USASCIIEncoding;
import org.jruby.anno.CRuby;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.JRubyAPI;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaSites.FloatSites;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.SimpleHash;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalDumper;
import org.jruby.runtime.marshal.MarshalLoader;
import org.jruby.util.ByteList;
import org.jruby.util.ConvertDouble;
import org.jruby.util.Numeric;
import org.jruby.util.io.RubyInputStream;
import org.jruby.util.io.RubyOutputStream;

import static org.jruby.api.Convert.*;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newSharedString;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;
import static org.jruby.util.Numeric.f_abs;
import static org.jruby.util.Numeric.f_add;
import static org.jruby.util.Numeric.f_expt;
import static org.jruby.util.Numeric.f_mul;
import static org.jruby.util.Numeric.f_negate;
import static org.jruby.util.Numeric.f_negative_p;
import static org.jruby.util.Numeric.f_sub;
import static org.jruby.util.Numeric.f_to_r;
import static org.jruby.util.Numeric.frexp;
import static org.jruby.util.Numeric.ldexp;
import static org.jruby.util.Numeric.nurat_rationalize_internal;

/**
  * A representation of a float object
 */
@JRubyClass(name="Float", parent="Numeric")
public class RubyFloat extends RubyNumeric implements Appendable, SimpleHash {
    public static final int ROUNDS = 1;
    public static final int RADIX = 2;
    public static final int MANT_DIG = 53;
    public static final int DIG = 15;
    public static final int MIN_EXP = -1021;
    public static final int MAX_EXP = 1024;
    public static final int MAX_10_EXP = 308;
    public static final int MIN_10_EXP = -307;
    public static final double EPSILON = 2.2204460492503131e-16;
    public static final double INFINITY = Double.POSITIVE_INFINITY;
    public static final double NAN = Double.NaN;
    public static final int FLOAT_DIG = DIG + 2;

    public static RubyClass createFloatClass(ThreadContext context, RubyClass Numeric) {
        RubyClass Float = defineClass(context, "Float", Numeric, ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR).
                reifiedClass(RubyFloat.class).
                kindOf(new RubyModule.JavaClassKindOf(RubyFloat.class)).
                classIndex(ClassIndex.FLOAT).
                defineMethods(context, RubyFloat.class).
                tap(c -> c.singletonClass(context).undefMethods(context, "new")).
                defineConstant(context, "ROUNDS", asFixnum(context, ROUNDS)).
                defineConstant(context, "RADIX", asFixnum(context, RADIX)).
                defineConstant(context, "MANT_DIG", asFixnum(context, MANT_DIG)).
                defineConstant(context, "DIG", asFixnum(context, DIG)).
                defineConstant(context, "MIN_EXP", asFixnum(context, MIN_EXP)).
                defineConstant(context, "MAX_EXP", asFixnum(context, MAX_EXP)).
                defineConstant(context, "MIN_10_EXP", asFixnum(context, MIN_10_EXP)).
                defineConstant(context, "MAX_10_EXP", asFixnum(context, MAX_10_EXP));

        Float.defineConstant(context, "MIN", new RubyFloat(Float, Double.MIN_NORMAL)).
                defineConstant(context, "MAX", new RubyFloat(Float, Double.MAX_VALUE)).
                defineConstant(context, "EPSILON", new RubyFloat(Float, EPSILON)).

                defineConstant(context, "INFINITY", new RubyFloat(Float, INFINITY)).
                defineConstant(context, "NAN", new RubyFloat(Float, NAN));

        return Float;
    }

    final double value;

    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.FLOAT;
    }

    public RubyFloat(Ruby runtime) {
        this(runtime, 0.0);
    }

    public RubyFloat(Ruby runtime, double value) {
        super(runtime.getFloat());
        this.value = value;
        this.setFrozen(true);
    }

    private RubyFloat(RubyClass klass, double value) {
        super(klass);
        this.value = value;
        this.setFrozen(true);
    }

    public RubyClass singletonClass(ThreadContext context) {
        throw typeError(context, "can't define singleton");
    }

    @Override
    public Class<?> getJavaClass() {
        return double.class;
    }

    /**
     * Get the raw float value.  If you want to access this as other Java primitive types then
     * you should use {@link #asLong(ThreadContext)}, {@link #asBigInteger(ThreadContext)}, or
     * {@link #asInt(ThreadContext)}.  Those will ensure that the double will fit into the range and
     * create the type if needed (e.g. make a BigInteger). If you KNOW the double is in the right range
     * then you can just use this method and manually cast it.
     * @return Value of property value.
     */
    @JRubyAPI
    public double getValue() {
        return this.value;
    }

    @Override
    @JRubyAPI
    public BigInteger asBigInteger(ThreadContext context) {
        return RubyBignum.toBigInteger(value);
    }

    @Override
    @JRubyAPI
    public double asDouble(ThreadContext context) {
        return value;
    }

    @Override
    @JRubyAPI
    public int asInt(ThreadContext context) {
        return (int) value;
    }

    @Override
    @JRubyAPI
    public long asLong(ThreadContext context) {
        return (long) value;
    }

    @Override
    public RubyFloat convertToFloat() {
    	return this;
    }

    @Override
    public RubyInteger convertToInteger() {
        return toInteger(metaClass.runtime.getCurrentContext());
    }

    private RubyInteger toInteger(ThreadContext context) {
        return asInteger(context, value > 0.0 ? Math.floor(value) : Math.ceil(value));
    }

    @Deprecated(since = "10.0.0.0")
    public int signum() {
        return signum(getCurrentContext());
    }

    @JRubyAPI
    public int signum(ThreadContext context) {
        return (int) Math.signum(value); // NOTE: (int) NaN ?
    }

    @Override
    @JRubyMethod(name = "negative?")
    public IRubyObject isNegative(ThreadContext context) {
        return asBoolean(context, isNegativeNumber(context));
    }

    @Override
    @JRubyMethod(name = "positive?")
    public IRubyObject isPositive(ThreadContext context) {
        return asBoolean(context, isPositiveNumber(context));
    }

    @Override
    public boolean isNegativeNumber(ThreadContext context) {
        return signum(context) < 0;
    }

    @Override
    public boolean isPositiveNumber(ThreadContext context) {
        return signum(context) > 0;
    }

    public static RubyFloat newFloat(Ruby runtime, double value) {
        return new RubyFloat(runtime, value);
    }

    /*  ================
     *  Instance Methods
     *  ================
     */

    @CRuby(value = "flo_to_s", file = "numeric.c")
    @JRubyMethod(name = {"to_s", "inspect"})
    @Override
    public IRubyObject to_s(ThreadContext context) {
        ByteList buf = new ByteList(24);
        formatFloat(this, buf);
        return newString(context, buf);
    }

    public static final ByteList POSITIVE_INFINITY_TO_S_BYTELIST = new ByteList(ByteList.plain("Infinity"), USASCIIEncoding.INSTANCE, false);
    public static final ByteList NEGATIVE_INFINITY_TO_S_BYTELIST = new ByteList(ByteList.plain("-Infinity"), USASCIIEncoding.INSTANCE, false);
    public static final ByteList NAN_TO_S_BYTELIST = new ByteList(ByteList.plain("NaN"), USASCIIEncoding.INSTANCE, false);

    @CRuby(value = "flo_coerce", file = "numeric.c")
    @JRubyMethod(name = "coerce")
    public IRubyObject coerce(ThreadContext context, IRubyObject other) {
        return newArray(context, RubyKernel.new_float(context, other), this);
    }

    /** flo_uminus
     *
     */
    @JRubyMethod(name = "-@")
    @Override
    public IRubyObject op_uminus(ThreadContext context) {
        return asFloat(context, -value);
    }

    /** flo_plus
     *
     */
    @JRubyMethod(name = "+")
    @Override
    public IRubyObject op_plus(ThreadContext context, IRubyObject other) {
        return switch (other.getMetaClass().getClassIndex()) {
            case INTEGER, FLOAT -> asFloat(context, value + ((RubyNumeric) other).asDouble(context));
            default -> coerceBin(context, sites(context).op_plus, other);
        };
    }

    public IRubyObject op_plus(ThreadContext context, double other) {
        return asFloat(context, value + other);
    }

    /** flo_minus
     *
     */
    @JRubyMethod(name = "-")
    public IRubyObject op_minus(ThreadContext context, IRubyObject other) {
        return switch (other.getMetaClass().getClassIndex()) {
            case INTEGER, FLOAT -> asFloat(context, value - ((RubyNumeric) other).asDouble(context));
            default -> coerceBin(context, sites(context).op_minus, other);
        };
    }

    public IRubyObject op_minus(ThreadContext context, double other) {
        return asFloat(context, value - other);
    }

    /** flo_mul
     *
     */
    @JRubyMethod(name = "*")
    public IRubyObject op_mul(ThreadContext context, IRubyObject other) {
        return switch (other.getMetaClass().getClassIndex()) {
            case INTEGER, FLOAT -> asFloat(context, value * ((RubyNumeric) other).asDouble(context));
            default -> coerceBin(context, sites(context).op_times, other);
        };
    }

    public IRubyObject op_mul(ThreadContext context, double other) {
        return asFloat(context, value * other);
    }

    /**
     * MRI: flo_div
     */
    @JRubyMethod(name = "/")
    public IRubyObject op_div(ThreadContext context, IRubyObject other) { // don't override Numeric#div !
        switch (getMetaClass(other).getClassIndex()) {
            case INTEGER, FLOAT:
                try {
                    return asFloat(context, value / ((RubyNumeric) other).asDouble(context));
                } catch (NumberFormatException nfe) {
                    throw context.runtime.newFloatDomainError(other.toString());
                }
            default:
                return coerceBin(context, sites(context).op_quo, other);
        }
    }

    public IRubyObject op_div(ThreadContext context, double other) { // don't override Numeric#div !
        return asFloat(context, value / other);
    }

    @CRuby(value = "flo_quo", file = "numeric.c")
    @JRubyMethod(name = {"quo", "fdiv"})
    public IRubyObject quo(ThreadContext context, IRubyObject other) {
        return numFuncall(context, this, sites(context).op_quo, other);
    }

    @CRuby(value = "flo_mod", file = "numeric.c")
    @JRubyMethod(name = {"%", "modulo"})
    public IRubyObject op_mod(ThreadContext context, IRubyObject other) {
        switch (getMetaClass(other).getClassIndex()) {
        case INTEGER, FLOAT:
            return op_mod(context, ((RubyNumeric) other).asDouble(context));
        default:
            return coerceBin(context, sites(context).op_mod, other);
        }
    }

    public IRubyObject op_mod(ThreadContext context, double other) {
        if (other == 0) throw context.runtime.newZeroDivisionError();
        // Modelled after c ruby implementation (java /,% not same as ruby)
        double x = value;

        double mod = Math.IEEEremainder(x, other);
        if (other * mod < 0) mod += other;

        return asFloat(context, mod);
    }

    @CRuby(value = "flo_divmod", file = "numeric.c")
    @Override
    @JRubyMethod(name = "divmod")
    public IRubyObject divmod(ThreadContext context, IRubyObject other) {
        switch (getMetaClass(other).getClassIndex()) {
        case INTEGER, FLOAT:
            double y = ((RubyNumeric) other).asDouble(context);
            if (y == 0) throw context.runtime.newZeroDivisionError();
            double x = value;

            double mod = Math.IEEEremainder(x, y);
            // MRI behavior:
            if (Double.isNaN(mod)) throw context.runtime.newFloatDomainError("NaN");

            double div = Math.floor(x / y);

            if (y * mod < 0) mod += y;

            return newArray(context, asInteger(context, div), asFloat(context, mod));
        default:
            return coerceBin(context, sites(context).divmod, other);
        }
    }

    /** flo_pow
     *
     */
    @JRubyMethod(name = "**")
    public IRubyObject op_pow(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
            case INTEGER, FLOAT:
                double d_other = ((RubyNumeric) other).asDouble(context);
                if (value < 0 && (d_other != Math.round(d_other))) {
                    RubyComplex complex = RubyComplex.newComplexRaw(context.runtime, this);
                    return sites(context).op_exp.call(context, complex, complex, other);
                }
                return asFloat(context, Math.pow(value, d_other));
            default:
                return coerceBin(context, sites(context).op_exp, other);
        }
    }

    public IRubyObject op_pow(ThreadContext context, double other) {
        return asFloat(context, Math.pow(value, other));
    }

    @CRuby(value = "flo_eq", file = "numeric.c")
    @JRubyMethod(name = {"==", "==="})
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if (Double.isNaN(value)) return context.fals;

        return switch (other.getMetaClass().getClassIndex()) {
            case INTEGER -> asBoolean(context, cmpInteger(context, (RubyInteger) other) == 0);
            case FLOAT -> asBoolean(context, value == ((RubyFloat) other).value);
            default -> super.op_num_equal(context, other); // Numeric.equal
        };
    }

    /**
     * Compare with an Integer exactly, rather than as doubles, which would round an Integer above
     * 2**53 to the nearest double.
     *
     * @return -1, 0 or 1 as this is less than, equal to or greater than other, or 2 for NaN
     */
    private int cmpInteger(ThreadContext context, RubyInteger other) {
        if (other instanceof RubyFixnum fixnum) {
            int rel = RubyFixnum.floatCmp(fixnum.getValue(), value);
            return rel == 2 ? 2 : -rel;
        }

        IRubyObject rel = ((RubyBignum) other).float_cmp(context, this);
        return rel.isNil() ? 2 : -(int) ((RubyFixnum) rel).getValue();
    }

    public IRubyObject op_equal(ThreadContext context, double other) {
        return Double.isNaN(value) ? context.fals : asBoolean(context, value == other);
    }

    public IRubyObject op_not_equal(ThreadContext context, double other) {
        return Double.isNaN(value) ? context.tru : asBoolean(context, value != other);
    }

    public boolean fastEqual(RubyFloat other) {
        return !Double.isNaN(value) && value == other.value;
    }

    @Override
    public final int compareTo(IRubyObject other) {
        ThreadContext context = metaClass.runtime.getCurrentContext();
        return switch (other.getMetaClass().getClassIndex()) {
            case INTEGER -> Double.isNaN(value) ?
                    Double.compare(value, ((RubyNumeric) other).asDouble(context)) : cmpInteger(context, (RubyInteger) other);
            case FLOAT -> Double.compare(value, ((RubyFloat) other).value);
            default -> (int) toLong(context, coerceCmp(context, sites(context).op_cmp, other));
        };
    }

    @CRuby(value = "flo_cmp", file = "numeric.c")
    @JRubyMethod(name = "<=>")
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case INTEGER:
            int rel = cmpInteger(context, (RubyInteger) other);
            return rel == 2 ? context.nil : asFixnum(context, rel);
        case FLOAT:
            return dbl_cmp(context.runtime, value, ((RubyFloat) other).value);
        default:
            FloatSites sites = sites(context);
            if (Double.isInfinite(value) && sites.respond_to_infinite.respondsTo(context, other, other, true)) {
                IRubyObject infinite = sites.infinite.call(context, other, other);
                if (infinite.isTrue()) {
                    int sign = RubyComparable.cmpint(context, infinite, this, other);

                    return sign > 0 ?
                            asFixnum(context, value > 0.0 ? 0 : -1) :
                            asFixnum(context, value < 0.0 ? 0 : 1);
                }

                return asFixnum(context, value > 0.0 ? 1 : -1);
            }
            return coerceCmp(context, sites.op_cmp, other);
        }
    }

    public IRubyObject op_cmp(ThreadContext context, double other) {
        return dbl_cmp(context.runtime, value, other);
    }

    /** flo_gt
     *
     */
    @JRubyMethod(name = ">")
    public IRubyObject op_gt(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case INTEGER:
            return asBoolean(context, cmpInteger(context, (RubyInteger) other) == 1);
        case FLOAT:
            double b = ((RubyFloat) other).value;
            return asBoolean(context, !Double.isNaN(b) && value > b);
        default:
            return coerceRelOp(context, sites(context).op_gt, other);
        }
    }

    public IRubyObject op_gt(ThreadContext context, double other) {
        return asBoolean(context, !Double.isNaN(other) && value > other);
    }

    @CRuby(value = "flo_ge", file = "numeric.c")
    @JRubyMethod(name = ">=")
    public IRubyObject op_ge(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case INTEGER:
            int rel = cmpInteger(context, (RubyInteger) other);
            return asBoolean(context, rel == 0 || rel == 1);
        case FLOAT:
            double b = ((RubyFloat) other).value;
            return asBoolean(context, !Double.isNaN(b) && value >= b);
        default:
            return coerceRelOp(context, sites(context).op_ge, other);
        }
    }

    public IRubyObject op_ge(ThreadContext context, double other) {
        return asBoolean(context, !Double.isNaN(other) && value >= other);
    }

    @CRuby(value = "flo_lt", file = "numeric.c")
    @JRubyMethod(name = "<")
    public IRubyObject op_lt(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case INTEGER:
            return asBoolean(context, cmpInteger(context, (RubyInteger) other) == -1);
        case FLOAT:
            double b = ((RubyFloat) other).value;
            return asBoolean(context, !Double.isNaN(b) && value < b);
        default:
            return coerceRelOp(context, sites(context).op_lt, other);
		}
    }

    public IRubyObject op_lt(ThreadContext context, double other) {
        return asBoolean(context, !Double.isNaN(other) && value < other);
    }

    @CRuby(value = "flo_le", file = "numeric.c")
    @JRubyMethod(name = "<=")
    public IRubyObject op_le(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case INTEGER:
            return asBoolean(context, cmpInteger(context, (RubyInteger) other) <= 0);
        case FLOAT:
            double b = ((RubyFloat) other).value;
            return asBoolean(context, !Double.isNaN(b) && value <= b);
        default:
            return coerceRelOp(context, sites(context).op_le, other);
		}
	}

    public IRubyObject op_le(ThreadContext context, double other) {
        return asBoolean(context, !Double.isNaN(other) && value <= other);
	}

    @CRuby(value = "flo_eql", file = "numeric.c")
    @JRubyMethod(name = "eql?")
    @Override
    public IRubyObject eql_p(ThreadContext context, IRubyObject other) {
        return equals(other) ? context.tru : context.fals;
    }

    /**
     * short circuit for Float key comparison
     */
    @Override
    public final boolean eql(IRubyObject other) {
        return equals(other);
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof RubyFloat) && equals((RubyFloat) other);
    }

    private boolean equals(RubyFloat that) {
        if ( Double.isNaN(this.value) || Double.isNaN(that.value) ) return false;
        final double val1 = this.value == -0.0 ? 0.0 : this.value;
        final double val2 = that.value == -0.0 ? 0.0 : that.value;
        return Double.doubleToLongBits(val1) == Double.doubleToLongBits(val2);
    }

    @CRuby(value = "flo_hash", file = "numeric.c")
    @JRubyMethod(name = "hash")
    public RubyFixnum hash(ThreadContext context) {
        return asFixnum(context, longHashCode());
    }

    @Override
    public final int hashCode() {
        return (int) longHashCode();
    }

    @Override
    public long longHashCode() {
        return floatHash(value);
    }

    private static long floatHash(double value) {
        final double val = value == 0.0 ? -0.0 : value;
        long hashLong = Double.doubleToLongBits(val);
        return Helpers.multAndMix(Ruby.getHashSeed0(), hashLong);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject to_f() {
        return to_f(getCurrentContext());
    }

    // MRI: flo_fo
    @JRubyMethod(name = "to_f")
    public IRubyObject to_f(ThreadContext context) {
        return this;
    }

    /** flo_abs
     *
     */
    @JRubyMethod(name = "abs")
    @Override
    public IRubyObject abs(ThreadContext context) {
        return Double.doubleToLongBits(value) < 0 ? asFloat(context, Math.abs(value)) : this;
    }

    /** flo_abs/1.9
     *
     */
    @JRubyMethod(name = "magnitude")
    @Override
    public IRubyObject magnitude(ThreadContext context) {
        return abs(context);
    }

    /**
     * MRI: flo_zero_p
     */
    @JRubyMethod(name = "zero?")
    @Override
    public IRubyObject zero_p(ThreadContext context) {
        return asBoolean(context, value == 0.0);
    }

    @Override
    public final boolean isZero(ThreadContext context) {
        return value == 0.0;
    }

    @Override
    public IRubyObject nonzero_p(ThreadContext context) {
        return isZero(context) ? context.nil : this;
    }

    @CRuby(value = "flo_truncate", file = "numeric.c")
    @JRubyMethod(name = {"truncate", "to_i", "to_int"})
    @Override
    public IRubyObject truncate(ThreadContext context) {
        return toInteger(context);
    }

    @CRuby(value = "flo_truncate", file = "numeric.c")
    @JRubyMethod(name = {"truncate", "to_i", "to_int"})
    public IRubyObject truncate(ThreadContext context, IRubyObject n) {
        if (value > 0.0) return floor(context, n);
        return ceil(context, n);
    }

    /** flo_numerator
     *
     */
    @JRubyMethod(name = "numerator")
    @Override
    public IRubyObject numerator(ThreadContext context) {
        if (Double.isInfinite(value) || Double.isNaN(value)) return this;
        return super.numerator(context);
    }

    /** flo_denominator
     *
     */
    @JRubyMethod(name = "denominator")
    @Override
    public IRubyObject denominator(ThreadContext context) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            return RubyFixnum.one(context.runtime);
        }
        return super.denominator(context);
    }

    /** float_to_r, float_decode
     *
     */
    static final int DBL_MANT_DIG = 53;

    @JRubyMethod(name = "to_r")
    public IRubyObject to_r(ThreadContext context) {
        long[] exp = new long[1];
        RubyInteger rf = asInteger(context, ldexp(frexp(value, exp), DBL_MANT_DIG));
        RubyFixnum rn = asFixnum(context, exp[0] - DBL_MANT_DIG);

        return f_mul(context, rf, f_expt(context, asFixnum(context, 2), rn));
    }

    @CRuby(value = "float_rationalize", file = "rational.c")
    @JRubyMethod(name = "rationalize", optional = 1, checkArity = false)
    public IRubyObject rationalize(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 1);

        if (f_negative_p(context, this)) {
            return f_negate(context, f_abs(context, this).rationalize(context, args));
        }

        final Ruby runtime = context.runtime;

        IRubyObject eps, a, b;
        if (args.length != 0) {
            eps = f_abs(context, args[0]);
            a = f_sub(context, this, eps);
            b = f_add(context, this, eps);
        } else {
            long[] exp = new long[1];

            // float_decode_internal
            double f = frexp(value, exp);
            f = ldexp(f, DBL_MANT_DIG);
            long n = exp[0] - DBL_MANT_DIG;

            RubyInteger rf = RubyBignum.newBignorm(runtime, f);
            RubyFixnum rn = asFixnum(context, n);

            if (rf.isZero(context) || toInt(context, rn) >= 0) {
                return RubyRational.newRationalRaw(runtime, rf.op_lshift(context, rn));
            }

            final RubyFixnum one = asFixnum(context, 1);
            RubyInteger den;

            RubyInteger two_times_f = (RubyInteger) rf.op_mul(context, 2);
            den = (RubyInteger) one.op_lshift(context, one.op_minus(context, n));

            a = RubyRational.newRationalRaw(runtime, two_times_f.op_minus(context, 1), den);
            b = RubyRational.newRationalRaw(runtime, two_times_f.op_plus(context, 1), den);
        }

        if (sites(context).op_equal.call(context, a, a, b).isTrue()) return f_to_r(context, this);

        IRubyObject[] ans = nurat_rationalize_internal(context, a, b);

        return RubyRational.newRationalRaw(runtime, ans[0], ans[1]);
    }

    @CRuby(value = "flo_floor", file = "numeric.c")
    @Override
    @JRubyMethod(name = "floor")
    public IRubyObject floor(ThreadContext context) {
        return asInteger(context, Math.floor(value));
    }

    @CRuby(value = "flo_floor", file = "numeric.c")
    @JRubyMethod(name = "floor")
    public IRubyObject floor(ThreadContext context, IRubyObject digits) {
        int ndigits = toInt(context, digits);
        double number = value;

        if (number == 0.0) return ndigits > 0 ? this : asFixnum(context, 0);

        if (ndigits > 0) {
            RubyNumeric[] num = {this};
            long[] binexp = {0};
            frexp(number, binexp);
            if (floatRoundOverflow(ndigits, binexp)) return num[0];
            if (number > 0.0 && floatRoundUnderflow(ndigits, binexp))
                return asFloat(context, 0.0);
            double f = Math.pow(10, ndigits);
            double mul = Math.floor(number * f);
            double res = (mul + 1) / f;
            if (res > number) res = mul / f;
            return asFloat(context, res);
        } else {
            RubyInteger num = asInteger(context, Math.floor(number));
            if (ndigits < 0) num = (RubyInteger) num.floor(context, digits);
            return num;
        }
    }

     @CRuby(value = "float_round_overflow", file = "numeric.c")
     private static boolean floatRoundOverflow(int ndigits, long[] binexp) {

        /* Let `exp` be such that `number` is written as:"0.#{digits}e#{exp}",
           i.e. such that  10 ** (exp - 1) <= |number| < 10 ** exp
           Recall that up to float_dig digits can be needed to represent a double,
           so if ndigits + exp >= float_dig, the intermediate value (number * 10 ** ndigits)
           will be an integer and thus the result is the original number.
           If ndigits + exp <= 0, the result is 0 or "1e#{exp}", so
           if ndigits + exp < 0, the result is 0.
           We have:
                2 ** (binexp-1) <= |number| < 2 ** binexp
                10 ** ((binexp-1)/log_2(10)) <= |number| < 10 ** (binexp/log_2(10))
                If binexp >= 0, and since log_2(10) = 3.322259:
                   10 ** (binexp/4 - 1) < |number| < 10 ** (binexp/3)
                   floor(binexp/4) <= exp <= ceil(binexp/3)
                If binexp <= 0, swap the /4 and the /3
                So if ndigits + floor(binexp/(4 or 3)) >= float_dig, the result is number
                If ndigits + ceil(binexp/(3 or 4)) < 0 the result is 0
        */

        return ndigits >= FLOAT_DIG - (binexp[0] > 0 ? binexp[0] / 4 : binexp[0] / 3 - 1);
    }

    @CRuby(value = "float_round_underflow", file = "numeric.c")
    private static boolean floatRoundUnderflow(int ndigits, long[] binexp) {
        return ndigits < - (binexp[0] > 0 ? binexp[0] / 3 + 1 : binexp[0] / 4);
    }

    @CRuby(value = "flo_ceil", file = "numeric.c")
    @JRubyMethod(name = "ceil")
    @Override
    public IRubyObject ceil(ThreadContext context) {
        return asInteger(context, Math.ceil(value));
    }

    @CRuby(value = "flo_ceil", file = "numeric.c")
    @JRubyMethod(name = "ceil")
    public IRubyObject ceil(ThreadContext context, IRubyObject digits) {
        int ndigits = toInt(context, digits);
        double number = value;

        if (number == 0.0) return ndigits > 0 ? this : asFixnum(context, 0);

        if (ndigits > 0) {
            long[] binexp = {0};
            frexp(number, binexp);
            if (floatRoundOverflow(ndigits, binexp)) return this;
            if (number < 0.0 && floatRoundUnderflow(ndigits, binexp))
                return asFloat(context, 0.0);
            double f = Math.pow(10, ndigits);
            f = Math.ceil(number * f) / f;
            return asFloat(context, f);
        } else {
            IRubyObject num = asInteger(context, Math.ceil(number));
            if (ndigits < 0) num = ((RubyInteger) num).ceil(context, digits);
            return num;
        }
    }

    @CRuby(value = "flo_round", file = "numeric.c")
    @Override
    @JRubyMethod(name = "round")
    public IRubyObject round(ThreadContext context) {
        return roundShared(context, 0, RoundingMode.HALF_UP);
    }

    @CRuby(value = "flo_round", file = "numeric.c")
    @JRubyMethod(name = "round")
    public IRubyObject round(ThreadContext context, IRubyObject arg0) {
        IRubyObject opts = ArgsUtil.getOptionsArg(context, arg0);  // options (only "half" supported right now)
        int digits = opts.isNil() ? toInt(context, arg0) : 0;

        return roundShared(context, digits, getRoundingMode(context, opts));
    }

    @CRuby(value = "flo_round", file = "numeric.c")
    @JRubyMethod(name = "round")
    public IRubyObject round(ThreadContext context, IRubyObject _digits, IRubyObject _opts) {
        IRubyObject opts = ArgsUtil.getOptionsArg(context, _opts);  // options (only "half" supported right now)
        int digits = toInt(context, _digits);

        return roundShared(context, digits, getRoundingMode(context, opts));
    }

    /*
     * MRI: flo_round main body
     */
    public IRubyObject roundShared(ThreadContext context, int ndigits, RoundingMode mode) {
        double f, x;
        double number = value;

        if (number == 0.0) return ndigits > 0 ? this : asFixnum(context, 0);
        if (ndigits < 0) return ((RubyInteger) to_int(context)).roundShared(context, ndigits, mode);
        if (ndigits == 0) return asInteger(context, doRound(context, mode, number, 1.0));
        if (Double.isFinite(value)) {
            long[] binexp = {0};
            frexp(number, binexp);
            if (floatRoundOverflow(ndigits, binexp)) return this;
            if (floatRoundUnderflow(ndigits, binexp)) return asFloat(context, 0);
            if (ndigits > 14) {
                /* In this case, pow(10, ndigits) may not be accurate. */
                return floatRoundByRational(context, asFixnum(context, ndigits), mode);
            }
            f = Math.pow(10, ndigits);
            x = doRound(context, mode, number, f);
            return asFloat(context, x / f);
        }

        return this;
    }

    private static double doRound(ThreadContext context, RoundingMode roundingMode, double number, double scale) {
        switch (roundingMode) {
            case HALF_UP:
                return roundHalfUp(number, scale);
            case HALF_DOWN:
                return roundHalfDown(number, scale);
            case HALF_EVEN:
                return roundHalfEven(number, scale);        }
        throw argumentError(context, "invalid rounding mode: " + roundingMode);
    }

    private static double roundHalfUp(double x, double s) {
        double f, xs = x * s;

        int signum = x >= 0.0 ? 1 : -1;
        xs = xs * signum;
        f = roundHalfUp(xs);
        f = f * signum;
        if (s == 1.0) return f;
        if (x > 0) {
            if ((f + 0.5) / s <= x) f += 1;
            x = f;
        }
        else {
            if ((f - 0.5) / s >= x) f -= 1;
            x = f;
        }
        return x;
    }

    private static double roundHalfDown(double x, double s) {
        double f, xs = x * s;

        int signum = x >= 0.0 ? 1 : -1;
        xs = xs * signum;
        f = roundHalfUp(xs);
        f = f * signum;
        if (x > 0) {
            if ((f - 0.5) / s >= x) f -= 1;
            x = f;
        }
        else {
            if ((f + 0.5) / s <= x) f += 1;
            x = f;
        }
        return x;
    }

    private static double roundHalfUp(double n) {
        double f = n;
        if (f >= 0.0) {
            f = Math.floor(f);

            if (n - f >= 0.5) {
                f += 1.0;
            }
        } else {
            f = Math.ceil(f);

            if (f - n >= 0.5) {
                f -= 1.0;
            }
        }
        return f;
    }

    private static double roundHalfEven(double x, double s) {
        double u, v, us = 0.0, vs, f, d, uf;

        if (x > 0.0) {
            u = Math.floor(x);
            v = x - u;
            us = u * s;
            vs = v * s;
            f = Math.floor(vs);
            uf = us + f;
            d = vs - f;
            if (d > 0.5)
                d = 1.0;
            else if (d == 0.5 || ((double)((uf + 0.5) / s) <= x))
                d = uf % 2.0;
            else
                d = 0.0;
            x = f + d;
        }
        else if (x < 0.0) {
            u = Math.ceil(x);
            v = x - u;
            us = u * s;
            vs = v * s;
            f = Math.ceil(vs);
            uf = us + f;
            d = f - vs;
            if (d > 0.5)
                d = 1.0;
            else if (d == 0.5 || ((double)((uf - 0.5) / s) >= x))
                d = -uf % 2.0;
            else
                d = 0.0;
            x = f - d;
        }
        return us + x;
    }

    @CRuby(value = "rb_flo_round_by_rational", file = "rational.c")
    private IRubyObject floatRoundByRational(ThreadContext context, IRubyObject ndigits, RoundingMode mode) {
        IRubyObject v = ((RubyRational)to_r(context)).roundCommon(context, ndigits, mode);
        return ((RubyRational)v).to_f(context);
    }

    @CRuby(value = "flo_is_nan_p", file = "numeric.c")
    @JRubyMethod(name = "nan?")
    public IRubyObject nan_p(ThreadContext context) {
        return asBoolean(context, isNaN());
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject nan_p() {
        return nan_p(getCurrentContext());
    }

    public boolean isNaN() {
        return Double.isNaN(value);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject infinite_p() {
        return infinite_p(getCurrentContext());
    }

    // MRI: flo_is_infinite_p
    @JRubyMethod(name = "infinite?")
    public IRubyObject infinite_p(ThreadContext context) {
        return Double.isInfinite(value) ?
                asFixnum(context, value < 0 ? -1 : 1) :
                context.nil;
    }

    public boolean isInfinite() {
        return Double.isInfinite(value);
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject finite_p() {
        return finite_p(getCurrentContext());
    }

    // MRI: flo_is_finite_p
    @JRubyMethod(name = "finite?")
    public IRubyObject finite_p(ThreadContext context) {
        return Double.isInfinite(value) || Double.isNaN(value) ? context.fals : context.tru;
    }

    /**
     * The bytes Marshal.dump writes for this Float. They use the same shortest digits as
     * Float#to_s, so they read back as the same value, but a more compact layout: no ".0"
     * on whole numbers ("1"), and exponent notation as soon as it is shorter than padding
     * with zeros ("1e2" for 100.0). The exponent has no "+" and no padding ("1e20", "1e-5").
     * Fixed notation is kept for up to three zeros after the point ("0.0001"). Infinities
     * and NaN have their own names, and -0.0 is "-0".
     */
    @CRuby(value = "w_float", file = "marshal.c")
    private ByteList marshalDump(ThreadContext context) {
        if (Double.isInfinite(value)) return value < 0 ? NEGATIVE_INFINITY_BYTELIST : INFINITY_BYTELIST;
        if (Double.isNaN(value)) return NAN_BYTELIST;

        byte[] bytes = new byte[FORMAT_FLOAT_CAPACITY];
        int pos = 0;

        // test the sign bit so that -0.0 dumps as "-0"
        if (Double.doubleToRawLongBits(value) < 0) bytes[pos++] = '-';

        // see shortestDigits for what digs and decpt mean, and formatFloat for how the digits
        // are moved in place
        long digitsAndPoint = shortestDigits(value, bytes, pos);
        int digs = (int) digitsAndPoint;
        int decpt = (int) (digitsAndPoint >> 32);

        if (decpt < -3 || decpt > digs) {
            // d.ddden or d.ddde-n, used for more than three zeros after the point or any
            // zeros before it. A single digit has no point ("1e20").
            if (digs > 1) {
                System.arraycopy(bytes, pos + 1, bytes, pos + 2, digs - 1);
                bytes[pos + 1] = '.';
                pos++;
            }
            pos += digs;
            bytes[pos++] = 'e';
            int exp = decpt - 1;
            if (exp < 0) {
                bytes[pos++] = '-';
                exp = -exp;
            }
            if (exp >= 100) bytes[pos++] = (byte) ('0' + exp / 100);
            if (exp >= 10) bytes[pos++] = (byte) ('0' + exp / 10 % 10);
            bytes[pos++] = (byte) ('0' + exp % 10);
        } else if (decpt > 0) {
            // ddd or ddd.ddd, with no ".0" on whole numbers
            if (decpt < digs) {
                System.arraycopy(bytes, pos + decpt, bytes, pos + decpt + 1, digs - decpt);
                bytes[pos + decpt] = '.';
                pos++;
            }
            pos += digs;
        } else {
            // 0.000ddd
            System.arraycopy(bytes, pos, bytes, pos + 2 - decpt, digs);
            bytes[pos++] = '0';
            bytes[pos++] = '.';
            for (int i = decpt; i < 0; i++) bytes[pos++] = '0';
            pos += digs;
        }

        return new ByteList(bytes, 0, pos, false);
    }

    // Room for a sign, 17 digits and either "0.000" or ".e-324", with some slack.
    private static final int FORMAT_FLOAT_CAPACITY = 32;

    /**
     * Write the Float#to_s form of self into buf, replacing its contents.
     *
     * The digits are the shortest ones that read back as the same double (see
     * {@link #shortestDigits}). Where the decimal point goes depends on decpt, the number
     * of digits before it: 1.5 has digits "15" and decpt 1, 0.015 has decpt -1 and 1500.0
     * has decpt 4. Fixed notation is used when the point falls inside the digits, when it
     * needs at most DIG (15) digits before the point ("100000000000000.0"), or when it needs
     * at most three zeros after the point ("0.0001"). Anything else is exponent notation
     * ("1.0e+15", "1.0e-05"). Fixed notation always has a digit after the point.
     */
    @CRuby(value = "flo_to_s", file = "numeric.c")
    public static ByteList formatFloat(RubyFloat self, ByteList buf) {
        buf.setRealSize(0);

        double value = self.value;

        if (Double.isInfinite(value)) {
            buf.append(value < 0
                ? NEGATIVE_INFINITY_TO_S_BYTELIST
                : POSITIVE_INFINITY_TO_S_BYTELIST);
            buf.setEncoding(USASCIIEncoding.INSTANCE);
            return buf;
        }

        if (Double.isNaN(value)) {
            buf.append(NAN_TO_S_BYTELIST);
            buf.setEncoding(USASCIIEncoding.INSTANCE);
            return buf;
        }

        buf.ensure(FORMAT_FLOAT_CAPACITY);
        byte[] bytes = buf.getUnsafeBytes();
        int begin = buf.getBegin();
        int pos = begin;

        // test the sign bit so that -0.0 keeps its sign
        if (Double.doubleToRawLongBits(value) < 0) bytes[pos++] = '-';

        // The digits are written at pos, then moved in place to make room for "0.", a "."
        // or padding zeros, which saves a second buffer.
        long digitsAndPoint = shortestDigits(value, bytes, pos);
        int digs = (int) digitsAndPoint;
        int decpt = (int) (digitsAndPoint >> 32);

        if (decpt > 0) {
            if (decpt < digs) {
                // ddd.ddd: shift the fraction digits right by one to insert the point
                System.arraycopy(bytes, pos + decpt, bytes, pos + decpt + 1, digs - decpt);
                bytes[pos + decpt] = '.';
                pos += digs + 1;
            } else if (decpt <= DIG) {
                // ddd000.0: a whole number, padded with zeros up to the point
                pos += digs;
                for (int i = digs; i < decpt; i++) bytes[pos++] = '0';
                bytes[pos++] = '.';
                bytes[pos++] = '0';
            } else {
                pos = formatFloatExponent(bytes, pos, digs, decpt);
            }
        } else if (decpt > -4) {
            // 0.000ddd: shift the digits right past "0." and the -decpt zeros before them
            System.arraycopy(bytes, pos, bytes, pos + 2 - decpt, digs);
            bytes[pos++] = '0';
            bytes[pos++] = '.';
            for (int i = decpt; i < 0; i++) bytes[pos++] = '0';
            pos += digs;
        } else {
            pos = formatFloatExponent(bytes, pos, digs, decpt);
        }

        buf.setRealSize(pos - begin);
        buf.setEncoding(USASCIIEncoding.INSTANCE);
        return buf;
    }

    /**
     * Turn the digits at pos into d.ddde+XX, the exponent form of Float#to_s. There is always
     * a digit after the point ("1.0e+20"), and the exponent always has a sign and at least two
     * digits ("1.0e-05"), like C's "e%+03d". A value with digits "15" and decpt 21 is
     * 1.5 times ten to the 20th, so the exponent is decpt - 1.
     *
     * @return the position after the last byte written
     */
    private static int formatFloatExponent(byte[] bytes, int pos, int digs, int decpt) {
        if (digs > 1) {
            // shift everything after the first digit right by one to insert the point
            System.arraycopy(bytes, pos + 1, bytes, pos + 2, digs - 1);
        } else {
            // a single digit gets a "0" after the point
            bytes[pos + 2] = '0';
            digs++;
        }
        bytes[pos + 1] = '.';
        pos += digs + 1;

        bytes[pos++] = 'e';
        int exp = decpt - 1;
        if (exp < 0) {
            bytes[pos++] = '-';
            exp = -exp;
        } else {
            bytes[pos++] = '+';
        }
        if (exp >= 100) {
            bytes[pos++] = (byte) ('0' + exp / 100);
            exp %= 100;
        }
        bytes[pos++] = (byte) ('0' + exp / 10);
        bytes[pos++] = (byte) ('0' + exp % 10);
        return pos;
    }

    /**
     * Write the shortest decimal digits that round-trip to the given finite value, as dtoa's
     * mode 0 does: no sign, no leading or trailing zeros, and "0" for zero. The value is
     * 0.ddd times ten to the power decpt, so decpt is the number of digits before the
     * decimal point, which is zero or negative for values below 1. For example 1500.0 gives
     * "15" with decpt 4 and 0.015 gives "15" with decpt -1.
     *
     * Since JDK 19, Double.toString uses Raffaello Giulietti's Schubfach algorithm, which
     * yields the shortest digits, so they are taken from its result rather than computed
     * here. Only the layout differs: Double.toString writes "1500.0" or "1.5E-5", so the
     * digits are copied out without the point or exponent, and decpt is worked out from
     * where the point and exponent were.
     *
     * @return decpt in the high 32 bits and the number of digits in the low 32 bits
     */
    @CRuby(value = "ruby_dtoa", file = "missing/dtoa.c", note = "mode 0")
    static long shortestDigits(double value, byte[] out, int off) {
        String str = Double.toString(value);
        int len = str.length();
        int n = 0, intDigits = 0, leadingZeros = 0, exp = 0;
        boolean fraction = false;

        // str is "ddd.ddd" or "d.dddE[-]n", after a "-" for a negative value. Count the
        // digits before the point, skip leading zeros such as those in "0.0015", and copy
        // the rest of the digits out.
        for (int i = str.charAt(0) == '-' ? 1 : 0; i < len; i++) {
            char c = str.charAt(i);
            if (c == '.') {
                fraction = true;
            } else if (c == 'E') {
                exp = Integer.parseInt(str, i + 1, len, 10);
                break;
            } else {
                if (!fraction) intDigits++;
                if (n == 0 && c == '0') {
                    leadingZeros++;
                } else {
                    out[off + n++] = (byte) c;
                }
            }
        }

        // drop trailing zeros, such as the one in "1500.0" or "1.0"
        while (n > 0 && out[off + n - 1] == '0') n--;

        // zero has no nonzero digits, and dtoa reports it as "0" with decpt 1
        if (n == 0) {
            out[off] = '0';
            return (1L << 32) | 1;
        }

        // "1500.0" has 4 digits before the point, "0.0015" has 1 minus 3 leading zeros, and
        // "1.5E-5" has 1 plus the exponent
        int decpt = intDigits - leadingZeros + exp;

        if (n == 2 && Math.abs(value) < Double.MIN_NORMAL) {
            long oneDigit = oneSubnormalDigit(Math.abs(value), out, off, decpt);
            if (oneDigit != 0) return oneDigit;
        }

        return ((long) decpt << 32) | n;
    }

    /**
     * When a single digit would round-trip, Double.toString may still choose two digits if
     * they are closer to the exact value, as with Double.MIN_VALUE ("4.9E-324"). That can only
     * happen to subnormals, which have few significant bits. dtoa wants the shortest digits,
     * so choose the closest single digit that round-trips, if any.
     *
     * @return the result for shortestDigits, or 0 to keep the two digits
     */
    private static long oneSubnormalDigit(double abs, byte[] out, int off, int decpt) {
        int lower = out[off] - '0';
        int exp = decpt - 1;
        // the exact value lies strictly between lower and lower + 1 times 10 ** exp
        boolean lowerFits = Double.parseDouble(lower + "e" + exp) == abs;
        boolean upperFits = Double.parseDouble((lower + 1) + "e" + exp) == abs;

        boolean upper;
        if (lowerFits && upperFits) {
            BigDecimal midpoint = new BigDecimal(BigInteger.valueOf(lower * 10 + 5), -(exp - 1));
            upper = new BigDecimal(abs).compareTo(midpoint) > 0;
        } else if (lowerFits || upperFits) {
            upper = upperFits;
        } else {
            return 0;
        }

        if (!upper) {
            out[off] = (byte) ('0' + lower);
        } else if (lower == 9) {
            out[off] = '1';
            decpt++;
        } else {
            out[off] = (byte) ('0' + lower + 1);
        }
        return ((long) decpt << 32) | 1;
    }

    @Deprecated(since = "10.0.0.0", forRemoval = true)
    @SuppressWarnings("removal")
    public static void marshalTo(RubyFloat aFloat, org.jruby.runtime.marshal.MarshalStream output) throws java.io.IOException {
        var context = aFloat.getRuntime().getCurrentContext();
        output.registerLinkTarget(context, aFloat);
        output.writeString(aFloat.marshalDump(context));
    }

    public static void marshalTo(ThreadContext context, RubyOutputStream out, RubyFloat aFloat, MarshalDumper output) {
        output.registerObject(aFloat);
        output.writeString(out, aFloat.marshalDump(context));
    }

    @Deprecated(since = "10.0.0.0", forRemoval = true)
    @SuppressWarnings("removal")
    public static RubyFloat unmarshalFrom(org.jruby.runtime.marshal.UnmarshalStream input) throws java.io.IOException {
        ByteList value = input.unmarshalString();

        if (value.equals(NAN_BYTELIST)) {
            return RubyFloat.newFloat(input.getRuntime(), RubyFloat.NAN);
        } else if (value.equals(NEGATIVE_INFINITY_BYTELIST)) {
            return RubyFloat.newFloat(input.getRuntime(), Double.NEGATIVE_INFINITY);
        } else if (value.equals(INFINITY_BYTELIST)) {
            return RubyFloat.newFloat(input.getRuntime(), Double.POSITIVE_INFINITY);
        } else {
            return RubyFloat.newFloat(input.getRuntime(), ConvertDouble.byteListToDouble(value, false));
        }
    }

    public static RubyFloat unmarshalFrom(ThreadContext context, RubyInputStream in, MarshalLoader input) {
        ByteList value = input.unmarshalString(context, in);

        if (value.equals(NAN_BYTELIST)) {
            return RubyFloat.newFloat(context.runtime, RubyFloat.NAN);
        } else if (value.equals(NEGATIVE_INFINITY_BYTELIST)) {
            return RubyFloat.newFloat(context.runtime, Double.NEGATIVE_INFINITY);
        } else if (value.equals(INFINITY_BYTELIST)) {
            return RubyFloat.newFloat(context.runtime, Double.POSITIVE_INFINITY);
        } else {
            return RubyFloat.newFloat(context.runtime, ConvertDouble.byteListToDouble(value, false));
        }
    }

    private static final ByteList NAN_BYTELIST = new ByteList("nan".getBytes());
    private static final ByteList NEGATIVE_INFINITY_BYTELIST = new ByteList("-inf".getBytes());
    private static final ByteList INFINITY_BYTELIST = new ByteList("inf".getBytes());

    @Deprecated(since = "10.0.0.0")
    public IRubyObject next_float() {
        return next_float(getCurrentContext());
    }

    @JRubyMethod(name = "next_float")
    public IRubyObject next_float(ThreadContext context) {
        return asFloat(context, Math.nextAfter(value, Double.POSITIVE_INFINITY));
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject prev_float() {
        return prev_float(getCurrentContext());
    }

    @JRubyMethod(name = "prev_float")
    public IRubyObject prev_float(ThreadContext context) {
        return asFloat(context, Math.nextAfter(value, Double.NEGATIVE_INFINITY));
    }

    @Deprecated(since = "10.0.3.0")
    @Override
    public IRubyObject id() {
        long longBits = Double.doubleToLongBits(value);
        long flonum;

        // calculate flonum to use for ID, or fall back on default incremental ID
        if (flonumRange(longBits)) {
            flonum = (Numeric.rotl(longBits, 3) & ~0x01) | 0x02;
        } else if (positiveZero(longBits)) {
            flonum = 0x8000000000000002L;
        } else {
            return super.id();
        }

        return RubyFixnum.newFixnum(metaClass.runtime, flonum);
    }

    /**
     * Produce an object ID for this Float.
     *
     * Values within the "flonum" range will produce a special object ID that emulates the CRuby tagged "flonum" pointer
     * logic. This ID is never registered but can be reversed by ObjectSpace._id2ref using the same bit manipulation as
     * in CRuby.
     *
     * @return the object ID for this Float
     */
    @Override
    public RubyInteger __id__(ThreadContext context) {
        long longBits = Double.doubleToLongBits(value);
        long flonum;

        // calculate flonum to use for ID, or fall back on default incremental ID
        if (flonumRange(longBits)) {
            flonum = (Numeric.rotl(longBits, 3) & ~0x01) | 0x02;
        } else if (positiveZero(longBits)) {
            flonum = 0x8000000000000002L;
        } else {
            return super.__id__(context);
        }

        return asFixnum(context, flonum);
    }

    /**
     * Compare this Float object with the given object and determine whether they are effectively identical.
     *
     * This logic for Float considers all values in the "flonum" range to be identical, since in CRuby they would have
     * the same pointer value (a tagged "flonum" pointer). We do not support flonums, but emulate this behavior for
     * compatibility.
     *
     * @param context the current context
     * @param obj the object with which to compare
     * @return true if this Float and the given object are effectively identical, false otherwise
     */
    @Override
    public IRubyObject equal_p(ThreadContext context, IRubyObject obj) {
        // if flonum, simlulate identity
        if (flonumable(value)) {
            return asBoolean(context, this == obj || eql(obj));
        } else {
            return super.equal_p(context, obj);
        }
    }

    /**
     * Determine if the given double value is representable as a "flonum", a bit-manipulated version of itself that
     * emulates the CRuby "flonum" tagged pointer.
     *
     * @param value the double value in question
     * @return true of the value can be represented as a "flonum", false otherwise
     */
    private static boolean flonumable(double value) {
        long longBits = Double.doubleToLongBits(value);
        return flonumRange(longBits) || positiveZero(longBits);
    }

    /**
     * Determine if the given double bits are in the "flonum" range, excluding positive zero.
     *
     * @param longBits the bits of the double in question
     * @return true if the double is in the non-zero "flonum" range, false otherwise
     */
    private static boolean flonumRange(long longBits) {
        int bits = (int)((longBits >>> 60) & 0x7);
        return longBits != 0x3000000000000000L /* 1.72723e-77 */
                && ((bits-3) & ~0x01) == 0;
    }

    /**
     * Determine of the given double bits represent positive zero.
     *
     * @param longBits the bits of the double in question
     * @return true of the bits represent positive zero, false otherwise
     */
    private static boolean positiveZero(long longBits) {
        return longBits == 0;
    }

    @Override
    public void appendIntoString(RubyString target) {
        target.catWithCodeRange((RubyString) to_s(getRuntime().getCurrentContext()));
    }

    @Override
    public boolean isImmediate() {
        return true;
    }

    private static FloatSites sites(ThreadContext context) {
        return context.sites.Float;
    }
}
