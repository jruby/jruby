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

import org.jruby.truffle.runtime.methods.SharedMethodInfo;

import java.util.ArrayList;
import java.util.List;

public class Method {

    private final CallGraph callGraph;
    private final SharedMethodInfo sharedInfo;
    private final List<MethodVersion> versions = new ArrayList<>();

    public Method(CallGraph callGraph, SharedMethodInfo sharedInfo) {
        this.callGraph = callGraph;
        this.sharedInfo = sharedInfo;
    }

    public List<MethodVersion> getVersions() {
        return versions;
    }

    public SharedMethodInfo getSharedInfo() {
        return sharedInfo;
    }
}
