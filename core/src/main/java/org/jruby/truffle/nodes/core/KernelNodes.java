/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import java.io.*;
import java.math.*;
import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;

import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.cast.*;
import org.jruby.truffle.nodes.control.*;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.literal.*;
import org.jruby.truffle.nodes.yield.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.subsystems.*;
import org.jruby.truffle.runtime.util.Supplier;

@CoreClass(name = "Kernel")
public abstract class KernelNodes {

    @CoreMethod(names = "abort", isModuleMethod = true, needsSelf = false, maxArgs = 0)
    public abstract static class AbortNode extends CoreMethodNode {

        public AbortNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AbortNode(AbortNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder abort() {
            CompilerDirectives.transferToInterpreter();
            System.exit(1);
            return NilPlaceholder.INSTANCE;
        }
    }

    @CoreMethod(names = "Array", isModuleMethod = true, needsSelf = false, isSplatted = true)
    public abstract static class ArrayNode extends CoreMethodNode {

        @Child ArrayBuilderNode arrayBuilderNode;

        public ArrayNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilderNode = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
        }

        public ArrayNode(ArrayNode prev) {
            super(prev);
            arrayBuilderNode = prev.arrayBuilderNode;
        }

        @Specialization(guards = "isOneArrayElement")
        public RubyArray arrayOneArrayElement(Object[] args) {
            return (RubyArray) args[0];
        }

        @Specialization(guards = "!isOneArrayElement")
        public RubyArray array(Object[] args) {
            final int length = args.length;
            Object store = arrayBuilderNode.start(length);

            for (int n = 0; n < length; n++) {
                store = arrayBuilderNode.append(store, n, args[n]);
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilderNode.finish(store, length), length);
        }

        protected boolean isOneArrayElement(Object[] args) {
            return args.length == 1 && args[0] instanceof RubyArray;
        }

    }

    @CoreMethod(names = "at_exit", isModuleMethod = true, needsSelf = false, needsBlock = true, maxArgs = 0)
    public abstract static class AtExitNode extends CoreMethodNode {

        public AtExitNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AtExitNode(AtExitNode prev) {
            super(prev);
        }

        @Specialization
        public Object atExit(RubyProc block) {
            notDesignedForCompilation();

            getContext().getAtExitManager().add(block);
            return NilPlaceholder.INSTANCE;
        }
    }

    @CoreMethod(names = "binding", isModuleMethod = true, needsSelf = true, maxArgs = 0)
    public abstract static class BindingNode extends CoreMethodNode {

        public BindingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BindingNode(BindingNode prev) {
            super(prev);
        }

        @Specialization
        public Object binding(Object self) {
            final MaterializedFrame callerFrame = Truffle.getRuntime().getCallerFrame()
                    .getFrame(FrameInstance.FrameAccess.MATERIALIZE, false).materialize();

            return new RubyBinding(
                    getContext().getCoreLibrary().getBindingClass(),
                    self,
                    callerFrame);
        }
    }

    @CoreMethod(names = "block_given?", isModuleMethod = true, needsSelf = false, maxArgs = 0)
    public abstract static class BlockGivenNode extends CoreMethodNode {

        public BlockGivenNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BlockGivenNode(BlockGivenNode prev) {
            super(prev);
        }

        @Specialization
        public boolean blockGiven() {
            notDesignedForCompilation();

            return RubyArguments.getBlock(Truffle.getRuntime().getCallerFrame().getFrame(FrameInstance.FrameAccess.READ_ONLY, false).getArguments()) != null;
        }
    }

    // TODO(CS): should hide this in a feature

    @CoreMethod(names = "callcc", isModuleMethod = true, needsSelf = false, needsBlock = true, maxArgs = 0)
    public abstract static class CallccNode extends CoreMethodNode {

        public CallccNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CallccNode(CallccNode prev) {
            super(prev);
        }

