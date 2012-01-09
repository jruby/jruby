package org.jruby.parser;


import org.jruby.Ruby;

/**
 * Gives instances of static scopes based on compile mode.
 */
public class StaticScopeFactory {
    private final StaticScope dummyScope;

    public StaticScopeFactory(Ruby runtime) {
        dummyScope = new LocalStaticScope(null);
        dummyScope.setModule(runtime.getObject());
    }

    public StaticScope newBlockScope(StaticScope parent) {
        return new BlockStaticScope(parent);
    }
    
    public StaticScope newBlockScope(StaticScope parent, String[] names) {
        return new BlockStaticScope(parent, names);
    }

    public StaticScope newEvalScope(StaticScope parent) {
        return new EvalStaticScope(parent);
    }
    
    public StaticScope newEvalScope(StaticScope parent, String[] names) {
        return new EvalStaticScope(parent, names);
    }
    
    public StaticScope newLocalScope(StaticScope parent) {
        return new LocalStaticScope(parent);   
    }
    
    public StaticScope newLocalScope(StaticScope parent, String[] names) {
        return new LocalStaticScope(parent, names);        
    }

    public StaticScope getDummyScope() {
        return dummyScope;
    }
}
