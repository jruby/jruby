package org.jruby.test;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;

public class MockRubyObject extends RubyObject {

	private final Ruby runtime;

	private static class TestMeta extends RubyClass {

		protected TestMeta(Ruby runtime) {
            // This null doesn't feel right
			super(runtime, runtime.getObject(), null);
		}
	}
	
	public MockRubyObject(Ruby runtime) {
		super(runtime, new TestMeta(runtime));
		this.runtime = runtime;
	}
	
	public Ruby getRuntime() {
		return runtime;
	}
	
	public static void throwException(IRubyObject recv, Block block) {
		throw new RuntimeException("x");
	}

}
