/*
 * Copyright (c) 2016, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.hash;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.objects.IsFrozenNode;
import org.jruby.truffle.language.objects.IsFrozenNodeGen;

@NodeChildren({@NodeChild("key"), @NodeChild("compareByIdentity")})
public abstract class FreezeHashKeyIfNeededNode extends RubyNode {

    @Child private IsFrozenNode isFrozenNode;
    @Child private CallDispatchHeadNode dupNode;
    @Child private CallDispatchHeadNode freezeNode;

    public abstract Object executeFreezeIfNeeded(VirtualFrame frame, Object key, boolean compareByIdentity);

    @Specialization(guards = {"isRubyString(string)", "isFrozen(string)"})
    Object alreadyFrozen(DynamicObject string, boolean compareByIdentity) {
        return string;
    }

    @Specialization(guards = {"isRubyString(string)", "!isFrozen(string)", "!compareByIdentity"})
    Object dupAndFreeze(VirtualFrame frame, DynamicObject string, boolean compareByIdentity) {
        return freeze(frame, dup(frame, string));
    }

    @Specialization(guards = {"isRubyString(string)", "!isFrozen(string)", "compareByIdentity"})
    Object compareByIdentity(VirtualFrame frame, DynamicObject string, boolean compareByIdentity) {
        return string;
    }

    @Specialization(guards = "!isRubyString(value)")
    Object passThrough(Object value, boolean compareByIdentity) {
        return value;
    }

    protected boolean isFrozen(Object value) {
        if (isFrozenNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isFrozenNode = insert(IsFrozenNodeGen.create(null));
        }
        return isFrozenNode.executeIsFrozen(value);
    }

    private Object dup(VirtualFrame frame, Object value) {
        if (dupNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            dupNode = insert(DispatchHeadNodeFactory.createMethodCall());
        }
        return dupNode.call(frame, value, "dup");
    }

    private Object freeze(VirtualFrame frame, Object value) {
        if (freezeNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            freezeNode = insert(DispatchHeadNodeFactory.createMethodCall());
        }
        return freezeNode.call(frame, value, "freeze");
    }

}
