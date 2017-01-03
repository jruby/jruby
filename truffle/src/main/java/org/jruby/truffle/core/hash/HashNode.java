/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.hash;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.ObjectNodes.ObjectIDPrimitiveNode;
import org.jruby.truffle.core.ObjectNodesFactory.ObjectIDPrimitiveNodeFactory;
import org.jruby.truffle.language.RubyBaseNode;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

public class HashNode extends RubyBaseNode {

    @Child private CallDispatchHeadNode hashNode;
    @Child private ObjectIDPrimitiveNode objectIDNode;

    private final ConditionProfile isIntegerProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isLongProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isBignumProfile = ConditionProfile.createBinaryProfile();

    public int hash(VirtualFrame frame, Object key, boolean compareByIdentity) {
        final Object hashedObject;
        if (compareByIdentity) {
            hashedObject = objectID(key);
        } else {
            hashedObject = hash(frame, key);
        }

        if (isIntegerProfile.profile(hashedObject instanceof Integer)) {
            return (int) hashedObject;
        } else if (isLongProfile.profile(hashedObject instanceof Long)) {
            return (int) (long) hashedObject;
        } else if (isBignumProfile.profile(Layouts.BIGNUM.isBignum(hashedObject))) {
            return Layouts.BIGNUM.getValue((DynamicObject) hashedObject).hashCode();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private Object hash(VirtualFrame frame, Object object) {
        if (hashNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            hashNode = insert(DispatchHeadNodeFactory.createMethodCall(true));
        }
        return hashNode.call(frame, object, "hash");
    }

    private Object objectID(Object object) {
        if (objectIDNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            objectIDNode = insert(ObjectIDPrimitiveNodeFactory.create(null));
        }
        return objectIDNode.executeObjectID(object);
    }

}
