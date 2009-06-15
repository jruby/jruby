package org.jruby.compiler.ir;

public class Fixnum extends Constant
{
	final public Long _value;

	public Fixnum(Long val) { _value = val; }
}
