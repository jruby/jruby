package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.IRClass;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.parser.StaticScope;

public class ClassMetaObject extends ModuleMetaObject {
    protected ClassMetaObject(IRClass scope) {
        super(scope);
    }

    @Override
    public boolean isClass() {
        return true;
    }
}
