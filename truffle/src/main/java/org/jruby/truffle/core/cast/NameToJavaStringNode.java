/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.core.cast;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.string.StringCachingGuards;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;

/**
 * Take a Symbol or some object accepting #to_str
 * and convert it to a Java String.
 */
@ImportStatic(StringCachingGuards.class)
@NodeChild(value = "value", type = RubyNode.class)
public abstract class NameToJavaStringNode extends RubyNode {

    public static NameToJavaStringNode create() {
        return NameToJavaStringNodeGen.create(null);
    }

    public abstract String executeToJavaString(VirtualFrame frame, Object name);

    @Specialization(guards = { "isRubyString(value)", "ropesEqual(value, cachedRope)" }, limit = "getLimit()")
    String stringCached(DynamicObject value,
            @Cached("privatizeRope(value)") Rope cachedRope,
            @Cached("value.toString()") String convertedString) {
        return convertedString;
    }

    @Specialization(guards = "isRubyString(value)", contains = "stringCached")
    public String stringUncached(DynamicObject value) {
        return StringOperations.getString(value);
    }

    @Specialization(guards = { "symbol == cachedSymbol", "isRubySymbol(cachedSymbol)" }, limit = "getLimit()")
    public String symbolCached(DynamicObject symbol,
            @Cached("symbol") DynamicObject cachedSymbol,
            @Cached("symbolToString(symbol)") String convertedString) {
        return convertedString;
    }

    @Specialization(guards = "isRubySymbol(symbol)", contains = "symbolCached")
    public String symbolUncached(DynamicObject symbol) {
        return symbolToString(symbol);
    }

    @Specialization(guards = "string == cachedString", limit = "getLimit()")
    public String javaStringCached(String string,
            @Cached("string") String cachedString) {
        return cachedString;
    }

    @Specialization(contains = "javaStringCached")
    public String javaStringUncached(String value) {
        return value;
    }

    @Specialization(guards = { "!isString(object)", "!isRubySymbol(object)", "!isRubyString(object)" })
    public String coerceObjectToStr(VirtualFrame frame, Object object,
            @Cached("create()") BranchProfile errorProfile,
            @Cached("createMethodCall()") CallDispatchHeadNode toStr) {
        final Object coerced;
        try {
            coerced = toStr.call(frame, object, "to_str");
        } catch (RaiseException e) {
            errorProfile.enter();
            if (Layouts.BASIC_OBJECT.getLogicalClass(e.getException()) == coreLibrary().getNoMethodErrorClass()) {
                throw new RaiseException(coreExceptions().typeErrorNoImplicitConversion(object, "String", this));
            } else {
                throw e;
            }
        }

        if (RubyGuards.isRubyString(coerced)) {
            return StringOperations.getString((DynamicObject) coerced);
        } else {
            errorProfile.enter();
            throw new RaiseException(coreExceptions().typeErrorBadCoercion(object, "String", "to_str", coerced, this));
        }
    }

    protected String symbolToString(DynamicObject symbol) {
        return Layouts.SYMBOL.getString(symbol);
    }

    protected int getLimit() {
        return getContext().getOptions().INTEROP_CONVERT_CACHE;
    }
}
