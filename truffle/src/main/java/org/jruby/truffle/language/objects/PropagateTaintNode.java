/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;

public class PropagateTaintNode extends Node {

    @Child private IsTaintedNode isTaintedNode = IsTaintedNode.create();
    @Child private TaintNode taintNode;

    private final ConditionProfile taintProfile = ConditionProfile.createBinaryProfile();

    public static PropagateTaintNode create() {
        return new PropagateTaintNode();
    }

    public void propagate(DynamicObject source, Object target) {
        if (taintProfile.profile(isTaintedNode.executeIsTainted(source))) {
            taint(target);
        }
    }

    private void taint(Object target) {
        if (taintNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            taintNode = insert(TaintNode.create());
        }
        taintNode.executeTaint(target);
    }

}
