package org.jruby.compiler.ir.operands;

import org.jruby.interpreter.InterpreterContext;

public class DynamicSymbol extends DynamicReference
{
    public DynamicSymbol(CompoundString s) { super(s); }

    @Override
    public Object retrieve(InterpreterContext interp) {
        return interp.getRuntime().newSymbol(_refName.retrieveJavaString(interp));
    }
}
