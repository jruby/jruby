/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.platform.sunmisc;

import org.jruby.truffle.platform.signal.Signal;

@SuppressWarnings("restriction")
public class SunMiscSignal implements Signal {

    private final sun.misc.Signal sunMiscSignal;

    public SunMiscSignal(String name) {
        sunMiscSignal = new sun.misc.Signal(name);
    }

    public sun.misc.Signal getSunMiscSignal() {
        return sunMiscSignal;
    }

}
