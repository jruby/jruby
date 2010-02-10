package org.jruby.compiler.ir.operands;

/**
 */
public class TemporaryClosureVariable extends TemporaryVariable {
    final int closureId;

    public TemporaryClosureVariable(int closureId, int offset) {
        super(offset);

        this.closureId = closureId;
    }

    @Override
    public String getPrefix() {
        return "%cl_" + closureId + "_";
    }
}
