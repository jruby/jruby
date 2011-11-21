package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.GlobalVariable;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PutGlobalVarInstr extends PutInstr {

    public PutGlobalVarInstr(String varName, Operand value) {
        super(Operation.PUT_GLOBAL_VAR, new GlobalVariable(varName), null, value);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new PutGlobalVarInstr(((GlobalVariable) operands[TARGET]).name, operands[VALUE].cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, IRubyObject self, Object[] temp, Block block) {
        getTarget().store(context, temp, getValue().retrieve(context, self, temp));
        return null;
    }
}
