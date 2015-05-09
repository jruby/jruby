package org.jruby.ir.instructions;

// This is of the form:
//   d = s

import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;

import java.util.Map;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;

public class CopyInstr extends ResultBaseInstr implements FixedArityInstr {
    public CopyInstr(Operation op, Variable result, Operand source) {
        super(op, result, new Operand[] { source });
    }

    public CopyInstr(Variable result, Operand source) {
        this(Operation.COPY, result, source);
    }

    public Operand getSource() {
        return operands[0];
    }

    @Override
    public Operand simplifyAndGetResult(IRScope scope, Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap, false);

        return getSource();
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new CopyInstr(getOperation(), ii.getRenamedVariable(result), getSource().cloneForInlining(ii));
    }

    public static CopyInstr decode(IRReaderDecoder d) {
        return new CopyInstr(d.decodeVariable(), d.decodeOperand());
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getSource());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.CopyInstr(this);
    }
}
