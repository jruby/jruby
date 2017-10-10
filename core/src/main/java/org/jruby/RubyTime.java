/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Joey Gibson <joey@joeygibson.com>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2006 Ola Bini <ola.bini@ki.se>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2009 Joseph LaFata <joe@quibb.org>
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

import jnr.posix.POSIX;
import jnr.posix.Timeval;
import org.jcodings.specific.USASCIIEncoding;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.IllegalFieldValueException;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.tz.FixedDateTimeZone;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.JavaSites.TimeSites;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ArraySupport;
import org.jruby.util.ByteList;
import org.jruby.util.RubyDateFormatter;
import org.jruby.util.TypeConverter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jruby.RubyComparable.invcmp;
import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.runtime.invokedynamic.MethodNames.OP_CMP;

/** The Time class.
 *
 * @author chadfowler, jpetersen
 */
@JRubyClass(name="Time", include="Comparable")
public class RubyTime extends RubyObject {
    public static final String UTC = "UTC";
    public static final BigDecimal ONE_MILLION_BD = BigDecimal.valueOf(1000000);
    public static final BigDecimal ONE_BILLION_BD = BigDecimal.valueOf(1000000000);

    private DateTime dt;
    private long nsec;

    private final static DateTimeFormatter ONE_DAY_CTIME_FORMATTER = DateTimeFormat.forPattern("EEE MMM  d HH:mm:ss yyyy").withLocale(Locale.ENGLISH);
    private final static DateTimeFormatter TWO_DAY_CTIME_FORMATTER = DateTimeFormat.forPattern("EEE MMM dd HH:mm:ss yyyy").withLocale(Locale.ENGLISH);

    private final static DateTimeFormatter TO_S_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss Z").withLocale(Locale.ENGLISH);
    private final static DateTimeFormatter TO_S_UTC_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withLocale(Locale.ENGLISH);
    // There are two different popular TZ formats: legacy (AST+3:00:00, GMT-3), and
    // newer one (US/Pacific, America/Los_Angeles). This pattern is to detect
    // the legacy TZ format in order to convert it to the newer format
    // understood by Java API.
    private static final Pattern TZ_PATTERN
            = Pattern.compile("([^-\\+\\d]+)?([\\+-]?)(\\d+)(?::(\\d+))?(?::(\\d+))?");

    private static final Pattern TIME_OFFSET_PATTERN
            = Pattern.compile("([\\+-])(\\d\\d):(\\d\\d)(?::(\\d\\d))?");

    private static final ByteList TZ_STRING = ByteList.create("TZ");

    private boolean isTzRelative = false; // true if and only if #new is called with a numeric offset (e.g., "+03:00")

    /* JRUBY-3560
     * joda-time disallows use of three-letter time zone IDs.
     * Since MRI accepts these values, we need to translate them.
     */
    private static final Map<String, String> LONG_TZNAME = Helpers.map(
        "MET", "CET", // JRUBY-2759
        "ROC", "Asia/Taipei", // Republic of China
        "WET", "Europe/Lisbon" // Western European Time
    );

    /* Some TZ values need to be overriden for Time#zone
     */
    private static final Map<String, String> SHORT_STD_TZNAME = Helpers.map(
        "Etc/UCT", "UCT",
        "MET", "MET", // needs to be overriden
        "UCT", "UCT"
    );

    private static final Map<String, String> SHORT_DL_TZNAME = Helpers.map(
        "Etc/UCT", "UCT",
        "MET", "MEST", // needs to be overriden
        "UCT", "UCT"
    );

