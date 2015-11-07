/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.SourceSection;

import org.jcodings.specific.UTF8Encoding;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Helpers;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.methods.DeclarationContext;
import org.jruby.truffle.nodes.objects.AllocateObjectNode;
import org.jruby.truffle.nodes.objects.AllocateObjectNodeGen;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.StringOperations;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;
import org.jruby.truffle.runtime.util.ArgumentDescriptorUtils;
import org.jruby.util.StringSupport;

@CoreClass(name = "Proc")
public abstract class ProcNodes {

    public static Object[] packArguments(DynamicObject proc, Object... args) {
        return RubyArguments.pack(
                Layouts.PROC.getMethod(proc),
                Layouts.PROC.getDeclarationFrame(proc),
                null, Layouts.PROC.getSelf(proc),
                Layouts.PROC.getBlock(proc),
                DeclarationContext.BLOCK,
                args);
    }

    public static Object rootCall(DynamicObject proc, Object... args) {
        assert RubyGuards.isRubyProc(proc);

        return Layouts.PROC.getCallTargetForType(proc).call(packArguments(proc, args));
    }

    public static DynamicObject createRubyProc(DynamicObject procClass, Type type, SharedMethodInfo sharedMethodInfo, CallTarget callTargetForProcs,
                                               CallTarget callTargetForLambdas, MaterializedFrame declarationFrame, InternalMethod method,
                                               Object self, DynamicObject block) {
        return createRubyProc(Layouts.CLASS.getInstanceFactory(procClass),
                type, sharedMethodInfo, callTargetForProcs,
                callTargetForLambdas, declarationFrame, method,
                self, block);
    }

    public static DynamicObject createRubyProc(DynamicObjectFactory instanceFactory, Type type, SharedMethodInfo sharedMethodInfo, CallTarget callTargetForProcs,
                                          CallTarget callTargetForLambdas, MaterializedFrame declarationFrame, InternalMethod method,
                                          Object self, DynamicObject block) {
        assert block == null || RubyGuards.isRubyProc(block);
        final CallTarget callTargetForType = (type == Type.PROC) ? callTargetForProcs : callTargetForLambdas;
        return Layouts.PROC.createProc(instanceFactory, type, sharedMethodInfo, callTargetForType, callTargetForLambdas, declarationFrame, method, self, block);
    }

