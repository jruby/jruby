package org.jruby.compiler.ir.operands;

// Represents a $1 .. $9 node in Ruby code
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, it could get converted to calls
//
public class NthRef extends Operand
{
    final public int _matchNumber; 

    public NthRef(int n) { _matchNumber = n; }

    public String toString() { return "$" + _matchNumber; }
}
