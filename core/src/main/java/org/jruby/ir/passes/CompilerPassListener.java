package org.jruby.ir.passes;

import org.jruby.ir.IRScope;

/**
 * To get information about all phases of executing a compiler pass.
 *
 * Note: Any data you retrieve may be later mutated by other tasks.  If you
 * care about persistent state it is up to you to clone that information. Also
 * Note, if you clone remember this will affect time it takes for passes to run
 * since these callbacks are synchronously executed in the compiler pass logic.
 */
public interface CompilerPassListener {
    /**
     * This dependent pass has been determined to already be satisfied and is
     * not going to call execute().
     */
    public void alreadyExecuted(CompilerPass passClass, IRScope scope, Object data, boolean childScope);

    /**
     * This pass is about to begin execute'ing.
     * @param pass
     */
    public void startExecute(CompilerPass pass, IRScope scope, boolean childScope);

    /**
     * This pass has just finished execute'ing.  data is the result it is
     * returning.
     */
    public void endExecute(CompilerPass pass, IRScope scope, Object data, boolean childScope);
}
