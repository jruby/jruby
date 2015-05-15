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

import org.jruby.truffle.nodes.RubyNode;
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

    public abstract RubyClass executeSingletonClass(VirtualFrame frame, Object value);

    @Specialization(guards = "isTrue(value)")
    protected RubyClass singletonClassTrue(boolean value) {
        return getContext().getCoreLibrary().getTrueClass();
    }

    @Specialization(guards = "!isTrue(value)")
    protected RubyClass singletonClassFalse(boolean value) {
        return getContext().getCoreLibrary().getFalseClass();
    }

    @Specialization(guards = "isNil(value)")
    protected RubyClass singletonClassNil(RubyBasicObject value) {
        return getContext().getCoreLibrary().getNilClass();
    }

    @Specialization
    protected RubyClass singletonClass(int value) {
        return noSingletonClass();
    }

    @Specialization
    protected RubyClass singletonClass(long value) {
        return noSingletonClass();
    }

    @Specialization
    protected RubyClass singletonClass(double value) {
        return noSingletonClass();
    }

    @Specialization(guards = "isRubyBignum(value)")
    protected RubyClass singletonClassBignum(RubyBasicObject value) {
        return noSingletonClass();
    }

    @Specialization(guards = "isRubySymbol(value)")
    protected RubyClass singletonClassSymbol(RubyBasicObject value) {
        return noSingletonClass();
    }

    @Specialization
    protected RubyClass singletonClass(RubyClass rubyClass) {
        CompilerAsserts.neverPartOfCompilation();

        return rubyClass.getSingletonClass();
    }

    @Specialization(guards = { "!isNil(object)", "!isRubyBignum(object)", "!isRubySymbol(object)", "!isRubyClass(object)" })
    protected RubyClass singletonClass(RubyBasicObject object) {
        return getNormalObjectSingletonClass(object);
    }

    public RubyClass getNormalObjectSingletonClass(RubyBasicObject object) {
        CompilerAsserts.neverPartOfCompilation();

        if (object instanceof RubyClass) { // For the direct caller
            return ((RubyClass) object).getSingletonClass();
        }

        if (object.getMetaClass().isSingleton()) {
            return object.getMetaClass();
        }

        CompilerDirectives.transferToInterpreter();
        final RubyClass logicalClass = object.getLogicalClass();

        RubyModule attached = null;
        if (object instanceof RubyModule) {
            attached = (RubyModule) object;
        }

        String name = String.format("#<Class:#<%s:0x%x>>", logicalClass.getName(), object.verySlowGetObjectID());
        RubyClass singletonClass = RubyClass.createSingletonClassOfObject(getContext(), logicalClass, attached, name);
        propagateFrozen(object, singletonClass);

        object.setMetaClass(singletonClass);

        return singletonClass;
    }

    private void propagateFrozen(Object object, RubyClass singletonClass) {
        if (isFrozenNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isFrozenNode = insert(IsFrozenNodeGen.create(getContext(), getSourceSection(), null));
            freezeNode = insert(FreezeNodeGen.create(getContext(), getSourceSection(), null));
        }

        if (isFrozenNode.executeIsFrozen(object)) {
            freezeNode.executeFreeze(singletonClass);
        }
    }

    private RubyClass noSingletonClass() {
        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(getContext().getCoreLibrary().typeErrorCantDefineSingleton(this));
    }

}
