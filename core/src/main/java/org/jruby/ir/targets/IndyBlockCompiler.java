package org.jruby.ir.targets;

import org.jruby.ir.IRClosure;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.sig;

class IndyBlockCompiler implements BlockCompiler {
    private IRBytecodeAdapter compiler;

    public IndyBlockCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
    }

    @Override
    public void prepareBlock(IRClosure closure, String parentScopeField, Handle handle, String file, int line, String encodedArgumentDescriptors, org.jruby.runtime.Signature signature) {
        String className = compiler.getClassData().clsName;

        Handle scopeHandle = new Handle(
                Opcodes.H_GETSTATIC,
                className,
                handle.getName() + "_StaticScope",
                ci(StaticScope.class),
                false);
        Handle setScopeHandle = new Handle(
                Opcodes.H_PUTSTATIC,
                className,
                handle.getName() + "_StaticScope",
                ci(StaticScope.class),
                false);
        Handle parentScopeHandle = new Handle(
                Opcodes.H_GETSTATIC,
                className,
                parentScopeField,
                ci(StaticScope.class),
                false);
        String scopeDescriptor = Helpers.describeScope(closure.getStaticScope());

        long encodedSignature = signature.encode();
        compiler.adapter.invokedynamic(handle.getName(), sig(Block.class, ThreadContext.class, IRubyObject.class, DynamicScope.class),
                Bootstrap.prepareBlock(), handle, scopeHandle, setScopeHandle, parentScopeHandle, scopeDescriptor, encodedSignature, file, line, encodedArgumentDescriptors);
    }
}
