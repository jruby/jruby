package org.jruby.util;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.ext.stringio.StringIO;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;

public class TestIOChannel {
    Ruby runtime;

    @Before
    public void setup() {
        runtime = Ruby.newInstance();
        runtime.evalScriptlet("class MyO; def write(str); @str = str; str.length; end; end");
    }

    @Test
    public void testBufferReuse() throws Exception {
        ThreadContext context = runtime.getCurrentContext();
        IRubyObject myo = runtime.getClass("MyO").newInstance(context, Block.NULL_BLOCK);
        IOChannel.IOWritableByteChannel ioc = new IOChannel.IOWritableByteChannel(myo);
        ByteBuffer buf = ByteBuffer.wrap(new byte[] {(byte) 'f', (byte) 'o', (byte) 'o'});
        ioc.write(buf);

        IRubyObject written = myo.getInstanceVariables().getInstanceVariable("@str");
        Assert.assertEquals("foo", written.toString());
        buf.put(0, (byte)'b');
        Assert.assertEquals("foo", written.toString());
    }
}
