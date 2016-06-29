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

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.language.methods.SharedMethodInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Method {

    private final CallGraph callGraph;
    private final SharedMethodInfo sharedInfo;
    private final List<MethodVersion> versions = new ArrayList<>();
    private final Map<SourceSection, CallSite> callSites = new HashMap<>();

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

    public Map<SourceSection, CallSite> getCallSites() {
        return callSites;
    }

    public CallSite getCallSite(Node node) {
        final SourceSection sourceSection = node.getEncapsulatingSourceSection();

        CallSite callSite = callSites.get(sourceSection);

        if (callSite == null) {
            callSite = new CallSite(this, sourceSection);
            callSites.put(sourceSection, callSite);
        }

        return callSite;
    }

    public CallGraph getCallGraph() {
        return callGraph;
    }
}
