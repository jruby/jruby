package org.jruby.compiler.ir.operands;

import java.util.List;
import org.jruby.compiler.ir.targets.JVM;
import org.jruby.util.ByteList;
import org.jruby.RubyString;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class StringLiteral extends Operand {
    // SSS FIXME: Pick one of bytelist or string, or add internal conversion methods to convert to the default representation

    final public ByteList bytelist;
    final public String   string;

    public StringLiteral(ByteList val) {
        bytelist = val;
        string = bytelist.toString();
    }
    
    public StringLiteral(String s) {
        bytelist = ByteList.create(s); string = s;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public void addUsedVariables(List<Variable> l) {
        /* Do nothing */
    }    

    @Override
    public String toString() {
        return "\"" + string + "\"";
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
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        // SSS FIXME: AST interpreter passes in a coderange argument.
        return RubyString.newStringShared(context.getRuntime(), bytelist);
    }

    @Override
    public void compile(JVM jvm) {
        jvm.method().push(bytelist);
    }
}
