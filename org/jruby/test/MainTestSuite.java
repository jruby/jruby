package org.jruby.test;

import junit.framework.*;

/**
*
* @author chadfowler
*/
public class MainTestSuite extends TestSuite {

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(TestRubyObject.class));
        suite.addTest(new TestSuite(TestRubyNil.class));
        suite.addTest(new TestSuite(org.jruby.test.TestRubyHash.class));
        suite.addTest(new TestSuite(org.jruby.test.TestRubyTime.class));
	suite.addTest(new TestSuite(org.jruby.test.TestRuby.class));
	suite.addTest(new TestSuite(org.jruby.scanner.test.TestStringToken.class));
        return suite;
    }
}
