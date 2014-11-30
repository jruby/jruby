/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

import java.util.Date;

/**
 * Represents the Ruby {@code Time} class. This is a very rough implementation and is only really
 * enough to run benchmark harnesses.
 */

public class RubyTime extends RubyBasicObject {

    /**
     * The class from which we create the object that is {@code Time}. A subclass of
     * {@link RubyClass} so that we can override {@link RubyClass#newInstance} and allocate a
     * {@link RubyTime} rather than a normal {@link RubyBasicObject}.
     */
    public static class RubyTimeClass extends RubyClass {

        public RubyTimeClass(RubyContext context, RubyClass objectClass) {
            super(context, objectClass, objectClass, "Time");
        }

        @Override
        public RubyBasicObject newInstance(RubyNode currentNode) {
            return new RubyTime(this, milisecondsToSeconds(System.currentTimeMillis()), milisecondsToNanoseconds(System.currentTimeMillis()));
        }

    }

    private long seconds;
    private long nanoseconds;
    private boolean isdst;

    public RubyTime(RubyClass timeClass, long seconds, long nanoseconds) {
        super(timeClass);
        this.seconds = seconds;
        this.nanoseconds = nanoseconds;
    }

    public long getWholeSeconds() {
        return seconds;
    }

    public double getRealSeconds() {
        return seconds + nanosecondsToSecond(nanoseconds);
    }

    public static RubyTime fromDate(RubyClass timeClass, long timeMiliseconds) {
        return new RubyTime(timeClass, milisecondsToSeconds(timeMiliseconds), milisecondsToNanoseconds(timeMiliseconds));
    }

    public static RubyTime fromArray(RubyClass timeClass,
                                     int second,
                                     int minute,
                                     int hour,
                                     int dayOfMonth,
                                     int month,
                                     int year,
                                     int nanoOfSecond,
                                     boolean isdst,
                                     RubyString zone) {
        throw new UnsupportedOperationException();
        //ZonedDateTime zdt = ZonedDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond, ZoneId.of(zone.toString()));
        //return new RubyTime(timeClass, zdt.toEpochSecond(), nanoOfSecond);
    }

    public Date toDate() {
        return new Date(secondsToMiliseconds(seconds) + nanosecondsToMiliseconds(nanoseconds));
    }

    private static long milisecondsToSeconds(long miliseconds) {
        return miliseconds / 1000;
    }

    private static long milisecondsToNanoseconds(long miliseconds) {
        return (miliseconds % 1000) * 1000000;
    }

    private static long nanosecondsToMiliseconds(long nanoseconds) {
        return nanoseconds / 1000000;
    }

    public static double nanosecondsToSecond(long nanoseconds) {
        return nanoseconds / 1e9;
    }

    public static long secondsToMiliseconds(long seconds) {
        return seconds * 1000;
    }

}