    private void setIsTzRelative(boolean tzRelative) {
        isTzRelative = tzRelative;
    }

    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.TIME;
    }

    private static IRubyObject getEnvTimeZone(Ruby runtime) {
        RubyString tzVar = (RubyString)runtime.getTime().getInternalVariable("tz_string");
        if (tzVar == null) {
            tzVar = runtime.newString(TZ_STRING);
            tzVar.setFrozen(true);
            runtime.getTime().setInternalVariable("tz_string", tzVar);
        }
        return runtime.getENV().op_aref(runtime.getCurrentContext(), tzVar);
    }

    public static DateTimeZone getLocalTimeZone(Ruby runtime) {
        IRubyObject tz = getEnvTimeZone(runtime);

        if (tz == null || ! (tz instanceof RubyString)) {
            return DateTimeZone.getDefault();
        } else {
            return getTimeZoneFromTZString(runtime, tz.toString());
        }
    }

    public static DateTimeZone getTimeZoneFromTZString(Ruby runtime, String zone) {
        DateTimeZone cachedZone = runtime.getTimezoneCache().get(zone);
        if (cachedZone != null) {
            return cachedZone;
        } else {
            DateTimeZone dtz = parseTZString(runtime, zone);
            runtime.getTimezoneCache().put(zone, dtz);
            return dtz;
        }
    }

    private static DateTimeZone parseTZString(Ruby runtime, String zone) {
        String upZone = zone.toUpperCase();

        Matcher tzMatcher = TZ_PATTERN.matcher(zone);
        if (tzMatcher.matches()) {
            String zoneName = tzMatcher.group(1);
            String sign = tzMatcher.group(2);
            String hours = tzMatcher.group(3);
            String minutes = tzMatcher.group(4);
            String seconds= tzMatcher.group(5);

            if (zoneName == null) {
                zoneName = "";
            }

            // Sign is reversed in legacy TZ notation
            return getTimeZoneFromHHMM(runtime, zoneName, sign.equals("-"), hours, minutes, seconds);
        } else {
            if (LONG_TZNAME.containsKey(upZone)) {
                zone = LONG_TZNAME.get(upZone);
            } else if (upZone.equals("UTC") || upZone.equals("GMT")) {
                // MRI behavior: With TZ equal to "GMT" or "UTC", Time.now
                // is *NOT* considered as a proper GMT/UTC time:
                //   ENV['TZ']="GMT"; Time.now.gmt? #=> false
                //   ENV['TZ']="UTC"; Time.now.utc? #=> false
                // Hence, we need to adjust for that.
                zone = "Etc/" + upZone;
            }

            try {
                return DateTimeZone.forID(zone);
            } catch (IllegalArgumentException e) {
                runtime.getWarnings().warning("Unrecognized time zone: "+zone);
                return DateTimeZone.UTC;
            }
        }
    }

    public static DateTimeZone getTimeZoneFromString(Ruby runtime, String zone) {
        DateTimeZone cachedZone = runtime.getTimezoneCache().get(zone);
        if (cachedZone != null) {
            return cachedZone;
        } else {
            DateTimeZone dtz = parseZoneString(runtime, zone);
            runtime.getTimezoneCache().put(zone, dtz);
            return dtz;
        }
    }

    private static DateTimeZone parseZoneString(Ruby runtime, String zone) {
        // TODO: handle possible differences with TZ format
        return parseTZString(runtime, zone);
    }

    public static DateTimeZone getTimeZoneFromUtcOffset(Ruby runtime, IRubyObject utcOffset) {
        String strOffset = utcOffset.toString();

        DateTimeZone cachedZone = runtime.getTimezoneCache().get(strOffset);
        if (cachedZone != null) return cachedZone;

        DateTimeZone dtz;
        if(utcOffset instanceof RubyString) {
            Matcher offsetMatcher = TIME_OFFSET_PATTERN.matcher(strOffset);
            if (!offsetMatcher.matches()) {
                throw runtime.newArgumentError("\"+HH:MM\" or \"-HH:MM\" expected for utc_offset");
            }
            String sign = offsetMatcher.group(1);
            String hours = offsetMatcher.group(2);
            String minutes = offsetMatcher.group(3);
            String seconds = offsetMatcher.group(4);
            dtz = getTimeZoneFromHHMM(runtime, "", !sign.equals("-"), hours, minutes, seconds);
        } else {
            IRubyObject numericOffset = numExact(runtime, utcOffset);
            int newOffset = (int)Math.round(numericOffset.convertToFloat().getDoubleValue() * 1000);
            dtz = getTimeZoneWithOffset(runtime, "", newOffset);
        }

        runtime.getTimezoneCache().put(strOffset, dtz);
        return dtz;
    }

    // mri: time.c num_exact
    private static IRubyObject numExact(Ruby runtime, IRubyObject v) {
        IRubyObject tmp;
        boolean typeError = false;

        switch (v.getMetaClass().getClassIndex()) {
            case INTEGER:
                return v;

            case RATIONAL:
                break;

            case STRING:
            case NIL:
                typeError = true;
                break;

            default:
                ThreadContext context = runtime.getCurrentContext();
                if ((tmp = v.getMetaClass().finvokeChecked(context, v, "to_r")) != null) {
                    /* test to_int method availability to reject non-Numeric
                     * objects such as String, Time, etc which have to_r method. */
                    if (!v.respondsTo("to_int")) {
                        typeError = true;
                        break;
                    }
                    v = tmp;
                    break;
                }
                if (!(tmp = TypeConverter.checkIntegerType(context, v)).isNil()) {
                    v = tmp;
                    break;
                }
                typeError = true;
                break;
        }

        switch (v.getMetaClass().getClassIndex()) {
            case INTEGER:
                return v;

            case RATIONAL:
                if (((RubyRational) v).getDenominator() == RubyFixnum.one(runtime)) {
                    v = ((RubyRational) v).getNumerator();
                }
                break;

            default:
                typeError = true;
                break;
        }

        if (typeError) {
            if (v.isNil()) throw runtime.newTypeError("can't convert nil into an exact number");
            throw runtime.newTypeError("can't convert " + v.getMetaClass() + " into an exact number");
        }

        return v;
    }

    private static void exactTypeError(Ruby runtime, IRubyObject received) {
        throw runtime.newTypeError(
                String.format("Can't convert %s into an exact number", received.getMetaClass().getRealClass()));
    }

    private static DateTimeZone getTimeZoneFromHHMM(Ruby runtime, String name, boolean positive, String hours, String minutes, String seconds) {
        int h = Integer.parseInt(hours);
        int m = 0;
        int s = 0;
        if (minutes != null) {
            m = Integer.parseInt(minutes);
        }

        if (seconds != null) {
            s = Integer.parseInt(seconds);
        }

        if (h > 23 || m > 59) {
            throw runtime.newArgumentError("utc_offset out of range");
        }

        int offset = (positive ? +1 : -1) * ((h * 3600) + m * 60 + s)  * 1000;
        return timeZoneWithOffset(name, offset);
    }

    public static DateTimeZone getTimeZone(Ruby runtime, long seconds) {
        if (seconds >= 60*60*24 || seconds <= -60*60*24) {
            throw runtime.newArgumentError("utc_offset out of range");
        }
        return getTimeZoneWithOffset(runtime, "", (int) (seconds * 1000));
    }

    public static DateTimeZone getTimeZoneWithOffset(Ruby runtime, String zoneName, int offset) {
        // validate_zone_name
        zoneName = zoneName.trim();

        String zone = zoneName + offset;

        DateTimeZone cachedZone = runtime.getTimezoneCache().get(zone);
        if (cachedZone != null) {
            return cachedZone;
        } else {
            DateTimeZone dtz = timeZoneWithOffset(zoneName, offset);
            runtime.getTimezoneCache().put(zone, dtz);
            return dtz;
        }
    }

    private static DateTimeZone timeZoneWithOffset(String zoneName, int offset) {
        if (zoneName.isEmpty()) {
            return DateTimeZone.forOffsetMillis(offset);
        } else {
            return new FixedDateTimeZone(zoneName, null, offset, offset);
        }
    }

    public RubyTime(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    public RubyTime(Ruby runtime, RubyClass rubyClass, DateTime dt) {
        super(runtime, rubyClass);
        this.dt = dt;
    }

    private static ObjectAllocator TIME_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            DateTimeZone dtz = getLocalTimeZone(runtime);
            long msecs;
            long nsecs;
            POSIX posix = runtime.getPosix();
            if (posix.isNative()) {
                // FIXME: we should have a pure Java fallback in jnr-posix and Windows is missing gettimeofday impl
                try {
                    Timeval tv = posix.allocateTimeval();
                    posix.gettimeofday(tv);

                    long secs = tv.sec();
                    long usecs = tv.usec();

                    msecs = secs * 1000 + (usecs / 1000);
                    nsecs = usecs % 1000 * 1000;
                } catch (RaiseException notImplementedError) {
                    msecs = System.currentTimeMillis();
                    nsecs = 0;
                }
            } else {
                msecs = System.currentTimeMillis();
                nsecs = 0;
            }
            DateTime dt = new DateTime(msecs, dtz);
            RubyTime rt =  new RubyTime(runtime, klass, dt);
            rt.setNSec(nsecs);

            return rt;
        }
    };

    public static RubyClass createTimeClass(Ruby runtime) {
        RubyClass timeClass = runtime.defineClass("Time", runtime.getObject(), TIME_ALLOCATOR);

        timeClass.setClassIndex(ClassIndex.TIME);
        timeClass.setReifiedClass(RubyTime.class);

        runtime.setTime(timeClass);

        timeClass.includeModule(runtime.getComparable());

        timeClass.defineAnnotatedMethods(RubyTime.class);

        return timeClass;
    }

    public void setNSec(long nsec) {
        this.nsec = nsec;
    }

    public long getNSec() {
        return nsec;
    }

    public void setUSec(long usec) {
        this.nsec = 1000 * usec;
    }

    public long getUSec() {
        return nsec / 1000;
    }

    public void updateCal(DateTime dt) {
        this.dt = dt;
    }

    protected long getTimeInMillis() {
        return dt.getMillis();
    }

    public static RubyTime newTime(Ruby runtime, long milliseconds) {
        return newTime(runtime, new DateTime(milliseconds));
    }

    public static RubyTime newTimeFromNanoseconds(Ruby runtime, long nanoseconds) {
        long milliseconds = nanoseconds / 1000000;
        long extraNanoseconds = nanoseconds % 1000000;
        return RubyTime.newTime(runtime, new DateTime(milliseconds), extraNanoseconds);
    }

    public static RubyTime newTime(Ruby runtime, DateTime dt) {
        return new RubyTime(runtime, runtime.getTime(), dt);
    }

    public static RubyTime newTime(Ruby runtime, DateTime dt, long nsec) {
        RubyTime t = new RubyTime(runtime, runtime.getTime(), dt);
        t.setNSec(nsec);
        return t;
    }

    @Override
    public Class<?> getJavaClass() {
        return Date.class;
    }

    @JRubyMethod(required = 1, visibility = Visibility.PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject original) {
        if (!(original instanceof RubyTime)) {
            throw getRuntime().newTypeError("Expecting an instance of class Time");
        }

        RubyTime originalTime = (RubyTime) original;

        // We can just use dt, since it is immutable
        dt = originalTime.dt;
        nsec = originalTime.nsec;

        return this;
    }

    @JRubyMethod
    public RubyTime succ() {
        return newTime(getRuntime(), dt.plusSeconds(1));
    }

    @JRubyMethod(name = {"gmtime", "utc"})
    public RubyTime gmtime() {
        return adjustTimeZone(getRuntime(), DateTimeZone.UTC);
    }

    public final RubyTime localtime() {
        return localtime(getRuntime().getCurrentContext());
    }

    @JRubyMethod(name = "localtime")
    public RubyTime localtime(ThreadContext context) {
        return adjustTimeZone(context.runtime, getLocalTimeZone(context.runtime));
    }

    @JRubyMethod(name = "localtime")
    public RubyTime localtime(ThreadContext context, IRubyObject arg) {
        final DateTimeZone zone = getTimeZoneFromUtcOffset(context.runtime, arg);
        return adjustTimeZone(context.runtime, zone);
    }

    private RubyTime adjustTimeZone(Ruby runtime, final DateTimeZone zone) {
        if (zone.equals(dt.getZone())) return this;
        if (isFrozen()) {
            throw runtime.newFrozenError("Time", true);
        }
        dt = dt.withZone(zone);
        return this;
    }

    @Deprecated
    public final RubyTime localtime19(ThreadContext context, IRubyObject[] args) {
        switch(args.length) {
            case 0: return localtime(context);
            case 1: return localtime(context, args[0]);
        }
        throw new AssertionError(java.util.Arrays.toString(args));
    }

    @JRubyMethod(name = {"gmt?", "utc?", "gmtime?"})
    public RubyBoolean gmt() {
        return getRuntime().newBoolean(dt.getZone().getID().equals("UTC"));
    }

    @JRubyMethod(name = {"getgm", "getutc"})
    public RubyTime getgm() {
        return newTime(getRuntime(), dt.withZone(DateTimeZone.UTC), nsec);
    }

    public final RubyTime getlocal() {
        return getlocal(getRuntime().getCurrentContext());
    }

    @JRubyMethod(name = "getlocal")
    public RubyTime getlocal(ThreadContext context) {
        return newTime(context.runtime, dt.withZone(getLocalTimeZone(context.runtime)), nsec);
    }

    @JRubyMethod(name = "getlocal")
    public RubyTime getlocal(ThreadContext context, IRubyObject arg) {
        if (arg.isNil()) {
            return newTime(context.runtime, dt.withZone(getLocalTimeZone(context.runtime)), nsec);
        }
        DateTimeZone dtz = getTimeZoneFromUtcOffset(context.runtime, arg);
        return newTime(context.runtime, dt.withZone(dtz), nsec);
    }

    @Deprecated
    public RubyTime getlocal19(ThreadContext context, IRubyObject[] args) {
        switch(args.length) {
            case 0: return getlocal(context);
            case 1: return getlocal(context, args[0]);
        }
        throw new AssertionError(java.util.Arrays.toString(args));
    }

    /**
     * @see #strftime(ThreadContext, IRubyObject)
     */
    public RubyString strftime(IRubyObject format) {
        return strftime(getRuntime().getCurrentContext(), format);
    }

    @JRubyMethod(required = 1)
    public RubyString strftime(ThreadContext context, IRubyObject format) {
        final RubyDateFormatter rdf = context.getRubyDateFormatter();
        return rdf.compileAndFormat(format.convertToString(), false, dt, nsec, null);
    }

    @JRubyMethod(name = "==", required = 1)
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if (other.isNil()) {
            return context.runtime.getFalse();
        }
        if (other instanceof RubyTime) {
            return context.runtime.newBoolean(cmp((RubyTime) other) == 0);
        }

        return RubyComparable.op_equal(context, this, other);
    }

    @JRubyMethod(name = ">=", required = 1)
    public IRubyObject op_ge(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyTime) {
            return context.runtime.newBoolean(cmp((RubyTime) other) >= 0);
        }

        return RubyComparable.op_ge(context, this, other);
    }

    @JRubyMethod(name = ">", required = 1)
    public IRubyObject op_gt(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyTime) {
            return context.runtime.newBoolean(cmp((RubyTime) other) > 0);
        }

        return RubyComparable.op_gt(context, this, other);
    }

    @JRubyMethod(name = "<=", required = 1)
    public IRubyObject op_le(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyTime) {
            return context.runtime.newBoolean(cmp((RubyTime) other) <= 0);
        }

        return RubyComparable.op_le(context, this, other);
    }

    @JRubyMethod(name = "<", required = 1)
    public IRubyObject op_lt(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyTime) {
            return context.runtime.newBoolean(cmp((RubyTime) other) < 0);
        }

        return RubyComparable.op_lt(context, this, other);
    }

    private int cmp(RubyTime other) {
        long millis = getTimeInMillis();
		long millis_other = other.getTimeInMillis();

        long nsec = this.nsec;
        long nsec_other = other.nsec;

		if (millis > millis_other || (millis == millis_other && nsec > nsec_other)) {
		    return 1;
		}
		if (millis < millis_other || (millis == millis_other && nsec < nsec_other)) {
		    return -1;
		}

        return 0;
    }

    public IRubyObject op_plus(IRubyObject other) {
        return op_plus(getRuntime().getCurrentContext(), other);
    }

    @JRubyMethod(name = "+", required = 1)
    public IRubyObject op_plus(ThreadContext context, IRubyObject other) {
        checkOpCoercion(context, other);
        if (other instanceof RubyTime) {
            throw context.runtime.newTypeError("time + time ?");
        }
        other = other.callMethod(context, "to_r");

        double adjustMillis = RubyNumeric.num2dbl(other) * 1000;
        return opPlusMillis(adjustMillis);
    }

    @Deprecated
    public IRubyObject op_plus19(ThreadContext context, IRubyObject other) {
        return op_plus(context, other);
    }

    private IRubyObject opPlusMillis(double adjustMillis) {
        long currentMillis = getTimeInMillis();

        long newMillisPart = currentMillis + (long)adjustMillis;
        long adjustNanos = (long)((adjustMillis - Math.floor(adjustMillis)) * 1000000);
        long newNanosPart =  nsec + adjustNanos;

        if (newNanosPart >= 1000000) {
            newNanosPart -= 1000000;
            newMillisPart++;
        }

        RubyTime newTime = new RubyTime(getRuntime(), getMetaClass());
        newTime.dt = new DateTime(newMillisPart).withZone(dt.getZone());
        newTime.setNSec(newNanosPart);

        return newTime;
    }

    private void checkOpCoercion(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyString) {
            throw context.runtime.newTypeError("no implicit conversion to rational from string");
        } else if (other.isNil()) {
            throw context.runtime.newTypeError("no implicit conversion to rational from nil");
        } else if (!other.respondsTo("to_r")){
            throw context.runtime.newTypeError("can't convert " + other.getMetaClass().getBaseName() + " into Rational");
        }
    }

    private RubyFloat opMinus(Ruby runtime, RubyTime other) {
        long timeInMillis = (getTimeInMillis() - other.getTimeInMillis());
        double timeInSeconds = timeInMillis/1000.0 + (getNSec() - other.getNSec())/1000000000.0;

        return RubyFloat.newFloat(runtime, timeInSeconds); // float number of seconds
    }

    public IRubyObject op_minus(IRubyObject other) {
        return op_minus(getRuntime().getCurrentContext(), other);
    }

    @JRubyMethod(name = "-", required = 1)
    public IRubyObject op_minus(ThreadContext context, IRubyObject other) {
        checkOpCoercion(context, other);
        if (other instanceof RubyTime) return opMinus(context.runtime, (RubyTime) other);
        return opMinusCommon(other.callMethod(context, "to_r"));
    }

    @Deprecated
    public IRubyObject op_minus19(ThreadContext context, IRubyObject other) {
        return op_minus(context, other);
    }

    private IRubyObject opMinusCommon(IRubyObject other) {
        long adjustmentInNanos = (long)(RubyNumeric.num2dbl(other)*1000000000);
        long adjustmentInMillis = adjustmentInNanos/1000000;
        long adjustmentInNanosLeft = adjustmentInNanos%1000000;

        long time = getTimeInMillis() - adjustmentInMillis;

        long nano;
        if (nsec < adjustmentInNanosLeft) {
            time--;
            nano = 1000000 - (adjustmentInNanosLeft - nsec);
        } else {
            nano = nsec - adjustmentInNanosLeft;
        }

        RubyTime newTime = new RubyTime(getRuntime(), getMetaClass());
        newTime.dt = new DateTime(time).withZone(dt.getZone());
        newTime.setNSec(nano);

        return newTime;
    }

    @JRubyMethod(name = "===", required = 1)
    @Override
    public IRubyObject op_eqq(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyTime) {
            return context.runtime.newBoolean(RubyNumeric.fix2int(invokedynamic(context, this, OP_CMP, other)) == 0);
        }

        return context.runtime.getFalse();
    }

    @JRubyMethod(name = "<=>", required = 1)
    @Override
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyTime) {
            return context.runtime.newFixnum(cmp((RubyTime) other));
        }

        return invcmp(context, sites(context).recursive_cmp, this, other);
    }

    @JRubyMethod(name = "eql?", required = 1)
    @Override
    public IRubyObject eql_p(IRubyObject other) {
        if (other instanceof RubyTime) {
            RubyTime otherTime = (RubyTime)other;
            return (nsec == otherTime.nsec && getTimeInMillis() == otherTime.getTimeInMillis()) ? getRuntime().getTrue() : getRuntime().getFalse();
        }
        return getRuntime().getFalse();
    }

    @JRubyMethod(name = {"asctime", "ctime"})
    public RubyString asctime() {
        DateTimeFormatter simpleDateFormat;

        if (dt.getDayOfMonth() < 10) {
            simpleDateFormat = ONE_DAY_CTIME_FORMATTER;
        } else {
            simpleDateFormat = TWO_DAY_CTIME_FORMATTER;
        }
        String result = simpleDateFormat.print(dt);
        return RubyString.newString(getRuntime(), result, USASCIIEncoding.INSTANCE);
    }

    @Override
    @JRubyMethod(name = {"to_s", "inspect"})
    public IRubyObject to_s() {
        return inspectCommon(TO_S_FORMATTER, TO_S_UTC_FORMATTER);
    }

    public final IRubyObject to_s19() {
        return to_s();
    }

    private IRubyObject inspectCommon(DateTimeFormatter formatter, DateTimeFormatter utcFormatter) {
        DateTimeFormatter simpleDateFormat;
        if (dt.getZone() == DateTimeZone.UTC) {
            simpleDateFormat = utcFormatter;
        } else {
            simpleDateFormat = formatter;
        }

        String result = simpleDateFormat.print(dt);

        if (isTzRelative) {
            // display format needs to invert the UTC offset if this object was
            // created with a specific offset in the 7-arg form of #new
            DateTimeZone dtz = dt.getZone();
            int offset = dtz.toTimeZone().getOffset(dt.getMillis());
            DateTimeZone invertedDTZ = DateTimeZone.forOffsetMillis(offset);
            DateTime invertedDT = dt.withZone(invertedDTZ);
            result = simpleDateFormat.print(invertedDT);
        }

        return RubyString.newString(getRuntime(), result, USASCIIEncoding.INSTANCE);
    }

    @JRubyMethod
    @Override
    public RubyArray to_a() {
        return RubyArray.newArrayMayCopy(getRuntime(), sec(), min(), hour(), mday(), month(), year(), wday(), yday(), isdst(), zone());
    }

    @JRubyMethod
    public RubyFloat to_f() {
        long millis = getTimeInMillis();
        long nanos = nsec;
        double secs = 0;
        if (millis != 0) secs += (millis / 1000.0);
        if (nanos != 0) secs += (nanos / 1000000000.0);
        return RubyFloat.newFloat(getRuntime(), secs);
    }

    @JRubyMethod(name = {"to_i", "tv_sec"})
    public RubyInteger to_i() {
        return getRuntime().newFixnum(getTimeInMillis() / 1000);
    }

    @JRubyMethod(name = {"nsec", "tv_nsec"})
    public RubyInteger nsec() {
        return getRuntime().newFixnum((getTimeInMillis() % 1000) * 1000000 + nsec);
    }

    @JRubyMethod
    public IRubyObject to_r(ThreadContext context) {
        return RubyRational.newRationalCanonicalize(
                context,
                getTimeInMillis() * 1000000 + nsec,
                1000000000);
    }

    @JRubyMethod(name = {"usec", "tv_usec"})
    public RubyInteger usec() {
        return getRuntime().newFixnum(dt.getMillisOfSecond() * 1000 + getUSec());
    }

    public void setMicroseconds(long mic) {
        long millis = getTimeInMillis() % 1000;
        long withoutMillis = getTimeInMillis() - millis;
        withoutMillis += (mic / 1000);
        dt = dt.withMillis(withoutMillis);
        nsec = (mic % 1000) * 1000;
    }

    public long microseconds() {
    	return getTimeInMillis() % 1000 * 1000 + getUSec();
    }

    @JRubyMethod
    public RubyInteger sec() {
        return getRuntime().newFixnum(dt.getSecondOfMinute());
    }

    @JRubyMethod
    public RubyInteger min() {
        return getRuntime().newFixnum(dt.getMinuteOfHour());
    }

    @JRubyMethod
    public RubyInteger hour() {
        return getRuntime().newFixnum(dt.getHourOfDay());
    }

    @JRubyMethod(name = {"mday", "day"})
    public RubyInteger mday() {
        return getRuntime().newFixnum(dt.getDayOfMonth());
    }

    @JRubyMethod(name = {"month", "mon"})
    public RubyInteger month() {
        return getRuntime().newFixnum(dt.getMonthOfYear());
    }

    @JRubyMethod
    public RubyInteger year() {
        return getRuntime().newFixnum(dt.getYear());
    }

    @JRubyMethod
    public RubyInteger wday() {
        return getRuntime().newFixnum((dt.getDayOfWeek() % 7));
    }

    @JRubyMethod
    public RubyInteger yday() {
        return getRuntime().newFixnum(dt.getDayOfYear());
    }

    @JRubyMethod
    public IRubyObject subsec() {
        Ruby runtime = getRuntime();
        long nanosec = dt.getMillisOfSecond() * 1000000 + this.nsec;

        if (nanosec % 1000000000 == 0) return RubyFixnum.zero(runtime);

        return runtime.newRationalReduced(
                nanosec, 1000000000);
    }

    @JRubyMethod(name = {"gmt_offset", "gmtoff", "utc_offset"})
    public RubyInteger gmt_offset() {
        int offset = dt.getZone().getOffset(dt.getMillis());

        return getRuntime().newFixnum(offset / 1000);
    }

    @JRubyMethod(name = {"isdst", "dst?"})
    public RubyBoolean isdst() {
        return getRuntime().newBoolean(!dt.getZone().isStandardOffset(dt.getMillis()));
    }

    @JRubyMethod
    public IRubyObject zone() {
        if (isTzRelative) return getRuntime().getNil();
        RubyString zone = getRuntime().newString(RubyTime.getRubyTimeZoneName(getRuntime(), dt));

        if (zone.isAsciiOnly()) zone.setEncoding(USASCIIEncoding.INSTANCE);

        return zone;
    }

	public static String getRubyTimeZoneName(Ruby runtime, DateTime dt) {
        return RubyTime.getRubyTimeZoneName(getEnvTimeZone(runtime).toString(), dt);
	}

	public static String getRubyTimeZoneName(String envTZ, DateTime dt) {
		// see declaration of SHORT_TZNAME
        if (SHORT_STD_TZNAME.containsKey(envTZ) && ! dt.getZone().toTimeZone().inDaylightTime(dt.toDate())) {
            return SHORT_STD_TZNAME.get(envTZ);
        }

        if (SHORT_DL_TZNAME.containsKey(envTZ) && dt.getZone().toTimeZone().inDaylightTime(dt.toDate())) {
            return SHORT_DL_TZNAME.get(envTZ);
        }

        String zone = dt.getZone().getShortName(dt.getMillis());

        Matcher offsetMatcher = TIME_OFFSET_PATTERN.matcher(zone);

        if (offsetMatcher.matches()) {
            if (zone.equals("+00:00")) {
                zone = "UTC";
            } else {
                // try non-localized time zone name
                zone = dt.getZone().getNameKey(dt.getMillis());
                if (zone == null) {
                    zone = "";
                }
            }
        }

        return zone;
	}

    public void setDateTime(DateTime dt) {
        this.dt = dt;
    }

    public DateTime getDateTime() {
        return this.dt;
    }

    public Date getJavaDate() {
        return this.dt.toDate();
    }

    @JRubyMethod
    @Override
    public RubyFixnum hash() {
    	// modified to match how hash is calculated in 1.8.2
        return getRuntime().newFixnum((int) (((dt.getMillis() / 1000) ^ microseconds()) << 1) >> 1);
    }

    @JRubyMethod(name = "_dump", optional = 1)
    public RubyString dump(IRubyObject[] args, Block unusedBlock) {
        RubyString str = (RubyString) mdump();
        str.syncVariables(this);
        return str;
    }

    public RubyObject mdump() {
        Ruby runtime = getRuntime();
        RubyTime obj = this;
        DateTime dateTime = obj.dt.toDateTime(DateTimeZone.UTC);
        byte dumpValue[] = new byte[8];
        long nanos = this.nsec;
        long usec = this.nsec / 1000;
        long nanosec = this.nsec % 1000;

        int pe =
            0x1                                 << 31 |
            ((obj.gmt().isTrue())? 0x1 : 0x0)   << 30 |
            (dateTime.getYear()-1900)           << 14 |
            (dateTime.getMonthOfYear()-1)       << 10 |
            dateTime.getDayOfMonth()            << 5  |
            dateTime.getHourOfDay();
        int se =
            dateTime.getMinuteOfHour()          << 26 |
            dateTime.getSecondOfMinute()        << 20 |
            (dateTime.getMillisOfSecond() * 1000 + (int)usec); // dump usec, not msec

        for(int i = 0; i < 4; i++) {
            dumpValue[i] = (byte)(pe & 0xFF);
            pe >>>= 8;
        }
        for(int i = 4; i < 8 ;i++) {
            dumpValue[i] = (byte)(se & 0xFF);
            se >>>= 8;
        }

        RubyString string = RubyString.newString(obj.getRuntime(), new ByteList(dumpValue));

        // 1.9 includes more nsecs
        copyInstanceVariablesInto(string);

        // nanos in numerator/denominator form
        if (nanosec != 0) {
            string.setInternalVariable("nano_num", runtime.newFixnum(nanosec));
            string.setInternalVariable("nano_den", runtime.newFixnum(1));
        }

        // submicro for 1.9.1 compat
        byte[] submicro = new byte[2];
        int len = 2;
        submicro[1] = (byte)((nanosec % 10) << 4);
        nanosec /= 10;
        submicro[0] = (byte)(nanosec % 10);
        nanosec /= 10;
        submicro[0] |= (byte)((nanosec % 10) << 4);
        if (submicro[1] == 0) len = 1;
        string.setInternalVariable("submicro", RubyString.newString(runtime, submicro, 0, len));

        // time zone
        if (dt.getZone() != DateTimeZone.UTC) {
            long offset = dt.getZone().getOffset(dt.getMillis());
            string.setInternalVariable("offset", runtime.newFixnum(offset / 1000));

            String zone = dt.getZone().getShortName(dt.getMillis());
            if (!TIME_OFFSET_PATTERN.matcher(zone).matches()) {
                string.setInternalVariable("zone", runtime.newString(zone));
            }
        }

        return string;
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(Block block) {
        return this;
    }

    @JRubyMethod(optional = 1)
    public RubyTime round(ThreadContext context, IRubyObject[] args) {
        int ndigits = args.length == 0 ? 0 : RubyNumeric.num2int(args[0]);
        // There are only 1_000_000_000 nanoseconds in 1 second,
        // so there is no need to keep more than 9 digits
        if (ndigits > 9) {
            ndigits = 9;
        } else if (ndigits < 0) {
            throw context.getRuntime().newArgumentError("negative ndigits given");
        }

        int _nsec = this.dt.getMillisOfSecond() * 1000000 + (int) (this.nsec);
        int pow = (int) Math.pow(10, 9 - ndigits);
        int rounded = ((_nsec + pow/2) / pow) * pow;
        DateTime _dt = this.dt.withMillisOfSecond(0).plusMillis(rounded / 1000000);
        return newTime(context.runtime, _dt, rounded % 1000000);
    }

   /* Time class methods */

    public static IRubyObject s_new(IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = recv.getRuntime();
        RubyTime time = new RubyTime(runtime, (RubyClass) recv, new DateTime(getLocalTimeZone(runtime)));
        time.callInit(args, block);
        return time;
    }

    /**
     * @deprecated Use {@link #newInstance(ThreadContext, IRubyObject)}
     */
    @Deprecated
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        return newInstance(context, recv);
    }

    @JRubyMethod(name = "now", meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv) {
        IRubyObject obj = ((RubyClass) recv).allocate();
        obj.getMetaClass().getBaseCallSite(RubyClass.CS_IDX_INITIALIZE).call(context, recv, obj);
        return obj;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject at(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        Ruby runtime = context.runtime;
        final RubyTime time;

        if (arg instanceof RubyTime) {
            RubyTime other = (RubyTime) arg;
            time = new RubyTime(runtime, (RubyClass) recv, other.dt);
            time.setNSec(other.getNSec());
        } else {
            long nanosecs;
            long millisecs;

            arg = numExact(runtime, arg);

            // In the case of two arguments, MRI will discard the portion of
            // the first argument after a decimal point (i.e., "floor").
            // However in the case of a single argument, any portion after
            // the decimal point is honored.
            if (arg instanceof RubyFloat || arg instanceof RubyRational) {
                long nano;

                if (arg instanceof RubyFloat) {
                    // use integral and decimal forms to calculate nanos
                    long seconds = RubyNumeric.num2long(arg);
                    double dbl = RubyNumeric.num2dbl(arg);

                    nano = (long)((dbl - seconds) * 1000000000);

                    if (dbl < 0 && nano != 0) {
                        nano += 1000000000;
                    }

                    millisecs = seconds * 1000 + nano / 1000000;
                    nanosecs = nano % 1000000;
                } else {
                    // use Rational numerator and denominator to calculate nanos
                    RubyRational rational = (RubyRational) arg;

                    // These could have rounding errors if numerator or denominator are not integral and < long. Can they be?
                    long numerator = rational.getNumerator().convertToInteger().getLongValue();
                    long denominator = rational.getDenominator().convertToInteger().getLongValue();

                    BigDecimal nanosBD = BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 50, BigDecimal.ROUND_HALF_UP).multiply(ONE_BILLION_BD);
                    BigInteger millis = nanosBD.divide(ONE_MILLION_BD).toBigInteger();
                    BigInteger nanos = nanosBD.remainder(ONE_MILLION_BD).toBigInteger();

                    millisecs = millis.longValue();
                    nanosecs = nanos.longValue();
                }
            } else {
                nanosecs = 0;
                millisecs = RubyNumeric.num2long(arg) * 1000;
            }

            try {
                time = new RubyTime(runtime, (RubyClass) recv,
                        new DateTime(millisecs, getLocalTimeZone(runtime)));
            }
            // joda-time 2.5 can throw this exception - seen locally
            catch(ArithmeticException e1) {
                throw runtime.newRangeError(e1.getMessage());
            }
            // joda-time 2.5 can throw this exception - seen on travis
            catch(IllegalFieldValueException e2) {
                throw runtime.newRangeError(e2.getMessage());
            }

            time.setNSec(nanosecs);
        }

        time.getMetaClass().getBaseCallSite(RubyClass.CS_IDX_INITIALIZE).call(context, recv, time);

        return time;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject at(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        Ruby runtime = context.runtime;

        RubyTime time = new RubyTime(runtime, (RubyClass) recv, new DateTime(0L, getLocalTimeZone(runtime)));
        long millisecs;
        long nanosecs = 0;

        arg1 = numExact(runtime, arg1);
        arg2 = numExact(runtime, arg2);

        if (arg1 instanceof RubyFloat || arg1 instanceof RubyRational) {
            double dbl = RubyNumeric.num2dbl(arg1);
            millisecs = (long) (dbl * 1000);
            nanosecs = ((long) (dbl * 1000000000)) % 1000000;
        } else {
            millisecs = RubyNumeric.num2long(arg1) * 1000;
        }

        if (arg2 instanceof RubyFloat || arg2 instanceof RubyRational) {
            double micros = RubyNumeric.num2dbl(arg2);
            double nanos = micros * 1000;
            millisecs += (long) (nanos / 1000000);
            nanosecs += (long) (nanos % 1000000);
        } else {
            long micros = RubyNumeric.num2long(arg2);
            long nanos = micros * 1000;
            millisecs += nanos / 1000000;
            nanosecs += nanos % 1000000;
        }

        long nanosecOverflow = (nanosecs / 1000000);

        time.setNSec(nanosecs % 1000000);
        time.dt = time.dt.withMillis(millisecs + nanosecOverflow);

        time.getMetaClass().getBaseCallSite(RubyClass.CS_IDX_INITIALIZE).call(context, recv, time);

        return time;
    }

    @JRubyMethod(name = {"local", "mktime"}, required = 1, optional = 9, meta = true)
    public static RubyTime local(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return createTime(context, (RubyClass) recv, args, false, false);
    }

    public static RubyTime new_local(IRubyObject recv, IRubyObject[] args) {
        return createTime(recv.getRuntime().getCurrentContext(), (RubyClass) recv, args, false, false);
    }

    @JRubyMethod(name = "new", optional = 7, meta = true)
    public static IRubyObject new19(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        if (args.length == 0) return newInstance(context, recv);

        if (args.length == 7) {
          Ruby runtime = context.runtime;

          // 7th argument can be the symbol :dst instead of an offset, so needs to be special cased
          final RubySymbol dstSymbol = RubySymbol.newSymbol(runtime, "dst");
          boolean receivedDstSymbolAsArgument = (args[6].op_equal(context, dstSymbol)).isTrue();

          final RubyBoolean isDst = RubyBoolean.newBoolean(runtime, receivedDstSymbolAsArgument);

          // Convert the 7-argument form of Time.new into the 10-argument form of Time.local:
          args = new IRubyObject[] { args[5],          // seconds
                                     args[4],          // minutes
                                     args[3],          // hours
                                     args[2],          // day
                                     args[1],          // month
                                     args[0],          // year
                                     runtime.getNil(), // weekday
                                     runtime.getNil(), // day of year
                                     isDst,            // is DST?
                                     args[6] };        // UTC offset
        }
        return createTime(context, (RubyClass) recv, args, false, true);
    }


    @JRubyMethod(name = {"utc", "gm"}, required = 1, optional = 9, meta = true)
    public static RubyTime utc(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return createTime(context, (RubyClass) recv, args, true, false);
    }

    public static RubyTime new_utc(IRubyObject recv, IRubyObject[] args) {
        return createTime(recv.getRuntime().getCurrentContext(), (RubyClass) recv, args, true, false);
    }

    @JRubyMethod(name = "_load", meta = true)
    public static RubyTime load(IRubyObject recv, IRubyObject from, Block block) {
        return s_mload(recv, (RubyTime)(((RubyClass)recv).allocate()), from);
    }

    @Override
    public Object toJava(Class target) {
        if (target.equals(Date.class)) {
            return getJavaDate();
        }
        if (target.equals(Calendar.class)) {
            Calendar cal = GregorianCalendar.getInstance();
            cal.setTime(getJavaDate());
            return cal;
        }
        if (target.equals(DateTime.class)) {
            return this.dt;
        }
        if (target.equals(java.sql.Date.class)) {
            return new java.sql.Date(dt.getMillis());
        }
        if (target.equals(java.sql.Time.class)) {
            return new java.sql.Time(dt.getMillis());
        }
        if (target.equals(java.sql.Timestamp.class)) {
            return new java.sql.Timestamp(dt.getMillis());
        }
        if (target.isAssignableFrom(Date.class)) {
            return getJavaDate();
        }
        return super.toJava(target);
    }

    // MRI: time.c ~ rb_time_interval 1.9 ... invokes time_timespec(VALUE num, TRUE)
    public static double convertTimeInterval(ThreadContext context, IRubyObject sec) {
        double seconds;
        if ( sec instanceof RubyNumeric ) {
            seconds = ((RubyNumeric) sec).getDoubleValue();
        }
        // NOTE: we can probably do better here, but we're matching MRI behavior
        // this is for converting custom objects such as ActiveSupport::Duration
        else if ( sec.respondsTo("divmod") ) {
            final Ruby runtime = context.runtime;
            IRubyObject result = sec.callMethod(context, "divmod", RubyFixnum.newFixnum(runtime, 1));
            if ( result instanceof RubyArray ) {
                seconds = ((RubyNumeric) ((RubyArray) result).eltOk(0) ).getDoubleValue(); // div
                seconds += ((RubyNumeric) ((RubyArray) result).eltOk(1) ).getDoubleValue(); // mod
            }
            else {
                throw runtime.newTypeError("unexpected divmod result: into %s" + result.getMetaClass().getName());
            }
        }
        else {
            throw context.runtime.newTypeError("can't convert " + sec.getMetaClass().getName() + " into time interval");
        }

        if ( seconds < 0 ) throw context.runtime.newArgumentError("time interval must be positive");

        return seconds;
    }

    protected static RubyTime s_mload(IRubyObject recv, RubyTime time, IRubyObject from) {
        Ruby runtime = recv.getRuntime();

        DateTime dt = new DateTime(DateTimeZone.UTC);

        byte[] fromAsBytes;
        fromAsBytes = from.convertToString().getBytes();
        if(fromAsBytes.length != 8) {
            throw runtime.newTypeError("marshaled time format differ");
        }
        int p=0;
        int s=0;
        for (int i = 0; i < 4; i++) {
            p |= ((int)fromAsBytes[i] & 0xFF) << (8 * i);
        }
        for (int i = 4; i < 8; i++) {
            s |= ((int)fromAsBytes[i] & 0xFF) << (8 * (i - 4));
        }
        boolean utc = false;
        if ((p & (1<<31)) == 0) {
            dt = dt.withMillis(p * 1000L);
            time.setUSec((s & 0xFFFFF) % 1000);
        } else {
            p &= ~(1<<31);
            utc = ((p >>> 30 & 0x1) == 0x1);
            dt = dt.withYear(((p >>> 14) & 0xFFFF) + 1900);
            dt = dt.withMonthOfYear(((p >>> 10) & 0xF) + 1);
            dt = dt.withDayOfMonth(((p >>> 5)  & 0x1F));
            dt = dt.withHourOfDay((p & 0x1F));
            dt = dt.withMinuteOfHour(((s >>> 26) & 0x3F));
            dt = dt.withSecondOfMinute(((s >>> 20) & 0x3F));
            // marsaling dumps usec, not msec
            dt = dt.withMillisOfSecond((s & 0xFFFFF) / 1000);
            time.setUSec((s & 0xFFFFF) % 1000);
        }
        time.setDateTime(dt);
        if (!utc) time.localtime();

        from.getInstanceVariables().copyInstanceVariablesInto(time);

        // pull out nanos, offset, zone
        IRubyObject nano_num = (IRubyObject) from.getInternalVariables().getInternalVariable("nano_num");
        IRubyObject nano_den = (IRubyObject) from.getInternalVariables().getInternalVariable("nano_den");
        IRubyObject offsetVar = (IRubyObject) from.getInternalVariables().getInternalVariable("offset");
        IRubyObject zoneVar = (IRubyObject) from.getInternalVariables().getInternalVariable("zone");

        if (nano_num != null && nano_den != null) {
            long nanos = nano_num.convertToInteger().getLongValue() / nano_den.convertToInteger().getLongValue();
            time.nsec += nanos;
        }

        int offset = 0;
        if (offsetVar != null && offsetVar.respondsTo("to_int")) {
            final IRubyObject $ex = runtime.getCurrentContext().getErrorInfo();
            try {
                offset = offsetVar.convertToInteger().getIntValue() * 1000;
            }
            catch (RaiseException typeError) {
                runtime.getCurrentContext().setErrorInfo($ex); // restore $!
            }
        }

        String zone = "";
        if (zoneVar != null && zoneVar.respondsTo("to_str")) {
            final IRubyObject $ex = runtime.getCurrentContext().getErrorInfo();
            try {
                zone = zoneVar.convertToString().toString();
            }
            catch (RaiseException typeError) {
                runtime.getCurrentContext().setErrorInfo($ex); // restore $!
            }
        }

        time.dt = dt.withZone(getTimeZoneWithOffset(runtime, zone, offset));
        return time;
    }

    private static final int ARG_SIZE = 7;

    private static RubyTime createTime(ThreadContext context, RubyClass klass, IRubyObject[] args, boolean gmt, boolean utcOffset) {
        Ruby runtime = context.runtime;
        int len = ARG_SIZE;
        boolean isDst = false;
        boolean setTzRelative = false;
        long nanos = 0;

        final DateTimeZone dtz;
        if (gmt) {
            dtz = DateTimeZone.UTC;
        } else if (args.length == 10 && args[9] instanceof RubyString) {
            if (utcOffset) {
                dtz = getTimeZoneFromUtcOffset(runtime, args[9]);
                setTzRelative = true;
            } else {
                dtz = getTimeZoneFromString(runtime, args[9].toString());
            }
        } else if (args.length == 10 && args[9].respondsTo("to_int")) {
            IRubyObject offsetInt = args[9].callMethod(context, "to_int");
            dtz = getTimeZone(runtime, ((RubyNumeric) offsetInt).getLongValue());
        } else {
            dtz = getLocalTimeZone(runtime);
        }

        if (args.length == 10) {
            if (args[8] instanceof RubyBoolean) isDst = args[8].isTrue();

            args = new IRubyObject[] { args[5], args[4], args[3], args[2], args[1], args[0], context.nil };
        } else {
            // MRI accepts additional wday argument which appears to be ignored.
            len = args.length;

            if (len < ARG_SIZE) {
                IRubyObject[] newArgs = new IRubyObject[ARG_SIZE];
                ArraySupport.copy(args, newArgs, 0, len);
                for (int i = len; i < ARG_SIZE; i++) newArgs[i] = context.nil;
                args = newArgs;
                len = ARG_SIZE;
            }
        }

        if (args[0] instanceof RubyString) {
            args[0] = RubyNumeric.str2inum(runtime, (RubyString) args[0], 10, false);
        }

        int year = (int) RubyNumeric.num2long(args[0]);
        int month = 1;

        if (len > 1) {
            if (!args[1].isNil()) {
                month = parseMonth(context, args[1]);
            }
            if (1 > month || month > 12) {
                throw runtime.newArgumentError("Argument out of range: for month: " + month);
            }
        }

        // int_args = { 1, 0, 0, 0, 0, 0 };
        int i_args0 = argToInt(context, args, 0 + 2, 1);
        int i_args1 = argToInt(context, args, 1 + 2, 0);
        int i_args2 = argToInt(context, args, 2 + 2, 0);
        int i_args3 = argToInt(context, args, 3 + 2, 0);
        int i_args4 = argToInt(context, args, 4 + 2, 0);
        //int i_args5 = argToInt(context, args, 5 + 2, 0);

        // Validate the times
        // Complying with MRI behavior makes it a little bit complicated. Logic copied from:
        // https://github.com/ruby/ruby/blob/trunk/time.c#L2609
        if (   (i_args0 < 1 || i_args0 > 31)
            || (i_args1 < 0 || i_args1 > 24)
            || (i_args1 == 24 && (i_args2 > 0 || i_args3 > 0))
            || (i_args2 < 0 || i_args2 > 59)
            || (i_args3 < 0 || i_args3 > 60)) {
            throw runtime.newArgumentError("argument out of range.");
        }

        DateTime dt;
        // set up with min values and then add to allow rolling over
        try {
            dt = new DateTime(year, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);

            dt = dt.plusMonths(month - 1)
                    .plusDays(i_args0 - 1)
                    .plusHours(i_args1)
                    .plusMinutes(i_args2)
                    .plusSeconds(i_args3);

            // 1.9 will observe fractional seconds *if* not given usec
            if (!args[5].isNil() && args[6].isNil()) {
                double secs = RubyFloat.num2dbl(args[5]);
                int int_millis = (int) (secs * 1000) % 1000;
                dt = dt.plusMillis(int_millis);
                nanos = ((long) (secs * 1000000000) % 1000000);
            }

            dt = dt.withZoneRetainFields(dtz);

            // If we're at a DST boundary, we need to choose the correct side of the boundary
            if (isDst) {
                final DateTime beforeDstBoundary = dt.withEarlierOffsetAtOverlap();
                final DateTime afterDstBoundary = dt.withLaterOffsetAtOverlap();

                final int offsetBeforeBoundary = dtz.getOffset(beforeDstBoundary);
                final int offsetAfterBoundary = dtz.getOffset(afterDstBoundary);

                // If the time is during DST, we need to pick the time with the highest offset
                dt = offsetBeforeBoundary > offsetAfterBoundary ? beforeDstBoundary : afterDstBoundary;
            }
        } catch (org.joda.time.IllegalFieldValueException e) {
            throw runtime.newArgumentError("time out of range");
        }

        RubyTime time = new RubyTime(runtime, klass, dt);
        // Ignores usec if 8 args (for compatibility with parsedate) or if not supplied.
        if (args.length != 8 && !args[6].isNil()) {
            boolean fractionalUSecGiven = args[6] instanceof RubyFloat || args[6] instanceof RubyRational;

            if (fractionalUSecGiven) {
                double micros = RubyNumeric.num2dbl(args[6]);
                time.dt = dt.withMillis(dt.getMillis() + (long) (micros / 1000));
                nanos = (long) Math.rint((micros * 1000) % 1000000);
            } else {
                int usec = i_args4 % 1000;
                int msec = i_args4 / 1000;

                if (i_args4 < 0) {
                    msec -= 1;
                    usec += 1000;
                }
                time.dt = dt.withMillis(dt.getMillis() + msec);
                time.setUSec(usec);
            }
        }

        if (nanos != 0)
            time.setNSec(nanos);

        time.callInit(IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
        time.setIsTzRelative(setTzRelative);
        return time;
    }

    private static int argToInt(final ThreadContext context, IRubyObject[] args, final int i, final int def) {
        IRubyObject arg = args[i];
        if (arg != context.nil) {
            if (!(arg instanceof RubyNumeric)) {
                if (arg.respondsTo("to_int")) {
                    arg = args[i] = arg.callMethod(context, "to_int");
                } else {
                    arg = args[i] = arg.callMethod(context, "to_i"); // TODO setting args[i] necessary?
                }
            }
            return RubyNumeric.num2int(arg);
        }
        return def;
    }

    private static int parseMonth(final ThreadContext context, IRubyObject arg) {
        IRubyObject tmp = arg.checkStringType();
        if (tmp != context.nil) {
            String monthStr = tmp.toString().toLowerCase();
            if (monthStr.length() == 3) {
                switch (monthStr) {
                    case "jan" : return  1;
                    case "feb" : return  2;
                    case "mar" : return  3;
                    case "apr" : return  4;
                    case "may" : return  5;
                    case "jun" : return  6;
                    case "jul" : return  7;
                    case "aug" : return  8;
                    case "sep" : return  9;
                    case "oct" : return 10;
                    case "nov" : return 11;
                    case "dec" : return 12;
                }
            }
            try {
                return Integer.parseInt(monthStr);
            } catch (NumberFormatException ex) {
                throw context.runtime.newArgumentError("Argument out of range.");
            }
        } else {
            return (int) RubyNumeric.num2long(arg);
        }
    }

    private static TimeSites sites(ThreadContext context) {
        return context.sites.Time;
    }
}
