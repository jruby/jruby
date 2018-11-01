package org.jruby.ir.instructions;

import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.runtime.CallType;
import org.jruby.util.ByteList;

import static org.jruby.ir.IRFlags.REQUIRES_BACKREF;

public class MatchInstr extends CallInstr implements FixedArityInstr {
    private static final ByteList MATCH = new ByteList(new byte[] {'=', '~'});

    public MatchInstr(IRScope scope, Variable result, Operand receiver, Operand arg) {
        super(scope, Operation.MATCH, CallType.NORMAL, result, scope.getManager().getRuntime().newSymbol(MATCH), receiver, new Operand[]{arg}, null, false);

        assert result != null : "Match2Instr result is null";
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        super.computeScopeFlags(scope);
        // $~ is implicitly used since Backref and NthRef operands
        // access it and $~ is not made explicit in those operands.
        scope.getFlags().add(REQUIRES_BACKREF);
        return true;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new MatchInstr(ii.getScope(), (Variable) result.cloneForInlining(ii),
                getReceiver().cloneForInlining(ii), getArg1().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        e.encode(getOperation());
        e.encode(getResult());
        e.encode(getReceiver());
        e.encode(getArg1());
    }

    public static MatchInstr decode(IRReaderDecoder d) {
        return new MatchInstr(d.getCurrentScope(), d.decodeVariable(), d.decodeOperand(), d.decodeOperand());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.MatchInstr(this);
    }
}
