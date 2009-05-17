package org.jruby.runtime.scope;

import org.jruby.Ruby;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;

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
        return new DummyDynamicScope(staticScope, parent);
    }

    /**
     * Get backref
     */
    public IRubyObject getBackRef(Ruby runtime) {
        if (!staticScope.isBackrefLastlineScope()) {
            return parent.getBackRef(runtime);
        }
        throw new RuntimeException("DummyDynamicScope should never be used for backref storage");
    }

    /**
     * Set backref
     */
    public IRubyObject setBackRef(IRubyObject backref) {
        if (!staticScope.isBackrefLastlineScope()) {
            return parent.setBackRef(backref);
        }
        throw new RuntimeException("DummyDynamicScope should never be used for backref storage");
    }

    /**
     * Get lastline
     */
    public IRubyObject getLastLine(Ruby runtime) {
        if (!staticScope.isBackrefLastlineScope()) {
            return parent.getLastLine(runtime);
        }
        throw new RuntimeException("DummyDynamicScope should never be used for lastline storage");
    }

    /**
     * Set lastline
     */
    public IRubyObject setLastLine(IRubyObject lastline) {
        if (!staticScope.isBackrefLastlineScope()) {
            return parent.setLastLine(lastline);
        }
        throw new RuntimeException("DummyDynamicScope should never be used for backref storage");
    }
}
