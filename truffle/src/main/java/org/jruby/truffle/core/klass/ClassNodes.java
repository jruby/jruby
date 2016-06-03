/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.klass;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.module.ModuleFields;
import org.jruby.truffle.core.module.ModuleNodes;
import org.jruby.truffle.core.module.ModuleNodesFactory;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

@CoreClass("Class")
public abstract class ClassNodes {

    private final static com.oracle.truffle.api.object.Layout LAYOUT = com.oracle.truffle.api.object.Layout.createLayout();

    /**
     * Special constructor for class Class
     */
    @TruffleBoundary
    public static DynamicObject createClassClass(RubyContext context) {
        final ModuleFields model = new ModuleFields(context, null, "Class");

        final DynamicObject rubyClass = LAYOUT.newInstance(LAYOUT.createShape(new ObjectType()));

        final DynamicObjectFactory factory = Layouts.CLASS.createClassShape(rubyClass, rubyClass);

        rubyClass.setShapeAndGrow(rubyClass.getShape(), factory.getShape());
        assert RubyGuards.isRubyModule(rubyClass);
        assert RubyGuards.isRubyClass(rubyClass);

        model.rubyModuleObject = rubyClass;
        Layouts.CLASS.setInstanceFactoryUnsafe(rubyClass, factory);
        Layouts.MODULE.setFields(rubyClass, model);
        model.setFullName(model.givenBaseName);

        assert RubyGuards.isRubyModule(rubyClass);
        assert RubyGuards.isRubyClass(rubyClass);
        assert Layouts.MODULE.getFields(rubyClass) == model;
        assert Layouts.BASIC_OBJECT.getLogicalClass(rubyClass) == rubyClass;

        return rubyClass;
    }

    /**
     * This constructor supports initialization and solves boot-order problems and should not
     * normally be used from outside this class.
     */
    @TruffleBoundary
    public static DynamicObject createBootClass(RubyContext context, DynamicObject classClass, DynamicObject superclass, String name) {
        assert RubyGuards.isRubyClass(classClass);
        assert superclass == null || RubyGuards.isRubyClass(superclass);
        final ModuleFields model = new ModuleFields(context, null, name);

        final DynamicObject rubyClass = Layouts.CLASS.createClass(Layouts.CLASS.getInstanceFactory(classClass), model, false, null, null, null);
        assert RubyGuards.isRubyClass(rubyClass) : classClass.getShape().getObjectType().getClass();
        assert RubyGuards.isRubyModule(rubyClass) : classClass.getShape().getObjectType().getClass();

        model.rubyModuleObject = rubyClass;

        final ModuleFields fields = Layouts.MODULE.getFields(rubyClass);
        if (model.lexicalParent == null) { // bootstrap or anonymous module
            fields.setFullName(fields.givenBaseName);
        } else {
            fields.getAdoptedByLexicalParent(context, model.lexicalParent, model.givenBaseName, null);
        }

        if (superclass != null) {
            assert RubyGuards.isRubyClass(superclass);
            assert RubyGuards.isRubyClass(rubyClass);

            fields.parentModule = Layouts.MODULE.getFields(superclass).start;
            Layouts.MODULE.getFields(superclass).addDependent(rubyClass);
            Layouts.CLASS.setSuperclass(rubyClass, superclass);

            fields.newVersion();
        }

        return rubyClass;
    }

    @TruffleBoundary
    public static DynamicObject createSingletonClassOfObject(RubyContext context, DynamicObject superclass, DynamicObject attached, String name) {
        // We also need to create the singleton class of a singleton class for proper lookup and consistency.
        // See rb_singleton_class() documentation in MRI.
        // Allocator is null here, we cannot create instances of singleton classes.
        assert RubyGuards.isRubyClass(superclass);
        assert attached != null;
        return ensureItHasSingletonClassCreated(context, createRubyClass(context, Layouts.BASIC_OBJECT.getLogicalClass(superclass), null, superclass, name, true, attached, true));
    }

    @TruffleBoundary
    public static DynamicObject createInitializedRubyClass(RubyContext context, DynamicObject lexicalParent, DynamicObject superclass, String name) {
        final DynamicObject rubyClass = createRubyClass(context, Layouts.BASIC_OBJECT.getLogicalClass(superclass), lexicalParent, superclass, name, false, null, true);
        ensureItHasSingletonClassCreated(context, rubyClass);
        return rubyClass;
    }

