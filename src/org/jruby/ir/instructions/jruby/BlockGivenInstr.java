package org.jruby.ir.instructions.jruby;

import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.representations.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class BlockGivenInstr extends Instr implements ResultInstr {
    private Variable result;
    
    public BlockGivenInstr(Variable result) {
        super(Operation.BLOCK_GIVEN);
        
        assert result != null: "BlockGivenInstr result is null";
        
        this.result = result;
    }

    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }
    
    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new BlockGivenInstr(ii.getRenamedVariable(result));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        return context.getRuntime().newBoolean(block.isGiven());
    }
}
