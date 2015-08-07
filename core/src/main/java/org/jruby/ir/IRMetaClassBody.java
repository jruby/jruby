package org.jruby.ir;

import org.jruby.parser.StaticScope;

public class IRMetaClassBody extends IRClassBody {
    public IRMetaClassBody(IRManager manager, IRScope lexicalParent, String name, int lineNumber, StaticScope scope) {
        super(manager, lexicalParent, name, lineNumber, scope);
    }

    @Override
    public IRScopeType getScopeType() {
        return IRScopeType.METACLASS_BODY;
    }

    @Override
    public boolean isNonSingletonClassBody() {
        return false;
    }
}
