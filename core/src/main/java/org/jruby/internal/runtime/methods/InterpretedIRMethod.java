package org.jruby.internal.runtime.methods;

import java.util.List;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.compiler.FullBuildSource;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.PositionAware;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

/**
 * Method for -X-C (interpreted only execution).  See MixedModeIRMethod for inter/JIT method impl.
 */
public class InterpretedIRMethod extends DynamicMethod implements IRMethodArgs, PositionAware, FullBuildSource {
    private static final Logger LOG = LoggerFactory.getLogger("InterpretedIRMethod");

    private Arity arity;
    private boolean displayedCFG = false; // FIXME: Remove when we find nicer way of logging CFG

    protected final IRScope method;

    protected InterpreterContext interpreterContext = null;
    protected int callCount = 0;

    public InterpretedIRMethod(IRScope method, Visibility visibility, RubyModule implementationClass) {
        super(implementationClass, visibility, CallConfiguration.FrameNoneScopeNone, method.getName());
        this.method = method;
        this.method.getStaticScope().determineModule();
        this.arity = calculateArity();

        // FIXME: Enable no full build promotion option (perhaps piggy back JIT threshold)
    }

    public IRScope getIRScope() {
        return method;
    }

    public void setCallCount(int callCount) {
        this.callCount = callCount;
    }

    public StaticScope getStaticScope() {
        return method.getStaticScope();
    }

    public String[] getParameterList() {
        ensureInstrsReady(); // Make sure method is minimally built before returning this info
        return ((IRMethod) method).getArgDesc();
    }

    private Arity calculateArity() {
        StaticScope s = method.getStaticScope();
        if (s.getOptionalArgs() > 0 || s.getRestArg() >= 0) return Arity.required(s.getRequiredArgs());

        return Arity.createArity(s.getRequiredArgs());
    }

    @Override
    public Arity getArity() {
        return arity;
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
        context.setCurrentVisibility(getVisibility());
    }

    // FIXME: for subclasses we should override this method since it can be simple get
    // FIXME: to avoid cost of synch call in lazilyacquire we can save the ic here
    public InterpreterContext ensureInstrsReady() {
        if (interpreterContext == null) {
            if (method instanceof IRMethod) {
                interpreterContext = ((IRMethod) method).lazilyAcquireInterpreterContext();
            }
            interpreterContext = method.getInterpreterContext();
        }

        return interpreterContext;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        if (callCount >= 0) promoteToFullBuild(context);
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass().getMethodLocation(), self, name, args, block);
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
                                         IRubyObject self, String name, IRubyObject[] args, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.engine.interpret(context, self, ic, implClass, name, args, block, null);
            } else {
                try {
                    pre(ic, context, self, name, block, implClass);
                    return ic.engine.interpret(context, self, ic, implClass, name, args, block, null);
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

        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass().getMethodLocation(), self, name, block);
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
                                         IRubyObject self, String name, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.engine.interpret(context, self, ic, implClass, name, block, null);
            } else {
                try {
                    pre(ic, context, self, name, block, implClass);
                    return ic.engine.interpret(context, self, ic, implClass, name, block, null);
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
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass().getMethodLocation(), self, name, arg0, block);
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
                                         IRubyObject self, String name, IRubyObject arg1, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.engine.interpret(context, self, ic, implClass, name, arg1, block, null);
            } else {
                try {
                    pre(ic, context, self, name, block, implClass);
                    return ic.engine.interpret(context, self, ic, implClass, name, arg1, block, null);
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
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass().getMethodLocation(), self, name, arg0, arg1, block);
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
                                         IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2,  Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.engine.interpret(context, self, ic, implClass, name, arg1, arg2, block, null);
            } else {
                try {
                    pre(ic, context, self, name, block, implClass);
                    return ic.engine.interpret(context, self, ic, implClass, name, arg1, arg2, block, null);
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
        return INTERPRET_METHOD(context, ensureInstrsReady(), getImplementationClass().getMethodLocation(), self, name, arg0, arg1, arg2, block);
    }

    private IRubyObject INTERPRET_METHOD(ThreadContext context, InterpreterContext ic, RubyModule implClass,
                                         IRubyObject self, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        try {
            ThreadContext.pushBacktrace(context, name, ic.getFileName(), context.getLine());

            if (ic.hasExplicitCallProtocol()) {
                return ic.engine.interpret(context, self, ic, implClass, name, arg1, arg2, arg3, block, null);
            } else {
                try {
                    pre(ic, context, self, name, block, implClass);
                    return ic.engine.interpret(context, self, ic, implClass, name, arg1, arg2, arg3, block, null);
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

    public void switchToFullBuild(InterpreterContext interpreterContext) {
        this.interpreterContext = interpreterContext;
    }

    // Unlike JIT in MixedMode this will always successfully build but if using executor pool it may take a while
    // and replace interpreterContext asynchronously.
    protected void promoteToFullBuild(ThreadContext context) {
        Ruby runtime = context.runtime;

        // don't Promote to full build during runtime boot
        if (runtime.isBooting()) return;

        if (callCount++ >= Options.JIT_THRESHOLD.load()) {
            runtime.getJITCompiler().fullBuildThresholdReached(this, context.runtime.getInstanceConfig());
        }
    }

    @Override
    public DynamicMethod dup() {
        return new InterpretedIRMethod(method, visibility, implementationClass);
    }

    public String getFile() {
        return method.getFileName();
    }

    public int getLine() {
        return method.getLineNumber();
    }
}
