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
import org.jruby.truffle.nodes.objectstorage.ReadHeadObjectFieldNode;
import org.jruby.truffle.nodes.objectstorage.WriteHeadObjectFieldNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.*;
import org.jruby.util.ByteList;
import org.jruby.util.RubyDateFormatter;

/**
 * Rubinius primitives associated with the Ruby {@code Time} class.
 * <p>
 * Also see {@link RubyTime}.
 */
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
            final long milliseconds = System.currentTimeMillis();
            return new RubyTime(timeClass,
                            TimeOperations.millisecondsToSeconds(milliseconds),
                            TimeOperations.millisecondsToNanoseconds(TimeOperations.millisecondsInCurrentSecond(milliseconds)));
        }

    }

    @RubiniusPrimitive(name = "time_s_dup", needsSelf = false)
    public static abstract class TimeSDupPrimitiveNode extends RubiniusPrimitiveNode {

        @Child protected ReadHeadObjectFieldNode readIsGMTNode = new ReadHeadObjectFieldNode("@is_gmt");
        @Child protected ReadHeadObjectFieldNode readOffsetNode = new ReadHeadObjectFieldNode("@offset");

        @Child protected WriteHeadObjectFieldNode writeIsGMTNode = new WriteHeadObjectFieldNode("@is_gmt");
        @Child protected WriteHeadObjectFieldNode writeOffsetNode = new WriteHeadObjectFieldNode("@offset");

        public TimeSDupPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TimeSDupPrimitiveNode(TimeSDupPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public RubyTime timeSDup(RubyTime other) {
            final RubyTime time = new RubyTime(getContext().getCoreLibrary().getTimeClass(), other.getSeconds(), other.getNanoseconds());
            writeIsGMTNode.execute(time, readIsGMTNode.execute(other));
            writeOffsetNode.execute(time, readOffsetNode.execute(other));
            return time;
        }

    }

    @RubiniusPrimitive(name = "time_s_specific", needsSelf = false)
    public static abstract class TimeSSpecificPrimitiveNode extends RubiniusPrimitiveNode {

        @Child protected WriteHeadObjectFieldNode writeIsGMTNode = new WriteHeadObjectFieldNode("@is_gmt");
        @Child protected WriteHeadObjectFieldNode writeOffsetNode = new WriteHeadObjectFieldNode("@offset");

        public TimeSSpecificPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TimeSSpecificPrimitiveNode(TimeSSpecificPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public RubyTime timeSSpecific(int seconds, int nanoseconds, Object isGMT, Object offset) {
            return timeSSpecific((long) seconds, (long) nanoseconds, isGMT, offset);
        }

        @Specialization
        public RubyTime timeSSpecific(long seconds, int nanoseconds, Object isGMT, Object offset) {
            return timeSSpecific(seconds, (long) nanoseconds, isGMT, offset);
        }

        @Specialization
        public RubyTime timeSSpecific(int seconds, long nanoseconds, Object isGMT, Object offset) {
            return timeSSpecific((long) seconds, nanoseconds, isGMT, offset);
        }

        @Specialization
        public RubyTime timeSSpecific(long seconds, long nanoseconds, Object isGMT, Object offset) {
            // TODO(CS): overflow checks here in Rbx
            final RubyTime time = new RubyTime(getContext().getCoreLibrary().getTimeClass(), seconds, nanoseconds);
            writeIsGMTNode.execute(time, isGMT);
            writeOffsetNode.execute(time, offset);
            return time;
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

        @Child protected RubyTimeToDateTimeNode toDateTimeNode;

        public TimeDecomposePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toDateTimeNode = new RubyTimeToDateTimeNode(context, sourceSection);
        }

        public TimeDecomposePrimitiveNode(TimeDecomposePrimitiveNode prev) {
            super(prev);
            toDateTimeNode = prev.toDateTimeNode;
        }

        @Specialization
        public RubyArray timeDecompose(VirtualFrame frame, RubyTime time) {
            final DateTime dateTime = toDateTimeNode.toDateTime(frame, time);
            final Object[] decomposed = decompose(dateTime);
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), decomposed, decomposed.length);
        }

        @CompilerDirectives.TruffleBoundary
        public Object[] decompose(DateTime dateTime) {
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
            return new Object[]{sec, min, hour, day, month, year, wday, yday, isdst, zone};
        }

    }

    @RubiniusPrimitive(name = "time_strftime")
    public static abstract class TimeStrftimePrimitiveNode extends RubiniusPrimitiveNode {

        @Child protected RubyTimeToDateTimeNode toDateTimeNode;

        public TimeStrftimePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toDateTimeNode = new RubyTimeToDateTimeNode(context, sourceSection);
        }

        public TimeStrftimePrimitiveNode(TimeStrftimePrimitiveNode prev) {
            super(prev);
            toDateTimeNode = prev.toDateTimeNode;
        }

        @Specialization
        public RubyString timeStrftime(VirtualFrame frame, RubyTime time, RubyString format) {
            return getContext().makeString(format(toDateTimeNode.toDateTime(frame, time), time.getNanoseconds(), format.getBytes()));
        }


        @CompilerDirectives.TruffleBoundary
        public ByteList format(DateTime time, long nanoseconds, ByteList format) {
            // TODO: converts everything to JRuby objects and back - should find a more direct way using ByteList
            final RubyDateFormatter rdf = getContext().getRuntime().getCurrentContext().getRubyDateFormatter();
            return rdf.compileAndFormat(org.jruby.RubyString.newString(getContext().getRuntime(), format), false, time, nanoseconds, null).getByteList();
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
