package org.jruby.util.log;

import junit.framework.TestCase;
import org.junit.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;


public class ParameterizedWriterTest extends TestCase {

    private ByteArrayOutputStream baos;
    private PrintStream stream;
    private ParameterizedWriter writer;

    public void setUp() throws Exception {
        baos = new ByteArrayOutputStream();
        stream = new PrintStream(baos);
        writer = new ParameterizedWriter(stream);
    }

    public void tearDown() throws IOException {
        baos.reset();
        baos.close();
    }

    public void testWithNoPlaceholder() {
        writer.write("test");
        stream.flush();
        Assert.assertEquals("test\n", baos.toString());
    }

    public void testWithJustOnePlaceholder() {
        writer.write("{}", "a");
        stream.flush();
        Assert.assertEquals("a\n", baos.toString());
    }

    public void testWithOnePlaceholder() {
        writer.write("a {}", "test");
        stream.flush();
        Assert.assertEquals("a test\n", baos.toString());
    }

    public void testWithTwoPlaceholders() {
        writer.write("{} and {}", "test1", "test2");
        stream.flush();
        Assert.assertEquals("test1 and test2\n", baos.toString());
    }
}
