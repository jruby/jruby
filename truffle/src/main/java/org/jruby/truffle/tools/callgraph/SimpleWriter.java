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
import org.jruby.truffle.language.methods.SharedMethodInfo;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;

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

        // Can't use the SourceSection from SharedMethodInfo as it is created before we translate and so can make a covering source section
        final SourceSection sourceSection = method.getVersions().get(0).getRootNode().getSourceSection();

        final String sourceName;

        if (sourceSection == null || sourceSection.getSource() == null) {
            sourceName = "(unknown)";
        } else {
            sourceName = sourceSection.getSource().getPath();
        }

        final int startLine;

        if (sourceSection == null) {
            startLine = -1;
        } else {
            startLine = sourceSection.getStartLine();
        }

        final int endLine;

        if (sourceSection == null) {
            endLine = -1;
        } else if (sourceSection.getSource() == null) {
            endLine = sourceSection.getStartLine();
        } else {
            endLine = sourceSection.getEndLine();
        }

        stream.printf("method %d %s %s %d %d%n",
                ids.getId(method),
                sharedInfo.getName(),
                sourceName,
                startLine,
                endLine);

        for (CallSite callSite : method.getCallSites().values()) {
            write(callSite);
        }

        for (MethodVersion version : method.getVersions()) {
            write(version);
        }
    }

    private void write(MethodVersion version) {
        stream.printf("method-version %d %d%n",
                ids.getId(version.getMethod()),
                ids.getId(version));

        for (String evalCode : version.getEvalCode()) {
            stream.printf("eval %d %s%n",
                    ids.getId(version),
                    evalCode);
        }

        for (CallSiteVersion callSiteVersion : version.getCallSiteVersions().values()) {
            write(callSiteVersion);
        }

        for (Map.Entry<String, Set<String>> x : callGraph.getLocalTypes(version.getRootNode()).entrySet()) {
            final String name = x.getKey();

            for (String type : x.getValue()) {
                stream.printf("local %d %s %s%n",
                        ids.getId(version),
                        name,
                        type);
            }
        }
    }

    private void write(CallSite callSite) {
        stream.printf("callsite %d %d %d%n",
                ids.getId(callSite.getMethod()),
                ids.getId(callSite),
                callSite.getSourceSection().getStartLine());
    }

    private void write(CallSiteVersion version) {
        stream.printf("callsite-version %d %d %d%n",
                ids.getId(version.getCallSite()),
                ids.getId(version.getMethodVersion()),
                ids.getId(version));

        for (Calls calls : version.getCalls()) {
            write(version, calls);
        }
    }

    private void write(CallSiteVersion callSiteVersion, Calls calls) {
        if (calls instanceof CallsMegamorphic) {
            stream.printf("calls %d mega%n",
                    ids.getId(callSiteVersion));
        } else if (calls instanceof CallsForeign) {
            stream.printf("calls %d foreign%n",
                    ids.getId(callSiteVersion));
        } else {
            final CallsMethod callsMethod = (CallsMethod) calls;

            stream.printf("calls %d %d%n",
                    ids.getId(callSiteVersion),
                    ids.getId(callsMethod.getMethodVersion()));
        }
    }

}
