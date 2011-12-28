package org.jruby.compiler.ir;

import org.jruby.parser.StaticScope;

/**
 * 
 */
public class IRMetaClassBody extends IRClassBody {
    public IRMetaClassBody(IRScope lexicalParent, String name, StaticScope scope) {
        super(lexicalParent, name, scope);
    }

    @Override
    public String getScopeName() {
        return "MetaClassBody";
    }
}
