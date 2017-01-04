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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Log {

    private static class RubyLevel extends Level {

        private static final long serialVersionUID = 3759389129096588683L;

        public RubyLevel(String name, Level parent) {
            super(name, parent.intValue(), parent.getResourceBundleName());
        }

    }

    public static final Level PERFORMANCE = new RubyLevel("PERFORMANCE", Level.WARNING);

    public static final Logger LOGGER = createLogger();

    public static class RubyHandler extends Handler {

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

    }

    private static Logger createLogger() {
        final Logger logger = Logger.getLogger("org.jruby.truffle");

        if (LogManager.getLogManager().getProperty("org.jruby.truffle.handlers") == null) {
            logger.setUseParentHandlers(false);
            logger.addHandler(new RubyHandler());
        }

        return logger;
    }

    private static final Set<String> displayedWarnings = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static final String KWARGS_NOT_OPTIMIZED_YET = "keyword arguments are not yet optimized";

    /**
     * Warn about code that works but is not yet optimized as Truffle code normally would be. Only prints the warning
     * once, and only if called from compiled code. Don't call this method from behind a boundary, as it will never
     * print the warning because it will never be called from compiled code. Use {@link #performanceOnce} instead
     * if you need to warn in code that is never compiled.
     */
    public static void notOptimizedOnce(String message) {
        if (CompilerDirectives.inCompiledCode()) {
            performanceOnce(message);
        }
    }

    /**
     * Warn about something that has lower performance than might be expected. Only prints the warning once.
     */
    @TruffleBoundary
    public static void performanceOnce(String message) {
        if (displayedWarnings.add(message)) {
            LOGGER.log(PERFORMANCE, message);
        }
    }

    @TruffleBoundary
    public static void info(String format, Object... args) {
        LOGGER.info(String.format(format, args));
    }

}
