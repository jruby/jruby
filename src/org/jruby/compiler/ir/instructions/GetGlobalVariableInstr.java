package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.GlobalVariable;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class GetGlobalVariableInstr extends GetInstr {
    public GetGlobalVariableInstr(Variable dest, String gvarName) {
        super(Operation.GET_GLOBAL_VAR, dest, new GlobalVariable(gvarName), null);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new GetGlobalVariableInstr(ii.getRenamedVariable(getResult()), ((GlobalVariable)getSource()).getName());
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        return getSource().retrieve(context, self, currDynScope, temp);
    }
}
