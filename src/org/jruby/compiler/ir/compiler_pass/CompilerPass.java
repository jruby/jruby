package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IR_Scope;

public interface CompilerPass
{
    public void run(IR_Scope s);
}
