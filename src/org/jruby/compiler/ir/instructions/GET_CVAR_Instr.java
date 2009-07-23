package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IR_Class;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

public class GET_CVAR_Instr extends GET_Instr
{
    public GET_CVAR_Instr(Variable dest, Operand scope, String varName)
    {
        super(Operation.GET_CVAR, dest, getParentmostScope(scope), varName);
    }

    public static Operand getParentmostScope(Operand scope) {
        // Walk up the scope tree right now as much as possible, to avoid run-time walking
        // SSS FIXME: Any reason why this might break in the presence of ruby's dynamic resolution?  What might break?
        while ((scope instanceof MetaObject) && !(((MetaObject)scope)._scope instanceof IR_Class))
            scope = ((MetaObject)scope)._scope.getParent();

        return scope;
    }
}
