package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;

/**
 * Take the {@link org.jruby.ext.coverage.MethodCoverage} counter, if any, that the calling DynamicMethod
 * passed through the ThreadContext (see DynamicMethod#prepareMethodCoverage).
 *
 * <p>Emitted as the first instruction of every method and block body built in methods mode. It runs before
 * the arguments are received, so a nested call made while receiving them (a default value, a to_ary
 * conversion) cannot take the counter first. {@link CoverMethodInstr} does the increment after the arguments
 * are in.</p>
 */
public class ReceiveMethodCoverageInstr extends NoOperandResultBaseInstr implements FixedArityInstr {
    public ReceiveMethodCoverageInstr(Variable result) {
        super(Operation.RECV_METHOD_COVERAGE, result);

        assert result != null : "ReceiveMethodCoverageInstr result is null";
    }

    @Override
    public Instr clone(CloneInfo info) {
        return new ReceiveMethodCoverageInstr(info.getRenamedVariable(result));
    }

    public static ReceiveMethodCoverageInstr decode(IRReaderDecoder d) {
        return new ReceiveMethodCoverageInstr(d.decodeVariable());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveMethodCoverageInstr(this);
    }
}
