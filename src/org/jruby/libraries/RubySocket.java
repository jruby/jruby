package org.jruby.libraries;

import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.internal.runtime.methods.DirectInvocationMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class RubySocket extends RubyObject {
    public static abstract class SocketMethod extends DirectInvocationMethod {
        public SocketMethod(RubyModule implementationClass, Arity arity, Visibility visibility) {
            super(implementationClass, arity, visibility);
        }
        
        public IRubyObject internalCall(ThreadContext context, IRubyObject receiver, RubyModule lastClass, String name, IRubyObject[] args, boolean noSuper) {
            RubySocket s = (RubySocket)receiver;
            
            return invoke(s, args);
        }
        
        public abstract IRubyObject invoke(RubySocket target, IRubyObject[] args);
    };

	public RubySocket(IRuby runtime, RubyClass metaClass) {
		super(runtime, metaClass);
	}

}
