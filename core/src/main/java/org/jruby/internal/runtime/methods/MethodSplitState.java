package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.internal.runtime.InternalSplitState;
import org.jruby.ir.interpreter.*;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;

class MethodSplitState implements InternalSplitState
{
	public final ExitableInterpreterContext eic;
	public final ExitableInterpreterEngineState state;
	public final ThreadContext context;
	public final DynamicScope scope;
	public final RubyModule implClass;
	public final IRubyObject self;
	public final String name;

	public MethodSplitState(ThreadContext context, ExitableInterpreterContext ic, RubyModule clazz, IRubyObject self, String name)
	{
		this.context = context; // TODO: metascope?
		this.eic = ic;
		this.state = ic.getEngineState();
		this.scope = DynamicScope.newDynamicScope(ic.getStaticScope());
		this.implClass = clazz;
		this.self = self;
		this.name = name;
	}
}
