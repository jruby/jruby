package org.jruby.ir.instructions;

import org.jruby.RubyRegexp;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.ir.IRFlags.USES_BACKREF_OR_LASTLINE;

public class MatchInstr extends ResultBaseInstr implements FixedArityInstr {
    public MatchInstr(Variable result, Operand receiver) {
        super(Operation.MATCH, result, new Operand[] { receiver });

        assert result != null: "MatchInstr result is null";
    }

    public Operand getReceiver() {
        return operands[0];
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        // $~ is implicitly used since Backref and NthRef operands
        // access it and $~ is not made explicit in those operands.
        scope.getFlags().add(USES_BACKREF_OR_LASTLINE);
        return true;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new MatchInstr((Variable) result.cloneForInlining(ii), getReceiver().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getReceiver());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        RubyRegexp regexp = (RubyRegexp) getReceiver().retrieve(context, self, currScope, currDynScope, temp);
        return regexp.op_match2_19(context);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.MatchInstr(this);
    }
}
