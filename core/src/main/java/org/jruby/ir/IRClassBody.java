package org.jruby.ir;

import org.jruby.RubySymbol;
import org.jruby.parser.StaticScope;
import org.jruby.util.ByteList;

public class IRClassBody extends IRModuleBody {
    public IRClassBody(IRManager manager, IRScope lexicalParent, RubySymbol name, int lineNumber, StaticScope scope) {
        super(manager, lexicalParent, name, lineNumber, scope);
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
