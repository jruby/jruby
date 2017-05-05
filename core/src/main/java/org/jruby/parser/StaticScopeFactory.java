package org.jruby.parser;


import org.jruby.Ruby;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

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

    public StaticScope newBlockScope(StaticScope parent, ByteList[] names) {
        return new StaticScope(StaticScope.Type.BLOCK, parent, names);
    }

    @Deprecated
    public StaticScope newBlockScope(StaticScope parent, String[] names) {
        return new StaticScope(StaticScope.Type.BLOCK, parent, StringSupport.stringsAsByteLists(names));
    }

    public StaticScope newEvalScope(StaticScope parent) {
        return new StaticScope(StaticScope.Type.EVAL, parent);
    }

    public StaticScope newEvalScope(StaticScope parent, ByteList[] names) {
        return new StaticScope(StaticScope.Type.EVAL, parent, names);
    }

    @Deprecated
    public StaticScope newEvalScope(StaticScope parent, String[] names) {
        return new StaticScope(StaticScope.Type.EVAL, parent, StringSupport.stringsAsByteLists(names));
    }

    public StaticScope newLocalScope(StaticScope parent, String file) {
        return new StaticScope(StaticScope.Type.LOCAL, parent, file);
    }

    public StaticScope newLocalScope(StaticScope parent) {
        return new StaticScope(StaticScope.Type.LOCAL, parent);
    }

    public StaticScope newLocalScope(StaticScope parent, ByteList[] names) {
        return new StaticScope(StaticScope.Type.LOCAL, parent, names);
    }

    @Deprecated
    public StaticScope newLocalScope(StaticScope parent, String[] names) {
        return new StaticScope(StaticScope.Type.LOCAL, parent, StringSupport.stringsAsByteLists(names));
    }

    // We only call these from inside IR impl (IR is all or nothing)
    public static StaticScope newIRBlockScope(StaticScope parent) {
        return new StaticScope(StaticScope.Type.BLOCK, parent);
    }

    @Deprecated
    public static StaticScope newStaticScope(StaticScope parent, StaticScope.Type type, String[] names) {
        if(names == null) {
            return new StaticScope(type, parent);
        } else {
            return new StaticScope(type, parent, StringSupport.stringsAsByteLists(names));
        }
    }

    public static StaticScope newStaticScope(StaticScope parent, StaticScope.Type type, ByteList[] names, int keywordArgIndex) {
        if(names == null) {
            return new StaticScope(type, parent);
        } else {
            return new StaticScope(type, parent, names, keywordArgIndex);
        }
    }

    @Deprecated
    public static StaticScope newStaticScope(StaticScope parent, StaticScope.Type type, String[] names, int keywordArgIndex) {
        return newStaticScope(parent, type, StringSupport.stringsAsByteLists(names), keywordArgIndex);
    }


    public StaticScope getDummyScope() {
        return dummyScope;
    }
}
