/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.core.ClassNodes;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.runtime.layouts.Layouts;

/**
 * Reads the singleton (meta, eigen) class of an object.
 */
@NodeChild(value = "value", type = RubyNode.class)
public abstract class SingletonClassNode extends RubyNode {

    @Child IsFrozenNode isFrozenNode;
    @Child FreezeNode freezeNode;

    public SingletonClassNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract DynamicObject executeSingletonClass(Object value);

    @Specialization(guards = "value")
    protected DynamicObject singletonClassTrue(boolean value) {
        return getContext().getCoreLibrary().getTrueClass();
    }

    @Specialization(guards = "!value")
    protected DynamicObject singletonClassFalse(boolean value) {
        return getContext().getCoreLibrary().getFalseClass();
    }

    @Specialization(guards = "isNil(value)")
    protected DynamicObject singletonClassNil(DynamicObject value) {
        return getContext().getCoreLibrary().getNilClass();
    }

    @Specialization
    protected DynamicObject singletonClass(int value) {
        return noSingletonClass();
    }

    @Specialization
    protected DynamicObject singletonClass(long value) {
        return noSingletonClass();
    }

    @Specialization
    protected DynamicObject singletonClass(double value) {
        return noSingletonClass();
    }

    @Specialization(guards = "isRubyBignum(value)")
    protected DynamicObject singletonClassBignum(DynamicObject value) {
        return noSingletonClass();
    }

    @Specialization(guards = "isRubySymbol(value)")
    protected DynamicObject singletonClassSymbol(DynamicObject value) {
        return noSingletonClass();
    }

    @Specialization(guards = "isRubyClass(rubyClass)")
    protected DynamicObject singletonClassClass(DynamicObject rubyClass) {
        return ClassNodes.getSingletonClass(getContext(), rubyClass);
    }

    @Specialization(guards = { "!isNil(object)", "!isRubyBignum(object)", "!isRubySymbol(object)", "!isRubyClass(object)" })
    protected DynamicObject singletonClass(DynamicObject object) {
        return getNormalObjectSingletonClass(object);
    }

    public DynamicObject getNormalObjectSingletonClass(DynamicObject object) {
        CompilerAsserts.neverPartOfCompilation();

        if (RubyGuards.isRubyClass(object)) { // For the direct caller
            return ClassNodes.getSingletonClass(getContext(), object);
        }

        if (Layouts.CLASS.getIsSingleton(Layouts.BASIC_OBJECT.getMetaClass(object))) {
            return Layouts.BASIC_OBJECT.getMetaClass(object);
        }

        CompilerDirectives.transferToInterpreter();
        final DynamicObject logicalClass = Layouts.BASIC_OBJECT.getLogicalClass(object);

        DynamicObject attached = null;
        if (RubyGuards.isRubyModule(object)) {
            attached = object;
        }

        final String name = String.format("#<Class:#<%s:0x%x>>", Layouts.MODULE.getFields(logicalClass).getName(), ObjectIDOperations.verySlowGetObjectID(getContext(), object));
        final DynamicObject singletonClass = ClassNodes.createSingletonClassOfObject(getContext(), logicalClass, attached, name);
        propagateFrozen(object, singletonClass);

        Layouts.BASIC_OBJECT.setMetaClass(object, singletonClass);

        return singletonClass;
    }

    private void propagateFrozen(Object object, DynamicObject singletonClass) {
        assert RubyGuards.isRubyClass(singletonClass);

        if (isFrozenNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isFrozenNode = insert(IsFrozenNodeGen.create(getContext(), getSourceSection(), null));
            freezeNode = insert(FreezeNodeGen.create(getContext(), getSourceSection(), null));
        }

        if (isFrozenNode.executeIsFrozen(object)) {
            freezeNode.executeFreeze(singletonClass);
        }
    }

    private DynamicObject noSingletonClass() {
        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(getContext().getCoreLibrary().typeErrorCantDefineSingleton(this));
    }

}
