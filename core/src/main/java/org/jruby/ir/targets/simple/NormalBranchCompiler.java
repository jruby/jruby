package org.jruby.ir.targets.simple;

import org.jruby.RubyArray;
import org.jruby.api.Error;
import org.jruby.ir.targets.BranchCompiler;
import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.ir.targets.indy.Bootstrap;
import org.jruby.ir.targets.indy.CheckArityBootstrap;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.Label;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.params;
import static org.jruby.util.CodegenUtils.sig;

public class NormalBranchCompiler implements BranchCompiler {
    private final IRBytecodeAdapter compiler;

    public NormalBranchCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
    }

    public void branchIfTruthy(Label target) {
        compiler.adapter.invokeinterface(p(IRubyObject.class), "isTrue", sig(boolean.class));
        btrue(target);
    }

    /**
     * Branch to label if value at top of stack is nil
     * <p>
     * stack: obj to check for nilness
     */
    public void branchIfNil(Label label) {
        compiler.getValueCompiler().pushNil();
        compiler.adapter.if_acmpeq(label);
    }

    public void bfalse(Label label) {
        compiler.adapter.iffalse(label);
    }

    public void btrue(Label label) {
        compiler.adapter.iftrue(label);
    }

    public void checkArgsArity(Runnable args, int required, int opt, boolean rest) {
        compiler.loadContext();
        args.run();
        compiler.adapter.pushInt(required);
        compiler.adapter.pushInt(opt);
        compiler.adapter.pushBoolean(rest);
        compiler.invokeHelper("irCheckArgsArrayArity", sig(void.class, params(ThreadContext.class, RubyArray.class, int.class, int.class, boolean.class)));
    }

    public void checkArity(int required, int opt, boolean rest, int restKey) {
        compiler.adapter.ldc(required);

        compiler.adapter.ldc(opt);
        compiler.adapter.ldc(rest);
        compiler.adapter.ldc(restKey);
        compiler.adapter.invokestatic(p(CheckArityBootstrap.class), "checkArity", sig(void.class, params(ThreadContext.class, StaticScope.class, Object[].class, Object.class, Block.class, int.class, int.class, boolean.class, int.class)));
    }

    public void checkAritySpecificArgs(int required, int opt, boolean rest, int restKey) {
        compiler.adapter.ldc(required);
        compiler.adapter.ldc(opt);
        compiler.adapter.ldc(rest);
        compiler.adapter.ldc(restKey);
        compiler.adapter.invokestatic(p(CheckArityBootstrap.class), "checkAritySpecificArgs", sig(void.class, params(ThreadContext.class, StaticScope.class, Object[].class, Block.class, int.class, int.class, boolean.class, int.class)));
    }

    public void raiseTypeError(String message) {
        compiler.adapter.ldc(message);
        compiler.adapter.invokestatic(p(Error.class), "typeError", sig(Throwable.class, ThreadContext.class, String.class));
        compiler.adapter.athrow();
    }
}
