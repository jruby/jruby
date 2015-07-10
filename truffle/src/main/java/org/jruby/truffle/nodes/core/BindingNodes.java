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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.locals.ReadFrameSlotNode;
import org.jruby.truffle.nodes.locals.ReadFrameSlotNodeGen;
import org.jruby.truffle.nodes.locals.WriteFrameSlotNode;
import org.jruby.truffle.nodes.locals.WriteFrameSlotNodeGen;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.ThreadLocalObject;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyBinding;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.methods.InternalMethod;

@CoreClass(name = "Binding")
public abstract class BindingNodes {

    public static RubyBinding createRubyBinding(RubyClass bindingClass) {
        return createRubyBinding(bindingClass, null, null);
    }

    public static RubyBinding createRubyBinding(RubyClass bindingClass, Object self, MaterializedFrame frame) {
        return new RubyBinding(bindingClass, self, frame);
    }

    public static void setSelfAndFrame(RubyBasicObject binding, Object self, MaterializedFrame frame) {
        ((RubyBinding) binding).self = self;
        ((RubyBinding) binding).frame = frame;
    }

    public static Object getSelf(RubyBasicObject binding) {
        return ((RubyBinding) binding).self;
    }

    public static MaterializedFrame getFrame(RubyBasicObject binding) {
        return ((RubyBinding) binding).frame;
    }

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

            final Object[] arguments = getFrame(from).getArguments();
            final InternalMethod method = RubyArguments.getMethod(arguments);
            final Object boundSelf = RubyArguments.getSelf(arguments);
            final RubyProc boundBlock = RubyArguments.getBlock(arguments);
            final Object[] userArguments = RubyArguments.extractUserArguments(arguments);

            final Object[] copiedArguments = RubyArguments.pack(method, getFrame(from), boundSelf, boundBlock, userArguments);
            final MaterializedFrame copiedFrame = Truffle.getRuntime().createMaterializedFrame(copiedArguments);

            setSelfAndFrame(self, getSelf(from), copiedFrame);

