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
        suite.addTest(new TestSuite(TestRubyHash.class));
        suite.addTest(new TestSuite(TestRubyTime.class));
        suite.addTest(new TestSuite(TestRuby.class));
        suite.addTest(new TestSuite(TestJavaUtil.class));
        suite.addTest(new TestSuite(TestKernel.class));
        suite.addTest(new TestSuite(TestRubyCollect.class));
        return suite;
    }
}
