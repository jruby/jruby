package org.jruby.ir.targets.simple;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.targets.ClassData;
import org.jruby.ir.targets.DynamicValueCompiler;
import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.ir.targets.JVM;
import org.jruby.ir.targets.indy.DRegexpObjectSite;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.specialized.RubyArraySpecialized;
import org.jruby.util.CodegenUtils;
import org.jruby.util.RegexpOptions;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.params;
import static org.jruby.util.CodegenUtils.sig;

public class NormalDynamicValueCompiler implements DynamicValueCompiler {
    private final IRBytecodeAdapter compiler;

    public NormalDynamicValueCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
    }

    public void pushDRegexp(Runnable callback, RegexpOptions options, int arity) {
        if (arity > IRBytecodeAdapter.MAX_ARGUMENTS)
            throw new NotCompilableException("dynamic regexp has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " elements");

        String cacheField = null;
        Label done = null;

        if (options.isOnce()) {
            // need to cache result forever
            cacheField = "dregexp" + compiler.getClassData().cacheFieldCount.getAndIncrement();
            done = new Label();
            compiler.adapter.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, cacheField, ci(RubyRegexp.class), null, null).visitEnd();
            compiler.adapter.getstatic(compiler.getClassData().clsName, cacheField, ci(RubyRegexp.class));
            compiler.adapter.dup();
            compiler.adapter.ifnonnull(done);
            compiler.adapter.pop();
        }

        // We may evaluate these operands multiple times or the upstream instrs that created them, which is a bug (jruby/jruby#2798).
        // However, only one dregexp will ever come out of the indy call.
        callback.run();
        compiler.adapter.invokedynamic("dregexp", sig(RubyRegexp.class, params(ThreadContext.class, RubyString.class, arity)), DRegexpObjectSite.BOOTSTRAP, options.toEmbeddedOptions());

        if (done != null) {
            compiler.adapter.dup();
            compiler.adapter.putstatic(compiler.getClassData().clsName, cacheField, ci(RubyRegexp.class));
            compiler.adapter.label(done);
        }
    }

    public void array(int length) {
        if (length > IRBytecodeAdapter.MAX_ARGUMENTS) throw new NotCompilableException("literal array has more than " + IRBytecodeAdapter.MAX_ARGUMENTS + " elements");

        // use utility method for supported sizes
        if (length <= RubyArraySpecialized.MAX_PACKED_SIZE) {
            compiler.invokeIRHelper("newArray", sig(RubyArray.class, params(ThreadContext.class, IRubyObject.class, length)));
            return;
        }

        SkinnyMethodAdapter adapter2;
        String incomingSig = CodegenUtils.sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, length));

        final String methodName = "array:" + length;
        final ClassData classData = compiler.getClassData();

        if (!classData.arrayMethodsDefined.containsKey(length)) {
            adapter2 = new SkinnyMethodAdapter(
                    compiler.adapter.getClassVisitor(),
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    methodName,
                    incomingSig,
                    null,
                    null);

            adapter2.aload(0);
            adapter2.getfield(p(ThreadContext.class), "runtime", ci(Ruby.class));
            IRBytecodeAdapter.buildArrayFromLocals(adapter2, 1, length);

            adapter2.invokevirtual(p(Ruby.class), "newArrayNoCopy", sig(RubyArray.class, IRubyObject[].class));
            adapter2.areturn();
            adapter2.end();

            classData.arrayMethodsDefined.put(length, null);
        }

        // now call it
        compiler.adapter.invokestatic(classData.clsName, methodName, incomingSig);
    }

    public void hash(int length) {
        if (length > IRBytecodeAdapter.MAX_ARGUMENTS / 2) throw new NotCompilableException("literal hash has more than " + (IRBytecodeAdapter.MAX_ARGUMENTS / 2) + " pairs");

        SkinnyMethodAdapter adapter2;
        String incomingSig = sig(JVM.OBJECT, params(ThreadContext.class, JVM.OBJECT, length * 2));

        final String methodName = "hash:" + length;
        final ClassData classData = compiler.getClassData();

        if (!classData.hashMethodsDefined.containsKey(length)) {
            adapter2 = new SkinnyMethodAdapter(
                    compiler.adapter.getClassVisitor(),
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    methodName,
                    incomingSig,
                    null,
                    null);

            adapter2.aload(0);
            IRBytecodeAdapter.buildArrayFromLocals(adapter2, 1, length * 2);

            adapter2.invokestatic(p(IRRuntimeHelpers.class), "constructHashFromArray", sig(RubyHash.class, ThreadContext.class, IRubyObject[].class));
            adapter2.areturn();
            adapter2.end();

            classData.hashMethodsDefined.put(length, null);
        }

        // now call it
        compiler.adapter.invokestatic(classData.clsName, methodName, incomingSig);
    }
}
