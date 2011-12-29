package org.jruby.compiler.ir;

import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.parser.StaticScope;
import org.jruby.parser.IRStaticScope;

public class IRModuleBody extends IRScope {
    private CodeVersion version;    // Current code version for this module
    
    public IRModuleBody(IRScope lexicalParent, String name, StaticScope scope) {
        super(lexicalParent, name, scope);

        if (!IRBuilder.inIRGenOnlyMode()) {
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
