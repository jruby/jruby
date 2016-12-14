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

import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import org.jruby.truffle.language.RubyRootNode;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MethodVersion {

    private final Method method;
    private final RubyRootNode rootNode;
    private final Map<CallSite, CallSiteVersion> callSiteVersions = new HashMap<>();
    private final Set<String> evalCode = new HashSet<>();

    public MethodVersion(Method method, RubyRootNode rootNode) {
        this.method = method;
        this.rootNode = rootNode;
        method.getVersions().add(this);
    }

    public Method getMethod() {
        return method;
    }

    public Map<CallSite, CallSiteVersion> getCallSiteVersions() {
        return callSiteVersions;
    }

    public void resolve() {
        rootNode.accept(node -> {
            resolve(node);
            return true;
        });
    }

    private void resolve(Node node) {
        if (node instanceof DirectCallNode || node instanceof IndirectCallNode) {
            final CallSiteVersion callSiteVersion = getCallSiteVersion(node);

            final Calls calls;

            if (node instanceof DirectCallNode) {
                final DirectCallNode directNode = (DirectCallNode) node;
                final RootNode rootNode = directNode.getCurrentRootNode();

                if (rootNode instanceof RubyRootNode) {
                    final MethodVersion methodVersion = method.getCallGraph().rootNodeToMethodVersion((RubyRootNode) rootNode);
                    calls = new CallsMethod(methodVersion);
                } else {
                    calls = CallsForeign.INSTANCE;
                }
            } else {
                calls = CallsMegamorphic.INSTANCE;
            }

            callSiteVersion.getCalls().add(calls);
        } else if (node.getClass().getName().indexOf("EvalNoBindingCachedNode") != -1) {
            try {
                final Field f = node.getClass().getDeclaredField("cachedSource");
                f.setAccessible(true);
                evalCode.add(f.get(node).toString());
            } catch (IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
    }

    private CallSiteVersion getCallSiteVersion(Node node) {
        final CallSite callSite = method.getCallSite(node);

        CallSiteVersion callSiteVersion = callSiteVersions.get(callSite);

        if (callSiteVersion == null) {
            callSiteVersion = new CallSiteVersion(callSite, this);
            callSiteVersions.put(callSite, callSiteVersion);
        }

        return callSiteVersion;
    }

    public RubyRootNode getRootNode() {
        return rootNode;
    }

    public Set<String> getEvalCode() {
        return evalCode;
    }
}
