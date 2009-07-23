package org.jruby.compiler.ir;

// SSS FIXME: I could make IR_Loop a scope too ... semantically, it is a scope, but, functionally, not sure if this is required yet ...

import org.jruby.compiler.ir.operands.Label;

public class IR_Loop
{
    public final IR_Scope _container;
    public final IR_Loop  _parentLoop;
    public final Label    _loopStartLabel;
    public final Label    _loopEndLabel;
    public final Label    _iterStartLabel;
    public final Label    _iterEndLabel;

    public IR_Loop(IR_Scope s)
    {
        _container = s;
        _parentLoop = s.getCurrentLoop();
        _loopStartLabel = s.getNewLabel("_LOOP_BEGIN");
        _loopEndLabel   = s.getNewLabel("_LOOP_END");
        _iterStartLabel = s.getNewLabel("_ITER_BEGIN");
        _iterEndLabel   = s.getNewLabel("_ITER_END");
    }
}
