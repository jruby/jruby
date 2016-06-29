/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.tools.callgraph;

import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.methods.SharedMethodInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CallGraph {

    private final Map<SharedMethodInfo, Method> sharedMethodInfoToMethod = new HashMap<>();
    private final Map<RubyRootNode, MethodVersion> rootNodeToMethodVersion = new HashMap<>();

    public synchronized void registerRootNode(RubyRootNode rootNode) {
        rootNodeToMethodVersion(rootNode);
    }

    public Method sharedMethodInfoToMethod(SharedMethodInfo sharedMethodInfo) {
        Method method = sharedMethodInfoToMethod.get(sharedMethodInfo);

        if (method == null) {
            method = new Method(this, sharedMethodInfo);
            sharedMethodInfoToMethod.put(sharedMethodInfo, method);
        }

        return method;
    }

    public MethodVersion rootNodeToMethodVersion(RubyRootNode rootNode) {
        MethodVersion methodVersion = rootNodeToMethodVersion.get(rootNode);

        if (methodVersion == null) {
            methodVersion = new MethodVersion(sharedMethodInfoToMethod(rootNode.getSharedMethodInfo()), rootNode);
            rootNodeToMethodVersion.put(rootNode, methodVersion);
        }

        return methodVersion;
    }

    public Collection<Method> getMethods() {
        return Collections.unmodifiableCollection(sharedMethodInfoToMethod.values());
    }

    public void resolve() {
        for (MethodVersion methodVersion : rootNodeToMethodVersion.values()) {
            methodVersion.resolve();
        }
    }

}
