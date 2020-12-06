package org.jruby.internal.runtime;

import org.jruby.runtime.builtin.IRubyObject;

public class SplitSuperState<T extends InternalSplitState>
{
	public final IRubyObject callArrayArgs;
	public final T state;
	
	public SplitSuperState(IRubyObject result, T state)
	{
		callArrayArgs = result;
		this.state = state;
	}
}
