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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyNode;

@ImportStatic(ShapeCachingGuards.class)
@NodeChild(value="object", type=RubyNode.class)
public abstract class LogicalClassNode extends RubyNode {

    public LogicalClassNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract DynamicObject executeLogicalClass(Object value);

    @Specialization(guards = "value")
    protected DynamicObject logicalClassTrue(boolean value) {
        return coreLibrary().getTrueClass();
    }

    @Specialization(guards = "!value")
    protected DynamicObject logicalClassFalse(boolean value) {
        return coreLibrary().getFalseClass();
    }

    @Specialization
    protected DynamicObject logicalClassInt(int value) {
        return coreLibrary().getFixnumClass();
    }

    @Specialization
    protected DynamicObject logicalClassLong(long value) {
        return coreLibrary().getFixnumClass();
    }

    @Specialization
    protected DynamicObject logicalClassDouble(double value) {
        return coreLibrary().getFloatClass();
    }

    @Specialization(guards = "object.getShape() == cachedShape",
            assumptions = "cachedShape.getValidAssumption()",
            limit = "getCacheLimit()")
    protected DynamicObject logicalClassCached(DynamicObject object,
                                            @Cached("object.getShape()") Shape cachedShape,
                                            @Cached("getLogicalClass(cachedShape)") DynamicObject logicalClass) {
        return logicalClass;
    }

    @Specialization(guards = "updateShape(object)")
    protected DynamicObject updateShapeAndLogicalClass(DynamicObject object) {
        return executeLogicalClass(object);
    }

    @Specialization(contains = { "logicalClassCached", "updateShapeAndLogicalClass" })
    protected DynamicObject logicalClassUncached(DynamicObject object) {
        return Layouts.BASIC_OBJECT.getLogicalClass(object);
    }

    protected static DynamicObject getLogicalClass(Shape shape) {
        return Layouts.BASIC_OBJECT.getLogicalClass(shape.getObjectType());
    }

    protected int getCacheLimit() {
        return getContext().getOptions().CLASS_CACHE;
    }

}
