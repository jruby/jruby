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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyModuleModel;

@CoreClass(name = "Class")
public abstract class ClassNodes {

    @Layout
    public interface ClassLayout extends ModuleNodes.ModuleLayout {

        DynamicObjectFactory createClassShape(DynamicObject logicalClass, DynamicObject metaClass);

        DynamicObject createClass(DynamicObjectFactory factory, RubyModuleModel model);

        boolean isClass(DynamicObject dynamicObject);

    }

    public static final ClassLayout CLASS_LAYOUT = ClassLayoutImpl.INSTANCE;

    /** Special constructor for class Class */
    public static DynamicObject createClassClass(RubyContext context) {
        final RubyModuleModel model = new RubyModuleModel(context, null, "Class", false, null, null);

        final com.oracle.truffle.api.object.Layout temporaryLayout = com.oracle.truffle.api.object.Layout.createLayout();

        final DynamicObject rubyClass = temporaryLayout.newInstance(temporaryLayout.createShape(new ObjectType()));

        model.factory = ClassNodes.CLASS_LAYOUT.createClassShape(rubyClass, rubyClass);
        model.rubyModuleObject = rubyClass;

        rubyClass.setShapeAndGrow(rubyClass.getShape(), model.factory.getShape());
        assert RubyGuards.isRubyModule(rubyClass);
        assert RubyGuards.isRubyClass(rubyClass);

        ModuleNodes.MODULE_LAYOUT.setModel(rubyClass, model);

        model.name = model.givenBaseName;

        assert RubyGuards.isRubyModule(rubyClass);
        assert RubyGuards.isRubyClass(rubyClass);
        assert ModuleNodes.getModel(rubyClass) == model;
        assert BasicObjectNodes.getLogicalClass(rubyClass) == rubyClass;

        return rubyClass;
    }

    /**
     * This constructor supports initialization and solves boot-order problems and should not
     * normally be used from outside this class.
     */
    public static DynamicObject createBootClass(DynamicObject classClass, DynamicObject superclass, String name) {
        assert RubyGuards.isRubyClass(classClass);
        assert superclass == null || RubyGuards.isRubyClass(superclass);
        final RubyModuleModel model = new RubyModuleModel(BasicObjectNodes.getContext(classClass), null, name, false, null, null);

        final DynamicObject rubyClass = CLASS_LAYOUT.createClass(ModuleNodes.getModel(classClass).factory, model);
        assert RubyGuards.isRubyClass(rubyClass) : classClass.getShape().getObjectType().getClass();
        assert RubyGuards.isRubyModule(rubyClass) : classClass.getShape().getObjectType().getClass();

        model.rubyModuleObject = rubyClass;

        if (model.lexicalParent == null) { // bootstrap or anonymous module
            ModuleNodes.getModel(rubyClass).name = ModuleNodes.getModel(rubyClass).givenBaseName;
        } else {
            ModuleNodes.getModel(rubyClass).getAdoptedByLexicalParent(model.lexicalParent, model.givenBaseName, null);
        }

        if (superclass != null) {
            assert RubyGuards.isRubyClass(superclass);
            assert ModuleNodes.getModel(rubyClass).isClass();

            ModuleNodes.getModel(rubyClass).parentModule = ModuleNodes.getModel(superclass).start;
            ModuleNodes.getModel(superclass).addDependent(ModuleNodes.getModel(rubyClass).rubyModuleObject);

            ModuleNodes.getModel(rubyClass).newVersion();
        }

        return rubyClass;
    }

    public static DynamicObject createSingletonClassOfObject(RubyContext context, DynamicObject superclass, DynamicObject attached, String name) {
        // We also need to create the singleton class of a singleton class for proper lookup and consistency.
        // See rb_singleton_class() documentation in MRI.
        // Allocator is null here, we cannot create instances of singleton classes.
        assert RubyGuards.isRubyClass(superclass);
        assert attached == null || RubyGuards.isRubyModule(attached);
        return ModuleNodes.getModel(createRubyClass(context, BasicObjectNodes.getLogicalClass(superclass), null, superclass, name, true, attached)).ensureSingletonConsistency();
    }

