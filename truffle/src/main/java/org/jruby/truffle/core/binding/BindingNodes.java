/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.binding;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.UnaryCoreMethodNode;
import org.jruby.truffle.core.array.ArrayHelpers;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.locals.ReadFrameSlotNode;
import org.jruby.truffle.language.locals.ReadFrameSlotNodeGen;
import org.jruby.truffle.language.locals.WriteFrameSlotNode;
import org.jruby.truffle.language.locals.WriteFrameSlotNodeGen;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.AllocateObjectNodeGen;
import org.jruby.truffle.language.threadlocal.ThreadLocalObject;

import java.util.ArrayList;
import java.util.List;

@CoreClass(name = "Binding")
public abstract class BindingNodes {

    public static DynamicObject createBinding(RubyContext context, MaterializedFrame frame) {
        final Object[] arguments = frame.getArguments();

        final MaterializedFrame bindingFrame = Truffle.getRuntime().createMaterializedFrame(
                RubyArguments.pack(frame, null, RubyArguments.getMethod(arguments), RubyArguments.getDeclarationContext(arguments), null, RubyArguments.getSelf(arguments), RubyArguments.getBlock(arguments), RubyArguments.getArguments(arguments)),
                newFrameDescriptor(context));

        return Layouts.BINDING.createBinding(context.getCoreLibrary().getBindingFactory(), bindingFrame);
    }

    @TruffleBoundary
    private static FrameDescriptor newFrameDescriptor(RubyContext context) {
        return new FrameDescriptor(context.getCoreLibrary().getNilObject());
    }

    public static FrameDescriptor getFrameDescriptor(DynamicObject binding) {
        assert RubyGuards.isRubyBinding(binding);
        return Layouts.BINDING.getFrame(binding).getFrameDescriptor();
    }

    public static MaterializedFrame getDeclarationFrame(DynamicObject binding) {
        assert RubyGuards.isRubyBinding(binding);
        return RubyArguments.getDeclarationFrame(Layouts.BINDING.getFrame(binding).getArguments());
    }

    protected static class FrameSlotAndDepth {
        private final FrameSlot slot;
        private final int depth;

        public FrameSlotAndDepth(FrameSlot slot, int depth) {
            this.slot = slot;
            this.depth = depth;
        }

        public FrameSlot getSlot() {
            return slot;
        }
    }

    public static FrameSlotAndDepth findFrameSlotOrNull(DynamicObject binding, DynamicObject symbol) {
        assert RubyGuards.isRubyBinding(binding);
        assert RubyGuards.isRubySymbol(symbol);

        final String identifier = Layouts.SYMBOL.getString(symbol);

        int depth = 0;
        MaterializedFrame frame = Layouts.BINDING.getFrame(binding);

        while (frame != null) {
            final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(identifier);
            if (frameSlot != null) {
                return new FrameSlotAndDepth(frameSlot, depth);
            }

            frame = RubyArguments.getDeclarationFrame(frame);
            depth++;
        }
        return null;
    }

    @CoreMethod(names = { "dup", "clone" })
    public abstract static class DupNode extends UnaryCoreMethodNode {

        @Child private AllocateObjectNode allocateObjectNode;

        public DupNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public DynamicObject dup(DynamicObject binding) {
            DynamicObject copy = allocateObjectNode.allocate(
                    Layouts.BASIC_OBJECT.getLogicalClass(binding),
                    copyFrame(Layouts.BINDING.getFrame(binding)));
            return copy;
        }

        private MaterializedFrame copyFrame(MaterializedFrame frame) {
            final MaterializedFrame copy = Truffle.getRuntime().createMaterializedFrame(frame.getArguments(), frame.getFrameDescriptor().copy());
            for (FrameSlot slot : frame.getFrameDescriptor().getSlots()) {
                copy.setObject(copy.getFrameDescriptor().findFrameSlot(slot.getIdentifier()), frame.getValue(slot));
            }
            return copy;
        }

    }

