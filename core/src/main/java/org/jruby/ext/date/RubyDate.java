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
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.util.ByteList;
import org.jruby.util.ConvertBytes;
import org.jruby.util.RubyDateParser;
import org.jruby.util.TypeConverter;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import static org.jruby.ext.date.DateUtils.*;
import static org.jruby.util.Numeric.*;

/**
 * JRuby's <code>Date</code> implementation - 'native' parts.
 * In MRI, since 2.x, all of date.rb has been moved to native (C) code.
 *
 * NOTE: there's still (date).rb parts, class is bootstrapped from .rb
 *
 * @author enebo
 * @author kares
 */
@JRubyClass(name = "Date")
public class RubyDate extends RubyObject {

    private static final Logger LOG = LoggerFactory.getLogger(RubyDate.class);

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
    static final int JULIAN = (int) Float.POSITIVE_INFINITY; // Infinity.new

    // A constant used to indicate that a Date should always use the Gregorian calendar.
    static final int GREGORIAN = (int) Float.NEGATIVE_INFINITY; // -Infinity.new

    static final int REFORM_BEGIN_YEAR = 1582;
    static final int REFORM_END_YEAR = 1930;

    protected RubyDate(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    DateTime dt;
    int off; // @of
    int start = ITALY; // @sg
    private float subMillis; // @sub_millis

    static RubyClass createDateClass(Ruby runtime) {
        RubyClass Date = runtime.defineClass("Date", runtime.getObject(), ALLOCATOR);
        Date.setReifiedClass(RubyDate.class);
        Date.includeModule(runtime.getComparable());
        Date.defineAnnotatedMethods(RubyDate.class);
        return Date;
    }

    // Julian Day Number day 0 ... `def self.civil(y=-4712, m=1, d=1, sg=ITALY)`
    static final DateTime defaultDateTime = new DateTime(-4712 - 1, 1, 1, 0, 0, CHRONO_ITALY_UTC);

    private static final ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyDate(runtime, klass, defaultDateTime);
        }
    };

    private static RubyClass getDate(final Ruby runtime) {
        return (RubyClass) runtime.getObject().getConstantAt("Date");
    }

    private RubyDate(Ruby runtime) {
        super(runtime, getDate(runtime));
    }

    public RubyDate(Ruby runtime, RubyClass klass, DateTime dt) {
        super(runtime, klass);

        this.dt = dt; // assuming of = 0 (UTC)
    }

    public RubyDate(Ruby runtime, DateTime dt) {
        this(runtime, getDate(runtime), dt);
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

    /**
     * @note since <code>Date.new</code> is a <code>civil</code> alias, this won't ever get used
     * @deprecated kept due AR-JDBC (uses RubyClass.newInstance(...) to 'fast' allocate a Date instance)
     */
    @JRubyMethod(visibility = Visibility.PRIVATE)
    public RubyDate initialize(ThreadContext context, IRubyObject dt) {
        this.dt = (DateTime) JavaUtil.unwrapJavaValue(dt);
        return this;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public RubyDate initialize(ThreadContext context, IRubyObject ajd, IRubyObject of) {
        initialize(context, ajd, of, ITALY);
        return this;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE) // used by marshal_load
    public RubyDate initialize(ThreadContext context, IRubyObject ajd, IRubyObject of, IRubyObject sg) {
        initialize(context, ajd, of, val2sg(context, sg));
        return this;
    }

    private void initialize(final ThreadContext context, IRubyObject arg, IRubyObject of, final int start) {
        final int off = of.convertToInteger().getIntValue();

        this.off = off; this.start = start;

        if (arg instanceof JavaProxy) { // backwards - compatibility with JRuby's date.rb
            this.dt = (DateTime) JavaUtil.unwrapJavaValue(arg);
            return;
        }
        this.dt = new DateTime(initMillis(context, arg), getChronology(start, off));
    }

    static final int DAY_IN_SECONDS = 86_400; // 24 * 60 * 60
    private static final int DAY_MS = 86_400_000; // 24 * 60 * 60 * 1000
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
            val = DAY_MS(context).op_mul(context, val);
        }

        if (val instanceof RubyFixnum) {
            return ((RubyFixnum) val).getLongValue();
        }

        // fallback
        val = ((RubyNumeric) val).divmod(context, RubyFixnum.one(context.runtime));
        IRubyObject millis = ((RubyArray) val).eltInternal(0);
        if (!(millis instanceof RubyFixnum)) { // > java.lang.Long::MAX_VALUE
            throw runtime.newArgumentError("Date out of range: millis=" + millis + " (" + millis.getMetaClass() + ")");
        }

        IRubyObject subMillis = ((RubyArray) val).eltInternal(1);
        this.subMillis = (float) subMillis.convertToFloat().getDoubleValue(); // TODO keep Rational num/del

        return ((RubyFixnum) millis).getLongValue();
    }

    private static RubyFixnum DAY_MS(final ThreadContext context) {
        RubyFixnum v = DAY_MS_CACHE;
        if (v == null) v = DAY_MS_CACHE = context.runtime.newFixnum(DAY_MS);
        return v;
    }

    // Date.new!(dt_or_ajd=0, of=0, sg=ITALY, sub_millis=0)

    /**
     * @deprecated internal Date.new!
     */
    @JRubyMethod(name = "new!", meta = true)
    public static RubyDate new_(ThreadContext context, IRubyObject self) {
        return new RubyDate(context.runtime, 0, CHRONO_ITALY_UTC);
    }

    /**
     * @deprecated internal Date.new!
     */
    @JRubyMethod(name = "new!", meta = true)
    public static RubyDate new_(ThreadContext context, IRubyObject self, IRubyObject ajd) {
        if (ajd instanceof JavaProxy) { // backwards - compatibility with JRuby's date.rb
            return new RubyDate(context.runtime, (DateTime) JavaUtil.unwrapJavaValue(ajd));
        }
        return new RubyDate(context, ajd, CHRONO_ITALY_UTC, ITALY);
    }

    /**
     * @deprecated internal Date.new!
     */
    @JRubyMethod(name = "new!", meta = true)
    public static RubyDate new_(ThreadContext context, IRubyObject self, IRubyObject ajd, IRubyObject of) {
        return new RubyDate(context.runtime).initialize(context, ajd, of);
    }

    /**
     * @deprecated internal Date.new!
     */
    @JRubyMethod(name = "new!", meta = true)
    public static RubyDate new_(ThreadContext context, IRubyObject self, IRubyObject ajd, IRubyObject of, IRubyObject sg) {
        return new RubyDate(context.runtime).initialize(context, ajd, of, sg);
    }

    // Date.civil([year=-4712[, month=1[, mday=1[, start=Date::ITALY]]]])
    // Date.new([year=-4712[, month=1[, mday=1[, start=Date::ITALY]]]])

    @JRubyMethod(name = "civil", alias = "new", meta = true)
    public static RubyDate civil(ThreadContext context, IRubyObject self) {
        return new RubyDate(context.runtime, defaultDateTime);
    }

    @JRubyMethod(name = "civil", alias = "new", meta = true)
    public static RubyDate civil(ThreadContext context, IRubyObject self, IRubyObject year) {
        int y = getYear(year);
        final DateTime dt;
        try {
            dt = defaultDateTime.withYear(y);
        }
        catch (IllegalArgumentException ex) {
            throw context.runtime.newArgumentError("invalid date");
        }
        return new RubyDate(context.runtime, dt);
    }

    @JRubyMethod(name = "civil", alias = "new", meta = true)
    public static RubyDate civil(ThreadContext context, IRubyObject self, IRubyObject year, IRubyObject month) {
        int y = getYear(year);
        int m = getMonth(month);
        final DateTime dt;
        final Chronology chronology = defaultDateTime.getChronology();
        long millis = defaultDateTime.getMillis();
        try {
            millis = chronology.year().set(millis, y);
            millis = chronology.monthOfYear().set(millis, m);
            dt = defaultDateTime.withMillis(millis);
        }
        catch (IllegalArgumentException ex) {
            throw context.runtime.newArgumentError("invalid date");
        }
        return new RubyDate(context.runtime, dt);
    }

    @JRubyMethod(name = "civil", alias = "new", meta = true)
    public static RubyDate civil(ThreadContext context, IRubyObject self, IRubyObject year, IRubyObject month, IRubyObject mday) {
        // return civil(context, self, new IRubyObject[] { year, month, mday, RubyFixnum.newFixnum(context.runtime, ITALY) });
        final int y = getYear(year);
        final int m = getMonth(month);
        final int d = mday.convertToInteger().getIntValue();
        return civilImpl(context, y, m ,d, defaultDateTime.getChronology());
    }

    private static RubyDate civilImpl(ThreadContext context, final int y, final int m, final int d, final Chronology chronology) {
        DateTime dt;
        try {
            if (d >= 0) { // let d == 0 fail (raise 'invalid date')
                dt = new DateTime(y, m, d, 0, 0, chronology);
            }
            else {
                dt = new DateTime(y, m, 1, 0, 0, chronology);
                long millis = dt.getMillis();
                int last = chronology.dayOfMonth().getMaximumValue(millis);
                millis = chronology.dayOfMonth().set(millis, last + d + 1); // d < 0 (d == -1 -> d == 31)
                dt = dt.withMillis(millis);
            }
        }
        catch (IllegalArgumentException ex) {
            if (context.runtime.isDebug() || LOG.isDebugEnabled()) LOG.info(ex);
            throw context.runtime.newArgumentError("invalid date");
        }
        return new RubyDate(context.runtime, dt);
    }

    @JRubyMethod(name = "civil", alias = "new", meta = true, required = 4)
    public static RubyDate civil(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        // IRubyObject year, IRubyObject month, IRubyObject mday, IRubyObject start

        final int sg = val2sg(context, args[3]);
        final int y = (sg > 0) ? getYear(args[0]) : args[0].convertToInteger().getIntValue();
        final int m = getMonth(args[1]);
        final int d = args[2].convertToInteger().getIntValue();

        RubyDate date = civilImpl(context, y, m, d, getChronology(sg, 0));
        date.start = sg;
        return date;

        //Long jd = DateUtils._valid_civil_p(y, m, d, sg);
        //if (jd == null) throw context.runtime.newArgumentError("invalid date");
        //
        //final Ruby runtime = context.runtime;
        //RubyFloat ajd = RubyFloat.newFloat(runtime, jd_to_ajd(jd, 0, 0));
        //
        //return new RubyDate(runtime).initialize(context, ajd, RubyFixnum.zero(runtime), args[3]);
    }

    // NOTE: no Bignum special care since JODA does not support 'huge' years anyway
    private static int getYear(IRubyObject year) {
        int y = year.convertToInteger().getIntValue();
        return (y <= 0) ? --y : y; // due julian date calc -> see adjustJodaYear
    }

    private static int getMonth(IRubyObject month) {
        int m = month.convertToInteger().getIntValue();
        return (m < 0) ? m + 13 : m;
    }

    @JRubyMethod(name = "valid_civil?", alias = "valid_date?", meta = true, required = 3, optional = 1)
    public static IRubyObject valid_civil_p(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        final Long jd = validCivilImpl(context, args);
        return jd == null ? context.fals : context.tru;
    }

    private static Long validCivilImpl(ThreadContext context, IRubyObject[] args) {
        final int sg = args.length > 3 ? val2sg(context, args[3]) : GREGORIAN;
        final int y = (sg > 0) ? getYear(args[0]) : args[0].convertToInteger().getIntValue();
        final int m = getMonth(args[1]);
        final int d = args[2].convertToInteger().getIntValue();

        return DateUtils._valid_civil_p(y, m, d, sg);
    }

    /**
     # Create a new Date object representing today.
     #
     # +sg+ specifies the Day of Calendar Reform.
     **/

    @JRubyMethod(meta = true)
    public static RubyDate today(ThreadContext context, IRubyObject self) { // sg=ITALY
        return new RubyDate(context.runtime, new DateTime(CHRONO_ITALY_UTC).withTimeAtStartOfDay());
    }

    @JRubyMethod(meta = true)
    public static RubyDate today(ThreadContext context, IRubyObject self, IRubyObject sg) {
        final int start = val2sg(context, sg);
        return new RubyDate(context.runtime, new DateTime(getChronology(start, 0)).withTimeAtStartOfDay(), 0, start, 0);
    }

    @Deprecated // NOTE: should go away once no date.rb is using it
    @JRubyMethod(name = "_valid_civil?", meta = true, required = 3, optional = 1, visibility = Visibility.PRIVATE)
    public static IRubyObject _valid_civil_p(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        final Long jd = validCivilImpl(context, args);
        return jd == null ? context.nil : RubyFixnum.newFixnum(context.runtime, jd);
    }

    @Deprecated // NOTE: should go away once no date.rb is using it
    @JRubyMethod(name = "_valid_civil?", required = 3, optional = 1, visibility = Visibility.PRIVATE)
    public IRubyObject _valid_civil_p(ThreadContext context, IRubyObject[] args) {
        final Long jd = validCivilImpl(context, args);
        return jd == null ? context.nil : RubyFixnum.newFixnum(context.runtime, jd);
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
        return f_cmp(context, res.eltInternal(0), res.eltInternal(1));
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
        //if (start == JULIAN) return true;
        // GREGORIAN.<=>(numeric)  => -1
        //if (start == GREGORIAN) return false;

        return getJulianDayNumber() < start;
    }

    // Get the date as an Astronomical Julian Day Number.
    @JRubyMethod
    public IRubyObject ajd(ThreadContext context) {
        final Ruby runtime = context.runtime;

        long num = 210_866_760_000_000l;
        num += this.dt.getMillis() + subMillis;

        return RubyRational.newInstance(context, RubyFixnum.newFixnum(runtime, num), RubyFixnum.newFixnum(runtime, DAY_MS));
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

    // Get any fractional day part of the date.
    @JRubyMethod(name = "day_fraction")
    public IRubyObject day_fraction(ThreadContext context) {
        long ms = dt.getSecondOfDay() * 1000 + dt.getMillisOfSecond() + (long) subMillis;
        return RubyRational.newRationalCanonicalize(context, ms, DAY_MS);
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
        return RubyRational.newRationalCanonicalize(context, dt.getMillisOfSecond() + (long) subMillis, 1000);
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
        return RubyRational.newRationalCanonicalize(context, offset, DAY_MS);
    }

    @JRubyMethod(optional = 1, visibility = Visibility.PRIVATE)
    public IRubyObject new_offset(ThreadContext context, IRubyObject[] args) {
        IRubyObject of = args.length > 0 ? args[0] : RubyFixnum.zero(context.runtime);

        final int off = val2off(context, of);
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
        return newStart(context, val2sg(context, sg));
    }

    private RubyDate newStart(ThreadContext context, final int start) {
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

    @JRubyMethod(name = "+")
    public IRubyObject op_plus(ThreadContext context, IRubyObject n) {
        if (n instanceof RubyFixnum) {
            int days = n.convertToInteger().getIntValue();
            return new RubyDate(context.runtime, dt.plusDays(+days), off, start, subMillis);
        }
        if (n instanceof RubyNumeric) {
            return op_plus_numeric(context, (RubyNumeric) n);
        }
        throw context.runtime.newTypeError("expected numeric");
    }

    private IRubyObject op_plus_numeric(ThreadContext context, RubyNumeric n) {
        final Ruby runtime = context.runtime;
        // ms, sub = (n * 86_400_000).divmod(1)
        // sub = 0 if sub == 0 # avoid Rational(0, 1)
        // sub_millis = @sub_millis + sub
        // if sub_millis >= 1
        //   sub_millis -= 1
        //   ms += 1
        // end
        IRubyObject val = n.callMethod(context, "*", RubyFixnum.newFixnum(runtime, DAY_MS));

        RubyArray res = (RubyArray) ((RubyNumeric) val).divmod(context, RubyFixnum.one(context.runtime));
        long ms = ((RubyInteger) res.eltInternal(0)).getLongValue();
        RubyNumeric sub = (RubyNumeric) res.eltInternal(1);
        if ( sub.zero_p(context).isTrue() ) sub = RubyFixnum.zero(runtime); // avoid Rational(0, 1)
        double sub_millis = subMillis + sub.getDoubleValue();
        if (sub_millis >= 1) {
            sub_millis -= 1; ms += 1;
        }
        return new RubyDate(runtime, dt.plus(ms), off, start, (float) sub_millis);
    }

    @JRubyMethod(name = "-")
    public IRubyObject op_minus(ThreadContext context, IRubyObject n) {
        if (n instanceof RubyFixnum) {
            int days = n.convertToInteger().getIntValue();
            return new RubyDate(context.runtime, dt.plusDays(-days), off, start, subMillis);
        }
        if (n instanceof RubyNumeric) {
            return op_plus_numeric(context, (RubyNumeric) ((RubyNumeric) n).op_uminus(context));
        }
        if (n instanceof RubyDate) {
            return op_minus_date(context, (RubyDate) n);
        }
        throw context.runtime.newTypeError("expected numeric or date");
    }

    private IRubyObject op_minus_date(ThreadContext context, final RubyDate that) {
        long diff = this.dt.getMillis() - that.dt.getMillis();
        float diff_sub = this.subMillis - that.subMillis;
        if (diff_sub != 0) diff += diff_sub;
        return RubyRational.newRationalCanonicalize(context, diff, DAY_MS);
    }

    // Return a new Date one day after this one.
    @JRubyMethod(name = "next", alias = "succ")
    public IRubyObject next(ThreadContext context) {
        return next_day(context);
    }

    @JRubyMethod
    public IRubyObject next_day(ThreadContext context) {
        return new RubyDate(context.runtime, dt.plusDays(+1), off, start, subMillis);
    }

    @JRubyMethod
    public IRubyObject next_day(ThreadContext context, IRubyObject n) {
        int days = n.convertToInteger().getIntValue();
        return new RubyDate(context.runtime, dt.plusDays(+days), off, start, subMillis);
    }

    @JRubyMethod
    public IRubyObject prev_day(ThreadContext context) {
        return new RubyDate(context.runtime, dt.plusDays(-1), off, start, subMillis);
    }

    @JRubyMethod
    public IRubyObject prev_day(ThreadContext context, IRubyObject n) {
        int days = n.convertToInteger().getIntValue();
        return new RubyDate(context.runtime, dt.plusDays(-days), off, start, subMillis);
    }

    @JRubyMethod
    public IRubyObject next_month(ThreadContext context) {
        return new RubyDate(context.runtime, dt.plusMonths(+1), off, start, subMillis);
    }

    @JRubyMethod
    public IRubyObject next_month(ThreadContext context, IRubyObject n) {
        int months = n.convertToInteger().getIntValue();
        return new RubyDate(context.runtime, dt.plusMonths(+months), off, start, subMillis);
    }

    @JRubyMethod
    public IRubyObject prev_month(ThreadContext context) {
        return new RubyDate(context.runtime, dt.plusMonths(-1), off, start, subMillis);
    }

    @JRubyMethod
    public IRubyObject prev_month(ThreadContext context, IRubyObject n) {
        int months = n.convertToInteger().getIntValue();
        return new RubyDate(context.runtime, dt.plusMonths(-months), off, start, subMillis);
    }

    @JRubyMethod(name = ">>")
    public IRubyObject shift_fw(ThreadContext context, IRubyObject n) {
        return next_month(context, n);
    }

    @JRubyMethod(name = "<<")
    public IRubyObject shift_bw(ThreadContext context, IRubyObject n) {
        return prev_month(context, n);
    }

    @JRubyMethod
    public IRubyObject next_year(ThreadContext context) {
        return new RubyDate(context.runtime, dt.plusYears(+1), off, start, subMillis);
    }

    @JRubyMethod
    public IRubyObject next_year(ThreadContext context, IRubyObject n) {
        int years = n.convertToInteger().getIntValue();
        return new RubyDate(context.runtime, dt.plusYears(+years), off, start, subMillis);
    }

    @JRubyMethod
    public IRubyObject prev_year(ThreadContext context) {
        return new RubyDate(context.runtime, dt.plusYears(-1), off, start, subMillis);
    }

    @JRubyMethod
    public IRubyObject prev_year(ThreadContext context, IRubyObject n) {
        int years = n.convertToInteger().getIntValue();
        return new RubyDate(context.runtime, dt.plusYears(-years), off, start, subMillis);
    }

    @JRubyMethod // [ ajd, @of, @sg ]
    public IRubyObject marshal_dump(ThreadContext context) {
        final Ruby runtime = context.runtime;
        return context.runtime.newArrayNoCopy(new IRubyObject[] {
                ajd(context),
                RubyFixnum.newFixnum(runtime, off),
                RubyFixnum.newFixnum(runtime, start)
        });
    }

    static IRubyObject day_to_sec(ThreadContext context, IRubyObject d) {
        //if (safe_mul_p(d, DAY_IN_SECONDS)) {
        //    return LONG2FIX(FIX2LONG(d) * DAY_IN_SECONDS);
        //}
        return RubyFixnum.newFixnum(context.runtime, DAY_IN_SECONDS).op_mul(context, d);
    }

    private static final CachingCallSite zone_to_diff = new FunctionalCachingCallSite("zone_to_diff");

    static IRubyObject date_zone_to_diff(final ThreadContext context, RubyString str) {
        final RubyClass klass = getDate(context.runtime);
        return zone_to_diff.call(context, klass, klass, str);
    }

    private static IRubyObject fract_zone_to_diff(final ThreadContext context, RubyString str) {
        // of = Rational(zone_to_diff(of) || 0, 86400)
        final RubyClass klass = getDate(context.runtime);
        IRubyObject offset = zone_to_diff.call(context, klass, klass, str);
        if (offset == context.nil) offset = RubyFixnum.zero(context.runtime);
        return RubyRational.newRationalCanonicalize(context, offset, RubyFixnum.newFixnum(context.runtime, DAY_IN_SECONDS));
    }

    // def jd_to_ajd(jd, fr, of=0) jd + fr - of - Rational(1, 2) end
    private static double jd_to_ajd(long jd, int fr, int of) { return jd + fr - of - 0.5; }

    static Chronology getChronology(final int sg, final int off) {
        final DateTimeZone zone;
        if (off == 0) {
            if (sg == ITALY) return CHRONO_ITALY_UTC;
            zone = DateTimeZone.UTC;
        }
        else {
            zone = DateTimeZone.forOffsetMillis(off * 1000); // off in seconds
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

    // MRI: #define val2sg(vsg,dsg)
    static int val2sg(ThreadContext context, IRubyObject sg) {
        return getValidStart(context, sg.convertToFloat().getDoubleValue(), ITALY);
    }

    static int valid_sg(ThreadContext context, IRubyObject sg) {
        return getValidStart(context, sg.convertToFloat().getDoubleValue(), 0);
    }

    // MRI: #define valid_sg(sg)
    static int getValidStart(final ThreadContext context, final double sg, final int DEFAULT_SG) {
        // MRI: c_valid_start_p(double sg)

        if (sg == Double.NEGATIVE_INFINITY) return (int) Float.NEGATIVE_INFINITY;
        if (sg == Double.POSITIVE_INFINITY) return (int) Float.POSITIVE_INFINITY;

        if (Double.isNaN(sg) || sg < REFORM_BEGIN_JD && sg > REFORM_END_JD) {
            RubyKernel.warn(context, null, RubyString.newString(context.runtime, "invalid start is ignored"));
            return DEFAULT_SG;
        }
        ;
        return (int) sg;
    }

    private static final int REFORM_BEGIN_JD = 2298874; /* ns 1582-01-01 */
    private static final int REFORM_END_JD = 2426355; /* os 1930-12-31 */

    // MRI: #define val2off(vof,iof)
    private static int val2off(ThreadContext context, IRubyObject of) {
        final int off = offset_to_sec(context, of);
        if (off == INVALID_OFFSET) {
            RubyKernel.warn(context, null, RubyString.newString(context.runtime, "invalid offset is ignored"));
            return 0;
        }
        return off;
    }

    @Override
    public final IRubyObject inspect() {
        return inspect(getRuntime().getCurrentContext());
    }

    @JRubyMethod
    public RubyString inspect(ThreadContext context) {
        long off = this.off * 86_400;
        long s = (dt.getHourOfDay() * 60 + dt.getMinuteOfHour()) * 60 + dt.getSecondOfMinute() - off;
        long ns = (long) ((dt.getMillisOfSecond() + subMillis) * 1_000_000);
        // TODO ns = ns.to_i if Rational === ns and ns.denominator == 1
        ByteList str = new ByteList(54); // e.g. #<Date: 2018-01-15 ((2458134j,0s,0n),+0s,2299161j)>
        str.append('#').append('<');
        str.append(((RubyString) getMetaClass().to_s()).getByteList());
        str.append(':').append(' ');
        str.append(to_s(context).getByteList()); // to_s
        str.append(' ').append('(').append('(');
        str.append(ConvertBytes.longToByteList(getJulianDayNumber(), 10));
        str.append('j').append(',');
        str.append(ConvertBytes.longToByteList(s, 10));
        str.append('s').append(',');
        str.append(ConvertBytes.longToByteList(ns, 10));
        str.append('n').append(')');
        str.append(',');
        if (off >= 0) str.append('+');
        str.append(ConvertBytes.longToByteList(off, 10));
        str.append('s').append(',');
        if (start == GREGORIAN) {
            str.append('-').append('I').append('n').append('f');
        }
        else if (start == JULIAN) {
            str.append('I').append('n').append('f');
        }
        else {
            str.append(ConvertBytes.longToByteList(start, 10));
        }
        str.append('j').append(')').append('>');

        return RubyString.newStringLight(context.runtime, str);
    }

    private static final ByteList TO_S_FORMAT = new ByteList(ByteList.plain("%.4d-%02d-%02d"), false);

    @Override
    public final IRubyObject to_s() {
        return to_s(getRuntime().getCurrentContext());
    }

    @JRubyMethod
    public RubyString to_s(ThreadContext context) {
        // format('%.4d-%02d-%02d', year, mon, mday)
        IRubyObject[] args = new IRubyObject[] {
            RubyString.newStringLight(context.runtime, TO_S_FORMAT),
            year(context), mon(context), mday(context)
        };
        return (RubyString) RubyKernel.sprintf(context, this, args);
    }

    @JRubyMethod
    public RubyDateTime to_datetime(ThreadContext context) {
        return new RubyDateTime(context.runtime, dt.withTimeAtStartOfDay(), off, start);
    }

    // date/format.rb

    @JRubyMethod // def strftime(fmt='%F')
    public RubyString strftime(ThreadContext context) {
        return strftime(context, RubyString.newStringLight(context.runtime, DEFAULT_FORMAT));
    }

    @JRubyMethod // alias_method :format, :strftime
    public RubyString strftime(ThreadContext context, IRubyObject fmt) {
        IRubyObject subMillis = this.subMillis == 0 ? context.nil : RubyFloat.newFloat(context.runtime, this.subMillis);
        RubyString format = context.getRubyDateFormatter().compileAndFormat(
                fmt.convertToString(), true, this.dt, 0, subMillis
        );
        if (fmt.isTaint()) format.setTaint(true);
        return format;
    }

    private static final ByteList DEFAULT_FORMAT = new ByteList(new byte[] {'%', 'F'}, false);

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