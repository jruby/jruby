package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.Map;

public class DynamicReference extends Operand
{
        // SSS FIXME: Should this be Operand or CompoundString?
        // Can it happen that symbols are built out of other than compound strings?  
        // Or can it happen during optimizations that this becomes a generic operand?
    CompoundString _refName;

    public DynamicReference(CompoundString n) { _refName = n; }

    public boolean isNonAtomicValue() { return true; }

    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap)
    { 
       _refName = (CompoundString)_refName.getSimplifiedOperand(valueMap);
       return this;
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l)
    {
        _refName.addUsedVariables(l);
    }
}
