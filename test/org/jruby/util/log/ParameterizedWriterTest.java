package org.jruby.util.log;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;


public class ParameterizedWriterTest {

    private ByteArrayOutputStream baos;
    private PrintStream stream;
    private ParameterizedWriter writer;

    @Before
    public void setUp() throws Exception {
        baos = new ByteArrayOutputStream();
        stream = new PrintStream(baos);
        writer = new ParameterizedWriter(stream);
    }

    @After
    public void tearDown() throws IOException {
        baos.reset();
        baos.close();
    }

    @Test
    public void withNoPlaceholder() {
        writer.write("test");
        stream.flush();
        Assert.assertEquals("test\n", baos.toString());
    }

    @Test
    public void withJustOnePlaceholder() {
        writer.write("{}", "a");
        stream.flush();
        Assert.assertEquals("a\n", baos.toString());
    }

    @Test
    public void withOnePlaceholder() {
        writer.write("a {}", "test");
        stream.flush();
        Assert.assertEquals("a test\n", baos.toString());
    }

    @Test
    public void withTwoPlaceholders() {
        writer.write("{} and {}", "test1", "test2");
        stream.flush();
        Assert.assertEquals("test1 and test2\n", baos.toString());
    }
}
