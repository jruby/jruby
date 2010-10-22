package org.jruby.compiler.ir.operands;

// Represents a $1 .. $9 node in Ruby code

import org.jruby.RubyRegexp;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;

//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, it could get converted to calls
//
public class NthRef extends Operand {
    final public int matchNumber;

    public NthRef(int matchNumber) {
        this.matchNumber = matchNumber;
    }

    @Override
    public String toString() {
        return "$" + matchNumber;
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        ThreadContext context = interp.getContext();

        return RubyRegexp.nth_match(matchNumber,
                context.getCurrentScope().getBackRef(context.getRuntime()));
    }
}
