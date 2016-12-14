/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyNode;

/**
 * Only converts primitive types (including java.lang.String).
 */
@NodeChild(value = "value", type = RubyNode.class)
public abstract class ForeignToRubyNode extends RubyNode {

    public static ForeignToRubyNode create() {
        return ForeignToRubyNodeGen.create(null);
    }

    public abstract Object executeConvert(VirtualFrame frame, Object value);

    @Specialization(guards = "stringsEquals(cachedValue, value)", limit = "getLimit()")
    public DynamicObject convertStringCached(
            String value,
            @Cached("value") String cachedValue,
            @Cached("getRope(value)") Rope cachedRope) {
        return createString(cachedRope);
    }

    @Specialization(contains = "convertStringCached")
    public DynamicObject convertStringUncached(String value) {
        return createString(getRope(value));
    }

    protected boolean stringsEquals(String a, String b) {
        return a.equals(b);
    }

    protected Rope getRope(String value) {
        return StringOperations.encodeRope(value, UTF8Encoding.INSTANCE);
    }

    protected int getLimit() {
        return getContext().getOptions().INTEROP_CONVERT_CACHE;
    }

    @Specialization(guards = "!isString(value)")
    public Object convert(Object value) {
        return value;
    }

}
