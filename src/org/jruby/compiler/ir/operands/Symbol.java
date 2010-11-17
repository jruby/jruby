package org.jruby.compiler.ir.operands;

import org.jruby.interpreter.InterpreterContext;

public class Symbol extends Reference {
    public Symbol(String name) {
        super(name);
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        return interp.getRuntime().newSymbol(getName());
    }
}
