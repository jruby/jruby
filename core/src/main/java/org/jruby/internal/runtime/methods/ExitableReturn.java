package org.jruby.internal.runtime.methods;

import org.jruby.RubyArray;
import org.jruby.runtime.Block;

public class ExitableReturn
{
	public final RubyArray<?> arguments;
	public final Block block;

	public ExitableReturn(RubyArray<?> arguments, Block block)
	{
		this.arguments = arguments;
		this.block = block;
	}

	public RubyArray<?> getArguments()
	{
		return arguments;
	}

	public Block getBlock()
	{
		return block;
	}
	
}
