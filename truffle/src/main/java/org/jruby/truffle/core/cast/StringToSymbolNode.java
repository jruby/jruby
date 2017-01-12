/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.cast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyNode;

/**
 * Creates a symbol from a string.
 */
@NodeChild("string")
public abstract class StringToSymbolNode extends RubyNode {

    @Specialization(guards = "isRubyString(string)")
    public DynamicObject doString(DynamicObject string) {
        return getSymbol(Layouts.STRING.getRope(string));
    }

}
