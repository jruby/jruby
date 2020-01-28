package org.jruby.ir.targets;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JavaNameMangler;

import static org.jruby.util.CodegenUtils.sig;

class IndyGlobalVariableCompiler implements GlobalVariableCompiler {
    private IRBytecodeAdapter compiler;

    public IndyGlobalVariableCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
    }

    @Override
    public void getGlobalVariable(String name, String file, int line) {
        compiler.loadContext();
        compiler.adapter.invokedynamic(
                "get:" + JavaNameMangler.mangleMethodName(name),
                sig(IRubyObject.class, ThreadContext.class),
                Bootstrap.global(),
                file, line);
    }

    @Override
    public void setGlobalVariable(String name, String file, int line) {
        compiler.loadContext();
        compiler.adapter.invokedynamic(
                "set:" + JavaNameMangler.mangleMethodName(name),
                sig(void.class, IRubyObject.class, ThreadContext.class),
                Bootstrap.global(),
                file, line);
    }
}
