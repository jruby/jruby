package org.jruby.compiler.ir;

import org.jruby.parser.StaticScope;

public class IRClass extends IRModule {
    // This is static information where available and may be null
    final public IRClass superClass;

    public IRClass(IRScope lexicalParent, IRClass superClass, String className, StaticScope staticScope) {
        super(lexicalParent, className, staticScope);
        this.superClass = superClass;
    }

    @Override
    public String getScopeName() {
        return "Class";
    }
}
