/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.platform.darwin;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Runtime;
import jnr.ffi.provider.MemoryManager;
import jnr.posix.POSIXFactory;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.queue.ArrayBlockingQueueLocksConditions;
import org.jruby.truffle.core.queue.LinkedBlockingQueueLocksConditions;
import org.jruby.truffle.platform.DefaultRubiniusConfiguration;
import org.jruby.truffle.platform.NativePlatform;
import org.jruby.truffle.platform.ProcessName;
import org.jruby.truffle.platform.RubiniusConfiguration;
import org.jruby.truffle.platform.java.JavaClockGetTime;
import org.jruby.truffle.platform.openjdk.OpenJDKArrayBlockingQueueLocksConditions;
import org.jruby.truffle.platform.openjdk.OpenJDKLinkedBlockingQueueLocksConditions;
import org.jruby.truffle.platform.posix.ClockGetTime;
import org.jruby.truffle.platform.posix.JNRTrufflePosix;
import org.jruby.truffle.platform.posix.Sockets;
import org.jruby.truffle.platform.posix.TrufflePosix;
import org.jruby.truffle.platform.posix.TrufflePosixHandler;
import org.jruby.truffle.platform.signal.SignalManager;
import org.jruby.truffle.platform.sunmisc.SunMiscSignalManager;

public class DarwinPlatform implements NativePlatform {

    private final TrufflePosix posix;
    private final MemoryManager memoryManager;
    private final SignalManager signalManager;
    private final ProcessName processName;
    private final Sockets sockets;
    private final ClockGetTime clockGetTime;
    private final RubiniusConfiguration rubiniusConfiguration;

    public DarwinPlatform(RubyContext context) {
        posix = new JNRTrufflePosix(POSIXFactory.getNativePOSIX(new TrufflePosixHandler(context)));
        memoryManager = Runtime.getSystemRuntime().getMemoryManager();
        signalManager = new SunMiscSignalManager();
        processName = new DarwinProcessName();
        sockets = LibraryLoader.create(Sockets.class).library("c").load();
        clockGetTime = new JavaClockGetTime();
        rubiniusConfiguration = new RubiniusConfiguration();
        DefaultRubiniusConfiguration.load(rubiniusConfiguration, context);
        DarwinRubiniusConfiguration.load(rubiniusConfiguration, context);
    }

    @Override
    public TrufflePosix getPosix() {
        return posix;
    }

    @Override
    public MemoryManager getMemoryManager() {
        return memoryManager;
    }

    @Override
    public SignalManager getSignalManager() {
        return signalManager;
    }

    @Override
    public ProcessName getProcessName() {
        return processName;
    }

    @Override
    public Sockets getSockets() {
        return sockets;
    }

    @Override
    public ClockGetTime getClockGetTime() {
        return clockGetTime;
    }

    @Override
    public RubiniusConfiguration getRubiniusConfiguration() {
        return rubiniusConfiguration;
    }

    @Override
    public <T> ArrayBlockingQueueLocksConditions<T> createArrayBlockingQueueLocksConditions(int capacity) {
        return new OpenJDKArrayBlockingQueueLocksConditions<>(capacity);
    }

    @Override
    public <T> LinkedBlockingQueueLocksConditions<T> createLinkedBlockingQueueLocksConditions() {
        return new OpenJDKLinkedBlockingQueueLocksConditions<>();
    }

}
