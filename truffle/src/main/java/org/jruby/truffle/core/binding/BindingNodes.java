/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.CoreMethodNode;
import org.jruby.truffle.builtins.UnaryCoreMethodNode;
import org.jruby.truffle.core.array.ArrayHelpers;
import org.jruby.truffle.core.cast.NameToJavaStringNodeGen;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.locals.ReadFrameSlotNode;
import org.jruby.truffle.language.locals.ReadFrameSlotNodeGen;
import org.jruby.truffle.language.locals.WriteFrameSlotNode;
import org.jruby.truffle.language.locals.WriteFrameSlotNodeGen;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.threadlocal.ThreadLocalObject;
import org.jruby.truffle.parser.Translator;

import java.util.LinkedHashSet;
import java.util.Set;

@CoreClass("Binding")
public abstract class BindingNodes {

    public static DynamicObject createBinding(RubyContext context, MaterializedFrame frame) {
        final MaterializedFrame bindingFrame = Truffle.getRuntime().createMaterializedFrame(
                RubyArguments.pack(frame, null, RubyArguments.getMethod(frame), RubyArguments.getDeclarationContext(frame), null, RubyArguments.getSelf(frame), RubyArguments.getBlock(frame), RubyArguments.getArguments(frame)),
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
        return RubyArguments.getDeclarationFrame(Layouts.BINDING.getFrame(binding));
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
        return findFrameSlotOrNull(binding, identifier);
    }

    public static FrameSlotAndDepth findFrameSlotOrNull(DynamicObject binding, String identifier) {
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

        @Child private AllocateObjectNode allocateObjectNode = AllocateObjectNode.create();

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
    @CoreMethod(names = "local_variable_defined?", required = 1)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "binding"),
        @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class LocalVariableDefinedNode extends CoreMethodNode {

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }

        @TruffleBoundary
        @Specialization(guards = "!isLastLine(name)")
        public boolean localVariableDefinedUncached(DynamicObject binding, String name) {
            final FrameSlotAndDepth frameSlot = findFrameSlotOrNull(binding, name);
            return frameSlot != null;
        }

        @TruffleBoundary
        @Specialization(guards = "isLastLine(name)")
        public Object localVariableDefinedLastLine(DynamicObject binding, String name) {
            final MaterializedFrame frame = Layouts.BINDING.getFrame(binding);
            final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(name);
            return frameSlot != null;
        }

        protected boolean isLastLine(String name) {
            return "$_".equals(name);
        }

    }

