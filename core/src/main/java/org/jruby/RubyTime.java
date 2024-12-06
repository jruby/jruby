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
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.TypeError;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaSites.TimeSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.util.ByteList;
import org.jruby.util.RubyDateFormatter;
import org.jruby.util.RubyTimeParser;
import org.jruby.util.Sprintf;
import org.jruby.util.time.TimeArgs;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jruby.RubyComparable.invcmp;
import static org.jruby.api.Convert.*;
import static org.jruby.api.Create.*;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.*;
import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.ThreadContext.hasKeywords;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.runtime.invokedynamic.MethodNames.OP_CMP;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.TypeConverter.typeAsString;

/** The Time class.
 *
 * @author chadfowler, jpetersen
 */
@JRubyClass(name="Time", include="Comparable")
public class RubyTime extends RubyObject {
    public static final String UTC = "UTC";

    private static final BigDecimal ONE_MILLION_BD = BigDecimal.valueOf(1000000);
    private static final BigDecimal ONE_BILLION_BD = BigDecimal.valueOf(1000000000);
    public static final int TIME_SCALE = 1_000_000_000;

    private DateTime dt;
    private long nsec;
    private IRubyObject zone;

    private final static DateTimeFormatter ONE_DAY_CTIME_FORMATTER = DateTimeFormat.forPattern("EEE MMM  d HH:mm:ss yyyy").withLocale(Locale.ENGLISH);
    private final static DateTimeFormatter TWO_DAY_CTIME_FORMATTER = DateTimeFormat.forPattern("EEE MMM dd HH:mm:ss yyyy").withLocale(Locale.ENGLISH);

    private final static DateTimeFormatter INSPECT_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").withLocale(Locale.ENGLISH);
    private final static DateTimeFormatter TZ_FORMATTER = DateTimeFormat.forPattern(" Z").withLocale(Locale.ENGLISH);
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

