package org.jruby.ir.instructions;

import org.jruby.RubyRange;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

// Represents a range (1..5) or (a..b) in ruby code
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, this range operand could get converted to calls
// that actually build the BuildRangeInstr object
public class BuildRangeInstr extends Instr implements ResultInstr {
    private Variable result;
    private Operand begin;
    private Operand end;
    private boolean exclusive;

    public BuildRangeInstr(Variable result, Operand begin, Operand end, boolean exclusive) {
        super(Operation.BUILD_RANGE);

        this.begin = begin;
        this.end = end;
        this.exclusive = exclusive;
        this.result = result;
    }

    @Override
    public Variable getResult() {
        return result;
    }

    @Override
    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { begin, end };
    }

    public Operand getBegin() {
        return begin;
    }

    public Operand getEnd() {
        return end;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    @Override
    public String toString() {
        return result + " = " + begin + (exclusive ? ".." : "...") + end;
    }

// ---------- These methods below are used during compile-time optimizations -------
    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        begin = begin.getSimplifiedOperand(valueMap, force);
        end= end.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BuildRangeInstr(ii.getRenamedVariable(result), begin.cloneForInlining(ii), end.cloneForInlining(ii), exclusive);
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        return RubyRange.newRange(context.runtime, context,
                (IRubyObject) begin.retrieve(context, self, currScope, currDynScope, temp),
                (IRubyObject) end.retrieve(context, self, currScope, currDynScope, temp), exclusive);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BuildRangeInstr(this);
    }
}
