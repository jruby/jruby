/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.core.cast.NameToJavaStringNode;
import org.jruby.truffle.core.string.StringCachingGuards;
import org.jruby.truffle.language.RubyNode;

@ImportStatic(StringCachingGuards.class)
@NodeChildren({
        @NodeChild("receiver"),
        @NodeChild("name"),
        @NodeChild("value")
})
abstract class ForeignWriteStringCachingHelperNode extends RubyNode {

    @Child private IsStringLikeNode isStringLikeNode;

    public abstract Object executeStringCachingHelper(VirtualFrame frame, DynamicObject receiver, Object name, Object value);

    @Specialization(guards = "isStringLike(name)")
    public Object cacheStringLikeAndForward(VirtualFrame frame, DynamicObject receiver, Object name, Object value,
            @Cached("create()") NameToJavaStringNode toJavaStringNode,
            @Cached("createNextHelper()") ForeignWriteStringCachedHelperNode nextHelper) {
        String nameAsJavaString = toJavaStringNode.executeToJavaString(frame, name);
        boolean isIVar = isIVar(nameAsJavaString);
        return nextHelper.executeStringCachedHelper(frame, receiver, name, nameAsJavaString, isIVar, value);
    }

    @Specialization(guards = "!isStringLike(name)")
    public Object passThroughNonString(VirtualFrame frame, DynamicObject receiver, Object name, Object value,
            @Cached("createNextHelper()") ForeignWriteStringCachedHelperNode nextHelper) {
        return nextHelper.executeStringCachedHelper(frame, receiver, name, null, false, value);
    }

    protected boolean isStringLike(Object value) {
        if (isStringLikeNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isStringLikeNode = insert(IsStringLikeNode.create());
        }

        return isStringLikeNode.executeIsStringLike(value);
    }

    protected boolean isIVar(String name) {
        return !name.isEmpty() && name.charAt(0) == '@';
    }

    protected ForeignWriteStringCachedHelperNode createNextHelper() {
        return ForeignWriteStringCachedHelperNodeGen.create(null, null, null, null, null);
    }

}
