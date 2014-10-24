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
    private List<WrappedIRClosure> endBlocks = null;

    public BeginEndInterpreterContext(IRScope scope, Instr[] instructions) {
        super(scope, instructions);

        beginBlocks = scope.getBeginBlocks();
    }

    public void recordEndBlock(WrappedIRClosure block) {
        if (endBlocks == null) endBlocks = new ArrayList<>();

        if (!endBlocks.contains(block)) endBlocks.add(block);
    }

    public List<IRClosure> getBeginBlocks() {
        return beginBlocks;
    }

    public List<WrappedIRClosure> getEndBlocks() {
        return endBlocks;
    }
}
