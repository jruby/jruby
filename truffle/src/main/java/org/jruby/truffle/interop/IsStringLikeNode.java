/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.language.RubyNode;

@NodeChild(value = "value", type = RubyNode.class)
public abstract class IsStringLikeNode extends RubyNode {

    public static IsStringLikeNode create() {
        return IsStringLikeNodeGen.create(null);
    }

    public abstract boolean executeIsStringLike(Object value);

    @Specialization(guards = "isRubyString(value)")
    boolean isRubyString(DynamicObject value) {
        return true;
    }

    @Specialization(guards = "isRubySymbol(value)")
    public boolean isRubySymbol(DynamicObject value) {
        return true;
    }

    @Specialization
    public boolean isJavaString(String value) {
        return true;
    }

    @Specialization(guards = { "!isRubyString(value)", "!isRubySymbol(value)", "!isString(value)" })
    public boolean notStringLike(Object value) {
        return false;
    }

}
