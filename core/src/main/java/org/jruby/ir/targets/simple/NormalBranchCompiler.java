package org.jruby.ir.targets.simple;

import org.jruby.ir.targets.BranchCompiler;
import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.Label;

import static org.jruby.util.CodegenUtils.p;
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
}
