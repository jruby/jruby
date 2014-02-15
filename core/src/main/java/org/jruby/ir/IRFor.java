package org.jruby.ir;

import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;

/**
 * Represents a 'for' loop
 */
public class IRFor extends IRClosure {
    public IRFor(IRManager manager, IRScope lexicalParent, int lineNumber, StaticScope staticScope, Arity arity, int argumentType) {
        super(manager, lexicalParent, true, lineNumber, staticScope, arity, argumentType);
    }

    @Override
    public IRScopeType getScopeType() {
        return IRScopeType.FOR;
    }
}
