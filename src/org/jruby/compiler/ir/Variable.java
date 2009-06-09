package org.jruby.compiler.ir;

public class Variable extends Operand
{
	final public String _name;

	public Variable(String n) { _name = n; }
}
