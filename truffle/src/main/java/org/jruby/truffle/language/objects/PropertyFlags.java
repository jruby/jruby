/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
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
