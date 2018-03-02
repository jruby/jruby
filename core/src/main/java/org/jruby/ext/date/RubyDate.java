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

import org.jcodings.specific.USASCIIEncoding;
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
import org.jruby.util.Numeric;
import org.jruby.util.RubyDateParser;
import org.jruby.util.TimeZoneConverter;
import org.jruby.util.TypeConverter;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import static org.jruby.ext.date.DateUtils.*;
import static org.jruby.ext.date.RubyDateTime.getDay;
import static org.jruby.ext.date.RubyDateTime.getHour;
import static org.jruby.ext.date.RubyDateTime.getMinute;
import static org.jruby.ext.date.RubyDateTime.getSecond;
import static org.jruby.util.Numeric.*;

/**
 * JRuby's <code>Date</code> implementation - 'native' parts.
 * In MRI, since 2.x, all of date.rb has been moved to native (C) code.
 *
 * NOTE: There's still date.rb, where this gets bootstrapped from.
 *
 * @author enebo
 * @author kares
 */
@JRubyClass(name = "Date")
public class RubyDate extends RubyObject {

    static final Logger LOG = LoggerFactory.getLogger(RubyDate.class);

    static final GJChronology CHRONO_ITALY_UTC = GJChronology.getInstance(DateTimeZone.UTC);

    // The Julian Day Number of the Day of Calendar Reform for Italy
    // and the Catholic countries.
    static final int ITALY = 2299161; // 1582-10-15

    // The Julian Day Number of the Day of Calendar Reform for England
    // and her Colonies.
    static final int ENGLAND = 2361222; // 1752-09-14

    // A constant used to indicate that a Date should always use the Julian calendar.
    private static final double JULIAN_INFINITY = Double.POSITIVE_INFINITY; // Infinity.new
    static final long JULIAN = (long) JULIAN_INFINITY; // Infinity.new

    // A constant used to indicate that a Date should always use the Gregorian calendar.
    private static final double GREGORIAN_INFINITY = Double.NEGATIVE_INFINITY; // -Infinity.new
    static final long GREGORIAN = (long) GREGORIAN_INFINITY; // -Infinity.new

    static final int REFORM_BEGIN_YEAR = 1582;
    static final int REFORM_END_YEAR = 1930;

