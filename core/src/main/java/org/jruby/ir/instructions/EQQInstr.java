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

// If v2 is an array, compare v1 with every element of v2 and stop on first match!
public class EQQInstr extends ResultBaseInstr implements FixedArityInstr {
    public EQQInstr(Variable result, Operand v1, Operand v2) {
        super(Operation.EQQ, result, new Operand[] {v1, v2});

        assert result != null: "EQQInstr result is null";
    }

    public Operand getArg1() {
        return operands[0];
    }

    public Operand getArg2() {
        return operands[1];
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new EQQInstr(ii.getRenamedVariable(result), getArg1().cloneForInlining(ii), getArg2().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getArg1());
        e.encode(getArg2());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        return IRRuntimeHelpers.isEQQ(context,
                (IRubyObject) getArg1().retrieve(context, self, currScope, currDynScope, temp),
                (IRubyObject) getArg2().retrieve(context, self, currScope, currDynScope, temp));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.EQQInstr(this);
    }
}
