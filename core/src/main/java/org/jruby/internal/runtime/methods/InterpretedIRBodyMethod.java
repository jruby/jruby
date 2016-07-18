package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.compiler.Compilable;
import org.jruby.compiler.Uncompilable;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by enebo on 2/6/15.
 */
public class InterpretedIRBodyMethod extends InterpretedIRMethod {
    public InterpretedIRBodyMethod(IRScope method, RubyModule implementationClass) {
        super(method, Visibility.PUBLIC, implementationClass);

        callCount = -1;
    }

    @Override
    public ArgumentDescriptor[] getArgumentDescriptors() {
        return ArgumentDescriptor.EMPTY_ARRAY;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        return call(context, self, clazz, name, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        return callInternal(context, self, clazz, name, block);
    }

    protected IRubyObject callInternal(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        InterpreterContext ic = ensureInstrsReady();

        if (!ic.hasExplicitCallProtocol()) this.pre(ic, context, self, name, block);

        try {
            Compilable uncompilable = new Uncompilable(clazz);  // FIXME: This can get compiled in theory but we might not want to for interpreted

            switch (method.getScopeType()) {
                case MODULE_BODY: return INTERPRET_MODULE(ic, uncompilable, context, self, method.getName(), block);
                case CLASS_BODY: return INTERPRET_CLASS(ic, uncompilable, context, self, method.getName(), block);
                case METACLASS_BODY: return INTERPRET_METACLASS(ic, uncompilable, context, self, "singleton class", block);
                default: throw new RuntimeException("invalid body method type: " + method);
            }
        } finally {
            if (!ic.hasExplicitCallProtocol()) this.post(ic, context);
        }
    }

    private IRubyObject INTERPRET_METACLASS(InterpreterContext ic, Compilable compilable, ThreadContext context, IRubyObject self, String name, Block block) {
        return interpretWithBacktrace(ic, compilable, context, self, name, block);
    }

    private IRubyObject INTERPRET_MODULE(InterpreterContext ic, Compilable compilable, ThreadContext context, IRubyObject self, String name, Block block) {
        return interpretWithBacktrace(ic, compilable, context, self, name, block);
    }

    private IRubyObject INTERPRET_CLASS(InterpreterContext ic, Compilable compilable, ThreadContext context, IRubyObject self, String name, Block block) {
        return interpretWithBacktrace(ic, compilable, context, self, name, block);
    }

    private IRubyObject interpretWithBacktrace(InterpreterContext ic, Compilable compilable, ThreadContext context, IRubyObject self, String name, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());
            return ic.getEngine().interpret(context, compilable, null, self, ic, name, block);
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        return call(context, self, clazz, name, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        return call(context, self, clazz, name, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return call(context, self, clazz, name, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        return call(context, self, clazz, name, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        return call(context, self, clazz, name, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        return call(context, self, clazz, name, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        return call(context, self, clazz, name, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return call(context, self, clazz, name, Block.NULL_BLOCK);
    }
}