        @Specialization
        public Object callcc(RubyProc block) {
            notDesignedForCompilation();

            final RubyContext context = getContext();

            if (block == null) {
                // TODO(CS): should really have acceptsBlock and needsBlock to do this automatically
                throw new RaiseException(context.getCoreLibrary().localJumpError("no block given", this));
            }

            final RubyContinuation continuation = new RubyContinuation(context.getCoreLibrary().getContinuationClass());
            return continuation.enter(block);
        }
    }

    @CoreMethod(names = "catch", isModuleMethod = true, needsSelf = false, needsBlock = true, minArgs = 1, maxArgs = 1)
    public abstract static class CatchNode extends YieldingCoreMethodNode {

        public CatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CatchNode(CatchNode prev) {
            super(prev);
        }

        @Specialization
        public Object doCatch(VirtualFrame frame, Object tag, RubyProc block) {
            notDesignedForCompilation();

            try {
                return yield(frame, block);
            } catch (ThrowException e) {
                if (e.getTag().equals(tag)) {
                    // TODO(cs): unset rather than set to Nil?
                    getContext().getCoreLibrary().getGlobalVariablesObject().setInstanceVariable("$!", NilPlaceholder.INSTANCE);
                    return e.getValue();
                } else {
                    throw e;
                }
            }
        }
    }

    @CoreMethod(names = "eval", isModuleMethod = true, needsSelf = false, minArgs = 1, maxArgs = 2)
    public abstract static class EvalNode extends CoreMethodNode {

        public EvalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EvalNode(EvalNode prev) {
            super(prev);
        }

        @Specialization
        public Object eval(RubyString source, @SuppressWarnings("unused") UndefinedPlaceholder binding) {
            notDesignedForCompilation();

            return getContext().eval(source.toString(), this);
        }

        @Specialization
        public Object eval(RubyString source, RubyBinding binding) {
            notDesignedForCompilation();

            return getContext().eval(source.toString(), binding, this);
        }

    }

    @CoreMethod(names = "exec", isModuleMethod = true, needsSelf = false, minArgs = 1, isSplatted = true)
    public abstract static class ExecNode extends CoreMethodNode {

        public ExecNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ExecNode(ExecNode prev) {
            super(prev);
        }

        @Specialization
        public Object require(Object[] args) {
            notDesignedForCompilation();

            final String[] commandLine = new String[args.length];

            for (int n = 0; n < args.length; n++) {
                commandLine[n] = args[n].toString();
            }

            exec(getContext(), commandLine);

            return null;
        }

        @SlowPath
        private static void exec(RubyContext context, String[] commandLine) {
            final ProcessBuilder builder = new ProcessBuilder(commandLine);
            builder.inheritIO();

            final RubyHash env = (RubyHash) context.getCoreLibrary().getObjectClass().lookupConstant("ENV").getValue();

            // TODO(CS): cast
            for (Map.Entry<Object, Object> entry : ((LinkedHashMap<Object, Object>) env.getStore()).entrySet()) {
                builder.environment().put(entry.getKey().toString(), entry.getValue().toString());
            }

            Process process;

            try {
                process = builder.start();
            } catch (IOException e) {
                // TODO(cs): proper Ruby exception
                throw new RuntimeException(e);
            }

            int exitCode;

            while (true) {
                try {
                    exitCode = process.waitFor();
                    break;
                } catch (InterruptedException e) {
                    continue;
                }
            }

            System.exit(exitCode);
        }

    }

    @CoreMethod(names = "exit", isModuleMethod = true, needsSelf = false, minArgs = 0, maxArgs = 1, lowerFixnumParameters = 0)
    public abstract static class ExitNode extends CoreMethodNode {

        public ExitNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ExitNode(ExitNode prev) {
            super(prev);
        }

        @Specialization
        public Object exit(@SuppressWarnings("unused") UndefinedPlaceholder exitCode) {
            notDesignedForCompilation();

            getContext().shutdown();
            System.exit(0);
            return null;
        }

