/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects.shared;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;

public class PropagateSharingNode extends Node {

    @Child private IsSharedNode isSharedNode;
    @Child private WriteBarrierNode writeBarrierNode;

    public static PropagateSharingNode create() {
        return new PropagateSharingNode();
    }

    public PropagateSharingNode() {
        isSharedNode = IsSharedNodeGen.create();
        writeBarrierNode = WriteBarrierNode.create();
    }

    public void propagate(DynamicObject source, Object value) {
        if (isSharedNode.executeIsShared(source)) {
            writeBarrierNode.executeWriteBarrier(value);
        }
    }

}
