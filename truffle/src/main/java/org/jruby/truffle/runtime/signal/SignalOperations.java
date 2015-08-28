/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.signal;

import java.util.Collections;
import java.util.Map;

import org.jruby.RubySignal;

public class SignalOperations {

    public static final Map<String, Integer> SIGNALS_LIST = Collections.unmodifiableMap(RubySignal.list());

    public static final SignalHandler IGNORE_HANDLER = new SignalHandler() {
        @Override
        public void handle(Signal arg0) {
            // Just ignore the signal.
        }
    };

    public static void watchSignal(Signal signal, SignalHandler newHandler) {
        Signal.handle(signal, newHandler);
    }

    public static void watchDefaultForSignal(Signal signal) {
        Signal.handleDefault(signal);
    }

    public static void raise(Signal signal) {
        Signal.raise(signal);
    }

}
