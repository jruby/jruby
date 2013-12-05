package org.jruby.util.log;

import junit.framework.TestCase;
import org.junit.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class StandardErrorLoggerTest extends TestCase {

    private ByteArrayOutputStream baos;
    private StandardErrorLogger logger;
    private PrintStream stream;

    public void setUp() {
        baos = new ByteArrayOutputStream();
        stream = new PrintStream(baos);
        logger = new StandardErrorLogger("test", stream);
    }

    public void tearDown() throws IOException {
        baos.reset();
        baos.close();
    }

    public void testWithDebuggingDisabled() {
        logger.setDebugEnable(false);
        logger.debug("test");
        stream.flush();
        Assert.assertEquals("", baos.toString());
    }

    public void testWithException() {
        logger.setDebugEnable(true);
        logger.debug(new IllegalStateException());
        stream.flush();
        Assert.assertTrue(baos.toString().contains(IllegalStateException.class.getName()));
    }

}
