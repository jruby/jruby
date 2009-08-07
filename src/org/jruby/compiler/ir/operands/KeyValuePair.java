package org.jruby.compiler.ir.operands;

public class KeyValuePair
{
    Operand _key;
    Operand _value;

    public KeyValuePair(Operand k, Operand v) { _key = k; _value = v; }
}
