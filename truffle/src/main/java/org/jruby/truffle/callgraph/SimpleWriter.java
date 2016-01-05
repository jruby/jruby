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

import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;
import org.jruby.truffle.runtime.util.IdProvider;

import java.io.PrintStream;

public class SimpleWriter {

    private final CallGraph callGraph;
    private final PrintStream stream;

    private final IdProvider ids = new IdProvider();

    public SimpleWriter(CallGraph callGraph, PrintStream stream) {
        this.callGraph = callGraph;
        this.stream = stream;
    }

    public void write() {
        for (Method method : callGraph.getMethods()) {
            write(method);
        }
    }

    private void write(Method method) {
        final SharedMethodInfo sharedInfo = method.getSharedInfo();
        final SourceSection sourceSection = sharedInfo.getSourceSection();

        final String sourceName;

        if (sourceSection.getSource() == null) {
            sourceName = "(unknown)";
        } else {
            sourceName = sourceSection.getSource().getName();
        }

        final int endLine;

        if (sourceSection.getSource() == null) {
            endLine = sourceSection.getStartLine();
        } else {
            endLine = sourceSection.getEndLine();
        }

        stream.printf("method %d %s %s %d %d\n",
                ids.getId(method),
                sharedInfo.getName(),
                sourceName,
                sourceSection.getStartLine(),
                endLine);

        for (MethodVersion version : method.getVersions()) {
            write(version);
        }
    }

    private void write(MethodVersion version) {
        stream.printf("method-version %d %d\n",
                ids.getId(version.getMethod()),
                ids.getId(version));
    }

}
