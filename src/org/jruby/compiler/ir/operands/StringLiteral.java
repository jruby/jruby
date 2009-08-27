package org.jruby.compiler.ir.operands;

import org.jruby.util.ByteList;
import org.jruby.compiler.ir.IR_Class;

// SSS FIXME: Pick one of bytelist or string, or add internal conversion methods to convert to the default representation
public class StringLiteral extends Constant
{
    final public ByteList _bl_value;
    final public String   _str_value;

    public StringLiteral(ByteList val) { _bl_value = val; _str_value = _bl_value.toString(); }
    public StringLiteral(String s) { _bl_value = ByteList.create(s); _str_value = s; }

    public String toString() {
        return "\"" + _str_value + "\"";
    }

    public IR_Class getTargetClass() { return IR_Class.getCoreClass("String"); }
}
