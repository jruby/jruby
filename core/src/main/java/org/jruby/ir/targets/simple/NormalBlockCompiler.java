package org.jruby.ir.targets.simple;

import org.jruby.ir.IRClosure;
import org.jruby.ir.targets.BlockCompiler;
import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.CompiledIRBlockBody;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class NormalBlockCompiler implements BlockCompiler {
    private final IRBytecodeAdapter compiler;

    public NormalBlockCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
    }

    @Override
    public void prepareBlock(IRClosure closure, String parentScopeField, Handle handle, String file, int line, String encodedArgumentDescriptors, org.jruby.runtime.Signature signature) {
        // FIXME: too much bytecode
        String cacheField = "blockBody" + compiler.getClassData().cacheFieldCount.getAndIncrement();
        Label done = new Label();
        compiler.adapter.getClassVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, cacheField, ci(CompiledIRBlockBody.class), null, null).visitEnd();
        String clsName = compiler.getClassData().clsName;
        compiler.adapter.getstatic(clsName, cacheField, ci(CompiledIRBlockBody.class));
        compiler.adapter.dup();
        compiler.adapter.ifnonnull(done);
        {
            compiler.adapter.pop();
            compiler.adapter.newobj(p(CompiledIRBlockBody.class));
            compiler.adapter.dup();

            compiler.adapter.ldc(handle);
            compiler.getStaticScope(handle.getName() + "_StaticScope");
            compiler.adapter.ldc(file);
            compiler.adapter.ldc(line);
            compiler.adapter.ldc(encodedArgumentDescriptors);
            compiler.adapter.ldc(signature.encode());

            compiler.adapter.invokespecial(p(CompiledIRBlockBody.class), "<init>", sig(void.class, java.lang.invoke.MethodHandle.class, StaticScope.class, String.class, int.class, String.class, long.class));
            compiler.adapter.dup();
            compiler.adapter.putstatic(clsName, cacheField, ci(CompiledIRBlockBody.class));
        }
        compiler.adapter.label(done);

        compiler.invokeIRHelper("prepareBlock", sig(Block.class, ThreadContext.class, IRubyObject.class, DynamicScope.class, BlockBody.class));
    }
}
