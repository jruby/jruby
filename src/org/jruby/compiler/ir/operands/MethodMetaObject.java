package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.IRMethod;
import org.jruby.interpreter.InterpreterContext;

public class MethodMetaObject extends MetaObject {
    public MethodMetaObject(IRMethod scope) {
        super(scope);
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        return ((IRMethod) scope).getDefiningModule().getStaticScope().getModule();
    }
}
