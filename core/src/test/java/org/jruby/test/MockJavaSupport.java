package org.jruby.test;

import org.jruby.Ruby;
import org.jruby.javasupport.JavaSupport;
import org.jruby.javasupport.JavaSupportImpl;

public class MockJavaSupport extends JavaSupport {

	public MockJavaSupport(Ruby ruby) {
		super(ruby);
	}

}