    @ImportStatic(BindingNodes.class)
    @CoreMethod(names = "local_variable_get", required = 1)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "binding"),
        @NodeChild(type = RubyNode.class, value = "name")
    })
    public abstract static class LocalVariableGetNode extends CoreMethodNode {

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }


        @Specialization(guards = {
                "name == cachedName",
                "!isLastLine(cachedName)",
                "compatibleFrames(binding, cachedBinding)",
                "cachedFrameSlot != null"
        },
                limit = "getCacheLimit()")
        public Object localVariableGetCached(DynamicObject binding, String name,
                                             @Cached("name") String cachedName,
                                             @Cached("binding") DynamicObject cachedBinding,
                                             @Cached("findFrameSlotOrNull(binding, name)") FrameSlotAndDepth cachedFrameSlot,
                                             @Cached("createReadNode(cachedFrameSlot)") ReadFrameSlotNode readLocalVariableNode) {
            final MaterializedFrame frame = RubyArguments.getDeclarationFrame(Layouts.BINDING.getFrame(binding), cachedFrameSlot.depth);
            return readLocalVariableNode.executeRead(frame);
        }

        @TruffleBoundary
        @Specialization(guards = "!isLastLine(name)" )
        public Object localVariableGetUncached(DynamicObject binding, String name) {
            final FrameSlotAndDepth frameSlot = findFrameSlotOrNull(binding, name);
            if (frameSlot == null) {
                throw new RaiseException(coreExceptions().nameErrorLocalVariableNotDefined(name, binding, this));
            } else {
                final MaterializedFrame frame = RubyArguments.getDeclarationFrame(Layouts.BINDING.getFrame(binding), frameSlot.depth);
                return frame.getValue(frameSlot.slot);
            }
        }

        @TruffleBoundary
        @Specialization(guards = "isLastLine(name)")
        public Object localVariableGetLastLine(DynamicObject binding, String name) {
            final MaterializedFrame frame = Layouts.BINDING.getFrame(binding);
            final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(name);

            if (frameSlot == null) {
                throw new RaiseException(coreExceptions().nameErrorLocalVariableNotDefined(name, binding, this));
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

        protected boolean isLastLine(String name) {
            return "$_".equals(name);
        }

        protected int getCacheLimit() {
            return getContext().getOptions().BINDING_LOCAL_VARIABLE_CACHE;
        }

    }

    @ImportStatic(BindingNodes.class)
    @CoreMethod(names = "local_variable_set", required = 2)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "binding"),
        @NodeChild(type = RubyNode.class, value = "name"),
        @NodeChild(type = RubyNode.class, value = "value")
    })
    public abstract static class LocalVariableSetNode extends CoreMethodNode {

        @CreateCast("name")
        public RubyNode coerceToString(RubyNode name) {
            return NameToJavaStringNodeGen.create(name);
        }

        @Specialization(guards = {
                "!isLastLine(name)",
                "getFrameDescriptor(binding) == cachedFrameDescriptor",
                "name == cachedName"
        }, limit = "getCacheLimit()")
        public Object localVariableSetCached(DynamicObject binding, String name, Object value,
                                             @Cached("name") String cachedName,
                                             @Cached("getFrameDescriptor(binding)") FrameDescriptor cachedFrameDescriptor,
                                             @Cached("findFrameSlot(binding, name)") FrameSlotAndDepth cachedFrameSlot,
                                             @Cached("createWriteNode(cachedFrameSlot)") WriteFrameSlotNode writeLocalVariableNode) {
            final MaterializedFrame frame = RubyArguments.getDeclarationFrame(Layouts.BINDING.getFrame(binding), cachedFrameSlot.depth);
            return writeLocalVariableNode.executeWrite(frame, value);
        }

        @TruffleBoundary
        @Specialization(guards = "!isLastLine(name)" )
        public Object localVariableSetUncached(DynamicObject binding, String name, Object value) {
            final FrameSlotAndDepth frameSlot = findFrameSlot(binding, name);
            final MaterializedFrame frame = RubyArguments.getDeclarationFrame(Layouts.BINDING.getFrame(binding), frameSlot.depth);
            frame.setObject(frameSlot.slot, value);
            return value;
        }

        @TruffleBoundary
        @Specialization(guards = "isLastLine(name)")
        public Object localVariableSetLastLine(DynamicObject binding, String name, Object value) {
            final MaterializedFrame frame = Layouts.BINDING.getFrame(binding);
            final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(name);
            frame.setObject(frameSlot, ThreadLocalObject.wrap(getContext(), value));
            return value;
        }

        protected FrameSlotAndDepth findFrameSlot(DynamicObject binding, String name) {
            final FrameSlotAndDepth frameSlot = BindingNodes.findFrameSlotOrNull(binding, name);
            if (frameSlot == null) {
                final FrameSlot newSlot = Layouts.BINDING.getFrame(binding).getFrameDescriptor().addFrameSlot(name);
                return new FrameSlotAndDepth(newSlot, 0);
            } else {
                return frameSlot;
            }
        }

        protected WriteFrameSlotNode createWriteNode(FrameSlotAndDepth frameSlot) {
            return WriteFrameSlotNodeGen.create(frameSlot.slot);
        }

        protected boolean isLastLine(String name) {
            return "$_".equals("name");
        }

        protected int getCacheLimit() {
            return getContext().getOptions().BINDING_LOCAL_VARIABLE_CACHE;
        }
    }

    @CoreMethod(names = "local_variables")
    public abstract static class LocalVariablesNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject localVariables(DynamicObject binding) {
            MaterializedFrame frame = Layouts.BINDING.getFrame(binding);

            return listLocalVariables(getContext(), frame);
        }

        @TruffleBoundary
        public static DynamicObject listLocalVariables(RubyContext context, Frame frame) {
            final Set<Object> names = new LinkedHashSet<>();
            while (frame != null) {
                for (FrameSlot slot : frame.getFrameDescriptor().getSlots()) {
                    if (slot.getIdentifier() instanceof String &&
                            !((String) slot.getIdentifier()).startsWith("rubytruffle_temp_frame_on_stack_marker") &&
                            !Translator.FRAME_LOCAL_GLOBAL_VARIABLES.contains(slot.getIdentifier())) {
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

        @Specialization
        public Object receiver(DynamicObject binding) {
            return RubyArguments.getSelf(Layouts.BINDING.getFrame(binding));
        }
    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            throw new RaiseException(coreExceptions().typeErrorAllocatorUndefinedFor(rubyClass, this));
        }

    }

}
