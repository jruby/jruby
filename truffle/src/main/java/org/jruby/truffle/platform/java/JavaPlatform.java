/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.platform.java;

import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.queue.ArrayBlockingQueueLocksConditions;
import org.jruby.truffle.core.queue.LinkedBlockingQueueLocksConditions;
import org.jruby.truffle.platform.ClockGetTime;
import org.jruby.truffle.platform.DefaultRubiniusConfiguration;
import org.jruby.truffle.platform.NativePlatform;
import org.jruby.truffle.platform.ProcessName;
import org.jruby.truffle.platform.RubiniusConfiguration;
import org.jruby.truffle.platform.Sockets;
import org.jruby.truffle.platform.TrufflePOSIXHandler;
import org.jruby.truffle.platform.linux.LinuxRubiniusConfiguration;
import org.jruby.truffle.platform.openjdk.OpenJDKArrayBlockingQueueLocksConditions;
import org.jruby.truffle.platform.openjdk.OpenJDKLinkedBlockingQueueLocksConditions;
import org.jruby.truffle.platform.signal.SignalManager;
import org.jruby.truffle.platform.sunmisc.SunMiscSignalManager;

public class JavaPlatform implements NativePlatform {

    private final POSIX posix;
    private final SignalManager signalManager;
    private final ProcessName processName;
    private final Sockets sockets;
    private final ClockGetTime clockGetTime;
    private final RubiniusConfiguration rubiniusConfiguration;

    public JavaPlatform(RubyContext context) {
        posix = new TruffleJavaPOSIX(context, POSIXFactory.getJavaPOSIX(new TrufflePOSIXHandler(context)));
        signalManager = new SunMiscSignalManager();
        processName = new JavaProcessName();
        sockets = new JavaSockets();
        clockGetTime = new JavaClockGetTime();
        rubiniusConfiguration = new RubiniusConfiguration();
        DefaultRubiniusConfiguration.load(rubiniusConfiguration, context);
        LinuxRubiniusConfiguration.load(rubiniusConfiguration, context); // Just load the Linux one - let errors happen later
    }

    @Override
    public POSIX getPosix() {
        return posix;
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
