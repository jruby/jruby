package org.jruby.compiler.ir;

// This is of the form:
//   v = OP(arg, attribute_array); Ex: v = NOT(v1)

public class OneOperandInstr extends IR_Instr
{
    public final Operand _arg;

    public OneOperandInstr(Operation op, Variable dest, Operand arg)
    {
        super(op, dest);
        _arg = arg;
    }

    public String toString() { return super.toString() + "(" + _arg + ")"; }
}
