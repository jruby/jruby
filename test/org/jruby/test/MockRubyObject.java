package org.jruby.test;

import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.runtime.builtin.IRubyObject;

public class MockRubyObject extends RubyObject {

	private final IRuby runtime;

	private static class TestMeta extends RubyClass {

		protected TestMeta(IRuby runtime) {
			super(runtime, runtime.getObject());
		}
	}
	
	public MockRubyObject(IRuby runtime) {
		super(runtime, new TestMeta(runtime));
		this.runtime = runtime;
	}
	
	public IRuby getRuntime() {
		return runtime;
	}
	
	public static void throwException(IRubyObject recv) {
		throw new RuntimeException("x");
	}

}
