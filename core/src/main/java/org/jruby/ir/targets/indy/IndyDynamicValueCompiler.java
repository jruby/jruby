package org.jruby.ir.targets.indy;

import org.jruby.compiler.NotCompilableException;
import org.jruby.ir.targets.DynamicValueCompiler;
import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.ir.targets.JVM;
import org.jruby.ir.targets.simple.NormalDynamicValueCompiler;
import org.jruby.runtime.ThreadContext;
import org.jruby.specialized.RubyArraySpecialized;
import org.jruby.util.CodegenUtils;
import org.jruby.util.RegexpOptions;

import static org.jruby.util.CodegenUtils.params;
import static org.jruby.util.CodegenUtils.sig;

public class IndyDynamicValueCompiler implements DynamicValueCompiler {
    private final IRBytecodeAdapter compiler;
    private final DynamicValueCompiler normalCompiler;

    public IndyDynamicValueCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
        this.normalCompiler = new NormalDynamicValueCompiler(compiler);
    }

    @Override
    public void pushDRegexp(Runnable callback, RegexpOptions options, int arity) {
        normalCompiler.pushDRegexp(callback, options, arity);
    }

    public void array(int length) {
        if (length > IRBytecodeAdapter.MAX_ARGUMENTS)
            throw new NotCompilableException("literal array has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " elements");

        // use utility method for supported sizes
        if (length <= RubyArraySpecialized.MAX_PACKED_SIZE) {
            normalCompiler.array(length);
            return;
        }

        compiler.adapter.invokedynamic("array", CodegenUtils.sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, length)), Bootstrap.array());
    }

    public void hash(int length) {
        if (length > IRBytecodeAdapter.MAX_ARGUMENTS / 2)
            throw new NotCompilableException("literal hash has more than " + (IRBytecodeAdapter.MAX_ARGUMENTS / 2) + " pairs");

        compiler.adapter.invokedynamic("hash", sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, length * 2)), Bootstrap.hash());
    }
}
