package org.jruby.ir;

import org.jruby.parser.StaticScope;

public class IRModuleBody extends IRScope {
    public IRModuleBody(IRManager manager, IRScope lexicalParent, String name, int lineNumber, StaticScope staticScope) {
        super(manager, lexicalParent, name, lineNumber, staticScope);

        if (!getManager().isDryRun()) {
            if (staticScope != null) staticScope.setIRScope(this);
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
}
