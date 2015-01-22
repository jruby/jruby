/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes;

import com.oracle.truffle.api.source.NullSourceSection;

/**
 * Source sections used for core method nodes.
 */
public final class CoreSourceSection extends NullSourceSection {

    private final String className;
    private final String methodName;

    public CoreSourceSection(String className, String methodName) {
        super("core", String.format("%s#%s", className, methodName));
        this.className = className;
        this.methodName = methodName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

}
