package org.jruby.ir.targets.simple;

import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.ir.targets.InstanceVariableCompiler;
import org.jruby.ir.targets.JVM;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JavaNameMangler;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.params;
import static org.jruby.util.CodegenUtils.sig;

public class NormalInstanceVariableCompiler implements InstanceVariableCompiler {
    private final IRBytecodeAdapter compiler;

    public NormalInstanceVariableCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
    }

    public void putField(String name) {
        compiler.adapter.dup2(); // self, value, self, value
        compiler.adapter.pop(); // self, value, self
        cacheVariableAccessor(name, true); // self, value, accessor
        compiler.invokeIRHelper("setVariableWithAccessor", sig(void.class, IRubyObject.class, IRubyObject.class, VariableAccessor.class));
    }

    public void getField(String name) {
        compiler.adapter.dup(); // self, self
        cacheVariableAccessor(name, false); // self, accessor
        compiler.loadContext(); // self, accessor, context
        compiler.adapter.ldc(name);
        compiler.invokeIRHelper("getVariableWithAccessor", sig(IRubyObject.class, IRubyObject.class, VariableAccessor.class, ThreadContext.class, String.class));
    }

    /**
     * Retrieve the proper variable accessor for the given arguments. The source object is expected to be on stack.
     *
     * @param name  name of the variable
     * @param write whether the accessor will be used for a write operation
     */
    private void cacheVariableAccessor(String name, boolean write) {
        SkinnyMethodAdapter adapter2;
        String incomingSig = sig(VariableAccessor.class, CodegenUtils.params(JVM.OBJECT));

        String methodName = (write ? "ivarSet" : "ivarGet") + compiler.getClassData().cacheFieldCount.getAndIncrement() + ':' + JavaNameMangler.mangleMethodName(name);

        adapter2 = new SkinnyMethodAdapter(
                compiler.adapter.getClassVisitor(),
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                methodName,
                incomingSig,
                null,
                null);

        // call site object field
        compiler.adapter.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, methodName, ci(VariableAccessor.class), null, null).visitEnd();

        final String className = compiler.getClassData().clsName;

        // retrieve accessor, verifying if non-null
        adapter2.getstatic(className, methodName, ci(VariableAccessor.class));
        adapter2.dup();
        Label get = new Label();
        adapter2.ifnull(get);

        // this might be a little faster if we cached the last class ID seen and used that rather than getMetaClass().getRealClass() in VariableAccessor
        adapter2.dup();
        adapter2.aload(0);
        adapter2.invokevirtual(p(VariableAccessor.class), "verify", sig(boolean.class, Object.class));
        adapter2.iffalse(get);
        adapter2.areturn();

        adapter2.label(get);
        adapter2.pop();
        adapter2.aload(0);
        adapter2.ldc(name);
        adapter2.invokestatic(p(IRRuntimeHelpers.class), write ? "getVariableAccessorForWrite" : "getVariableAccessorForRead", sig(VariableAccessor.class, IRubyObject.class, String.class));
        adapter2.dup();
        adapter2.putstatic(className, methodName, ci(VariableAccessor.class));
        adapter2.areturn();

        adapter2.end();

        // call it from original method to get accessor
        compiler.adapter.invokestatic(className, methodName, incomingSig);
    }
}