    public enum Type {
        PROC, LAMBDA
    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            throw new RaiseException(getContext().getCoreLibrary().typeErrorAllocatorUndefinedFor(rubyClass, this));
        }

    }

    @CoreMethod(names = "new", constructor = true, needsBlock = true, rest = true)
    public abstract static class ProcNewNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode initializeNode;
        @Child private AllocateObjectNode allocateObjectNode;

        protected final DynamicObject PROC_CLASS = getContext().getCoreLibrary().getProcClass();
        protected final Shape PROC_SHAPE = getContext().getCoreLibrary().getProcFactory().getShape();

        public ProcNewNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            initializeNode = DispatchHeadNodeFactory.createMethodCallOnSelf(context);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        public abstract DynamicObject executeProcNew(VirtualFrame frame, DynamicObject procClass, Object[] args, Object block);

        @Specialization
        public DynamicObject proc(VirtualFrame frame, DynamicObject procClass, Object[] args, NotProvided block) {
            final Frame parentFrame = RubyCallStack.getCallerFrame(getContext()).getFrame(FrameAccess.READ_ONLY, true);
            final DynamicObject parentBlock = RubyArguments.getBlock(parentFrame.getArguments());

            if (parentBlock == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("tried to create Proc object without a block", this));
            }

            return executeProcNew(frame, procClass, args, parentBlock);
        }

        @Specialization(guards = { "procClass == PROC_CLASS", "block.getShape() == PROC_SHAPE" })
        public DynamicObject procNormalOptimized(DynamicObject procClass, Object[] args, DynamicObject block) {
            return block;
        }

        @Specialization(guards = "procClass == metaClass(block)")
        public DynamicObject procNormal(DynamicObject procClass, Object[] args, DynamicObject block) {
            return block;
        }

        @Specialization(guards = "procClass != metaClass(block)")
        public DynamicObject procSpecial(VirtualFrame frame, DynamicObject procClass, Object[] args, DynamicObject block) {
            // Instantiate a new instance of procClass as classes do not correspond
            DynamicObject proc = allocateObjectNode.allocate(
                    procClass,
                    Layouts.PROC.getType(block),
                    Layouts.PROC.getSharedMethodInfo(block),
                    Layouts.PROC.getCallTargetForType(block),
                    Layouts.PROC.getCallTargetForLambdas(block),
                    Layouts.PROC.getDeclarationFrame(block),
                    Layouts.PROC.getMethod(block),
                    Layouts.PROC.getSelf(block),
                    Layouts.PROC.getBlock(block));
            initializeNode.call(frame, proc, "initialize", block, args);
            return proc;
        }

        protected DynamicObject metaClass(DynamicObject object) {
            return Layouts.BASIC_OBJECT.getMetaClass(object);
        }

    }

    @CoreMethod(names = { "dup", "clone" })
    public abstract static class DupNode extends UnaryCoreMethodNode {

        @Child private AllocateObjectNode allocateObjectNode;

        public DupNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public DynamicObject dup(DynamicObject proc) {
            DynamicObject copy = allocateObjectNode.allocate(
                    Layouts.BASIC_OBJECT.getLogicalClass(proc),
                    Layouts.PROC.getType(proc),
                    Layouts.PROC.getSharedMethodInfo(proc),
                    Layouts.PROC.getCallTargetForType(proc),
                    Layouts.PROC.getCallTargetForLambdas(proc),
                    Layouts.PROC.getDeclarationFrame(proc),
                    Layouts.PROC.getMethod(proc),
                    Layouts.PROC.getSelf(proc),
                    Layouts.PROC.getBlock(proc));
            return copy;
        }

    }

    @CoreMethod(names = "arity")
    public abstract static class ArityNode extends CoreMethodArrayArgumentsNode {

        public ArityNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int arity(DynamicObject proc) {
            return Layouts.PROC.getSharedMethodInfo(proc).getArity().getArityNumber();
        }

    }

    @CoreMethod(names = "binding")
    public abstract static class BindingNode extends CoreMethodArrayArgumentsNode {

        public BindingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject binding(DynamicObject proc) {
            final MaterializedFrame frame = Layouts.PROC.getDeclarationFrame(proc);
            return BindingNodes.createBinding(getContext(), frame);
        }

    }

    @CoreMethod(names = {"call", "[]", "yield"}, rest = true, needsBlock = true)
    public abstract static class CallNode extends CoreMethodArrayArgumentsNode {

        @Child private YieldDispatchHeadNode yieldNode;

        public CallNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            yieldNode = new YieldDispatchHeadNode(context);
        }

        @Specialization
        public Object call(VirtualFrame frame, DynamicObject proc, Object[] args, NotProvided block) {
            return yieldNode.dispatch(frame, proc, args);
        }

        @Specialization
        public Object call(VirtualFrame frame, DynamicObject proc, Object[] args, DynamicObject block) {
            return yieldNode.dispatchWithModifiedBlock(frame, proc, block, args);
        }

    }

    @CoreMethod(names = "lambda?")
    public abstract static class LambdaNode extends CoreMethodArrayArgumentsNode {

        public LambdaNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean lambda(DynamicObject proc) {
            return Layouts.PROC.getType(proc) == Type.LAMBDA;
        }

    }

    @CoreMethod(names = "parameters")
    public abstract static class ParametersNode extends CoreMethodArrayArgumentsNode {

        public ParametersNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject parameters(DynamicObject proc) {
            final ArgumentDescriptor[] argsDesc = Layouts.PROC.getSharedMethodInfo(proc).getArgumentDescriptors();

            return ArgumentDescriptorUtils.argumentDescriptorsToParameters(getContext(), argsDesc, Layouts.PROC.getType(proc) == Type.LAMBDA);
        }

    }

    @CoreMethod(names = "source_location")
    public abstract static class SourceLocationNode extends CoreMethodArrayArgumentsNode {

        public SourceLocationNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object sourceLocation(DynamicObject proc) {
            SourceSection sourceSection = Layouts.PROC.getSharedMethodInfo(proc).getSourceSection();

            if (sourceSection.getSource() == null) {
                return nil();
            } else {
                DynamicObject file = createString(StringOperations.encodeByteList(sourceSection.getSource().getName(), UTF8Encoding.INSTANCE));
                Object[] objects = new Object[]{file, sourceSection.getStartLine()};
                return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), objects, objects.length);
            }
        }

    }

}
