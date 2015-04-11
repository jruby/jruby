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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.NullSourceSection;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.ast.ArgsNode;
import org.jruby.runtime.Helpers;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBinding;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.Memo;

@CoreClass(name = "Proc")
public abstract class ProcNodes {

    @CoreMethod(names = "arity")
    public abstract static class ArityNode extends CoreMethodNode {

        public ArityNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int arity(RubyProc proc) {
            return proc.getSharedMethodInfo().getArity().getArityNumber();
        }

    }

    @CoreMethod(names = "binding")
    public abstract static class BindingNode extends CoreMethodNode {

        public BindingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object binding(RubyProc proc) {
            final MaterializedFrame frame = proc.getDeclarationFrame();

            return new RubyBinding(getContext().getCoreLibrary().getBindingClass(),
                    RubyArguments.getSelf(frame.getArguments()),
                    frame);
        }

    }

    @CoreMethod(names = {"call", "[]"}, argumentsAsArray = true, needsBlock = true)
    public abstract static class CallNode extends CoreMethodNode {

        @Child private YieldDispatchHeadNode yieldNode;

        public CallNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            yieldNode = new YieldDispatchHeadNode(context);
        }

        @Specialization
        public Object call(VirtualFrame frame, RubyProc proc, Object[] args, UndefinedPlaceholder block) {
            return yieldNode.dispatch(frame, proc, args);
        }

        @Specialization
        public Object call(VirtualFrame frame, RubyProc proc, Object[] args, RubyProc block) {
            return yieldNode.dispatchWithModifiedBlock(frame, proc, block, args);
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true)
    public abstract static class InitializeNode extends CoreMethodNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyNilClass initialize(RubyProc proc, RubyProc block) {
            proc.initialize(block.getSharedMethodInfo(), block.getCallTargetForProcs(),
                    block.getCallTargetForProcs(), block.getCallTargetForMethods(), block.getDeclarationFrame(),
                    block.getMethod(), block.getSelfCapturedInScope(), block.getBlockCapturedInScope());

            return nil();
        }

        @Specialization
        public RubyNilClass initialize(VirtualFrame frame, RubyProc proc, @SuppressWarnings("unused") UndefinedPlaceholder block) {
            notDesignedForCompilation();

            final Memo<Integer> frameCount = new Memo<>(0);

            // The parent will be the Proc.new call.  We need to go an extra level up in order to get the parent
            // of the Proc.new call, since that is where the block should be inherited from.
            final MaterializedFrame grandparentFrame = Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<MaterializedFrame>() {

                @Override
                public MaterializedFrame visitFrame(FrameInstance frameInstance) {
                    if (frameCount.get() == 1) {
                        return frameInstance.getFrame(FrameInstance.FrameAccess.READ_WRITE, false).materialize();
                    } else {
                        frameCount.set(frameCount.get() + 1);
                        return null;
                    }
                }

            });

            final RubyProc grandparentBlock = RubyArguments.getBlock(grandparentFrame.getArguments());

            if (grandparentBlock == null) {
                CompilerDirectives.transferToInterpreter();

                // TODO (nirvdrum 19-Feb-15) MRI reports this error on the #new method, not #initialize.
                throw new RaiseException(getContext().getCoreLibrary().argumentError("tried to create Proc object without a block", this));
            }

            return initialize(proc, grandparentBlock);
        }

    }

    @CoreMethod(names = "lambda?")
    public abstract static class LambdaNode extends CoreMethodNode {

        public LambdaNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean lambda(RubyProc proc) {
            return proc.getType() == RubyProc.Type.LAMBDA;
        }

    }

    @CoreMethod(names = "parameters")
    public abstract static class ParametersNode extends CoreMethodNode {

        public ParametersNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyArray parameters(RubyProc proc) {
            final ArgsNode argsNode = proc.getSharedMethodInfo().getParseTree().findFirstChild(ArgsNode.class);

            final String[] parameters = Helpers.encodeParameterList((ArgsNode) argsNode).split(";");

            return (RubyArray) getContext().toTruffle(Helpers.parameterListToParameters(getContext().getRuntime(),
                    parameters, proc.getType() == RubyProc.Type.LAMBDA));
        }

    }

    @CoreMethod(names = "source_location")
    public abstract static class SourceLocationNode extends CoreMethodNode {

        public SourceLocationNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object sourceLocation(RubyProc proc) {
            notDesignedForCompilation();

            SourceSection sourceSection = proc.getSharedMethodInfo().getSourceSection();

            if (sourceSection instanceof NullSourceSection) {
                return nil();
            } else {
                RubyString file = getContext().makeString(sourceSection.getSource().getName());
                return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                        file, sourceSection.getStartLine());
            }
        }

    }

}
