package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRScope;

public interface CompilerPass
{
    // Should we run this pass on the current scope before running it on nested scopes?
    public boolean isPreOrder();

    // Run the pass on the passed in scope!
    public void run(IRScope s);
}
