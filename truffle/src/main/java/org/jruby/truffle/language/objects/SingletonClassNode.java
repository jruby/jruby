/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.klass.ClassNodes;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

@NodeChild(value = "value", type = RubyNode.class)
public abstract class SingletonClassNode extends RubyNode {

    @Child private IsFrozenNode isFrozenNode;
    @Child private FreezeNode freezeNode;

    public SingletonClassNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract DynamicObject executeSingletonClass(Object value);

    @Specialization(guards = "value")
    protected DynamicObject singletonClassTrue(boolean value) {
        return coreLibrary().getTrueClass();
    }

    @Specialization(guards = "!value")
    protected DynamicObject singletonClassFalse(boolean value) {
        return coreLibrary().getFalseClass();
    }

    @Specialization(guards = "isNil(value)")
    protected DynamicObject singletonClassNil(DynamicObject value) {
        return coreLibrary().getNilClass();
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

    @Specialization(guards = {
            "!isNil(object)",
            "!isRubyBignum(object)",
            "!isRubySymbol(object)",
            "!isRubyClass(object)"
    })
    protected DynamicObject singletonClass(
            DynamicObject object,
            @Cached("create()") BranchProfile needsToFreeze) {
        if (RubyGuards.isRubyClass(object)) {
            return ClassNodes.getSingletonClass(getContext(), object);
        }

        if (Layouts.CLASS.getIsSingleton(Layouts.BASIC_OBJECT.getMetaClass(object))) {
            return Layouts.BASIC_OBJECT.getMetaClass(object);
        }

        final DynamicObject logicalClass = Layouts.BASIC_OBJECT.getLogicalClass(object);

        DynamicObject attached = null;

        if (RubyGuards.isRubyModule(object)) {
            attached = object;
        }

        final String name = String.format("#<Class:#<%s:0x%x>>", Layouts.MODULE.getFields(logicalClass).getName(), ObjectIDOperations.verySlowGetObjectID(getContext(), object));
        final DynamicObject singletonClass = ClassNodes.createSingletonClassOfObject(getContext(), logicalClass, attached, name);

        if (isFrozenNode == null) {
            CompilerDirectives.transferToInterpreter();
            isFrozenNode = insert(IsFrozenNodeGen.create(getContext(), getSourceSection(), null));
        }

        if (isFrozenNode.executeIsFrozen(object)) {
            needsToFreeze.enter();

            if (freezeNode == null) {
                CompilerDirectives.transferToInterpreter();
                freezeNode = insert(FreezeNodeGen.create(getContext(), getSourceSection(), null));
            }

            freezeNode.executeFreeze(singletonClass);
        }

        Layouts.BASIC_OBJECT.setMetaClass(object, singletonClass);

        return singletonClass;
    }

    private DynamicObject noSingletonClass() {
        throw new RaiseException(coreLibrary().typeErrorCantDefineSingleton(this));
    }

}
