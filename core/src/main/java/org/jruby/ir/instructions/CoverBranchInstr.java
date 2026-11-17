package org.jruby.ir.instructions;

import org.jruby.ext.coverage.BranchTarget;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.runtime.ThreadContext;

/**
 * Counts that execution reached a branch target for Coverage's branches mode. Emitted at the start of each
 * arm of a Ruby-level conditional, loop body, when/in clause, or safe-navigation call while branches are being
 * measured; it holds a direct reference to the target's counter (JIT-compiled code finds the counter again
 * through the file name and target index, see CoverageSite).
 */
public class CoverBranchInstr extends NoOperandInstr implements FixedArityInstr {
    private final BranchTarget target;
    private final String file;
    private final int index;

    public CoverBranchInstr(BranchTarget target, String file, int index) {
        super(Operation.COVER_BRANCH);

        this.target = target;
        this.file = file;
        this.index = index;
    }

    public BranchTarget getTarget() {
        return target;
    }

    public String getFile() {
        return file;
    }

    public int getIndex() {
        return index;
    }

    public void cover(ThreadContext context) {
        if (target != null) target.cover(context);
    }

    @Override
    public Instr clone(CloneInfo info) {
        return new CoverBranchInstr(target, file, index);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(file);
        e.encode(index);
    }

    public static CoverBranchInstr decode(IRReaderDecoder d) {
        // persisted IR carries no live counter; the JIT re-resolves it by file and index
        return new CoverBranchInstr(null, d.decodeString(), d.decodeInt());
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "target: " + target };
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.CoverBranchInstr(this);
    }
}
