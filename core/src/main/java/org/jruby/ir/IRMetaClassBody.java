package org.jruby.ir;

import org.jruby.parser.StaticScope;
import org.jruby.util.ByteList;

public class IRMetaClassBody extends IRClassBody {
    public IRMetaClassBody(IRManager manager, IRScope lexicalParent, ByteList name, int lineNumber, StaticScope scope) {
        // FIXME: Eliminate obvious one-time use metaclass bodies if not too complicated
        super(manager, lexicalParent, name, lineNumber, scope, false);
    }

    @Override
    protected IRModuleBody cloneInstance(IRScope lexicalParent, StaticScope scope) {
        return new IRMetaClassBody(getManager(), lexicalParent, getByteName(), getLine(), scope);
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
