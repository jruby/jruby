package org.jruby.ir.instructions;

import java.util.Map;
import org.jruby.RubyNil;
import org.jruby.RubyProc;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class BlockGivenInstr extends Instr implements ResultInstr {
    private Variable result;
    private Operand blockArg;

    public BlockGivenInstr(Variable result, Operand block) {
        super(Operation.BLOCK_GIVEN);

        assert result != null: "BlockGivenInstr result is null";

        this.result = result;
        this.blockArg = block;
    }

    public Operand[] getOperands() {
        return new Operand[]{blockArg};
    }

    public Variable getResult() {
        return result;
    }

    public Operand getBlockArg() {
        return blockArg;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        blockArg = blockArg.getSimplifiedOperand(valueMap, force);
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new BlockGivenInstr(ii.getRenamedVariable(result), blockArg.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Object blk = (Object) blockArg.retrieve(context, self, currDynScope, temp);
        if (blk instanceof RubyProc) blk = ((RubyProc)blk).getBlock();
        if (blk instanceof RubyNil) blk = Block.NULL_BLOCK;
        Block b = (Block)blk;
        return context.runtime.newBoolean(b.isGiven());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BlockGivenInstr(this);
    }
}
