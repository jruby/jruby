package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.Map;

import org.jruby.compiler.ir.IRClass;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class BoxedValue extends Operand
{
    final public Operand _value;

    public BoxedValue(Operand v) { _value = v; }

    public String toString() { return "boxed(" + _value + ")"; }

    public boolean isConstant() { return _value.isConstant(); }

    public boolean isNonAtomicValue() { return _value.isNonAtomicValue(); }

    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap) {
        Operand v = _value.getSimplifiedOperand(valueMap);
        return (v == _value) ? this 
		                       : (v instanceof UnboxedValue) ? ((UnboxedValue)v)._value : new BoxedValue(v);
    }

    public void addUsedVariables(List<Variable> l) {
        _value.addUsedVariables(l);
    }

    public Operand cloneForInlining(InlinerInfo ii) {
        return new BoxedValue(_value.cloneForInlining(ii));
    }
}
