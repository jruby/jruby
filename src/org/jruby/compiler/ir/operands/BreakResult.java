package org.jruby.compiler.ir.operands;

public class BreakResult extends Operand
{
    final public Operand _result;
    final public Label   _jumpTarget;

    public BreakResult(Operand v, Label l) { _result = v; _jumpTarget = l; }

    public String toString() { return "BRK(" + _result + ", " + _jumpTarget + ")"; }
}