        @Specialization
        public Object exit(int exitCode) {
            notDesignedForCompilation();

            getContext().shutdown();
            System.exit(exitCode);
            return null;
        }

    }

    @CoreMethod(names = "exit!", isModuleMethod = true, needsSelf = false, maxArgs = 0)
    public abstract static class ExitBangNode extends CoreMethodNode {

        public ExitBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ExitBangNode(ExitBangNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder exit() {
            CompilerDirectives.transferToInterpreter();
            System.exit(1);
            return NilPlaceholder.INSTANCE;
        }
    }

    @CoreMethod(names = "fork", isModuleMethod = true, needsSelf = false, isSplatted = true)
    public abstract static class ForkNode extends CoreMethodNode {

        public ForkNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ForkNode(ForkNode prev) {
            super(prev);
        }

        @Specialization
        public Object fork(Object[] args) {
            notDesignedForCompilation();
            getContext().getWarnings().warn("Kernel#fork not implemented - defined to satisfy some metaprogramming in RubySpec");
            return NilPlaceholder.INSTANCE;
        }

    }

    @CoreMethod(names = "gets", isModuleMethod = true, needsSelf = false, maxArgs = 0)
    public abstract static class GetsNode extends CoreMethodNode {

        public GetsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GetsNode(GetsNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString gets(VirtualFrame frame) {
            notDesignedForCompilation();

            final RubyContext context = getContext();

            final Frame caller = Truffle.getRuntime().getCallerFrame().getFrame(FrameInstance.FrameAccess.READ_WRITE, false);

            final ThreadManager threadManager = context.getThreadManager();

            final String line = getContext().outsideGlobalLock(new Supplier<String>() {

                @Override
                public String get() {
                    try {
                        return gets(context);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            final RubyString rubyLine = context.makeString(line);

            // Set the local variable $_ in the caller

            final FrameSlot slot = caller.getFrameDescriptor().findFrameSlot("$_");

            if (slot != null) {
                caller.setObject(slot, rubyLine);
            }

            return rubyLine;
        }

        @SlowPath
        private static String gets(RubyContext context) throws IOException {
            // TODO(CS): having some trouble interacting with JRuby stdin - so using this hack

            final StringBuilder builder = new StringBuilder();

            while (true) {
                final int c = context.getRuntime().getInstanceConfig().getInput().read();

                if (c == -1 || c == '\r' || c == '\n') {
                    break;
                }

                builder.append((char) c);
            }

            return builder.toString();
        }

    }

    @CoreMethod(names = "Integer", isModuleMethod = true, needsSelf = false, minArgs = 1, maxArgs = 1)
    public abstract static class IntegerNode extends CoreMethodNode {

        @Child protected DispatchHeadNode toInt;

        public IntegerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toInt = new DispatchHeadNode(context);
        }

        public IntegerNode(IntegerNode prev) {
            super(prev);
            toInt = prev.toInt;
        }

        @Specialization
        public int integer(int value) {
            return value;
        }

        @Specialization
        public BigInteger integer(BigInteger value) {
            return value;
        }

        @Specialization
        public int integer(double value) {
            return (int) value;
        }

        @Specialization
        public Object integer(RubyString value) {
            return value.toInteger();
        }

        @Specialization
        public Object integer(VirtualFrame frame, Object value) {
            return toInt.call(frame, value, "to_int", null);
        }

    }

    @CoreMethod(names = "lambda", isModuleMethod = true, needsSelf = false, needsBlock = true, maxArgs = 0)
    public abstract static class LambdaNode extends CoreMethodNode {

        public LambdaNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LambdaNode(LambdaNode prev) {
            super(prev);
        }

        @Specialization
        public RubyProc proc(RubyProc block) {
            notDesignedForCompilation();

            return new RubyProc(getContext().getCoreLibrary().getProcClass(), RubyProc.Type.LAMBDA,
                    block.getSharedMethodInfo(), block.getCallTargetForMethods(), block.getCallTargetForMethods(),
                    block.getDeclarationFrame(), block.getSelfCapturedInScope(), block.getBlockCapturedInScope());
        }
    }

    @CoreMethod(names = "load", isModuleMethod = true, needsSelf = false, minArgs = 1, maxArgs = 1)
    public abstract static class LoadNode extends CoreMethodNode {

        public LoadNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LoadNode(LoadNode prev) {
            super(prev);
        }

        @Specialization
        public boolean load(RubyString file) {
            notDesignedForCompilation();

            getContext().loadFile(file.toString(), this);
            return true;
        }
    }

    @CoreMethod(names = "loop", isModuleMethod = true, needsSelf = false, maxArgs = 0)
    public abstract static class LoopNode extends CoreMethodNode {

        @Child protected WhileNode whileNode;

        public LoopNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            whileNode = new WhileNode(context, sourceSection, BooleanCastNodeFactory.create(context, sourceSection,
                    new BooleanLiteralNode(context, sourceSection, true)),
                    new YieldNode(context, getSourceSection(), new RubyNode[]{}, false)
            );
        }

        public LoopNode(LoopNode prev) {
            super(prev);
            whileNode = prev.whileNode;
        }

        @Specialization
        public Object loop(VirtualFrame frame) {
            return whileNode.execute(frame);
        }
    }

    @CoreMethod(names = "p", visibility = Visibility.PRIVATE, isModuleMethod = true, needsSelf = false, isSplatted = true)
    public abstract static class PNode extends CoreMethodNode {

        @Child protected DispatchHeadNode inspect;

        public PNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            inspect = new DispatchHeadNode(context);
        }

        public PNode(PNode prev) {
            super(prev);
            inspect = prev.inspect;
        }

        @Specialization
        public NilPlaceholder p(final VirtualFrame frame, final Object[] args) {
            notDesignedForCompilation();

            getContext().outsideGlobalLock(new Runnable() {

                @Override
                public void run() {
                    for (Object arg : args) {
                        getContext().getRuntime().getInstanceConfig().getOutput().println(inspect.call(frame, arg, "inspect", null));
                    }
                }

            });

            return NilPlaceholder.INSTANCE;
        }
    }

    @CoreMethod(names = "print", visibility = Visibility.PRIVATE, isModuleMethod = true, needsSelf = false, isSplatted = true)
    public abstract static class PrintNode extends CoreMethodNode {

        @Child protected DispatchHeadNode toS;

        public PrintNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toS = new DispatchHeadNode(context);
        }

        public PrintNode(PrintNode prev) {
            super(prev);
            toS = prev.toS;
        }

        @Specialization
        public NilPlaceholder print(final VirtualFrame frame, final Object[] args) {
            getContext().outsideGlobalLock(new Runnable() {

                @Override
                public void run() {
                    for (Object arg : args) {
                        try {
                            getContext().getRuntime().getInstanceConfig().getOutput().write(((RubyString) toS.call(frame, arg, "to_s", null)).getBytes().bytes());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

            });

            return NilPlaceholder.INSTANCE;
        }
    }

    @CoreMethod(names = "printf", isModuleMethod = true, needsSelf = false, isSplatted = true)
    public abstract static class PrintfNode extends CoreMethodNode {

        public PrintfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PrintfNode(PrintfNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder printf(Object[] args) {
            notDesignedForCompilation();

            if (args.length > 0) {
                final String format = args[0].toString();
                final List<Object> values = Arrays.asList(args).subList(1, args.length);

                getContext().outsideGlobalLock(new Runnable() {

                    @Override
                    public void run() {
                        StringFormatter.format(getContext().getRuntime().getInstanceConfig().getOutput(), format, values);
                    }

                });
            }

            return NilPlaceholder.INSTANCE;
        }
    }

    /*
     * Kernel#pretty_inspect is normally part of stdlib, in pp.rb, but we aren't able to execute
     * that file yet. Instead we implement a very simple version here, which is the solution
     * suggested by RubySpec.
     */

    @CoreMethod(names = "pretty_inspect", maxArgs = 0)
    public abstract static class PrettyInspectNode extends CoreMethodNode {

        @Child protected DispatchHeadNode toS;

        public PrettyInspectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toS = new DispatchHeadNode(context);
        }

        public PrettyInspectNode(PrettyInspectNode prev) {
            super(prev);
            toS = prev.toS;
        }

        @Specialization
        public Object prettyInspect(VirtualFrame frame, Object self) {
            return toS.call(frame, self, "to_s", null);
        }
    }

    @CoreMethod(names = "proc", isModuleMethod = true, needsSelf = false, needsBlock = true, maxArgs = 0)
    public abstract static class ProcNode extends CoreMethodNode {

        public ProcNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ProcNode(ProcNode prev) {
            super(prev);
        }

        @Specialization
        public RubyProc proc(RubyProc block) {
            notDesignedForCompilation();

            return new RubyProc(getContext().getCoreLibrary().getProcClass(), RubyProc.Type.PROC,
                    block.getSharedMethodInfo(), block.getCallTarget(), block.getCallTarget(), block.getDeclarationFrame(),
                    block.getSelfCapturedInScope(), block.getBlockCapturedInScope());
        }
    }

    @CoreMethod(names = "raise", isModuleMethod = true, needsSelf = false, minArgs = 1, maxArgs = 2)
    public abstract static class RaiseNode extends CoreMethodNode {

        @Child protected DispatchHeadNode initialize;

        public RaiseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            initialize = new DispatchHeadNode(context);
        }

        public RaiseNode(RaiseNode prev) {
            super(prev);
            initialize = prev.initialize;
        }

        @Specialization
        public Object raise(VirtualFrame frame, RubyString message, @SuppressWarnings("unused") UndefinedPlaceholder undefined) {
            notDesignedForCompilation();

            return raise(frame, getContext().getCoreLibrary().getRuntimeErrorClass(), message);
        }

        @Specialization
        public Object raise(VirtualFrame frame, RubyClass exceptionClass, @SuppressWarnings("unused") UndefinedPlaceholder undefined) {
            notDesignedForCompilation();

            return raise(frame, exceptionClass, getContext().makeString(""));
        }

        @Specialization
        public Object raise(VirtualFrame frame, RubyClass exceptionClass, RubyString message) {
            notDesignedForCompilation();

            final RubyBasicObject exception = exceptionClass.newInstance(this);
            initialize.call(frame, exception, "initialize", null, message);
            throw new RaiseException(exception);
        }

    }

    @CoreMethod(names = "rand", isModuleMethod = true, needsSelf = false, maxArgs = 0, visibility = Visibility.PRIVATE)
    public abstract static class RandNode extends CoreMethodNode {

        public RandNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RandNode(RandNode prev) {
            super(prev);
        }

        @Specialization
        public double rand() {
            return Math.random();
        }

    }

    @CoreMethod(names = "require", isModuleMethod = true, needsSelf = false, minArgs = 1, maxArgs = 1)
    public abstract static class RequireNode extends CoreMethodNode {

        public RequireNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RequireNode(RequireNode prev) {
            super(prev);
        }

        @Specialization
        public boolean require(RubyString feature) {
            notDesignedForCompilation();

            try {
                getContext().getFeatureManager().require(feature.toString(), this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return true;
        }
    }

    @CoreMethod(names = "set_trace_func", isModuleMethod = true, needsSelf = false, minArgs = 1, maxArgs = 1)
    public abstract static class SetTraceFuncNode extends CoreMethodNode {

        public SetTraceFuncNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SetTraceFuncNode(SetTraceFuncNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder setTraceFunc(NilPlaceholder nil) {
            notDesignedForCompilation();

            getContext().getTraceManager().setTraceFunc(null);
            return nil;
        }

        @Specialization
        public RubyProc setTraceFunc(RubyProc traceFunc) {
            notDesignedForCompilation();

            getContext().getTraceManager().setTraceFunc(traceFunc);
            return traceFunc;
        }
    }

    @CoreMethod(names = "String", isModuleMethod = true, needsSelf = false, minArgs = 1, maxArgs = 1)
    public abstract static class StringNode extends CoreMethodNode {

        @Child protected DispatchHeadNode toS;

        public StringNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toS = new DispatchHeadNode(context);
        }

        public StringNode(StringNode prev) {
            super(prev);
            toS = prev.toS;
        }

        @Specialization
        public RubyString string(int value) {
            return getContext().makeString(Integer.toString(value));
        }

        @Specialization
        public RubyString string(BigInteger value) {
            return getContext().makeString(value.toString());
        }

        @Specialization
        public RubyString string(double value) {
            return getContext().makeString(Double.toString(value));
        }

        @Specialization
        public RubyString string(RubyString value) {
            return value;
        }

        @Specialization
        public Object string(VirtualFrame frame, Object value) {
            return toS.call(frame, value, "to_s", null);
        }

    }

    @CoreMethod(names = "sleep", isModuleMethod = true, needsSelf = false, maxArgs = 1)
    public abstract static class SleepNode extends CoreMethodNode {

        public SleepNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SleepNode(SleepNode prev) {
            super(prev);
        }

        @Specialization
        public double sleep(double duration) {
            return doSleep(duration);
        }

        @SlowPath
        private double doSleep(final double duration) {
            notDesignedForCompilation();

            return getContext().outsideGlobalLock(new Supplier<Double>() {

                @Override
                public Double get() {
                    final long start = System.nanoTime();

                    try {
                        Thread.sleep((long) (duration * 1000));
                    } catch (InterruptedException e) {
                        // Ignore interruption
                    }

                    final long end = System.nanoTime();

                    return (end - start) / 1e9;
                }

            });
        }

        @Specialization
        public double sleep(int duration) {
            return sleep((double) duration);
        }

    }

    @CoreMethod(names = "system", isModuleMethod = true, needsSelf = false, isSplatted = true)
    public abstract static class SystemNode extends CoreMethodNode {

        public SystemNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SystemNode(SystemNode prev) {
            super(prev);
        }

        @Specialization
        public Object fork(Object[] args) {
            notDesignedForCompilation();
            getContext().getWarnings().warn("Kernel#system not implemented - defined to satisfy some metaprogramming in RubySpec");
            return NilPlaceholder.INSTANCE;
        }

    }

    @CoreMethod(names = "throw", isModuleMethod = true, needsSelf = false, minArgs = 1, maxArgs = 2)
    public abstract static class ThrowNode extends CoreMethodNode {

        public ThrowNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ThrowNode(ThrowNode prev) {
            super(prev);
        }

        @Specialization
        public Object doThrow(Object tag, UndefinedPlaceholder value) {
            return doThrow(tag, (Object) value);
        }

        @Specialization
        public Object doThrow(Object tag, Object value) {
            notDesignedForCompilation();

            if (value instanceof UndefinedPlaceholder) {
                throw new ThrowException(tag, NilPlaceholder.INSTANCE);
            } else {
                throw new ThrowException(tag, value);
            }
        }

    }

    @CoreMethod(names = "truffelized?", isModuleMethod = true, needsSelf = false, maxArgs = 0)
    public abstract static class TruffelizedNode extends CoreMethodNode {

        public TruffelizedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TruffelizedNode(ThrowNode prev) {
            super(prev);
        }

        @Specialization
        public boolean truffelized() {
            return true;
        }

    }

}
