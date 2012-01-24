package org.jruby.compiler.ir;

import org.jruby.parser.StaticScope;

/**
 */
public class IRClassBody extends IRModuleBody {
    public IRClassBody(IRScope lexicalParent, String name, int lineNumber, StaticScope scope) {
        super(lexicalParent, name, lineNumber, scope);
    }

    public IRClassBody(IRScope lexicalParent, String name, String fileName, int lineNumber, StaticScope scope) {
        super(lexicalParent, name, fileName, lineNumber, scope);
    }

    @Override
    public String getScopeName() {
        return "ClassBody";
    }
}
