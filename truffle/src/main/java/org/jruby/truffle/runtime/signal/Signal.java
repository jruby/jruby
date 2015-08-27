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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("restriction")
public class Signal {

    private final sun.misc.Signal sunSignal;

    private static final ConcurrentMap<sun.misc.Signal, sun.misc.SignalHandler> DEFAULT_HANDLERS = new ConcurrentHashMap<sun.misc.Signal, sun.misc.SignalHandler>();

    public Signal(String name) {
        sunSignal = new sun.misc.Signal(name);
    }

    public static void handle(final Signal signal, final SignalHandler newHandler) {
        final sun.misc.SignalHandler oldSunHandler = sun.misc.Signal.handle(signal.sunSignal, wrapHandler(signal, newHandler));
        DEFAULT_HANDLERS.putIfAbsent(signal.sunSignal, oldSunHandler);
    }

    public static void handleDefault(final Signal signal) {
        final sun.misc.SignalHandler defaultHandler = DEFAULT_HANDLERS.get(signal.sunSignal);
        if (defaultHandler != null) { // otherwise it is already the default signal
            sun.misc.Signal.handle(signal.sunSignal, defaultHandler);
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

    public static void raise(Signal signal) {
        sun.misc.Signal.raise(signal.sunSignal);
    }

}
