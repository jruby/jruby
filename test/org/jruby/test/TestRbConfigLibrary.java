package org.jruby.test;

import org.jruby.Ruby;
import jnr.posix.util.Platform;

public class TestRbConfigLibrary extends TestRubyBase {

    protected void setUp() throws Exception {
        super.setUp();
        runtime = Ruby.newInstance();
    }
    
    public void testConfigTargetOs() throws Exception {
        String script = 
            "require 'rbconfig'\n" +
            "p RbConfig::CONFIG['target_os']";
        if (Platform.IS_MAC) {
            assertTrue(eval(script).indexOf("darwin") >= 0);
        }
    }
    

}
