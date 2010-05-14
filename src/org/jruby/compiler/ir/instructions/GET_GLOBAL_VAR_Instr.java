package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.GlobalVariable;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class GET_GLOBAL_VAR_Instr extends GET_Instr
{
    public GET_GLOBAL_VAR_Instr(Variable dest, String gvarName) {
        super(Operation.GET_GLOBAL_VAR, dest, new GlobalVariable(gvarName), null);
    }

    public IR_Instr cloneForInlining(InlinerInfo ii) {
        return new GET_GLOBAL_VAR_Instr(ii.getRenamedVariable(_result), ((GlobalVariable)_source).name);
    }
}
