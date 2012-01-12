package org.jruby.compiler.ir;

import org.jruby.parser.StaticScope;

/**
 */
public class IRClassBody extends IRModuleBody {
    public IRClassBody(IRScope lexicalParent, String name, StaticScope scope) {
        super(lexicalParent, name, scope);
    }

    public IRClassBody(IRScope lexicalParent, String name, String fileName, StaticScope scope) {
        super(lexicalParent, name, fileName, scope);
    }

    @Override
    public String getScopeName() {
        return "ClassBody";
    }
}
