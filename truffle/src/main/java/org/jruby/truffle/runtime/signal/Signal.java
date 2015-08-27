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

    private final sun.misc.Signal signal;

    private static final ConcurrentMap<sun.misc.SignalHandler, SignalHandler> SUN2OUR_HANDLERS = new ConcurrentHashMap<sun.misc.SignalHandler, SignalHandler>();

    public Signal(String name) {
        signal = new sun.misc.Signal(name);
    }

    public static SignalHandler handle(final Signal signal, final SignalHandler newHandler) {
        final sun.misc.SignalHandler wrappedNewHandler = wrapHandler(signal, newHandler);
        SUN2OUR_HANDLERS.put(wrappedNewHandler, newHandler);

        final sun.misc.SignalHandler oldWrappedHandler = sun.misc.Signal.handle(signal.signal, wrappedNewHandler);

        final SignalHandler oldHandler = SUN2OUR_HANDLERS.putIfAbsent(oldWrappedHandler, unwrapHandler(oldWrappedHandler));
        return oldHandler;
    }

    private static sun.misc.SignalHandler wrapHandler(final Signal signal, final SignalHandler newHandler) {
        return new sun.misc.SignalHandler() {
            @Override
            public void handle(sun.misc.Signal wrappedSignal) {
                newHandler.handle(signal);
            }
        };
    }

    private static SignalHandler unwrapHandler(final sun.misc.SignalHandler oldWrappedHandler) {
        return new SignalHandler() {
            @Override
            public void handle(Signal signal) {
                oldWrappedHandler.handle(signal.signal);
            }
        };
    }

    public static void raise(Signal signal) {
        sun.misc.Signal.raise(signal.signal);
    }

}
