/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;

import org.jruby.common.IRubyWarnings;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.cast.*;
import org.jruby.truffle.nodes.control.*;
import org.jruby.truffle.nodes.dispatch.Dispatch;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.literal.*;
import org.jruby.truffle.nodes.yield.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.methods.RubyMethod;
import org.jruby.util.cli.Options;

@CoreClass(name = "Kernel")
public abstract class KernelNodes {

    /**
     * Check if operands are the same object or call #==.
     * Known as rb_equal() in MRI. The fact Kernel#=== uses this is pure coincidence.
     */
    @CoreMethod(names = "===", required = 1)
    public abstract static class SameOrEqualNode extends CoreMethodNode {

        @Child protected BasicObjectNodes.ReferenceEqualNode referenceEqualNode;
        @Child protected DispatchHeadNode equalNode;

        public SameOrEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            referenceEqualNode = BasicObjectNodesFactory.ReferenceEqualNodeFactory.create(context, sourceSection, new RubyNode[]{null, null});
            equalNode = new DispatchHeadNode(context);
        }

        public SameOrEqualNode(SameOrEqualNode prev) {
            super(prev);
            referenceEqualNode = prev.referenceEqualNode;
            equalNode = prev.equalNode;
        }

        public abstract boolean executeSameOrEqual(VirtualFrame frame, Object a, Object b);

