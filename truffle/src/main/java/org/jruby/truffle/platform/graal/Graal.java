/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.platform.graal;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;

import java.util.Locale;

public abstract class Graal {

    @TruffleBoundary
    public static boolean isGraal() {
        return Truffle.getRuntime().getName().toLowerCase(Locale.ENGLISH).contains("graal");
    }

    public static String getVersion() {
        return System.getProperty("graal.version", "unknown");
    }

}
