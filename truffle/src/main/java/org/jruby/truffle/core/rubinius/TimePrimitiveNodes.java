/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 *  * Version: EPL 1.0/GPL 2.0/LGPL 2.1
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
package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.ExactMath;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.tz.FixedDateTimeZone;
import org.jruby.runtime.Helpers;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.RubiniusPrimitive;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.core.time.ReadTimeZoneNode;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.SnippetNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.AllocateObjectNodeGen;
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
    public static abstract class TimeSNowPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

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
            return allocateObjectNode.allocate(timeClass, now((DynamicObject) readTimeZoneNode.execute(frame)), 0, nil(), nil(), false, false);
        }

        @TruffleBoundary
        private DateTime now(DynamicObject timeZone) {
            assert RubyGuards.isRubyString(timeZone);
            return DateTime.now(TimeZoneParser.parse(this, StringOperations.getString(getContext(), timeZone)));
        }

    }

    @RubiniusPrimitive(name = "time_s_specific", needsSelf = false, lowerFixnumParameters = { 1 })
    public static abstract class TimeSSpecificPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Child private ReadTimeZoneNode readTimeZoneNode;

        public TimeSSpecificPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readTimeZoneNode = new ReadTimeZoneNode(context, sourceSection);
        }

        @Specialization(guards = { "isUTC" })
        public DynamicObject timeSSpecificUTC(long seconds, int nanoseconds, boolean isUTC, Object offset) {
            final long milliseconds = getMillis(seconds, nanoseconds);
            return Layouts.TIME.createTime(coreLibrary().getTimeFactory(), utcTime(milliseconds), nanoseconds % 1_000_000, nil(), nil(), false, isUTC);
        }

        @Specialization(guards = { "!isUTC", "isNil(offset)" })
        public DynamicObject timeSSpecific(VirtualFrame frame, long seconds, int nanoseconds, boolean isUTC, Object offset) {
            final long milliseconds = getMillis(seconds, nanoseconds);
            return Layouts.TIME.createTime(coreLibrary().getTimeFactory(),
                    localtime(milliseconds, (DynamicObject) readTimeZoneNode.execute(frame)), nanoseconds % 1_000_000, nil(), offset, false, isUTC);
        }

        @Specialization(guards = { "!isUTC" })
        public DynamicObject timeSSpecific(VirtualFrame frame, long seconds, int nanoseconds, boolean isUTC, long offset) {
            final long milliseconds = getMillis(seconds, nanoseconds);
            return Layouts.TIME.createTime(coreLibrary().getTimeFactory(),
                    offsetTime(milliseconds, offset), nanoseconds % 1_000_000, nil(), nil(), false, isUTC);
        }

        private long getMillis(long seconds, int nanoseconds) {
            try {
                return ExactMath.addExact(ExactMath.multiplyExact(seconds, 1000L), (nanoseconds / 1_000_000));
            } catch (ArithmeticException e) {
                CompilerDirectives.transferToInterpreter();
                String message = String.format("UNIX epoch + %d seconds out of range for Time (Joda-Time limitation)", seconds);
                throw new RaiseException(coreExceptions().rangeError(message, this));
            }
        }

        @TruffleBoundary
        private DateTime utcTime(long milliseconds) {
            return new DateTime(milliseconds, DateTimeZone.UTC);
        }

        @TruffleBoundary
        private DateTime offsetTime(long milliseconds, long offset) {

            return new DateTime(milliseconds, DateTimeZone.forOffsetMillis((int) offset * 1000));
        }

        @TruffleBoundary
        private DateTime localtime(long milliseconds, DynamicObject zoneName) {
            assert RubyGuards.isRubyString(zoneName);
            return new DateTime(milliseconds, TimeZoneParser.parse(this, StringOperations.getString(getContext(), zoneName)));
        }

    }

    @RubiniusPrimitive(name = "time_seconds")
    public static abstract class TimeSecondsPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Specialization
        public long timeSeconds(DynamicObject time) {
            return Layouts.TIME.getDateTime(time).getMillis() / 1_000;
        }

    }

    @RubiniusPrimitive(name = "time_useconds")
    public static abstract class TimeUSecondsPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public long timeUSeconds(DynamicObject time) {
            return Layouts.TIME.getDateTime(time).getMillisOfSecond() * 1_000L + (Layouts.TIME.getNSec(time) / 1_000L);
        }

    }

    @RubiniusPrimitive(name = "time_decompose")
    public static abstract class TimeDecomposePrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Child private ReadTimeZoneNode readTimeZoneNode;

        public TimeDecomposePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readTimeZoneNode = new ReadTimeZoneNode(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject timeDecompose(DynamicObject time) {
            final DateTime dateTime = Layouts.TIME.getDateTime(time);
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
            final boolean isdst = !dateTime.getZone().isStandardOffset(dateTime.getMillis());

            final Object zone;
            if (Layouts.TIME.getRelativeOffset(time)) {
                zone = nil();
            } else {
                final Object timeZone = Layouts.TIME.getZone(time);
                if (timeZone == nil()) {
                    final String zoneString = TimeZoneParser.getShortZoneName(dateTime, dateTime.getZone());
                    zone = createString(StringOperations.encodeRope(zoneString, UTF8Encoding.INSTANCE));
                } else {
                    zone = timeZone;
                }
            }

            final Object[] decomposed = new Object[]{ sec, min, hour, day, month, year, wday, yday, isdst, zone };
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), decomposed, decomposed.length);
        }

    }

    @RubiniusPrimitive(name = "time_strftime")
    public static abstract class TimeStrftimePrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyString(format)")
        public DynamicObject timeStrftime(DynamicObject time, DynamicObject format) {
            final RubyDateFormatter rdf = getContext().getJRubyRuntime().getCurrentContext().getRubyDateFormatter();
            return createString(rdf.formatToByteList(rdf.compilePattern(StringOperations.getByteListReadOnly(format), false),
                    Layouts.TIME.getDateTime(time), Layouts.TIME.getNSec(time), null));
        }

    }

    @RubiniusPrimitive(name = "time_s_from_array", needsSelf = true, lowerFixnumParameters = { 0 /*sec*/, 1 /* min */, 2 /* hour */, 3 /* mday */, 4 /* month */, 5 /* year */, 6 /*nsec*/, 7 /*isdst*/ })
    public static abstract class TimeSFromArrayPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Child ReadTimeZoneNode readTimeZoneNode;
        @Child AllocateObjectNode allocateObjectNode;

        public TimeSFromArrayPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readTimeZoneNode = new ReadTimeZoneNode(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization(guards = {"!fromutc", "!isNil(utcoffset)"})
        public DynamicObject timeSFromArray(VirtualFrame frame, DynamicObject timeClass, int sec, int min, int hour, int mday, int month, int year,
                                            int nsec, int isdst, boolean fromutc, DynamicObject utcoffset,
                                            @Cached("new()") SnippetNode snippetNode) {

            DynamicObject envZon = null;
            if (!fromutc && utcoffset == nil()) {
                envZon = (DynamicObject) readTimeZoneNode.execute(frame);
            }

            final int millis = cast(snippetNode.execute(frame, "(offset * 1000).to_i", "offset", utcoffset));

            return buildTime(timeClass, sec, min, hour, mday, month, year, nsec, isdst, fromutc, utcoffset, envZon, millis);
        }

        @Specialization(guards = "(fromutc || !isDynamicObject(utcoffset)) || isNil(utcoffset)")
        public DynamicObject timeSFromArray(VirtualFrame frame, DynamicObject timeClass, int sec, int min, int hour, int mday, int month, int year,
                                            int nsec, int isdst, boolean fromutc, Object utcoffset) {

            DynamicObject envZon = null;
            if (!fromutc && utcoffset == nil()) {
                envZon = (DynamicObject) readTimeZoneNode.execute(frame);
            }
            return buildTime(timeClass, sec, min, hour, mday, month, year, nsec, isdst, fromutc, utcoffset, envZon, -1);
        }

        @Specialization(guards = "!isInteger(sec) || !isInteger(nsec)")
        public DynamicObject timeSFromArrayFallback(VirtualFrame frame, DynamicObject timeClass, Object sec, int min, int hour, int mday, int month, int year,
                                                    Object nsec, int isdst, boolean fromutc, Object utcoffset) {
            return null; // Primitive failure
        }

        @TruffleBoundary
        private DynamicObject buildTime(DynamicObject timeClass, int sec, int min, int hour, int mday, int month, int year,
                                        int nsec, int isdst, boolean fromutc, Object utcoffset, DynamicObject envZon, int millis) {
            if (sec < 0 || sec > 59 ||
                    min < 0 || min > 59 ||
                    hour < 0 || hour > 23 ||
                    mday < 1 || mday > 31 ||
                    month < 1 || month > 12) {
                throw new RaiseException(coreExceptions().argumentErrorOutOfRange(this));
            }

            DateTime dt = new DateTime(year, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);

            dt = dt.plusMonths(month - 1)
                    .plusDays(mday - 1)
                    .plusHours(hour)
                    .plusMinutes(min)
                    .plusSeconds(sec)
                    .plusMillis(nsec / 1_000_000);

            final DateTimeZone zone;
            final boolean relativeOffset;
            DynamicObject zoneToStore;
            if (fromutc) {
                zone = DateTimeZone.UTC;
                relativeOffset = false;
                zoneToStore = nil();
            } else if (utcoffset == nil()) {
                zone = TimeZoneParser.parse(this, StringOperations.getString(getContext(), envZon));
                // TODO BJF 16-Feb-2016 verify which zone the following date time should be in
                final String zoneName = TimeZoneParser.getShortZoneName(dt.withZoneRetainFields(zone), zone);
                zoneToStore = createString(StringOperations.encodeRope(zoneName, UTF8Encoding.INSTANCE));
                relativeOffset = false;
            } else if (utcoffset instanceof Integer) {
                zone = DateTimeZone.forOffsetMillis(((int) utcoffset) * 1_000);
                relativeOffset = true;
                zoneToStore = nil();
            } else if (utcoffset instanceof Long) {
                zone = DateTimeZone.forOffsetMillis((int) ((long) utcoffset) * 1_000);
                relativeOffset = true;
                zoneToStore = nil();
            } else if (utcoffset instanceof DynamicObject) {
                zone = DateTimeZone.forOffsetMillis(millis);
                relativeOffset = true;
                zoneToStore = nil();
            } else {
                throw new UnsupportedOperationException(String.format("%s %s %s %s", isdst, fromutc, utcoffset, utcoffset.getClass()));
            }

            dt = dt.withZoneRetainFields(zone);

            if (isdst == 0) {
                dt = dt.withLaterOffsetAtOverlap();
            }

            if (isdst == 1) {
                dt = dt.withEarlierOffsetAtOverlap();
            }

            return allocateObjectNode.allocate(timeClass, dt, nsec % 1_000_000, zoneToStore, utcoffset, relativeOffset, fromutc);
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
    public static abstract class TimeNSecondsPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public long timeNSeconds(DynamicObject time) {
            return (Layouts.TIME.getDateTime(time).getMillisOfSecond() % 1000) * 1_000_000L + Layouts.TIME.getNSec(time);
        }

    }

    @RubiniusPrimitive(name = "time_set_nseconds", lowerFixnumParameters = 0)
    public static abstract class TimeSetNSecondsPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public long timeSetNSeconds(DynamicObject time, int nanoseconds) {
            Layouts.TIME.setDateTime(time, Layouts.TIME.getDateTime(time).withMillisOfSecond(nanoseconds / 1_000_000));
            Layouts.TIME.setNSec(time, nanoseconds % 1_000_000);
            return nanoseconds;
        }

    }

    @RubiniusPrimitive(name = "time_utc_offset")
    public static abstract class TimeUTCOffsetPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public Object timeUTCOffset(DynamicObject time) {
            return Layouts.TIME.getDateTime(time).getZone().getOffset(Layouts.TIME.getDateTime(time).getMillis()) / 1_000;
        }

    }

    public static class TimeZoneParser {
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

        public static String getShortZoneName(DynamicObject time, DateTimeZone zone) {
            DateTime dateTime = Layouts.TIME.getDateTime(time);
            return getShortZoneName(dateTime, zone);
        }

        @TruffleBoundary
        public static String getShortZoneName(DateTime dateTime, DateTimeZone zone) {
            return zone.getShortName(dateTime.getMillis());
        }

        @TruffleBoundary
        public static DateTimeZone parse(RubyNode node, String zone) {
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
                return getTimeZoneFromHHMM(node, zoneName, sign.equals("-"), hours, minutes, seconds);
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

        private static DateTimeZone getTimeZoneFromHHMM(RubyNode node,
                                                        String name,
                                                        boolean positive,
                                                        String hours,
                                                        String minutes,
                                                        String seconds) {
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
                throw new RaiseException(node.getContext().getCoreExceptions().argumentError("utc_offset out of range", node));
            }

            int offset = (positive ? +1 : -1) * ((h * 3600) + m * 60 + s) * 1000;
            return timeZoneWithOffset(name, offset);
        }

        private static DateTimeZone timeZoneWithOffset(String zoneName, int offset) {
            if (zoneName.isEmpty()) {
                return DateTimeZone.forOffsetMillis(offset);
            } else {
                return new FixedDateTimeZone(zoneName, null, offset, offset);
            }
        }

    }

}
