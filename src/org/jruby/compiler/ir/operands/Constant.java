package org.jruby.compiler.ir.operands;

public abstract class Constant extends Operand
{
    public boolean isConstant() { return true; }
}
