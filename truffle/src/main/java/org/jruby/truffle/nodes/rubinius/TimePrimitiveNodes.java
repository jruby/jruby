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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jruby.truffle.nodes.objectstorage.ReadHeadObjectFieldNode;
import org.jruby.truffle.nodes.objectstorage.WriteHeadObjectFieldNode;
import org.jruby.truffle.nodes.time.ReadTimeZoneNode;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.util.RubyDateFormatter;

/**
 * Rubinius primitives associated with the Ruby {@code Time} class.
 * <p>
 * Also see {@link RubyTime}.
 */
public abstract class TimePrimitiveNodes {

    @RubiniusPrimitive(name = "time_s_now")
    public static abstract class TimeSNowPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private ReadTimeZoneNode readTimeZoneNode;
        
        public TimeSNowPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readTimeZoneNode = new ReadTimeZoneNode(context, sourceSection);
        }

        public TimeSNowPrimitiveNode(TimeSNowPrimitiveNode prev) {
            super(prev);
            readTimeZoneNode = prev.readTimeZoneNode;
        }

        @Specialization
        public RubyTime timeSNow(VirtualFrame frame, RubyClass timeClass) {
            // TODO CS 4-Mar-15 whenever we get time we have to convert lookup and time zone to a string and look it up - need to cache somehow...
            return new RubyTime(timeClass, now(readTimeZoneNode.executeRubyString(frame)), null);
        }
        
        @CompilerDirectives.TruffleBoundary
        private DateTime now(RubyString timeZone) {
            return DateTime.now(org.jruby.RubyTime.getTimeZoneFromTZString(getContext().getRuntime(), timeZone.toString()));
        }

    }

    @RubiniusPrimitive(name = "time_s_dup", needsSelf = false)
    public static abstract class TimeSDupPrimitiveNode extends RubiniusPrimitiveNode {

        public TimeSDupPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TimeSDupPrimitiveNode(TimeSDupPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public RubyTime timeSDup(RubyTime other) {
            final RubyTime time = new RubyTime(getContext().getCoreLibrary().getTimeClass(), other.getDateTime(), other.getOffset());
            return time;
        }

    }

    @RubiniusPrimitive(name = "time_s_specific", needsSelf = false)
    public static abstract class TimeSSpecificPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private ReadTimeZoneNode readTimeZoneNode;

        public TimeSSpecificPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readTimeZoneNode = new ReadTimeZoneNode(context, sourceSection);
        }

        public TimeSSpecificPrimitiveNode(TimeSSpecificPrimitiveNode prev) {
            super(prev);
            readTimeZoneNode = prev.readTimeZoneNode;
        }

        @Specialization(guards = "isTrue(isUTC)")
        public RubyTime timeSSpecificUTC(long seconds, long nanoseconds, boolean isUTC, RubyNilClass offset) {
            return timeSSpecificUTC((int) seconds, (int) nanoseconds, isUTC, offset);
        }

        @Specialization(guards = "isTrue(isUTC)")
        public RubyTime timeSSpecificUTC(int seconds, int nanoseconds, boolean isUTC, RubyNilClass offset) {
            // TODO(CS): overflow checks needed?
            final long milliseconds = seconds * 1_000L + (nanoseconds / 1_000_000);
            return new RubyTime(getContext().getCoreLibrary().getTimeClass(), time(milliseconds), null);
        }


        @Specialization(guards = "!isTrue(isUTC)")
        public RubyTime timeSSpecific(VirtualFrame frame, long seconds, long nanoseconds, boolean isUTC, RubyNilClass offset) {
            return timeSSpecific(frame, (int) seconds, (int) nanoseconds, isUTC, offset);
        }

        @Specialization(guards = "!isTrue(isUTC)")
        public RubyTime timeSSpecific(VirtualFrame frame, int seconds, int nanoseconds, boolean isUTC, RubyNilClass offset) {
            // TODO(CS): overflow checks needed?
            final long milliseconds = (long) seconds * 1_000 + ((long) nanoseconds / 1_000_000);
            return new RubyTime(getContext().getCoreLibrary().getTimeClass(), time(milliseconds, readTimeZoneNode.executeRubyString(frame)), null);
        }

        @CompilerDirectives.TruffleBoundary
        public DateTime time(long milliseconds) {
            return new DateTime(milliseconds, DateTimeZone.UTC);
        }

        @CompilerDirectives.TruffleBoundary
        private DateTime time(long milliseconds, RubyString timeZone) {
            return new DateTime(milliseconds, org.jruby.RubyTime.getTimeZoneFromTZString(getContext().getRuntime(), timeZone.toString()));
        }

    }

    @RubiniusPrimitive(name = "time_seconds")
    public static abstract class TimeSecondsPrimitiveNode extends RubiniusPrimitiveNode {

        public TimeSecondsPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TimeSecondsPrimitiveNode(TimeSecondsPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public long timeSeconds(RubyTime time) {
            return time.getDateTime().getMillis() / 1_000;
        }

    }

    @RubiniusPrimitive(name = "time_useconds")
    public static abstract class TimeUSecondsPrimitiveNode extends RubiniusPrimitiveNode {

        public TimeUSecondsPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TimeUSecondsPrimitiveNode(TimeUSecondsPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public long timeUSeconds(RubyTime time) {
            return time.getDateTime().getMillisOfSecond() * 1_000L;
        }

    }

    @RubiniusPrimitive(name = "time_decompose")
    public static abstract class TimeDecomposePrimitiveNode extends RubiniusPrimitiveNode {

        public TimeDecomposePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TimeDecomposePrimitiveNode(TimeDecomposePrimitiveNode prev) {
            super(prev);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyArray timeDecompose(RubyTime time) {
            final DateTime dateTime = time.getDateTime();
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

            // TODO CS 14-Feb-15 uses debug send
            final String envTimeZoneString = DebugOperations.send(getContext(), getContext().getCoreLibrary().getENV(), "[]", null, getContext().makeString("TZ")).toString();
            String zoneString = org.jruby.RubyTime.zoneHelper(envTimeZoneString, dateTime, false);
            Object zone;
            if (zoneString.matches(".*-\\d+")) {
                zone = getContext().getCoreLibrary().getNilObject();
            } else {
                zone = getContext().makeString(zoneString);
            }

            final Object[] decomposed = new Object[]{sec, min, hour, day, month, year, wday, yday, isdst, zone};
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), decomposed, decomposed.length);
        }

    }

    @RubiniusPrimitive(name = "time_strftime")
    public static abstract class TimeStrftimePrimitiveNode extends RubiniusPrimitiveNode {

        public TimeStrftimePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TimeStrftimePrimitiveNode(TimeStrftimePrimitiveNode prev) {
            super(prev);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyString timeStrftime(RubyTime time, RubyString format) {
            final RubyDateFormatter rdf = getContext().getRuntime().getCurrentContext().getRubyDateFormatter();
            // TODO CS 15-Feb-15 ok to just pass nanoseconds as 0?
            return getContext().makeString(rdf.formatToByteList(rdf.compilePattern(format.getBytes(), false), time.getDateTime(), 0, null));
        }

    }

    @RubiniusPrimitive(name = "time_s_from_array", needsSelf = true)
    public static abstract class TimeSFromArrayPrimitiveNode extends RubiniusPrimitiveNode {

        public TimeSFromArrayPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TimeSFromArrayPrimitiveNode(TimeSFromArrayPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public RubyTime timeSFromArray(RubyClass timeClass, RubyNilClass sec, int min, int hour, int mday, int month, int year,
                                       RubyNilClass nsec, int isdst, boolean fromutc, Object utcoffset) {
            return timeSFromArray(timeClass, 0, min, hour, mday, month, year, nsec, isdst, fromutc, utcoffset);
        }

        @Specialization
        public RubyTime timeSFromArray(RubyClass timeClass, int sec, int min, int hour, int mday, int month, int year,
                                       RubyNilClass nsec, int isdst, boolean fromutc, Object utcoffset) {
            return timeSFromArray(timeClass, sec, min, hour, mday, month, year, 0, isdst, fromutc, utcoffset);
        }

        @Specialization
        public RubyTime timeSFromArray(RubyClass timeClass, long sec, int min, int hour, int mday, int month, int year,
                                       int nsec, int isdst, boolean fromutc, Object utcoffset) {
            // TODO CS 15-Feb-15 that cast
            return timeSFromArray(timeClass, (int) sec, min, hour, mday, month, year, nsec, isdst, fromutc, utcoffset);
        }

        @Specialization
        public RubyTime timeSFromArray(RubyClass timeClass, int sec, int min, int hour, int mday, int month, int year,
                                       long nsec, int isdst, boolean fromutc, Object utcoffset) {
            // TODO CS 15-Feb-15 that cast
            return timeSFromArray(timeClass, sec, min, hour, mday, month, year, (int) nsec, isdst, fromutc, utcoffset);
        }

        @Specialization
        public RubyTime timeSFromArray(RubyClass timeClass, int sec, int min, int hour, int mday, int month, int year,
                                       int nsec, int isdst, boolean fromutc, Object utcoffset) {
            if (sec < 0 || sec > 59 ||
                    min < 0 || min > 59 ||
                    hour < 0 || hour > 23 ||
                    mday < 1 || mday > 31 ||
                    month < 1 || month > 12) {
                throw new RaiseException(getContext().getCoreLibrary().argumentErrorOutOfRange(this));
            }

            if (isdst == -1 && !fromutc && utcoffset instanceof Integer) {
                final DateTime dateTime = new DateTime(year, month, mday, hour, min, sec, nsec / 1_000_000, DateTimeZone.forOffsetMillis(((int) utcoffset) * 1_000));
                return new RubyTime(timeClass, dateTime, utcoffset);
            } else if (isdst == -1 && !fromutc && utcoffset instanceof Long) {
                final DateTime dateTime = new DateTime(year, month, mday, hour, min, sec, nsec / 1_000_000, DateTimeZone.forOffsetMillis((int) ((long) utcoffset) * 1_000));
                return new RubyTime(timeClass, dateTime, utcoffset);
            } else if (isdst == -1 && !fromutc && utcoffset instanceof RubyBasicObject && isRational((RubyBasicObject) utcoffset)) {
                // TODO CS 15-Feb-15 debug send and cast
                final int millis = cast(DebugOperations.send(getContext(), utcoffset, "_offset_to_milliseconds", null));
                final DateTime dateTime = new DateTime(year, month, mday, hour, min, sec, nsec / 1_000_000, DateTimeZone.forOffsetMillis(millis));
                return new RubyTime(timeClass, dateTime, utcoffset);
            } else if (isdst == -1 && !fromutc && utcoffset == getContext().getCoreLibrary().getNilObject()) {
                // TODO CS 14-Feb-15 uses debug send
                final DateTimeZone zone = org.jruby.RubyTime.getTimeZoneFromTZString(getContext().getRuntime(),
                        DebugOperations.send(getContext(), getContext().getCoreLibrary().getENV(), "[]", null, getContext().makeString("TZ")).toString());
                final DateTime dateTime = new DateTime(year, month, mday, hour, min, sec, nsec / 1_000_000, zone);
                return new RubyTime(timeClass, dateTime, null);
            } else if (isdst == -1 && fromutc && utcoffset == getContext().getCoreLibrary().getNilObject()) {
                final DateTime dateTime = new DateTime(year, month, mday, hour, min, sec, nsec / 1_000_000, DateTimeZone.UTC);
                return new RubyTime(timeClass, dateTime, utcoffset);
            } else {
                throw new UnsupportedOperationException(String.format("%s %s %s %s", isdst, fromutc, utcoffset, utcoffset.getClass()));
            }
        }

        @Specialization
        public RubyTime timeSFromArray(RubyClass timeClass, RubyBasicObject sec, int min, int hour, int mday, int month, int year,
                                       RubyNilClass nsec, int isdst, boolean fromutc, Object utcoffset) {
            return null;
        }

        @Specialization
        public RubyTime timeSFromArray(RubyClass timeClass, double sec, int min, int hour, int mday, int month, int year,
                                       long nsec, int isdst, boolean fromutc, Object utcoffset) {
            return timeSFromArray(timeClass, sec, min, hour, mday, month, year, (int) nsec, isdst, fromutc, utcoffset);
        }

        @Specialization
        public RubyTime timeSFromArray(RubyClass timeClass, double sec, int min, int hour, int mday, int month, int year,
                                       int nsec, long isdst, boolean fromutc, Object utcoffset) {
            return timeSFromArray(timeClass, sec, min, hour, mday, month, year, nsec, (int) isdst, fromutc, utcoffset);
        }

        @Specialization
        public RubyTime timeSFromArray(RubyClass timeClass, double sec, int min, int hour, int mday, int month, int year,
                                       RubyNilClass nsec, int isdst, boolean fromutc, Object utcoffset) {
            final int secondsWhole = (int) sec;
            final int nanosecondsFractional = (int) ((sec * 1_000_000_000) - (secondsWhole * 1_000_000_000));
            return timeSFromArray(timeClass, secondsWhole, min, hour, mday, month, year, nanosecondsFractional, isdst, fromutc, utcoffset);
        }

        @Specialization
        public RubyTime timeSFromArray(RubyClass timeClass, double sec, int min, int hour, int mday, int month, int year,
                                       int nsec, int isdst, boolean fromutc, Object utcoffset) {
            final int secondsWhole = (int) sec;
            return timeSFromArray(timeClass, secondsWhole, min, hour, mday, month, year, nsec, isdst, fromutc, utcoffset);
        }

        private int cast(Object value) {
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

        public TimeNSecondsPrimitiveNode(TimeNSecondsPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public long timeNSeconds(RubyTime time) {
            return time.getDateTime().getMillisOfSecond() * 1_000_000L;
        }

    }

    @RubiniusPrimitive(name = "time_set_nseconds")
    public static abstract class TimeSetNSecondsPrimitiveNode extends RubiniusPrimitiveNode {

        public TimeSetNSecondsPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TimeSetNSecondsPrimitiveNode(TimeSetNSecondsPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public long timeSetNSeconds(RubyTime time, int nanoseconds) {
            time.setDateTime(time.getDateTime().withMillisOfSecond(nanoseconds / 1_000_000));
            return nanoseconds;
        }

    }

    @RubiniusPrimitive(name = "time_env_zone")
    public static abstract class TimeEnvZonePrimitiveNode extends RubiniusPrimitiveNode {

        public TimeEnvZonePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TimeEnvZonePrimitiveNode(TimeEnvZonePrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public Object timeEnvZone(RubyTime time) {
            throw new UnsupportedOperationException("time_env_zone");
        }

    }

    @RubiniusPrimitive(name = "time_utc_offset")
    public static abstract class TimeUTCOffsetPrimitiveNode extends RubiniusPrimitiveNode {

        public TimeUTCOffsetPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TimeUTCOffsetPrimitiveNode(TimeUTCOffsetPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public Object timeUTCOffset(RubyTime time) {
            if (time.getOffset() != null) {
                return time.getOffset();
            } else {
                return time.getDateTime().getZone().getOffset(time.getDateTime().getMillis()) / 1_000;
            }
        }

    }

}
