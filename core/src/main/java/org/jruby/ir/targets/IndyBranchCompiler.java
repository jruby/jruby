package org.jruby.ir.targets;

import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.Label;

import static org.jruby.util.CodegenUtils.sig;

class IndyBranchCompiler implements BranchCompiler {
    private IRBytecodeAdapter compiler;
    private final BranchCompiler normalCompiler;

    IndyBranchCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
        this.normalCompiler = new NormalBranchCompiler(compiler);
    }

    @Override
    public void branchIfNil(Label label) {
        compiler.adapter.invokedynamic("isNil", sig(boolean.class, IRubyObject.class), Bootstrap.isNilBoot());
        compiler.adapter.iftrue(label);
    }

    @Override
    public void branchIfTruthy(Label label) {
        compiler.adapter.invokedynamic("isTrue", sig(boolean.class, IRubyObject.class), Bootstrap.isTrueBoot());
        compiler.adapter.iftrue(label);
    }

    @Override
    public void bfalse(Label label) {
        normalCompiler.bfalse(label);
    }

    @Override
    public void btrue(Label label) {
        normalCompiler.btrue(label);
    }
}
