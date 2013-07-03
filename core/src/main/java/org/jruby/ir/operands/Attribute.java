package org.jruby.ir.operands;

import org.jruby.ir.transformations.inlining.InlinerInfo;

import java.util.List;
import java.util.Map;

// Attribute represents some fact discovered during dataflow analysis.
//
// The attribute makes explicit the dataflow value which is normally stored and
// carried around in external tables.  This lets us capture path-specific information
// for a variable.  For example, the type of a variable can be different along two
// different paths.  By adding an attribute along each path, we convert the path-specific
// state into a path-independent state constant which lets us analyze this via standard
// constant propagation algorithms like SCCP.
//
// Example: v = BOXED_FIXNUM(n)
//          v = HAS_TYPE(Fixnum)
//
public class Attribute extends Operand
{
    Operand        _target;    // The operand that this attribute targets
//    DEFERRED
//    AttributeValue _val;       // Attribute value

    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force)
    {
/*
        _target = _target.getSimplifiedOperand(valueMap);
        return _target;
*/
        throw new RuntimeException("Unused & not implemented yet!");
    }

    public void addUsedVariables(List<Variable> l) { _target.addUsedVariables(l); }

    public Operand cloneForInlining(InlinerInfo ii) { throw new RuntimeException("Unused & not implemented yet!"); }
}
