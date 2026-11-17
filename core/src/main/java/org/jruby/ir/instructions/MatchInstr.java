package org.jruby.ir.instructions;

import org.jruby.RubyInstanceConfig;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.NullBlock;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.runtime.CallType;
import org.jruby.util.ByteList;

import java.util.EnumSet;

import static org.jruby.ir.IRFlags.REQUIRES_BACKREF;

public class MatchInstr extends CallInstr implements FixedArityInstr {
    private static final ByteList MATCH = new ByteList(new byte[] {'=', '~'});

    // normal constructor
    public MatchInstr(IRScope scope, Variable result, Operand receiver, Operand arg) {
        super(scope, Operation.MATCH, CallType.NORMAL, result, scope.getManager().getRuntime().newSymbol(MATCH),
                receiver, new Operand[]{arg}, NullBlock.INSTANCE, 0, false);

        assert result != null : "Match2Instr result is null";
    }

    @Override
    public boolean computeScopeFlags(IRScope scope, EnumSet<IRFlags> flags) {
        super.computeScopeFlags(scope, flags);
        // $~ is implicitly used since Backref and NthRef operands
        // access it and $~ is not made explicit in those operands.
        flags.add(REQUIRES_BACKREF);
        return true;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new MatchInstr(ii.getScope(), (Variable) result.cloneForInlining(ii), getReceiver().cloneForInlining(ii),
                getArg1().cloneForInlining(ii));
    }

    // We do not call super here to bypass having to pass this exaclty like a call.
    @Override
    public void encode(IRWriterEncoder e) {
        if (RubyInstanceConfig.IR_WRITING_DEBUG) System.out.println("Instr(" + getOperation() + "): " + this);

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
