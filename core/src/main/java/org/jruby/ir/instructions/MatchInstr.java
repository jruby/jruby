package org.jruby.ir.instructions;

import org.jruby.RubyRegexp;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

import static org.jruby.ir.IRFlags.USES_BACKREF_OR_LASTLINE;

public class MatchInstr extends Instr implements ResultInstr, FixedArityInstr {
    private Variable result;
    private Operand receiver;

    public MatchInstr(Variable result, Operand receiver) {
        super(Operation.MATCH);

        assert result != null: "MatchInstr result is null";

        this.result = result;
        this.receiver = receiver;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { receiver };
    }

    public Operand getReceiver() {
        return receiver;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + receiver + ")";
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(USES_BACKREF_OR_LASTLINE);
        return true;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        receiver = receiver.getSimplifiedOperand(valueMap, force);
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new MatchInstr((Variable) result.cloneForInlining(ii), receiver.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        RubyRegexp regexp = (RubyRegexp) receiver.retrieve(context, self, currDynScope, temp);
        return regexp.op_match2_19(context);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.MatchInstr(this);
    }
}
