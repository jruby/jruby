package org.jruby.internal.runtime;

import org.jruby.RubyArray;
import org.jruby.internal.runtime.methods.ExitableReturn;
import org.jruby.runtime.Block;

public class SplitSuperState<T extends InternalSplitState>
{
	public final RubyArray<?> callArrayArgs;
	public final Block callBlockArgs;
	public final T state;
	
	public SplitSuperState(ExitableReturn result, T state)
	{
		callArrayArgs = result.getArguments();
		callBlockArgs = result.getBlock();
		this.state = state;
	}
}
