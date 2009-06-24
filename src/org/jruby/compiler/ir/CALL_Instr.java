package org.jruby.compiler.ir;

public class CALL_Instr extends MultiOperandInstr
{
    Operand _methAddr;
    Operand _closure;
   
    public CALL_Instr(Variable result, Operand methAddr, Operand[] args, Operand closure)
    {
        _methAddr = addr;
        _closure = closure;
        super(Operation.CALL, result, args);
    }
   
    public boolean isRubyInternalsCall() { return false; }
    public boolean isStaticCallTarget()   { return false; }
}
