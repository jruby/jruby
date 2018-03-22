package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.internal.runtime.AbstractIRMethod;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.FullInterpreterContext;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.persistence.IRDumper;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.io.ByteArrayOutputStream;

/**
 * Method for -X-C (interpreted only execution).  See MixedModeIRMethod for inter/JIT method impl.
 */
public class OptInterpretedIRMethod extends AbstractIRMethod {
    private static final Logger LOG = LoggerFactory.getLogger(OptInterpretedIRMethod.class);

    protected FullInterpreterContext interpreterContext;

    public OptInterpretedIRMethod(IRScope method, Visibility visibility, RubyModule implementationClass) {
        super(method, visibility, implementationClass);

        interpreterContext = method.prepareFullBuild();
    }

    @Override
    public InterpreterContext ensureInstrsReady() {
        return interpreterContext;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass(), self, name, args, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass(), self, name, args, Block.NULL_BLOCK);
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
                                         IRubyObject self, String name, IRubyObject[] args, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());
            return ic.getEngine().interpret(context, null, self, ic, implClass, name, args, block);
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass(), self, name, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass(), self, name, Block.NULL_BLOCK);
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
                                         IRubyObject self, String name, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());
            return ic.getEngine().interpret(context, null, self, ic, implClass, name, block);
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass(), self, name, arg0, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass(), self, name, arg0, Block.NULL_BLOCK);
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
                                         IRubyObject self, String name, IRubyObject arg1, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());
            return ic.getEngine().interpret(context, null, self, ic, implClass, name, arg1, block);
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass(), self, name, arg0, arg1, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass(), self, name, arg0, arg1, Block.NULL_BLOCK);
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
                                         IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());
            return ic.getEngine().interpret(context, null, self, ic, implClass, name, arg1, arg2, block);
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass(), self, name, arg0, arg1, arg2, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass(), self, name, arg0, arg1, arg2, Block.NULL_BLOCK);
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
                                         IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());
            return ic.getEngine().interpret(context, null, self, ic, implClass, name, arg1, arg2, arg3, block);
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }
}
