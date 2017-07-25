package org.jruby.ir;

import org.jruby.RubySymbol;
import org.jruby.parser.StaticScope;

public class IRClassBody extends IRModuleBody {
    @Deprecated
    public IRClassBody(IRManager manager, IRScope lexicalParent, String name, int lineNumber, StaticScope scope) {
        this(manager, lexicalParent, manager.getRuntime().newSymbol(name), lineNumber, scope);
    }

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
