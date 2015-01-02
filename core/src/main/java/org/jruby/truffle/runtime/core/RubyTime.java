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
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;

public class RubyTime extends RubyBasicObject {

    private long seconds;
    private long nanoseconds;
    private Object fromGMT;
    private Object offset;

    public RubyTime(RubyClass timeClass, long seconds, long nanoseconds, Object fromGMT, Object offset) {
        super(timeClass);
        this.seconds = seconds;
        this.nanoseconds = nanoseconds;
        this.fromGMT = fromGMT;
        this.offset = offset;
    }

    public RubyTime(RubyClass timeClass, long seconds, long nanoseconds) {
        this(timeClass,
                seconds,
                nanoseconds,
                timeClass.getContext().getCoreLibrary().getNilObject(),
                timeClass.getContext().getCoreLibrary().getNilObject());
    }

    public RubyTime(RubyClass timeClass, long milliseconds) {
        this(timeClass,
                TimeOperations.millisecondsToSeconds(milliseconds),
                TimeOperations.millisecondsToNanoseconds(TimeOperations.millisecondsInCurrentSecond(milliseconds)));
    }

    public static class TimeAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, RubyNode currentNode) {
            return new RubyTime(rubyClass, 0, 0);
        }

    }

    public long getSeconds() {
        return seconds;
    }

    public void setSeconds(long nanoseconds) {
        this.seconds = seconds;
    }

    public long getNanoseconds() {
        return nanoseconds;
    }

    public void setNanoseconds(long nanoseconds) {
        this.nanoseconds = nanoseconds;
    }

}
