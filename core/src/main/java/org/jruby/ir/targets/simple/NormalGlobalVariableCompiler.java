package org.jruby.ir.targets.simple;

import org.jruby.Ruby;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.ir.targets.GlobalVariableCompiler;
import org.jruby.ir.targets.IRBytecodeAdapter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import static org.jruby.util.CodegenUtils.sig;

public class NormalGlobalVariableCompiler implements GlobalVariableCompiler {
    private final IRBytecodeAdapter compiler;

    public NormalGlobalVariableCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
    }

    @Override
    public void getGlobalVariable(String name, String file) {
        compiler.getValueCompiler().pushRuntime();
        compiler.invokeVirtual(Type.getType(Ruby.class), Method.getMethod("org.jruby.internal.runtime.GlobalVariables getGlobalVariables()"));
        compiler.adapter.ldc(name);
        compiler.invokeVirtual(Type.getType(GlobalVariables.class), Method.getMethod("org.jruby.runtime.builtin.IRubyObject get(String)"));
    }

    @Override
    public void setGlobalVariable(String name, String file) {
        compiler.getValueCompiler().pushRuntime();
        compiler.invokeVirtual(Type.getType(Ruby.class), Method.getMethod("org.jruby.internal.runtime.GlobalVariables getGlobalVariables()"));
        compiler.adapter.swap();
        compiler.adapter.ldc(name);
        compiler.adapter.swap();
        compiler.invokeVirtual(Type.getType(GlobalVariables.class), Method.getMethod("org.jruby.runtime.builtin.IRubyObject set(String, org.jruby.runtime.builtin.IRubyObject)"));
        compiler.adapter.pop();
    }
}
