package org.jruby.ir.targets.simple;

import org.jruby.RubyHash;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.targets.ArgumentsCompiler;
import org.jruby.ir.targets.ClassData;
import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.ir.targets.JVM;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;
import org.objectweb.asm.Opcodes;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.params;
import static org.jruby.util.CodegenUtils.sig;

public class NormalArgumentsCompiler implements ArgumentsCompiler {
    private final IRBytecodeAdapter compiler;

    public NormalArgumentsCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
    }

    @Override
    public void kwargsHash(int length) {
        if (length > IRBytecodeAdapter.MAX_ARGUMENTS / 2)
            throw new NotCompilableException("kwargs hash has more than " + (IRBytecodeAdapter.MAX_ARGUMENTS / 2) + " pairs");

        SkinnyMethodAdapter adapter2;
        String incomingSig = CodegenUtils.sig(JVM.OBJECT, params(ThreadContext.class, RubyHash.class, IRubyObject.class, length * 2));

        final String methodName = "kwargsHash:" + length;
        final ClassData classData = compiler.getClassData();

        if (!classData.kwargsHashMethodsDefined.containsKey(length)) {
            adapter2 = new SkinnyMethodAdapter(
                    compiler.adapter.getClassVisitor(),
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    methodName,
                    incomingSig,
                    null,
                    null);

            adapter2.aload(0);
            adapter2.aload(1);
            IRBytecodeAdapter.buildArrayFromLocals(adapter2, 2, length * 2);

            adapter2.invokestatic(p(IRRuntimeHelpers.class), "dupKwargsHashAndPopulateFromArray", sig(RubyHash.class, ThreadContext.class, RubyHash.class, IRubyObject[].class));
            adapter2.areturn();
            adapter2.end();

            classData.kwargsHashMethodsDefined.put(length, null);
        }

        // now call it
        compiler.adapter.invokestatic(classData.clsName, methodName, incomingSig);
    }
}
