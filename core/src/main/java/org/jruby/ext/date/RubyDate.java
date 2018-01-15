/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2017-2018 The JRuby Team
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
package org.jruby.ext.date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.Chronology;
import org.joda.time.Instant;
import org.joda.time.chrono.GJChronology;
import org.joda.time.chrono.GregorianChronology;
import org.joda.time.chrono.JulianChronology;

import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.RubyDateParser;
import org.jruby.util.TypeConverter;

/**
 * JRuby's <code>Date</code> implementation - 'native' parts.
 * In MRI, since 2.x, all of date.rb has been moved to native (C) code.
 *
 * NOTE: there's still (date).rb parts, class is bootstrapped from .rb
 *
 * @author enebo
 * @author kares
 */
@JRubyModule(name = "Date")
public class RubyDate extends RubyObject {

    //private static final DateTimeZone DEFAULT_DTZ = DateTimeZone.getDefault();

    //private static final GJChronology CHRONO_ITALY_DEFAULT_DTZ = GJChronology.getInstance(DEFAULT_DTZ);

    private static final GJChronology CHRONO_ITALY_UTC = GJChronology.getInstance(DateTimeZone.UTC);

    // The Julian Day Number of the Day of Calendar Reform for Italy
    // and the Catholic countries.
    private static final int ITALY = 2299161; // 1582-10-15

    // The Julian Day Number of the Day of Calendar Reform for England
    // and her Colonies.
    private static final int ENGLAND = 2361222; // 1752-09-14

    // A constant used to indicate that a Date should always use the Julian calendar.
    private static final int JULIAN = (int) Float.POSITIVE_INFINITY; // Infinity.new

    // A constant used to indicate that a Date should always use the Gregorian calendar.
    private static final int GREGORIAN = (int) Float.NEGATIVE_INFINITY; // -Infinity.new

