/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

@NodeChild(value = "child")
public abstract class IsFrozenNode extends RubyNode {

    private final BranchProfile errorProfile = BranchProfile.create();

    public abstract boolean executeIsFrozen(Object object);

    public void raiseIfFrozen(Object object) {
        if (executeIsFrozen(object)) {
            errorProfile.enter();
            throw new RaiseException(coreExceptions().frozenError(object, this));
        }
    }

    @Specialization
    public boolean isFrozen(boolean object) {
        return true;
    }

    @Specialization
    public boolean isFrozen(int object) {
        return true;
    }

    @Specialization
    public boolean isFrozen(long object) {
        return true;
    }

    @Specialization
    public boolean isFrozen(double object) {
        return true;
    }

    @Specialization
    protected boolean isFrozen(
            DynamicObject object,
            @Cached("createReadFrozenNode()") ReadObjectFieldNode readFrozenNode) {
        return (boolean) readFrozenNode.execute(object);
    }

    protected ReadObjectFieldNode createReadFrozenNode() {
        return ReadObjectFieldNodeGen.create(Layouts.FROZEN_IDENTIFIER, false);
    }

    @TruffleBoundary
    public static boolean isFrozen(Object object) {
        return !(object instanceof DynamicObject) ||
                ((DynamicObject) object).containsKey(Layouts.FROZEN_IDENTIFIER);
    }

}
