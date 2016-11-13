package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.object.Property;

public abstract class PropertyFlags {

    public static final int REMOVED = 1;

    public static boolean isDefined(Property property) {
        return property != null && !isRemoved(property);
    }

    public static boolean isRemoved(Property property) {
        return (property.getFlags() & REMOVED) != 0;
    }

    public static Property asRemoved(Property property) {
        return property.copyWithFlags(property.getFlags() | REMOVED);
    }

}
