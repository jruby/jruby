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
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.NullSourceSection;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.ast.ArgsNode;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Helpers;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;
import org.jruby.util.Memo;

@CoreClass(name = "Proc")
public abstract class ProcNodes {

    public static Type getType(RubyBasicObject proc) {
        assert RubyGuards.isRubyProc(proc);
        return ((RubyProc) proc).type;
    }

    public static SharedMethodInfo getSharedMethodInfo(RubyBasicObject proc) {
        assert RubyGuards.isRubyProc(proc);
        return ((RubyProc) proc).sharedMethodInfo;
    }

    public static CallTarget getCallTargetForBlocks(RubyBasicObject proc) {
        assert RubyGuards.isRubyProc(proc);
        return ((RubyProc) proc).callTargetForBlocks;
    }

    public static CallTarget getCallTargetForProcs(RubyBasicObject proc) {
        assert RubyGuards.isRubyProc(proc);
        return ((RubyProc) proc).callTargetForProcs;
    }

    public static CallTarget getCallTargetForLambdas(RubyBasicObject proc) {
        assert RubyGuards.isRubyProc(proc);
        return ((RubyProc) proc).callTargetForLambdas;
    }

    public static MaterializedFrame getDeclarationFrame(RubyBasicObject proc) {
        assert RubyGuards.isRubyProc(proc);
        return ((RubyProc) proc).declarationFrame;
    }

    public static InternalMethod getMethod(RubyBasicObject proc) {
        assert RubyGuards.isRubyProc(proc);
        return ((RubyProc) proc).method;
    }

    public static Object getSelfCapturedInScope(RubyBasicObject proc) {
        assert RubyGuards.isRubyProc(proc);
        return ((RubyProc) proc).self;
    }

    public static RubyBasicObject getBlockCapturedInScope(RubyBasicObject proc) {
        assert RubyGuards.isRubyProc(proc);
        return ((RubyProc) proc).block;
    }

    public static CallTarget getCallTargetForType(RubyBasicObject proc) {
        assert RubyGuards.isRubyProc(proc);
        switch (((RubyProc) proc).type) {
            case BLOCK:
                return ((RubyProc) proc).callTargetForBlocks;
            case PROC:
                return ((RubyProc) proc).callTargetForProcs;
            case LAMBDA:
                return ((RubyProc) proc).callTargetForLambdas;
        }

        throw new UnsupportedOperationException(((RubyProc) proc).type.toString());
    }

    public static Object rootCall(RubyBasicObject proc, Object... args) {
        assert RubyGuards.isRubyProc(proc);

        return getCallTargetForType(proc).call(RubyArguments.pack(
                getMethod(proc),
                getDeclarationFrame(proc),
                getSelfCapturedInScope(proc),
                getBlockCapturedInScope(proc),
                args));
    }

    public static void initialize(RubyBasicObject proc, SharedMethodInfo sharedMethodInfo, CallTarget callTargetForBlocks, CallTarget callTargetForProcs,
                                  CallTarget callTargetForLambdas, MaterializedFrame declarationFrame, InternalMethod method,
                                  Object self, RubyBasicObject block) {
        assert RubyGuards.isRubyProc(proc);

        ((RubyProc) proc).sharedMethodInfo = sharedMethodInfo;
        ((RubyProc) proc).callTargetForBlocks = callTargetForBlocks;
        ((RubyProc) proc).callTargetForProcs = callTargetForProcs;
        ((RubyProc) proc).callTargetForLambdas = callTargetForLambdas;
        ((RubyProc) proc).declarationFrame = declarationFrame;
        ((RubyProc) proc).method = method;
        ((RubyProc) proc).self = self;
        assert block == null || RubyGuards.isRubyProc(block);
        ((RubyProc) proc).block = block;
    }

    public static RubyBasicObject createRubyProc(RubyClass procClass, Type type) {
        return createRubyProc(procClass, type, null, null, null, null, null, null, null, null);
    }

    public static RubyBasicObject createRubyProc(RubyClass procClass, Type type, SharedMethodInfo sharedMethodInfo, CallTarget callTargetForBlocks,
                                          CallTarget callTargetForProcs, CallTarget callTargetForLambdas, MaterializedFrame declarationFrame,
                                          InternalMethod method, Object self, RubyBasicObject block) {
        return new RubyProc(procClass, type, sharedMethodInfo, callTargetForBlocks, callTargetForProcs, callTargetForLambdas, declarationFrame, method, self, block);
    }

