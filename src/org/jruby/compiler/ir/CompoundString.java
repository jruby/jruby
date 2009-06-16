package org.jruby.compiler.ir;

// This represents a compound string in Ruby
// Ex: - "Hi " + "there"
//     - "Hi #{name}"
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this string operand could get converted to calls
// that appends the components of the compound string into a single string object
public class CompoundString extends Operand
{
	final public List<Operand> _pieces;

	public CompoundString(List<Operand> pieces) { _pieces = pieces; }
}
