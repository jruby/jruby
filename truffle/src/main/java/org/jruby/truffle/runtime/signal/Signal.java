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

import java.util.Map;
import java.util.WeakHashMap;

public class Signal {

    private final sun.misc.Signal signal;

    private final static Map<sun.misc.SignalHandler, SignalHandler> handlers = new WeakHashMap<>();

    public Signal(String name) {
        signal = new sun.misc.Signal(name);
    }

    public static SignalHandler handle(final Signal signal, final SignalHandler newHandler) {
        final sun.misc.SignalHandler wrappedNewHandler = new sun.misc.SignalHandler() {

            @Override
            public void handle(sun.misc.Signal wrappedSignal) {
                newHandler.handle(signal);
            }

        };

        synchronized (handlers) {
            handlers.put(wrappedNewHandler, newHandler);

            final sun.misc.SignalHandler oldWrappedHandler = sun.misc.Signal.handle(signal.signal, wrappedNewHandler);

            SignalHandler oldHandler = handlers.get(oldWrappedHandler);

            if (oldHandler == null) {
                oldHandler = new SignalHandler() {
                    @Override
                    public void handle(Signal signal) {
                        oldWrappedHandler.handle(signal.signal);
                    }
                };

                handlers.put(oldWrappedHandler, oldHandler);
            }

            return oldHandler;
        }
    }

    public static void raise(Signal signal) {
        sun.misc.Signal.raise(signal.signal);
    }

}
