package org.jruby.test;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Access.objectClass;

public class MockRubyObject extends RubyObject {

	private static class TestMeta extends RubyClass {
		protected TestMeta(Ruby runtime) {
			super(runtime, objectClass(runtime.getCurrentContext()), runtime.isObjectSpaceEnabled());
		}
	}
	
	public MockRubyObject(Ruby runtime) {
		super(runtime, new TestMeta(runtime));
	}
	
	public static void throwException(IRubyObject recv, Block block) {
		throw new RuntimeException("x");
	}

}
