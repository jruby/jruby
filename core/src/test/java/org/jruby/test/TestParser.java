package org.jruby.test;

import org.jruby.exceptions.RaiseException;

public class TestParser extends Base {
    
    public void testWarningLineNumber() throws Exception {
        String out;
        String script = "def foo; END {}; end";
        out = eval(script);
        assertTrue(out.indexOf("test:1") != -1);
    }
      
    public void testErrorLineNumber() throws Exception {
        String script = "String.new 'a \n" +
                        "p 'b'\n";
        try {
            eval(script);
            fail("should have raised an exception");
        } catch (RaiseException re) {
            assertTrue(re.toString().indexOf("test:2") != -1);
        }
    }

}
