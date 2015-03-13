package org.jruby.internal.runtime.methods;

import java.util.ArrayList;
import java.util.List;
import org.jruby.RubyModule;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.runtime.IRRuntimeHelpers;
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
    public String[] getParameterList() {
        return new String[0];
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

        if (!ic.hasExplicitCallProtocol()) this.pre(ic, context, self, name, block, getImplementationClass());

        try {
            switch (method.getScopeType()) {
                case MODULE_BODY: return INTERPRET_MODULE(ic, context, self, clazz, method.getName(), block);
                case CLASS_BODY: return INTERPRET_CLASS(ic, context, self, clazz, method.getName(), block);
                case METACLASS_BODY: return INTERPRET_METACLASS(ic, context, self, clazz, "singleton class", block);
                default: throw new RuntimeException("invalid body method type: " + method);
            }
        } finally {
            if (!ic.hasExplicitCallProtocol()) this.post(ic, context);
        }
    }

    private IRubyObject INTERPRET_METACLASS(InterpreterContext ic, ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        return interpretWithBacktrace(ic, context, self, name, block);
    }

    private IRubyObject INTERPRET_MODULE(InterpreterContext ic, ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        return interpretWithBacktrace(ic, context, self, name, block);
    }

    private IRubyObject INTERPRET_CLASS(InterpreterContext ic, ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        return interpretWithBacktrace(ic, context, self, name, block);
    }

    private IRubyObject interpretWithBacktrace(InterpreterContext ic, ThreadContext context, IRubyObject self, String name, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());
            return ic.engine.interpret(context, self, ic, getImplementationClass().getMethodLocation(), name, block, null);
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
