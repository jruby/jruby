package org.jruby.ext.date;

import org.jruby.*;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Convert.toLong;
import static org.jruby.ext.date.RubyDate.*;
import static org.jruby.util.Numeric.*;

abstract class DateUtils {

    /**
     * Convert a Civil Date to a Julian Day Number.
     * +y+, +m+, and +d+ are the year, month, and day of the
     * month.
     * @param y
     * @param m
     * @param d
     * @param sg specifies the Day of Calendar Reform
     * @return the corresponding Julian Day Number
     */
    static long civil_to_jd(int y, int m, int d, double sg) { // MRI: c_civil_to_jd
        double a, b, jd;

        if (m <= 2) {
            y -= 1;
            m += 12;
        }
        a = Math.floor(y / 100.0);
        b = 2 - a + Math.floor(a / 4.0);
        jd = Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + b - 1524;
        if (jd < sg) {
            jd -= b;
        }

        return (long) jd;
    }

    /**
     * Convert a Julian Day Number to a Civil Date.  +jd+ is
     * the Julian Day Number. +sg+ specifies the Day of Calendar Reform.
     * @param jd
     * @param sg specifies the Day of Calendar Reform
     * @return the corresponding [year, month, day_of_month] as a three-element array.
     */
    static int[] jd_to_civil(long jd, double sg) { // MRI: c_jd_to_civil
        double x, a, b, c, d, e, y, m, dom;

        if (jd < sg)
            a = jd;
        else {
            x = Math.floor((jd - 1867216.25) / 36524.25);
            a = jd + 1 + x - Math.floor(x / 4.0);
        }
        b = a + 1524;
        c = Math.floor((b - 122.1) / 365.25);
        d = Math.floor(365.25 * c);
        e = Math.floor((b - d) / 30.6001);
        dom = b - d - Math.floor(30.6001 * e);
        if (e <= 13) {
            m = e - 1;
            y = c - 4716;
        }
        else {
            m = e - 13;
            y = c - 4715;
        }

        return new int[] { (int) y, (int) m, (int) dom };
    }

    /**
     * Convert a (civil) Julian Day Number to an Astronomical Julian
     * Day Number.
     *
     * +jd+ is the Julian Day Number to convert, and +fr+ is a fractional day.
     * +of+ is the offset from UTC as a fraction of a day (defaults to 0).
     *
     * Returns the Astronomical Julian Day Number as a single numeric value.
     */
    // def jd_to_ajd(jd, fr, of=0) jd + fr - of - HALF_DAYS_IN_DAY (1/2) end


    /**
     * Convert an Ordinal Date to a Julian Day Number.
     *
     * +y+ and +d+ are the year and day-of-year to convert.
     * +sg+ specifies the Day of Calendar Reform.
     *
     * Returns the corresponding Julian Day Number.
     */
    static long ordinal_to_jd(int y, int d, final long sg) {
        return find_fdoy(y, sg) + d - 1;
    }

    /**
     * Convert a Julian Day Number to an Ordinal Date.
     *
     * +jd+ is the Julian Day Number to convert.
     * +sg+ specifies the Day of Calendar Reform.
     *
     * Returns the corresponding Ordinal Date as
     * [year, day_of_year]
     */
    static int[] jd_to_ordinal(long jd, final double sg) {
        int y = jd_to_civil(jd, sg)[0];
        long j = find_fdoy(y, (int) sg);
        return new int[] { y, (int) (jd - j + 1) }; // (y, doy)
    }

    /**
     # Convert a Commercial Date to a Julian Day Number.
     #
     # +y+, +w+, and +d+ are the (commercial) year, week of the year,
     # and day of the week of the Commercial Date to convert.
     # +sg+ specifies the Day of Calendar Reform.
     */
    static long commercial_to_jd(int y, int w, int d, final long sg) {
        long j = find_fdoy(y, sg) + 3;
        return (j - (((j - 1) + 1) % 7)) + 7 * (w - 1) + (d - 1);
    }

