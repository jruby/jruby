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

import com.oracle.truffle.api.CompilerAsserts;
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
import org.jruby.truffle.om.dsl.api.Nullable;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.ModuleFields;

@CoreClass(name = "Class")
public abstract class ClassNodes {

    @Layout
    public interface ClassLayout extends ModuleNodes.ModuleLayout {

        DynamicObjectFactory createClassShape(DynamicObject logicalClass, DynamicObject metaClass);

        DynamicObject createClass(DynamicObjectFactory factory,
                                  ModuleFields model,
                                  boolean isSingleton,
                                  @Nullable DynamicObject attached,
                                  @Nullable DynamicObjectFactory instanceFactory);

        boolean isClass(DynamicObject object);

        boolean getIsSingleton(DynamicObject object);

        DynamicObject getAttached(DynamicObject object);

        DynamicObjectFactory getInstanceFactory(DynamicObject object);
        void setInstanceFactory(DynamicObject object, DynamicObjectFactory instanceFactory);

    }

    public static final ClassLayout CLASS_LAYOUT = ClassLayoutImpl.INSTANCE;

    /** Special constructor for class Class */
    public static DynamicObject createClassClass(RubyContext context) {
        final ModuleFields model = new ModuleFields(context, null, "Class");

        final com.oracle.truffle.api.object.Layout temporaryLayout = com.oracle.truffle.api.object.Layout.createLayout();

        final DynamicObject rubyClass = temporaryLayout.newInstance(temporaryLayout.createShape(new ObjectType()));

        final DynamicObjectFactory factory = ClassNodes.CLASS_LAYOUT.createClassShape(rubyClass, rubyClass);

        rubyClass.setShapeAndGrow(rubyClass.getShape(), factory.getShape());
        assert RubyGuards.isRubyModule(rubyClass);
        assert RubyGuards.isRubyClass(rubyClass);

        model.rubyModuleObject = rubyClass;
        CLASS_LAYOUT.setInstanceFactory(rubyClass, factory);
        ModuleNodes.MODULE_LAYOUT.setFields(rubyClass, model);
        model.name = model.givenBaseName;

        assert RubyGuards.isRubyModule(rubyClass);
        assert RubyGuards.isRubyClass(rubyClass);
        assert ModuleNodes.getFields(rubyClass) == model;
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
        final ModuleFields model = new ModuleFields(BasicObjectNodes.getContext(classClass), null, name);

        final DynamicObject rubyClass = CLASS_LAYOUT.createClass(CLASS_LAYOUT.getInstanceFactory(classClass), model, false, null, null);
        assert RubyGuards.isRubyClass(rubyClass) : classClass.getShape().getObjectType().getClass();
        assert RubyGuards.isRubyModule(rubyClass) : classClass.getShape().getObjectType().getClass();

        model.rubyModuleObject = rubyClass;

        if (model.lexicalParent == null) { // bootstrap or anonymous module
            ModuleNodes.getFields(rubyClass).name = ModuleNodes.getFields(rubyClass).givenBaseName;
        } else {
            ModuleNodes.getFields(rubyClass).getAdoptedByLexicalParent(model.lexicalParent, model.givenBaseName, null);
        }

        if (superclass != null) {
            assert RubyGuards.isRubyClass(superclass);
            assert RubyGuards.isRubyClass(ModuleNodes.getFields(rubyClass).rubyModuleObject);

            ModuleNodes.getFields(rubyClass).parentModule = ModuleNodes.getFields(superclass).start;
            ModuleNodes.getFields(superclass).addDependent(ModuleNodes.getFields(rubyClass).rubyModuleObject);

            ModuleNodes.getFields(rubyClass).newVersion();
        }

        return rubyClass;
    }

    public static DynamicObject createSingletonClassOfObject(RubyContext context, DynamicObject superclass, DynamicObject attached, String name) {
        // We also need to create the singleton class of a singleton class for proper lookup and consistency.
        // See rb_singleton_class() documentation in MRI.
        // Allocator is null here, we cannot create instances of singleton classes.
        assert RubyGuards.isRubyClass(superclass);
        assert attached == null || RubyGuards.isRubyModule(attached);
        return ensureSingletonConsistency(createRubyClass(context, BasicObjectNodes.getLogicalClass(superclass), null, superclass, name, true, attached));
    }

    public static DynamicObject createRubyClass(RubyContext context, DynamicObject lexicalParent, DynamicObject superclass, String name) {
        final DynamicObject rubyClass = createRubyClass(context, BasicObjectNodes.getLogicalClass(superclass), lexicalParent, superclass, name, false, null);
        ensureSingletonConsistency(rubyClass);
        return rubyClass;
    }

