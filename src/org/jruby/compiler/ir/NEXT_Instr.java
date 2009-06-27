package org.jruby.compiler.ir;

// A next instruction could be implemented with a regular closure return when in closures
// But, outside closures (for, while), it is a jump to the top of the loop
//
public class NEXT_Instr extends OneOperandInstr
{
    Operand _retval;

    public NEXT_Instr(Operand rv)
    {
        super(Operation.NEXT, null, _retval);
    }
}