    /**
     # Convert a Julian Day Number to a Commercial Date
     #
     # +jd+ is the Julian Day Number to convert.
     # +sg+ specifies the Day of Calendar Reform.
     #
     # Returns the corresponding Commercial Date as
     # [commercial_year, week_of_year, day_of_week]
     */
    static int[] jd_to_commercial(long jd, final long sg) {
        int a = jd_to_civil(jd - 3, sg)[0];
        final int y;
        if (jd >= commercial_to_jd(a + 1, 1, 1, sg)) {
            y = a + 1;
        }
        else {
            y = a;
        }
        int w = 1 + (int) ((jd - commercial_to_jd(y, 1, 1, sg)) / 7);
        int d = (int) ((jd + 1) % 7);
        if (d == 0) d = 7;
        return new int[] { y, w, d };
    }

    private static long weeknum_to_jd(int y, int w, int d, int f, final long sg) {
        long a = find_fdoy(y, sg) + 6;
        return (a - ((a - f) + 1) % 7 - 7) + 7 * w + d;
    }

    private static int[] jd_to_weeknum(long jd, int f, final long sg) {
        final int y = jd_to_civil(jd, sg)[0];
        long a = find_fdoy(y, sg) + 6;

        long val = (jd - (a - ((a - f) + 1) % 7) + 7);
        int w = (int) (val / 7), d = (int) (val % 7);

        return new int[] { y, w, d };
    }

    private static long nth_kday_to_jd(int y, int m, int n, int k, final long sg) {
        final long j;
        if (n > 0) {
            j = find_fdom(y, m, sg) - 1;
        }
        else {
            j = find_ldom(y, m, sg) + 7;
        }
        return (j - (((j - k) + 1) % 7)) + 7 * n;
    }

    private static int[] jd_to_nth_kday(long jd, final long sg) {
        final int[] y_m_d = jd_to_civil(jd, sg);
        final int y = y_m_d[0];
        final int m = y_m_d[1];

        long j = find_fdom(y, m, sg);
        // Sunday is day-of-week 0; Saturday is day-of-week 6.
        int jd_to_wday = (int) ((jd + 1) % 7);

        return new int[] { y, m, (int) (((jd - j) / 7) + 1), jd_to_wday };
    }

    static boolean valid_time_p(long h, long min, long s) { // MRI: c_valid_time_p
        if (h < 0) h += 24;
        if (min < 0) min += 60;
        if (s < 0) s += 60;
        return !(h  < 0 || h   > 24 ||
                min < 0 || min > 59 ||
                s   < 0 || s   > 59 ||
                (h == 24 && (min > 0 || s > 0)));
    }

    //private static boolean safe_mul_p(IRubyObject x, long m) {
    //    if (!(x instanceof RubyFixnum)) return false;
    //
    //    long ix = ((RubyFixnum) x).getLongValue();
    //    if (ix < 0) {
    //        if (ix <= (RubyFixnum.MIN / m)) return false;
    //    }
    //    else {
    //        if (ix >= (RubyFixnum.MAX / m)) return false;
    //    }
    //    return true;
    //}

    static IRubyObject day_to_sec(ThreadContext context, IRubyObject d) {
        //if (safe_mul_p(d, DAY_IN_SECONDS)) {
        //    return LONG2FIX(FIX2LONG(d) * DAY_IN_SECONDS);
        //}
        return asFixnum(context, DAY_IN_SECONDS).op_mul(context, d);
    }

    static final int INVALID_OFFSET = Integer.MIN_VALUE;

