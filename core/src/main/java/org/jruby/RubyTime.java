/***** BEGIN LICENSE BLOCK *****
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
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.tz.FixedDateTimeZone;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.TypeError;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.JavaSites.TimeSites;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ArraySupport;
import org.jruby.util.ByteList;
import org.jruby.util.RubyDateFormatter;
import org.jruby.util.TypeConverter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jruby.RubyComparable.invcmp;
import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.runtime.invokedynamic.MethodNames.OP_CMP;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.types;

/** The Time class.
 *
 * @author chadfowler, jpetersen
 */
@JRubyClass(name="Time", include="Comparable")
public class RubyTime extends RubyObject {
    public static final String UTC = "UTC";

    private static final BigDecimal ONE_MILLION_BD = BigDecimal.valueOf(1000000);
    private static final BigDecimal ONE_BILLION_BD = BigDecimal.valueOf(1000000000);
    public static final int TIME_SCALE = 1000000000;

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

    private void setIsTzRelative(boolean tzRelative) {
        isTzRelative = tzRelative;
    }

    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.TIME;
    }

    public static String getEnvTimeZone(Ruby runtime) {
        RubyString tz = runtime.tzVar;
        if (tz == null) {
            tz = runtime.newString(TZ_STRING);
            tz.setFrozen(true);
            runtime.tzVar = tz;
        }
        
        RubyHash.RubyHashEntry entry = runtime.getENV().getEntry(tz);
        if (entry.key == null || entry.key == NEVER) return null; // NO_ENTRY

        if (entry.key != tz) runtime.tzVar = (RubyString) entry.key;

        return (entry.value instanceof RubyString) ? ((RubyString) entry.value).asJavaString() : null;
    }

    public static DateTimeZone getLocalTimeZone(Ruby runtime) {
        final String tz = getEnvTimeZone(runtime);
        return tz == null ? DateTimeZone.getDefault() : getTimeZoneFromTZString(runtime, tz);
    }

    public static DateTimeZone getTimeZoneFromTZString(Ruby runtime, String zone) {
        DateTimeZone cachedZone = runtime.getTimezoneCache().get(zone);
        if (cachedZone != null) return cachedZone;

        DateTimeZone dtz = parseTZString(runtime, zone);
        runtime.getTimezoneCache().put(zone, dtz);
        return dtz;
    }

    private static DateTimeZone parseTZString(Ruby runtime, String zone) {
        Matcher tzMatcher = TZ_PATTERN.matcher(zone);
        if (tzMatcher.matches()) {
            String zoneName = tzMatcher.group(1);
            String sign = tzMatcher.group(2);
            String hours = tzMatcher.group(3);
            String minutes = tzMatcher.group(4);
            String seconds= tzMatcher.group(5);

            if (zoneName == null) zoneName = "";

            // Sign is reversed in legacy TZ notation
            return getTimeZoneFromHHMM(runtime, zoneName, sign.equals("-"), hours, minutes, seconds);
        }

        if (zone.length() == 3) {
            switch (zone.toUpperCase()) {
                // joda-time disallows use of three-letter time zone IDs.
                // Since MRI accepts these values, we need to translate them.
                case "MET": zone = "CET"; break;
                case "ROC": zone = "Asia/Taipei"; break; // Republic of China
                case "WET": zone = "Europe/Lisbon"; break; // Western European Time
                // MRI behavior: With TZ equal to "GMT" or "UTC", Time.now
                // is *NOT* considered as a proper GMT/UTC time:
                //   ENV['TZ']="GMT"; Time.now.gmt? #=> false
                //   ENV['TZ']="UTC"; Time.now.utc? #=> false
                // Hence, we need to adjust for that.
                case "UTC": zone = "Etc/UTC"; break;
                case "GMT": zone = "Etc/GMT"; break;
            }
        }

        try {
            return DateTimeZone.forID(zone);
        } catch (IllegalArgumentException e) {
            runtime.getWarnings().warning("Unrecognized time zone: " + zone);
            return DateTimeZone.UTC;
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

    public static DateTimeZone getTimeZoneFromUtcOffset(ThreadContext context, IRubyObject utcOffset) {
        final Ruby runtime = context.runtime;
        String strOffset = utcOffset.toString();

        DateTimeZone cachedZone = runtime.getTimezoneCache().get(strOffset);
        if (cachedZone != null) return cachedZone;

        DateTimeZone dtz;
        if (utcOffset instanceof RubyString) {
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
            RubyNumeric numericOffset = numExact(context, utcOffset);
            int newOffset = (int) Math.round(numericOffset.convertToFloat().getDoubleValue() * 1000);
            dtz = getTimeZoneWithOffset(runtime, "", newOffset);
        }

        runtime.getTimezoneCache().put(strOffset, dtz);
        return dtz;
    }

    // mri: time.c num_exact
    private static RubyNumeric numExact(ThreadContext context, IRubyObject v) {
        boolean typeError = false;

        switch (v.getMetaClass().getClassIndex()) {
            case NIL:
                throw context.runtime.newTypeError("can't convert nil into an exact number");

            case INTEGER: return (RubyInteger) v;

            case RATIONAL: break;

            case STRING: typeError = true; break;

            default:
                IRubyObject tmp;
                if ((tmp = v.getMetaClass().finvokeChecked(context, v, sites(context).checked_to_r)) != null) {
                    /* test to_int method availability to reject non-Numeric
                     * objects such as String, Time, etc which have to_r method. */
                    if (!sites(context).respond_to_to_int.respondsTo(context, v, v)) {
                        typeError = true; break;
                    }
                    v = tmp; break;
                }
                if (!(tmp = TypeConverter.checkIntegerType(context, v)).isNil()) {
                    v = tmp; // return tmp;
                }
                else {
                    typeError = true;
                }
        }

        switch (v.getMetaClass().getClassIndex()) {
            case INTEGER: return (RubyInteger) v;

            case RATIONAL:
                if (((RubyRational) v).getDenominator().isOne()) {
                    return ((RubyRational) v).getNumerator();
                }
                break;

            default:
                typeError = true;
                break;
        }

        if (typeError) {
            Ruby runtime = context.runtime;
            throw runtime.newTypeError(str(runtime, "can't convert ", types(runtime, v.getMetaClass()), " into an exact number"));
        }

        return (RubyNumeric) v;
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

    private static final ObjectAllocator TIME_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyTime(runtime, klass);
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

    /**
     * Set the nano-second (only) part for this time.
     *
     * Note that {@code nsec} means the 4 to 9 decimal places of sec fractional part of time.
     * For example, 123456 for {@code nsec} means {@code .000123456}, not {@code .123456000}.
     *
     * @param nsec the nano second part only (4 to 9 decimal places) of time
     */
    public void setNSec(long nsec) {
        this.nsec = nsec;
    }

    /**
     * Get the nano-second (only) part of the time.
     *
     * Note that it returns the 4 to 9 decimal places, not the entire sec fraction part till nano.
     * For example for an epoch second {@code 1500000000.123456789} returns {@code 456789}.
     *
     * @return the nano second part (only) of time
     */
    public long getNSec() {
        return nsec;
    }

    /**
     * Set the micro-second (only) part of the time.
     * @param usec
     * @see #setNSec(long)
     */
    public void setUSec(long usec) {
        this.nsec = 1000 * usec;
    }

    /**
     * Get the micro-second (only) part of the time.
     * @return the micro-second only part of this time
     * @see #getUSec()
     */
    public long getUSec() {
        return nsec / 1000;
    }

    /**
     * Use {@link #setDateTime(DateTime)} instead.
     */
    @Deprecated
    public void updateCal(DateTime dt) {
        this.dt = dt;
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

    /**
     * Create new (Ruby) Time instance.
     *
     * Note that {@code dt} of {@code org.joda.time.DateTime} represents the integer part and
     * the fraction part to milliseconds, and {@code nsec} the nano part (4 to 9 decimal places).
     *
     * For example, {@code RubyTime.newTime(rt, new DateTime(987000), 345678)} creates an epoch
     * second of {@code 987.000345678}, not {@code 987000.345678}.
     *
     * @param runtime the runtime
     * @param dt the integer part of time + the fraction part in milliseconds
     * @param nsec the nanos only party of the time (millis excluded)
     * @see #setNSec(long)
     * @return the new Time
     */
    public static RubyTime newTime(Ruby runtime, DateTime dt, long nsec) {
        RubyTime t = new RubyTime(runtime, runtime.getTime(), dt);
        t.setNSec(nsec);
        return t;
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
        final DateTimeZone zone = getTimeZoneFromUtcOffset(context, arg);
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
        return getRuntime().newBoolean(isUTC());
    }

    public boolean isUTC() {
        return !isTzRelative && dt.getZone().getID().equals("UTC");
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
        if (arg == context.nil) {
            return newTime(context.runtime, dt.withZone(getLocalTimeZone(context.runtime)), nsec);
        }
        DateTimeZone dtz = getTimeZoneFromUtcOffset(context, arg);
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

    @Deprecated
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
        if (other instanceof RubyTime) {
            return context.runtime.newBoolean(cmp((RubyTime) other) == 0);
        }
        if (other == context.nil) {
            return context.fals;
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
        if (other instanceof RubyTime) {
            throw context.runtime.newTypeError("time + time?");
        }

        double adjustMillis = RubyNumeric.num2dbl(context, numExact(context, other)) * 1000;
        return opPlusMillis(context.runtime, adjustMillis);
    }

    @Deprecated
    public IRubyObject op_plus19(ThreadContext context, IRubyObject other) {
        return op_plus(context, other);
    }

    private RubyTime opPlusMillis(final Ruby runtime, double adjustMillis) {
        long currentMillis = getTimeInMillis();

        long newMillisPart = currentMillis + (long) adjustMillis;
        long adjustNanos = (long)((adjustMillis - Math.floor(adjustMillis)) * 1000000);
        long newNanosPart =  nsec + adjustNanos;

        if (newNanosPart >= 1000000) {
            newNanosPart -= 1000000;
            newMillisPart++;
        }

        RubyTime newTime = new RubyTime(runtime, getMetaClass());
        newTime.dt = new DateTime(newMillisPart, dt.getZone());
        newTime.setNSec(newNanosPart);

        return newTime;
    }

    private RubyFloat opMinus(Ruby runtime, RubyTime other) {
        long timeInMillis = getTimeInMillis() - other.getTimeInMillis();
        double timeInSeconds = timeInMillis / 1000.0 + (getNSec() - other.getNSec()) / 1000000000.0;

        return RubyFloat.newFloat(runtime, timeInSeconds); // float number of seconds
    }

    public IRubyObject op_minus(IRubyObject other) {
        return op_minus(getRuntime().getCurrentContext(), other);
    }

    @JRubyMethod(name = "-", required = 1)
    public IRubyObject op_minus(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyTime) return opMinus(context.runtime, (RubyTime) other);

        return opMinus(context.runtime, RubyNumeric.num2dbl(context, numExact(context, other)));
    }
    
    @Deprecated
    public IRubyObject op_minus19(ThreadContext context, IRubyObject other) {
        return op_minus(context, other);
    }

    private RubyTime opMinus(Ruby runtime, double other) {
        long adjustmentInNanos = (long) (other * 1000000000);
        long adjustmentInMillis = adjustmentInNanos / 1000000;
        long adjustmentInNanosLeft = adjustmentInNanos % 1000000;

        long time = getTimeInMillis() - adjustmentInMillis;

        long nano;
        if (nsec < adjustmentInNanosLeft) {
            time--;
            nano = 1000000 - (adjustmentInNanosLeft - nsec);
        } else {
            nano = nsec - adjustmentInNanosLeft;
        }

        RubyTime newTime = new RubyTime(runtime, getMetaClass());
        newTime.dt = new DateTime(time, dt.getZone());
        newTime.setNSec(nano);

        return newTime;
    }

    @JRubyMethod(name = "===", required = 1)
    @Override
    public IRubyObject op_eqq(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyTime) {
            return context.runtime.newBoolean(RubyNumeric.fix2int(invokedynamic(context, this, OP_CMP, other)) == 0);
        }

        return context.fals;
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
        final String str = inspectCommon(TO_S_FORMATTER, TO_S_UTC_FORMATTER);
        return RubyString.newString(getRuntime(), str, USASCIIEncoding.INSTANCE);
    }

    public final IRubyObject to_s19() {
        return to_s();
    }

    private String inspectCommon(final DateTimeFormatter formatter, final DateTimeFormatter utcFormatter) {
        DateTimeFormatter simpleDateFormat;
        if (dt.getZone() == DateTimeZone.UTC && !isTzRelative) {
            simpleDateFormat = utcFormatter;
        } else {
            simpleDateFormat = formatter;
        }

        if (isTzRelative) {
            // display format needs to invert the UTC offset if this object was
            // created with a specific offset in the 7-arg form of #new
            DateTimeZone dtz = dt.getZone();
            int offset = dtz.toTimeZone().getOffset(dt.getMillis());
            DateTimeZone invertedDTZ = DateTimeZone.forOffsetMillis(offset);
            DateTime invertedDT = dt.withZone(invertedDTZ);
            return simpleDateFormat.print(invertedDT);
        }

        return simpleDateFormat.print(dt);
    }

    @Override
    public String toString() {
        return inspectCommon(TO_S_FORMATTER, TO_S_UTC_FORMATTER);
    }

    @JRubyMethod
    @Override
    public RubyArray to_a() {
        return RubyArray.newArrayNoCopy(getRuntime(), sec(), min(), hour(), mday(), month(), year(), wday(), yday(), isdst(), zone());
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

    /**
     * Get the fractional part of time in nanoseconds.
     *
     * Note that this returns the entire fraction part of the time in nanosecond, unlike
     * {@code RubyTime#getNSec}. This method represents Ruby's {@code nsec} method.
     *
     * @return the (sec) fractional part of time (in nanos)
     */
    @JRubyMethod(name = {"nsec", "tv_nsec"})
    public RubyInteger nsec() {
        return getRuntime().newFixnum(getNanos());
    }

    @JRubyMethod
    public IRubyObject to_r(ThreadContext context) {
        return RubyRational.newRationalCanonicalize(context, getTimeInMillis() * 1000000 + nsec, 1000000000);
    }

    /**
     * Get the microsecond part of this time value.
     *
     * @return the (whole) microsecond part of time
     */
    @JRubyMethod(name = {"usec", "tv_usec"})
    public RubyInteger usec() {
        return getRuntime().newFixnum(dt.getMillisOfSecond() * 1000 + getUSec());
    }

    /**
     * @return the total time in microseconds
     * @see #getTimeInMillis()
     */
    public long getTimeInMicros() {
        return getTimeInMillis() * 1000 + getUSec();
    }

    /**
     * Return the micro-seconds of this time.
     * @return micro seconds (only)
     * @see #getTimeInMicros()
     */
    public int getMicros() {
        return (int) (getTimeInMillis() % 1000) * 1000 + (int) getUSec();
    }

    /**
     * Sets the microsecond part for this Time object.
     * @param micros the microseconds to be set
     * @see #getMicros()
     */
    public void setMicros(int micros) {
        long millis = getTimeInMillis();
        millis = ( millis - (millis % 1000) ) + (micros / 1000);
        dt = dt.withMillis(millis);
        nsec = (micros % 1000) * 1000;
    }

    /**
     * @deprecated use {@link #setMicros(int)} instead
     */
    public void setMicroseconds(long micros) { setMicros((int) micros); }

    /**
     * @deprecated use {@link #getMicros()} instead
     */
    public long microseconds() {
    	return getMicros();
    }

    /**
     * Return the nano-seconds of this time.
     * @return nano seconds (only)
     */
    public int getNanos() {
        return (int) (dt.getMillisOfSecond() * 1_000_000L + getNSec());
    }

    /**
     * Sets the nanosecond part for this Time object.
     * @param nanos the nanoseconds to be set
     * @see #getNanos()
     */
    public void setNanos(int nanos) {
        long millis = getTimeInMillis();
        millis = ( millis - (millis % 1000) ) + (nanos / 1_000_000);
        dt = dt.withMillis(millis);
        nsec = (nanos % 1_000_000);
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

    @Deprecated
    public IRubyObject subsec() {
        return subsec(getRuntime().getCurrentContext());
    }

    @JRubyMethod
    public RubyNumeric subsec(final ThreadContext context) {
        long nanosec = dt.getMillisOfSecond() * 1_000_000 + this.nsec;

        RubyNumeric subsec = (RubyNumeric) RubyRational.newRationalCanonicalize(context, nanosec, 1_000_000_000);
        return subsec.isZero() ? RubyFixnum.zero(context.runtime) : subsec;
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

        RubyString zone = getRuntime().newString(getZoneName());
        if (zone.isAsciiOnly()) zone.setEncoding(USASCIIEncoding.INSTANCE);
        return zone;
    }

    public String getZoneName() {
        return getRubyTimeZoneName(getRuntime(), dt);
    }

	public static String getRubyTimeZoneName(Ruby runtime, DateTime dt) {
        final String tz = getEnvTimeZone(runtime);
        return RubyTime.getRubyTimeZoneName(tz == null ? "" : tz, dt);
	}

	public static String getRubyTimeZoneName(String envTZ, DateTime dt) {
        switch (envTZ) { // Some TZ values need to be overriden for Time#zone
            case "Etc/UCT":
            case "UCT":
                return "UCT";
            case "MET":
                return inDaylighTime(dt) ? "MEST" : "MET"; // needs to be overriden
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

	private static boolean inDaylighTime(final DateTime dt) {
        return dt.getZone().toTimeZone().inDaylightTime(dt.toDate());
    }

    public void setDateTime(DateTime dt) {
        this.dt = dt;
    }

    public DateTime getDateTime() {
        return this.dt;
    }

    @JRubyMethod
    @Override
    public RubyFixnum hash() {
        return RubyFixnum.newFixnum(getRuntime(), hashCode());
    }

    @Override
    public int hashCode() {
        // modified to match how hash is calculated in 1.8.2
        return (int) (((dt.getMillis() / 1000) ^ getMicros()) << 1) >> 1;
    }

    @JRubyMethod(name = "_dump")
    public RubyString dump(final ThreadContext context) {
        RubyString str = mdump(context.runtime);
        str.syncVariables(this);
        return str;
    }

    @JRubyMethod(name = "_dump")
    public RubyString dump(final ThreadContext context, final IRubyObject arg) {
        return dump(context);
    }

    @Deprecated
    public RubyString dump(IRubyObject[] args, Block unusedBlock) {
        RubyString str = (RubyString) mdump();
        str.syncVariables(this);
        return str;
    }

    @Deprecated
    public RubyObject mdump() {
        return mdump(getRuntime());
    }

    private RubyString mdump(final Ruby runtime) {
        DateTime dateTime = dt.toDateTime(DateTimeZone.UTC);
        byte dumpValue[] = new byte[8];
        long usec = this.nsec / 1000;
        long nanosec = this.nsec % 1000;

        int pe =
            0x1                                 << 31 |
            (isUTC() ? 0x1 : 0x0)               << 30 |
            (dateTime.getYear() - 1900)         << 14 |
            (dateTime.getMonthOfYear() - 1)     << 10 |
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

        RubyString string = RubyString.newString(runtime, new ByteList(dumpValue, false));

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
        final DateTimeZone zone = dt.getZone();
        if (zone != DateTimeZone.UTC) {
            long offset = zone.getOffset(dt.getMillis());
            string.setInternalVariable("offset", runtime.newFixnum(offset / 1000));

            String zoneName = zone.getShortName(dt.getMillis());
            if (!TIME_OFFSET_PATTERN.matcher(zoneName).matches()) {
                string.setInternalVariable("zone", runtime.newString(zoneName));
            }
        }

        return string;
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

    /**
     * @deprecated Use {@link #newInstance(ThreadContext, IRubyObject)}
     */
    @Deprecated
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
    public static RubyTime newInstance(ThreadContext context, IRubyObject recv) {
        RubyTime obj = (RubyTime) ((RubyClass) recv).allocate();
        obj.getMetaClass().getBaseCallSite(RubyClass.CS_IDX_INITIALIZE).call(context, recv, obj);
        return obj;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject at(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        final Ruby runtime = context.runtime;
        final RubyTime time;

        if (arg instanceof RubyTime) {
            RubyTime other = (RubyTime) arg;
            time = new RubyTime(runtime, (RubyClass) recv, other.dt);
            time.setNSec(other.getNSec());
        } else {
            long nanosecs;
            long millisecs;

            arg = numExact(context, arg);

            // In the case of two arguments, MRI will discard the portion of
            // the first argument after a decimal point (i.e., "floor").
            // However in the case of a single argument, any portion after the decimal point is honored.
            if (arg instanceof RubyFloat) {
                // use integral and decimal forms to calculate nanos
                long seconds = RubyNumeric.float2long((RubyFloat) arg);
                double dbl = ((RubyFloat) arg).getDoubleValue();

                long nano = (long)((dbl - seconds) * 1000000000);

                if (dbl < 0 && nano != 0) {
                    nano += 1000000000;
                }

                millisecs = seconds * 1000 + nano / 1000000;
                nanosecs = nano % 1000000;
            } else if (arg instanceof RubyRational) {
                // use Rational numerator and denominator to calculate nanos
                RubyRational rational = (RubyRational) arg;

                BigInteger numerator = rational.getNumerator().getBigIntegerValue();
                BigInteger denominator = rational.getDenominator().getBigIntegerValue();

                BigDecimal nanosBD = new BigDecimal(numerator).divide(new BigDecimal(denominator), 50, BigDecimal.ROUND_HALF_UP).multiply(ONE_BILLION_BD);
                BigInteger millis = nanosBD.divide(ONE_MILLION_BD).toBigInteger();
                BigInteger nanos = nanosBD.remainder(ONE_MILLION_BD).toBigInteger();

                millisecs = millis.longValue();
                nanosecs = nanos.longValue();
            } else {
                nanosecs = 0;
                millisecs = RubyNumeric.num2long(arg) * 1000;
            }

            try {
                time = new RubyTime(runtime, (RubyClass) recv, new DateTime(millisecs, getLocalTimeZone(runtime)));
            }
            catch (ArithmeticException|IllegalFieldValueException ex) {
                throw runtime.newRangeError(ex.getMessage());
            }

            time.setNSec(nanosecs);
        }

        return time;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject at(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        RubySymbol ms = context.runtime.newSymbol("microsecond");
        return at(context, recv, arg1, arg2, ms);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject at(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        Ruby runtime = context.runtime;

        RubyTime time = new RubyTime(runtime, (RubyClass) recv, new DateTime(0L, getLocalTimeZone(runtime)));
        long millisecs;
        long nanosecs = 0;

        arg1 = numExact(context, arg1);
        arg2 = numExact(context, arg2);

        if (arg1 instanceof RubyFloat || arg1 instanceof RubyRational) {
            double dbl = RubyNumeric.num2dbl(context, arg1);
            millisecs = (long) (dbl * 1000);
            nanosecs = ((long) (dbl * 1000000000)) % 1000000;
        } else {
            millisecs = RubyNumeric.num2long(arg1) * 1000;
        }

        if (!(arg3 instanceof RubySymbol)) {
            throw context.runtime.newArgumentError("unexpected unit " + arg3);
        }

        RubySymbol unit = (RubySymbol) arg3;

        if (arg2 instanceof RubyFloat || arg2 instanceof RubyRational) {
            if (runtime.newSymbol("microsecond").eql(unit) || runtime.newSymbol("usec").eql(unit)) {
                double micros = RubyNumeric.num2dbl(context, arg2);
                double nanos = micros * 1000;
                millisecs += (long) (nanos / 1000000);
                nanosecs += (long) (nanos % 1000000);
            } else if (runtime.newSymbol("millisecond").eql(unit)) {
                double millis = RubyNumeric.num2dbl(context, arg2);
                double nanos = millis * 1000000;
                millisecs += (long) (nanos / 1000000);
                nanosecs += (long) (nanos % 1000000);
            } else if (runtime.newSymbol("nanosecond").eql(unit) || runtime.newSymbol("nsec").eql(unit)) {
                nanosecs += RubyNumeric.num2long(arg2);
            } else {
                throw context.runtime.newArgumentError("unexpected unit " + arg3);
            }
        } else {
            if (runtime.newSymbol("microsecond").eql(unit) || runtime.newSymbol("usec").eql(unit)) {
                long micros = RubyNumeric.num2long(arg2);
                long nanos = micros * 1000;
                millisecs += nanos / 1000000;
                nanosecs += nanos % 1000000;
            } else if (runtime.newSymbol("millisecond").eql(unit)) {
                double millis = RubyNumeric.num2long(arg2);
                double nanos = millis * 1000000;
                millisecs += nanos / 1000000;
                nanosecs += nanos % 1000000;
            } else if (runtime.newSymbol("nanosecond").eql(unit) || runtime.newSymbol("nsec").eql(unit)) {
                nanosecs += RubyNumeric.num2long(arg2);
            } else {
                throw context.runtime.newArgumentError("unexpected unit " + arg3);
            }
        }

        long nanosecOverflow = (nanosecs / 1000000);

        time.setNSec(nanosecs % 1000000);
        time.dt = time.dt.withMillis(millisecs + nanosecOverflow);

        return time;
    }

    @JRubyMethod(name = {"local", "mktime"}, required = 1, optional = 9, meta = true)
    public static RubyTime local(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return ((RubyTime) ((RubyClass) recv).allocate()).initTime(context, args, false, false);
    }

    @JRubyMethod(name = "initialize", optional = 7, visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        if (args.length == 0) {
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
            this.dt = dt;
            this.setNSec(nsecs);

            return context.nil;
        }

        if (args.length == 7) {
            // 7th argument can be the symbol :dst instead of an offset, so needs to be special cased
            final RubySymbol dstSymbol = RubySymbol.newSymbol(runtime, "dst");
            boolean receivedDstSymbolAsArgument = (args[6].op_equal(context, dstSymbol)).isTrue();

            final RubyBoolean isDst = RubyBoolean.newBoolean(runtime, receivedDstSymbolAsArgument);

            // Convert the 7-argument form of Time.new into the 10-argument form of Time.local:
            args = new IRubyObject[]{
                    args[5],          // seconds
                    args[4],          // minutes
                    args[3],          // hours
                    args[2],          // day
                    args[1],          // month
                    args[0],          // year
                    context.nil, // weekday
                    context.nil, // day of year
                    isDst,            // is DST?
                    args[6]};         // UTC offset
        }

        return initTime(context, args, false, true);
    }

    @JRubyMethod(name = {"utc", "gm"}, required = 1, optional = 9, meta = true)
    public static RubyTime utc(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return ((RubyTime) ((RubyClass) recv).allocate()).initTime(context, args, true, false);
    }

    @Deprecated
    public static RubyTime load(IRubyObject recv, IRubyObject from, Block block) {
        return s_mload(recv.getRuntime().getCurrentContext(), (RubyTime) ((RubyClass) recv).allocate(), from);
    }

    @JRubyMethod(name = "_load", meta = true)
    public static RubyTime load(ThreadContext context, IRubyObject recv, IRubyObject from) {
        return s_mload(context, (RubyTime) ((RubyClass) recv).allocate(), from);
    }

    // Java API

    @Override
    public Class<?> getJavaClass() {
        return Date.class;
    }

    @Override
    public <T> T toJava(Class<T> target) {
        // retain some compatibility with `target.isAssignableFrom(Date.class)` (pre 9.2)
        if (target == Date.class || target == Comparable.class || target == Object.class) {
            return target.cast(getJavaDate());
        }
        if (target == Calendar.class || target == GregorianCalendar.class) {
            return target.cast(dt.toGregorianCalendar());
        }

        // target == Comparable.class and target == Object.class already handled above

        if (target.isAssignableFrom(DateTime.class) && target != Serializable.class) {
            return target.cast(this.dt);
        }

        // SQL
        if (target == java.sql.Date.class) {
            return target.cast(new java.sql.Date(dt.getMillis()));
        }
        if (target == java.sql.Time.class) {
            return target.cast(new java.sql.Time(dt.getMillis()));
        }
        if (target == java.sql.Timestamp.class) {
            java.sql.Timestamp timestamp = new java.sql.Timestamp(dt.getMillis());
            timestamp.setNanos(getNanos());
            return target.cast(timestamp);
        }

        // Java 8
        if (target != Serializable.class) {
            if (target.isAssignableFrom(Instant.class)) { // covers Temporal/TemporalAdjuster
                return (T) toInstant();
            }
            if (target.isAssignableFrom(LocalDateTime.class)) { // java.time.chrono.ChronoLocalDateTime.class
                return (T) toLocalDateTime();
            }
            if (target.isAssignableFrom(ZonedDateTime.class)) { // java.time.chrono.ChronoZonedDateTime.class
                return (T) toZonedDateTime();
            }
            if (target.isAssignableFrom(OffsetDateTime.class)) {
                return (T) toOffsetDateTime();
            }
        }

        return super.toJava(target);
    }

    /**
     * @return millis since epoch this (date-time) value represents
     * @since 9.2 (public)
     */
    public long getTimeInMillis() {
        return dt.getMillis();
    }

    /**
     * @return year
     * @since 9.2
     */
    public int getYear() { return dt.getYear(); }

    /**
     * @return month-of-year (1..12)
     * @since 9.2
     */
    public int getMonth() { return dt.getMonthOfYear(); }

    /**
     * @return day-of-month
     * @since 9.2
     */
    public int getDay() { return dt.getDayOfMonth(); }

    /**
     * @return hour-of-day (0..23)
     * @since 9.2
     */
    public int getHour() { return dt.getHourOfDay(); }

    /**
     * @return minute-of-hour
     * @since 9.2
     */
    public int getMinute() { return dt.getMinuteOfHour(); }

    /**
     * @return second-of-minute
     * @since 9.2
     */
    public int getSecond() { return dt.getSecondOfMinute(); }

    // getUsec / getNsec

    /**
     * @return a Java (legacy) Date instance
     * @since 1.7
     */
    public Date getJavaDate() {
        return this.dt.toDate();
    }

    /**
     * @return an instant
     * @since 9.2
     */
    public java.time.Instant toInstant() {
        return java.time.Instant.ofEpochMilli(getTimeInMillis()).plusNanos(getNSec());
    }

    /**
     * @return a date time
     * @since 9.2
     */
    public LocalDateTime toLocalDateTime() {
        return LocalDateTime.of(getYear(), getMonth(), getDay(), getHour(), getMinute(), getSecond(), getNanos());
    }

    /**
     * @return a date time
     * @since 9.2
     */
    public ZonedDateTime toZonedDateTime() {
        return ZonedDateTime.of(toLocalDateTime(), ZoneId.of(dt.getZone().getID()));
    }

    /**
     * @return a date time
     * @since 9.2
     */
    public OffsetDateTime toOffsetDateTime() {
        final int offset = dt.getZone().getOffset(dt.getMillis()) / 1000;
        return OffsetDateTime.of(toLocalDateTime(), ZoneOffset.ofTotalSeconds(offset));
    }

    // MRI: time.c ~ rb_time_interval 1.9 ... invokes time_timespec(VALUE num, TRUE)
    public static double convertTimeInterval(ThreadContext context, IRubyObject sec) {
        double seconds;
        if ( sec instanceof RubyNumeric ) {
            seconds = ((RubyNumeric) sec).getDoubleValue();
        }
        // NOTE: we can probably do better here, but we're matching MRI behavior
        // this is for converting custom objects such as ActiveSupport::Duration
        else if ( sites(context).respond_to_divmod.respondsTo(context, sec, sec) ) {
            final Ruby runtime = context.runtime;
            IRubyObject result = sites(context).divmod.call(context, sec, sec, 1);
            if ( result instanceof RubyArray ) {
                seconds = ((RubyNumeric) ((RubyArray) result).eltOk(0) ).getDoubleValue(); // div
                seconds += ((RubyNumeric) ((RubyArray) result).eltOk(1) ).getDoubleValue(); // mod
            }
            else {
                throw runtime.newTypeError(str(runtime, "unexpected divmod result: into %s", types(runtime, result.getMetaClass())));
            }
        }
        else {
            seconds = 0; boolean raise = true;
            if ( sec instanceof JavaProxy ) {
                try { // support java.lang.Number proxies
                    seconds = sec.convertToFloat().getDoubleValue(); raise = false;
                } catch (TypeError ex) { /* fallback bellow to raising a TypeError */ }
            }

            if (raise) {
                Ruby runtime = context.runtime;
                throw context.runtime.newTypeError(str(runtime, "can't convert ", types(runtime, sec.getMetaClass()), " into time interval"));
            }
        }

        if ( seconds < 0 ) throw context.runtime.newArgumentError("time interval must be positive");

        return seconds;
    }

    private static final DateTime TIME0 = new DateTime(0, DateTimeZone.UTC);

    private static RubyTime s_mload(ThreadContext context, final RubyTime time, IRubyObject from) {
        DateTime dt = TIME0;

        byte[] fromAsBytes;
        fromAsBytes = from.convertToString().getBytes();
        if (fromAsBytes.length != 8) {
            throw context.runtime.newTypeError("marshaled time format differ");
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
            final IRubyObject $ex = context.getErrorInfo();
            try {
                offset = offsetVar.convertToInteger().getIntValue() * 1000;
            }
            catch (TypeError typeError) {
                context.setErrorInfo($ex); // restore $!
            }
        }

        String zone = "";
        if (zoneVar != null && zoneVar.respondsTo("to_str")) {
            final IRubyObject $ex = context.getErrorInfo();
            try {
                zone = zoneVar.convertToString().toString();
            }
            catch (TypeError typeError) {
                context.setErrorInfo($ex); // restore $!
            }
        }

        time.dt = dt.withZone(getTimeZoneWithOffset(context.runtime, zone, offset));
        return time;
    }

    private static final int ARG_SIZE = 7;

    private RubyTime initTime(ThreadContext context, IRubyObject[] args, boolean gmt, boolean utcOffset) {
        Ruby runtime = context.runtime;
        int len = ARG_SIZE;
        boolean isDst = false;
        boolean setTzRelative = false;
        long nanos = 0;

        final DateTimeZone dtz;
        if (gmt) {
            dtz = DateTimeZone.UTC;
        } else {
            if (utcOffset) {
                if (args.length == 10 && args[9] instanceof RubyString) {
                    dtz = getTimeZoneFromUtcOffset(context, args[9]);
                    setTzRelative = true;
                } else if (args.length == 10 && sites(context).respond_to_to_int.respondsTo(context, args[9], args[9])) {
                    IRubyObject offsetInt = sites(context).to_int.call(context, args[9], args[9]);
                    dtz = getTimeZone(runtime, ((RubyNumeric) offsetInt).getLongValue());
                } else {
                    dtz = getLocalTimeZone(runtime);
                }
            } else {
                dtz = getLocalTimeZone(runtime);
            }
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
            if (args[1] != context.nil) {
                month = parseMonth(context, args[1]);
            }
            if (month < 1 || month > 12) {
                throw runtime.newArgumentError("Argument out of range: for month: " + month);
            }
        }

        // int_args = { 1, 0, 0, 0, 0, 0 };
        int i_args0 = argToInt(context, args, 0 + 2, 1);
        int i_args1 = argToInt(context, args, 1 + 2, 0);
        int i_args2 = argToInt(context, args, 2 + 2, 0);
        int i_args3 = argToInt(context, args, 3 + 2, 0);
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
        try { // set up with min values and then add to allow rolling over
            dt = new DateTime(year, month, 1, 0, 0, 0, 0, DateTimeZone.UTC);

            final Chronology chrono = dt.getChronology();
            long instant = dt.getMillis();
            instant = chrono.days().add(instant, i_args0 - 1);
            if (i_args1 != 0) instant = chrono.hours().add(instant, i_args1);
            if (i_args2 != 0) instant = chrono.minutes().add(instant, i_args2);
            if (i_args3 != 0) instant = chrono.seconds().add(instant, i_args3);

            // 1.9 will observe fractional seconds *if* not given usec
            if (args[5] != context.nil && args[6] == context.nil) {
                double secs = RubyFloat.num2dbl(context, args[5]);
                if (secs < 0 || secs >= TIME_SCALE) {
                    throw runtime.newArgumentError("argument out of range.");
                }
                int int_millis = (int) (secs * 1000) % 1000;
                instant = chrono.millis().add(instant, int_millis);
                nanos = ((long) (secs * 1000000000) % 1000000);
            }

            dt = dt.withMillis(instant);
            dt = dt.withZoneRetainFields(dtz);

            // If we're at a DST boundary, we need to choose the correct side of the boundary
            final DateTime beforeDstBoundary = dt.withEarlierOffsetAtOverlap();
            final DateTime afterDstBoundary = dt.withLaterOffsetAtOverlap();

            final int offsetBeforeBoundary = dtz.getOffset(beforeDstBoundary);
            final int offsetAfterBoundary = dtz.getOffset(afterDstBoundary);

            if (isDst) {
                // If the time is during DST, we need to pick the time with the highest offset
                dt = offsetBeforeBoundary > offsetAfterBoundary ? beforeDstBoundary : afterDstBoundary;
            }
            else {
                dt = offsetBeforeBoundary > offsetAfterBoundary ? afterDstBoundary : beforeDstBoundary;
            }
        }
        catch (org.joda.time.IllegalFieldValueException e) {
            throw runtime.newArgumentError("time out of range");
        }

        // Ignores usec if 8 args (for compatibility with parse-date) or if not supplied.
        if (args.length != 8 && args[6] != context.nil) {
            if (args[6] instanceof RubyRational) {
                RubyRational rat = (RubyRational) args[6];
                if (rat.isNegative()) {
                    throw runtime.newArgumentError("argument out of range.");
                }
                RubyRational nsec = (RubyRational) rat.op_mul(context, runtime.newFixnum(1000));
                long tmpNanos = (long) nsec.getDoubleValue(context);
                dt = dt.withMillis(dt.getMillis() + (tmpNanos / 1_000_000));
                nanos = tmpNanos % 1_000_000;
            } else if (args[6] instanceof RubyFloat) {
                RubyFloat flo = (RubyFloat) args[6];
                if (flo.isNegative()) {
                    throw runtime.newArgumentError("argument out of range.");
                }
                double micros = flo.getDoubleValue();
                dt = dt.withMillis(dt.getMillis() + (long) (micros / 1000));
                nanos = (long) Math.rint((micros * 1000) % 1_000_000);
            } else {
                int i_args4 = argToInt(context, args, 4 + 2, 0);

                if (i_args4 < 0 || i_args4 >= TIME_SCALE / 1000) {
                    throw runtime.newArgumentError("argument out of range.");
                }

                int usec = i_args4 % 1000;
                int msec = i_args4 / 1000;

                if (i_args4 < 0) {
                    msec -= 1;
                    usec += 1000;
                }
                dt = dt.withMillis(dt.getMillis() + msec);
                this.setUSec(usec);
            }
        }

        this.dt = dt;

        if (nanos != 0) this.setNSec(nanos);

        this.setIsTzRelative(setTzRelative);

        return this;
    }

    private static int argToInt(final ThreadContext context, IRubyObject[] args, final int i, final int def) {
        IRubyObject arg = args[i];
        if (arg != context.nil) {
            if (!(arg instanceof RubyNumeric)) {
                final TimeSites sites = sites(context);
                if (sites.respond_to_to_int.respondsTo(context, arg, arg)) {
                    arg = args[i] = sites.to_int.call(context, arg, arg);
                } else {
                    arg = args[i] = sites.to_i.call(context, arg, arg);
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

    @Deprecated
    public static RubyTime new_local(IRubyObject recv, IRubyObject[] args) {
        return ((RubyTime) ((RubyClass) recv).allocate()).initTime(recv.getRuntime().getCurrentContext(), args, false, false);
    }

    @Deprecated
    public static RubyTime new_utc(IRubyObject recv, IRubyObject[] args) {
        return ((RubyTime) ((RubyClass) recv).allocate()).initTime(recv.getRuntime().getCurrentContext(), args, true, false);
    }

    @Deprecated
    public static IRubyObject new19(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return ((RubyClass) recv).allocate().callMethod(context, "initialize", args);
    }
}
