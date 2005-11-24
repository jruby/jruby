package org.jruby.test;

import org.jruby.IRuby;
import org.jruby.javasupport.JavaSupport;

public class MockJavaSupport extends JavaSupport {

	public MockJavaSupport(IRuby ruby) {
		super(ruby);
	}

}
