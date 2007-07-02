package org.jruby.test;

import org.jruby.Ruby;

public class TestRbConfigLibrary extends TestRubyBase {

    protected void setUp() throws Exception {
        super.setUp();
        runtime = Ruby.getDefaultInstance();
    }
    
    public void testConfigTargetOs() throws Exception {
        String script = 
            "require 'rbconfig'\n" +
            "p Config::CONFIG['target_os']";
        if (System.getProperty("os.name").compareTo("Mac OS X") == 0) {
            assertTrue(eval(script).contains("darwin"));
        }
    }
    

}
