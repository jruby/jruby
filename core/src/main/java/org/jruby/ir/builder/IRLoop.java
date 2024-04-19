package org.jruby.ir.builder;

import org.jruby.ir.IRScope;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Variable;

class IRLoop {
    public final IRScope container;
    public final IRLoop   parentLoop;
    public final Label loopStartLabel;
    public final Label    loopEndLabel; // for `break`
    public final Label    iterStartLabel;
    public final Label    iterEndLabel; // for `next`
    public final Variable loopResult;

    public boolean hasNext = false;
    public boolean hasBreak = false;

    public IRLoop(IRScope s, IRLoop outerLoop, Variable result) {
        container = s;
        parentLoop = outerLoop;
        loopStartLabel = s.getNewLabel("_LOOP_BEGIN");
        loopEndLabel   = s.getNewLabel("_LOOP_END");
        iterStartLabel = s.getNewLabel("_ITER_BEGIN");
        iterEndLabel   = s.getNewLabel("_ITER_END");
        loopResult     = result;
        s.setHasLoops();
    }
}
