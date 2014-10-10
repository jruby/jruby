package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

public class BlockGivenInstr extends Instr implements ResultInstr, FixedArityInstr {
    private Variable result;
    private Operand blockArg;

    public BlockGivenInstr(Variable result, Operand block) {
        super(Operation.BLOCK_GIVEN);

        assert result != null: "BlockGivenInstr result is null";

        this.result = result;
        this.blockArg = block;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[]{blockArg};
    }

    @Override
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

    @Override
    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BlockGivenInstr(ii.getRenamedVariable(result), blockArg.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Object blk = (Object) blockArg.retrieve(context, self, currScope, currDynScope, temp);

        return IRRuntimeHelpers.isBlockGiven(context, blk);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BlockGivenInstr(this);
    }
}
