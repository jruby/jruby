/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.array;

import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;

public abstract class ArrayHelpers {

    public static Object getStore(DynamicObject array) {
        return Layouts.ARRAY.getStore(array);
    }

    public static int getSize(DynamicObject array) {
        return Layouts.ARRAY.getSize(array);
    }

    public static void setStoreAndSize(DynamicObject array, Object store, int size) {
        assert !(store instanceof ArrayMirror);
        Layouts.ARRAY.setStore(array, store);
        Layouts.ARRAY.setSize(array, size);
    }

    public static DynamicObject createArray(RubyContext context, Object store, int size) {
        return Layouts.ARRAY.createArray(context.getCoreLibrary().getArrayFactory(), store, size);
    }

}
