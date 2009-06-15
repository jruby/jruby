package org.jruby.compiler.ir;

public class StringLiteral extends Constant
{
	final public String _value;

	public StringLiteral(String val) { _value = val; }
}
