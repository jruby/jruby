package org.jruby.compiler.ir;

// SSS FIXME: I could make IR_Loop a scope too ... semantically, it is a scope, but, functionally, not sure if this is required yet ...

import org.jruby.compiler.ir.operands.Label;

public class IRLoop {
    public final IRExecutionScope container;
    public final IRLoop  parentLoop;
    public final Label    loopStartLabel;
    public final Label    loopEndLabel;
    public final Label    iterStartLabel;
    public final Label    iterEndLabel;

    public IRLoop(IRExecutionScope s) {
        container = s;
        parentLoop = s.getCurrentLoop();
        loopStartLabel = s.getNewLabel("_LOOP_BEGIN");
        loopEndLabel   = s.getNewLabel("_LOOP_END");
        iterStartLabel = s.getNewLabel("_ITER_BEGIN");
        iterEndLabel   = s.getNewLabel("_ITER_END");
    }
}