    public static DynamicObject createRubyClass(RubyContext context, DynamicObject classClass, DynamicObject lexicalParent, DynamicObject superclass, String name, boolean isSingleton, DynamicObject attached) {
        final ModuleFields model = new ModuleFields(context, lexicalParent, name);

        final DynamicObject rubyClass = CLASS_LAYOUT.createClass(CLASS_LAYOUT.getInstanceFactory(classClass), model, isSingleton, attached, null);
        assert RubyGuards.isRubyClass(rubyClass) : classClass.getShape().getObjectType().getClass();
        assert RubyGuards.isRubyModule(rubyClass) : classClass.getShape().getObjectType().getClass();

        model.rubyModuleObject = rubyClass;

        if (model.lexicalParent == null) { // bootstrap or anonymous module
            ModuleNodes.getFields(rubyClass).name = ModuleNodes.getFields(rubyClass).givenBaseName;
        } else {
            ModuleNodes.getFields(rubyClass).getAdoptedByLexicalParent(model.lexicalParent, model.givenBaseName, null);
        }

        if (superclass != null) {
            assert RubyGuards.isRubyClass(superclass);
            assert RubyGuards.isRubyClass(ModuleNodes.getFields(rubyClass).rubyModuleObject);

            ModuleNodes.getFields(rubyClass).parentModule = ModuleNodes.getFields(superclass).start;
            ModuleNodes.getFields(superclass).addDependent(ModuleNodes.getFields(rubyClass).rubyModuleObject);

            ModuleNodes.getFields(rubyClass).newVersion();
        }

        DynamicObjectFactory factory = CLASS_LAYOUT.getInstanceFactory(superclass);
        factory = BasicObjectNodes.BASIC_OBJECT_LAYOUT.setLogicalClass(factory, rubyClass);
        factory = BasicObjectNodes.BASIC_OBJECT_LAYOUT.setMetaClass(factory, rubyClass);
        CLASS_LAYOUT.setInstanceFactory(rubyClass, factory);

        return rubyClass;
    }


    public static void initialize(DynamicObject rubyClass, DynamicObject superclass) {
        assert RubyGuards.isRubyClass(superclass);

        ModuleNodes.getFields(rubyClass).parentModule = ModuleNodes.getFields(superclass).start;
        ModuleNodes.getFields(superclass).addDependent(rubyClass);

        ModuleNodes.getFields(rubyClass).newVersion();
        ensureSingletonConsistency(rubyClass);

        DynamicObjectFactory factory = CLASS_LAYOUT.getInstanceFactory(superclass);
        factory = BasicObjectNodes.BASIC_OBJECT_LAYOUT.setLogicalClass(factory, rubyClass);
        factory = BasicObjectNodes.BASIC_OBJECT_LAYOUT.setMetaClass(factory, rubyClass);
        CLASS_LAYOUT.setInstanceFactory(rubyClass, factory);
    }

    public static DynamicObject ensureSingletonConsistency(DynamicObject rubyClass) {
        createOneSingletonClass(rubyClass);
        return rubyClass;
    }

    public static DynamicObject getSingletonClass(DynamicObject rubyClass) {
        // We also need to create the singleton class of a singleton class for proper lookup and consistency.
        // See rb_singleton_class() documentation in MRI.
        return ensureSingletonConsistency(createOneSingletonClass(rubyClass));
    }

    public static DynamicObject createOneSingletonClass(DynamicObject rubyClass) {
        CompilerAsserts.neverPartOfCompilation();

        if (isSingleton(BasicObjectNodes.getMetaClass(rubyClass))) {
            return BasicObjectNodes.getMetaClass(rubyClass);
        }

        final DynamicObject singletonSuperclass;
        if (getSuperClass(rubyClass) == null) {
            singletonSuperclass = BasicObjectNodes.getLogicalClass(rubyClass);
        } else {
            singletonSuperclass = createOneSingletonClass(getSuperClass(rubyClass));
        }

        String name = String.format("#<Class:%s>", ModuleNodes.getFields(rubyClass).getName());
        BasicObjectNodes.setMetaClass(rubyClass, ClassNodes.createRubyClass(BasicObjectNodes.getContext(rubyClass), BasicObjectNodes.getLogicalClass(rubyClass), null, singletonSuperclass, name, true, rubyClass));

        return BasicObjectNodes.getMetaClass(rubyClass);
    }


    public static boolean isSingleton(DynamicObject rubyClass) {
        return CLASS_LAYOUT.getIsSingleton(rubyClass);
    }

    public static DynamicObject getAttached(DynamicObject rubyClass) {
        return CLASS_LAYOUT.getAttached(rubyClass);
    }

    public static DynamicObject getSuperClass(DynamicObject rubyClass) {
        CompilerAsserts.neverPartOfCompilation();

        for (DynamicObject ancestor : ModuleNodes.getFields(rubyClass).parentAncestors()) {
            if (RubyGuards.isRubyClass(ancestor)) {
                return ancestor;
            }
        }

        return null;
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

            ClassNodes.initialize(rubyClass, superclass);
            triggerInheritedHook(frame, rubyClass, superclass);

            return rubyClass;
        }

        private DynamicObject initializeGeneralWithBlock(VirtualFrame frame, DynamicObject rubyClass, DynamicObject superclass, DynamicObject block) {
            assert RubyGuards.isRubyClass(rubyClass);
            assert RubyGuards.isRubyClass(superclass);
            assert RubyGuards.isRubyProc(block);

            ClassNodes.initialize(rubyClass, superclass);
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
            final DynamicObject superclass = ClassNodes.getSuperClass(rubyClass);
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
