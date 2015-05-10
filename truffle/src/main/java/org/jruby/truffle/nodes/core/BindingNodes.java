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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.globals.GetFromThreadLocalNode;
import org.jruby.truffle.nodes.globals.WrapInThreadLocalNode;
import org.jruby.truffle.nodes.locals.ReadFrameSlotNode;
import org.jruby.truffle.nodes.locals.ReadFrameSlotNodeGen;
import org.jruby.truffle.nodes.locals.WriteFrameSlotNode;
import org.jruby.truffle.nodes.locals.WriteFrameSlotNodeGen;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBinding;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.truffle.runtime.methods.InternalMethod;

@CoreClass(name = "Binding")
public abstract class BindingNodes {

    @CoreMethod(names = "initialize_copy", required = 1)
    public abstract static class InitializeCopyNode extends CoreMethodArrayArgumentsNode {

        public InitializeCopyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object initializeCopy(RubyBinding self, RubyBinding from) {
            if (self == from) {
                return self;
            }

            final Object[] arguments = from.getFrame().getArguments();
            final InternalMethod method = RubyArguments.getMethod(arguments);
            final Object boundSelf = RubyArguments.getSelf(arguments);
            final RubyProc boundBlock = RubyArguments.getBlock(arguments);
            final Object[] userArguments = RubyArguments.extractUserArguments(arguments);

            final Object[] copiedArguments = RubyArguments.pack(method, from.getFrame(), boundSelf, boundBlock, userArguments);
            final MaterializedFrame copiedFrame = Truffle.getRuntime().createMaterializedFrame(copiedArguments);

            self.initialize(from.getSelf(), copiedFrame);

            return self;
        }

    }

    @CoreMethod(names = "local_variable_get", required = 1)
    public abstract static class LocalVariableGetNode extends CoreMethodArrayArgumentsNode {

        private final RubySymbol dollarUnderscore;

        public LocalVariableGetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            dollarUnderscore = getContext().getSymbol("$_");
        }

