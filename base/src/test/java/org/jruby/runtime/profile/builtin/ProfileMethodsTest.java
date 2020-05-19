package org.jruby.runtime.profile.builtin;

import junit.framework.TestCase;
import org.jruby.Ruby;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.test.MockRubyObject;

/**
 * @author Andre Kullmann
 */
public class ProfileMethodsTest extends TestCase {

    public ProfileMethodsTest(final String testName) {
        super(testName);
    }

    public void testSimplePutAndGetWorks() {

        Ruby runtime = Ruby.getGlobalRuntime();
        DynamicMethod method = runtime.getString().getMethods().values().iterator().next();
        ProfiledMethods methods = new ProfiledMethods( runtime );
        methods.addProfiledMethod( "doSomething", method );

        ProfiledMethod pMethod = methods.getProfiledMethod(method.getSerialNumber());

        assertNotNull( pMethod );
        assertEquals( "doSomething", pMethod.getName() );
        assertEquals( method.getSerialNumber(), pMethod.getMethod().getSerialNumber() );
    }

    public void testNothingFoundReturnsNull() {

        Ruby runtime = Ruby.getGlobalRuntime();
        DynamicMethod method = runtime.getString().getMethods().values().iterator().next();
        ProfiledMethods methods = new ProfiledMethods( runtime );
        methods.addProfiledMethod( "doSomething", method );

        ProfiledMethod pMethod = methods.getProfiledMethod( method.getSerialNumber() + 1 );

        assertNull( pMethod );
    }
}
