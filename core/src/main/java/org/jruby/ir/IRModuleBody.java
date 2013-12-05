package org.jruby.ir;

import org.jruby.ir.operands.LocalVariable;
import org.jruby.parser.IRStaticScope;
import org.jruby.parser.StaticScope;

public class IRModuleBody extends IRScope {
    private CodeVersion version;    // Current code version for this module

    public IRModuleBody(IRManager manager, IRScope lexicalParent, String name, int lineNumber, StaticScope scope) {
        this(manager, lexicalParent, name, lexicalParent.getFileName(), lineNumber, scope);
    }

    public IRModuleBody(IRManager manager, IRScope lexicalParent, String name,
            String fileName, int lineNumber, StaticScope scope) {
        super(manager, lexicalParent, name, fileName, lineNumber, scope);

        if (!getManager().isDryRun()) {
            if (scope != null) ((IRStaticScope)scope).setIRScope(this);
            updateVersion();
        }
    }

    @Override
    public IRScope getNearestModuleReferencingScope() {
        return this;
    }

    public void updateVersion() {
        version = CodeVersion.getClassVersionToken();
    }

    public String getScopeName() {
        return "ModuleBody";
    }

    public CodeVersion getVersion() {
        return version;
    }

    @Override
    public LocalVariable getImplicitBlockArg() {
        assert false: "A module/class/metaclass body never accepts block args";

        return null;
    }

    @Override
    public boolean isModuleBody() {
        return true;
    }
}
