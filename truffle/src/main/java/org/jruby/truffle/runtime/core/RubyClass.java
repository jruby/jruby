/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.Node;

import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;

/**
 * Represents the Ruby {@code Class} class. Note that most of the functionality you might associate
 * with {@code Class} is actually in {@code Module}, implemented by {@link RubyModule}.
 */
public class RubyClass extends RubyModule {

    // TODO(CS): is this compilation final needed? Is it a problem for correctness?
    @CompilationFinal
    private Allocator allocator;

    private final boolean isSingleton;
    private final RubyModule attached;

    /**
     * This constructor supports initialization and solves boot-order problems and should not
     * normally be used from outside this class.
     */
    public static RubyClass createBootClass(RubyContext context, RubyClass classClass, String name, Allocator allocator) {
        return new RubyClass(context, classClass, null, null, name, false, null, allocator);
    }

    public RubyClass(RubyContext context, RubyModule lexicalParent, RubyClass superclass, String name, Allocator allocator) {
        this(context, superclass.getLogicalClass(), lexicalParent, superclass, name, false, null, allocator);
        // Always create a class singleton class for normal classes for consistency.
        ensureSingletonConsistency();
    }

    public static RubyClass createSingletonClassOfObject(RubyContext context, RubyClass superclass, RubyModule attached, String name) {
        // We also need to create the singleton class of a singleton class for proper lookup and consistency.
        // See rb_singleton_class() documentation in MRI.
        // Allocator is null here, we cannot create instances of singleton classes.
        return new RubyClass(context, superclass.getLogicalClass(), null, superclass, name, true, attached, null).ensureSingletonConsistency();
    }

    private RubyClass(RubyContext context, RubyClass classClass, RubyModule lexicalParent, RubyClass superclass, String name, boolean isSingleton, RubyModule attached, Allocator allocator) {
        super(context, classClass, lexicalParent, name, null);

        assert isSingleton || attached == null;

        this.unsafeSetAllocator(allocator);
        this.isSingleton = isSingleton;
        this.attached = attached;

        if (superclass != null) {
            unsafeSetSuperclass(superclass);
        }
    }

    public void initialize(RubyClass superclass) {
        unsafeSetSuperclass(superclass);
        ensureSingletonConsistency();
        unsafeSetAllocator(superclass.getAllocator());
    }

    /**
     * This method supports initialization and solves boot-order problems and should not normally be
     * used.
     */
    protected void unsafeSetSuperclass(RubyClass superClass) {
        assert parentModule == null;

        parentModule = superClass;
        superClass.addDependent(this);

        newVersion();
    }

    public void initCopy(RubyClass from) {
        super.initCopy(from);
        this.unsafeSetAllocator(((RubyClass) from).getAllocator());
        // isSingleton is false as we cannot copy a singleton class.
        // and therefore attached is null.
    }

    private RubyClass ensureSingletonConsistency() {
        createOneSingletonClass();
        return this;
    }

    public RubyClass getSingletonClass() {
        // We also need to create the singleton class of a singleton class for proper lookup and consistency.
        // See rb_singleton_class() documentation in MRI.
        return createOneSingletonClass().ensureSingletonConsistency();
    }

    private RubyClass createOneSingletonClass() {
        CompilerAsserts.neverPartOfCompilation();

        if (getMetaClass().isSingleton()) {
            return getMetaClass();
        }

        final RubyClass singletonSuperclass;
        if (getSuperClass() == null) {
            singletonSuperclass = getLogicalClass();
        } else {
            singletonSuperclass = getSuperClass().createOneSingletonClass();
        }

        String name = String.format("#<Class:%s>", getName());
        setMetaClass(new RubyClass(getContext(), getLogicalClass(), null, singletonSuperclass, name, true, this, null));

        return getMetaClass();
    }

    public RubyBasicObject allocate(Node currentNode) {
        return getAllocator().allocate(getContext(), this, currentNode);
    }

    public boolean isSingleton() {
        return isSingleton;
    }

    public RubyModule getAttached() {
        return attached;
    }

    public RubyClass getSuperClass() {
        CompilerAsserts.neverPartOfCompilation();

        for (RubyModule ancestor : parentAncestors()) {
            if (ancestor instanceof RubyClass) {
                return (RubyClass) ancestor;
            }
        }

        return null;
    }

    public Allocator getAllocator() {
        return allocator;
    }

    public void unsafeSetAllocator(Allocator allocator) {
        this.allocator = allocator;
    }

    public static class ClassAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyClass(context, context.getCoreLibrary().getClassClass(), null, null, null, false, null, null);
        }

    }

}
