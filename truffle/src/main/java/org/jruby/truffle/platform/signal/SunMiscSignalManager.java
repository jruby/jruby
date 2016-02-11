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

public class SunMiscSignalManager implements SignalManager {

    public final Map<String, Integer> SIGNALS_LIST = Collections.unmodifiableMap(RubySignal.list());

    public final SignalHandler IGNORE_HANDLER = new SignalHandler() {
        @Override
        public void handle(Signal arg0) {
            // Just ignore the signal.
        }
    };
    private final ConcurrentMap<sun.misc.Signal, sun.misc.SignalHandler> DEFAULT_HANDLERS = new ConcurrentHashMap<sun.misc.Signal, sun.misc.SignalHandler>();

    @Override
    public Signal createSignal(String name) {
        return new SunMiscSignal(name);
    }

    @Override
    public void watchSignal(Signal signal, SignalHandler newHandler) throws IllegalArgumentException {
        handle((SunMiscSignal) signal, newHandler);
    }

    @Override
    public void watchDefaultForSignal(Signal signal) throws IllegalArgumentException {
        handleDefault((SunMiscSignal) signal);
    }

    @Override
    public void handle(final Signal signal, final SignalHandler newHandler) throws IllegalArgumentException {
        final SunMiscSignal smSignal = (SunMiscSignal) signal;
        final sun.misc.SignalHandler oldSunHandler = sun.misc.Signal.handle(smSignal.getSunMiscSignal(), wrapHandler(signal, newHandler));
        DEFAULT_HANDLERS.putIfAbsent(smSignal.getSunMiscSignal(), oldSunHandler);
    }

    @Override
    public void handleDefault(final Signal signal) throws IllegalArgumentException {
        final SunMiscSignal smSignal = (SunMiscSignal) signal;
        final sun.misc.SignalHandler defaultHandler = DEFAULT_HANDLERS.get(smSignal.getSunMiscSignal());
        if (defaultHandler != null) { // otherwise it is already the default signal
            sun.misc.Signal.handle(smSignal.getSunMiscSignal(), defaultHandler);
        }
    }

    private sun.misc.SignalHandler wrapHandler(final Signal signal, final SignalHandler newHandler) {
        final SunMiscSignal smSignal = (SunMiscSignal) signal;
        return new sun.misc.SignalHandler() {
            @Override
            public void handle(sun.misc.Signal wrappedSignal) {
                newHandler.handle(smSignal);
            }
        };
    }

    @Override
    public  void raise(Signal signal) throws IllegalArgumentException {
        sun.misc.Signal.raise(((SunMiscSignal) signal).getSunMiscSignal());
    }
}
