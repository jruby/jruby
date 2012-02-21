package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.representations.InlinerInfo;

public class TemporaryClosureVariable extends TemporaryVariable {
    final int closureId;
    final String prefix;

    public TemporaryClosureVariable(int closureId, int offset) {
        super(offset);
        this.closureId = closureId;
        this.prefix =  "%cl_" + closureId + "_";
        this.name = getPrefix() + offset;
    }

    public TemporaryClosureVariable(String name, int offset) {
        super(name, offset);
        this.closureId = -1;
        this.prefix = "";
    }

    @Override
    public Variable cloneForCloningClosure(InlinerInfo ii) {
        return new TemporaryClosureVariable(name, offset);
    }

    @Override
    protected String getPrefix() {
        return this.prefix;
    }
}