    /**
     * @param runtime
     * @return ""
     * @deprecated Use {@link RubyTime#getEnvTimeZone(ThreadContext)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public static String getEnvTimeZone(Ruby runtime) {
        return getEnvTimeZone(runtime.getCurrentContext());
    }

    public static String getEnvTimeZone(ThreadContext context) {
        RubyString tz = context.runtime.tzVar;
        if (tz == null) {
            tz = newString(context, TZ_STRING);
            tz.setFrozen(true);
            context.runtime.tzVar = tz;
        }
        
        RubyHash.RubyHashEntry entry = context.runtime.getENV().getEntry(tz);
        if (entry.key == null || entry.key == NEVER) return null; // NO_ENTRY

        if (entry.key != tz) context.runtime.tzVar = (RubyString) entry.key;

        return (entry.value instanceof RubyString) ? entry.value.asJavaString() : null;
    }

    /**
     * @param runtime
     * @return ""
     * @deprecated Use {@link RubyTime#getLocalTimeZone(ThreadContext)} instead
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public static DateTimeZone getLocalTimeZone(Ruby runtime) {
        return getLocalTimeZone(runtime.getCurrentContext());
    }

    public static DateTimeZone getLocalTimeZone(ThreadContext context) {
        final String tz = getEnvTimeZone(context);
        return tz == null ? DateTimeZone.getDefault() : getTimeZoneFromTZString(context, tz);
    }

    /**
     * @param runtime
     * @param zone
     * @return ""
     * @deprecated Use {@link RubyTime#getTimeZoneFromTZString(ThreadContext, String)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public static DateTimeZone getTimeZoneFromTZString(Ruby runtime, String zone) {
        return getTimeZoneFromTZString(runtime.getCurrentContext(), zone);
    }

    public static DateTimeZone getTimeZoneFromTZString(ThreadContext context, String zone) {
        DateTimeZone cachedZone = context.runtime.getTimezoneCache().get(zone);
        if (cachedZone != null) return cachedZone;

        DateTimeZone dtz = parseTZString(context, zone);
        context.runtime.getTimezoneCache().put(zone, dtz);
        return dtz;
    }

    private static DateTimeZone parseTZString(ThreadContext context, String zone) {
        Matcher tzMatcher = TZ_PATTERN.matcher(zone);
        if (tzMatcher.matches()) {
            String zoneName = tzMatcher.group(1);
            String sign = tzMatcher.group(2);
            String hours = tzMatcher.group(3);
            String minutes = tzMatcher.group(4);
            String seconds= tzMatcher.group(5);

            if (zoneName == null) zoneName = "";

            // Sign is reversed in legacy TZ notation
            return getTimeZoneFromHHMM(context, zoneName, sign.equals("-"), hours, minutes, seconds);
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
            context.runtime.getWarnings().warning("Unrecognized time zone: " + zone);
            return DateTimeZone.UTC;
        }
    }

    @Deprecated
    public static DateTimeZone getTimeZoneFromString(Ruby runtime, String zone) {
        DateTimeZone cachedZone = runtime.getTimezoneCache().get(zone);
        if (cachedZone != null) return cachedZone;

        DateTimeZone dtz = parseZoneString(runtime.getCurrentContext(), zone);
        runtime.getTimezoneCache().put(zone, dtz);
        return dtz;
    }

    private static DateTimeZone parseZoneString(ThreadContext context, String zone) {
        // TODO: handle possible differences with TZ format
        return parseTZString(context, zone);
    }

    // MRI: utc_offset_arg
    public static DateTimeZone getTimeZoneFromUtcOffset(ThreadContext context, IRubyObject arg) {
        String strOffset = arg.toString();
        DateTimeZone cachedZone = context.runtime.getTimezoneCache().get(strOffset);
        if (cachedZone != null) return cachedZone;

        DateTimeZone dtz;
        IRubyObject tmp;

        if (!(tmp = arg.checkStringType()).isNil()) {
            RubyString tmpString = (RubyString) tmp;
            ByteList tmpBytes = tmpString.getByteList();
            int n = 0;
            int min = -1;
            int sec = -1;
            if (!tmpBytes.getEncoding().isAsciiCompatible()) {
                return null;
            }
            switch (tmpBytes.realSize()) {
                case 1:
                    if (tmpBytes.get(0) == 'Z') {
                        return DateTimeZone.UTC;
                    }
                    /* Military Time Zone Names */
                    if (tmpBytes.get(0) >= 'A' && tmpBytes.get(0) <= 'I') {
                        n = tmpBytes.get(0) - 'A' + 1;
                    } else if (tmpBytes.get(0) >= 'K' && tmpBytes.get(0) <= 'M') {
                        n = tmpBytes.get(0) - 'A';
                    } else if (tmpBytes.get(0) >= 'N' && tmpBytes.get(0) <= 'Y') {
                        n = 'M' - tmpBytes.get(0);
                    } else {
                        return null;
                    }
                    n *= 3600;
                    break;
                case 3:
                    if (tmpBytes.toByteString().equals("UTC")) return DateTimeZone.UTC;
                    // +HH
                    break;
                case 5:
                    // +HHMM
                    min = 3;
                    break;
                case 6:
                    // +HH:MM
                    min = 4;
                    break;
                case 7:
                    // +HHMMSS
                    sec = 5;
                    min = 3;
                    break;
                case 9:
                    // +HH:MM:SS
                    sec = 7;
                    min = 4;
                    break;
                default:
                    return null;
            }

            if (sec > -1) {
                if (sec == 7 && tmpBytes.get(sec-1) != ':') return null;
                if (!Character.isDigit(tmpBytes.get(sec)) || !Character.isDigit(tmpBytes.get(sec+1))) return null;
                n += (tmpBytes.get(sec) * 10 + tmpBytes.get(sec+1) - '0' * 11);
            }

            if (min > -1) {
                if (min == 4 && tmpBytes.get(min-1) != ':') return null;
                if (!Character.isDigit(tmpBytes.get(min)) || !Character.isDigit(tmpBytes.get(min+1))) return null;
                if (tmpBytes.get(min) > '5') return null;
                n += (tmpBytes.get(min) * 10 + tmpBytes.get(min+1) - '0' * 11) * 60;
            }

            if (tmpBytes.realSize() >= 3) { // [+-]HH((MM)?SS)?
                byte first = (byte) tmpBytes.get(0);
                if (first != '+' && first != '-') return null;

                if (!Character.isDigit(tmpBytes.get(1)) || !Character.isDigit(tmpBytes.get(2))) return null;

                n += (tmpBytes.get(1) * 10 + tmpBytes.get(2) - '0' * 11) * 3600;

                if (first == '-') {
                    if (n == 0) return DateTimeZone.UTC;
                    n = -n;
                }
            }

            dtz = getTimeZoneWithOffset(context, "", n * 1000);
        } else {
            RubyNumeric numericOffset = numExact(context, arg);
            int newOffset = (int) Math.round(numericOffset.convertToFloat().value * 1000);
            dtz = getTimeZoneWithOffset(context, "", newOffset);
        }

        context.runtime.getTimezoneCache().put(strOffset, dtz);

        return dtz;
    }

    /**
     * @param runtime
     * @return ""
     * @deprecated Use {@link RubyTime#invalidUTCOffset(ThreadContext)} instead
     */
    public static RaiseException invalidUTCOffset(Ruby runtime) {
        return invalidUTCOffset(runtime.getCurrentContext());
    }

    public static RaiseException invalidUTCOffset(ThreadContext context) {
        return argumentError(context, "\"+HH:MM\" or \"-HH:MM\" expected for utc_offset");
    }

    /**
     * @param runtime
     * @return ""
     * @deprecated Use {@link RubyTime#invalidUTCOffset(ThreadContext)} instead
     */
    public static RaiseException invalidUTCOffset(Ruby runtime, IRubyObject value) {
        return invalidUTCOffset(runtime.getCurrentContext(), value);
    }

    public static RaiseException invalidUTCOffset(ThreadContext context, IRubyObject value) {
        return argumentError(context, str(context.runtime, "\"+HH:MM\", \"-HH:MM\", \"UTC\" or \"A\"..\"I\",\"K\"..\"Z\" expected for utc_offset: ", value));
    }

    // mri: time.c num_exact
    private static RubyNumeric numExact(ThreadContext context, IRubyObject v) {
        boolean hasTypeError = false;

        switch (v.getMetaClass().getClassIndex()) {
            case NIL:
                throw typeError(context, "can't convert nil into an exact number");
            case INTEGER: return (RubyInteger) v;
            case RATIONAL: break;
            case STRING: hasTypeError = true; break;
            default:
                IRubyObject tmp;
                if ((tmp = v.getMetaClass().finvokeChecked(context, v, sites(context).checked_to_r)) != null) {
                    /* test to_int method availability to reject non-Numeric
                     * objects such as String, Time, etc which have to_r method. */
                    if (!sites(context).respond_to_to_int.respondsTo(context, v, v)) {
                        hasTypeError = true; break;
                    }
                    v = tmp; break;
                }
                if (!(tmp = checkToInteger(context, v)).isNil()) {
                    v = tmp; // return tmp;
                }
                else {
                    hasTypeError = true;
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
                hasTypeError = true;
                break;
        }

        if (hasTypeError) throw typeError(context, "can't convert ", v, " into an exact number");

        return (RubyNumeric) v;
    }

    private static DateTimeZone getTimeZoneFromHHMM(ThreadContext context, String name, boolean positive,
                                                    String hours, String minutes, String seconds) {
        int h = Integer.parseInt(hours);
        int m = minutes != null ? Integer.parseInt(minutes) : 0;
        int s = seconds != null ? Integer.parseInt(seconds) : 0;

        if (h > 23 || m > 59) throw argumentError(context, "utc_offset out of range");

        int offset = (positive ? +1 : -1) * ((h * 3600) + m * 60 + s)  * 1000;
        return timeZoneWithOffset(name, offset);
    }

    /**
     * @param runtime
     * @param seconds
     * @return ""
     * @deprecated Use {@link RubyTime#getTimeZone(ThreadContext, long)} instead
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public static DateTimeZone getTimeZone(Ruby runtime, long seconds) {
        return getTimeZone(runtime.getCurrentContext(), seconds);
    }

    public static DateTimeZone getTimeZone(ThreadContext context, long seconds) {
        if (seconds >= 60*60*24 || seconds <= -60*60*24) throw argumentError(context, "utc_offset out of range");

        return getTimeZoneWithOffset(context, "", (int) (seconds * 1000));
    }

    /**
     * @param runtime
     * @param zoneName
     * @param offset
     * @return ""
     * @deprecated Use {@link RubyTime#getTimeZoneWithOffset(ThreadContext, String, int)} instead
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public static DateTimeZone getTimeZoneWithOffset(Ruby runtime, String zoneName, int offset) {
        return getTimeZoneWithOffset(runtime.getCurrentContext(), zoneName, offset);
    }

    public static DateTimeZone getTimeZoneWithOffset(ThreadContext context, String zoneName, int offset) {
        // validate_zone_name
        zoneName = zoneName.trim();

        String zone = zoneName + offset;

        DateTimeZone cachedZone = context.runtime.getTimezoneCache().get(zone);
        if (cachedZone != null) {
            return cachedZone;
        } else {
            try {
                DateTimeZone dtz = timeZoneWithOffset(zoneName, offset);
                if (zone.length() > 0) context.runtime.getTimezoneCache().put(zone, dtz);
                return dtz;
            } catch (IllegalArgumentException iae) {
                throw argumentError(context, "utc_offset out of range");
            }
        }
    }

    private static DateTimeZone timeZoneWithOffset(String zoneName, int offset) {
        return zoneName.isEmpty() ?
            DateTimeZone.forOffsetMillis(offset) : new FixedDateTimeZone(zoneName, null, offset, offset);
    }

    public RubyTime(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    public RubyTime(Ruby runtime, RubyClass rubyClass, DateTime dt) {
        super(runtime, rubyClass);
        this.dt = dt;
    }

    public RubyTime(Ruby runtime, RubyClass rubyClass, DateTime dt, boolean tzRelative) {
        super(runtime, rubyClass);
        this.dt = dt;
        setIsTzRelative(tzRelative);
    }

    public static RubyClass createTimeClass(ThreadContext context, RubyClass Object, RubyModule Comparable) {
        return defineClass(context, "Time", Object, RubyTime::new).
                reifiedClass(RubyTime.class).
                classIndex(ClassIndex.TIME).
                include(context, Comparable).
                defineMethods(context, RubyTime.class);
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

    public void setZoneObject(IRubyObject zone) {
        this.zone = zone;
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

    /**
     * @param runtime
     * @param nanoseconds
     * @return ""
     * @deprecated Use {@link RubyTime#newTimeFromNanoseconds(ThreadContext, long)} instead
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public static RubyTime newTimeFromNanoseconds(Ruby runtime, long nanoseconds) {
        return newTimeFromNanoseconds(runtime.getCurrentContext(), nanoseconds);
    }

    public static RubyTime newTimeFromNanoseconds(ThreadContext context, long nanoseconds) {
        long milliseconds = nanoseconds / 1000000;
        long extraNanoseconds = nanoseconds % 1000000;
        return RubyTime.newTime(context.runtime, new DateTime(milliseconds, getLocalTimeZone(context)), extraNanoseconds);
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

    @JRubyMethod(visibility = Visibility.PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject original) {
        if (!(original instanceof RubyTime)) throw typeError(getRuntime().getCurrentContext(), original, "Time");
        RubyTime originalTime = (RubyTime) original;

        // We can just use dt, since it is immutable
        dt = originalTime.dt;
        nsec = originalTime.nsec;
        isTzRelative = originalTime.isTzRelative;
        zone = originalTime.zone;

        return this;
    }

    /**
     * @return ""
     * @deprecated Use {@link RubyTime#gmtime(ThreadContext)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public RubyTime gmtime() {
        return gmtime(getCurrentContext());
    }

    @JRubyMethod(name = {"gmtime", "utc", "to_time"})
    public RubyTime gmtime(ThreadContext context) {
        return adjustTimeZone(context, DateTimeZone.UTC, false);
    }

    public final RubyTime localtime() {
        return localtime(metaClass.runtime.getCurrentContext());
    }

    @JRubyMethod(name = "localtime")
    public RubyTime localtime(ThreadContext context) {
        return adjustTimeZone(context, getLocalTimeZone(context), false);
    }

    @JRubyMethod(name = "localtime")
    public RubyTime localtime(ThreadContext context, IRubyObject arg) {
        final DateTimeZone zone = getTimeZoneFromUtcOffset(context, arg);

        if (zone == null) throw invalidUTCOffset(context);

        return adjustTimeZone(context, zone, true);
    }

    private RubyTime adjustTimeZone(ThreadContext context, final DateTimeZone zone, boolean isTzRelative) {
        boolean zoneOk = zone.equals(dt.getZone());
        if (zoneOk && isTzRelative == this.isTzRelative) return this;
        if (isFrozen()) throw context.runtime.newFrozenError("Time", this);
        if (!zoneOk) dt = dt.withZone(zone);
        setIsTzRelative(isTzRelative);
        return this;
    }

    @JRubyMethod(name = {"gmt?", "utc?", "gmtime?"})
    public RubyBoolean gmt(ThreadContext context) {
        return asBoolean(context, isUTC());
    }

    @Deprecated
    public RubyBoolean gmt() {
        return gmt(getRuntime().getCurrentContext());
    }

    public boolean isUTC() {
        return !isTzRelative && dt.getZone() == DateTimeZone.UTC;
    }

    @JRubyMethod(name = {"getgm", "getutc"})
    public RubyTime getgm() {
        return newTime(metaClass.runtime, dt.withZone(DateTimeZone.UTC), nsec);
    }

    public final RubyTime getlocal() {
        return getlocal(metaClass.runtime.getCurrentContext());
    }

    @JRubyMethod(name = "getlocal")
    public RubyTime getlocal(ThreadContext context) {
        return newTime(context.runtime, dt.withZone(getLocalTimeZone(context)), nsec);
    }

    @JRubyMethod(name = "getlocal")
    public RubyTime getlocal(ThreadContext context, IRubyObject off) {
        if (off == context.nil) return newTime(context.runtime, dt.withZone(getLocalTimeZone(context)), nsec);

        IRubyObject zone = off;
        DateTimeZone dtz;
        if (maybeTimezoneObject(zone)) {
            RubyTime t = (RubyTime) dup();
            if (zoneLocalTime(context, off, t)) return t;
        }

        if ((dtz = getTimeZoneFromUtcOffset(context, off)) == null) {
            zone = findTimezone(context, zone);
            RubyTime t = (RubyTime) dup();
            if (!zoneLocalTime(context, zone, t)) throw invalidUTCOffset(context, zone);
            return t;
        }
        else if (dtz == DateTimeZone.UTC) {
            return ((RubyTime) dup()).gmtime(context);
        }

        return ((RubyTime) dup()).adjustTimeZone(context, dtz, false);
    }

    @Deprecated
    public RubyString strftime(IRubyObject format) {
        return strftime(getRuntime().getCurrentContext(), format);
    }

    @JRubyMethod
    public RubyString strftime(ThreadContext context, IRubyObject format) {
        final RubyDateFormatter rdf = context.getRubyDateFormatter();
        return rdf.compileAndFormat(format.convertToString().getByteList(), false, dt, nsec, null);
    }

    @JRubyMethod(name = "==")
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyTime) return asBoolean(context, safeCmp(context, this, other) == 0);
        if (other == context.nil) return context.fals;

        return RubyComparable.op_equal(context, this, other);
    }

    private static int safeCmp(ThreadContext context, RubyTime self, IRubyObject other) {
        int cmpResult;
        CachingCallSite cmp = sites(context).cmp;
        if (cmp.isBuiltin(self)) {
            cmpResult = self.cmp((RubyTime) other);
        } else {
            cmpResult = RubyNumeric.fix2int(cmp.call(context, self, self, other));
        }
        return cmpResult;
    }

    @JRubyMethod(name = ">=")
    public IRubyObject op_ge(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyTime) return asBoolean(context, safeCmp(context, this, other) >= 0);

        return RubyComparable.op_ge(context, this, other);
    }

    @JRubyMethod(name = ">")
    public IRubyObject op_gt(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyTime) return asBoolean(context, safeCmp(context, this, other) > 0);

        return RubyComparable.op_gt(context, this, other);
    }

    @JRubyMethod(name = "<=")
    public IRubyObject op_le(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyTime) return asBoolean(context, safeCmp(context, this, other) <= 0);

        return RubyComparable.op_le(context, this, other);
    }

    @JRubyMethod(name = "<")
    public IRubyObject op_lt(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyTime) return asBoolean(context, safeCmp(context, this, other) < 0);

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

    @JRubyMethod(name = "+")
    public IRubyObject op_plus(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyTime) throw typeError(context, "time + time?");

        double adjustMillis = RubyNumeric.num2dbl(context, numExact(context, other)) * 1000;
        return opPlusMillis(context.runtime, adjustMillis);
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
        newTime.setIsTzRelative(isTzRelative);
        newTime.setZoneObject(zone);

        return newTime;
    }

    private RubyFloat opMinus(ThreadContext context, RubyTime other) {
        long timeInMillis = getTimeInMillis() - other.getTimeInMillis();
        double timeInSeconds = timeInMillis / 1000.0 + (getNSec() - other.getNSec()) / 1000000000.0;

        return asFloat(context, timeInSeconds); // float number of seconds
    }

    public IRubyObject op_minus(IRubyObject other) {
        return op_minus(getRuntime().getCurrentContext(), other);
    }

    @JRubyMethod(name = "-")
    public IRubyObject op_minus(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyTime) return opMinus(context, (RubyTime) other);

        return opMinus(context, RubyNumeric.num2dbl(context, numExact(context, other)));
    }

    private RubyTime opMinus(ThreadContext context, double other) {
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

        RubyTime newTime = new RubyTime(context.runtime, getMetaClass());
        newTime.dt = new DateTime(time, dt.getZone());
        newTime.setNSec(nano);
        newTime.setIsTzRelative(isTzRelative);
        newTime.setZoneObject(zone);

        return newTime;
    }

    @JRubyMethod(name = "===")
    @Override
    public IRubyObject op_eqq(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyTime) {
            return asBoolean(context, RubyNumeric.fix2int(invokedynamic(context, this, OP_CMP, other)) == 0);
        }

        return context.fals;
    }

    @JRubyMethod(name = "<=>")
    @Override
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        return other instanceof RubyTime ?
                asFixnum(context, cmp((RubyTime) other)) :
                invcmp(context, sites(context).recursive_cmp, this, other);
    }

    @JRubyMethod(name = "eql?")
    @Override
    public IRubyObject eql_p(IRubyObject other) {
        if (other instanceof RubyTime otherTime) {
            return (nsec == otherTime.nsec && getTimeInMillis() == otherTime.getTimeInMillis()) ? getRuntime().getTrue() : getRuntime().getFalse();
        }
        return getRuntime().getFalse();
    }

    @JRubyMethod(name = {"asctime", "ctime"})
    public RubyString asctime() {
        DateTimeFormatter simpleDateFormat = dt.getDayOfMonth() < 10 ? ONE_DAY_CTIME_FORMATTER : TWO_DAY_CTIME_FORMATTER;

        return RubyString.newString(getRuntime(), simpleDateFormat.print(dt), USASCIIEncoding.INSTANCE);
    }

    @Override
    @JRubyMethod
    public IRubyObject to_s(ThreadContext context) {
        DateTimeFormatter simpleDateFormat = isUTC() ? TO_S_UTC_FORMATTER : TO_S_FORMATTER;

        return newString(context, simpleDateFormat.print(getInspectDateTime()), USASCIIEncoding.INSTANCE);
    }

    @JRubyMethod
    public IRubyObject inspect() {
        DateTime dtz = getInspectDateTime();
        StringBuilder builder = new StringBuilder(INSPECT_FORMATTER.print(dtz));
        long nanos = getNanos();

        if (nanos != 0) {
            ByteList buf = new ByteList(9);
            Sprintf.sprintf(buf, ".%09d", asFixnum(getRuntime().getCurrentContext(), nanos));

            // Remove trailing zeroes
            int len = buf.realSize();
            while (buf.charAt(len - 1) == '0' && len > 0) {
                len--;
            }
            buf.setRealSize(len);
            builder.append(buf);
        }

        builder.append(isUTC() ? " UTC" : TZ_FORMATTER.print(dtz));

        return RubyString.newString(getRuntime(), builder.toString(), USASCIIEncoding.INSTANCE);
    }

    private DateTime getInspectDateTime() {
        if (isTzRelative) {
            // display format needs to invert the UTC offset if this object was
            // created with a specific offset in the 7-arg form of #new
            int offset = dt.getZone().toTimeZone().getOffset(dt.getMillis());
            return dt.withZone(DateTimeZone.forOffsetMillis(offset));
        }

        return dt;
    }

    @Override
    public String toString() {
        return to_s(getRuntime().getCurrentContext()).asJavaString();
    }

    @JRubyMethod
    @Override
    public RubyArray to_a(ThreadContext context) {
        return newArrayNoCopy(context,
            sec(), min(), hour(),
            mday(), month(), year(),
            wday(), yday(), isdst(),
            getTimezoneShortName(context));
    }

    private RubyString getTimezoneShortName(ThreadContext context) {
        return newString(context, dt.getZone().getShortName(dt.getMillis()));
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
    public RubyInteger to_i(ThreadContext context) {
        return asFixnum(context, Math.floorDiv(getTimeInMillis(), (long) 1000));
    }

    @Deprecated(since = "9.4-", forRemoval = true)
    public RubyInteger to_i() {
        return to_i(getCurrentContext());
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
    public RubyInteger nsec(ThreadContext context) {
        return asFixnum(context, getNanos());
    }

    @Deprecated(since = "9.4-", forRemoval = true)
    public RubyInteger nsec() {
        return nsec(getCurrentContext());
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
    public RubyInteger usec(ThreadContext context) {
        return asFixnum(context, dt.getMillisOfSecond() * 1000 + getUSec());
    }

    @Deprecated
    public RubyInteger usec() {
        return usec(getCurrentContext());
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
    @Deprecated(since = "9.4-", forRemoval = true)
    public void setMicroseconds(long micros) { setMicros((int) micros); }

    /**
     * @deprecated use {@link #getMicros()} instead
     */
    @Deprecated(since = "9.4-", forRemoval = true)
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
    public RubyInteger sec(ThreadContext context) {
        return asFixnum(context, dt.getSecondOfMinute());
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public RubyInteger sec() {
        return sec(getCurrentContext());
    }

    @JRubyMethod
    public RubyInteger min(ThreadContext context) {
        return asFixnum(context, dt.getMinuteOfHour());
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public RubyInteger min() {
        return min(getCurrentContext());
    }

    @JRubyMethod
    public RubyInteger hour(ThreadContext context) {
        return asFixnum(context, dt.getHourOfDay());
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public RubyInteger hour() {
        return hour(getCurrentContext());
    }

    @JRubyMethod(name = {"mday", "day"})
    public RubyInteger mday(ThreadContext context) {
        return asFixnum(context, dt.getDayOfMonth());
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public RubyInteger mday() {
        return mday(getCurrentContext());
    }

    @JRubyMethod(name = {"month", "mon"})
    public RubyInteger month(ThreadContext context) {
        return asFixnum(context, dt.getMonthOfYear());
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public RubyInteger month() {
        return month(getCurrentContext());
    }

    @JRubyMethod
    public RubyInteger year(ThreadContext context) {
        return asFixnum(context, dt.getYear());
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public RubyInteger year() {
        return year(getCurrentContext());
    }

    @JRubyMethod
    public RubyInteger wday(ThreadContext context) {
        return asFixnum(context, (dt.getDayOfWeek() % 7));
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public RubyInteger wday() {
        return wday(getCurrentContext());
    }

    @JRubyMethod
    public RubyInteger yday(ThreadContext context) {
        return asFixnum(context, dt.getDayOfYear());
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public RubyInteger yday() {
        return yday(getCurrentContext());
    }

    @JRubyMethod(name = "sunday?")
    public RubyBoolean sunday_p(ThreadContext context) {
        return asBoolean(context, (dt.getDayOfWeek() % 7) == 0);
    }

    @JRubyMethod(name = "monday?")
    public RubyBoolean monday_p(ThreadContext context) {
        return asBoolean(context, (dt.getDayOfWeek() % 7) == 1);
    }

    @JRubyMethod(name = "tuesday?")
    public RubyBoolean tuesday_p(ThreadContext context) {
        return asBoolean(context, (dt.getDayOfWeek() % 7) == 2);
    }

    @JRubyMethod(name = "wednesday?")
    public RubyBoolean wednesday_p(ThreadContext context) {
        return asBoolean(context, (dt.getDayOfWeek() % 7) == 3);
    }

    @JRubyMethod(name = "thursday?")
    public RubyBoolean thursday_p(ThreadContext context) {
        return asBoolean(context, (dt.getDayOfWeek() % 7) == 4);
    }

    @JRubyMethod(name = "friday?")
    public RubyBoolean friday_p(ThreadContext context) {
        return asBoolean(context, (dt.getDayOfWeek() % 7) == 5);
    }

    @JRubyMethod(name = "saturday?")
    public RubyBoolean saturday_p(ThreadContext context) {
        return asBoolean(context, (dt.getDayOfWeek() % 7) == 6);
    }

    @Deprecated
    public IRubyObject subsec() {
        return subsec(getRuntime().getCurrentContext());
    }

    @JRubyMethod
    public RubyNumeric subsec(final ThreadContext context) {
        long nanosec = dt.getMillisOfSecond() * 1_000_000 + this.nsec;

        RubyNumeric subsec = (RubyNumeric) RubyRational.newRationalCanonicalize(context, nanosec, TIME_SCALE);
        return subsec.isZero() ? RubyFixnum.zero(context.runtime) : subsec;
    }

    @JRubyMethod(name = {"gmt_offset", "gmtoff", "utc_offset"})
    public RubyInteger gmt_offset(ThreadContext context) {
        int offset = dt.getZone().getOffset(dt.getMillis());

        return asFixnum(context, offset / 1000);
    }

    @Deprecated(since = "9.4-", forRemoval = true)
    public RubyInteger gmt_offset() {
        return gmt_offset(getCurrentContext());
    }

    @JRubyMethod(name = {"isdst", "dst?"})
    public RubyBoolean isdst(ThreadContext context) {
        return asBoolean(context, !dt.getZone().isStandardOffset(dt.getMillis()));
    }

    @Deprecated
    public RubyBoolean isdst() {
        return isdst(getRuntime().getCurrentContext());
    }

    /**
     * @return ""
     * @deprecated Use {@link RubyTime#zone(ThreadContext)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject zone() {
        return zone(getCurrentContext());
    }

    @JRubyMethod
    public IRubyObject zone(ThreadContext context) {
        if (this.zone != null) return zone;
        if (isUTC()) return RubyString.newUSASCIIString(context.runtime, "UTC");
        if (isTzRelative) return context.nil;

        String zoneName = getRubyTimeZoneName(context, dt);
        if ("".equals(zoneName)) return context.nil;

        RubyString zone = newString(context, zoneName);
        if (zone.isAsciiOnly()) zone.setEncoding(USASCIIEncoding.INSTANCE);
        return zone;
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public String getZoneName() {
        return getRubyTimeZoneName(getCurrentContext(), dt);
    }

    /**
     * @param runtime
     * @param dt
     * @return ""
     * @deprecated Use {@link RubyTime#getRubyTimeZoneName(ThreadContext, DateTime)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public static String getRubyTimeZoneName(Ruby runtime, DateTime dt) {
        return getRubyTimeZoneName(runtime.getCurrentContext(), dt);
    }

    public static String getRubyTimeZoneName(ThreadContext context, DateTime dt) {
        final String tz = getEnvTimeZone(context);
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
        return asFixnum(getRuntime().getCurrentContext(), hashCode());
    }

    @Override
    public int hashCode() {
        // modified to match how hash is calculated in 1.8.2
        return (int) (((dt.getMillis() / 1000) ^ getMicros()) << 1) >> 1;
    }

    @JRubyMethod(name = "_dump")
    public RubyString dump(final ThreadContext context) {
        RubyString str = mdump(context);
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
        return mdump(getCurrentContext());
    }

    private RubyString mdump(ThreadContext context) {
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

        RubyString string = newString(context, new ByteList(dumpValue, false));

        // 1.9 includes more nsecs
        copyInstanceVariablesInto(string);

        // nanos in numerator/denominator form
        if (nanosec != 0) {
            string.setInternalVariable("nano_num", asFixnum(context, nanosec));
            string.setInternalVariable("nano_den", asFixnum(context, 1));
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
        string.setInternalVariable("submicro", RubyString.newString(context.runtime, submicro, 0, len));

        // time zone
        final DateTimeZone zone = dt.getZone();
        if (zone != DateTimeZone.UTC) {
            long offset = zone.getOffset(dt.getMillis());
            string.setInternalVariable("offset", asFixnum(context, offset / 1000));

            String zoneName = zone.getShortName(dt.getMillis());
            if (!TIME_OFFSET_PATTERN.matcher(zoneName).matches()) {
                string.setInternalVariable("zone", newString(context, zoneName));
            }
        }

        return string;
    }

    @JRubyMethod(optional = 1, checkArity = false)
    public RubyTime round(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 1);

        int ndigits = getNdigits(context, args);

        int _nsec = this.dt.getMillisOfSecond() * 1000000 + (int) (this.nsec);
        int pow = (int) Math.pow(10, 9 - ndigits);
        int rounded = ((_nsec + pow/2) / pow) * pow;
        DateTime _dt = this.dt.withMillisOfSecond(0).plusMillis(rounded / 1000000);
        return newTime(context.runtime, _dt, rounded % 1000000);
    }

    @JRubyMethod(optional = 1, checkArity = false)
    public RubyTime floor(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 1);

        int ndigits = getNdigits(context, args);

        int _nsec = this.dt.getMillisOfSecond() * 1000000 + (int) (this.nsec);
        int pow = (int) Math.pow(10, 9 - ndigits);
        int floored = ((_nsec) / pow) * pow;
        DateTime _dt = this.dt.withMillisOfSecond(0).plusMillis(floored / 1000000);
        return newTime(context.runtime, _dt, floored % 1000000);
    }

    @JRubyMethod(optional = 1, checkArity = false)
    public RubyTime ceil(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 1);

        int ndigits = getNdigits(context, args);

        int _nsec = this.dt.getMillisOfSecond() * 1000000 + (int) (this.nsec);
        int pow = (int) Math.pow(10, 9 - ndigits);
        int ceiled = _nsec;
        if (pow > 1) {
            ceiled = ((_nsec + pow) / pow) * pow;
        }
        DateTime _dt = this.dt.withMillisOfSecond(0).plusMillis(ceiled / 1000000);
        return newTime(context.runtime, _dt, ceiled % 1000000);
    }

    private int getNdigits(ThreadContext context, IRubyObject[] args) {
        int ndigits = args.length == 0 ? 0 : RubyNumeric.num2int(args[0]);
        // There are only 1_000_000_000 nanoseconds in 1 second,
        // so there is no need to keep more than 9 digits
        if (ndigits > 9) ndigits = 9;
        if (ndigits < 0) throw argumentError(context, "negative ndigits given");

        return ndigits;
    }

    /* Time class methods */

    /**
     * @deprecated Use {@link #newInstance(ThreadContext, IRubyObject, IRubyObject[])}
     */
    @Deprecated
    public static IRubyObject s_new(IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = recv.getRuntime();
        RubyTime time = new RubyTime(runtime, (RubyClass) recv, new DateTime(getLocalTimeZone(runtime.getCurrentContext())));
        time.callInit(args, block);
        return time;
    }

    /**
     * @deprecated Use {@link #newInstance(ThreadContext, IRubyObject, IRubyObject[])}
     */
    @Deprecated
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        return newInstance(context, recv, args);
    }

    @JRubyMethod(name = "now", meta = true, optional = 1, checkArity = false, keywords = true)
    public static RubyTime newInstance(ThreadContext context, IRubyObject recv, IRubyObject args[]) {
        int argc = Arity.checkArgumentCount(context, args, 0, 1);

        RubyTime obj = allocateInstance((RubyClass) recv);
        if (argc == 1) {
            obj.getMetaClass().getBaseCallSite(RubyClass.CS_IDX_INITIALIZE).call(context, recv, obj, args);
        } else {
            obj.getMetaClass().getBaseCallSite(RubyClass.CS_IDX_INITIALIZE).call(context, recv, obj);
        }
        return obj;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject at(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return at1(context, recv, arg);
    }

    @JRubyMethod(meta = true, keywords = true)
    public static IRubyObject at(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        IRubyObject maybeOpts = ArgsUtil.getOptionsArg(context, arg2);

        return maybeOpts.isNil() ?
                atOpts(context, recv, arg1, arg2, asSymbol(context, "microsecond"), context.nil) :
                atOpts(context, recv, arg1, context.nil, null, maybeOpts);
    }

    @JRubyMethod(meta = true, keywords = true)
    public static IRubyObject at(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        IRubyObject maybeOpts = ArgsUtil.getOptionsArg(context, arg3);

        return maybeOpts.isNil() ?
                atOpts(context, recv, arg1, arg2, arg3, context.nil) :
                atOpts(context, recv, arg1, arg2, null, arg3);
    }

    @JRubyMethod(required = 1, optional = 3, checkArity = false, meta = true)
    public static IRubyObject at(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return switch (args.length) {
            case 1 -> at(context, recv, args[0]);
            case 2 -> at(context, recv, args[0], args[1]);
            case 3 -> at(context, recv, args[0], args[1], args[2]);
            case 4 -> atOpts(context, recv, args[0], args[1], args[2], args[3]);
            default -> throw argumentError(context, args.length, 1, 4);
        };
    }

    private static IRubyObject atOpts(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject opts) {
        IRubyObject zone = ArgsUtil.extractKeywordArg(context, "in", opts);

        if (arg2.isNil() && (arg3 == null || arg3.isNil())) {
            RubyTime time = at1(context, recv, arg1);

            time = time.gmtime(context);

            if (!zone.isNil()) time = timeZoneLocal(context, zone, time);

            return time;
        }

        if (arg3 == null) arg3 = asSymbol(context, "microsecond");

        return atMulti(context, (RubyClass) recv, arg1, arg2, arg3, zone);
    }

    private static RubyTime at1(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        final RubyTime time;

        if (arg instanceof RubyTime) {
            RubyTime other = (RubyTime) arg;
            time = new RubyTime(context.runtime, (RubyClass) recv, other.dt);
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
                double dbl = ((RubyFloat) arg).value;

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

                BigDecimal nanosBD = new BigDecimal(numerator).divide(new BigDecimal(denominator), 50, RoundingMode.HALF_UP).multiply(ONE_BILLION_BD);
                BigInteger millis = nanosBD.divide(ONE_MILLION_BD).toBigInteger();
                BigInteger nanos = nanosBD.remainder(ONE_MILLION_BD).toBigInteger();

                millisecs = millis.longValue();
                nanosecs = nanos.longValue();
            } else {
                nanosecs = 0;
                millisecs = numericToLong(context, arg) * 1000;
            }

            try {
                time = new RubyTime(context.runtime, (RubyClass) recv, new DateTime(millisecs, getLocalTimeZone(context)));
            }
            catch (ArithmeticException| IllegalFieldValueException ex) {
                throw rangeError(context, ex.getMessage());
            }

            time.setNSec(nanosecs);
        }

        return time;
    }

    private static RubyTime atMulti(ThreadContext context, RubyClass recv, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject zone) {
        long millisecs;
        long nanosecs = 0;

        arg1 = numExact(context, arg1);
        arg2 = numExact(context, arg2);

        if (arg1 instanceof RubyFloat || arg1 instanceof RubyRational) {
            double dbl = RubyNumeric.num2dbl(context, arg1);
            millisecs = (long) (dbl * 1000);
            nanosecs = ((long) (dbl * 1000000000)) % 1000000;
        } else {
            millisecs = numericToLong(context, arg1) * 1000;
        }

        if (!(arg3 instanceof RubySymbol unit)) throw argumentError(context, "unexpected unit " + arg3);

        if (arg2 instanceof RubyFloat || arg2 instanceof RubyRational) {
            if (asSymbol(context, "microsecond").eql(unit) || asSymbol(context, "usec").eql(unit)) {
                double micros = RubyNumeric.num2dbl(context, arg2);
                double nanos = micros * 1000;
                millisecs += (long) (nanos / 1000000);
                nanosecs += (long) (nanos % 1000000);
            } else if (asSymbol(context, "millisecond").eql(unit)) {
                double millis = RubyNumeric.num2dbl(context, arg2);
                double nanos = millis * 1000000;
                millisecs += (long) (nanos / 1000000);
                nanosecs += (long) (nanos % 1000000);
            } else if (asSymbol(context, "nanosecond").eql(unit) || asSymbol(context, "nsec").eql(unit)) {
                nanosecs += numericToLong(context, arg2);
            } else {
                throw argumentError(context, "unexpected unit " + arg3);
            }
        } else {
            if (asSymbol(context, "microsecond").eql(unit) || asSymbol(context, "usec").eql(unit)) {
                long micros = numericToLong(context, arg2);
                long nanos = micros * 1000;
                millisecs += nanos / 1000000;
                nanosecs += nanos % 1000000;
            } else if (asSymbol(context, "millisecond").eql(unit)) {
                double millis = numericToLong(context, arg2);
                double nanos = millis * 1000000;
                millisecs += nanos / 1000000;
                nanosecs += nanos % 1000000;
            } else if (asSymbol(context, "nanosecond").eql(unit) || asSymbol(context, "nsec").eql(unit)) {
                nanosecs += numericToLong(context, arg2);
            } else {
                throw argumentError(context, "unexpected unit " + arg3);
            }
        }

        long nanosecOverflow = (nanosecs / 1000000);

        RubyTime time = new RubyTime(context.runtime, recv, new DateTime(millisecs + nanosecOverflow, getLocalTimeZone(context)));
        time.setNSec(nanosecs % 1000000);

        if (!zone.isNil()) {
            time = timeZoneLocal(context, zone, time.gmtime(context));
        }

        return time;
    }

    @JRubyMethod(name = {"local", "mktime"}, required = 1, optional = 9, checkArity = false, meta = true)
    public static RubyTime local(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 1, 10);

        RubyTime time = allocateInstance((RubyClass) recv);
        TimeArgs timeArgs = new TimeArgs(context, args);

        timeArgs.initializeTime(context, time, getLocalTimeZone(context));

        return time;
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context) {
        return initializeNow(context, context.nil);
    }

    private IRubyObject initializeNow(ThreadContext context, IRubyObject zone) {
        long msecs;
        long nsecs;
        DateTimeZone dtz;
        boolean maybeZoneObj = false;

        if (zone.isNil()) {
            dtz = getLocalTimeZone(context);
        } else if (zone == asSymbol(context, "dst")) {
            dtz = getLocalTimeZone(context);
        } else if (zone == asSymbol(context, "std")) {
            dtz = getLocalTimeZone(context);
        } else if (maybeTimezoneObject(zone)) {
            maybeZoneObj = true;
            this.zone = zone;
            dtz = DateTimeZone.UTC;
        } else {
            dtz = getTimeZoneFromUtcOffset(context, zone);
            if (dtz != null) {
                this.setIsTzRelative(true);
            } else {
                this.zone = findTimezone(context, zone);
                maybeZoneObj = true;
                dtz = DateTimeZone.UTC;
            }
        }

        POSIX posix = context.runtime.getPosix();
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

        if (maybeZoneObj) {
            if (zoneTimeLocal(context, zone, this)) {
                return this;
            } else {
                dtz = getTimeZoneFromUtcOffset(context, zone);
                if (dtz != null) {
                    this.dt = dt.withZoneRetainFields(dtz);
                } else {
                    zone = findTimezone(context, zone);
                    if (!zoneTimeLocal(context, zone, this)) throw invalidUTCOffset(context, zone);
                }
            }
        }

        return context.nil;
    }

    @JRubyMethod(name = "initialize", optional = 8, checkArity = false, visibility = PRIVATE, keywords = true)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        boolean keywords = hasKeywords(ThreadContext.resetCallInfo(context));
        int argc = args.length - (keywords ? 1 : 0);
        IRubyObject zone = context.nil;
        IRubyObject precision = context.nil;

        if (keywords) {
            IRubyObject[] opts = ArgsUtil.extractKeywordArgs(context, args[args.length - 1], "in", "precision");
            if (opts[0] != null) {
                if (args.length > 7) throw argumentError(context, "timezone argument given as positional and keyword arguments");
                zone = opts[0];
            } else if (args.length > 6) {
                zone = args[6];
            }

            if (opts[1] != null) {
                if (!(opts[1] instanceof RubyNumeric)) {
                    // Weird error since all numerics work at this point so why mention Integer?
                    throw typeError(context, str(context.runtime, "no implicit conversion of ", typeAsString(opts[1]), " into Integer"));
                }
                precision = opts[1];
            }
        } else if (args.length > 6) {
            zone = args[6];
        }

        IRubyObject nil = context.nil;

        return switch (argc) {
            case 0 -> initializeNow(context, zone);
            case 1 -> timeInitParse(context, args[0], zone, precision);
            case 2 -> keywords ?
                    initialize(context, args[0], nil, nil, nil, nil, nil, precision, zone) :
                    initialize(context, args[0], args[1], nil, nil, nil, nil, precision);
            case 3 -> keywords ?
                    initialize(context, args[0], args[1], nil, nil, nil, nil, precision, zone) :
                    initialize(context, args[0], args[1], args[2], nil, nil, nil, precision);
            case 4 -> keywords ?
                    initialize(context, args[0], args[1], args[2], nil, nil, nil, precision, zone) :
                    initialize(context, args[0], args[1], args[2], args[3], nil, nil, precision);
            case 5 -> keywords ?
                    initialize(context, args[0], args[1], args[2], args[3], nil, nil, precision, zone) :
                    initialize(context, args[0], args[1], args[2], args[3], args[4], nil, precision);
            case 6 -> keywords ?
                    initialize(context, args[0], args[1], args[2], args[3], args[4], nil, precision, zone) :
                    initialize(context, args[0], args[1], args[2], args[3], args[4], args[5], precision);
            case 7 -> initialize(context, args[0], args[1], args[2], args[3], args[4], args[5], precision, zone);
            default -> throw argumentError(context, argc, 0, 7);
        };
    }

    private IRubyObject timeInitParse(ThreadContext context, IRubyObject arg, IRubyObject zone, IRubyObject precision) {
        IRubyObject strArg = arg.checkStringType();
        if (strArg.isNil()) {
            return initialize(context, arg, context.nil, context.nil, context.nil, context.nil, context.nil, precision, zone);
        }
        RubyString str = (RubyString) strArg;

        if (!str.getEncoding().isAsciiCompatible()) {
            throw argumentError(context, "time string should have ASCII compatible encoding");
        }

        return new RubyTimeParser().parse(context, this, str, zone, precision);
    }

    private IRubyObject initialize(ThreadContext context, IRubyObject year, IRubyObject month, IRubyObject day,
                                   IRubyObject hour, IRubyObject minute, IRubyObject second, IRubyObject usec) {
        TimeArgs timeArgs = new TimeArgs(context, year, month, day, hour, minute, second, usec, false);

        timeArgs.initializeTime(context, this, getLocalTimeZone(context));

        return context.nil;
    }

    public IRubyObject initialize(ThreadContext context, IRubyObject year, IRubyObject month, IRubyObject day,
                                  IRubyObject hour, IRubyObject minute, IRubyObject second, IRubyObject usec,
                                  IRubyObject zone) {
        IRubyObject nil = context.nil;

        boolean maybeZoneObj = false;
        DateTimeZone dtz;

        // 7th argument can be the symbol :dst instead of an offset
        boolean dst = false;

        if (zone.isNil()) {
            return initialize(context, year, month, day, hour, minute, second, usec);
        } else if (zone == asSymbol(context, "dst")) {
            dst = true;
            dtz = getLocalTimeZone(context);
        } else if (zone == asSymbol(context, "std")) {
            dst = false;
            dtz = getLocalTimeZone(context);
        } else if (maybeTimezoneObject(zone)) {
            maybeZoneObj = true;
            this.zone = zone;
            dtz = DateTimeZone.UTC;
        } else {
            dtz = getTimeZoneFromUtcOffset(context, zone);
            if (dtz != null) {
                this.setIsTzRelative(true);
            } else {
                this.zone = findTimezone(context, zone);
                maybeZoneObj = true;
                dtz = DateTimeZone.UTC;
            }
        }

        TimeArgs timeArgs = new TimeArgs(context, year, month, day, hour, minute, second, usec, dst);

        timeArgs.initializeTime(context, this, dtz);

        if (maybeZoneObj) {
            if (zoneTimeLocal(context, zone, this)) return this;

            dtz = getTimeZoneFromUtcOffset(context, zone);
            if (dtz != null) {
                dt = dt.withZoneRetainFields(dtz);
            } else {
                if ((zone = findTimezone(context, zone)) == null) throw invalidUTCOffset(context);
                if (!zoneTimeLocal(context, zone, this)) throw invalidUTCOffset(context, zone);
            }
        }

        return nil;
    }

    private static boolean maybeTimezoneObject(IRubyObject obj) {
        return !(obj.isNil() || obj instanceof RubyInteger || obj instanceof RubyString);
    }

    private IRubyObject findTimezone(ThreadContext context, IRubyObject zone) {
        IRubyObject foundZone = Helpers.invokeChecked(context, getMetaClass(), "find_timezone", zone);
        if (foundZone == null || foundZone.isNil()) throw invalidUTCOffset(context, zone);
        return foundZone;
    }

    @JRubyMethod(name = {"utc", "gm"}, required = 1, optional = 9, checkArity = false, meta = true)
    public static RubyTime utc(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 1, 10);

        RubyTime time = allocateInstance((RubyClass) recv);

        TimeArgs timeArgs = new TimeArgs(context, args);

        timeArgs.initializeTime(context, time, DateTimeZone.UTC);

        return time;
    }

    private static RubyTime allocateInstance(RubyClass recv) {
        return (RubyTime) recv.allocate();
    }

    @Deprecated
    public static RubyTime load(IRubyObject recv, IRubyObject from, Block block) {
        return s_mload(recv.getRuntime().getCurrentContext(), allocateInstance((RubyClass) recv), from);
    }

    @JRubyMethod(name = "_load", meta = true)
    public static RubyTime load(ThreadContext context, IRubyObject recv, IRubyObject from) {
        return s_mload(context, allocateInstance((RubyClass) recv), from);
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
        final long millis = getTimeInMillis();
        // Keep this long cast to work on Java 8
        long sec = Math.floorDiv(millis, (long) 1000);
        // Keep this long cast to work on Java 8
        long nanoAdj = getNSec() + (Math.floorMod(millis, (long) 1000) * 1_000_000);
        return java.time.Instant.ofEpochSecond(sec, nanoAdj);
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
        if (sec instanceof RubyNumeric num) seconds = num.getDoubleValue();

        // NOTE: we can probably do better here, but we're matching MRI behavior
        // this is for converting custom objects such as ActiveSupport::Duration
        else if ( sites(context).respond_to_divmod.respondsTo(context, sec, sec) ) {
            IRubyObject result = sites(context).divmod.call(context, sec, sec, 1);
            if (!(result instanceof RubyArray arr)) throw typeError(context, "unexpected divmod result: into ", result, "");

            seconds = ((RubyNumeric) arr.eltOk(0)).getDoubleValue() + // div
                    ((RubyNumeric) arr.eltOk(1)).getDoubleValue(); // mod
        } else {
            boolean raise = true;
            seconds = 0;

            if (sec instanceof JavaProxy) {
                try { // support java.lang.Number proxies
                    seconds = sec.convertToFloat().value;
                    raise = false;
                } catch (TypeError ex) { /* fallback bellow to raising a TypeError */ }
            }

            if (raise) throw typeError(context, "can't convert ", sec, " into time interval");
        }

        if (seconds < 0) throw argumentError(context,"time interval must not be negative");

        return seconds;
    }

    private static final DateTime TIME0 = new DateTime(0, DateTimeZone.UTC);

    private static RubyTime s_mload(ThreadContext context, final RubyTime time, IRubyObject from) {
        DateTime dt = TIME0;

        byte[] fromAsBytes;
        fromAsBytes = from.convertToString().getBytes();
        if (fromAsBytes.length != 8) throw typeError(context, "marshaled time format differ");

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
            int year = ((p >>> 14) & 0xFFFF) + 1900;
            int month = ((p >>> 10) & 0xF);
            if (month >= 12) {
                month -= 12;
                year += 1;
            }
            dt = dt.withYear(year);
            dt = dt.withMonthOfYear(month + 1);
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

        time.dt = dt.withZone(getTimeZoneWithOffset(context, zone, offset));
        return time;
    }

    public static boolean zoneTimeLocal(ThreadContext context, IRubyObject zone, RubyTime time) {
        IRubyObject utc = Helpers.invokeChecked(context, zone, "local_to_utc", time);
        if (utc == null) return false;
        long s = extractTime(context, utc);
        DateTime dt = time.getDateTime();
        dt = dt.withZoneRetainFields(getTimeZoneWithOffset(context, "", (int) (dt.getMillis() - s)));
        time.setDateTime(dt);
        time.setZoneObject(zone);

        return true;
    }

    public static boolean zoneLocalTime(ThreadContext context, IRubyObject zone, RubyTime time) {
        IRubyObject local = Helpers.invokeChecked(context, zone, "utc_to_local", time);
        if (local == null) return false;
        long s = extractTime(context, local);
        DateTime dt = time.getDateTime();
        dt = dt.withZone(getTimeZoneWithOffset(context, "", (int) (s - dt.getMillis())));
        time.setDateTime(dt);
        time.setZoneObject(zone);

        return true;
    }

    public static RubyTime timeZoneLocal(ThreadContext context, IRubyObject off, RubyTime time) {
        IRubyObject zone = off;

        DateTimeZone dtz;

        if (zoneLocalTime(context, zone, time)) return time;

        if ((dtz = getTimeZoneFromUtcOffset(context, off)) == null) {
            zone = time.findTimezone(context, zone);
            if (!zoneLocalTime(context, zone, time)) throw invalidUTCOffset(context, zone);
            return time;
        } else if (dtz == DateTimeZone.UTC) {
            return time.gmtime(context);
        }

        time.adjustTimeZone(context, dtz, false);

        return time;
    }

    private static long extractTime(ThreadContext context, IRubyObject time) {
        long t;

        if (time instanceof RubyTime) {
            return ((RubyTime) time).getDateTime().withZoneRetainFields(DateTimeZone.UTC).getMillis();
        } else if (time instanceof RubyStruct) {
            t = ((RubyStruct) time).aref(asSymbol(context, "to_i")).convertToInteger().getLongValue();
        } else {
            t =  time.callMethod(context, "to_i").convertToInteger().getLongValue();
        }

        return t * 1000;
    }

    private static TimeSites sites(ThreadContext context) {
        return context.sites.Time;
    }
}
