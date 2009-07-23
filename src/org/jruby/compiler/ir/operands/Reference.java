package org.jruby.compiler.ir.operands;

public class Reference extends Operand
{
    final public String _refName;

    public Reference(String n) { _refName = n; }

    public String toString() { return _refName; }
}