    @TruffleBoundary
    public static DynamicObject createRubyClass(RubyContext context,
                                                DynamicObject classClass,
                                                DynamicObject lexicalParent,
                                                DynamicObject superclass,
                                                String name,
                                                boolean isSingleton,
                                                DynamicObject attached,
                                                boolean initialized) {
        assert superclass == null || RubyGuards.isRubyClass(superclass);

        final ModuleFields model = new ModuleFields(context, lexicalParent, name);

        final DynamicObject rubyClass = Layouts.CLASS.createClass(
                Layouts.CLASS.getInstanceFactory(classClass),
                model,
                isSingleton,
                attached,
                null,
                initialized ? superclass : null);
        assert RubyGuards.isRubyClass(rubyClass) : classClass.getShape().getObjectType().getClass();
        assert RubyGuards.isRubyModule(rubyClass) : classClass.getShape().getObjectType().getClass();

        model.rubyModuleObject = rubyClass;

        final ModuleFields fields = Layouts.MODULE.getFields(rubyClass);
        if (model.lexicalParent != null) {
            fields.getAdoptedByLexicalParent(context, model.lexicalParent, model.givenBaseName, null);
        } else if (fields.givenBaseName != null) { // bootstrap module
            fields.setFullName(fields.givenBaseName);
        }

        if (superclass != null) {
            fields.parentModule = Layouts.MODULE.getFields(superclass).start;
            Layouts.MODULE.getFields(superclass).addDependent(rubyClass);

            fields.newVersion();
        }

        DynamicObjectFactory factory = Layouts.CLASS.getInstanceFactory(superclass);
        factory = Layouts.BASIC_OBJECT.setLogicalClass(factory, rubyClass);
        factory = Layouts.BASIC_OBJECT.setMetaClass(factory, rubyClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(rubyClass, factory);

        return rubyClass;
    }

    @TruffleBoundary
    public static void initialize(RubyContext context, DynamicObject rubyClass, DynamicObject superclass) {
        assert RubyGuards.isRubyClass(superclass);

        Layouts.MODULE.getFields(rubyClass).parentModule = Layouts.MODULE.getFields(superclass).start;
        Layouts.MODULE.getFields(superclass).addDependent(rubyClass);

        Layouts.MODULE.getFields(rubyClass).newVersion();
        ensureItHasSingletonClassCreated(context, rubyClass);

        DynamicObjectFactory factory = Layouts.CLASS.getInstanceFactory(superclass);
        factory = Layouts.BASIC_OBJECT.setLogicalClass(factory, rubyClass);
        factory = Layouts.BASIC_OBJECT.setMetaClass(factory, rubyClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(rubyClass, factory);
        // superclass is set only here in initialize method to its final value
        Layouts.CLASS.setSuperclass(rubyClass, superclass);
    }

    private static DynamicObject ensureItHasSingletonClassCreated(RubyContext context, DynamicObject rubyClass) {
        getLazyCreatedSingletonClass(context, rubyClass);
        return rubyClass;
    }

    @TruffleBoundary
    public static DynamicObject getSingletonClass(RubyContext context, DynamicObject rubyClass) {
        // We also need to create the singleton class of a singleton class for proper lookup and consistency.
        // See rb_singleton_class() documentation in MRI.
        return ensureItHasSingletonClassCreated(context, getLazyCreatedSingletonClass(context, rubyClass));
    }

    private static DynamicObject getLazyCreatedSingletonClass(RubyContext context, DynamicObject rubyClass) {
        CompilerAsserts.neverPartOfCompilation();

        if (Layouts.CLASS.getIsSingleton(Layouts.BASIC_OBJECT.getMetaClass(rubyClass))) {
            return Layouts.BASIC_OBJECT.getMetaClass(rubyClass);
        }

        final DynamicObject singletonSuperclass;
        if (getSuperClass(rubyClass) == null) {
            singletonSuperclass = Layouts.BASIC_OBJECT.getLogicalClass(rubyClass);
        } else {
            singletonSuperclass = getLazyCreatedSingletonClass(context, getSuperClass(rubyClass));
        }

        String name = String.format("#<Class:%s>", Layouts.MODULE.getFields(rubyClass).getName());
        Layouts.BASIC_OBJECT.setMetaClass(rubyClass, ClassNodes.createRubyClass(context, Layouts.BASIC_OBJECT.getLogicalClass(rubyClass), null, singletonSuperclass, name, true, rubyClass, true));

        return Layouts.BASIC_OBJECT.getMetaClass(rubyClass);
    }

    public static DynamicObject getSuperClass(DynamicObject rubyClass) {
        CompilerAsserts.neverPartOfCompilation();

        for (DynamicObject ancestor : Layouts.MODULE.getFields(rubyClass).parentAncestors()) {
            if (RubyGuards.isRubyClass(ancestor) && ancestor != rubyClass) {
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

        @Specialization
        public Object newInstance(VirtualFrame frame, DynamicObject rubyClass, Object[] args, DynamicObject block) {
            return doNewInstance(frame, rubyClass, args, block);
        }

        private Object doNewInstance(VirtualFrame frame, DynamicObject rubyClass, Object[] args, DynamicObject block) {
            final Object instance = allocateNode.call(frame, rubyClass, "allocate", null);
            initialize.call(frame, instance, "initialize", block, args);
            return instance;
        }
    }

    @CoreMethod(names = "initialize", optional = 1, needsBlock = true)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        @Child private ModuleNodes.InitializeNode moduleInitializeNode;
        @Child private CallDispatchHeadNode inheritedNode;

        void triggerInheritedHook(VirtualFrame frame, DynamicObject subClass, DynamicObject superClass) {
            if (inheritedNode == null) {
                CompilerDirectives.transferToInterpreter();
                inheritedNode = insert(DispatchHeadNodeFactory.createMethodCallOnSelf(getContext()));
            }
            inheritedNode.call(frame, superClass, "inherited", null, subClass);
        }

        void moduleInitialize(VirtualFrame frame, DynamicObject rubyClass, DynamicObject block) {
            if (moduleInitializeNode == null) {
                CompilerDirectives.transferToInterpreter();
                moduleInitializeNode = insert(ModuleNodesFactory.InitializeNodeFactory.create(null));
            }
            moduleInitializeNode.executeInitialize(frame, rubyClass, block);
        }

        @Specialization
        public DynamicObject initialize(VirtualFrame frame, DynamicObject rubyClass, NotProvided superclass, NotProvided block) {
            return initializeGeneralWithoutBlock(frame, rubyClass, coreLibrary().getObjectClass());
        }

        @Specialization(guards = "isRubyClass(superclass)")
        public DynamicObject initialize(VirtualFrame frame, DynamicObject rubyClass, DynamicObject superclass, NotProvided block) {
            return initializeGeneralWithoutBlock(frame, rubyClass, superclass);
        }

        @Specialization
        public DynamicObject initialize(VirtualFrame frame, DynamicObject rubyClass, NotProvided superclass, DynamicObject block) {
            return initializeGeneralWithBlock(frame, rubyClass, coreLibrary().getObjectClass(), block);
        }

        @Specialization(guards = "isRubyClass(superclass)")
        public DynamicObject initialize(VirtualFrame frame, DynamicObject rubyClass, DynamicObject superclass, DynamicObject block) {
            return initializeGeneralWithBlock(frame, rubyClass, superclass, block);
        }

        private DynamicObject initializeGeneralWithoutBlock(VirtualFrame frame, DynamicObject rubyClass, DynamicObject superclass) {
            assert RubyGuards.isRubyClass(rubyClass);
            assert RubyGuards.isRubyClass(superclass);

            ClassNodes.initialize(getContext(), rubyClass, superclass);
            triggerInheritedHook(frame, rubyClass, superclass);

            return rubyClass;
        }

        private DynamicObject initializeGeneralWithBlock(VirtualFrame frame, DynamicObject rubyClass, DynamicObject superclass, DynamicObject block) {
            assert RubyGuards.isRubyClass(superclass);

            ClassNodes.initialize(getContext(), rubyClass, superclass);
            triggerInheritedHook(frame, rubyClass, superclass);
            moduleInitialize(frame, rubyClass, block);

            return rubyClass;
        }

    }

    @CoreMethod(names = "inherited", required = 1, visibility = Visibility.PRIVATE)
    public abstract static class InheritedNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject inherited(Object subclass) {
            return nil();
        }

    }

    @CoreMethod(names = "superclass")
    public abstract static class SuperClassNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = { "rubyClass == cachedRubyCLass", "cachedSuperclass != null" }, limit = "getCacheLimit()")
        public Object getSuperClass(DynamicObject rubyClass,
                                    @Cached("rubyClass") DynamicObject cachedRubyCLass,
                                    @Cached("fastLookUp(cachedRubyCLass)") DynamicObject cachedSuperclass) {
            // caches only initialized classes, just allocated will go through slow look up
            return cachedSuperclass;
        }

        @Specialization(contains = "getSuperClass")
        DynamicObject getSuperClassUncached(DynamicObject rubyClass,
                @Cached("create()") BranchProfile errorProfile) {
            final DynamicObject superclass = fastLookUp(rubyClass);
            if (superclass != null) {
                return superclass;
            } else {
                errorProfile.enter();
                throw new RaiseException(getContext().getCoreExceptions().typeError("uninitialized class", this));
            }
        }

        protected DynamicObject fastLookUp(DynamicObject rubyClass) {
            return Layouts.CLASS.getSuperclass(rubyClass);
        }

        protected int getCacheLimit() {
            return getContext().getOptions().CLASS_CACHE;
        }
    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateConstructorNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return createRubyClass(getContext(), coreLibrary().getClassClass(), null, coreLibrary().getObjectClass(), null, false, null, false);
        }

    }
}
