package org.jruby.ir;

import org.jruby.ir.operands.LocalVariable;
import org.jruby.parser.StaticScope;
import org.jruby.parser.IRStaticScope;

public class IRModuleBody extends IRScope {
    private CodeVersion version;    // Current code version for this module

    public IRModuleBody(IRManager manager, IRScope lexicalParent, String name, int lineNumber, StaticScope scope) {
        this(manager, lexicalParent, name, lexicalParent.getFileName(), lineNumber, scope);
    }

    public IRModuleBody(IRManager manager, IRScope lexicalParent, String name,
            String fileName, int lineNumber, StaticScope staticScope) {
        super(manager, lexicalParent, name, fileName, lineNumber, staticScope);

        if (!getManager().isDryRun()) {
            updateVersion();
            if (staticScope != null) {
                ((IRStaticScope)staticScope).setIRScope(this);
                staticScope.setScopeType(this.getScopeType());
            }
        }
    }

    @Override
    public IRScope getNearestModuleReferencingScope() {
        return this;
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
