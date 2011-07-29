package org.jruby.compiler.ir.operands;

// Placeholder class for method address

import org.jruby.interpreter.InterpreterContext;

public class MethAddr extends Reference {
    public MethAddr(String name) {
        super(name);
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof MethAddr) && ((MethAddr)o).getName().equals(getName());
    }
}
