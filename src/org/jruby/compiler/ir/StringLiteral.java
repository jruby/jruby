package org.jruby.compiler.ir;

public class StringLiteral extends Constant
{
    final public ByteList _value;

    public StringLiteral(ByteList val) { _value = val; }
}
