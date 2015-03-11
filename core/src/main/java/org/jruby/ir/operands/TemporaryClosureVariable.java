package org.jruby.ir.operands;

import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

public class TemporaryClosureVariable extends TemporaryLocalVariable {
    private final int closureId;

    public TemporaryClosureVariable(int closureId, int offset) {
        super("%cl_" + closureId + "_" + offset, offset);

        this.closureId = closureId;
    }

    public int getClosureId() {
        return closureId;
    }

    @Override
    public TemporaryVariableType getType() {
        return TemporaryVariableType.CLOSURE;
    }

    @Override
    public Variable clone(SimpleCloneInfo ii) {
        return this;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(closureId);
    }

    public static TemporaryClosureVariable decode(IRReaderDecoder d) {
        return new TemporaryClosureVariable(d.decodeInt(), d.decodeInt());
    }

    @Override
    public String getPrefix() {
        return "%cl_" + closureId + "_";
    }
}
