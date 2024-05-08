package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.Interpreter;
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

        return callInternal(context, clazz);
    }

    protected IRubyObject callInternal(ThreadContext context, RubyModule clazz) {
        ensureInstrsReady();

        switch (getIRScope().getScopeType()) {
            case MODULE_BODY: return Interpreter.INTERPRET_MODULE(context, getIRScope(), clazz, name);
            case CLASS_BODY: return Interpreter.INTERPRET_CLASS(context, getIRScope(), clazz, name);
            case METACLASS_BODY: return Interpreter.INTERPRET_METACLASS(context, getIRScope(), clazz, name, getVisibility());
            default: throw new RuntimeException("invalid body method type: " + getIRScope());
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
