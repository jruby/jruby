package org.jruby.compiler.ir;

import org.jruby.parser.StaticScope;

/**
 */
public class IRClassBody extends IRModuleBody {
    public IRClassBody(IRScope lexicalParent, String name, StaticScope scope) {
        super(lexicalParent, name, scope);
    }

    @Override
    public String getScopeName() {
        return "ClassBody";
    }
}
