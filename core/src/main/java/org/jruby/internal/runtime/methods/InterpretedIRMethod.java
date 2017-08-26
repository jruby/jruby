package org.jruby.internal.runtime.methods;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.compiler.Compilable;
import org.jruby.internal.runtime.AbstractIRMethod;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.persistence.IRDumper;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.io.ByteArrayOutputStream;

/**
 * Method for -X-C (interpreted only execution).  See MixedModeIRMethod for inter/JIT method impl.
 */
public class InterpretedIRMethod extends AbstractIRMethod implements Compilable<InterpreterContext> {
    private static final Logger LOG = LoggerFactory.getLogger(InterpretedIRMethod.class);

    private boolean displayedCFG = false; // FIXME: Remove when we find nicer way of logging CFG

    protected InterpreterContext interpreterContext = null;
    protected int callCount = 0;

    public InterpretedIRMethod(IRScope method, Visibility visibility, RubyModule implementationClass) {
        super(method, visibility, implementationClass);

        // -1 jit.threshold is way of having interpreter not promote full builds.
        if (Options.JIT_THRESHOLD.load() == -1) callCount = -1;

        // If we are printing, do the build right at creation time so we can see it
        if (Options.IR_PRINT.load()) {
            ensureInstrsReady();
        }
    }

    public void setCallCount(int callCount) {
        this.callCount = callCount;
    }

    protected void post(InterpreterContext ic, ThreadContext context) {
        // update call stacks (pop: ..)
        context.popFrame();
        if (ic.popDynScope()) {
            context.popScope();
        }
    }

    protected void pre(InterpreterContext ic, ThreadContext context, IRubyObject self, String name, Block block, RubyModule implClass) {
        // update call stacks (push: frame, class, scope, etc.)
        context.preMethodFrameOnly(implClass, name, self, block);
        if (ic.pushNewDynScope()) {
            context.pushScope(DynamicScope.newDynamicScope(ic.getStaticScope()));
        }
    }

    // FIXME: for subclasses we should override this method since it can be simple get
    // FIXME: to avoid cost of synch call in lazilyacquire we can save the ic here
    @Override
    public InterpreterContext ensureInstrsReady() {
        if (interpreterContext == null) {
            if (method instanceof IRMethod) {
                interpreterContext = ((IRMethod) method).lazilyAcquireInterpreterContext();
            }
            interpreterContext = method.getInterpreterContext();

            if (Options.IR_PRINT.load()) {
                ByteArrayOutputStream baos = IRDumper.printIR(method, false, true);

                LOG.info("Printing simple IR for " + method.getName() + ":\n" + new String(baos.toByteArray()));
            }
        }

        return interpreterContext;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        if (callCount >= 0) promoteToFullBuild(context);
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass(), self, name, args, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        if (callCount >= 0) promoteToFullBuild(context);
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass(), self, name, args, Block.NULL_BLOCK);
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
                                         IRubyObject self, String name, IRubyObject[] args, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.getEngine().interpret(context, null, self, ic, implClass, name, args, block);
            } else {
                try {
                    pre(ic, context, self, name, block, implClass);
                    return ic.getEngine().interpret(context, null, self, ic, implClass, name, args, block);
                } finally {
                    post(ic, context);
                }
            }
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        if (callCount >= 0) promoteToFullBuild(context);

        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass(), self, name, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        if (callCount >= 0) promoteToFullBuild(context);

        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass(), self, name, Block.NULL_BLOCK);
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
                                         IRubyObject self, String name, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.getEngine().interpret(context, null, self, ic, implClass, name, block);
            } else {
                try {
                    pre(ic, context, self, name, block, implClass);
                    return ic.getEngine().interpret(context, null, self, ic, implClass, name, block);
                } finally {
                    post(ic, context);
                }
            }
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        if (callCount >= 0) promoteToFullBuild(context);
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass(), self, name, arg0, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        if (callCount >= 0) promoteToFullBuild(context);
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass(), self, name, arg0, Block.NULL_BLOCK);
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
                                         IRubyObject self, String name, IRubyObject arg1, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.getEngine().interpret(context, null, self, ic, implClass, name, arg1, block);
            } else {
                try {
                    pre(ic, context, self, name, block, implClass);
                    return ic.getEngine().interpret(context, null, self, ic, implClass, name, arg1, block);
                } finally {
                    post(ic, context);
                }
            }
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        if (callCount >= 0) promoteToFullBuild(context);
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass(), self, name, arg0, arg1, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        if (callCount >= 0) promoteToFullBuild(context);
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass(), self, name, arg0, arg1, Block.NULL_BLOCK);
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
                                         IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2,  Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.getEngine().interpret(context, null, self, ic, implClass, name, arg1, arg2, block);
            } else {
                try {
                    pre(ic, context, self, name, block, implClass);
                    return ic.getEngine().interpret(context, null, self, ic, implClass, name, arg1, arg2, block);
                } finally {
                    post(ic, context);
                }
            }
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        if (callCount >= 0) promoteToFullBuild(context);
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass(), self, name, arg0, arg1, arg2, block);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        if (callCount >= 0) promoteToFullBuild(context);
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass(), self, name, arg0, arg1, arg2, Block.NULL_BLOCK);
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
                                         IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.getEngine().interpret(context, null, self, ic, implClass, name, arg1, arg2, arg3, block);
            } else {
                try {
                    pre(ic, context, self, name, block, implClass);
                    return ic.getEngine().interpret(context, null, self, ic, implClass, name, arg1, arg2, arg3, block);
                } finally {
                    post(ic, context);
                }
            }
        } finally {
            ThreadContext.popBacktrace(context);
        }

    }

    protected void doDebug() {
        // FIXME: This is printing out IRScope CFG but JIT may be active and it might not reflect
        // currently executing.  Move into JIT and into interp since they will be getting CFG from
        // different sources
        // FIXME: This is only printing out CFG once.  If we keep applying more passes then we
        // will want to print out after those new passes.
        ensureInstrsReady();
        LOG.info("Executing '" + method.getName() + "'");
        if (!displayedCFG) {
            LOG.info(method.debugOutput());
            displayedCFG = true;
        }
    }

    public void completeBuild(InterpreterContext interpreterContext) {
        this.interpreterContext = interpreterContext;
        // Reset so that we can see the new instr dump again
        this.displayedCFG = false;
    }

    // Unlike JIT in MixedMode this will always successfully build but if using executor pool it may take a while
    // and replace interpreterContext asynchronously.
    private void promoteToFullBuild(ThreadContext context) {
        Ruby runtime = context.runtime;

        if (runtime.isBooting() && !Options.JIT_KERNEL.load()) return;   // don't Promote to full build during runtime boot

        if (callCount++ >= Options.JIT_THRESHOLD.load()) runtime.getJITCompiler().buildThresholdReached(context, this);
    }

    public String getClassName(ThreadContext context) {
        return null;
    }
}
