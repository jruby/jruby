package org.jruby.ir;

import org.jruby.ir.operands.LocalVariable;
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

    /**
     * For scopes never store their own local variables but since they are scopes we
     * add an extra scope Depth to account for navigating through it.
     */
    @Override
    public LocalVariable getLocalVariable(String name, int scopeDepth) {
        return super.getLocalVariable(name, scopeDepth + 1);
    }

    /**
     * For scopes never store their own local variables but since they are scopes we
     * add an extra scope Depth to account for navigating through it.
     */
    @Override
    public LocalVariable getNewLocalVariable(String name, int scopeDepth) {
        return super.getNewLocalVariable(name, scopeDepth + 1);
    }
}