    public static DynamicObject createRubyClass(RubyContext context, DynamicObject lexicalParent, DynamicObject superclass, String name) {
        final DynamicObject rubyClass = createRubyClass(context, BasicObjectNodes.getLogicalClass(superclass), lexicalParent, superclass, name, false, null);
        ModuleNodes.getModel(rubyClass).ensureSingletonConsistency();

        return rubyClass;
    }

    public static DynamicObject createRubyClass(RubyContext context, DynamicObject classClass, DynamicObject lexicalParent, DynamicObject superclass, String name, boolean isSingleton, DynamicObject attached) {
        final RubyModuleModel model = new RubyModuleModel(context, lexicalParent, name, isSingleton, attached, null);

        final DynamicObject rubyClass = CLASS_LAYOUT.createClass(ModuleNodes.getModel(classClass).factory, model);
        assert RubyGuards.isRubyClass(rubyClass) : classClass.getShape().getObjectType().getClass();
        assert RubyGuards.isRubyModule(rubyClass) : classClass.getShape().getObjectType().getClass();

        model.rubyModuleObject = rubyClass;

        if (model.lexicalParent == null) { // bootstrap or anonymous module
            ModuleNodes.getModel(rubyClass).name = ModuleNodes.getModel(rubyClass).givenBaseName;
        } else {
            ModuleNodes.getModel(rubyClass).getAdoptedByLexicalParent(model.lexicalParent, model.givenBaseName, null);
        }

        if (superclass != null) {
            assert RubyGuards.isRubyClass(superclass);
            assert ModuleNodes.getModel(rubyClass).isClass();

            ModuleNodes.getModel(rubyClass).parentModule = ModuleNodes.getModel(superclass).start;
            ModuleNodes.getModel(superclass).addDependent(ModuleNodes.getModel(rubyClass).rubyModuleObject);

            ModuleNodes.getModel(rubyClass).newVersion();
        }

        DynamicObjectFactory factory = ModuleNodes.getModel(superclass).factory;
        factory = BasicObjectNodes.BASIC_OBJECT_LAYOUT.setLogicalClass(factory, rubyClass);
        factory = BasicObjectNodes.BASIC_OBJECT_LAYOUT.setMetaClass(factory, rubyClass);
        ModuleNodes.getModel(rubyClass).factory = factory;

        return rubyClass;
    }

    @CoreMethod(names = "new", needsBlock = true, rest = true)
    public abstract static class NewNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode allocateNode;
        @Child private CallDispatchHeadNode initialize;

