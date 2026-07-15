package org.jruby.ir;

import org.jruby.parser.StaticScope;
import org.jruby.util.ByteList;

public class IRClassBody extends IRModuleBody {
    public IRClassBody(IRManager manager, IRScope lexicalParent, ByteList name, int lineNumber, StaticScope scope,
                       boolean executesOnce) {
        super(manager, lexicalParent, name, lineNumber, scope, executesOnce);
    }

    @Override
    protected IRModuleBody cloneInstance(IRScope lexicalParent, StaticScope scope) {
        return new IRClassBody(getManager(), lexicalParent, getByteName(), getLine(), scope, executesOnce());
    }

    @Override
    public IRScopeType getScopeType() {
        return IRScopeType.CLASS_BODY;
    }

    @Override
    public boolean isNonSingletonClassBody() {
        return true;
    }
}
