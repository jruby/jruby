package org.jruby.compiler.ir.operands;

import java.util.List;
import org.jruby.compiler.ir.representations.InlinerInfo;

// Represents the StandardError object -- this operand used in rescue blocks
// for when the rescue block doesn't specify an exception object class
public class StandardError extends Operand {
    public String toString() { return "StandardError"; }

    @Override
    public void addUsedVariables(List<Variable> l) { 
        /* Nothing to do */
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) {
        return this;
    }
}
