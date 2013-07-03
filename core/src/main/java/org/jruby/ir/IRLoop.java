package org.jruby.ir;

import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Variable;

public class IRLoop {
    public final IRScope  container;
    public final IRLoop   parentLoop;
    public final Label    loopStartLabel;
    public final Label    loopEndLabel;
    public final Label    iterStartLabel;
    public final Label    iterEndLabel;
    public final Variable loopResult;

    public IRLoop(IRScope s, IRLoop outerLoop) {
        container = s;
        parentLoop = outerLoop;
        loopStartLabel = s.getNewLabel("_LOOP_BEGIN");
        loopEndLabel   = s.getNewLabel("_LOOP_END");
        iterStartLabel = s.getNewLabel("_ITER_BEGIN");
        iterEndLabel   = s.getNewLabel("_ITER_END");
        loopResult     = s.getNewTemporaryVariable();
        s.setHasLoopsFlag(true);
    }
}
