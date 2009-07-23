package org.jruby.compiler.ir.operands;

// Represents a regexp from ruby
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, this regexp operand could get converted to calls
// that actually build the Regexp object
public class Regexp extends Operand
{
    final public Operand _re;
    final public int     _opts;

    public Regexp(Operand re, int opts) { _re = re; _opts = opts; }

    public boolean isConstant() { return _re.isConstant(); }

    public String toString() { return "RE:|" + _re + "|" + _opts; }
}
