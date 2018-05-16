/*
 * Copyright (c) 2015 JRuby.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *    JRuby - initial API and implementation and/or initial documentation
 */
package org.jruby.util.log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.Assert;
import org.junit.Test;
import org.junit.After;

/**
 * @author kares
 */
public class OutputStreamLoggerTest extends junit.framework.TestCase {

    static final String LINE_SEPARATOR = System.getProperty("line.separator");

    protected final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    protected final PrintStream stream = new PrintStream(baos);

    protected final Logger logger = newLogger();

    protected Logger newLogger() {
        return new OutputStreamLogger(OutputStreamLoggerTest.class.getSimpleName(), stream);
    }

    @After
    public void resetStream() throws IOException {
        baos.reset();
    }

    @Test
    public void testPlainMessage() {
        logger.info("test");
        assertStreamEndsWith("test");
    }

    @Test
    public void testMessageWithPlaceholder() {
        logger.warn("hello {}", "world");
        assertStreamEndsWith("hello world");
    }

    @Test
    public void testMessageWithPlaceholders() {
        logger.warn("{} {}", 1, new StringBuilder("23"));
        assertStreamEndsWith("1 23\n", false);
    }

    @Test
    public void testWithDebuggingDisabled() {
        final boolean debug = logger.isDebugEnabled();
        try {
            logger.setDebugEnable(false);
            logger.debug("debug-test-1");
            logger.debug("debug-test-{}", 2);
            logger.debug("debug-test-x", new RuntimeException());
            assertStreamEmpty();
        }
        finally {
            logger.setDebugEnable(debug);
        }
    }

    @Test
    public void testWithDebuggingEnabled() {
        final boolean debug = logger.isDebugEnabled();
        try {
            logger.setDebugEnable(true);
            String arg = null;
            logger.debug("debug-test-{}", arg);
            assertStreamNotEmpty();
            assertStreamEndsWith("debug-test-null");
        }
        finally {
            logger.setDebugEnable(debug);
        }
    }

    @Test
    public void testIncludesLevel() {
        final boolean debug = logger.isDebugEnabled();
        try {
            logger.setDebugEnable(true);
            logger.debug("{} message", "a debug");
            assertStreamNotEmpty();
            assertStreamEndsWith("DEBUG OutputStreamLoggerTest : a debug message");

            logger.info("hello!");
            assertStreamNotEmpty();
            assertStreamEndsWith("INFO OutputStreamLoggerTest : hello!");

            logger.warn("red-{}", "alert");
            assertStreamNotEmpty();
            assertStreamEndsWith("WARN OutputStreamLoggerTest : red-alert");
        }
        finally {
            logger.setDebugEnable(debug);
        }
    }

    @Test
    public void testWithException() {
        logger.error("debug-test-x", new RuntimeException("42"));
        assertStreamNotEmpty();

        assertStreamContains("debug-test-x\njava.lang.RuntimeException: 42");
        // with stack trace :
        assertStreamContains("at org.jruby.util.log.OutputStreamLoggerTest.testWithException");
    }

    protected void assertStreamEmpty() {
        stream.flush();
        Assert.assertEquals("", baos.toString());
    }

    protected void assertStreamNotEmpty() {
        stream.flush();
        Assert.assertNotEquals("", baos.toString());
    }

    protected void assertStreamEndsWith(String expected) {
        assertStreamEndsWith(expected, true);
    }

    protected void assertStreamEndsWith(String expected, final boolean newLine) {
        stream.flush();
        if ( newLine ) expected = expected + LINE_SEPARATOR;

        final String actual = baos.toString();
        final String endPart = actual.substring(actual.length() - expected.length());

        Assert.assertEquals("expected: \"" + actual + "\" to end with: \"" + expected + "\"", expected, endPart);
    }

    protected void assertStreamContains(String expected) {
        stream.flush();

        final String actual = baos.toString();

        Assert.assertTrue("expected: \"" + actual + "\" to contain: \"" + expected + "\"", actual.contains(expected));
    }

}
