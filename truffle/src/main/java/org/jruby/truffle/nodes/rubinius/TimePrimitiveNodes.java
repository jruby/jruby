/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.ExactMath;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.tz.FixedDateTimeZone;
import org.jruby.runtime.Helpers;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.TimeNodes;
import org.jruby.truffle.nodes.objects.AllocateObjectNode;
import org.jruby.truffle.nodes.objects.AllocateObjectNodeGen;
import org.jruby.truffle.nodes.time.ReadTimeZoneNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.StringOperations;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.util.RubyDateFormatter;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rubinius primitives associated with the Ruby {@code Time} class.
 */
public abstract class TimePrimitiveNodes {

    @RubiniusPrimitive(name = "time_s_now")
    public static abstract class TimeSNowPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private AllocateObjectNode allocateObjectNode;
        @Child private ReadTimeZoneNode readTimeZoneNode;

        public TimeSNowPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
            readTimeZoneNode = new ReadTimeZoneNode(context, sourceSection);
        }

        @Specialization
        public DynamicObject timeSNow(VirtualFrame frame, DynamicObject timeClass) {
            // TODO CS 4-Mar-15 whenever we get time we have to convert lookup and time zone to a string and look it up - need to cache somehow...
            return allocateObjectNode.allocate(timeClass, now((DynamicObject) readTimeZoneNode.execute(frame)), nil());
        }

        @TruffleBoundary
        private DateTime now(DynamicObject timeZone) {
            assert RubyGuards.isRubyString(timeZone);
            return DateTime.now(org.jruby.RubyTime.getTimeZoneFromTZString(getContext().getRuntime(), timeZone.toString()));
        }

    }

    @RubiniusPrimitive(name = "time_s_dup", needsSelf = false)
    public static abstract class TimeSDupPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private AllocateObjectNode allocateObjectNode;

        public TimeSDupPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public DynamicObject timeSDup(DynamicObject other) {
            return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(other), TimeNodes.getDateTime(other), Layouts.TIME.getOffset(other));
        }

    }

    @RubiniusPrimitive(name = "time_s_specific", needsSelf = false, lowerFixnumParameters = { 1 })
    public static abstract class TimeSSpecificPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private ReadTimeZoneNode readTimeZoneNode;

        public TimeSSpecificPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readTimeZoneNode = new ReadTimeZoneNode(context, sourceSection);
        }

        @Specialization(guards = { "isUTC", "isNil(offset)" })
        public DynamicObject timeSSpecificUTC(long seconds, int nanoseconds, boolean isUTC, Object offset) {
            // TODO(CS): overflow checks needed?
            final long milliseconds = getMillis(seconds, nanoseconds);
            return Layouts.TIME.createTime(getContext().getCoreLibrary().getTimeFactory(), time(milliseconds), nil());
        }

        @Specialization(guards = { "!isUTC", "isNil(offset)" })
        public DynamicObject timeSSpecific(VirtualFrame frame, long seconds, int nanoseconds, boolean isUTC, Object offset) {
            // TODO(CS): overflow checks needed?
            final long milliseconds = getMillis(seconds, nanoseconds);
            return Layouts.TIME.createTime(getContext().getCoreLibrary().getTimeFactory(), localtime(milliseconds, (DynamicObject) readTimeZoneNode.execute(frame)), offset);
        }

        private long getMillis(long seconds, int nanoseconds) {
            try {
                return ExactMath.addExact(ExactMath.multiplyExact(seconds, 1000L), (nanoseconds / 1_000_000));
            } catch (ArithmeticException e) {
                CompilerDirectives.transferToInterpreter();
                String message = String.format("UNIX epoch + %d seconds out of range for Time (Joda-Time limitation)", seconds);
                throw new RaiseException(getContext().getCoreLibrary().rangeError(message, this));
            }
        }

        @TruffleBoundary
        private DateTime time(long milliseconds) {
            return new DateTime(milliseconds, DateTimeZone.UTC);
        }

        @TruffleBoundary
        private DateTime localtime(long milliseconds, DynamicObject timeZone) {
            assert RubyGuards.isRubyString(timeZone);
            return new DateTime(milliseconds, org.jruby.RubyTime.getTimeZoneFromTZString(getContext().getRuntime(), timeZone.toString()));
        }

    }

    @RubiniusPrimitive(name = "time_seconds")
    public static abstract class TimeSecondsPrimitiveNode extends RubiniusPrimitiveNode {

        public TimeSecondsPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long timeSeconds(DynamicObject time) {
            return TimeNodes.getDateTime(time).getMillis() / 1_000;
        }

    }

    @RubiniusPrimitive(name = "time_useconds")
    public static abstract class TimeUSecondsPrimitiveNode extends RubiniusPrimitiveNode {

        public TimeUSecondsPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long timeUSeconds(DynamicObject time) {
            return TimeNodes.getDateTime(time).getMillisOfSecond() * 1_000L;
        }

    }

    @RubiniusPrimitive(name = "time_decompose")
    public static abstract class TimeDecomposePrimitiveNode extends RubiniusPrimitiveNode {

        @Child private ReadTimeZoneNode readTimeZoneNode;

        public TimeDecomposePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readTimeZoneNode = new ReadTimeZoneNode(context, sourceSection);
        }

        @Specialization
        public DynamicObject timeDecompose(VirtualFrame frame, DynamicObject time) {
            CompilerDirectives.transferToInterpreter();
            final DateTime dateTime = TimeNodes.getDateTime(time);
            final int sec = dateTime.getSecondOfMinute();
            final int min = dateTime.getMinuteOfHour();
            final int hour = dateTime.getHourOfDay();
            final int day = dateTime.getDayOfMonth();
            final int month = dateTime.getMonthOfYear();
            final int year = dateTime.getYear();

            int wday = dateTime.getDayOfWeek();

            if (wday == 7) {
                wday = 0;
            }

            final int yday = dateTime.getDayOfYear();
            final boolean isdst = false;

            final String envTimeZoneString = readTimeZoneNode.execute(frame).toString();
            String zoneString = org.jruby.RubyTime.getRubyTimeZoneName(envTimeZoneString, dateTime);
            Object zone;
            if (zoneString.matches(".*-\\d+")) {
                zone = nil();
            } else {
                zone = createString(StringOperations.encodeByteList(zoneString, UTF8Encoding.INSTANCE));
            }

            final Object[] decomposed = new Object[]{ sec, min, hour, day, month, year, wday, yday, isdst, zone };
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), decomposed, decomposed.length);
        }

    }

    @RubiniusPrimitive(name = "time_strftime")
    public static abstract class TimeStrftimePrimitiveNode extends RubiniusPrimitiveNode {

        public TimeStrftimePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(format)")
        public DynamicObject timeStrftime(DynamicObject time, DynamicObject format) {
            final RubyDateFormatter rdf = getContext().getRuntime().getCurrentContext().getRubyDateFormatter();
            // TODO CS 15-Feb-15 ok to just pass nanoseconds as 0?
            return createString(rdf.formatToByteList(rdf.compilePattern(StringOperations.getByteList(format), false), TimeNodes.getDateTime(time), 0, null));
        }

    }

    @RubiniusPrimitive(name = "time_s_from_array", needsSelf = true, lowerFixnumParameters = { 0 /*sec*/, 1 /* min */, 2 /* hour */, 3 /* mday */, 4 /* month */, 5 /* year */, 6 /*nsec*/, 7 /*isdst*/ })
    public static abstract class TimeSFromArrayPrimitiveNode extends RubiniusPrimitiveNode {

        @Child ReadTimeZoneNode readTimeZoneNode;
        @Child AllocateObjectNode allocateObjectNode;

        public TimeSFromArrayPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readTimeZoneNode = new ReadTimeZoneNode(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public DynamicObject timeSFromArray(VirtualFrame frame, DynamicObject timeClass, int sec, int min, int hour, int mday, int month, int year,
                                            int nsec, int isdst, boolean fromutc, Object utcoffset) {
            return buildTime(frame, timeClass, sec, min, hour, mday, month, year, nsec, isdst, fromutc, utcoffset);
        }

        @Specialization(guards = "!isInteger(sec) || !isInteger(nsec)")
        public DynamicObject timeSFromArrayFallback(VirtualFrame frame, DynamicObject timeClass, Object sec, int min, int hour, int mday, int month, int year,
                                                    Object nsec, int isdst, boolean fromutc, Object utcoffset) {
            return null; // Primitive failure
        }

        private DynamicObject buildTime(VirtualFrame frame, DynamicObject timeClass, int sec, int min, int hour, int mday, int month, int year,
                                        int nsec, int isdst, boolean fromutc, Object utcoffset) {
            CompilerDirectives.transferToInterpreter();

            if (sec < 0 || sec > 59 ||
                    min < 0 || min > 59 ||
                    hour < 0 || hour > 23 ||
                    mday < 1 || mday > 31 ||
                    month < 1 || month > 12) {
                throw new RaiseException(getContext().getCoreLibrary().argumentErrorOutOfRange(this));
            }

            final DateTimeZone zone;
            if (fromutc) {
                zone = DateTimeZone.UTC;
            } else if (utcoffset == nil()) {
                String tz = readTimeZoneNode.execute(frame).toString();
                zone = org.jruby.RubyTime.getTimeZoneFromTZString(getContext().getRuntime(), tz);
            } else if (utcoffset instanceof Integer) {
                zone = DateTimeZone.forOffsetMillis(((int) utcoffset) * 1_000);
            } else if (utcoffset instanceof Long) {
                zone = DateTimeZone.forOffsetMillis((int) ((long) utcoffset) * 1_000);
            } else if (utcoffset instanceof DynamicObject) {
                final int millis = cast(ruby(frame, "(offset * 1000).to_i", "offset", utcoffset));
                zone = DateTimeZone.forOffsetMillis(millis);
            } else {
                throw new UnsupportedOperationException(String.format("%s %s %s %s", isdst, fromutc, utcoffset, utcoffset.getClass()));
            }

            if (isdst == -1) {
                final DateTime dateTime = new DateTime(year, month, mday, hour, min, sec, nsec / 1_000_000, zone);
                return allocateObjectNode.allocate(timeClass, dateTime, utcoffset);
            } else {
                throw new UnsupportedOperationException(String.format("%s %s %s %s", isdst, fromutc, utcoffset, utcoffset.getClass()));
            }
        }

        private static int cast(Object value) {
            if (value instanceof Integer) {
                return (int) value;
            } else if (value instanceof Long) {
                return (int) (long) value;
            } else {
                throw new UnsupportedOperationException("Can't cast " + value.getClass());
            }
        }

    }

    @RubiniusPrimitive(name = "time_nseconds")
    public static abstract class TimeNSecondsPrimitiveNode extends RubiniusPrimitiveNode {

        public TimeNSecondsPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long timeNSeconds(DynamicObject time) {
            return TimeNodes.getDateTime(time).getMillisOfSecond() * 1_000_000L;
        }

    }

    @RubiniusPrimitive(name = "time_set_nseconds", lowerFixnumParameters = 0)
    public static abstract class TimeSetNSecondsPrimitiveNode extends RubiniusPrimitiveNode {

        public TimeSetNSecondsPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public long timeSetNSeconds(DynamicObject time, int nanoseconds) {
            Layouts.TIME.setDateTime(time, TimeNodes.getDateTime(time).withMillisOfSecond(nanoseconds / 1_000_000));
            return nanoseconds;
        }

    }

    // TODO (pitr 25-Nov-2015): clen up the implementation
    @RubiniusPrimitive(name = "time_env_zone")
    public static abstract class TimeEnvZonePrimitiveNode extends RubiniusPrimitiveNode {

        @Child private ReadTimeZoneNode readTimeZoneNode;

        public TimeEnvZonePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readTimeZoneNode = new ReadTimeZoneNode(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object timeEnvZone(VirtualFrame frame, DynamicObject time) {
            DateTime dt = Layouts.TIME.getDateTime(time);
            final DynamicObject zoneName = (DynamicObject) readTimeZoneNode.execute(frame);
            final DateTimeZone zone = parseTimeZoneString(StringOperations.getString(getContext(), zoneName));

            dt = dt.withZone(zone);
            Layouts.TIME.setDateTime(time, dt);
            Layouts.TIME.setOffset(time, dt.getZone().getOffset(dt.getMillis()) / 1_000);

            final String timezone = dt.getZone().getShortName(dt.getMillis());

            return createString(StringOperations.encodeByteList(timezone, UTF8Encoding.INSTANCE));
        }

        // Following private methods in this class were copied over from org.jruby.RubyTime.
        // Slight modifications were made.

        /* Version: EPL 1.0/GPL 2.0/LGPL 2.1
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
         */

        private static final Pattern TZ_PATTERN
                = Pattern.compile("([^-\\+\\d]+)?([\\+-]?)(\\d+)(?::(\\d+))?(?::(\\d+))?");

        private static final Map<String, String> LONG_TZNAME = Helpers.map(
                "MET", "CET", // JRUBY-2759
                "ROC", "Asia/Taipei", // Republic of China
                "WET", "Europe/Lisbon" // Western European Time
        );

        private DateTimeZone parseTimeZoneString(String zone) {
            String upZone = zone.toUpperCase(Locale.ENGLISH);

            Matcher tzMatcher = TZ_PATTERN.matcher(zone);
            if (tzMatcher.matches()) {
                String zoneName = tzMatcher.group(1);
                String sign = tzMatcher.group(2);
                String hours = tzMatcher.group(3);
                String minutes = tzMatcher.group(4);
                String seconds = tzMatcher.group(5);

                if (zoneName == null) {
                    zoneName = "";
                }

                // Sign is reversed in legacy TZ notation
                return getTimeZoneFromHHMM(zoneName, sign.equals("-"), hours, minutes, seconds);
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
                    return DateTimeZone.UTC;
                }
            }
        }

        private DateTimeZone getTimeZoneFromHHMM(String name, boolean positive, String hours, String minutes, String seconds) {
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
                throw new RaiseException(getContext().getCoreLibrary().argumentError("utc_offset out of range", this));
            }

            int offset = (positive ? +1 : -1) * ((h * 3600) + m * 60 + s) * 1000;
            return timeZoneWithOffset(name, offset);
        }

        private DateTimeZone timeZoneWithOffset(String zoneName, int offset) {
            if (zoneName.isEmpty()) {
                return DateTimeZone.forOffsetMillis(offset);
            } else {
                return new FixedDateTimeZone(zoneName, null, offset, offset);
            }
        }

    }

    @RubiniusPrimitive(name = "time_utc_offset")
    public static abstract class TimeUTCOffsetPrimitiveNode extends RubiniusPrimitiveNode {

        public TimeUTCOffsetPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object timeUTCOffset(DynamicObject time) {

            Object offset = Layouts.TIME.getOffset(time);
            if (offset != nil()) {
                return offset;
            } else {
                return TimeNodes.getDateTime(time).getZone().getOffset(TimeNodes.getDateTime(time).getMillis()) / 1_000;
            }
        }

    }

}
