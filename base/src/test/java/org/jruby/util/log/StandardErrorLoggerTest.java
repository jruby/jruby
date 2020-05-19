package org.jruby.util.log;

import java.io.PrintStream;

import org.junit.Test;

public class StandardErrorLoggerTest extends OutputStreamLoggerTest {

    private static final PrintStream defaultErr = System.err;

    protected Logger newLogger() {
        return new StandardErrorLogger(OutputStreamLoggerTest.class.getSimpleName(), stream);
    }

    @Test
    public void testWithDebuggingDisabledUsingSystemErr() {
        try {
            System.setErr(stream);

            StandardErrorLogger logger = new StandardErrorLogger("StandardErrorLoggerTest");

            logger.setDebugEnable(false);
            logger.debug("test");
            stream.flush();
            assertStreamEmpty();
        }
        finally {
            System.setErr(defaultErr);
        }
    }

    @Test
    public void testWithDebuggingEnabledUsingSystemErr() {
        try {
            System.setErr(stream);

            StandardErrorLogger logger = new StandardErrorLogger("StandardErrorLoggerTest");

            logger.setDebugEnable(true);
            String arg = null;
            logger.debug("debug-test-{}", arg);
            assertStreamNotEmpty();
            assertStreamEndsWith("debug-test-null");
        }
        finally {
            System.setErr(defaultErr);
        }
    }

    @Test
    public void testIncludesLevelUsingSystemErr() {
        try {
            System.setErr(stream);

            StandardErrorLogger logger = new StandardErrorLogger("StandardErrorLoggerTest");

            logger.setDebugEnable(true);
            logger.debug("{} message", "a debug");
            assertStreamNotEmpty();
            assertStreamEndsWith("DEBUG StandardErrorLoggerTest : a debug message");

            logger.info("hello!");
            assertStreamNotEmpty();
            assertStreamEndsWith("INFO StandardErrorLoggerTest : hello!");

            logger.warn("red-{}", "alert");
            assertStreamNotEmpty();
            assertStreamEndsWith("WARN StandardErrorLoggerTest : red-alert");
        }
        finally {
            System.setErr(defaultErr);
        }
    }

    @Override @Test
    public void testWithException() {
        try {
            System.setErr(stream);

            StandardErrorLogger logger = new StandardErrorLogger("StandardErrorLoggerTest");

        logger.error("debug-test-x", new RuntimeException("42"));
        assertStreamNotEmpty();

        assertStreamContains("debug-test-x\njava.lang.RuntimeException: 42");
        // with stack trace :
            assertStreamContains("at org.jruby.util.log.StandardErrorLoggerTest.testWithException");
        }
        finally {
            System.setErr(defaultErr);
        }
    }

}
