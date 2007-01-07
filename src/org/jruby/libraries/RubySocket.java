package org.jruby.libraries;

import org.jruby.IRuby;
import org.jruby.RubyBasicSocket;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class RubySocket extends RubyBasicSocket {

	public RubySocket(IRuby runtime, RubyClass metaClass) {
		super(runtime, metaClass);
	}

}
