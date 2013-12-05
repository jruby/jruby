package org.jruby.runtime.scope;

import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;

/**
 * This is a DynamicScope that does not support any variables. It differs from
 * NoVarsDynamicScope in that it has hard failures for "backref" and "lastline"
 * accesses, since in the JRuby 1.3 cycle it was discovered that threads were
 * sharing a single dummyscope while still setting per-call backrefs. The hard
 * errors here are an attempt to catch such situations in the future, before
 * the optimizing compiler work makes such determinations in advance.
 */
public class DummyDynamicScope extends NoVarsDynamicScope {
    private static final int SIZE = 0;
    private static final String GROW_ERROR = "DummyDynamicScope cannot be grown; use ManyVarsDynamicScope";
    
    public DummyDynamicScope(StaticScope staticScope, DynamicScope parent) {
        super(staticScope, parent);
    }

    public DummyDynamicScope(StaticScope staticScope) {
        super(staticScope);
    }
    
    public void growIfNeeded() {
        growIfNeeded(SIZE, GROW_ERROR);
    }

    protected void growIfNeeded(int size, String message) {
        if (staticScope.getNumberOfVariables() != size) {
            throw new RuntimeException(message);
        }
    }
    
    public DynamicScope cloneScope() {
        // there should be no mutable state in this scope, so return same
        return this;
    }
}
