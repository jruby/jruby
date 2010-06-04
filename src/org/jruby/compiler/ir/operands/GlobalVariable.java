package org.jruby.compiler.ir.operands;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class GlobalVariable extends LocalVariable {
    public GlobalVariable(String n) { 
        super(n);
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) { 
       return this;
    }
}
