package org.jruby.ir.interpreter;

import java.util.List;
import java.util.concurrent.Callable;

import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.Instr;

/**
 * Script body and Evals both have begin/end bodies and need the same state
 * to interpret.
 */
public class BeginEndInterpreterContext extends InterpreterContext {
    private List<IRClosure> beginBlocks;

    public BeginEndInterpreterContext(IRScope scope, List<Instr> instructions) {
        super(scope, instructions);

        beginBlocks = scope.getBeginBlocks();
    }

    public BeginEndInterpreterContext(IRScope scope, Callable<List<Instr>> instructions) throws Exception {
        super(scope, instructions);

        beginBlocks = scope.getBeginBlocks();
    }

    public List<IRClosure> getBeginBlocks() {
        return beginBlocks;
    }
}
