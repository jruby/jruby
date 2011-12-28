package org.jruby.compiler.ir;

import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.parser.StaticScope;
import org.jruby.parser.IRStaticScope;

public class IRBody extends IRScope {
    public enum BodyType {
        Module, Class, MetaClass, Script;
    }

    private CodeVersion version;    // Current code version for this module
    private BodyType bodyType;
    
    public IRBody(IRScope lexicalParent, String name, StaticScope scope, BodyType bodyType) {
        super(lexicalParent, name, scope);
        
        this.bodyType = bodyType;
        
        if (!IRBuilder.inIRGenOnlyMode()) {
            if (scope != null) ((IRStaticScope)scope).setIRScope(this);
            updateVersion();
        }
    }

    @Override
    public IRScope getNearestModule() {
        return this;
    }

    public void updateVersion() {
        version = CodeVersion.getClassVersionToken();
    }

    public String getScopeName() {
        return bodyType.name();
    }

    public CodeVersion getVersion() {
        return version;
    }

    @Override
    public LocalVariable getImplicitBlockArg() {
        assert false: "A module/class/metaclass body never accepts block args";
        
        return null;
    }
    
    public boolean isClass() {
        return bodyType == BodyType.Class;
    }
    
    public boolean isMetaClass() {
        return bodyType == BodyType.MetaClass;
    }
    
    // FIXME: This is a poor name.  It is used for super and some complex exprs for
    // figuring out class bodys for cvars
    @Override
    public boolean isBody() {
        return true;
    }
}
