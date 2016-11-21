/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.time;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.CoreMethodNode;
import org.jruby.truffle.builtins.NonStandard;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.core.time.RubyDateFormatter.Token;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.SnippetNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.util.StringUtils;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CoreClass("Time")
public abstract class TimeNodes {

    private static final ZonedDateTime ZERO = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
    private static final ZoneId UTC = ZoneId.of("UTC");

    // We need it to copy the internal data for a call to Kernel#clone.
    @CoreMethod(names = "initialize_copy", required = 1)
    public abstract static class InitializeCopyNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyTime(from)")
        public Object initializeCopy(DynamicObject self, DynamicObject from) {
            Layouts.TIME.setDateTime(self, Layouts.TIME.getDateTime(from));
            Layouts.TIME.setOffset(self, Layouts.TIME.getOffset(from));
            Layouts.TIME.setRelativeOffset(self, Layouts.TIME.getRelativeOffset(from));
            return self;
        }

    }

    // Not a core method, used to simulate Rubinius @is_gmt.
    @NodeChild(type = RubyNode.class, value = "self")
    public abstract static class InternalGMTNode extends CoreMethodNode {

        @Specialization
        public boolean internalGMT(DynamicObject time) {
            return Layouts.TIME.getIsUtc(time);
        }
    }

    // Not a core method, used to simulate Rubinius @offset.
    @NodeChild(type = RubyNode.class, value = "self")
    public abstract static class InternalOffsetNode extends CoreMethodNode {

        @TruffleBoundary
        @Specialization
        public Object internalOffset(DynamicObject time) {
            final Object offset = Layouts.TIME.getOffset(time);
            if (offset == nil()) {
                final ZonedDateTime dateTime = Layouts.TIME.getDateTime(time);
                return dateTime.getOffset().getTotalSeconds();
            } else {
                return offset;
            }
        }
    }

    @CoreMethod(names = "localtime_internal", optional = 1)
    public abstract static class LocalTimeNode extends CoreMethodArrayArgumentsNode {

        @Child private GetTimeZoneNode getTimeZoneNode;

        public LocalTimeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            getTimeZoneNode = GetTimeZoneNodeGen.create();
        }

        @Specialization
        public DynamicObject localtime(VirtualFrame frame, DynamicObject time, NotProvided offset) {
            final TimeZoneAndName timeZoneAndName = getTimeZoneNode.executeGetTimeZone(frame);
            final ZoneId dateTimeZone = timeZoneAndName.getZone();
            final String shortZoneName = TimeZoneParser.getShortZoneName(time, dateTimeZone);
            final DynamicObject zone = createString(StringOperations.encodeRope(shortZoneName, UTF8Encoding.INSTANCE));
            final ZonedDateTime dateTime = Layouts.TIME.getDateTime(time);

            Layouts.TIME.setIsUtc(time, false);
            Layouts.TIME.setRelativeOffset(time, false);
            Layouts.TIME.setZone(time, zone);
            Layouts.TIME.setDateTime(time, withZone(dateTime, dateTimeZone));

            return time;
        }

        @Specialization
        public DynamicObject localtime(DynamicObject time, long offset) {
            final ZonedDateTime dateTime = Layouts.TIME.getDateTime(time);
            final ZoneId zone = getDateTimeZone((int) offset);

            Layouts.TIME.setIsUtc(time, false);
            Layouts.TIME.setRelativeOffset(time, true);
            Layouts.TIME.setZone(time, nil());
            Layouts.TIME.setDateTime(time, withZone(dateTime, zone));

            return time;
        }

        @TruffleBoundary
        public ZoneId getDateTimeZone(int offset) {
            try {
                return ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds(offset));
            } catch (DateTimeException e) {
                throw new RaiseException(getContext().getCoreExceptions().argumentError(e.getMessage(), this));
            }
        }

        @TruffleBoundary
        private ZonedDateTime withZone(ZonedDateTime dateTime, ZoneId zone) {
            return dateTime.withZoneSameInstant(zone);
        }

    }

    @CoreMethod(names = "add_internal!", required = 2, visibility = Visibility.PROTECTED)
    public abstract static class AddInternalNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject addInternal(DynamicObject time, long seconds, long nanoSeconds) {
            final ZonedDateTime dateTime = Layouts.TIME.getDateTime(time);
            Layouts.TIME.setDateTime(time, dateTime.plusSeconds(seconds).plusNanos(nanoSeconds));
            return time;
        }
    }

    @CoreMethod(names = "dup_internal", required = 1, visibility = Visibility.PROTECTED)
    public static abstract class DupInternalNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode;

        public DupInternalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNode.create();
        }

        @Specialization
        public DynamicObject dup(DynamicObject time, DynamicObject klass) {
            return allocateObjectNode.allocate(klass, Layouts.TIME.build(
                            Layouts.TIME.getDateTime(time),
                            Layouts.TIME.getZone(time),
                            Layouts.TIME.getOffset(time),
                            Layouts.TIME.getRelativeOffset(time),
                            Layouts.TIME.getIsUtc(time)));
        }
    }

    @CoreMethod(names = "gmtime")
    public abstract static class GmTimeNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject localtime(DynamicObject time) {
            final ZonedDateTime dateTime = Layouts.TIME.getDateTime(time);

            Layouts.TIME.setIsUtc(time, true);
            Layouts.TIME.setRelativeOffset(time, false);
            Layouts.TIME.setZone(time, create7BitString(UTC.getDisplayName(TextStyle.NARROW, Locale.ENGLISH), USASCIIEncoding.INSTANCE));
            Layouts.TIME.setDateTime(time, inUTC(dateTime));

            return time;
        }

        @TruffleBoundary
        private ZonedDateTime inUTC(final ZonedDateTime dateTime) {
            return dateTime.withZoneSameInstant(UTC);
        }

    }

    @CoreMethod(names = "gmt?")
    public abstract static class GmtNode extends CoreMethodArrayArgumentsNode {

        @Child private InternalGMTNode internalGMTNode;

        public GmtNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            internalGMTNode = TimeNodesFactory.InternalGMTNodeFactory.create(null);
        }

        @Specialization
        public boolean allocate(DynamicObject time) {
            return internalGMTNode.internalGMT(time);
        }

    }

    @NonStandard
    @CoreMethod(names = "internal_offset")
    public abstract static class InternalOffsetCoreNode extends CoreMethodArrayArgumentsNode {

        @Child private InternalOffsetNode internalOffsetNode;

        public InternalOffsetCoreNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            internalOffsetNode = TimeNodesFactory.InternalOffsetNodeFactory.create(null);
        }

        @Specialization
        public Object allocate(DynamicObject time) {
            return internalOffsetNode.internalOffset(time);
        }

    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode;

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNode.create();
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateObjectNode.allocate(rubyClass, Layouts.TIME.build(ZERO, coreLibrary().getNilObject(), 0, false, false));
        }

    }

    @Primitive(name = "time_s_now")
    public static abstract class TimeSNowPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode;
        @Child private GetTimeZoneNode getTimeZoneNode;

        public TimeSNowPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNode.create();
            getTimeZoneNode = GetTimeZoneNodeGen.create();
        }

        @Specialization
        public DynamicObject timeSNow(VirtualFrame frame, DynamicObject timeClass) {
            final TimeZoneAndName zoneName = getTimeZoneNode.executeGetTimeZone(frame);
            return allocateObjectNode.allocate(timeClass, Layouts.TIME.build(now(zoneName.getZone()), nil(), nil(), false, false));
        }

        @TruffleBoundary
        private ZonedDateTime now(ZoneId timeZone) {
            return ZonedDateTime.now(timeZone);
        }

    }

    @Primitive(name = "time_s_specific", lowerFixnum = 2)
    public static abstract class TimeSSpecificPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private GetTimeZoneNode getTimeZoneNode;
        @Child private AllocateObjectNode allocateObjectNode;

        public TimeSSpecificPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            getTimeZoneNode = GetTimeZoneNodeGen.create();
            allocateObjectNode = AllocateObjectNode.create();
        }

        @Specialization(guards = { "isUTC" })
        public DynamicObject timeSSpecificUTC(DynamicObject timeClass, long seconds, int nanoseconds, boolean isUTC, Object offset) {
            return allocateObjectNode.allocate(timeClass, Layouts.TIME.build(getDateTime(seconds, nanoseconds, UTC), nil(), nil(), false, isUTC));
        }

        @Specialization(guards = { "!isUTC", "isNil(offset)" })
        public DynamicObject timeSSpecific(VirtualFrame frame, DynamicObject timeClass, long seconds, int nanoseconds, boolean isUTC, Object offset) {
            final TimeZoneAndName zoneName = getTimeZoneNode.executeGetTimeZone(frame);
            return allocateObjectNode.allocate(timeClass, Layouts.TIME.build(
                            getDateTime(seconds, nanoseconds, zoneName.getZone()),
                            nil(), offset, false, isUTC));
        }

        @Specialization(guards = { "!isUTC" })
        public DynamicObject timeSSpecific(VirtualFrame frame, DynamicObject timeClass, long seconds, int nanoseconds, boolean isUTC, long offset) {
            ZoneId timeZone = ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds((int) offset));
            return allocateObjectNode.allocate(timeClass, Layouts.TIME.build(
                            getDateTime(seconds, nanoseconds, timeZone), nil(), nil(), false, isUTC));
        }


        @TruffleBoundary
        private ZonedDateTime getDateTime(long seconds, int nanoseconds, ZoneId timeZone) {
            try {
                return ZonedDateTime.ofInstant(Instant.ofEpochSecond(seconds, nanoseconds), timeZone);
            } catch (DateTimeException e) {
                String message = StringUtils.format("UNIX epoch + %d seconds out of range for Time (java.time limitation)", seconds);
                throw new RaiseException(coreExceptions().rangeError(message, this));
            }
        }

    }

    @Primitive(name = "time_seconds")
    public static abstract class TimeSecondsPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public long timeSeconds(DynamicObject time) {
            return Layouts.TIME.getDateTime(time).toInstant().getEpochSecond();
        }

    }

    @Primitive(name = "time_useconds")
    public static abstract class TimeUSecondsPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public long timeUSeconds(DynamicObject time) {
            return Layouts.TIME.getDateTime(time).getNano() / 1000;
        }

    }

    @Primitive(name = "time_decompose")
    public static abstract class TimeDecomposePrimitiveNode extends PrimitiveArrayArgumentsNode {

        public TimeDecomposePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject timeDecompose(DynamicObject time) {
            final ZonedDateTime dateTime = Layouts.TIME.getDateTime(time);
            final int sec = dateTime.getSecond();
            final int min = dateTime.getMinute();
            final int hour = dateTime.getHour();
            final int day = dateTime.getDayOfMonth();
            final int month = dateTime.getMonthValue();
            final int year = dateTime.getYear();

            int wday = dateTime.getDayOfWeek().getValue();

            if (wday == 7) {
                wday = 0;
            }

            final int yday = dateTime.getDayOfYear();
            final boolean isdst = dateTime.getZone().getRules().isDaylightSavings(dateTime.toInstant());

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
            return createArray(decomposed, decomposed.length);
        }

    }

    @Primitive(name = "time_strftime")
    public static abstract class TimeStrftimePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyString(format)")
        public DynamicObject timeStrftime(DynamicObject time, DynamicObject format) {
            final RubyDateFormatter rdf = new RubyDateFormatter(getContext(), this);
            final List<Token> pattern = rdf.compilePattern(StringOperations.getByteListReadOnly(format), false);
            return createString(rdf.formatToByteList(pattern, Layouts.TIME.getDateTime(time)));
        }

    }

    @Primitive(name = "time_s_from_array", needsSelf = true, lowerFixnum = { 1 /*sec*/, 2 /* min */, 3 /* hour */, 4 /* mday */, 5 /* month */, 6 /* year */, 7 /*nsec*/, 8 /*isdst*/})
    public static abstract class TimeSFromArrayPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child GetTimeZoneNode getTimeZoneNode;
        @Child AllocateObjectNode allocateObjectNode;

        public TimeSFromArrayPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            getTimeZoneNode = GetTimeZoneNodeGen.create();
            allocateObjectNode = AllocateObjectNode.create();
        }

        @Specialization(guards = {"!fromutc", "!isNil(utcoffset)"})
        public DynamicObject timeSFromArray(VirtualFrame frame, DynamicObject timeClass, int sec, int min, int hour, int mday, int month, int year,
                                            int nsec, int isdst, boolean fromutc, DynamicObject utcoffset,
                                            @Cached("new()") SnippetNode snippetNode) {

            final TimeZoneAndName zoneAndName;
            if (!fromutc && utcoffset == nil()) {
                zoneAndName = getTimeZoneNode.executeGetTimeZone(frame);
            } else {
                zoneAndName = null;
            }

            final int millis = cast(snippetNode.execute(frame, "(offset * 1000).to_i", "offset", utcoffset));

            return buildTime(timeClass, sec, min, hour, mday, month, year, nsec, isdst, fromutc, utcoffset, zoneAndName, millis);
        }

        @Specialization(guards = "(fromutc || !isDynamicObject(utcoffset)) || isNil(utcoffset)")
        public DynamicObject timeSFromArray(VirtualFrame frame, DynamicObject timeClass, int sec, int min, int hour, int mday, int month, int year,
                                            int nsec, int isdst, boolean fromutc, Object utcoffset) {

            final TimeZoneAndName zoneAndName;
            if (!fromutc && utcoffset == nil()) {
                zoneAndName = getTimeZoneNode.executeGetTimeZone(frame);
            } else {
                zoneAndName = null;
            }
            return buildTime(timeClass, sec, min, hour, mday, month, year, nsec, isdst, fromutc, utcoffset, zoneAndName, -1);
        }

        @Specialization(guards = "!isInteger(sec) || !isInteger(nsec)")
        public DynamicObject timeSFromArrayFallback(VirtualFrame frame, DynamicObject timeClass, Object sec, int min, int hour, int mday, int month, int year,
                                                    Object nsec, int isdst, boolean fromutc, Object utcoffset) {
            return null; // Primitive failure
        }

        @TruffleBoundary
        private DynamicObject buildTime(DynamicObject timeClass, int sec, int min, int hour, int mday, int month, int year,
                int nsec, int isdst, boolean fromutc, Object utcoffset, TimeZoneAndName envZone, int zoneOffsetMillis) {
            if (sec < 0 || sec > 59 ||
                    min < 0 || min > 59 ||
                    hour < 0 || hour > 23 ||
                    mday < 1 || mday > 31 ||
                    month < 1 || month > 12) {
                throw new RaiseException(coreExceptions().argumentErrorOutOfRange(this));
            }

            ZonedDateTime dt = ZonedDateTime.of(year, 1, 1, 0, 0, 0, 0, UTC);

            dt = dt.plusMonths(month - 1)
                    .plusDays(mday - 1)
                    .plusHours(hour)
                    .plusMinutes(min)
                    .plusSeconds(sec)
                    .plusNanos(nsec);

            final ZoneId zone;
            final boolean relativeOffset;
            DynamicObject zoneToStore;

            try {
                if (fromutc) {
                    zone = UTC;
                    relativeOffset = false;
                    zoneToStore = nil();
                } else if (utcoffset == nil()) {
                    zone = envZone.getZone();
                    // TODO BJF 16-Feb-2016 verify which zone the following date time should be in
                    // final String zoneName = TimeZoneParser.getShortZoneName(dt.withZoneSameInstant(zone), zone);
                    zoneToStore = envZone.getNameAsRubyObject(getContext()); // createString(StringOperations.encodeRope(zoneName, UTF8Encoding.INSTANCE));
                    relativeOffset = false;
                } else if (utcoffset instanceof Integer) {
                    zone = ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds((int) utcoffset));
                    relativeOffset = true;
                    zoneToStore = nil();
                } else if (utcoffset instanceof Long) {
                    zone = ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds((int) (long) utcoffset));
                    relativeOffset = true;
                    zoneToStore = nil();
                } else if (utcoffset instanceof DynamicObject) {
                    zone = ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds(zoneOffsetMillis / 1_000));
                    relativeOffset = true;
                    zoneToStore = nil();
                } else {
                    throw new UnsupportedOperationException(StringUtils.format("%s %s %s %s", isdst, fromutc, utcoffset, utcoffset.getClass()));
                }
            } catch (DateTimeException e) {
                throw new RaiseException(coreExceptions().argumentError(e.getMessage(), this));
            }

            dt = dt.withZoneSameLocal(zone);

            if (isdst == 0) {
                dt = dt.withLaterOffsetAtOverlap();
            }

            if (isdst == 1) {
                dt = dt.withEarlierOffsetAtOverlap();
            }

            return allocateObjectNode.allocate(timeClass, Layouts.TIME.build(dt, zoneToStore, utcoffset, relativeOffset, fromutc));
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

    @Primitive(name = "time_nseconds")
    public static abstract class TimeNSecondsPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public long timeNSeconds(DynamicObject time) {
            return Layouts.TIME.getDateTime(time).getNano();
        }

    }

    @Primitive(name = "time_set_nseconds", lowerFixnum = 1)
    public static abstract class TimeSetNSecondsPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public long timeSetNSeconds(DynamicObject time, int nanoseconds) {
            final ZonedDateTime dateTime = Layouts.TIME.getDateTime(time);
            Layouts.TIME.setDateTime(time, dateTime.plusNanos(nanoseconds - dateTime.getNano()));
            return nanoseconds;
        }

    }

    @Primitive(name = "time_utc_offset")
    public static abstract class TimeUTCOffsetPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public Object timeUTCOffset(DynamicObject time) {
            return Layouts.TIME.getDateTime(time).getOffset().getTotalSeconds();
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

        public static String getShortZoneName(DynamicObject time, ZoneId zone) {
            ZonedDateTime dateTime = Layouts.TIME.getDateTime(time);
            return getShortZoneName(dateTime, zone);
        }

        @TruffleBoundary
        public static String getShortZoneName(ZonedDateTime dateTime, ZoneId zone) {
            String name = zone.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            // Joda used to let us get the time zone at a given instance, which gave use EST rather than ET

            // This solution is a bit of a joke, I know

            final boolean summer = zone.getRules().isDaylightSavings(dateTime.toInstant());

            switch (name) {
                case "AT":
                    if (summer) {
                        name = "ADT";
                    } else {
                        name = "AST";
                    }
                    break;

                case "ET":
                    if (summer) {
                        name = "EDT";
                    } else {
                        name = "EST";
                    }
                    break;

                case "CT":
                    if (summer) {
                        name = "CDT";
                    } else {
                        name = "CST";
                    }
                    break;

                case "CET":
                    if (summer) {
                        name = "CEST";
                    }
                    break;
            }

            return name;
        }

        @TruffleBoundary(throwsControlFlowException = true)
        public static TimeZoneAndName parse(RubyNode node, String zoneString) {
            String zone = zoneString;
            String upZone = zone.toUpperCase(Locale.ENGLISH);

            Matcher tzMatcher = TZ_PATTERN.matcher(zone);
            if (tzMatcher.matches()) {
                String name = tzMatcher.group(1);
                String sign = tzMatcher.group(2);
                String hours = tzMatcher.group(3);
                String minutes = tzMatcher.group(4);
                String seconds = tzMatcher.group(5);

                if (name == null) {
                    name = "";
                }

                // Sign is reversed in legacy TZ notation
                return getTimeZoneFromHHMM(node, name, sign.equals("-"), hours, minutes, seconds);
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
                    return new TimeZoneAndName(ZoneId.of(zone), null);
                } catch (IllegalArgumentException e) {
                    return new TimeZoneAndName(UTC, null);
                }
            }
        }

        private static TimeZoneAndName getTimeZoneFromHHMM(RubyNode node,
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
            return timeZoneWithOffset(node, name, offset);
        }

        private static TimeZoneAndName timeZoneWithOffset(RubyNode node, String zoneName, int offset) {
            final ZoneId zone;

            try {
                zone = ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds(offset / 1000));
            } catch (DateTimeException e) {
                throw new RaiseException(node.getContext().getCoreExceptions().argumentError(e.getMessage(), node));
            }

            if (zoneName.isEmpty()) {
                return new TimeZoneAndName(zone, null);
            } else {
                return new TimeZoneAndName(zone, zoneName);
            }
        }

    }

}
