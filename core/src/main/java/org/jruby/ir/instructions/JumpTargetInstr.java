package org.jruby.ir.instructions;

import org.jruby.ir.operands.Label;

/**
 * Simple interface so we can ask for jump label for branches or jumps
 */
public interface JumpTargetInstr {
    Label getJumpTarget();
    void setJumpTarget(Label target);
}
