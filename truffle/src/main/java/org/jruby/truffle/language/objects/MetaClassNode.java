/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyNode;

@ImportStatic(ShapeCachingGuards.class)
@NodeChild(value="object", type=RubyNode.class)
public abstract class MetaClassNode extends RubyNode {

    public static MetaClassNode create() {
        return MetaClassNodeGen.create(null);
    }

    public abstract DynamicObject executeMetaClass(Object value);

    @Specialization(guards = "value")
    protected DynamicObject metaClassTrue(boolean value) {
        return coreLibrary().getTrueClass();
    }

    @Specialization(guards = "!value")
    protected DynamicObject metaClassFalse(boolean value) {
        return coreLibrary().getFalseClass();
    }

    @Specialization
    protected DynamicObject metaClassInt(int value) {
        return coreLibrary().getFixnumClass();
    }

    @Specialization
    protected DynamicObject metaClassLong(long value) {
        return coreLibrary().getFixnumClass();
    }

    @Specialization
    protected DynamicObject metaClassDouble(double value) {
        return coreLibrary().getFloatClass();
    }

    @Specialization(guards = "object.getShape() == cachedShape",
            assumptions = "cachedShape.getValidAssumption()",
            limit = "getCacheLimit()")
    protected DynamicObject metaClassCached(DynamicObject object,
            @Cached("object.getShape()") Shape cachedShape,
            @Cached("getMetaClass(cachedShape)") DynamicObject metaClass) {
        return metaClass;
    }

    @Specialization(guards = "updateShape(object)")
    protected DynamicObject updateShapeAndMetaClass(DynamicObject object) {
        return executeMetaClass(object);
    }

    @Specialization(contains = { "metaClassCached", "updateShapeAndMetaClass" })
    protected DynamicObject metaClassUncached(DynamicObject object) {
        return Layouts.BASIC_OBJECT.getMetaClass(object);
    }

    @Fallback
    protected DynamicObject metaClassFallback(Object object) {
        return getContext().getCoreLibrary().getMetaClass(object);
    }

    protected static DynamicObject getMetaClass(Shape shape) {
        return Layouts.BASIC_OBJECT.getMetaClass(shape.getObjectType());
    }

    protected int getCacheLimit() {
        return getContext().getOptions().CLASS_CACHE;
    }

}
