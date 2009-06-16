package org.jruby.compiler.ir;

public class Range extends Operand
{
	final public Operand _begin;
	final public Operand _end;

	public Range(Operand b, Operand e) { _begin = b; _end = e; }
}
