package org.jruby.parser;

import org.jruby.Ruby;

/**
 * Allocate IR-friendly static scopes (it is also a marker
 * for constructing IR-friendly dynamic scopes.
 */
public class IRStaticScopeFactory extends StaticScopeFactory {
    public IRStaticScopeFactory(Ruby runtime) {
        super(runtime);
    }

    @Override
    public StaticScope newBlockScope(StaticScope parent) {
        return new IRStaticScope(parent, true, false);
    }

    @Override
    public StaticScope newBlockScope(StaticScope parent, String[] names) {
        return new IRStaticScope(parent, names, true, false);
    }

    @Override
    public StaticScope newEvalScope(StaticScope parent) {
        return new IRStaticScope(parent, true, true);
    }

    @Override
    public StaticScope newEvalScope(StaticScope parent, String[] names) {
        return new IRStaticScope(parent, names, true, true);
    }

    @Override
    public StaticScope newLocalScope(StaticScope parent) {
        return new IRStaticScope(parent, false, false);
    }

    @Override
    public StaticScope newLocalScope(StaticScope parent, String[] names) {
        return new IRStaticScope(parent, names, false, false);
    }
    
    // We only call these from inside IR impl (IR is all or nothing)
    public static StaticScope newIRBlockScope(StaticScope parent) {
        return new IRStaticScope(parent, true, false);
    }
    
    // We only call these from inside IR impl (IR is all or nothing)    
    public static StaticScope newIRLocalScope(StaticScope parent) {
        return new IRStaticScope(parent, false, false);
    }
    
    // We only call these from inside IR impl (IR is all or nothing)    
    public static StaticScope newIREvalScope(StaticScope parent) {
        return new IRStaticScope(parent, true, true);
    }
    
}