        @Specialization(guards = {
                "symbol == cachedSymbol",
                "!isLastLine(cachedSymbol)",
                "getFrameDescriptor(binding) == cachedFrameDescriptor"

        })
        public Object localVariableGetCached(RubyBinding binding, RubySymbol symbol,
                                             @Cached("symbol") RubySymbol cachedSymbol,
                                             @Cached("getFrameDescriptor(binding)") FrameDescriptor cachedFrameDescriptor,
                                             @Cached("findFrameSlot(binding, symbol)") FrameSlot cachedFrameSlot,
                                             @Cached("createReadNode(cachedFrameSlot)") ReadFrameSlotNode readLocalVariableNode) {
            if (cachedFrameSlot == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().nameErrorLocalVariableNotDefined(symbol.toString(), binding, this));
            } else {
                return readLocalVariableNode.executeRead(binding.getFrame());
            }
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "!isLastLine(symbol)")
        public Object localVariableGetUncached(RubyBinding binding, RubySymbol symbol) {
            final MaterializedFrame frame = binding.getFrame();
            final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(symbol.toString());

            if (frameSlot == null) {
                throw new RaiseException(getContext().getCoreLibrary().nameErrorLocalVariableNotDefined(symbol.toString(), binding, this));
            }

            return frame.getValue(frameSlot);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isLastLine(symbol)")
        public Object localVariableGetLastLine(RubyBinding binding, RubySymbol symbol) {
            final MaterializedFrame frame = binding.getFrame();
            final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(symbol.toString());

            if (frameSlot == null) {
                throw new RaiseException(getContext().getCoreLibrary().nameErrorLocalVariableNotDefined(symbol.toString(), binding, this));
            }

            final Object value = frame.getValue(frameSlot);
            return GetFromThreadLocalNode.get(getContext(), value);
        }

        protected FrameDescriptor getFrameDescriptor(RubyBinding binding) {
            return binding.getFrame().getFrameDescriptor();
        }

        protected FrameSlot findFrameSlot(RubyBinding binding, RubySymbol symbol) {
            final String symbolString = symbol.toString();

            MaterializedFrame frame = binding.getFrame();

            while (frame != null) {
                final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(symbolString);

                if (frameSlot != null) {
                    return frameSlot;
                }

                frame = RubyArguments.getDeclarationFrame(frame.getArguments());
            }

            return null;
        }

        protected ReadFrameSlotNode createReadNode(FrameSlot frameSlot) {
            if (frameSlot == null) {
                return null;
            } else {
                return ReadFrameSlotNodeGen.create(frameSlot);
            }
        }

        protected boolean isLastLine(RubySymbol symbol) {
            return symbol == dollarUnderscore;
        }

    }

    @CoreMethod(names = "local_variable_set", required = 2)
    public abstract static class LocalVariableSetNode extends CoreMethodArrayArgumentsNode {

        private final RubySymbol dollarUnderscore;

        public LocalVariableSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            dollarUnderscore = getContext().getSymbol("$_");
        }

        @Specialization(guards = {
                "!isLastLine(symbol)",
                "getFrameDescriptor(binding) == cachedFrameDescriptor",
                "symbol == cachedSymbol"
        })
        public Object localVariableSetCached(RubyBinding binding, RubySymbol symbol, Object value,
                                             @Cached("symbol") RubySymbol cachedSymbol,
                                             @Cached("getFrameDescriptor(binding)") FrameDescriptor cachedFrameDescriptor,
                                             @Cached("createWriteNode(findFrameSlot(binding, symbol))") WriteFrameSlotNode writeLocalVariableNode) {
            return writeLocalVariableNode.executeWrite(binding.getFrame(), value);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "!isLastLine(symbol)")
        public Object localVariableSetUncached(RubyBinding binding, RubySymbol symbol, Object value) {
            final MaterializedFrame frame = binding.getFrame();
            final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(symbol.toString());
            frame.setObject(frameSlot, value);
            return value;
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isLastLine(symbol)")
        public Object localVariableSetLastLine(RubyBinding binding, RubySymbol symbol, Object value) {
            final MaterializedFrame frame = binding.getFrame();
            final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(symbol.toString());
            frame.setObject(frameSlot, WrapInThreadLocalNode.wrap(getContext(), value));
            return value;
        }

        protected FrameDescriptor getFrameDescriptor(RubyBinding binding) {
            return binding.getFrame().getFrameDescriptor();
        }

        protected FrameSlot findFrameSlot(RubyBinding binding, RubySymbol symbol) {
            final String symbolString = symbol.toString();

            MaterializedFrame frame = binding.getFrame();

            while (frame != null) {
                final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(symbolString);

                if (frameSlot != null) {
                    return frameSlot;
                }

                frame = RubyArguments.getDeclarationFrame(frame.getArguments());
            }

            return binding.getFrame().getFrameDescriptor().addFrameSlot(symbolString);
        }

        protected WriteFrameSlotNode createWriteNode(FrameSlot frameSlot) {
            return WriteFrameSlotNodeGen.create(frameSlot);
        }

        protected boolean isLastLine(RubySymbol symbol) {
            return symbol == dollarUnderscore;
        }
    }

    @CoreMethod(names = "local_variables")
    public abstract static class LocalVariablesNode extends CoreMethodArrayArgumentsNode {

        public LocalVariablesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyArray localVariables(RubyBinding binding) {
            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass());

            MaterializedFrame frame = binding.getFrame();

            while (frame != null) {
                for (Object name : frame.getFrameDescriptor().getIdentifiers()) {
                    if (name instanceof String) {
                        array.slowPush(getContext().getSymbol((String) name));
                    }
                }

                frame = RubyArguments.getDeclarationFrame(frame.getArguments());
            }

            return array;
        }
    }

}