        public NewNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateNode = DispatchHeadNodeFactory.createMethodCallOnSelf(context);
            initialize = DispatchHeadNodeFactory.createMethodCallOnSelf(context);
        }

        @Specialization
        public Object newInstance(VirtualFrame frame, DynamicObject rubyClass, Object[] args, NotProvided block) {
            return doNewInstance(frame, rubyClass, args, null);
        }

        @Specialization(guards = "isRubyProc(block)")
        public Object newInstance(VirtualFrame frame, DynamicObject rubyClass, Object[] args, DynamicObject block) {
            return doNewInstance(frame, rubyClass, args, block);
        }

        private Object doNewInstance(VirtualFrame frame, DynamicObject rubyClass, Object[] args, DynamicObject block) {
            assert block == null || RubyGuards.isRubyProc(block);

            final Object instance = allocateNode.call(frame, rubyClass, "allocate", null);
            initialize.call(frame, instance, "initialize", block, args);
            return instance;
        }
    }

    @CoreMethod(names = "initialize", optional = 1, needsBlock = true)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        @Child private ModuleNodes.InitializeNode moduleInitializeNode;
        @Child private CallDispatchHeadNode inheritedNode;

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        void triggerInheritedHook(VirtualFrame frame, DynamicObject subClass, DynamicObject superClass) {
            assert RubyGuards.isRubyClass(subClass);
            assert RubyGuards.isRubyClass(superClass);

            if (inheritedNode == null) {
                CompilerDirectives.transferToInterpreter();
                inheritedNode = insert(DispatchHeadNodeFactory.createMethodCallOnSelf(getContext()));
            }
            inheritedNode.call(frame, superClass, "inherited", null, subClass);
        }

        void moduleInitialize(VirtualFrame frame, DynamicObject rubyClass, DynamicObject block) {
            assert RubyGuards.isRubyClass(rubyClass);
            assert RubyGuards.isRubyProc(block);

            if (moduleInitializeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                moduleInitializeNode = insert(ModuleNodesFactory.InitializeNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{null,null}));
            }
            moduleInitializeNode.executeInitialize(frame, rubyClass, block);
        }

        @Specialization
        public DynamicObject initialize(VirtualFrame frame, DynamicObject rubyClass, NotProvided superclass, NotProvided block) {
            return initializeGeneralWithoutBlock(frame, rubyClass, getContext().getCoreLibrary().getObjectClass());
        }

        @Specialization(guards = "isRubyClass(superclass)")
        public DynamicObject initialize(VirtualFrame frame, DynamicObject rubyClass, DynamicObject superclass, NotProvided block) {
            return initializeGeneralWithoutBlock(frame, rubyClass, superclass);
        }

        @Specialization(guards = "isRubyProc(block)")
        public DynamicObject initialize(VirtualFrame frame, DynamicObject rubyClass, NotProvided superclass, DynamicObject block) {
            return initializeGeneralWithBlock(frame, rubyClass, getContext().getCoreLibrary().getObjectClass(), block);
        }

        @Specialization(guards = {"isRubyClass(superclass)", "isRubyProc(block)"})
        public DynamicObject initialize(VirtualFrame frame, DynamicObject rubyClass, DynamicObject superclass, DynamicObject block) {
            return initializeGeneralWithBlock(frame, rubyClass, superclass, block);
        }

        private DynamicObject initializeGeneralWithoutBlock(VirtualFrame frame, DynamicObject rubyClass, DynamicObject superclass) {
            assert RubyGuards.isRubyClass(rubyClass);
            assert RubyGuards.isRubyClass(superclass);

            ModuleNodes.getModel(rubyClass).initialize(superclass);
            triggerInheritedHook(frame, rubyClass, superclass);

            return rubyClass;
        }

        private DynamicObject initializeGeneralWithBlock(VirtualFrame frame, DynamicObject rubyClass, DynamicObject superclass, DynamicObject block) {
            assert RubyGuards.isRubyClass(rubyClass);
            assert RubyGuards.isRubyClass(superclass);
            assert RubyGuards.isRubyProc(block);

            ModuleNodes.getModel(rubyClass).initialize(superclass);
            triggerInheritedHook(frame, rubyClass, superclass);
            moduleInitialize(frame, rubyClass, block);

            return rubyClass;
        }

    }

    @CoreMethod(names = "inherited", required = 1, visibility = Visibility.PRIVATE)
    public abstract static class InheritedNode extends CoreMethodArrayArgumentsNode {

        public InheritedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject inherited(Object subclass) {
            return nil();
        }

    }

    @CoreMethod(names = "superclass")
    public abstract static class SuperClassNode extends CoreMethodArrayArgumentsNode {

        public SuperClassNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object getSuperClass(DynamicObject rubyClass) {
            final DynamicObject superclass = ModuleNodes.getModel(rubyClass).getSuperClass();
            if (superclass == null) {
                return nil();
            } else {
                return superclass;
            }
        }
    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateConstructorNode extends CoreMethodArrayArgumentsNode {

        public AllocateConstructorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return createRubyClass(getContext(), getContext().getCoreLibrary().getClassClass(), null, getContext().getCoreLibrary().getObjectClass(), null, false, null);
        }

    }
}
