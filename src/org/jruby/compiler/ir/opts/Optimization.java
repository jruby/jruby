package org.jruby.compiler.ir.opts;

import org.jruby.compiler.ir.IR_Method;

public interface Optimization
{
    public void run(IR_Method m);
}
