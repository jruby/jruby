package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.IR_Class;
import org.jruby.compiler.ir.IR_Method;

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
    public boolean isStaticCallTarget()  { return getTargetMethod() != null; }
       // Can this call lead to ruby code getting modified?  
    public boolean canModifyCode()       { IR_Method m = getTargetMethod(); return (m == null) ? true : m.modifiesCode(); }

    public Operand[] getCallArgs() { return _args; }
    public Operand   getMethodAddr() { return _methAddr; }
    public Operand   getClosureArg() { return _closure; }

    public IR_Method getTargetMethod()
    {
        if (!(_methAddr instanceof MethAddr))
           return null;

            // Fetch class of receiver
        Operand  receiver = _args[0];
        IR_Class c;
        if ((receiver instanceof Variable) && ((Variable)receiver).isSelf()) {
            c = null; // SSS FIXME
        }
        else {
            c = receiver.getTargetClass();
        }

            // Fetch method from the class
        if (c != null) {
            return c.getMethod(((MethAddr)_methAddr).getName());
        }
        else {
            return null;
        }
    }

    public String toString() {
        return   "\t" 
               + (_result == null ? "" : _result + " = ") 
               + _op + "(" + _methAddr + ", " + java.util.Arrays.toString(_args) + ")"
               + (_closure == null ? "" : ", closure: " + _closure);
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap)
    {
        super.simplifyOperands(valueMap);
        _methAddr = _methAddr.getSimplifiedOperand(valueMap);
        if (_closure != null)
            _closure = _closure.getSimplifiedOperand(valueMap);
    }
}