            return self;
        }

    }

    @CoreMethod(names = "local_variable_get", required = 1)
    public abstract static class LocalVariableGetNode extends CoreMethodArrayArgumentsNode {

        private final RubyBasicObject dollarUnderscore;

        public LocalVariableGetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            dollarUnderscore = getSymbol("$_");
        }

        @Specialization(guards = {
                "isRubySymbol(symbol)",
                "symbol == cachedSymbol",
                "!isLastLine(cachedSymbol)",
                "getFrameDescriptor(binding) == cachedFrameDescriptor"

        })
        public Object localVariableGetCached(RubyBinding binding, RubyBasicObject symbol,
                                             @Cached("symbol") RubyBasicObject cachedSymbol,
                                             @Cached("getFrameDescriptor(binding)") FrameDescriptor cachedFrameDescriptor,
                                             @Cached("findFrameSlot(binding, symbol)") FrameSlot cachedFrameSlot,
                                             @Cached("createReadNode(cachedFrameSlot)") ReadFrameSlotNode readLocalVariableNode) {
            if (cachedFrameSlot == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().nameErrorLocalVariableNotDefined(SymbolNodes.getString(symbol), binding, this));
            } else {
                return readLocalVariableNode.executeRead(getFrame(binding));
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubySymbol(symbol)", "!isLastLine(symbol)"})
        public Object localVariableGetUncached(RubyBinding binding, RubyBasicObject symbol) {
            final MaterializedFrame frame = getFrame(binding);
            final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(SymbolNodes.getString(symbol));

            if (frameSlot == null) {
                throw new RaiseException(getContext().getCoreLibrary().nameErrorLocalVariableNotDefined(SymbolNodes.getString(symbol), binding, this));
            }

            return frame.getValue(frameSlot);
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubySymbol(symbol)", "isLastLine(symbol)"})
        public Object localVariableGetLastLine(RubyBinding binding, RubyBasicObject symbol) {
            final MaterializedFrame frame = getFrame(binding);
            final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(SymbolNodes.getString(symbol));

            if (frameSlot == null) {
                throw new RaiseException(getContext().getCoreLibrary().nameErrorLocalVariableNotDefined(SymbolNodes.getString(symbol), binding, this));
            }

            final Object value = frame.getValue(frameSlot);
            if (value instanceof ThreadLocalObject) {
                return ((ThreadLocalObject) value).get();
            } else {
                return value;
            }
        }

        protected FrameDescriptor getFrameDescriptor(RubyBinding binding) {
            return getFrame(binding).getFrameDescriptor();
        }

        protected FrameSlot findFrameSlot(RubyBinding binding, RubyBasicObject symbol) {
            assert RubyGuards.isRubySymbol(symbol);

            final String symbolString = SymbolNodes.getString(symbol);

            MaterializedFrame frame = getFrame(binding);

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

        protected boolean isLastLine(RubyBasicObject symbol) {
            return symbol == dollarUnderscore;
        }

    }

    @CoreMethod(names = "local_variable_set", required = 2)
    public abstract static class LocalVariableSetNode extends CoreMethodArrayArgumentsNode {

        private final RubyBasicObject dollarUnderscore;

        public LocalVariableSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            dollarUnderscore = getSymbol("$_");
        }

        @Specialization(guards = {
                "isRubySymbol(symbol)",
                "!isLastLine(symbol)",
                "getFrameDescriptor(binding) == cachedFrameDescriptor",
                "symbol == cachedSymbol"
        })
        public Object localVariableSetCached(RubyBinding binding, RubyBasicObject symbol, Object value,
                                             @Cached("symbol") RubyBasicObject cachedSymbol,
                                             @Cached("getFrameDescriptor(binding)") FrameDescriptor cachedFrameDescriptor,
                                             @Cached("createWriteNode(findFrameSlot(binding, symbol))") WriteFrameSlotNode writeLocalVariableNode) {
            return writeLocalVariableNode.executeWrite(getFrame(binding), value);
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubySymbol(symbol)", "!isLastLine(symbol)"})
        public Object localVariableSetUncached(RubyBinding binding, RubyBasicObject symbol, Object value) {
            final MaterializedFrame frame = getFrame(binding);
            final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(SymbolNodes.getString(symbol));
            frame.setObject(frameSlot, value);
            return value;
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubySymbol(symbol)", "isLastLine(symbol)"})
        public Object localVariableSetLastLine(RubyBinding binding, RubyBasicObject symbol, Object value) {
            final MaterializedFrame frame = getFrame(binding);
            final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(SymbolNodes.getString(symbol));
            frame.setObject(frameSlot, ThreadLocalObject.wrap(getContext(), value));
            return value;
        }

        protected FrameDescriptor getFrameDescriptor(RubyBinding binding) {
            return getFrame(binding).getFrameDescriptor();
        }

        protected FrameSlot findFrameSlot(RubyBinding binding, RubyBasicObject symbol) {
            assert RubyGuards.isRubySymbol(symbol);

            final String symbolString = SymbolNodes.getString(symbol);

            MaterializedFrame frame = getFrame(binding);

            while (frame != null) {
                final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(symbolString);

                if (frameSlot != null) {
                    return frameSlot;
                }

                frame = RubyArguments.getDeclarationFrame(frame.getArguments());
            }

            return getFrame(binding).getFrameDescriptor().addFrameSlot(symbolString);
        }

        protected WriteFrameSlotNode createWriteNode(FrameSlot frameSlot) {
            return WriteFrameSlotNodeGen.create(frameSlot);
        }

        protected boolean isLastLine(RubyBasicObject symbol) {
            return symbol == dollarUnderscore;
        }
    }

    @CoreMethod(names = "local_variables")
    public abstract static class LocalVariablesNode extends CoreMethodArrayArgumentsNode {

        public LocalVariablesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject localVariables(RubyBinding binding) {
            final RubyBasicObject array = createEmptyArray();

            MaterializedFrame frame = getFrame(binding);

            while (frame != null) {
                for (Object name : frame.getFrameDescriptor().getIdentifiers()) {
                    if (name instanceof String) {
                        ArrayNodes.slowPush(array, getSymbol((String) name));
                    }
                }

                frame = RubyArguments.getDeclarationFrame(frame.getArguments());
            }

            return array;
        }
    }

}
