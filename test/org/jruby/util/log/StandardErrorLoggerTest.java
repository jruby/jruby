package org.jruby.util.log;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class StandardErrorLoggerTest {

    private ByteArrayOutputStream baos;
    private StandardErrorLogger logger;
    private PrintStream stream;

    @Before
    public void setup() {
        baos = new ByteArrayOutputStream();
        stream = new PrintStream(baos);
        logger = new StandardErrorLogger("test", stream);
    }

    @After
    public void tearDown() throws IOException {
        baos.reset();
        baos.close();
    }


    @Test
    public void withDebuggingDisabled() {
        logger.setDebugEnable(false);
        logger.debug("test");
        stream.flush();
        Assert.assertEquals("", baos.toString());
    }

}
