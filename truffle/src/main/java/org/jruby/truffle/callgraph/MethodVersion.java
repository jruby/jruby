/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.callgraph;

import org.jruby.truffle.nodes.RubyRootNode;

public class MethodVersion {

    private final Method method;
    private final RubyRootNode rootNode;

    public MethodVersion(Method method, RubyRootNode rootNode) {
        this.method = method;
        this.rootNode = rootNode;
        method.getVersions().add(this);
    }

    public Method getMethod() {
        return method;
    }
}
