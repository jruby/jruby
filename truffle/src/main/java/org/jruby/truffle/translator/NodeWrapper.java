/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.translator;

import org.jruby.truffle.nodes.RubyNode;

public interface NodeWrapper {

    public static NodeWrapper IDENTITY = new NodeWrapper() {
        @Override
        public RubyNode wrap(RubyNode node) {
            return node;
        }
    };

    RubyNode wrap(RubyNode node);

}
