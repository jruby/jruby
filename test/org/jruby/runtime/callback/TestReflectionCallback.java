package org.jruby.runtime.callback;

import junit.framework.TestCase;

import org.jruby.Ruby;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.test.MockRubyObject;

public class TestReflectionCallback extends TestCase {

	public void testExecuteWithStaticMethodThrowingException() throws Exception {
		ReflectionCallback callback = new ReflectionCallback(MockRubyObject.class, "throwException", 
				new Class[0], false, true, Arity.noArguments());
		try {
			callback.execute(new MockRubyObject(Ruby.getDefaultInstance()), new IRubyObject[0]);
		} catch (RaiseException e) {
			assertEquals(RuntimeException.class, e.getCause().getClass());
		}
		
	}
}
