/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objectstorage;

import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.jruby.truffle.runtime.objectstorage.ObjectLayout;
import org.jruby.truffle.runtime.objectstorage.ObjectStorage;

@NodeInfo(cost = NodeCost.POLYMORPHIC)
public class ReadMissingObjectFieldNode extends ReadObjectFieldChainNode {

    private final ObjectLayout objectLayout;

    public ReadMissingObjectFieldNode(ObjectLayout objectLayout, ReadObjectFieldNode next) {
        super(next);
        this.objectLayout = objectLayout;
    }

    @Override
    public Object execute(ObjectStorage object) {
        if (object.getObjectLayout() == objectLayout) {
            return null;
        } else {
            return next.execute(object);
        }
    }

}
