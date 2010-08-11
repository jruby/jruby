package org.jruby.compiler.ir.operands;

import org.jruby.interpreter.InterpreterContext;

/**
 * A set of variables which are only used in a particular scope and never
 * visible to Ruby itself.
 */
public class TemporaryVariable extends Variable {
    final int offset;

    public TemporaryVariable(int offset) {
        this.offset = offset;
    }

    @Override
    public String getName() {
        return getPrefix() + offset;
    }

    public int compareTo(Object other) {
        if (!(other instanceof TemporaryVariable)) return 0;
        
        TemporaryVariable temporary = (TemporaryVariable) other;
        int prefixCompare = getPrefix().compareTo(temporary.getPrefix());
        if (prefixCompare != 0) return prefixCompare;

        if (offset < temporary.offset) {
            return -1;
        } else if (offset > temporary.offset) {
            return 1;
        }

        return 0;
    }

    public String getPrefix() {
        return "%v_";
    }

    @Override
    public String toString() {
        return getPrefix() + offset;
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        return interp.getTemporaryVariable(offset);
    }

    @Override
    public Object store(InterpreterContext interp, Object value) {
        return interp.setTemporaryVariable(offset, value);
    }
}
