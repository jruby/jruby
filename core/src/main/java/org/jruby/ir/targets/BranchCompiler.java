package org.jruby.ir.targets;

import org.objectweb.asm.Label;

public interface BranchCompiler {
    public void branchIfTruthy(Label target);

    /**
     * Branch to label if value at top of stack is nil
     * <p>
     * stack: obj to check for nilness
     */
    public void branchIfNil(Label label);

    public void bfalse(Label label);

    public void btrue(Label label);

    void checkArgsArity(Runnable args, int required, int opt, boolean rest);

    void checkArity(int required, int opt, boolean rest, int restKey);

    void checkAritySpecificArgs(int required, int opt, boolean rest, int restKey);

    void raiseTypeError(String message);
}
