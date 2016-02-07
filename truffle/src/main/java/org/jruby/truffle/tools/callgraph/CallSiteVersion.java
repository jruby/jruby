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

import java.util.ArrayList;
import java.util.List;

public class CallSiteVersion {

    private final CallSite callSite;
    private final MethodVersion methodVersion;
    private final List<Calls> calls = new ArrayList<>();

    public CallSiteVersion(CallSite callSite, MethodVersion methodVersion) {
        this.callSite = callSite;
        this.methodVersion = methodVersion;
        callSite.getVersions().add(this);
    }

    public CallSite getCallSite() {
        return callSite;
    }

    public List<Calls> getCalls() {
        return calls;
    }

    public Object getMethodVersion() {
        return methodVersion;
    }
}
