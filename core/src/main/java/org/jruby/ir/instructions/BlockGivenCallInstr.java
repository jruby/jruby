package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;

import java.util.Objects;

/**
 * A call to block_given? which can be optimized like defined?(yield) or a regular call.
 */
public class BlockGivenCallInstr extends OneOperandResultBaseInstr implements FixedArityInstr {
    private final FunctionalCachingCallSite blockGivenSite;

    public BlockGivenCallInstr(Variable result, Operand block, String methodName) {
        super(Operation.BLOCK_GIVEN_CALL, Objects.requireNonNull(result, "BlockGivenCallInstr result is null"), block);

        this.blockGivenSite =
                new FunctionalCachingCallSite(Objects.requireNonNull(methodName, "BlockGivenCallInstr methodName is null"));
    }

    public Operand getBlockArg() {
        return getOperand1();
    }

    public String getMethodName() {
        return blockGivenSite.getMethodName();
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BlockGivenCallInstr(ii.getRenamedVariable(result), getBlockArg().cloneForInlining(ii), blockGivenSite.getMethodName());
    }

    public static BlockGivenCallInstr decode(IRReaderDecoder d) {
        return new BlockGivenCallInstr(d.decodeVariable(), d.decodeOperand(), d.decodeString());
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(blockGivenSite.getMethodName());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Object blk = getBlockArg().retrieve(context, self, currScope, currDynScope, temp);

        return IRRuntimeHelpers.blockGivenOrCall(context, self, blockGivenSite, blk);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BlockGivenCallInstr(this);
    }
}
