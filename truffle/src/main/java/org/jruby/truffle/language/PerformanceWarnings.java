/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import java.util.HashSet;
import java.util.Set;
import org.jruby.util.cli.Options;

public class PerformanceWarnings {

    public static final String KWARGS_NOT_OPTIMIZED_YET = "Ruby keyword arguments are not yet optimized";

    private static final boolean ENABLED = Options.TRUFFLE_PERF_WARNING.load();
    private static final Set<String> DISPLAYED_WARNINGS = new HashSet<>();

    public static void warn(String message) {
        if (ENABLED) {
            doWarn(message);
        }
    }

    @TruffleBoundary
    private static void doWarn(String message) {
        synchronized (DISPLAYED_WARNINGS) {
            if (DISPLAYED_WARNINGS.add(message)) {
                System.err.println("[perf warning] " + message);
            }
        }
    }

}
