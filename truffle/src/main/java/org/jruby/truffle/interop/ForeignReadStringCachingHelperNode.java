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
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.cast.NameToJavaStringNode;
import org.jruby.truffle.core.string.StringCachingGuards;
import org.jruby.truffle.language.RubyNode;

@ImportStatic(StringCachingGuards.class)
@NodeChildren({
        @NodeChild("receiver"),
        @NodeChild("name")
})
abstract class ForeignReadStringCachingHelperNode extends RubyNode {

    @Child private IsStringLikeNode isStringLikeNode;

    public abstract Object executeStringCachingHelper(VirtualFrame frame, DynamicObject receiver, Object name);

    @Specialization(guards = "isStringLike(name)")
    public Object cacheStringLikeAndForward(VirtualFrame frame, DynamicObject receiver, Object name,
            @Cached("create()") NameToJavaStringNode toJavaStringNode,
            @Cached("createNextHelper()") ForeignReadStringCachedHelperNode nextHelper) {
        String nameAsJavaString = toJavaStringNode.executeToJavaString(frame, name);
        boolean isIVar = isIVar(nameAsJavaString);
        return nextHelper.executeStringCachedHelper(frame, receiver, name, nameAsJavaString, isIVar);
    }

    @Specialization(guards = { "isRubyString(receiver)", "inRange(receiver, index)" })
    public int indexString(DynamicObject receiver, int index) {
        return Layouts.STRING.getRope(receiver).get(index);
    }

    @Specialization(guards = { "isRubyString(receiver)", "!inRange(receiver, index)" })
    public int indexStringOutOfRange(DynamicObject receiver, int index) {
        return 0;
    }

    @Specialization(guards = { "!isRubyString(receiver)", "!isStringLike(name)" })
    public Object indexObject(VirtualFrame frame, DynamicObject receiver, Object name,
            @Cached("createNextHelper()") ForeignReadStringCachedHelperNode nextHelper) {
        return nextHelper.executeStringCachedHelper(frame, receiver, name, null, false);
    }

    protected boolean inRange(DynamicObject string, int index) {
        return index >= 0 && index < Layouts.STRING.getRope(string).byteLength();
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

    protected ForeignReadStringCachedHelperNode createNextHelper() {
        return ForeignReadStringCachedHelperNodeGen.create(null, null, null, null);
    }

}
