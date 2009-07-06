package org.jruby.compiler.ir;

public class CALL_Instr extends MultiOperandInstr
{
    Operand _methAddr;
    Operand _closure;
   
    public CALL_Instr(Variable result, Operand methAddr, Operand[] args, Operand closure)
    {
        super(Operation.CALL, result, args);
        _methAddr = methAddr;
        _closure = closure;
    }
   
    public boolean isRubyInternalsCall() { return false; }
    public boolean isStaticCallTarget()   { return false; }

    public String toString() {
        return   "\t" 
		         + (_result == null ? "" : _result + " = ") 
		         + _op + "(" + _methAddr + ", " + java.util.Arrays.toString(_args) + ")"
					+ (_closure == null ? "" : ", closure: " + _closure);
    }
}
