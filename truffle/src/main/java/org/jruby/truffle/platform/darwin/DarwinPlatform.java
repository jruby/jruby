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
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.platform.*;
import org.jruby.truffle.platform.java.JavaClockGetTime;
import org.jruby.truffle.platform.signal.SignalManager;
import org.jruby.truffle.platform.sunmisc.SunMiscSignalManager;

public class DarwinPlatform implements NativePlatform {

    private final POSIX posix;
    private final SignalManager signalManager;
    private final ProcessName processName;
    private final Sockets sockets;
    private final ClockGetTime clockGetTime;
    private final RubiniusConfiguration rubiniusConfiguration;

    public DarwinPlatform(RubyContext context) {
        posix = POSIXFactory.getNativePOSIX(new TrufflePOSIXHandler(context));
        signalManager = new SunMiscSignalManager();
        processName = new DarwinProcessName();
        sockets = LibraryLoader.create(Sockets.class).library("c").load();
        clockGetTime = new JavaClockGetTime();
        rubiniusConfiguration = new RubiniusConfiguration();
        DefaultRubiniusConfiguration.load(rubiniusConfiguration, context);
        DarwinRubiniusConfiguration.load(rubiniusConfiguration, context);
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

}
