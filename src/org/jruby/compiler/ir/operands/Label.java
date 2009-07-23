package org.jruby.compiler.ir.operands;

public class Label extends Operand
{
    public final String _label;

    public static int index = 0;

    public Label(String l) { _label = l; }

    public String toString() {
        return _label;
    }
}
