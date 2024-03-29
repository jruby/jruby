package org.jruby.ir.targets.indy;

import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.ir.targets.InstanceVariableCompiler;
import org.jruby.ir.targets.JVM;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.invokedynamic.VariableSite;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JavaNameMangler;

import static org.jruby.util.CodegenUtils.sig;

public class IndyInstanceVariableCompiler implements InstanceVariableCompiler {
    private final IRBytecodeAdapter compiler;

    public IndyInstanceVariableCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
    }

    public void putField(Runnable target, Runnable value, String name) {
        target.run();
        value.run();
        compiler.adapter.invokedynamic("ivarSet:" + JavaNameMangler.mangleMethodName(name), sig(void.class, IRubyObject.class, IRubyObject.class), VariableSite.IVAR_ASM_HANDLE);
    }

    public void getField(Runnable source, String name, boolean rawValue) {
        source.run();
        compiler.adapter.invokedynamic("ivarGet:" + JavaNameMangler.mangleMethodName(name) + (rawValue ? ":true" : ""), CodegenUtils.sig(JVM.OBJECT, IRubyObject.class), VariableSite.IVAR_ASM_HANDLE);
    }
}
