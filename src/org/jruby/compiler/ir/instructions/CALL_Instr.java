package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

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
   
    public CALL_Instr(Operation op, Variable result, Operand methAddr, Operand[] args, Operand closure)
    {
        super(op, result, args);
        _methAddr = methAddr;
        _closure = closure;
    }
   
    public boolean isRubyInternalsCall() { return false; }
    public boolean isStaticCallTarget()   { return false; }

    public Operand[] getCallArgs() { return _args; }
    public Operand   getMethodAddr() { return _methAddr; }
    public Operand   getClosureArg() { return _closure; }

    public String toString() {
        return   "\t" 
               + (_result == null ? "" : _result + " = ") 
               + _op + "(" + _methAddr + ", " + java.util.Arrays.toString(_args) + ")"
               + (_closure == null ? "" : ", closure: " + _closure);
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap)
    {
        super.simplifyOperands(valueMap);
        _methAddr = _methAddr.getSimplifiedValue(valueMap);
        if (_closure != null)
            _closure = _closure.getSimplifiedValue(valueMap);
    }
}
