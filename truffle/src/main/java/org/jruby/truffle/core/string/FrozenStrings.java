/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.string;

import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.rope.Rope;

import java.util.Map;
import java.util.WeakHashMap;

public class FrozenStrings {

    private final RubyContext context;

    private final Map<Rope, DynamicObject> frozenStrings = new WeakHashMap<>();

    public FrozenStrings(RubyContext context) {
        this.context = context;
    }

    public synchronized DynamicObject getFrozenString(Rope rope) {
        assert context.getRopeTable().contains(rope);

        DynamicObject string = frozenStrings.get(rope);

        if (string == null) {
            string = StringOperations.createString(context, rope);
            string.define(Layouts.FROZEN_IDENTIFIER, true);
            frozenStrings.put(rope, string);
        }

        return string;
    }

}
