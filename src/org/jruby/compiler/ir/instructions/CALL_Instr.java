package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.IR_Class;
import org.jruby.compiler.ir.IR_Module;
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
        // If we don't know what method we are calling, we assume it can (pessimistic, but safe!)
        // If we do know the target method, we ask the method itself whether it modifies ruby code
    public boolean canModifyCode()       { IR_Method m = getTargetMethod(); return (m == null) ? true : m.modifiesCode(); }

    public Operand[] getCallArgs()   { return _args; }
    public Operand   getMethodAddr() { return _methAddr; }
    public Operand   getClosureArg() { return _closure; }
    public Operand   getReceiver()   { return _args[0]; }

    public IR_Method getTargetMethodWithReceiver(Operand receiver)
    {
        if (!(_methAddr instanceof MethAddr))
           return null;

        if (receiver instanceof MetaObject) {
            IR_Module m = (IR_Module)(((MetaObject)receiver)._scope);
            return m.getClassMethod(((MethAddr)_methAddr).getName());
        }
/**
            // self.foo(..);
            // If this call instruction is in a class method, we'll fetch a class method
            // If this call instruction is in an instance method, we'll fetch an instance method
        else if ((receiver instanceof Variable) && ((Variable)receiver).isSelf()) {
            IR_Class c = null; // SSS FIXME
            return null;
        }
**/
        else {
            IR_Class c = receiver.getTargetClass();
            return (c == null) ? null : c.getInstanceMethod(((MethAddr)_methAddr).getName());
        }
    }

    public IR_Method getTargetMethod()
    {
        return getTargetMethodWithReceiver(getReceiver());
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
