/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.Shape;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.CheckLayoutNodeFactory.GetObjectTypeNodeGen;
import org.jruby.truffle.language.objects.ShapeCachingGuards;

public class CheckLayoutNode extends RubyBaseNode {

    @Child private GetObjectTypeNode getObjectTypeNode = GetObjectTypeNodeGen.create(null);

    public boolean isArray(DynamicObject object) {
        return Layouts.ARRAY.isArray(getObjectTypeNode.executeGetObjectType(object));
    }

    public boolean isModule(DynamicObject object) {
        return Layouts.MODULE.isModule(getObjectTypeNode.executeGetObjectType(object));
    }

    public boolean isString(DynamicObject object) {
        return Layouts.STRING.isString(getObjectTypeNode.executeGetObjectType(object));
    }

    @NodeChild("object")
    @ImportStatic(ShapeCachingGuards.class)
    public static abstract class GetObjectTypeNode extends RubyNode {

        public abstract ObjectType executeGetObjectType(DynamicObject object);

        @Specialization(
                guards = { "object == cachedObject", "cachedShape.isLeaf()" },
                assumptions = "cachedShape.getLeafAssumption()",
                limit = "getLimit()")
        ObjectType cachedLeafShapeGetObjectType(DynamicObject object,
                @Cached("object") DynamicObject cachedObject,
                @Cached("cachedObject.getShape()") Shape cachedShape) {
            return cachedShape.getObjectType();
        }

        @Specialization(
                guards = "object.getShape() == cachedShape",
                limit = "getLimit()")
        ObjectType cachedShapeGetObjectType(DynamicObject object,
                @Cached("object.getShape()") Shape cachedShape) {
            return cachedShape.getObjectType();
        }

        @Specialization(guards = "updateShape(object)")
        ObjectType updateShapeAndRetry(DynamicObject object) {
            return executeGetObjectType(object);
        }

        @Specialization(contains = { "cachedLeafShapeGetObjectType", "cachedShapeGetObjectType", "updateShapeAndRetry" })
        ObjectType uncachedGetObjectType(DynamicObject object) {
            return object.getShape().getObjectType();
        }

        protected int getLimit() {
            return getContext().getOptions().IS_A_CACHE;
        }

    }

}
