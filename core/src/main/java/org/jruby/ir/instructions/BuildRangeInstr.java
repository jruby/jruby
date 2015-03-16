package org.jruby.ir.instructions;

import org.jruby.RubyRange;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// Represents a range (1..5) or (a..b) in ruby code
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, this range operand could get converted to calls
// that actually build the BuildRangeInstr object
public class BuildRangeInstr extends ResultBaseInstr {
    private boolean exclusive;

    public BuildRangeInstr(Variable result, Operand begin, Operand end, boolean exclusive) {
        super(Operation.BUILD_RANGE, result, new Operand[] { begin, end });

        this.exclusive = exclusive;
    }

    public Operand getBegin() {
        return operands[0];
    }

    public Operand getEnd() {
        return operands[1];
    }

    public boolean isExclusive() {
        return exclusive;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "type: " + (exclusive ? "exclusive" : "inclusive")};
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BuildRangeInstr(ii.getRenamedVariable(result), getBegin().cloneForInlining(ii),
                getEnd().cloneForInlining(ii), exclusive);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getBegin());
        e.encode(getEnd());
        e.encode(isExclusive());
    }

    public static BuildRangeInstr decode(IRReaderDecoder d) {
        return new BuildRangeInstr(d.decodeVariable(), d.decodeOperand(), d.decodeOperand(), d.decodeBoolean());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        return RubyRange.newRange(context,
                (IRubyObject) getBegin().retrieve(context, self, currScope, currDynScope, temp),
                (IRubyObject) getEnd().retrieve(context, self, currScope, currDynScope, temp), exclusive);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BuildRangeInstr(this);
    }
}
