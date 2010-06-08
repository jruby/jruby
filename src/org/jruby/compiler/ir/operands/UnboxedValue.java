package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.Map;

import org.jruby.compiler.ir.IRClass;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class UnboxedValue extends Operand
{
    final public Operand _value;

    public UnboxedValue(Operand v) { _value = v; }

    public String toString() { return "unboxed(" + _value + ")"; }

    public boolean isConstant() { return _value.isConstant(); }

    public boolean isNonAtomicValue() { return _value.isNonAtomicValue(); }

    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap) {
        Operand v = _value.getSimplifiedOperand(valueMap);
        return (v == _value) ? this 
		                       : (v instanceof BoxedValue) ? ((BoxedValue)v)._value : new UnboxedValue(v);
    }

    public void addUsedVariables(List<Variable> l) {
        _value.addUsedVariables(l);
    }

    public Operand cloneForInlining(InlinerInfo ii) {
        return new UnboxedValue(_value.cloneForInlining(ii));
    }
}
