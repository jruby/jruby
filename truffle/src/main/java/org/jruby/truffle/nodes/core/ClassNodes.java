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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyProc;

@CoreClass(name = "Class")
public abstract class ClassNodes {

    @CoreMethod(names = "allocate")
    public abstract static class AllocateNode extends CoreMethodNode {

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AllocateNode(AllocateNode prev) {
            super(prev);
        }

        public abstract RubyBasicObject executeAllocate(VirtualFrame frame, RubyClass rubyClass);

        @Specialization
        public RubyBasicObject allocate(RubyClass rubyClass) {
            if (rubyClass.isSingleton()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeError("can't create instance of singleton class", this));
            }
            return rubyClass.allocate(this);
        }

    }

    @CoreMethod(names = "new", needsBlock = true, argumentsAsArray = true)
    public abstract static class NewNode extends CoreMethodNode {

        @Child private AllocateNode allocateNode;
        @Child private CallDispatchHeadNode initialize;
        @CompilerDirectives.CompilationFinal private boolean isCached = true;
        @CompilerDirectives.CompilationFinal private RubyClass cachedClass;

        public NewNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateNode = ClassNodesFactory.AllocateNodeFactory.create(context, sourceSection, new RubyNode[]{null});
            initialize = DispatchHeadNodeFactory.createMethodCallOnSelf(context);
        }

        public NewNode(NewNode prev) {
            super(prev);
            allocateNode = prev.allocateNode;
            initialize = prev.initialize;
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
            final RubyBasicObject instance = allocateNode.executeAllocate(frame, rubyClass);
            initialize.call(frame, instance, "initialize", block, args);
            return instance;
        }
    }

    @CoreMethod(names = "initialize", optional = 1, needsBlock = true)
    public abstract static class InitializeNode extends CoreMethodNode {

        @Child private ModuleNodes.InitializeNode moduleInitializeNode;

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
        }

        void moduleInitialize(VirtualFrame frame, RubyClass rubyClass, RubyProc block) {
            if (moduleInitializeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                moduleInitializeNode = insert(ModuleNodesFactory.InitializeNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{null,null}));
            }
            moduleInitializeNode.executeInitialize(frame, rubyClass, block);
        }

        @Specialization
        public RubyClass initialize(RubyClass rubyClass, UndefinedPlaceholder superclass, UndefinedPlaceholder block) {
            return initialize(rubyClass, getContext().getCoreLibrary().getObjectClass(), block);
        }

        @Specialization
        public RubyClass initialize(RubyClass rubyClass, RubyClass superclass, UndefinedPlaceholder block) {
            rubyClass.initialize(superclass);
            return rubyClass;
        }

        @Specialization
        public RubyClass initialize(VirtualFrame frame, RubyClass rubyClass, UndefinedPlaceholder superclass, RubyProc block) {
            return initialize(frame, rubyClass, getContext().getCoreLibrary().getObjectClass(), block);
        }

        @Specialization
        public RubyClass initialize(VirtualFrame frame, RubyClass rubyClass, RubyClass superclass, RubyProc block) {
            rubyClass.initialize(superclass);
            moduleInitialize(frame, rubyClass, block);
            return rubyClass;
        }

    }

    @CoreMethod(names = "inherited", required = 1, visibility = Visibility.PRIVATE)
    public abstract static class InheritedNode extends CoreMethodNode {

        public InheritedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InheritedNode(InheritedNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass inherited(Object subclass) {
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "superclass")
    public abstract static class SuperClassNode extends CoreMethodNode {

        public SuperClassNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SuperClassNode(SuperClassNode prev) {
            super(prev);
        }

        @Specialization
        public RubyClass getSuperClass(RubyClass rubyClass) {
            return rubyClass.getSuperClass();
        }
    }
}
