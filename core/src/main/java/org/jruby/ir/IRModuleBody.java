package org.jruby.ir;

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
