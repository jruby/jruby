package org.jruby.compiler.ir.operands;

import org.jruby.runtime.builtin.IRubyObject;

public abstract class Constant extends Operand
{
    public boolean isConstant() { return true; }

	 // Cache value during interpretation (to prevent useless rebuilding of this constant over and over again)
	 // May need to be cleared if the corresponding Ruby object is modified to add methods to it
	 protected IRubyObject cachedValue = null;
}
