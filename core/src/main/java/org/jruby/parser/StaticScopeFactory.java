package org.jruby.parser;


import org.jruby.Ruby;

/**
 * Gives instances of static scopes based on compile mode.
 */
public class StaticScopeFactory {
    private final StaticScope dummyScope;

    public StaticScopeFactory(Ruby runtime) {
        dummyScope = new StaticScope(StaticScope.Type.LOCAL, null);
        dummyScope.setModule(runtime.getObject());
    }

    public StaticScope newBlockScope(StaticScope parent, String file) {
        return new StaticScope(StaticScope.Type.BLOCK, parent, file);
    }

    public StaticScope newBlockScope(StaticScope parent) {
        return new StaticScope(StaticScope.Type.BLOCK, parent);
    }

    public StaticScope newBlockScope(StaticScope parent, String[] names) {
        return new StaticScope(StaticScope.Type.BLOCK, parent, names);
    }

    public StaticScope newEvalScope(StaticScope parent) {
        return new StaticScope(StaticScope.Type.EVAL, parent);
    }

    public StaticScope newEvalScope(StaticScope parent, String[] names) {
        return new StaticScope(StaticScope.Type.EVAL, parent, names);
    }

    public StaticScope newLocalScope(StaticScope parent, String file) {
        return new StaticScope(StaticScope.Type.LOCAL, parent, file);
    }

    public StaticScope newLocalScope(StaticScope parent) {
        return new StaticScope(StaticScope.Type.LOCAL, parent);
    }

    public StaticScope newLocalScope(StaticScope parent, String[] names) {
        return new StaticScope(StaticScope.Type.LOCAL, parent, names);
    }

    // We only call these from inside IR impl (IR is all or nothing)
    public static StaticScope newIRBlockScope(StaticScope parent) {
        StaticScope scope = new StaticScope(StaticScope.Type.BLOCK, parent);

        scope.setFile(parent.getFile());

        return scope;
    }

    @Deprecated(since = "9.3.0.0")
    public static StaticScope newStaticScope(StaticScope parent, StaticScope.Type type, String[] names) {
        if(names == null) {
            return new StaticScope(type, parent);
        } else {
            return new StaticScope(type, parent, names);
        }
    }

    @Deprecated(since = "9.3.0.0")
    public static StaticScope newStaticScope(StaticScope parent, StaticScope.Type type, String[] names, int keywordArgIndex) {
        if(names == null) {
            return new StaticScope(type, parent);
        } else {
            return new StaticScope(type, parent, names, keywordArgIndex);
        }
    }

    public static StaticScope newStaticScope(StaticScope parent, StaticScope.Type type, String file, String[] names, int keywordArgIndex) {
        if(names == null) {
            return new StaticScope(type, parent, file);
        } else {
            return new StaticScope(type, parent, file, names, keywordArgIndex);
        }
    }

    public StaticScope getDummyScope() {
        return dummyScope;
    }
}
