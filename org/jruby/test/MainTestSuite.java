package org.jruby.test;

import junit.framework.*;

/**
*
* @author chadfowler
*/
public class MainTestSuite extends TestSuite {

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(org.jruby.test.TestRubyNil.class));
        suite.addTest(new TestSuite(org.jruby.test.TestRubyHash.class));
        return suite;
    }
}
