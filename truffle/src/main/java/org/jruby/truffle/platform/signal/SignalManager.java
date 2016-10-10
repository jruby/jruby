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

public interface SignalManager {

    Map<String, Integer> SIGNALS_LIST = Collections.unmodifiableMap(RubySignal.list());

    SignalHandler IGNORE_HANDLER = signal -> {
        // Just ignore the signal.
    };

    Signal createSignal(String name);

    void watchSignal(Signal signal, SignalHandler newHandler) throws IllegalArgumentException;

    void watchDefaultForSignal(Signal signal) throws IllegalArgumentException;

    void handle(final Signal signal, final SignalHandler newHandler) throws IllegalArgumentException;

    void handleDefault(final Signal signal) throws IllegalArgumentException;

    void raise(Signal signal) throws IllegalArgumentException;

}
