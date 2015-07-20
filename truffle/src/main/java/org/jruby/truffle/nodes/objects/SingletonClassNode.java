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
import org.jruby.truffle.nodes.core.ClassNodes;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyModule;

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

    public abstract RubyBasicObject executeSingletonClass(VirtualFrame frame, Object value);

    @Specialization(guards = "value")
    protected RubyBasicObject singletonClassTrue(boolean value) {
        return getContext().getCoreLibrary().getTrueClass();
    }

    @Specialization(guards = "!value")
    protected RubyBasicObject singletonClassFalse(boolean value) {
        return getContext().getCoreLibrary().getFalseClass();
    }

    @Specialization(guards = "isNil(value)")
    protected RubyBasicObject singletonClassNil(RubyBasicObject value) {
        return getContext().getCoreLibrary().getNilClass();
    }

    @Specialization
    protected RubyBasicObject singletonClass(int value) {
        return noSingletonClass();
    }

    @Specialization
    protected RubyBasicObject singletonClass(long value) {
        return noSingletonClass();
    }

    @Specialization
    protected RubyBasicObject singletonClass(double value) {
        return noSingletonClass();
    }

    @Specialization(guards = "isRubyBignum(value)")
    protected RubyBasicObject singletonClassBignum(RubyBasicObject value) {
        return noSingletonClass();
    }

    @Specialization(guards = "isRubySymbol(value)")
    protected RubyBasicObject singletonClassSymbol(RubyBasicObject value) {
        return noSingletonClass();
    }

    @Specialization
    protected RubyBasicObject singletonClass(RubyClass rubyClass) {
        CompilerAsserts.neverPartOfCompilation();

        return ModuleNodes.getModel(rubyClass).getSingletonClass();
    }

    @Specialization(guards = { "!isNil(object)", "!isRubyBignum(object)", "!isRubySymbol(object)", "!isRubyClass(object)" })
    protected RubyBasicObject singletonClass(RubyBasicObject object) {
        return getNormalObjectSingletonClass(object);
    }

    public RubyBasicObject getNormalObjectSingletonClass(RubyBasicObject object) {
        CompilerAsserts.neverPartOfCompilation();

        if (object instanceof RubyClass) { // For the direct caller
            return ModuleNodes.getModel(((RubyClass) object)).getSingletonClass();
        }

        if (ModuleNodes.getModel(object.getMetaClass()).isSingleton()) {
            return object.getMetaClass();
        }

        CompilerDirectives.transferToInterpreter();
        final RubyBasicObject logicalClass = object.getLogicalClass();

        RubyModule attached = null;
        if (object instanceof RubyModule) {
            attached = (RubyModule) object;
        }

        final String name = String.format("#<Class:#<%s:0x%x>>", ModuleNodes.getModel(logicalClass).getName(), object.verySlowGetObjectID());
        final RubyBasicObject singletonClass = ClassNodes.createSingletonClassOfObject(getContext(), logicalClass, attached, name);
        propagateFrozen(object, singletonClass);

        object.setMetaClass(singletonClass);

        return singletonClass;
    }

    private void propagateFrozen(Object object, RubyBasicObject singletonClass) {
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

    private RubyBasicObject noSingletonClass() {
        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(getContext().getCoreLibrary().typeErrorCantDefineSingleton(this));
    }

}
