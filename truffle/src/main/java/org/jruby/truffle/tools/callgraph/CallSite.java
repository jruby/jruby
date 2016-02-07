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

import com.oracle.truffle.api.source.SourceSection;

import java.util.ArrayList;
import java.util.List;

public class CallSite {

    private final Method method;
    private final SourceSection sourceSection;
    private final List<CallSiteVersion> versions = new ArrayList<>();

    public CallSite(Method method, SourceSection sourceSection) {
        this.method = method;
        this.sourceSection = sourceSection;
    }

    public Method getMethod() {
        return method;
    }

    public SourceSection getSourceSection() {
        return sourceSection;
    }

    public List<CallSiteVersion> getVersions() {
        return versions;
    }
}
