/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.proc;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.UnaryCoreMethodNode;
import org.jruby.truffle.core.binding.BindingNodes;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.arguments.ArgumentDescriptorUtils;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.AllocateObjectNodeGen;
import org.jruby.truffle.language.yield.YieldNode;

@CoreClass(name = "Proc")
public abstract class ProcNodes {

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            throw new RaiseException(coreLibrary().typeErrorAllocatorUndefinedFor(rubyClass, this));
        }

    }

    @CoreMethod(names = "new", constructor = true, needsBlock = true, rest = true)
    public abstract static class ProcNewNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode initializeNode;
        @Child private AllocateObjectNode allocateObjectNode;

        protected final DynamicObject PROC_CLASS = coreLibrary().getProcClass();
        protected final Shape PROC_SHAPE = coreLibrary().getProcFactory().getShape();

        public ProcNewNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            initializeNode = DispatchHeadNodeFactory.createMethodCallOnSelf(context);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        public abstract DynamicObject executeProcNew(
                VirtualFrame frame,
                DynamicObject procClass,
                Object[] args,
                Object block);

        @Specialization
        public DynamicObject proc(VirtualFrame frame, DynamicObject procClass, Object[] args, NotProvided block) {
            final Frame parentFrame = getContext().getCallStack().getCallerFrameIgnoringSend()
                    .getFrame(FrameAccess.READ_ONLY, true);

            final DynamicObject parentBlock = RubyArguments.getBlock(parentFrame.getArguments());

            if (parentBlock == null) {
                throw new RaiseException(coreLibrary().argumentErrorProcWithoutBlock(this));
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

            final DynamicObject proc = allocateObjectNode.allocate(
                    procClass,
                    Layouts.PROC.getType(block),
                    Layouts.PROC.getSharedMethodInfo(block),
                    Layouts.PROC.getCallTargetForType(block),
                    Layouts.PROC.getCallTargetForLambdas(block),
                    Layouts.PROC.getDeclarationFrame(block),
                    Layouts.PROC.getMethod(block),
                    Layouts.PROC.getSelf(block),
                    Layouts.PROC.getBlock(block),
                    Layouts.PROC.getFrameOnStackMarker(block));

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
            final DynamicObject copy = allocateObjectNode.allocate(
                    Layouts.BASIC_OBJECT.getLogicalClass(proc),
                    Layouts.PROC.getType(proc),
                    Layouts.PROC.getSharedMethodInfo(proc),
                    Layouts.PROC.getCallTargetForType(proc),
                    Layouts.PROC.getCallTargetForLambdas(proc),
                    Layouts.PROC.getDeclarationFrame(proc),
                    Layouts.PROC.getMethod(proc),
                    Layouts.PROC.getSelf(proc),
                    Layouts.PROC.getBlock(proc),
                    Layouts.PROC.getFrameOnStackMarker(proc));

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

        @Child private YieldNode yieldNode;

        public CallNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            yieldNode = new YieldNode(context);
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
            return Layouts.PROC.getType(proc) == ProcType.LAMBDA;
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
            final boolean isLambda = Layouts.PROC.getType(proc) == ProcType.LAMBDA;
            return ArgumentDescriptorUtils.argumentDescriptorsToParameters(getContext(), argsDesc, isLambda);
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
                final DynamicObject file = createString(StringOperations.encodeRope(
                        sourceSection.getSource().getName(), UTF8Encoding.INSTANCE));

                final Object[] objects = new Object[]{file, sourceSection.getStartLine()};
                return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), objects, objects.length);
            }
        }

    }

}
