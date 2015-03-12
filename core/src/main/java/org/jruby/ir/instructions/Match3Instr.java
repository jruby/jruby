package org.jruby.ir.instructions;

import org.jruby.RubyRegexp;
import org.jruby.ir.IRScope;
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

import static org.jruby.ir.IRFlags.USES_BACKREF_OR_LASTLINE;

public class Match3Instr extends ResultBaseInstr implements FixedArityInstr {
    public Match3Instr(Variable result, Operand receiver, Operand arg) {
        super(Operation.MATCH3, result, new Operand[] { receiver, arg });

        assert result != null: "Match3Instr result is null";
    }

    public Operand getArg() {
        return operands[1];
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
        return new Match3Instr((Variable) result.cloneForInlining(ii),
                getReceiver().cloneForInlining(ii), getArg().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getReceiver());
        e.encode(getArg());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        RubyRegexp regexp = (RubyRegexp) getReceiver().retrieve(context, self, currScope, currDynScope, temp);
        IRubyObject argValue = (IRubyObject) getArg().retrieve(context, self, currScope, currDynScope, temp);

        return IRRuntimeHelpers.match3(context, regexp, argValue);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Match3Instr(this);
    }
}
