/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objects;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.BasicObjectNodes;
import org.jruby.truffle.nodes.core.ClassNodes;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import com.oracle.truffle.api.object.DynamicObject;

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

    public abstract DynamicObject executeSingletonClass(VirtualFrame frame, Object value);

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
        return ClassNodes.getSingletonClass(rubyClass);
    }

    @Specialization(guards = { "!isNil(object)", "!isRubyBignum(object)", "!isRubySymbol(object)", "!isRubyClass(object)" })
    protected DynamicObject singletonClass(DynamicObject object) {
        return getNormalObjectSingletonClass(object);
    }

    public DynamicObject getNormalObjectSingletonClass(DynamicObject object) {
        CompilerAsserts.neverPartOfCompilation();

        if (RubyGuards.isRubyClass(object)) { // For the direct caller
            return ClassNodes.getSingletonClass(object);
        }

        if (ClassNodes.isSingleton(BasicObjectNodes.getMetaClass(object))) {
            return BasicObjectNodes.getMetaClass(object);
        }

        CompilerDirectives.transferToInterpreter();
        final DynamicObject logicalClass = BasicObjectNodes.getLogicalClass(object);

        DynamicObject attached = null;
        if (RubyGuards.isRubyModule(object)) {
            attached = object;
        }

        final String name = String.format("#<Class:#<%s:0x%x>>", ModuleNodes.getFields(logicalClass).getName(), BasicObjectNodes.verySlowGetObjectID(object));
        final DynamicObject singletonClass = ClassNodes.createSingletonClassOfObject(getContext(), logicalClass, attached, name);
        propagateFrozen(object, singletonClass);

        BasicObjectNodes.setMetaClass(object, singletonClass);

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
