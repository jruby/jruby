package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;

// Represents target.ref = value or target = value where target is not a stack variable
public abstract class PUT_Instr extends IR_Instr
{
    public final Operand _target;
    public final String  _ref;
    public final Operand _value;

    public PUT_Instr(Operation op, Operand target, String ref, Operand value)
    {
        super(op);
        _value = value;
        _target = target;
        _ref = ref;
    }

    public Operand[] getOperands() { return new Operand[] { _value, _target }; }

    public String toString() { return super.toString() + "(" + _target + (_ref == null ? "" : ", " + _ref) + ") = " + _value; }
}
