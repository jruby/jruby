package org.jruby.ir.targets.simple;

import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.targets.ClassData;
import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.ir.targets.JVM;
import org.jruby.ir.targets.YieldCompiler;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;
import org.objectweb.asm.Opcodes;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.params;
import static org.jruby.util.CodegenUtils.sig;

public class NormalYieldCompiler implements YieldCompiler {
    private final IRBytecodeAdapter compiler;

    public NormalYieldCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
    }

    @Override
    public void yield(boolean unwrap) {
        compiler.adapter.ldc(unwrap);
        compiler.invokeIRHelper("yield", sig(IRubyObject.class, ThreadContext.class, Block.class, IRubyObject.class, boolean.class));
    }

    @Override
    public void yieldSpecific() {
        compiler.invokeIRHelper("yieldSpecific", sig(IRubyObject.class, ThreadContext.class, Block.class));
    }

    @Override
    public void yieldValues(int arity) {
        // Largely copied from non-indy Array compilation; TODO: unify
        if (arity > IRBytecodeAdapter.MAX_ARGUMENTS) throw new NotCompilableException("yield values has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " arguments");

        SkinnyMethodAdapter adapter2;
        String incomingSig = CodegenUtils.sig(JVM.OBJECT, params(ThreadContext.class, Block.class, IRubyObject.class, arity));

        final String methodName = "yieldValues:" + arity;
        final ClassData classData = compiler.getClassData();

        if (!classData.arrayMethodsDefined.containsKey(arity)) {
            adapter2 = new SkinnyMethodAdapter(
                    compiler.adapter.getClassVisitor(),
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    methodName,
                    incomingSig,
                    null,
                    null);

            adapter2.aload(0);
            adapter2.aload(1);
            IRBytecodeAdapter.buildArrayFromLocals(adapter2, 2, arity);

            adapter2.invokestatic(p(IRRuntimeHelpers.class), "yieldValues", sig(IRubyObject.class, ThreadContext.class, Block.class, IRubyObject[].class));
            adapter2.areturn();
            adapter2.end();

            classData.arrayMethodsDefined.put(arity, null);
        }

        // now call it
        compiler.adapter.invokestatic(classData.clsName, methodName, incomingSig);
    }
}
