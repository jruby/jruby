package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.InlinerInfo;

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

    @Override
    public void visit(IRVisitor visitor) {
        visitor.TemporaryClosureVariable(this);
    }
}
