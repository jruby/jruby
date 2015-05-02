/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.backtrace;

import org.jruby.TruffleBridge;
import org.jruby.util.cli.Options;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Backtrace {

    public static final BacktraceFormatter DISPLAY_FORMATTER = getFormatter(Options.TRUFFLE_BACKTRACE_DISPLAY_FORMAT.load());
    public static final BacktraceFormatter EXCEPTION_FORMATTER = getFormatter(Options.TRUFFLE_BACKTRACE_EXCEPTION_FORMAT.load());
    public static final BacktraceFormatter DEBUG_FORMATTER = getFormatter(Options.TRUFFLE_BACKTRACE_DEBUG_FORMAT.load());
    public static final BacktraceFormatter PANIC_FORMATTER = getFormatter(Options.TRUFFLE_BACKTRACE_DISPLAY_FORMAT.load());

    private final Activation[] activations;

    public Backtrace(Activation[] activations) {
        this.activations = activations;
    }

    public List<Activation> getActivations() {
        return Collections.unmodifiableList(Arrays.asList(activations));
    }

    private static BacktraceFormatter getFormatter(TruffleBridge.BacktraceFormatter formatter) {
        switch (formatter) {
            case MRI:
                return new MRIBacktraceFormatter();
            case DEBUG:
                return new DebugBacktraceFormatter();
            case IMPL_DEBUG:
                return new ImplementationDebugBacktraceFormatter();
            default:
                throw new UnsupportedOperationException();
        }
    }

}
