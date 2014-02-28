/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.nodes.call.DispatchHeadNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.truffle.runtime.core.array.RubyArray;
import org.jruby.truffle.runtime.core.RubyBasicObject;

@CoreClass(name = "Class")
public abstract class ClassNodes {

    @CoreMethod(names = "===", minArgs = 1, maxArgs = 1)
    public abstract static class ContainsInstanceNode extends CoreMethodNode {

        public ContainsInstanceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ContainsInstanceNode(ContainsInstanceNode prev) {
            super(prev);
        }

        @Specialization
        public boolean containsInstance(RubyClass rubyClass, RubyBasicObject instance) {
            return instance.getRubyClass().assignableTo(rubyClass);
        }
    }

    @CoreMethod(names = "new", needsBlock = true, isSplatted = true)
    public abstract static class NewNode extends CoreMethodNode {

        @Child protected DispatchHeadNode initialize;

        public NewNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            initialize = adoptChild(new DispatchHeadNode(context, getSourceSection(), "initialize", false));
        }

        public NewNode(NewNode prev) {
            super(prev);
            initialize = adoptChild(prev.initialize);
        }

        @Specialization
        public RubyBasicObject newInstance(VirtualFrame frame, RubyClass rubyClass, Object[] args, @SuppressWarnings("unused") UndefinedPlaceholder block) {
            return doNewInstance(frame, rubyClass, args, null);
        }

        @Specialization
        public RubyBasicObject newInstance(VirtualFrame frame, RubyClass rubyClass, Object[] args, RubyProc block) {
            return doNewInstance(frame, rubyClass, args, block);
        }

        private RubyBasicObject doNewInstance(VirtualFrame frame, RubyClass rubyClass, Object[] args, RubyProc block) {
            final RubyBasicObject instance = rubyClass.newInstance();
            initialize.dispatch(frame, instance, block, args);
            return instance;
        }

    }

    @CoreMethod(names = "to_s", maxArgs = 0)
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString toS(RubyClass rubyClass) {
            return getContext().makeString(rubyClass.getName());
        }
    }

    @CoreMethod(names = "class_variables", maxArgs = 0)
    public abstract static class ClassVariablesNode extends CoreMethodNode {

        public ClassVariablesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ClassVariablesNode(ClassVariablesNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray getClassVariables(RubyClass rubyClass) {
            final RubyArray array = new RubyArray(rubyClass.getContext().getCoreLibrary().getArrayClass());

            for (String variable : rubyClass.getClassVariables()) {
                array.push(RubySymbol.newSymbol(rubyClass.getContext(), variable));
            }
            return array;
        }
    }

}
