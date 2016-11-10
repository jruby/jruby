/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.Shape;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.objects.shared.SharedObjects;

public abstract class ShapeCachingGuards {

    public static boolean updateShape(DynamicObject object) {
        CompilerDirectives.transferToInterpreter();
        boolean updated = object.updateShape();
        if (updated) {
            assert !SharedObjects.isShared(object);
        }
        return updated;
    }

    public static boolean isArrayShape(Shape shape) {
        return Layouts.ARRAY.isArray(shape.getObjectType());
    }

    public static boolean isQueueShape(Shape shape) {
        return Layouts.QUEUE.isQueue(shape.getObjectType());
    }

    private static final ObjectType BASIC_OBJECT_OBJECT_TYPE =
            Layouts.BASIC_OBJECT.createBasicObjectShape(null, null).getShape().getObjectType();

    public static boolean isBasicObjectShape(Shape shape) {
        return shape.getObjectType().getClass() == BASIC_OBJECT_OBJECT_TYPE.getClass();
    }

}
