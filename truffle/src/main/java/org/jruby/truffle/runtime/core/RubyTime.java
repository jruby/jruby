/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import org.joda.time.DateTimeZone;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;

public class RubyTime extends RubyBasicObject {

    private long seconds;
    private long nanoseconds;
    private DateTimeZone zone;

    public RubyTime(RubyClass timeClass, long seconds, long nanoseconds, DateTimeZone zone) {
        super(timeClass);
        this.seconds = seconds;
        this.nanoseconds = nanoseconds;
        this.zone = zone;
    }

    public static class TimeAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, RubyNode currentNode) {
            return new RubyTime(rubyClass, 0, 0, DateTimeZone.UTC);
        }

    }

    public long getSeconds() {
        return seconds;
    }

    public void setSeconds(long seconds) {
        this.seconds = seconds;
    }

    public long getNanoseconds() {
        return nanoseconds;
    }

    public void setNanoseconds(long nanoseconds) {
        this.nanoseconds = nanoseconds;
    }

    public DateTimeZone getZone() {
        return zone;
    }

    public void setZone(DateTimeZone zone) {
        this.zone = zone;
    }
}
