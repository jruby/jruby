package org.jruby.compiler.ir;

public class Label extends Operand
{
    public final String _label;

    public Label(String l) { _label = l; }
}
