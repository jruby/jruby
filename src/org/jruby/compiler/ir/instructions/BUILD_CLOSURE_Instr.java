package org.jruby.compiler.ir.instructions;

// This is of the form:
//   d = <closure> { .. }

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.IR_Closure;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Variable;

public class BUILD_CLOSURE_Instr extends OneOperandInstr 
{
    public BUILD_CLOSURE_Instr(Variable d, IR_Closure c)
    {
        super(Operation.BUILD_CLOSURE, d, new MetaObject(c));
    }

    public String toString() { return "\t" + _result + " = " + _arg + " [CLOSURE]"; }

    public IR_Closure getClosure() { return (IR_Closure)(((MetaObject)_arg)._scope); }

    // SSS FIXME: Later on, we would probably implement simplifyOperands here and simplify the body of the closure .. something to think about.
}
