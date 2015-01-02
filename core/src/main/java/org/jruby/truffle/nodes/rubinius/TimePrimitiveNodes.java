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
import com.oracle.truffle.api.source.SourceSection;
import org.joda.time.DateTime;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.*;
import org.jruby.util.RubyDateFormatter;

public abstract class TimePrimitiveNodes {

    @RubiniusPrimitive(name = "time_s_now")
    public static abstract class TimeSNowPrimitiveNode extends RubiniusPrimitiveNode {

        public TimeSNowPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TimeSNowPrimitiveNode(TimeSNowPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public RubyTime timeSNow(RubyClass timeClass) {
            return new RubyTime(timeClass, System.currentTimeMillis());
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
            return new RubyTime(getContext().getCoreLibrary().getTimeClass(), other.getSeconds(), other.getNanoseconds());
        }

    }

    @RubiniusPrimitive(name = "time_s_specific", needsSelf = false)
    public static abstract class TimeSSpecificPrimitiveNode extends RubiniusPrimitiveNode {

        public TimeSSpecificPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TimeSSpecificPrimitiveNode(TimeSSpecificPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public RubyTime timeSSpecific(int seconds, int nanoseconds, Object fromGMT, Object offset) {
            return timeSSpecific((long) seconds, (long) nanoseconds, fromGMT, offset);
        }

        @Specialization
        public RubyTime timeSSpecific(long seconds, int nanoseconds, Object fromGMT, Object offset) {
            return timeSSpecific((long) seconds, (long) nanoseconds, fromGMT, offset);
        }

        @Specialization
        public RubyTime timeSSpecific(int seconds, long nanoseconds, Object fromGMT, Object offset) {
            return timeSSpecific((long) seconds, (long) nanoseconds, fromGMT, offset);
        }

        @Specialization
        public RubyTime timeSSpecific(long seconds, long nanoseconds, Object fromGMT, Object offset) {
            return new RubyTime(getContext().getCoreLibrary().getTimeClass(), seconds, nanoseconds, fromGMT, offset);
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
            return time.getSeconds();
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
            return time.getNanoseconds();
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

        @Specialization
        public RubyArray timeDecompose(RubyTime time) {
            final DateTime dateTime = TimeOperations.secondsAndNanosecondsToDateTime(time.getSeconds(), time.getNanoseconds());

            final int sec = dateTime.getSecondOfMinute();
            final int min = dateTime.getMinuteOfDay();
            final int hour = dateTime.getHourOfDay();
            final int day = dateTime.getDayOfMonth();
            final int month = dateTime.getMonthOfYear();
            final int year = dateTime.getYear();
            final int wday = dateTime.getDayOfWeek();
            final int yday = dateTime.getDayOfYear();
            final Object isdst = getContext().getCoreLibrary().getNilObject();
            final Object zone = getContext().getCoreLibrary().getNilObject();

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
            // TODO: converts everything to JRuby objects and back - should find a more direct way
            final DateTime dateTime = TimeOperations.secondsAndNanosecondsToDateTime(time.getSeconds(), time.getNanoseconds());
            final RubyDateFormatter rdf = getContext().getRuntime().getCurrentContext().getRubyDateFormatter();
            return getContext().makeString(rdf.compileAndFormat(getContext().toJRuby(format), false, dateTime, time.getNanoseconds(), null).getByteList());
        }

    }

    @RubiniusPrimitive(name = "time_s_from_array", needsSelf = false)
    public static abstract class TimeSFromArrayPrimitiveNode extends RubiniusPrimitiveNode {

        public TimeSFromArrayPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TimeSFromArrayPrimitiveNode(TimeSFromArrayPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public RubyTime timeSFromArray(Object sec, Object min, Object hour, Object mday, Object month, Object year,
                                       Object nsec, Object isdst, Object fromgmt, Object utcoffset) {
            throw new UnsupportedOperationException("time_s_from_array");
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
            return time.getNanoseconds();
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
        public long timeSetNSeconds(RubyTime time, long nanoseconds) {
            time.setNanoseconds(nanoseconds);
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
            throw new UnsupportedOperationException("time_utc_offset");
        }

    }

}
