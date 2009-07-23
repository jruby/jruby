package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IR_Class;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;

public class PUT_CVAR_Instr extends PUT_Instr
{
    public PUT_CVAR_Instr(Operand scope, String varName, Operand value)
    {
        super(Operation.PUT_CVAR, getParentmostScope(scope), varName, value);
    }

    public static Operand getParentmostScope(Operand scope) {
        // Walk up the scope tree right now as much as possible, to avoid run-time walking
        // SSS FIXME: Any reason why this might break in the presence of ruby's dynamic resolution?  What might break?
        while ((scope instanceof MetaObject) && !(((MetaObject)scope)._scope instanceof IR_Class))
            scope = ((MetaObject)scope)._scope.getParent();

        return scope;
    }
}
