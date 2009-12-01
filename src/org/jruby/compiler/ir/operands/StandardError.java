package org.jruby.compiler.ir.operands;

// Represents the StandardError object -- this operand used in rescue blocks
// for when the rescue block doesn't specify an exception object class
public class StandardError extends Operand
{
    public String toString() { return "StandardError"; }
}
