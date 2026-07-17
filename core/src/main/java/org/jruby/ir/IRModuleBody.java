package org.jruby.ir;

import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.util.ByteList;

public class IRModuleBody extends IRScope {
    private final boolean executesOnce;

    public IRModuleBody(IRManager manager, IRScope lexicalParent, ByteList name, int lineNumber,
                        StaticScope staticScope, boolean executesOnce) {
        super(manager, lexicalParent, name, lineNumber, staticScope);

        this.executesOnce = executesOnce;

        if (staticScope != null) {
            staticScope.setIRScope(this);
        }
    }

    /**
     * Deep-copy this module/class body for a Proc#refined clone: a {@code class}/{@code module} (re)opened
     * inside a refined proc must see that proc's refinements, in the body and in any {@code def} it contains.
     * For ordinary inlining the shared original is returned unchanged.
     */
    public IRModuleBody cloneForInlining(CloneInfo ii) {
        if (!ii.isRefinementsClone()) return this;

        // Class/module bodies are built eagerly when their enclosing scope is built, so the IC is available here.
        return cloneScopeForRefinements(ii.getScope(), builtInterpreterContext(), this::cloneInstance);
    }

    /** Construct an empty copy of this body of the correct concrete type for a refinements clone. */
    protected IRModuleBody cloneInstance(IRScope lexicalParent, StaticScope scope) {
        return new IRModuleBody(getManager(), lexicalParent, getByteName(), getLine(), scope, executesOnce);
    }

    @Override
    public int getNearestModuleReferencingScopeDepth() {
        return 0;
    }

    @Override
    public IRScopeType getScopeType() {
        return IRScopeType.MODULE_BODY;
     }

    @Override
    public boolean isModuleBody() {
        return true;
    }

    @Override
    public void cleanupAfterExecution() {
        if (executesOnce && getClosures().isEmpty()) {
            interpreterContext = null;
            fullInterpreterContext = null;
            localVars = null;
        }
    }

    public boolean executesOnce() {
        return executesOnce;
    }
}
