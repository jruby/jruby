/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.klass.ClassNodes;
import org.jruby.truffle.core.string.StringUtils;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.objects.shared.SharedObjects;

@NodeChild(value = "value", type = RubyNode.class)
public abstract class SingletonClassNode extends RubyNode {

    @Child private IsFrozenNode isFrozenNode;
    @Child private FreezeNode freezeNode;

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

    @Specialization(
            guards = {
                    "isRubyClass(rubyClass)",
                    "rubyClass.getShape() == cachedShape",
                    "cachedSingletonClass != null"
            },
            limit = "getCacheLimit()"
    )
    protected DynamicObject singletonClassClassCached(
            DynamicObject rubyClass,
            @Cached("rubyClass.getShape()") Shape cachedShape,
            @Cached("getSingletonClassOrNull(rubyClass)") DynamicObject cachedSingletonClass) {

        return cachedSingletonClass;
    }

    @Specialization(
            guards = "isRubyClass(rubyClass)",
            contains = "singletonClassClassCached"
    )
    protected DynamicObject singletonClassClassUncached(DynamicObject rubyClass) {
        return ClassNodes.getSingletonClass(getContext(), rubyClass);
    }

    @Specialization(
            guards = {
                    "object == cachedObject",
                    "!isNil(cachedObject)",
                    "!isRubyBignum(cachedObject)",
                    "!isRubySymbol(cachedObject)",
                    "!isRubyClass(cachedObject)"
            },
            limit = "getCacheLimit()")
    protected DynamicObject singletonClassInstanceCached(
            DynamicObject object,
            @Cached("object") DynamicObject cachedObject,
            @Cached("getSingletonClassForInstance(object)") DynamicObject cachedSingletonClass) {
        return cachedSingletonClass;
    }

    @Specialization(
            guards = {
                "!isNil(object)",
                "!isRubyBignum(object)",
                "!isRubySymbol(object)",
                "!isRubyClass(object)"
            },
            contains = "singletonClassInstanceCached"
    )
    protected DynamicObject singletonClassInstanceUncached(DynamicObject object) {
        return getSingletonClassForInstance(object);
    }

    private DynamicObject noSingletonClass() {
        throw new RaiseException(coreExceptions().typeErrorCantDefineSingleton(this));
    }

    protected DynamicObject getSingletonClassOrNull(DynamicObject object) {
        return ClassNodes.getSingletonClassOrNull(getContext(), object);
    }

    @TruffleBoundary
    protected DynamicObject getSingletonClassForInstance(DynamicObject object) {
        synchronized (object) {
            DynamicObject metaClass = Layouts.BASIC_OBJECT.getMetaClass(object);
            if (Layouts.CLASS.getIsSingleton(metaClass)) {
                return metaClass;
            }

            final DynamicObject logicalClass = Layouts.BASIC_OBJECT.getLogicalClass(object);

            final String name = StringUtils.format("#<Class:#<%s:0x%x>>", Layouts.MODULE.getFields(logicalClass).getName(),
                    ObjectIDOperations.verySlowGetObjectID(getContext(), object));

            final DynamicObject singletonClass = ClassNodes.createSingletonClassOfObject(
                    getContext(), getEncapsulatingSourceSection(), logicalClass, object, name);

            if (isFrozen(object)) {
                freeze(singletonClass);
            }

            SharedObjects.propagate(object, singletonClass);

            Layouts.BASIC_OBJECT.setMetaClass(object, singletonClass);
            return singletonClass;
        }
    }

    public void freeze(final DynamicObject singletonClass) {
        if (freezeNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            freezeNode = insert(FreezeNodeGen.create(null));
        }
        freezeNode.executeFreeze(singletonClass);
    }

    protected boolean isFrozen(Object object) {
        if (isFrozenNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isFrozenNode = insert(IsFrozenNodeGen.create(null));
        }
        return isFrozenNode.executeIsFrozen(object);
    }

    protected int getCacheLimit() {
        return getContext().getOptions().CLASS_CACHE;
    }

}