    @ImportStatic(BindingNodes.class)
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
                "compatibleFrames(binding, cachedBinding)",
                "cachedFrameSlot != null"
        },
                limit = "getCacheLimit()")
        public Object localVariableGetCached(DynamicObject binding, DynamicObject symbol,
                                             @Cached("symbol") DynamicObject cachedSymbol,
                                             @Cached("binding") DynamicObject cachedBinding,
                                             @Cached("findFrameSlotOrNull(binding, symbol)") FrameSlotAndDepth cachedFrameSlot,
                                             @Cached("createReadNode(cachedFrameSlot)") ReadFrameSlotNode readLocalVariableNode) {
            final MaterializedFrame frame = RubyArguments.getDeclarationFrame(Layouts.BINDING.getFrame(binding), cachedFrameSlot.depth);
            return readLocalVariableNode.executeRead(frame);
        }

        @TruffleBoundary
        @Specialization(guards = { "isRubySymbol(symbol)", "!isLastLine(symbol)" })
        public Object localVariableGetUncached(DynamicObject binding, DynamicObject symbol) {
            final FrameSlotAndDepth frameSlot = findFrameSlotOrNull(binding, symbol);
            if (frameSlot == null) {
                throw new RaiseException(coreLibrary().nameErrorLocalVariableNotDefined(Layouts.SYMBOL.getString(symbol), binding, this));
            } else {
                final MaterializedFrame frame = RubyArguments.getDeclarationFrame(Layouts.BINDING.getFrame(binding), frameSlot.depth);
                return frame.getValue(frameSlot.slot);
            }
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubySymbol(symbol)", "isLastLine(symbol)"})
        public Object localVariableGetLastLine(DynamicObject binding, DynamicObject symbol) {
            final MaterializedFrame frame = Layouts.BINDING.getFrame(binding);
            final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(Layouts.SYMBOL.getString(symbol));

            if (frameSlot == null) {
                throw new RaiseException(coreLibrary().nameErrorLocalVariableNotDefined(Layouts.SYMBOL.getString(symbol), binding, this));
            }

            final Object value = frame.getValue(frameSlot);
            if (value instanceof ThreadLocalObject) {
                return ((ThreadLocalObject) value).get();
            } else {
                return value;
            }
        }

        protected boolean compatibleFrames(DynamicObject binding1, DynamicObject binding2) {
            final FrameDescriptor fd1 = getFrameDescriptor(binding1);
            final FrameDescriptor fd2 = getFrameDescriptor(binding2);

            if (!((fd1 == fd2) || (fd1.getSize() == 0 && fd2.getSize() == 0))) {
                return false;
            }

            final MaterializedFrame df1 = getDeclarationFrame(binding1);
            final MaterializedFrame df2 = getDeclarationFrame(binding2);

            if ((df1 == null) != (df2 == null)) {
                return false;
            }

            if (df1 == null) {
                return true;
            }

            return df1.getFrameDescriptor() == df2.getFrameDescriptor();
        }

        protected ReadFrameSlotNode createReadNode(FrameSlotAndDepth frameSlot) {
            if (frameSlot == null) {
                return null;
            } else {
                return ReadFrameSlotNodeGen.create(frameSlot.slot);
            }
        }

        protected boolean isLastLine(DynamicObject symbol) {
            return symbol == dollarUnderscore;
        }

        protected int getCacheLimit() {
            return getContext().getOptions().BINDING_LOCAL_VARIABLE_CACHE;
        }

    }

    @ImportStatic(BindingNodes.class)
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
        }, limit = "getCacheLimit()")
        public Object localVariableSetCached(DynamicObject binding, DynamicObject symbol, Object value,
                                             @Cached("symbol") DynamicObject cachedSymbol,
                                             @Cached("getFrameDescriptor(binding)") FrameDescriptor cachedFrameDescriptor,
                                             @Cached("findFrameSlot(binding, symbol)") FrameSlotAndDepth cachedFrameSlot,
                                             @Cached("createWriteNode(cachedFrameSlot)") WriteFrameSlotNode writeLocalVariableNode) {
            final MaterializedFrame frame = RubyArguments.getDeclarationFrame(Layouts.BINDING.getFrame(binding), cachedFrameSlot.depth);
            return writeLocalVariableNode.executeWrite(frame, value);
        }

        @TruffleBoundary
        @Specialization(guards = { "isRubySymbol(symbol)", "!isLastLine(symbol)" })
        public Object localVariableSetUncached(DynamicObject binding, DynamicObject symbol, Object value) {
            final FrameSlotAndDepth frameSlot = findFrameSlot(binding, symbol);
            final MaterializedFrame frame = RubyArguments.getDeclarationFrame(Layouts.BINDING.getFrame(binding), frameSlot.depth);
            frame.setObject(frameSlot.slot, value);
            return value;
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubySymbol(symbol)", "isLastLine(symbol)"})
        public Object localVariableSetLastLine(DynamicObject binding, DynamicObject symbol, Object value) {
            final MaterializedFrame frame = Layouts.BINDING.getFrame(binding);
            final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(Layouts.SYMBOL.getString(symbol));
            frame.setObject(frameSlot, ThreadLocalObject.wrap(getContext(), value));
            return value;
        }

        protected FrameSlotAndDepth findFrameSlot(DynamicObject binding, DynamicObject symbol) {
            final FrameSlotAndDepth frameSlot = BindingNodes.findFrameSlotOrNull(binding, symbol);
            if (frameSlot == null) {
                final FrameSlot newSlot = Layouts.BINDING.getFrame(binding).getFrameDescriptor().addFrameSlot(Layouts.SYMBOL.getString(symbol));
                return new FrameSlotAndDepth(newSlot, 0);
            } else {
                return frameSlot;
            }
        }

        protected WriteFrameSlotNode createWriteNode(FrameSlotAndDepth frameSlot) {
            return WriteFrameSlotNodeGen.create(frameSlot.slot);
        }

        protected boolean isLastLine(DynamicObject symbol) {
            return symbol == dollarUnderscore;
        }

        protected int getCacheLimit() {
            return getContext().getOptions().BINDING_LOCAL_VARIABLE_CACHE;
        }
    }

    @CoreMethod(names = "local_variables")
    public abstract static class LocalVariablesNode extends CoreMethodArrayArgumentsNode {

        public LocalVariablesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject localVariables(DynamicObject binding) {
            MaterializedFrame frame = Layouts.BINDING.getFrame(binding);

            return listLocalVariables(getContext(), frame);
        }

        @TruffleBoundary
        public static DynamicObject listLocalVariables(RubyContext context, Frame frame) {
            final List<Object> names = new ArrayList<>();
            while (frame != null) {
                for (FrameSlot slot : frame.getFrameDescriptor().getSlots()) {
                    if (slot.getIdentifier() instanceof String && !((String) slot.getIdentifier()).startsWith("rubytruffle_temp_frame_on_stack_marker")) {
                        names.add(context.getSymbolTable().getSymbol((String) slot.getIdentifier()));
                    }
                }

                frame = RubyArguments.getDeclarationFrame(frame);
            }
            final int size = names.size();
            return ArrayHelpers.createArray(context, names.toArray(new Object[size]), size);
        }
    }

    @CoreMethod(names = "receiver")
    public abstract static class ReceiverNode extends UnaryCoreMethodNode {

        public ReceiverNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object receiver(DynamicObject binding) {
            return RubyArguments.getSelf(Layouts.BINDING.getFrame(binding).getArguments());
        }
    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            throw new RaiseException(coreLibrary().typeErrorAllocatorUndefinedFor(rubyClass, this));
        }

    }

}
