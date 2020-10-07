package org.jruby.ir.targets.indy;

import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.ir.targets.JVM;
import org.jruby.ir.targets.YieldCompiler;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.CodegenUtils;

import static org.jruby.util.CodegenUtils.params;
import static org.jruby.util.CodegenUtils.sig;

public class IndyYieldCompiler implements YieldCompiler {
    private final IRBytecodeAdapter compiler;

    public IndyYieldCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
    }

    @Override
    public void yield(boolean unwrap) {
        compiler.adapter.invokedynamic("yield", CodegenUtils.sig(JVM.OBJECT, params(ThreadContext.class, Block.class, JVM.OBJECT)), YieldSite.BOOTSTRAP, unwrap ? 1 : 0);
    }

    @Override
    public void yieldSpecific() {
        compiler.adapter.invokedynamic("yieldSpecific", sig(JVM.OBJECT, params(ThreadContext.class, Block.class)), YieldSite.BOOTSTRAP, 0);
    }

    @Override
    public void yieldValues(int arity) {
        compiler.adapter.invokedynamic("yieldValues", sig(JVM.OBJECT, params(ThreadContext.class, Block.class, JVM.OBJECT, arity)), YieldSite.BOOTSTRAP, 0);
    }
}
