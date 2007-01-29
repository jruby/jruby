package org.jruby.libraries;

import org.jruby.IRuby;
import org.jruby.RubyBasicSocket;
import org.jruby.RubyClass;

public class RubySocket extends RubyBasicSocket {

	public RubySocket(IRuby runtime, RubyClass metaClass) {
		super(runtime, metaClass);
	}

}
