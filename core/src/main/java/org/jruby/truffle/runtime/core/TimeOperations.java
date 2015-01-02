/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import org.joda.time.DateTime;

public abstract class TimeOperations {

    public static DateTime secondsAndNanosecondsToDateTime(long seconds, long nanoseconds) {
        return new DateTime(secondsToMiliseconds(seconds) + nanosecondsToMilliseconds(nanoseconds));
    }

    public static long secondsToMiliseconds(long seconds) {
        return seconds * 1_000;
    }

    public static long nanosecondsToMilliseconds(long nanoseconds) {
        return nanoseconds * 1_000_000;
    }

    public static long millisecondsToSeconds(long miliseconds) {
        return miliseconds / 1_000;
    }

    public static long millisecondsInCurrentSecond(long miliseconds) {
        return miliseconds % 1_000;
    }

    public static long millisecondsToNanoseconds(long milliseconds) {
        return milliseconds * 1_000_000;
    }

}
