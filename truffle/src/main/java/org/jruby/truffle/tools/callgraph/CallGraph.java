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

import com.oracle.truffle.api.TruffleOptions;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.methods.SharedMethodInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CallGraph {

    private final Map<SharedMethodInfo, Method> sharedMethodInfoToMethod = new HashMap<>();
    private final Map<RubyRootNode, MethodVersion> rootNodeToMethodVersion = new HashMap<>();
    private final Map<RubyRootNode, Map<String, Set<String>>> localTypes = new HashMap<>();

    public synchronized void registerRootNode(RubyRootNode rootNode) {
        rootNodeToMethodVersion(rootNode);
    }

    public synchronized void recordLocalWrite(RubyRootNode rootNode, String name, String type) {
        if (name.equals("foo_a") && type.equals("Object")) {
            throw new UnsupportedOperationException();
        }

        final Map<String, Set<String>> rootNodeLocalTypes = localTypes.computeIfAbsent(rootNode,
                (k) -> new HashMap<>());

        final Set<String> rootNodeLocalTypeSet = rootNodeLocalTypes.computeIfAbsent(name,
                (k) -> new HashSet<>());

        rootNodeLocalTypeSet.add(type);
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

    public Map<String, Set<String>> getLocalTypes(RubyRootNode rootNode) {
        final Map<String, Set<String>> rootNodeLocalTypes = localTypes.get(rootNode);

        if (rootNodeLocalTypes == null) {
            return Collections.<String, Set<String>>emptyMap();
        } else {
            return rootNodeLocalTypes;
        }
    }

    public void resolve() {
        // Call graph currently doesn't work with AOT due to runtime reflection.
        if (!TruffleOptions.AOT) {
            rootNodeToMethodVersion.values().forEach(MethodVersion::resolve);
        }
    }
}
