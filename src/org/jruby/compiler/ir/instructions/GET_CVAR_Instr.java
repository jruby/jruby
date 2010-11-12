package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class GET_CVAR_Instr extends GetInstr {
    public GET_CVAR_Instr(Variable dest, Operand scope, String varName) {
        super(Operation.GET_CVAR, dest, scope, varName);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new GET_CVAR_Instr(ii.getRenamedVariable(result), getSource().cloneForInlining(ii), getName());
    }
}
