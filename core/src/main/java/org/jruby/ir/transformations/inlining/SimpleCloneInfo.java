package org.jruby.ir.transformations.inlining;

import org.jruby.ir.IRScope;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Variable;

/**
 * Context info for simple cloning operation.
 */
public class SimpleCloneInfo extends CloneInfo {
    private boolean isEnsureBlock;
    private boolean cloneIPC = false;

    public SimpleCloneInfo(IRScope scope, boolean isEnsureBlock, boolean cloneIPC) {
        this(scope, isEnsureBlock);

        this.cloneIPC = cloneIPC;

    }

    public SimpleCloneInfo(IRScope scope, boolean isEnsureBlock) {
        super(scope);

        this.isEnsureBlock = isEnsureBlock;
    }

    public boolean shouldCloneIPC() {
        return cloneIPC;
    }

    public boolean isEnsureBlockCloneMode() {
        return this.isEnsureBlock;
    }

    public Variable getRenamedVariable(Variable variable) {
        Variable renamed = super.getRenamedVariable(variable);

        // FIXME: I don't understand how this case can possibly exist.  If it does a qualitative comment should be added here.
        if (variable instanceof LocalVariable && !((LocalVariable) variable).isSameDepth((LocalVariable) renamed)) {
            return ((LocalVariable) renamed).cloneForDepth(((LocalVariable) variable).getScopeDepth());
        }

        return renamed;
    }

    protected Label getRenamedLabelSimple(Label l) {
        // In ensure-block-clone mode, no cloning of labels not already pre-renamed and initialized
        // FIXME: IRScope.java:prepareInstructionsForInterpretation/Compilation assumes that
        // multiple labels with the same name are identical java objects. So, reuse the object here.
        return isEnsureBlock ? l : l.clone();
    }

    public Variable getRenamedSelfVariable(Variable self) {
        return self;
    }

    protected Variable getRenamedVariableSimple(Variable v) {
        return v.clone(this);
    }

    // Unconditional renaming of labels -- used to initialize ensure region cloning
    public void renameLabel(Label l) {
        labelRenameMap.put(l, getScope().getNewLabel());
    }
}
