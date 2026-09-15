package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Count one call of the running method for Coverage's methods mode. The operand is the
 * {@link org.jruby.ext.coverage.MethodCoverage} counter taken by {@link ReceiveMethodCoverageInstr}. It is
 * nil or null when the scope is not running as a counted method, for example a block called as a block.
 *
 * <p>Emitted right after the instructions that receive the arguments. That is where MRI fires the CALL event,
 * so a call that fails while receiving arguments is not counted.</p>
 */
public class CoverMethodInstr extends OneOperandInstr implements FixedArityInstr {
    public CoverMethodInstr(Operand coverage) {
        super(Operation.COVER_METHOD, coverage);
    }

    public Operand getCoverage() {
        return getOperand1();
    }

    @Override
    public Instr clone(CloneInfo info) {
        return new CoverMethodInstr(getCoverage().cloneForInlining(info));
    }

    public static CoverMethodInstr decode(IRReaderDecoder d) {
        return new CoverMethodInstr(d.decodeOperand());
    }

    public void cover(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRRuntimeHelpers.coverMethod(context, currScope, getCoverage().retrieve(context, self, currScope, currDynScope, temp));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.CoverMethodInstr(this);
    }
}
