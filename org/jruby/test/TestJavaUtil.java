package org.jruby.test;

import junit.framework.*;
import org.jruby.*;
import org.jruby.javasupport.*;
import org.jruby.regexp.*;

public class TestJavaUtil extends TestCase {
    private Ruby ruby;

    public TestJavaUtil(String name) {
        super(name);
    }

    public void setUp() {
        ruby = Ruby.getDefaultInstance(GNURegexpAdapter.class);
    }
    
    public void testConvertJavaToRuby() {
        // assertEquals(JavaUtil.convertJavaToRuby(ruby, null, Object.class).type().toName(), "NilClass");
        assertEquals(JavaUtil.convertJavaToRuby(ruby, new Integer(1000), Integer.TYPE).type().toName(), "Fixnum");
        assertEquals(JavaUtil.convertJavaToRuby(ruby, new Double(1.0), Double.TYPE).type().toName(), "Float");
        assertEquals(JavaUtil.convertJavaToRuby(ruby, Boolean.TRUE, Boolean.TYPE).type().toName(), "TrueClass");
        assertEquals(JavaUtil.convertJavaToRuby(ruby, Boolean.FALSE, Boolean.TYPE).type().toName(), "TrueClass");
        assertEquals(JavaUtil.convertJavaToRuby(ruby, "AString", String.class).type().toName(), "String");
    }
}