        @Specialization
        public boolean sameOrEqual(VirtualFrame frame, Object a, Object b) {
            if (referenceEqualNode.executeEqual(frame, a, b))
                return true;
            return equalNode.callIsTruthy(frame, a, "==", null, b);
        }

    }

    @CoreMethod(names = "=~", required = 1, needsSelf = false)
    public abstract static class MatchNode extends CoreMethodNode {

        public MatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MatchNode(MatchNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass equal(Object other) {
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "!~", required = 1)
    public abstract static class NotMatchNode extends CoreMethodNode {

        @Child protected DispatchHeadNode matchNode;

        public NotMatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            matchNode = new DispatchHeadNode(context);
        }

        public NotMatchNode(NotMatchNode prev) {
            super(prev);
            matchNode = prev.matchNode;
        }

        @Specialization
        public boolean notMatch(VirtualFrame frame, Object self, Object other) {
            return !matchNode.callIsTruthy(frame, self, "=~", null, other);
        }

    }

    @CoreMethod(names = {"<=>"}, required = 1)
    public abstract static class CompareNode extends CoreMethodNode {

        @Child protected DispatchHeadNode equalNode;
        @Child protected BooleanCastNode booleanCast;

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            equalNode = new DispatchHeadNode(context);
            booleanCast = BooleanCastNodeFactory.create(context, sourceSection, null);
        }

        public CompareNode(CompareNode prev) {
            super(prev);
            equalNode = prev.equalNode;
            booleanCast = prev.booleanCast;
        }

        @Specialization
        public Object compare(VirtualFrame frame, RubyObject self, RubyObject other) {
            notDesignedForCompilation();

            if ((self == other) || booleanCast.executeBoolean(frame, equalNode.call(frame, self, "==", null, other))) {
                return 0;
            }

            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "abort", isModuleFunction = true)
    public abstract static class AbortNode extends CoreMethodNode {

        public AbortNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AbortNode(AbortNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass abort() {
            CompilerDirectives.transferToInterpreter();
            System.exit(1);
            return getContext().getCoreLibrary().getNilObject();
        }
    }

    @CoreMethod(names = "Array", isModuleFunction = true, argumentsAsArray = true)
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

    @CoreMethod(names = "at_exit", isModuleFunction = true, needsBlock = true)
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
            return getContext().getCoreLibrary().getNilObject();
        }
    }

    @CoreMethod(names = "binding", isModuleFunction = true)
    public abstract static class BindingNode extends CoreMethodNode {

        public BindingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BindingNode(BindingNode prev) {
            super(prev);
        }

        @Specialization
        public Object binding() {
            // Materialize the caller's frame - false means don't use a slow path to get it - we want to optimize it

            final MaterializedFrame callerFrame = Truffle.getRuntime().getCallerFrame()
                    .getFrame(FrameInstance.FrameAccess.MATERIALIZE, false).materialize();

            return new RubyBinding(
                    getContext().getCoreLibrary().getBindingClass(),
                    RubyArguments.getSelf(callerFrame.getArguments()),
                    callerFrame);
        }
    }

    @CoreMethod(names = "block_given?", isModuleFunction = true)
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

    @CoreMethod(names = "callcc", isModuleFunction = true, needsBlock = true)
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

    @CoreMethod(names = "catch", isModuleFunction = true, needsBlock = true, required = 1)
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
                getContext().getThrowTags().add(tag);

                return yield(frame, block);
            } catch (ThrowException e) {
                if (e.getTag().equals(tag)) {
                    // TODO(cs): unset rather than set to Nil?
                    getContext().getCoreLibrary().getGlobalVariablesObject().setInstanceVariable("$!", getContext().getCoreLibrary().getNilObject());
                    return e.getValue();
                } else {
                    throw e;
                }
            } finally {
                getContext().getThrowTags().remove();
            }
        }
    }

    @CoreMethod(names = "class")
    public abstract static class ClassNode extends CoreMethodNode {

        public ClassNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ClassNode(ClassNode prev) {
            super(prev);
        }

        @Specialization
        public RubyClass getClass(boolean value) {
            notDesignedForCompilation();

            if (value) {
                return getContext().getCoreLibrary().getTrueClass();
            } else {
                return getContext().getCoreLibrary().getFalseClass();
            }
        }

        @Specialization
        public RubyClass getClass(@SuppressWarnings("unused") int value) {
            return getContext().getCoreLibrary().getFixnumClass();
        }

        @Specialization
        public RubyClass getClass(@SuppressWarnings("unused") BigInteger value) {
            return getContext().getCoreLibrary().getBignumClass();
        }

        @Specialization
        public RubyClass getClass(@SuppressWarnings("unused") double value) {
            return getContext().getCoreLibrary().getFloatClass();
        }

        @Specialization
        public RubyClass getClass(RubyBasicObject self) {
            return self.getLogicalClass();
        }

    }

    @CoreMethod(names = {"dup", "clone"})
    public abstract static class DupNode extends CoreMethodNode {

        @Child protected DispatchHeadNode initializeDupNode;

        public DupNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            // Calls private initialize_dup on the new copy.
            initializeDupNode = new DispatchHeadNode(context, true, Dispatch.MissingBehavior.CALL_METHOD_MISSING);
        }

        public DupNode(DupNode prev) {
            super(prev);
            initializeDupNode = prev.initializeDupNode;
        }

        @Specialization
        public Object dup(VirtualFrame frame, RubyModule self) {
            notDesignedForCompilation();

            final RubyBasicObject newObject = self.getLogicalClass().newInstance(this);
            newObject.setInstanceVariables(self.getFields());
            initializeDupNode.call(frame, newObject, "initialize_dup", null, self);
            return newObject;
        }

        @Specialization
        public Object dup(VirtualFrame frame, RubyObject self) {
            notDesignedForCompilation();

            final RubyObject newObject = new RubyObject(self.getLogicalClass());
            newObject.setInstanceVariables(self.getFields());
            initializeDupNode.call(frame, newObject, "initialize_dup", null, self);
            return newObject;
        }

    }

    @CoreMethod(names = "eql?", required = 1)
    public abstract static class EqlNode extends BasicObjectNodes.ReferenceEqualNode {
        public EqlNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EqlNode(EqlNode prev) {
            super(prev);
        }
    }

    @CoreMethod(names = "eval", isModuleFunction = true, required = 1, optional = 1)
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

    @CoreMethod(names = "exec", isModuleFunction = true, required = 1, argumentsAsArray = true)
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

        @TruffleBoundary
        private static void exec(RubyContext context, String[] commandLine) {
            final ProcessBuilder builder = new ProcessBuilder(commandLine);
            builder.inheritIO();

            final RubyHash env = context.getCoreLibrary().getENV();

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

    @CoreMethod(names = "exit", isModuleFunction = true, optional = 1, lowerFixnumParameters = 0)
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

    @CoreMethod(names = "exit!", isModuleFunction = true)
    public abstract static class ExitBangNode extends CoreMethodNode {

        public ExitBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ExitBangNode(ExitBangNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass exit() {
            CompilerDirectives.transferToInterpreter();
            System.exit(1);
            return getContext().getCoreLibrary().getNilObject();
        }
    }

    @CoreMethod(names = "extend", argumentsAsArray = true, required = 1)
    public abstract static class ExtendNode extends CoreMethodNode {

        public ExtendNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ExtendNode(ExtendNode prev) {
            super(prev);
        }

        @Specialization
        public RubyBasicObject extend(RubyBasicObject self, Object[] args) {
            notDesignedForCompilation();

            for (int n = 0; n < args.length; n++) {
                self.extend((RubyModule) args[n], this);
            }

            return self;
        }

    }

    @CoreMethod(names = "fork", isModuleFunction = true, argumentsAsArray = true)
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
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "freeze")
    public abstract static class FreezeNode extends CoreMethodNode {

        public FreezeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FreezeNode(FreezeNode prev) {
            super(prev);
        }

        @Specialization
        public RubyObject freeze(RubyObject self) {
            notDesignedForCompilation();

            self.frozen = true;
            return self;
        }

    }

    @CoreMethod(names = "frozen?")
    public abstract static class FrozenNode extends CoreMethodNode {

        public FrozenNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FrozenNode(FrozenNode prev) {
            super(prev);
        }

        @Specialization
        public boolean isFrozen(RubyObject self) {
            notDesignedForCompilation();

            return self.frozen;
        }

    }

    @CoreMethod(names = "gets", isModuleFunction = true)
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

            final String line;

            final RubyThread runningThread = getContext().getThreadManager().leaveGlobalLock();

            try {
                line = gets(context);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                getContext().getThreadManager().enterGlobalLock(runningThread);
            }

            final RubyString rubyLine = context.makeString(line);

            // Set the local variable $_ in the caller

            final FrameSlot slot = caller.getFrameDescriptor().findFrameSlot("$_");

            if (slot != null) {
                caller.setObject(slot, rubyLine);
            }

            return rubyLine;
        }

        @TruffleBoundary
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

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodNode {

        public HashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public HashNode(HashNode prev) {
            super(prev);
        }

        @Specialization
        public int hash(int value) {
            return value;
        }

        @Specialization
        public int hash(long value) {
            return (int) (value ^ value >>> 32);
        }

        @Specialization
        public int hash(BigInteger value) {
            return value.hashCode();
        }

        @Specialization
        public int hash(RubyObject self) {
            notDesignedForCompilation();

            return self.hashCode();
        }

    }

    @CoreMethod(names = "initialize_copy", visibility = Visibility.PRIVATE, required = 1)
    public abstract static class InitializeCopyNode extends CoreMethodNode {

        public InitializeCopyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeCopyNode(InitializeCopyNode prev) {
            super(prev);
        }

        @Specialization
        public Object initializeCopy(RubyObject self, RubyObject other) {
            notDesignedForCompilation();

            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "initialize_dup", visibility = Visibility.PRIVATE, required = 1)
    public abstract static class InitializeDupNode extends CoreMethodNode {

        @Child protected DispatchHeadNode initializeCopyNode;

        public InitializeDupNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            initializeCopyNode = DispatchHeadNode.onSelf(context);
        }

        public InitializeDupNode(InitializeDupNode prev) {
            super(prev);
            initializeCopyNode = prev.initializeCopyNode;
        }

        @Specialization
        public Object initializeDup(VirtualFrame frame, RubyObject self, RubyObject other) {
            notDesignedForCompilation();
            return initializeCopyNode.call(frame, self, "initialize_copy", null, other);
        }

    }

    @CoreMethod(names = "instance_of?", required = 1)
    public abstract static class InstanceOfNode extends CoreMethodNode {

        public InstanceOfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InstanceOfNode(InstanceOfNode prev) {
            super(prev);
        }

        @SlowPath
        @Specialization
        public boolean instanceOf(Object self, RubyClass rubyClass) {
            // TODO(CS): fast path
            return getContext().getCoreLibrary().box(self).getLogicalClass() == rubyClass;
        }

    }

    @CoreMethod(names = "instance_variable_defined?", required = 1)
    public abstract static class InstanceVariableDefinedNode extends CoreMethodNode {

        public InstanceVariableDefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InstanceVariableDefinedNode(InstanceVariableDefinedNode prev) {
            super(prev);
        }

        @Specialization
        public boolean isInstanceVariableDefined(RubyBasicObject object, RubyString name) {
            notDesignedForCompilation();

            return object.isFieldDefined(RubyObject.checkInstanceVariableName(getContext(), name.toString(), this));
        }

        @Specialization
        public boolean isInstanceVariableDefined(RubyBasicObject object, RubySymbol name) {
            notDesignedForCompilation();

            return object.isFieldDefined(RubyObject.checkInstanceVariableName(getContext(), name.toString(), this));
        }

    }

    @CoreMethod(names = "instance_variable_get", required = 1)
    public abstract static class InstanceVariableGetNode extends CoreMethodNode {

        public InstanceVariableGetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InstanceVariableGetNode(InstanceVariableGetNode prev) {
            super(prev);
        }

        @Specialization
        public Object isInstanceVariableGet(RubyBasicObject object, RubyString name) {
            notDesignedForCompilation();

            return object.getInstanceVariable(RubyObject.checkInstanceVariableName(getContext(), name.toString(), this));
        }

        @Specialization
        public Object isInstanceVariableGet(RubyBasicObject object, RubySymbol name) {
            notDesignedForCompilation();

            return object.getInstanceVariable(RubyObject.checkInstanceVariableName(getContext(), name.toString(), this));
        }

    }

    @CoreMethod(names = "instance_variable_set", required = 2)
    public abstract static class InstanceVariableSetNode extends CoreMethodNode {

        public InstanceVariableSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InstanceVariableSetNode(InstanceVariableSetNode prev) {
            super(prev);
        }

        @Specialization
        public Object isInstanceVariableSet(RubyBasicObject object, RubyString name, Object value) {
            notDesignedForCompilation();

            object.setInstanceVariable(RubyObject.checkInstanceVariableName(getContext(), name.toString(), this), value);
            return value;
        }

        @Specialization
        public Object isInstanceVariableSet(RubyBasicObject object, RubySymbol name, Object value) {
            notDesignedForCompilation();

            object.setInstanceVariable(RubyObject.checkInstanceVariableName(getContext(), name.toString(), this), value);
            return value;
        }

    }

    @CoreMethod(names = "instance_variables")
    public abstract static class InstanceVariablesNode extends CoreMethodNode {

        public InstanceVariablesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InstanceVariablesNode(InstanceVariablesNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray instanceVariables(RubyObject self) {
            notDesignedForCompilation();

            final String[] instanceVariableNames = self.getFieldNames();

            Arrays.sort(instanceVariableNames);

            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass());

            for (String name : instanceVariableNames) {
                array.slowPush(RubyString.fromJavaString(getContext().getCoreLibrary().getStringClass(), name));
            }

            return array;
        }

    }

    @CoreMethod(names = "Integer", isModuleFunction = true, required = 1)
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

    @CoreMethod(names = {"is_a?", "kind_of?"}, required = 1)
    public abstract static class IsANode extends CoreMethodNode {

        public IsANode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IsANode(IsANode prev) {
            super(prev);
        }

        @Specialization
        public boolean isA(@SuppressWarnings("unused") RubyBasicObject self, @SuppressWarnings("unused") RubyNilClass nil) {
            return false;
        }

        @TruffleBoundary
        @Specialization
        public boolean isA(Object self, RubyClass rubyClass) {
            // TODO(CS): fast path
            return ModuleOperations.assignableTo(getContext().getCoreLibrary().box(self).getMetaClass(), rubyClass);
        }

    }

    @CoreMethod(names = "lambda", isModuleFunction = true, needsBlock = true)
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

    @CoreMethod(names = "load", isModuleFunction = true, required = 1)
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

    @CoreMethod(names = "loop", isModuleFunction = true)
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

    @CoreMethod(names = "methods", optional = 1)
    public abstract static class MethodsNode extends CoreMethodNode {

        public MethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MethodsNode(MethodsNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray methods(RubyObject self, boolean includeInherited) {
            notDesignedForCompilation();

            if (!includeInherited) {
                getContext().getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection().getSource().getName(), Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection().getStartLine(), "Object#methods always returns inherited methods at the moment");
            }

            return methods(self, UndefinedPlaceholder.INSTANCE);
        }

        @Specialization
        public RubyArray methods(RubyObject self, @SuppressWarnings("unused") UndefinedPlaceholder includeInherited) {
            notDesignedForCompilation();

            final RubyArray array = new RubyArray(self.getContext().getCoreLibrary().getArrayClass());

            final Map<String, RubyMethod> methods = ModuleOperations.getAllMethods(self.getMetaClass());

            for (RubyMethod method : methods.values()) {
                if (method.getVisibility() == Visibility.PUBLIC || method.getVisibility() == Visibility.PROTECTED) {
                    array.slowPush(self.getContext().newSymbol(method.getName()));
                }
            }

            return array;
        }

    }

    @CoreMethod(names = "nil?", needsSelf = false)
    public abstract static class NilNode extends CoreMethodNode {

        public NilNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NilNode(NilNode prev) {
            super(prev);
        }

        @Specialization
        public boolean nil() {
            return false;
        }
    }

    @CoreMethod(names = "object_id", needsSelf = true)
    public abstract static class ObjectIDNode extends CoreMethodNode {

        public ObjectIDNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ObjectIDNode(ObjectIDNode prev) {
            super(prev);
        }

        @Specialization
        public long objectID(int fixnum) {
            notDesignedForCompilation();

            return ObjectIDOperations.fixnumToID(fixnum);
        }

        @Specialization
        public long objectID(RubyBasicObject object) {
            notDesignedForCompilation();

            return object.getObjectID();
        }

    }

    @CoreMethod(names = "print", isModuleFunction = true, argumentsAsArray = true)
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
        public RubyNilClass print(final VirtualFrame frame, final Object[] args) {
            final RubyThread runningThread = getContext().getThreadManager().leaveGlobalLock();

            try {
                for (Object arg : args) {
                    write(((RubyString) toS.call(frame, arg, "to_s", null)).getBytes().bytes());
                }
            } finally {
                getContext().getThreadManager().enterGlobalLock(runningThread);
            }

            return getContext().getCoreLibrary().getNilObject();
        }

        @TruffleBoundary
        private void write(byte[] bytes) {
            try{
                getContext().getRuntime().getInstanceConfig().getOutput().write(bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    @CoreMethod(names = "printf", isModuleFunction = true, argumentsAsArray = true)
    public abstract static class PrintfNode extends CoreMethodNode {

        public PrintfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PrintfNode(PrintfNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass printf(Object[] args) {
            notDesignedForCompilation();

            if (args.length > 0) {
                final String format = args[0].toString();
                final List<Object> values = Arrays.asList(args).subList(1, args.length);

                final RubyThread runningThread = getContext().getThreadManager().leaveGlobalLock();

                try {
                    StringFormatter.format(getContext().getRuntime().getInstanceConfig().getOutput(), format, values);
                } finally {
                    getContext().getThreadManager().enterGlobalLock(runningThread);
                }
            }

            return getContext().getCoreLibrary().getNilObject();
        }
    }

    /*
     * Kernel#pretty_inspect is normally part of stdlib, in pp.rb, but we aren't able to execute
     * that file yet. Instead we implement a very simple version here, which is the solution
     * suggested by RubySpec.
     */

    @CoreMethod(names = "pretty_inspect")
    public abstract static class PrettyInspectNode extends CoreMethodNode {

        @Child protected DispatchHeadNode toS;

        public PrettyInspectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toS = DispatchHeadNode.onSelf(context);
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

    @CoreMethod(names = "proc", isModuleFunction = true, needsBlock = true)
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
                    block.getSharedMethodInfo(), block.getCallTarget(), block.getCallTargetForMethods(), block.getDeclarationFrame(),
                    block.getSelfCapturedInScope(), block.getBlockCapturedInScope());
        }
    }

    @CoreMethod(names = "public_methods", optional = 1)
    public abstract static class PublicMethodsNode extends CoreMethodNode {

        public PublicMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PublicMethodsNode(PublicMethodsNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray methods(RubyObject self, boolean includeInherited) {
            notDesignedForCompilation();

            if (!includeInherited) {
                getContext().getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection().getSource().getName(), Truffle.getRuntime().getCallerFrame().getCallNode().getEncapsulatingSourceSection().getStartLine(), "Object#methods always returns inherited methods at the moment");
            }

            return methods(self, UndefinedPlaceholder.INSTANCE);
        }

        @Specialization
        public RubyArray methods(RubyObject self, @SuppressWarnings("unused") UndefinedPlaceholder includeInherited) {
            notDesignedForCompilation();

            final RubyArray array = new RubyArray(self.getContext().getCoreLibrary().getArrayClass());

            final Map<String, RubyMethod> methods = self.getMetaClass().getMethods();

            for (RubyMethod method : methods.values()) {
                if (method.getVisibility() == Visibility.PUBLIC) {
                    array.slowPush(self.getContext().newSymbol(method.getName()));
                }
            }

            return array;
        }

    }

    @CoreMethod(names = "raise", isModuleFunction = true, optional = 2)
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
        public Object raise(VirtualFrame frame, UndefinedPlaceholder undefined1, @SuppressWarnings("unused") UndefinedPlaceholder undefined2) {
            notDesignedForCompilation();

            return raise(frame, getContext().getCoreLibrary().getRuntimeErrorClass(), getContext().makeString("re-raised - don't have the current exception yet!"));
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

    @CoreMethod(names = "rand", isModuleFunction = true, optional = 1)
    public abstract static class RandNode extends CoreMethodNode {

        public RandNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RandNode(RandNode prev) {
            super(prev);
        }

        @Specialization
        public double rand(UndefinedPlaceholder undefined) {
            return getContext().getRandom().nextDouble();
        }

        @Specialization(guards = "isZero")
        public double randZero(int max) {
            return getContext().getRandom().nextDouble();
        }

        @Specialization(guards = "isNonZero")
        public int randNonZero(int max) {
            return getContext().getRandom().nextInt(max);
        }

        protected boolean isZero(int max) {
            return max == 0;
        }

        protected boolean isNonZero(int max) {
            return max != 0;
        }

    }

    @CoreMethod(names = "require", isModuleFunction = true, required = 1)
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

    @CoreMethod(names = "respond_to?", required = 1, optional = 1)
    public abstract static class RespondToNode extends CoreMethodNode {

        @Child protected DispatchHeadNode dispatch;
        @Child protected DispatchHeadNode dispatchIgnoreVisibility;

        public RespondToNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);

            dispatch = new DispatchHeadNode(context, false, Dispatch.MissingBehavior.CALL_METHOD_MISSING);
            dispatchIgnoreVisibility = new DispatchHeadNode(context, true, Dispatch.MissingBehavior.CALL_METHOD_MISSING);

            if (Options.TRUFFLE_DISPATCH_METAPROGRAMMING_ALWAYS_UNCACHED.load()) {
                dispatch.forceUncached();
                dispatchIgnoreVisibility.forceUncached();
            }
        }

        public RespondToNode(RespondToNode prev) {
            super(prev);
            dispatch = prev.dispatch;
            dispatchIgnoreVisibility = prev.dispatchIgnoreVisibility;
        }

        @Specialization
        public boolean doesRespondTo(VirtualFrame frame, Object object, RubyString name, @SuppressWarnings("unused") UndefinedPlaceholder checkVisibility) {
            return dispatch.doesRespondTo(frame, name, object);
        }

        @Specialization
        public boolean doesRespondTo(VirtualFrame frame, Object object, RubyString name, boolean ignoreVisibility) {
            if (ignoreVisibility) {
                return dispatchIgnoreVisibility.doesRespondTo(frame, name, object);
            } else {
                return dispatch.doesRespondTo(frame, name, object);
            }
        }

        @Specialization
        public boolean doesRespondTo(VirtualFrame frame, Object object, RubySymbol name, @SuppressWarnings("unused") UndefinedPlaceholder checkVisibility) {
            return dispatch.doesRespondTo(frame, name, object);
        }

        @Specialization
        public boolean doesRespondTo(VirtualFrame frame, Object object, RubySymbol name, boolean ignoreVisibility) {
            if (ignoreVisibility) {
                return dispatchIgnoreVisibility.doesRespondTo(frame, name, object);
            } else {
                return dispatch.doesRespondTo(frame, name, object);
            }
        }

    }

    @CoreMethod(names = "respond_to_missing?", required = 1, optional = 1, visibility = Visibility.PRIVATE)
    public abstract static class RespondToMissingNode extends CoreMethodNode {

        public RespondToMissingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RespondToMissingNode(RespondToMissingNode prev) {
            super(prev);
        }

        @Specialization
        public boolean doesRespondToMissing(Object object, RubyString name, UndefinedPlaceholder includeAll) {
            return false;
        }

        @Specialization
        public boolean doesRespondToMissing(Object object, RubySymbol name, UndefinedPlaceholder includeAll) {
            return false;
        }

        @Specialization
        public boolean doesRespondToMissing(Object object, RubySymbol name, boolean includeAll) {
            return false;
        }

        @Specialization
        public boolean doesRespondToMissing(Object object, RubyString name, boolean includeAll) {
            return false;
        }

    }

    @CoreMethod(names = "send", needsBlock = true, required = 1, argumentsAsArray = true)
    public abstract static class SendNode extends BasicObjectNodes.SendNode {
        public SendNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SendNode(SendNode prev) {
            super(prev);
        }
    }

    @CoreMethod(names = "set_trace_func", isModuleFunction = true, required = 1)
    public abstract static class SetTraceFuncNode extends CoreMethodNode {

        public SetTraceFuncNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SetTraceFuncNode(SetTraceFuncNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass setTraceFunc(RubyNilClass nil) {
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

    @CoreMethod(names = "singleton_class")
    public abstract static class SingletonClassMethodNode extends CoreMethodNode {

        public SingletonClassMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SingletonClassMethodNode(SingletonClassMethodNode prev) {
            super(prev);
        }

        @Specialization
        public RubyClass singletonClass(Object self) {
            notDesignedForCompilation();

            return getContext().getCoreLibrary().box(self).getSingletonClass(this);
        }

    }

    @CoreMethod(names = "singleton_methods", optional = 1)
    public abstract static class SingletonMethodsNode extends CoreMethodNode {

        public SingletonMethodsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SingletonMethodsNode(SingletonMethodsNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray singletonMethods(RubyObject self, boolean includeInherited) {
            notDesignedForCompilation();

            final RubyArray array = new RubyArray(self.getContext().getCoreLibrary().getArrayClass());

            final Collection<RubyMethod> methods;

            if (includeInherited) {
                methods = ModuleOperations.getAllMethods(self.getSingletonClass(this)).values();
            } else {
                methods = self.getSingletonClass(this).getMethods().values();
            }

            for (RubyMethod method : methods) {
                array.slowPush(RubySymbol.newSymbol(self.getContext(), method.getName()));
            }

            return array;
        }

        @Specialization
        public RubyArray singletonMethods(RubyObject self, @SuppressWarnings("unused") UndefinedPlaceholder includeInherited) {
            return singletonMethods(self, false);
        }

    }

    @CoreMethod(names = "String", isModuleFunction = true, required = 1)
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

    @CoreMethod(names = "sleep", isModuleFunction = true, optional = 1)
    public abstract static class SleepNode extends CoreMethodNode {

        public SleepNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SleepNode(SleepNode prev) {
            super(prev);
        }

        @Specialization
        public double sleep(UndefinedPlaceholder duration) {
            return doSleep(0);
        }

        @Specialization
        public double sleep(int duration) {
            return doSleep(duration);
        }

        @Specialization
        public double sleep(long duration) {
            return doSleep(duration);
        }

        @Specialization
        public double sleep(double duration) {
            return doSleep(duration);
        }

        @TruffleBoundary
        private double doSleep(final double duration) {
            final RubyThread runningThread = getContext().getThreadManager().leaveGlobalLock();

            try {
                final long start = System.nanoTime();

                try {
                    Thread.sleep((long) (duration * 1000));
                } catch (InterruptedException e) {
                    // Ignore interruption
                }

                final long end = System.nanoTime();

                return (end - start) / 1e9;
            } finally {
                getContext().getThreadManager().enterGlobalLock(runningThread);
            }
        }

    }

    @CoreMethod(names = "system", isModuleFunction = true, argumentsAsArray = true)
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
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "throw", isModuleFunction = true, required = 1, optional = 1)
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

            if (!getContext().getThrowTags().contains(tag)) {
                throw new RaiseException(new RubyException(
                        getContext().getCoreLibrary().getArgumentErrorClass(),
                        getContext().makeString(String.format("uncaught throw \"%s\"", tag)),
                        RubyCallStack.getBacktrace(this)));
            }

            if (value instanceof UndefinedPlaceholder) {
                throw new ThrowException(tag, getContext().getCoreLibrary().getNilObject());
            } else {
                throw new ThrowException(tag, value);
            }
        }

    }

    @CoreMethod(names = {"to_s", "inspect"})
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString toS(RubyBasicObject self) {
            notDesignedForCompilation();

            return getContext().makeString("#<" + self.getLogicalClass().getName() + ":0x" + Long.toHexString(self.getObjectID()) + ">");
        }

    }

    @CoreMethod(names = "truffelized?", isModuleFunction = true)
    public abstract static class TruffelizedNode extends CoreMethodNode {

        public TruffelizedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TruffelizedNode(TruffelizedNode prev) {
            super(prev);
        }

        @Specialization
        public boolean truffelized() {
            return true;
        }

    }

    // Rubinius API
    @CoreMethod(names = "undefined", isModuleFunction = true)
    public abstract static class UndefinedNode extends CoreMethodNode {

        public UndefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public UndefinedNode(UndefinedNode prev) {
            super(prev);
        }

        @Specialization
        public UndefinedPlaceholder undefined() {
            return UndefinedPlaceholder.INSTANCE;
        }

    }

    // Rubinius API
    @CoreMethod(names = "StringValue", isModuleFunction = true, required = 1)
    public abstract static class StringValueNode extends CoreMethodNode {
        @Child
        protected DispatchHeadNode argToStringNode;

        public StringValueNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            argToStringNode = new DispatchHeadNode(context);
        }

        public StringValueNode(StringValueNode prev) {
            super(prev);
            argToStringNode = prev.argToStringNode;
        }

        @Specialization
        public RubyString StringValue(VirtualFrame frame, Object arg) {
            return (RubyString) argToStringNode.call(frame, arg, "to_s", null);
        }
    }
}
