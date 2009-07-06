package org.jruby.compiler.ir;

public class ArgIndex extends Operand
{
    final public int _index;

    public ArgIndex(int n) { _index = n; }

    public String toString() { return Integer.toString(_index); }
}