    static int offset_to_sec(ThreadContext context, IRubyObject of) {
        long n; IRubyObject vs;
        switch (of.getMetaClass().getClassIndex()) {
            case INTEGER:
                long i = ((RubyInteger) of).asLong(context);
                if (i != -1 && i != 0 && i != 1) return INVALID_OFFSET;
	            return (int) i * DAY_IN_SECONDS;
            case FLOAT:
                double d = ((RubyFloat) of).asDouble(context);

                d = d * DAY_IN_SECONDS;
                if (d < -DAY_IN_SECONDS || d > DAY_IN_SECONDS) return INVALID_OFFSET;
	            n = Math.round(d);
                //if (d != n) rb_warning("fraction of offset is ignored");
                return (int) n;
            case STRING:
                RubyClass date = getDate(context);
                vs = sites(context).zone_to_diff.call(context, date, date, of);

                if (!(vs instanceof RubyFixnum fixnum)) return INVALID_OFFSET;
                n = fixnum.getValue();
                if (n < -DAY_IN_SECONDS || n > DAY_IN_SECONDS) return INVALID_OFFSET;
                return (int) n;
//            default:
//                expect_numeric(vof);
//                vof = f_to_r(vof);
//#ifdef CANONICALIZATION_FOR_MATHN
//                if (!k_rational_p(vof))
//                    return offset_to_sec(vof, rof);
//#endif
                /* fall through */
            case RATIONAL:
                vs = day_to_sec(context, of);

                if (!(vs instanceof RubyRational)) {
                    if (!(vs instanceof RubyFixnum vsf)) return INVALID_OFFSET;
                    n = vsf.getValue();
                    if (n < -DAY_IN_SECONDS || n > DAY_IN_SECONDS) return INVALID_OFFSET;
		            return (int) n;
                }

                RubyInteger vn = ((RubyRational) vs).getNumerator();
                RubyInteger vd = ((RubyRational) vs).getDenominator();

                if (vn instanceof RubyFixnum vnf && vd instanceof RubyFixnum vdf && vdf.getValue() == 1)
                    n = vnf.getValue();
                else {
                    vn = (RubyInteger) ((RubyRational) vs).round(context);
                    //if (!f_eqeq_p(vn, vs)) rb_warning("fraction of offset is ignored");
                    if (!(vn instanceof RubyFixnum vnf)) return INVALID_OFFSET;
                    n = vnf.getValue();
                    if (n < -DAY_IN_SECONDS || n > DAY_IN_SECONDS) return INVALID_OFFSET;
                }
	            return (int) n;
        }
        return INVALID_OFFSET; // 0
    }

    static Long find_ldom(int y, int m, final long sg) {
        Long j = null;
        for (int d = 31; d >= 1; d--) {
            j = _valid_civil_p(y, m, d, sg);
            if (j != null) break;
        }
        return j;
    }

    static Long find_fdom(int y, int m, final long sg) {
        Long j = null;
        for (int d = 1; d <= 31; d++) {
            j = _valid_civil_p(y, m, d, sg);
            if (j != null) break;
        }
        return j;
    }

    static Long find_fdoy(int y, final long sg) {
        Long j = null;
        for (int d = 1; d <= 31; d++) {
            j = _valid_civil_p(y, 1, d, sg);
            if (j != null) break;
        }
        return j;
    }

    static Long find_ldoy(int y, final long sg) {
        Long j = null;
        for (int d = 31; d >= 1; d--) {
            j = _valid_civil_p(y, 12, d, sg);
            if (j != null) break;
        }
        return j;
    }

    static Long _valid_civil_p(int y, int m, int d, final long sg) {
        if (d < 0) {
            Long j = find_ldom(y, m, sg);
            if (j == null) return null;
            int[] ny_nm_nd = jd_to_civil(j + d + 1, sg);
            if (y != ny_nm_nd[0] || m != ny_nm_nd[1]) return null;
            d = ny_nm_nd[2];
        }
        long jd = civil_to_jd(y, m, d, sg);

        int[] y_m_d = jd_to_civil(jd, sg);
        if (y != y_m_d[0] || m != y_m_d[1] || d != y_m_d[2]) return null;

        return jd;
    }

    static Long _valid_ordinal_p(int y, int d, final long sg) {
        if (d < 0) {
            Long j = find_ldoy(y, sg);
            if (j == null) return null;
            int[] ny_nd = jd_to_ordinal(j + d + 1, sg);
            if (y != ny_nd[0]) return null;
            d = ny_nd[1];
        }

        long jd = ordinal_to_jd(y, d, sg);

        int[] y_d = jd_to_ordinal(jd, sg);
        if (y != y_d[0] || d != y_d[1]) return null;

        return jd;
    }

    static Long _valid_commercial_p(int y, int w, int d, final long sg) {
        if (d < 0) d += 8;

        if (w < 0) {
            int[] ny_nw_nd = jd_to_commercial(commercial_to_jd(y + 1, 1, 1, sg) + w * 7, sg);
            if (y != ny_nw_nd[0]) return null;
            w = ny_nw_nd[1];
        }
        long jd = commercial_to_jd(y, w, d, sg);

        int[] ny_nw_nd = jd_to_commercial(jd, sg);
        if (y != ny_nw_nd[0] || w != ny_nw_nd[1] || d != ny_nw_nd[2]) return null;

        return jd;
    }

