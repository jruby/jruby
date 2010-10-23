package org.jruby.compiler.ir;

import org.jruby.compiler.ir.operands.Operand;
import org.jruby.parser.StaticScope;

public class IRClass extends IRModule {
    final public Operand superClass;

    public IRClass(IRScope lexicalParent, Operand container, Operand superClass, String className, StaticScope staticScope) {
        super(lexicalParent, container, className, staticScope);
        this.superClass = superClass;
    }

    @Override
    public String getScopeName() {
        return "Class";
    }
}
