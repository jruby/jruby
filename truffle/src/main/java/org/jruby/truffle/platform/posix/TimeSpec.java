/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.platform.posix;

import jnr.ffi.Struct;

public final class TimeSpec extends Struct {

    public final time_t tv_sec = new time_t();
    public final SignedLong tv_nsec = new SignedLong();

    public TimeSpec(jnr.ffi.Runtime runtime) {
        super(runtime);
    }

    public long getTVsec() {
        return tv_sec.get();
    }

    public long getTVnsec() {
        return tv_nsec.get();
    }

}