    protected RubyDate(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    DateTime dt;
    int off; // @of (in seconds)
    long start = ITALY; // @sg
    long subMillisNum = 0, subMillisDen = 1; // @sub_millis

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

    static RubyClass getDate(final Ruby runtime) {
        return (RubyClass) runtime.getObject().getConstantAt("Date");
    }

    static RubyClass getDateTime(final Ruby runtime) {
        return (RubyClass) runtime.getObject().getConstantAt("DateTime");
    }

    public RubyDate(Ruby runtime, RubyClass klass, DateTime dt) {
        super(runtime, klass);

        this.dt = dt; // assuming of = 0 (UTC)
    }

    public RubyDate(Ruby runtime, DateTime dt) {
        this(runtime, getDate(runtime), dt);
    }

    RubyDate(Ruby runtime, RubyClass klass, DateTime dt, int off, long start) {
        super(runtime, klass);

        this.dt = dt;
        this.off = off; this.start = start;
    }

    RubyDate(Ruby runtime, RubyClass klass, DateTime dt, int off, long start, long subMillisNum, long subMillisDen) {
        super(runtime, klass);

        this.dt = dt;
        this.off = off; this.start = start;
        this.subMillisNum = subMillisNum; this.subMillisDen = subMillisDen;
    }

    public RubyDate(Ruby runtime, long millis, Chronology chronology) {
        super(runtime, getDate(runtime));

        this.dt = new DateTime(millis, chronology);
    }

    RubyDate(ThreadContext context, RubyClass klass, IRubyObject ajd, Chronology chronology, int off) {
        this(context, klass, ajd, chronology, off, 0);
    }

    RubyDate(ThreadContext context, RubyClass klass, IRubyObject ajd, int off, long start) {
        this(context, klass, ajd, getChronology(context, start, off), off, start);
    }

    private RubyDate(ThreadContext context, RubyClass klass, IRubyObject ajd, Chronology chronology, int off, long start) {
        super(context.runtime, klass);

        this.dt = new DateTime(initMillis(context, ajd), chronology);
        this.off = off; this.start = start;
    }

    final RubyDate newInstance(final ThreadContext context, final DateTime dt, int off, long start) {
        return newInstance(context, dt, off, start, subMillisNum, subMillisDen);
    }

    // to be overriden by RubyDateTime
    RubyDate newInstance(final ThreadContext context, final DateTime dt, int off, long start, long subNum, long subDen) {
        return new RubyDate(context.runtime, getMetaClass(), dt, off, start, subNum, subDen);
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

    private void initialize(final ThreadContext context, IRubyObject arg, IRubyObject of, final long start) {
        final int off = val2off(context, of);

        this.off = off; this.start = start;

        if (arg instanceof JavaProxy) { // backwards - compatibility with JRuby's date.rb
            this.dt = (DateTime) JavaUtil.unwrapJavaValue(arg);
            return;
        }
        this.dt = new DateTime(initMillis(context, arg), getChronology(context, start, off));
    }

    static final int DAY_IN_SECONDS = 86_400; // 24 * 60 * 60
    static final int DAY_MS = 86_400_000; // 24 * 60 * 60 * 1000
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
        this.subMillisNum = ((RubyNumeric) subMillis).numerator(context).convertToInteger().getLongValue();
        this.subMillisDen = ((RubyNumeric) subMillis).denominator(context).convertToInteger().getLongValue();

        return ((RubyFixnum) millis).getLongValue();
    }

    private static RubyFixnum DAY_MS(final ThreadContext context) {
        RubyFixnum v = DAY_MS_CACHE;
        if (v == null) v = DAY_MS_CACHE = context.runtime.newFixnum(DAY_MS);
        return v;
    }

    @Override
    public IRubyObject initialize_copy(IRubyObject original) {
        final RubyDate from = (RubyDate) original;

        this.dt = from.dt; this.off = from.off; this.start = from.start;
        this.subMillisNum = from.subMillisNum; this.subMillisDen = from.subMillisDen;

        return this;
    }

    // Date.new!(dt_or_ajd=0, of=0, sg=ITALY, sub_millis=0)

    /**
     * @deprecated internal Date.new!
     */
    @JRubyMethod(name = "new!", meta = true)
    public static RubyDate new_(ThreadContext context, IRubyObject self) {
        if (self == getDateTime(context.runtime)) {
            return new RubyDateTime(context.runtime, 0, CHRONO_ITALY_UTC);
        }
        return new RubyDate(context.runtime, 0, CHRONO_ITALY_UTC);
    }

    /**
     * @deprecated internal Date.new!
     */
    @JRubyMethod(name = "new!", meta = true)
    public static RubyDate new_(ThreadContext context, IRubyObject self, IRubyObject ajd) {
        if (ajd instanceof JavaProxy) { // backwards - compatibility with JRuby's date.rb
            if (self == getDateTime(context.runtime)) {
                return new RubyDateTime(context.runtime, (RubyClass) self, (DateTime) JavaUtil.unwrapJavaValue(ajd));
            }
            return new RubyDate(context.runtime, (RubyClass) self, (DateTime) JavaUtil.unwrapJavaValue(ajd));
        }
        if (self == getDateTime(context.runtime)) {
            return new RubyDateTime(context, (RubyClass) self, ajd, CHRONO_ITALY_UTC, ITALY);
        }
        return new RubyDate(context, (RubyClass) self, ajd, CHRONO_ITALY_UTC, ITALY);
    }

    /**
     * @deprecated internal Date.new!
     */
    @JRubyMethod(name = "new!", meta = true)
    public static RubyDate new_(ThreadContext context, IRubyObject self, IRubyObject ajd, IRubyObject of) {
        if (self == getDateTime(context.runtime)) {
            return new RubyDateTime(context.runtime, (RubyClass) self).initialize(context, ajd, of);
        }
        return new RubyDate(context.runtime, (RubyClass) self).initialize(context, ajd, of);
    }

    /**
     * @deprecated internal Date.new!
     */
    @JRubyMethod(name = "new!", meta = true)
    public static RubyDate new_(ThreadContext context, IRubyObject self, IRubyObject ajd, IRubyObject of, IRubyObject sg) {
        if (self == getDateTime(context.runtime)) {
            return new RubyDateTime(context.runtime, (RubyClass) self).initialize(context, ajd, of, sg);
        }
        return new RubyDate(context.runtime, (RubyClass) self).initialize(context, ajd, of, sg);
    }

    /**
     # Create a new Date object for the Civil Date specified by
     # year +y+, month +m+, and day-of-month +d+.
     #
     # +m+ and +d+ can be negative, in which case they count
     # backwards from the end of the year and the end of the
     # month respectively.  No wraparound is performed, however,
     # and invalid values cause an ArgumentError to be raised.
     # can be negative
     #
     # +y+ defaults to -4712, +m+ to 1, and +d+ to 1; this is
     # Julian Day Number day 0.
     #
     # +sg+ specifies the Day of Calendar Reform.
     **/
    // Date.civil([year=-4712[, month=1[, mday=1[, start=Date::ITALY]]]])
    // Date.new([year=-4712[, month=1[, mday=1[, start=Date::ITALY]]]])

    @JRubyMethod(name = "civil", alias = "new", meta = true)
    public static RubyDate civil(ThreadContext context, IRubyObject self) {
        return new RubyDate(context.runtime, (RubyClass) self, defaultDateTime);
    }

    @JRubyMethod(name = "civil", alias = "new", meta = true)
    public static RubyDate civil(ThreadContext context, IRubyObject self, IRubyObject year) {
        return new RubyDate(context.runtime, (RubyClass) self, civilImpl(context, year));
    }

    static DateTime civilImpl(ThreadContext context, IRubyObject year) {
        int y = getYear(year);
        final DateTime dt;
        try {
            dt = defaultDateTime.withYear(y);
        }
        catch (IllegalArgumentException ex) {
            throw context.runtime.newArgumentError("invalid date");
        }
        return dt;
    }

    @JRubyMethod(name = "civil", alias = "new", meta = true)
    public static RubyDate civil(ThreadContext context, IRubyObject self, IRubyObject year, IRubyObject month) {
        return new RubyDate(context.runtime, (RubyClass) self, civilImpl(context, year, month));
    }

    static DateTime civilImpl(ThreadContext context, IRubyObject year, IRubyObject month) {
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
        return dt;
    }

    @JRubyMethod(name = "civil", alias = "new", meta = true)
    public static RubyDate civil(ThreadContext context, IRubyObject self, IRubyObject year, IRubyObject month, IRubyObject mday) {
        // return civil(context, self, new IRubyObject[] { year, month, mday, RubyFixnum.newFixnum(context.runtime, ITALY) });
        return new RubyDate(context.runtime, (RubyClass) self, civilImpl(context, year, month, mday, ITALY));
    }

    static DateTime civilImpl(ThreadContext context, IRubyObject year, IRubyObject month, IRubyObject mday, final long sg) {
        final int y = (sg > 0) ? getYear(year) : year.convertToInteger().getIntValue();
        final int m = getMonth(month);
        final long[] rest = new long[] { 0, 1 };
        final int d = (int) getDay(context, mday, rest);

        DateTime dt = civilDate(context, y, m ,d, getChronology(context, sg, 0));
        if (rest[0] != 0) dt = adjustWithDayFraction(context, dt, rest);
        return dt;
    }

    @JRubyMethod(name = "civil", alias = "new", meta = true, optional = 4) // 4 args case
    public static RubyDate civil(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        // IRubyObject year, IRubyObject month, IRubyObject mday, IRubyObject start
        switch (args.length) {
            // NOTE: slightly annoying but send might route its Date.send(:civil, *args) here
            case 0: return civil(context, self);
            case 1: return civil(context, self, args[0]);
            case 2: return civil(context, self, args[0], args[1]);
            // besides the above note on send ^^
            // interpreter does not have a ThreeOperandArgNoBlockCallInstr thus routes 3 args as args[] here
            case 3: return civil(context, self, args[0], args[1], args[2]);
        }

        final long sg = val2sg(context, args[3]);

        RubyDate date = new RubyDate(context.runtime, (RubyClass) self, civilImpl(context, args[0], args[1], args[2], sg));
        date.start = sg;
        return date;
    }

    static DateTime civilDate(ThreadContext context, final int y, final int m, final int d, final Chronology chronology) {
        DateTime dt;
        try {
            if (d >= 0) { // let d == 0 fail (raise 'invalid date')
                dt = new DateTime(y, m, d, 0, 0, chronology);
            }
            else {
                dt = new DateTime(y, m, 1, 0, 0, chronology);
                long ms = dt.getMillis();
                int last = chronology.dayOfMonth().getMaximumValue(ms);
                ms = chronology.dayOfMonth().set(ms, last + d + 1); // d < 0 (d == -1 -> d == 31)
                dt = dt.withMillis(ms);
            }
        }
        catch (IllegalArgumentException ex) {
            debug(context, "invalid date", ex);
            throw context.runtime.newArgumentError("invalid date");
        }
        return dt;
    }

    // NOTE: no Bignum special care since JODA does not support 'huge' years anyway
    static int getYear(IRubyObject year) {
        int y = year.convertToInteger().getIntValue(); // handles Rational(x, y)
        return (y <= 0) ? --y : y; // due julian date calc -> see adjustJodaYear
    }

    static int getMonth(IRubyObject month) {
        int m = month.convertToInteger().getIntValue(); // handles Rational(x, y)
        return (m < 0) ? m + 13 : m;
    }

    @JRubyMethod(name = "valid_civil?", alias = "valid_date?", meta = true, required = 3, optional = 1)
    public static IRubyObject valid_civil_p(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        final long sg = args.length > 3 ? val2sg(context, args[3]) : ITALY;
        final Long jd = validCivilImpl(args[0], args[1], args[2], sg);
        return jd == null ? context.fals : context.tru;
    }

    static Long validCivilImpl(IRubyObject year, IRubyObject month, IRubyObject day, final long sg) {
        final int y = (sg > 0) ? getYear(year) : year.convertToInteger().getIntValue();
        final int m = getMonth(month);
        final int d = day.convertToInteger().getIntValue();

        return DateUtils._valid_civil_p(y, m, d, sg);
    }

    // Do hour +h+, minute +min+, and second +s+ constitute a valid time?
    // If they do, returns their value as a fraction of a day.  If not, returns nil.
    @JRubyMethod(name = "_valid_time?", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject _valid_time_p(ThreadContext context, IRubyObject self,
                                            IRubyObject h, IRubyObject m, IRubyObject s) {

        long hour = normIntValue(h, 24);
        long min = normIntValue(m, 60);
        long sec = normIntValue(s, 60);

        if (valid_time_p(hour, min, sec)) {
            return timeToDayFraction(context, (int) hour, (int) min, (int) sec);
        }
        return context.nil;
    }

    private static long normIntValue(IRubyObject val, final int negOffset) {
        long v;
        if (val instanceof RubyFixnum) {
            v = ((RubyFixnum) val).getLongValue();
        }
        else {
            v = val.convertToInteger().getLongValue();
        }
        return (v < 0) ? v + negOffset : v;
    }

    // Rational(h * 3600 + min * 60 + s, 86400)
    static RubyNumeric timeToDayFraction(ThreadContext context, int hour, int min, int sec) {
        return (RubyNumeric) RubyRational.newRationalCanonicalize(context, hour * 3600 + min * 60 + sec, DAY_IN_SECONDS);
    }

    /**
     * Create a new Date object from a Julian Day Number.
     *
     * +jd+ is the Julian Day Number; if not specified, it defaults to 0.
     * +sg+ specifies the Day of Calendar Reform.
     */

    @JRubyMethod(name = "jd", meta = true)
    public static RubyDate jd(ThreadContext context, IRubyObject self) { // jd = 0, sg = ITALY
        return new RubyDate(context.runtime, (RubyClass) self, defaultDateTime);
    }

    @JRubyMethod(name = "jd", meta = true)
    public static RubyDate jd(ThreadContext context, IRubyObject self, IRubyObject jd) { // sg = ITALY
        return jdImpl(context, self, jd, ITALY);
    }

    @JRubyMethod(name = "jd", meta = true)
    public static RubyDate jd(ThreadContext context, IRubyObject self, IRubyObject jd, IRubyObject sg) {
        return jdImpl(context, self, jd, val2sg(context, sg));
    }

    private static RubyDate jdImpl(ThreadContext context, IRubyObject self, IRubyObject jd, final long sg) {
        final long[] rest = new long[] { 0, 1 };
        long jdi = getDay(context, jd, rest);
        RubyNumeric ajd = jd_to_ajd(context, jdi);

        RubyDate date = new RubyDate(context, (RubyClass) self, ajd, 0, sg);
        if (rest[0] != 0) date.dt = adjustWithDayFraction(context, date.dt, rest);
        return date;
    }

    static DateTime adjustWithDayFraction(ThreadContext context, DateTime dt, final long[] rest) {
        final RubyFixnum zero = RubyFixnum.zero(context.runtime);
        int ival = getHour(context, zero, rest);
        dt = dt.plusHours(ival);

        if (rest[0] != 0) {
            ival = getHour(context, zero, rest);
            dt = dt.plusHours(ival);

            if (rest[0] != 0) {
                ival = getMinute(context, zero, rest);
                dt = dt.plusMinutes(ival);

                if (rest[0] != 0) {
                    ival = getSecond(context, zero, rest);
                    dt = dt.plusSeconds(ival);
                }
            }
        }
        return dt;
    }

    @JRubyMethod(name = "valid_jd?", meta = true)
    public static IRubyObject valid_jd_p(ThreadContext context, IRubyObject self, IRubyObject jd) {
        return jd == context.nil ? context.fals : context.tru; // @see _valid_jd_p
    }

    @JRubyMethod(name = "valid_jd?", meta = true)
    public static IRubyObject valid_jd_p(ThreadContext context, IRubyObject self, IRubyObject jd, IRubyObject sg) {
        return jd == context.nil ? context.fals : context.tru; // @see _valid_jd_p
    }

    // Is +jd+ a valid Julian Day Number?
    //
    // If it is, returns it.  In fact, any value is treated as a valid Julian Day Number.

    @JRubyMethod(name = "_valid_jd?", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject _valid_jd_p(IRubyObject self, IRubyObject jd) {
        return jd;
    }

    @JRubyMethod(name = "_valid_jd?", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject _valid_jd_p(IRubyObject self, IRubyObject jd, IRubyObject sg) {
        return jd;
    }

    @JRubyMethod(name = "ordinal", meta = true, optional = 3)
    public static RubyDate ordinal(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        // ordinal(y=-4712, d=1, sg=ITALY)

        final int len = args.length;

        final long sg = len > 2 ? val2sg(context, args[2]) : ITALY;
        IRubyObject year = (len > 0) ? args[0] : RubyFixnum.newFixnum(context.runtime, -4712);
        IRubyObject day = (len > 1) ? args[1] : RubyFixnum.newFixnum(context.runtime, 1);

        Long jd = validOrdinalImpl(year, day, sg);
        if (jd == null) {
            throw context.runtime.newArgumentError("invalid date");
        }
        return new RubyDate(context, (RubyClass) self, jd_to_ajd(context, jd), 0, sg);
    }

    @JRubyMethod(name = "valid_ordinal?", meta = true, required = 2, optional = 1)
    public static IRubyObject valid_ordinal_p(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        final long sg = args.length > 2 ? val2sg(context, args[2]) : ITALY;
        final Long jd = validOrdinalImpl(args[0], args[1], sg);
        return jd == null ? context.fals : context.tru;
    }

    static Long validOrdinalImpl(IRubyObject year, IRubyObject day, final long sg) {
        final int y = year.convertToInteger().getIntValue();
        int d = day.convertToInteger().getIntValue();
        return DateUtils._valid_ordinal_p(y, d, sg);
    }

    @Deprecated // NOTE: should go away once no date.rb is using it
    @JRubyMethod(name = "_valid_ordinal?", meta = true, required = 2, optional = 1, visibility = Visibility.PRIVATE)
    public static IRubyObject _valid_ordinal_p(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        final long sg = args.length > 2 ? val2sg(context, args[2]) : GREGORIAN;
        final Long jd = validOrdinalImpl(args[0], args[1], sg);
        return jd == null ? context.nil : RubyFixnum.newFixnum(context.runtime, jd);
    }

    @Deprecated // NOTE: should go away once no date.rb is using it
    @JRubyMethod(name = "_valid_ordinal?", required = 2, optional = 1, visibility = Visibility.PRIVATE)
    public IRubyObject _valid_ordinal_p(ThreadContext context, IRubyObject[] args) {
        return RubyDate._valid_ordinal_p(context, null, args);
    }

    @JRubyMethod(name = "commercial", meta = true, optional = 4)
    public static RubyDate commercial(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        // commercial(y=-4712, w=1, d=1, sg=ITALY)

        final int len = args.length;

        final long sg = len > 3 ? val2sg(context, args[3]) : ITALY;
        IRubyObject year = (len > 0) ? args[0] : RubyFixnum.newFixnum(context.runtime, -4712);
        IRubyObject week = (len > 1) ? args[1] : RubyFixnum.newFixnum(context.runtime, 1);
        IRubyObject day = (len > 2) ? args[2] : RubyFixnum.newFixnum(context.runtime, 1);

        Long jd = validCommercialImpl(year, week, day, sg);
        if (jd == null) {
            throw context.runtime.newArgumentError("invalid date");
        }
        return new RubyDate(context, (RubyClass) self, jd_to_ajd(context, jd), 0, sg);
    }

    @JRubyMethod(name = "valid_commercial?", meta = true, required = 3, optional = 1)
    public static IRubyObject valid_commercial_p(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        final long sg = args.length > 3 ? val2sg(context, args[3]) : ITALY;
        final Long jd = validCommercialImpl(args[0], args[1], args[2], sg);
        return jd == null ? context.fals : context.tru;
    }

    static Long validCommercialImpl(IRubyObject year, IRubyObject week, IRubyObject day, final long sg) {
        final int y = year.convertToInteger().getIntValue();
        int w = week.convertToInteger().getIntValue();
        int d = day.convertToInteger().getIntValue();
        return DateUtils._valid_commercial_p(y, w, d, sg);
    }

    @Deprecated // NOTE: should go away once no date.rb is using it
    @JRubyMethod(name = "_valid_commercial?", meta = true, required = 3, optional = 1, visibility = Visibility.PRIVATE)
    public static IRubyObject _valid_commercial_p(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        final long sg = args.length > 3 ? val2sg(context, args[3]) : GREGORIAN;
        final Long jd = validCommercialImpl(args[0], args[1], args[2], sg);
        return jd == null ? context.nil : RubyFixnum.newFixnum(context.runtime, jd);
    }

    @Deprecated // NOTE: should go away once no date.rb is using it
    @JRubyMethod(name = "_valid_weeknum?", meta = true, required = 4, optional = 1, visibility = Visibility.PRIVATE)
    public static IRubyObject _valid_weeknum_p(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        final long sg = args.length > 4 ? val2sg(context, args[4]) : GREGORIAN;
        final int y = args[0].convertToInteger().getIntValue();
        final int w = args[1].convertToInteger().getIntValue();
        final int d = args[2].convertToInteger().getIntValue();
        final int f = args[3].convertToInteger().getIntValue();
        final Long jd = DateUtils._valid_weeknum_p(y, w, d, f, sg);
        return jd == null ? context.nil : RubyFixnum.newFixnum(context.runtime, jd);
    }

    /**
     # Create a new Date object representing today.
     #
     # +sg+ specifies the Day of Calendar Reform.
     **/

    @JRubyMethod(meta = true)
    public static RubyDate today(ThreadContext context, IRubyObject self) { // sg=ITALY
        return new RubyDate(context.runtime, (RubyClass) self, new DateTime(CHRONO_ITALY_UTC).withTimeAtStartOfDay());
    }

    @JRubyMethod(meta = true)
    public static RubyDate today(ThreadContext context, IRubyObject self, IRubyObject sg) {
        final long start = val2sg(context, sg);
        return new RubyDate(context.runtime, (RubyClass) self, new DateTime(getChronology(context, start, 0)).withTimeAtStartOfDay(), 0, start);
    }

    @Deprecated // NOTE: should go away once no date.rb is using it
    @JRubyMethod(name = "_valid_civil?", meta = true, required = 3, optional = 1, visibility = Visibility.PRIVATE)
    public static IRubyObject _valid_civil_p(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        final long sg = args.length > 3 ? val2sg(context, args[3]) : GREGORIAN;
        final Long jd = validCivilImpl(args[0], args[1], args[2], sg);
        return jd == null ? context.nil : RubyFixnum.newFixnum(context.runtime, jd);
    }

    @Deprecated // NOTE: should go away once no date.rb is using it
    @JRubyMethod(name = "_valid_civil?", required = 3, optional = 1, visibility = Visibility.PRIVATE)
    public IRubyObject _valid_civil_p(ThreadContext context, IRubyObject[] args) {
        return RubyDate._valid_civil_p(context, null, args);
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
        return this.start == that.start && this.dt.equals(that.dt) &&
               this.subMillisNum == that.subMillisNum && this.subMillisDen == that.subMillisDen;
    }

    @Override
    @JRubyMethod(name = "eql?", required = 1)
    public IRubyObject eql_p(IRubyObject other) throws RuntimeException {
        if (other instanceof RubyDate) {
            return getRuntime().newBoolean( equals((RubyDate) other) );
        }
        return getRuntime().getFalse();
    }

    @Override
    @JRubyMethod(name = "<=>", required = 1)
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) throws RaiseException {
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
            if (this.subMillisDen == 1 && that.subMillisDen == 1) {
                long diff = this.subMillisNum - that.subMillisNum;
                return diff < 0 ? -1 : ( diff == 0 ? 0 : +1 );
            }
            return cmpSubMillis(that);
        }

        return cmp;
    }

    private int cmpSubMillis(final RubyDate that) {
        ThreadContext context = getRuntime().getCurrentContext();
        RubyNumeric diff = subMillisDiff(context, that);
        return diff.isZero() ? 0 : ( Numeric.f_negative_p(context, diff) ? -1 : +1 );
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
    public RubyFixnum jd(ThreadContext context) {
        return RubyFixnum.newFixnum(context.runtime, getJulianDayNumber());
    }

    public final long getJulianDayNumber() {
        return DateTimeUtils.toJulianDayNumber(dt.getMillis() + off * 1000);
    }

    @JRubyMethod(name = "julian?")
    public RubyBoolean julian_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context.runtime, isJulian());
    }

    @JRubyMethod(name = "gregorian?")
    public RubyBoolean gregorian_p(ThreadContext context) {
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

        long num = 210_866_760_000_000l + dt.getMillis();
        // + subMillis :
        if (subMillisDen == 1) {
            num += subMillisNum;
            return RubyRational.newInstance(context, RubyFixnum.newFixnum(runtime, num), DAY_MS(context));
        }

        RubyNumeric val = (RubyNumeric) RubyFixnum.newFixnum(runtime, num).op_plus(context, subMillis(runtime));
        return RubyRational.newRationalConvert(context, val, DAY_MS(context));
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
    public IRubyObject day_fraction(ThreadContext context) { // Rational(millis, 86_400_000)
        long ms = dt.getSecondOfDay() * 1000 + dt.getMillisOfSecond();
        if (subMillisDen == 1) {
            return RubyRational.newRationalCanonicalize(context, ms + subMillisNum, DAY_MS);
        }
        final Ruby runtime = context.runtime;
        RubyNumeric sum = RubyRational.newRational(runtime, ms, 1).op_plus(context, subMillis(runtime));
        return sum.convertToRational().op_div(context, RubyFixnum.newFixnum(runtime, DAY_MS));
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
        long ms = dt.getMillisOfSecond();
        if (subMillisDen == 1) {
            return RubyRational.newRationalCanonicalize(context, ms + subMillisNum, 1000);
        }
        final Ruby runtime = context.runtime;
        RubyNumeric sum = RubyRational.newRational(runtime, ms, 1).op_plus(context, subMillis(runtime));
        return sum.convertToRational().op_div(context, RubyFixnum.newFixnum(runtime, 1000));
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

    private static final ByteList UTC_ZONE = new ByteList(new byte[] { '+','0','0',':','0','0' }, false);

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public RubyString zone(ThreadContext context) {
        // MRI: m_zone
        if (this.off == 0) {
            return RubyString.newUsAsciiStringShared(context.runtime, UTC_ZONE);
        }
        return RubyString.newUsAsciiStringNoCopy(context.runtime, of2str(this.off));
    }

    private static final int HOUR_IN_SECONDS = 60 * 60;
    private static final int MINUTE_IN_SECONDS = 60;

    private static ByteList of2str(final int of) {
        // MRI: decode_offset
        byte s = (byte) ((of < 0) ? '-' : '+');
        int a = (of < 0) ? -of : of;
        int h = a / HOUR_IN_SECONDS;
        int m = a % HOUR_IN_SECONDS / MINUTE_IN_SECONDS;

        // format "%c%02d:%02d", s, h, m
        String digs;
        ByteList str = new ByteList(6);
        str.append(s);

        digs = Integer.toString(h);
        if (digs.length() == 1) {
            str.append('0').append(digs.charAt(0));
        }
        else if (digs.length() == 2) {
            str.append(digs.charAt(0)).append(digs.charAt(1));
        }
        else {
            append(str, digs);
        }

        str.append(':');

        digs = Integer.toString(m);
        if (digs.length() == 1) {
            str.append('0').append(digs.charAt(0));
        }
        else if (digs.length() == 2) {
            str.append(digs.charAt(0)).append(digs.charAt(1));
        }
        else {
            append(str, digs);
        }

        return str;
    }

    private static void append(final ByteList out, String val) {
        for (int i=0; i<val.length(); i++) out.append(val.charAt(i));
    }

    // def zone() strftime('%:z') end

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
        DateTime dt = this.dt.withChronology(getChronology(context, start, off));
        return newInstance(context, dt, off, start);
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

    private RubyDate newStart(ThreadContext context, final long start) {
        DateTime dt = this.dt.withChronology(getChronology(context, start, off));
        return newInstance(context, dt, off, start, subMillisNum, subMillisDen);
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
            return newInstance(context, dt.plusDays(+days), off, start);
        }
        if (n instanceof RubyNumeric) {
            return op_plus_numeric(context, (RubyNumeric) n);
        }
        throw context.runtime.newTypeError("expected numeric");
    }

    IRubyObject op_plus_numeric(ThreadContext context, RubyNumeric n) {
        final Ruby runtime = context.runtime;
        // ms, sub = (n * 86_400_000).divmod(1)
        // sub = 0 if sub == 0 # avoid Rational(0, 1)
        // sub_millis = @sub_millis + sub
        // if sub_millis >= 1
        //   sub_millis -= 1
        //   ms += 1
        // end
        RubyNumeric val = (RubyNumeric) RubyFixnum.newFixnum(runtime, DAY_MS).op_mul(context, n);

        RubyArray res = (RubyArray) val.divmod(context, RubyFixnum.one(context.runtime));
        long ms = ((RubyInteger) res.eltInternal(0)).getLongValue();
        RubyNumeric sub = (RubyNumeric) res.eltInternal(1);
        //if ( sub.isZero() ) sub = RubyFixnum.zero(runtime); // avoid Rational(0, 1)
        RubyNumeric sub_millis = (RubyNumeric) subMillis(context.runtime).op_plus(context, sub);
        int subNum = sub_millis.numerator(context).convertToInteger().getIntValue();
        int subDen = sub_millis.denominator(context).convertToInteger().getIntValue();
        if (subNum / subDen >= 1) { // sub_millis >= 1
            subNum -= subDen; ms += 1; // sub_millis -= 1
        }
        return newInstance(context, dt.plus(ms), off, start, subNum, subDen);
    }

    @JRubyMethod(name = "-")
    public IRubyObject op_minus(ThreadContext context, IRubyObject n) {
        if (n instanceof RubyFixnum) {
            int days = n.convertToInteger().getIntValue();
            return newInstance(context, dt.plusDays(-days), off, start);
        }
        if (n instanceof RubyNumeric) {
            return op_plus_numeric(context, (RubyNumeric) ((RubyNumeric) n).op_uminus(context));
        }
        if (n instanceof RubyDate) {
            return op_minus_date(context, (RubyDate) n);
        }
        throw context.runtime.newTypeError("expected numeric or date");
    }

    private RubyNumeric op_minus_date(ThreadContext context, final RubyDate that) {
        long diff = this.dt.getMillis() - that.dt.getMillis();
        RubyNumeric diffMillis = (RubyNumeric) RubyRational.newRationalCanonicalize(context, diff, DAY_MS);

        RubyNumeric subDiff = subMillisDiff(context, that);
        if ( ! subDiff.isZero() ) { // diff += diff_sub;
            return (RubyNumeric) Numeric.f_add(context, diffMillis, subDiff);
        }
        return diffMillis;
    }

    private RubyNumeric subMillisDiff(final ThreadContext context, final RubyDate that) {
        final Ruby runtime = context.runtime;
        if (this.subMillisDen == 1 && that.subMillisDen == 1) {
            return RubyFixnum.newFixnum(runtime, this.subMillisNum - that.subMillisNum);
        }
        // this.subMillis - that.subMillis
        return this.subMillis(runtime).op_minus(context, that.subMillis(runtime));
    }

    final RubyRational subMillis(final Ruby runtime) {
        return RubyRational.newRational(runtime, subMillisNum, subMillisDen);
    }

    // Return a new Date one day after this one.
    @JRubyMethod(name = "next", alias = "succ")
    public IRubyObject next(ThreadContext context) {
        return next_day(context);
    }

    @JRubyMethod
    public IRubyObject next_day(ThreadContext context) {
        return newInstance(context, dt.plusDays(+1), off, start);
    }

    @JRubyMethod
    public IRubyObject next_day(ThreadContext context, IRubyObject n) {
        return newInstance(context, dt.plusDays(+simpleIntDiff(n)), off, start);
    }

    @JRubyMethod
    public IRubyObject prev_day(ThreadContext context) {
        return newInstance(context, dt.plusDays(-1), off, start);
    }

    @JRubyMethod
    public IRubyObject prev_day(ThreadContext context, IRubyObject n) {
        return newInstance(context, dt.plusDays(-simpleIntDiff(n)), off, start);
    }

    @JRubyMethod
    public IRubyObject next_month(ThreadContext context) {
        return newInstance(context, dt.plusMonths(+1), off, start);
    }

    @JRubyMethod
    public IRubyObject next_month(ThreadContext context, IRubyObject n) {
        return newInstance(context, dt.plusMonths(+simpleIntDiff(n)), off, start);
    }

    @JRubyMethod
    public IRubyObject prev_month(ThreadContext context) {
        return newInstance(context, dt.plusMonths(-1), off, start);
    }

    @JRubyMethod
    public IRubyObject prev_month(ThreadContext context, IRubyObject n) {
        return newInstance(context, dt.plusMonths(-simpleIntDiff(n)), off, start);
    }

    private static int simpleIntDiff(IRubyObject n) {
        final int days = n.convertToInteger().getIntValue();
        if (n instanceof RubyRational) {
            if (((RubyRational) n).getDenominator().getLongValue() != 1) {
                return days + 1; // MRI rulez: 1/2 -> 1 (but 0.5 -> 0)
            }
        }
        return days;
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
        return newInstance(context, dt.plusYears(+1), off, start);
    }

    @JRubyMethod
    public IRubyObject next_year(ThreadContext context, IRubyObject n) {
        return prevNextYear(context, n, false);
    }

    @JRubyMethod
    public IRubyObject prev_year(ThreadContext context) {
        return newInstance(context, dt.plusYears(-1), off, start);
    }

    @JRubyMethod
    public IRubyObject prev_year(ThreadContext context, IRubyObject n) {
        return prevNextYear(context, n, true);
    }

    private RubyDate prevNextYear(ThreadContext context, IRubyObject n, final boolean negate) {
        long months = timesIntDiff(context, n, 12);
        if (negate) months = -months; // prev_year
        final int years = RubyNumeric.checkInt(context.runtime, months / 12);
        return newInstance(context, this.dt.plusYears(years).plusMonths((int) (months % 12)), off, start);
    }

    static long timesIntDiff(final ThreadContext context, IRubyObject n, final int times) {
        IRubyObject mul = RubyFixnum.newFixnum(context.runtime, times).op_mul(context, n);
        return ((RubyNumeric) mul).round(context).convertToInteger().getLongValue();
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

    @JRubyMethod(meta = true)
    public static RubyDate _load(ThreadContext context, IRubyObject klass, IRubyObject str) {
        IRubyObject a = RubyMarshal.load(context, null, new IRubyObject[] { str }, null);
        RubyDate obj = (RubyDate) ((RubyClass) klass).allocate();
        return obj.marshal_load(context, a);
    }

    @JRubyMethod
    public RubyDate marshal_load(ThreadContext context, IRubyObject a) {
        checkFrozen();

        if (!(a instanceof RubyArray)) {
            throw context.runtime.newTypeError("expected an array");
        }

        final RubyArray ary = (RubyArray) a;

        IRubyObject ajd, of, sg;

        switch (ary.size()) {
            case 2: /* 1.6.x */
                ajd = valMinusOneHalf(context, ary.eltInternal(0));
                of = RubyFixnum.zero(context.runtime);
                sg = ary.eltInternal(1);
                if (!k_numeric_p(sg)) {
                    sg = RubyFloat.newFloat(context.runtime, sg.isTrue() ? GREGORIAN_INFINITY : JULIAN_INFINITY);
                }
                break;
            case 3: /* 1.8.x, 1.9.2 */ // ajd, of, sg = a
                ajd = ary.eltInternal(0);
                of = ary.eltInternal(1);
                sg = ary.eltInternal(2);
                break;
            case 6: // _, jd, df, sf, of, sg = a
                IRubyObject jd = ary.eltInternal(1);
                IRubyObject df = ary.eltInternal(2);
                IRubyObject sf = ary.eltInternal(3);
                of = ary.eltInternal(4);
                sg = ary.eltInternal(5);
                of = newRationalConvert(context, of, DAY_IN_SECONDS);
                ajd = marshal_load_6(context, jd, df, sf);
                break;
            default:
                throw context.runtime.newTypeError("invalid size: " + ary.size());
        }

        return initialize(context, ajd, of, sg);
    }

    private IRubyObject marshal_load_6(ThreadContext context, IRubyObject jd, IRubyObject df, IRubyObject sf) {
        IRubyObject ajd = valMinusOneHalf(context, jd);
        if ( ! ( (RubyNumeric) df ).isZero() ) {
            ajd = newRationalConvert(context, df, DAY_IN_SECONDS).op_plus(context, ajd);
        }
        if ( ! ( (RubyNumeric) sf ).isZero() ) {
            ajd = newRationalConvert(context, sf, DAY_IN_SECONDS * 1_000_000_000).op_plus(context, ajd);
        }
        return ajd;
    }

    private static IRubyObject valMinusOneHalf(ThreadContext context, IRubyObject val) {
        return RubyRational.newRational(context.runtime, -1, 2).op_plus(context, val);
    }

    static RubyRational newRationalConvert(ThreadContext context, IRubyObject num, long den) {
        return (RubyRational) RubyRational.newRationalConvert(context, num, context.runtime.newFixnum(den));
    }

    private static final CachingCallSite zone_to_diff = new FunctionalCachingCallSite("zone_to_diff");

    static IRubyObject date_zone_to_diff(final ThreadContext context, RubyString str) {
        final RubyClass klass = getDate(context.runtime);
        return zone_to_diff.call(context, klass, klass, str);
    }

    // def jd_to_ajd(jd, fr, of=0) jd + fr - of - Rational(1, 2) end
    private static double jd_to_ajd(long jd) { return jd - 0.5; }

    static RubyNumeric jd_to_ajd(ThreadContext context, long jd) {
        return RubyRational.newRational(context.runtime, (jd * 2) - 1, 2);
    }

    static RubyNumeric jd_to_ajd(ThreadContext context, long jd, RubyNumeric fr, int of_sec) {
        return jd_to_ajd(context, RubyFixnum.newFixnum(context.runtime, jd), fr, of_sec);
    }

    static RubyNumeric jd_to_ajd(ThreadContext context, RubyNumeric jd, RubyNumeric fr, int of_sec) {
        RubyNumeric tmp = jd; // jd - of :
        if (of_sec != 0) {
            tmp = (RubyNumeric) tmp.op_plus(context, RubyRational.newRationalCanonicalize(context, -of_sec, DAY_IN_SECONDS));
        }
        final RubyRational MINUS_HALF = RubyRational.newRational(context.runtime, -1, 2);
        return (RubyNumeric) ((RubyNumeric) tmp.op_plus(context, fr)).op_plus(context, MINUS_HALF);
    }

    @JRubyMethod(meta = true, required = 2, optional = 1, visibility = Visibility.PRIVATE)
    public static RubyNumeric jd_to_ajd(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        RubyNumeric jd = (RubyNumeric) args[0];
        RubyNumeric fr = (RubyNumeric) args[1];
        int of_sec = 0;
        if (args.length > 2 && ! ((RubyNumeric) args[2]).isZero()) {
            RubyNumeric of = (RubyNumeric) f_mul(context, args[2], RubyFixnum.newFixnum(context.runtime, DAY_IN_SECONDS));
            of_sec = of.getIntValue();
        }
        return jd_to_ajd(context, jd, fr, of_sec);
    }

    static Chronology getChronology(ThreadContext context, final long sg, final int off) {
        final DateTimeZone zone;
        if (off == 0) {
            if (sg == ITALY) return CHRONO_ITALY_UTC;
            zone = DateTimeZone.UTC;
        }
        else {
            try {
                zone = DateTimeZone.forOffsetMillis(off * 1000); // off in seconds
            } // NOTE: JODA only allows 'valid': -23:59:59.999 to +23:59:59.999
            catch (IllegalArgumentException ex) { // while MRI handles 25/24 fine
                debug(context, "invalid offset", ex);
                throw context.runtime.newArgumentError("invalid offset: " + off);
            }
        }
        return getChronology(context, sg, zone);
    }

    static Chronology getChronology(ThreadContext context, final long sg, final DateTimeZone zone) {
        if (sg == ITALY) return GJChronology.getInstance(zone);
        if (sg == JULIAN) return JulianChronology.getInstance(zone);
        if (sg == GREGORIAN) return GregorianChronology.getInstance(zone);

        Instant cutover = new Instant(DateTimeUtils.fromJulianDay(jd_to_ajd(sg)));
        try {
            return GJChronology.getInstance(zone, cutover);
        } // java.lang.IllegalArgumentException: Cutover too early. Must be on or after 0001-01-01.
        catch (IllegalArgumentException ex) {
            debug(context, "invalid date", ex);
            throw context.runtime.newArgumentError("invalid date");
        }
    }

    // MRI: #define val2sg(vsg,dsg)
    static long val2sg(ThreadContext context, IRubyObject sg) {
        return getValidStart(context, sg.convertToFloat().getDoubleValue(), ITALY);
    }

    static long valid_sg(ThreadContext context, IRubyObject sg) {
        return getValidStart(context, sg.convertToFloat().getDoubleValue(), 0);
    }

    // MRI: #define valid_sg(sg)
    static long getValidStart(final ThreadContext context, final double sg, final int DEFAULT_SG) {
        // MRI: c_valid_start_p(double sg)

        if (sg == Double.NEGATIVE_INFINITY || sg == Double.POSITIVE_INFINITY) return (long) sg;

        if (Double.isNaN(sg) || sg < REFORM_BEGIN_JD && sg > REFORM_END_JD) {
            RubyKernel.warn(context, null, RubyString.newString(context.runtime, "invalid start is ignored"));
            return DEFAULT_SG;
        }
        ;
        return (long) sg;
    }

    private static final int REFORM_BEGIN_JD = 2298874; /* ns 1582-01-01 */
    private static final int REFORM_END_JD = 2426355; /* os 1930-12-31 */

    // MRI: #define val2off(vof,iof)
    static int val2off(ThreadContext context, IRubyObject of) {
        final int off = offset_to_sec(context, of);
        if (off == INVALID_OFFSET) {
            RubyKernel.warn(context, null, RubyString.newString(context.runtime, "invalid offset is ignored"));
            return 0;
        }
        return off;
    }

    static void debug(ThreadContext context, final String msg, Exception ex) {
        if (LOG.isDebugEnabled()) LOG.debug(msg, ex);
        else if (context.runtime.isDebug()) LOG.info(msg, ex);
    }

    @Override
    public final IRubyObject inspect() {
        return inspect(getRuntime().getCurrentContext());
    }

    @JRubyMethod
    public RubyString inspect(ThreadContext context) {
        int off = this.off;
        int s = (dt.getHourOfDay() * 60 + dt.getMinuteOfHour()) * 60 + dt.getSecondOfMinute() - off;
        long ns = (dt.getMillisOfSecond() * 1_000_000) + (subMillisNum * 1_000_000) / subMillisDen;
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

        return RubyString.newUsAsciiStringNoCopy(context.runtime, str);
    }

    private static final ByteList TO_S_FORMAT = new ByteList(ByteList.plain("%.4d-%02d-%02d"), false);
    static { TO_S_FORMAT.setEncoding(USASCIIEncoding.INSTANCE); }

    @Override
    public final IRubyObject to_s() {
        return to_s(getRuntime().getCurrentContext());
    }

    @JRubyMethod
    public RubyString to_s(ThreadContext context) { // format('%.4d-%02d-%02d', year, mon, mday)
        return format(context, TO_S_FORMAT, year(context), mon(context), mday(context));
    }

    static RubyString format(ThreadContext context, ByteList fmt, IRubyObject... args) {
        final RubyString str = RubyString.newStringLight(context.runtime, fmt);
        return str.op_format(context, RubyArray.newArrayNoCopy(context.runtime, args));
    }

    @JRubyMethod
    public RubyDate to_date() { return this; }

    @JRubyMethod
    public RubyDateTime to_datetime(ThreadContext context) {
        return new RubyDateTime(context.runtime, getDateTime(context.runtime), dt.withTimeAtStartOfDay(), off, start);
    }

    @JRubyMethod // Time.local(year, mon, mday)
    public RubyTime to_time(ThreadContext context) {
        final Ruby runtime = context.runtime;
        DateTime dt = this.dt;

        dt = new DateTime(dt.getYear(), dt.getMonthOfYear(), dt.getDayOfMonth(),
                0, 0, 0,
                RubyTime.getLocalTimeZone(runtime)
        );
        return new RubyTime(runtime, runtime.getTime(), dt);
    }

    // date/format.rb

    @JRubyMethod // def strftime(fmt='%F')
    public RubyString strftime(ThreadContext context) {
        return strftime(context, RubyString.newStringLight(context.runtime, DEFAULT_FORMAT_BYTES));
    }

    @JRubyMethod // alias_method :format, :strftime
    public RubyString strftime(ThreadContext context, IRubyObject fmt) {
        RubyRational subMillis = this.subMillisNum == 0 ? null :
                RubyRational.newRational(context.runtime, this.subMillisNum, this.subMillisDen);
        RubyString format = context.getRubyDateFormatter().compileAndFormat(
                fmt.convertToString(), true, this.dt, 0, subMillis
        );
        if (fmt.isTaint()) format.setTaint(true);
        return format;
    }

    private static final String DEFAULT_FORMAT = "%F";
    private static final ByteList DEFAULT_FORMAT_BYTES = ByteList.create(DEFAULT_FORMAT);
    static { DEFAULT_FORMAT_BYTES.setEncoding(USASCIIEncoding.INSTANCE); }

    @JRubyMethod(meta = true)
    public static IRubyObject _strptime(ThreadContext context, IRubyObject self, IRubyObject string) {
        return parse(context, string, DEFAULT_FORMAT);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject _strptime(ThreadContext context, IRubyObject self, IRubyObject string, IRubyObject format) {
        format = TypeConverter.checkStringType(context.runtime, format);
        return parse(context, string, ((RubyString) format).decodeString());
    }

    static IRubyObject parse(ThreadContext context, IRubyObject string, String format) {
        string = TypeConverter.checkStringType(context.runtime, string);

        return new RubyDateParser().parse(context, format, (RubyString) string);
    }

    // @Deprecated
    public static IRubyObject _strptime(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        switch (args.length) {
            case 1:
                return _strptime(context, self, args[0]);
            case 2:
                return _strptime(context, self, args[0], args[1]);
            default:
                throw context.runtime.newArgumentError(args.length, 1);
        }
    }

    @JRubyMethod(name = "zone_to_diff", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject zone_to_diff(ThreadContext context, IRubyObject self, IRubyObject zone) {
        final int offset = TimeZoneConverter.dateZoneToDiff(zone.asJavaString());
        if (offset == TimeZoneConverter.INVALID_ZONE) return context.nil;
        return RubyFixnum.newFixnum(context.runtime, offset);
    }

    @JRubyMethod(name = "i", meta = true, visibility = Visibility.PRIVATE)
    public static RubyInteger _i(ThreadContext context, IRubyObject self, IRubyObject val) {
        return (RubyInteger) TypeConverter.convertToInteger(context, val, 10); // Integer(str, 10)
    }

    @JRubyMethod(name = "comp_year69", meta = true, visibility = Visibility.PRIVATE)
    public static RubyInteger _comp_year69(ThreadContext context, IRubyObject self, IRubyObject year) {
        RubyInteger y = _i(context, self, year);
        if (((RubyString) year).strLength() < 4) {
            final long yi = y.getLongValue();
            return RubyFixnum.newFixnum(context.runtime, yi >= 69 ? yi + 1900 : yi + 2000);
        }
        return y;
    }

}