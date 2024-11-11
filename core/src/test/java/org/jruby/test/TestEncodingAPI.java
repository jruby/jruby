package org.jruby.test;

import junit.framework.TestCase;
import org.jcodings.specific.SJISEncoding;
import org.jcodings.specific.UTF8Encoding;

import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.io.EncodingUtils;

import static org.jruby.api.Create.newString;

public class TestEncodingAPI extends TestCase {
    private ThreadContext context;

    public TestEncodingAPI(String name) {
        super(name);
    }

    public void setUp() {
        context= Ruby.newInstance().getCurrentContext();
    }

    public void testStrConvEncThatGrows() throws Exception {
        String javaStr = "--- こんにちは！";
        RubyString rubyStr = newString(context, javaStr);
        rubyStr = EncodingUtils.strConvEnc(context, rubyStr, rubyStr.getEncoding(), SJISEncoding.INSTANCE);
        assertEquals(rubyStr.getEncoding(), SJISEncoding.INSTANCE);
        rubyStr = EncodingUtils.strConvEnc(context, rubyStr, SJISEncoding.INSTANCE, UTF8Encoding.INSTANCE);
        assertEquals(rubyStr.getEncoding(), UTF8Encoding.INSTANCE);
    }
}
