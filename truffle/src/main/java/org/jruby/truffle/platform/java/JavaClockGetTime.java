/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.platform.java;

import org.jruby.truffle.platform.posix.ClockGetTime;
import org.jruby.truffle.platform.posix.TimeSpec;

public class JavaClockGetTime implements ClockGetTime {

    @Override
    public int clock_gettime(int clock_id, TimeSpec timeSpec) {
        throw new UnsupportedOperationException();
    }

}