    protected RubyDate(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    private DateTime dt;
    private int off; // @of
    private int start = ITALY; // @sg
    private float subMillis; // @sub_millis

    private static RubyClass getDate(final Ruby runtime) {
        return (RubyClass) runtime.getObject().getConstantAt("Date");
    }

    public RubyDate(Ruby runtime, DateTime dt) {
        super(runtime, getDate(runtime));

        this.dt = dt;
        // TODO calc off?
    }

    RubyDate(Ruby runtime, DateTime dt, int off, int start, float subMillis) {
        super(runtime, getDate(runtime));

        this.dt = dt;
        this.off = off; this.start = start; this.subMillis = subMillis;
    }

    public RubyDate(Ruby runtime, long millis, Chronology chronology) {
        super(runtime, getDate(runtime));

        this.dt = new DateTime(millis, chronology);
    }

    private RubyDate(ThreadContext context, IRubyObject ajd, Chronology chronology, int off) {
        this(context, ajd, chronology, off, 0);
    }

    private RubyDate(ThreadContext context, IRubyObject ajd, Chronology chronology, int off, int start) {
        super(context.runtime, getDate(context.runtime));

        this.dt = new DateTime(initMillis(context, ajd), chronology);
        this.off = off; this.start = start;
    }

    // Date.new!(dt_or_ajd=0, of=0, sg=ITALY, sub_millis=0)

    /**
     * @deprecated internal Date.new!
     */
    @JRubyMethod(name = "new!", meta = true)
    public static IRubyObject new_(ThreadContext context, IRubyObject self) {
        return new RubyDate(context.runtime, 0, CHRONO_ITALY_UTC);
    }

    /**
     * @deprecated internal Date.new!
     */
    @JRubyMethod(name = "new!", meta = true)
    public static IRubyObject new_(ThreadContext context, IRubyObject self, IRubyObject ajd) {
        if (ajd instanceof JavaProxy) { // backwards - compatibility with JRuby's date.rb
            return new RubyDate(context.runtime, (DateTime) JavaUtil.unwrapJavaValue(ajd));
        }
        return new RubyDate(context, ajd, CHRONO_ITALY_UTC, ITALY);
    }

    /**
     * @deprecated internal Date.new!
     */
    @JRubyMethod(name = "new!", meta = true)
    public static IRubyObject new_(ThreadContext context, IRubyObject self, IRubyObject ajd, IRubyObject of) {
        final int off = of.convertToInteger().getIntValue();

        if (ajd instanceof JavaProxy) { // backwards - compatibility with JRuby's date.rb
            RubyDate instance = new RubyDate(context.runtime, (DateTime) JavaUtil.unwrapJavaValue(ajd));
            instance.off = off;
            return instance;
        }
        return new RubyDate(context, ajd, getChronology(ITALY, off), off);
    }

    /**
     * @deprecated internal Date.new!
     */
    @JRubyMethod(name = "new!", meta = true)
    public static IRubyObject new_(ThreadContext context, IRubyObject self, IRubyObject ajd, IRubyObject of, IRubyObject sg) {
        final int off = of.convertToInteger().getIntValue();
        final int start = convertStart(sg);

        if (ajd instanceof JavaProxy) { // backwards - compatibility with JRuby's date.rb
            RubyDate instance = new RubyDate(context.runtime, (DateTime) JavaUtil.unwrapJavaValue(ajd));
            instance.off = off;
            instance.start = start;
            return instance;
        }
        return new RubyDate(context, ajd, getChronology(start, off), off, start);
    }

    private static int convertStart(IRubyObject sg) {
        double start = sg.convertToFloat().getDoubleValue();
        if (start == Double.NEGATIVE_INFINITY) return (int) Float.NEGATIVE_INFINITY;
        if (start == Double.POSITIVE_INFINITY) return (int) Float.POSITIVE_INFINITY;
        return (int) start;
    }

    private static final long DAY_MS = 86400000; // 24 * 60 * 60 * 1000
    private static RubyFixnum DAY_MS_CACHE;

    private long initMillis(final ThreadContext context, IRubyObject ajd) {
        final Ruby runtime = context.runtime;
        // cannot use DateTimeUtils.fromJulianDay since we need to keep ajd as a Rational for precision

        // millis, @sub_millis = ((ajd - UNIX_EPOCH_IN_AJD) * 86400000).divmod(1)

        IRubyObject val;
        if (ajd instanceof RubyFixnum) {
            val = ((RubyFixnum) ajd).op_minus(context, 4881175 / 2);
            val = ((RubyFixnum) val).op_mul(context, DAY_MS);
            val = ((RubyInteger) val).op_plus(context, RubyFixnum.newFixnum(runtime, DAY_MS / 2)); // missing 1/2
        }
        else {
            RubyRational _UNIX_EPOCH_IN_AJD = RubyRational.newRational(runtime, -4881175, 2); // -(1970-01-01)
            val = _UNIX_EPOCH_IN_AJD.op_plus(context, ajd);
            val = val.callMethod(context, "*", DAY_MS(context));
        }

        if (val instanceof RubyFixnum) {
            return ((RubyFixnum) val).getLongValue();
        }

        // fallback
        val = val.callMethod(context, "divmod", RubyFixnum.one(context.runtime));
        IRubyObject millis = ((RubyArray) val).eltInternal(0);
        if (!(millis instanceof RubyFixnum)) { // > java.lang.Long::MAX_VALUE
            throw runtime.newArgumentError("Date out of range: millis=" + millis + " (" + millis.getMetaClass() + ")");
        }

        IRubyObject subMillis = ((RubyArray) val).eltInternal(1);
        this.subMillis = (float) subMillis.convertToFloat().getDoubleValue();

        return ((RubyFixnum) millis).getLongValue();
    }

    private static RubyFixnum DAY_MS(final ThreadContext context) {
        RubyFixnum v = DAY_MS_CACHE;
        if (v == null) v = DAY_MS_CACHE = context.runtime.newFixnum(DAY_MS);
        return v;
    }

    public DateTime getDateTime() { return dt; }

    @Override
    public boolean equals(Object other) {
        if (other instanceof RubyDate) {
            return equals((RubyDate) other);
        }
        return false;
    }

    public final boolean equals(RubyDate that) {
        return this.start == that.start && this.dt.equals(that.dt) && this.subMillis == that.subMillis;
    }

    @Override
    @JRubyMethod(name = "eql?", required = 1)
    public IRubyObject eql_p(IRubyObject other) {
        if (other instanceof RubyDate) {
            return getRuntime().newBoolean( equals((RubyDate) other) );
        }
        return getRuntime().getFalse();
    }

    @Override
    @JRubyMethod(name = "<=>", required = 1)
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyDate) {
            return context.runtime.newFixnum(cmp((RubyDate) other));
        }

        // other (Numeric) - interpreted as an Astronomical Julian Day Number.

        // Comparison is by Astronomical Julian Day Number, including
        // fractional days.  This means that both the time and the
        // timezone offset are taken into account when comparing
        // two DateTime instances.  When comparing a DateTime instance
        // with a Date instance, the time of the latter will be
        // considered as falling on midnight UTC.

