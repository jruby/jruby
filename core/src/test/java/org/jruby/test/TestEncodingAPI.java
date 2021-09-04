package org.jruby.test;

import junit.framework.TestCase;
import org.jcodings.specific.SJISEncoding;
import org.jcodings.specific.UTF8Encoding;

import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.util.io.EncodingUtils;

public class TestEncodingAPI extends TestCase {
    private Ruby runtime;

    public TestEncodingAPI(String name) {
        super(name);
    }

    public void setUp() {
        runtime = Ruby.newInstance();
    }

    public void testStrConvEncThatGrows() throws Exception {
        String javaStr = "--- こんにちは！";
        RubyString rubyStr = RubyString.newString(runtime, javaStr);
        rubyStr = EncodingUtils.strConvEnc(runtime.getCurrentContext(), rubyStr, rubyStr.getEncoding(), SJISEncoding.INSTANCE);
        assertEquals(rubyStr.getEncoding(), SJISEncoding.INSTANCE);
        rubyStr = EncodingUtils.strConvEnc(runtime.getCurrentContext(), rubyStr, SJISEncoding.INSTANCE, UTF8Encoding.INSTANCE);
        assertEquals(rubyStr.getEncoding(), UTF8Encoding.INSTANCE);
    }
}
