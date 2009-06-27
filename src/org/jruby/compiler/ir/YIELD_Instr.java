package org.jruby.compiler.ir;

public class YIELD_Instr extends MultiOperandInstr
{
    // SSS FIXME: Correct?  Where does closure arg come from?
    public YIELD_Instr(Variable result, Operand[] args)
    {
        super(Operation.YIELD, result, args);
    }
   
    public boolean isRubyInternalsCall() { return false; }
    public boolean isStaticCallTarget()  { return false; }
}
