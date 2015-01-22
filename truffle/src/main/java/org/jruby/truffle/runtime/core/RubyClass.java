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
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Represents the Ruby {@code Class} class. Note that most of the functionality you might associate
 * with {@code Class} is actually in {@code Module}, implemented by {@link RubyModule}.
 */
public class RubyClass extends RubyModule {

    // TODO(CS): is this compilation final needed? Is it a problem for correctness?
    @CompilationFinal Allocator allocator = new RubyBasicObject.BasicObjectAllocator();

    private boolean isSingleton;
    private final Set<RubyClass> subClasses = Collections.newSetFromMap(new WeakHashMap<RubyClass, Boolean>());

    /**
     * This constructor supports initialization and solves boot-order problems and should not
     * normally be used from outside this class.
     */
    public static RubyClass createBootClass(RubyContext context, String name) {
        return new RubyClass(context, null, null, name, false);
    }

    public RubyClass(RubyContext context, RubyModule lexicalParent, RubyClass superclass, String name) {
        this(context, lexicalParent, superclass, name, false);
        // Always create a class singleton class for normal classes for consistency.
        ensureSingletonConsistency();
    }

    protected static RubyClass createSingletonClassOfObject(RubyContext context, RubyClass superclass, String name) {
        // We also need to create the singleton class of a singleton class for proper lookup and consistency.
        // See rb_singleton_class() documentation in MRI.
        return new RubyClass(context, null, superclass, name, true).ensureSingletonConsistency();
    }

    protected RubyClass(RubyContext context, RubyModule lexicalParent, RubyClass superclass, String name, boolean isSingleton) {
        super(context, context.getCoreLibrary().getClassClass(), lexicalParent, name, null);
        this.isSingleton = isSingleton;

        if (superclass != null) {
            unsafeSetSuperclass(superclass);
        }
    }

    public void setAllocator(Allocator allocator) {
        this.allocator = allocator;
    }

    public void initialize(RubyClass superclass) {
        unsafeSetSuperclass(superclass);
        ensureSingletonConsistency();
        allocator = superclass.allocator;
    }

    @Override
    public void initCopy(RubyModule other) {
        super.initCopy(other);
        assert other instanceof RubyClass;
    }

    private RubyClass ensureSingletonConsistency() {
        createOneSingletonClass();
        return this;
    }

    @Override
    public RubyClass getSingletonClass(Node currentNode) {
        // We also need to create the singleton class of a singleton class for proper lookup and consistency.
        // See rb_singleton_class() documentation in MRI.
        return createOneSingletonClass().ensureSingletonConsistency();
    }

    private RubyClass createOneSingletonClass() {
        CompilerAsserts.neverPartOfCompilation();

        if (metaClass.isSingleton()) {
            return metaClass;
        }

        RubyClass singletonSuperclass;

        if (getSuperClass() == null) {
            singletonSuperclass = getLogicalClass();
        } else {
            singletonSuperclass = getSuperClass().createOneSingletonClass();
        }

        metaClass = new RubyClass(getContext(),
                null, singletonSuperclass, String.format("#<Class:%s>", getName()), true);

        return metaClass;
    }

    /**
     * This method supports initialization and solves boot-order problems and should not normally be
     * used.
     */
    public void unsafeSetSuperclass(RubyClass newSuperclass) {
        RubyNode.notDesignedForCompilation();

        assert parentModule == null;

        unsafeSetParent(newSuperclass);
        newSuperclass.subClasses.add(this);

        newVersion();
    }

    public RubyBasicObject allocate(RubyNode currentNode) {
        return allocator.allocate(getContext(), this, currentNode);
    }

    public boolean isSingleton() {
        return isSingleton;
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

    public static class ClassAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, RubyNode currentNode) {
            return new RubyClass(context, null, null, null, false);
        }

    }

}
