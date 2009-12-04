package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.BooleanLiteral;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Nil;
import org.jruby.compiler.ir.operands.Variable;

//    is_true(a) = (!a.nil? && a != false) 
//
// Only nil and false compute to false
//
public class IS_TRUE_Instr extends OneOperandInstr
{
    public IS_TRUE_Instr(Variable result, Operand arg)
    {
        super(Operation.IS_TRUE, result, arg);
    }

    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap)
    {
        simplifyOperands(valueMap);
        if (_arg.isConstant()) {
            return (_arg == Nil.NIL || _arg == BooleanLiteral.FALSE) ? BooleanLiteral.FALSE : BooleanLiteral.TRUE;
        }
        else {
            return null;
        }
    }
}