    static Long _valid_weeknum_p(int y, int w, int d, int f, final long sg) {
        if (d < 0) d += 7;

        if (w < 0) {
            int[] ny_nw_nd = jd_to_weeknum(weeknum_to_jd(y + 1, 1, f, f, sg) + w * 7, f, sg);
            if (y != ny_nw_nd[0]) return null;
            w = ny_nw_nd[1];
        }

        long jd = weeknum_to_jd(y, w, d, f, sg);

        int[] ny_nw_nd = jd_to_weeknum(jd, f, sg);
        if (y != ny_nw_nd[0] || w != ny_nw_nd[1] || d != ny_nw_nd[2]) return null;

        return jd;
    }

    static Long _valid_nth_kday_p(int y, int m, int n, int k, final long sg) {
        if (k < 0) k += 7;

        if (n < 0) {
            int val = (y * 12 + m);
            int ny = val / 12, nm = val % 12;
            nm = (nm + 1) / 1;
            int [] ny_nm_nn_nk = jd_to_nth_kday(nth_kday_to_jd(ny, nm, 1, k, sg) + n * 7, sg);
            if (y != ny_nm_nn_nk[0] || m != ny_nm_nn_nk[1]) return null;
            n = ny_nm_nn_nk[2];
        }

        long jd = nth_kday_to_jd(y, m, n, k, sg);
        int[] ny_nm_nn_nk = jd_to_nth_kday(jd, sg);
        if (y != ny_nm_nn_nk[0] || m != ny_nm_nn_nk[1] || n != ny_nm_nn_nk[2] || k != ny_nm_nn_nk[3]) return null;

        return jd;
    }

    // static void decode_year(VALUE y, double style, VALUE *nth, int *ry)
    static int decode_year(ThreadContext context, IRubyObject y, final int style, RubyInteger[] nth) {
        final long period = style < 0 ? CM_PERIOD_GCY : CM_PERIOD_JCY;
        if (y instanceof RubyFixnum yf) {
            long iy = yf.getValue();
            if (iy < RubyFixnum.MAX - 4712) {
                long it = iy + 4712; /* shift */
                long inth = it / period;
                if (inth != 0) it = it % period;

                nth[0] = asFixnum(context, inth);
                return (int) it - 4712; /* unshift */
            }
        }
        // big:
        IRubyObject t;
        t = f_add(context, y, asFixnum(context, 4712)); /* shift */
        nth[0] = (RubyInteger) f_idiv(context, t, asFixnum(context, period));
        if (!f_zero_p(context, nth[0])) { // f_nonzero_p(*nth)
            t = f_mod(context, t, asFixnum(context, period));
        }
        return toInt(context, t) - 4712; /* unshift */
    }

    static long guess_style(ThreadContext context, IRubyObject y, double sg) { /* -/+oo or zero */
        long style = 0;

        if (sg == Double.POSITIVE_INFINITY) { // Double.isInfinite
            style = JULIAN;
        } else if (sg == Double.NEGATIVE_INFINITY) { // Double.isInfinite
            style = GREGORIAN;
        } else if (!(y instanceof RubyFixnum)) {
            style = ((RubyNumeric) y).isPositive(context) ? GREGORIAN : JULIAN;
        } else {
            long iy = toLong(context, y);

            if (iy < REFORM_BEGIN_YEAR)
                style = JULIAN; // Double.POSITIVE_INFINITY;
            else if (iy > REFORM_END_YEAR)
                style = GREGORIAN; // Double.NEGATIVE_INFINITY;
        }
        return style;
    }

    private static JavaSites.DateSites sites(ThreadContext context) {
        return context.sites.Date;
    }

    private static final int JC_PERIOD0 = 1461;		/* 365.25 * 4 */
    private static final int GC_PERIOD0 = 146097; /* 365.2425 * 400 */
    private static final int CM_PERIOD0 = 71149239;	/* (lcm 7 1461 146097) */
    static final int CM_PERIOD = (0xfffffff / CM_PERIOD0 * CM_PERIOD0);
    private static final int CM_PERIOD_JCY = (CM_PERIOD / JC_PERIOD0 * 4);
    private static final int CM_PERIOD_GCY = (CM_PERIOD / GC_PERIOD0 * 400);

}
