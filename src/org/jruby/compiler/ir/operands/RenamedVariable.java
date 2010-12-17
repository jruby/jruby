package org.jruby.compiler.ir.operands;

import org.jruby.interpreter.InterpreterContext;

/**
 * Generic variable with a custom prefix -- mostly used during optimization passes
 * where we need to rename existing variables
 */
public class RenamedVariable extends TemporaryVariable {
    final String prefix;

    public RenamedVariable(String prefix, int offset) {
        super(offset);
		  this.prefix = prefix;
    }

    @Override
    public String getPrefix() {
        return this.prefix + "_";
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        return interp.getRenamedVariable(offset);
    }

    @Override
    public Object store(InterpreterContext interp, Object value) {
        return interp.setRenamedVariable(offset, value);
    }
}
