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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.locals.ReadFrameSlotNode;
import org.jruby.truffle.nodes.locals.ReadFrameSlotNodeGen;
import org.jruby.truffle.nodes.locals.WriteFrameSlotNode;
import org.jruby.truffle.nodes.locals.WriteFrameSlotNodeGen;
import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.truffle.om.dsl.api.Nullable;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.ThreadLocalObject;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.methods.InternalMethod;

@CoreClass(name = "Binding")
public abstract class BindingNodes {

    @Layout
    public interface BindingLayout extends BasicObjectNodes.BasicObjectLayout {

        DynamicObjectFactory createBindingShape(DynamicObject logicalClass, DynamicObject metaClass);

        DynamicObject createBinding(DynamicObjectFactory factory, @Nullable Object self, @Nullable MaterializedFrame frame);

        boolean isBinding(DynamicObject object);

        @Nullable
        Object getSelf(DynamicObject object);

        @Nullable
        void setSelf(DynamicObject object, Object self);

        @Nullable
        MaterializedFrame getFrame(DynamicObject object);

        @Nullable
        void setFrame(DynamicObject object, MaterializedFrame frame);

    }

    public static final BindingLayout BINDING_LAYOUT = BindingLayoutImpl.INSTANCE;

    public static DynamicObject createRubyBinding(DynamicObject bindingClass) {
        return createRubyBinding(bindingClass, null, null);
    }

    public static DynamicObject createRubyBinding(DynamicObject bindingClass, Object self, MaterializedFrame frame) {
        return BINDING_LAYOUT.createBinding(ModuleNodes.getModel(bindingClass).factory, self, frame);
    }

    public static void setSelfAndFrame(DynamicObject binding, Object self, MaterializedFrame frame) {
        BINDING_LAYOUT.setSelf(binding, self);
        BINDING_LAYOUT.setFrame(binding, frame);
    }

    public static Object getSelf(DynamicObject binding) {
        return BINDING_LAYOUT.getSelf(binding);
    }

    public static MaterializedFrame getFrame(DynamicObject binding) {
        return BINDING_LAYOUT.getFrame(binding);
    }

    @CoreMethod(names = "initialize_copy", required = 1)
    public abstract static class InitializeCopyNode extends CoreMethodArrayArgumentsNode {

        public InitializeCopyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyBinding(from)")
        public Object initializeCopy(DynamicObject self, DynamicObject from) {
            if (self == from) {
                return self;
            }

            final Object[] arguments = getFrame(from).getArguments();
            final InternalMethod method = RubyArguments.getMethod(arguments);
            final Object boundSelf = RubyArguments.getSelf(arguments);
            final DynamicObject boundBlock = RubyArguments.getBlock(arguments);
            final Object[] userArguments = RubyArguments.extractUserArguments(arguments);

            final Object[] copiedArguments = RubyArguments.pack(method, getFrame(from), boundSelf, boundBlock, userArguments);
            final MaterializedFrame copiedFrame = Truffle.getRuntime().createMaterializedFrame(copiedArguments);

            setSelfAndFrame(self, getSelf(from), copiedFrame);

            return self;
        }

    }

    @CoreMethod(names = "local_variable_get", required = 1)
    public abstract static class LocalVariableGetNode extends CoreMethodArrayArgumentsNode {

        private final DynamicObject dollarUnderscore;

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
        public Object localVariableGetCached(DynamicObject binding, DynamicObject symbol,
                                             @Cached("symbol") DynamicObject cachedSymbol,
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
        public Object localVariableGetUncached(DynamicObject binding, DynamicObject symbol) {
            final MaterializedFrame frame = getFrame(binding);
            final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(SymbolNodes.getString(symbol));

            if (frameSlot == null) {
                throw new RaiseException(getContext().getCoreLibrary().nameErrorLocalVariableNotDefined(SymbolNodes.getString(symbol), binding, this));
            }

            return frame.getValue(frameSlot);
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubySymbol(symbol)", "isLastLine(symbol)"})
        public Object localVariableGetLastLine(DynamicObject binding, DynamicObject symbol) {
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

        protected FrameDescriptor getFrameDescriptor(DynamicObject binding) {
            assert RubyGuards.isRubyBinding(binding);
            return getFrame(binding).getFrameDescriptor();
        }

        protected FrameSlot findFrameSlot(DynamicObject binding, DynamicObject symbol) {
            assert RubyGuards.isRubyBinding(binding);
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

        protected boolean isLastLine(DynamicObject symbol) {
            return symbol == dollarUnderscore;
        }

    }

    @CoreMethod(names = "local_variable_set", required = 2)
    public abstract static class LocalVariableSetNode extends CoreMethodArrayArgumentsNode {

        private final DynamicObject dollarUnderscore;

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
        public Object localVariableSetCached(DynamicObject binding, DynamicObject symbol, Object value,
                                             @Cached("symbol") DynamicObject cachedSymbol,
                                             @Cached("getFrameDescriptor(binding)") FrameDescriptor cachedFrameDescriptor,
                                             @Cached("createWriteNode(findFrameSlot(binding, symbol))") WriteFrameSlotNode writeLocalVariableNode) {
            return writeLocalVariableNode.executeWrite(getFrame(binding), value);
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubySymbol(symbol)", "!isLastLine(symbol)"})
        public Object localVariableSetUncached(DynamicObject binding, DynamicObject symbol, Object value) {
            final MaterializedFrame frame = getFrame(binding);
            final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(SymbolNodes.getString(symbol));
            frame.setObject(frameSlot, value);
            return value;
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubySymbol(symbol)", "isLastLine(symbol)"})
        public Object localVariableSetLastLine(DynamicObject binding, DynamicObject symbol, Object value) {
            final MaterializedFrame frame = getFrame(binding);
            final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(SymbolNodes.getString(symbol));
            frame.setObject(frameSlot, ThreadLocalObject.wrap(getContext(), value));
            return value;
        }

        protected FrameDescriptor getFrameDescriptor(DynamicObject binding) {
            assert RubyGuards.isRubyBinding(binding);
            return getFrame(binding).getFrameDescriptor();
        }

        protected FrameSlot findFrameSlot(DynamicObject binding, DynamicObject symbol) {
            assert RubyGuards.isRubyBinding(binding);
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

        protected boolean isLastLine(DynamicObject symbol) {
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
        public DynamicObject localVariables(DynamicObject binding) {
            final DynamicObject array = createEmptyArray();

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

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return BindingNodes.createRubyBinding(rubyClass);
        }

    }

}
