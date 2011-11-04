package org.jruby.compiler.ir.operands;
import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class GlobalVariable extends Operand {
    final public String name;
    public GlobalVariable(String name) {
        super();

        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int compareTo(Object arg0) {
        // ENEBO: what should compareTo when it is not comparable?
        if (!(arg0 instanceof GlobalVariable)) return 0;

        return name.compareTo(((GlobalVariable) arg0).name);
    }
    
    @Interp
    @Override
    public Object retrieve(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        return context.getRuntime().getGlobalVariables().get(getName());
    }

    @Interp
    @Override
    public Object store(InterpreterContext interp, ThreadContext context, IRubyObject self, Object value) {
        return context.getRuntime().getGlobalVariables().set(getName(), (IRubyObject) value);
    }

    @Override
    public String toString() {
        return name;
    }
}
