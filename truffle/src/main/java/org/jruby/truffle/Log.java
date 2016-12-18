/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Log {

    private static class RubyLevel extends Level {

        public RubyLevel(String name, Level parent) {
            super(name, parent.intValue(), parent.getResourceBundleName());
        }

    }

    public static final Level PERFORMANCE = new RubyLevel("PERFORMANCE", Level.WARNING);

    private static final Logger LOGGER = createLogger();

    private static Logger createLogger() {
        final Logger logger = Logger.getLogger("org.jruby.truffle");

        logger.setUseParentHandlers(false);

        logger.addHandler(new Handler() {

            @Override
            public void publish(LogRecord record) {
                System.err.printf("[ruby] %s %s%n", record.getLevel().getName(), record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }

        });

        return logger;
    }

    private static final Set<String> displayedWarnings = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static final String KWARGS_NOT_OPTIMIZED_YET = "keyword arguments are not yet optimized";

    @TruffleBoundary
    public static void performanceOnce(String message) {
        // This isn't double-checked locking, because we aren't publishing an object that is then used

        if (!displayedWarnings.contains(message)) {
            synchronized (displayedWarnings) {
                if (!displayedWarnings.contains(message)) {
                    displayedWarnings.add(message);
                    performance(message);
                }
            }
        }
    }

    @TruffleBoundary
    public static void performance(String message) {
        LOGGER.log(PERFORMANCE, message);
    }

    @TruffleBoundary
    public static void warning(String message) {
        LOGGER.warning(message);
    }

    @TruffleBoundary
    public static void info(String message) {
        LOGGER.info(message);
    }

    @TruffleBoundary
    public static void info(String format, Object... args) {
        info(String.format(format, args));
    }

}
