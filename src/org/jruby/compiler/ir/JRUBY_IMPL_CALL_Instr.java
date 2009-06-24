package org.jruby.compiler.ir;

public class JRUBY_IMPL_CALL_Instr extends CALL_Instr
{
    public JRUBY_IMPL_CALL_Instr(Variable result, Operand methAddr, Operand[] args)
    {
        super(result, methAddr, args, null);
    }
   
    public JRUBY_IMPL_CALL_Instr(Variable result, Operand methAddr, Operand[] args, Operand closure)
    {
        super(result, methAddr, args, closure);
    }
   
    public boolean isStaticCallTarget()  { return true; }
}
