package org.jruby.compiler.ir;

public abstract class Constant extends Operand
{
    public boolean isConstant() { return true; }
}
