package org.jruby.compiler.ir.operands;
import org.jruby.compiler.ir.representations.InlinerInfo;

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
}
