package org.jruby.ir.instructions;

import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/* Receive the closure argument (either implicit or explicit in Ruby source code) */
public class ReifyClosureInstr extends ResultBaseInstr implements FixedArityInstr {
    public ReifyClosureInstr(Variable result, Variable source) {
        super(Operation.REIFY_CLOSURE, result, new Operand[] { source });

        assert result != null: "ReceiveClosureInstr result is null";
    }

    public Variable getSource() {
        return (Variable) operands[0];
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(IRFlags.RECEIVES_CLOSURE_ARG);
        return true;
    }

    @Override
    public Instr clone(CloneInfo info) {
        if (info instanceof SimpleCloneInfo) return new ReifyClosureInstr(info.getRenamedVariable(getResult()), info.getRenamedVariable(getSource()));

        // SSS FIXME: This code below is for inlining and is untested.

        InlineCloneInfo ii = (InlineCloneInfo) info;

        // SSS FIXME: This is not strictly correct -- we have to wrap the block into an
        // operand type that converts the static code block to a proc which is a closure.
        if (ii.getCallClosure() instanceof WrappedIRClosure) return NopInstr.NOP;

        return new CopyInstr(ii.getRenamedVariable(result), ii.getCallClosure());
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getSource());
        e.encode(getResult());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Block block = (Block) getSource().retrieve(context, self, currScope, currDynScope, temp);
        return IRRuntimeHelpers.newProc(context.runtime, block);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReifyClosureInstr(this);
    }
}
