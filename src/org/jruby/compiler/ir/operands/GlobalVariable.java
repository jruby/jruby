package org.jruby.compiler.ir.operands;
import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class GlobalVariable extends Variable {
    final public String name;
    public GlobalVariable(String name) {
        super();

        this.name = name;
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) { 
       return this;
    }

    @Override
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
    public Object retrieve(InterpreterContext interp) {
        return interp.getRuntime().getGlobalVariables().get(getName());
    }

    @Interp
    @Override
    public Object store(InterpreterContext interp, Object value) {
        return interp.getRuntime().getGlobalVariables().set(getName(), (IRubyObject) value);
    }
}
