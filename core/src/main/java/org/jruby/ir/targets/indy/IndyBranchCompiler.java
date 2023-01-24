package org.jruby.ir.targets.indy;

import org.jruby.RubyArray;
import org.jruby.ir.targets.BranchCompiler;
import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.ir.targets.simple.NormalBranchCompiler;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.Label;

import static org.jruby.util.CodegenUtils.sig;

public class IndyBranchCompiler implements BranchCompiler {
    private final IRBytecodeAdapter compiler;
    private final BranchCompiler normalCompiler;

    public IndyBranchCompiler(IRBytecodeAdapter compiler) {
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

    public void checkArgsArity(Runnable args, int required, int opt, boolean rest) {
        compiler.loadContext();
        args.run();
        compiler.adapter.invokedynamic("checkArrayArity", sig(void.class, ThreadContext.class, RubyArray.class), Bootstrap.CHECK_ARRAY_ARITY_BOOTSTRAP, required, opt, rest ? 1 : 0);
    }
}
