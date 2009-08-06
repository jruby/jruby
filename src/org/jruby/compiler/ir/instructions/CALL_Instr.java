package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

public class CALL_Instr extends MultiOperandInstr
{
    public Operand _methAddr;
    public Operand _closure;
   
    public CALL_Instr(Variable result, Operand methAddr, Operand[] args, Operand closure)
    {
        super(Operation.CALL, result, args);
        _methAddr = methAddr;
        _closure = closure;
    }
   
    public CALL_Instr(Operation op, Variable result, Operand methAddr, Operand[] args, Operand closure)
    {
        super(op, result, args);
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