        if (other instanceof RubyNumeric) {
            final IRubyObject ajd = ajd(context);
            return context.sites.Numeric.op_cmp.call(context, ajd, ajd, other);
        }

        return fallback_cmp(context, other);
    }

    private int cmp(final RubyDate that) {
        int cmp = this.dt.compareTo(that.dt); // 0, +1, -1

        if (cmp == 0) {
            int diff = (int) (this.subMillis - that.subMillis);
            return diff < 0 ? 1 : ( diff == 0 ? 0 : -1 );
        }

        return cmp;
    }

    private IRubyObject fallback_cmp(ThreadContext context, IRubyObject other) {
        RubyArray res;
        try {
            res = (RubyArray) other.callMethod(context, "coerce", this);
        }
        catch (RaiseException ex) {
            if (ex.getException() instanceof RubyNoMethodError) return context.nil;
            throw ex;
        }
        return res.eltInternal(0).callMethod(context, "<=>", res.eltInternal(1));
    }

    @Override
    public int hashCode() {
        return (int) (dt.getMillis() ^ dt.getMillis() >>> 32);
    }

    @JRubyMethod
    public RubyFixnum hash(ThreadContext context) {
        return hashImpl(context.runtime);
    }

    private RubyFixnum hashImpl(final Ruby runtime) {
        return new RubyFixnum(runtime, this.dt.getMillis());
    }

    @Override
    public RubyFixnum hash() {
        return hashImpl(getRuntime());
    }

    @JRubyMethod // Get the date as a Julian Day Number.
    public IRubyObject jd(ThreadContext context) {
        return RubyFixnum.newFixnum(context.runtime, getJulianDayNumber());
    }

    private long getJulianDayNumber() {
        double day = DateTimeUtils.toJulianDay(dt.getMillis()) + off;
        return (long) Math.floor(day + 0.5);
    }

    @JRubyMethod(name = "julian?")
    public IRubyObject julian_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context.runtime, isJulian());
    }

    @JRubyMethod(name = "gregorian?")
    public IRubyObject gregorian_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context.runtime, ! isJulian());
    }

    public final boolean isJulian() {
        // JULIAN.<=>(numeric)     => +1
        if (start == JULIAN) return true;
        // GREGORIAN.<=>(numeric)  => -1
        if (start == GREGORIAN) return false;

        return getJulianDayNumber() < start;
    }

    // Get the date as an Astronomical Julian Day Number.
    @JRubyMethod
    public IRubyObject ajd(ThreadContext context) {
        final Ruby runtime = context.runtime;

        long num = 210_866_760_000_000l;
        num += this.dt.getMillis() + subMillis;

        return RubyRational.newInstance(context, RubyFixnum.newFixnum(runtime, num), RubyFixnum.newFixnum(runtime, 86_400_000));
    }

    // Get the date as an Astronomical Modified Julian Day Number.
    @JRubyMethod
    public IRubyObject amjd(ThreadContext context) { // ajd - MJD_EPOCH_IN_AJD
        final RubyRational _MJD_EPOCH_IN_AJD = RubyRational.newRational(context.runtime, -4800001, 2); // 1858-11-17
        return _MJD_EPOCH_IN_AJD.op_plus(context, ajd(context));
    }

    // When is the Day of Calendar Reform for this Date object?
    @JRubyMethod
    public IRubyObject start(ThreadContext context) {
        Chronology chrono = dt.getChronology();
        if (chrono instanceof GregorianChronology) {
            return getMetaClass().getConstant("GREGORIAN"); // Date::GREGORIAN (-Date::Infinity)
        }
        if (chrono instanceof JulianChronology) {
            return getMetaClass().getConstant("JULIAN"); // Date::JULIAN (+Date::Infinity)
        }
        long cutover = DateTimeUtils.toJulianDayNumber(((GJChronology) chrono).getGregorianCutover().getMillis());
        return new RubyFixnum(context.runtime, cutover);
    }

    private int adjustJodaYear(int year) {
        if (year < 0 && isJulian()) {
            // Joda-time returns -x for year x BC in JulianChronology (so there is no year 0),
            // while date.rb returns -x+1, following astronomical year numbering (with year 0)
            return ++year;
        }
        return year;
    }

    @JRubyMethod(name = "year")
    public RubyInteger year(ThreadContext context) {
        return RubyFixnum.newFixnum(context.runtime, adjustJodaYear(dt.getYear()));
    }

    @JRubyMethod(name = "yday")
    public RubyInteger yday(ThreadContext context) {
        return RubyFixnum.newFixnum(context.runtime, dt.getDayOfYear());
    }

    @JRubyMethod(name = "mon", alias = "month")
    public RubyInteger mon(ThreadContext context) {
        return RubyFixnum.newFixnum(context.runtime, dt.getMonthOfYear());
    }

    @JRubyMethod(name = "mday", alias = "day")
    public RubyInteger mday(ThreadContext context) {
        return RubyFixnum.newFixnum(context.runtime, dt.getDayOfMonth());
    }

    @JRubyMethod(name = "hour", visibility = Visibility.PRIVATE)
    public RubyInteger hour(ThreadContext context) {
        return RubyFixnum.newFixnum(context.runtime, dt.getHourOfDay());
    }

    @JRubyMethod(name = "min", alias = "minute", visibility = Visibility.PRIVATE)
    public RubyInteger minute(ThreadContext context) {
        return RubyFixnum.newFixnum(context.runtime, dt.getMinuteOfHour());
    }

    @JRubyMethod(name = "sec", alias = "second", visibility = Visibility.PRIVATE)
    public RubyInteger second(ThreadContext context) {
        return RubyFixnum.newFixnum(context.runtime, dt.getSecondOfMinute());
    }

    @JRubyMethod(name = "sec_fraction", alias = "second_fraction", visibility = Visibility.PRIVATE)
    public IRubyObject sec_fraction(ThreadContext context) {
        final Ruby runtime = context.runtime;
        RubyFixnum num = RubyFixnum.newFixnum(runtime, dt.getMillisOfSecond() + (long) subMillis);
        return RubyRational.newInstance(context, num, RubyFixnum.newFixnum(runtime, 1000));
    }

    @JRubyMethod
    public RubyInteger cwyear(ThreadContext context) {
        return RubyFixnum.newFixnum(context.runtime, adjustJodaYear(dt.getWeekyear()));
    }

    @JRubyMethod
    public RubyInteger cweek(ThreadContext context) {
        return RubyFixnum.newFixnum(context.runtime, dt.getWeekOfWeekyear());
    }

    @JRubyMethod
    public RubyInteger cwday(ThreadContext context) {
        // Monday is commercial day-of-week 1; Sunday is commercial day-of-week 7.
        return RubyFixnum.newFixnum(context.runtime, dt.getDayOfWeek());
    }

    @JRubyMethod
    public RubyInteger wday(ThreadContext context) {
        // Sunday is day-of-week 0; Saturday is day-of-week 6.
        return RubyFixnum.newFixnum(context.runtime, dt.getDayOfWeek() % 7);
    }

    //

    private static final int MJD_EPOCH_IN_CJD = 2400001;
    //private static final int UNIX_EPOCH_IN_CJD = 2440588;
    private static final int LD_EPOCH_IN_CJD = 2299160;

    // Get the date as a Modified Julian Day Number.
    @JRubyMethod
    public IRubyObject mjd(ThreadContext context) {
        return RubyFixnum.newFixnum(context.runtime, getJulianDayNumber() - MJD_EPOCH_IN_CJD);
    }

    // Get the date as the number of days since the Day of Calendar
    // Reform (in Italy and the Catholic countries).
    @JRubyMethod
    public IRubyObject ld(ThreadContext context) {
        return RubyFixnum.newFixnum(context.runtime, getJulianDayNumber() - LD_EPOCH_IN_CJD);
    }

    //

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject offset(ThreadContext context) {
        final int offset = dt.getChronology().getZone().getOffset(dt);
        return RubyRational.newInstance(context,
                RubyFixnum.newFixnum(context.runtime, offset),
                RubyFixnum.newFixnum(context.runtime, 86_400_000)
        );
    }

    @JRubyMethod(optional = 1, visibility = Visibility.PRIVATE)
    public IRubyObject new_offset(ThreadContext context, IRubyObject[] args) {
        IRubyObject of = args.length > 0 ? args[0] : RubyFixnum.zero(context.runtime);

        if (of instanceof RubyString) { // of = Rational(zone_to_diff(of) || 0, 86400)
            IRubyObject offset = getMetaClass().callMethod("zone_to_diff", of);
            if (offset == context.nil) offset = RubyFixnum.zero(context.runtime);
            of = RubyRational.newInstance(context, offset, RubyFixnum.newFixnum(context.runtime, 86_400));
        }

        final int off = of.convertToInteger().getIntValue();
        DateTime dt = this.dt.withChronology(getChronology(start, off));
        return new RubyDate(context.runtime, dt, off, start, subMillis);
    }

    @JRubyMethod
    public IRubyObject new_start(ThreadContext context) {
        return newStart(context, ITALY);
    }

    // Create a copy of this Date object using a new Day of Calendar Reform.
    @JRubyMethod
    public IRubyObject new_start(ThreadContext context, IRubyObject sg) {
        return newStart(context, convertStart(sg));
    }

    private RubyDate newStart(ThreadContext context, int start) {
        DateTime dt = this.dt.withChronology(getChronology(start, off));
        return new RubyDate(context.runtime, dt, off, start, subMillis);
    }

    @JRubyMethod
    public IRubyObject italy(ThreadContext context) { return newStart(context, ITALY); }

    @JRubyMethod
    public IRubyObject england(ThreadContext context) { return newStart(context, ENGLAND); }

    @JRubyMethod
    public IRubyObject julian(ThreadContext context) { return newStart(context, JULIAN); }

    @JRubyMethod
    public IRubyObject gregorian(ThreadContext context) { return newStart(context, GREGORIAN); }

    @JRubyMethod(name = "julian_leap?", meta = true)
    public static IRubyObject julian_leap_p(ThreadContext context, IRubyObject self, IRubyObject year) {
        final RubyInteger y = year.convertToInteger();
        return context.runtime.newBoolean(isJulianLeap(y.getLongValue()));
    }

    @JRubyMethod(name = "gregorian_leap?", alias = "leap?", meta = true)
    public static IRubyObject gregorian_leap_p(ThreadContext context, IRubyObject self, IRubyObject year) {
        final RubyInteger y = year.convertToInteger();
        return context.runtime.newBoolean(isGregorianLeap(y.getLongValue()));
    }

    // All years divisible by 4 are leap years in the Julian calendar.
    private static boolean isJulianLeap(final long year) {
        return year % 4 == 0;
    }

    // All years divisible by 4 are leap years in the Gregorian calendar,
    // except for years divisible by 100 and not by 400.
    private static boolean isGregorianLeap(final long year) {
        return year % 4 == 0 && year % 100 != 0 || year % 400 == 0;
    }

    @JRubyMethod(name = "leap?")
    public IRubyObject leap_p(ThreadContext context) {
        final long year = dt.getYear();
        return context.runtime.newBoolean( isJulian() ? isJulianLeap(year) : isGregorianLeap(year) );
    }

    //

    // def jd_to_ajd(jd, fr, of=0) jd + fr - of - Rational(1, 2) end
    private static double jd_to_ajd(long jd, int fr, int of) { return jd + fr - of - 0.5; }

    private static Chronology getChronology(final int sg, final int off) {
        final DateTimeZone zone;
        if (off == 0) {
            if (sg == ITALY) return CHRONO_ITALY_UTC;
            zone = DateTimeZone.UTC;
        }
        else {
            zone = DateTimeZone.forOffsetHours(off);
        }

        switch (sg) {
            case ITALY:
                return GJChronology.getInstance(zone);
            case JULIAN:
                return JulianChronology.getInstance(zone);
            case GREGORIAN:
                return GregorianChronology.getInstance(zone);
        }

        Instant cutover = new Instant(DateTimeUtils.fromJulianDay(jd_to_ajd(sg, 0, 0)));
        return GJChronology.getInstance(zone, cutover);
    }

    private static final ByteList DEFAULT_FORMAT = new ByteList(new byte[] {'%', 'F'});

    public static IRubyObject _strptime(ThreadContext context, IRubyObject str) {
        return _strptime(context, str, context.runtime.newString(DEFAULT_FORMAT));
    }

    public static IRubyObject _strptime(ThreadContext context, IRubyObject string, IRubyObject format) {
        RubyString stringString = (RubyString) TypeConverter.checkStringType(context.runtime, string);
        RubyString formatString = (RubyString) TypeConverter.checkStringType(context.runtime, format);

        return new RubyDateParser().parse(context, formatString, stringString);
    }

    @JRubyMethod(meta = true, required = 1, optional = 1)
    public static IRubyObject _strptime(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        switch(args.length) {
            case 1:
                return _strptime(context, args[0]);
            case 2:
                return _strptime(context, args[0], args[1]);
            default:
                throw context.runtime.newArgumentError(args.length, 1);
        }
    }
}