package org.jruby.compiler.ir;

// Represents a range (1..5) or (a..b) in ruby code
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, this range operand could get converted to calls
// that actually build the Range object
public class Range extends Operand
{
    final public Operand _begin;
    final public Operand _end;

    public Range(Operand b, Operand e) { _begin = b; _end = e; }
}
