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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import org.jruby.truffle.platform.signal.Signal;
import org.jruby.truffle.platform.signal.SignalHandler;
import org.jruby.truffle.platform.signal.SignalManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("restriction")
public class SunMiscSignalManager implements SignalManager {

    private final ConcurrentMap<sun.misc.Signal, sun.misc.SignalHandler> DEFAULT_HANDLERS = new ConcurrentHashMap<>();

    @Override
    public Signal createSignal(String name) {
        return new SunMiscSignal(name);
    }

    @Override
    public void watchSignal(Signal signal, SignalHandler newHandler) throws IllegalArgumentException {
        handle(signal, newHandler);
    }

    @Override
    public void watchDefaultForSignal(Signal signal) throws IllegalArgumentException {
        handleDefault(signal);
    }

    @TruffleBoundary
    @Override
    public void handle(final Signal signal, final SignalHandler newHandler) throws IllegalArgumentException {
        final SunMiscSignal smSignal = (SunMiscSignal) signal;

        final sun.misc.SignalHandler oldSunHandler = sun.misc.Signal.handle(
                smSignal.getSunMiscSignal(), wrapHandler(signal, newHandler));

        DEFAULT_HANDLERS.putIfAbsent(smSignal.getSunMiscSignal(), oldSunHandler);
    }

    @TruffleBoundary
    @Override
    public void handleDefault(final Signal signal) throws IllegalArgumentException {
        final SunMiscSignal smSignal = (SunMiscSignal) signal;
        final sun.misc.SignalHandler defaultHandler = DEFAULT_HANDLERS.get(smSignal.getSunMiscSignal());
        if (defaultHandler != null) { // otherwise it is already the default signal
            sun.misc.Signal.handle(smSignal.getSunMiscSignal(), defaultHandler);
        }
    }

    @TruffleBoundary
    private sun.misc.SignalHandler wrapHandler(final Signal signal, final SignalHandler newHandler) {
        final SunMiscSignal smSignal = (SunMiscSignal) signal;
        return wrappedSignal -> newHandler.handle(smSignal);
    }

    @TruffleBoundary
    @Override
    public void raise(Signal signal) throws IllegalArgumentException {
        sun.misc.Signal.raise(((SunMiscSignal) signal).getSunMiscSignal());
    }
}
