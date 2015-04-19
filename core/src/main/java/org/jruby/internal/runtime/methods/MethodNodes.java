/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.internal.runtime.methods;

import org.jruby.ast.ArgsNode;
import org.jruby.ast.Node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MethodNodes {

    private final ArgsNode argsNode;
    private final Node bodyNode;

    public MethodNodes(ArgsNode argsNode, Node bodyNode) {
        assert argsNode != null;
        assert bodyNode != null;

        this.argsNode = argsNode;
        this.bodyNode = bodyNode;
    }

    public ArgsNode getArgsNode() {
        return argsNode;
    }

    public Node getBodyNode() {
        return bodyNode;
    }

}
