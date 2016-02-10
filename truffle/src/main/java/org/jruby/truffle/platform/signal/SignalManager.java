/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.platform.signal;

import org.jruby.RubySignal;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SignalManager {

    public static final Map<String, Integer> SIGNALS_LIST = Collections.unmodifiableMap(RubySignal.list());

    public static final SignalHandler IGNORE_HANDLER = new SignalHandler() {
        @Override
        public void handle(Signal arg0) {
            // Just ignore the signal.
        }
    };
    private static final ConcurrentMap<sun.misc.Signal, sun.misc.SignalHandler> DEFAULT_HANDLERS = new ConcurrentHashMap<sun.misc.Signal, sun.misc.SignalHandler>();

    public static void watchSignal(Signal signal, SignalHandler newHandler) throws IllegalArgumentException {
        handle(signal, newHandler);
    }

    public static void watchDefaultForSignal(Signal signal) throws IllegalArgumentException {
        handleDefault(signal);
    }

    public static void handle(final Signal signal, final SignalHandler newHandler) throws IllegalArgumentException {
        final sun.misc.SignalHandler oldSunHandler = sun.misc.Signal.handle(signal.getSunMiscSignal(), wrapHandler(signal, newHandler));
        DEFAULT_HANDLERS.putIfAbsent(signal.getSunMiscSignal(), oldSunHandler);
    }

    public static void handleDefault(final Signal signal) throws IllegalArgumentException {
        final sun.misc.SignalHandler defaultHandler = DEFAULT_HANDLERS.get(signal.getSunMiscSignal());
        if (defaultHandler != null) { // otherwise it is already the default signal
            sun.misc.Signal.handle(signal.getSunMiscSignal(), defaultHandler);
        }
    }

    private static sun.misc.SignalHandler wrapHandler(final Signal signal, final SignalHandler newHandler) {
        return new sun.misc.SignalHandler() {
            @Override
            public void handle(sun.misc.Signal wrappedSignal) {
                newHandler.handle(signal);
            }
        };
    }

    public static void raise(Signal signal) throws IllegalArgumentException {
        sun.misc.Signal.raise(signal.getSunMiscSignal());
    }
}
