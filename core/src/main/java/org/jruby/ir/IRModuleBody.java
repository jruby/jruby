package org.jruby.ir;

import org.jruby.parser.StaticScope;

public class IRModuleBody extends IRScope {
    private CodeVersion version;    // Current code version for this module

    public IRModuleBody(IRManager manager, IRScope lexicalParent, String name, int lineNumber, StaticScope staticScope) {
        super(manager, lexicalParent, name, lineNumber, staticScope);

        if (!getManager().isDryRun()) {
            updateVersion();
            if (staticScope != null) staticScope.setIRScope(this);
        }
    }

    @Override
    public int getNearestModuleReferencingScopeDepth() {
        return 0;
    }

    public void updateVersion() {
        version = CodeVersion.getClassVersionToken();
    }

    @Override
    public IRScopeType getScopeType() {
        return IRScopeType.MODULE_BODY;
     }

    public CodeVersion getVersion() {
        return version;
    }

    @Override
    public boolean isModuleBody() {
        return true;
    }
}
