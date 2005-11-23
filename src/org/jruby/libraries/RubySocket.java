package org.jruby.libraries;

import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.internal.runtime.methods.DirectInvocationMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class RubySocket extends RubyObject {

	public RubySocket(IRuby runtime, RubyClass metaClass) {
		super(runtime, metaClass);
	}

}
