package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.Map;

import org.jruby.compiler.ir.IRClass;
import org.jruby.compiler.ir.representations.InlinerInfo;

// Represents a hash { _ =>_, _ => _ .. } in ruby
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this hash could get converted to calls
// that actually build the hash
public class Hash extends Operand
{
    final public List<KeyValuePair> _pairs;

    public Hash(List<KeyValuePair> pairs) { _pairs = pairs; }

    public boolean isBlank() { return _pairs == null || _pairs.size() == 0; }

    public boolean isConstant() 
    {
       for (KeyValuePair kp: _pairs)
          if (!kp._key.isConstant() || !kp._value.isConstant())
             return false;

       return true;
    }

    public boolean isNonAtomicValue() { return true; }

    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap)
    {
        int i = 0;
        for (KeyValuePair kv: _pairs) {
           kv._key   = kv._key.getSimplifiedOperand(valueMap);
           kv._value = kv._value.getSimplifiedOperand(valueMap);
           i++;
        }

        return this;
    }

    public IRClass getTargetClass() { return IRClass.getCoreClass("Hash"); }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l)
    {
        for (KeyValuePair kv: _pairs) {
            kv._key.addUsedVariables(l);
            kv._value.addUsedVariables(l);
        }
    }

    public Operand cloneForInlining(InlinerInfo ii) { 
        if (isConstant()) {
            return this;
        }
        else {
            List<KeyValuePair> newPairs = new java.util.ArrayList<KeyValuePair>();
            for (KeyValuePair kv: _pairs) {
                newPairs.add(new KeyValuePair(kv._key.cloneForInlining(ii), kv._value.cloneForInlining(ii)));
            }
            return new Hash(newPairs);
        }
    }
}
