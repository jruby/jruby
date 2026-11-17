package org.jruby.ir.targets.indy;

import org.jruby.ir.targets.GlobalVariableCompiler;
import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.invokedynamic.GlobalSite;
import org.jruby.util.JavaNameMangler;

import static org.jruby.util.CodegenUtils.sig;

public class IndyGlobalVariableCompiler implements GlobalVariableCompiler {
    private final IRBytecodeAdapter compiler;

    public IndyGlobalVariableCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
    }

    @Override
    public void getGlobalVariable(String name, String file) {
        compiler.loadContext();
        compiler.adapter.invokedynamic(
                "get:" + JavaNameMangler.mangleMethodName(name),
                sig(IRubyObject.class, ThreadContext.class),
                org.jruby.runtime.invokedynamic.GlobalSite.GLOBAL_BOOTSTRAP_H,
                file, compiler.getLastLine());
    }

    @Override
    public void setGlobalVariable(String name, String file) {
        compiler.loadContext();
        compiler.adapter.invokedynamic(
                "set:" + JavaNameMangler.mangleMethodName(name),
                sig(void.class, IRubyObject.class, ThreadContext.class),
                GlobalSite.GLOBAL_BOOTSTRAP_H,
                file, compiler.getLastLine());
    }
}
