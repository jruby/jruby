/*
 * Copyright (c) 2015 JRuby.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    JRuby - initial API and implementation and/or initial documentation
 */
package org.jruby.util.log;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author kares
 */
public class LoggerFactoryTest {

    @Test
    public void hasCorrectBackupLoggerClassName() {
        assertEquals( StandardErrorLogger.class.getName(), LoggerFactory.BACKUP_LOGGER_CLASS );
    }

    @Test
    public void usesStandardErrorLoggerByDefault() {
        if ( LoggerFactory.LOGGER_CLASS != null ) return; // skip test

        Logger logger = LoggerFactory.getLogger("LoggerFactoryTest");
        assertTrue( logger instanceof StandardErrorLogger );
        assertFalse( logger.isDebugEnabled() );
        logger.debug("not-logged", new IllegalStateException("if you're reading this - smt is BAD"));
        // try if it works without throwing :
        logger.info("hello {}", "world");
    }

    @Test
    public void usingJULLogger() throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger("JULLogger");
        java.util.logging.SimpleFormatter fmt = new java.util.logging.SimpleFormatter() {

            @Override
            public synchronized String format(java.util.logging.LogRecord record) {
                final String format = "%2$s %4$s: %5$s%6$s%n";
                String source;
                if (record.getSourceClassName() != null) {
                    source = record.getSourceClassName();
                    if (record.getSourceMethodName() != null) {
                       source += " " + record.getSourceMethodName();
                    }
                } else {
                    source = record.getLoggerName();
                }
                String message = formatMessage(record);
                String throwable = "";
                if (record.getThrown() != null) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    pw.println();
                    record.getThrown().printStackTrace(pw);
                    pw.close();
                    throwable = sw.toString();
                }
                return String.format(format,
                                     "", // no timestamp
                                     source,
                                     record.getLoggerName(),
                                     record.getLevel().getLocalizedName(),
                                     message,
                                     throwable);
            }

        };
        java.util.logging.StreamHandler handler = new java.util.logging.StreamHandler(out, fmt);
        handler.setLevel(java.util.logging.Level.ALL);
        julLogger.addHandler(handler);
        julLogger.setUseParentHandlers(false);

        julLogger.setLevel(java.util.logging.Level.INFO);

        changeLoggerImpl(JULLogger.class);

        Logger logger = LoggerFactory.getLogger("JULLogger");
        assertFalse( logger.isDebugEnabled() );

        logger.debug("ignored debug stuff");
        logger.debug("more ignored debug stuff {}", 10);
        handler.flush();
        assertEquals("", out.toString());

        String log = "";

        logger.info("logged at info level");
        handler.flush();
        assertEquals(log += "JULLogger INFO: logged at info level\n", out.toString());

        logger.warn("logged at {} {}", "warn", new StringBuilder("level"));
        handler.flush();
        assertEquals(log += "JULLogger WARNING: logged at warn level\n", out.toString());

        julLogger.setLevel(java.util.logging.Level.WARNING);

        logger.info("more at info level {}", 'z');
        handler.flush(); assertEquals(log, out.toString());
        logger.info("even more at info level", new RuntimeException("ex"));
        handler.flush(); assertEquals(log, out.toString());

        logger.error("bad news", new RuntimeException("exception happened"));
        handler.flush();
        assertStartsWith(log += "JULLogger SEVERE: bad news\njava.lang.RuntimeException: exception happened", out.toString());
    }

    final static Constructor DEFAULT_LOGGER = LoggerFactory.LOGGER;

    @After
    public void restoreLoggerImpl() throws Exception {
        if ( LoggerFactory.LOGGER != DEFAULT_LOGGER ) {
            setLoggerImpl(DEFAULT_LOGGER);
        }
    }

    static void changeLoggerImpl(final Class<? extends Logger> type) throws NoSuchFieldException, IllegalAccessException {
        setLoggerImpl( LoggerFactory.resolveLoggerConstructor(type.getName()) );
    }

    static void setLoggerImpl(final Constructor logger) throws NoSuchFieldException, IllegalAccessException {
        final Field LOGGER = LoggerFactory.class.getDeclaredField("LOGGER");
        LOGGER.setAccessible(true);
        final int mod = LOGGER.getModifiers();
        changeModifiers(LOGGER, mod & (~Modifier.FINAL));

        LOGGER.set(null, logger);
    }

    private static void changeModifiers(final Field field, final int mod)
        throws NoSuchFieldException, IllegalAccessException {
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, mod);
    }

    static void assertStartsWith(String expected, String actual) {
        final String begPart = actual.substring(0, expected.length());
        assertEquals("expected: \"" + actual + "\" to start with: \"" + expected + "\"", expected, begPart);
    }

}