    public enum Type {
        BLOCK, PROC, LAMBDA
    }

    @CoreMethod(names = "arity")
    public abstract static class ArityNode extends CoreMethodArrayArgumentsNode {

        public ArityNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int arity(RubyBasicObject proc) {
            return getSharedMethodInfo(proc).getArity().getArityNumber();
        }

    }

    @CoreMethod(names = "binding")
    public abstract static class BindingNode extends CoreMethodArrayArgumentsNode {

        public BindingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object binding(RubyBasicObject proc) {
            final MaterializedFrame frame = getDeclarationFrame(proc);

            return BindingNodes.createRubyBinding(getContext().getCoreLibrary().getBindingClass(),
                    RubyArguments.getSelf(frame.getArguments()),
                    frame);
        }

    }

    @CoreMethod(names = {"call", "[]", "yield"}, argumentsAsArray = true, needsBlock = true)
    public abstract static class CallNode extends CoreMethodArrayArgumentsNode {

        @Child private YieldDispatchHeadNode yieldNode;

        public CallNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            yieldNode = new YieldDispatchHeadNode(context);
        }

        @Specialization
        public Object call(VirtualFrame frame, RubyBasicObject proc, Object[] args, NotProvided block) {
            return yieldNode.dispatch(frame, proc, args);
        }

        @Specialization(guards = "isRubyProc(block)")
        public Object call(VirtualFrame frame, RubyBasicObject proc, Object[] args, RubyBasicObject block) {
            return yieldNode.dispatchWithModifiedBlock(frame, proc, block, args);
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyProc(block)")
        public RubyBasicObject initialize(RubyBasicObject proc, RubyBasicObject block) {
            ProcNodes.initialize(proc, getSharedMethodInfo(block), getCallTargetForProcs(block),
                    getCallTargetForProcs(block), getCallTargetForLambdas(block), getDeclarationFrame(block),
                    getMethod(block), getSelfCapturedInScope(block), getBlockCapturedInScope(block));

            return nil();
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject initialize(RubyBasicObject proc, NotProvided block) {
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

            final RubyBasicObject grandparentBlock = RubyArguments.getBlock(grandparentFrame.getArguments());

            if (grandparentBlock == null) {
                CompilerDirectives.transferToInterpreter();

                // TODO (nirvdrum 19-Feb-15) MRI reports this error on the #new method, not #initialize.
                throw new RaiseException(getContext().getCoreLibrary().argumentError("tried to create Proc object without a block", this));
            }

            return initialize(proc, grandparentBlock);
        }

    }

    @CoreMethod(names = "lambda?")
    public abstract static class LambdaNode extends CoreMethodArrayArgumentsNode {

        public LambdaNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean lambda(RubyBasicObject proc) {
            return getType(proc) == Type.LAMBDA;
        }

    }

    @CoreMethod(names = "parameters")
    public abstract static class ParametersNode extends CoreMethodArrayArgumentsNode {

        public ParametersNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject parameters(RubyBasicObject proc) {
            final ArgsNode argsNode = getSharedMethodInfo(proc).getParseTree().findFirstChild(ArgsNode.class);

            final ArgumentDescriptor[] argsDesc = Helpers.argsNodeToArgumentDescriptors(argsNode);

            return getContext().toTruffle(Helpers.argumentDescriptorsToParameters(getContext().getRuntime(),
                    argsDesc, getType(proc) == Type.LAMBDA));
        }

    }

    @CoreMethod(names = "source_location")
    public abstract static class SourceLocationNode extends CoreMethodArrayArgumentsNode {

        public SourceLocationNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object sourceLocation(RubyBasicObject proc) {
            SourceSection sourceSection = getSharedMethodInfo(proc).getSourceSection();

            if (sourceSection instanceof NullSourceSection) {
                return nil();
            } else {
                RubyBasicObject file = createString(sourceSection.getSource().getName());
                return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                        file, sourceSection.getStartLine());
            }
        }

    }

    public static class ProcAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return createRubyProc(rubyClass, Type.PROC);
        }

    }
}
