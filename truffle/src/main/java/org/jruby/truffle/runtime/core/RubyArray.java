/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.nodes.core.array.ArrayNodes;

@Deprecated
public final class RubyArray extends RubyBasicObject {

    public Object store;
    public int size;

    public RubyArray(RubyBasicObject arrayClass, Object store, int size, DynamicObject dynamicObject) {
        super(arrayClass, dynamicObject);
        ArrayNodes.setStore(this, store, size);
    }

}
