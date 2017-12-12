package org.jruby.ir;

import org.jruby.parser.StaticScope;
import org.jruby.util.ByteList;

public class IRMetaClassBody extends IRClassBody {
    public IRMetaClassBody(IRManager manager, IRScope lexicalParent, ByteList name, int lineNumber, StaticScope scope) {
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
