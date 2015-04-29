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

import org.jruby.RubySignal;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("restriction")
public class SignalOperations {

    private static final ConcurrentMap<Signal, SignalHandler> ORIGINAL_HANDLERS = new ConcurrentHashMap<Signal, SignalHandler>();

    public static final Map<String, Integer> SIGNALS_LIST = Collections.unmodifiableMap(RubySignal.list());

    public static final SignalHandler IGNORE_HANDLER = new SignalHandler() {
        @Override
        public void handle(Signal arg0) {
            // Just ignore the signal.
        }
    };

    public static void watchSignal(Signal signal, SignalHandler newHandler) {
        SignalHandler oldHandler = Signal.handle(signal, newHandler);
        ORIGINAL_HANDLERS.putIfAbsent(signal, oldHandler);
    }

    public static void watchDefaultForSignal(Signal signal) {
        SignalHandler defaultHandler = ORIGINAL_HANDLERS.get(signal);
        if (defaultHandler != null) {
            Signal.handle(signal, defaultHandler);
        }
    }

    public static void raise(Signal signal) {
        Signal.raise(signal);
    }

}
