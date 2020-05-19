package org.jruby.ir.transformations.inlining;

import org.jruby.ir.IRScope;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Variable;

/**
 * Context info for simple cloning operation.
 */
public class SimpleCloneInfo extends CloneInfo {
    private final boolean isEnsureBlock;
    private boolean cloneIPC;

    public SimpleCloneInfo(IRScope scope, boolean isEnsureBlock, boolean cloneIPC) {
        super(scope);

        this.isEnsureBlock = isEnsureBlock;
    }

    public SimpleCloneInfo(IRScope scope, boolean isEnsureBlock) {
        this(scope, isEnsureBlock, false);
    }

    public boolean isEnsureBlockCloneMode() {
        return this.isEnsureBlock;
    }

    public boolean shouldCloneIPC() {
        return cloneIPC;
    }

    public Variable getRenamedVariable(Variable variable) {
        Variable renamed = super.getRenamedVariable(variable);

        // WrappedIRClosure operands in ensure blocks processing will cloneForInlining and inherit the top-scopes
        // SimpleCloneInfo instance which will already contain a depth 0 version of this "local" variable.  We are
        // in a closure in early stages of cloning it around anywhere it should be for ensure (we copy same ensure
        // bodies to all possible exit paths) We know closures containing lvars will have the wrong depth.
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
