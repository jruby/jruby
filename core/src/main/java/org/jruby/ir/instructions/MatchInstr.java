package org.jruby.ir.instructions;

import org.jruby.RubyRegexp;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.NormalCachingCallSite;

import static org.jruby.ir.IRFlags.USES_BACKREF_OR_LASTLINE;

public class MatchInstr extends CallBase implements FixedArityInstr, ResultInstr {
    protected Variable result;

    public MatchInstr(Variable result, Operand receiver, Operand arg) {
        super(Operation.MATCH, CallType.NORMAL, "=~", receiver, new Operand[]{arg}, null, false);

        assert result != null : "Match2Instr result is null";

        this.result = result;
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
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
        return new MatchInstr((Variable) result.cloneForInlining(ii),
                getReceiver().cloneForInlining(ii), getArg1().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getReceiver());
        e.encode(getArg1());
    }

    public static MatchInstr decode(IRReaderDecoder d) {
        return new MatchInstr(d.decodeVariable(), d.decodeOperand(), d.decodeOperand());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.MatchInstr(this);
    }
}
