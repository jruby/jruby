package org.jruby.truffle.core.array;

import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;

import com.oracle.truffle.api.object.DynamicObject;

public abstract class ArrayHelpers {

    public static Object getStore(DynamicObject array) {
        return Layouts.ARRAY.getStore(array);
    }

    public static int getSize(DynamicObject array) {
        return Layouts.ARRAY.getSize(array);
    }

    public static void setStoreAndSize(DynamicObject array, Object store, int size) {
        Layouts.ARRAY.setStore(array, store);
        Layouts.ARRAY.setSize(array, size);
    }

    public static DynamicObject createArray(RubyContext context, Object store, int size) {
        return Layouts.ARRAY.createArray(context.getCoreLibrary().getArrayFactory(), store, size);
    }

}
