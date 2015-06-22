/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.dsl.NodeFactory;
import org.jruby.truffle.nodes.RubyNode;

public class RubiniusPrimitiveNodeConstructor implements RubiniusPrimitiveConstructor {

    private final RubiniusPrimitive annotation;
    private final NodeFactory<? extends RubyNode> factory;

    public RubiniusPrimitiveNodeConstructor(RubiniusPrimitive annotation, NodeFactory<? extends RubyNode> factory) {
        this.annotation = annotation;
        this.factory = factory;
    }

    public RubiniusPrimitive getAnnotation() {
        return annotation;
    }

    public NodeFactory<? extends RubyNode> getFactory() {
        return factory;
    }

}
