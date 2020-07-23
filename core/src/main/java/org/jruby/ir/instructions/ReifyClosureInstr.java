package org.jruby.ir.instructions;

import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
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

import java.util.EnumSet;

/* Receive the closure argument (either implicit or explicit in Ruby source code) */
public class ReifyClosureInstr extends OneOperandResultBaseInstr implements FixedArityInstr {
    public ReifyClosureInstr(Variable result, Variable source) {
        super(Operation.REIFY_CLOSURE, result, source);

        assert result != null: "ReceiveClosureInstr result is null";
    }

    public Variable getSource() {
        return (Variable) getOperand1();
    }

    @Override
    public boolean computeScopeFlags(IRScope scope, EnumSet<IRFlags> flags) {
        scope.setReceivesClosureArg();
        return true;
    }

    @Override
    public Instr clone(CloneInfo info) {
        if (info instanceof SimpleCloneInfo) return new ReifyClosureInstr(info.getRenamedVariable(getResult()), info.getRenamedVariable(getSource()));

        InlineCloneInfo ii = (InlineCloneInfo) info;

        return new CopyInstr(ii.getRenamedVariable(result), ii.getRenamedVariable(getSource()));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getSource());
    }

    public static ReifyClosureInstr decode(IRReaderDecoder d) {
        return new ReifyClosureInstr(d.decodeVariable(), d.decodeVariable());
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
