package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

public class BlockGivenInstr extends ResultBaseInstr implements FixedArityInstr {
    public BlockGivenInstr(Variable result, Operand block) {
        super(Operation.BLOCK_GIVEN, result, new Operand[] {block});

        assert result != null: "BlockGivenInstr result is null";
    }

    public Operand getBlockArg() {
        return operands[0];
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BlockGivenInstr(ii.getRenamedVariable(result), getBlockArg().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getBlockArg());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Object blk = getBlockArg().retrieve(context, self, currScope, currDynScope, temp);

        return IRRuntimeHelpers.isBlockGiven(context, blk);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BlockGivenInstr(this);
    }
}
