package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class PUT_CVAR_Instr extends PutInstr {
    public PUT_CVAR_Instr(Operand scope, String varName, Operand value) {
        super(Operation.PUT_CVAR, getParentmostScope(scope), varName, value);
    }

    public static Operand getParentmostScope(Operand scope) {
        // Walk up the scope tree right now as much as possible, to avoid run-time walking
        // SSS FIXME: Any reason why this might break in the presence of ruby's dynamic resolution?  What might break?
        while ((scope instanceof MetaObject) && !(((MetaObject)scope).isClass())) {
            scope = ((MetaObject)scope).getContainer();
        }

        return scope;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new PUT_CVAR_Instr(operands[TARGET].cloneForInlining(ii), ref, operands[VALUE].cloneForInlining(ii));
    }
}
