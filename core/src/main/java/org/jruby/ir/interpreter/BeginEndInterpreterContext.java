package org.jruby.ir.interpreter;

import java.util.ArrayList;
import java.util.List;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.WrappedIRClosure;

/**
 * Script body and Evals both have begin/end bodies and need the same state
 * to interpret.
 */
public class BeginEndInterpreterContext extends InterpreterContext {
    private List<IRClosure> beginBlocks;

    public BeginEndInterpreterContext(IRScope scope, Instr[] instructions, boolean rebuild) {
        super(scope, instructions, rebuild);

        beginBlocks = scope.getBeginBlocks();
    }

    public List<IRClosure> getBeginBlocks() {
        return beginBlocks;
    }
}
