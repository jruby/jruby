package org.jruby.test;
import junit.framework.TestCase;
import org.jruby.NativeException;
import org.jruby.Ruby;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Tests that a Java Exception can be thrown from Java through JRuby and
 * caught again by Java (when unwrapped).
 * 
 * Designed to test JRUBY-2652
 * 
 * @author Rick Moynihan rick@calicojack.co.uk
 */
public class TestJavaReentrantExceptions extends TestCase {
    private Ruby runtime;

    public TestJavaReentrantExceptions(String name) {
        super(name);
    }

    @Override
    public void setUp() {
        runtime = Ruby.newInstance();
    }

    public void testExceptionsAcrossTheBridge() {
        final IRubyObject wrappedThrower = JavaEmbedUtils.javaToRuby(runtime, new ExceptionThrower());
        boolean exceptionThrown = false;
        try {
            //call throwException via JRuby
            JavaEmbedUtils.invokeMethod(runtime, wrappedThrower, "throwException", new Object[] { }, Object.class);
        } catch(final RaiseException e) {
            exceptionThrown = true;
            final NativeException ne = (NativeException) e.getException();
            final ExpectedException ee = (ExpectedException) ne.getCause();
            assertEquals("The unpacked exception we receive should be the one we threw.",
                         ExceptionThrower.expectedException,ee);
        } finally {
            assertTrue("Java Exception should have been thrown and wrapped as a RaiseException",exceptionThrown);
        }
    }
}
class ExpectedException extends Exception {
    
}

class ExceptionThrower {
    public static final ExpectedException expectedException = new ExpectedException();
    
    public void throwException() throws ExpectedException {
        throw expectedException;
    }
}