package org.jruby.compiler.ir.operands;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class GlobalVariable extends LocalVariable {
    public GlobalVariable(String n) { 
        super(n);
    }

    public Operand cloneForInlining(InlinerInfo ii) { 
       return this;
    }
}
