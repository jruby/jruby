package org.jruby.compiler.ir.operands;

import org.jruby.interpreter.InterpreterContext;
import org.jruby.util.ByteList;
import org.jruby.compiler.ir.IRClass;
import org.jruby.RubyString;

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

    // SSS: Yes, this is non-atomic because you cannot create multiple copies of the string-literal by propagating it.
    // Because of being able to define singleton methods on strings, "abc" != "abc" for 2 separate instances of the
    // string literal.  This is similar to the java equality / intern issues with non-atomic objects
    //
    // Here is an example in Ruby:
    // 
    //    a1 = "abc"
    //    a2 = "abc"
    //    class < a1; def bar; puts 'a1'; end; end
    //    class < a2; def bar; puts 'a2'; end; end
    //    a2.bar != a1.bar
    //
    // Hence, I cannot value-propagate "abc" during optimizations
    @Override
    public boolean isNonAtomicValue() { 
        return true;
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        // ENEBO: This is not only used for full RubyStrings, but also for bytelist retrieval....extra wrapping
        // return interp.getRuntime().newString(_bl_value); 
        // SSS FIXME: AST interpreter passes in a coderange argument.
        return RubyString.newStringShared(interp.getRuntime(), _bl_value);
    }
}
