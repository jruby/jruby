package org.jruby.compiler.ir.operands;

import org.jruby.interpreter.InterpreterContext;
import org.jruby.util.ByteList;
import org.jruby.compiler.ir.IRClass;

public class StringLiteral extends Constant
{
// SSS FIXME: Pick one of bytelist or string, or add internal conversion methods to convert to the default representation
// SSS: Get rid of _bl_value since it is not needed anymore

    final public ByteList _bl_value;
    final public String   _str_value;

    public StringLiteral(ByteList val) { _bl_value = val; _str_value = _bl_value.toString(); }
    public StringLiteral(String s) { _bl_value = ByteList.create(s); _str_value = s; }

    @Override
    public String toString() {
        return "\"" + _str_value + "\"";
    }

    @Override
    public IRClass getTargetClass() {
        return IRClass.getCoreClass("String");
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        // ENEBO: This is not only used for full RubyStrings, but also for bytelist retrieval....extra wrapping
        // return interp.getRuntime().newString(_bl_value); // SSS: Looks like newString just points to _bl_value rather than cloning it?
		  return interp.getRuntime().newString(_str_value);
    }
}
