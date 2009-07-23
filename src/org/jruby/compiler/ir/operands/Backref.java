package org.jruby.compiler.ir.operands;

// Represents a backref node in Ruby code
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, it could get converted to calls
//
public class Backref extends Operand
{
    final public char _type; 

    public Backref(char t) { _type = t; }

    public String toString() { return "$" + _type; }
}
