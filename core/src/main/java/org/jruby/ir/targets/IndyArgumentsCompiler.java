package org.jruby.ir.targets;

import org.jruby.RubyHash;
import org.jruby.compiler.NotCompilableException;
import org.jruby.runtime.ThreadContext;

import static org.jruby.util.CodegenUtils.params;
import static org.jruby.util.CodegenUtils.sig;

class IndyArgumentsCompiler implements ArgumentsCompiler {
    private IRBytecodeAdapter compiler;

    public IndyArgumentsCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
    }

    @Override
    public void kwargsHash(int length) {
        if (length > IRBytecodeAdapter.MAX_ARGUMENTS / 2)
            throw new NotCompilableException("kwargs hash has more than " + (IRBytecodeAdapter.MAX_ARGUMENTS / 2) + " pairs");

        compiler.adapter.invokedynamic("kwargsHash", sig(JVM.OBJECT, params(ThreadContext.class, RubyHash.class, JVM.OBJECT, length * 2)), Bootstrap.kwargsHash());
    }
}
