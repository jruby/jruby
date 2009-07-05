package org.jruby.compiler.ir;

import org.jruby.util.ByteList;

// SSS FIXME: Pick one of bytelist or string, or add interal conversion methods to convert to the default representation
public class StringLiteral extends Constant
{
    final public ByteList _bl_value;
    final public String   _str_value;

    public StringLiteral(ByteList val) { _bl_value = val; }
    public StringLiteral(String s) { _str_value = s; }
}
