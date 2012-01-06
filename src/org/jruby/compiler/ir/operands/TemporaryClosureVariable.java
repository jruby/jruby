package org.jruby.compiler.ir.operands;

/**
 */
public class TemporaryClosureVariable extends TemporaryVariable {
    final int closureId;
    final String prefix;

    public TemporaryClosureVariable(int closureId, int offset) {
        super(offset);
        this.closureId = closureId;
        this.prefix =  "%cl_" + closureId + "_";
        this.name = getPrefix() + offset;
    }

    @Override
    public String getPrefix() {
        return this.prefix;
    }
}
