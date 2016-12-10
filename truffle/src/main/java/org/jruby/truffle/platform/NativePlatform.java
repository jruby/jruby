/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.platform;

import jnr.ffi.provider.MemoryManager;
import org.jruby.truffle.core.queue.ArrayBlockingQueueLocksConditions;
import org.jruby.truffle.core.queue.LinkedBlockingQueueLocksConditions;
import org.jruby.truffle.platform.posix.ClockGetTime;
import org.jruby.truffle.platform.posix.MallocFree;
import org.jruby.truffle.platform.posix.Sockets;
import org.jruby.truffle.platform.posix.TrufflePosix;
import org.jruby.truffle.platform.signal.SignalManager;

public interface NativePlatform {

    TrufflePosix getPosix();

    MemoryManager getMemoryManager();

    SignalManager getSignalManager();

    ProcessName getProcessName();

    Sockets getSockets();

    ClockGetTime getClockGetTime();

    MallocFree getMallocFree();

    RubiniusConfiguration getRubiniusConfiguration();

    FDSet createFDSet();

    <T> ArrayBlockingQueueLocksConditions<T> createArrayBlockingQueueLocksConditions(int capacity);

    <T> LinkedBlockingQueueLocksConditions<T> createLinkedBlockingQueueLocksConditions();

}
