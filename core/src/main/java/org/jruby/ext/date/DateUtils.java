package org.jruby.ext.date;

import org.jruby.*;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.ext.date.RubyDate.*;

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
    static int civil_to_jd(int y, int m, int d, double sg) { // MRI: c_civil_to_jd
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

        return (int) jd;
    }

    /**
     * Convert a Julian Day Number to a Civil Date.  +jd+ is
     * the Julian Day Number. +sg+ specifies the Day of Calendar Reform.
     * @param jd
     * @param sg specifies the Day of Calendar Reform
     * @return the corresponding [year, month, day_of_month] as a three-element array.
     */
    static int[] jd_to_civil(int jd, double sg) { // MRI: c_jd_to_civil
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

    static boolean valid_time_p(int h, int min, int s) { // MRI: c_valid_time_p
        if (h < 0) h += 24;
        if (min < 0) min += 60;
        if (s < 0) s += 60;
        return !(h  < 0 || h   > 24 ||
                min < 0 || min > 59 ||
                s   < 0 || s   > 59 ||
                (h == 24 && (min > 0 || s > 0)));
    }

    private static boolean safe_mul_p(IRubyObject x, long m) {
        if (!(x instanceof RubyFixnum)) return false;

        long ix = ((RubyFixnum) x).getLongValue();
        if (ix < 0) {
            if (ix <= (RubyFixnum.MIN / m)) return false;
        }
        else {
            if (ix >= (RubyFixnum.MAX / m)) return false;
        }
        return true;
    }

    static IRubyObject day_to_sec(ThreadContext context, IRubyObject d) {
        //if (safe_mul_p(d, DAY_IN_SECONDS)) {
        //    return LONG2FIX(FIX2LONG(d) * DAY_IN_SECONDS);
        //}
        return RubyFixnum.newFixnum(context.runtime, DAY_IN_SECONDS).op_mul(context, d);
    }

    static final int INVALID_OFFSET = Integer.MIN_VALUE;

    static int offset_to_sec(ThreadContext context, IRubyObject of) {
        long n; IRubyObject vs;
        switch (of.getMetaClass().getClassIndex()) {
            case FIXNUM:
                int i = ((RubyFixnum) of).getIntValue();
                if (i != -1 && i != 0 && i != 1) return INVALID_OFFSET;
	            return i * DAY_IN_SECONDS;
            case FLOAT:
                double d = ((RubyFloat) of).getDoubleValue();

                d = d * DAY_IN_SECONDS;
                if (d < -DAY_IN_SECONDS || d > DAY_IN_SECONDS) return INVALID_OFFSET;
	            n = Math.round(d);
                //if (d != n) rb_warning("fraction of offset is ignored");
                return (int) n;
            case STRING:
                vs = date_zone_to_diff(context, (RubyString) of);
                
                if (!(vs instanceof RubyFixnum)) return INVALID_OFFSET;
                n = ((RubyFixnum) vs).getLongValue();
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
                    if (!(vs instanceof RubyFixnum)) return INVALID_OFFSET;
                    n = ((RubyFixnum) vs).getLongValue();
                    if (n < -DAY_IN_SECONDS || n > DAY_IN_SECONDS) return INVALID_OFFSET;
		            return (int) n;
                }

                RubyInteger vn = (RubyInteger) ((RubyRational) vs).getNumerator();
                RubyInteger vd = (RubyInteger) ((RubyRational) vs).getDenominator();

                if (vn instanceof RubyFixnum && vd instanceof RubyFixnum && vd.getLongValue() == 1)
                    n = ((RubyFixnum) vn).getLongValue();
                else {
                    vn = (RubyInteger) ((RubyRational) vs).round(context);
                    //if (!f_eqeq_p(vn, vs)) rb_warning("fraction of offset is ignored");
                    if (!(vn instanceof RubyFixnum)) return INVALID_OFFSET;
                    n = ((RubyFixnum) vn).getLongValue();
                    if (n < -DAY_IN_SECONDS || n > DAY_IN_SECONDS) return INVALID_OFFSET;
                }
	            return (int) n;
        }
        return INVALID_OFFSET; // 0
    }

}
