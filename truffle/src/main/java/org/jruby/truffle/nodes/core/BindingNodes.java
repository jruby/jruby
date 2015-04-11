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

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.Ruby;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.globals.GetFromThreadLocalNode;
import org.jruby.truffle.nodes.globals.WrapInThreadLocalNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.InternalMethod;

@CoreClass(name = "Binding")
public abstract class BindingNodes {

    @CoreMethod(names = "initialize_copy", visibility = Visibility.PRIVATE, required = 1)
    public abstract static class InitializeCopyNode extends CoreMethodNode {

        public InitializeCopyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object initializeCopy(RubyBinding self, RubyBinding from) {
            notDesignedForCompilation();

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
    public abstract static class LocalVariableGetNode extends CoreMethodNode {

        public LocalVariableGetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object localVariableGet(RubyBinding binding, RubySymbol symbol) {
            notDesignedForCompilation();

            final MaterializedFrame frame = binding.getFrame();

            Object value = frame.getValue(frame.getFrameDescriptor().findFrameSlot(symbol.toString()));

            // TODO(CS): temporary hack for $_
            if (symbol.toString().equals("$_")) {
                value = GetFromThreadLocalNode.get(getContext(), value);
            }

            return value;
        }
    }

    @CoreMethod(names = "local_variable_set", required = 2)
    public abstract static class LocalVariableSetNode extends CoreMethodNode {

        public LocalVariableSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object localVariableSetNode(RubyBinding binding, RubySymbol symbol, Object value) {
            notDesignedForCompilation();

            // TODO(CS): temporary hack for $_
            if (symbol.toString().equals("$_")) {
                value = WrapInThreadLocalNode.wrap(getContext(), value);
            }

            MaterializedFrame frame = binding.getFrame();

            while (frame != null) {
                final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(symbol.toString());

                if (frameSlot != null) {
                    frame.setObject(frameSlot, value);
                    return value;
                }

                frame = RubyArguments.getDeclarationFrame(frame.getArguments());
            }

            final FrameSlot newFrameSlot = binding.getFrame().getFrameDescriptor().addFrameSlot(symbol.toString());
            binding.getFrame().setObject(newFrameSlot, value);
            return value;
        }
    }

    @CoreMethod(names = "local_variables")
    public abstract static class LocalVariablesNode extends CoreMethodNode {

        public LocalVariablesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray localVariables(RubyBinding binding) {
            notDesignedForCompilation();

            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass());

            MaterializedFrame frame = binding.getFrame();

            while (frame != null) {
                for (Object name : frame.getFrameDescriptor().getIdentifiers()) {
                    if (name instanceof String) {
                        array.slowPush(getContext().newSymbol((String) name));
                    }
                }

                frame = RubyArguments.getDeclarationFrame(frame.getArguments());
            }

            return array;
        }
    }

}
